package com.probe.stack.code.generator.controller;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.dto.CodeGenerationResponse;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import com.probe.stack.code.generator.service.CodeGenerationService;
import com.probe.stack.code.generator.service.DownloadService;
import com.probe.stack.code.generator.service.MultipartRequestService;
import com.probe.stack.code.generator.service.RequestValidationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * REST controller for code generation operations
 * Thin controller - all business logic delegated to service layer
 */
@RestController
@RequestMapping("/api/v1/codegen")
public class CodeGenerationController {

    private static final Logger log = LoggerFactory.getLogger(CodeGenerationController.class);

    private final CodeGenerationService codeGenerationService;
    private final MultipartRequestService multipartRequestService;
    private final RequestValidationService validationService;
    private final DownloadService downloadService;

    @Autowired
    public CodeGenerationController(CodeGenerationService codeGenerationService,
                                   MultipartRequestService multipartRequestService,
                                   RequestValidationService validationService,
                                   DownloadService downloadService) {
        this.codeGenerationService = codeGenerationService;
        this.multipartRequestService = multipartRequestService;
        this.validationService = validationService;
        this.downloadService = downloadService;
    }

    /**
     * Generate Spring Boot project from OpenAPI specification (JSON payload)
     * Supports URL or raw content in JSON body
     *
     * @param request Code generation request
     * @return Generation response with project details
     */
    @PostMapping("/generate")
    public ResponseEntity<CodeGenerationResponse> generateCode(
            @Valid @RequestBody CodeGenerationRequest request) {

        log.info("Received code generation request for artifact: {}", request.getArtifactId());

        try {
            validationService.validateRequest(request);
            CodeGenerationResponse response = codeGenerationService.generateProject(request);
            HttpStatus status = determineHttpStatus(response);

            return ResponseEntity.status(status).body(response);

        } catch (CodeGenerationException e) {
            log.error("Code generation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Generate Spring Boot project from OpenAPI specification (File Upload)
     * Accepts multipart form data with spec file
     *
     * @param file OpenAPI specification file (YAML or JSON)
     * @param requestJson JSON string containing other request parameters
     * @return Generation response with project details
     */
    @PostMapping(value = "/generate/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CodeGenerationResponse> generateCodeFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("request") String requestJson) {

        log.info("Received file upload request - file: {}, size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        try {
            CodeGenerationRequest request = multipartRequestService.processMultipartRequest(file, requestJson);
            validationService.validateRequest(request);
            CodeGenerationResponse response = codeGenerationService.generateProject(request);
            HttpStatus status = determineHttpStatus(response);

            return ResponseEntity.status(status).body(response);

        } catch (CodeGenerationException e) {
            log.error("File upload code generation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Download generated project as ZIP archive
     *
     * @param generationId Generation ID
     * @return ZIP file resource
     */
    @GetMapping("/download/{generationId}")
    public ResponseEntity<Resource> downloadProject(@PathVariable String generationId) {

        log.info("Download request for generation ID: {}", generationId);

        try {
            return downloadService.getGeneratedProjectArchive(generationId)
                    .map(resource -> buildDownloadResponse(resource))
                    .orElseGet(() -> {
                        log.warn("Archive not found for generation ID: {}", generationId);
                        return ResponseEntity.notFound().build();
                    });

        } catch (CodeGenerationException e) {
            log.error("Failed to download project for generation ID: {}", generationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Determines HTTP status code based on generation response status
     */
    private HttpStatus determineHttpStatus(CodeGenerationResponse response) {
        return switch (response.getStatus()) {
            case SUCCESS -> HttpStatus.OK;
            case PARTIAL_SUCCESS -> HttpStatus.OK;
            case FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * Builds error response
     */
    private CodeGenerationResponse buildErrorResponse(String errorMessage) {
        return CodeGenerationResponse.builder()
                .status(CodeGenerationResponse.GenerationStatus.FAILED)
                .timestamp(LocalDateTime.now())
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Builds download response with proper headers
     */
    private ResponseEntity<Resource> buildDownloadResponse(Resource resource) {
        String filename = downloadService.getFilename(resource);
        long contentLength = downloadService.getContentLength(resource);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(contentLength)
                .body(resource);
    }

    /**
     * Simple health response record
     */
    private record HealthResponse(String status, String message) {}
}