package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for processing templates and replacing placeholders
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateProcessingService {
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    /**
     * Replaces placeholders in all files within the project directory
     *
     * @param projectDir Project directory
     * @param request Code generation request containing replacement values
     * @return Number of files processed
     */
    public int replacePlaceholders(Path projectDir, CodeGenerationRequest request) {
        log.info("Replacing placeholders in project files");
        
        Map<String, String> replacements = buildReplacementMap(request);
        int filesProcessed = 0;
        
        try {
            Files.walkFileTree(projectDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (isTextFile(file)) {
                        processFile(file, replacements);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            
            filesProcessed = countProcessedFiles(projectDir);
            log.info("Processed {} files for placeholder replacement", filesProcessed);
            
        } catch (IOException e) {
            throw new CodeGenerationException("Failed to replace placeholders", e);
        }
        
        return filesProcessed;
    }
    
    /**
     * Updates package declarations in Java files
     *
     * @param projectDir Project directory
     * @param oldPackage Old package name
     * @param newPackage New package name
     */
    public void updatePackageDeclarations(Path projectDir, String oldPackage, String newPackage) {
        log.info("Updating package declarations from {} to {}", oldPackage, newPackage);
        
        try {
            Files.walkFileTree(projectDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".java")) {
                        String content = Files.readString(file, StandardCharsets.UTF_8);
                        String updated = content.replaceAll(
                            "package " + Pattern.quote(oldPackage),
                            "package " + newPackage
                        );
                        
                        if (!content.equals(updated)) {
                            Files.writeString(file, updated, StandardCharsets.UTF_8);
                            log.debug("Updated package in: {}", file.getFileName());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            
        } catch (IOException e) {
            log.error("Failed to update package declarations", e);
        }
    }
    
    /**
     * Builds a map of placeholder keys to replacement values
     */
    private Map<String, String> buildReplacementMap(CodeGenerationRequest request) {
        Map<String, String> replacements = new HashMap<>();
        
        replacements.put("basePackage", request.getBasePackage());
        replacements.put("groupId", request.getGroupName());
        replacements.put("artifactId", request.getArtifactId());
        replacements.put("version", request.getVersion());
        replacements.put("projectName", toTitleCase(request.getArtifactId()));
        replacements.put("apiPackage", request.getBasePackage() + ".api");
        replacements.put("modelPackage", request.getBasePackage() + ".model");
        replacements.put("servicePackage", request.getBasePackage() + ".service");
        replacements.put("configPackage", request.getBasePackage() + ".config");
        
        // Add package path (dots replaced with slashes)
        replacements.put("basePackagePath", request.getBasePackage().replace(".", "/"));
        
        return replacements;
    }
    
    /**
     * Processes a single file, replacing placeholders
     */
    private void processFile(Path file, Map<String, String> replacements) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        String originalContent = content;
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = replacements.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        if (!originalContent.equals(sb.toString())) {
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
            log.debug("Replaced placeholders in: {}", file.getFileName());
        }
    }
    
    /**
     * Checks if a file is a text file that should be processed
     */
    private boolean isTextFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        
        return fileName.endsWith(".java") ||
               fileName.endsWith(".xml") ||
               fileName.endsWith(".properties") ||
               fileName.endsWith(".yml") ||
               fileName.endsWith(".yaml") ||
               fileName.endsWith(".md") ||
               fileName.endsWith(".txt") ||
               fileName.equals("dockerfile") ||
               fileName.equals(".openapi-generator-ignore");
    }
    
    /**
     * Counts files in directory
     */
    private int countProcessedFiles(Path directory) throws IOException {
        return (int) Files.walk(directory)
                .filter(Files::isRegularFile)
                .filter(this::isTextFile)
                .count();
    }
    
    /**
     * Converts hyphenated string to title case
     */
    private String toTitleCase(String input) {
        String[] words = input.split("-");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase());
            }
        }
        
        return result.toString();
    }
}