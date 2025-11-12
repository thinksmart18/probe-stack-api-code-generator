package com.probe.stack.code.generator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Utility class for code generation operations
 */
public class CodeGenerationUtils {

    private static final Logger log = LoggerFactory.getLogger(CodeGenerationUtils.class);

    private static final Pattern PACKAGE_PATTERN =
        Pattern.compile("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$");

    // Private constructor to prevent instantiation
    private CodeGenerationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    private static final Pattern ARTIFACT_PATTERN = 
        Pattern.compile("^[a-z][a-z0-9-]*$");
    
    /**
     * Validates a Java package name
     *
     * @param packageName Package name to validate
     * @return true if valid
     */
    public static boolean isValidPackageName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        
        // Check against reserved keywords
        String[] parts = packageName.split("\\.");
        for (String part : parts) {
            if (isJavaKeyword(part)) {
                log.warn("Package contains Java keyword: {}", part);
                return false;
            }
        }
        
        return PACKAGE_PATTERN.matcher(packageName).matches();
    }
    
    /**
     * Validates an artifact ID
     *
     * @param artifactId Artifact ID to validate
     * @return true if valid
     */
    public static boolean isValidArtifactId(String artifactId) {
        if (artifactId == null || artifactId.isEmpty()) {
            return false;
        }
        return ARTIFACT_PATTERN.matcher(artifactId).matches();
    }
    
    /**
     * Converts package name to directory path
     *
     * @param packageName Package name (e.g., com.example.service)
     * @return Directory path (e.g., com/example/service)
     */
    public static String packageToPath(String packageName) {
        return packageName.replace('.', '/');
    }
    
    /**
     * Converts directory path to package name
     *
     * @param path Directory path (e.g., com/example/service)
     * @return Package name (e.g., com.example.service)
     */
    public static String pathToPackage(String path) {
        return path.replace('/', '.');
    }
    
    /**
     * Converts hyphenated string to camel case
     *
     * @param input Hyphenated string (e.g., my-service-name)
     * @return Camel case string (e.g., myServiceName)
     */
    public static String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String[] parts = input.split("-");
        StringBuilder result = new StringBuilder(parts[0].toLowerCase());
        
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(Character.toUpperCase(parts[i].charAt(0)))
                      .append(parts[i].substring(1).toLowerCase());
            }
        }
        
        return result.toString();
    }
    
    /**
     * Converts hyphenated string to Pascal case
     *
     * @param input Hyphenated string (e.g., my-service-name)
     * @return Pascal case string (e.g., MyServiceName)
     */
    public static String toPascalCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String camelCase = toCamelCase(input);
        return Character.toUpperCase(camelCase.charAt(0)) + camelCase.substring(1);
    }
    
    /**
     * Sanitizes a file name by removing invalid characters
     *
     * @param fileName File name to sanitize
     * @return Sanitized file name
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        
        // Remove or replace invalid characters
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    
    /**
     * Generates a unique project identifier
     *
     * @param groupId Group ID
     * @param artifactId Artifact ID
     * @return Unique identifier
     */
    public static String generateProjectId(String groupId, String artifactId) {
        return String.format("%s_%s_%d", 
            groupId.replace('.', '_'), 
            artifactId, 
            System.currentTimeMillis()
        );
    }
    
    /**
     * Checks if a string is a Java reserved keyword
     *
     * @param word Word to check
     * @return true if reserved keyword
     */
    private static boolean isJavaKeyword(String word) {
        String[] keywords = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while"
        };
        
        for (String keyword : keywords) {
            if (keyword.equals(word)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Estimates the size of generated project in MB
     *
     * @param numberOfEndpoints Number of API endpoints
     * @return Estimated size in MB
     */
    public static double estimateProjectSize(int numberOfEndpoints) {
        // Base project: ~5MB
        // Each endpoint adds approximately 50KB
        double baseSize = 5.0;
        double perEndpointSize = 0.05;
        
        return baseSize + (numberOfEndpoints * perEndpointSize);
    }
    
    /**
     * Validates URL format
     *
     * @param url URL to validate
     * @return true if valid URL
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        try {
            new java.net.URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}