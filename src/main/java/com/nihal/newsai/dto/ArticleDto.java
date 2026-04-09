package com.nihal.newsai.dto;

public record ArticleDto(
        String title,
        String source,
        String description,
        String url,
        String publishedAt
) { }
