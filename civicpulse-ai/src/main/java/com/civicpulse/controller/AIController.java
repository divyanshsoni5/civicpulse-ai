package com.civicpulse.controller;

import com.civicpulse.ai.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing AI test endpoints.
 * <p>
 * Intended for quick smoke-testing of the Gemini integration.
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final GeminiService geminiService;

    /**
     * Smoke-test endpoint that calls Gemini with a fixed prompt and returns
     * the generated text as plain text.
     *
     * <p>Example:
     * <pre>
     * GET http://localhost:8082/api/ai/test
     * </pre>
     *
     * @return plain-text response from Gemini
     */
    @GetMapping(value = "/test", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> testGemini() {
        log.info("Received request on GET /api/ai/test");
        String response = geminiService.generateText("Say hello from Gemini");
        return ResponseEntity.ok(response);
    }
}
