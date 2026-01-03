package com.baccalaureat.ai;

/**
 * Mock AI client implementation for testing purposes.
 * Provides deterministic responses based on simple rules.
 */
public class MockAIClient implements AIClient {
    
    private final String clientName;
    private final boolean healthy;
    private final AIResponse defaultResponse;
    
    /**
     * Create a mock AI client that always returns the same response.
     * 
     * @param clientName name for this mock client
     * @param defaultResponse the response to always return
     */
    public MockAIClient(String clientName, AIResponse defaultResponse) {
        this(clientName, true, defaultResponse);
    }
    
    /**
     * Create a mock AI client with configurable health status.
     * 
     * @param clientName name for this mock client
     * @param healthy whether this client should report as healthy
     * @param defaultResponse the response to return (if healthy)
     */
    public MockAIClient(String clientName, boolean healthy, AIResponse defaultResponse) {
        this.clientName = clientName != null ? clientName : "MockAI";
        this.healthy = healthy;
        this.defaultResponse = defaultResponse;
    }
    
    @Override
    public AIResponse query(String prompt) throws AIClientException {
        if (!healthy) {
            throw new AIClientException(
                "Mock AI client is not healthy", 
                clientName, 
                AIClientException.ErrorType.NETWORK_ERROR
            );
        }
        
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new AIClientException(
                "Empty prompt provided", 
                clientName, 
                AIClientException.ErrorType.API_ERROR
            );
        }
        
        // Simple mock logic: if default response is null, create a basic one
        if (defaultResponse == null) {
            // Extract word from prompt (very simple parsing)
            boolean seemsValid = prompt.toLowerCase().contains("valid");
            double confidence = seemsValid ? 0.8 : 0.3;
            return new AIResponse(seemsValid, confidence, "Mock response");
        }
        
        return defaultResponse;
    }
    
    @Override
    public String getClientName() {
        return clientName;
    }
    
    @Override
    public boolean isHealthy() {
        return healthy;
    }
    
    /**
     * Create a mock client that simulates high confidence validation.
     * 
     * @param valid whether words should be considered valid
     * @return MockAIClient instance
     */
    public static MockAIClient createHighConfidenceMock(boolean valid) {
        return new MockAIClient(
            "HighConfidenceMock", 
            new AIResponse(valid, 0.9, "High confidence mock response")
        );
    }
    
    /**
     * Create a mock client that simulates low confidence validation.
     * 
     * @param valid whether words should be considered valid
     * @return MockAIClient instance
     */
    public static MockAIClient createLowConfidenceMock(boolean valid) {
        return new MockAIClient(
            "LowConfidenceMock", 
            new AIResponse(valid, 0.4, "Low confidence mock response")
        );
    }
    
    /**
     * Create a mock client that always throws exceptions.
     * 
     * @return MockAIClient instance that simulates failures
     */
    public static MockAIClient createFailingMock() {
        return new MockAIClient("FailingMock", false, null);
    }
}