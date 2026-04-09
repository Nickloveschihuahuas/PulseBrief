# News AI — AI-Powered News Summarizer

An intelligent news aggregation and summarization service powered by **Google Gemini** and **NewsAPI**. Enter a topic, get a concise AI-generated summary with source articles.

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.3
- **AI:** Google Gemini 2.5 Flash
- **News:** NewsAPI.org
- **Frontend:** Vanilla HTML/CSS/JS
- **Caching:** Caffeine (10-min TTL)

## Quick Start

### Prerequisites

- Java 17+
- API keys from [NewsAPI](https://newsapi.org/) and [Google AI Studio](https://aistudio.google.com/apikey)

### Setup

```bash
# 1. Clone the repo
git clone <your-repo-url>
cd news-ai

# 2. Set environment variables
export NEWS_API_KEY=your_key_here
export GEMINI_API_KEY=your_key_here

# 3. Run the backend
./mvnw spring-boot:run

# 4. Open the frontend
open frontend/index.html
```

The backend runs on `http://localhost:8080`. The API is available at:
```
GET /api/v1/news/summary?topic=artificial+intelligence
```

### Docker

```bash
docker build -t news-ai .
docker run -p 8080:8080 \
  -e NEWS_API_KEY=your_key \
  -e GEMINI_API_KEY=your_key \
  news-ai
```

## API Reference

### `GET /api/v1/news/summary`

| Param   | Type   | Required | Description                    |
|---------|--------|----------|--------------------------------|
| `topic` | string | Yes      | News topic (2-80 characters)   |

**Success Response (200):**
```json
{
  "topic": "AI",
  "articleCount": 8,
  "sources": ["TechCrunch", "The Verge"],
  "summary": "AI-generated summary text...",
  "articles": [
    {
      "title": "Article Title",
      "source": "TechCrunch",
      "description": "...",
      "url": "https://...",
      "publishedAt": "2026-04-09T..."
    }
  ]
}
```

## Architecture

```
Frontend (HTML/CSS/JS)
    ↓ HTTP GET
Spring Boot Controller
    ↓
NewsSummaryService (@Cacheable)
    ├── NewsApiClient → newsapi.org
    └── GeminiClient  → Gemini API (with retry + backoff)
```

## Configuration

| Environment Variable     | Required | Default | Description                     |
|--------------------------|----------|---------|---------------------------------|
| `NEWS_API_KEY`           | Yes      | —       | NewsAPI.org API key             |
| `GEMINI_API_KEY`         | Yes      | —       | Google Gemini API key           |
| `CORS_ALLOWED_ORIGIN`   | No       | `*`     | Allowed CORS origin             |

## License

MIT
