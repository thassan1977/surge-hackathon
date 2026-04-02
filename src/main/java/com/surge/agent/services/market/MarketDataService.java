package com.surge.agent.services.market;

import com.surge.agent.dto.MarketState;
import com.surge.agent.services.news.NewsAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.bollinger.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;

import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MarketDataService — V4
 *
 * V4 additions vs V3:
 *   EMA100                   — intermediate trend anchor
 *   EMA slopes (3-bar)       — direction + speed per EMA
 *   Golden/Death cross       — EMA50/EMA100 crossover detection
 *   MACD(12,26,9)            — momentum + crossover signals
 *   Bollinger Bands(20,2σ)   — squeeze detection + band touch flags
 *   Real bar volume          — uses aggTrade volume not bookTicker qty
 *   CVD reset on bar close   — volumeDelta is now per-bar (not cumulative)
 *   Session CVD midnight reset — cumulativeDelta bounded to 24h
 *   fundingRateAvailable     — prevents Python agents hallucinating on 0.0
 *   barCount exposed         — Python adjusts confidence based on readiness
 *   bidAskSpreadDollars      — computed from real L2 best bid/ask
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {

    @Value("${ai.orchestrator.bar.count:50}")
    private Integer MAX_BARS;

    @Value("${ai.orchestrator.bar.duration:60}")
    private Integer BAR_DURATION_SECONDS;

    @Value("${ai.orchestrator.bar.warmupms:5000}")
    private Integer WARMUP_LOG_INTERVAL_MS;

    private final NewsAggregator newsAggregator;

    // ── TA4J series ───────────────────────────────────────────────────────────
    private final BarSeries series = new BaseBarSeriesBuilder().withMaxBarCount(500).build();

    // ── Indicators ────────────────────────────────────────────────────────────
    private ClosePriceIndicator         closePriceIndicator;
    private RSIIndicator                rsiIndicator;
    private EMAIndicator                ema50Indicator;
    private EMAIndicator                ema100Indicator;   // NEW
    private EMAIndicator                ema200Indicator;
    private StandardDeviationIndicator  sdIndicator;
    private ATRIndicator                atrIndicator;

    // MACD(12, 26, 9)
    private MACDIndicator               macdIndicator;     // NEW
    private EMAIndicator                macdSignalIndicator; // NEW

    // Bollinger Bands (20 period, 2σ)
    private BollingerBandsMiddleIndicator bbMiddle;        // NEW
    private BollingerBandsUpperIndicator  bbUpper;         // NEW
    private BollingerBandsLowerIndicator  bbLower;         // NEW
    private BollingerBandWidthIndicator   bbWidth;         // NEW

    // ── Microstructure state ──────────────────────────────────────────────────
    private BigDecimal latestBidQty  = BigDecimal.ZERO;
    private BigDecimal latestAskQty  = BigDecimal.ZERO;
    private BigDecimal latestPrice   = BigDecimal.ZERO;
    private BigDecimal latestBid     = BigDecimal.ZERO;
    private BigDecimal latestAsk     = BigDecimal.ZERO;

    // ── Bar period ────────────────────────────────────────────────────────────
    private ZonedDateTime currentBarEndTime = null;

    // ── Aggression metrics ────────────────────────────────────────────────────
    private double currentPeriodVolumeDelta = 0.0;  // per-bar, reset on close
    private double cumulativeVolumeDelta    = 0.0;  // session CVD, reset at midnight
    private double currentBarTradeVolume    = 0.0;  // real aggTrade volume this bar

    // ── VWAP accumulators ─────────────────────────────────────────────────────
    private double currentBarVwapNumerator = 0.0;
    private double currentBarVwapVolume    = 0.0;

    // ── Intrabar H/L ──────────────────────────────────────────────────────────
    private BigDecimal currentBarHigh = BigDecimal.ZERO;
    private BigDecimal currentBarLow  = new BigDecimal("999999999");
    private BigDecimal currentBarOpen = BigDecimal.ZERO;

    // ── Derivative state ──────────────────────────────────────────────────────
    private volatile double latestFundingRate        = 0.0;
    private volatile boolean fundingRateAvailable    = false;
    private volatile double latestOpenInterest       = 0.0;
    private volatile double latestOpenInterestChange = 0.0;

    // ── L2 depth ──────────────────────────────────────────────────────────────
    private volatile List<List<Double>> latestBids = new ArrayList<>();
    private volatile List<List<Double>> latestAsks = new ArrayList<>();

    // ── Cache ─────────────────────────────────────────────────────────────────
    private volatile MarketState latestMarketState = null;
    private volatile String      latestSymbol      = "ETH/USDC";
    private volatile long        lastWarmupLogMs   = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────────────────

    @PostConstruct
    public void init() {
        closePriceIndicator  = new ClosePriceIndicator(series);
        rsiIndicator         = new RSIIndicator(closePriceIndicator, 14);
        ema50Indicator       = new EMAIndicator(closePriceIndicator, 50);
        ema100Indicator      = new EMAIndicator(closePriceIndicator, 100);
        ema200Indicator      = new EMAIndicator(closePriceIndicator, 200);
        sdIndicator          = new StandardDeviationIndicator(closePriceIndicator, 20);
        atrIndicator         = new ATRIndicator(series, 14);

        // MACD: fast=12, slow=26, signal=9
        macdIndicator        = new MACDIndicator(closePriceIndicator, 12, 26);
        macdSignalIndicator  = new EMAIndicator(macdIndicator, 9);

        // Bollinger Bands: period=20, multiplier=2.0
        bbMiddle = new BollingerBandsMiddleIndicator(closePriceIndicator);
        bbUpper  = new BollingerBandsUpperIndicator(bbMiddle, sdIndicator);
        bbLower  = new BollingerBandsLowerIndicator(bbMiddle, sdIndicator);
        bbWidth  = new BollingerBandWidthIndicator(bbUpper, bbMiddle, bbLower);

        log.info("MarketDataService initialised. Warmup target: {} bars (~{}min)",
                MAX_BARS, MAX_BARS * BAR_DURATION_SECONDS / 60);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LAYER 1: Order book tick (bookTicker)
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void processNewTick(String symbol,
                                            BigDecimal midPrice,
                                            BigDecimal bidPrice,
                                            BigDecimal askPrice,
                                            BigDecimal bidQty,
                                            BigDecimal askQty) {
        this.latestPrice  = midPrice;
        this.latestBid    = bidPrice;
        this.latestAsk    = askPrice;
        this.latestBidQty = bidQty;
        this.latestAskQty = askQty;
        this.latestSymbol = symbol;

        ZonedDateTime now = ZonedDateTime.now();

        if (currentBarEndTime == null) {
            currentBarEndTime = now.plusSeconds(BAR_DURATION_SECONDS);
            currentBarOpen    = midPrice;
            currentBarHigh    = midPrice;
            currentBarLow     = midPrice;
        }

        // Expand H/L with every tick
        if (midPrice.compareTo(currentBarHigh) > 0) currentBarHigh = midPrice;
        if (midPrice.compareTo(currentBarLow)  < 0) currentBarLow  = midPrice;

        // Close bar when time elapses
        if (now.isAfter(currentBarEndTime)) {
            closeCurrentBar(midPrice, bidQty.add(askQty));
        }

        refreshCache(symbol);
    }

    private void closeCurrentBar(BigDecimal closePrice, BigDecimal tickVolume) {
        try {
            // Use real aggTrade volume if available, fall back to bookTicker qty
            double barVol = currentBarTradeVolume > 0
                    ? currentBarTradeVolume
                    : tickVolume.doubleValue();

            series.addBar(
                    currentBarEndTime,
                    currentBarOpen,
                    currentBarHigh,
                    currentBarLow,
                    closePrice,
                    barVol
            );
        } catch (Exception e) {
            log.debug("Bar add skipped: {}", e.getMessage());
        }

        // Reset per-bar accumulators
        currentBarEndTime           = ZonedDateTime.now().plusSeconds(BAR_DURATION_SECONDS);
        currentBarOpen              = closePrice;
        currentBarHigh              = closePrice;
        currentBarLow               = closePrice;
        currentPeriodVolumeDelta    = 0.0;   // FIX: per-bar delta resets on close
        currentBarTradeVolume       = 0.0;
        currentBarVwapNumerator     = 0.0;
        currentBarVwapVolume        = 0.0;

        log.debug("Bar closed. Bars={} ATR_ready={} MACD_ready={} EMA200_ready={}",
                series.getBarCount(), isAtrReady(), isMacdReady(), isEma200Ready());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LAYER 2: Trade aggression (aggTrade)
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void processTrade(String symbol,
                                          BigDecimal price,
                                          BigDecimal qty,
                                          boolean isBuyerMaker) {
        double amount = qty.doubleValue();
        double priceD = price.doubleValue();
        double qtyD   = qty.doubleValue();

        // Real trade volume (not bookTicker quantity)
        currentBarTradeVolume += amount;

        // Volume delta: isBuyerMaker=true → taker is SELL (bearish), false → taker is BUY
        if (!isBuyerMaker) {
            currentPeriodVolumeDelta += amount;
            cumulativeVolumeDelta    += amount;
        } else {
            currentPeriodVolumeDelta -= amount;
            cumulativeVolumeDelta    -= amount;
        }

        // Expand intrabar H/L with actual fills
        if (price.compareTo(currentBarHigh) > 0) currentBarHigh = price;
        if (price.compareTo(currentBarLow)  < 0) currentBarLow  = price;

        // VWAP accumulation
        currentBarVwapNumerator += priceD * qtyD;
        currentBarVwapVolume    += qtyD;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LAYER 3: Derivative data (futures markPrice WebSocket)
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void processDerivativeData(double fundingRate,
                                                   double openInterest,
                                                   double oiChange1hPct) {
        this.latestFundingRate        = fundingRate;
        this.latestOpenInterest       = openInterest;
        this.latestOpenInterestChange = oiChange1hPct;
        this.fundingRateAvailable     = true;  // mark as live data
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LAYER 4: L2 depth (depth10 stream)
    // ─────────────────────────────────────────────────────────────────────────

    public synchronized void processDepthUpdate(String symbol,
                                                List<List<Double>> bids,
                                                List<List<Double>> asks) {
        this.latestBids = bids;
        this.latestAsks = asks;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GRADUATED READINESS CHECKS
    // ─────────────────────────────────────────────────────────────────────────

    public boolean isWarmupComplete()   { return series.getBarCount() >= MAX_BARS; }
    public boolean isRsiReady()         { return series.getBarCount() >= 28; }
    public boolean isAtrReady()         { return series.getBarCount() >= 28; }
    public boolean isEma50Reliable()    { return series.getBarCount() >= 50; }
    public boolean isEma100Ready()      { return series.getBarCount() >= 100; }
    public boolean isEma200Ready()      { return series.getBarCount() >= 200; }
    public boolean isMacdReady()        { return series.getBarCount() >= 60; }
    public boolean isBollingerReady()   { return series.getBarCount() >= 40; }
    public boolean isFullyWarmedUp()    { return series.getBarCount() >= 200; }

    public int  getBarCount()           { return series.getBarCount(); }
    public double getLatestFundingRate(){ return latestFundingRate; }

    public MarketState getLatestMarketState() { return latestMarketState; }

    // ─────────────────────────────────────────────────────────────────────────
    // SESSION RESET — CVD bounded to 24h
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 0 * * *")   // midnight UTC
    public synchronized void resetSessionDelta() {
        log.info("Session CVD reset. Final session delta: {:.2f}", cumulativeVolumeDelta);
        cumulativeVolumeDelta = 0.0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN STATE BUILDER
    // ─────────────────────────────────────────────────────────────────────────

    public MarketState getUnifiedState(String symbol) {
        int barCount  = series.getBarCount();
        int lastIndex = series.getEndIndex();

        if (barCount < MAX_BARS) {
            logWarmup(barCount);
            return null;
        }

        double price = latestPrice.doubleValue();
        MarketState state = new MarketState();

        // ── Identity ──────────────────────────────────────────────────────────
        state.setSymbol(symbol);
        state.setCurrentPrice(latestPrice);
        state.setBarCount(barCount);

        // ── L2 Order Book ─────────────────────────────────────────────────────
        state.setBids(latestBids);
        state.setAsks(latestAsks);
        state.setVwap(currentBarVwapVolume > 0
                ? currentBarVwapNumerator / currentBarVwapVolume : price);

        // Real bid-ask spread in dollars from L2
        if (!latestBids.isEmpty() && !latestAsks.isEmpty()
                && latestBids.get(0).size() >= 2 && latestAsks.get(0).size() >= 2) {
            state.setBidAskSpreadDollars(latestAsks.get(0).get(0) - latestBids.get(0).get(0));
        } else {
            state.setBidAskSpreadDollars(latestAsk.subtract(latestBid).doubleValue());
        }

        state.setOrderFlowImbalance(currentPeriodVolumeDelta);

        // ── Price & History ───────────────────────────────────────────────────
        state.setPriceHistory(getRecentPriceList(21));
        double change1h  = calculatePercentageChange(60);
        double change24h = calculatePercentageChange(Math.min(1440, barCount - 1));
        state.setChange1h(change1h);
        state.setChange24h(change24h);
        state.setPriceTrend(
                change1h > 1.5  ? "STRONG_UP"   :
                        change1h > 0.5  ? "UP"           :
                                change1h < -1.5 ? "STRONG_DOWN"  :
                                        change1h < -0.5 ? "DOWN"         : "STABLE");

        // ── Microstructure ────────────────────────────────────────────────────
        double totalBookQty = latestBidQty.add(latestAskQty).doubleValue();
        state.setOrderBookImbalance(totalBookQty > 0
                ? latestBidQty.subtract(latestAskQty).doubleValue() / totalBookQty : 0);
        state.setVolumeDelta(currentPeriodVolumeDelta);
        state.setCumulativeDelta(cumulativeVolumeDelta);

        // ── RSI ───────────────────────────────────────────────────────────────
        double rsi = rsiIndicator.getValue(lastIndex).doubleValue();
        state.setRsi(rsi);
        state.setRsiDivergence(detectRsiDivergence(lastIndex, rsi, price));

        // ── Volatility + ATR ──────────────────────────────────────────────────
        double volSd = sdIndicator.getValue(lastIndex).doubleValue();
        state.setVolatility(volSd);

        double atr;
        if (isAtrReady()) {
            double rawAtr = atrIndicator.getValue(lastIndex).doubleValue();
            atr = rawAtr > 0.001 ? rawAtr : volSd * Math.sqrt(2);
        } else {
            atr = volSd * Math.sqrt(2);
        }
        state.setAtr(atr);

        // ── EMA50 ─────────────────────────────────────────────────────────────
        double ema50 = ema50Indicator.getValue(lastIndex).doubleValue();
        state.setEma50(ema50);
        state.setDistanceToEma50(ema50 > 0 ? (price - ema50) / ema50 * 100.0 : 0.0);

        // EMA50 slope (% change over 3 bars)
        if (lastIndex >= 3) {
            double ema50prev = ema50Indicator.getValue(lastIndex - 3).doubleValue();
            state.setEma50Slope(ema50prev > 0 ? (ema50 - ema50prev) / ema50prev * 100.0 : 0.0);
        }

        // ── EMA100 ────────────────────────────────────────────────────────────
        if (isEma100Ready()) {
            double ema100 = ema100Indicator.getValue(lastIndex).doubleValue();
            state.setEma100(ema100);
            state.setDistanceToEma100(ema100 > 0 ? (price - ema100) / ema100 * 100.0 : 0.0);

            if (lastIndex >= 3) {
                double ema100prev = ema100Indicator.getValue(lastIndex - 3).doubleValue();
                state.setEma100Slope(ema100prev > 0
                        ? (ema100 - ema100prev) / ema100prev * 100.0 : 0.0);

                // EMA50/EMA100 cross detection (look back 1 bar)
                double ema50prev1  = ema50Indicator.getValue(lastIndex - 1).doubleValue();
                double ema100prev1 = ema100Indicator.getValue(lastIndex - 1).doubleValue();
                boolean ema50aboveNow  = ema50 >= ema100;
                boolean ema50abovePrev = ema50prev1 >= ema100prev1;
                state.setGoldenCross(!ema50abovePrev && ema50aboveNow);  // crossed above
                state.setDeathCross(ema50abovePrev && !ema50aboveNow);   // crossed below

                if (state.isGoldenCross())
                    log.info("🟡 GOLDEN CROSS: EMA50 crossed above EMA100 at ${}", latestPrice);
                if (state.isDeathCross())
                    log.info("💀 DEATH CROSS: EMA50 crossed below EMA100 at ${}", latestPrice);
            }
        }

        // ── EMA200 ────────────────────────────────────────────────────────────
        if (isEma200Ready()) {
            double ema200 = ema200Indicator.getValue(lastIndex).doubleValue();
            state.setEma200(ema200);
            state.setDistanceToEma200(ema200 > 0 ? (price - ema200) / ema200 * 100.0 : 0.0);

            if (lastIndex >= 3) {
                double ema200prev = ema200Indicator.getValue(lastIndex - 3).doubleValue();
                state.setEma200Slope(ema200prev > 0
                        ? (ema200 - ema200prev) / ema200prev * 100.0 : 0.0);
            }
        }

        // ── MACD(12, 26, 9) ───────────────────────────────────────────────────
        if (isMacdReady()) {
            double macdVal    = macdIndicator.getValue(lastIndex).doubleValue();
            double macdSig    = macdSignalIndicator.getValue(lastIndex).doubleValue();
            double macdHist   = macdVal - macdSig;
            state.setMacd(macdVal);
            state.setMacdSignal(macdSig);
            state.setMacdHistogram(macdHist);

            // Crossover: compare to previous bar
            if (lastIndex >= 1) {
                double prevMacd = macdIndicator.getValue(lastIndex - 1).doubleValue();
                double prevSig  = macdSignalIndicator.getValue(lastIndex - 1).doubleValue();
                state.setMacdBullishCross(prevMacd < prevSig && macdVal >= macdSig);
                state.setMacdBearishCross(prevMacd > prevSig && macdVal <= macdSig);

                if (state.isMacdBullishCross())
                    log.info("📈 MACD Bullish Cross at ${}", latestPrice);
                if (state.isMacdBearishCross())
                    log.info("📉 MACD Bearish Cross at ${}", latestPrice);
            }
        }

        // ── Bollinger Bands (20, 2σ) ─────────────────────────────────────────
        if (isBollingerReady()) {
            double upper  = bbUpper.getValue(lastIndex).doubleValue();
            double lower  = bbLower.getValue(lastIndex).doubleValue();
            double width  = bbWidth.getValue(lastIndex).doubleValue();
            state.setBbUpper(upper);
            state.setBbLower(lower);
            state.setBbWidth(width);

            // Band touch flags
            state.setBbUpperTouch(price >= upper * 0.9995);
            state.setBbLowerTouch(price <= lower * 1.0005);

            // Squeeze: current width < 70% of 20-bar average width
            if (lastIndex >= 20) {
                double sumWidth = 0.0;
                for (int i = lastIndex - 20; i <= lastIndex; i++) {
                    sumWidth += bbWidth.getValue(i).doubleValue();
                }
                double avgWidth = sumWidth / 21.0;
                state.setBbSqueeze(width < avgWidth * 0.7);

                if (state.isBbSqueeze())
                    log.debug("BB Squeeze detected — low volatility, breakout likely");
            }
        }

        // ── Derivatives ───────────────────────────────────────────────────────
        state.setFundingRate(latestFundingRate);
        state.setFundingRateAvailable(fundingRateAvailable);
        state.setOpenInterest(latestOpenInterest);
        state.setOpenInterestChange1h(latestOpenInterestChange);

        // ── Sentiment ─────────────────────────────────────────────────────────
        state.setRecentHeadlines(newsAggregator.getRecentArticles());
        state.setNewsPanicDetected(newsAggregator.isPanicPresent());
        state.setRollingSentimentScore(newsAggregator.getGlobalSentiment());

        return state;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void refreshCache(String symbol) {
        try {
            MarketState fresh = getUnifiedState(symbol);
            if (fresh != null) this.latestMarketState = fresh;
        } catch (Exception e) {
            log.trace("Cache refresh skipped: {}", e.getMessage());
        }
    }

    private void logWarmup(int barCount) {
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastWarmupLogMs < WARMUP_LOG_INTERVAL_MS) return;
        lastWarmupLogMs = nowMs;

        if (barCount == 0) {
            long secs = currentBarEndTime != null
                    ? java.time.Duration.between(ZonedDateTime.now(), currentBarEndTime).toSeconds()
                    : BAR_DURATION_SECONDS;
            log.info("Warming up — first bar closes in ~{}s. Price={}", Math.max(0, secs), latestPrice);
        } else {
            log.info("Warming up: {}/{} bars | RSI_ready={} MACD_ready={} EMA200_ready={}",
                    barCount, MAX_BARS, isRsiReady(), isMacdReady(), isEma200Ready());
        }
    }

    private List<Double> getRecentPriceList(int count) {
        List<Double> history = new ArrayList<>();
        int end   = series.getEndIndex();
        int start = Math.max(0, end - count + 1);
        for (int i = start; i <= end; i++) {
            history.add(series.getBar(i).getClosePrice().doubleValue());
        }
        return history;
    }

    private double calculatePercentageChange(int barsBack) {
        int end   = series.getEndIndex();
        int start = Math.max(0, end - barsBack);
        double sPrice = series.getBar(start).getClosePrice().doubleValue();
        double cPrice = series.getBar(end).getClosePrice().doubleValue();
        return sPrice > 0 ? (cPrice - sPrice) / sPrice * 100.0 : 0.0;
    }

    /**
     * RSI divergence — 5-bar lookback.
     * Bullish: price lower low + RSI higher low → potential reversal up.
     * Bearish: price higher high + RSI lower high → potential reversal down.
     */
    private boolean detectRsiDivergence(int lastIndex, double currentRsi, double currentPrice) {
        try {
            int lookback = 5;
            if (lastIndex < lookback) return false;
            double prevPrice = series.getBar(lastIndex - lookback).getClosePrice().doubleValue();
            double prevRsi   = rsiIndicator.getValue(lastIndex - lookback).doubleValue();
            boolean bearishDiv = currentPrice > prevPrice && currentRsi < prevRsi - 2.0;
            boolean bullishDiv = currentPrice < prevPrice && currentRsi > prevRsi + 2.0;
            return bearishDiv || bullishDiv;
        } catch (Exception e) {
            return false;
        }
    }
}