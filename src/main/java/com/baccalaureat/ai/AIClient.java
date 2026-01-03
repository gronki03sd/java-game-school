package com.baccalaureat.ai;

/**
 * Abstract interface for AI client implementations.
 * Allows different AI providers (OpenAI, Hugging Face, local models, etc.)
 * to be used interchangeably in the validation pipeline.
 */
public interface AIClient {
    
    /**
     * Query the AI model with a prompt and get a response.
     * 
     * @param prompt The question/prompt to send to the AI
     * @return AIResponse containing validation result and confidence
     * @throws AIClientException if the AI query fails, times out, or returns invalid data
     */
    AIResponse query(String prompt) throws AIClientException;
    
    /**
     * Get the name/identifier of this AI client implementation.
     * Useful for logging and debugging.
     * 
     * @return String identifier for this AI client
     */
    String getClientName();
    
    /**
     * Check if the AI client is currently available/healthy.
     * Can be used to implement circuit breaker patterns.
     * 
     * @return true if the client is ready to accept queries
     */
    boolean isHealthy();
}