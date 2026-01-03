package com.baccalaureat.ai;

/**
 * Exception thrown when AI client operations fail.
 * This can include network timeouts, API errors, parsing failures, etc.
 */
public class AIClientException extends Exception {
    
    private final String clientName;
    private final ErrorType errorType;
    
    public enum ErrorType {
        NETWORK_ERROR,
        TIMEOUT,
        API_ERROR,
        PARSING_ERROR,
        AUTHENTICATION_ERROR,
        RATE_LIMIT_EXCEEDED,
        UNKNOWN_ERROR
    }
    
    public AIClientException(String message, String clientName, ErrorType errorType) {
        super(message);
        this.clientName = clientName;
        this.errorType = errorType;
    }
    
    public AIClientException(String message, Throwable cause, String clientName, ErrorType errorType) {
        super(message, cause);
        this.clientName = clientName;
        this.errorType = errorType;
    }
    
    /**
     * @return the name of the AI client that threw this exception
     */
    public String getClientName() {
        return clientName;
    }
    
    /**
     * @return the type of error that occurred
     */
    public ErrorType getErrorType() {
        return errorType;
    }
    
    @Override
    public String toString() {
        return String.format("AIClientException{client='%s', type=%s, message='%s'}", 
            clientName, errorType, getMessage());
    }
}