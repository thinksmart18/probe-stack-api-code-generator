package com.probe.stack.code.generator.controller;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.dto.CodeGenerationResponse;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import com.probe.stack.code.generator.service.CodeGenerationService;
import com.probe.stack.code.generator.service.DownloadService;
import com.probe.stack.code.generator.service.MultipartRequestService;
import com.probe.stack.code.generator.service.RequestValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CodeGenerationController
 * Tests all endpoints, error handling, and request validation
 */
@ExtendWith(MockitoExtension.class)
class CodeGenerationControllerTest {

    @Mock
    private CodeGenerationService codeGenerationService;

    @Mock
    private MultipartRequestService multipartRequestService;

    @Mock
    private RequestValidationService validationService;

    @Mock
    private DownloadService downloadService;

    @InjectMocks
    private CodeGenerationController controller;

    private CodeGenerationRequest testRequest;
    private CodeGenerationResponse successResponse;

    @BeforeEach
    void setUp() {
        testRequest = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .version("1.0.0")
                .build();

        successResponse = CodeGenerationResponse.builder()
                .generationId("test-gen-id")
                .projectPath("/path/to/project")
                .status(CodeGenerationResponse.GenerationStatus.SUCCESS)
                .timestamp(LocalDateTime.now())
                .messages(new ArrayList<>())
                .build();
    }

    // ==================== Generate Code Tests ====================

    @Test
    void testGenerateCode_Success_ReturnsOkStatus() {
        // Arrange
        when(codeGenerationService.generateProject(any(CodeGenerationRequest.class)))
                .thenReturn(successResponse);

        // Act
        ResponseEntity<CodeGenerationResponse> response = controller.generateCode(testRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(successResponse, response.getBody());
        verify(validationService).validateRequest(testRequest);
        verify(codeGenerationService).generateProject(testRequest);
    }

    @Test
    void testGenerateCode_PartialSuccess_ReturnsOkStatus() {
        // Arrange
        CodeGenerationResponse partialResponse = CodeGenerationResponse.builder()
                .generationId("test-gen-id")
                .status(CodeGenerationResponse.GenerationStatus.PARTIAL_SUCCESS)
                .timestamp(LocalDateTime.now())
                .build();
        when(codeGenerationService.generateProject(any(CodeGenerationRequest.class)))
                .thenReturn(partialResponse);

        // Act
        ResponseEntity<CodeGenerationResponse> response = controller.generateCode(testRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(CodeGenerationResponse.GenerationStatus.PARTIAL_SUCCESS,
                response.getBody().getStatus());
    }

    @Test
    void testGenerateCode_Failed_ReturnsInternalServerError() {
        // Arrange
        CodeGenerationResponse failedResponse = CodeGenerationResponse.builder()
                .generationId("test-gen-id")
                .status(CodeGenerationResponse.GenerationStatus.FAILED)
                .timestamp(LocalDateTime.now())
                .errorMessage("Generation failed")
                .build();
        when(codeGenerationService.generateProject(any(CodeGenerationRequest.class)))
                .thenReturn(failedResponse);

        // Act
        ResponseEntity<CodeGenerationResponse> response = controller.generateCode(testRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(CodeGenerationResponse.GenerationStatus.FAILED,
                response.getBody().getStatus());
    }

    @Test
    void testGenerateCode_ValidationException_ReturnsBadRequest() {
        // Arrange
        doThrow(new CodeGenerationException("Validation failed"))
                .when(validationService).validateRequest(any());

        // Act
        ResponseEntity<CodeGenerationResponse> response = controller.generateCode(testRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(CodeGenerationResponse.GenerationStatus.FAILED,
                response.getBody().getStatus());
        assertEquals("Validation failed", response.getBody().getErrorMessage());
        verify(codeGenerationService, never()).generateProject(any());
    }

    @Test
    void testGenerateCode_ServiceException_ReturnsBadRequest() {
        // Arrange
        when(codeGenerationService.generateProject(any(CodeGenerationRequest.class)))
                .thenThrow(new CodeGenerationException("Service error"));

        // Act
        ResponseEntity<CodeGenerationResponse> response = controller.generateCode(testRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(CodeGenerationResponse.GenerationStatus.FAILED,
                response.getBody().getStatus());
        assertEquals("Service error", response.getBody().getErrorMessage());
    }

    // ==================== Generate Code From File Tests ====================

    @Test
    void testGenerateCodeFromFile_Success_ReturnsOkStatus() {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("api-spec.yaml");
        when(mockFile.getSize()).thenReturn(1024L);

        String requestJson = "{\"groupName\":\"com.example\",\"artifactId\":\"test-service\"}";

        when(multipartRequestService.processMultipartRequest(mockFile, requestJson))
                .thenReturn(testRequest);
        when(codeGenerationService.generateProject(any(CodeGenerationRequest.class)))
                .thenReturn(successResponse);

        // Act
        ResponseEntity<CodeGenerationResponse> response =
                controller.generateCodeFromFile(mockFile, requestJson);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(successResponse, response.getBody());
        verify(multipartRequestService).processMultipartRequest(mockFile, requestJson);
        verify(validationService).validateRequest(testRequest);
        verify(codeGenerationService).generateProject(testRequest);
    }

    @Test
    void testGenerateCodeFromFile_LargeFile_Success() {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("large-spec.yaml");
        when(mockFile.getSize()).thenReturn(5_000_000L); // 5MB

        String requestJson = "{\"groupName\":\"com.example\",\"artifactId\":\"test-service\"}";

        when(multipartRequestService.processMultipartRequest(mockFile, requestJson))
                .thenReturn(testRequest);
        when(codeGenerationService.generateProject(any(CodeGenerationRequest.class)))
                .thenReturn(successResponse);

        // Act
        ResponseEntity<CodeGenerationResponse> response =
                controller.generateCodeFromFile(mockFile, requestJson);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(multipartRequestService).processMultipartRequest(mockFile, requestJson);
    }

    @Test
    void testGenerateCodeFromFile_ProcessingException_ReturnsBadRequest() {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("api-spec.yaml");
        when(mockFile.getSize()).thenReturn(1024L);

        String requestJson = "{\"groupName\":\"com.example\"}";

        when(multipartRequestService.processMultipartRequest(mockFile, requestJson))
                .thenThrow(new CodeGenerationException("Failed to process file"));

        // Act
        ResponseEntity<CodeGenerationResponse> response =
                controller.generateCodeFromFile(mockFile, requestJson);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(CodeGenerationResponse.GenerationStatus.FAILED,
                response.getBody().getStatus());
        assertEquals("Failed to process file", response.getBody().getErrorMessage());
        verify(validationService, never()).validateRequest(any());
        verify(codeGenerationService, never()).generateProject(any());
    }

    @Test
    void testGenerateCodeFromFile_ValidationFails_ReturnsBadRequest() {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("api-spec.yaml");
        when(mockFile.getSize()).thenReturn(1024L);

        String requestJson = "{\"groupName\":\"com.example\"}";

        when(multipartRequestService.processMultipartRequest(mockFile, requestJson))
                .thenReturn(testRequest);
        doThrow(new CodeGenerationException("Invalid request"))
                .when(validationService).validateRequest(testRequest);

        // Act
        ResponseEntity<CodeGenerationResponse> response =
                controller.generateCodeFromFile(mockFile, requestJson);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid request", response.getBody().getErrorMessage());
        verify(codeGenerationService, never()).generateProject(any());
    }

    @Test
    void testGenerateCodeFromFile_ServiceFails_ReturnsBadRequest() {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("api-spec.yaml");
        when(mockFile.getSize()).thenReturn(1024L);

        String requestJson = "{\"groupName\":\"com.example\"}";

        when(multipartRequestService.processMultipartRequest(mockFile, requestJson))
                .thenReturn(testRequest);
        when(codeGenerationService.generateProject(testRequest))
                .thenThrow(new CodeGenerationException("Generation failed"));

        // Act
        ResponseEntity<CodeGenerationResponse> response =
                controller.generateCodeFromFile(mockFile, requestJson);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Generation failed", response.getBody().getErrorMessage());
    }

    // ==================== Download Project Tests ====================

    @Test
    void testDownloadProject_Success_ReturnsResourceWithHeaders() {
        // Arrange
        String generationId = "test-gen-id";
        Resource mockResource = mock(Resource.class);

        when(downloadService.getGeneratedProjectArchive(generationId))
                .thenReturn(Optional.of(mockResource));
        when(downloadService.getFilename(mockResource))
                .thenReturn("test-service.zip");
        when(downloadService.getContentLength(mockResource))
                .thenReturn(1024L);

        // Act
        ResponseEntity<Resource> response = controller.downloadProject(generationId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResource, response.getBody());
        assertNotNull(response.getHeaders());
        assertTrue(response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION)
                .get(0).contains("attachment"));
        assertTrue(response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION)
                .get(0).contains("test-service.zip"));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE,
                response.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
        assertEquals(1024L, response.getHeaders().getContentLength());
    }

    @Test
    void testDownloadProject_NotFound_ReturnsNotFound() {
        // Arrange
        String generationId = "non-existent-id";
        when(downloadService.getGeneratedProjectArchive(generationId))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<Resource> response = controller.downloadProject(generationId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(downloadService).getGeneratedProjectArchive(generationId);
    }

    @Test
    void testDownloadProject_ServiceException_ReturnsInternalServerError() {
        // Arrange
        String generationId = "test-gen-id";
        when(downloadService.getGeneratedProjectArchive(generationId))
                .thenThrow(new CodeGenerationException("Download failed"));

        // Act
        ResponseEntity<Resource> response = controller.downloadProject(generationId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void testDownloadProject_NullGenerationId_HandledGracefully() {
        // Arrange
        when(downloadService.getGeneratedProjectArchive(null))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<Resource> response = controller.downloadProject(null);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testDownloadProject_EmptyGenerationId_HandledGracefully() {
        // Arrange
        when(downloadService.getGeneratedProjectArchive(""))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<Resource> response = controller.downloadProject("");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ==================== Edge Cases and Integration Tests ====================

    @Test
    void testGenerateCode_WithAllRequestFields_Success() {
        // Arrange
        CodeGenerationRequest fullRequest = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .version("2.0.0")
                .returnAsArchive(true)
                .githubToken("github-token")
                .organization("test-org")
                .branchName("develop")
                .repositoryName("test-repo")
                .build();

        when(codeGenerationService.generateProject(fullRequest))
                .thenReturn(successResponse);

        // Act
        ResponseEntity<CodeGenerationResponse> response = controller.generateCode(fullRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(codeGenerationService).generateProject(fullRequest);
    }

    @Test
    void testGenerateCode_WithMinimalRequestFields_Success() {
        // Arrange
        CodeGenerationRequest minimalRequest = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/api-spec.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();

        when(codeGenerationService.generateProject(minimalRequest))
                .thenReturn(successResponse);

        // Act
        ResponseEntity<CodeGenerationResponse> response = controller.generateCode(minimalRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGenerateCodeFromFile_WithJsonFile_Success() {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("api-spec.json");
        when(mockFile.getSize()).thenReturn(2048L);

        String requestJson = "{\"groupName\":\"com.example\",\"artifactId\":\"test-service\"}";

        when(multipartRequestService.processMultipartRequest(mockFile, requestJson))
                .thenReturn(testRequest);
        when(codeGenerationService.generateProject(any()))
                .thenReturn(successResponse);

        // Act
        ResponseEntity<CodeGenerationResponse> response =
                controller.generateCodeFromFile(mockFile, requestJson);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGenerateCode_ResponseContainsTimestamp_Success() {
        // Act
        when(codeGenerationService.generateProject(testRequest))
                .thenReturn(successResponse);

        ResponseEntity<CodeGenerationResponse> response = controller.generateCode(testRequest);

        // Assert
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testGenerateCode_ResponseContainsGenerationId_Success() {
        // Act
        when(codeGenerationService.generateProject(testRequest))
                .thenReturn(successResponse);

        ResponseEntity<CodeGenerationResponse> response = controller.generateCode(testRequest);

        // Assert
        assertNotNull(response.getBody().getGenerationId());
        assertEquals("test-gen-id", response.getBody().getGenerationId());
    }
}
