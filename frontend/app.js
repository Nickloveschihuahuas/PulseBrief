// Auto-detect backend URL: Use localhost in local dev, fallback to Render backend in production.
const isLocal = window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1" || window.location.protocol === "file:";
const BACKEND_BASE_URL = isLocal
    ? "http://localhost:8080"
    : "https://pulsebrief-csrj.onrender.com";

const summaryForm = document.getElementById("summaryForm");
const topicInput = document.getElementById("topic");
const submitBtn = document.getElementById("submitBtn");
const btnText = submitBtn.querySelector(".btn-text");
const loader = document.getElementById("loader");
const errorBox = document.getElementById("errorBox");

const emptyState = document.getElementById("emptyState");
const resultCard = document.getElementById("resultCard");

const resultTopic = document.getElementById("resultTopic");
const articleCount = document.getElementById("articleCount");
const summaryText = document.getElementById("summaryText");
const sources = document.getElementById("sources");
const articleList = document.getElementById("articleList");

summaryForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    await fetchSummary();
});

async function fetchSummary() {
    const topic = topicInput.value.trim();
    if (!topic) {
        showError("Please enter a clear topic parameter.");
        return;
    }

    showLoading(true);
    clearError();
    resultCard.classList.add("hidden");

    const loaderStart = Date.now();

    try {
        const response = await fetch(
            `${BACKEND_BASE_URL}/api/v1/news/summary?topic=${encodeURIComponent(topic)}`
        );

        let payload;
        try {
            payload = await response.json();
        } catch {
            throw new Error("Invalid response from server. Please try again.");
        }

        if (!response.ok) {
            throw new Error(payload.message || "Synthesis engine failed. Request aborted.");
        }

        await enforceMinLoaderTime(loaderStart);
        renderSummary(payload);
    } catch (error) {
        await enforceMinLoaderTime(loaderStart);
        if (error instanceof TypeError) {
            showError("System offline. Verify local backend connectivity on port 8080.");
            return;
        }
        showError(error.message || "Unexpected critical error during intelligence synthesis.");
    } finally {
        showLoading(false);
    }
}

function enforceMinLoaderTime(startMs, minMs = 600) {
    const elapsed = Date.now() - startMs;
    if (elapsed < minMs) {
        return new Promise((resolve) => setTimeout(resolve, minMs - elapsed));
    }
    return Promise.resolve();
}

function renderSummary(data) {
    resultTopic.textContent = data.topic;
    articleCount.textContent = data.articleCount;
    summaryText.textContent = data.summary || "Synthesis context could not be established.";
    
    // Clear old data
    sources.innerHTML = "";
    articleList.innerHTML = "";

    // Parse Sources
    const rawSources = data.sources || [];
    if (rawSources.length === 0) {
        const span = document.createElement("span");
        span.textContent = "N/A";
        sources.appendChild(span);
    } else {
        rawSources.slice(0, 8).forEach((source) => {
            const chip = document.createElement("span");
            chip.textContent = source;
            sources.appendChild(chip);
        });
    }

    // Parse Articles
    const articles = data.articles || [];
    articles.forEach((article, index) => {
        const link = document.createElement("a");
        link.className = "article-item";
        link.href = article.url || "#";
        if (article.url) {
            link.target = "_blank";
            link.rel = "noopener noreferrer";
        } else {
            link.removeAttribute("href");
            link.style.cursor = "default";
        }

        // Add Number
        const itemNumber = index + 1;
        const numberDiv = document.createElement("div");
        numberDiv.className = "article-number";
        numberDiv.textContent = itemNumber < 10 ? `0${itemNumber}` : itemNumber;

        // Article Content Wrapper
        const contentDiv = document.createElement("div");
        contentDiv.className = "article-content";

        const title = document.createElement("h4");
        title.className = "article-title";
        title.textContent = article.title || "Unindexed Record";

        // Meta (Source + Date)
        const metaDiv = document.createElement("div");
        metaDiv.className = "article-meta";

        const sourceSpan = document.createElement("span");
        sourceSpan.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path></svg> ${article.source || "Unknown Node"}`;
        
        metaDiv.appendChild(sourceSpan);

        if (article.publishedAt) {
            const dot = document.createElement("span");
            dot.textContent = "•";
            const dateSpan = document.createElement("span");
            dateSpan.textContent = formatDate(article.publishedAt);
            metaDiv.appendChild(dot);
            metaDiv.appendChild(dateSpan);
        }

        if (!article.url) {
            const dot = document.createElement("span");
            dot.textContent = "•";
            const unavail = document.createElement("span");
            unavail.textContent = "Orphaned Link";
            metaDiv.appendChild(dot);
            metaDiv.appendChild(unavail);
        }

        contentDiv.appendChild(title);
        contentDiv.appendChild(metaDiv);

        link.appendChild(numberDiv);
        link.appendChild(contentDiv);
        
        articleList.appendChild(link);
    });

    // Toggle Visibility
    emptyState.classList.add("hidden");
    resultCard.classList.remove("hidden");
    
    // Smooth scroll to top on mobile
    if (window.innerWidth <= 768) {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

function showLoading(isLoading) {
    loader.classList.toggle("hidden", !isLoading);
    submitBtn.disabled = isLoading;
    btnText.textContent = isLoading ? "Synthesizing..." : "Generate Brief";
}

function hideResults() {
    resultCard.classList.add("hidden");
    emptyState.classList.remove("hidden");
}

function showError(message) {
    errorBox.textContent = message;
    errorBox.classList.remove("hidden");
    // Show empty state if no active results
    if (resultCard.classList.contains("hidden")) {
        emptyState.classList.remove("hidden");
    }
}

function clearError() {
    errorBox.textContent = "";
    errorBox.classList.add("hidden");
}

function formatDate(isoDate) {
    const date = new Date(isoDate);
    if (Number.isNaN(date.getTime())) {
        return isoDate;
    }
    return date.toLocaleString(undefined, {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}
