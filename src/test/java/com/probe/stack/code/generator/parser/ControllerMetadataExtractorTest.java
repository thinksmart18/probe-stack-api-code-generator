package com.probe.stack.code.generator.parser;

import com.probe.stack.code.generator.parser.ControllerMetadataExtractor.ControllerMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ControllerMetadataExtractor
 */
@ExtendWith(MockitoExtension.class)
class ControllerMetadataExtractorTest {

    @InjectMocks
    private ControllerMetadataExtractor controllerMetadataExtractor;

    @TempDir
    Path tempDir;

    @Test
    void testExtractMetadata_ValidControllerFile_ExtractsMetadata() throws Exception {
        // Arrange
        String javaContent = "package com.example.api;\n" +
                "public interface UserApi {\n" +
                "    User createUser(User user);\n" +
                "}";
        File controllerFile = tempDir.resolve("UserApi.java").toFile();
        Files.writeString(controllerFile.toPath(), javaContent);

        // Act
        ControllerMetadata metadata = controllerMetadataExtractor.extractMetadata(controllerFile);

        // Assert
        assertNotNull(metadata);
        assertEquals("UserApi", metadata.getClassName());
        assertEquals("com.example.api", metadata.getPackageName());
    }

    @Test
    void testExtractMetadata_NonExistentFile_ThrowsException() {
        // Arrange
        File nonExistent = new File(tempDir.toFile(), "NonExistent.java");

        // Act & Assert
        assertThrows(Exception.class,
                () -> controllerMetadataExtractor.extractMetadata(nonExistent));
    }

    @Test
    void testExtractMetadata_EmptyFile_HandlesGracefully() throws Exception {
        // Arrange
        File emptyFile = tempDir.resolve("Empty.java").toFile();
        Files.writeString(emptyFile.toPath(), "");

        // Act & Assert
        assertThrows(Exception.class,
                () -> controllerMetadataExtractor.extractMetadata(emptyFile));
    }

    @Test
    void testExtractMetadata_ComplexController_ExtractsAllMetadata() throws Exception {
        // Arrange
        String complexContent = "package com.example.api;\n" +
                "import org.springframework.http.ResponseEntity;\n" +
                "public interface ProductApi {\n" +
                "    ResponseEntity<Product> getProduct(String id);\n" +
                "    ResponseEntity<List<Product>> getAllProducts();\n" +
                "}";
        File controllerFile = tempDir.resolve("ProductApi.java").toFile();
        Files.writeString(controllerFile.toPath(), complexContent);

        // Act
        ControllerMetadata metadata = controllerMetadataExtractor.extractMetadata(controllerFile);

        // Assert
        assertNotNull(metadata);
        assertEquals("ProductApi", metadata.getClassName());
    }
}
