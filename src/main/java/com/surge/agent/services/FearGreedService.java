package com.surge.agent.services;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surge.agent.services.market.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

/**
 * FearGreedService — fetches the Crypto Fear & Greed Index from Alternative.me.
 *
 * API: https://api.alternative.me/fng/?limit=1
 *   - Free, no API key required
 *   - Updates once per day (fetching every 5 minutes is safe and gives fresh data after midnight)
 *   - Returns a value 0–100:
 *       0–25  = Extreme Fear
 *       25–45 = Fear
 *       46–55 = Neutral
 *       55–75 = Greed
 *       75–100 = Extreme Greed
 *
 * Integration:
 *   1. Add this service to your Spring context (it's @Service — auto-detected)
 *   2. Inject into AutonomousTradingOrchestrator or wherever marketState is built
 *   3. Call marketState.setFearGreedIndex(fearGreedService.getIndex()) before
 *      passing marketState to pythonAIClient.analyzeMarket()
 *
 * The index defaults to 50 (neutral) until the first successful fetch,
 * preventing false CRISIS triggers from the 0 default.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FearGreedService {

    private final RestTemplate      restTemplate;
    private final ObjectMapper      objectMapper = new ObjectMapper();

    private static final String API_URL = "https://api.alternative.me/fng/?limit=1";

    // Volatile for thread-safe reads from scheduling + request threads
    private volatile int    latestIndex     = 50;     // default = neutral
    private volatile String latestLabel     = "NEUTRAL";
    private volatile long   lastFetchEpoch  = 0;

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    /** Fetch immediately on startup so first trade analysis has real data. */
    @PostConstruct
    public void init() {
        fetch();
    }

    /** Refresh every 5 minutes. The index updates daily but this ensures
     *  we pick up midnight resets without restarting the service. */
    @Scheduled(fixedDelay = 300_000)
    public void scheduledFetch() {
        fetch();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public accessors
    // ─────────────────────────────────────────────────────────────────────

    /** Returns the latest Fear & Greed index (0–100). Default 50 until fetched. */
    public int getIndex() {
        return latestIndex;
    }

    /** Returns the human-readable label for the current index. */
    public String getLabel() {
        return latestLabel;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Fetch logic
    // ─────────────────────────────────────────────────────────────────────

    private void fetch() {
        try {
            String response = restTemplate.getForObject(API_URL, String.class);
            if (response == null || response.isBlank()) {
                log.warn("Fear/Greed API returned empty response");
                return;
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");

            if (!data.isArray() || data.isEmpty()) {
                log.warn("Fear/Greed API: no 'data' array in response: {}", response.substring(0, Math.min(200, response.length())));
                return;
            }

            JsonNode latest      = data.get(0);
            int      index       = latest.path("value").asInt(50);
            String   label       = latest.path("value_classification").asText("Neutral");
            long     timestamp   = latest.path("timestamp").asLong(0);

            // Validate the index is in range
            if (index < 0 || index > 100) {
                log.warn("Fear/Greed index out of range: {} — ignoring", index);
                return;
            }

            this.latestIndex    = index;
            this.latestLabel    = label.toUpperCase().replace(" ", "_");
            this.lastFetchEpoch = timestamp;

            log.info("Fear/Greed updated: {} ({}) — timestamp epoch {}",
                    index, label, timestamp);

        } catch (Exception e) {
            // Non-fatal: use previous value or default 50
            log.warn("Fear/Greed fetch failed (using last value {}): {}", latestIndex, e.getMessage());
        }
    }
}