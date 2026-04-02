package com.surge.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.surge.agent.dto.artifact.AgentVerdict;
import com.surge.agent.enums.MarketRegime;
import com.surge.agent.enums.RiskLevel;
import com.surge.agent.enums.TradeAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AITradeDecision {

    private TradeAction action;

    /** 0.0–1.0 overall council confidence */
    private double confidence;

    /** Human-readable Judge synthesis */
    private String reasoning;

    private RiskLevel riskLevel;


    /**
     * Take-profit as a decimal fraction (e.g. 0.025 = 2.5%).
     * Computed by AI Judge using ATR. Defaults to 0.02 (your current hardcoded value).
     */
    @JsonProperty("takeProfitPct")
    @Builder.Default
    private double takeProfitPct = 0.02;

    /**
     * Stop-loss as a decimal fraction (e.g. 0.012 = 1.2%).
     * Computed by AI Judge using ATR. Defaults to 0.01 (your current hardcoded value).
     */
    @JsonProperty("stopLossPct")
    @Builder.Default
    private double stopLossPct = 0.01;

    /**
     * Market regime at decision time.
     * BULL_TREND | BEAR_TREND | RANGING | ACCUMULATION | DISTRIBUTION | CRISIS
     */
    @JsonProperty("marketRegime")
    @Builder.Default
    private MarketRegime marketRegime = MarketRegime.RANGING;

    // defaults to 0.65 (RANGING default) so existing artifacts are valid.
    @JsonProperty("regimeConfidence")
    @Builder.Default
    private double regimeConfidence = 0.65;

    @JsonProperty("tradeId")
    private String tradeId;


    /**
     * Structured verdict from each of the 5 council agents.
     * Null-safe — will be null if Python V2 is still running.
     */
    @JsonProperty("agentVerdicts")
    private List<AgentVerdict> agentVerdicts;

    // ── Original helper — UNCHANGED ───────────────────────────────────────

    public boolean isActionable() {
        return TradeAction.BUY.equals(action) ||
                TradeAction.SELL.equals(action);
    }

    // ── New computed helpers ──────────────────────────────────────────────

    /** Multiplier for take-profit price: entryPrice * getTakeProfitMultiplier() */
    public double getTakeProfitMultiplier() {
        return 1.0 + takeProfitPct;
    }

    /** Multiplier for stop-loss price: entryPrice * getStopLossMultiplier() */
    public double getStopLossMultiplier() {
        return 1.0 - stopLossPct;
    }

    /** Reward-to-risk ratio. >= 1.5 is required to pass validation scoring. */
    public double getRewardRiskRatio() {
        return stopLossPct > 0 ? takeProfitPct / stopLossPct : 0.0;
    }

    public boolean hasAgentVerdicts() {
        return agentVerdicts != null && !agentVerdicts.isEmpty();
    }

    public int getAgentCount() {
        return agentVerdicts != null ? agentVerdicts.size() : 0;
    }

    public boolean hasTradeId() {
        return tradeId != null && !tradeId.isBlank();
    }
}