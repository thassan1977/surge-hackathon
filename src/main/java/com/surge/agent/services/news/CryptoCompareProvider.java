package com.surge.agent.services.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.surge.agent.model.NewsArticle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class CryptoCompareProvider implements NewsProvider {

    @Value("${api.cryptocompare.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<NewsArticle> fetchNews(String ticker) {
        String url = String.format("https://min-api.cryptocompare.com/data/v2/news/?lang=EN&api_key=%s", apiKey);
        try {
            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            List<NewsArticle> articles = new ArrayList<>();

            if (root != null && root.has("Data")) {
                for (JsonNode node : root.get("Data")) {
                    articles.add(new NewsArticle(
                            node.path("title").asText("No Title"),
                            node.path("source_info").path("name").asText("CryptoCompare"),
                            node.path("url").asText("#"),
                            0, // positiveVotes (Not available in free API)
                            0, // negativeVotes
                            0, // panicVotes
                            node.path("published_on").asLong() * 1000
                    ));
                }
            }
            return articles;
        } catch (Exception e) {
            log.error("CryptoCompare fetch failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public String getProviderName() { return "CryptoCompare"; }
}