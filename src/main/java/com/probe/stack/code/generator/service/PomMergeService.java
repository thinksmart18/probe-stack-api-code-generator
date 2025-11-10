package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.config.CodeGeneratorConfig;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Service for merging Maven POM files
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PomMergeService {
    
    private final CodeGeneratorConfig config;
    
    /**
     * Merges additional properties, dependencies, and plugins into generated pom.xml
     *
     * @param pomPath Path to the pom.xml file
     */
    public void mergePomConfiguration(Path pomPath) {
        log.info("Merging POM configuration: {}", pomPath);
        
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model;
            
            try (FileReader fileReader = new FileReader(pomPath.toFile())) {
                model = reader.read(fileReader);
            }
            
            // Merge properties
            mergeProperties(model);
            
            // Merge dependencies
            mergeDependencies(model);
            
            // Write updated POM
            MavenXpp3Writer writer = new MavenXpp3Writer();
            try (FileWriter fileWriter = new FileWriter(pomPath.toFile())) {
                writer.write(fileWriter, model);
            }
            
            log.info("Successfully merged POM configuration");
            
        } catch (Exception e) {
            throw new CodeGenerationException("Failed to merge POM configuration", e);
        }
    }
    
    /**
     * Merges additional properties into the POM
     */
    private void mergeProperties(Model model) {
        Properties properties = model.getProperties();
        
        if (config.getPom().getProperties() != null) {
            for (Map.Entry<String, String> entry : config.getPom().getProperties().entrySet()) {
                if (!properties.containsKey(entry.getKey())) {
                    properties.setProperty(entry.getKey(), entry.getValue());
                    log.debug("Added property: {} = {}", entry.getKey(), entry.getValue());
                }
            }
        }
    }
    
    /**
     * Merges additional dependencies into the POM
     */
    private void mergeDependencies(Model model) {
        if (config.getPom().getDependencies() == null) {
            return;
        }
        
        List<Dependency> existingDeps = model.getDependencies();
        
        for (CodeGeneratorConfig.DependencyConfig depConfig : config.getPom().getDependencies()) {
            if (!dependencyExists(existingDeps, depConfig)) {
                Dependency dependency = new Dependency();
                dependency.setGroupId(depConfig.getGroupId());
                dependency.setArtifactId(depConfig.getArtifactId());
                
                if (depConfig.getVersion() != null) {
                    dependency.setVersion(depConfig.getVersion());
                }
                
                if (depConfig.getScope() != null) {
                    dependency.setScope(depConfig.getScope());
                }
                
                existingDeps.add(dependency);
                log.debug("Added dependency: {}:{}", depConfig.getGroupId(), depConfig.getArtifactId());
            }
        }
    }
    
    /**
     * Checks if a dependency already exists in the POM
     */
    private boolean dependencyExists(List<Dependency> dependencies, 
                                    CodeGeneratorConfig.DependencyConfig depConfig) {
        return dependencies.stream()
                .anyMatch(dep -> 
                    dep.getGroupId().equals(depConfig.getGroupId()) &&
                    dep.getArtifactId().equals(depConfig.getArtifactId())
                );
    }
}