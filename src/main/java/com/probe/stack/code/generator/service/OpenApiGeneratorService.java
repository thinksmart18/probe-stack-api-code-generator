package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.config.CodeGeneratorConfig;
import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/**
 * Service for generating Spring Boot code using OpenAPI Generator
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiGeneratorService {
    
    private final CodeGeneratorConfig config;
    
    /**
     * Generates Spring Boot project from OpenAPI specification
     *
     * @param specPath Path to the OpenAPI spec file
     * @param request Code generation request
     * @param outputDir Output directory for generated code
     * @return List of generated files
     */
    public List<String> generateCode(Path specPath, CodeGenerationRequest request, Path outputDir) {
        log.info("Generating code for artifact: {}", request.getArtifactId());
        
        try {
            CodegenConfigurator configurator = new CodegenConfigurator();
            
            // Basic configuration
            configurator.setInputSpec(specPath.toString());
            configurator.setGeneratorName(config.getOpenapi().getGenerator().getLanguage());
            configurator.setLibrary(config.getOpenapi().getGenerator().getLibrary());
            configurator.setOutputDir(outputDir.toString());

            // Package configuration
            String apiPackage = request.getBasePackage() + "." + config.getOpenapi().getGenerator().getApiPackageSuffix();
            String modelPackage = request.getBasePackage() + "." + config.getOpenapi().getGenerator().getModelPackageSuffix();
            String invokerPackage = request.getBasePackage();
            
            configurator.setApiPackage(apiPackage);
            configurator.setModelPackage(modelPackage);
            configurator.setInvokerPackage(invokerPackage);
            
            // Group and artifact configuration
            configurator.setGroupId(request.getGroupName());
            configurator.setArtifactId(request.getArtifactId());
            configurator.setArtifactVersion(request.getVersion());
            
            // Additional properties
            configurator.addAdditionalProperty("basePackage", request.getBasePackage());
            configurator.addAdditionalProperty("configPackage", request.getBasePackage() + ".config");
            configurator.addAdditionalProperty("java8", "true");
            configurator.addAdditionalProperty("dateLibrary", "java8");
            configurator.addAdditionalProperty("interfaceOnly", "false");
            configurator.addAdditionalProperty("delegatePattern", "false");
            configurator.addAdditionalProperty("useTags", "true");
            configurator.addAdditionalProperty("useSpringBoot3", "true");
            configurator.addAdditionalProperty("documentationProvider", "springdoc");
            
            // Generate code
            ClientOptInput clientOptInput = configurator.toClientOptInput();
            DefaultGenerator generator = new DefaultGenerator();
            
            List<String> generatedFiles = generator.opts(clientOptInput).generate()
                    .stream()
                    .map(file -> file.toString())
                    .toList();
            
            log.info("Successfully generated {} files", generatedFiles.size());
            return generatedFiles;
            
        } catch (Exception e) {
            throw new CodeGenerationException("Failed to generate code from OpenAPI specification", e);
        }
    }
}