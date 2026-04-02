package com.surge.agent.model;

import lombok.Data;
import java.math.BigInteger;

@Data
public class AgentIdentity {
    private BigInteger agentId;
    private String metadataUri;
    private boolean registered;
}