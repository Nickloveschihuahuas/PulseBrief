package com.nihal.newsai.client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nihal.newsai.dto.ArticleDto;
import com.nihal.newsai.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class NewsApiClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String newsApiKey;
    private final int pageSize;

    public NewsApiClient(
            RestClient restClient,
            ObjectMapper objectMapper,
            @Value("${news.api.key}") String newsApiKey,
            @Value("${news.fetch.page-size}") int pageSize
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.newsApiKey = newsApiKey;
        this.pageSize = pageSize;
    }

    public NewsBundle fetchNewsBundle(String topic) {
        if (newsApiKey == null || newsApiKey.isBlank()) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Server configuration missing: NEWS_API_KEY is not set."
            );
        }

        String url = UriComponentsBuilder
                .fromHttpUrl("https://newsapi.org/v2/everything")
                .queryParam("q", topic)
                .queryParam("sortBy", "publishedAt")
                .queryParam("language", "en")
                .queryParam("pageSize", pageSize)
                .queryParam("apiKey", newsApiKey)
                .toUriString();

        String response = restClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();
            if (!"ok".equalsIgnoreCase(status)) {
                String message = root.path("message").asText("News API returned an error.");
                throw new ApiException(HttpStatus.BAD_GATEWAY, message);
            }

            JsonNode articles = root.path("articles");
            if (!articles.isArray() || articles.isEmpty()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "No recent articles found for this topic.");
            }

            StringBuilder combinedContext = new StringBuilder();
            Set<String> uniqueSources = new LinkedHashSet<>();
            List<ArticleDto> articleItems = new ArrayList<>();
            int articleCount = 0;

            for (JsonNode article : articles) {
                String title = clean(article.path("title").asText(""));
                String description = clean(article.path("description").asText(""));
                String content = clean(article.path("content").asText(""));
                String source = clean(article.path("source").path("name").asText(""));
                String urlValue = clean(article.path("url").asText(""));
                String publishedAt = clean(article.path("publishedAt").asText(""));

                if (!source.isBlank()) {
                    uniqueSources.add(source);
                }
                if (title.isBlank() && description.isBlank() && content.isBlank() && urlValue.isBlank()) {
                    continue;
                }

                combinedContext.append("Title: ").append(title).append("\n")
                        .append("Description: ").append(description).append("\n")
                        .append("Content: ").append(content).append("\n\n");

                articleItems.add(new ArticleDto(
                        title.isBlank() ? "Untitled Article" : title,
                        source.isBlank() ? "Unknown Source" : source,
                        description,
                        urlValue,
                        publishedAt
                ));
                articleCount++;
            }

            if (articleCount == 0 || combinedContext.isEmpty()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Articles found, but they had no usable content to summarize.");
            }

            return new NewsBundle(
                    combinedContext.toString().trim(),
                    articleCount,
                    new ArrayList<>(uniqueSources),
                    articleItems
            );
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Unable to parse News API response.");
        }
    }

    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("\\s+", " ")
                .replace("[+\\d+ chars]", "")
                .trim();
    }

    public record NewsBundle(
            String combinedContext,
            int articleCount,
            List<String> sources,
            List<ArticleDto> articles
    ) { }
}
