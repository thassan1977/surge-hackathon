package com.surge.agent.services.news;

import com.surge.agent.model.NewsArticle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NewsAggregator {
    private final List<NewsProvider> providers;
    private final List<NewsArticle> history = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HISTORY = 100;

    // Thresholds for professional "Panic" detection
    private static final int PANIC_THRESHOLD = 5;

    public NewsAggregator(List<NewsProvider> providers) {
        this.providers = providers;
    }

    @Scheduled(fixedRate = 300000) // Every 5 mins for higher responsiveness
    public void refreshContext() {
        try {
            List<NewsArticle> freshNews = providers.parallelStream()
                    .flatMap(p -> p.fetchNews("ETH").stream())
                    .filter(distinctByKey(NewsArticle::title))
                    .collect(Collectors.toList());

            history.addAll(0, freshNews);
            if (history.size() > MAX_HISTORY) {
                history.subList(MAX_HISTORY, history.size()).clear();
            }
            log.info("Aggregated {} news items. Current Sentiment: {}", freshNews.size(), getGlobalSentiment());
        } catch (Exception e) {
            log.error("News Aggregation failed: {}", e.getMessage());
        }
    }

    /**
     * Requirement for MarketDataService: Calculates a score from -1.0 (Bearish) to 1.0 (Bullish)
     */
    public double getGlobalSentiment() {
        if (history.isEmpty()) return 0.0;

        long now = System.currentTimeMillis();
        double weightedSum = 0;
        double totalWeight = 0;

        // Use a copy to prevent ConcurrentModificationException
        List<NewsArticle> snapshot = new ArrayList<>(history).stream().limit(20).toList();

        for (NewsArticle article : snapshot) {
            double rawScore = calculateArticleScore(article);

            // Age Decay: Linear decay over 2 hours (7200000 ms)
            long ageMs = now - article.timestamp();
            double ageWeight = Math.max(0, 1.0 - (ageMs / 7200000.0));

            // Source Weight (Example: Trust certain sources more)
            double sourceWeight = article.source().equalsIgnoreCase("Reuters") ? 1.5 : 1.0;

            weightedSum += (rawScore * ageWeight * sourceWeight);
            totalWeight += (ageWeight * sourceWeight);
        }

        return totalWeight > 0 ? weightedSum / totalWeight : 0.0;
    }

    /**
     * Requirement for MarketDataService: Detects if there's a spike in 'Panic' votes
     * Requires multiple articles to confirm panic, or one extreme outlier.
     */
    public boolean isPanicPresent() {
        List<NewsArticle> recent = new ArrayList<>(history).stream().limit(10).toList();

        long articlesWithPanic = recent.stream()
                .filter(a -> a.panicVotes() >= 3) // Lower threshold per article...
                .count();

        int totalPanicVotes = recent.stream().mapToInt(NewsArticle::panicVotes).sum();

        // Panic is TRUE if:
        // 1. More than 30% of recent news carries panic votes OR
        // 2. The total vote count is extremely high (Consensus)
        return articlesWithPanic >= 3 || totalPanicVotes >= 15;
    }

    /**
     * Requirement for MarketDataService: Returns raw articles for AI context
     */
    public List<NewsArticle> getRecentArticles() {
        return history.stream().limit(10).collect(Collectors.toList());
    }

    private double calculateArticleScore(NewsArticle a) {
        int totalVotes = a.positiveVotes() + a.negativeVotes() + a.panicVotes();
        if (totalVotes == 0) return 0.0;

        // Simple professional weight: Positives are +1, Negatives/Panic are -1
        double score = (a.positiveVotes() - (a.negativeVotes() + a.panicVotes())) / (double) totalVotes;
        return Math.max(-1.0, Math.min(1.0, score));
    }

    public String getAINewsContext() {
        StringBuilder sb = new StringBuilder("### CURRENT MARKET SENTIMENT ###\n");
        sb.append(String.format("Score: %.2f | Panic Detected: %s\n\n", getGlobalSentiment(), isPanicPresent()));

        history.stream().limit(5).forEach(a ->
                sb.append(String.format("[%s] %s (Votes: +%d/-%d)\n",
                        a.source(), a.title(), a.positiveVotes(), a.negativeVotes()))
        );
        return sb.toString();
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    /**
     * Computes a -1.0 to +1.0 sentiment score from news headline text.
     * Used when CryptoPanic vote counts are unavailable (all zeros).
     */
    public double computeHeadlineSentiment(List<?> headlines) {
        if (headlines == null || headlines.isEmpty()) return 0.0;

        java.util.List<String> BULLISH = java.util.List.of(
                "surge", "rally", "buy", "bullish", "breakout", "gains", "rises",
                "jumps", "soars", "boost", "adoption", "institutional", "ath",
                "record", "strong", "buys", "accumulate", "bottom"
        );
        java.util.List<String> BEARISH = java.util.List.of(
                "crash", "drop", "sell", "bearish", "fear", "falls", "plunges",
                "cuts", "stalls", "ban", "hack", "concern", "risk", "warning",
                "dump", "collapse", "lawsuit", "investigation", "recession"
        );

        int score = 0;
        for (Object article : headlines) {
            // Works with both NewsArticle record and any object with toString()
            String title = article.toString().toLowerCase();
            for (String w : BULLISH) if (title.contains(w)) score++;
            for (String w : BEARISH) if (title.contains(w)) score--;
        }

        return Math.max(-1.0, Math.min(1.0, score / (double) headlines.size()));
    }
}