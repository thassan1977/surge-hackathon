package com.surge.agent.services.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.surge.agent.model.NewsArticle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GeneralNewsProvider implements NewsProvider {

    @Value("${api.newsapi.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<NewsArticle> fetchNews(String ticker) {
        String url = String.format("https://newsapi.org/v2/everything?q=%s&sortBy=publishedAt&apiKey=%s", ticker, apiKey);

        try {
            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            List<NewsArticle> articles = new ArrayList<>();

            if (root != null && root.has("articles")) {
                for (JsonNode node : root.get("articles")) {
                    articles.add(new NewsArticle(
                            node.path("title").asText("No Title"),
                            node.path("source").path("name").asText("General News"),
                            node.path("url").asText("#"),
                            0, // positiveVotes
                            0, // negativeVotes
                            0, // panicVotes
                            parseTimestamp(node.path("publishedAt").asText())
                    ));
                }
            }
            return articles;
        } catch (Exception e) {
            log.error("NewsAPI fetch failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public String getProviderName() { return "GeneralNews"; }

    private long parseTimestamp(String dateStr) {
        try { return Instant.parse(dateStr).toEpochMilli(); }
        catch (Exception e) { return System.currentTimeMillis(); }
    }
}