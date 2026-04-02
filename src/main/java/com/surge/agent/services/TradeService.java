package com.surge.agent.services;


import com.surge.agent.enums.TradeAction;
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
import org.web3j.protocol.Web3j;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * TradeService — V3 final.
 *
 * Fixes vs the V2 version that was provided:
 *
 *   1. Python /analyze is now called to get AITradeDecision BEFORE building
 *      the TradeIntent. V2 skipped this entirely — the trade executed with no
 *      AI reasoning and the validation artifact had no agent verdicts.
 *
 *   2. minAmountOut is now slippage-protected (ATR-based). V2 hardcoded
 *      BigInteger.ZERO, meaning the router would accept any amount out —
 *      infinite slippage. This makes the risk guard "FAILED".
 *
 *   3. riskHash now uses keccak256 (Solidity-compatible). V2 used SHA-256,
 *      which produces a valid 32-byte array but doesn't match anything the
 *      router can verify.
 *
 *   4. ValidationService.postTradeArtifact() is now called with the full
 *      signature: (intent, risk, decision, marketState, entryPrice, txHash,
 *      blockNumber). V2 called the old 3-arg version with no decision.
 *
 *   5. TradeRecord is registered with TradeMonitorService after execution so
 *      TP/SL monitoring starts immediately and feedback loops back to Python.
 *
 *   6. HOLD and non-actionable decisions are skipped cleanly. V2 would have
 *      tried to build and submit a HOLD trade intent to the router.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final IdentityService       identityService;
    private final PythonAIClient        pythonAIClient;
    private final BlockchainService     blockchainService;
    private final ValidationService     validationService;
    private final TradeMonitorService   tradeMonitorService;
    private final MarketDataService marketDataService;
    private final RiskManagementService riskService;
    private final EIP712Signer eip712Signer;
    private final Web3j                 web3j;
    private final NewsAggregator newsAggregator;

    @Value("${contract.usdc}")
    private String usdcAddress;

    @Value("${contract.weth:}")
    private String wethAddress;

    // ─────────────────────────────────────────────────────────────────────
    // MAIN ENTRY POINT
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Full V3 trade flow:
     *
     *   1. Guard checks (registered, circuit breaker)
     *   2. Fetch MarketState + call Python /analyze
     *   3. Skip if HOLD / low confidence / risk check fails
     *   4. Build TradeIntent with slippage-protected minAmountOut
     *   5. Compute keccak256 riskHash from AITradeDecision
     *   6. EIP-712 sign
     *   7. Execute on-chain, capture txHash + blockNumber
     *   8. Post validation artifact (with full decision + market context)
     *   9. Register TradeRecord for TP/SL monitoring
     */
    public void processSignal(TradeSignal signal) {
        try {
            // ── Guard 1: Identity ──────────────────────────────────────────
            if (!identityService.isRegistered()) {
                log.error("Agent not registered — skipping trade. Call /api/agent/register-uri first.");
                return;
            }

            // ── Guard 2: Circuit breaker ───────────────────────────────────
            if (riskService.isCircuitBreakerTripped()) {
                log.warn("Circuit breaker TRIPPED (drawdown >= 8%) — all new trades halted.");
                return;
            }

            BigInteger agentId = identityService.getAgentId();

            // ── Step 1: Fetch current market state ─────────────────────────
            MarketState marketState = marketDataService.getLatestMarketState();
            if (marketState == null) {
                log.warn("No market state available — skipping trade.");
                return;
            }
            double currentPrice = marketState.getCurrentPrice() != null
                    ? marketState.getCurrentPrice().doubleValue() : 0.0;
            if (currentPrice <= 0) {
                log.warn("Invalid current price {} — skipping trade.", currentPrice);
                return;
            }

            // ── Step 2: Call Python AI council ─────────────────────────────
            // This is the core fix: V2 skipped this entirely.
            // analyzeMarket() posts to /api/v1/analyze with the full MarketState.
            String newsContext = newsAggregator.getAINewsContext();

            AITradeDecision decision = pythonAIClient.analyzeMarket(new AnalysisRequest(
                    marketState,
                    newsContext,
                    0.0,
                    0,
                    riskService.getVaultBalanceUsdc())
            );

            log.info("AI decision: action={} confidence={} regime={} R:R={} tradeId={}",
                    decision.getAction(), decision.getConfidence(),
                    decision.getMarketRegime(), decision.getRewardRiskRatio(),
                    decision.hasTradeId() ? decision.getTradeId() : "pending");

            // ── Step 3: Skip non-actionable decisions ──────────────────────
            if (!decision.isActionable()) {
                log.info("AI returned {} — no trade submitted.", decision.getAction());
                return;
            }
            if (decision.getConfidence() < 0.60) {
                log.info("Confidence {} below floor 0.60 — skipping.", decision.getConfidence());
                return;
            }
            if ("CRITICAL".equals(decision.getRiskLevel())) {
                log.info("Risk level CRITICAL — skipping.");
                return;
            }

            // ── Step 4: Build TradeIntent ──────────────────────────────────
            String tokenIn  = usdcAddress;
            String tokenOut = (wethAddress != null && !wethAddress.isBlank())
                    ? wethAddress : usdcAddress;

            // Position size: 1000 USDC (6-decimal) for BUY; 0 for SELL
            // In production replace with Kelly-sized amount from RiskManagementService
            BigInteger amountIn = TradeAction.BUY.equals(decision.getAction())
                    ? BigInteger.valueOf(1_000_000_000L)   // 1000 USDC (6 decimals)
                    : BigInteger.ZERO;

            // FIX: slippage-protected minAmountOut — never submit BigInteger.ZERO
            BigInteger minAmountOut = computeMinAmountOut(amountIn, marketState);

            // FIX: keccak256 riskHash (Solidity-compatible, matches router verify)
            byte[] riskHash = computeRiskHash(decision);

            TradeIntent intent = TradeIntent.builder()
                    .agentId(agentId)
                    .tokenIn(tokenIn)
                    .tokenOut(tokenOut)
                    .amountIn(amountIn)
                    .minAmountOut(minAmountOut)
                    .deadline(BigInteger.valueOf(Instant.now().getEpochSecond() + 600))
                    .riskParams(riskHash)
                    .build();

            // ── Step 5: EIP-712 sign ───────────────────────────────────────
            byte[] signature = eip712Signer.signTradeIntent(intent);

            // ── Step 6: Execute on-chain ───────────────────────────────────
            var receipt = blockchainService.executeTrade(intent, signature);
            String txHash = receipt.getTransactionHash();
            Long blockNumber = receipt.getBlockNumber() != null
                    ? receipt.getBlockNumber().longValue() : null;

            log.info("Trade executed | tx={} block={} action={} price={}",
                    txHash.substring(0, 12) + "...", blockNumber,
                    decision.getAction(), currentPrice);

            // ── Step 7: Post validation artifact ──────────────────────────
            // This is the critical fix: pass the full decision + marketState so
            // the artifact contains agent verdicts, regime confidence, and all
            // market context fields. V2 called the 3-arg version and got none of that.
            byte[] artifactHash = validationService.postTradeArtifact(
                    intent,
                    null,            // RiskAssessment — pass if you have a separate Python risk call
                    decision,
                    marketState,
                    currentPrice,
                    txHash,
                    blockNumber
            );

            // ── Step 8: Register with TradeMonitorService ──────────────────
            // Resolves the tradeId from the decision (Python-issued) or falls back
            // to the intent nonce. This is what links the monitoring to the artifact.
            String tradeId = decision.hasTradeId()
                    ? decision.getTradeId()
                    : "intent_" + intent.getNonce();

            TradeRecord record = TradeRecord.fromDecision(
                    tradeId, agentId, currentPrice, intent, decision);
            record.setExecutionTxHash(txHash);
            tradeMonitorService.register(record);

            log.info("Trade complete | tradeId={} TP={} SL={} artifactHash={}",
                    tradeId,
                    currentPrice * decision.getTakeProfitMultiplier(),
                    currentPrice * decision.getStopLossMultiplier(),
                    Numeric.toHexString(artifactHash).substring(0, 12) + "...");

        } catch (Exception e) {
            log.error("Trade flow failed for signal {}: {}", signal, e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Computes a slippage-protected minimum amount out.
     *
     * Slippage = max(0.5%, ATR% * 1.5) — widens automatically in volatile markets.
     * Capped at 3% — never accept more than 3% slippage.
     *
     * FIX: V2 hardcoded BigInteger.ZERO which means the router accepts any amount
     * out. This makes the "reward_risk_ratio" risk guard fail every single trade.
     */
    private BigInteger computeMinAmountOut(BigInteger amountIn, MarketState mkt) {
        if (amountIn == null || amountIn.compareTo(BigInteger.ZERO) <= 0) {
            return BigInteger.ZERO;
        }
        double price   = mkt.getCurrentPrice() != null ? mkt.getCurrentPrice().doubleValue() : 0;
        double atr     = mkt.getAtr();
        double atrPct  = price > 0 ? atr / price : 0;
        double slippage = Math.min(0.03, Math.max(0.005, atrPct * 1.5));
        return new BigDecimal(amountIn)
                .multiply(BigDecimal.valueOf(1.0 - slippage))
                .toBigInteger();
    }

    /**
     * Computes a 32-byte keccak256 hash of the AI decision for the riskParams field.
     *
     * FIX: V2 used SHA-256 which produces a valid byte array but isn't
     * Solidity-compatible. keccak256 matches what the router's verify() expects.
     *
     * The hash encodes action + confidence + regime + R:R so the router can
     * verify the risk parameters haven't been tampered with between AI call and
     * trade submission.
     */
    private byte[] computeRiskHash(AITradeDecision decision) {
        String riskString = String.format("%s|%.4f|%s|%.3f|%.4f|%.4f",
                decision.getAction(),
                decision.getConfidence(),
                decision.getMarketRegime(),
                decision.getRewardRiskRatio(),
                decision.getTakeProfitPct(),
                decision.getStopLossPct());
        // Hash.sha3 is web3j's keccak256 — matches Solidity keccak256()
        return Hash.sha3(riskString.getBytes(StandardCharsets.UTF_8));
    }
}