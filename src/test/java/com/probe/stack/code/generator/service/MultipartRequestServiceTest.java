package com.probe.stack.code.generator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for MultipartRequestService
 * Tests file upload processing, validation, and content type detection
 */
@ExtendWith(MockitoExtension.class)
class MultipartRequestServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MultipartRequestService multipartRequestService;

    @Mock
    private MultipartFile mockFile;

    private String validRequestJson;
    private CodeGenerationRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequestJson = "{\"groupName\":\"com.example\",\"artifactId\":\"test-service\"," +
                "\"basePackage\":\"com.example.testservice\"}";

        validRequest = CodeGenerationRequest.builder()
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .build();
    }

    // ==================== Process Multipart Request Tests ====================

    @Test
    void testProcessMultipartRequest_WithYamlFile_Success() throws Exception {
        // Arrange
        String yamlContent = "openapi: 3.0.0\ninfo:\n  title: Test API";
        when(mockFile.getOriginalFilename()).thenReturn("api-spec.yaml");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getBytes()).thenReturn(yamlContent.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act
        CodeGenerationRequest result = multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson);

        // Assert
        assertNotNull(result);
        assertEquals(yamlContent, result.getSpecContent());
        assertEquals("yaml", result.getSpecContentType());
        verify(objectMapper).readValue(validRequestJson, CodeGenerationRequest.class);
    }

    @Test
    void testProcessMultipartRequest_WithJsonFile_Success() throws Exception {
        // Arrange
        String jsonContent = "{\"openapi\": \"3.0.0\"}";
        when(mockFile.getOriginalFilename()).thenReturn("api-spec.json");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getBytes()).thenReturn(jsonContent.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act
        CodeGenerationRequest result = multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson);

        // Assert
        assertNotNull(result);
        assertEquals(jsonContent, result.getSpecContent());
        assertEquals("json", result.getSpecContentType());
    }

    @Test
    void testProcessMultipartRequest_WithYmlExtension_Success() throws Exception {
        // Arrange
        String yamlContent = "openapi: 3.0.0";
        when(mockFile.getOriginalFilename()).thenReturn("api-spec.yml");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getBytes()).thenReturn(yamlContent.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act
        CodeGenerationRequest result = multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson);

        // Assert
        assertEquals("yaml", result.getSpecContentType());
    }

    @Test
    void testProcessMultipartRequest_EmptyFile_ThrowsException() {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(true);

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class,
                () -> multipartRequestService.processMultipartRequest(mockFile, validRequestJson));
        assertEquals("Uploaded file is empty", exception.getMessage());
        verify(objectMapper, never()).readValue(anyString(), any(Class.class));
    }

    @Test
    void testProcessMultipartRequest_FileTooLarge_ThrowsException() {
        // Arrange
        long maxSize = 10 * 1024 * 1024; // 10MB
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(maxSize + 1); // Exceeds max size

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class,
                () -> multipartRequestService.processMultipartRequest(mockFile, validRequestJson));
        assertTrue(exception.getMessage().contains("File size exceeds maximum allowed size"));
        assertTrue(exception.getMessage().contains("10MB"));
    }

    @Test
    void testProcessMultipartRequest_ExactlyMaxSize_Success() throws Exception {
        // Arrange
        long maxSize = 10 * 1024 * 1024; // 10MB
        String content = "test content";
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(maxSize);
        when(mockFile.getOriginalFilename()).thenReturn("api-spec.yaml");
        when(mockFile.getBytes()).thenReturn(content.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act & Assert
        assertDoesNotThrow(() -> multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson));
    }

    @Test
    void testProcessMultipartRequest_InvalidJson_ThrowsException() throws Exception {
        // Arrange
        String content = "test content";
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("api-spec.yaml");
        when(mockFile.getBytes()).thenReturn(content.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenThrow(new IOException("Invalid JSON"));

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class,
                () -> multipartRequestService.processMultipartRequest(mockFile, validRequestJson));
        assertTrue(exception.getMessage().contains("Invalid request JSON"));
    }

    @Test
    void testProcessMultipartRequest_IOExceptionReadingFile_ThrowsException() throws Exception {
        // Arrange
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("api-spec.yaml");
        when(mockFile.getBytes()).thenThrow(new IOException("Cannot read file"));
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class,
                () -> multipartRequestService.processMultipartRequest(mockFile, validRequestJson));
        assertTrue(exception.getMessage().contains("Failed to read uploaded file"));
    }

    // ==================== Content Type Detection Tests ====================

    @Test
    void testProcessMultipartRequest_DetectsJsonFromContent_Success() throws Exception {
        // Arrange
        String jsonContent = "{\"openapi\": \"3.0.0\"}";
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("spec"); // No extension
        when(mockFile.getBytes()).thenReturn(jsonContent.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act
        CodeGenerationRequest result = multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson);

        // Assert
        assertEquals("json", result.getSpecContentType());
    }

    @Test
    void testProcessMultipartRequest_DetectsYamlFromContent_Success() throws Exception {
        // Arrange
        String yamlContent = "openapi: 3.0.0";
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("spec"); // No extension
        when(mockFile.getBytes()).thenReturn(yamlContent.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act
        CodeGenerationRequest result = multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson);

        // Assert
        assertEquals("yaml", result.getSpecContentType());
    }

    @Test
    void testProcessMultipartRequest_JsonWithWhitespace_DetectsCorrectly() throws Exception {
        // Arrange
        String jsonContent = "   {\"openapi\": \"3.0.0\"}";
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("spec.txt");
        when(mockFile.getBytes()).thenReturn(jsonContent.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act
        CodeGenerationRequest result = multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson);

        // Assert
        assertEquals("json", result.getSpecContentType());
    }

    @Test
    void testProcessMultipartRequest_JsonArray_DetectsCorrectly() throws Exception {
        // Arrange
        String jsonContent = "[{\"test\": \"value\"}]";
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("spec.txt");
        when(mockFile.getBytes()).thenReturn(jsonContent.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act
        CodeGenerationRequest result = multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson);

        // Assert
        assertEquals("json", result.getSpecContentType());
    }

    @Test
    void testProcessMultipartRequest_CaseInsensitiveExtension_Success() throws Exception {
        // Arrange
        String content = "openapi: 3.0.0";
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("API-SPEC.YAML");
        when(mockFile.getBytes()).thenReturn(content.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act
        CodeGenerationRequest result = multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson);

        // Assert
        assertEquals("yaml", result.getSpecContentType());
    }

    @Test
    void testProcessMultipartRequest_NullFilename_DetectsFromContent() throws Exception {
        // Arrange
        String jsonContent = "{\"openapi\": \"3.0.0\"}";
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn(null);
        when(mockFile.getBytes()).thenReturn(jsonContent.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act
        CodeGenerationRequest result = multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson);

        // Assert
        assertEquals("json", result.getSpecContentType());
    }

    // ==================== Edge Cases Tests ====================

    @Test
    void testProcessMultipartRequest_NonStandardExtension_LogsWarning() throws Exception {
        // Arrange
        String content = "openapi: 3.0.0";
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("api-spec.txt");
        when(mockFile.getBytes()).thenReturn(content.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act
        CodeGenerationRequest result = multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson);

        // Assert - Should still succeed and detect from content
        assertNotNull(result);
        assertEquals("yaml", result.getSpecContentType());
    }

    @Test
    void testProcessMultipartRequest_LargeValidFile_Success() throws Exception {
        // Arrange
        byte[] largeContent = new byte[5 * 1024 * 1024]; // 5MB
        String jsonStart = "{\"openapi\": \"3.0.0\"}";
        System.arraycopy(jsonStart.getBytes(), 0, largeContent, 0, jsonStart.getBytes().length);

        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn((long) largeContent.length);
        when(mockFile.getOriginalFilename()).thenReturn("large-spec.json");
        when(mockFile.getBytes()).thenReturn(largeContent);
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act
        CodeGenerationRequest result = multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson);

        // Assert
        assertNotNull(result);
        assertEquals("json", result.getSpecContentType());
    }

    @Test
    void testProcessMultipartRequest_SpecialCharactersInFilename_Success() throws Exception {
        // Arrange
        String content = "openapi: 3.0.0";
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("api-spec@v1.0.yaml");
        when(mockFile.getBytes()).thenReturn(content.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act
        CodeGenerationRequest result = multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson);

        // Assert
        assertNotNull(result);
        assertEquals("yaml", result.getSpecContentType());
    }

    @Test
    void testProcessMultipartRequest_EmptyFilename_DetectsFromContent() throws Exception {
        // Arrange
        String content = "{\"test\": \"value\"}";
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("");
        when(mockFile.getBytes()).thenReturn(content.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act
        CodeGenerationRequest result = multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson);

        // Assert
        assertEquals("json", result.getSpecContentType());
    }

    @Test
    void testProcessMultipartRequest_UTF8Content_Success() throws Exception {
        // Arrange
        String utf8Content = "openapi: 3.0.0\ninfo:\n  title: Tëst ÄPÏ 中文";
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn((long) utf8Content.getBytes().length);
        when(mockFile.getOriginalFilename()).thenReturn("api-spec.yaml");
        when(mockFile.getBytes()).thenReturn(utf8Content.getBytes());
        when(objectMapper.readValue(validRequestJson, CodeGenerationRequest.class))
                .thenReturn(validRequest);

        // Act
        CodeGenerationRequest result = multipartRequestService.processMultipartRequest(
                mockFile, validRequestJson);

        // Assert
        assertNotNull(result);
        assertEquals(utf8Content, result.getSpecContent());
    }
}
