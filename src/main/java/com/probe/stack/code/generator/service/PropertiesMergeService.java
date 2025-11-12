package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.config.CodeGeneratorConfig;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Service for merging application properties files
 */
@Service
public class PropertiesMergeService {

    private static final Logger log = LoggerFactory.getLogger(PropertiesMergeService.class);

    private final CodeGeneratorConfig config;

    @Autowired
    public PropertiesMergeService(CodeGeneratorConfig config) {
        this.config = config;
    }
    
    /**
     * Merges additional properties into application.properties
     *
     * @param propertiesPath Path to application.properties file
     */
    public void mergeApplicationProperties(Path propertiesPath) {
        log.info("Merging application properties: {}", propertiesPath);
        
        try {
            Properties properties = new Properties();
            
            // Load existing properties if file exists
            if (Files.exists(propertiesPath)) {
                try (FileInputStream fis = new FileInputStream(propertiesPath.toFile())) {
                    properties.load(fis);
                }
            }
            
            // Merge additional properties
            if (config.getApplicationProperties() != null) {
                for (Map.Entry<String, String> entry : config.getApplicationProperties().entrySet()) {
                    if (!properties.containsKey(entry.getKey())) {
                        properties.setProperty(entry.getKey(), entry.getValue());
                        log.debug("Added property: {} = {}", entry.getKey(), entry.getValue());
                    }
                }
            }
            
            // Write updated properties
            try (FileOutputStream fos = new FileOutputStream(propertiesPath.toFile())) {
                properties.store(fos, "Merged application properties");
            }
            
            log.info("Successfully merged application properties");
            
        } catch (IOException e) {
            throw new CodeGenerationException("Failed to merge application properties", e);
        }
    }
    
    /**
     * Creates application.properties if it doesn't exist
     *
     * @param resourcesDir Path to resources directory
     * @return Path to application.properties
     */
    public Path ensurePropertiesFileExists(Path resourcesDir) throws IOException {
        Path propertiesPath = resourcesDir.resolve("application.properties");
        
        if (!Files.exists(resourcesDir)) {
            Files.createDirectories(resourcesDir);
        }
        
        if (!Files.exists(propertiesPath)) {
            Files.createFile(propertiesPath);
            log.info("Created application.properties file");
        }
        
        return propertiesPath;
    }
}