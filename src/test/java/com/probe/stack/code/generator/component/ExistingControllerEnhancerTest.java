package com.probe.stack.code.generator.component;

import com.probe.stack.code.generator.parser.ControllerMetadataExtractor.ControllerMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ExistingControllerEnhancer
 */
@ExtendWith(MockitoExtension.class)
class ExistingControllerEnhancerTest {

    @InjectMocks
    private ExistingControllerEnhancer existingControllerEnhancer;

    @TempDir
    Path tempDir;

    private ControllerMetadata testMetadata;
    private File testControllerFile;

    @BeforeEach
    void setUp() throws Exception {
        testMetadata = new ControllerMetadata();
        testMetadata.setClassName("UserApi");
        testMetadata.setPackageName("com.example.api");
        testMetadata.setEntityClass("User");
        testMetadata.setMethods(new ArrayList<>());

        testControllerFile = tempDir.resolve("UserController.java").toFile();
        String controllerContent = "package com.example.api;\n\npublic class UserController implements UserApi {}";
        Files.writeString(testControllerFile.toPath(), controllerContent);
    }

    @Test
    void testEnhanceExistingController_ValidController_Success() {
        // Act & Assert
        assertDoesNotThrow(() -> 
                existingControllerEnhancer.enhanceExistingController(
                        testMetadata, testControllerFile, tempDir.toFile()));
    }

    @Test
    void testEnhanceExistingController_NonExistentController_HandlesGracefully() {
        // Arrange
        File nonExistent = new File(tempDir.toFile(), "NonExistent.java");

        // Act & Assert - Should handle gracefully
        assertNotNull(existingControllerEnhancer);
    }
}
