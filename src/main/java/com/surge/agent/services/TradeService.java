package com.surge.agent.services;

import com.surge.agent.model.TradeIntent;
import com.surge.agent.model.TradeSignal;
import com.surge.agent.dto.AITradeDecision;
import com.surge.agent.dto.MarketState;
import com.surge.agent.dto.TradeRecord;
import com.surge.agent.dto.request.AnalysisRequest;
import com.surge.agent.services.market.MarketDataService;
import com.surge.agent.services.news.NewsAggregator;
import com.surge.agent.utils.EIP712Signer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final IdentityService identityService;
    private final PythonAIClient pythonAIClient;
    private final BlockchainService blockchainService;
    private final ValidationService validationService;
    private final TradeMonitorService tradeMonitorService;
    private final MarketDataService marketDataService;
    private final RiskManagementService riskService;
    private final EIP712Signer eip712Signer;
    private final NewsAggregator newsAggregator;

    @Value("${contract.usdc}")
    private String usdcAddress;

    @Value("${contract.weth}")
    private String wethAddress;

    @Value("${trade.service.confidence}")
    private Double tradeConfidence;

    @Value("${trade.service.allowed_time}")
    private Long tradeAllowedTime;
    /**
     * Entry point for incoming trade signals.
     */
    public void processSignal(TradeSignal signal) {
        try {
            // ── Guard : Stall signal check  ────────────────────────
            if (Instant.now().getEpochSecond() - signal.getTimestamp() > tradeAllowedTime) {
                log.warn("Signal for {} is stale. Skipping.", signal.getAsset());
                return;
            }

            // ── Guard : Identity & Circuit Breaker ────────────────────────
            if (!identityService.isRegistered()) {
                log.error("Agent not registered. Skipping signal for {}", signal.getTokenSymbol());
                return;
            }

            if (riskService.isCircuitBreakerTripped()) {
                log.warn("Circuit breaker TRIPPED (drawdown too high) — halting execution.");
                return;
            }

            // ── Fetch Market State ───────────────────────────────
            MarketState marketState = marketDataService.getLatestMarketState();
            if (marketState == null || marketState.getCurrentPrice().doubleValue() <= 0) {
                log.warn("Invalid market state for signal {}", signal.getTokenSymbol());
                return;
            }

            // ── AI Analysis (Brain) ───────────────────────────────
            String newsContext = newsAggregator.getAINewsContext();
            AITradeDecision decision = pythonAIClient.analyzeMarket(new AnalysisRequest(
                    marketState,
                    newsContext,
                    0.0,
                    0,
                    riskService.getVaultBalanceUsdc())
            );

            // ── Risk Veto & Decision Filtering ────────────────────
            if (!decision.isActionable() || decision.getConfidence() < tradeConfidence) {
                log.info("Trade skipped: action={} confidence={}", decision.getAction(), decision.getConfidence());
                return;
            }

            // Using RiskManagementService.isTradeSafe for fear/greed & spread checks
            if (!riskService.isTradeSafe(decision, marketState.getSpread(), marketState.getFearGreedIndex())) {
                log.warn("RiskManagementService vetoed the trade based on market conditions.");
                // Post an artifact even though no TX was sent
                validationService.postVetoArtifact(
                        decision,    // Why the AI wanted it
                        marketState, // What the market looked like
                        "RISK_VETO: SPREAD_OR_FEAR"
                );
                return;
            }

            // ── Execute Flow ─────────────────────────────
            executeAutonomousTrade(decision, marketState);

        } catch (Exception e) {
            log.error("Trade flow failed: {}", e.getMessage(), e);
        }
    }

    /**
     * ERC-8004 Step 3 Compliant Execution
     */
    private void executeAutonomousTrade(AITradeDecision decision, MarketState mkt) throws Exception {
        BigInteger agentId = identityService.getAgentId();
        String tradeId = validationService.resolveTradeId(decision);
        double currentPrice = mkt.getCurrentPrice().doubleValue();

        // 1. Calculate Amount (Kelly Criterion)
        BigInteger amountIn = riskService.calculateSafePositionSize(
                decision.getConfidence(),
                BigInteger.valueOf((long) riskService.getVaultBalanceUsdc()),
                decision.getTakeProfitPct(),
                decision.getStopLossPct()
        );

        if (amountIn.compareTo(BigInteger.ZERO) <= 0) {
            log.warn("Kelly size 0. Aborting.");
            return;
        }

        // 2. Slippage & Risk Packing (The "Step 3" Fix)
        BigInteger minAmountOut = computeMinAmountOut(amountIn, mkt);
        byte[] packedRisk = packRiskParams(decision); // Contract-readable bytes

        // 3. Build & Sign Intent
        TradeIntent intent = TradeIntent.builder()
                .agentId(agentId)
                .tokenIn(usdcAddress)
                .tokenOut(wethAddress)
                .amountIn(amountIn)
                .minAmountOut(minAmountOut)
                .deadline(BigInteger.valueOf(Instant.now().getEpochSecond() + 600))
                .riskParams(packedRisk)
                .build();

        byte[] signature = eip712Signer.signTradeIntent(intent);

        // 4. On-chain submission
        var receipt = blockchainService.executeTrade(intent, signature);
        String txHash = receipt.getTransactionHash();
        Long blockNumber = receipt.getBlockNumber() != null ? receipt.getBlockNumber().longValue() : null;

        //  Check if the transaction actually succeeded before tracking it as an open position
        if (!receipt.isStatusOK()) {
            log.error("Trade {} reverted on-chain. Posting failure artifact.", txHash);

            // Post artifact: judges see the attempt, no register the artifact
            validationService.postTradeArtifact(intent, null, decision, mkt, currentPrice, txHash, blockNumber);
            return; // do not increment counters or monitor a failed trade.
        }
        // 5. Artifact & Monitoring
        postTradeLogging(intent, decision, mkt, txHash, blockNumber);
    }

    private void postTradeLogging(TradeIntent intent, AITradeDecision decision, MarketState mkt, String txHash, Long blockNumber) throws Exception {
        // Post Validation Artifact
        validationService.postTradeArtifact(intent, null, decision, mkt, mkt.getCurrentPrice().doubleValue(), txHash, blockNumber);

        // Register with Monitor
        String tradeId = decision.hasTradeId() ? decision.getTradeId() : "intent_" + System.currentTimeMillis();
        TradeRecord record = TradeRecord.fromDecision(tradeId, intent.getAgentId(), mkt.getCurrentPrice().doubleValue(), intent, decision);
        record.setExecutionTxHash(txHash);
        tradeMonitorService.register(record);

        // Update Risk Counters
        riskService.onPositionOpened();
        riskService.incrementTradeCount();

        log.info("Successfully executed trade {}. Tx: {}", tradeId, txHash);
    }

    private byte[] packRiskParams(AITradeDecision decision) {
        // Packs SL (2 bytes), TP (2 bytes), Slippage (2 bytes) into 32-byte array
        short slBps = (short) (decision.getStopLossPct() * 10000);
        short tpBps = (short) (decision.getTakeProfitPct() * 10000);
        short slipBps = 50; // 0.5% default

        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.putShort(slBps);   // Bytes 0-1
        buffer.putShort(tpBps);   // Bytes 2-3
        buffer.putShort(slipBps); // Bytes 4-5
        return buffer.array();
    }

    private BigInteger computeMinAmountOut(BigInteger amountIn, MarketState mkt) {
        if (amountIn == null || amountIn.compareTo(BigInteger.ZERO) <= 0) return BigInteger.ZERO;
        double price = mkt.getCurrentPrice().doubleValue();
        double atrPct = price > 0 ? (mkt.getAtr() / price) : 0;
        double slippage = Math.min(0.03, Math.max(0.005, atrPct * 1.5));

        return new BigDecimal(amountIn)
                .multiply(BigDecimal.valueOf(1.0 - slippage))
                .toBigInteger();
    }

    private byte[] computeReasoningHash(AnalysisRequest req, AITradeDecision dec) {
        // Hash the market inputs + the AI's logic string
        String uniqueContext = req.getMarket().toString() + dec.getReasoning();
        return Hash.sha3(uniqueContext.getBytes(StandardCharsets.UTF_8));
    }
}