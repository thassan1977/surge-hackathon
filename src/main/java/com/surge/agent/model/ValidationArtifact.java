package com.surge.agent.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ValidationArtifact {
    private Instant timestamp;
    private String type; // "trade_intent", "risk_check", "checkpoint"
    private BigInteger agentId;
    private TradeIntent trade;
    private RiskAssessment riskMetrics;
    private Map<String, Object> modelInfo;
    private double[] shapValues;
    private String[] topFeatures;
}