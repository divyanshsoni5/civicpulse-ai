package com.civicpulse.service;

import com.civicpulse.ai.AIClassificationService;
import com.civicpulse.ai.AIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Facade service that adapts the {@link AIClassificationService} AI module
 * to the legacy {@code Map<String, String>} contract consumed by
 * {@link IssueService}.
 *
 * <p>This class intentionally holds <em>no AI-provider logic</em>; all
 * intelligence lives in the {@code com.civicpulse.ai} package.
 * Swapping the underlying AI provider only requires changing which
 * {@link AIClassificationService} bean is registered in Spring context.
 *
 * @deprecated Callers should migrate to injecting
 *             {@link AIClassificationService} directly to get the richer
 *             {@link AIResponse} with category, priority, department, and
 *             summary.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final AIClassificationService aiClassificationService;

    /**
     * Analyses a community issue and returns a map containing
     * {@code "category"} and {@code "severity"} keys.
     *
     * <p>Severity is mapped from the AI-generated {@code priority} field
     * to the four-value scale used by the {@code Issue} entity
     * (CRITICAL | HIGH | MEDIUM | LOW).
     *
     * @param title       issue title
     * @param description issue description
     * @return map with keys {@code category} and {@code severity}
     */
    public Map<String, String> analyzeIssue(String title, String description) {
        log.debug("analyzeIssue delegating to AIClassificationService – title: '{}'", title);

        AIResponse response = aiClassificationService.classifyComplaint(title, description);

        Map<String, String> result = new HashMap<>();

        if (response.isSuccess()) {
            // Map AI category to upper-case legacy values used by the Issue entity
            result.put("category", mapCategory(response.getCategory()));
            // Map AI priority → severity expected by Issue entity
            result.put("severity", mapPriorityToSeverity(response.getPriority()));
        } else {
            log.warn("AI classification failed ({}), using defaults.", response.getErrorMessage());
            result.put("category", "OTHER");
            result.put("severity", "LOW");
        }

        return result;
    }

    /**
     * Delegates city-summary generation to the AI module.
     *
     * @see AIClassificationService#generateCitySummary
     */
    public String generateCitySummary(long total, long open, long inProgress,
                                      long resolved, long critical,
                                      long high, String issueList) {
        return aiClassificationService.generateCitySummary(
                total, open, inProgress, resolved, critical, high, issueList);
    }

    // -----------------------------------------------------------------------
    // Mapping helpers
    // -----------------------------------------------------------------------

    /**
     * Normalises the AI-generated category to the upper-case values stored
     * in the {@code Issue} entity.
     */
    private String mapCategory(String aiCategory) {
        if (aiCategory == null || aiCategory.isBlank()) return "OTHER";
        return switch (aiCategory.trim().toUpperCase()) {
            case "ROAD"        -> "POTHOLE";   // legacy label kept for DB compatibility
            case "WATER"       -> "WATER_SUPPLY";
            case "ELECTRICITY" -> "STREETLIGHT";
            case "SANITATION"  -> "GARBAGE";
            case "DRAINAGE"    -> "DRAINAGE";
            case "NOISE"       -> "NOISE";
            default            -> aiCategory.trim().toUpperCase();
        };
    }

    /**
     * Converts the AI-generated priority string to the severity scale used
     * by the {@code Issue} entity.
     */
    private String mapPriorityToSeverity(String priority) {
        if (priority == null || priority.isBlank()) return "LOW";
        return switch (priority.trim().toUpperCase()) {
            case "CRITICAL" -> "CRITICAL";
            case "HIGH"     -> "HIGH";
            case "MEDIUM"   -> "MEDIUM";
            default         -> "LOW";
        };
    }
}