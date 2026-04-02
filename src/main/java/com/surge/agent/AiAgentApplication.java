package com.surge.agent;

import com.surge.agent.services.IdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
@EnableConfigurationProperties
@EnableScheduling
public class AiAgentApplication implements CommandLineRunner {
    private final IdentityService identityService;

    public static void main(String[] args) {
        SpringApplication.run(AiAgentApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Register agent on startup with a default metadata URI
        if (!identityService.isRegistered()) {
            String metadataUri = "ipfs://QmYourAgentMetadata";
            identityService.registerAgent(metadataUri);
        }
    }
}
