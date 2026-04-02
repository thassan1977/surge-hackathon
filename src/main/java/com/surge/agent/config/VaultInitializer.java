package com.surge.agent.config;

import com.surge.agent.contracts.MockVault;
import com.surge.agent.contracts.ReputationRegistry;
import com.surge.agent.contracts.ValidationRegistry;
import com.surge.agent.services.IdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class VaultInitializer implements CommandLineRunner {

    private final MockVault           mockVault;
    private final Web3j               web3j;
    private final Credentials         credentials;
    private final ContractGasProvider gasProvider;
    private final IdentityService identityService;
    private final ContractConfig contractConfig;

    @Value("${contract.usdc}")
    private String usdcAddress;

    @Value("${ai.agent.metadata.uri:https://surge.agent/metadata/v3}")
    private String metadataUri;

    @Value("${ai.agent.private-key:}")
    private String validatorPrivateKey;

    @Override
    public void run(String... args) {
        try {
            log.info("--- Vault Initialization Started ---");
            log.info("Agent Wallet: {}", credentials.getAddress());
            log.info("Vault Address: {}", mockVault.getContractAddress());
            log.info("USDC Address:  {}", usdcAddress);

            // STEP 0: register agent first — gives us the real agentId
            if (!identityService.isRegistered()) {
                identityService.registerAgent(metadataUri);
            }

            // THE FIX — use real agentId (e.g. 42), not BigInteger.ZERO
            BigInteger agentId    = identityService.getAgentId();
            BigInteger seedAmount = new BigInteger("1000000000"); // 1000 USDC

            log.info("Agent ID: {}", agentId);
            log.info("Detected Stablecoin Address: {}", credentials.getAddress());
            mintUsdcManually(credentials.getAddress(), seedAmount);

            // STEP 1: check THIS agent's balance — not balances[0]
            BigInteger currentBalance = mockVault.balances(agentId).send();
            if (currentBalance.compareTo(BigInteger.ZERO) > 0) {
                log.info("Agent {} already funded. Balance: {} USDC.",
                        agentId, formatUsdc(currentBalance));
                // Still register validator in case it wasn't done on a previous run
                registerValidator(agentId);
                return;
            }

            // STEP 2: approve vault to pull USDC from wallet
            log.info("Step 1: Approving Vault to spend {} USDC for agent {}...",
                    formatUsdc(seedAmount), agentId);

            Function approveFunc = new Function(
                    "approve",
                    Arrays.asList(new Address(mockVault.getContractAddress()), new Uint256(seedAmount)),
                    Collections.emptyList()
            );

            TransactionManager txManager = new RawTransactionManager(web3j, credentials);
            EthSendTransaction approvalTx = txManager.sendTransaction(
                    gasProvider.getGasPrice("approve"),
                    gasProvider.getGasLimit("approve"),
                    usdcAddress,
                    FunctionEncoder.encode(approveFunc),
                    BigInteger.ZERO
            );

            if (approvalTx.hasError()) {
                log.error("Approval failed: {}", approvalTx.getError().getMessage());
                return;
            }

            TransactionReceipt approveReceipt = new PollingTransactionReceiptProcessor(web3j, 1000, 15)
                    .waitForTransactionReceipt(approvalTx.getTransactionHash());

            if (!approveReceipt.isStatusOK()) {
                log.error("Approval reverted on-chain.");
                return;
            }
            log.info("Step 1 Complete: Approval confirmed in block {}", approveReceipt.getBlockNumber());

            // STEP 3: deposit into agent's specific slot
            log.info("Step 2: Depositing {} USDC into Vault for Agent {}...",
                    formatUsdc(seedAmount), agentId);

            TransactionReceipt depositReceipt = mockVault.deposit(agentId, seedAmount).send();

            if (depositReceipt.isStatusOK()) {
                BigInteger newBalance = mockVault.balances(agentId).send();
                log.info("Agent {} funded. Balance: {} USDC", agentId, formatUsdc(newBalance));

                // ── STEP 4: register wallet as validator for this agent ────────
                // ReputationRegistry.recordValidatorScore() checks validators[agentId][msg.sender].
                // Without this call every recordValidatorScore() reverts with
                // "Not a validator for this agent".
                // The main wallet self-registers as validator — sufficient for the hackathon.
                registerValidator(agentId);

                log.info("--- Vault Initialized Successfully for Agent {} ---", agentId);
            } else {
                log.error("Deposit reverted. Does wallet {} have {} USDC?",
                        credentials.getAddress(), formatUsdc(seedAmount));
            }

        } catch (Exception e) {
            log.error("VAULT INITIALIZER ERROR: {}", e.getMessage(), e);
        }
    }

    /**
     * Ensures the main wallet actually owns the USDC tokens before trying to deposit them.
     */
    private void mintUsdcManually(String to, BigInteger amount) throws Exception {
        log.info("Attempting manual mint of {} USDC to {}", amount, to);

        // 1. Define the 'mint(address,uint256)' function
        Function function = new Function(
                "mint",
                Arrays.asList(new Address(to), new Uint256(amount)),
                Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);

        // 2. Send the transaction
        TransactionManager txManager = new RawTransactionManager(web3j, credentials);
        EthSendTransaction response = txManager.sendTransaction(
                gasProvider.getGasPrice("mint"),
                gasProvider.getGasLimit("mint"),
                usdcAddress,
                encodedFunction,
                BigInteger.ZERO
        );

        if (response.hasError()) {
            throw new RuntimeException("Mint failed: " + response.getError().getMessage());
        }

        // 3. Wait for confirmation
        TransactionReceipt receipt = new PollingTransactionReceiptProcessor(web3j, 1000, 15)
                .waitForTransactionReceipt(response.getTransactionHash());

        if (receipt.isStatusOK()) {
            log.info("Manual mint confirmed in block {}", receipt.getBlockNumber());
        }
    }


    private void registerValidator(BigInteger agentId) {
        try {
            ReputationRegistry rep = ReputationRegistry.load(
                    contractConfig.getReputation(), web3j, credentials, gasProvider);

            // Register BOTH wallets — main wallet AND validator private key wallet
            registerAddress(rep, agentId, credentials.getAddress());

            // Also register the validator wallet if configured
            if (validatorPrivateKey != null && !validatorPrivateKey.isBlank()) {
                String validatorAddress = Credentials.create(validatorPrivateKey).getAddress();
                registerAddress(rep, agentId, validatorAddress);
            }

            try {
                ValidationRegistry val = ValidationRegistry.load(
                        contractConfig.getValidation(), web3j, credentials, gasProvider);
                // Check the method name in your ValidationRegistry.java wrapper
                // It's likely addValidator or registerValidator
                val.addValidator(agentId, credentials.getAddress()).send();
                log.info("Registered as validator on ValidationRegistry for agent {}", agentId);
            } catch (Exception e) {
                log.warn("ValidationRegistry.addValidator failed: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("registerValidator failed: {}", e.getMessage());
        }
    }

    private void registerAddress(ReputationRegistry rep, BigInteger agentId, String address) throws Exception {
        Boolean already = rep.validators(agentId, address).send();
        if (Boolean.TRUE.equals(already)) {
            log.info("Already validator: agent={} address={}", agentId, address);
            return;
        }
        TransactionReceipt r = rep.addValidator(agentId, address).send();
        if (r.isStatusOK()) log.info("Registered validator: agent={} address={}", agentId, address);
        else log.error("addValidator reverted for address={}", address);
    }
    private String formatUsdc(BigInteger amount) {
        return new BigDecimal(amount).movePointLeft(6).toPlainString();
    }
}