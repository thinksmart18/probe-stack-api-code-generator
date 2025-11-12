package com.probe.stack.code.generator.component;

import com.probe.stack.code.generator.parser.ControllerMetadataExtractor.ControllerMetadata;
import com.probe.stack.code.generator.parser.ControllerMetadataExtractor.MethodMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ServiceClassGenerator
 */
@ExtendWith(MockitoExtension.class)
class ServiceClassGeneratorTest {

    @InjectMocks
    private ServiceClassGenerator serviceClassGenerator;

    @TempDir
    Path tempDir;

    private ControllerMetadata testMetadata;

    @BeforeEach
    void setUp() {
        testMetadata = new ControllerMetadata();
        testMetadata.setClassName("UserApi");
        testMetadata.setPackageName("com.example.api");
        testMetadata.setEntityClass("User");
        
        List<MethodMetadata> methods = new ArrayList<>();
        MethodMetadata method = new MethodMetadata();
        method.setMethodName("createUser");
        method.setReturnType("ResponseEntity<User>");
        method.setParameters(new ArrayList<>());
        methods.add(method);
        
        testMetadata.setMethods(methods);
    }

    @Test
    void testGenerateServiceClass_ValidMetadata_CreatesServiceFile() throws Exception {
        // Act
        serviceClassGenerator.generateServiceClass(testMetadata, tempDir.toFile());

        // Assert
        File serviceFile = new File(tempDir.toFile(), "com/example/service/UserService.java");
        assertTrue(serviceFile.exists() || tempDir.toFile().listFiles().length > 0);
    }

    @Test
    void testGenerateServiceClass_NullEntityClass_ThrowsException() {
        // Arrange
        testMetadata.setEntityClass(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> serviceClassGenerator.generateServiceClass(testMetadata, tempDir.toFile()));
    }

    @Test
    void testGenerateServiceClass_EmptyMethods_ThrowsException() {
        // Arrange
        testMetadata.setMethods(new ArrayList<>());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> serviceClassGenerator.generateServiceClass(testMetadata, tempDir.toFile()));
    }

    @Test
    void testGenerateServiceClass_ComplexEntityName_Success() throws Exception {
        // Arrange
        testMetadata.setEntityClass("CompanyRegistration");
        testMetadata.setClassName("CompanyRegistrationApi");

        // Act & Assert
        assertDoesNotThrow(() -> serviceClassGenerator.generateServiceClass(testMetadata, tempDir.toFile()));
    }
}
