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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Main service orchestrating the code generation process from OpenAPI specifications.
 * This service coordinates the entire code generation workflow including specification
 * download/validation, OpenAPI code generation, template enhancement, POM/properties
 * merging, and optional GitHub repository creation and push.
 *
 * <p>The generation process follows these steps:
 * <ol>
 *   <li>Setup directories for code generation</li>
 *   <li>Download or process OpenAPI specification</li>
 *   <li>Generate code using OpenAPI Generator</li>
 *   <li>Enhance project with custom templates</li>
 *   <li>Generate Service and Repository classes</li>
 *   <li>Update configuration files (.openapi-generator-ignore, README)</li>
 *   <li>Create archive if requested</li>
 *   <li>Push to GitHub if enabled</li>
 *   <li>Cleanup temporary files</li>
 * </ol>
 *
 * @author ProbeStack
 * @version 1.0
 * @since 1.0
 */
@Service
public class CodeGenerationService {

    private static final Logger log = LoggerFactory.getLogger(CodeGenerationService.class);

    /**
     * Configuration for code generator including directories, templates, and OpenAPI settings
     */
    private final CodeGeneratorConfig config;

    /**
     * Service for downloading and processing OpenAPI specifications
     */
    private final SpecificationDownloadService specDownloadService;

    /**
     * Service for generating code using OpenAPI Generator
     */
    private final OpenApiGeneratorService generatorService;

    /**
     * Service for file system operations
     */
    private final FileOperationsService fileOpsService;

    /**
     * Service for processing templates and replacing placeholders
     */
    private final TemplateProcessingService templateService;

    /**
     * Service for merging Maven POM files
     */
    private final PomMergeService pomMergeService;

    /**
     * Service for advanced POM customization including plugins
     */
    private final PomCustomizationService pomCustomizationService;

    /**
     * Service for merging application properties files
     */
    private final PropertiesMergeService propertiesMergeService;

    /**
     * Service for GitHub operations - repository creation and code push
     */
    private final GitHubService gitHubService;

    /**
     * Service to enhance generated projects with custom templates
     */
    private final TemplateEnhancementService templateEnhancementService;

    /**
     * Service for validating code generation requests
     */
    private final RequestValidationService validationService;

    /**
     * GitHub configuration from application properties
     */
    private final GitHubConfig githubPropertiesConfig;

    /**
     * Orchestrator for generating Service and Repository classes
     */
    private final CodeGenerationOrchestrator codeGenerationOrchestrator;

    /**
     * Scanner for locating controller class files
     */
    private final ControllerPathScanner controllerPathScanner;

    /**
     * Extractor for parsing controller metadata
     */
    private final ControllerMetadataExtractor controllerMetadataExtractor;

    /**
     * Constructs a new CodeGenerationService with all required dependencies.
     *
     * @param config Configuration for code generator
     * @param specDownloadService Service for downloading OpenAPI specifications
     * @param generatorService Service for OpenAPI code generation
     * @param fileOpsService Service for file operations
     * @param templateService Service for template processing
     * @param pomMergeService Service for POM merging
     * @param pomCustomizationService Service for POM customization
     * @param propertiesMergeService Service for properties merging
     * @param gitHubService Service for GitHub operations
     * @param templateEnhancementService Service for template enhancement
     * @param validationService Service for request validation
     * @param githubPropertiesConfig GitHub configuration
     * @param codeGenerationOrchestrator Orchestrator for artifact generation
     * @param controllerPathScanner Scanner for controller paths
     * @param controllerMetadataExtractor Extractor for controller metadata
     */
    @Autowired
    public CodeGenerationService(CodeGeneratorConfig config,
                                  SpecificationDownloadService specDownloadService,
                                  OpenApiGeneratorService generatorService,
                                  FileOperationsService fileOpsService,
                                  TemplateProcessingService templateService,
                                  PomMergeService pomMergeService,
                                  PomCustomizationService pomCustomizationService,
                                  PropertiesMergeService propertiesMergeService,
                                  GitHubService gitHubService,
                                  TemplateEnhancementService templateEnhancementService,
                                  RequestValidationService validationService,
                                  GitHubConfig githubPropertiesConfig,
                                  CodeGenerationOrchestrator codeGenerationOrchestrator,
                                  ControllerPathScanner controllerPathScanner,
                                  ControllerMetadataExtractor controllerMetadataExtractor) {
        this.config = config;
        this.specDownloadService = specDownloadService;
        this.generatorService = generatorService;
        this.fileOpsService = fileOpsService;
        this.templateService = templateService;
        this.pomMergeService = pomMergeService;
        this.pomCustomizationService = pomCustomizationService;
        this.propertiesMergeService = propertiesMergeService;
        this.gitHubService = gitHubService;
        this.templateEnhancementService = templateEnhancementService;
        this.validationService = validationService;
        this.githubPropertiesConfig = githubPropertiesConfig;
        this.codeGenerationOrchestrator = codeGenerationOrchestrator;
        this.controllerPathScanner = controllerPathScanner;
        this.controllerMetadataExtractor = controllerMetadataExtractor;
    }

    /**
     * Generates a complete Spring Boot project from an OpenAPI specification.
     * This method orchestrates the entire code generation workflow including
     * downloading the specification, generating code, enhancing with templates,
     * creating archives, and optionally pushing to GitHub.
     *
     * <p>The method handles all exceptions and returns a response object
     * indicating success or failure along with relevant details and messages.
     *
     * @param request The code generation request containing specification URL/content,
     *                project metadata (groupId, artifactId, version), base package,
     *                GitHub configuration, and other generation options
     * @return CodeGenerationResponse containing generation ID, project path, status,
     *         timestamp, GitHub repository info (if applicable), and status messages.
     *         Returns SUCCESS status on successful generation, FAILED status otherwise
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
            CodeGenerationResponse.GitHubRepositoryInfo githubInfo = createRepoAndPushCode(request, projectDir, messages);

            // Cleanup temporary files
            cleanupTempFiles();

            log.info("Code generation completed successfully - ID: {}", generationId);

            // create response
            return CodeGenerationResponse.builder()
                    .generationId(generationId)
                    .projectPath(projectDir.toString())
                  //  .archivePath(archivePath != null ? archivePath.toString() : null)
                    .status(CodeGenerationResponse.GenerationStatus.SUCCESS)
                    .timestamp(LocalDateTime.now())
                   // .generatedFiles(generatedFiles)
                   // .messages(messages)
                    .gitHubRepositoryInfo(githubInfo) // NEW: Include GitHub info
                    .build();

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

    /**
     * Generates Service and Repository classes based on generated controller files.
     * Scans the generated project for controller files and creates corresponding
     * service and repository classes in appropriate packages.
     *
     * @param request The code generation request containing base package information
     * @param projectDir The path to the generated project directory
     * @throws Exception if scanning controllers or generating artifacts fails
     */
    private void generateServiceAndRepositoryClasses(CodeGenerationRequest request, Path projectDir) throws Exception {
        List<File> generatedProjectControllerFiles = controllerPathScanner.getGeneratedControllerClassFiles(projectDir.toString(), request.getBasePackage(),"api");
        File outputPathForServiceAndRepoClasses = controllerPathScanner.constructOutputPackageDirector(projectDir.toString(), request.getBasePackage());
        codeGenerationOrchestrator.generateAllArtifacts(generatedProjectControllerFiles, projectDir.toString(), request.getBasePackage(),outputPathForServiceAndRepoClasses);
    }

    /**
     * Creates a GitHub repository and pushes the generated code if GitHub integration is enabled.
     * Handles errors gracefully by logging warnings without failing the entire generation process.
     *
     * @param request The code generation request containing GitHub configuration
     * @param projectDir The path to the generated project directory to push
     * @param messages List of status messages to append GitHub push results
     * @return GitHubRepositoryInfo containing repository URL, clone URL, SSH URL, branch name,
     *         commit SHA, and push success status. Returns null if GitHub integration is disabled
     */
    private CodeGenerationResponse.GitHubRepositoryInfo createRepoAndPushCode(CodeGenerationRequest request, Path projectDir, List<String> messages) {
        CodeGenerationResponse.GitHubRepositoryInfo githubInfo = null;
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
     * Sets up required directories for code generation.
     * Creates output base directory, temp directory, and generation-specific output directory.
     *
     * @param generationId Unique identifier for this generation session
     * @return Path to the generation-specific output directory
     * @throws IOException if directory creation fails
     */
    private Path setupDirectories(String generationId) throws IOException {
        Path outputBaseDir = Paths.get(config.getDirectories().getOutputBase());
        Path tempDir = Paths.get(config.getDirectories().getTemp());
        Path outputDir = outputBaseDir.resolve(generationId);

        fileOpsService.createDirectory(outputBaseDir);
        fileOpsService.createDirectory(tempDir);
        fileOpsService.createDirectory(outputDir);

        return outputDir;
    }

    /**
     * Downloads and validates OpenAPI specification from URL or processes raw content.
     * Determines the specification source (URL, raw content, or file upload) and
     * handles it appropriately. Adds status messages about the source type.
     *
     * @param request The code generation request containing either specUrl or specContent
     * @param messages List of status messages to append specification processing results
     * @return Path to the downloaded or saved specification file
     */
    private Path downloadSpecification(CodeGenerationRequest request, List<String> messages) {
        Path tempDir = Paths.get(config.getDirectories().getTemp());
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
     * Updates .openapi-generator-ignore file to preserve custom configuration files.
     * Adds custom ignore patterns to prevent OpenAPI Generator from overwriting
     * certain files like application.properties during regeneration.
     *
     * @param projectDir The path to the generated project directory
     * @param messages List of status messages to append update results
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
     * Updates README with project-specific information.
     * Creates a README.md file with project metadata including artifact ID,
     * group name, version, and base package information.
     *
     * @param projectDir The path to the generated project directory
     * @param request The code generation request containing project metadata
     * @param messages List of status messages to append README creation results
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
     * Creates ZIP archive of the generated project.
     * Packages the entire project directory into a ZIP file for easy distribution.
     *
     * @param projectDir The path to the project directory to archive
     * @param generationId Unique identifier for this generation session
     * @param artifactId The artifact ID used for naming the archive file
     * @return Path to the created ZIP archive
     * @throws IOException if archive creation fails
     */
    private Path createArchive(Path projectDir, String generationId, String artifactId) throws IOException {
        Path outputBaseDir = Paths.get(config.getDirectories().getOutputBase());
        Path archivePath = outputBaseDir.resolve(generationId + "/" + artifactId + ".zip");

        return fileOpsService.createArchive(projectDir, archivePath);
    }

    /**
     * Cleans up temporary specification files from the temp directory.
     * Removes all files starting with "openapi-spec-" to free up disk space.
     * Logs warnings if cleanup fails but doesn't throw exceptions.
     */
    private void cleanupTempFiles() {
        try {
            Path tempDir = Paths.get(config.getDirectories().getTemp());
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
     * Performs scheduled cleanup of old generated projects.
     * Deletes generated projects older than the configured retention period.
     * This method is typically called by a scheduled task to prevent disk space issues.
     * Only performs cleanup if cleanup hours is configured (greater than 0).
     */
    public void scheduledCleanup() {
        if (config.getCleanup().getHours() > 0) {
            Path outputBaseDir = Paths.get(config.getDirectories().getOutputBase());
            fileOpsService.cleanupOldProjects(outputBaseDir, config.getCleanup().getHours());
        }
    }
}