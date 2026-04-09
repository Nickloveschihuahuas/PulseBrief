package com.nihal.newsai.dto;

import java.util.List;

public record NewsSummaryResponse(
        String topic,
        int articleCount,
        List<String> sources,
        String summary,
        List<ArticleDto> articles
) { }
