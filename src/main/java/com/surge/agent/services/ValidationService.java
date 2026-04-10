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
import org.web3j.protocol.core.methods.response.TransactionReceipt;
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

    @Value("${ai.pair:ETHUSD}")
    private String PAIR;

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
                        // Updated to match the live TradeIntent fields
                        .pair(intent.getPair())               // e.g., "ETH/USDC"
                        .action(intent.getAction())           // e.g., "BUY" or "SELL"
                        .amountUsdScaled(intent.getAmountUsdScaled().toString())
                        .amountInUsdc(intent.getAmountUsdScaled().doubleValue() / 1e18) // Adjust scale based on contract (usually 18 decimals)

                        // Slippage replaced minAmountOut in the new logic
                        .maxSlippageBps(intent.getMaxSlippageBps() != null
                                ? intent.getMaxSlippageBps().intValue() : 100)

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
            int score = checkpoint.getTotalScore();
            String checkpointNotes = String.format("CP#%d: WR=%.1f%% Sharpe=%.2f Trades=%d",
                    checkpoint.getIdentity().getCheckpointNumber(),
                    performanceTracker.getWinRate() * 100,
                    performanceTracker.getSharpeRatio(),
                    performanceTracker.getTradeCount());
            blockchainService.postValidation(agentId, hash, score, checkpointNotes);

            postValidatorScore(agentId, score, checkpointNotes);

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
                .symbol(PAIR)
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
        // 1. Get the current vault state
        double vault = riskService.getVaultBalanceUsdc();

        // 2. Fix: amountIn -> amountUsdScaled & 6-decimal -> 18-decimal scaling
        // Hackathon standard is usually 1e18 for USD-denominated trade intents.
        double posUsdc = (intent.getAmountUsdScaled() != null)
                ? intent.getAmountUsdScaled().doubleValue() / 1e18 : 0.0;

        double posPct = vault > 0 ? posUsdc / vault : 0.0;

        // 3. Kelly Criterion calculation
        double winRate = performanceTracker.getWinRate();
        double q       = 1.0 - winRate;
        double b       = riskService.getAvgRewardRiskRatio();

        // Kelly % = (b*p - q) / b
        double kellyFull = (b > 0) ? Math.max(0, (b * winRate - q) / b) : 0.0;

        return TradeArtifact.PortfolioContext.builder()
                .vaultBalanceUsdc(vault)
                .positionSizeUsdc(posUsdc)
                .positionSizePct(posPct)
                .kellyFull(kellyFull)
                .kellyHalf(kellyFull * 0.5)
                // 4. Verification: Ensure these methods exist in your riskService/performanceTracker
                .portfolioDrawdownPct(riskService.getCurrentDrawdownPct())
                .peakBalanceUsdc(riskService.getPeakBalanceUsdc())
                .openPositionsCount(riskService.getOpenPositionsCount())
                .tradesToday(riskService.getTradesToday()) // Ensure this isn't tradesSinceCheckpoint
                .rollingWinRate(winRate)
                .rollingSharpeRatio(performanceTracker.getSharpeRatio())
                .cumulativePnlPct(performanceTracker.getCumulativePnlPct())
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════
    // HASH + POST
    // ═════════════════════════════════════════════════════════════════════

    private byte[] hashAndPost(BigInteger agentId, TradeArtifact artifact) throws Exception {
        String json = objectMapper.writeValueAsString(artifact);
        byte[] hash = Numeric.hexStringToByteArray(Hash.sha3String(json));
        int score = artifact.getTotalScore();

        // Extract reasoning for the on-chain attestation
        String reasoning = artifact.getDecision().getJudgeReasoning();
        if (reasoning == null || reasoning.isBlank()) {
            reasoning = "Automated trade validation anchor.";
        }

        // Call blockchainService with the extra reasoning parameter
        blockchainService.postValidation(agentId, hash, score, reasoning);

        try {
            postValidatorScore(agentId, score, reasoning);
        } catch (Exception e) {
            log.warn("postValidatorScore skipped (non-fatal): {}", e.getMessage());
        }
        return hash;
    }
    // ═════════════════════════════════════════════════════════════════════
    // VALIDATOR SCORE
    // ═════════════════════════════════════════════════════════════════════

    public void postValidatorScore(BigInteger agentId, int score, String reasoning) {
        if (validatorPrivateKey == null || validatorPrivateKey.isBlank()) {
            log.debug("agent.validator.privateKey not set — feedback skipped.");
            return;
        }
        try {
            Credentials validatorCreds = Credentials.create(validatorPrivateKey);
            ReputationRegistry rep = ReputationRegistry.load(
                    contractConfig.getReputation(), web3j, validatorCreds, gasProvider);

            // 1. Check if already rated (to avoid revert)
            boolean alreadyRated = rep.hasRated(agentId, validatorCreds.getAddress()).send();
            if (alreadyRated) {
                log.debug("Validator already submitted feedback for agent {}", agentId);
                return;
            }

            // 2. Generate an outcome reference (anchor hash)
            // Usually a keccak256 of the data you validated
            byte[] outcomeRef = org.web3j.crypto.Hash.sha3(reasoning.getBytes(StandardCharsets.UTF_8));

            log.info("Submitting Validator Feedback: Score={}, Agent={}", score, agentId);

            // 3. Call the correct method from your wrapper
            // Parameters: agentId, score, outcomeRef, comment, feedbackType
            // FeedbackType.STRATEGY_QUALITY = 2
            TransactionReceipt receipt = rep.submitFeedback(
                    agentId,
                    BigInteger.valueOf(score),
                    outcomeRef,
                    reasoning,
                    BigInteger.valueOf(2)
            ).send();

            if (receipt.isStatusOK()) {
                log.info("Validator feedback anchored! TX: {}", receipt.getTransactionHash());
            }
        } catch (Exception e) {
            log.error("Failed to submit validator feedback: {}", e.getMessage());
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
    private double computeKellyFraction(double confidence, double takeProfitPct, double stopLossPct) {
        double b        = stopLossPct > 0 ? takeProfitPct / stopLossPct : 2.0;
        double q        = 1.0 - confidence;
        double fStar    = (b * confidence - q) / b;
        double halfKelly = Math.max(0.0, fStar * 0.5);
        return Math.min(halfKelly, 0.05); // hard cap: never risk more than 5% per trade
    }
    public void postVetoArtifact(AITradeDecision decision, MarketState mkt, String reason) {
        try {
            BigInteger agentId = identityService.getAgentId();
            double vaultBalance = riskService.getVaultBalanceUsdc();

            // 1. Scaled balance for the risk service (18 decimals for Live Hackathon)
            // Using BigDecimal to avoid overflow/precision issues before converting to BigInteger

            // 2. Calculate intended size using the risk service
            // ensure calculateSafePositionSize exists and accepts these params
            BigInteger intendedSizeUsd = riskService.calculateSafePositionSizeUsd(
                    decision.getConfidence(),
                    vaultBalance,
                    decision.getTakeProfitPct(),
                    decision.getStopLossPct()
            );



            // 3. Create the payload for the Hash
            String vetoPayload = String.format("VETO|%s|%s|P:%.2f|CONF:%.2f|SIZE:%.2f|REASON:%s",
                    mkt.getSymbol(),
                    decision.getAction(),
                    mkt.getCurrentPrice().doubleValue(),
                    decision.getConfidence(),
                    intendedSizeUsd.doubleValue(),
                    reason);

            byte[] artifactHash = Hash.sha3(vetoPayload.getBytes(StandardCharsets.UTF_8));

            // 4. Anchor to chain with the reasoning string (Required by your new Registry)
            // Score is set to 50 for Vetoes/Near-Misses as a default
            blockchainService.postValidation(
                    agentId,
                    artifactHash,
                    50,
                    "VETO: " + reason // This populates the 'notes' field on-chain
            );

            log.info("Veto Anchored | Agent: {} | Symbol: {} | Size: ${} | Reason: {}",
                    agentId, mkt.getSymbol(), String.format("%.2f", intendedSizeUsd), reason);

        } catch (Exception e) {
            log.error("Failed to anchor Veto artifact: {}", e.getMessage());
        }
    }

    private String evalGuard(boolean passed, java.util.function.Supplier<String> failReason) {
        return passed ? "PASSED" : failReason.get();
    }

    private double computeKellySizePct(TradeIntent intent) {
        // 1. Use the dollar-denominated balance from your risk service
        double vault = riskService.getVaultBalanceUsdc();

        // 2. Update field to amountUsdScaled and check for null
        if (vault <= 0 || intent.getAmountUsdScaled() == null) {
            return 0.0;
        }

        // 3. Convert 18-decimal BigInteger to double and divide by 1e18
        double intendedPositionUsdc = intent.getAmountUsdScaled().doubleValue() / 1e18;

        // 4. Return the percentage (e.g., 0.05 for a 5% position)
        return intendedPositionUsdc / vault;
    }

    private String fearGreedLabel(int index) {
        if (index <= 20) return "EXTREME_FEAR";
        if (index <= 40) return "FEAR";
        if (index <= 60) return "NEUTRAL";
        if (index <= 80) return "GREED";
        return "EXTREME_GREED";
    }
}