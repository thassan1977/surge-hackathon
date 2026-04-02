package com.surge.agent.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TradeSignal {
    private String asset;
    private BigDecimal price;
    private String action; // "BUY" or "SELL"
    private BigDecimal confidence;
}