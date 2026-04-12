package com.surge.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TradeRecord {

    private String tradeId;

    // BigInteger serialization: Jackson handles it as a number or string
    // We'll use a custom serializer to store as decimal string for readability
    @JsonSerialize(using = BigIntegerSerializer.class)
    @JsonDeserialize(using = BigIntegerDeserializer.class)
    private BigInteger agentId;

    private TradeAction action;
    private String      pair;   // e.g., "ETH/USDC"

    private double entryPrice;
    private double positionSizeEth;   // ETH amount (for inventory tracking)
    private double positionSizeUsdc;  // USD value at entry
    private double takeProfitPrice;
    private double stopLossPrice;

    @Deprecated private String tokenIn;
    @Deprecated private String tokenOut;

    private long openedAtEpoch;
    private long closedAtEpoch;
    private boolean closed;

    private String executionTxHash;
    private String closeTxHash;

    private MarketRegime marketRegime;
    private List<AgentVerdict> agentVerdicts;

    // ─────────────────────────────────────────────────────────────────────
    // FACTORY METHODS
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
                .pair(intent.getPair())
                .entryPrice(currentPrice)
                .positionSizeEth(weiToEth(intent.getAmountUsdScaled())) // if needed
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

    public static TradeRecord fromDecision(String tradeId,
                                           BigInteger agentId,
                                           double currentPrice,
                                           TradeIntent intent,
                                           AITradeDecision decision) {
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
    // LOGIC METHODS
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
                : (exitPrice - entryPrice) / entryPrice;
    }

    public double calculatePnlUsdc(double exitPrice) {
        return positionSizeUsdc * calculatePnl(exitPrice);
    }

    public long getHoldDurationSeconds() {
        long closeEpoch = closedAtEpoch > 0 ? closedAtEpoch : Instant.now().getEpochSecond();
        return closeEpoch - openedAtEpoch;
    }

    // ─────────────────────────────────────────────────────────────────────
    // CUSTOM SERIALIZERS FOR BigInteger (to store as decimal string)
    // ─────────────────────────────────────────────────────────────────────

    public static class BigIntegerSerializer extends com.fasterxml.jackson.databind.JsonSerializer<BigInteger> {
        @Override
        public void serialize(BigInteger value, com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider serializers) throws java.io.IOException {
            gen.writeString(value.toString());
        }
    }

    public static class BigIntegerDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<BigInteger> {
        @Override
        public BigInteger deserialize(com.fasterxml.jackson.core.JsonParser p,
                                      com.fasterxml.jackson.databind.DeserializationContext ctxt) throws java.io.IOException {
            return new BigInteger(p.getText());
        }
    }
}