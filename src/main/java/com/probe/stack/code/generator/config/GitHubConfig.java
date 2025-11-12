package com.probe.stack.code.generator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for GitHub integration
 * Maps to probe.stack.generator.github.* properties in application.yaml
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "probe.stack.generator.github")
public class GitHubConfig {

    /**
     * GitHub integration version
     */
    @NotBlank
    private String version;

    private String description;

    /**
     * Push configuration settings
     */
    private PushConfig push = new PushConfig();

    /**
     * Repository configuration settings
     */
    private Config config = new Config();

    /**
     * Personal access token configuration
     */
    private PersonalConfig personal = new PersonalConfig();

    /**
     * Push configuration for GitHub operations
     */
    @Data
    public static class PushConfig {
        /**
         * Flag to enable/disable automatic push to GitHub
         */
        private boolean enabled = true;
    }

    /**
     * General GitHub repository configuration
     */
    @Data
    public static class Config {
        /**
         * Flag to determine if the repository should be private
         */
        private boolean isPrivate = true;

        /**
         * Commit configuration settings
         */
        private CommitConfig commit = new CommitConfig();
    }

    /**
     * Commit message configuration
     */
    @Data
    public static class CommitConfig {
        /**
         * Commit message template
         * Supports placeholders like {app_name}
         */
        @NotBlank
        private String message;
    }

    /**
     * Personal access token configuration for GitHub authentication
     */
    @Data
    public static class PersonalConfig {
        /**
         * GitHub personal access token
         * Recommended to use environment variable: ${github_personal_access_token}
         */
        @NotBlank
        private String accessToken;
    }
}