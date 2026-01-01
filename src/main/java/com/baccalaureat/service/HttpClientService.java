package com.baccalaureat.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client service for making external API requests.
 * Provides a simple interface for GET requests with proper error handling.
 */
public class HttpClientService {
    
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    
    /**
     * Makes a GET request to the specified URL and returns the response body.
     * 
     * @param url the URL to request
     * @return the response body as a string
     * @throws IOException if the request fails
     * @throws InterruptedException if the request is interrupted
     */
    public static String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("HTTP request failed with status: " + response.statusCode());
        }
        
        return response.body();
    }
    
    /**
     * Makes a GET request with custom timeout.
     * 
     * @param url the URL to request
     * @param timeoutSeconds timeout in seconds
     * @return the response body as a string
     * @throws IOException if the request fails
     * @throws InterruptedException if the request is interrupted
     */
    public static String get(String url, int timeoutSeconds) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .GET()
                .build();
        
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("HTTP request failed with status: " + response.statusCode());
        }
        
        return response.body();
    }
}