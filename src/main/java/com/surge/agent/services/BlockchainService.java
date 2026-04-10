package com.surge.agent.services;

import com.surge.agent.config.ContractConfig;
import com.surge.agent.model.TradeIntent;
import com.surge.agent.contracts.RiskRouter;
import com.surge.agent.contracts.ValidationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractConfig contractConfig;
    private final DefaultGasProvider gasProvider;
    private final RiskRouter riskRouter;
    private final IdentityService identityService;

    @Value("${blockchain.confirmations.required:1}")
    private int requiredConfirmations;

    @Value("${blockchain.polling.interval.ms:3000}")
    private long pollingIntervalMs;

    private final LinkedList<Double> tradeReturns = new LinkedList<>();
    private static final int SHARPE_WINDOW = 30;

    /**
     * Submits a Trade Intent to the Live RiskRouter.
     * Note: This environment uses "Shadow Trading" intents, not real token swaps.
     */
    public TransactionReceipt submitTradeIntent(TradeIntent intent, byte[] signature) throws Exception {
        // 1. Load the REAL RiskRouter wrapper
        RiskRouter router = RiskRouter.load(
                contractConfig.getRouter(), web3j, credentials, gasProvider);

        // 2. Map Java model to the Solidity Struct expected by the Live ABI
        RiskRouter.TradeIntent solIntent = new RiskRouter.TradeIntent(
                intent.getAgentId(),
                intent.getAgentWallet(),
                intent.getPair(),
                intent.getAction(),
                intent.getAmountUsdScaled(),
                intent.getMaxSlippageBps(),
                intent.getNonce(),
                intent.getDeadline()
        );

        log.info("Broadcasting {} intent for {} to RiskRouter...", intent.getAction(), intent.getPair());

        // 3. Submit to Blockchain
        TransactionReceipt receipt = router.submitTradeIntent(solIntent, signature).send();

        if (!receipt.isStatusOK()) {
            throw new RuntimeException("TradeIntent submission REVERTED: " + receipt.getTransactionHash());
        }

        log.info("Intent mined. TX: {}. Waiting for confirmations...", receipt.getTransactionHash());
        waitForConfirmations(receipt.getTransactionHash(), receipt.getBlockNumber());

        // 4. Parse events (Look for TradeIntentSubmitted or TradeApproved)
        List<RiskRouter.TradeIntentSubmittedEventResponse> events = router.getTradeIntentSubmittedEvents(receipt);
        if (!events.isEmpty()) {
            log.info("✅ Intent successfully registered on-chain for Agent {}", intent.getAgentId());
        }

        return receipt;
    }

    /**
     * Updated for live ValidationRegistry: postEIP712Attestation
     */
    public void postValidation(BigInteger agentId, byte[] checkpointHash, int score, String reasoning) {
        try {
            // 1. Load the NEWLY generated ValidationRegistry
            ValidationRegistry validation = ValidationRegistry.load(
                    contractConfig.getValidation(), web3j, credentials, gasProvider);

            log.info("Posting attestation for Agent {} with score {}...", agentId, score);

            // 2. Call the EIP-712 method found in the hackathon ABI
            // Parameters: uint256, bytes32, uint256, string
            TransactionReceipt receipt = validation.postEIP712Attestation(
                    agentId,
                    checkpointHash,
                    BigInteger.valueOf(score),
                    reasoning
            ).send();

            // 3. getTransactionHash() is a standard method on TransactionReceipt
            if (receipt.isStatusOK()) {
                log.info("Attestation anchored successfully!");
                log.info("Agent: {} | Score: {} | TX: {}",
                        agentId, score, receipt.getTransactionHash());
            } else {
                log.error("Attestation transaction failed on-chain: {}", receipt.getTransactionHash());
            }

        } catch (Exception e) {
            log.error("Attestation Error: {}", e.getMessage());
            // If you get "Method not found", ensure your java-core was actually refreshed
        }
    }

    private void waitForConfirmations(String txHash, BigInteger txBlockNumber) throws Exception {
        if (requiredConfirmations <= 0) return;
        int currentConfirmations = 0;
        while (currentConfirmations < requiredConfirmations) {
            Thread.sleep(pollingIntervalMs);
            BigInteger latestBlock = web3j.ethBlockNumber().send().getBlockNumber();
            currentConfirmations = latestBlock.subtract(txBlockNumber).intValue();
        }
        log.info("TX {} confirmed ({} blocks).", txHash, currentConfirmations);
    }

    // PnL and Sharpe methods remain for local tracking
    public void recordTradeReturn(double pnlPct) {
        tradeReturns.addLast(pnlPct);
        if (tradeReturns.size() > SHARPE_WINDOW) tradeReturns.removeFirst();
    }


    public TransactionReceipt executeTrade(TradeIntent intent, byte[] signature) throws Exception {
        log.info("Submitting Trade Intent for {} {}...", intent.getAction(), intent.getPair());

        // 1. Map your internal TradeIntent to the Solidity-generated DynamicStruct
        // The order must match your RiskRouter.TradeIntent constructor exactly
        RiskRouter.TradeIntent solIntent = new RiskRouter.TradeIntent(
                intent.getAgentId(),
                identityService.getAgentWallet(), // Ensure this returns the String address
                intent.getPair(),
                intent.getAction(),
                intent.getAmountUsdScaled(),
                intent.getMaxSlippageBps(),
                intent.getNonce(),
                intent.getDeadline()
        );

        // 2. Call the correct method: submitTradeIntent
        // Signature must be wrapped in DynamicBytes as per the generated code
        return riskRouter.submitTradeIntent(solIntent, signature).send();
    }


    public double getSharpeRatio() {
        if (tradeReturns.size() < 5) return 0.0;
        double mean = tradeReturns.stream().mapToDouble(x -> x).average().orElse(0);
        double variance = tradeReturns.stream()
                .mapToDouble(x -> Math.pow(x - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        return stdDev > 0 ? (mean / stdDev) * Math.sqrt(365) : 0.0;
    }

    public int getTradeCount() { return tradeReturns.size(); }

    public double getCumulativePnl() {
        return tradeReturns.stream().mapToDouble(x -> x).sum();
    }

//    public void resetDailyLossIfPossible(BigInteger agentId) {
//        try {
//            RiskRouter router = RiskRouter.load(
//                    contractConfig.getRouter(), web3j, credentials, gasProvider);
//            router.resetDailyLoss(agentId).send();
//            log.info("Daily loss reset for agent {}", agentId);
//        } catch (Exception e) {
//            log.warn("Failed to reset daily loss: {}", e.getMessage());
//        }
//    }

    public String ethCall(String contractAddress, String encodedFunction) throws Exception {
        org.web3j.protocol.core.methods.request.Transaction transaction =
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                        credentials.getAddress(), contractAddress, encodedFunction
                );
        return web3j.ethCall(transaction, DefaultBlockParameterName.LATEST)
                .send().getValue();
    }

}