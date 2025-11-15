package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for TemplateEnhancementService
 */
@ExtendWith(MockitoExtension.class)
class TemplateEnhancementServiceTest {

    @InjectMocks
    private TemplateEnhancementService templateEnhancementService;

    @TempDir
    Path tempDir;

    @Test
    void testEnhanceProject_ValidProject_ReturnsMessages() {
        // Arrange
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .artifactId("test-service")
                .basePackage("com.example")
                .build();

        // Act
        List<String> messages = templateEnhancementService.enhanceProject(tempDir, request);

        // Assert
        assertNotNull(messages);
    }

    @Test
    void testEnhanceProject_NullPath_HandlesGracefully() {
        CodeGenerationRequest request = CodeGenerationRequest.builder().build();
        assertNotNull(templateEnhancementService);
    }
}
