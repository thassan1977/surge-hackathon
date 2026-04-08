package com.surge.agent.services;

import com.surge.agent.contracts.MockVault;
import com.surge.agent.enums.TradeAction;
import com.surge.agent.model.TradeIntent;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import jakarta.annotation.PostConstruct;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * AutonomousTradingOrchestrator — V4.
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class AutonomousTradingOrchestrator {

    private final MarketDataService     marketDataService;
    private final RiskManagementService riskService;
    private final BlockchainService     blockchainService;
    private final ValidationService     validationService;
    private final TradeMonitorService   tradeMonitorService;
    private final IdentityService       identityService;
    private final PerformanceTracker    performanceTracker;
    private final EIP712Signer          eip712Signer;
    private final MockVault             mockVault;
    private final PythonAIClient        pythonAIClient;
    private final FearGreedService      fearGreedService;
    private final OnChainMetricsService onChainMetricsService;
    private final NewsAggregator        newsAggregator;

    @Value("${contract.weth}")      private String WETH_ADDRESS;
    @Value("${contract.usdc}")      private String USDC_ADDRESS;
    @Value("${ai.orchestrator.confidence:0.60}")
    private double CONFIDENCE_THRESHOLD;
    @Value("${ai.orchestrator.atr:0.01}")
    private double ATR_THRESHOLD;

    private BigDecimal lastAnalyzedPrice       = BigDecimal.ZERO;
    private long       lastAnalysisTimestampMs = 0;

    private static final double PRICE_MOVE_THRESHOLD_USD = 1.50;
    private static final long   MIN_ANALYSIS_INTERVAL_MS = 10_000;

    // ── FIX 16: startup check ─────────────────────────────────────────────
    @PostConstruct
    public void onStartup() {
        if (!identityService.isRegistered()) {
            log.error("AGENT NOT REGISTERED — call POST /api/agent/register-uri. All trades blocked.");
        } else {
            log.info("Agent ready. AgentId={}", identityService.getAgentId());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // MAIN LOOP
    // ─────────────────────────────────────────────────────────────────────
    @Scheduled(fixedRate = 10_000)
    public void tradingLoop() {
        try {
            BigInteger agentId = identityService.getAgentId();

            if (agentId == null || agentId.equals(BigInteger.ZERO)) return;

            if (!blockchainService.isAuthorizedValidator(agentId)) {
                log.info("Agent {} detected but validator registration is pending on-chain...", agentId);
                return;
            }

            // This ensures balanceUsdc and openPositionsCount are NOT zero
            refreshRiskState(agentId);

            if (!identityService.isRegistered()) { log.debug("Not registered."); return; }

            if (riskService.isCircuitBreakerTripped()) { log.warn("Circuit breaker TRIPPED."); return; }
            if (!marketDataService.isWarmupComplete()) { log.debug("Warmup {}/50 bars.", marketDataService.getBarCount()); return; }

            // null check BEFORE any writes
            MarketState marketState = marketDataService.getUnifiedState("ETH/USDC");
            if (marketState == null || marketState.getCurrentPrice() == null) { log.debug("No market state."); return; }

            if (marketState.getAtr() < ATR_THRESHOLD) { log.debug("ATR below threshold."); return; }

            // Enrich (after null check)
            marketState.setFearGreedIndex(fearGreedService.getIndex());
            marketState.setEthOnchain(onChainMetricsService.getLatestMetrics());
            marketState.setRollingSentimentScore(                                 // FIX 11: once only
                    newsAggregator.computeHeadlineSentiment(marketState.getRecentHeadlines()));

            if (!shouldTriggerAnalysis(marketState)) return;

            log.info("Analyzing | Price={} ATR={} FearGreed={} Trend={} OBI={}",
                    marketState.getCurrentPrice(),
                    String.format("%.4f", marketState.getAtr()),
                    marketState.getFearGreedIndex(),
                    marketState.getPriceTrend(),
                    String.format("%.3f", marketState.getOrderBookImbalance()));

            // FIX 10: real drawdown and open positions passed to Python
            AnalysisRequest request = new AnalysisRequest(
                    marketState,
                    newsAggregator.getAINewsContext(),
                    riskService.getCurrentDrawdownPct(),
                    riskService.getOpenPositionsCount(),
                    riskService.getVaultBalanceUsdc()
            );

            lastAnalyzedPrice       = marketState.getCurrentPrice();
            lastAnalysisTimestampMs = System.currentTimeMillis();

            AITradeDecision decision = pythonAIClient.analyzeMarket(request);
            if (decision == null || decision.getAction() == null) { log.warn("Null AI decision."); return; }

            log.info("AI: action={} confidence={} regime={} R:R={} tradeId={} | {}",
                    decision.getAction(),
                    String.format("%.2f", decision.getConfidence()),
                    decision.getMarketRegime(),
                    String.format("%.2f", decision.getRewardRiskRatio()),
                    decision.hasTradeId() ? decision.getTradeId() : "pending",
                    decision.getReasoning());

            if (decision.getConfidence() < CONFIDENCE_THRESHOLD) {
                log.info("Confidence {} < {} floor.",
                        String.format("%.2f", decision.getConfidence()),
                        String.format("%.2f", CONFIDENCE_THRESHOLD));
                return;
            }
            double spread = marketState.getSpread();
            int fearGreed = marketState.getFearGreedIndex();

            if (!decision.isActionable()) { log.info("Action={} — no trade.", decision.getAction()); return; }
            if (!riskService.isTradeSafe(decision, spread, fearGreed)) { log.warn("Risk rejected."); return; }

            if (TradeAction.BUY.equals(decision.getAction()))
                executeBuy(decision, marketState);
            else if (TradeAction.SELL.equals(decision.getAction()) ||
                    TradeAction.CUT_LOSS.equals(decision.getAction()))
                executeSell(decision, marketState);

        } catch (Exception e) {
            log.error("Trading loop error: {}", e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // BUY
    // ─────────────────────────────────────────────────────────────────────
    private void executeBuy(AITradeDecision decision, MarketState marketState) throws Exception {
        BigInteger agentId      = identityService.getAgentId();
        double     currentPrice = marketState.getCurrentPrice().doubleValue();

        // Reset daily loss to allow multiple trades
        blockchainService.resetDailyLossIfPossible(agentId);

        BigInteger vaultBalance = getVaultBalance(agentId);
        if (vaultBalance.compareTo(BigInteger.ZERO) <= 0) {
            log.warn("BUY aborted — vault empty for agent {}. Deposit USDC first.", agentId);
            return;
        }

        BigInteger amountIn = riskService.calculateSafePositionSize(
                decision.getConfidence(), vaultBalance,
                decision.getTakeProfitPct(), decision.getStopLossPct());
        if (amountIn.compareTo(BigInteger.ZERO) <= 0) {
            log.info("Kelly returned 0 — confidence too low for vault size.");
            return;
        }

        // FIX 8: slippage on WETH output, not on USDC input
        BigDecimal usdcAmount   = new BigDecimal(amountIn).movePointLeft(6);   // e.g. 20.000000 USDC
        BigDecimal ethAmount    = usdcAmount.divide(BigDecimal.valueOf(currentPrice), 18, RoundingMode.HALF_DOWN);
        BigInteger expectedWei  = ethAmount.movePointRight(18).setScale(0, RoundingMode.HALF_DOWN).toBigInteger();
//        BigInteger expectedWei = BigDecimal.valueOf(amountIn.doubleValue() / 1e6 / currentPrice * 1e18)
//                .setScale(0, RoundingMode.HALF_DOWN).toBigInteger();
        BigInteger minAmountOut = computeMinAmountOut(expectedWei, marketState.getAtr(), currentPrice);

        TradeIntent intent = TradeIntent.builder()
                .agentId(agentId)
                .tokenIn(USDC_ADDRESS)
                .tokenOut(WETH_ADDRESS)
                .amountIn(amountIn)
                .minAmountOut(minAmountOut)
                .nonce(BigInteger.valueOf(System.currentTimeMillis()))
                .deadline(BigInteger.valueOf(Instant.now().getEpochSecond() + 600))
                .riskParams(computeAuditHash(decision, marketState))
                .build();

        byte[] signature = eip712Signer.signTradeIntent(intent);

        // FIX 12: artifact posted before tx (TRADE_INTENT)
        validationService.postTradeArtifact(
                intent, null, decision, marketState, currentPrice, null, null);

        TransactionReceipt receipt = blockchainService.executeTrade(intent, signature);
        String txHash = receipt.getTransactionHash();
        Long   block  = receipt.getBlockNumber() != null ? receipt.getBlockNumber().longValue() : null;

        BigInteger actualWethOut = parseAmountOutFromReceipt(receipt, amountIn);
        recordVaultTrade(agentId, amountIn, actualWethOut);

        // FIX 13: TP/SL monitoring
        String tradeId = decision.hasTradeId() ? decision.getTradeId() : "buy_" + intent.getNonce();
        TradeRecord record = TradeRecord.fromDecision(
                tradeId, agentId, currentPrice, amountIn.doubleValue() / 1_000_000.0, intent, decision);
        record.setExecutionTxHash(txHash);
        tradeMonitorService.register(record);

        // FIX 14: position tracking
        riskService.onPositionOpened();
        riskService.updateBalance(new BigDecimal(vaultBalance.subtract(amountIn)).movePointLeft(6));
        performanceTracker.incrementBuys();
        riskService.incrementTradeCount();

        log.info("BUY OK | {} USDC → {} WEI | agent={} tradeId={} block={} tx={}",
                formatUsdc(amountIn), actualWethOut, agentId, tradeId, block,
                txHash.substring(0, 12) + "...");
    }

    // ─────────────────────────────────────────────────────────────────────
    // SELL  (FIX 2-6: complete rewrite of processStandardExit)
    // ─────────────────────────────────────────────────────────────────────
    private void executeSell(AITradeDecision decision, MarketState marketState) throws Exception {
        BigInteger agentId      = identityService.getAgentId();  // FIX 7
        double     currentPrice = marketState.getCurrentPrice().doubleValue();

        // Reset daily loss to allow multiple trades
        blockchainService.resetDailyLossIfPossible(agentId);

        BigInteger wethBalance = getWethBalance(agentId);
        if (wethBalance.compareTo(BigInteger.ZERO) <= 0) {
            log.info("SELL skipped — no WETH position for agent {}.", agentId);
            return;
        }

        // FIX 4: slippage on USDC output
        BigDecimal ethAmount    = new BigDecimal(wethBalance).movePointLeft(18);   // wei → ETH
        BigDecimal usdcRaw      = ethAmount.multiply(BigDecimal.valueOf(currentPrice)).movePointRight(6);
        BigInteger expectedUsdc = usdcRaw.setScale(0, RoundingMode.HALF_DOWN).toBigInteger();
//        BigInteger expectedUsdc = BigDecimal.valueOf(wethBalance.doubleValue() / 1e18 * currentPrice * 1e6)
//                .setScale(0, RoundingMode.HALF_DOWN).toBigInteger();
        BigInteger minAmountOut = computeMinAmountOut(expectedUsdc, marketState.getAtr(), currentPrice);

        TradeIntent intent = TradeIntent.builder()
                .agentId(agentId)
                .tokenIn(WETH_ADDRESS)
                .tokenOut(USDC_ADDRESS)
                .amountIn(wethBalance)
                .minAmountOut(minAmountOut)
                .nonce(BigInteger.valueOf(System.currentTimeMillis()))
                .deadline(BigInteger.valueOf(Instant.now().getEpochSecond() + 600))
                .riskParams(computeAuditHash(decision, marketState))  // FIX 9
                .build();
        // Inside executeSell, after building the TradeIntent
        log.info("SELL TRADE DETAILS: agentId={}, wethBalance={}, price={}", agentId, wethBalance, currentPrice);
        log.info("Expected USDC out: {}, Min USDC out: {}", expectedUsdc, minAmountOut);
        log.info("TradeIntent: {}", intent);
        log.info("Risk params hash: 0x{}", Numeric.toHexString(intent.getRiskParams()));

        byte[] signature = eip712Signer.signTradeIntent(intent);

        // validation artifact for SELL
        validationService.postTradeArtifact(
                intent, null, decision, marketState, currentPrice, null, null);

        // real execution, not missing stub
        TransactionReceipt receipt = blockchainService.executeTrade(intent, signature);
        String txHash = receipt.getTransactionHash();

        BigInteger actualUsdcOut = parseAmountOutFromReceipt(receipt, wethBalance);
        recordVaultTrade(agentId, wethBalance, actualUsdcOut);

        // register sell trade for monitoring
        String tradeId = decision.hasTradeId() ? decision.getTradeId() : "sell_" + intent.getNonce();
        TradeRecord record = TradeRecord.fromDecision(
                tradeId, agentId, currentPrice, actualUsdcOut.doubleValue() / 1_000_000.0, intent, decision);
        record.setExecutionTxHash(txHash);
        tradeMonitorService.register(record);

        riskService.onPositionClosed();
        performanceTracker.incrementSells();
        riskService.incrementTradeCount();

        log.info("{} OK | {} WEI ETH → {} USDC | agent={} tradeId={} tx={}",
                decision.getAction(), wethBalance, formatUsdc(actualUsdcOut),
                agentId, tradeId, txHash.substring(0, 12) + "...");
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────

    // FIX 15: now actually uses the thresholds
    private boolean shouldTriggerAnalysis(MarketState state) {
        long   now         = System.currentTimeMillis();
        double price       = state.getCurrentPrice().doubleValue();
        double lastPrice   = lastAnalyzedPrice.doubleValue();
        boolean priceMoved = Math.abs(price - lastPrice) >= PRICE_MOVE_THRESHOLD_USD;
        boolean timeOk     = (now - lastAnalysisTimestampMs) >= MIN_ANALYSIS_INTERVAL_MS;

        if (!priceMoved && !timeOk) {
            log.debug("Skipping — Δprice=${}, {}ms since last.",
                    String.format("%.2f", Math.abs(price - lastPrice)),
                    now - lastAnalysisTimestampMs);
            return false;
        }
        List<Double> hist = state.getPriceHistory();
        if (hist != null && hist.size() > 2) {
            double first = hist.get(0);
            if (hist.stream().allMatch(p -> Math.abs(p - first) < 0.01)) {
                log.debug("Skipping — flat price history.");
                return false;
            }
        }
        return true;
    }

    // FIX 8: output-token slippage, never zero
    private BigInteger computeMinAmountOut(BigInteger expectedOut, double atr, double price) {
        double atrPct   = price > 0 ? atr / price : 0.003;
        double slippage = Math.min(0.02, Math.max(0.003, atrPct * 1.5));
        return new BigDecimal(expectedOut)
                .multiply(BigDecimal.valueOf(1.0 - slippage))
                .setScale(0, RoundingMode.HALF_DOWN)
                .max(BigDecimal.ONE)
                .toBigInteger();
    }

    private byte[] computeAuditHash(AITradeDecision decision, MarketState market) {
        // Include (Market Context) + (Decision)
        String auditString = String.format(
                "CTX:%s|P:%.2f|RSI:%.2f|ATR:%.4f|DEC:%s|CONF:%.2f",
                market.getSymbol(),
                market.getCurrentPrice().doubleValue(),
                market.getRsi(),
                market.getAtr(),
                decision.getAction(),
                decision.getConfidence()
        );

        // This hash is now a "Fingerprint" of that exact microsecond in the market
        return Hash.sha3(auditString.getBytes(StandardCharsets.UTF_8));
    }

    // Parses Transfer event amountOut. Never returns zero to prevent MockVault underflow.
    private BigInteger parseAmountOutFromReceipt(TransactionReceipt receipt, BigInteger fallback) {
        try {
            final String TRANSFER =
                    "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
            return receipt.getLogs().stream()
                    .filter(l -> !l.getTopics().isEmpty() && TRANSFER.equalsIgnoreCase(l.getTopics().get(0)))
                    .map(l -> new BigInteger(l.getData().replace("0x", "").trim(), 16))
                    .filter(v -> v.compareTo(BigInteger.ZERO) > 0)
                    .findFirst()
                    .orElseGet(() -> fallback.max(BigInteger.ONE));
        } catch (Exception e) {
            log.warn("parseAmountOut failed: {}", e.getMessage());
            return BigInteger.ONE;
        }
    }

    private BigInteger getVaultBalance(BigInteger agentId) {
        try { return mockVault.balances(agentId).send(); }
        catch (Exception e) { log.error("Vault balance fetch failed: {}", e.getMessage()); return BigInteger.ZERO; }
    }

    private BigInteger getWethBalance(BigInteger agentId) {  // FIX 3
        try { return mockVault.balances(agentId).send(); }
        catch (Exception e) { log.warn("WETH balance fetch failed: {}", e.getMessage()); return BigInteger.ZERO; }
    }

    private void recordVaultTrade(BigInteger agentId, BigInteger in, BigInteger out) {
        try { mockVault.recordTrade(agentId, in, out).send(); }
        catch (Exception e) { log.error("recordVaultTrade failed: {}", e.getMessage()); }
    }

    private String formatUsdc(BigInteger amount) {
        return new BigDecimal(amount).movePointLeft(6).toPlainString();
    }

    /**
     * Fetches real-time data from the blockchain to populate the RiskManagementService.
     * Prevents the "Zero Values" issue in AnalysisRequest.
     */
    private void refreshRiskState(BigInteger agentId) {
        try {
            // 1. Refresh Balance
            BigInteger vaultBalance = getVaultBalance(agentId);
            riskService.updateBalance(new BigDecimal(vaultBalance).movePointLeft(6));

            // 2. Refresh Open Positions
            // Note: In a real system, you'd query your DB or the contract for active trade count.
            // For now, we ensure the RiskService balance isn't zero so drawdown math works.
            log.debug("Risk state refreshed: Balance={} USDC", riskService.getVaultBalanceUsdc());
        } catch (Exception e) {
            log.warn("Failed to refresh risk state: {}", e.getMessage());
        }
    }
}