package com.surge.agent.model;

public record NewsArticle(
        String title,
        String source,
        String url,
        int positiveVotes,
        int negativeVotes,
        int panicVotes,
        long timestamp
) {}