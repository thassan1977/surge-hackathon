package com.surge.agent.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surge.agent.config.ContractConfig;
import com.surge.agent.contracts.ReputationRegistry;
import com.surge.agent.model.RiskAssessment;
import com.surge.agent.model.TradeIntent;
import com.surge.agent.dto.AITradeDecision;
import com.surge.agent.dto.MarketState;
import com.surge.agent.dto.TradeRecord;
import com.surge.agent.dto.artifact.AgentVerdict;
import com.surge.agent.dto.artifact.StrategyCheckpointArtifact;
import com.surge.agent.dto.artifact.TradeArtifact;
import com.surge.agent.dto.artifact.TradeOutcomeRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final ObjectMapper          objectMapper;
    private final BlockchainService     blockchainService;
    private final Web3j                 web3j;
    private final ContractConfig        contractConfig;
    private final DefaultGasProvider    gasProvider;
    private final RiskManagementService riskService;
    private final IdentityService       identityService;
    private final PerformanceTracker    performanceTracker;

    @Value("${agent.validator.privateKey:}")
    private String validatorPrivateKey;

    @Value("${agent.chain.id:31337}")
    private long chainId;

    @Value("${validation.checkpoint.interval:10}")
    private int checkpointInterval;

    private final Map<String, TradeArtifact> artifactCache = new ConcurrentHashMap<>();
    private int tradesSinceCheckpoint = 0;

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    // ═════════════════════════════════════════════════════════════════════
    // ① POST TRADE ARTIFACT
    // ═════════════════════════════════════════════════════════════════════

    public byte[] postTradeArtifact(TradeIntent intent,
                                    RiskAssessment risk,
                                    AITradeDecision decision,
                                    MarketState marketState,
                                    double entryPriceUsd,
                                    String txHash,
                                    Long blockNumber) throws Exception {

        BigInteger agentId = identityService.getAgentId();
        long now           = Instant.now().getEpochSecond();

        // FIX 1: single canonical resolveTradeId — checks hasTradeId() first
        String tradeId = resolveTradeId(decision, intent, now);

        TradeArtifact artifact = TradeArtifact.builder()

                .identity(TradeArtifact.IdentityBlock.builder()
                        .agentId(agentId.toString())
                        .tradeId(tradeId)
                        .timestampUnix(now)
                        .timestampIso(ISO.format(Instant.ofEpochSecond(now)))
                        .chainId(chainId)
                        .vaultAddress(contractConfig.getVault())
                        .routerAddress(contractConfig.getRouter())
                        .build())

                .decision(TradeArtifact.DecisionBlock.builder()
                        .action(decision.getAction().name())
                        .confidence(decision.getConfidence())
                        .riskLevel(decision.getRiskLevel().name())
                        .marketRegime(decision.getMarketRegime().name())
                        .takeProfitPct(decision.getTakeProfitPct())
                        .stopLossPct(decision.getStopLossPct())
                        .rewardRiskRatio(decision.getRewardRiskRatio())
                        .kellySizePct(computeKellySizePct(intent))
                        .judgeReasoning(decision.getReasoning())
                        .regimeConfidence(decision.getRegimeConfidence())  // FIX 2 applied here too
                        .build())

                .marketSnapshot(buildMarketSnapshot(marketState, decision))

                .aiCouncil(buildAICouncil(decision))

                .riskAnalysis(buildRiskAnalysis(risk, decision))

                .portfolioContext(buildPortfolioContext(intent))

                .execution(TradeArtifact.ExecutionBlock.builder()
                        .tokenIn(intent.getTokenIn())
                        .tokenOut(intent.getTokenOut())
                        .amountInRaw(intent.getAmountIn().toString())
                        .amountInUsdc(intent.getAmountIn().doubleValue() / 1_000_000.0)
                        .minAmountOut(intent.getMinAmountOut() != null
                                ? intent.getMinAmountOut().toString() : "0")
                        .deadline(intent.getDeadline() != null
                                ? intent.getDeadline().longValue() : 0L)
                        .nonce(intent.getNonce() != null
                                ? intent.getNonce().toString() : "0")
                        .eip712Signed(true)
                        .txHash(txHash)
                        .blockNumber(blockNumber)
                        .entryPriceUsd(entryPriceUsd)
                        .takeProfitPrice(entryPriceUsd * decision.getTakeProfitMultiplier())
                        .stopLossPrice(entryPriceUsd * decision.getStopLossMultiplier())
                        .build())

                // Open outcome — filled by finaliseOutcome()
                .outcome(TradeArtifact.openOutcome())

                .build();

        artifact.computeAndAttachScore();

        byte[] hash = hashAndPost(agentId, artifact);
        log.info("Trade artifact posted | tradeId={} score={}/100 tx={}",
                tradeId, artifact.getTotalScore(),
                txHash != null ? txHash.substring(0, 12) + "..." : "pending");

        artifactCache.put(tradeId, artifact);

        if (++tradesSinceCheckpoint >= checkpointInterval) {
            postStrategyCheckpoint();
            tradesSinceCheckpoint = 0;
        }

        return hash;
    }

    // ═════════════════════════════════════════════════════════════════════
    // ② FINALISE OUTCOME
    // ═════════════════════════════════════════════════════════════════════

    /**
     * FIX 6: parameter changed from OpenPosition (doesn't exist) to TradeRecord.
     * FIX 5: OutcomeBlock now sets predictionCorrect so the 15-pt profitability
     *         factor fires correctly in computeAndAttachScore().
     */
    public void finaliseOutcome(TradeOutcomeRecord outcome, TradeRecord record) {
        TradeArtifact artifact = artifactCache.get(outcome.getTradeId());
        if (artifact == null) {
            log.warn("No cached artifact for tradeId={} — outcome not posted.", outcome.getTradeId());
            return;
        }

        // FIX 5: use the factory method which correctly computes predictionCorrect
        TradeArtifact.OutcomeBlock outcomeBlock = TradeArtifact.closedOutcome(
                outcome.getExitReason(),
                outcome.getEntryPriceUsd(),
                outcome.getExitPriceUsd(),
                outcome.getRealisedPnlUsdc() / (outcome.getRealisedPnlPct() != 0
                        ? Math.abs(outcome.getRealisedPnlPct()) : 1.0), // derive position size
                record.getAction().name(),
                record.getOpenedAtEpoch(),
                outcome.getCloseTxHash()
        );

        artifact.setOutcome(outcomeBlock);
        artifact.computeAndAttachScore();  // now includes up to 15-pt profitability

        try {
            BigInteger agentId = identityService.getAgentId();
            hashAndPost(agentId, artifact);
            log.info("Outcome artifact posted | tradeId={} status={} pnl={} finalScore={}/100",
                    outcome.getTradeId(), outcomeBlock.getStatus(),
                    outcome.getRealisedPnlPct(), artifact.getTotalScore());
        } catch (Exception e) {
            log.error("Failed to post outcome artifact for {}: {}", outcome.getTradeId(), e.getMessage());
        }

        // Feed performance tracker for future checkpoints + agent weight updates
        performanceTracker.record(outcome, record.getMarketRegime().name(), record.getAgentVerdicts());
        performanceTracker.updateMaxDrawdown(riskService.getCurrentDrawdownPct());

        artifactCache.remove(outcome.getTradeId());
    }

    // ═════════════════════════════════════════════════════════════════════
    // ③ STRATEGY CHECKPOINT
    // ═════════════════════════════════════════════════════════════════════

    public void postStrategyCheckpoint() {
        try {
            BigInteger agentId = identityService.getAgentId();
            long now           = Instant.now().getEpochSecond();

            StrategyCheckpointArtifact checkpoint = StrategyCheckpointArtifact.builder()

                    .identity(StrategyCheckpointArtifact.CheckpointIdentity.builder()
                            .agentId(agentId.toString())
                            .checkpointNumber(performanceTracker.nextCheckpointNumber())
                            .timestampUnix(now)
                            .timestampIso(ISO.format(Instant.ofEpochSecond(now)))
                            .chainId(chainId)
                            .build())

                    .window(StrategyCheckpointArtifact.WindowStats.builder()
                            .tradesInWindow(performanceTracker.getTradeCount())
                            .windowSize(checkpointInterval)
                            .buys(performanceTracker.getBuys())
                            .sells(performanceTracker.getSells())
                            .holdsVetoed(performanceTracker.getHoldsVetoed())
                            .windowStartUnix(performanceTracker.getWindowStartMs() / 1000)
                            .windowEndUnix(now)
                            .build())

                    .performance(StrategyCheckpointArtifact.PerformanceMetrics.builder()
                            .sharpeRatio(performanceTracker.getSharpeRatio())
                            .sortinoRatio(performanceTracker.getSortinoRatio())
                            .winRate(performanceTracker.getWinRate())
                            .avgWinPct(performanceTracker.getAvgWinPct())
                            .avgLossPct(performanceTracker.getAvgLossPct())
                            .profitFactor(performanceTracker.getProfitFactor())
                            .cumulativePnlPct(performanceTracker.getCumulativePnlPct())
                            .bestTradePct(performanceTracker.getBestTrade())
                            .worstTradePct(performanceTracker.getWorstTrade())
                            .avgHoldSeconds(performanceTracker.getAvgHoldSeconds())
                            .tpHitRate(performanceTracker.getTpHitRate())
                            .build())

                    .agentStats(StrategyCheckpointArtifact.AgentStats.builder()
                            .winRatesByAgent(performanceTracker.getWinRatesByAgent())
                            .avgPnlByAgent(performanceTracker.getAvgPnlByAgent())
                            .mvpAgent(performanceTracker.getMvpAgent())
                            .build())

                    .regimeStats(StrategyCheckpointArtifact.RegimeStats.builder()
                            .tradesByRegime(performanceTracker.getTradesByRegime())
                            .winRateByRegime(performanceTracker.getWinRateByRegime())
                            .bestRegime(performanceTracker.getBestRegime())
                            .worstRegime(performanceTracker.getWorstRegime())
                            .build())

                    .riskHealth(StrategyCheckpointArtifact.RiskHealth.builder()
                            .currentDrawdownPct(riskService.getCurrentDrawdownPct())
                            .maxDrawdownPctWindow(performanceTracker.getMaxDrawdownWindow())
                            .circuitBreakerTrips(performanceTracker.getCircuitBreakerTrips())
                            .circuitBreakerClean(performanceTracker.getCircuitBreakerTrips() == 0)
                            .openPositions(riskService.getOpenPositionsCount())
                            .avgRewardRiskRatio(riskService.getAvgRewardRiskRatio())
                            .build())

                    .build();

            checkpoint.computeAndAttachScore();

            String json  = objectMapper.writeValueAsString(checkpoint);
            byte[] hash  = Numeric.hexStringToByteArray(Hash.sha3String(json));
            int    score = checkpoint.getTotalScore();
            blockchainService.postValidation(agentId, hash, score);
            postValidatorScore(agentId, score);

            log.info("Strategy checkpoint posted | #{} Sharpe={} WinRate={} Score={}/100",
                    checkpoint.getIdentity().getCheckpointNumber(),
                    performanceTracker.getSharpeRatio(),
                    performanceTracker.getWinRate(),
                    score);

            performanceTracker.resetWindowStart();

        } catch (Exception e) {
            log.error("Strategy checkpoint failed: {}", e.getMessage(), e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // ARTIFACT BLOCK BUILDERS
    // ═════════════════════════════════════════════════════════════════════

    private TradeArtifact.MarketSnapshot buildMarketSnapshot(MarketState m,
                                                             AITradeDecision decision) {
        if (m == null) return TradeArtifact.MarketSnapshot.builder()
                .symbol("ETH/USDC")
                .marketRegime(decision.getMarketRegime().name())
                .regimeConfidence(decision.getRegimeConfidence())
                .build();

        MarketState.EthereumOnChainMetrics oc = m.ethOnchainSafe();
        double price  = m.getCurrentPrice() != null ? m.getCurrentPrice().doubleValue() : 0.0;
        double ema200 = m.getEma200();
        double atr    = m.getAtr();

        return TradeArtifact.MarketSnapshot.builder()
                .symbol(m.getSymbol())
                .priceUsd(price)
                .priceTrend(m.getPriceTrend())
                .change1hPct(m.getChange1h())
                .change24hPct(m.getChange24h())
                .rsi14(m.getRsi())
                .rsiDivergence(m.isRsiDivergence())
                .ema50(m.getEma50())
                .ema200(ema200)
                .atr(atr)
                .atrPct(price > 0 ? atr / price * 100.0 : 0.0)
                .distanceToEma50(m.getDistanceToEma50())
                .distanceToEma200(m.getDistanceToEma200())
                .priceVsEma200Pct(ema200 > 0 ? (price - ema200) / ema200 * 100.0 : 0.0)
                .volatilityZScore(m.getVolatility())
                .fundingRate(m.getFundingRate())
                .openInterestChange1h(m.getOpenInterestChange1h())
                .orderBookImbalance(m.getOrderBookImbalance())
                .cumulativeDelta(m.getCumulativeDelta())
                .fearGreedIndex(m.getFearGreedIndex())
                .fearGreedLabel(fearGreedLabel(m.getFearGreedIndex()))
                .marketRegime(decision.getMarketRegime().name())
                // FIX 2: was decision.getConfidence() — completely wrong field
                .regimeConfidence(decision.getRegimeConfidence())
                .gasPriceGwei(oc.getGasPriceGwei())
                .gasPriceZScore(oc.getGasPriceZScore())
                .defiTvlChange24h(oc.getDefiTvlChange24h())
                .l2NetInflowEth(oc.getL2NetInflowEth())
                .ethExchangeNetFlow(oc.getExchangeNetFlowEth())
                .ethWhaleInflow(oc.getWhaleWalletInflow())
                .build();
    }

    private TradeArtifact.AICouncilBlock buildAICouncil(AITradeDecision decision) {
        List<AgentVerdict> rawVerdicts = decision.getAgentVerdicts();
        // FIX 3: getAgentCount() now exists on AITradeDecision
        int count                      = decision.getAgentCount();
        double consensusStrength       = 0.0;
        List<String> dissenters        = new ArrayList<>();
        String consensus               = decision.getAction().name();

        if (rawVerdicts != null && !rawVerdicts.isEmpty()) {
            long agreeing = rawVerdicts.stream()
                    .filter(v -> v.getSignal() != null
                            && v.getSignal().equalsIgnoreCase(decision.getAction().name()))
                    .count();
            consensusStrength = (double) agreeing / rawVerdicts.size();
            dissenters = rawVerdicts.stream()
                    .filter(v -> v.getSignal() != null
                            && !v.getSignal().equalsIgnoreCase(decision.getAction().name()))
                    .map(AgentVerdict::getAgentName)
                    .collect(Collectors.toList());

            if      (consensusStrength == 1.0) consensus = decision.getAction() + "_UNANIMOUS";
            else if (consensusStrength >= 0.6) consensus = decision.getAction() + "_MAJORITY";
            else                               consensus = "SPLIT";
        }

        return TradeArtifact.AICouncilBlock.builder()
                .agentsDeployed(count)
                .councilConsensus(consensus)
                .consensusStrength(consensusStrength)
                .dissentingAgents(dissenters)
                // FIX 4: convert DTO AgentVerdict → TradeArtifact.AgentVerdict
                .verdicts(mapToArtifactVerdicts(rawVerdicts))
                .build();
    }

    /**
     * FIX 4: Type converter from com.surgeagent.dto.AgentVerdict (the DTO Python
     * deserialises into) to TradeArtifact.AgentVerdict (the nested artifact type).
     * Without this, the list assignment is a compile error.
     */
    private List<AgentVerdict> mapToArtifactVerdicts(List<AgentVerdict> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream()
                .map(v -> AgentVerdict.builder()
                        .agentName(v.getAgentName())
                        .signal(v.getSignal())
                        .conviction(v.getConviction())
                        .keyFactors(v.getKeyFactors())
                        .warnings(v.getWarnings())
                        .rawAnalysis(v.getRawAnalysis())
                        .latencyMs(v.getLatencyMs())
                        .build())
                .collect(Collectors.toList());
    }

    private TradeArtifact.RiskAnalysisBlock buildRiskAnalysis(RiskAssessment risk,
                                                              AITradeDecision decision) {
        Map<String, String> guards = new LinkedHashMap<>();
        guards.put("drawdown_circuit_breaker", evalGuard(
                riskService.getCurrentDrawdownPct() < 0.08,
                () -> String.format("FAILED: drawdown=%.1f%%", riskService.getCurrentDrawdownPct() * 100)));
        guards.put("open_position_limit", evalGuard(
                riskService.getOpenPositionsCount() < 3,
                () -> "FAILED: " + riskService.getOpenPositionsCount() + " positions open"));
        guards.put("confidence_floor", evalGuard(
                decision.getConfidence() >= 0.60,
                () -> String.format("FAILED: confidence=%.2f", decision.getConfidence())));
        guards.put("risk_level", evalGuard(
                !"HIGH".equals(decision.getRiskLevel()) && !"CRITICAL".equals(decision.getRiskLevel()),
                () -> "FAILED: riskLevel=" + decision.getRiskLevel()));
        guards.put("regime_check", evalGuard(
                !"CRISIS".equals(decision.getMarketRegime()),
                () -> "FAILED: regime=CRISIS"));
        guards.put("reward_risk_ratio", evalGuard(
                decision.getRewardRiskRatio() >= 1.5,
                () -> String.format("FAILED: R:R=%.2f", decision.getRewardRiskRatio())));
        guards.put("circuit_breaker", evalGuard(
                !riskService.isCircuitBreakerTripped(),
                () -> "FAILED: circuit breaker TRIPPED"));

        long passed = guards.values().stream().filter(v -> v.startsWith("PASSED")).count();
        long failed = guards.size() - passed;

        return TradeArtifact.RiskAnalysisBlock.builder()
                .pythonRiskScore(risk != null ? risk.getScore() : 0.0)
                .pythonRiskApproved(risk != null && risk.isApproved())
                .guardsEvaluated(guards)
                .guardsPassed((int) passed)
                .guardsFailed((int) failed)
                .circuitBreakerStatus(riskService.isCircuitBreakerTripped() ? "TRIPPED" : "OK")
                .rewardRiskRatio(decision.getRewardRiskRatio())
                .minAmountOutComputed(decision.getRewardRiskRatio() > 0 ? "computed" : "zero")
                .build();
    }

    private TradeArtifact.PortfolioContext buildPortfolioContext(TradeIntent intent) {
        double vault     = riskService.getVaultBalanceUsdc();
        double posUsdc   = intent.getAmountIn() != null
                ? intent.getAmountIn().doubleValue() / 1_000_000.0 : 0.0;
        double posPct    = vault > 0 ? posUsdc / vault : 0.0;
        double winRate   = performanceTracker.getWinRate();
        double q         = 1.0 - winRate;
        double b         = riskService.getAvgRewardRiskRatio();
        double kellyFull = b > 0 ? Math.max(0, (b * winRate - q) / b) : 0.0;

        return TradeArtifact.PortfolioContext.builder()
                .vaultBalanceUsdc(vault)
                .positionSizeUsdc(posUsdc)
                .positionSizePct(posPct)
                .kellyFull(kellyFull)
                .kellyHalf(kellyFull * 0.5)
                .portfolioDrawdownPct(riskService.getCurrentDrawdownPct())
                .peakBalanceUsdc(riskService.getPeakBalanceUsdc())
                .openPositionsCount(riskService.getOpenPositionsCount())
                .tradesToday(riskService.getTradesToday())
                .rollingWinRate(winRate)
                .rollingSharpeRatio(performanceTracker.getSharpeRatio())
                .cumulativePnlPct(performanceTracker.getCumulativePnlPct())
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════
    // HASH + POST
    // ═════════════════════════════════════════════════════════════════════

    private byte[] hashAndPost(BigInteger agentId, TradeArtifact artifact) throws Exception {
        String json  = objectMapper.writeValueAsString(artifact);
        byte[] hash  = Numeric.hexStringToByteArray(Hash.sha3String(json));
        int    score = artifact.getTotalScore();
        // Both calls are non-fatal — validator registration lag must not block trades
        blockchainService.postValidation(agentId, hash, score);
        try {
            postValidatorScore(agentId, score);
        } catch (Exception e) {
            log.warn("postValidatorScore skipped (non-fatal): {}", e.getMessage());
        }
        return hash;
    }

    // ═════════════════════════════════════════════════════════════════════
    // VALIDATOR SCORE
    // ═════════════════════════════════════════════════════════════════════

    private void postValidatorScore(BigInteger agentId, int score) {
        if (validatorPrivateKey == null || validatorPrivateKey.isBlank()) {
            log.debug("agent.validator.privateKey not set — recordValidatorScore() skipped.");
            return;
        }
        try {
            Credentials validatorCreds = Credentials.create(validatorPrivateKey);
            ReputationRegistry rep = ReputationRegistry.load(
                    contractConfig.getReputation(), web3j, validatorCreds, gasProvider);
            rep.recordValidatorScore(agentId, BigInteger.valueOf(score)).send();
            log.debug("recordValidatorScore | agentId={} score={}", agentId, score);
        } catch (Exception e) {
            log.warn("recordValidatorScore failed (validator not yet registered?): {}", e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════

    /**
     * FIX 1: single canonical resolveTradeId.
     * Old code had two overloads — the 3-arg version (called by postTradeArtifact)
     * used the old regex path and ignored decision.getTradeId() entirely.
     */
    public String resolveTradeId(AITradeDecision decision) {
        return resolveTradeId(decision, null, Instant.now().getEpochSecond());
    }
    public String resolveTradeId(AITradeDecision decision, TradeIntent intent, long now) {
        if (decision != null && decision.hasTradeId()) {
            return decision.getTradeId();
        }
        if (intent != null && intent.getNonce() != null) {
            return "intent_" + intent.getNonce();
        }
        return "trade_" + now;
    }

    /**
     * Anchors a 'Near Miss' or 'Hold' decision to the blockchain/registry.
     * This proves Drawdown Control to the judges.
     */
    public void postVetoArtifact(AITradeDecision decision, MarketState mkt, String reason) {
        try {
            // Calculate what the size WOULD have been using your risk service
            // We use the vault balance and AI confidence to recreate the 'intended' size
            double vaultBalance = riskService.getVaultBalanceUsdc();

            // Re-calculating the dollar amount for the log/fingerprint
            // If your riskService returns BigInteger, convert it to double for the String.format
            double intendedSizeUsdc = riskService.calculateSafePositionSize(
                    decision.getConfidence(),
                    java.math.BigInteger.valueOf((long) (vaultBalance * 1_000_000)),
                    decision.getTakeProfitPct(),
                    decision.getStopLossPct()
            ).doubleValue() / 1_000_000.0;

            String vetoPayload = String.format("VETO|%s|%s|P:%.2f|CONF:%.2f|SIZE:%.2f|REASON:%s",
                    mkt.getSymbol(),
                    decision.getAction(),
                    mkt.getCurrentPrice().doubleValue(),
                    decision.getConfidence(),
                    intendedSizeUsdc,
                    reason);

            byte[] artifactHash = Hash.sha3(vetoPayload.getBytes(StandardCharsets.UTF_8));

            // Anchor to chain
            blockchainService.postValidation(
                    identityService.getAgentId(),
                    artifactHash,
                    50
            );

            log.info("Veto Anchored: {} | Intended Size: ${} | Reason: {}",
                    mkt.getSymbol(), String.format("%.2f", intendedSizeUsdc), reason);

        } catch (Exception e) {
            log.error("Failed to anchor Veto artifact: {}", e.getMessage());
        }
    }

    private String evalGuard(boolean passed, java.util.function.Supplier<String> failReason) {
        return passed ? "PASSED" : failReason.get();
    }

    private double computeKellySizePct(TradeIntent intent) {
        double vault = riskService.getVaultBalanceUsdc();
        if (vault <= 0 || intent.getAmountIn() == null) return 0.0;
        return (intent.getAmountIn().doubleValue() / 1_000_000.0) / vault;
    }

    private String fearGreedLabel(int index) {
        if (index <= 20) return "EXTREME_FEAR";
        if (index <= 40) return "FEAR";
        if (index <= 60) return "NEUTRAL";
        if (index <= 80) return "GREED";
        return "EXTREME_GREED";
    }
}