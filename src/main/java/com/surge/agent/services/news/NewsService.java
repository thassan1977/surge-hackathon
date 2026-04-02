package com.surge.agent.services.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;
import com.surge.agent.model.NewsArticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsService {
    private final String API_KEY = "be7649b6b62bf5a408ce18cef062a359930d6257b3992835302cea657c6342a3";
//    private final String BASE_URL = "https://cryptopanic.com/api/v1/posts/?auth_token=" + API_KEY + "&currencies=ETH";
    private final String BASE_URL = "https://data-api.coindesk.com/news/v1/article/list?lang=EN&limit=10";

    private final Queue<NewsArticle> newsHistory = Queues.synchronizedQueue(EvictingQueue.create(100));
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    // Panic Threshold: If 10% or more of the news window is "Panic" news
    private static final double PANIC_THRESHOLD = 0.10;

    /**
     * Fixed fetchNews() — mapped to CryptoCompare News API response structure.
     *
     * CoinDesk/ CryptoCompare response shape:
     * {
     *   "Data": [
     *     {
     *       "ID":           59341937,
     *       "TITLE":        "Wall Street Is Watching XRP...",
     *       "URL":          "https://...",
     *       "BODY":         "full article text...",
     *       "SENTIMENT":    "POSITIVE" | "NEGATIVE" | "NEUTRAL",
     *       "UPVOTES":      0,
     *       "DOWNVOTES":    0,
     *       "SCORE":        0,
     *       "PUBLISHED_ON": 1773757896,   ← Unix epoch seconds, NOT millis
     *       "SOURCE_DATA":  { "NAME": "TimesTabloid", ... },
     *       "CATEGORY_DATA":[{ "CATEGORY": "ETH" }, ...]
     *     }, ...
     *   ],
     *   "Err": {}
     * }
     */

    @Scheduled(fixedRate = 60_000)
    public void fetchNews() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json; charset=UTF-8");
            headers.set("Authorization", "Apikey " + API_KEY);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(
                    BASE_URL,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            MediaType contentType = responseEntity.getHeaders().getContentType();
            if (contentType == null || !contentType.includes(MediaType.APPLICATION_JSON)) {
                log.error("Non-JSON response from CryptoCompare — check API key and subscription.");
                return;
            }

            JsonNode body = responseEntity.getBody();
            if (body == null) {
                log.warn("CryptoCompare returned null body.");
                return;
            }

            // FIX 1: top-level key is "Data", not "results"
            JsonNode data = body.get("Data");
            if (data == null || !data.isArray() || data.isEmpty()) {
                // Check if there's an error message
                JsonNode err = body.path("Err");
                log.warn("CryptoCompare: no 'Data' array in response. Err: {}", err);
                return;
            }

            int added   = 0;
            int skipped = 0;

            for (JsonNode node : data) {

                // FIX 6: filter to ETH-relevant articles using CATEGORY_DATA
                // Skip articles that are only about BTC, XRP, or other non-ETH assets
                if (!isEthRelevant(node)) {
                    skipped++;
                    continue;
                }

                // FIX 7: use the API's own SENTIMENT field for panic detection
                String sentiment   = node.path("SENTIMENT").asText("NEUTRAL").toUpperCase();
                boolean isPanic    = "NEGATIVE".equals(sentiment);

                // FIX 2+3: correct field paths
                String title    = node.path("TITLE").asText("");
                String url      = node.path("URL").asText("");

                // FIX 3: source name is nested under SOURCE_DATA.NAME
                String source   = node.path("SOURCE_DATA").path("NAME").asText("Unknown");

                // FIX 4: CryptoCompare uses UPVOTES / DOWNVOTES / SCORE, not votes.positive etc.
                int upvotes     = node.path("UPVOTES").asInt(0);
                int downvotes   = node.path("DOWNVOTES").asInt(0);
                int panicVotes  = isPanic ? 1 : 0;  // derive panic from SENTIMENT since no panic vote field

                // FIX 5: PUBLISHED_ON is Unix epoch SECONDS — multiply by 1000 for Java millis
                long publishedMs = node.path("PUBLISHED_ON").asLong(0L) * 1000L;

                if (title.isBlank() || url.isBlank()) continue;

                NewsArticle article = new NewsArticle(
                        title,
                        source,
                        url,
                        upvotes,
                        downvotes,
                        panicVotes,
                        publishedMs
                );

                newsHistory.add(article);
                added++;
            }

            // Compute panic ratio from SENTIMENT field
            long panicCount = newsHistory.stream()
                    .filter(n -> n.panicVotes() > 0)
                    .count();
            double panicRatio = newsHistory.isEmpty() ? 0.0
                    : (double) panicCount / newsHistory.size() * 100;

            log.info("CryptoCompare news: {} added, {} skipped (non-ETH). "
                            + "Total: {} | Panic ratio: {}%",
                    added, skipped, newsHistory.size(), panicRatio);

        } catch (Exception e) {
            log.error("Failed to fetch news: {}", e.getMessage());
        }
    }

    /**
     * Returns true if the article is relevant to ETH/USDC trading.
     *
     * CryptoCompare CATEGORY_DATA contains objects like {"CATEGORY": "ETH"}.
     * We include articles that mention ETH, blockchain, DeFi, or the broader
     * crypto market. We skip articles that are purely about other chains/tokens
     * (XRP-only, BTC-only FARTCOIN spam, etc.) unless they also mention ETH.
     */
    private boolean isEthRelevant(JsonNode node) {
        JsonNode categories = node.path("CATEGORY_DATA");
        if (!categories.isArray()) return true; // no categories = include by default

        boolean hasEth      = false;
        boolean hasOtherOnly = false;

        // Categories that are ETH-relevant
        java.util.Set<String> ETH_CATS = java.util.Set.of(
                "ETH", "BLOCKCHAIN", "DEFI", "MARKET", "CRYPTOCURRENCY",
                "REGULATION", "TECHNOLOGY", "BUSINESS", "EXCHANGE", "TRADING"
        );
        // Categories that indicate non-ETH focus
        java.util.Set<String> SKIP_CATS = java.util.Set.of(
                "XRP", "BTC", "USDT", "ADA", "SOL", "MATIC", "SPONSORED"
        );

        for (JsonNode cat : categories) {
            String c = cat.path("CATEGORY").asText("").toUpperCase();
            if (ETH_CATS.contains(c)) return true;   // immediately relevant
            if ("ETH".equals(c))       hasEth = true;
            if (SKIP_CATS.contains(c)) hasOtherOnly = true;
        }

        // If only skip categories and no ETH category, filter out
        return hasEth || !hasOtherOnly;
    }

    /**
     * Checks if the "Panic" sentiment is currently significant.
     * Logic: Count how many articles in our 100-item window have panic votes > 0.
     */
    public boolean isPanicActive() {
        if (newsHistory.isEmpty()) return false;

        long panicArticleCount = newsHistory.stream()
                .filter(n -> n.panicVotes() > 0)
                .count();

        double panicRatio = (double) panicArticleCount / newsHistory.size();

        if (panicRatio >= PANIC_THRESHOLD) {
            log.warn("!!! PANIC DETECTED !!! News Panic Ratio: {}%", (panicRatio * 100));
            return true;
        }

        return false;
    }

    public double calculateRollingSentiment() {
        if (newsHistory.isEmpty()) return 0.0;

        // Weighting panic more heavily than standard negative news
        double totalScore = newsHistory.stream()
                .mapToDouble(n -> {
                    double score = n.positiveVotes() - n.negativeVotes();
                    if (n.panicVotes() > 0) {
                        score -= (n.panicVotes() * 5); // Massive penalty for panic
                    }
                    return score;
                })
                .sum();

        return totalScore / newsHistory.size();
    }

    public List<NewsArticle> getWindowHeadlines() {
        // Return the latest 5 articles from the history
        List<NewsArticle> list = new ArrayList<>(newsHistory);
        Collections.reverse(list);
        return list.stream().limit(5).toList();
    }
}