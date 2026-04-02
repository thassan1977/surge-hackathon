package com.surge.agent.services;

import com.surge.agent.dto.artifact.AgentVerdict;
import com.surge.agent.dto.artifact.TradeOutcomeRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PerformanceTracker — rolling statistics for the StrategyCheckpointArtifact.
 *
 * Sharpe and Sortino are calculated here, not in RiskManagementService.
 * RiskManagementService.getSharpeRatio() exists as a secondary signal for
 * the risk guards. THIS class owns the authoritative values that go into
 * the on-chain checkpoint artifact judged by the hackathon leaderboard.
 *
 * ─────────────────────────────────────────────────────────────────────
 * SHARPE RATIO (annualised)
 * ─────────────────────────────────────────────────────────────────────
 * Formula:  Sharpe = (meanReturn / stdDev) × sqrt(N)
 *
 * Where N is the annualisation factor.
 * For trade-based returns (not time-series), N = expected trades/year.
 * At ~6 trades/day × 365 days ≈ 2190.
 * We use sqrt(252) × sqrt(tradeFrequencyFactor) which simplifies to
 * sqrt(tradeFreqPerYear). Conservatively we use 365 (daily equivalent).
 *
 * A Sharpe > 1.0 is good. > 2.0 is excellent for a trading strategy.
 *
 * ─────────────────────────────────────────────────────────────────────
 * SORTINO RATIO (annualised)
 * ─────────────────────────────────────────────────────────────────────
 * Sortino = (meanReturn / downsideDeviation) × sqrt(N)
 *
 * Downside deviation only counts returns BELOW zero (losses).
 * Sortino is a better metric than Sharpe for asymmetric strategies
 * because it doesn't penalise upside volatility.
 *
 * The hackathon scoring uses Sharpe as the primary metric but
 * Sortino is included in the checkpoint artifact for transparency.
 *
 * ─────────────────────────────────────────────────────────────────────
 * PER-AGENT WIN-RATE
 * ─────────────────────────────────────────────────────────────────────
 * Tracks which agents gave the correct signal on each closed trade.
 * "Correct" = agent signal matched the trade direction AND trade was profitable.
 * This feeds back to Python's AgentPerformanceTracker via sendTradeFeedback()
 * to update Judge weighting.
 */
@Slf4j
@Service
public class PerformanceTracker {

    // ── Window ───────────────────────────────────────────────────────────
    private static final int    WINDOW         = 30;
    private static final double ANNUALISE      = Math.sqrt(365.0);

    // ── Rolling trade returns (last WINDOW closed trades) ────────────────
    private final LinkedList<Double> returns       = new LinkedList<>();
    private final LinkedList<Long>   holdDurations = new LinkedList<>();  // seconds

    // ── Counts ────────────────────────────────────────────────────────────
    private int    buys           = 0;
    private int    sells          = 0;
    private int    holdsVetoed    = 0;
    private int    checkpointNum  = 0;
    private int    tpHits         = 0;
    private int    totalClosed    = 0;
    private int    circuitTrips   = 0;

    // ── Cumulative stats ──────────────────────────────────────────────────
    private double cumulativePnl  = 0.0;
    private double bestTrade      = 0.0;
    private double worstTrade     = 0.0;
    private double maxDrawdown    = 0.0;

    // ── Window start ──────────────────────────────────────────────────────
    private long windowStartMs = System.currentTimeMillis();

    // ── Per-agent win tracking ────────────────────────────────────────────
    // agentName → [wins, total]
    private final Map<String, int[]> agentStats = new ConcurrentHashMap<>();

    // agentName → cumulative pnl on trades where agent voted correctly
    private final Map<String, Double> agentPnlSum = new ConcurrentHashMap<>();

    // ── Per-regime tracking ───────────────────────────────────────────────
    // regime → [wins, total]
    private final Map<String, int[]>    regimeStats  = new ConcurrentHashMap<>();
    private final Map<String, Double>   regimePnlSum = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────
    // RECORD — called by ValidationService.finaliseOutcome()
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Records a closed trade result into all rolling windows.
     *
     * @param outcome     closed trade data (pnlPct, exitReason, holdSeconds)
     * @param regime      market regime string at time of trade
     * @param verdicts    agent verdicts from when the trade was entered
     */
    public synchronized void record(TradeOutcomeRecord outcome,
                                    String regime,
                                    List<AgentVerdict> verdicts) {
        double pnl    = outcome.getRealisedPnlPct();
        long   hold   = outcome.getHoldDurationSeconds();
        boolean won   = pnl > 0;

        // Rolling window
        returns.addLast(pnl);
        holdDurations.addLast(hold);
        if (returns.size() > WINDOW) {
            returns.removeFirst();
            holdDurations.removeFirst();
        }

        // Counts
        totalClosed++;
        cumulativePnl += pnl;
        if (pnl > bestTrade)  bestTrade  = pnl;
        if (pnl < worstTrade) worstTrade = pnl;
        if ("TP".equals(outcome.getExitReason())) tpHits++;

        // Per-regime
        String r = regime != null ? regime : "UNKNOWN";
        regimeStats.computeIfAbsent(r, k -> new int[]{0, 0});
        regimeStats.get(r)[1]++;
        if (won) regimeStats.get(r)[0]++;
        regimePnlSum.merge(r, pnl, Double::sum);

        // Per-agent — credit agents whose signal matched the trade outcome
        if (verdicts != null) {
            for (AgentVerdict v : verdicts) {
                if (v == null || v.getAgentName() == null) continue;
                String name    = v.getAgentName();
                boolean correct = agentSignalWasCorrect(v.getSignal(), pnl);
                agentStats.computeIfAbsent(name, k -> new int[]{0, 0});
                agentStats.get(name)[1]++;
                if (correct) agentStats.get(name)[0]++;
                agentPnlSum.merge(name, correct ? pnl : 0.0, Double::sum);
            }
        }

        log.debug("PerformanceTracker: pnl={} regime={} sharpe={} winRate={}%",
                pnl, r, getSharpeRatio(), getWinRate());
    }

    private boolean agentSignalWasCorrect(String signal, double pnl) {
        if (signal == null) return false;
        return switch (signal.toUpperCase()) {
            case "BUY"  -> pnl > 0;
            case "SELL" -> pnl > 0;   // SELL trade profited
            case "HOLD" -> pnl == 0;  // no trade = no loss
            default     -> false;
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // SHARPE (primary hackathon metric)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Rolling Sharpe ratio over last WINDOW trades, annualised.
     * Returns 0.0 if fewer than 5 trades — not statistically meaningful.
     *
     * Formula: (mean(returns) / stdDev(returns)) × sqrt(365)
     *
     * sqrt(365) assumes ~1 trade per day equivalent annualisation.
     * If you trade more frequently, the true annualised Sharpe is higher —
     * but this conservative figure is what the checkpoint artifact reports.
     */
    public synchronized double getSharpeRatio() {
        if (returns.size() < 5) return 0.0;
        double mean   = returns.stream().mapToDouble(x -> x).average().orElse(0);
        double var    = returns.stream().mapToDouble(x -> (x - mean) * (x - mean)).average().orElse(0);
        double stdDev = Math.sqrt(var);
        return stdDev > 0 ? (mean / stdDev) * ANNUALISE : 0.0;
    }

    // ─────────────────────────────────────────────────────────────────────
    // SORTINO
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Rolling Sortino ratio over last WINDOW trades, annualised.
     * Only counts downside (negative) returns in the denominator.
     *
     * Formula: (mean(returns) / downsideDeviation) × sqrt(365)
     */
    public synchronized double getSortinoRatio() {
        if (returns.size() < 5) return 0.0;
        double mean          = returns.stream().mapToDouble(x -> x).average().orElse(0);
        double downsideVar   = returns.stream()
                .mapToDouble(x -> x < 0 ? x * x : 0.0)
                .average().orElse(0);
        double downsideDev   = Math.sqrt(downsideVar);
        return downsideDev > 0 ? (mean / downsideDev) * ANNUALISE : 0.0;
    }

    // ─────────────────────────────────────────────────────────────────────
    // ACCESSORS — all called by ValidationService.postStrategyCheckpoint()
    // ─────────────────────────────────────────────────────────────────────

    public synchronized double getWinRate() {
        if (returns.isEmpty()) return 0.0;
        long wins = returns.stream().filter(r -> r > 0).count();
        return (double) wins / returns.size();
    }

    public synchronized double getAvgWinPct() {
        return returns.stream().filter(r -> r > 0)
                .mapToDouble(x -> x).average().orElse(0.0);
    }

    public synchronized double getAvgLossPct() {
        return returns.stream().filter(r -> r <= 0)
                .mapToDouble(x -> x).average().orElse(0.0);
    }

    /** Profit factor = gross wins / |gross losses|. > 1.0 means profitable. */
    public synchronized double getProfitFactor() {
        double grossWins   = returns.stream().filter(r -> r > 0).mapToDouble(x -> x).sum();
        double grossLosses = Math.abs(returns.stream().filter(r -> r <= 0).mapToDouble(x -> x).sum());
        return grossLosses > 0 ? grossWins / grossLosses : grossWins > 0 ? 999.0 : 0.0;
    }

    public double getCumulativePnlPct()   { return cumulativePnl; }
    public double getBestTrade()          { return bestTrade; }
    public double getWorstTrade()         { return worstTrade; }
    public double getMaxDrawdownWindow()  { return maxDrawdown; }
    public int    getCircuitBreakerTrips(){ return circuitTrips; }
    public int    getBuys()               { return buys; }
    public int    getSells()              { return sells; }
    public int    getHoldsVetoed()        { return holdsVetoed; }
    public int    getTradeCount()         { return returns.size(); }
    public long   getWindowStartMs()      { return windowStartMs; }

    public synchronized double getAvgHoldSeconds() {
        return holdDurations.stream().mapToLong(x -> x).average().orElse(0.0);
    }

    public synchronized double getTpHitRate() {
        return totalClosed > 0 ? (double) tpHits / totalClosed : 0.0;
    }

    // ── Per-agent stats ───────────────────────────────────────────────────

    public Map<String, Double> getWinRatesByAgent() {
        Map<String, Double> result = new LinkedHashMap<>();
        agentStats.forEach((name, stats) ->
                result.put(name, stats[1] > 0 ? (double) stats[0] / stats[1] : 0.0));
        return result;
    }

    public Map<String, Double> getAvgPnlByAgent() {
        Map<String, Double> result = new LinkedHashMap<>();
        agentStats.forEach((name, stats) ->
                result.put(name, stats[1] > 0 ? agentPnlSum.getOrDefault(name, 0.0) / stats[1] : 0.0));
        return result;
    }

    /** Returns the agent name with the highest win rate. */
    public String getMvpAgent() {
        return getWinRatesByAgent().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("TechnicalAlpha");
    }

    // ── Per-regime stats ──────────────────────────────────────────────────

    public Map<String, Integer> getTradesByRegime() {
        Map<String, Integer> result = new LinkedHashMap<>();
        regimeStats.forEach((r, stats) -> result.put(r, stats[1]));
        return result;
    }

    public Map<String, Double> getWinRateByRegime() {
        Map<String, Double> result = new LinkedHashMap<>();
        regimeStats.forEach((r, stats) ->
                result.put(r, stats[1] > 0 ? (double) stats[0] / stats[1] : 0.0));
        return result;
    }

    public String getBestRegime() {
        return getWinRateByRegime().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");
    }

    public String getWorstRegime() {
        return getWinRateByRegime().entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");
    }

    // ── Mutation methods ──────────────────────────────────────────────────

    public void incrementBuys()                             { buys++; }
    public void incrementSells()                            { sells++; }
    public void incrementHoldsVetoed()                      { holdsVetoed++; }
    public void incrementCircuitBreakerTrips()              { circuitTrips++; }
    public int  nextCheckpointNumber()                      { return ++checkpointNum; }
    public void resetWindowStart()                          { windowStartMs = System.currentTimeMillis(); }
    public void updateMaxDrawdown(double currentDrawdownPct) {
        if (currentDrawdownPct > maxDrawdown) maxDrawdown = currentDrawdownPct;
    }
}