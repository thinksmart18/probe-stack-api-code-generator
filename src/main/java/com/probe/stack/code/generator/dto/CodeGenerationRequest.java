package com.probe.stack.code.generator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for code generation from OpenAPI specification
 * Supports three input methods: URL, raw content, or file upload
 */
public class CodeGenerationRequest {

    /**
     * URL to the OpenAPI specification file (YAML or JSON)
     * Use this OR specContent, not both
     */
    private String openApiSpecUrl;

    /**
     * Raw OpenAPI specification content (YAML or JSON text)
     * Use this OR openApiSpecUrl, not both
     */
    private String specContent;

    /**
     * Type of spec content: "json" or "yaml"
     * Required when using specContent
     */
    private String specContentType;

    /**
     * Maven group ID (e.g., com.example)
     */
    @NotBlank(message = "Group name is required")
    @Pattern(regexp = "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$",
            message = "Invalid group name format")
    private String groupName;

    /**
     * Maven artifact ID (e.g., my-service)
     */
    @NotBlank(message = "Artifact ID is required")
    @Pattern(regexp = "^[a-z][a-z0-9-]*$",
            message = "Invalid artifact ID format")
    private String artifactId;

    /**
     * Base package for generated code (e.g., com.example.myservice)
     */
    @NotBlank(message = "Base package is required")
    @Pattern(regexp = "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$",
            message = "Invalid package name format")
    private String basePackage;

    /**
     * GitHub token for accessing private repositories (optional)
     */
    private String githubToken;

    /**
     * Project version (defaults to 1.0.0)
     */
    private String version = "1.0.0";

    /**
     * Whether to return as ZIP archive
     */
    private boolean returnAsArchive = false;

    /**
     * GitHub repository configuration
     */
    private GitHubConfig gitHubConfig;

    /**
     * GitHub organization or username
     */
    private String organization;

    /**
     * Initial branch name (defaults to main)
     */
    private String branchName = "main";

    /**
     * Repository name (defaults to artifactId if not provided)
     */
    private String repositoryName;

    // Constructors
    public CodeGenerationRequest() {
    }

    public CodeGenerationRequest(String openApiSpecUrl, String specContent, String specContentType,
                                  String groupName, String artifactId, String basePackage,
                                  String githubToken, String version, boolean returnAsArchive,
                                  GitHubConfig gitHubConfig, String organization, String branchName,
                                  String repositoryName) {
        this.openApiSpecUrl = openApiSpecUrl;
        this.specContent = specContent;
        this.specContentType = specContentType;
        this.groupName = groupName;
        this.artifactId = artifactId;
        this.basePackage = basePackage;
        this.githubToken = githubToken;
        this.version = version;
        this.returnAsArchive = returnAsArchive;
        this.gitHubConfig = gitHubConfig;
        this.organization = organization;
        this.branchName = branchName;
        this.repositoryName = repositoryName;
    }

    // Getters and Setters
    public String getOpenApiSpecUrl() {
        return openApiSpecUrl;
    }

    public void setOpenApiSpecUrl(String openApiSpecUrl) {
        this.openApiSpecUrl = openApiSpecUrl;
    }

    public String getSpecContent() {
        return specContent;
    }

    public void setSpecContent(String specContent) {
        this.specContent = specContent;
    }

    public String getSpecContentType() {
        return specContentType;
    }

    public void setSpecContentType(String specContentType) {
        this.specContentType = specContentType;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isReturnAsArchive() {
        return returnAsArchive;
    }

    public void setReturnAsArchive(boolean returnAsArchive) {
        this.returnAsArchive = returnAsArchive;
    }

    public GitHubConfig getGitHubConfig() {
        return gitHubConfig;
    }

    public void setGitHubConfig(GitHubConfig gitHubConfig) {
        this.gitHubConfig = gitHubConfig;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    /**
     * GitHub repository configuration
     */
    public static class GitHubConfig {

        /**
         * Whether to create and push to GitHub repository
         */
        private boolean enabled = false;

        /**
         * Repository description
         */
        private String description;

        /**
         * Whether repository should be private
         */
        private boolean isPrivate = true;

        /**
         * Commit message for initial commit
         */
        private String commitMessage = "Initial commit - Generated from OpenAPI specification";

        /**
         * Whether to initialize with README
         */
        private boolean autoInit = false;

        // Constructors
        public GitHubConfig() {
        }

        public GitHubConfig(boolean enabled, String description, boolean isPrivate,
                            String commitMessage, boolean autoInit) {
            this.enabled = enabled;
            this.description = description;
            this.isPrivate = isPrivate;
            this.commitMessage = commitMessage;
            this.autoInit = autoInit;
        }

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isPrivate() {
            return isPrivate;
        }

        public void setPrivate(boolean isPrivate) {
            this.isPrivate = isPrivate;
        }

        public String getCommitMessage() {
            return commitMessage;
        }

        public void setCommitMessage(String commitMessage) {
            this.commitMessage = commitMessage;
        }

        public boolean isAutoInit() {
            return autoInit;
        }

        public void setAutoInit(boolean autoInit) {
            this.autoInit = autoInit;
        }
    }
}
