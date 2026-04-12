package com.surge.agent.services;

import com.surge.agent.dto.TradeRecord;
import com.surge.agent.dto.artifact.TradeOutcomeRecord;
import com.surge.agent.dto.request.TradeFeedbackRequest;
import com.surge.agent.enums.TradeAction;
import com.surge.agent.services.market.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * TradeMonitorService — V4 with Redis persistence + on-chain close.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeMonitorService {

    private final MarketDataService marketDataService;
    private final ValidationService validationService;
    private final PythonAIClient pythonAIClient;
    private final RiskManagementService riskService;
    private final OpenPositionStore openPositionStore;          // NEW: Redis store
    private final PositionCloserService positionCloserService;   // NEW: to close on-chain

    /** tradeId → open TradeRecord (in‑memory cache) */
    private final ConcurrentHashMap<String, TradeRecord> openTrades = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────
    // INIT: Load from Redis on startup
    // ─────────────────────────────────────────────────────────────────────

    @PostConstruct
    public void init() {
        List<TradeRecord> saved = openPositionStore.loadAll();
        for (TradeRecord record : saved) {
            if (!record.isClosed()) {
                openTrades.put(record.getTradeId(), record);
                log.info("Restored open trade {} from Redis", record.getTradeId());
            } else {
                // Should not happen, but clean up
                openPositionStore.delete(record.getTradeId());
            }
        }
        log.info("TradeMonitorService initialised. {} open positions loaded.", openTrades.size());
    }

    // ─────────────────────────────────────────────────────────────────────
    // REGISTRATION (persist to Redis)
    // ─────────────────────────────────────────────────────────────────────

    public void register(TradeRecord record) {
        openTrades.put(record.getTradeId(), record);
        openPositionStore.save(record.getTradeId(), record);
        log.info("Position registered | tradeId={} action={} entry={} TP={} SL={} size=${}",
                record.getTradeId(), record.getAction(),
                record.getEntryPrice(), record.getTakeProfitPrice(), record.getStopLossPrice(),
                record.getPositionSizeUsdc());
    }

    public Collection<TradeRecord> getOpenTrades() {
        return List.copyOf(openTrades.values());
    }

    public int getOpenCount() {
        return openTrades.size();
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRICE MONITOR (every 500ms)
    // ─────────────────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 500)
    public void monitorPositions() {
        if (openTrades.isEmpty()) return;

        double currentPrice = getCurrentPrice();
        if (currentPrice <= 0) {
            log.debug("Price unavailable — skipping monitor tick.");
            return;
        }

        new ArrayList<>(openTrades.keySet()).forEach(tradeId -> {
            TradeRecord trade = openTrades.get(tradeId);
            if (trade != null && !trade.isClosed()) {
                checkTriggers(trade, currentPrice);
            }
        });
    }

    private void checkTriggers(TradeRecord trade, double price) {
        boolean tpHit, slHit;

        if (TradeAction.BUY.equals(trade.getAction())) {
            tpHit = price >= trade.getTakeProfitPrice();
            slHit = price <= trade.getStopLossPrice();
        } else { // SELL (short)
            tpHit = price <= trade.getTakeProfitPrice();
            slHit = price >= trade.getStopLossPrice();
        }

        if (tpHit) {
            closeTrade(trade, trade.getTakeProfitPrice(), "TP");
        } else if (slHit) {
            closeTrade(trade, trade.getStopLossPrice(), "SL");
        } else {
            long maxHoldSeconds = 1800; // 1/2 hour
            if (Instant.now().getEpochSecond() - trade.getOpenedAtEpoch() > maxHoldSeconds) {
                closeTrade(trade, getCurrentPrice(), "TIMEOUT");
            }
        }

    }

    // ─────────────────────────────────────────────────────────────────────
    // CLOSE TRADE (with on‑chain SELL)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Closes a trade: removes from local cache + Redis, submits SELL intent on‑chain,
     * then updates local stats, validation, and Python feedback.
     */
    public void closeTrade(TradeRecord trade, double exitPrice, String reason) {
        // 1. Remove from local map and Redis immediately to prevent double‑close
        TradeRecord removed = openTrades.remove(trade.getTradeId());
        if (removed == null) return; // already closed
        openPositionStore.delete(trade.getTradeId());

        // 2. Submit SELL intent on‑chain (critical for leaderboard)
        boolean onChainClosed = false;
        try {
            positionCloserService.closePosition(trade, exitPrice, reason);
            onChainClosed = true;
            log.info("On-chain SELL submitted for trade {} at price {} (reason: {})",
                    trade.getTradeId(), exitPrice, reason);
        } catch (Exception e) {
            log.error("On-chain close failed for trade {}: {}", trade.getTradeId(), e.getMessage());
            // Re‑add to cache and Redis so we retry later
            openTrades.put(trade.getTradeId(), trade);
            openPositionStore.save(trade.getTradeId(), trade);
            return;
        }

        // 3. Mark as closed and compute outcome
        trade.setClosed(true);
        trade.setClosedAtEpoch(Instant.now().getEpochSecond());

        double pnlPct = trade.calculatePnl(exitPrice);
        double pnlUsdc = trade.calculatePnlUsdc(exitPrice);
        long holdSec = trade.getHoldDurationSeconds();
        double updatedBalance = riskService.getCurrentEquityUsdc() + pnlUsdc;

        TradeOutcomeRecord outcome = TradeOutcomeRecord.builder()
                .tradeId(trade.getTradeId())
                .exitReason(reason)
                .entryPriceUsd(trade.getEntryPrice())
                .exitPriceUsd(exitPrice)
                .realisedPnlPct(pnlPct)
                .realisedPnlUsdc(pnlUsdc)
                .holdDurationSeconds(holdSec)
                .closeTxHash(null) // We don't have the SELL tx hash here; could be passed from orchestrator
                .build();

        log.info("Position closed | tradeId={} reason={} entry={} exit={} pnl={}% hold={}s",
                trade.getTradeId(), reason, trade.getEntryPrice(), exitPrice,
                String.format("%.2f", pnlPct * 100), holdSec);

        // 4. Post final validation artifact (with outcome)
        try {
            validationService.finaliseOutcome(outcome, trade);
        } catch (Exception e) {
            log.error("finaliseOutcome failed for tradeId={}: {}", trade.getTradeId(), e.getMessage());
        }

        // 5. Send feedback to Python for agent weight update
        try {
            pythonAIClient.sendTradeFeedback(buildFeedbackRequest(trade, outcome));
        } catch (Exception e) {
            log.warn("sendTradeFeedback failed (non-fatal) for tradeId={}: {}",
                    trade.getTradeId(), e.getMessage());
        }

        // 6. Update local rolling risk metrics (Sharpe, win rate, etc.)
        try {
            riskService.recordTradeReturn(pnlPct, updatedBalance);
        } catch (Exception e) {
            log.warn("recordTradeReturn failed for tradeId={}: {}", trade.getTradeId(), e.getMessage());
        }
    }

    /**
     * Force‑close all open positions (circuit breaker or shutdown).
     */
    public void closeAllPositions() {
        if (openTrades.isEmpty()) return;
        double currentPrice = getCurrentPrice();
        log.warn("Force-closing {} open positions at price {}", openTrades.size(), currentPrice);
        new ArrayList<>(openTrades.values()).forEach(t -> closeTrade(t, currentPrice, "REVERT"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // FEEDBACK REQUEST BUILDER (unchanged)
    // ─────────────────────────────────────────────────────────────────────

    private TradeFeedbackRequest buildFeedbackRequest(TradeRecord trade,
                                                      TradeOutcomeRecord outcome) {
        List<TradeFeedbackRequest.AgentVerdictSlim> slimVerdicts = null;
        if (trade.getAgentVerdicts() != null) {
            slimVerdicts = trade.getAgentVerdicts().stream()
                    .map(v -> TradeFeedbackRequest.AgentVerdictSlim.builder()
                            .agentName(v.getAgentName())
                            .signal(v.getSignal())
                            .conviction(v.getConviction())
                            .build())
                    .collect(Collectors.toList());
        }
        return TradeFeedbackRequest.builder()
                .tradeId(trade.getTradeId())
                .pnl(outcome.getRealisedPnlPct())
                .exitReason(outcome.getExitReason())
                .agentVerdicts(slimVerdicts)
                .marketRegime(trade.getMarketRegime() != null ? trade.getMarketRegime().name() : "RANGING")
                .build();
    }

    private double getCurrentPrice() {
        try {
            BigDecimal rawPrice = marketDataService.getLatestPrice();
            if (rawPrice == null || rawPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Monitor: Raw price is 0 or null. Waiting for next tick.");
                return 0.0;
            }
            return rawPrice.doubleValue();
        } catch (Exception e) {
            log.error("Monitor: Error fetching raw price: {}", e.getMessage());
            return 0.0;
        }
    }
}