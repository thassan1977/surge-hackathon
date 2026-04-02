package com.surge.agent.dto.artifact;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════
 * STRATEGY CHECKPOINT ARTIFACT  —  Surge-Agent V3
 * ═══════════════════════════════════════════════════════════════════════
 *
 * Posted to ValidationRegistry every N trades (default: 10).
 * Proves the strategy is healthy and disciplined over a rolling window.
 *
 * Hackathon judges use this to evaluate Sharpe ratio, drawdown control,
 * and whether the circuit breaker was ever tripped.
 *
 * ── Score Formula (100 pts) ──────────────────────────────────────────
 *
 *   50 pts — Sharpe quality   (> 1.5 = 50, > 0.5 = 30, > 0 = 15)
 *   30 pts — Drawdown control (< 3% = 30, < 6% = 15, < 8% = 5)
 *   20 pts — Circuit breaker  (never tripped = 20)
 *
 * ═══════════════════════════════════════════════════════════════════════
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "artifact_type", "artifact_version",
        "identity", "window", "performance",
        "agent_stats", "regime_stats", "risk_health", "score_breakdown"
})
public class StrategyCheckpointArtifact {

    @JsonProperty("artifact_type")
    @Builder.Default
    private String artifactType = "STRATEGY_CHECKPOINT";

    @JsonProperty("artifact_version")
    @Builder.Default
    private String artifactVersion = "3.0";

    // ── Identity ──────────────────────────────────────────────────────────

    @JsonProperty("identity")
    private CheckpointIdentity identity;

    @Data @Builder
    public static class CheckpointIdentity {
        @JsonProperty("agent_id")           private String agentId;
        @JsonProperty("checkpoint_number")  private int    checkpointNumber;
        @JsonProperty("timestamp_unix")     private long   timestampUnix;
        @JsonProperty("timestamp_iso")      private String timestampIso;
        @JsonProperty("chain_id")           private long   chainId;
    }

    // ── Window stats ──────────────────────────────────────────────────────

    @JsonProperty("window")
    private WindowStats window;

    @Data @Builder
    public static class WindowStats {
        @JsonProperty("trades_in_window")   private int  tradesInWindow;
        @JsonProperty("window_size")        private int  windowSize;
        @JsonProperty("buys")               private int  buys;
        @JsonProperty("sells")              private int  sells;
        @JsonProperty("holds_vetoed")       private int  holdsVetoed;
        @JsonProperty("window_start_unix")  private long windowStartUnix;
        @JsonProperty("window_end_unix")    private long windowEndUnix;
    }

    // ── Performance ───────────────────────────────────────────────────────

    @JsonProperty("performance")
    private PerformanceMetrics performance;

    @Data @Builder
    public static class PerformanceMetrics {
        @JsonProperty("sharpe_ratio")        private double sharpeRatio;
        @JsonProperty("sortino_ratio")       private double sortinoRatio;
        @JsonProperty("win_rate")            private double winRate;
        @JsonProperty("avg_win_pct")         private double avgWinPct;
        @JsonProperty("avg_loss_pct")        private double avgLossPct;
        @JsonProperty("profit_factor")       private double profitFactor;
        @JsonProperty("cumulative_pnl_pct")  private double cumulativePnlPct;
        @JsonProperty("best_trade_pct")      private double bestTradePct;
        @JsonProperty("worst_trade_pct")     private double worstTradePct;
        @JsonProperty("avg_hold_seconds")    private double avgHoldSeconds;
        @JsonProperty("tp_hit_rate")         private double tpHitRate;
    }

    // ── Agent stats ───────────────────────────────────────────────────────

    @JsonProperty("agent_stats")
    private AgentStats agentStats;

    @Data @Builder
    public static class AgentStats {
        @JsonProperty("win_rates_by_agent")  private Map<String, Double> winRatesByAgent;
        @JsonProperty("avg_pnl_by_agent")    private Map<String, Double> avgPnlByAgent;
        @JsonProperty("mvp_agent")           private String              mvpAgent;
    }

    // ── Regime stats ──────────────────────────────────────────────────────

    @JsonProperty("regime_stats")
    private RegimeStats regimeStats;

    @Data @Builder
    public static class RegimeStats {
        @JsonProperty("trades_by_regime")    private Map<String, Integer> tradesByRegime;
        @JsonProperty("win_rate_by_regime")  private Map<String, Double>  winRateByRegime;
        @JsonProperty("best_regime")         private String               bestRegime;
        @JsonProperty("worst_regime")        private String               worstRegime;
    }

    // ── Risk health ───────────────────────────────────────────────────────

    @JsonProperty("risk_health")
    private RiskHealth riskHealth;

    @Data @Builder
    public static class RiskHealth {
        @JsonProperty("current_drawdown_pct")      private double  currentDrawdownPct;
        @JsonProperty("max_drawdown_pct_window")   private double  maxDrawdownPctWindow;
        @JsonProperty("circuit_breaker_trips")     private int     circuitBreakerTrips;
        @JsonProperty("circuit_breaker_clean")     private boolean circuitBreakerClean;
        @JsonProperty("open_positions")            private int     openPositions;
        @JsonProperty("avg_reward_risk_ratio")     private double  avgRewardRiskRatio;
    }

    // ── Score ─────────────────────────────────────────────────────────────

    @JsonProperty("score_breakdown")
    private CheckpointScore scoreBreakdown;

    @Data @Builder
    public static class CheckpointScore {
        @JsonProperty("total_score")      private int    totalScore;
        @JsonProperty("sharpe_pts")       private int    sharpePts;
        @JsonProperty("drawdown_pts")     private int    drawdownPts;
        @JsonProperty("cb_clean_pts")     private int    cbCleanPts;
        @JsonProperty("scoring_formula")  private String scoringFormula;
        @JsonProperty("scored_at_unix")   private long   scoredAtUnix;
    }

    /**
     * Computes and attaches the checkpoint score.
     * @return this artifact (fluent)
     */
    public StrategyCheckpointArtifact computeAndAttachScore() {
        int sharpePts   = 0;
        int drawdownPts = 0;
        int cbPts       = 0;

        if (performance != null) {
            double sr = performance.getSharpeRatio();
            if      (sr > 1.5) sharpePts = 50;
            else if (sr > 0.5) sharpePts = 30;
            else if (sr > 0.0) sharpePts = 15;
        }
        if (riskHealth != null) {
            double dd = riskHealth.getCurrentDrawdownPct();
            if      (dd < 0.03) drawdownPts = 30;
            else if (dd < 0.06) drawdownPts = 15;
            else if (dd < 0.08) drawdownPts = 5;
            if (riskHealth.isCircuitBreakerClean()) cbPts = 20;
        }

        int total = sharpePts + drawdownPts + cbPts;
        this.scoreBreakdown = CheckpointScore.builder()
                .totalScore(total)
                .sharpePts(sharpePts)
                .drawdownPts(drawdownPts)
                .cbCleanPts(cbPts)
                .scoringFormula("sharpe(50) + drawdown_control(30) + cb_clean(20) = 100")
                .scoredAtUnix(System.currentTimeMillis() / 1000)
                .build();
        return this;
    }

    public int getTotalScore() {
        return scoreBreakdown != null ? scoreBreakdown.getTotalScore() : 0;
    }
}
