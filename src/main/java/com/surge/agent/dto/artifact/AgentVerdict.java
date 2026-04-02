package com.surge.agent.dto.artifact;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class AgentVerdict {

    /**
     * Agent identifier: TechnicalAlpha | MacroSentiment | OnChainIntel | RiskAuditor | DevilsAdvocate
     */
    @JsonProperty("agent_name")
    private String agentName;

    /**
     * BUY, SELL, or HOLD
     */
    @JsonProperty("signal")
    private String signal;

    /**
     * 0.0–1.0 — how strongly the agent holds its position
     */
    @JsonProperty("conviction")
    private double conviction;

    /**
     * Top evidence points supporting the signal
     */
    @JsonProperty("key_factors")
    private List<String> keyFactors;

    /**
     * Counter-signals or risks this agent flagged
     */
    @JsonProperty("warnings")
    private List<String> warnings;

    /**
     * 2–3 sentence summary of the agent's full analysis
     */
    @JsonProperty("raw_analysis")
    private String rawAnalysis;

    @JsonProperty("latency_ms")
    private Long latencyMs;

}