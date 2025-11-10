package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Service for handling multipart file upload requests
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultipartRequestService {

    private final ObjectMapper objectMapper;

    /**
     * Processes multipart request with file upload
     * Combines file content with request JSON
     *
     * @param file Uploaded OpenAPI specification file
     * @param requestJson JSON string containing request parameters
     * @return Complete CodeGenerationRequest with file content
     */
    public CodeGenerationRequest processMultipartRequest(MultipartFile file, String requestJson) {
        log.info("Processing multipart request - file: {}, size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        try {
            // Validate file
            validateFile(file);

            // Parse request JSON
            CodeGenerationRequest request = parseRequestJson(requestJson);

            // Read and set file content
            String specContent = readFileContent(file);
            request.setSpecContent(specContent);

            // Detect and set content type
            String contentType = detectContentType(file, specContent);
            request.setSpecContentType(contentType);

            log.info("Multipart request processed successfully - contentType: {}", contentType);

            return request;

        } catch (IOException e) {
            log.error("Failed to process multipart request", e);
            throw new CodeGenerationException("Failed to read uploaded file: " + e.getMessage(), e);
        }
    }

    /**
     * Validates uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new CodeGenerationException("Uploaded file is empty");
        }

        // Check file size (max 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new CodeGenerationException(
                    String.format("File size exceeds maximum allowed size of 10MB. File size: %d bytes",
                            file.getSize())
            );
        }

        // Validate file extension
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lowerFilename = filename.toLowerCase();
            if (!lowerFilename.endsWith(".json") &&
                    !lowerFilename.endsWith(".yaml") &&
                    !lowerFilename.endsWith(".yml")) {
                log.warn("File has non-standard extension: {}", filename);
            }
        }
    }

    /**
     * Parses request JSON string
     */
    private CodeGenerationRequest parseRequestJson(String requestJson) {
        try {
            return objectMapper.readValue(requestJson, CodeGenerationRequest.class);
        } catch (IOException e) {
            log.error("Failed to parse request JSON", e);
            throw new CodeGenerationException("Invalid request JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Reads file content as string
     */
    private String readFileContent(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Detects content type from file extension or content
     */
    private String detectContentType(MultipartFile file, String content) {
        String filename = file.getOriginalFilename();

        // Try to detect from filename
        if (filename != null) {
            String lowerFilename = filename.toLowerCase();
            if (lowerFilename.endsWith(".json")) {
                return "json";
            } else if (lowerFilename.endsWith(".yaml") || lowerFilename.endsWith(".yml")) {
                return "yaml";
            }
        }

        // Fallback to content-based detection
        String trimmed = content.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return "json";
        } else {
            return "yaml";
        }
    }
}