package com.surge.agent.services;

import com.surge.agent.config.ContractConfig;
import com.surge.agent.contracts.IdentityRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractConfig contractConfig;
    private final DefaultGasProvider gasProvider;

    private BigInteger agentId;
    private boolean registered = false;

    public BigInteger registerAgent(String metadataUri) throws Exception {
        if (registered) {
            log.info("Agent already registered with ID: {}", agentId);
            return agentId;
        }

        IdentityRegistry registry = IdentityRegistry.load(
                contractConfig.getIdentity(),
                web3j,
                credentials,
                gasProvider
        );

        var receipt = registry.mintIdentity(metadataUri).send();
        var events = registry.getTransferEvents(receipt);
        if (events.isEmpty()) {
            throw new RuntimeException("No Transfer event found");
        }
        agentId = events.get(0).tokenId;
        registered = true;
        log.info("Agent registered successfully with ID: {}", agentId);
        return agentId;
    }

    public BigInteger getAgentId() {
        return agentId;
    }

    public boolean isRegistered() {
        return registered;
    }
}