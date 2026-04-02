package com.surge.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.surge.agent.model.NewsArticle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * MarketState — complete market snapshot sent from Java to Python AI council.
 *
 * V4 additions vs V3:
 *   EMA100              — intermediate trend anchor between EMA50 and EMA200
 *   EMA slopes          — direction + speed of each EMA (% change over 3 bars)
 *   Golden/Death cross  — EMA50 crossing EMA100 (institutional signal)
 *   MACD(12,26,9)       — momentum divergence: value, signal, histogram, crossovers
 *   Bollinger Bands     — upper/lower/width + squeeze flag + band touch flags
 *   fundingRateAvailable— prevents agents hallucinating from 0.0 defaults
 *   bidAskSpreadDollars — real spread in USD (from L2 data)
 *   barCount            — lets Python know how many bars Java has collected
 *                         (agents adjust confidence when EMA200 not yet ready)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketState {

    // ── Identity ──────────────────────────────────────────────────────────────
    private String symbol;

    // ── Price & History ───────────────────────────────────────────────────────
    private BigDecimal currentPrice;
    @Builder.Default
    private List<Double> priceHistory = new ArrayList<>();
    private double change1h;
    private double change24h;
    private String priceTrend;           // STABLE/UP/DOWN/STRONG_UP/STRONG_DOWN

    // ── Order Flow ────────────────────────────────────────────────────────────
    private double volumeDelta;          // taker buy - sell volume this bar
    private double cumulativeDelta;      // session CVD (reset at midnight)
    private double orderBookImbalance;   // (bidQty - askQty) / total

    // ── Level 2 Order Book ────────────────────────────────────────────────────
    @Builder.Default
    private List<List<Double>> bids = new ArrayList<>();  // [[price, qty], ...]
    @Builder.Default
    private List<List<Double>> asks = new ArrayList<>();
    private double vwap;                 // volume-weighted average price this bar
    private double orderFlowImbalance;   // same as volumeDelta — taker net flow
    private double bidAskSpreadDollars;  // real-time best ask - best bid in USD

    // ── Technicals — RSI ─────────────────────────────────────────────────────
    private double rsi;                  // 14-period RSI
    private boolean rsiDivergence;       // price/RSI divergence flag (5-bar lookback)

    // ── Technicals — EMAs ────────────────────────────────────────────────────
    private double ema50;
    private double ema100;               // NEW — intermediate trend anchor
    private double ema200;
    private double distanceToEma50;      // % deviation from EMA50
    private double distanceToEma100;     // NEW
    private double distanceToEma200;

    // EMA slopes — % change over last 3 bars (positive=rising, negative=falling)
    private double ema50Slope;           // NEW — rising EMA = trend continuation
    private double ema100Slope;          // NEW
    private double ema200Slope;          // NEW

    // EMA cross events — true only on the bar the cross occurs
    private boolean goldenCross;         // NEW — EMA50 crossed above EMA100 (bullish)
    private boolean deathCross;          // NEW — EMA50 crossed below EMA100 (bearish)

    // ── Technicals — MACD(12, 26, 9) ─────────────────────────────────────────
    private double macd;                 // NEW — MACD line (EMA12 - EMA26)
    private double macdSignal;           // NEW — 9-period EMA of MACD
    private double macdHistogram;        // NEW — macd - signal (positive=bullish)
    private boolean macdBullishCross;    // NEW — MACD crossed above signal this bar
    private boolean macdBearishCross;    // NEW — MACD crossed below signal this bar

    // ── Technicals — Bollinger Bands (20, 2σ) ────────────────────────────────
    private double bbUpper;              // NEW — upper band
    private double bbLower;             // NEW — lower band
    private double bbWidth;             // NEW — (upper - lower) / middle, normalised
    private boolean bbUpperTouch;        // NEW — price >= upper band (overbought signal)
    private boolean bbLowerTouch;       // NEW — price <= lower band (oversold signal)
    private boolean bbSqueeze;          // NEW — width < 70% of 20-bar avg (coiled spring)

    // ── Technicals — Volatility ───────────────────────────────────────────────
    private double atr;                 // 14-period ATR from real H/L ticks
    private double volatility;          // 20-period standard deviation of close

    // ── Derivatives ───────────────────────────────────────────────────────────
    private double fundingRate;
    private boolean fundingRateAvailable; // NEW — false when spot (no futures feed)
    private double openInterest;
    private double openInterestChange1h;

    // ── Sentiment ────────────────────────────────────────────────────────────
    private double rollingSentimentScore;
    @Builder.Default
    private List<NewsArticle> recentHeadlines = new ArrayList<>();
    private boolean newsPanicDetected;
    @Builder.Default
    private int fearGreedIndex = 50;     // 0-100; default 50 = neutral

    // ── Data Quality ─────────────────────────────────────────────────────────
    private int barCount;                // NEW — how many bars collected so far
    // Agents use this to caveat EMA200/MACD reliability

    // ── On-Chain ─────────────────────────────────────────────────────────────
    @JsonIgnore
    private EthereumOnChainMetrics ethOnchain;

    /**
     * Jackson getter — never returns null, prevents Python 422.
     */
    @JsonProperty("ethOnchain")
    public EthereumOnChainMetrics getEthOnchain() {
        return ethOnchain != null ? ethOnchain : new EthereumOnChainMetrics();
    }

    // ── Convenience ──────────────────────────────────────────────────────────
    public EthereumOnChainMetrics ethOnchainSafe() {
        return getEthOnchain();
    }

    public boolean isPriceAboveEma200() {
        return ema200 > 0 && currentPrice != null && currentPrice.doubleValue() > ema200;
    }

    public boolean isPriceAboveEma100() {
        return ema100 > 0 && currentPrice != null && currentPrice.doubleValue() > ema100;
    }

    public boolean isPriceAboveEma50() {
        return ema50 > 0 && currentPrice != null && currentPrice.doubleValue() > ema50;
    }

    /**
     * True when all three EMAs are aligned upward — full bull stack.
     */
    public boolean isEmaStackedBullish() {
        return isPriceAboveEma50() && ema50 > ema100 && (ema200 == 0 || ema100 > ema200);
    }

    /**
     * True when all three EMAs are aligned downward — full bear stack.
     */
    public boolean isEmaStackedBearish() {
        double price = currentPrice != null ? currentPrice.doubleValue() : 0;
        return ema50 > 0 && price < ema50 && ema50 < ema100
                && (ema200 == 0 || ema100 < ema200);
    }

    public boolean isOverbought() {
        return rsi > 75.0;
    }

    public boolean isOversold() {
        return rsi < 25.0;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Nested: Ethereum On-Chain Metrics
    // ════════════════════════════════════════════════════════════════════════
    @lombok.Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EthereumOnChainMetrics {
        @Builder.Default
        private double gasPriceGwei = 20.0;
        @Builder.Default
        private double gasPriceZScore = 0.0;
        @Builder.Default
        private int validatorQueueSize = 0;
        @Builder.Default
        private double stakingApy = 4.0;
        @Builder.Default
        private double l2NetInflowEth = 0.0;
        @Builder.Default
        private double defiTvlChange24h = 0.0;
        @Builder.Default
        private int mevBundleCount1h = 0;
        @Builder.Default
        private double whaleWalletInflow = 0.0;
        @Builder.Default
        private double exchangeNetFlowEth = 0.0;

        public boolean isExchangeOutflow() {
            return exchangeNetFlowEth < -1_000;
        }

        public boolean isWhaleAccumulating() {
            return whaleWalletInflow > 500;
        }

        public boolean isGasAnomaly() {
            return gasPriceZScore > 3.0;
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // Convenience helpers (used by regime detector + risk engine in Java)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Computes the implied reward:risk ratio from ATR-based levels.
     * Used by RiskManagementService to validate TP/SL before submitting.
     *
     * @param takeProfitPct  from AITradeDecision (e.g. 0.025 = 2.5%)
     * @param stopLossPct    from AITradeDecision (e.g. 0.010 = 1.0%)
     */
    public double computeRewardRisk(double takeProfitPct, double stopLossPct) {
        return stopLossPct > 0 ? takeProfitPct / stopLossPct : 0.0;
    }

    public double getSpread() {
        if (bids == null || asks == null || bids.isEmpty() || asks.isEmpty()) {
            // Default spread 0.2% when depth missing
            return 0.002;
        }
        double bestBid = bids.get(0).get(0);
        double bestAsk = asks.get(0).get(0);
        return (bestAsk - bestBid) / bestBid;
    }
}