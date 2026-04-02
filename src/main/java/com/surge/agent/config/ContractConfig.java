package com.surge.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "contract")
public class ContractConfig {
    private String identity;
    private String reputation;
    private String validation;
    private String vault;
    private String router;
}