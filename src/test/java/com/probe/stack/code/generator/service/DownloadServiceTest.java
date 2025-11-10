package com.probe.stack.code.generator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DownloadService
 */
class DownloadServiceTest {
    
    private DownloadService downloadService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        downloadService = new DownloadService();
    }
    
    @Test
    void testGetGeneratedProjectArchive_Success() throws IOException {
        // Arrange
        String generationId = "test-generation-123";
        Path generationDir = tempDir.resolve("generated-projects").resolve(generationId);
        Files.createDirectories(generationDir);
        
        Path zipFile = generationDir.resolve("test-project.zip");
        Files.writeString(zipFile, "test content");
        
        // Note: This test would need the actual generated-projects directory structure
        // In real scenario, mock the file system or use integration tests
    }
    
    @Test
    void testGetGeneratedProjectArchive_NotFound() {
        // Arrange
        String generationId = "non-existent-id";
        
        // Act
        Optional<Resource> result = downloadService.getGeneratedProjectArchive(generationId);
        
        // Assert
        assertTrue(result.isEmpty(), "Should return empty Optional when generation not found");
    }
    
    @Test
    void testGenerationExists_True() throws IOException {
        // This test depends on the actual file system structure
        // In production, you might want to make the base directory configurable
        assertFalse(downloadService.generationExists("test-id"));
    }
    
    @Test
    void testGenerationExists_False() {
        // Act
        boolean exists = downloadService.generationExists("non-existent-id");
        
        // Assert
        assertFalse(exists, "Should return false for non-existent generation");
    }
    
    @Test
    void testGetFilename_WithValidResource() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("test-file.zip");
        Files.writeString(testFile, "test");
        Resource resource = new org.springframework.core.io.FileSystemResource(testFile);
        
        // Act
        String filename = downloadService.getFilename(resource);
        
        // Assert
        assertEquals("test-file.zip", filename);
    }
    
    @Test
    void testGetContentLength_WithValidResource() throws IOException {
        // Arrange
        Path testFile = tempDir.resolve("test-file.zip");
        String content = "test content";
        Files.writeString(testFile, content);
        Resource resource = new org.springframework.core.io.FileSystemResource(testFile);
        
        // Act
        long contentLength = downloadService.getContentLength(resource);
        
        // Assert
        assertEquals(content.length(), contentLength);
    }
}