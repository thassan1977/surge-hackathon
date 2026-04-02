package com.surge.agent.services.news;

import com.surge.agent.model.NewsArticle;

import java.util.List;

public interface NewsProvider {
    List<NewsArticle> fetchNews(String ticker);
    String getProviderName();
}