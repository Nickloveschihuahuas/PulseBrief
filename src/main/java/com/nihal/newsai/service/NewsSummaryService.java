package com.nihal.newsai.service;

import com.nihal.newsai.client.GeminiClient;
import com.nihal.newsai.client.NewsApiClient;
import com.nihal.newsai.dto.NewsSummaryResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class NewsSummaryService {
    private final NewsApiClient newsApiClient;
    private final GeminiClient geminiClient;

    public NewsSummaryService(NewsApiClient newsApiClient, GeminiClient geminiClient) {
        this.newsApiClient = newsApiClient;
        this.geminiClient = geminiClient;
    }

    @Cacheable(value = "summaries", key = "#topic.toLowerCase()")
    public NewsSummaryResponse summarizeTopic(String topic) {
        NewsApiClient.NewsBundle bundle = newsApiClient.fetchNewsBundle(topic);

        // We cap payload size to avoid very large model prompts.
        String modelInput = bundle.combinedContext().length() > 6000
                ? bundle.combinedContext().substring(0, 6000)
                : bundle.combinedContext();

        String summary = geminiClient.summarize(modelInput);

        return new NewsSummaryResponse(
                topic,
                bundle.articleCount(),
                bundle.sources(),
                summary,
                bundle.articles()
        );
    }
}

