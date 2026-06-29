package com.civicpulse.ai;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a generic AI request payload.
 * <p>
 * Decouples the caller from any specific AI provider's request schema.
 * Future features (duplicate detection, response generation, analytics) can
 * reuse this contract by adding new {@link AIRequest.Type} values.
 */
@Data
@Builder
public class AIRequest {

    /**
     * The type of AI operation being requested.
     * Allows the AI module to route or adapt prompts per use-case.
     */
    public enum Type {
        COMPLAINT_CLASSIFICATION,
        PRIORITY_PREDICTION,
        DUPLICATE_DETECTION,
        COMPLAINT_SUMMARIZATION,
        CITIZEN_RESPONSE_GENERATION,
        ANALYTICS_INSIGHTS,
        CITY_SUMMARY
    }

    /** The type of AI operation being performed. */
    private Type type;

    /**
     * The title of the complaint / issue being analysed.
     * Optional – some request types may not need it.
     */
    private String title;

    /**
     * The full description of the complaint / issue.
     * Optional – some request types may not need it.
     */
    private String description;

    /**
     * An arbitrary context payload for extended request types
     * (e.g. serialised JSON for analytics, issue list for city summary).
     */
    private String contextPayload;
}
