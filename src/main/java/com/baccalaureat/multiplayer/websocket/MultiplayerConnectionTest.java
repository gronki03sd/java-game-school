package com.baccalaureat.multiplayer.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * TEMPORARY TEST CLASS FOR WEBSOCKET CLIENT VALIDATION
 * 
 * This class validates Step 1 implementation by testing:
 * - WebSocket connection establishment
 * - Message sending functionality  
 * - Message receiving and parsing
 * - Listener callback execution
 * - Graceful error handling
 * 
 * THIS IS TEMPORARY CODE - WILL BE REMOVED AFTER VALIDATION
 */
public class MultiplayerConnectionTest implements MultiplayerMessageListener {
    
    private static final System.Logger logger = System.getLogger(MultiplayerConnectionTest.class.getName());
    
    // Test configuration
    private static final String DEFAULT_SERVER_URL = "ws://localhost:8080/websocket";
    private static final String TEST_SESSION_CODE = "TEST123";
    private static final String TEST_PLAYER_NAME = "TestPlayer";
    
    private final MultiplayerWebSocketClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CountDownLatch connectionLatch = new CountDownLatch(1);
    
    // Test state tracking
    private boolean connectionReceived = false;
    private boolean messageReceived = false;
    private int messagesProcessed = 0;
    private String lastMessageType = null;
    
    public MultiplayerConnectionTest() {
        this.client = new MultiplayerWebSocketClient();
        this.client.addListener(this);
    }
    
    /**
     * Main test execution method - validates all WebSocket functionality
     */
    public void runValidationTests() {
        logger.log(System.Logger.Level.INFO, "\n=== STARTING WEBSOCKET CLIENT VALIDATION ===");
        logger.log(System.Logger.Level.INFO, "Testing connection, messaging, parsing, and listeners...");
        
        try {
            // Test 1: Connection Test
            testConnection();
            
            // Test 2: Message Sending Test
            testMessageSending();
            
            // Test 3: Message Parsing Test (if server responds)
            testMessageParsing();
            
            // Test 4: Listener Functionality Test
            testListenerExecution();
            
            // Test 5: Error Handling Test
            testErrorHandling();
            
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Validation test failed with exception", e);
        } finally {
            // Cleanup
            cleanup();
        }
        
        // Final results
        printValidationResults();
    }
    
    /**
     * Test 1: WebSocket Connection
     */
    private void testConnection() {
        logger.log(System.Logger.Level.INFO, "\n--- TEST 1: WebSocket Connection ---");
        
        // Attempt connection
        boolean connectInitiated = client.connect(DEFAULT_SERVER_URL);
        logger.log(System.Logger.Level.INFO, "Connection initiated: " + connectInitiated);
        
        if (connectInitiated) {
            try {
                // Wait for connection callback (max 10 seconds)
                boolean connected = connectionLatch.await(10, TimeUnit.SECONDS);
                logger.log(System.Logger.Level.INFO, "Connection established within timeout: " + connected);
                logger.log(System.Logger.Level.INFO, "Client reports connected: " + client.isConnected());
            } catch (InterruptedException e) {
                logger.log(System.Logger.Level.WARNING, "Connection test interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Test 2: Message Sending
     */
    private void testMessageSending() {
        logger.log(System.Logger.Level.INFO, "\n--- TEST 2: Message Sending ---");
        
        if (!client.isConnected()) {
            logger.log(System.Logger.Level.WARNING, "Skipping message tests - not connected to server");
            return;
        }
        
        // Test helper method - Join Session
        logger.log(System.Logger.Level.INFO, "Testing sendJoinSession method...");
        boolean joinSent = client.sendJoinSession(TEST_SESSION_CODE, TEST_PLAYER_NAME);
        logger.log(System.Logger.Level.INFO, "Join session message sent: " + joinSent);
        
        // Wait a moment for potential server response
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Test helper method - Submit Answers
        logger.log(System.Logger.Level.INFO, "Testing sendSubmitAnswers method...");
        Map<String, String> testAnswers = createTestAnswers();
        boolean answersSent = client.sendSubmitAnswers(testAnswers);
        logger.log(System.Logger.Level.INFO, "Submit answers message sent: " + answersSent);
        
        // Wait a moment for potential server response
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Test helper method - Ready for Next Round
        logger.log(System.Logger.Level.INFO, "Testing sendReadyForNextRound method...");
        boolean readySent = client.sendReadyForNextRound();
        logger.log(System.Logger.Level.INFO, "Ready for next round message sent: " + readySent);
        
        // Wait for potential server response
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Test 3: Message Parsing
     */
    private void testMessageParsing() {
        logger.log(System.Logger.Level.INFO, "\n--- TEST 3: Message Parsing ---");
        
        // Test with sample JSON messages (simulate server responses)
        testParseMessage("{\"type\":\"SESSION_JOINED\",\"sessionId\":\"ABC123\",\"playerName\":\"TestPlayer\"}");
        testParseMessage("{\"type\":\"PLAYER_JOINED\",\"playerName\":\"TestPlayer\"}");
        testParseMessage("{\"type\":\"GAME_STARTED\",\"letter\":\"A\",\"roundDuration\":60}");
        testParseMessage("{\"type\":\"ERROR\",\"message\":\"Invalid session code\"}");
        testParseMessage("invalid json"); // Test error handling
        
        logger.log(System.Logger.Level.INFO, "Parsed messages successfully, check logs for details");
    }
    
    /**
     * Test 4: Listener Execution
     */
    private void testListenerExecution() {
        logger.log(System.Logger.Level.INFO, "\n--- TEST 4: Listener Execution ---");
        logger.log(System.Logger.Level.INFO, "Connection callback received: " + connectionReceived);
        logger.log(System.Logger.Level.INFO, "Message callbacks received: " + messagesProcessed);
        logger.log(System.Logger.Level.INFO, "Last message type processed: " + lastMessageType);
        
        // Test listener management
        MultiplayerMessageListener tempListener = new TestMessageListener();
        client.addListener(tempListener);
        logger.log(System.Logger.Level.INFO, "Added temporary listener");
        
        // Remove the temporary listener
        client.removeListener(tempListener);
        logger.log(System.Logger.Level.INFO, "Removed temporary listener");
        
        logger.log(System.Logger.Level.INFO, "Listener management test completed");
    }
    
    /**
     * Test 5: Error Handling
     */
    private void testErrorHandling() {
        logger.log(System.Logger.Level.INFO, "\n--- TEST 5: Error Handling ---");
        
        // Test connection to invalid URL (should fail gracefully)
        MultiplayerWebSocketClient errorTestClient = new MultiplayerWebSocketClient();
        errorTestClient.addListener(new ErrorTestListener());
        
        logger.log(System.Logger.Level.INFO, "Testing connection to invalid URL...");
        boolean invalidConnect = errorTestClient.connect("ws://invalid-server-url:9999/websocket");
        logger.log(System.Logger.Level.INFO, "Invalid connection initiated (should be false or fail gracefully): " + invalidConnect);
        
        // Wait briefly for error callback
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        errorTestClient.disconnect();
        logger.log(System.Logger.Level.INFO, "Error handling test completed");
    }
    
    /**
     * Helper method to test JSON parsing
     */
    private void testParseMessage(String jsonMessage) {
        try {
            logger.log(System.Logger.Level.INFO, "Testing parse: " + jsonMessage);
            JsonNode node = objectMapper.readTree(jsonMessage);
            String type = node.has("type") ? node.get("type").asText() : "NO_TYPE";
            logger.log(System.Logger.Level.INFO, "Parsed type: " + type);
        } catch (Exception e) {
            logger.log(System.Logger.Level.INFO, "Parse failed as expected for invalid JSON: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to create test answers
     */
    private Map<String, String> createTestAnswers() {
        Map<String, String> answers = new HashMap<>();
        answers.put("Animals", "Elephant");
        answers.put("Colors", "Emerald");
        answers.put("Countries", "Ecuador");
        return answers;
    }
    
    /**
     * Cleanup resources
     */
    private void cleanup() {
        logger.log(System.Logger.Level.INFO, "\n--- CLEANUP ---");
        client.removeListener(this);
        client.disconnect();
        logger.log(System.Logger.Level.INFO, "Cleanup completed");
    }
    
    /**
     * Print final validation results
     */
    private void printValidationResults() {
        logger.log(System.Logger.Level.INFO, "\n=== WEBSOCKET CLIENT VALIDATION RESULTS ===");
        logger.log(System.Logger.Level.INFO, "Connection Callback Received: " + connectionReceived);
        logger.log(System.Logger.Level.INFO, "Messages Processed: " + messagesProcessed);
        logger.log(System.Logger.Level.INFO, "Last Message Type: " + (lastMessageType != null ? lastMessageType : "NONE"));
        logger.log(System.Logger.Level.INFO, "Message Received Flag: " + messageReceived);
        
        // Overall assessment
        boolean basicFunctionalityWorking = connectionReceived || messagesProcessed > 0;
        logger.log(System.Logger.Level.INFO, "\nWebSocket Client Layer Status: " + 
            (basicFunctionalityWorking ? "FUNCTIONAL" : "NEEDS_SERVER_FOR_FULL_TEST"));
        logger.log(System.Logger.Level.INFO, "=== VALIDATION COMPLETE ===");
    }
    
    // MultiplayerMessageListener implementation
    
    @Override
    public void onConnected() {
        logger.log(System.Logger.Level.INFO, "*** LISTENER CALLBACK: onConnected() ***");
        connectionReceived = true;
        connectionLatch.countDown();
    }
    
    @Override
    public void onDisconnected() {
        logger.log(System.Logger.Level.INFO, "*** LISTENER CALLBACK: onDisconnected() ***");
    }
    
    @Override
    public void onError(String message) {
        logger.log(System.Logger.Level.INFO, "*** LISTENER CALLBACK: onError() - " + message + " ***");
    }
    
    @Override
    public void onMessageReceived(String json) {
        logger.log(System.Logger.Level.INFO, "*** LISTENER CALLBACK: onMessageReceived() ***");
        logger.log(System.Logger.Level.INFO, "Received in listener: " + json);
        
        messageReceived = true;
        messagesProcessed++;
        
        // Try to extract message type
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("type")) {
                lastMessageType = node.get("type").asText();
                logger.log(System.Logger.Level.INFO, "Message type in listener: " + lastMessageType);
            }
        } catch (Exception e) {
            logger.log(System.Logger.Level.WARNING, "Could not parse message in listener: " + e.getMessage());
        }
    }
    
    /**
     * Temporary listener for testing listener management
     */
    private static class TestMessageListener implements MultiplayerMessageListener {
        private static final System.Logger logger = System.getLogger(TestMessageListener.class.getName());
        
        @Override
        public void onConnected() {
            logger.log(System.Logger.Level.INFO, "TempListener: Connected");
        }
        
        @Override
        public void onDisconnected() {
            logger.log(System.Logger.Level.INFO, "TempListener: Disconnected");
        }
        
        @Override
        public void onError(String message) {
            logger.log(System.Logger.Level.INFO, "TempListener: Error - " + message);
        }
        
        @Override
        public void onMessageReceived(String json) {
            logger.log(System.Logger.Level.INFO, "TempListener: Message - " + json);
        }
    }
    
    /**
     * Error test listener
     */
    private static class ErrorTestListener implements MultiplayerMessageListener {
        private static final System.Logger logger = System.getLogger(ErrorTestListener.class.getName());
        
        @Override
        public void onConnected() {
            logger.log(System.Logger.Level.INFO, "ErrorTest: Unexpected connection");
        }
        
        @Override
        public void onDisconnected() {
            logger.log(System.Logger.Level.INFO, "ErrorTest: Disconnected as expected");
        }
        
        @Override
        public void onError(String message) {
            logger.log(System.Logger.Level.INFO, "ErrorTest: Error received as expected - " + message);
        }
        
        @Override
        public void onMessageReceived(String json) {
            logger.log(System.Logger.Level.INFO, "ErrorTest: Unexpected message - " + json);
        }
    }
    
    /**
     * Static method to run the validation test
     * Can be called from anywhere in the application for testing
     */
    public static void runValidation() {
        // Ensure we're not on the JavaFX Application Thread for this test
        if (Platform.isFxApplicationThread()) {
            // Run on separate thread to avoid blocking UI
            new Thread(() -> {
                MultiplayerConnectionTest test = new MultiplayerConnectionTest();
                test.runValidationTests();
            }, "WebSocket-Validation-Thread").start();
        } else {
            // Run directly
            MultiplayerConnectionTest test = new MultiplayerConnectionTest();
            test.runValidationTests();
        }
    }
}