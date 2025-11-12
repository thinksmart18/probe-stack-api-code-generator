package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.config.CodeGeneratorConfig;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import org.apache.maven.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Service for advanced POM customization including plugins
 *
 * UPDATED: Now supports reading from separate XML files:
 * - maven_config/dependencies.xml
 * - maven_config/plugins.xml
 * - maven_config/properties.xml
 */
@Service
public class PomCustomizationService {

    private static final Logger log = LoggerFactory.getLogger(PomCustomizationService.class);

    private final CodeGeneratorConfig config;

    @Autowired
    public PomCustomizationService(CodeGeneratorConfig config) {
        this.config = config;
    }

    /**
     * Merges custom POM additions from template directory
     *
     * NEW: Reads from separate files in maven_config directory:
     * - codegen_config/maven_config/dependencies.xml
     * - codegen_config/maven_config/plugins.xml
     * - codegen_config/maven_config/properties.xml
     *
     * @param pomPath Path to the generated pom.xml
     * @param templateDir Path to template directory (codegen_config)
     */
    public void mergePomCustomizations(Path pomPath, Path templateDir) {
        log.info("Merging POM customizations from template directory");

        try {
            // Read the generated POM
            Model model = readPomModel(pomPath);

            // Get maven_config directory
            Path mavenConfigDir = templateDir.resolve(config.getTemplates().getPomTemplate());

            if (Files.exists(mavenConfigDir) && Files.isDirectory(mavenConfigDir)) {
                log.info("Found maven_config directory: {}", mavenConfigDir);

                // Merge from separate XML files
                mergeFromSeparateFiles(model, mavenConfigDir);

            } else {
                log.info("Maven config directory not found at: {}, using default configuration", mavenConfigDir);
                mergeDefaultConfiguration(model);
            }

            // Write updated POM
            writePomModel(model, pomPath);

            log.info("Successfully merged POM customizations");

        } catch (Exception e) {
            throw new CodeGenerationException("Failed to merge POM customizations", e);
        }
    }

    /**
     * Merges POM configurations from separate XML files
     *
     * @param model Main POM model to merge into
     * @param mavenConfigDir Path to maven_config directory
     * @throws IOException if reading files fails
     */
    private void mergeFromSeparateFiles(Model model, Path mavenConfigDir) throws IOException {
        // 1. Merge properties from properties.xml
        Path propertiesFile = mavenConfigDir.resolve("properties.xml");
        if (Files.exists(propertiesFile)) {
            log.info("Merging properties from: {}", propertiesFile);
            Model propertiesModel = readPomModel(propertiesFile);
            mergeProperties(model, propertiesModel);
        } else {
            log.debug("No properties.xml found");
        }

        // 2. Merge dependencies from dependencies.xml
        Path dependenciesFile = mavenConfigDir.resolve("dependencies.xml");
        if (Files.exists(dependenciesFile)) {
            log.info("Merging dependencies from: {}", dependenciesFile);
            Model dependenciesModel = readPomModel(dependenciesFile);
            mergeDependencies(model, dependenciesModel);
            mergeDependencyManagement(model, dependenciesModel);
        } else {
            log.debug("No dependencies.xml found");
        }

        // 3. Merge plugins from plugins.xml
        Path pluginsFile = mavenConfigDir.resolve("plugins.xml");
        if (Files.exists(pluginsFile)) {
            log.info("Merging plugins from: {}", pluginsFile);
            Model pluginsModel = readPomModel(pluginsFile);
            mergePlugins(model, pluginsModel);
        } else {
            log.debug("No plugins.xml found");
        }
    }

    /**
     * Reads Maven POM model from file
     */
    private Model readPomModel(Path pomPath) throws IOException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (FileReader fileReader = new FileReader(pomPath.toFile())) {
            return reader.read(fileReader);
        } catch (Exception e) {
            throw new IOException("Failed to read POM file: " + pomPath, e);
        }
    }

    /**
     * Writes Maven POM model to file
     */
    private void writePomModel(Model model, Path pomPath) throws IOException {
        MavenXpp3Writer writer = new MavenXpp3Writer();
        try (FileWriter fileWriter = new FileWriter(pomPath.toFile())) {
            writer.write(fileWriter, model);
        }
    }

    /**
     * Merges properties from additions into main model
     */
    private void mergeProperties(Model mainModel, Model additionsModel) {
        Properties mainProps = mainModel.getProperties();
        Properties additionProps = additionsModel.getProperties();

        if (additionProps != null && !additionProps.isEmpty()) {
            int addedCount = 0;
            for (Object key : additionProps.keySet()) {
                if (!mainProps.containsKey(key)) {
                    mainProps.setProperty((String) key, (String) additionProps.get(key));
                    log.debug("Added property: {} = {}", key, additionProps.get(key));
                    addedCount++;
                } else {
                    log.debug("Property already exists, skipping: {}", key);
                }
            }
            log.info("Merged {} properties from template", addedCount);
        }
    }

    /**
     * Merges dependencies from additions into main model
     */
    private void mergeDependencies(Model mainModel, Model additionsModel) {
        List<Dependency> mainDeps = mainModel.getDependencies();
        List<Dependency> additionDeps = additionsModel.getDependencies();

        if (additionDeps != null && !additionDeps.isEmpty()) {
            int addedCount = 0;
            for (Dependency dep : additionDeps) {
                if (!dependencyExists(mainDeps, dep)) {
                    mainDeps.add(dep);
                    log.debug("Added dependency: {}:{}", dep.getGroupId(), dep.getArtifactId());
                    addedCount++;
                } else {
                    log.debug("Dependency already exists, skipping: {}:{}",
                            dep.getGroupId(), dep.getArtifactId());
                }
            }
            log.info("Merged {} dependencies from template", addedCount);
        }
    }

    /**
     * Merges build plugins from additions into main model
     */
    private void mergePlugins(Model mainModel, Model additionsModel) {
        Build mainBuild = mainModel.getBuild();
        if (mainBuild == null) {
            mainBuild = new Build();
            mainModel.setBuild(mainBuild);
        }

        Build additionsBuild = additionsModel.getBuild();
        if (additionsBuild != null && additionsBuild.getPlugins() != null) {
            List<Plugin> mainPlugins = mainBuild.getPlugins();
            List<Plugin> additionPlugins = additionsBuild.getPlugins();

            int addedCount = 0;
            for (Plugin plugin : additionPlugins) {
                if (!pluginExists(mainPlugins, plugin)) {
                    mainPlugins.add(plugin);
                    log.debug("Added plugin: {}:{}", plugin.getGroupId(), plugin.getArtifactId());
                    addedCount++;
                } else {
                    log.debug("Plugin already exists, skipping: {}:{}",
                            plugin.getGroupId(), plugin.getArtifactId());
                }
            }
            log.info("Merged {} plugins from template", addedCount);
        }
    }

    /**
     * Merges dependency management section
     */
    private void mergeDependencyManagement(Model mainModel, Model additionsModel) {
        DependencyManagement additionsDm = additionsModel.getDependencyManagement();

        if (additionsDm != null && additionsDm.getDependencies() != null) {
            DependencyManagement mainDm = mainModel.getDependencyManagement();
            if (mainDm == null) {
                mainDm = new DependencyManagement();
                mainModel.setDependencyManagement(mainDm);
            }

            List<Dependency> mainDeps = mainDm.getDependencies();
            List<Dependency> additionDeps = additionsDm.getDependencies();

            int addedCount = 0;
            for (Dependency dep : additionDeps) {
                if (!dependencyExists(mainDeps, dep)) {
                    mainDeps.add(dep);
                    log.debug("Added managed dependency: {}:{}",
                            dep.getGroupId(), dep.getArtifactId());
                    addedCount++;
                }
            }
            if (addedCount > 0) {
                log.info("Merged {} managed dependencies from template", addedCount);
            }
        }
    }

    /**
     * Merges default configuration from application.yml
     */
    private void mergeDefaultConfiguration(Model model) {
        log.info("Using default configuration from application.yml");

        // Merge properties
        Properties properties = model.getProperties();
        if (config.getPom() != null && config.getPom().getProperties() != null) {
            config.getPom().getProperties().forEach((key, value) -> {
                if (!properties.containsKey(key)) {
                    properties.setProperty(key, value);
                    log.debug("Added default property: {} = {}", key, value);
                }
            });
        }

        // Merge dependencies
        List<Dependency> existingDeps = model.getDependencies();
        if (config.getPom() != null && config.getPom().getDependencies() != null) {
            for (CodeGeneratorConfig.DependencyConfig depConfig : config.getPom().getDependencies()) {
                if (!dependencyExistsFromConfig(existingDeps, depConfig)) {
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
                    log.debug("Added default dependency: {}:{}",
                            depConfig.getGroupId(), depConfig.getArtifactId());
                }
            }
        }
    }

    /**
     * Checks if a dependency exists in the list
     */
    private boolean dependencyExists(List<Dependency> dependencies, Dependency dependency) {
        return dependencies.stream()
                .anyMatch(dep ->
                        dep.getGroupId().equals(dependency.getGroupId()) &&
                                dep.getArtifactId().equals(dependency.getArtifactId())
                );
    }

    /**
     * Checks if a dependency exists from config
     */
    private boolean dependencyExistsFromConfig(List<Dependency> dependencies,
                                               CodeGeneratorConfig.DependencyConfig depConfig) {
        return dependencies.stream()
                .anyMatch(dep ->
                        dep.getGroupId().equals(depConfig.getGroupId()) &&
                                dep.getArtifactId().equals(depConfig.getArtifactId())
                );
    }

    /**
     * Checks if a plugin exists in the list
     */
    private boolean pluginExists(List<Plugin> plugins, Plugin plugin) {
        return plugins.stream()
                .anyMatch(p ->
                        p.getGroupId().equals(plugin.getGroupId()) &&
                                p.getArtifactId().equals(plugin.getArtifactId())
                );
    }
}