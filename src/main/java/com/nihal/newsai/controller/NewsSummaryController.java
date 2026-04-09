package com.nihal.newsai.controller;

import com.nihal.newsai.dto.NewsSummaryResponse;
import com.nihal.newsai.exception.ApiException;
import com.nihal.newsai.service.NewsSummaryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/news")
public class NewsSummaryController {
    private final NewsSummaryService newsSummaryService;

    public NewsSummaryController(NewsSummaryService newsSummaryService) {
        this.newsSummaryService = newsSummaryService;
    }

    @GetMapping("/summary")
    public NewsSummaryResponse summarizeNews(
            @RequestParam
            @NotBlank(message = "Topic is required.")
            @Size(min = 2, max = 80, message = "Topic must be between 2 and 80 characters.")
            String topic
    ) {
        String normalizedTopic = topic.trim();
        if (normalizedTopic.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Topic is required.");
        }
        return newsSummaryService.summarizeTopic(normalizedTopic);
    }
}
