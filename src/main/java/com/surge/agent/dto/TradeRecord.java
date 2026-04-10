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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeRecord {

    private String     tradeId;
    private BigInteger agentId;

    // ── Updated Trade parameters for Live Hackathon ──────────────────────

    private TradeAction action;
    private String      pair;   // Added: e.g., "ETH/USDC"

    private double entryPrice;
    private double positionSizeEth;  // Used for tracking on-chain inventory
    private double positionSizeUsdc; // Real dollar value for PnL
    private double takeProfitPrice;
    private double stopLossPrice;

    // tokenIn/tokenOut are now deprecated in favor of 'pair'
    @Deprecated private String tokenIn;
    @Deprecated private String tokenOut;

    // ── Timing ───────────────────────────────────────────────────────────
    private long openedAtEpoch;
    private long closedAtEpoch;

    // ── State ────────────────────────────────────────────────────────────
    private boolean closed;

    // ── On-chain linkage ─────────────────────────────────────────────────
    private String executionTxHash;
    private String closeTxHash;

    // ── AI context ───────────────────────────────────────────────────────
    private MarketRegime marketRegime;
    private List<AgentVerdict> agentVerdicts;

    // ─────────────────────────────────────────────────────────────────────
    // UPDATED FACTORY
    // ─────────────────────────────────────────────────────────────────────

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
                .pair(intent.getPair()) // Logic updated to use Pair
                .entryPrice(currentPrice)
                // Use the new intent field: amountUsdScaled (18 decimals)
                .positionSizeEth(weiToEth(intent.getAmountUsdScaled()))
                .positionSizeUsdc(positionSizeUsdc)
                .takeProfitPrice(currentPrice * decision.getTakeProfitMultiplier())
                .stopLossPrice(currentPrice * decision.getStopLossMultiplier())
                .openedAtEpoch(Instant.now().getEpochSecond())
                .closedAtEpoch(0L)
                .closed(false)
                .marketRegime(decision.getMarketRegime())
                .agentVerdicts(decision.getAgentVerdicts())
                .build();
    }

    /** * FIX: Updated scaling from 1e6 to 1e18 for the live hackathon.
     */
    public static TradeRecord fromDecision(String tradeId,
                                           BigInteger agentId,
                                           double currentPrice,
                                           TradeIntent intent,
                                           AITradeDecision decision) {
        // Updated scaling to 1e18 for amountUsdScaled
        double usdc = intent.getAmountUsdScaled() != null
                ? intent.getAmountUsdScaled().doubleValue() / 1e18 : 0.0;
        return fromDecision(tradeId, agentId, currentPrice, usdc, intent, decision);
    }

    private static double weiToEth(BigInteger wei) {
        if (wei == null) return 0;
        return new BigDecimal(wei)
                .divide(new BigDecimal("1000000000000000000"), 6, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // ─────────────────────────────────────────────────────────────────────
    // LOGIC METHODS (Unchanged as they rely on doubles)
    // ─────────────────────────────────────────────────────────────────────

    public boolean isAtTakeProfit(double currentPrice) {
        return TradeAction.BUY.equals(action)
                ? currentPrice >= takeProfitPrice
                : currentPrice <= takeProfitPrice;
    }

    public boolean isAtStopLoss(double currentPrice) {
        return TradeAction.BUY.equals(action)
                ? currentPrice <= stopLossPrice
                : currentPrice >= stopLossPrice;
    }

    public double calculatePnl(double exitPrice) {
        if (entryPrice <= 0) return 0.0;
        return TradeAction.SELL.equals(action)
                ? (entryPrice - exitPrice) / entryPrice
                : (exitPrice  - entryPrice) / entryPrice;
    }

    public double calculatePnlUsdc(double exitPrice) {
        return positionSizeUsdc * calculatePnl(exitPrice);
    }

    public long getHoldDurationSeconds() {
        long closeEpoch = closedAtEpoch > 0 ? closedAtEpoch : Instant.now().getEpochSecond();
        return closeEpoch - openedAtEpoch;
    }
}