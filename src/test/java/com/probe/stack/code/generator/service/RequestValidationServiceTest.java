package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for RequestValidationService
 * Tests validation logic for all request scenarios
 */
@ExtendWith(MockitoExtension.class)
class RequestValidationServiceTest {

    @InjectMocks
    private RequestValidationService validationService;

    private CodeGenerationRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .version("1.0.0")
                .build();
    }

    // ==================== Specification Source Validation Tests ====================

    @Test
    void testValidateSpecificationSource_WithUrl_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateSpecificationSource(validRequest));
    }

    @Test
    void testValidateSpecificationSource_WithContent_Success() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent("openapi: 3.0.0")
                .specContentType("yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateSpecificationSource(request));
    }

    @Test
    void testValidateSpecificationSource_WithBothUrlAndContent_Success() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .specContent("openapi: 3.0.0")
                .specContentType("yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act & Assert - Should not throw, but will log warning
        assertDoesNotThrow(() -> validationService.validateSpecificationSource(request));
    }

    @Test
    void testValidateSpecificationSource_WithoutUrlOrContent_ThrowsException() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class,
                () -> validationService.validateSpecificationSource(request));
        assertTrue(exception.getMessage().contains("Either 'openApiSpecUrl' or 'specContent' must be provided"));
    }

    @Test
    void testValidateSpecificationSource_WithEmptyUrl_ThrowsException() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .openApiSpecUrl("")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class,
                () -> validationService.validateSpecificationSource(request));
        assertTrue(exception.getMessage().contains("Either 'openApiSpecUrl' or 'specContent' must be provided"));
    }

    @Test
    void testValidateSpecificationSource_WithEmptyContent_ThrowsException() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent("")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class,
                () -> validationService.validateSpecificationSource(request));
        assertTrue(exception.getMessage().contains("Either 'openApiSpecUrl' or 'specContent' must be provided"));
    }

    @Test
    void testValidateSpecificationSource_WithNullUrl_ThrowsException() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .openApiSpecUrl(null)
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class,
                () -> validationService.validateSpecificationSource(request));
        assertTrue(exception.getMessage().contains("Either 'openApiSpecUrl' or 'specContent' must be provided"));
    }

    // ==================== Content Type Validation Tests ====================

    @Test
    void testValidateContentType_WithYamlContentType_Success() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent("openapi: 3.0.0")
                .specContentType("yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateContentType(request));
        assertEquals("yaml", request.getSpecContentType());
    }

    @Test
    void testValidateContentType_WithJsonContentType_Success() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent("{\"openapi\": \"3.0.0\"}")
                .specContentType("json")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateContentType(request));
        assertEquals("json", request.getSpecContentType());
    }

    @Test
    void testValidateContentType_AutoDetectJson_Success() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent("{\"openapi\": \"3.0.0\"}")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act
        validationService.validateContentType(request);

        // Assert
        assertEquals("json", request.getSpecContentType());
    }

    @Test
    void testValidateContentType_AutoDetectJsonWithArray_Success() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent("[{\"openapi\": \"3.0.0\"}]")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act
        validationService.validateContentType(request);

        // Assert
        assertEquals("json", request.getSpecContentType());
    }

    @Test
    void testValidateContentType_AutoDetectYaml_Success() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent("openapi: 3.0.0\ninfo:\n  title: Test API")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act
        validationService.validateContentType(request);

        // Assert
        assertEquals("yaml", request.getSpecContentType());
    }

    @Test
    void testValidateContentType_WithWhitespace_AutoDetectsCorrectly() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent("   {\"openapi\": \"3.0.0\"}")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act
        validationService.validateContentType(request);

        // Assert
        assertEquals("json", request.getSpecContentType());
    }

    @Test
    void testValidateContentType_WithNoContent_DoesNothing() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateContentType(request));
        assertNull(request.getSpecContentType());
    }

    @Test
    void testValidateContentType_WithEmptyContentType_AutoDetects() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent("{\"test\": \"value\"}")
                .specContentType("")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act
        validationService.validateContentType(request);

        // Assert
        assertEquals("json", request.getSpecContentType());
    }

    // ==================== GitHub Configuration Validation Tests ====================

    @Test
    void testValidateGitHubConfig_Disabled_Success() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateGitHubConfig(request));
    }

    @Test
    void testValidateGitHubConfig_EnabledWithValidConfig_Success() {
        // Arrange
        CodeGenerationRequest.GitHubConfig gitHubConfig = CodeGenerationRequest.GitHubConfig.builder()
                .enabled(true)
                .description("Test repository")
                .isPrivate(true)
                .build();

        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .gitHubConfig(gitHubConfig)
                .githubToken("test-token")
                .organization("test-org")
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateGitHubConfig(request));
    }

    @Test
    void testValidateGitHubConfig_EnabledWithoutToken_ThrowsException() {
        // Arrange
        CodeGenerationRequest.GitHubConfig gitHubConfig = CodeGenerationRequest.GitHubConfig.builder()
                .enabled(true)
                .build();

        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .gitHubConfig(gitHubConfig)
                .organization("test-org")
                .build();

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class,
                () -> validationService.validateGitHubConfig(request));
        assertTrue(exception.getMessage().contains("GitHub token is required"));
    }

    @Test
    void testValidateGitHubConfig_EnabledWithEmptyToken_ThrowsException() {
        // Arrange
        CodeGenerationRequest.GitHubConfig gitHubConfig = CodeGenerationRequest.GitHubConfig.builder()
                .enabled(true)
                .build();

        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .gitHubConfig(gitHubConfig)
                .githubToken("")
                .organization("test-org")
                .build();

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class,
                () -> validationService.validateGitHubConfig(request));
        assertTrue(exception.getMessage().contains("GitHub token is required"));
    }

    @Test
    void testValidateGitHubConfig_EnabledWithoutOrganization_ThrowsException() {
        // Arrange
        CodeGenerationRequest.GitHubConfig gitHubConfig = CodeGenerationRequest.GitHubConfig.builder()
                .enabled(true)
                .build();

        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .gitHubConfig(gitHubConfig)
                .githubToken("test-token")
                .build();

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class,
                () -> validationService.validateGitHubConfig(request));
        assertTrue(exception.getMessage().contains("GitHub organization/username is required"));
    }

    @Test
    void testValidateGitHubConfig_EnabledWithEmptyOrganization_ThrowsException() {
        // Arrange
        CodeGenerationRequest.GitHubConfig gitHubConfig = CodeGenerationRequest.GitHubConfig.builder()
                .enabled(true)
                .build();

        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .gitHubConfig(gitHubConfig)
                .githubToken("test-token")
                .organization("")
                .build();

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class,
                () -> validationService.validateGitHubConfig(request));
        assertTrue(exception.getMessage().contains("GitHub organization/username is required"));
    }

    @Test
    void testValidateGitHubConfig_NullGitHubConfig_Success() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .gitHubConfig(null)
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateGitHubConfig(request));
    }

    // ==================== Full Request Validation Tests ====================

    @Test
    void testValidateRequest_ValidRequestWithUrl_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateRequest(validRequest));
    }

    @Test
    void testValidateRequest_ValidRequestWithContent_Success() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .specContent("openapi: 3.0.0")
                .specContentType("yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateRequest(request));
    }

    @Test
    void testValidateRequest_InvalidRequest_ThrowsException() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        // Act & Assert
        assertThrows(CodeGenerationException.class,
                () -> validationService.validateRequest(request));
    }

    @Test
    void testValidateRequest_WithGitHubConfigEnabled_ValidatesAll() {
        // Arrange
        CodeGenerationRequest.GitHubConfig gitHubConfig = CodeGenerationRequest.GitHubConfig.builder()
                .enabled(true)
                .build();

        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .gitHubConfig(gitHubConfig)
                .githubToken("test-token")
                .organization("test-org")
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> validationService.validateRequest(request));
    }

    @Test
    void testValidateRequest_WithInvalidGitHubConfig_ThrowsException() {
        // Arrange
        CodeGenerationRequest.GitHubConfig gitHubConfig = CodeGenerationRequest.GitHubConfig.builder()
                .enabled(true)
                .build();

        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .gitHubConfig(gitHubConfig)
                .build();

        // Act & Assert
        assertThrows(CodeGenerationException.class,
                () -> validationService.validateRequest(request));
    }
}
