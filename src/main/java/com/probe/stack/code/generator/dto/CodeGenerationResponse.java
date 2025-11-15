package com.probe.stack.code.generator.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for code generation results
 */
public class CodeGenerationResponse {

    /**
     * Unique identifier for this generation request
     */
    private String generationId;

    /**
     * Path to the generated project
     */
    private String projectPath;

    /**
     * Path to archive file if returnAsArchive was true
     */
    private String archivePath;

    /**
     * Generation status
     */
    private GenerationStatus status;

    /**
     * Timestamp when generation completed
     */
    private LocalDateTime timestamp;

    /**
     * List of generated files
     */
    private List<String> generatedFiles;

    /**
     * Any warnings or messages
     */
    private List<String> messages;

    /**
     * Error message if generation failed
     */
    private String errorMessage;

    /**
     * Repository URL
     */
    private String repositoryUrl;

    /**
     * Clone URL (HTTPS)
     */
    private String cloneUrl;

    /**
     * SSH URL
     */
    private String sshUrl;

    /**
     * Repository full name (org/repo)
     */
    private String fullName;

    /**
     * Initial commit SHA
     */
    private String commitSha;

    /**
     * Branch name
     */
    private String branchName;

    /**
     * Whether push was successful
     */
    private boolean pushSuccessful;

    // Constructors
    public CodeGenerationResponse() {
    }

    public CodeGenerationResponse(String generationId, String projectPath, String archivePath,
                                   GenerationStatus status, LocalDateTime timestamp,
                                   List<String> generatedFiles, List<String> messages,
                                   String errorMessage, String repositoryUrl, String cloneUrl,
                                   String sshUrl, String fullName, String commitSha,
                                   String branchName, boolean pushSuccessful) {
        this.generationId = generationId;
        this.projectPath = projectPath;
        this.archivePath = archivePath;
        this.status = status;
        this.timestamp = timestamp;
        this.generatedFiles = generatedFiles;
        this.messages = messages;
        this.errorMessage = errorMessage;
        this.repositoryUrl = repositoryUrl;
        this.cloneUrl = cloneUrl;
        this.sshUrl = sshUrl;
        this.fullName = fullName;
        this.commitSha = commitSha;
        this.branchName = branchName;
        this.pushSuccessful = pushSuccessful;
    }

    // Builder pattern (simplified)
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String generationId;
        private String projectPath;
        private String archivePath;
        private GenerationStatus status;
        private LocalDateTime timestamp;
        private List<String> generatedFiles;
        private List<String> messages;
        private String errorMessage;
        private String repositoryUrl;
        private String cloneUrl;
        private String sshUrl;
        private String fullName;
        private String commitSha;
        private String branchName;
        private boolean pushSuccessful;

        public Builder generationId(String generationId) {
            this.generationId = generationId;
            return this;
        }

        public Builder projectPath(String projectPath) {
            this.projectPath = projectPath;
            return this;
        }

        public Builder archivePath(String archivePath) {
            this.archivePath = archivePath;
            return this;
        }

        public Builder status(GenerationStatus status) {
            this.status = status;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder generatedFiles(List<String> generatedFiles) {
            this.generatedFiles = generatedFiles;
            return this;
        }

        public Builder messages(List<String> messages) {
            this.messages = messages;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder repositoryUrl(String repositoryUrl) {
            this.repositoryUrl = repositoryUrl;
            return this;
        }

        public Builder cloneUrl(String cloneUrl) {
            this.cloneUrl = cloneUrl;
            return this;
        }

        public Builder sshUrl(String sshUrl) {
            this.sshUrl = sshUrl;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder commitSha(String commitSha) {
            this.commitSha = commitSha;
            return this;
        }

        public Builder branchName(String branchName) {
            this.branchName = branchName;
            return this;
        }

        public Builder pushSuccessful(boolean pushSuccessful) {
            this.pushSuccessful = pushSuccessful;
            return this;
        }

        public CodeGenerationResponse build() {
            return new CodeGenerationResponse(generationId, projectPath, archivePath, status,
                    timestamp, generatedFiles, messages, errorMessage, repositoryUrl, cloneUrl,
                    sshUrl, fullName, commitSha, branchName, pushSuccessful);
        }
    }

    // Getters and Setters
    public String getGenerationId() {
        return generationId;
    }

    public void setGenerationId(String generationId) {
        this.generationId = generationId;
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

    public GenerationStatus getStatus() {
        return status;
    }

    public void setStatus(GenerationStatus status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getCloneUrl() {
        return cloneUrl;
    }

    public void setCloneUrl(String cloneUrl) {
        this.cloneUrl = cloneUrl;
    }

    public String getSshUrl() {
        return sshUrl;
    }

    public void setSshUrl(String sshUrl) {
        this.sshUrl = sshUrl;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public boolean isPushSuccessful() {
        return pushSuccessful;
    }

    public void setPushSuccessful(boolean pushSuccessful) {
        this.pushSuccessful = pushSuccessful;
    }

    public enum GenerationStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILED
    }
}
