package com.nihal.newsai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nihal.newsai.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class GeminiClient {
    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 2000;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String geminiApiKey;
    private final String geminiApiUrl;

    public GeminiClient(
            RestClient restClient,
            ObjectMapper objectMapper,
            @Value("${gemini.api.key}") String geminiApiKey,
            @Value("${gemini.api.url}") String geminiApiUrl
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.geminiApiKey = geminiApiKey;
        this.geminiApiUrl = geminiApiUrl;
    }

    public String summarize(String content) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Server configuration missing: GEMINI_API_KEY is not set."
            );
        }

        String prompt = """
                You are a professional news analyst.
                Produce ONE quick, short summary that combines insights from all provided articles.
                Requirements:
                - One paragraph only.
                - 70 to 110 words.
                - Cover the overall trend and most important developments.
                - Mention key entities only when relevant.
                - Return plain text only (no bullets, no markdown, no JSON).
                
                News Context:
                """ + content;

        Map<String, Object> payload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 1024
                )
        );

        String url = UriComponentsBuilder
                .fromHttpUrl(geminiApiUrl)
                .queryParam("key", geminiApiKey)
                .toUriString();

        RestClientResponseException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String rawResponse = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(String.class);

                JsonNode root = objectMapper.readTree(rawResponse);

                if (root.has("error")) {
                    String errorMessage = root.path("error").path("message").asText("Gemini request failed.");
                    throw new ApiException(HttpStatus.BAD_GATEWAY, errorMessage);
                }

                JsonNode textNode = root.path("candidates")
                        .path(0)
                        .path("content")
                        .path("parts")
                        .path(0)
                        .path("text");

                String summary = textNode.asText("").trim();
                if (summary.isBlank()) {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, "Gemini returned an empty summary.");
                }

                return summary;

            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                if (isRetryable(status) && attempt < MAX_RETRIES) {
                    long backoff = INITIAL_BACKOFF_MS * (1L << (attempt - 1)); // 2s, 4s, 8s
                    log.warn("Gemini API returned {} on attempt {}/{}. Retrying in {}ms...",
                            status, attempt, MAX_RETRIES, backoff);
                    sleep(backoff);
                    lastException = ex;
                } else {
                    String providerMessage = extractProviderError(ex.getResponseBodyAsString());
                    throw new ApiException(
                            HttpStatus.BAD_GATEWAY,
                            "Gemini API request failed: " + providerMessage
                    );
                }
            } catch (ApiException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Unable to parse Gemini response.");
            }
        }

        // All retries exhausted
        String providerMessage = lastException != null
                ? extractProviderError(lastException.getResponseBodyAsString())
                : "Unknown error";
        throw new ApiException(
                HttpStatus.BAD_GATEWAY,
                "Gemini API failed after " + MAX_RETRIES + " retries: " + providerMessage
        );
    }

    private boolean isRetryable(int httpStatus) {
        return httpStatus == 429 || httpStatus == 503;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Request interrupted during retry.");
        }
    }

    private String extractProviderError(String body) {
        if (body == null || body.isBlank()) {
            return "No error body returned by provider.";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String message = root.path("error").path("message").asText("").trim();
            if (!message.isBlank()) {
                return message;
            }
        } catch (Exception ignored) {
            // Fall back to raw body if provider response isn't JSON.
        }
        return body;
    }
}
