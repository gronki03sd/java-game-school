package com.baccalaureat.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * N8n webhook-based implementation of the AIClient interface.
 * Uses a simple HTTP POST to n8n workflow that returns deterministic true/false responses.
 */
public class N8nAIClient implements AIClient {
    
    private static final String N8N_WEBHOOK_URL = "https://gronki.app.n8n.cloud/webhook/chat";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public N8nAIClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
        
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public AIResponse query(String prompt) throws AIClientException {
        try {
            // Create the n8n webhook request payload
            N8nRequest request = new N8nRequest(prompt);
            
            // Serialize to JSON
            String requestBody = objectMapper.writeValueAsString(request);
            
            // Create HTTP request
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(N8N_WEBHOOK_URL))
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            
            // Send request and get response
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            // N8n should always return 200, but check for errors
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AIClientException(
                    "N8n webhook returned status: " + response.statusCode() + ", body: " + response.body(), 
                    getClientName(), 
                    AIClientException.ErrorType.API_ERROR
                );
            }
            
            // Parse the JSON response
            N8nResponse n8nResponse = objectMapper.readValue(response.body(), N8nResponse.class);
            
            // Convert n8n boolean response to AIResponse with confidence 1.0
            // N8n gives us deterministic true/false, so confidence is always 1.0
            double confidence = 1.0;
            String reasoning = "N8n webhook validation: " + (n8nResponse.valid ? "valid" : "invalid");
            
            return new AIResponse(n8nResponse.valid, confidence, reasoning);
            
        } catch (IOException e) {
            throw new AIClientException("Network error calling n8n webhook", e, getClientName(), AIClientException.ErrorType.NETWORK_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AIClientException("N8n webhook request was interrupted", e, getClientName(), AIClientException.ErrorType.TIMEOUT);
        } catch (Exception e) {
            throw new AIClientException("Unexpected error calling n8n webhook: " + e.getMessage(), e, getClientName(), AIClientException.ErrorType.UNKNOWN_ERROR);
        }
    }
    
    @Override
    public String getClientName() {
        return "N8n Webhook";
    }
    
    @Override
    public boolean isHealthy() {
        // Simple health check - just verify we can create HTTP client
        try {
            return httpClient != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    // JSON data classes for n8n webhook API
    
    /**
     * Request payload for n8n webhook
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class N8nRequest {
        @JsonProperty("chatInput")
        public String chatInput;
        
        public N8nRequest(String chatInput) {
            this.chatInput = chatInput;
        }
        
        // Default constructor for Jackson
        public N8nRequest() {}
    }
    
    /**
     * Response payload from n8n webhook
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class N8nResponse {
        @JsonProperty("valid")
        public boolean valid;
        
        // Default constructor for Jackson
        public N8nResponse() {}
    }
}