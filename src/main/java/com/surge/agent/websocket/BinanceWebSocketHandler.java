package com.surge.agent.websocket;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.surge.agent.services.market.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * BinanceWebSocketHandler — V3 fixed.
 *
 * Bugs fixed vs provided version:
 *
 *   FIX 1 — BigDecimal.divide(divisor, RoundingMode) without scale throws
 *            ArithmeticException on non-terminating decimals (e.g. 4697.01 / 2).
 *            Added explicit scale of 8.
 *
 *   FIX 2 — Binance sends symbol "ETHUSDT". MarketDataService.getUnifiedState()
 *            and all Java/Python checks use "ETH/USDC". normalizeSymbol() maps
 *            Binance format → internal format so MarketState.symbol is consistent.
 *
 *   FIX 3 — processBookTicker() used node.get() which throws NullPointerException
 *            if a field is absent. Replaced with node.path().asText() which
 *            returns "" on missing fields rather than crashing.
 *
 *   FIX 4 — Reconnection on status 1006 (unexpected stream end) was mentioned
 *            in the comment but not implemented. Added @Scheduled heartbeat that
 *            restarts the WebSocketConnectionManager if the session goes dead.
 *            Wire the manager via setConnectionManager() after Spring creates it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BinanceWebSocketHandler extends TextWebSocketHandler {

    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Injected by WebSocketConfig after manager is created
    private WebSocketConnectionManager connectionManager;
    private volatile WebSocketSession   activeSession = null;
    private final AtomicLong lastMessageMs = new AtomicLong(System.currentTimeMillis());

    @Value("${ai.binance.stream.url}")
    private String WS_STREAM_URL;

    private final ObjectReader streamReader = objectMapper.readerFor(BinanceStreamMessage.class);

    // ── Symbol mapping: Binance → internal ───────────────────────────────
    private static String normalizeSymbol(String binanceSymbol) {
        if (binanceSymbol == null) return "ETH/USDC";
        return switch (binanceSymbol.toUpperCase()) {
            case "ETHUSDT" -> "ETH/USDC";
            case "BTCUSDT" -> "BTC/USDC";
            case "ETHBTC"  -> "ETH/BTC";
            default        -> binanceSymbol;  // pass through unknown symbols unchanged
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // Connection lifecycle
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        activeSession = session;
        lastMessageMs.set(System.currentTimeMillis());
        log.info("Connected to Binance stream. Session: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        activeSession = null;
        log.warn("Binance connection closed. Code={} Reason={}",
                status.getCode(), status.getReason());
        // FIX 4: trigger immediate reconnect on unexpected close (1006)
        if (status.getCode() == 1006 || status.getCode() == 1001) {
            scheduleReconnect();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error: {}", exception.getMessage());
        activeSession = null;
        scheduleReconnect();
    }

    // ── Heartbeat: reconnect if no message received for 30 seconds ───────
    @Scheduled(fixedDelay = 30_000)
    public void heartbeat() {
        long silentMs = System.currentTimeMillis() - lastMessageMs.get();
        if (silentMs > 30_000) {
            log.warn("No Binance message for {}s — reconnecting.", silentMs / 1000);
            scheduleReconnect();
        }
    }
    private void scheduleReconnect() {
        log.info("Attempting to rebuild Binance WebSocket connection...");
        try {
            if (connectionManager != null && connectionManager.isRunning()) {
                connectionManager.stop();
            }

            Thread.sleep(3000); // 3-second backoff

            // REBUILD from scratch to clear corrupted SSL states
            StandardWebSocketClient client = new StandardWebSocketClient();

            WebSocketConnectionManager newManager = new WebSocketConnectionManager(
                    client,
                    this,
                    WS_STREAM_URL
            );

            newManager.setAutoStartup(true);
            newManager.start();

            // Replace the old corrupted manager with the fresh one
            this.connectionManager = newManager;

            log.info("Binance WebSocket reconnect sequence completed.");
        } catch (Exception e) {
            log.error("Reconnect failed: {}", e.getMessage());
        }
    }

    /** Called by WebSocketConfig so the handler can self-reconnect. */
    public void setConnectionManager(WebSocketConnectionManager manager) {
        this.connectionManager = manager;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Message routing
    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        lastMessageMs.set(System.currentTimeMillis());
        String payload = message.getPayload();

        try {
            // 1. FAST: Direct mapping to DTO instead of building a full DOM tree
            BinanceStreamMessage wrapper = streamReader.readValue(payload);

            String stream = wrapper.stream();
            JsonNode dataNode = wrapper.data();

            // 2. Defensive check: If the message wasn't a combined stream,
            // fallback to parsing the root (handles raw stream subscriptions)
            if (stream == null || dataNode == null) {
                dataNode = objectMapper.readTree(payload);
                stream = dataNode.path("e").asText(""); // Binance often puts event type in 'e'
            }

            // 3. Routing
            if (stream.contains("@bookTicker") || dataNode.has("b")) {
                // Priority 1: L2 Best Bid/Ask
                processBookTicker(dataNode);
            } else if (stream.contains("@aggTrade")) {
                // Priority 2: Real-time Trades
                processAggTrade(dataNode);
            } else if (stream.contains("@depth10")) {
                // Priority 3: Order Book Depth
                String rawSymbol = stream.split("@")[0].toUpperCase();
                String symbol = normalizeSymbol(rawSymbol);
                processDepthUpdate(symbol, dataNode);
            } else {
                log.trace("Unhandled stream: {} | Payload: {}", stream, payload);
            }

        } catch (Exception e) {
            log.error("Parsing error: {}. Payload: {}", e.getMessage(), payload);
        }
    }
    // ─────────────────────────────────────────────────────────────────────
    // Message processors
    // ─────────────────────────────────────────────────────────────────────

    private void processDepthUpdate(String symbol, JsonNode node) {
        List<List<Double>> bids = new ArrayList<>();
        List<List<Double>> asks = new ArrayList<>();

        for (JsonNode bid : node.get("bids")) {
            double price = bid.get(0).asDouble();
            double qty   = bid.get(1).asDouble();
            bids.add(Arrays.asList(price, qty));
        }
        for (JsonNode ask : node.get("asks")) {
            double price = ask.get(0).asDouble();
            double qty   = ask.get(1).asDouble();
            asks.add(Arrays.asList(price, qty));
        }
        marketDataService.processDepthUpdate(symbol, bids, asks);
    }

    private void processBookTicker(JsonNode node) {
        String rawSymbol = node.path("s").asText("ETHUSDT");
        String symbol    = normalizeSymbol(rawSymbol);

        String bidStr = node.path("b").asText("0");
        String askStr = node.path("a").asText("0");
        String bidQStr = node.path("B").asText("0");
        String askQStr = node.path("A").asText("0");

        if (bidStr.isEmpty() || askStr.isEmpty()) return;

        BigDecimal bidPrice = new BigDecimal(bidStr);
        BigDecimal askPrice = new BigDecimal(askStr);

        // ArithmeticException on non-terminating decimals like 4697.01/2
        BigDecimal midPrice = bidPrice.add(askPrice)
                .divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);

        BigDecimal bidQty = new BigDecimal(bidQStr.isEmpty() ? "0" : bidQStr);
        BigDecimal askQty = new BigDecimal(askQStr.isEmpty() ? "0" : askQStr);

        marketDataService.processNewTick(symbol, midPrice, bidPrice, askPrice, bidQty, askQty);
    }



    private void processAggTrade(JsonNode node) {
        String rawSymbol = node.path("s").asText("ETHUSDT");
        String symbol    = normalizeSymbol(rawSymbol);       // FIX 2

        String priceStr = node.path("p").asText("");
        String qtyStr   = node.path("q").asText("0");
        if (priceStr.isEmpty()) return;

        BigDecimal price = new BigDecimal(priceStr);
        BigDecimal qty   = new BigDecimal(qtyStr);

        // m=true: buyer is maker → taker was a SELL (bearish aggression)
        // m=false: buyer is taker → taker was a BUY (bullish aggression)
        boolean isBuyerMaker = node.path("m").asBoolean(false);

        marketDataService.processTrade(symbol, price, qty, isBuyerMaker);
    }
}