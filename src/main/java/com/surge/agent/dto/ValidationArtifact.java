package com.surge.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.surge.agent.dto.artifact.AgentVerdict;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ValidationArtifact {

    // ── Identity ──────────────────────────────────────────────────────────
    @JsonProperty("agent_id")
    private long agentId;

    @JsonProperty("trade_id")
    private String tradeId;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("artifact_version")
    @Builder.Default
    private String artifactVersion = "3.0";

    // ── Decision Summary ───────────────────────────────────────────────────
    @JsonProperty("action")
    private String action;                    // BUY, SELL, HOLD

    @JsonProperty("confidence")
    private double confidence;

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("market_regime")
    private String marketRegime;

    // ── Dynamic TP/SL ──────────────────────────────────────────────────────
    @JsonProperty("take_profit_pct")
    private double takeProfitPct;

    @JsonProperty("stop_loss_pct")
    private double stopLossPct;

    @JsonProperty("reward_risk_ratio")
    private double rewardRiskRatio;           // takeProfitPct / stopLossPct

    @JsonProperty("kelly_size_pct")
    private double kellySizePct;              // fraction of portfolio risked

    // ── Market Snapshot ───────────────────────────────────────────────────
    @JsonProperty("entry_price")
    private double entryPrice;

    @JsonProperty("rsi")
    private double rsi;

    @JsonProperty("ema50")
    private double ema50;

    @JsonProperty("ema200")
    private double ema200;

    @JsonProperty("atr")
    private double atr;

    @JsonProperty("funding_rate")
    private double fundingRate;

    @JsonProperty("fear_greed_index")
    private int fearGreedIndex;

    @JsonProperty("exchange_net_flow_eth")
    private double exchangeNetFlowEth;

    // ── Full AI Council Reasoning ─────────────────────────────────────────
    /**
     * Each element is the structured verdict from one of the 5 agents.
     * This is the core of the artifact — judges can see exactly why the AI traded.
     */
    @JsonProperty("agent_verdicts")
    private List<AgentVerdict> agentVerdicts;

    @JsonProperty("judge_reasoning")
    private String judgeReasoning;

    // ── Risk Guard Outcomes ────────────────────────────────────────────────
    /** Map of riskGuardName → "PASSED" or "FAILED: reason" */
    @JsonProperty("risk_guards")
    private Map<String, String> riskGuards;

    // ── Portfolio Context ─────────────────────────────────────────────────
    @JsonProperty("portfolio_drawdown_pct")
    private double portfolioDrawdownPct;

    @JsonProperty("open_positions_at_entry")
    private int openPositionsAtEntry;

    @JsonProperty("sharpe_ratio_rolling")
    private double sharpeRatioRolling;        // Sharpe at time of trade

    // ── Outcome (updated in feedback) ────────────────────────────────────
    @JsonProperty("outcome_pnl")
    @Builder.Default
    private double outcomePnl = 0.0;          // Populated when trade closes

    @JsonProperty("outcome_exit_reason")
    @Builder.Default
    private String outcomeExitReason = "OPEN";

    /**
     * Computes an objective 0–100 validator score from measurable criteria.
     * This is the uint8 score passed to postValidation().
     *
     * Criteria (100 points total):
     *  15 — Confidence >= 0.65
     *  15 — All 5 agent verdicts present
     *  20 — Risk level not HIGH or CRITICAL
     *  20 — TP/SL reward:risk >= 1.5
     *  15 — Market regime not CRISIS
     *  15 — Trade resulted in profit (updated ex-post)
     */
    public int computeObjectiveScore() {
        int score = 0;
        if (confidence >= 0.65)                       score += 15;
        if (agentVerdicts != null && agentVerdicts.size() == 5) score += 15;
        if (!"HIGH".equals(riskLevel) && !"CRITICAL".equals(riskLevel)) score += 20;
        if (rewardRiskRatio >= 1.5)                   score += 20;
        if (!"CRISIS".equals(marketRegime))           score += 15;
        if (outcomePnl > 0)                           score += 15;
        return Math.min(score, 100);
    }
}