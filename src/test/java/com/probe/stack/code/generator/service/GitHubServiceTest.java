package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.dto.CodeGenerationResponse;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GitHubService
 * Note: These are unit tests for validation logic only.
 * Full integration tests with real GitHub API require a valid token.
 */
class GitHubServiceTest {

    private GitHubService gitHubService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        gitHubService = new GitHubService();
    }

    @Test
    void testCreateAndPushToGitHub_DisabledConfig() throws Exception {
        // Arrange
        Path projectDir = tempDir.resolve("test-project");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("README.md"), "Test Project");
        Files.writeString(projectDir.resolve("pom.xml"), "<project></project>");

        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .artifactId("test-service")
                .groupName("com.test")
                .basePackage("com.test.service")
                .githubToken("test-token")
                .gitHubConfig(CodeGenerationRequest.GitHubConfig.builder()
                        .enabled(false)
                        .build())
                .build();

        // Act
        CodeGenerationResponse.GitHubRepositoryInfo result =
                gitHubService.createAndPushToGitHub(projectDir, request);

        // Assert
        assertNull(result, "Should return null when GitHub config is disabled");
    }

    @Test
    void testCreateAndPushToGitHub_NullConfig() throws Exception {
        // Arrange
        Path projectDir = tempDir.resolve("test-project");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("README.md"), "Test Project");

        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .artifactId("test-service")
                .groupName("com.test")
                .basePackage("com.test.service")
                .githubToken("test-token")
                .gitHubConfig(null)
                .build();

        // Act
        CodeGenerationResponse.GitHubRepositoryInfo result =
                gitHubService.createAndPushToGitHub(projectDir, request);

        // Assert
        assertNull(result, "Should return null when GitHub config is null");
    }

    @Test
    void testCreateAndPushToGitHub_MissingToken() {
        // Arrange
        Path projectDir = tempDir.resolve("test-project");

        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .artifactId("test-service")
                .groupName("com.test")
                .basePackage("com.test.service")
                .githubToken(null)
                .gitHubConfig(CodeGenerationRequest.GitHubConfig.builder()
                        .enabled(true)
                        .organization("test-org")
                        .build())
                .build();

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class, () -> {
            gitHubService.createAndPushToGitHub(projectDir, request);
        });

        assertTrue(exception.getMessage().contains("GitHub token is required"),
                "Exception message should mention GitHub token requirement");
    }

    @Test
    void testCreateAndPushToGitHub_EmptyToken() {
        // Arrange
        Path projectDir = tempDir.resolve("test-project");

        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .artifactId("test-service")
                .groupName("com.test")
                .basePackage("com.test.service")
                .githubToken("")
                .gitHubConfig(CodeGenerationRequest.GitHubConfig.builder()
                        .enabled(true)
                        .organization("test-org")
                        .build())
                .build();

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class, () -> {
            gitHubService.createAndPushToGitHub(projectDir, request);
        });

        assertTrue(exception.getMessage().contains("GitHub token is required"));
    }

    @Test
    void testCreateAndPushToGitHub_MissingOrganization() {
        // Arrange
        Path projectDir = tempDir.resolve("test-project");

        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .artifactId("test-service")
                .groupName("com.test")
                .basePackage("com.test.service")
                .githubToken("ghp_test_token_12345")
                .gitHubConfig(CodeGenerationRequest.GitHubConfig.builder()
                        .enabled(true)
                        .organization(null)
                        .build())
                .build();

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class, () -> {
            gitHubService.createAndPushToGitHub(projectDir, request);
        });

        assertTrue(exception.getMessage().contains("organization/username is required"),
                "Exception message should mention organization requirement");
    }

    @Test
    void testCreateAndPushToGitHub_EmptyOrganization() {
        // Arrange
        Path projectDir = tempDir.resolve("test-project");

        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .artifactId("test-service")
                .groupName("com.test")
                .basePackage("com.test.service")
                .githubToken("ghp_test_token_12345")
                .gitHubConfig(CodeGenerationRequest.GitHubConfig.builder()
                        .enabled(true)
                        .organization("")
                        .build())
                .build();

        // Act & Assert
        CodeGenerationException exception = assertThrows(CodeGenerationException.class, () -> {
            gitHubService.createAndPushToGitHub(projectDir, request);
        });

        assertTrue(exception.getMessage().contains("organization/username is required"));
    }

    @Test
    void testGitHubConfig_DefaultValues() {
        // Arrange & Act
        CodeGenerationRequest.GitHubConfig config = CodeGenerationRequest.GitHubConfig.builder()
                .enabled(true)
                .organization("test-org")
                .build();

        // Assert
        assertTrue(config.isEnabled(), "Enabled should be true");
        assertTrue(config.isPrivate(), "Default should be private repository");
        assertEquals("main", config.getBranchName(), "Default branch should be main");
        assertFalse(config.isAutoInit(), "AutoInit should be false by default");
        assertNotNull(config.getCommitMessage(), "Commit message should have default value");
        assertTrue(config.getCommitMessage().contains("Initial commit"),
                "Default commit message should contain 'Initial commit'");
    }

    @Test
    void testGitHubConfig_CustomValues() {
        // Arrange & Act
        CodeGenerationRequest.GitHubConfig config = CodeGenerationRequest.GitHubConfig.builder()
                .enabled(true)
                .organization("my-org")
                .repositoryName("custom-repo")
                .description("Custom repository description")
                .isPrivate(false)
                .branchName("develop")
                .commitMessage("Custom initial commit message")
                .autoInit(true)
                .build();

        // Assert
        assertTrue(config.isEnabled());
        assertEquals("my-org", config.getOrganization());
        assertEquals("custom-repo", config.getRepositoryName());
        assertEquals("Custom repository description", config.getDescription());
        assertFalse(config.isPrivate(), "Should be public repository");
        assertEquals("develop", config.getBranchName());
        assertEquals("Custom initial commit message", config.getCommitMessage());
        assertTrue(config.isAutoInit());
    }

    @Test
    void testGitHubConfig_RepositoryNameDefaultsToArtifactId() {
        // Arrange & Act
        CodeGenerationRequest.GitHubConfig config = CodeGenerationRequest.GitHubConfig.builder()
                .enabled(true)
                .organization("test-org")
                .build();

        // Assert
        assertNull(config.getRepositoryName(),
                "Repository name should be null to allow defaulting to artifactId");
    }

    @Test
    void testGitHubRepositoryInfo_AllFieldsSet() {
        // Arrange & Act
        CodeGenerationResponse.GitHubRepositoryInfo info =
                CodeGenerationResponse.GitHubRepositoryInfo.builder()
                        .repositoryUrl("https://github.com/test-org/test-repo")
                        .cloneUrl("https://github.com/test-org/test-repo.git")
                        .sshUrl("git@github.com:test-org/test-repo.git")
                        .fullName("test-org/test-repo")
                        .commitSha("abc123def456")
                        .branchName("main")
                        .pushSuccessful(true)
                        .build();

        // Assert
        assertNotNull(info);
        assertEquals("https://github.com/test-org/test-repo", info.getRepositoryUrl());
        assertEquals("https://github.com/test-org/test-repo.git", info.getCloneUrl());
        assertEquals("git@github.com:test-org/test-repo.git", info.getSshUrl());
        assertEquals("test-org/test-repo", info.getFullName());
        assertEquals("abc123def456", info.getCommitSha());
        assertEquals("main", info.getBranchName());
        assertTrue(info.isPushSuccessful());
    }

    @Test
    void testGitHubConfig_BranchNameVariations() {
        // Test different branch names
        String[] branchNames = {"main", "master", "develop", "feature/test", "release/v1.0"};

        for (String branchName : branchNames) {
            CodeGenerationRequest.GitHubConfig config =
                    CodeGenerationRequest.GitHubConfig.builder()
                            .enabled(true)
                            .organization("test-org")
                            .branchName(branchName)
                            .build();

            assertEquals(branchName, config.getBranchName(),
                    "Branch name should be: " + branchName);
        }
    }

    @Test
    void testGitHubConfig_PrivateRepositoryOptions() {
        // Test private repository
        CodeGenerationRequest.GitHubConfig privateConfig =
                CodeGenerationRequest.GitHubConfig.builder()
                        .enabled(true)
                        .organization("test-org")
                        .isPrivate(true)
                        .build();

        assertTrue(privateConfig.isPrivate(), "Should be private repository");

        // Test public repository
        CodeGenerationRequest.GitHubConfig publicConfig =
                CodeGenerationRequest.GitHubConfig.builder()
                        .enabled(true)
                        .organization("test-org")
                        .isPrivate(false)
                        .build();

        assertFalse(publicConfig.isPrivate(), "Should be public repository");
    }

    @Test
    void testCompleteRequestWithGitHubConfig() {
        // Arrange & Act - Complete request object
        CodeGenerationRequest request = CodeGenerationRequest.builder()
                .openApiSpecUrl("https://example.com/openapi.yaml")
                .groupName("com.example")
                .artifactId("test-service")
                .basePackage("com.example.testservice")
                .version("1.0.0")
                .githubToken("ghp_test_token_12345")
                .returnAsArchive(true)
                .gitHubConfig(CodeGenerationRequest.GitHubConfig.builder()
                        .enabled(true)
                        .organization("example-org")
                        .repositoryName("test-service")
                        .description("Test service generated from OpenAPI")
                        .isPrivate(false)
                        .branchName("main")
                        .commitMessage("Initial commit: Test Service")
                        .build())
                .build();

        // Assert
        assertNotNull(request);
        assertNotNull(request.getGitHubConfig());
        assertTrue(request.getGitHubConfig().isEnabled());
        assertEquals("example-org", request.getGitHubConfig().getOrganization());
        assertEquals("test-service", request.getGitHubConfig().getRepositoryName());
        assertFalse(request.getGitHubConfig().isPrivate());
        assertEquals("main", request.getGitHubConfig().getBranchName());
    }
}