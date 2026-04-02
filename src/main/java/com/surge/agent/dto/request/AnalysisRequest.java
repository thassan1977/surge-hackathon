package com.surge.agent.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.surge.agent.dto.MarketState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps to Python's AnalysisRequest Pydantic model.
 *
 * Python field aliases (camelCase → snake_case in Python):
 *   market               → market:                MarketState
 *   news                 → news:                  str
 *   currentDrawdownPct   → current_drawdown_pct:  float
 *   openPositionsCount   → open_positions_count:  int
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisRequest {
    private MarketState market;
    private String      news;

    @JsonProperty("currentDrawdownPct")
    @Builder.Default
    private double currentDrawdownPct = 0.0;

    @JsonProperty("openPositionsCount")
    @Builder.Default
    private int openPositionsCount = 0;


    @JsonProperty("balanceUsdc")
    private double balanceUsdc;

}
