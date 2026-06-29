package com.civicpulse.ai;

/**
 * Contract for AI-powered classification and analysis operations.
 * <p>
 * Keeping this interface separate from any provider implementation
 * (e.g. {@link GeminiService}) allows the AI backend to be swapped or
 * supplemented without touching business-logic callers.
 *
 * <h2>Planned extension points</h2>
 * <ul>
 *   <li>Complaint categorisation – {@link #classifyComplaint}</li>
 *   <li>Priority prediction – extend {@link #classifyComplaint} or add a
 *       dedicated {@code predictPriority} method</li>
 *   <li>Duplicate detection – add {@code detectDuplicate(AIRequest)}</li>
 *   <li>Complaint summarisation – add {@code summariseComplaint(AIRequest)}</li>
 *   <li>Citizen response generation – add {@code generateCitizenResponse(AIRequest)}</li>
 *   <li>Analytics insights – add {@code generateInsights(AIRequest)}</li>
 *   <li>City summary – add {@code generateCitySummary(AIRequest)}</li>
 * </ul>
 */
public interface AIClassificationService {

    /**
     * Classifies a citizen complaint using AI.
     * <p>
     * The implementation must call the underlying AI provider with a
     * structured prompt and parse the response into an {@link AIResponse}.
     * If the provider call fails for any reason the method should return
     * {@link AIResponse#failure(String)} rather than propagating an exception.
     *
     * @param title       short title of the complaint (must not be {@code null})
     * @param description full description of the complaint
     * @return a non-null {@link AIResponse}; callers should check
     *         {@link AIResponse#isSuccess()} before trusting the fields
     */
    AIResponse classifyComplaint(String title, String description);

    /**
     * Generates a natural-language city-wide summary from aggregated issue
     * statistics and an issue list narrative.
     *
     * @param total       total number of issues
     * @param open        number of open issues
     * @param inProgress  number of in-progress issues
     * @param resolved    number of resolved issues
     * @param critical    number of critical-severity issues
     * @param high        number of high-severity issues
     * @param issueList   human-readable newline-delimited issue list
     * @return a generated summary string, or a fallback message on failure
     */
    String generateCitySummary(long total, long open, long inProgress,
                               long resolved, long critical,
                               long high, String issueList);
}
