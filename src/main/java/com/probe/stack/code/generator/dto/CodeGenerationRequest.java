package com.probe.stack.code.generator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for code generation from OpenAPI specification
 * Supports three input methods: URL, raw content, or file upload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    @Builder.Default
    private String version = "1.0.0";

    /**
     * Whether to return as ZIP archive
     */
    @Builder.Default
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
    @Builder.Default
    private String branchName = "main";

    /**
     * Repository name (defaults to artifactId if not provided)
     */
    private String repositoryName;

    /**
     * GitHub repository configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitHubConfig {

        /**
         * Whether to create and push to GitHub repository
         */
        @Builder.Default
        private boolean enabled = false;

        /**
         * Repository description
         */
        private String description;

        /**
         * Whether repository should be private
         */
        @Builder.Default
        private boolean isPrivate = true;



        /**
         * Commit message for initial commit
         */
        @Builder.Default
        private String commitMessage = "Initial commit - Generated from OpenAPI specification";

        /**
         * Whether to initialize with README
         */
        @Builder.Default
        private boolean autoInit = false;
    }
}