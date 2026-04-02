package com.surge.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.surge.agent.enums.MarketRegime;
import com.surge.agent.enums.TradeAction;
import com.surge.agent.model.TradeIntent;
import com.surge.agent.dto.artifact.AgentVerdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.math.RoundingMode;

/**
 * TradeRecord — V3 final.
 *
 * Lightweight record of one open/closed position stored in TradeMonitorService.
 *
 * Added vs the SupportingDTOs.java version:
 *   marketRegime      — needed by ValidationService.finaliseOutcome() to call
 *                       performanceTracker.record(outcome, record.getMarketRegime(), ...)
 *   agentVerdicts     — needed by ValidationService.finaliseOutcome() to pass to
 *                       performanceTracker for per-agent win-rate attribution
 *   positionSizeUsdc  — needed by ValidationService.finaliseOutcome() to compute
 *                       realised PnL in USDC for the OutcomeBlock
 *   closedAt          — epoch of when the position closed (for hold duration)
 *   closeTxHash       — tx hash of the close transaction if available
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeRecord {

    /** Python-issued trade ID — links TradeRecord to ValidationArtifact cache key. */
    private String     tradeId;

    /** On-chain agent token ID from IdentityService. */
    private BigInteger agentId;

    // ── Trade parameters ─────────────────────────────────────────────────

    private TradeAction action;

    /** Price at which the trade filled (USD). */
    private double entryPrice;

    /** Position size in ETH. */
    private double positionSizeEth;

    /** Position size in USDC — used to compute PnL in dollar terms. */
    private double positionSizeUsdc;

    /** Absolute TP price level: entryPrice * (1 + takeProfitPct) */
    private double takeProfitPrice;

    /** Absolute SL price level: entryPrice * (1 - stopLossPct) */
    private double stopLossPrice;

    private String tokenIn;
    private String tokenOut;

    // ── Timing ───────────────────────────────────────────────────────────

    /** Unix epoch seconds when the trade was opened. */
    private long openedAtEpoch;

    /** Unix epoch seconds when the trade closed (0 if still open). */
    private long closedAtEpoch;

    // ── State ────────────────────────────────────────────────────────────

    /** True once TP or SL has been hit and finaliseOutcome() has been called. */
    private boolean closed;

    // ── On-chain linkage ─────────────────────────────────────────────────

    /** Tx hash of the executeTrade() call. */
    private String executionTxHash;

    /** Tx hash of the close transaction (if on-chain settlement). */
    private String closeTxHash;

    // ── AI context (needed for feedback loop + per-agent attribution) ────

    /**
     * Market regime at the time of the trade decision.
     * Used by ValidationService.finaliseOutcome() →
     *   performanceTracker.record(outcome, record.getMarketRegime(), ...)
     */
    private MarketRegime marketRegime;

    /**
     * Agent verdicts from the council at decision time.
     * Used by ValidationService.finaliseOutcome() →
     *   performanceTracker.record(outcome, regime, record.getAgentVerdicts())
     * This updates per-agent win-rate so Judge weights diverge from default 1.0.
     */
    private List<AgentVerdict> agentVerdicts;

    // ─────────────────────────────────────────────────────────────────────
    // FACTORY
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Creates a TradeRecord from everything available at trade execution time.
     *
     * Usage in TradeService.processSignal():
     *
     *   byte[] hash = validationService.postTradeArtifact(...);
     *   String tradeId = decision.hasTradeId() ? decision.getTradeId() : "intent_" + nonce;
     *   TradeRecord record = TradeRecord.fromDecision(tradeId, agentId, currentPrice, positionUsdc, intent, decision);
     *   tradeMonitorService.register(record);
     */
    public static TradeRecord fromDecision(String tradeId,
                                           BigInteger agentId,
                                           double currentPrice,
                                           double positionSizeUsdc,
                                           TradeIntent intent,
                                           AITradeDecision decision) {
        return TradeRecord.builder()
                .tradeId(tradeId)
                .agentId(agentId)
                .action(decision.getAction())
                .entryPrice(currentPrice)
                .positionSizeEth(weiToEth(intent.getAmountIn()))
                .positionSizeUsdc(positionSizeUsdc)
                .takeProfitPrice(currentPrice * decision.getTakeProfitMultiplier())
                .stopLossPrice(currentPrice * decision.getStopLossMultiplier())
                .tokenIn(intent.getTokenIn())
                .tokenOut(intent.getTokenOut())
                .openedAtEpoch(Instant.now().getEpochSecond())
                .closedAtEpoch(0L)
                .closed(false)
                .marketRegime(decision.getMarketRegime())
                .agentVerdicts(decision.getAgentVerdicts())
                .build();
    }

    /** Convenience: backward-compatible overload without positionSizeUsdc. */
    public static TradeRecord fromDecision(String tradeId,
                                           BigInteger agentId,
                                           double currentPrice,
                                           TradeIntent intent,
                                           AITradeDecision decision) {
        double usdc = intent.getAmountIn() != null
                ? intent.getAmountIn().doubleValue() / 1_000_000.0 : 0.0;
        return fromDecision(tradeId, agentId, currentPrice, usdc, intent, decision);
    }

    private static double weiToEth(BigInteger wei) {
        if (wei == null) return 0;
        return new BigDecimal(wei)
                .divide(BigDecimal.TEN.pow(18), 6, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRICE MONITORING
    // ─────────────────────────────────────────────────────────────────────

    /** Returns true if currentPrice has reached or passed the take-profit level */
    public boolean isAtTakeProfit(double currentPrice) {
        return TradeAction.BUY.equals(action)
                ? currentPrice >= takeProfitPrice
                : currentPrice <= takeProfitPrice;
    }

    /** Returns true if currentPrice has reached or passed the stop-loss level */
    public boolean isAtStopLoss(double currentPrice) {
        return TradeAction.BUY.equals(action)
                ? currentPrice <= stopLossPrice
                : currentPrice >= stopLossPrice;
    }

    // ─────────────────────────────────────────────────────────────────────
    // PNL CALCULATION
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Calculates realised P&L as a decimal fraction.
     * e.g. +0.025 = +2.5%,  -0.012 = -1.2%
     *
     * For BUY positions:  (exitPrice - entryPrice) / entryPrice
     * For SELL positions: (entryPrice - exitPrice) / entryPrice
     */
    public double calculatePnl(double exitPrice) {
        if (entryPrice <= 0) return 0.0;
        return TradeAction.SELL.equals(action)
                ? (entryPrice - exitPrice) / entryPrice
                : (exitPrice  - entryPrice) / entryPrice;
    }

    /**
     * Calculates realised P&L in USDC.
     * amountInRaw is the raw 6-decimal USDC amount.
     */
    public double calculatePnlUsdc(double exitPrice) {
        return positionSizeUsdc * calculatePnl(exitPrice);
    }

    /** Returns how many seconds the position has been open */
    public long getHoldDurationSeconds() {
        long closeEpoch = closedAtEpoch > 0 ? closedAtEpoch : Instant.now().getEpochSecond();
        return closeEpoch - openedAtEpoch;
    }
}