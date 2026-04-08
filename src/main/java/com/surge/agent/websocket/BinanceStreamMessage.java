package com.surge.agent.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BinanceStreamMessage(
        @JsonProperty("stream") String stream,
        @JsonProperty("data") JsonNode data
) {}