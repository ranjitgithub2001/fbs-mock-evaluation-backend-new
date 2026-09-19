package com.fbs.mock_evaluation_system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final String GROQ_API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    @Value("${groq.api.key:}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/generate")
    public ResponseEntity<String> generate(@RequestBody Map<String, Object> request) {
        System.out.println("AI endpoint hit successfully");

        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(503)
                    .body("{\"error\":\"AI is not configured on this server\"}");
        }

        try {
            String promptText = extractPrompt(request);

            String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", "openai/gpt-oss-20b",
                "messages", List.of(
                    Map.of("role", "user", "content", promptText)
                ),
                "max_tokens", 1000
            ));

            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("Groq status: " + response.statusCode());
            return fromGroqResponse(response.statusCode(), response.body());

        } catch (Exception e) {
            System.out.println("AI error: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    static ResponseEntity<String> fromGroqResponse(int statusCode, String body) {
        return ResponseEntity.status(statusCode).body(body);
    }

    @SuppressWarnings("unchecked")
    private String extractPrompt(Map<String, Object> request) {
        try {
            List<Map<String, Object>> contents =
                    (List<Map<String, Object>>) request.get("contents");
            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) contents.get(0).get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return "";
        }
    }
}