package com.civicpulse.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Gemini-backed implementation of {@link AIClassificationService}.
 * <p>
 * Uses Spring's {@link RestClient} (introduced in Spring Boot 3.2) to call
 * the Google Gemini REST API.  The API key is injected from
 * {@code application.properties} via the property {@code gemini.api.key} and
 * must never be committed to source control.
 *
 * <h2>Prompt contract</h2>
 * {@link #classifyComplaint} instructs Gemini to return <em>only</em> a
 * JSON object in the shape of {@link AIResponse}, for example:
 * <pre>
 * {
 *   "category":   "Road",
 *   "priority":   "High",
 *   "department": "Public Works",
 *   "summary":    "Large pothole causing traffic disruption."
 * }
 * </pre>
 *
 * <h2>Error handling</h2>
 * All exceptions are caught, logged, and converted to a graceful
 * {@link AIResponse#failure(String)} so callers are never broken by an AI
 * outage.
 */
@Slf4j
@Service
public class GeminiService implements AIClassificationService {

    // -----------------------------------------------------------------------
    // Configuration
    // -----------------------------------------------------------------------

    /** Injected from {@code gemini.api.key} in application.properties. */
    @Value("${gemini.api.key}")
    private String apiKey;

    /**
     * Base URL of the Gemini generateContent endpoint.
     * Model is appended at call time so it can be changed per use-case.
     */
    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    /** Default model used for structured classification tasks. */
    private static final String CLASSIFICATION_MODEL = "gemini-2.0-flash:generateContent";

    /** Default model used for longer free-form summarisation tasks. */
    private static final String SUMMARY_MODEL = "gemini-2.0-flash:generateContent";

    // -----------------------------------------------------------------------
    // Dependencies
    // -----------------------------------------------------------------------

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // Build a shared RestClient with sensible defaults.
        // A timeout / connection-pool can be wired here via a custom
        // ClientHttpRequestFactory if required in the future.
        this.restClient = RestClient.builder()
                .baseUrl(GEMINI_BASE_URL)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // -----------------------------------------------------------------------
    // AIClassificationService implementation
    // -----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Sends the complaint title and description to Gemini and parses the
     * structured JSON response into an {@link AIResponse}.
     */
    @Override
    public AIResponse classifyComplaint(String title, String description) {
        log.info("Classifying complaint via Gemini – title: '{}'", title);

        String prompt = buildClassificationPrompt(title, description);

        try {
            String rawJson = callGemini(CLASSIFICATION_MODEL, prompt);
            log.debug("Gemini raw response for classification: {}", rawJson);

            String text = extractTextFromResponse(rawJson);
            log.debug("Gemini text block: {}", text);

            text = stripMarkdownFences(text);
            AIResponse response = objectMapper.readValue(text, AIResponse.class);
            response.setSuccess(true);

            log.info("Complaint classified – category: {}, priority: {}, department: {}",
                    response.getCategory(), response.getPriority(), response.getDepartment());
            return response;

        } catch (RestClientException e) {
            log.error("HTTP error calling Gemini API for classification: {}", e.getMessage(), e);
            return AIResponse.failure("Gemini API HTTP error: " + e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Gemini classification response: {}", e.getMessage(), e);
            return AIResponse.failure("Failed to parse AI response: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during complaint classification: {}", e.getMessage(), e);
            return AIResponse.failure("Unexpected AI error: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces a free-text city-wide summary using Gemini.
     */
    @Override
    public String generateCitySummary(long total, long open, long inProgress,
                                      long resolved, long critical,
                                      long high, String issueList) {
        log.info("Generating city summary – total issues: {}", total);

        String prompt = buildCitySummaryPrompt(total, open, inProgress, resolved, critical, high, issueList);

        try {
            String rawJson = callGemini(SUMMARY_MODEL, prompt);
            log.debug("Gemini raw response for city summary: {}", rawJson);

            String text = extractTextFromResponse(rawJson);
            log.info("City summary generated successfully.");
            return text;

        } catch (RestClientException e) {
            log.error("HTTP error calling Gemini API for city summary: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error generating city summary: {}", e.getMessage(), e);
        }
        return "Unable to generate summary at this time.";
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Sends a single-turn prompt to the specified Gemini model and returns
     * the raw JSON response body as a string.
     *
     * @param model  model path segment (e.g. "gemini-2.0-flash:generateContent")
     * @param prompt plain-text prompt to send
     * @return raw JSON string from Gemini
     */
    private String callGemini(String model, String prompt) {
        // Build the request body using Jackson-safe structures (no manual escaping)
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        String url = model + "?key=" + apiKey;

        return restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
    }

    /**
     * Navigates the Gemini response envelope and extracts the generated text.
     *
     * <pre>
     * {
     *   "candidates": [{
     *     "content": {
     *       "parts": [{ "text": "..." }]
     *     }
     *   }]
     * }
     * </pre>
     *
     * @param rawJson full JSON string returned by the Gemini endpoint
     * @return the text value inside the first candidate's first part
     * @throws IllegalStateException if the expected JSON structure is absent
     */
    private String extractTextFromResponse(String rawJson) throws Exception {
        JsonNode root = objectMapper.readTree(rawJson);

        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            log.error("Gemini response contained no candidates: {}", root.toPrettyString());
            throw new IllegalStateException("Gemini returned no candidates: " + rawJson);
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            log.error("Gemini candidate contained no parts: {}", root.toPrettyString());
            throw new IllegalStateException("Gemini candidate contained no parts: " + rawJson);
        }

        return parts.get(0).path("text").asText();
    }

    /**
     * Removes markdown code fences that Gemini sometimes wraps JSON in,
     * e.g. <code>```json\n{...}\n```</code>.
     *
     * @param text raw text from Gemini
     * @return clean JSON string
     */
    private String stripMarkdownFences(String text) {
        return text
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();
    }

    // -----------------------------------------------------------------------
    // Prompt builders
    // -----------------------------------------------------------------------

    /**
     * Builds the structured classification prompt.
     * <p>
     * The prompt is explicit about format to minimise hallucinations and
     * ensure the response can be deserialised directly into {@link AIResponse}.
     */
    private String buildClassificationPrompt(String title, String description) {
        return """
                You are a civic complaint classification AI for a smart city platform.
                Analyse the following citizen complaint and respond with ONLY a valid JSON object.
                Do NOT include any explanation, markdown, or extra text — only raw JSON.

                The JSON must strictly follow this schema:
                {
                  "category":   "<one of: Road, Water, Electricity, Sanitation, Parks, Noise, Building, Traffic, Other>",
                  "priority":   "<one of: Critical, High, Medium, Low>",
                  "department": "<responsible city department, e.g. Public Works, Water Board, BESCOM, Municipal Corporation>",
                  "summary":    "<one concise sentence summarising the core complaint>"
                }

                Complaint Title: %s
                Complaint Description: %s
                """.formatted(title, description);
    }

    /**
     * Builds the prompt for the city-wide issue summary use-case.
     */
    private String buildCitySummaryPrompt(long total, long open, long inProgress,
                                          long resolved, long critical,
                                          long high, String issueList) {
        return """
                You are a smart city AI assistant. Analyse these community issues and generate
                a concise, helpful summary for city administrators. Include key insights,
                the most urgent areas, and actionable recommendations. Keep it under 150 words.

                Statistics:
                  Total Issues   : %d
                  Open           : %d
                  In Progress    : %d
                  Resolved       : %d
                  Critical       : %d
                  High Severity  : %d

                Issue List:
                %s
                """.formatted(total, open, inProgress, resolved, critical, high, issueList);
    }
}
