package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.dto.CodeGenerationResponse;
import com.probe.stack.code.generator.entity.CodeGenerationRequestEntity;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import com.probe.stack.code.generator.repository.CodeGenerationRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Smart Agent Service for intelligent code generation request management.
 *
 * This service provides Smart Agent functionality by:
 * - Tracking all code generation requests in MongoDB
 * - Detecting duplicate requests and providing upsert capabilities
 * - Maintaining comprehensive audit trails
 * - Enabling request reprocessing and retry mechanisms
 * - Providing detailed logging for troubleshooting
 *
 * Key Features:
 * - Automatic upsert: Updates existing requests or creates new ones
 * - Robust exception handling with detailed error messages
 * - Comprehensive logging at each operation stage
 * - Support for request lifecycle management (create, update, archive)
 * - Integration with code generation workflow
 *
 * @author Probe Stack Code Generator
 * @version 1.0
 */
@Service
public class SmartAgentService {

    private static final Logger log = LoggerFactory.getLogger(SmartAgentService.class);

    private final CodeGenerationRequestRepository repository;

    public SmartAgentService(CodeGenerationRequestRepository repository) {
        this.repository = repository;
    }

    /**
     * Saves or updates a code generation request in MongoDB.
     * Implements Smart Agent upsert logic:
     * - If request with generationId exists: Updates existing record
     * - If request doesn't exist: Creates new record
     *
     * This method provides the core Smart Agent functionality for request persistence.
     *
     * @param request The code generation request DTO
     * @param generationId The unique generation identifier
     * @return The persisted entity
     * @throws CodeGenerationException if persistence fails
     */
    @Transactional
    public CodeGenerationRequestEntity saveOrUpdateRequest(
            CodeGenerationRequest request,
            String generationId) {

        log.info("Smart Agent: Processing request for generationId: {}, artifactId: {}",
                generationId, request.getArtifactId());

        try {
            // Check if request already exists in database
            Optional<CodeGenerationRequestEntity> existingEntity =
                    repository.findByGenerationId(generationId);

            CodeGenerationRequestEntity entity;

            if (existingEntity.isPresent()) {
                // UPDATE SCENARIO: Request exists, update it
                entity = existingEntity.get();
                log.info("Smart Agent: Found existing request with ID: {}. Updating record.",
                        entity.getId());

                updateEntityFromRequest(entity, request);
                entity.incrementRetryCount();

                log.debug("Smart Agent: Updated entity - retryCount: {}, updatedAt: {}",
                        entity.getRetryCount(), entity.getUpdatedAt());

            } else {
                // INSERT SCENARIO: New request, create it
                log.info("Smart Agent: No existing request found. Creating new record.");

                entity = createEntityFromRequest(request, generationId);

                log.debug("Smart Agent: Created new entity - generationId: {}, artifactId: {}",
                        entity.getGenerationId(), entity.getArtifactId());
            }

            // Persist the entity (insert or update)
            CodeGenerationRequestEntity savedEntity = repository.save(entity);

            log.info("Smart Agent: Successfully persisted request. Document ID: {}, Generation ID: {}",
                    savedEntity.getId(), savedEntity.getGenerationId());

            return savedEntity;

        } catch (DataAccessException e) {
            log.error("Smart Agent: Database error while saving request for generationId: {}. Error: {}",
                    generationId, e.getMessage(), e);
            throw new CodeGenerationException(
                    "Failed to persist code generation request: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Smart Agent: Unexpected error while processing request for generationId: {}. Error: {}",
                    generationId, e.getMessage(), e);
            throw new CodeGenerationException(
                    "Unexpected error during request persistence: " + e.getMessage(), e);
        }
    }

    /**
     * Updates an existing request entity with response data after code generation completes.
     * This method is called after successful (or failed) code generation to store results.
     *
     * @param generationId The unique generation identifier
     * @param response The code generation response with results
     * @return The updated entity
     * @throws CodeGenerationException if update fails or entity not found
     */
    @Transactional
    public CodeGenerationRequestEntity updateWithResponse(
            String generationId,
            CodeGenerationResponse response) {

        log.info("Smart Agent: Updating request with generation results for generationId: {}",
                generationId);

        try {
            CodeGenerationRequestEntity entity = repository.findByGenerationId(generationId)
                    .orElseThrow(() -> {
                        log.error("Smart Agent: Cannot update - request not found for generationId: {}",
                                generationId);
                        return new CodeGenerationException(
                                "Cannot update response: Request not found for generationId: " + generationId);
                    });

            log.debug("Smart Agent: Found request entity with ID: {}. Applying response data.",
                    entity.getId());

            // Update entity with response data
            updateEntityWithResponse(entity, response);
            entity.setCompletedAt(LocalDateTime.now());

            CodeGenerationRequestEntity updatedEntity = repository.save(entity);

            log.info("Smart Agent: Successfully updated request with response. Status: {}, " +
                    "Generated files: {}",
                    updatedEntity.getStatus(),
                    updatedEntity.getGeneratedFiles() != null ? updatedEntity.getGeneratedFiles().size() : 0);

            return updatedEntity;

        } catch (DataAccessException e) {
            log.error("Smart Agent: Database error while updating response for generationId: {}. Error: {}",
                    generationId, e.getMessage(), e);
            throw new CodeGenerationException(
                    "Failed to update request with response: " + e.getMessage(), e);

        } catch (CodeGenerationException e) {
            // Re-throw CodeGenerationException as-is
            throw e;

        } catch (Exception e) {
            log.error("Smart Agent: Unexpected error while updating response for generationId: {}. Error: {}",
                    generationId, e.getMessage(), e);
            throw new CodeGenerationException(
                    "Unexpected error during response update: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a code generation request by its generation ID.
     *
     * @param generationId The unique generation identifier
     * @return Optional containing the entity if found
     */
    public Optional<CodeGenerationRequestEntity> findByGenerationId(String generationId) {
        log.debug("Smart Agent: Looking up request for generationId: {}", generationId);

        try {
            Optional<CodeGenerationRequestEntity> entity = repository.findByGenerationId(generationId);

            if (entity.isPresent()) {
                log.debug("Smart Agent: Found request - ID: {}, Status: {}",
                        entity.get().getId(), entity.get().getStatus());
            } else {
                log.debug("Smart Agent: No request found for generationId: {}", generationId);
            }

            return entity;

        } catch (DataAccessException e) {
            log.error("Smart Agent: Database error while finding request for generationId: {}. Error: {}",
                    generationId, e.getMessage(), e);
            throw new CodeGenerationException(
                    "Failed to retrieve request: " + e.getMessage(), e);
        }
    }

    /**
     * Archives a code generation request (soft delete).
     * Archived requests are excluded from active queries but retained for audit purposes.
     *
     * @param generationId The unique generation identifier
     * @return true if successfully archived, false if not found
     */
    @Transactional
    public boolean archiveRequest(String generationId) {
        log.info("Smart Agent: Archiving request for generationId: {}", generationId);

        try {
            Optional<CodeGenerationRequestEntity> entity = repository.findByGenerationId(generationId);

            if (entity.isPresent()) {
                CodeGenerationRequestEntity requestEntity = entity.get();
                requestEntity.setArchived(true);
                repository.save(requestEntity);

                log.info("Smart Agent: Successfully archived request - ID: {}", requestEntity.getId());
                return true;

            } else {
                log.warn("Smart Agent: Cannot archive - request not found for generationId: {}",
                        generationId);
                return false;
            }

        } catch (DataAccessException e) {
            log.error("Smart Agent: Database error while archiving request for generationId: {}. Error: {}",
                    generationId, e.getMessage(), e);
            throw new CodeGenerationException(
                    "Failed to archive request: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new entity from a code generation request DTO.
     * Maps request fields to entity fields for initial persistence.
     *
     * @param request The code generation request
     * @param generationId The unique generation identifier
     * @return The newly created entity (not yet persisted)
     */
    private CodeGenerationRequestEntity createEntityFromRequest(
            CodeGenerationRequest request,
            String generationId) {

        log.debug("Smart Agent: Creating new entity from request");

        CodeGenerationRequestEntity entity = new CodeGenerationRequestEntity();
        entity.setGenerationId(generationId);
        entity.setArtifactId(request.getArtifactId());
        entity.setGroupName(request.getGroupName());
        entity.setBasePackage(request.getBasePackage());
        entity.setVersion(request.getVersion());
        entity.setOpenApiSpecUrl(request.getOpenApiSpecUrl());
        entity.setSpecContentType(request.getSpecContentType());
        entity.setOrganization(request.getOrganization());
        entity.setRepositoryName(request.getRepositoryName());
        entity.setBranchName(request.getBranchName());
        entity.setStatus("PENDING");

        return entity;
    }

    /**
     * Updates an existing entity with data from a new request.
     * Used when a request is being reprocessed or updated.
     *
     * @param entity The existing entity to update
     * @param request The new request data
     */
    private void updateEntityFromRequest(
            CodeGenerationRequestEntity entity,
            CodeGenerationRequest request) {

        log.debug("Smart Agent: Updating existing entity with new request data");

        // Update request fields (generationId remains unchanged)
        entity.setArtifactId(request.getArtifactId());
        entity.setGroupName(request.getGroupName());
        entity.setBasePackage(request.getBasePackage());
        entity.setVersion(request.getVersion());
        entity.setOpenApiSpecUrl(request.getOpenApiSpecUrl());
        entity.setSpecContentType(request.getSpecContentType());
        entity.setOrganization(request.getOrganization());
        entity.setRepositoryName(request.getRepositoryName());
        entity.setBranchName(request.getBranchName());
        entity.setStatus("PENDING"); // Reset status for reprocessing

        // Clear previous results for fresh generation
        entity.setErrorMessage(null);
        entity.setCompletedAt(null);
    }

    /**
     * Updates an entity with code generation response data.
     * Stores the results of code generation (success, failure, GitHub info, etc.).
     *
     * @param entity The entity to update
     * @param response The generation response
     */
    private void updateEntityWithResponse(
            CodeGenerationRequestEntity entity,
            CodeGenerationResponse response) {

        log.debug("Smart Agent: Updating entity with response data");

        entity.setStatus(response.getStatus() != null ? response.getStatus().toString() : "UNKNOWN");
        entity.setProjectPath(response.getProjectPath());
        entity.setArchivePath(response.getArchivePath());
        entity.setGeneratedFiles(response.getGeneratedFiles());
        entity.setMessages(response.getMessages());
        entity.setErrorMessage(response.getErrorMessage());

        // Update GitHub information if available
        if (response.getGitHubRepositoryInfo() != null) {
            CodeGenerationResponse.GitHubRepositoryInfo ghInfo = response.getGitHubRepositoryInfo();
            entity.setGithubRepositoryUrl(ghInfo.getRepositoryUrl());
            entity.setGithubCloneUrl(ghInfo.getCloneUrl());
            entity.setGithubCommitSha(ghInfo.getCommitSha());
            entity.setGithubPushSuccessful(ghInfo.isPushSuccessful());
        }
    }
}
