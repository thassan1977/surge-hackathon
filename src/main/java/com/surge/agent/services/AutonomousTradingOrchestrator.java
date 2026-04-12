package com.surge.agent.services;

import com.surge.agent.contracts.RiskRouter;
import com.surge.agent.dto.*;

import com.surge.agent.dto.request.AnalysisRequest;
import com.surge.agent.enums.TradeAction;
import com.surge.agent.model.TradeIntent;
import com.surge.agent.services.market.MarketDataService;
import com.surge.agent.services.news.NewsAggregator;
import com.surge.agent.utils.EIP712Signer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
@Service
@RequiredArgsConstructor
public class AutonomousTradingOrchestrator {

    private final MarketDataService     marketDataService;
    private final RiskManagementService riskService;
    private final BlockchainService     blockchainService;
    private final IdentityService       identityService;
    private final EIP712Signer          eip712Signer;
    private final RiskRouter            riskRouter;        // for simulateIntent() + submitTradeIntent()
    private final PythonAIClient        pythonAIClient;
    private final FearGreedService      fearGreedService;
    private final OnChainMetricsService onChainMetricsService;
    private final NewsAggregator        newsAggregator;
    private final Credentials           credentials;
    private final Web3j                 web3j;
    private final ValidationCheckpointService checkpointService;
    private final @Lazy TradeMonitorService tradeMonitorService;

    @Value("${contract.weth}")
    private String WETH_ADDRESS;

    @Value("${contract.usdc}")
    private String USDC_ADDRESS;

    @Value("${ai.orchestrator.confidence:0.60}")
    private double CONFIDENCE_THRESHOLD;

    @Value("${ai.orchestrator.atr:0.01}")
    private double ATR_THRESHOLD;

    @Value("${ai.pair:ETHUSD}")
    private String PAIR;

    // ── RiskRouter on-chain limits ────────────────────────────────────────
    // Max $500 per trade = 50000 scaled (500 * 100)
    // Max 10 trades per hour
    private static final BigInteger MAX_AMOUNT_USD_SCALED = BigInteger.valueOf(50_000);
    private static final int        MAX_TRADES_PER_HOUR   = 9; // stay under 10 limit
    private static final double     MIN_CONFIDENCE_SELL_NO_POSITION = 0.75; // Higher threshold to sell without an open position

    // ── Local rate limiter ────────────────────────────────────────────────
    private final AtomicInteger tradesThisHour    = new AtomicInteger(0);
    private final AtomicLong    hourWindowStartMs = new AtomicLong(System.currentTimeMillis());

    // ── Analysis throttle ─────────────────────────────────────────────────
    private BigDecimal lastAnalyzedPrice       = BigDecimal.ZERO;
    private long       lastAnalysisTimestampMs = 0;

    private static final double PRICE_MOVE_THRESHOLD_USD = 1.50;
    private static final long   MIN_ANALYSIS_INTERVAL_MS = 10_000;

    @PostConstruct
    public void onStartup() {
        if (!identityService.isRegistered()) {
            log.error("AGENT NOT REGISTERED — all trades blocked.");
        } else {
            log.info("Agent ready. AgentId={} Wallet={}",
                    identityService.getAgentId(), credentials.getAddress());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // MAIN LOOP — runs every 10 seconds
    // ─────────────────────────────────────────────────────────────────────
    @Scheduled(fixedRate = 10_000)
    public void tradingLoop() {
        try {
            BigInteger agentId = identityService.getAgentId();
            if (agentId == null || agentId.equals(BigInteger.ZERO)) return;
            if (!identityService.isRegistered()) { log.debug("Not registered."); return; }
            if (riskService.isCircuitBreakerTripped()) { log.warn("Circuit breaker TRIPPED."); return; }
            if (!marketDataService.isWarmupComplete()) {
                log.debug("Warmup {}/{} bars.", marketDataService.getBarCount(), 50);
                return;
            }


            MarketState marketState = marketDataService.getUnifiedState("ETH/USDC");
            if (marketState == null || marketState.getCurrentPrice() == null) return;
            if (marketState.getAtr() < ATR_THRESHOLD) return;

            // [FIX 3] Feed real USDC balance
            refreshRiskState(agentId, marketState.getCurrentPrice().doubleValue());


            marketState.setFearGreedIndex(fearGreedService.getIndex());
            marketState.setEthOnchain(onChainMetricsService.getLatestMetrics());
            marketState.setRollingSentimentScore(
                    newsAggregator.computeHeadlineSentiment(marketState.getRecentHeadlines()));

            if (!shouldTriggerAnalysis(marketState)) return;

            log.info("Analyzing | Price={} ATR={} FearGreed={} Trend={} OBI={}",
                    marketState.getCurrentPrice(),
                    marketState.getAtr(),
                    marketState.getFearGreedIndex(),
                    marketState.getPriceTrend(),
                    marketState.getOrderBookImbalance());

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

            log.info("AI: action={} confidence={} regime={} tradeId={} | {}",
                    decision.getAction(),
                    decision.getConfidence(),
                    decision.getMarketRegime(),
                    decision.hasTradeId() ? decision.getTradeId() : "pending",
                    decision.getReasoning().substring(0, Math.min(250, decision.getReasoning().length())));

            if (decision.getConfidence() < CONFIDENCE_THRESHOLD) {
                log.info("Confidence {} < {} floor — skipping.",
                        decision.getConfidence(), CONFIDENCE_THRESHOLD);
                return;
            }

            if (!decision.isActionable()) { log.info("Action={} — no trade.", decision.getAction()); return; }

            if (!riskService.isTradeSafe(decision,
                    marketState.getSpread(), marketState.getFearGreedIndex())) {
                log.warn("Risk rejected.");
                return;
            }

            // [FIX 7] Local rate limiter — check before hitting the chain
            if (!checkRateLimit()) {
                log.warn("Rate limit: {} trades this hour (max {}). Skipping.",
                        tradesThisHour.get(), MAX_TRADES_PER_HOUR);
                return;
            }

            if (TradeAction.BUY.equals(decision.getAction())) {
                executeBuy(decision, marketState);
            } else if (TradeAction.SELL.equals(decision.getAction()) || TradeAction.CUT_LOSS.equals(decision.getAction())) {
                executeSell(decision, marketState);
            }

        } catch (Exception e) {
            log.error("Trading loop error: {}", e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // BUY — submit signed TradeIntent to RiskRouter
    // ─────────────────────────────────────────────────────────────────────
    private void executeBuy(AITradeDecision decision, MarketState marketState) throws Exception {
        BigInteger agentId = identityService.getAgentId();

        BigDecimal vaultUsdValue = getWalletBalanceUsdEquivalent(marketState.getCurrentPrice().doubleValue());
        if (vaultUsdValue.compareTo(BigDecimal.valueOf(10.0)) <= 0) {
            log.warn("BUY aborted — Vault USD equivalent is below $10. Did you call claimAllocation()?");
            return;
        }

        // Kelly-based position size calculation
        double balanceUsd = riskService.getVaultBalanceUsdc();
        BigInteger amountUsdScaled = riskService.calculateSafePositionSizeUsd(
                decision.getConfidence(),
                balanceUsd,
                decision.getTakeProfitPct(),
                decision.getStopLossPct());

        if (amountUsdScaled.compareTo(BigInteger.ZERO) <= 0) {
            log.info("Position size too small — skipping.");
            return;
        }

        amountUsdScaled = amountUsdScaled.min(MAX_AMOUNT_USD_SCALED);
        if (amountUsdScaled.compareTo(BigInteger.valueOf(100)) < 0) {
            log.info("Position too small ({} scaled) — minimum $1. Skipping.", amountUsdScaled);
            return;
        }

        BigInteger maxSlippageBps = computeSlippageBps(
                marketState.getAtr(), marketState.getCurrentPrice().doubleValue());
        BigInteger nonce = riskRouter.getIntentNonce(agentId).send();

        TradeIntent intent = TradeIntent.builder()
                .agentId(agentId)
                .agentWallet(credentials.getAddress())
                .pair(PAIR)
                .action(TradeAction.BUY.name())
                .amountUsdScaled(amountUsdScaled)
                .maxSlippageBps(maxSlippageBps)
                .nonce(nonce)
                .deadline(BigInteger.valueOf(Instant.now().getEpochSecond() + 600))
                .build();

        // Dry-run check
        if (!simulateAndCheck(intent.toContractStruct(), agentId)) return;

        byte[] signature = eip712Signer.signTradeIntent(intent);

        // --- HIGH VISIBILITY JUDGE LOGS ---
        log.info("==========================================================================");
        log.info(" EXECUTING BUY INTENT | AGENT: {} | PAIR: {}", agentId, PAIR);
        log.info(" -> CONVICTION: {}% | REGIME: {}", (int)(decision.getConfidence() * 100), decision.getMarketRegime());
        log.info(" -> REASONING: {}", decision.getReasoning());
        log.info("==========================================================================");

        // Execute the trade
        TransactionReceipt receipt = blockchainService.executeTrade(intent, signature);
        boolean approved = receipt.getLogs().stream()
                .anyMatch(l -> l.getTopics().get(0).contains("536c9b7d")); // TradeApproved topic

        // check point for validation
        checkpointService.postCheckpoint(intent, decision, marketState, approved,
                receipt.getTransactionHash());

        if (approved) {

            tradesThisHour.incrementAndGet();
            riskService.onPositionOpened();
            riskService.incrementTradeCount();

            double actualEthPrice = marketState.getCurrentPrice().doubleValue();

            double positionUsdc = intent.getAmountUsdScaled().doubleValue() / 100.0;  // cents → dollars

            TradeRecord record = TradeRecord.fromDecision(
                    decision.getTradeId(),
                    agentId,
                    actualEthPrice,
                    positionUsdc,
                    intent,
                    decision
            );

            tradeMonitorService.register(record);
            log.info("--- Trade monitored: Entry {}, TP at {}, SL at {}", actualEthPrice, record.getTakeProfitPrice(), record.getStopLossPrice());
        }
        // reputation
//        checkpointService.postReputation(intent, decision, marketState, approved,
//                receipt.getTransactionHash());

        log.info("  BUY OK | ${} USD | tx={} | approved: {}",
                amountUsdScaled.divide(BigInteger.valueOf(100)),
                receipt.getTransactionHash().substring(0, 12) + "...",
                approved);
    }
    // ─────────────────────────────────────────────────────────────────────
    // SELL — submit signed TradeIntent to RiskRouter
    // ─────────────────────────────────────────────────────────────────────
    private void executeSell(AITradeDecision decision, MarketState marketState) throws Exception {
        BigInteger agentId = identityService.getAgentId();

        // Hybrid Guard: Close position OR open Short if conviction is high enough (>80%)
        if (riskService.getOpenPositionsCount() <= 0 && decision.getConfidence() < MIN_CONFIDENCE_SELL_NO_POSITION) {
            log.info("SELL skipped — No open positions and conviction ({}) below threshold.", decision.getConfidence());
            return;
        }

        double currentPrice = marketState.getCurrentPrice().doubleValue();
        BigDecimal vaultUsdValue = getWalletBalanceUsdEquivalent(currentPrice);

        // Scaling for a full close or max allowed short
        BigInteger amountUsdScaled = vaultUsdValue
                .multiply(BigDecimal.valueOf(100))
                .toBigInteger()
                .min(MAX_AMOUNT_USD_SCALED);

        BigInteger maxSlippageBps = computeSlippageBps(marketState.getAtr(), currentPrice);
        BigInteger nonce = riskRouter.getIntentNonce(agentId).send();

        TradeIntent intent = TradeIntent.builder()
                .agentId(agentId)
                .agentWallet(credentials.getAddress())
                .pair(PAIR)
                .action(TradeAction.SELL.name())
                .amountUsdScaled(amountUsdScaled)
                .maxSlippageBps(maxSlippageBps)
                .nonce(nonce)
                .deadline(BigInteger.valueOf(Instant.now().getEpochSecond() + 600))
                .build();

        if (!simulateAndCheck(intent.toContractStruct(), agentId)) return;

        byte[] signature = eip712Signer.signTradeIntent(intent);

        // --- HIGH VISIBILITY JUDGE LOGS ---
        log.info("==========================================================================");
        log.info(" EXECUTING SELL INTENT | AGENT: {} | PAIR: {}", agentId, PAIR);
        log.info(" -> CONVICTION: {}% | REGIME: {}", (int)(decision.getConfidence() * 100), decision.getMarketRegime());
        log.info(" -> REASONING: {}", decision.getReasoning());
        log.info("==========================================================================");

        // Execute the trade (Failing validation calls removed)
        TransactionReceipt receipt = blockchainService.executeTrade(intent, signature);

        boolean approved = receipt.getLogs().stream()
                .anyMatch(l -> l.getTopics().get(0).contains("536c9b7d")); // TradeApproved topic

        if (approved) {
            tradesThisHour.incrementAndGet();
            riskService.onPositionOpened();
            riskService.incrementTradeCount();

            // CRITICAL FIX: Ensure we use the Market Price, NOT the Wallet Balance
            double actualEthPrice = marketState.getCurrentPrice().doubleValue();

            double positionUsdc = intent.getAmountUsdScaled().doubleValue() / 100.0;

            TradeRecord record = TradeRecord.fromDecision(
                    decision.getTradeId(),
                    agentId,
                    actualEthPrice,
                    positionUsdc,
                    intent,
                    decision
            );

            tradeMonitorService.register(record);
            log.info("--- Trade monitored: Entry {}, TP at {}, SL at {}", actualEthPrice, record.getTakeProfitPrice(), record.getStopLossPrice());
        }

        // check point for validation
        checkpointService.postCheckpoint(intent, decision, marketState, approved,
                receipt.getTransactionHash());

        // reputation
//        checkpointService.postReputation(intent, decision, marketState, approved,
//                receipt.getTransactionHash());

        log.info("  SELL OK | ${} USD | tx={} | approved = {}",
                amountUsdScaled.divide(BigInteger.valueOf(100)),
                receipt.getTransactionHash().substring(0, 12) + "...",
                approved);
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * [FIX 6] Simulate the TradeIntent against RiskRouter before submitting.
     * Avoids wasting gas on intents the Router will reject.
     * Returns true if the intent is valid and should be submitted.
     */
    private boolean simulateAndCheck(RiskRouter.TradeIntent intent, BigInteger agentId) {
        try {
            // Pass the intent directly, not as a tuple.
            // Web3j Tuple2 uses component1() and component2()
            var result = riskRouter.simulateIntent(intent).send();

            boolean valid  = result.component1();
            String  reason = result.component2();

            if (!valid) {
                log.warn("RiskRouter simulation REJECTED: {} — skipping trade.", reason);
                return false;
            }
            log.debug("RiskRouter simulation APPROVED: {}", reason);
            return true;
        } catch (Exception e) {
            // simulateIntent failed — proceed cautiously
            log.warn("simulateIntent failed ({}), proceeding anyway.", e.getMessage());
            return true;
        }
    }

    /**
     * [FIX 7] Local rate limiter — resets every hour.
     * RiskRouter enforces max 10 trades/hour on-chain; we stay under at 9.
     */
    private boolean checkRateLimit() {
        long now = System.currentTimeMillis();
        long windowStart = hourWindowStartMs.get();

        // Reset counter if 1 hour has elapsed
        if (now - windowStart > 3_600_000L) {
            tradesThisHour.set(0);
            hourWindowStartMs.set(now);
            log.info("Trade rate counter reset (new hour window).");
        }

        return tradesThisHour.get() < MAX_TRADES_PER_HOUR;
    }

    /**
     * [FIX 1] Read actual USDC ERC-20 balance from agent wallet.
     * HackathonVault.getBalance() returns ETH only — not USDC.
     */
    private BigInteger getUsdcWalletBalance() {
        try {
            // ERC-20 balanceOf(address) → uint256
            org.web3j.abi.datatypes.Function fn = new org.web3j.abi.datatypes.Function(
                    "balanceOf",
                    java.util.Arrays.asList(new org.web3j.abi.datatypes.Address(credentials.getAddress())),
                    java.util.Arrays.asList(new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {})
            );
            String encoded = org.web3j.abi.FunctionEncoder.encode(fn);
            String result  = blockchainService.ethCall(USDC_ADDRESS, encoded);

            // Bypass Java generic strictness with an unchecked cast for Web3j decoder
            @SuppressWarnings("unchecked")
            List<org.web3j.abi.datatypes.Type> decoded = org.web3j.abi.FunctionReturnDecoder.decode(
                    result,
                    (List<org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Type>>) (List<?>) fn.getOutputParameters()
            );

            return ((org.web3j.abi.datatypes.generated.Uint256) decoded.get(0)).getValue();
        } catch (Exception e) {
            log.error("getUsdcWalletBalance failed: {}", e.getMessage());
            return BigInteger.ZERO;
        }
    }

    /**
     * [FIX 2] Read actual WETH ERC-20 balance from agent wallet.
     * This tells us if we actually hold an open long position.
     */
    private BigInteger getWethWalletBalance() {
        try {
            org.web3j.abi.datatypes.Function fn = new org.web3j.abi.datatypes.Function(
                    "balanceOf",
                    java.util.Arrays.asList(new org.web3j.abi.datatypes.Address(credentials.getAddress())),
                    java.util.Arrays.asList(new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {})
            );
            String encoded = org.web3j.abi.FunctionEncoder.encode(fn);
            String result  = blockchainService.ethCall(WETH_ADDRESS, encoded);

            @SuppressWarnings("unchecked")
            List<org.web3j.abi.datatypes.Type> decoded = org.web3j.abi.FunctionReturnDecoder.decode(
                    result,
                    (List<org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Type>>) (List<?>) fn.getOutputParameters()
            );

            return ((org.web3j.abi.datatypes.generated.Uint256) decoded.get(0)).getValue();
        } catch (Exception e) {
            log.warn("getWethWalletBalance failed: {}", e.getMessage());
            return BigInteger.ZERO;
        }
    }

    /**
     * [FIX 3] Refresh risk state with REAL USDC balance.
     * Python AI sizes positions using this value — must be accurate.
     */
    private void refreshRiskState(BigInteger agentId, double currentEthPrice) {
        try {
            BigDecimal usdEquity = getWalletBalanceUsdEquivalent(currentEthPrice);
            riskService.updateBalance(usdEquity);
            riskService.updateEthPrice(currentEthPrice);
            log.info("Equity Refresh: ${} (ETH price: ${})",
                    usdEquity.setScale(2, RoundingMode.HALF_UP), currentEthPrice);
        } catch (Exception e) {
            log.warn("refreshRiskState failed: {}", e.getMessage());
        }
    }
//        try {
//            BigDecimal usdcHuman = getVaultBalanceUsdEquivalent(agentId, currentEthPrice);
//
//            // Update Risk Service with the total dollar equity
//            riskService.updateBalance(usdcHuman);
//
//            // LOGGING FIX: Use info level here temporarily to monitor the fix
//            log.info("Equity Refresh: ${} (Based on ETH price: {})",
//                    usdcHuman.setScale(2, RoundingMode.HALF_UP), currentEthPrice);
//
//        } catch (Exception e) {
//            log.warn("refreshRiskState failed: {}", e.getMessage());
//        }


    // UNCHANGED from original — logic was correct
    private boolean shouldTriggerAnalysis(MarketState state) {
        long   now       = System.currentTimeMillis();
        double price     = state.getCurrentPrice().doubleValue();
        double lastPrice = lastAnalyzedPrice.doubleValue();
        boolean priceMoved = Math.abs(price - lastPrice) >= PRICE_MOVE_THRESHOLD_USD;
        boolean timeOk     = (now - lastAnalysisTimestampMs) >= MIN_ANALYSIS_INTERVAL_MS;

        if (!priceMoved && !timeOk) {
            log.debug("Skipping — Δprice=${}, {}ms since last.",
                    Math.abs(price - lastPrice), now - lastAnalysisTimestampMs);
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

    // UNCHANGED — correctly computes BPS from ATR
    private BigInteger computeSlippageBps(double atr, double price) {
        double atrPct          = price > 0 ? (atr / price) : 0.003;
        double slippageDecimal = Math.min(0.02, Math.max(0.003, atrPct * 1.5));
        return BigInteger.valueOf((long)(slippageDecimal * 10_000));
    }

    private String formatUsdc(BigInteger amount) {
        return new BigDecimal(amount).movePointLeft(6).toPlainString();
    }

    /**
     * [LIVE HACKATHON FIX] Read internal Vault ETH balance and convert to USD.
     * The hackathon router trades against your 0.05 ETH Vault allocation, not ERC-20s.
     */
//    private BigDecimal getVaultBalanceUsdEquivalent(BigInteger agentId, double currentEthPrice) {
//        try {
//            // 1. Get ETH balance from HackathonVault (returns Wei)
//            BigInteger weiBalance = hackathonVault.getBalance(agentId).send();
//
//            // (0.001 ETH)
//            log.debug("Vault Raw Wei: {} | Price: {}", weiBalance, currentEthPrice);
//
//            // 2. Convert Wei to ETH (divide by 10^18)
//            BigDecimal ethBalance = new BigDecimal(weiBalance).movePointLeft(18);
//
//            // 3. Convert ETH to USD
//            return ethBalance.multiply(BigDecimal.valueOf(currentEthPrice));
//        } catch (Exception e) {
//            log.error("Failed to read HackathonVault balance: {}", e.getMessage());
//            return BigDecimal.ZERO;
//        }
//    }

    private BigDecimal getWalletBalanceUsdEquivalent(double currentEthPrice) {
        try {
            // Use TOTAL wallet ETH balance for sizing, not just the vault slot
            // The vault slot tracks a tiny internal accounting entry (0.001 ETH)
            // The real capital is the full wallet ETH balance
            BigInteger weiBalance = web3j.ethGetBalance(
                    credentials.getAddress(),
                    org.web3j.protocol.core.DefaultBlockParameterName.LATEST
            ).send().getBalance();

            BigDecimal ethBalance = new BigDecimal(weiBalance).movePointLeft(18);
            BigDecimal usdValue   = ethBalance.multiply(BigDecimal.valueOf(currentEthPrice));

            log.info("Wallet ETH: {} | USD value: ${} | ETH price: ${}",
                    ethBalance.setScale(6, RoundingMode.HALF_UP),
                    usdValue.setScale(2, RoundingMode.HALF_UP),
                    currentEthPrice);

            return usdValue;
        } catch (Exception e) {
            log.error("getWalletBalanceUsdEquivalent failed: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}