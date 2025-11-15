package com.probe.stack.code.generator.service.enhancement;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for integrating AI tooling into generated microservices.
 * Adds and configures:
 * - Embabel (on top of Spring AI)
 * - Agent Communication Protocol (ACPJava)
 *
 * @author ProbeStack
 */
@Slf4j
@Service
public class AIToolingIntegrationService {

    private static final String SPRING_AI_VERSION = "1.0.0-M3";
    private static final String EMBABEL_VERSION = "0.1.0";
    private static final String ACP_JAVA_VERSION = "1.0.0";

    /**
     * Integrates AI tooling into the generated project.
     *
     * @param projectDir Path to the generated project directory
     * @param request Original code generation request
     * @return List of integration messages
     */
    public List<String> integrateAITooling(Path projectDir, CodeGenerationRequest request) {
        List<String> messages = new ArrayList<>();

        try {
            log.info("Integrating AI tooling into project...");

            // Add dependencies to pom.xml
            addAIDependencies(projectDir, messages);

            // Create AI configuration class
            createAIConfiguration(projectDir, request, messages);

            // Create Embabel configuration
            createEmbabelConfiguration(projectDir, request, messages);

            // Create ACP Java configuration
            createACPConfiguration(projectDir, request, messages);

            // Update application.properties with AI settings
            updateApplicationProperties(projectDir, messages);

            messages.add("✓ AI tooling integration completed successfully");

        } catch (Exception e) {
            log.error("Error integrating AI tooling", e);
            messages.add("AI tooling integration failed: " + e.getMessage());
        }

        return messages;
    }

    /**
     * Adds AI-related dependencies to the project's pom.xml.
     */
    private void addAIDependencies(Path projectDir, List<String> messages) throws Exception {
        Path pomPath = projectDir.resolve("pom.xml");
        if (!Files.exists(pomPath)) {
            messages.add("Warning: pom.xml not found, skipping dependency addition");
            return;
        }

        log.info("Adding AI dependencies to pom.xml...");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(pomPath.toFile());

        // Find or create dependencies element
        NodeList dependenciesList = doc.getElementsByTagName("dependencies");
        Element dependencies;

        if (dependenciesList.getLength() > 0) {
            dependencies = (Element) dependenciesList.item(0);
        } else {
            dependencies = doc.createElement("dependencies");
            doc.getDocumentElement().appendChild(dependencies);
        }

        // Add Spring AI dependency
        addDependency(doc, dependencies, "org.springframework.ai", "spring-ai-core", SPRING_AI_VERSION);
        messages.add("  Added Spring AI Core");

        // Add Spring AI OpenAI dependency (for Embabel)
        addDependency(doc, dependencies, "org.springframework.ai", "spring-ai-openai", SPRING_AI_VERSION);
        messages.add("  Added Spring AI OpenAI");

        // Add Embabel dependency (placeholder - adjust groupId/artifactId as needed)
        addDependency(doc, dependencies, "io.embabel", "embabel-spring-boot-starter", EMBABEL_VERSION);
        messages.add("  Added Embabel");

        // Add ACP Java dependency (placeholder - adjust groupId/artifactId as needed)
        addDependency(doc, dependencies, "io.acp", "acp-java-client", ACP_JAVA_VERSION);
        messages.add("  Added ACP Java");

        // Save the updated pom.xml
        savePomFile(doc, pomPath);
        messages.add("✓ Updated pom.xml with AI dependencies");
    }

    /**
     * Adds a dependency element to the pom.xml document.
     */
    private void addDependency(Document doc, Element dependencies,
                                String groupId, String artifactId, String version) {
        // Check if dependency already exists
        NodeList existingDeps = dependencies.getElementsByTagName("dependency");
        for (int i = 0; i < existingDeps.getLength(); i++) {
            Element dep = (Element) existingDeps.item(i);
            String existingArtifactId = getElementTextContent(dep, "artifactId");
            if (artifactId.equals(existingArtifactId)) {
                log.debug("Dependency {} already exists, skipping", artifactId);
                return;
            }
        }

        Element dependency = doc.createElement("dependency");

        Element groupIdElem = doc.createElement("groupId");
        groupIdElem.setTextContent(groupId);
        dependency.appendChild(groupIdElem);

        Element artifactIdElem = doc.createElement("artifactId");
        artifactIdElem.setTextContent(artifactId);
        dependency.appendChild(artifactIdElem);

        Element versionElem = doc.createElement("version");
        versionElem.setTextContent(version);
        dependency.appendChild(versionElem);

        dependencies.appendChild(dependency);
    }

    private String getElementTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    /**
     * Saves the updated pom.xml document to file.
     */
    private void savePomFile(Document doc, Path pomPath) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(pomPath.toFile());
        transformer.transform(source, result);
    }

    /**
     * Creates AI configuration class for the project.
     */
    private void createAIConfiguration(Path projectDir, CodeGenerationRequest request,
                                       List<String> messages) throws Exception {
        String basePackage = request.getBasePackage();
        String configPackage = basePackage + ".config";
        Path configDir = projectDir.resolve("src/main/java")
                .resolve(configPackage.replace('.', '/'));

        Files.createDirectories(configDir);

        String configContent = String.format("""
                package %s;

                import org.springframework.ai.chat.ChatClient;
                import org.springframework.ai.openai.OpenAiChatClient;
                import org.springframework.ai.openai.api.OpenAiApi;
                import org.springframework.beans.factory.annotation.Value;
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                /**
                 * Configuration for Spring AI integration.
                 * Enables AI-powered features in the microservice.
                 */
                @Configuration
                public class AIConfiguration {

                    @Value("${spring.ai.openai.api-key:}")
                    private String openAiApiKey;

                    @Value("${spring.ai.openai.model:gpt-4}")
                    private String model;

                    /**
                     * Creates the OpenAI API client.
                     */
                    @Bean
                    public OpenAiApi openAiApi() {
                        return new OpenAiApi(openAiApiKey);
                    }

                    /**
                     * Creates the ChatClient for AI interactions.
                     */
                    @Bean
                    public ChatClient chatClient(OpenAiApi openAiApi) {
                        return OpenAiChatClient.builder(openAiApi)
                                .withModel(model)
                                .build();
                    }
                }
                """, configPackage);

        Path configFile = configDir.resolve("AIConfiguration.java");
        Files.writeString(configFile, configContent);
        messages.add("  Created AIConfiguration.java");
    }

    /**
     * Creates Embabel configuration for enhanced AI capabilities.
     */
    private void createEmbabelConfiguration(Path projectDir, CodeGenerationRequest request,
                                            List<String> messages) throws Exception {
        String basePackage = request.getBasePackage();
        String configPackage = basePackage + ".config";
        Path configDir = projectDir.resolve("src/main/java")
                .resolve(configPackage.replace('.', '/'));

        Files.createDirectories(configDir);

        String configContent = String.format("""
                package %s;

                import org.springframework.context.annotation.Configuration;

                /**
                 * Configuration for Embabel - Enhanced AI capabilities on top of Spring AI.
                 * Provides advanced AI features including:
                 * - Context-aware AI interactions
                 * - Multi-model support
                 * - Enhanced prompt engineering
                 */
                @Configuration
                public class EmbabelConfiguration {

                    // Embabel configuration will be added here
                    // This is a placeholder for Embabel-specific beans and settings
                }
                """, configPackage);

        Path configFile = configDir.resolve("EmbabelConfiguration.java");
        Files.writeString(configFile, configContent);
        messages.add("  Created EmbabelConfiguration.java");
    }

    /**
     * Creates ACP (Agent Communication Protocol) Java configuration.
     */
    private void createACPConfiguration(Path projectDir, CodeGenerationRequest request,
                                        List<String> messages) throws Exception {
        String basePackage = request.getBasePackage();
        String configPackage = basePackage + ".config";
        Path configDir = projectDir.resolve("src/main/java")
                .resolve(configPackage.replace('.', '/'));

        Files.createDirectories(configDir);

        String configContent = String.format("""
                package %s;

                import org.springframework.context.annotation.Configuration;

                /**
                 * Configuration for Agent Communication Protocol (ACP) Java.
                 * Enables agent-to-agent communication capabilities.
                 */
                @Configuration
                public class ACPConfiguration {

                    // ACP configuration will be added here
                    // This is a placeholder for ACP-specific beans and settings
                }
                """, configPackage);

        Path configFile = configDir.resolve("ACPConfiguration.java");
        Files.writeString(configFile, configContent);
        messages.add("  Created ACPConfiguration.java");
    }

    /**
     * Updates application.properties with AI-related configuration.
     */
    private void updateApplicationProperties(Path projectDir, List<String> messages) throws Exception {
        Path propsPath = projectDir.resolve("src/main/resources/application.properties");

        if (!Files.exists(propsPath)) {
            Files.createDirectories(propsPath.getParent());
            Files.createFile(propsPath);
        }

        String aiProperties = """

                # Spring AI Configuration
                spring.ai.openai.api-key=${OPENAI_API_KEY:your-api-key-here}
                spring.ai.openai.model=gpt-4
                spring.ai.openai.temperature=0.7

                # Embabel Configuration
                embabel.enabled=true
                embabel.context-window=4096

                # ACP Java Configuration
                acp.enabled=true
                acp.agent-id=${spring.application.name}
                """;

        // Append AI properties if they don't already exist
        String existingContent = Files.readString(propsPath);
        if (!existingContent.contains("spring.ai.openai")) {
            Files.writeString(propsPath, existingContent + aiProperties);
            messages.add("  Updated application.properties with AI settings");
        }
    }
}
