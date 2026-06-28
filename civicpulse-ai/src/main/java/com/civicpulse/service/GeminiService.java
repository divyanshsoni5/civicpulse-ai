package com.civicpulse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, String> analyzeIssue(String title, String description) {
        Map<String, String> result = new HashMap<>();
        result.put("category", "OTHER");
        result.put("severity", "LOW");

        try {
            String prompt = String.format(
                    "Analyze this community issue and respond in JSON only " +
                            "with keys 'category' and 'severity'. " +
                            "Categories: POTHOLE, STREETLIGHT, WATER_SUPPLY, " +
                            "GARBAGE, DRAINAGE, NOISE, OTHER. " +
                            "Severity: LOW, MEDIUM, HIGH, CRITICAL. " +
                            "Title: %s. Description: %s. " +
                            "Respond with JSON only, no extra text.",
                    title, description);

            String requestBody = String.format("""
                {
                    "contents": [{
                        "parts": [{
                            "text": "%s"
                        }]
                    }]
                }
                """, prompt.replace("\"", "\\\""));

            String url = "https://generativelanguage.googleapis.com/v1beta/" +
                    "models/gemini-2.0-flash:generateContent?key=" + apiKey;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("candidates")
                    .get(0).path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            text = text.replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            JsonNode analysisResult = objectMapper.readTree(text);
            result.put("category", analysisResult.path("category")
                    .asText("OTHER"));
            result.put("severity", analysisResult.path("severity")
                    .asText("LOW"));

        } catch (Exception e) {
            System.err.println("Gemini API error: " + e.getMessage());
        }

        return result;
    }
}