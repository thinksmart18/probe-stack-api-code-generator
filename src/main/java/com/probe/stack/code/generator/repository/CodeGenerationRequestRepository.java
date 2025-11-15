package com.probe.stack.code.generator.repository;

import com.probe.stack.code.generator.entity.CodeGenerationRequestEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for CodeGenerationRequestEntity.
 * Provides data access operations for code generation request persistence.
 *
 * Features:
 * - Standard CRUD operations via MongoRepository
 * - Custom query methods for Smart Agent functionality
 * - Efficient lookups by generationId and artifactId
 * - Support for request status filtering and audit queries
 *
 * @see CodeGenerationRequestEntity
 */
@Repository
public interface CodeGenerationRequestRepository extends MongoRepository<CodeGenerationRequestEntity, String> {

    /**
     * Find a code generation request by its unique generation ID.
     * This is the primary lookup method for checking if a request already exists.
     *
     * @param generationId The unique generation identifier
     * @return Optional containing the entity if found, empty otherwise
     */
    Optional<CodeGenerationRequestEntity> findByGenerationId(String generationId);

    /**
     * Find all code generation requests for a specific artifact ID.
     * Useful for tracking all generations of a particular project.
     *
     * @param artifactId The Maven artifact ID
     * @return List of all requests matching the artifact ID
     */
    List<CodeGenerationRequestEntity> findByArtifactId(String artifactId);

    /**
     * Find all requests with a specific status.
     * Useful for monitoring failed or pending requests.
     *
     * @param status The generation status (SUCCESS, PARTIAL_SUCCESS, FAILED)
     * @return List of requests with the specified status
     */
    List<CodeGenerationRequestEntity> findByStatus(String status);

    /**
     * Find all requests by artifact ID and status.
     * Combines artifact filtering with status filtering.
     *
     * @param artifactId The Maven artifact ID
     * @param status The generation status
     * @return List of matching requests
     */
    List<CodeGenerationRequestEntity> findByArtifactIdAndStatus(String artifactId, String status);

    /**
     * Find all non-archived requests created after a specific date.
     * Useful for cleanup operations and report generation.
     *
     * @param createdAt The cutoff date/time
     * @return List of requests created after the specified date
     */
    @Query("{ 'createdAt': { $gte: ?0 }, 'archived': false }")
    List<CodeGenerationRequestEntity> findRecentNonArchivedRequests(LocalDateTime createdAt);

    /**
     * Find all archived requests older than a specific date.
     * Useful for purging old archived data.
     *
     * @param createdAt The cutoff date/time
     * @return List of archived requests older than the specified date
     */
    @Query("{ 'createdAt': { $lt: ?0 }, 'archived': true }")
    List<CodeGenerationRequestEntity> findOldArchivedRequests(LocalDateTime createdAt);

    /**
     * Find all requests by GitHub organization.
     * Useful for tracking all projects pushed to a specific organization.
     *
     * @param organization The GitHub organization or username
     * @return List of requests for the specified organization
     */
    List<CodeGenerationRequestEntity> findByOrganization(String organization);

    /**
     * Find all requests with successful GitHub push.
     * Useful for tracking successful GitHub integrations.
     *
     * @return List of requests with successful GitHub pushes
     */
    @Query("{ 'githubPushSuccessful': true }")
    List<CodeGenerationRequestEntity> findSuccessfulGithubPushes();

    /**
     * Find all failed requests (for retry or investigation).
     * Excludes archived requests.
     *
     * @return List of failed, non-archived requests
     */
    @Query("{ 'status': 'FAILED', 'archived': false }")
    List<CodeGenerationRequestEntity> findFailedRequests();

    /**
     * Check if a request exists with the given generation ID.
     * Efficient existence check without loading the full entity.
     *
     * @param generationId The unique generation identifier
     * @return true if exists, false otherwise
     */
    boolean existsByGenerationId(String generationId);

    /**
     * Count total requests by status.
     * Useful for dashboard statistics.
     *
     * @param status The generation status
     * @return Count of requests with the specified status
     */
    long countByStatus(String status);

    /**
     * Count total requests for an artifact ID.
     * Useful for tracking generation frequency.
     *
     * @param artifactId The Maven artifact ID
     * @return Count of requests for the artifact
     */
    long countByArtifactId(String artifactId);

    /**
     * Delete all archived requests older than a specific date.
     * Useful for data retention compliance.
     *
     * @param createdAt The cutoff date/time
     * @return Number of deleted records
     */
    @Query(value = "{ 'createdAt': { $lt: ?0 }, 'archived': true }", delete = true)
    long deleteArchivedRequestsOlderThan(LocalDateTime createdAt);
}
