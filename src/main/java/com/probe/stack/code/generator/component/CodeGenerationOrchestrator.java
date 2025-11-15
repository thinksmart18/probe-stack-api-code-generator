package com.probe.stack.code.generator.component;

import com.probe.stack.code.generator.parser.ControllerMetadataExtractor;
import com.probe.stack.code.generator.parser.ControllerMetadataExtractor.ControllerMetadata;
import com.probe.stack.code.generator.util.ControllerPathScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the complete code generation process:
 * 1. Parses API interfaces
 * 2. Generates Service classes
 * 3. Generates Repository interfaces
 * 4. Enhancing existing controllers
 *
 * @author ProbeStack
 */
@Component
public class CodeGenerationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CodeGenerationOrchestrator.class);

    private final ControllerMetadataExtractor metadataExtractor;
    private final ServiceClassGenerator serviceGenerator;
    private final RepositoryInterfaceGenerator repositoryGenerator;
    private final ExistingControllerEnhancer controllerEnhancer;
    private final ControllerPathScanner controllerLocator;

    @Autowired
    public CodeGenerationOrchestrator(
            ControllerMetadataExtractor metadataExtractor,
            ServiceClassGenerator serviceGenerator,
            RepositoryInterfaceGenerator repositoryGenerator,
            ExistingControllerEnhancer controllerEnhancer,
            ControllerPathScanner controllerLocator
    ) {
        this.metadataExtractor = metadataExtractor;
        this.serviceGenerator = serviceGenerator;
        this.repositoryGenerator = repositoryGenerator;
        this.controllerEnhancer = controllerEnhancer;
        this.controllerLocator = controllerLocator;
    }

    /**
     * Generates all artifacts, enhancing existing controllers instead of creating duplicates.
     *
     * @param apiInterfaceFiles list of API interface files
     * @param projectDirectory the project root directory
     * @param basePackage the base package
     * @param outputDir output directory for generated files
     */
    public void generateAllArtifacts(
            List<File> apiInterfaceFiles,
            String projectDirectory,
            String basePackage,
            File outputDir
    ) {
        log.info("=".repeat(80));
        log.info("Starting Smart Code Generation for {} API interfaces", apiInterfaceFiles.size());
        log.info("=".repeat(80));

        int successCount = 0;
        int failureCount = 0;

        for (File apiInterfaceFile : apiInterfaceFiles) {
            try {
                log.info("\n" + "-".repeat(80));
                log.info("Processing: {}", apiInterfaceFile.getName());
                log.info("-".repeat(80));

                // Extract metadata
                ControllerMetadata metadata = metadataExtractor.extractMetadata(apiInterfaceFile);

                log.info("API Interface: {}", metadata.getClassName());
                log.info("Package: {}", metadata.getPackageName());
                log.info("Entity: {}", metadata.getEntityClass());
                log.info("Methods: {}", metadata.getMethods().size());

                // Generate Service
                try {
                    serviceGenerator.generateServiceClass(metadata, outputDir);
                    log.info("✓ Service generated: {}", generateServiceName(metadata.getClassName()));
                } catch (Exception e) {
                    log.error("✗ Service generation failed: {}", e.getMessage());
                    throw e;
                }

                // Generate Repository
                if (metadata.getEntityClass() != null && !metadata.getEntityClass().equals("null")) {
                    try {
                        repositoryGenerator.generateRepositoryInterface(metadata, outputDir);
                        log.info("✓ Repository generated: {}Repository", metadata.getEntityClass());
                    } catch (Exception e) {
                        log.error("✗ Repository generation failed: {}", e.getMessage());
                        throw e;
                    }
                } else {
                    log.warn("⚠ Repository skipped - no entity class found");
                }

                // Check for existing controller
                Optional<File> existingController = controllerLocator.findExistingController(
                        projectDirectory,
                        basePackage,
                        "api",
                        metadata.getClassName()
                );

                if (existingController.isPresent()) {
                    // Enhance existing controller
                    try {
                        controllerEnhancer.enhanceExistingController(
                                metadata,
                                existingController.get(),
                                outputDir
                        );
                        log.info("✓ Controller enhanced: {}", existingController.get().getName());
                    } catch (Exception e) {
                        log.error("✗ Controller enhancement failed: {}", e.getMessage());
                        throw e;
                    }
                } else {
                    log.warn("⚠ No existing controller found for: {}", metadata.getClassName());
                    log.warn("  Expected location: {}/{}/{}.java",
                            outputDir.getPath(),
                            metadata.getPackageName().replace('.', '/'),
                            metadata.getClassName() + "Controller");
                }

                successCount++;
                log.info("✓ All artifacts processed successfully for: {}", apiInterfaceFile.getName());

            } catch (Exception e) {
                failureCount++;
                log.error("✗ Failed to process {}: {}", apiInterfaceFile.getName(), e.getMessage(), e);
            }
        }

        log.info("\n" + "=".repeat(80));
        log.info("Code Generation Summary");
        log.info("=".repeat(80));
        log.info("Total Processed: {}", apiInterfaceFiles.size());
        log.info("Successful: {}", successCount);
        log.info("Failed: {}", failureCount);
        log.info("=".repeat(80));
    }

    /**
     * Generates service class name from API interface/controller name.
     * MUST match logic in ServiceClassGenerator and ExistingControllerEnhancer.
     */
    private String generateServiceName(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }

        // Remove "ApiController" suffix first (most specific)
        if (className.endsWith("ApiController")) {
            String baseName = className.substring(0, className.length() - "ApiController".length());
            return baseName + "Service";
        }

        // Remove "Api" suffix
        if (className.endsWith("Api")) {
            String baseName = className.substring(0, className.length() - "Api".length());
            return baseName + "Service";
        }

        // Remove "Controller" suffix
        if (className.endsWith("Controller")) {
            String baseName = className.substring(0, className.length() - "Controller".length());
            return baseName + "Service";
        }

        // No known suffix, just append Service
        return className + "Service";
    }
}