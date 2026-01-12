package com.baccalaureat.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;

/**
 * Utility class for loading images from resources in a JAR-safe way.
 * 
 * This class ensures that images are loaded correctly both during development
 * and when running from a JAR file.
 * 
 * Best practices:
 * - Always use absolute paths from the classpath root (e.g., "/com/baccalaureat/images/logo.png")
 * - Use getResourceAsStream() instead of direct file paths
 * - Never use absolute file system paths (e.g., "C:/Users/...")
 * - Place all images in src/main/resources/ directory
 */
public class ImageLoader {
    
    /**
     * Loads an image from the classpath resources
     * 
     * @param resourcePath Absolute path from classpath root (e.g., "/com/baccalaureat/images/logo.png")
     * @return The loaded Image, or null if loading fails
     */
    public static Image loadImage(String resourcePath) {
        try {
            InputStream stream = ImageLoader.class.getResourceAsStream(resourcePath);
            if (stream == null) {
                System.err.println("Warning: Image not found at path: " + resourcePath);
                return null;
            }
            return new Image(stream);
        } catch (Exception e) {
            System.err.println("Error loading image from " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Loads an image with specified dimensions from the classpath resources
     * 
     * @param resourcePath Absolute path from classpath root
     * @param width Requested width
     * @param height Requested height
     * @param preserveRatio Whether to preserve aspect ratio
     * @param smooth Whether to use smooth scaling
     * @return The loaded Image with specified dimensions, or null if loading fails
     */
    public static Image loadImage(String resourcePath, double width, double height, 
                                   boolean preserveRatio, boolean smooth) {
        try {
            InputStream stream = ImageLoader.class.getResourceAsStream(resourcePath);
            if (stream == null) {
                System.err.println("Warning: Image not found at path: " + resourcePath);
                return null;
            }
            return new Image(stream, width, height, preserveRatio, smooth);
        } catch (Exception e) {
            System.err.println("Error loading image from " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Creates an ImageView from a resource path
     * 
     * @param resourcePath Absolute path from classpath root
     * @return An ImageView containing the loaded image, or null if loading fails
     */
    public static ImageView loadImageView(String resourcePath) {
        Image image = loadImage(resourcePath);
        return image != null ? new ImageView(image) : null;
    }
    
    /**
     * Creates an ImageView with specified dimensions from a resource path
     * 
     * @param resourcePath Absolute path from classpath root
     * @param fitWidth Desired width of the ImageView
     * @param fitHeight Desired height of the ImageView
     * @param preserveRatio Whether to preserve aspect ratio
     * @return An ImageView containing the loaded image with specified dimensions
     */
    public static ImageView loadImageView(String resourcePath, double fitWidth, double fitHeight, 
                                          boolean preserveRatio) {
        ImageView imageView = loadImageView(resourcePath);
        if (imageView != null) {
            imageView.setFitWidth(fitWidth);
            imageView.setFitHeight(fitHeight);
            imageView.setPreserveRatio(preserveRatio);
            imageView.setSmooth(true);
        }
        return imageView;
    }
    
    /**
     * Checks if a resource exists at the given path
     * 
     * @param resourcePath Absolute path from classpath root
     * @return true if the resource exists, false otherwise
     */
    public static boolean resourceExists(String resourcePath) {
        return ImageLoader.class.getResource(resourcePath) != null;
    }
    
    /**
     * Example usage:
     * 
     * // Correct way to load images:
     * Image logo = ImageLoader.loadImage("/com/baccalaureat/images/logo.png");
     * ImageView iconView = ImageLoader.loadImageView("/com/baccalaureat/icons/settings.png", 32, 32, true);
     * 
     * // WRONG - Never use these patterns:
     * // new Image("C:/Users/myuser/images/icon.png")  // Absolute file system path
     * // new Image("images/icon.png")  // Relative path (won't work in JAR)
     * // new Image("file:///C:/path/to/file.png")  // File URL
     */
}
