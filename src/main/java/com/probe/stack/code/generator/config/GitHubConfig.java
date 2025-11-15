package com.probe.stack.code.generator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for GitHub integration
 * Maps to probestack.github.* properties in application.yml
 */
@Validated
@Configuration
@ConfigurationProperties(prefix = "probestack.github")
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

    // Getters and Setters
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PushConfig getPush() {
        return push;
    }

    public void setPush(PushConfig push) {
        this.push = push;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public PersonalConfig getPersonal() {
        return personal;
    }

    public void setPersonal(PersonalConfig personal) {
        this.personal = personal;
    }

    /**
     * Push configuration for GitHub operations
     */
    public static class PushConfig {
        /**
         * Flag to enable/disable automatic push to GitHub
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * General GitHub repository configuration
     */
    public static class Config {
        /**
         * Flag to determine if the repository should be private
         */
        private boolean isPrivate = true;

        /**
         * Commit configuration settings
         */
        private CommitConfig commit = new CommitConfig();

        public boolean isPrivate() {
            return isPrivate;
        }

        public void setPrivate(boolean isPrivate) {
            this.isPrivate = isPrivate;
        }

        public CommitConfig getCommit() {
            return commit;
        }

        public void setCommit(CommitConfig commit) {
            this.commit = commit;
        }
    }

    /**
     * Commit message configuration
     */
    public static class CommitConfig {
        /**
         * Commit message template
         * Supports placeholders like {app_name}
         */
        @NotBlank
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * Personal access token configuration for GitHub authentication
     */
    public static class PersonalConfig {
        /**
         * GitHub personal access token
         * Recommended to use environment variable: ${github_personal_access_token}
         */
        @NotBlank
        private String accesstoken;

        public String getAccesstoken() {
            return accesstoken;
        }

        public void setAccesstoken(String accesstoken) {
            this.accesstoken = accesstoken;
        }
    }
}
