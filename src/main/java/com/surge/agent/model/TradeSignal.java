package com.surge.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Enhanced TradeSignal for V3.
 * Bridges the gap between the Price Listener and the TradeService.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeSignal {

    // e.g., "ETH", "BTC", "USDC"
    private String asset;

    // The price at the moment the signal was generated
    private BigDecimal price;

    // "BUY", "SELL", or "HOLD"
    private String action;

    // 0.0 to 1.0 - used by RiskManagementService for Kelly Sizing
    private BigDecimal confidence;

    // Optional: The symbol used for contract lookups (e.g., "WETH")
    private String tokenSymbol;

    // ── NEW ENHANCEMENTS ─────────────────────────────────────────────────

    // Used for the EIP-712 deadline and TTL checks
    @Builder.Default
    private long timestamp = Instant.now().getEpochSecond();

    // If your price provider gives spread, include it here
    // This allows RiskManagementService to veto "TP < Spread" scenarios immediately
    private double currentSpread;

    /**
     * Helper to get symbol for TradeService compatibility.
     */
    public String getTokenSymbol() {
        return tokenSymbol != null ? tokenSymbol : asset;
    }
}