package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.component.CodeGenerationOrchestrator;
import com.probe.stack.code.generator.config.CodeGeneratorConfig;
import com.probe.stack.code.generator.config.GitHubConfig;
import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.dto.CodeGenerationResponse;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import com.probe.stack.code.generator.parser.ControllerMetadataExtractor;
import com.probe.stack.code.generator.util.AppConstants;
import com.probe.stack.code.generator.util.ControllerPathScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main service orchestrating the code generation process
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGenerationService {

    private final CodeGeneratorConfig config;
    private final SpecificationDownloadService specDownloadService;
    private final OpenApiGeneratorService generatorService;
    private final FileOperationsService fileOpsService;
    private final TemplateProcessingService templateService;
    private final PomMergeService pomMergeService;
    private final PomCustomizationService pomCustomizationService; // NEW
    private final PropertiesMergeService propertiesMergeService;
    private final GitHubService gitHubService;
    private final TemplateEnhancementService templateEnhancementService;
    private final RequestValidationService validationService;
    private final GitHubConfig githubPropertiesConfig;
    private final CodeGenerationOrchestrator codeGenerationOrchestrator;
    private final ControllerPathScanner controllerPathScanner;
    private final ControllerMetadataExtractor controllerMetadataExtractor;

    /**
     * Generates Spring Boot project from OpenAPI specification
     *
     * @param request Code generation request
     * @return Code generation response with project details
     */
    public CodeGenerationResponse generateProject(CodeGenerationRequest request) {
        String generationId = UUID.randomUUID().toString();
        log.info("Starting code generation - ID: {}, Artifact: {}",
                generationId, request.getArtifactId());

        List<String> messages = new ArrayList<>();
        Path projectDir = null;
        Path archivePath = null;

        try {
            // Step 1: Setup directories
            Path outputDir = setupDirectories(generationId);
            projectDir = outputDir.resolve(request.getArtifactId());

            // Step 2: Download OpenAPI specification
            Path specPath = downloadSpecification(request, messages);

            // Step 3: Generate code using OpenAPI Generator
            List<String> generatedFiles = generatorService.generateCode(specPath, request, projectDir);
            messages.add(String.format("Generated %d files from OpenAPI spec", generatedFiles.size()));

            // Step 4: Enhance project with templates
            List<String> enhancementMessages = templateEnhancementService.enhanceProject(projectDir, request);
            messages.addAll(enhancementMessages);

            // Generate Service and Repository classes
            generateServiceAndRepositoryClasses(request, projectDir);

            // Step 5: Update .openapi-generator-ignore
            updateGeneratorIgnoreFile(projectDir, messages);

            // Step 6: Update README
            updateReadme(projectDir, request, messages);

            // Step 7: Create archive if requested
            if (request.isReturnAsArchive()) {
                archivePath = createArchive(projectDir, generationId, request.getArtifactId());
                messages.add("Created project archive");
            }

            // Step 8: Push to GitHub if enabled (NEW)
            CodeGenerationResponse githubInfo = createRepoAndPushCode(request, projectDir, messages);

            // Cleanup temporary files
            cleanupTempFiles();

            log.info("Code generation completed successfully - ID: {}", generationId);

            // create response
            CodeGenerationResponse.CodeGenerationResponseBuilder builder = CodeGenerationResponse.builder()
                    .generationId(generationId)
                    .projectPath(projectDir.toString())
                  //  .archivePath(archivePath != null ? archivePath.toString() : null)
                    .status(CodeGenerationResponse.GenerationStatus.SUCCESS)
                    .timestamp(LocalDateTime.now());
                   // .generatedFiles(generatedFiles)
                   // .messages(messages)

            // Include GitHub info if available
            if (githubInfo != null) {
                builder.repositoryUrl(githubInfo.getRepositoryUrl())
                       .cloneUrl(githubInfo.getCloneUrl())
                       .sshUrl(githubInfo.getSshUrl())
                       .fullName(githubInfo.getFullName())
                       .commitSha(githubInfo.getCommitSha())
                       .branchName(githubInfo.getBranchName())
                       .pushSuccessful(githubInfo.isPushSuccessful());
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Code generation failed - ID: {}", generationId, e);

            return CodeGenerationResponse.builder()
                    .generationId(generationId)
                    .projectPath(projectDir != null ? projectDir.toString() : null)
                    .status(CodeGenerationResponse.GenerationStatus.FAILED)
                    .timestamp(LocalDateTime.now())
                    .messages(messages)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private void generateServiceAndRepositoryClasses(CodeGenerationRequest request, Path projectDir) throws Exception {
        List<File> generatedProjectControllerFiles = controllerPathScanner.getGeneratedControllerClassFiles(projectDir.toString(), request.getBasePackage(),"api");
        File outputPathForServiceAndRepoClasses = controllerPathScanner.constructOutputPackageDirector(projectDir.toString(), request.getBasePackage());
        codeGenerationOrchestrator.generateAllArtifacts(generatedProjectControllerFiles, projectDir.toString(), request.getBasePackage(),outputPathForServiceAndRepoClasses);
    }

    private CodeGenerationResponse createRepoAndPushCode(CodeGenerationRequest request, Path projectDir, List<String> messages) {
        CodeGenerationResponse githubInfo = null;
        if (githubPropertiesConfig != null && githubPropertiesConfig.getPush().isEnabled()) {
            try {
                log.info("GitHub integration enabled, pushing to repository");
                githubInfo = gitHubService.createAndPushToGitHub(projectDir, request);
                if (githubInfo != null && githubInfo.isPushSuccessful()) {
                    String repoMessage = "Successfully pushed to GitHub: " + githubInfo.getRepositoryUrl();
                    messages.add(repoMessage);
                    log.info(repoMessage);
                }
            } catch (CodeGenerationException e) {
                log.error("Failed to push to GitHub", e);
                String errorMsg = e.getMessage();

                // Provide helpful error messages
                if (errorMsg.contains("already exists")) {
                    messages.add("Warning: Repository already exists. " +
                            "The code was generated successfully but not pushed to GitHub. " +
                            "Please use a different repository name or delete the existing repository.");
                } else if (errorMsg.contains("401") || errorMsg.contains("authentication")) {
                    messages.add("Warning: GitHub authentication failed. " +
                            "Please verify your GitHub token is valid and has 'repo' scope.");
                } else if (errorMsg.contains("403") || errorMsg.contains("permission")) {
                    messages.add("Warning: Permission denied. " +
                            "Please verify you have write access to this repository/organization.");
                } else {
                    messages.add("Warning: Failed to push to GitHub - " + errorMsg);
                }
                // Don't fail the entire generation if GitHub push fails
            } catch (Exception e) {
                log.error("Unexpected error during GitHub push", e);
                messages.add("Warning: Unexpected error pushing to GitHub - " + e.getMessage());
            }
        }
        return githubInfo;
    }

    /**
     * Sets up required directories for code generation
     */
    private Path setupDirectories(String generationId) throws IOException {
        Path outputBaseDir = Paths.get(config.getOutputBaseDir());
        Path tempDir = Paths.get(config.getTempDir());
        Path outputDir = outputBaseDir.resolve(generationId);

        fileOpsService.createDirectory(outputBaseDir);
        fileOpsService.createDirectory(tempDir);
        fileOpsService.createDirectory(outputDir);

        return outputDir;
    }

    /**
     * Downloads and validates OpenAPI specification
     */
    private Path downloadSpecification(CodeGenerationRequest request, List<String> messages) {
        Path tempDir = Paths.get(config.getTempDir());
        Path specPath = tempDir.resolve("openapi-spec-" + UUID.randomUUID() + ".yaml");

        // Use new method that handles URL, raw content, or file upload
        specDownloadService.getSpecification(request, specPath);

        if (!specDownloadService.validateSpecification(specPath)) {
            messages.add("Warning: Specification validation returned warnings");
        }

        // Determine source type for message
        if (request.getSpecContent() != null && !request.getSpecContent().isEmpty()) {
            messages.add("Processed OpenAPI specification from raw content");
        } else {
            messages.add("Downloaded OpenAPI specification from URL");
        }

        return specPath;
    }

    /**
     * Updates .openapi-generator-ignore file
     */
    private void updateGeneratorIgnoreFile(Path projectDir, List<String> messages) {
        Path ignoreFile = projectDir.resolve(".openapi-generator-ignore");

        try {
            if (Files.exists(ignoreFile)) {
                String content = Files.readString(ignoreFile);

                // Add custom ignores
                if (!content.contains("# Custom ignores")) {
                    content += "\n\n# Custom ignores\nsrc/main/resources/application.properties\n";
                    Files.writeString(ignoreFile, content);
                    messages.add("Updated .openapi-generator-ignore");
                }
            }
        } catch (IOException e) {
            log.warn("Failed to update .openapi-generator-ignore", e);
        }
    }

    /**
     * Updates README with project-specific information
     */
    private void updateReadme(Path projectDir, CodeGenerationRequest request, List<String> messages) {
        Path readmePath = projectDir.resolve("README.md");

        try {
            String readme = String.format(AppConstants.README_MD_CONTENT,
                    request.getArtifactId(),
                    request.getGroupName(),
                    request.getArtifactId(),
                    request.getVersion(),
                    request.getBasePackage()
            );

            Files.writeString(readmePath, readme);
            messages.add("Created README.md");

        } catch (IOException e) {
            log.warn("Failed to create README", e);
        }
    }

    /**
     * Creates ZIP archive of the project
     */
    private Path createArchive(Path projectDir, String generationId, String artifactId) throws IOException {
        Path outputBaseDir = Paths.get(config.getOutputBaseDir());
        Path archivePath = outputBaseDir.resolve(generationId + "/" + artifactId + ".zip");

        return fileOpsService.createArchive(projectDir, archivePath);
    }

    /**
     * Cleans up temporary files
     */
    private void cleanupTempFiles() {
        try {
            Path tempDir = Paths.get(config.getTempDir());
            if (Files.exists(tempDir)) {
                Files.list(tempDir)
                        .filter(path -> path.getFileName().toString().startsWith("openapi-spec-"))
                        .forEach(path -> {
                            if (Files.isDirectory(path)) {
                                fileOpsService.deleteDirectory(path);
                            } else {
                                fileOpsService.deleteFile(path);
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup temp files", e);
        }
    }

    /**
     * Performs scheduled cleanup of old generated projects
     */
    public void scheduledCleanup() {
        if (config.getCleanupHours() > 0) {
            Path outputBaseDir = Paths.get(config.getOutputBaseDir());
            fileOpsService.cleanupOldProjects(outputBaseDir, config.getCleanupHours());
        }
    }
}