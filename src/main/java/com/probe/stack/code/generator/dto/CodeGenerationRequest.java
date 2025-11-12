package com.probe.stack.code.generator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

/**
 * Request DTO for code generation from OpenAPI specification.
 * Supports three input methods: URL, raw content, or file upload.
 *
 * @author ProbeStack
 * @version 1.0
 * @since 1.0
 */
public class CodeGenerationRequest {

    /**
     * URL to the OpenAPI specification file (YAML or JSON).
     * Use this OR specContent, not both.
     */
    private String openApiSpecUrl;

    /**
     * Raw OpenAPI specification content (YAML or JSON text).
     * Use this OR openApiSpecUrl, not both.
     */
    private String specContent;

    /**
     * Type of spec content: "json" or "yaml".
     * Required when using specContent.
     */
    private String specContentType;

    /**
     * Maven group ID (e.g., com.example).
     */
    @NotBlank(message = "Group name is required")
    @Pattern(regexp = "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$",
            message = "Invalid group name format")
    private String groupName;

    /**
     * Maven artifact ID (e.g., my-service).
     */
    @NotBlank(message = "Artifact ID is required")
    @Pattern(regexp = "^[a-z][a-z0-9-]*$",
            message = "Invalid artifact ID format")
    private String artifactId;

    /**
     * Base package for generated code (e.g., com.example.myservice).
     */
    @NotBlank(message = "Base package is required")
    @Pattern(regexp = "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$",
            message = "Invalid package name format")
    private String basePackage;

    /**
     * GitHub token for accessing private repositories (optional).
     */
    private String githubToken;

    /**
     * Project version (defaults to 1.0.0).
     */
    private String version = "1.0.0";

    /**
     * Whether to return as ZIP archive.
     */
    private boolean returnAsArchive = false;

    /**
     * GitHub repository configuration.
     */
    private GitHubConfig gitHubConfig;

    /**
     * GitHub organization or username.
     */
    private String organization;

    /**
     * Initial branch name (defaults to main).
     */
    private String branchName = "main";

    /**
     * Repository name (defaults to artifactId if not provided).
     */
    private String repositoryName;

    /**
     * Default constructor.
     */
    public CodeGenerationRequest() {
    }

    /**
     * All-args constructor.
     *
     * @param openApiSpecUrl the OpenAPI specification URL
     * @param specContent the raw OpenAPI specification content
     * @param specContentType the type of spec content (json or yaml)
     * @param groupName the Maven group ID
     * @param artifactId the Maven artifact ID
     * @param basePackage the base package for generated code
     * @param githubToken the GitHub token for authentication
     * @param version the project version
     * @param returnAsArchive whether to return as ZIP archive
     * @param gitHubConfig the GitHub repository configuration
     * @param organization the GitHub organization or username
     * @param branchName the initial branch name
     * @param repositoryName the repository name
     */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeGenerationRequest that = (CodeGenerationRequest) o;
        return returnAsArchive == that.returnAsArchive &&
                Objects.equals(openApiSpecUrl, that.openApiSpecUrl) &&
                Objects.equals(specContent, that.specContent) &&
                Objects.equals(specContentType, that.specContentType) &&
                Objects.equals(groupName, that.groupName) &&
                Objects.equals(artifactId, that.artifactId) &&
                Objects.equals(basePackage, that.basePackage) &&
                Objects.equals(githubToken, that.githubToken) &&
                Objects.equals(version, that.version) &&
                Objects.equals(gitHubConfig, that.gitHubConfig) &&
                Objects.equals(organization, that.organization) &&
                Objects.equals(branchName, that.branchName) &&
                Objects.equals(repositoryName, that.repositoryName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(openApiSpecUrl, specContent, specContentType, groupName, artifactId,
                basePackage, githubToken, version, returnAsArchive, gitHubConfig, organization,
                branchName, repositoryName);
    }

    @Override
    public String toString() {
        return "CodeGenerationRequest{" +
                "openApiSpecUrl='" + openApiSpecUrl + '\'' +
                ", specContent='" + (specContent != null ? "[CONTENT]" : null) + '\'' +
                ", specContentType='" + specContentType + '\'' +
                ", groupName='" + groupName + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", basePackage='" + basePackage + '\'' +
                ", githubToken='" + (githubToken != null ? "[REDACTED]" : null) + '\'' +
                ", version='" + version + '\'' +
                ", returnAsArchive=" + returnAsArchive +
                ", gitHubConfig=" + gitHubConfig +
                ", organization='" + organization + '\'' +
                ", branchName='" + branchName + '\'' +
                ", repositoryName='" + repositoryName + '\'' +
                '}';
    }

    /**
     * Creates a new builder for CodeGenerationRequest.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for CodeGenerationRequest.
     */
    public static class Builder {
        private String openApiSpecUrl;
        private String specContent;
        private String specContentType;
        private String groupName;
        private String artifactId;
        private String basePackage;
        private String githubToken;
        private String version = "1.0.0";
        private boolean returnAsArchive = false;
        private GitHubConfig gitHubConfig;
        private String organization;
        private String branchName = "main";
        private String repositoryName;

        public Builder openApiSpecUrl(String openApiSpecUrl) {
            this.openApiSpecUrl = openApiSpecUrl;
            return this;
        }

        public Builder specContent(String specContent) {
            this.specContent = specContent;
            return this;
        }

        public Builder specContentType(String specContentType) {
            this.specContentType = specContentType;
            return this;
        }

        public Builder groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        public Builder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder basePackage(String basePackage) {
            this.basePackage = basePackage;
            return this;
        }

        public Builder githubToken(String githubToken) {
            this.githubToken = githubToken;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder returnAsArchive(boolean returnAsArchive) {
            this.returnAsArchive = returnAsArchive;
            return this;
        }

        public Builder gitHubConfig(GitHubConfig gitHubConfig) {
            this.gitHubConfig = gitHubConfig;
            return this;
        }

        public Builder organization(String organization) {
            this.organization = organization;
            return this;
        }

        public Builder branchName(String branchName) {
            this.branchName = branchName;
            return this;
        }

        public Builder repositoryName(String repositoryName) {
            this.repositoryName = repositoryName;
            return this;
        }

        public CodeGenerationRequest build() {
            return new CodeGenerationRequest(openApiSpecUrl, specContent, specContentType,
                    groupName, artifactId, basePackage, githubToken, version, returnAsArchive,
                    gitHubConfig, organization, branchName, repositoryName);
        }
    }

    /**
     * GitHub repository configuration.
     */
    public static class GitHubConfig {

        /**
         * Whether to create and push to GitHub repository.
         */
        private boolean enabled = false;

        /**
         * Repository description.
         */
        private String description;

        /**
         * Whether repository should be private.
         */
        private boolean isPrivate = true;

        /**
         * Commit message for initial commit.
         */
        private String commitMessage = "Initial commit - Generated from OpenAPI specification";

        /**
         * Whether to initialize with README.
         */
        private boolean autoInit = false;

        /**
         * Default constructor.
         */
        public GitHubConfig() {
        }

        /**
         * All-args constructor.
         *
         * @param enabled whether to create and push to GitHub repository
         * @param description repository description
         * @param isPrivate whether repository should be private
         * @param commitMessage commit message for initial commit
         * @param autoInit whether to initialize with README
         */
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GitHubConfig that = (GitHubConfig) o;
            return enabled == that.enabled &&
                    isPrivate == that.isPrivate &&
                    autoInit == that.autoInit &&
                    Objects.equals(description, that.description) &&
                    Objects.equals(commitMessage, that.commitMessage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, description, isPrivate, commitMessage, autoInit);
        }

        @Override
        public String toString() {
            return "GitHubConfig{" +
                    "enabled=" + enabled +
                    ", description='" + description + '\'' +
                    ", isPrivate=" + isPrivate +
                    ", commitMessage='" + commitMessage + '\'' +
                    ", autoInit=" + autoInit +
                    '}';
        }

        /**
         * Creates a new builder for GitHubConfig.
         *
         * @return a new Builder instance
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder class for GitHubConfig.
         */
        public static class Builder {
            private boolean enabled = false;
            private String description;
            private boolean isPrivate = true;
            private String commitMessage = "Initial commit - Generated from OpenAPI specification";
            private boolean autoInit = false;

            public Builder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder isPrivate(boolean isPrivate) {
                this.isPrivate = isPrivate;
                return this;
            }

            public Builder commitMessage(String commitMessage) {
                this.commitMessage = commitMessage;
                return this;
            }

            public Builder autoInit(boolean autoInit) {
                this.autoInit = autoInit;
                return this;
            }

            public GitHubConfig build() {
                return new GitHubConfig(enabled, description, isPrivate, commitMessage, autoInit);
            }
        }
    }
}
