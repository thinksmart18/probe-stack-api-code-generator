package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Service for downloading/processing OpenAPI specifications
 * Supports URL, raw content, or file upload
 */
@Service
public class SpecificationDownloadService {

    private static final Logger log = LoggerFactory.getLogger(SpecificationDownloadService.class);

    /**
     * Gets OpenAPI specification and saves it to a file
     * Handles URL download or raw content
     *
     * @param request Code generation request
     * @param outputPath Path where to save the specification
     * @return Path to the specification file
     */
    public Path getSpecification(CodeGenerationRequest request, Path outputPath) {
        // Check if raw content is provided
        if (request.getSpecContent() != null && !request.getSpecContent().isEmpty()) {
            return saveRawContent(request.getSpecContent(), request.getSpecContentType(), outputPath);
        }

        // Otherwise, download from URL
        if (request.getOpenApiSpecUrl() != null && !request.getOpenApiSpecUrl().isEmpty()) {
            return downloadSpecification(request.getOpenApiSpecUrl(), request.getGithubToken(), outputPath);
        }

        throw new CodeGenerationException("Either openApiSpecUrl or specContent must be provided");
    }

    /**
     * Saves raw specification content to file
     *
     * @param content Raw specification content
     * @param contentType Type of content (json or yaml)
     * @param outputPath Path where to save the file
     * @return Path to the saved file
     */
    private Path saveRawContent(String content, String contentType, Path outputPath) {
        log.info("Saving raw specification content to: {}", outputPath);

        try {
            // Adjust file extension based on content type
            if (contentType != null && contentType.equalsIgnoreCase("json")) {
                outputPath = outputPath.resolveSibling(
                        outputPath.getFileName().toString().replace(".yaml", ".json")
                );
            }

            Files.writeString(outputPath, content, StandardCharsets.UTF_8);
            log.info("Successfully saved specification content to: {}", outputPath);
            return outputPath;

        } catch (IOException e) {
            throw new CodeGenerationException("Failed to save specification content", e);
        }
    }

    /**
     * Downloads OpenAPI specification from URL
     *
     * @param specUrl URL to the specification
     * @param githubToken Optional GitHub token for private repos
     * @param outputPath Path where to save the downloaded file
     * @return Path to the downloaded file
     */
    public Path downloadSpecification(String specUrl, String githubToken, Path outputPath) {
        log.info("Downloading OpenAPI specification from: {}", specUrl);

        try {
            URL url = new URL(specUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set headers
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            // Add GitHub token if provided
            if (githubToken != null && !githubToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "token " + githubToken);
            }

            // Set User-Agent
            connection.setRequestProperty("User-Agent", "OpenAPI-Code-Generator/1.0");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream()) {
                    Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Successfully downloaded specification to: {}", outputPath);
                    return outputPath;
                }
            } else {
                throw new CodeGenerationException(
                        String.format("Failed to download specification. HTTP Status: %d", responseCode)
                );
            }

        } catch (IOException e) {
            throw new CodeGenerationException(
                    "Failed to download OpenAPI specification from: " + specUrl, e
            );
        }
    }

    /**
     * Validates that the specification is a valid OpenAPI spec
     *
     * @param specPath Path to the specification file
     * @return true if valid
     */
    public boolean validateSpecification(Path specPath) {
        try {
            String content = Files.readString(specPath);

            // Basic validation - check for OpenAPI markers
            boolean isValid = content.contains("openapi:") ||
                    content.contains("\"openapi\"") ||
                    content.contains("swagger:") ||
                    content.contains("\"swagger\"");

            if (!isValid) {
                log.warn("File does not appear to be a valid OpenAPI specification");
            }

            return isValid;

        } catch (IOException e) {
            log.error("Failed to validate specification", e);
            return false;
        }
    }

    /**
     * Auto-detects specification format (JSON or YAML)
     *
     * @param specPath Path to the specification file
     * @return "json" or "yaml"
     */
    public String detectSpecFormat(Path specPath) {
        try {
            String content = Files.readString(specPath);
            String trimmed = content.trim();

            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                return "json";
            } else {
                return "yaml";
            }

        } catch (IOException e) {
            log.warn("Failed to detect spec format, defaulting to yaml", e);
            return "yaml";
        }
    }
}