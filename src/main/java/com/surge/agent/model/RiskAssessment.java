package com.surge.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiskAssessment {

    @JsonProperty("score")
    private int score;          // 0–100 risk score from Python risk engine

    @JsonProperty("approved")
    private boolean approved;   // true if all Python-side risk guards passed

    @JsonProperty("reason")
    private String reason;      // human-readable explanation if rejected

    @JsonProperty("max_position_size_eth")
    private Double maxPositionSizeEth; // Python's suggested max size (optional)
}