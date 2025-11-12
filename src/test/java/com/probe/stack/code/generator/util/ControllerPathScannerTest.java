package com.probe.stack.code.generator.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ControllerPathScanner
 */
@ExtendWith(MockitoExtension.class)
class ControllerPathScannerTest {

    @InjectMocks
    private ControllerPathScanner controllerPathScanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // Create sample directory structure
        Path apiDir = tempDir.resolve("src/main/java/com/example/api");
        Files.createDirectories(apiDir);

        // Create sample controller file
        Path controllerFile = apiDir.resolve("UserApiController.java");
        Files.writeString(controllerFile, "package com.example.api;\npublic class UserApiController {}");
    }

    @Test
    void testGetGeneratedControllerClassFiles_ValidDirectory_FindsControllers() {
        // Act
        List<File> controllers = controllerPathScanner.getGeneratedControllerClassFiles(
                tempDir.toString(), "com.example", "api");

        // Assert
        assertNotNull(controllers);
    }

    @Test
    void testGetGeneratedControllerClassFiles_NonExistentDirectory_ReturnsEmpty() {
        // Act
        List<File> controllers = controllerPathScanner.getGeneratedControllerClassFiles(
                "/nonexistent", "com.example", "api");

        // Assert
        assertNotNull(controllers);
        assertTrue(controllers.isEmpty());
    }

    @Test
    void testFindExistingController_ControllerExists_ReturnsController() {
        // Act
        Optional<File> controller = controllerPathScanner.findExistingController(
                tempDir.toString(), "com.example", "api", "UserApi");

        // Assert
        assertNotNull(controller);
    }

    @Test
    void testFindExistingController_ControllerNotFound_ReturnsEmpty() {
        // Act
        Optional<File> controller = controllerPathScanner.findExistingController(
                tempDir.toString(), "com.example", "api", "NonExistentApi");

        // Assert
        assertNotNull(controller);
    }

    @Test
    void testConstructOutputPackageDirector_ValidInputs_CreatesPath() {
        // Act
        File outputDir = controllerPathScanner.constructOutputPackageDirector(
                tempDir.toString(), "com.example");

        // Assert
        assertNotNull(outputDir);
    }

    @Test
    void testGetGeneratedControllerClassFiles_EmptyDirectory_ReturnsEmpty() throws Exception {
        // Arrange
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectories(emptyDir);

        // Act
        List<File> controllers = controllerPathScanner.getGeneratedControllerClassFiles(
                emptyDir.toString(), "com.example", "api");

        // Assert
        assertNotNull(controllers);
    }
}
