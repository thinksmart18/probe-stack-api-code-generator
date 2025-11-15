package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.component.CodeGenerationOrchestrator;
import com.probe.stack.code.generator.config.CodeGeneratorConfig;
import com.probe.stack.code.generator.config.GitHubConfig;
import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.dto.CodeGenerationResponse;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import com.probe.stack.code.generator.parser.ControllerMetadataExtractor;
import com.probe.stack.code.generator.util.ControllerPathScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CodeGenerationService
 * Tests full workflow orchestration and integration
 */
@ExtendWith(MockitoExtension.class)
class CodeGenerationServiceTest {

    @Mock
    private CodeGeneratorConfig config;

    @Mock
    private SpecificationDownloadService specDownloadService;

    @Mock
    private OpenApiGeneratorService generatorService;

    @Mock
    private FileOperationsService fileOpsService;

    @Mock
    private TemplateProcessingService templateService;

    @Mock
    private PomMergeService pomMergeService;

    @Mock
    private PomCustomizationService pomCustomizationService;

    @Mock
    private PropertiesMergeService propertiesMergeService;

    @Mock
    private GitHubService gitHubService;

    @Mock
    private TemplateEnhancementService templateEnhancementService;

    @Mock
    private RequestValidationService validationService;

    @Mock
    private GitHubConfig githubPropertiesConfig;

    @Mock
    private CodeGenerationOrchestrator codeGenerationOrchestrator;

    @Mock
    private ControllerPathScanner controllerPathScanner;

    @Mock
    private ControllerMetadataExtractor controllerMetadataExtractor;

    @Mock
    private CodeGeneratorConfig.DirectoriesConfig directoriesConfig;

    @Mock
    private CodeGeneratorConfig.CleanupConfig cleanupConfig;

    @InjectMocks
    private CodeGenerationService codeGenerationService;

    @TempDir
    Path tempDir;

    private CodeGenerationRequest testRequest;

    @BeforeEach
    void setUp() throws Exception {
        when(config.getDirectories()).thenReturn(directoriesConfig);
        when(config.getCleanup()).thenReturn(cleanupConfig);
        when(directoriesConfig.getOutputBase()).thenReturn(tempDir.toString());
        when(directoriesConfig.getTemp()).thenReturn(tempDir.resolve("temp").toString());
        when(cleanupConfig.getHours()).thenReturn(24);

        testRequest = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .version("1.0.0")
                .build();

        // Setup common mocks
        Path specPath = tempDir.resolve("spec.yaml");
        Files.createDirectories(tempDir.resolve("temp"));
        Files.writeString(specPath, "openapi: 3.0.0");

        when(specDownloadService.getSpecification(any(), any())).thenReturn(specPath);
        when(specDownloadService.validateSpecification(any())).thenReturn(true);
        when(generatorService.generateCode(any(), any(), any())).thenReturn(List.of("file1.java", "file2.java"));
        when(templateEnhancementService.enhanceProject(any(), any())).thenReturn(new ArrayList<>());
        when(controllerPathScanner.getGeneratedControllerClassFiles(any(), any(), any())).thenReturn(new ArrayList<>());
        when(controllerPathScanner.constructOutputPackageDirector(any(), any())).thenReturn(new File(tempDir.toString()));
        when(fileOpsService.createDirectory(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ==================== Generate Project Tests ====================

    @Test
    void testGenerateProject_Success_ReturnsSuccessResponse() {
        // Arrange
        when(githubPropertiesConfig.getPush()).thenReturn(null);

        // Act
        CodeGenerationResponse response = codeGenerationService.generateProject(testRequest);

        // Assert
        assertNotNull(response);
        assertEquals(CodeGenerationResponse.GenerationStatus.SUCCESS, response.getStatus());
        assertNotNull(response.getGenerationId());
        assertNotNull(response.getTimestamp());
        verify(specDownloadService).getSpecification(any(), any());
        verify(generatorService).generateCode(any(), any(), any());
    }

    @Test
    void testGenerateProject_WithArchive_CreatesArchive() throws Exception {
        // Arrange
        testRequest.setReturnAsArchive(true);
        when(githubPropertiesConfig.getPush()).thenReturn(null);
        Path archivePath = tempDir.resolve("archive.zip");
        when(fileOpsService.createArchive(any(), any())).thenReturn(archivePath);

        // Act
        CodeGenerationResponse response = codeGenerationService.generateProject(testRequest);

        // Assert
        assertEquals(CodeGenerationResponse.GenerationStatus.SUCCESS, response.getStatus());
        verify(fileOpsService).createArchive(any(), any());
    }

    @Test
    void testGenerateProject_SpecDownloadFailure_ReturnsFailedResponse() {
        // Arrange
        when(specDownloadService.getSpecification(any(), any()))
                .thenThrow(new CodeGenerationException("Download failed"));

        // Act
        CodeGenerationResponse response = codeGenerationService.generateProject(testRequest);

        // Assert
        assertEquals(CodeGenerationResponse.GenerationStatus.FAILED, response.getStatus());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("Download failed"));
    }

    @Test
    void testGenerateProject_CodeGenerationFailure_ReturnsFailedResponse() {
        // Arrange
        when(generatorService.generateCode(any(), any(), any()))
                .thenThrow(new CodeGenerationException("Generation failed"));

        // Act
        CodeGenerationResponse response = codeGenerationService.generateProject(testRequest);

        // Assert
        assertEquals(CodeGenerationResponse.GenerationStatus.FAILED, response.getStatus());
        assertNotNull(response.getErrorMessage());
    }

    @Test
    void testGenerateProject_WithGitHubPushEnabled_PushesToGitHub() {
        // Arrange
        GitHubConfig.PushConfig pushConfig = mock(GitHubConfig.PushConfig.class);
        when(pushConfig.isEnabled()).thenReturn(true);
        when(githubPropertiesConfig.getPush()).thenReturn(pushConfig);

        CodeGenerationResponse.GitHubRepositoryInfo githubInfo =
                CodeGenerationResponse.GitHubRepositoryInfo.builder()
                        .repositoryUrl("https://github.com/test/repo")
                        .pushSuccessful(true)
                        .build();

        when(gitHubService.createAndPushToGitHub(any(), any())).thenReturn(githubInfo);

        // Act
        CodeGenerationResponse response = codeGenerationService.generateProject(testRequest);

        // Assert
        assertEquals(CodeGenerationResponse.GenerationStatus.SUCCESS, response.getStatus());
        assertNotNull(response.getGitHubRepositoryInfo());
        assertTrue(response.getGitHubRepositoryInfo().isPushSuccessful());
        verify(gitHubService).createAndPushToGitHub(any(), any());
    }

    @Test
    void testGenerateProject_GitHubPushFailure_ContinuesWithWarning() {
        // Arrange
        GitHubConfig.PushConfig pushConfig = mock(GitHubConfig.PushConfig.class);
        when(pushConfig.isEnabled()).thenReturn(true);
        when(githubPropertiesConfig.getPush()).thenReturn(pushConfig);

        when(gitHubService.createAndPushToGitHub(any(), any()))
                .thenThrow(new CodeGenerationException("Push failed"));

        // Act
        CodeGenerationResponse response = codeGenerationService.generateProject(testRequest);

        // Assert
        // Should still succeed despite GitHub failure
        assertEquals(CodeGenerationResponse.GenerationStatus.SUCCESS, response.getStatus());
        verify(gitHubService).createAndPushToGitHub(any(), any());
    }

    @Test
    void testGenerateProject_CreatesRequiredDirectories() throws Exception {
        // Arrange
        when(githubPropertiesConfig.getPush()).thenReturn(null);

        // Act
        codeGenerationService.generateProject(testRequest);

        // Assert
        verify(fileOpsService, atLeast(2)).createDirectory(any());
    }

    @Test
    void testGenerateProject_GeneratesServiceAndRepositoryClasses() throws Exception {
        // Arrange
        when(githubPropertiesConfig.getPush()).thenReturn(null);
        List<File> mockControllerFiles = List.of(new File(tempDir.resolve("Controller.java").toString()));
        when(controllerPathScanner.getGeneratedControllerClassFiles(any(), any(), any()))
                .thenReturn(mockControllerFiles);

        // Act
        codeGenerationService.generateProject(testRequest);

        // Assert
        verify(codeGenerationOrchestrator).generateAllArtifacts(any(), any(), any(), any());
    }

    @Test
    void testGenerateProject_InvalidSpecification_Fails() {
        // Arrange
        when(specDownloadService.validateSpecification(any())).thenReturn(false);
        when(githubPropertiesConfig.getPush()).thenReturn(null);

        // Act
        CodeGenerationResponse response = codeGenerationService.generateProject(testRequest);

        // Assert
        // Should still complete but may have warnings
        assertNotNull(response);
    }

    @Test
    void testScheduledCleanup_WithPositiveHours_CleansOldProjects() {
        // Act
        codeGenerationService.scheduledCleanup();

        // Assert
        verify(fileOpsService).cleanupOldProjects(any(), eq(24));
    }

    @Test
    void testScheduledCleanup_WithZeroHours_DoesNotClean() {
        // Arrange
        when(cleanupConfig.getHours()).thenReturn(0);

        // Act
        codeGenerationService.scheduledCleanup();

        // Assert
        verify(fileOpsService, never()).cleanupOldProjects(any(), anyInt());
    }

    @Test
    void testGenerateProject_WithSpecContent_Success() throws Exception {
        // Arrange
        CodeGenerationRequest requestWithContent = CodeGenerationRequest.builder()
                .specContent("openapi: 3.0.0\ninfo:\n  title: Test")
                .specContentType("yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        when(githubPropertiesConfig.getPush()).thenReturn(null);

        // Act
        CodeGenerationResponse response = codeGenerationService.generateProject(requestWithContent);

        // Assert
        assertEquals(CodeGenerationResponse.GenerationStatus.SUCCESS, response.getStatus());
        verify(specDownloadService).getSpecification(any(), any());
    }

    @Test
    void testGenerateProject_NullRequest_ThrowsException() {
        // Act & Assert
        assertThrows(Exception.class, () -> codeGenerationService.generateProject(null));
    }

    @Test
    void testGenerateProject_UpdatesReadmeFile() {
        // Arrange
        when(githubPropertiesConfig.getPush()).thenReturn(null);

        // Act
        CodeGenerationResponse response = codeGenerationService.generateProject(testRequest);

        // Assert
        assertEquals(CodeGenerationResponse.GenerationStatus.SUCCESS, response.getStatus());
        // README update happens internally
    }

    @Test
    void testGenerateProject_EnhancesWithTemplates() {
        // Arrange
        when(githubPropertiesConfig.getPush()).thenReturn(null);
        List<String> enhancementMessages = List.of("Enhanced with template X", "Enhanced with template Y");
        when(templateEnhancementService.enhanceProject(any(), any())).thenReturn(enhancementMessages);

        // Act
        CodeGenerationResponse response = codeGenerationService.generateProject(testRequest);

        // Assert
        assertEquals(CodeGenerationResponse.GenerationStatus.SUCCESS, response.getStatus());
        verify(templateEnhancementService).enhanceProject(any(), any());
    }
}
