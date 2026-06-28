package com.civicpulse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, String> analyzeIssue(String title, String description) {
        Map<String, String> result = new HashMap<>();
        result.put("category", "OTHER");
        result.put("severity", "LOW");

        try {
            String prompt = "Analyze this community issue and respond in JSON only " +
                    "with keys 'category' and 'severity'. " +
                    "Categories: POTHOLE, STREETLIGHT, WATER_SUPPLY, " +
                    "GARBAGE, DRAINAGE, NOISE, OTHER. " +
                    "Severity: LOW, MEDIUM, HIGH, CRITICAL. " +
                    "Title: " + title + ". Description: " + description + ". " +
                    "Respond with JSON only, no extra text, no markdown.";

            String requestBody = "{"
                    + "\"contents\": [{"
                    + "\"parts\": [{"
                    + "\"text\": \"" + prompt.replace("\"", "\\\"") + "\""
                    + "}]"
                    + "}]"
                    + "}";

            String url = "https://generativelanguage.googleapis.com/v1beta/" +
                    "models/gemini-2.0-flash-lite:generateContent?key=" + apiKey;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            System.out.println("Gemini raw response: " + response.body());

            JsonNode root = objectMapper.readTree(response.body());

            // Safely navigate the response
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText();
                    System.out.println("Gemini text response: " + text);

                    // Clean markdown if present
                    text = text.replaceAll("```json", "")
                            .replaceAll("```", "")
                            .trim();

                    JsonNode analysisResult = objectMapper.readTree(text);
                    String category = analysisResult.path("category").asText("OTHER");
                    String severity = analysisResult.path("severity").asText("LOW");

                    result.put("category", category);
                    result.put("severity", severity);
                }
            } else {
                // Print error details from Gemini
                System.err.println("Gemini error response: " + root.toPrettyString());
            }

        } catch (Exception e) {
            System.err.println("Gemini API error: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }
}