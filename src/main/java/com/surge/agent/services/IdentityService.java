package com.surge.agent.services;

import com.surge.agent.config.ContractConfig;
import com.surge.agent.contracts.IdentityRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
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

    @Value("${ai.agent.id:36}")
    private Long storedAgentId;
    private BigInteger agentId;
    private boolean registered = false;

    /**
     * On startup: check if this wallet already has an ERC-8004 Identity.
     */
    @PostConstruct
    public void init() {
        try {
            log.info("Checking on-chain identity for address: {}", credentials.getAddress());
            IdentityRegistry registry = loadRegistry();

            // 1. Check if this wallet address owns any Agent NFTs
            BigInteger balance = registry.balanceOf(credentials.getAddress()).send();

            if (balance != null && balance.compareTo(BigInteger.ZERO) > 0) {
                // 2. Fetch the actual ID linked to this wallet from the blockchain
                //this.agentId = registry.getAgentIdByAddress(credentials.getAddress()).send();
                this.agentId = BigInteger.valueOf(storedAgentId);
                this.registered = true;

                log.info("Identity Verified! Agent ID: {}", agentId);

                // 3. Optional: Cross-check with your application.properties
                // This ensures you didn't accidentally register one ID but are trying to use another
                if (agentId.intValue() != storedAgentId) {
                    log.warn("Warning: Blockchain says ID is {}, but you expected {}", agentId, storedAgentId);
                }
            } else {
                this.agentId = null;
                this.registered = false;
                log.error("No Identity found on-chain for address {}. right private key?", credentials.getAddress());
            }
        } catch (Exception e) {
            log.error("Failed to load on-chain identity: {}", e.getMessage(), e);
        }
    }
    /**
     * Register a new Identity if none exists.
     */
    public BigInteger registerAgent(String metadataUri) throws Exception {
        if (registered && agentId != null) {
            log.info("Agent already registered with ID: {}", agentId);
            return agentId;
        }

        IdentityRegistry registry = loadRegistry();

        log.info("Minting new ERC-8004 Identity with metadata: {}", metadataUri);

        TransactionReceipt receipt = registry.mintIdentity(metadataUri).send();

        if (!receipt.isStatusOK()) {
            throw new RuntimeException("Minting failed: " + receipt.getTransactionHash());
        }

        // Extract tokenId from the Transfer event (this part was already correct)
        var events = registry.getTransferEvents(receipt);
        if (events.isEmpty()) {
            throw new RuntimeException("No Transfer event found after mint");
        }

        this.agentId = events.get(0).tokenId;
        this.registered = true;

        log.info("Successfully registered! Agent ID {} linked to {}", agentId, credentials.getAddress());
        return agentId;
    }

    public BigInteger getAgentId() {
        if (!registered || agentId == null) {
            throw new IllegalStateException("Agent is not registered yet. Call registerAgent() first.");
        }
        return agentId;
    }

    public boolean isRegistered() {
        return registered && agentId != null;
    }

    private IdentityRegistry loadRegistry() {
        return IdentityRegistry.load(
                contractConfig.getIdentity(),
                web3j,
                credentials,
                gasProvider
        );
    }
}