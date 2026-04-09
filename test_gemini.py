import requests, json, os

with open('.env') as f:
    for line in f:
        if line.startswith('GEMINI_API_KEY='):
            api_key = line.strip().split('=', 1)[1]

payload = {
    "contents": [{"parts": [{"text": "Hello, write a short story of 50 words about AI."}]}],
    "generationConfig": {"temperature": 0.2, "maxOutputTokens": 180}
}
resp = requests.post(
    f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={api_key}",
    json=payload
)
print(resp.json())
