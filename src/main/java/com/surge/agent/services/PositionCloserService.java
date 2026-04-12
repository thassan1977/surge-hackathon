package com.surge.agent.services;

import com.surge.agent.contracts.RiskRouter;
import com.surge.agent.dto.TradeRecord;
import com.surge.agent.model.TradeIntent;
import com.surge.agent.services.market.MarketDataService;
import com.surge.agent.utils.EIP712Signer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionCloserService {

    private final RiskRouter riskRouter;
    private final Credentials credentials;
    private final EIP712Signer eip712Signer;
    private final BlockchainService blockchainService;
    private final RiskManagementService riskService;
    private final MarketDataService marketDataService;

    @Value("${ai.pair:ETHUSD}")
    private String PAIR;

    @Value("${router.max.amount.usd.scaled:50000}")
    private BigInteger MAX_AMOUNT_USD_SCALED;

    /**
     * Closes an existing position by submitting a SELL intent.
     * Returns true if successful, false otherwise.
     */
    public boolean closePosition(TradeRecord trade, double exitPrice, String reason) {
        try {
            BigInteger agentId = trade.getAgentId();

            long positionUsdCents = (long) (trade.getPositionSizeUsdc() * 100);
            BigInteger amountUsdScaled = BigInteger.valueOf(positionUsdCents)
                    .min(MAX_AMOUNT_USD_SCALED);

            if (amountUsdScaled.compareTo(BigInteger.valueOf(100)) < 0) {
                log.warn("Close position size too small ({} cents) – minimum $1. Aborting.", amountUsdScaled);
                return false;
            }

            double currentPrice = exitPrice;
            var mkt = marketDataService.getLatestMarketState();
            double atr = (mkt != null) ? mkt.getAtr() : trade.getStopLossPrice() * 0.01;
            BigInteger maxSlippageBps = computeSlippageBps(atr, currentPrice);

            BigInteger nonce = riskRouter.getIntentNonce(agentId).send();

            TradeIntent intent = TradeIntent.builder()
                    .agentId(agentId)
                    .agentWallet(credentials.getAddress())
                    .pair(PAIR)
                    .action("SELL")
                    .amountUsdScaled(amountUsdScaled)
                    .maxSlippageBps(maxSlippageBps)
                    .nonce(nonce)
                    .deadline(BigInteger.valueOf(Instant.now().getEpochSecond() + 600))
                    .build();

            // Optional: simulate (if method exists)
            // if (!simulateAndCheck(intent)) return false;

            byte[] signature = eip712Signer.signTradeIntent(intent);
            TransactionReceipt receipt = blockchainService.executeTrade(intent, signature);

            boolean approved = receipt.getLogs().stream()
                    .anyMatch(l -> l.getTopics().get(0).contains("536c9b7d")); // TradeApproved topic

            if (!approved) {
                log.error("SELL intent for closing trade {} was rejected on‑chain", trade.getTradeId());
                return false;
            }

            trade.setCloseTxHash(receipt.getTransactionHash());
            trade.setClosed(true);
            trade.setClosedAtEpoch(Instant.now().getEpochSecond());

            double pnlPct = trade.calculatePnl(exitPrice);
            double pnlUsdc = trade.calculatePnlUsdc(exitPrice);
            double newBalance = riskService.getCurrentEquityUsdc() + pnlUsdc;
            riskService.recordTradeReturn(pnlPct, newBalance);

            log.info("Position closed on‑chain | tradeId={} | tx={} | PnL={}%",
                    trade.getTradeId(), receipt.getTransactionHash().substring(0, 12), pnlPct * 100);
            return true;

        } catch (Exception e) {
            log.error("Failed to close position {}: {}", trade.getTradeId(), e.getMessage());
            return false;
        }
    }

    private BigInteger computeSlippageBps(double atr, double price) {
        double atrPct = price > 0 ? (atr / price) : 0.003;
        double slippageDecimal = Math.min(0.02, Math.max(0.003, atrPct * 1.5));
        return BigInteger.valueOf((long) (slippageDecimal * 10_000));
    }
}