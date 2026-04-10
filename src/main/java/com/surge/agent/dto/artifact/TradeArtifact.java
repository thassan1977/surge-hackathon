package com.surge.agent.dto.artifact;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * TRADE ARTIFACT  —  Surge-Agent V3  —  World-Class Hackathon Edition
 * ═══════════════════════════════════════════════════════════════════════
 *
 * The document that is keccak256-hashed and posted to ValidationRegistry
 * for every executed trade.
 *
 * Design principles:
 *
 *   VERIFIABLE  — every field is objective and reproducible from on-chain
 *                 events + the Python AI response. Judges can verify the
 *                 hash independently.
 *
 *   TRANSPARENT — contains the full 5-agent council reasoning so judges
 *                 can see WHY the agent traded, not just that it did.
 *
 *   SCOREABLE   — computeAndAttachScore() produces an objective 0–100
 *                 score with a formula that any judge can audit and verify.
 *
 *   UPDATABLE   — outcome fields start as OPEN and are updated when the
 *                 trade closes (TP/SL hit). A re-post captures final score
 *                 including the profitability factor.
 *
 * ── Score Formula (100 pts) ──────────────────────────────────────────
 *
 *   15 pts — confidence >= 0.65                  (non-trivial conviction)
 *   15 pts — all 5 agents deployed               (full council engaged)
 *   20 pts — risk level LOW or MEDIUM            (disciplined entry)
 *   20 pts — reward:risk >= 1.5                  (structural edge enforced)
 *   15 pts — regime not CRISIS                   (rational market)
 *    1 pt  — regime confidence >= 0.80 (bonus)   (detector certainty)
 *   15 pts — trade closed profitably             (ex-post truth, on outcome update)
 *
 * Total possible: 101 (capped at 100)
 * ═══════════════════════════════════════════════════════════════════════
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "artifact_type", "artifact_version", "schema_url",
        "identity", "decision", "market_snapshot",
        "ai_council", "risk_analysis", "portfolio_context",
        "execution", "outcome", "score_breakdown"
})
public class TradeArtifact {

    @JsonProperty("artifact_type")
    @Builder.Default
    private String artifactType = "TRADE_DECISION";

    @JsonProperty("artifact_version")
    @Builder.Default
    private String artifactVersion = "3.1"; // Incremented for live hackathon schema

    @JsonProperty("schema_url")
    @Builder.Default
    private String schemaUrl = "https://surge-agent.io/schemas/trade-artifact-v3.1.json";

    @JsonProperty("identity")
    private IdentityBlock identity;

    @Data @Builder
    public static class IdentityBlock {
        @JsonProperty("agent_id")         private String agentId;
        @JsonProperty("trade_id")         private String tradeId;
        @JsonProperty("timestamp_unix")   private long   timestampUnix;
        @JsonProperty("timestamp_iso")    private String timestampIso;
        @JsonProperty("chain_id")         private long   chainId;
        @JsonProperty("vault_address")    private String vaultAddress;
        @JsonProperty("router_address")   private String routerAddress;
    }

    @JsonProperty("decision")
    private DecisionBlock decision;

    @Data @Builder
    public static class DecisionBlock {
        @JsonProperty("action")              private String  action;
        @JsonProperty("confidence")          private double  confidence;
        @JsonProperty("risk_level")          private String  riskLevel;
        @JsonProperty("market_regime")       private String  marketRegime;
        @JsonProperty("take_profit_pct")     private double  takeProfitPct;
        @JsonProperty("stop_loss_pct")       private double  stopLossPct;
        @JsonProperty("reward_risk_ratio")   private double  rewardRiskRatio;
        @JsonProperty("kelly_size_pct")      private double  kellySizePct;
        @JsonProperty("judge_reasoning")     private String  judgeReasoning;
        @JsonProperty("regime_confidence")   private double  regimeConfidence;
    }

    @JsonProperty("market_snapshot")
    private MarketSnapshot marketSnapshot;

    @Data @Builder
    public static class MarketSnapshot {
        @JsonProperty("symbol")                  private String  symbol;
        @JsonProperty("price_usd")               private double  priceUsd;
        @JsonProperty("price_trend")             private String  priceTrend;
        @JsonProperty("change_1h_pct")           private double  change1hPct;
        @JsonProperty("change_24h_pct")          private double  change24hPct;
        @JsonProperty("rsi_14")                  private double  rsi14;
        @JsonProperty("rsi_divergence")          private boolean rsiDivergence;
        @JsonProperty("ema_50")                  private double  ema50;
        @JsonProperty("ema_200")                 private double  ema200;
        @JsonProperty("atr")                     private double  atr;
        @JsonProperty("atr_pct")                 private double  atrPct;
        @JsonProperty("distance_to_ema50")       private double  distanceToEma50;
        @JsonProperty("distance_to_ema200")      private double  distanceToEma200;
        @JsonProperty("price_vs_ema200_pct")     private double  priceVsEma200Pct;
        @JsonProperty("volatility_z_score")      private double  volatilityZScore;
        @JsonProperty("funding_rate")            private double  fundingRate;
        @JsonProperty("open_interest_change_1h") private double  openInterestChange1h;
        @JsonProperty("order_book_imbalance")    private double  orderBookImbalance;
        @JsonProperty("cumulative_delta")        private double  cumulativeDelta;
        @JsonProperty("fear_greed_index")        private int     fearGreedIndex;
        @JsonProperty("fear_greed_label")        private String  fearGreedLabel;
        @JsonProperty("market_regime")           private String  marketRegime;
        @JsonProperty("regime_confidence")       private double  regimeConfidence;
        @JsonProperty("gas_price_gwei")          private double  gasPriceGwei;
        @JsonProperty("gas_price_z_score")       private double  gasPriceZScore;
        @JsonProperty("defi_tvl_change_24h")     private double  defiTvlChange24h;
        @JsonProperty("l2_net_inflow_eth")       private double  l2NetInflowEth;
        @JsonProperty("eth_exchange_net_flow")   private double  ethExchangeNetFlow;
        @JsonProperty("eth_whale_inflow")        private double  ethWhaleInflow;
    }

    @JsonProperty("ai_council")
    private AICouncilBlock aiCouncil;

    @Data @Builder
    public static class AICouncilBlock {
        @JsonProperty("agents_deployed")       private int                  agentsDeployed;
        @JsonProperty("council_consensus")     private String               councilConsensus;
        @JsonProperty("consensus_strength")    private double               consensusStrength;
        @JsonProperty("dissenting_agents")     private List<String>         dissentingAgents;
        @JsonProperty("verdicts")              private List<AgentVerdict>   verdicts;
        @JsonProperty("agent_weights")         private Map<String, Double>  agentWeights;
    }

    @JsonProperty("risk_analysis")
    private RiskAnalysisBlock riskAnalysis;

    @Data @Builder
    public static class RiskAnalysisBlock {
        @JsonProperty("python_risk_score")       private double              pythonRiskScore;
        @JsonProperty("python_risk_approved")    private boolean             pythonRiskApproved;
        @JsonProperty("guards_evaluated")        private Map<String,String>  guardsEvaluated;
        @JsonProperty("guards_passed")           private int                 guardsPassed;
        @JsonProperty("guards_failed")           private int                 guardsFailed;
        @JsonProperty("circuit_breaker_status")  private String              circuitBreakerStatus;
        @JsonProperty("reward_risk_ratio")       private double              rewardRiskRatio;
        @JsonProperty("min_amount_out_computed") private String              minAmountOutComputed;
    }

    @JsonProperty("portfolio_context")
    private PortfolioContext portfolioContext;

    @Data @Builder
    public static class PortfolioContext {
        @JsonProperty("vault_balance_usdc")      private double  vaultBalanceUsdc;
        @JsonProperty("position_size_usdc")      private double  positionSizeUsdc;
        @JsonProperty("position_size_pct")       private double  positionSizePct;
        @JsonProperty("kelly_full")              private double  kellyFull;
        @JsonProperty("kelly_half")              private double  kellyHalf;
        @JsonProperty("portfolio_drawdown_pct")  private double  portfolioDrawdownPct;
        @JsonProperty("peak_balance_usdc")       private double  peakBalanceUsdc;
        @JsonProperty("open_positions_count")    private int     openPositionsCount;
        @JsonProperty("trades_today")            private int     tradesToday;
        @JsonProperty("rolling_win_rate")        private double  rollingWinRate;
        @JsonProperty("rolling_sharpe_ratio")    private double  rollingSharpeRatio;
        @JsonProperty("cumulative_pnl_pct")      private double  cumulativePnlPct;
    }

    // ─────────────────────────────────────────────────────────────────────
    // BLOCK 8 — Execution (Updated for Live Hackathon RiskRouter)
    // ─────────────────────────────────────────────────────────────────────

    @JsonProperty("execution")
    private ExecutionBlock execution;

    @Data @Builder
    public static class ExecutionBlock {
        @JsonProperty("pair")               private String  pair;               // e.g. "ETH/USDC"
        @JsonProperty("action")             private String  action;             // e.g. "BUY"
        @JsonProperty("amount_usd_scaled")  private String  amountUsdScaled;    // Raw EIP-712 value (18 decimals)
        @JsonProperty("amount_in_usdc")     private double  amountInUsdc;       // Human readable dollar value
        @JsonProperty("max_slippage_bps")    private int     maxSlippageBps;     // Bps instead of raw minAmountOut
        @JsonProperty("deadline")           private long    deadline;
        @JsonProperty("nonce")              private String  nonce;
        @JsonProperty("eip712_signed")      private boolean eip712Signed;
        @JsonProperty("tx_hash")            private String  txHash;
        @JsonProperty("block_number")       private Long    blockNumber;
        @JsonProperty("entry_price_usd")    private double  entryPriceUsd;
        @JsonProperty("take_profit_price")  private double  takeProfitPrice;
        @JsonProperty("stop_loss_price")    private double  stopLossPrice;
    }

    @JsonProperty("outcome")
    private OutcomeBlock outcome;

    @Data @Builder
    public static class OutcomeBlock {
        @JsonProperty("status")                  private String  status;
        @JsonProperty("exit_price_usd")          private double  exitPriceUsd;
        @JsonProperty("realised_pnl_pct")        private double  realisedPnlPct;
        @JsonProperty("realised_pnl_usdc")       private double  realisedPnlUsdc;
        @JsonProperty("hold_duration_seconds")   private long    holdDurationSeconds;
        @JsonProperty("closed_at_unix")          private long    closedAtUnix;
        @JsonProperty("closed_at_iso")           private String  closedAtIso;
        @JsonProperty("close_tx_hash")           private String  closeTxHash;
        @JsonProperty("prediction_correct")      private boolean predictionCorrect;
    }

    @JsonProperty("score_breakdown")
    private ScoreBreakdown scoreBreakdown;

    @Data @Builder
    public static class ScoreBreakdown {
        @JsonProperty("total_score")            private int    totalScore;
        @JsonProperty("confidence_pts")         private int    confidencePts;
        @JsonProperty("council_coverage_pts")   private int    councilCoveragePts;
        @JsonProperty("risk_discipline_pts")    private int    riskDisciplinePts;
        @JsonProperty("reward_risk_pts")        private int    rewardRiskPts;
        @JsonProperty("regime_awareness_pts")   private int    regimeAwarenessPts;
        @JsonProperty("regime_confidence_bonus") private int   regimeConfidenceBonus;
        @JsonProperty("profitability_pts")      private int    profitabilityPts;
        @JsonProperty("scoring_formula")        private String scoringFormula;
        @JsonProperty("scored_at_unix")         private long   scoredAtUnix;
    }

    public TradeArtifact computeAndAttachScore() {
        int confPts      = 0;
        int councilPts   = 0;
        int riskPts      = 0;
        int rrPts        = 0;
        int regimePts    = 0;
        int regimeBon    = 0;
        int profitPts    = 0;

        if (decision != null) {
            if (decision.getConfidence() >= 0.65)                                confPts   = 15;
            if (!"HIGH".equals(decision.getRiskLevel()) && !"CRITICAL".equals(decision.getRiskLevel())) riskPts = 20;
            if (decision.getRewardRiskRatio() >= 1.5)                           rrPts     = 20;
            if (!"CRISIS".equals(decision.getMarketRegime()))                   regimePts = 15;
            if (regimePts > 0 && decision.getRegimeConfidence() >= 0.80)        regimeBon = 1;
        }

        if (aiCouncil != null && aiCouncil.getAgentsDeployed() == 5)            councilPts = 15;
        if (outcome != null && !"OPEN".equals(outcome.getStatus()) && outcome.isPredictionCorrect()) profitPts = 15;

        int total = Math.min(100, confPts + councilPts + riskPts + rrPts + regimePts + regimeBon + profitPts);

        this.scoreBreakdown = ScoreBreakdown.builder()
                .totalScore(total)
                .confidencePts(confPts)
                .councilCoveragePts(councilPts)
                .riskDisciplinePts(riskPts)
                .rewardRiskPts(rrPts)
                .regimeAwarenessPts(regimePts)
                .regimeConfidenceBonus(regimeBon)
                .profitabilityPts(profitPts)
                .scoringFormula("confidence(15) + council_coverage(15) + risk_discipline(20) + reward_risk(20) + regime_awareness(15) + regime_confidence_bonus(0|1) + profitability(15) = max 101, capped at 100")
                .scoredAtUnix(Instant.now().getEpochSecond())
                .build();

        return this;
    }

    public int getTotalScore() {
        return scoreBreakdown != null ? scoreBreakdown.getTotalScore() : 0;
    }

    public static OutcomeBlock openOutcome() {
        return OutcomeBlock.builder()
                .status("OPEN")
                .realisedPnlPct(0.0)
                .realisedPnlUsdc(0.0)
                .predictionCorrect(false)
                .build();
    }

    public static OutcomeBlock closedOutcome(String exitReason, double entryPrice, double exitPrice, double positionSizeUsdc, String action, long openedAtEpoch, String closeTxHash) {
        double pnlPct = "SELL".equalsIgnoreCase(action) ? (entryPrice - exitPrice) / entryPrice : (exitPrice - entryPrice) / entryPrice;
        double pnlUsdc = positionSizeUsdc * pnlPct;
        long now = Instant.now().getEpochSecond();
        String closedIso = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(now).atOffset(ZoneOffset.UTC));

        String status = switch (exitReason.toUpperCase()) {
            case "TP" -> "CLOSED_TP";
            case "SL" -> "CLOSED_SL";
            case "REVERT" -> "CLOSED_REVERT";
            default -> "CLOSED_" + exitReason.toUpperCase();
        };

        return OutcomeBlock.builder()
                .status(status)
                .exitPriceUsd(exitPrice)
                .realisedPnlPct(pnlPct)
                .realisedPnlUsdc(pnlUsdc)
                .holdDurationSeconds(now - openedAtEpoch)
                .closedAtUnix(now)
                .closedAtIso(closedIso)
                .closeTxHash(closeTxHash)
                .predictionCorrect(pnlPct > 0)
                .build();
    }
}