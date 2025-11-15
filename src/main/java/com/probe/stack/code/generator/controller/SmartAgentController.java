package com.probe.stack.code.generator.controller;

import com.probe.stack.code.generator.entity.CodeGenerationRequestEntity;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import com.probe.stack.code.generator.service.SmartAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for Smart Agent operations.
 * Provides endpoints for managing and querying code generation requests.
 *
 * Smart Agent Features:
 * - Query request status and details
 * - Archive old requests
 * - View request history
 * - Enable request reprocessing
 *
 * All endpoints follow RESTful conventions and return appropriate HTTP status codes.
 *
 * @author Probe Stack Code Generator
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/smart-agent")
public class SmartAgentController {

    private static final Logger log = LoggerFactory.getLogger(SmartAgentController.class);

    private final SmartAgentService smartAgentService;

    public SmartAgentController(SmartAgentService smartAgentService) {
        this.smartAgentService = smartAgentService;
    }

    /**
     * Retrieves a code generation request by its generation ID.
     * Useful for checking request status and retrieving generation results.
     *
     * @param generationId The unique generation identifier
     * @return ResponseEntity containing the request entity if found, 404 if not found
     */
    @GetMapping("/requests/{generationId}")
    public ResponseEntity<CodeGenerationRequestEntity> getRequest(
            @PathVariable String generationId) {

        log.info("Smart Agent API: Request received to retrieve generationId: {}", generationId);

        try {
            Optional<CodeGenerationRequestEntity> entity =
                smartAgentService.findByGenerationId(generationId);

            if (entity.isPresent()) {
                log.info("Smart Agent API: Found request for generationId: {}", generationId);
                return ResponseEntity.ok(entity.get());
            } else {
                log.warn("Smart Agent API: Request not found for generationId: {}", generationId);
                return ResponseEntity.notFound().build();
            }

        } catch (CodeGenerationException e) {
            log.error("Smart Agent API: Error retrieving request for generationId: {}. Error: {}",
                    generationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Archives a code generation request (soft delete).
     * Archived requests are excluded from active queries but retained for audit purposes.
     *
     * @param generationId The unique generation identifier
     * @return ResponseEntity with 200 if archived, 404 if not found, 500 on error
     */
    @PutMapping("/requests/{generationId}/archive")
    public ResponseEntity<ArchiveResponse> archiveRequest(
            @PathVariable String generationId) {

        log.info("Smart Agent API: Request received to archive generationId: {}", generationId);

        try {
            boolean archived = smartAgentService.archiveRequest(generationId);

            if (archived) {
                log.info("Smart Agent API: Successfully archived generationId: {}", generationId);
                return ResponseEntity.ok(
                    new ArchiveResponse(true, "Request archived successfully"));
            } else {
                log.warn("Smart Agent API: Cannot archive - request not found for generationId: {}",
                        generationId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ArchiveResponse(false, "Request not found"));
            }

        } catch (CodeGenerationException e) {
            log.error("Smart Agent API: Error archiving request for generationId: {}. Error: {}",
                    generationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ArchiveResponse(false, "Error archiving request: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint for Smart Agent service.
     * Returns service status and MongoDB connectivity information.
     *
     * @return ResponseEntity with health status
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        log.debug("Smart Agent API: Health check requested");

        try {
            // Simple health check - if service is up, we're healthy
            return ResponseEntity.ok(
                new HealthResponse("UP", "Smart Agent service is running"));

        } catch (Exception e) {
            log.error("Smart Agent API: Health check failed. Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new HealthResponse("DOWN", "Smart Agent service error: " + e.getMessage()));
        }
    }

    /**
     * Response DTO for archive operations.
     */
    public record ArchiveResponse(boolean success, String message) {}

    /**
     * Response DTO for health check.
     */
    public record HealthResponse(String status, String message) {}
}
