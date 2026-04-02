package com.surge.agent.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Maps to Python's TradeFeedbackRequest Pydantic model.
 *
 * Python field names (camelCase aliases):
 *   trade_id       (alias: tradeId)
 *   pnl
 *   exit_reason    (alias: exitReason)
 *   agentVerdicts  (alias: agentVerdicts)
 *   marketRegime   (alias: marketRegime)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeFeedbackRequest {

    @JsonProperty("trade_id")
    private String tradeId;

    private double pnl;

    @JsonProperty("exit_reason")
    private String exitReason;

    /** Slim verdict list — only agent_name, signal, conviction needed for attribution. */
    @JsonProperty("agentVerdicts")
    private List<AgentVerdictSlim> agentVerdicts;

    @JsonProperty("marketRegime")
    @Builder.Default
    private String marketRegime = "RANGING";

    /** Minimal verdict payload — avoids sending keyFactors/warnings back to Python. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentVerdictSlim {
        @JsonProperty("agent_name") private String agentName;
        @JsonProperty("signal")     private String signal;
        @JsonProperty("conviction") private double conviction;
    }
}
