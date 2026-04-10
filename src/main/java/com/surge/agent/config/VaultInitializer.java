package com.surge.agent.config;

import com.surge.agent.contracts.HackathonVault;
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
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

/**
 * VaultInitializer — Sepolia Shared Contracts Edition
 *
 * What changed from the Hardhat version:
 *   - mintUsdcManually() REMOVED: Circle USDC has a minter-role guard.
 *     Calling mint() from a non-minter reverts. Get USDC from faucet.circle.com.
 *   - MockVault REPLACED with direct raw transactions to HackathonVault.
 *     MockVault ABI does not match the shared contract.
 *   - seedAmount changed from 1000 USDC to 18 USDC (we have 20, keep 2 for buffer).
 *   - Vault approve target is now the real HackathonVault address from config.
 *   - RiskRouter USDC approval ADDED: executeBuy() pulls USDC via the router,
 *     which also needs an allowance or every buy will revert.
 *   - Validator registration kept exactly as before (was correct).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VaultInitializer implements CommandLineRunner {

    private final Web3j             web3j;
    private final Credentials       credentials;
    private final ContractGasProvider gasProvider;
    private final IdentityService   identityService;
    private final ContractConfig    contractConfig;

    @Value("${contract.usdc}")
    private String usdcAddress;

    @Value("${contract.vault}")
    private String vaultAddress;

    @Value("${contract.router}")
    private String routerAddress;

    @Value("${ai.agent.metadata.uri:ipfs://QmDefault}")
    private String metadataUri;

    @Value("${ai.agent.private-key:}")
    private String validatorPrivateKey;

    // 18 USDC in raw units (6 decimals).
    // We have 20 USDC from the faucet — keep 2 as wallet buffer.
    private static final BigInteger DEPOSIT_AMOUNT = new BigInteger("18000000");

    // Max uint256 — approve once, never need to re-approve
    private static final BigInteger MAX_APPROVAL =
            BigInteger.TWO.pow(256).subtract(BigInteger.ONE);

    @Override
    public void run(String... args) {
//        try {
//            log.info("=== Vault Initialization (Sepolia) ===");
//            log.info("Agent Wallet : {}", credentials.getAddress());
//            log.info("Vault Address: {}", vaultAddress);
//            log.info("USDC Address : {}", usdcAddress);
//            log.info("Router       : {}", routerAddress);
//
//            // ── STEP 0: Register agent identity ──────────────────────────────
//            if (!identityService.isRegistered()) {
//                log.info("Registering agent identity...");
//                identityService.registerAgent(metadataUri);
//            }
//            BigInteger agentId = identityService.getAgentId();
//            log.info("Agent ID: {}", agentId);
//
//            // ── STEP 1: Approve HackathonVault to spend USDC ─────────────────
//            // The vault pulls USDC from our wallet during deposit().
//            // Without this approve the deposit reverts.
//            log.info("Step 1: Approving HackathonVault to spend USDC...");
//            boolean vaultApproved = approveSpender(vaultAddress, MAX_APPROVAL, "HackathonVault");
//            if (!vaultApproved) {
//                log.error("Vault approval failed — aborting initialisation.");
//                return;
//            }
//
//            // ── STEP 2: Deposit USDC into HackathonVault for this agent ──────
//            // HackathonVault.deposit(agentId, amount) records the balance
//            // against our specific agentId on-chain.
//            log.info("Step 2: Depositing {} USDC into HackathonVault for agent {}...",
//                    formatUsdc(DEPOSIT_AMOUNT), agentId);
//            boolean deposited = depositIntoVault(DEPOSIT_AMOUNT);
//            if (!deposited) {
//                log.warn("Vault deposit failed or already funded — continuing.");
//                // Not fatal: if already deposited on a previous run, we continue.
//            }
//
//            // ── STEP 3: Approve RiskRouter to spend remaining wallet USDC ────
//            // executeBuy() submits a TradeIntent through the RiskRouter.
//            // The router pulls USDC directly from our wallet during swap.
//            // This approval is SEPARATE from the vault approval above.
//            log.info("Step 3: Approving RiskRouter to spend USDC...");
//            approveSpender(routerAddress, MAX_APPROVAL, "RiskRouter");
//
//
//            log.info("=== Vault Initialization Complete (agent={}) ===", agentId);
//
//        } catch (Exception e) {
//            log.error("VAULT INITIALIZER ERROR: {}", e.getMessage(), e);
//        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ERC-20 approve — generic, works for both vault and router
    // ─────────────────────────────────────────────────────────────────────────

    private boolean approveSpender(String spender, BigInteger amount, String label) {
        try {
            Function approveFunc = new Function(
                    "approve",
                    Arrays.asList(new Address(spender), new Uint256(amount)),
                    Collections.emptyList()
            );
            TransactionManager txManager = new RawTransactionManager(web3j, credentials);
            EthSendTransaction tx = txManager.sendTransaction(
                    gasProvider.getGasPrice("approve"),
                    gasProvider.getGasLimit("approve"),
                    usdcAddress,
                    FunctionEncoder.encode(approveFunc),
                    BigInteger.ZERO
            );
            if (tx.hasError()) {
                log.error("approve({}) send error: {}", label, tx.getError().getMessage());
                return false;
            }
            TransactionReceipt receipt = new PollingTransactionReceiptProcessor(web3j, 1000, 15)
                    .waitForTransactionReceipt(tx.getTransactionHash());

            if (receipt.isStatusOK()) {
                log.info("approve({}) confirmed. tx={}", label,
                        tx.getTransactionHash().substring(0, 14) + "...");
                return true;
            } else {
                log.error("approve({}) reverted on-chain.", label);
                return false;
            }
        } catch (Exception e) {
            log.error("approve({}) exception: {}", label, e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HackathonVault.deposit(agentId, amount)
    // Using raw transaction — avoids MockVault ABI mismatch
    // ─────────────────────────────────────────────────────────────────────────

    private boolean depositIntoVault(BigInteger amountInWei) {
        try {
            // 1. Load the contract using the wrapper
            HackathonVault vault = HackathonVault.load(
                    vaultAddress,
                    web3j,
                    credentials,
                    new DefaultGasProvider() // Or your custom gas provider
            );

            // 2. Call the deposit function
            // Note: The parameter here is the amount of ETH/Wei you are sending TO the contract.
            TransactionReceipt receipt = vault.deposit(amountInWei).send();

            if (receipt.isStatusOK()) {
                log.info("Vault deposit confirmed. Tx: {}", receipt.getTransactionHash());
                return true;
            } else {
                log.error("Vault deposit reverted.");
                return false;
            }
        } catch (Exception e) {
            log.error("Deposit failed: {}", e.getMessage());
            return false;
        }
    }

    private String formatUsdc(BigInteger amount) {
        return new BigDecimal(amount).movePointLeft(6).toPlainString();
    }
}