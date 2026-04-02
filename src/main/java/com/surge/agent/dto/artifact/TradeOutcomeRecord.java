package com.surge.agent.dto.artifact;

import lombok.Builder;
import lombok.Data;

/**
 * Passed from TradeMonitorService → ValidationService when a trade closes.
 * Contains everything needed to populate the artifact outcome block and re-score.
 */
@Data
@Builder
public class TradeOutcomeRecord {

    /** Must match the tradeId used when the artifact was first posted */
    private String tradeId;

    /** entry price in USD */
    private double entryPriceUsd;

    /** Exit price in USD */
    private double exitPriceUsd;

    /** Realised P&L as a decimal fraction (+0.025 = +2.5%, -0.012 = -1.2%) */
    private double realisedPnlPct;

    /** Realised P&L in USDC (absolute dollar amount) */
    private double realisedPnlUsdc;

    /** "TP" | "SL" | "REVERT" */
    private String exitReason;

    /** How long the position was open in seconds */
    private long holdDurationSeconds;

    /** Closing transaction hash (null if not available) */
    private String closeTxHash;
}
