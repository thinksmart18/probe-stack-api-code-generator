package com.probe.stack.code.generator.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB entity representing a code generation request and its execution history.
 * This entity provides Smart Agent functionality by tracking request lifecycle,
 * enabling request reprocessing, and maintaining audit trails.
 *
 * Features:
 * - Tracks unique requests by generationId
 * - Supports upsert operations for request updates
 * - Maintains complete audit trail with timestamps
 * - Stores request metadata and execution results
 */
@Document(collection = "code_generation_requests")
public class CodeGenerationRequestEntity {

    /**
     * Unique identifier for the code generation request.
     * This is the MongoDB document ID and serves as the primary key.
     */
    @Id
    private String id;

    /**
     * Generation ID assigned to this request.
     * Indexed for fast lookups when checking for existing requests.
     */
    @Indexed(unique = true)
    private String generationId;

    /**
     * Maven artifact ID for the generated project.
     * Indexed for quick queries by artifact name.
     */
    @Indexed
    private String artifactId;

    /**
     * Maven group name (e.g., com.example).
     */
    private String groupName;

    /**
     * Base package for generated code.
     */
    private String basePackage;

    /**
     * Project version (e.g., 1.0.0).
     */
    private String version;

    /**
     * OpenAPI specification URL (if provided).
     */
    private String openApiSpecUrl;

    /**
     * Type of specification content: json or yaml.
     */
    private String specContentType;

    /**
     * GitHub organization or username.
     */
    private String organization;

    /**
     * Repository name for GitHub integration.
     */
    private String repositoryName;

    /**
     * Branch name for GitHub integration.
     */
    private String branchName;

    /**
     * Generation status: SUCCESS, PARTIAL_SUCCESS, or FAILED.
     */
    private String status;

    /**
     * Path to the generated project directory.
     */
    private String projectPath;

    /**
     * Path to the archive file if generated.
     */
    private String archivePath;

    /**
     * GitHub repository URL if pushed to GitHub.
     */
    private String githubRepositoryUrl;

    /**
     * GitHub clone URL (HTTPS).
     */
    private String githubCloneUrl;

    /**
     * GitHub commit SHA for the initial commit.
     */
    private String githubCommitSha;

    /**
     * Whether GitHub push was successful.
     */
    private Boolean githubPushSuccessful;

    /**
     * List of generated files.
     */
    private List<String> generatedFiles;

    /**
     * Warnings or informational messages from generation process.
     */
    private List<String> messages;

    /**
     * Error message if generation failed.
     */
    private String errorMessage;

    /**
     * Timestamp when the request was first created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the request was last updated.
     */
    private LocalDateTime updatedAt;

    /**
     * Timestamp when code generation completed.
     */
    private LocalDateTime completedAt;

    /**
     * Number of times this request has been reprocessed.
     */
    private Integer retryCount;

    /**
     * Whether this is an archived/soft-deleted request.
     */
    private Boolean archived;

    /**
     * User or system that initiated the request (for audit trail).
     */
    private String requestedBy;

    // Default constructor
    public CodeGenerationRequestEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.retryCount = 0;
        this.archived = false;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGenerationId() {
        return generationId;
    }

    public void setGenerationId(String generationId) {
        this.generationId = generationId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOpenApiSpecUrl() {
        return openApiSpecUrl;
    }

    public void setOpenApiSpecUrl(String openApiSpecUrl) {
        this.openApiSpecUrl = openApiSpecUrl;
    }

    public String getSpecContentType() {
        return specContentType;
    }

    public void setSpecContentType(String specContentType) {
        this.specContentType = specContentType;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getArchivePath() {
        return archivePath;
    }

    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

    public String getGithubRepositoryUrl() {
        return githubRepositoryUrl;
    }

    public void setGithubRepositoryUrl(String githubRepositoryUrl) {
        this.githubRepositoryUrl = githubRepositoryUrl;
    }

    public String getGithubCloneUrl() {
        return githubCloneUrl;
    }

    public void setGithubCloneUrl(String githubCloneUrl) {
        this.githubCloneUrl = githubCloneUrl;
    }

    public String getGithubCommitSha() {
        return githubCommitSha;
    }

    public void setGithubCommitSha(String githubCommitSha) {
        this.githubCommitSha = githubCommitSha;
    }

    public Boolean getGithubPushSuccessful() {
        return githubPushSuccessful;
    }

    public void setGithubPushSuccessful(Boolean githubPushSuccessful) {
        this.githubPushSuccessful = githubPushSuccessful;
    }

    public List<String> getGeneratedFiles() {
        return generatedFiles;
    }

    public void setGeneratedFiles(List<String> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
        this.updatedAt = LocalDateTime.now();
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    @Override
    public String toString() {
        return "CodeGenerationRequestEntity{" +
                "id='" + id + '\'' +
                ", generationId='" + generationId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
