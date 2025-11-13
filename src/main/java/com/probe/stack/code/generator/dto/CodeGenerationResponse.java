package com.probe.stack.code.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for code generation results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    public enum GenerationStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILED
    }
}