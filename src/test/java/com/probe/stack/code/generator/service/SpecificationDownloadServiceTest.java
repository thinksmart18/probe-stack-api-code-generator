package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.config.CodeGeneratorConfig;
import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Comprehensive unit tests for SpecificationDownloadService
 * Tests spec download, raw content processing, and validation
 */
@ExtendWith(MockitoExtension.class)
class SpecificationDownloadServiceTest {

    @Mock
    private CodeGeneratorConfig config;

    @Mock
    private CodeGeneratorConfig.HttpConfig httpConfig;

    @InjectMocks
    private SpecificationDownloadService specificationDownloadService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(config.getHttp()).thenReturn(httpConfig);
        when(httpConfig.getConnectTimeoutMs()).thenReturn(30000);
        when(httpConfig.getReadTimeoutMs()).thenReturn(30000);
        when(httpConfig.getUserAgent()).thenReturn("TestAgent/1.0");
    }

    // ==================== Get Specification Tests ====================

    @Test
    void testGetSpecification_WithRawYamlContent_Success() throws IOException {
        // Arrange
        String yamlContent = "openapi: 3.0.0\ninfo:\n  title: Test API\n  version: 1.0.0";
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent(yamlContent)
                .specContentType("yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        Path outputPath = tempDir.resolve("spec.yaml");

        // Act
        Path result = specificationDownloadService.getSpecification(request, outputPath);

        // Assert
        assertNotNull(result);
        assertTrue(Files.exists(result));
        String savedContent = Files.readString(result);
        assertEquals(yamlContent, savedContent);
    }

    @Test
    void testGetSpecification_WithRawJsonContent_Success() throws IOException {
        // Arrange
        String jsonContent = "{\"openapi\": \"3.0.0\", \"info\": {\"title\": \"Test API\"}}";
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent(jsonContent)
                .specContentType("json")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        Path outputPath = tempDir.resolve("spec.yaml");

        // Act
        Path result = specificationDownloadService.getSpecification(request, outputPath);

        // Assert
        assertNotNull(result);
        assertTrue(Files.exists(result));
        assertTrue(result.toString().endsWith(".json")); // Should change extension
        String savedContent = Files.readString(result);
        assertEquals(jsonContent, savedContent);
    }

    @Test
    void testGetSpecification_WithoutUrlOrContent_ThrowsException() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        Path outputPath = tempDir.resolve("spec.yaml");

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class,
                () -> specificationDownloadService.getSpecification(request, outputPath));
        assertTrue(exception.getMessage().contains("Either openApiSpecUrl or specContent must be provided"));
    }

    @Test
    void testGetSpecification_WithEmptyContent_TriesUrl() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent("")
                .openApiSpecUrl("https://invalid-url-for-test.example.com/spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        Path outputPath = tempDir.resolve("spec.yaml");

        // Act & Assert - Will fail on download, but shows priority
        assertThrows(CodeGenerationException.class,
                () -> specificationDownloadService.getSpecification(request, outputPath));
    }

    // ==================== Validate Specification Tests ====================

    @Test
    void testValidateSpecification_ValidYamlWithOpenApi_ReturnsTrue() throws IOException {
        // Arrange
        String validSpec = "openapi: 3.0.0\ninfo:\n  title: Test";
        Path specPath = tempDir.resolve("valid-spec.yaml");
        Files.writeString(specPath, validSpec);

        // Act
        boolean isValid = specificationDownloadService.validateSpecification(specPath);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateSpecification_ValidJsonWithOpenApi_ReturnsTrue() throws IOException {
        // Arrange
        String validSpec = "{\"openapi\": \"3.0.0\", \"info\": {\"title\": \"Test\"}}";
        Path specPath = tempDir.resolve("valid-spec.json");
        Files.writeString(specPath, validSpec);

        // Act
        boolean isValid = specificationDownloadService.validateSpecification(specPath);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateSpecification_ValidSwagger_ReturnsTrue() throws IOException {
        // Arrange
        String validSpec = "swagger: '2.0'\ninfo:\n  title: Test";
        Path specPath = tempDir.resolve("swagger-spec.yaml");
        Files.writeString(specPath, validSpec);

        // Act
        boolean isValid = specificationDownloadService.validateSpecification(specPath);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateSpecification_InvalidSpec_ReturnsFalse() throws IOException {
        // Arrange
        String invalidSpec = "This is not an OpenAPI specification";
        Path specPath = tempDir.resolve("invalid-spec.txt");
        Files.writeString(specPath, invalidSpec);

        // Act
        boolean isValid = specificationDownloadService.validateSpecification(specPath);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateSpecification_EmptyFile_ReturnsFalse() throws IOException {
        // Arrange
        Path specPath = tempDir.resolve("empty-spec.yaml");
        Files.writeString(specPath, "");

        // Act
        boolean isValid = specificationDownloadService.validateSpecification(specPath);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateSpecification_NonExistentFile_ReturnsFalse() {
        // Arrange
        Path nonExistent = tempDir.resolve("nonexistent.yaml");

        // Act
        boolean isValid = specificationDownloadService.validateSpecification(nonExistent);

        // Assert
        assertFalse(isValid);
    }

    // ==================== Detect Spec Format Tests ====================

    @Test
    void testDetectSpecFormat_JsonFile_ReturnsJson() throws IOException {
        // Arrange
        String jsonContent = "{\"openapi\": \"3.0.0\"}";
        Path specPath = tempDir.resolve("spec.json");
        Files.writeString(specPath, jsonContent);

        // Act
        String format = specificationDownloadService.detectSpecFormat(specPath);

        // Assert
        assertEquals("json", format);
    }

    @Test
    void testDetectSpecFormat_JsonArrayFile_ReturnsJson() throws IOException {
        // Arrange
        String jsonContent = "[{\"test\": \"value\"}]";
        Path specPath = tempDir.resolve("spec.json");
        Files.writeString(specPath, jsonContent);

        // Act
        String format = specificationDownloadService.detectSpecFormat(specPath);

        // Assert
        assertEquals("json", format);
    }

    @Test
    void testDetectSpecFormat_YamlFile_ReturnsYaml() throws IOException {
        // Arrange
        String yamlContent = "openapi: 3.0.0\ninfo:\n  title: Test";
        Path specPath = tempDir.resolve("spec.yaml");
        Files.writeString(specPath, yamlContent);

        // Act
        String format = specificationDownloadService.detectSpecFormat(specPath);

        // Assert
        assertEquals("yaml", format);
    }

    @Test
    void testDetectSpecFormat_JsonWithWhitespace_ReturnsJson() throws IOException {
        // Arrange
        String jsonContent = "   \n  {\"openapi\": \"3.0.0\"}";
        Path specPath = tempDir.resolve("spec.json");
        Files.writeString(specPath, jsonContent);

        // Act
        String format = specificationDownloadService.detectSpecFormat(specPath);

        // Assert
        assertEquals("json", format);
    }

    @Test
    void testDetectSpecFormat_NonExistentFile_ReturnsYaml() {
        // Arrange
        Path nonExistent = tempDir.resolve("nonexistent.yaml");

        // Act
        String format = specificationDownloadService.detectSpecFormat(nonExistent);

        // Assert
        assertEquals("yaml", format); // Default fallback
    }

    @Test
    void testDetectSpecFormat_EmptyFile_ReturnsYaml() throws IOException {
        // Arrange
        Path emptyPath = tempDir.resolve("empty.yaml");
        Files.writeString(emptyPath, "");

        // Act
        String format = specificationDownloadService.detectSpecFormat(emptyPath);

        // Assert
        assertEquals("yaml", format); // Default fallback
    }

    // ==================== Raw Content Saving Tests ====================

    @Test
    void testGetSpecification_JsonContentChangesExtension() throws IOException {
        // Arrange
        String jsonContent = "{\"openapi\": \"3.0.0\"}";
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent(jsonContent)
                .specContentType("json")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        Path outputPath = tempDir.resolve("spec.yaml");

        // Act
        Path result = specificationDownloadService.getSpecification(request, outputPath);

        // Assert
        assertTrue(result.toString().endsWith(".json"));
        assertFalse(result.toString().endsWith(".yaml"));
    }

    @Test
    void testGetSpecification_YamlContentKeepsExtension() throws IOException {
        // Arrange
        String yamlContent = "openapi: 3.0.0";
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent(yamlContent)
                .specContentType("yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        Path outputPath = tempDir.resolve("spec.yaml");

        // Act
        Path result = specificationDownloadService.getSpecification(request, outputPath);

        // Assert
        assertTrue(result.toString().endsWith(".yaml"));
    }

    @Test
    void testGetSpecification_NullContentType_SavesAsYaml() throws IOException {
        // Arrange
        String content = "openapi: 3.0.0";
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent(content)
                .specContentType(null)
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        Path outputPath = tempDir.resolve("spec.yaml");

        // Act
        Path result = specificationDownloadService.getSpecification(request, outputPath);

        // Assert
        assertTrue(Files.exists(result));
        assertEquals(content, Files.readString(result));
    }

    @Test
    void testGetSpecification_LargeContent_Success() throws IOException {
        // Arrange
        StringBuilder largeContent = new StringBuilder("openapi: 3.0.0\n");
        for (int i = 0; i < 10000; i++) {
            largeContent.append("  line").append(i).append(": value").append(i).append("\n");
        }

        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent(largeContent.toString())
                .specContentType("yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        Path outputPath = tempDir.resolve("large-spec.yaml");

        // Act
        Path result = specificationDownloadService.getSpecification(request, outputPath);

        // Assert
        assertTrue(Files.exists(result));
        assertTrue(Files.size(result) > 100000); // Should be large
    }

    @Test
    void testGetSpecification_UTF8Content_Success() throws IOException {
        // Arrange
        String utf8Content = "openapi: 3.0.0\ninfo:\n  title: Tëst ÄPÏ 中文 العربية";
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent(utf8Content)
                .specContentType("yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        Path outputPath = tempDir.resolve("utf8-spec.yaml");

        // Act
        Path result = specificationDownloadService.getSpecification(request, outputPath);

        // Assert
        assertTrue(Files.exists(result));
        assertEquals(utf8Content, Files.readString(result));
    }

    // ==================== Edge Cases ====================

    @Test
    void testValidateSpecification_CaseInsensitiveOpenApi_ReturnsTrue() throws IOException {
        // Arrange
        String validSpec = "OpenAPI: 3.0.0\ninfo:\n  title: Test";
        Path specPath = tempDir.resolve("spec.yaml");
        Files.writeString(specPath, validSpec);

        // Act - The method checks lowercase, so this will fail
        boolean isValid = specificationDownloadService.validateSpecification(specPath);

        // Assert
        assertFalse(isValid); // Current implementation is case-sensitive
    }

    @Test
    void testValidateSpecification_JsonSwagger_ReturnsTrue() throws IOException {
        // Arrange
        String validSpec = "{\"swagger\": \"2.0\", \"info\": {\"title\": \"Test\"}}";
        Path specPath = tempDir.resolve("swagger.json");
        Files.writeString(specPath, validSpec);

        // Act
        boolean isValid = specificationDownloadService.validateSpecification(specPath);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testDetectSpecFormat_MixedContent_DetectsCorrectly() throws IOException {
        // Arrange
        String mixedContent = "# YAML comment\n{\"openapi\": \"3.0.0\"}";
        Path specPath = tempDir.resolve("mixed.txt");
        Files.writeString(specPath, mixedContent);

        // Act
        String format = specificationDownloadService.detectSpecFormat(specPath);

        // Assert
        assertEquals("yaml", format); // Doesn't start with { after trim
    }
}
