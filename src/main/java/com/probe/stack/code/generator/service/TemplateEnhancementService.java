package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.config.CodeGeneratorConfig;
import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service to enhance generated projects with custom templates
 * Handles copying Java classes, merging properties, and POM configurations
 */
@Service
public class TemplateEnhancementService {

    private static final Logger log = LoggerFactory.getLogger(TemplateEnhancementService.class);

    private final CodeGeneratorConfig config;
    private final PomCustomizationService pomCustomizationService;
    private final PropertiesMergeService propertiesMergeService;
    private final TemplateProcessingService templateProcessingService;

    @Autowired
    public TemplateEnhancementService(CodeGeneratorConfig config,
                                       PomCustomizationService pomCustomizationService,
                                       PropertiesMergeService propertiesMergeService,
                                       TemplateProcessingService templateProcessingService) {
        this.config = config;
        this.pomCustomizationService = pomCustomizationService;
        this.propertiesMergeService = propertiesMergeService;
        this.templateProcessingService = templateProcessingService;
    }

    /**
     * Enhances generated project with all template customizations
     *
     * @param projectDir Generated project directory
     * @param request Code generation request
     * @return List of enhancement messages
     */
    public List<String> enhanceProject(Path projectDir, CodeGenerationRequest request) {
        log.info("Starting project enhancement with templates");
        log.info("Project directory: {}", projectDir);
        log.info("Template config directory: {}", config.getDirectories().getTemplateConfig());

        List<String> messages = new ArrayList<>();

        Path templateDir = Paths.get(config.getDirectories().getTemplateConfig());

        if (!Files.exists(templateDir)) {
            log.warn("Template directory not found: {}", templateDir);
            messages.add("Warning: Template directory not found, skipping enhancements");
            return messages;
        }

        try {
            // Step 1: Copy Java classes to base package (advice, config, util, etc.)
            copyJavaTemplates(projectDir, templateDir, request, messages);

            // Step 2: Merge application properties
            mergeApplicationProperties(projectDir, templateDir, messages);

            // Step 3: Merge POM configurations (NOW SUPPORTS maven_config structure)
            mergePomConfigurations(projectDir, templateDir, messages);

            // Step 4: Replace placeholders in copied templates
            replacePlaceholdersInEnhancements(projectDir, request, messages);

            log.info("Project enhancement completed successfully");

        } catch (IOException e) {
            log.error("Failed to enhance project with templates", e);
            throw new CodeGenerationException("Failed to enhance project with templates", e);
        }

        return messages;
    }

    /**
     * Copies Java template classes to generated project's base package
     *
     * @param projectDir Generated project directory
     * @param templateDir Template configuration directory
     * @param request Code generation request (contains base package info)
     * @param messages List to collect status messages
     * @throws IOException if file operations fail
     */
    private void copyJavaTemplates(Path projectDir, Path templateDir,
                                   CodeGenerationRequest request, List<String> messages) throws IOException {

        Path javaSourceTemplate = templateDir.resolve(config.getTemplates().getJavaSourceDir());

        if (!Files.exists(javaSourceTemplate)) {
            log.info("No Java template directory found at: {}", javaSourceTemplate);
            messages.add("No Java templates to copy");
            return;
        }

        // Extract base package from request
        String basePackage = extractBasePackage(request);

        if (basePackage == null || basePackage.trim().isEmpty()) {
            log.warn("Base package not found in request, using default copy location");
            // Fallback to old behavior if base package is not available
            Path projectJavaDir = projectDir.resolve("src/main/java");
            int fileCount = copyDirectory(javaSourceTemplate, projectJavaDir);

            if (fileCount > 0) {
                messages.add(String.format("Copied %d Java template file(s) to src/main/java (no base package)", fileCount));
            }
            return;
        }

        // Convert base package to path (e.g., com.probe.stack.api -> com/probe/stack/api)
        String basePackagePath = basePackage.replace('.', '/');

        // Build target path including base package
        Path projectJavaDir = projectDir.resolve("src/main/java").resolve(basePackagePath);

        log.info("Copying Java templates from: {}", javaSourceTemplate);
        log.info("Target base package: {}", basePackage);
        log.info("Target directory: {}", projectJavaDir);

        // Ensure target base package directory exists
        if (!Files.exists(projectJavaDir)) {
            Files.createDirectories(projectJavaDir);
            log.debug("Created base package directory: {}", projectJavaDir);
        }

        // Copy template files to base package location
        int fileCount = copyDirectory(javaSourceTemplate, projectJavaDir);

        if (fileCount > 0) {
            messages.add(String.format("Copied %d Java template file(s) to base package: %s", fileCount, basePackage));
            log.info("Successfully copied {} Java template files to base package", fileCount);
        }
        // UpdateBasePackage placeholder in the generated project.
        updateBasePackage(projectJavaDir,"${base_package}", basePackage);
    }

    /**
     * Extracts base package from CodeGenerationRequest
     *
     * Checks multiple possible locations:
     * 1. request.getBasePackage()
     * 2. request.getApiPackage()
     * 3. request.getModelPackage() (fallback)
     *
     * @param request Code generation request
     * @return Base package string, or null if not found
     */
    private String extractBasePackage(CodeGenerationRequest request) {
        if (request == null) {
            log.warn("CodeGenerationRequest is null");
            return null;
        }

        // Try direct base package getter
        if (StringUtils.hasText(request.getBasePackage())) {
            log.debug("Using basePackage: {}", request.getBasePackage());
            return request.getBasePackage();
        }

        log.warn("Could not extract base package from request");
        return null;
    }

    /**
     * Merges application properties from template
     * Merges from: codegen_config/java_code/src/main/resources/application.properties
     * Into: generated-project/src/main/resources/application.properties
     */
    private void mergeApplicationProperties(Path projectDir, Path templateDir,
                                            List<String> messages) throws IOException {

        Path resourcesTemplate = templateDir.resolve(config.getTemplates().getResourcesDir());
        Path templatePropertiesFile = resourcesTemplate.resolve("application.properties");

        if (!Files.exists(templatePropertiesFile)) {
            log.info("No template application.properties found at: {}", templatePropertiesFile);
            messages.add("No template properties to merge (using defaults)");
            return;
        }

        Path projectResourcesDir = projectDir.resolve("src/main/resources");
        Files.createDirectories(projectResourcesDir);

        Path projectPropertiesFile = propertiesMergeService.ensurePropertiesFileExists(projectResourcesDir);

        // Read template properties and merge
        String templateContent = Files.readString(templatePropertiesFile);
        String existingContent = Files.readString(projectPropertiesFile);

        // Simple merge: append template properties if not already present
        String mergedContent = mergePropertiesContent(existingContent, templateContent);
        Files.writeString(projectPropertiesFile, mergedContent);

        messages.add("Merged template application.properties");
        log.info("Successfully merged application properties from template");
    }

    /**
     * Merges POM configurations from template
     *
     * @param projectDir Generated project directory
     * @param templateDir Template configuration directory (codegen_config)
     * @param messages Status messages list
     */
    private void mergePomConfigurations(Path projectDir, Path templateDir,
                                        List<String> messages) {

        Path projectPomPath = projectDir.resolve("pom.xml");

        if (!Files.exists(projectPomPath)) {
            log.warn("Project pom.xml not found at: {}", projectPomPath);
            messages.add("Warning: Project pom.xml not found");
            return;
        }

        // Check if maven_config directory exists
        Path mavenConfigDir = templateDir.resolve(config.getTemplates().getPomTemplate());

        if (!Files.exists(mavenConfigDir)) {
            log.info("Maven config directory not found at: {}", mavenConfigDir);
            messages.add("No maven_config directory found (using defaults)");
            // PomCustomizationService will use default configuration
        } else if (!Files.isDirectory(mavenConfigDir)) {
            log.warn("Expected directory but found file at: {}", mavenConfigDir);
            messages.add("Warning: maven_config is not a directory");
            return;
        } else {
            log.info("Found maven_config directory at: {}", mavenConfigDir);

            // Log which configuration files exist
            Path dependenciesFile = mavenConfigDir.resolve("dependencies.xml");
            Path pluginsFile = mavenConfigDir.resolve("plugins.xml");
            Path propertiesFile = mavenConfigDir.resolve("properties.xml");

            log.info("Checking for configuration files:");
            log.info("  - dependencies.xml: {}", Files.exists(dependenciesFile) ? "FOUND" : "NOT FOUND");
            log.info("  - plugins.xml: {}", Files.exists(pluginsFile) ? "FOUND" : "NOT FOUND");
            log.info("  - properties.xml: {}", Files.exists(propertiesFile) ? "FOUND" : "NOT FOUND");
        }

        try {
            // Use PomCustomizationService to merge (it now handles the new structure)
            pomCustomizationService.mergePomCustomizations(projectPomPath, templateDir);

            messages.add("Merged POM configurations (dependencies, plugins, properties)");
            log.info("Successfully merged POM configurations from maven_config");

        } catch (Exception e) {
            log.error("Failed to merge POM configurations", e);
            messages.add("Error: Failed to merge POM configurations - " + e.getMessage());
            // Don't throw exception, allow other enhancements to continue
        }
    }

    /**
     * Replaces placeholders in copied template files
     */
    private void replacePlaceholdersInEnhancements(Path projectDir,
                                                   CodeGenerationRequest request,
                                                   List<String> messages) {

        // Replace placeholders in all copied files
        int filesProcessed = templateProcessingService.replacePlaceholders(projectDir, request);

        if (filesProcessed > 0) {
            messages.add(String.format("Updated placeholders in %d template file(s)", filesProcessed));
            log.info("Replaced placeholders in {} files", filesProcessed);
        }
    }

    /**
     * Copies directory recursively, preserving structure
     *
     * @param source Source directory to copy from
     * @param target Target directory to copy to
     * @return Number of files copied
     * @throws IOException if file operations fail
     */
    private int copyDirectory(Path source, Path target) throws IOException {
        final int[] fileCount = {0};

        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                fileCount[0]++;
                log.debug("Copied: {} -> {}", file.getFileName(), targetFile);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                log.error("Failed to copy file: {}", file, exc);
                return FileVisitResult.CONTINUE; // Continue copying other files
            }
        });

        return fileCount[0];
    }

    /**
     * Merges properties content, avoiding duplicates
     *
     * @param existing Existing properties content
     * @param template Template properties content to merge
     * @return Merged properties content
     */
    private String mergePropertiesContent(String existing, String template) {
        StringBuilder merged = new StringBuilder(existing);

        // Add separator if existing content doesn't end with newline
        if (!existing.endsWith("\n")) {
            merged.append("\n");
        }

        merged.append("\n# ===== Template Properties =====\n");

        // Parse template properties and only add non-existing ones
        String[] templateLines = template.split("\n");
        for (String line : templateLines) {
            String trimmed = line.trim();

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                merged.append(line).append("\n");
                continue;
            }

            // Extract property key
            int equalsIndex = trimmed.indexOf('=');
            if (equalsIndex > 0) {
                String key = trimmed.substring(0, equalsIndex).trim();

                // Only add if not already present
                if (!existing.contains(key + "=")) {
                    merged.append(line).append("\n");
                    log.debug("Added property: {}", key);
                } else {
                    log.debug("Skipped duplicate property: {}", key);
                }
            }
        }

        return merged.toString();
    }

    /**
     * Validates template directory structure
     *
     * @return true if valid structure exists
     */
    public boolean validateTemplateStructure() {
        Path templateDir = Paths.get(config.getDirectories().getTemplateConfig());

        if (!Files.exists(templateDir)) {
            log.warn("Template directory does not exist: {}", templateDir);
            return false;
        }

        boolean valid = true;

        // Check Java source directory
        Path javaDir = templateDir.resolve(config.getTemplates().getJavaSourceDir());
        if (!Files.exists(javaDir)) {
            log.info("Java template directory not found: {}", javaDir);
            valid = false;
        }

        // Check resources directory
        Path resourcesDir = templateDir.resolve(config.getTemplates().getResourcesDir());
        if (!Files.exists(resourcesDir)) {
            log.info("Resources template directory not found: {}", resourcesDir);
            valid = false;
        }

        // Check maven_config directory (new structure)
        Path mavenConfigDir = templateDir.resolve(config.getTemplates().getPomTemplate());
        if (!Files.exists(mavenConfigDir)) {
            log.info("Maven config directory not found: {}", mavenConfigDir);
            valid = false;
        } else if (!Files.isDirectory(mavenConfigDir)) {
            log.warn("Expected directory but found file at: {}", mavenConfigDir);
            valid = false;
        }

        return valid;
    }

    public void updateBasePackage(Path rootDir, String placeholder, String actualPackage) {
        try (Stream<Path> paths = Files.walk(rootDir)) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            String content = Files.readString(path);
                            if (content.contains(placeholder)) {
                                String updated = content.replace(placeholder, actualPackage);
                                Files.writeString(path, updated, StandardOpenOption.TRUNCATE_EXISTING);
                                log.info("Placeholder base package is updated in generated project: " + path);
                            }
                        } catch (IOException e) {
                            log.error("Failed to replace the base package place holder in generated project: " + path);
                        }
                    });
        } catch (IOException e) {
            log.error("updateBasePackage - Error walking directory: " + rootDir);
        }
    }
}