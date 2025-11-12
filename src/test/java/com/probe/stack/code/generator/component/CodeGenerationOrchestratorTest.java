package com.probe.stack.code.generator.component;

import com.probe.stack.code.generator.parser.ControllerMetadataExtractor;
import com.probe.stack.code.generator.parser.ControllerMetadataExtractor.ControllerMetadata;
import com.probe.stack.code.generator.parser.ControllerMetadataExtractor.MethodMetadata;
import com.probe.stack.code.generator.util.ControllerPathScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CodeGenerationOrchestrator
 * Tests orchestration of service, repository, and controller generation
 */
@ExtendWith(MockitoExtension.class)
class CodeGenerationOrchestratorTest {

    @Mock
    private ControllerMetadataExtractor metadataExtractor;

    @Mock
    private ServiceClassGenerator serviceGenerator;

    @Mock
    private RepositoryInterfaceGenerator repositoryGenerator;

    @Mock
    private ExistingControllerEnhancer controllerEnhancer;

    @Mock
    private ControllerPathScanner controllerLocator;

    @InjectMocks
    private CodeGenerationOrchestrator orchestrator;

    @TempDir
    Path tempDir;

    private File testApiFile;
    private ControllerMetadata testMetadata;

    @BeforeEach
    void setUp() throws Exception {
        testApiFile = tempDir.resolve("UserApi.java").toFile();
        testApiFile.createNewFile();

        testMetadata = new ControllerMetadata();
        testMetadata.setClassName("UserApi");
        testMetadata.setPackageName("com.example.api");
        testMetadata.setEntityClass("User");
        testMetadata.setMethods(new ArrayList<>());

        // Add sample method
        MethodMetadata method = new MethodMetadata();
        method.setMethodName("createUser");
        method.setReturnType("ResponseEntity<User>");
        method.setParameters(new ArrayList<>());
        testMetadata.getMethods().add(method);
    }

    // ==================== Generate All Artifacts Tests ====================

    @Test
    void testGenerateAllArtifacts_Success_GeneratesAllComponents() throws Exception {
        // Arrange
        List<File> apiFiles = List.of(testApiFile);
        File outputDir = tempDir.toFile();
        String projectDirectory = tempDir.toString();
        String basePackage = "com.example";

        when(metadataExtractor.extractMetadata(testApiFile)).thenReturn(testMetadata);
        when(controllerLocator.findExistingController(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(testApiFile));

        // Act
        orchestrator.generateAllArtifacts(apiFiles, projectDirectory, basePackage, outputDir);

        // Assert
        verify(metadataExtractor).extractMetadata(testApiFile);
        verify(serviceGenerator).generateServiceClass(testMetadata, outputDir);
        verify(repositoryGenerator).generateRepositoryInterface(testMetadata, outputDir);
        verify(controllerEnhancer).enhanceExistingController(eq(testMetadata), eq(testApiFile), eq(outputDir));
    }

    @Test
    void testGenerateAllArtifacts_NoExistingController_SkipsEnhancement() throws Exception {
        // Arrange
        List<File> apiFiles = List.of(testApiFile);
        File outputDir = tempDir.toFile();

        when(metadataExtractor.extractMetadata(testApiFile)).thenReturn(testMetadata);
        when(controllerLocator.findExistingController(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act
        orchestrator.generateAllArtifacts(apiFiles, tempDir.toString(), "com.example", outputDir);

        // Assert
        verify(serviceGenerator).generateServiceClass(testMetadata, outputDir);
        verify(repositoryGenerator).generateRepositoryInterface(testMetadata, outputDir);
        verify(controllerEnhancer, never()).enhanceExistingController(any(), any(), any());
    }

    @Test
    void testGenerateAllArtifacts_NoEntityClass_SkipsRepository() throws Exception {
        // Arrange
        testMetadata.setEntityClass(null);
        List<File> apiFiles = List.of(testApiFile);
        File outputDir = tempDir.toFile();

        when(metadataExtractor.extractMetadata(testApiFile)).thenReturn(testMetadata);
        when(controllerLocator.findExistingController(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(testApiFile));

        // Act
        orchestrator.generateAllArtifacts(apiFiles, tempDir.toString(), "com.example", outputDir);

        // Assert
        verify(serviceGenerator).generateServiceClass(testMetadata, outputDir);
        verify(repositoryGenerator, never()).generateRepositoryInterface(any(), any());
    }

    @Test
    void testGenerateAllArtifacts_NullEntityClass_SkipsRepository() throws Exception {
        // Arrange
        testMetadata.setEntityClass("null");
        List<File> apiFiles = List.of(testApiFile);
        File outputDir = tempDir.toFile();

        when(metadataExtractor.extractMetadata(testApiFile)).thenReturn(testMetadata);
        when(controllerLocator.findExistingController(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(testApiFile));

        // Act
        orchestrator.generateAllArtifacts(apiFiles, tempDir.toString(), "com.example", outputDir);

        // Assert
        verify(repositoryGenerator, never()).generateRepositoryInterface(any(), any());
    }

    @Test
    void testGenerateAllArtifacts_MultipleApiFiles_ProcessesAll() throws Exception {
        // Arrange
        File secondApiFile = tempDir.resolve("OrderApi.java").toFile();
        secondApiFile.createNewFile();

        ControllerMetadata secondMetadata = new ControllerMetadata();
        secondMetadata.setClassName("OrderApi");
        secondMetadata.setPackageName("com.example.api");
        secondMetadata.setEntityClass("Order");
        secondMetadata.setMethods(List.of(new MethodMetadata()));

        List<File> apiFiles = List.of(testApiFile, secondApiFile);
        File outputDir = tempDir.toFile();

        when(metadataExtractor.extractMetadata(testApiFile)).thenReturn(testMetadata);
        when(metadataExtractor.extractMetadata(secondApiFile)).thenReturn(secondMetadata);
        when(controllerLocator.findExistingController(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act
        orchestrator.generateAllArtifacts(apiFiles, tempDir.toString(), "com.example", outputDir);

        // Assert
        verify(metadataExtractor, times(2)).extractMetadata(any());
        verify(serviceGenerator, times(2)).generateServiceClass(any(), eq(outputDir));
        verify(repositoryGenerator, times(2)).generateRepositoryInterface(any(), eq(outputDir));
    }

    @Test
    void testGenerateAllArtifacts_ServiceGenerationFailure_ContinuesWithOthers() throws Exception {
        // Arrange
        List<File> apiFiles = List.of(testApiFile);
        File outputDir = tempDir.toFile();

        when(metadataExtractor.extractMetadata(testApiFile)).thenReturn(testMetadata);
        doThrow(new RuntimeException("Service generation failed"))
                .when(serviceGenerator).generateServiceClass(any(), any());

        // Act
        orchestrator.generateAllArtifacts(apiFiles, tempDir.toString(), "com.example", outputDir);

        // Assert
        verify(metadataExtractor).extractMetadata(testApiFile);
        verify(serviceGenerator).generateServiceClass(testMetadata, outputDir);
        // Should continue despite failure
    }

    @Test
    void testGenerateAllArtifacts_MetadataExtractionFailure_SkipsFile() throws Exception {
        // Arrange
        List<File> apiFiles = List.of(testApiFile);
        File outputDir = tempDir.toFile();

        when(metadataExtractor.extractMetadata(testApiFile))
                .thenThrow(new RuntimeException("Metadata extraction failed"));

        // Act
        orchestrator.generateAllArtifacts(apiFiles, tempDir.toString(), "com.example", outputDir);

        // Assert
        verify(metadataExtractor).extractMetadata(testApiFile);
        verify(serviceGenerator, never()).generateServiceClass(any(), any());
        verify(repositoryGenerator, never()).generateRepositoryInterface(any(), any());
    }

    @Test
    void testGenerateAllArtifacts_ControllerEnhancementFailure_Continues() throws Exception {
        // Arrange
        List<File> apiFiles = List.of(testApiFile);
        File outputDir = tempDir.toFile();

        when(metadataExtractor.extractMetadata(testApiFile)).thenReturn(testMetadata);
        when(controllerLocator.findExistingController(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(testApiFile));
        doThrow(new RuntimeException("Enhancement failed"))
                .when(controllerEnhancer).enhanceExistingController(any(), any(), any());

        // Act
        orchestrator.generateAllArtifacts(apiFiles, tempDir.toString(), "com.example", outputDir);

        // Assert
        verify(controllerEnhancer).enhanceExistingController(any(), any(), any());
        // Should log error and continue
    }

    @Test
    void testGenerateAllArtifacts_EmptyApiFileList_DoesNothing() {
        // Arrange
        List<File> emptyList = new ArrayList<>();
        File outputDir = tempDir.toFile();

        // Act
        orchestrator.generateAllArtifacts(emptyList, tempDir.toString(), "com.example", outputDir);

        // Assert
        verify(metadataExtractor, never()).extractMetadata(any());
        verify(serviceGenerator, never()).generateServiceClass(any(), any());
        verify(repositoryGenerator, never()).generateRepositoryInterface(any(), any());
    }

    @Test
    void testGenerateAllArtifacts_WithComplexEntityName_Success() throws Exception {
        // Arrange
        testMetadata.setEntityClass("CompanyRegistration");
        testMetadata.setClassName("CompanyRegistrationApi");
        List<File> apiFiles = List.of(testApiFile);
        File outputDir = tempDir.toFile();

        when(metadataExtractor.extractMetadata(testApiFile)).thenReturn(testMetadata);
        when(controllerLocator.findExistingController(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act
        orchestrator.generateAllArtifacts(apiFiles, tempDir.toString(), "com.example", outputDir);

        // Assert
        verify(serviceGenerator).generateServiceClass(testMetadata, outputDir);
        verify(repositoryGenerator).generateRepositoryInterface(testMetadata, outputDir);
    }

    @Test
    void testGenerateAllArtifacts_WithApiControllerSuffix_HandlesCorrectly() throws Exception {
        // Arrange
        testMetadata.setClassName("UserApiController");
        List<File> apiFiles = List.of(testApiFile);
        File outputDir = tempDir.toFile();

        when(metadataExtractor.extractMetadata(testApiFile)).thenReturn(testMetadata);
        when(controllerLocator.findExistingController(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act
        orchestrator.generateAllArtifacts(apiFiles, tempDir.toString(), "com.example", outputDir);

        // Assert
        verify(serviceGenerator).generateServiceClass(testMetadata, outputDir);
        verify(repositoryGenerator).generateRepositoryInterface(testMetadata, outputDir);
    }

    @Test
    void testGenerateAllArtifacts_WithMethodsButNoEntity_ServiceGenerationAttempted() throws Exception {
        // Arrange
        testMetadata.setEntityClass(null);
        List<File> apiFiles = List.of(testApiFile);
        File outputDir = tempDir.toFile();

        when(metadataExtractor.extractMetadata(testApiFile)).thenReturn(testMetadata);
        when(controllerLocator.findExistingController(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        // Act
        orchestrator.generateAllArtifacts(apiFiles, tempDir.toString(), "com.example", outputDir);

        // Assert
        verify(serviceGenerator).generateServiceClass(testMetadata, outputDir);
    }
}
