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

    public String generateCitySummary(long total, long open, long inProgress,
                                      long resolved, long critical,
                                      long high, String issueList) {
        try {
            String prompt = "You are a smart city AI assistant. Analyze these " +
                    "community issues and generate a concise, helpful summary " +
                    "for city administrators. Include key insights, most urgent " +
                    "areas, and recommendations. Keep it under 150 words.\n\n" +
                    "Total Issues: " + total + "\n" +
                    "Open: " + open + "\n" +
                    "In Progress: " + inProgress + "\n" +
                    "Resolved: " + resolved + "\n" +
                    "Critical: " + critical + "\n" +
                    "High Severity: " + high + "\n\n" +
                    "Issue List:\n" + issueList;

            String requestBody = "{"
                    + "\"contents\": [{"
                    + "\"parts\": [{"
                    + "\"text\": \"" + prompt.replace("\"", "\\\"")
                    .replace("\n", "\\n") + "\""
                    + "}]"
                    + "}]"
                    + "}";

            String url = "https://generativelanguage.googleapis.com/v1beta/" +
                    "models/gemini-1.5-flash:generateContent?key=" + apiKey;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                return candidates.get(0).path("content")
                        .path("parts").get(0)
                        .path("text").asText();
            }
        } catch (Exception e) {
            System.err.println("Gemini summary error: " + e.getMessage());
        }
        return "Unable to generate summary at this time.";
    }
}