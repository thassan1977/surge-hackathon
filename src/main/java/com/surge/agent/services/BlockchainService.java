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

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractConfig contractConfig;
    private final DefaultGasProvider gasProvider;

    // Rolling PnL window for Sharpe computation
    private final LinkedList<Double> tradeReturns = new LinkedList<>();
    private static final int SHARPE_WINDOW = 30;

    // ── Trade Execution (THE CRITICAL FIX) ───────────────────────────────

    /**
     * FIXED: Actually submits the signed EIP-712 trade intent to MockRiskRouter.
     * Previously this method only logged a message. Now it mines a transaction.
     *
     * The receipt contains the TradeExecuted + RiskCheckPassed events that the
     * hackathon judges use to score on-chain performance.
     */
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

        // ★ THIS IS THE FIX: actually send the transaction ★
        TransactionReceipt receipt = router.executeTrade(solIntent, signature).send();

        log.info("Trade mined: txHash={} block={}",
                receipt.getTransactionHash(), receipt.getBlockNumber());

        // Parse the TradeExecuted event to get amountOut for PnL
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
        }

        return receipt;
    }

    /**
     * Posts a validation score to ValidationRegistry.
     * Called by ValidationService — preserved from V2.
     */
    public void postValidation(BigInteger agentId, byte[] artifactHash, int score) {
        try {
            if (!isAuthorizedValidator(agentId)) {
                log.warn("⏭️ Skipping postValidation: Address {} is NOT yet a registered validator for Agent {}",
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
            // In production, we log this but don't throw it back to the orchestrator
            log.error("Validation Error: {}", e.getMessage());
        }
    }

    // ── Sharpe Ratio ──────────────────────────────────────────────────────

    /**
     * Records a trade return for the rolling Sharpe calculation.
     * Call this after each trade closes.
     */
    public void recordTradeReturn(double pnlPct) {
        tradeReturns.addLast(pnlPct);
        if (tradeReturns.size() > SHARPE_WINDOW) tradeReturns.removeFirst();
    }

    public boolean isAuthorizedValidator(BigInteger agentId) {
        if (agentId == null || agentId.equals(BigInteger.ZERO)) return false;
        try {
            ValidationRegistry validation = ValidationRegistry.load(
                    contractConfig.getValidation(), web3j, credentials, gasProvider);

            // This calls the 'validators' mapping in Solidity contract
            return validation.validators(agentId, credentials.getAddress()).send();
        } catch (Exception e) {
            log.error("Failed to verify validator status for agent {}: {}", agentId, e.getMessage());
            return false;
        }
    }


    /**
     * Computes rolling Sharpe ratio over the last SHARPE_WINDOW trades.
     * Sharpe = Mean(returns) / StdDev(returns) * sqrt(365)
     * Returns 0.0 if fewer than 5 trades recorded.
     */
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
