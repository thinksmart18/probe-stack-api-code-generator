package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.config.CodeGeneratorConfig;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for PomCustomizationService
 */
@ExtendWith(MockitoExtension.class)
class PomCustomizationServiceTest {

    @Mock
    private CodeGeneratorConfig config;

    @Mock
    private CodeGeneratorConfig.TemplateConfig templateConfig;

    @InjectMocks
    private PomCustomizationService pomCustomizationService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(config.getTemplates()).thenReturn(templateConfig);
        when(templateConfig.getPomTemplate()).thenReturn("maven_config");
    }

    @Test
    void testMergePomCustomizations_ValidPom_Success() throws Exception {
        // Arrange
        Path pomPath = tempDir.resolve("pom.xml");
        String pomContent = "<?xml version=\"1.0\"?>\n<project><modelVersion>4.0.0</modelVersion></project>";
        Files.writeString(pomPath, pomContent);
        
        Path templateDir = tempDir.resolve("templates");
        Files.createDirectories(templateDir);

        // Act & Assert
        assertDoesNotThrow(() -> pomCustomizationService.mergePomCustomizations(pomPath, templateDir));
    }

    @Test
    void testMergePomCustomizations_NonExistentPom_ThrowsException() {
        // Arrange
        Path nonExistentPom = tempDir.resolve("nonexistent.xml");
        Path templateDir = tempDir.resolve("templates");

        // Act & Assert
        assertThrows(CodeGenerationException.class,
                () -> pomCustomizationService.mergePomCustomizations(nonExistentPom, templateDir));
    }

    @Test
    void testMergePomCustomizations_NoTemplateDir_UsesDefaults() throws Exception {
        // Arrange
        Path pomPath = tempDir.resolve("pom.xml");
        String pomContent = "<?xml version=\"1.0\"?>\n<project><modelVersion>4.0.0</modelVersion></project>";
        Files.writeString(pomPath, pomContent);
        
        Path nonExistentTemplateDir = tempDir.resolve("nonexistent");

        // Act & Assert
        assertDoesNotThrow(() -> pomCustomizationService.mergePomCustomizations(pomPath, nonExistentTemplateDir));
    }
}
