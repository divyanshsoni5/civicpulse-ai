package com.civicpulse.ai;

import com.google.genai.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the Google Gen AI (Gemini) client.
 * <p>
 * Reads {@code gemini.api.key} from {@code application.properties} and
 * constructs a singleton {@link Client} bean that is injected wherever
 * Gemini calls are needed.  The API key must be supplied via the
 * {@code GEMINI_API_KEY} environment variable – it is never hard-coded.
 */
@Slf4j
@Configuration
public class GeminiConfig {

    /**
     * Injected from {@code gemini.api.key} in application.properties.
     * That property resolves to the {@code GEMINI_API_KEY} environment variable.
     */
    @Value("${gemini.api.key}")
    private String apiKey;

    /**
     * Creates and exposes the official Google Gen AI {@link Client} as a
     * Spring-managed singleton bean.
     *
     * <p>The client is configured for the <em>Gemini Developer API</em>
     * (AI Studio) using an explicit API key.  Switch to Vertex AI by setting
     * the {@code GOOGLE_GENAI_USE_VERTEXAI=true} environment variable instead.
     *
     * @return a fully configured {@link Client} ready to make Gemini API calls
     */
    @Bean
    public Client geminiClient() {
        log.info("Initialising Google Gen AI client (Gemini Developer API)");
        return Client.builder()
                .apiKey(apiKey)
                .build();
    }
}
