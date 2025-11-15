package com.probe.stack.code.generator.component;

import com.probe.stack.code.generator.parser.ControllerMetadataExtractor.ControllerMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for RepositoryInterfaceGenerator
 */
@ExtendWith(MockitoExtension.class)
class RepositoryInterfaceGeneratorTest {

    @InjectMocks
    private RepositoryInterfaceGenerator repositoryInterfaceGenerator;

    @TempDir
    Path tempDir;

    private ControllerMetadata testMetadata;

    @BeforeEach
    void setUp() {
        testMetadata = new ControllerMetadata();
        testMetadata.setClassName("UserApi");
        testMetadata.setPackageName("com.example.api");
        testMetadata.setEntityClass("User");
    }

    @Test
    void testGenerateRepositoryInterface_ValidMetadata_CreatesRepositoryFile() throws Exception {
        // Act
        repositoryInterfaceGenerator.generateRepositoryInterface(testMetadata, tempDir.toFile());

        // Assert - File should be created
        assertTrue(tempDir.toFile().exists());
    }

    @Test
    void testGenerateRepositoryInterface_NullEntityClass_ThrowsException() {
        // Arrange
        testMetadata.setEntityClass(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> repositoryInterfaceGenerator.generateRepositoryInterface(testMetadata, tempDir.toFile()));
    }

    @Test
    void testGenerateRepositoryInterface_EmptyEntityClass_ThrowsException() {
        // Arrange
        testMetadata.setEntityClass("");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> repositoryInterfaceGenerator.generateRepositoryInterface(testMetadata, tempDir.toFile()));
    }

    @Test
    void testGenerateRepositoryInterface_NullStringEntityClass_ThrowsException() {
        // Arrange
        testMetadata.setEntityClass("null");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> repositoryInterfaceGenerator.generateRepositoryInterface(testMetadata, tempDir.toFile()));
    }
}
