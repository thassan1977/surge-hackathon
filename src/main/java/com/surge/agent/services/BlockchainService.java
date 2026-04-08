package com.surge.agent.services;

import com.surge.agent.config.ContractConfig;
import com.surge.agent.contracts.MockRiskRouter;
import com.surge.agent.contracts.ValidationRegistry;
import com.surge.agent.model.TradeIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;


@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractConfig contractConfig;
    private final DefaultGasProvider gasProvider;

    @Value("${blockchain.confirmations.required:1}")
    private int requiredConfirmations;

    @Value("${blockchain.polling.interval.ms:2000}")
    private long pollingIntervalMs;

    // Rolling PnL window for Sharpe computation
    private final LinkedList<Double> tradeReturns = new LinkedList<>();
    private static final int SHARPE_WINDOW = 30;

    // ── Trade Execution (WITH CONFIRMATION WAITING) ──────────────────────

    public TransactionReceipt executeTrade(TradeIntent intent, byte[] signature) throws Exception {
        MockRiskRouter router = MockRiskRouter.load(
                contractConfig.getRouter(), web3j, credentials, gasProvider);

        // Convert Java model to Solidity struct
        MockRiskRouter.TradeIntent solIntent = new MockRiskRouter.TradeIntent(
                intent.getAgentId(),
                intent.getTokenIn(),
                intent.getTokenOut(),
                intent.getAmountIn(),
                intent.getMinAmountOut(),
                intent.getDeadline(),
                Numeric.toBytesPadded(new BigInteger(1, intent.getRiskParams()), 32)
        );

        log.info("Broadcasting trade intent to network for Agent {}...", intent.getAgentId());

        // 1. Send the transaction (this blocks until it is MINED into the first block)
        TransactionReceipt receipt = router.executeTrade(solIntent, signature).send();
        String txHash = receipt.getTransactionHash();
        BigInteger txBlockNumber = receipt.getBlockNumber();

        log.info("Trade mined: txHash={} block={}. Waiting for {} confirmations...",
                txHash, txBlockNumber, requiredConfirmations);

        // 2. Wait for N Confirmations (Re-org Protection)
        waitForConfirmations(txHash, txBlockNumber);

        // 3. Verify on-chain success status (0x1 = Success, 0x0 = Revert)
        if (!receipt.isStatusOK()) {
            log.error("Transaction REVERTED on-chain! txHash={}", txHash);
            throw new RuntimeException("Execution reverted on-chain: " + txHash);
        }

        // 4. Parse the TradeExecuted event to get amountOut for PnL
        List<MockRiskRouter.TradeExecutedEventResponse> events =
                router.getTradeExecutedEvents(receipt);

        if (!events.isEmpty()) {
            MockRiskRouter.TradeExecutedEventResponse event = events.get(0);
            double pnlRaw = event.amountOut.subtract(event.amountIn).doubleValue();
            double pnlPct = event.amountIn.longValue() > 0
                    ? pnlRaw / event.amountIn.doubleValue() : 0.0;
            recordTradeReturn(pnlPct);
            log.info("TradeExecuted: amountIn={} amountOut={} pnl={}",
                    event.amountIn, event.amountOut, pnlPct);
        } else {
            log.warn("Trade mined successfully but no TradeExecuted event was emitted. txHash={}", txHash);
        }

        return receipt;
    }

    /**
     * Polls the blockchain to ensure the transaction block is buried under
     * a specific number of subsequent blocks to prevent re-org invalidation.
     */
    private void waitForConfirmations(String txHash, BigInteger txBlockNumber) throws Exception {
        if (requiredConfirmations <= 0) return;

        int currentConfirmations = 0;
        int maxAttempts = 60; // Max polling time (e.g., 60 * 2s = 2 minutes)
        int attempts = 0;

        while (currentConfirmations < requiredConfirmations && attempts < maxAttempts) {
            Thread.sleep(pollingIntervalMs);

            BigInteger latestBlock = web3j.ethBlockNumber().send().getBlockNumber();
            currentConfirmations = latestBlock.subtract(txBlockNumber).intValue();

            attempts++;
            if (attempts % 5 == 0) {
                log.debug("Tx {} confirmations: {}/{}", txHash, Math.max(0, currentConfirmations), requiredConfirmations);
            }
        }

        if (currentConfirmations < requiredConfirmations) {
            log.warn("Timeout waiting for confirmations. Reached {}/{} for tx: {}",
                    currentConfirmations, requiredConfirmations, txHash);
        } else {
            log.info("Transaction {} fully confirmed with {} block(s).", txHash, currentConfirmations);
        }
    }

    // ── Validation & Utility Methods (Preserved) ──────────────────────────

    public void postValidation(BigInteger agentId, byte[] artifactHash, int score) {
        try {
            if (!isAuthorizedValidator(agentId)) {
                log.warn("kipping postValidation: Address {} is NOT yet a registered validator for Agent {}",
                        credentials.getAddress(), agentId);
                return;
            }

            ValidationRegistry validation = ValidationRegistry.load(
                    contractConfig.getValidation(), web3j, credentials, gasProvider);

            var receipt = validation.postValidation(agentId, artifactHash,
                    BigInteger.valueOf(score)).send();

            log.info("Validation anchored: agentId={} score={} tx={}",
                    agentId, score, receipt.getTransactionHash());
        } catch (Exception e) {
            log.error("Validation Error: {}", e.getMessage());
        }
    }

    public void recordTradeReturn(double pnlPct) {
        tradeReturns.addLast(pnlPct);
        if (tradeReturns.size() > SHARPE_WINDOW) tradeReturns.removeFirst();
    }

    public boolean isAuthorizedValidator(BigInteger agentId) {
        if (agentId == null || agentId.equals(BigInteger.ZERO)) return false;
        try {
            ValidationRegistry validation = ValidationRegistry.load(
                    contractConfig.getValidation(), web3j, credentials, gasProvider);
            return validation.validators(agentId, credentials.getAddress()).send();
        } catch (Exception e) {
            log.error("Failed to verify validator status for agent {}: {}", agentId, e.getMessage());
            return false;
        }
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

    public void resetDailyLossIfPossible(BigInteger agentId) {
        try {
            MockRiskRouter router = MockRiskRouter.load(
                    contractConfig.getRouter(), web3j, credentials, gasProvider);
            router.resetDailyLoss(agentId).send();
            log.info("Daily loss reset for agent {}", agentId);
        } catch (Exception e) {
            log.warn("Failed to reset daily loss: {}", e.getMessage());
        }
    }
}