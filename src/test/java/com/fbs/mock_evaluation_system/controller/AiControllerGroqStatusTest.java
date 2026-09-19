package com.fbs.mock_evaluation_system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class AiControllerGroqStatusTest {

    @Test
    void groqSuccessStaysHttp200() {
        String body = "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}";
        ResponseEntity<String> response = AiController.fromGroqResponse(200, body);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(body, response.getBody());
    }

    @Test
    void groqClientErrorIsNotHttp200() {
        String body = "{\"error\":{\"message\":\"Invalid API Key\"}}";
        ResponseEntity<String> response = AiController.fromGroqResponse(401, body);
        assertEquals(401, response.getStatusCode().value());
        assertTrue(response.getStatusCode().isError());
        assertEquals(body, response.getBody());
    }

    @Test
    void groqNotFoundIsNotHttp200() {
        String body = "{\"error\":{\"message\":\"model does not exist\"}}";
        ResponseEntity<String> response = AiController.fromGroqResponse(404, body);
        assertEquals(404, response.getStatusCode().value());
        assertTrue(response.getStatusCode().isError());
        assertEquals(body, response.getBody());
    }
}
