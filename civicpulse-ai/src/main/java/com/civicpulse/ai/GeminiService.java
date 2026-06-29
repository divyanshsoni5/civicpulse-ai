package com.civicpulse.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Gemini-backed implementation of {@link AIClassificationService}.
 * <p>
 * Uses the <strong>official Google Gen AI Java SDK</strong>
 * ({@code com.google.genai:google-genai}) to call the Gemini API.
 * The {@link Client} bean is injected from {@link GeminiConfig}.
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
    // Dependencies
    // -----------------------------------------------------------------------

    /** Official Google Gen AI client – configured in {@link GeminiConfig}. */
    private final Client geminiClient;

    private final ObjectMapper objectMapper;

    /** Model name injected from {@code gemini.model} in application.properties. */
    @Value("${gemini.model}")
    private String model;

    public GeminiService(Client geminiClient, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    // -----------------------------------------------------------------------
    // Public API – generateText
    // -----------------------------------------------------------------------

    /**
     * Sends a free-form prompt to Gemini and returns the generated text.
     *
     * @param prompt the user prompt to send
     * @return the generated text from Gemini
     * @throws RuntimeException if the Gemini API call fails
     */
    public String generateText(String prompt) {
        log.info("Sending prompt to Gemini model '{}': {}", model, prompt);
        try {
            GenerateContentResponse response = geminiClient.models.generateContent(
                    model, prompt, null);
            String text = response.text();
            log.info("Gemini responded successfully.");
            log.debug("Gemini response text: {}", text);
            return text;
        } catch (Exception e) {
            log.error("Error calling Gemini generateContent: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini API error: " + e.getMessage(), e);
        }
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
            String text = generateText(prompt);
            log.debug("Gemini text block: {}", text);

            text = stripMarkdownFences(text);
            AIResponse response = objectMapper.readValue(text, AIResponse.class);
            response.setSuccess(true);

            log.info("Complaint classified – category: {}, priority: {}, department: {}",
                    response.getCategory(), response.getPriority(), response.getDepartment());
            return response;

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
            String text = generateText(prompt);
            log.info("City summary generated successfully.");
            return text;
        } catch (Exception e) {
            log.error("Error generating city summary: {}", e.getMessage(), e);
            return "Unable to generate summary at this time.";
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Removes markdown code fences that Gemini sometimes wraps JSON in,
     * e.g. <code>```json\n{...}\n```</code>.
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
