package com.baccalaureat.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration loader utility for loading server settings from config.properties.
 * Provides safe loading of configuration with proper error handling and fallback values.
 */
public class ConfigLoader {
    
    private static final System.Logger logger = System.getLogger(ConfigLoader.class.getName());
    private static final String CONFIG_FILE = "/config.properties";
    
    // Default fallback values
    private static final String DEFAULT_WS_URL = "ws://localhost:8080/websocket";
    private static final String DEFAULT_API_URL = "http://localhost:8080/api/sessions";
    
    private static Properties properties;
    
    static {
        loadConfiguration();
    }
    
    /**
     * Loads configuration from config.properties file.
     * If loading fails, logs error and uses default values.
     */
    private static void loadConfiguration() {
        properties = new Properties();
        
        try (InputStream inputStream = ConfigLoader.class.getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                logger.log(System.Logger.Level.ERROR, 
                    "Configuration file not found: " + CONFIG_FILE + ". Using default values.");
                return;
            }
            
            properties.load(inputStream);
            logger.log(System.Logger.Level.INFO, 
                "Configuration loaded successfully from " + CONFIG_FILE);
            
        } catch (IOException e) {
            logger.log(System.Logger.Level.ERROR, 
                "Failed to load configuration from " + CONFIG_FILE + ": " + e.getMessage() + ". Using default values.", e);
        }
    }
    
    /**
     * Gets the WebSocket server URL.
     * @return WebSocket URL from config or default value
     */
    public static String getWebSocketUrl() {
        String url = properties.getProperty("server.ws.url", DEFAULT_WS_URL);
        logger.log(System.Logger.Level.DEBUG, "Using WebSocket URL: " + url);
        return url;
    }
    
    /**
     * Gets the REST API server URL.
     * @return API URL from config or default value
     */
    public static String getApiUrl() {
        String url = properties.getProperty("server.api.url", DEFAULT_API_URL);
        logger.log(System.Logger.Level.DEBUG, "Using API URL: " + url);
        return url;
    }
    
    /**
     * Gets a configuration property by key with fallback value.
     * @param key Property key
     * @param defaultValue Default value if property not found
     * @return Property value or default value
     */
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}