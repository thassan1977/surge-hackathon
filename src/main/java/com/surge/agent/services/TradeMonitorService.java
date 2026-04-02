package com.surge.agent.services;

import com.surge.agent.dto.MarketState;
import com.surge.agent.dto.TradeRecord;
import com.surge.agent.dto.artifact.TradeOutcomeRecord;
import com.surge.agent.dto.request.TradeFeedbackRequest;
import com.surge.agent.enums.TradeAction;
import com.surge.agent.services.market.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * TradeMonitorService — V3 full implementation.
 *
 * Responsibilities:
 *   1. Holds all open TradeRecords in a thread-safe map.
 *   2. Every 5 seconds, checks each open trade against the current price.
 *   3. When TP or SL is hit, builds a TradeOutcomeRecord and calls:
 *        - ValidationService.finaliseOutcome()  → re-scores and re-posts artifact
 *        - PythonAIClient.sendTradeFeedback()   → ChromaDB + agent weight update
 *        - RiskManagementService.recordTrade()  → drawdown + Sharpe tracking
 *
 * Thread safety:
 *   - openTrades is a ConcurrentHashMap — safe for concurrent reads from the
 *     @Scheduled monitor and writes from TradeService.
 *   - closeTrade() removes from the map before calling out, preventing double-close.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeMonitorService {

    private final MarketDataService marketDataService;
    private final ValidationService     validationService;
    private final PythonAIClient        pythonAIClient;
    private final RiskManagementService riskService;

    /** tradeId → open TradeRecord */
    private final ConcurrentHashMap<String, TradeRecord> openTrades = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────
    // REGISTRATION
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Called by TradeService after a trade executes on-chain.
     * Starts monitoring from this point forward.
     */
    public void register(TradeRecord record) {
        openTrades.put(record.getTradeId(), record);
        log.info("Position registered | tradeId={} action={} entry={} TP={} SL={}",
                record.getTradeId(), record.getAction(),
                record.getEntryPrice(), record.getTakeProfitPrice(), record.getStopLossPrice());
    }

    /** Returns a snapshot of all currently open trades (read-only). */
    public Collection<TradeRecord> getOpenTrades() {
        return List.copyOf(openTrades.values());
    }

    /** Count of open positions — used by RiskManagementService guards. */
    public int getOpenCount() {
        return openTrades.size();
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRICE MONITOR (runs every 5 seconds)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Scheduled price tick monitor.
     * Checks every open trade against the current market price.
     * Closes any trade whose TP or SL level has been breached.
     */
    @Scheduled(fixedDelay = 5_000)
    public void monitorPositions() {
        if (openTrades.isEmpty()) return;

        double currentPrice = getCurrentPrice();
        if (currentPrice <= 0) {
            log.debug("Price unavailable — skipping monitor tick.");
            return;
        }

        // Snapshot keys to avoid ConcurrentModificationException during iteration
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
        } else {
            // SELL (short): TP when price falls, SL when price rises
            tpHit = price <= trade.getTakeProfitPrice();
            slHit = price >= trade.getStopLossPrice();
        }

        if (tpHit) {
            closeTrade(trade, price, "TP");
        } else if (slHit) {
            closeTrade(trade, price, "SL");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // CLOSE TRADE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Closes a trade, fires all downstream update hooks, and removes it from
     * the open trades map.
     *
     * Can also be called manually (e.g. from an admin endpoint or circuit breaker).
     */
    public void closeTrade(TradeRecord trade, double exitPrice, String reason) {
        // Remove first — prevents double-close if two ticks fire simultaneously
        TradeRecord removed = openTrades.remove(trade.getTradeId());
        if (removed == null) return; // already closed by a concurrent tick

        trade.setClosed(true);
        trade.setClosedAtEpoch(Instant.now().getEpochSecond());


        double pnlPct  = trade.calculatePnl(exitPrice);
        double pnlUsdc = trade.calculatePnlUsdc(exitPrice);
        long   holdSec = trade.getHoldDurationSeconds();

        TradeOutcomeRecord outcome = TradeOutcomeRecord.builder()
                .tradeId(trade.getTradeId())
                .exitReason(reason)
                .entryPriceUsd(trade.getEntryPrice())
                .exitPriceUsd(exitPrice)
                .realisedPnlPct(pnlPct)
                .realisedPnlUsdc(pnlUsdc)
                .holdDurationSeconds(holdSec)
                .closeTxHash(null)  // on-chain settlement tx not available here
                .build();

        log.info("Position closed | tradeId={} reason={} entry={} exit={} pnl={}% hold={}s",
                trade.getTradeId(), reason, trade.getEntryPrice(), exitPrice, pnlPct, holdSec);

        // ── 1. Re-score and re-post validation artifact with outcome ───────
        try {
            validationService.finaliseOutcome(outcome, trade);
        } catch (Exception e) {
            log.error("finaliseOutcome failed for tradeId={}: {}", trade.getTradeId(), e.getMessage());
        }

        // ── 2. Send feedback to Python for ChromaDB + agent weight update ──
        try {
            pythonAIClient.sendTradeFeedback(buildFeedbackRequest(trade, outcome));
        } catch (Exception e) {
            log.warn("sendTradeFeedback failed (non-fatal) for tradeId={}: {}",
                    trade.getTradeId(), e.getMessage());
        }

        // ── 3. Update rolling risk metrics (drawdown, Sharpe window) ──────
        try {
            riskService.recordTradeReturn(pnlPct, riskService.getCurrentEquityUsdc());
        } catch (Exception e) {
            log.warn("recordTradeReturn failed for tradeId={}: {}", trade.getTradeId(), e.getMessage());
        }
    }

    /**
     * Force-closes all open positions (called by circuit breaker or admin endpoint).
     * Uses the current market price and "REVERT" as the exit reason.
     */
    public void closeAllPositions() {
        if (openTrades.isEmpty()) return;
        double currentPrice = getCurrentPrice();
        log.warn("Force-closing {} open positions at price {}", openTrades.size(), currentPrice);
        new ArrayList<>(openTrades.values()).forEach(t -> closeTrade(t, currentPrice, "REVERT"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // FEEDBACK REQUEST BUILDER
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Builds the TradeFeedbackRequest for Python.
     * Includes slim agent verdicts (name + signal + conviction only) so Python
     * can update per-agent win-rate attribution in AgentPerformanceTracker.
     */
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
            MarketState mkt = marketDataService.getLatestMarketState();
            return mkt != null && mkt.getCurrentPrice() != null
                    ? mkt.getCurrentPrice().doubleValue() : 0.0;
        } catch (Exception e) {
            log.debug("Could not fetch current price: {}", e.getMessage());
            return 0.0;
        }
    }
}