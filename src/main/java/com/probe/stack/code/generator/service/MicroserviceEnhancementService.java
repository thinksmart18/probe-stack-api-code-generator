package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.service.enhancement.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Main orchestrator service for post-generation microservice enhancements.
 * This service performs comprehensive scans and enhancements including:
 * - Compile error detection and fixing
 * - AI tooling integration (Embabel, ACPJava)
 * - Swagger analysis and implementation verification
 * - Business logic implementation
 * - Code quality improvements (logging, exception handling, documentation)
 *
 * @author ProbeStack
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MicroserviceEnhancementService {

    private final SwaggerAnalysisService swaggerAnalysisService;
    private final CompileErrorDetectionService compileErrorDetectionService;
    private final AIToolingIntegrationService aiToolingIntegrationService;
    private final MissingClassGeneratorService missingClassGeneratorService;
    private final BusinessLogicEnhancerService businessLogicEnhancerService;
    private final CodeQualityEnhancerService codeQualityEnhancerService;

    /**
     * Performs full scan and enhancement of the generated microservice.
     *
     * @param projectDir Path to the generated project directory
     * @param request Original code generation request
     * @return List of enhancement messages and results
     */
    public List<String> enhanceMicroservice(Path projectDir, CodeGenerationRequest request) {
        log.info("=".repeat(80));
        log.info("Starting Microservice Enhancement for: {}", projectDir.getFileName());
        log.info("=".repeat(80));

        List<String> enhancementMessages = new ArrayList<>();

        try {
            // Step 1: Integrate AI Tooling (Embabel, ACPJava)
            log.info("\n[Step 1/6] Integrating AI Tooling...");
            List<String> aiToolingMessages = aiToolingIntegrationService.integrateAITooling(projectDir, request);
            enhancementMessages.addAll(aiToolingMessages);
            log.info("✓ AI Tooling integration completed");

            // Step 2: Analyze Swagger and identify missing implementations
            log.info("\n[Step 2/6] Analyzing Swagger specification...");
            List<String> swaggerMessages = swaggerAnalysisService.analyzeAndValidate(projectDir, request);
            enhancementMessages.addAll(swaggerMessages);
            log.info("✓ Swagger analysis completed");

            // Step 3: Generate missing classes (controllers, services, repositories, models)
            log.info("\n[Step 3/6] Generating missing classes...");
            List<String> missingClassMessages = missingClassGeneratorService.generateMissingClasses(projectDir, request);
            enhancementMessages.addAll(missingClassMessages);
            log.info("✓ Missing class generation completed");

            // Step 4: Enhance business logic with CRUD operations
            log.info("\n[Step 4/6] Implementing business logic...");
            List<String> businessLogicMessages = businessLogicEnhancerService.enhanceBusinessLogic(projectDir, request);
            enhancementMessages.addAll(businessLogicMessages);
            log.info("✓ Business logic implementation completed");

            // Step 5: Enhance code quality (logging, exceptions, documentation)
            log.info("\n[Step 5/6] Enhancing code quality...");
            List<String> qualityMessages = codeQualityEnhancerService.enhanceCodeQuality(projectDir, request);
            enhancementMessages.addAll(qualityMessages);
            log.info("✓ Code quality enhancement completed");

            // Step 6: Detect and fix compile errors
            log.info("\n[Step 6/6] Detecting and fixing compile errors...");
            List<String> compileMessages = compileErrorDetectionService.detectAndFixErrors(projectDir, request);
            enhancementMessages.addAll(compileMessages);
            log.info("✓ Compile error fixing completed");

            log.info("\n" + "=".repeat(80));
            log.info("Microservice Enhancement Summary");
            log.info("=".repeat(80));
            log.info("Total enhancements applied: {}", enhancementMessages.size());
            enhancementMessages.forEach(msg -> log.info("  - {}", msg));
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("Error during microservice enhancement", e);
            enhancementMessages.add("Enhancement failed: " + e.getMessage());
        }

        return enhancementMessages;
    }

    /**
     * Performs only critical enhancements (compile fixes and missing classes).
     *
     * @param projectDir Path to the generated project directory
     * @param request Original code generation request
     * @return List of enhancement messages
     */
    public List<String> performCriticalEnhancements(Path projectDir, CodeGenerationRequest request) {
        log.info("Performing critical enhancements for: {}", projectDir.getFileName());

        List<String> enhancementMessages = new ArrayList<>();

        try {
            // Fix compile errors
            enhancementMessages.addAll(
                    compileErrorDetectionService.detectAndFixErrors(projectDir, request)
            );

            // Generate missing classes from Swagger
            enhancementMessages.addAll(
                    swaggerAnalysisService.analyzeAndValidate(projectDir, request)
            );
            enhancementMessages.addAll(
                    missingClassGeneratorService.generateMissingClasses(projectDir, request)
            );

        } catch (Exception e) {
            log.error("Error during critical enhancements", e);
            enhancementMessages.add("Critical enhancement failed: " + e.getMessage());
        }

        return enhancementMessages;
    }
}
