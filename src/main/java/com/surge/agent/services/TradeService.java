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
import org.web3j.crypto.Credentials;
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
    private final Credentials credentials;

    @Value("${contract.usdc}")
    private String usdcAddress;

    @Value("${contract.weth}")
    private String wethAddress;

    @Value("${trade.service.confidence}")
    private Double tradeConfidence;

    @Value("${trade.service.allowed_time}")
    private Long tradeAllowedTime;

    @Value("${ai.pair}")
    private String PAIR;

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
     * ERC-8004 Step 3 Compliant Execution — Updated for Live Hackathon
     */
    private double computeKellyFraction(double confidence, double takeProfitPct, double stopLossPct) {
        double b        = stopLossPct > 0 ? takeProfitPct / stopLossPct : 2.0;
        double q        = 1.0 - confidence;
        double fStar    = (b * confidence - q) / b;
        double halfKelly = Math.max(0.0, fStar * 0.5);
        return Math.min(halfKelly, 0.05); // hard cap: never risk more than 5% per trade
    }
    private void executeAutonomousTrade(AITradeDecision decision, MarketState mkt) throws Exception {
        BigInteger agentId = identityService.getAgentId();
        double currentPrice = mkt.getCurrentPrice().doubleValue();

        // 1. Calculate Amount with 18-decimal scaling
        // We convert the vault double (e.g. 1500.50) to a scaled BigInteger (10^18)
        double balanceUsd  = riskService.getVaultBalanceUsdc();
        double kellyFrac   = computeKellyFraction(
                decision.getConfidence(),
                decision.getTakeProfitPct(),
                decision.getStopLossPct());
        double betUsd      = balanceUsd * kellyFrac;

        if (betUsd < 1.0) {
            log.warn("Position too small: ${:.2f} — skipping.", betUsd);
            return;
        }

        // amountUsdScaled = USD * 100  (e.g. $10.29 → 1029)
        // capped at RiskRouter max $500 = 50000
        BigInteger amountUsdScaled = BigInteger.valueOf((long)(betUsd * 100))
                .min(BigInteger.valueOf(50_000));

        // 2. Slippage Management (Basis Points for the live router)
        int slippageBps = computeSlippageBps(mkt);

        // 3. Build & Sign Intent (Pair-based schema)
        TradeIntent intent = TradeIntent.builder()
                .agentId(agentId)
                .agentWallet(credentials.getAddress())
                .pair(PAIR)
                .action(decision.getAction().name())
                .amountUsdScaled(amountUsdScaled)
                .maxSlippageBps(BigInteger.valueOf(slippageBps))
                .nonce(BigInteger.valueOf(System.currentTimeMillis()))
                .deadline(BigInteger.valueOf(Instant.now().getEpochSecond() + 600))
                .build();

        byte[] signature = eip712Signer.signTradeIntent(intent);

        // 4. On-chain submission & Receipt Handling
        // Note: If your BlockchainService uses a wrapper like Web3j, it returns a TransactionReceipt
        var receipt = blockchainService.executeTrade(intent, signature);

        // Safety check: ensure we handle null receipts or missing methods
        String txHash = (receipt != null) ? receipt.getTransactionHash() : "0x_failed";
        Long blockNumber = (receipt != null && receipt.getBlockNumber() != null)
                ? receipt.getBlockNumber().longValue() : 0L;

        // Check transaction status: "0x1" is success in EVM
        boolean success = receipt != null && "0x1".equals(receipt.getStatus());

        if (!success) {
            log.error("Trade transaction REVERTED on-chain. Hash: {}", txHash);
            // Still post the artifact so judges see the attempt and the AI's reasoning
            validationService.postTradeArtifact(intent, null, decision, mkt, currentPrice, txHash, blockNumber);
            return;
        }

        // 5. Artifact & Monitoring (Success path)
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



    private int computeSlippageBps(MarketState mkt) {
        double price = mkt.getCurrentPrice().doubleValue();
        // ATR % represents how much the price typically moves in a single bar
        double atrPct = price > 0 ? (mkt.getAtr() / price) : 0;

        // We scale the slippage to be 1.5x the current volatility (ATR)
        // capped between 0.5% (50 bps) and 3.0% (300 bps)
        double slippage = Math.min(0.03, Math.max(0.005, atrPct * 1.5));

        // Convert decimal (0.01) to Basis Points (100)
        return (int) (slippage * 10000);
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