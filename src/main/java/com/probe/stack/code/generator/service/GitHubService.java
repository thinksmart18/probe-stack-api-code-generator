package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.config.GitHubConfig;
import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.dto.CodeGenerationResponse;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import com.probe.stack.code.generator.util.AppConstants;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service for GitHub operations - repository creation and code push
 */
@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    private final GitHubConfig githubPropertiesConfig;

    @Autowired
    public GitHubService(GitHubConfig githubPropertiesConfig) {
        this.githubPropertiesConfig = githubPropertiesConfig;
    }

    /**
     * Creates a GitHub repository and pushes the generated code
     *
     * @param projectDir Path to the generated project
     * @param request Code generation request with GitHub config
     * @return GitHub repository information
     */
    public CodeGenerationResponse.GitHubRepositoryInfo createAndPushToGitHub(
            Path projectDir, CodeGenerationRequest request) {

        if (githubPropertiesConfig == null || !githubPropertiesConfig.getPush().isEnabled()) {
            log.info("GitHub integration not enabled, skipping repository creation");
            return null;
        }

        validateGitHubConfig(request);

        try {
            // Connect to GitHub
            GitHub github = connectToGitHub(githubPropertiesConfig.getPersonal().getAccessToken());

            // Create repository
            GHRepository repository = createRepository(github, request);

            // Initialize Git and push code
            String commitSha = initializeAndPush(projectDir, repository, request);

            // Build response
            return buildGitHubRepositoryInfo(repository, commitSha, request);

        } catch (IOException | GitAPIException e) {
            throw new CodeGenerationException("Failed to push code to GitHub: " + e.getMessage(), e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validates GitHub configuration
     */
    private void validateGitHubConfig(CodeGenerationRequest request) {

        if (githubPropertiesConfig.getPersonal().getAccessToken() == null || githubPropertiesConfig.getPersonal().getAccessToken().isEmpty()) {
            throw new CodeGenerationException("GitHub token is required for repository creation");
        }

        if (request.getOrganization() == null || request.getOrganization().isEmpty()) {
            throw new CodeGenerationException(
                    "GitHub username is required. " +
                            "For personal accounts, use your GitHub username. " +
                            "For organizations, use the organization name."
            );
        }
    }

    /**
     * Connects to GitHub using provided token
     */
    private GitHub connectToGitHub(String token) throws IOException {
        log.info("Connecting to GitHub");
        return new GitHubBuilder().withOAuthToken(token).build();
    }

    /**
     * Creates a new GitHub repository
     */
    private GHRepository createRepository(GitHub github, CodeGenerationRequest request)
            throws IOException {

        //CodeGenerationRequest.GitHubConfig config = request.getGitHubConfig();
        String repoName = request.getRepositoryName() != null
                ? request.getRepositoryName()
                : request.getArtifactId();

        String fullRepoName = request.getOrganization() + "/" + repoName;

        log.info("Checking if repository exists: {}", fullRepoName);

        try {
            // Try to get existing repository
            GHRepository existingRepo = github.getRepository(fullRepoName);

            if (existingRepo != null) {
                log.info("Repository already exists, will push to existing repository: {}",
                        existingRepo.getHtmlUrl());
                return existingRepo;
            }
        } catch (IOException e) {
            // Repository doesn't exist, this is expected - continue with creation
            log.debug("Repository doesn't exist yet, will create new one");
        }

        log.info("Creating new GitHub repository: {}", fullRepoName);

        // Create repository builder
        GHCreateRepositoryBuilder builder;

        // Check if organization or personal account
        try {
            GHOrganization org = github.getOrganization(request.getOrganization());
            builder = org.createRepository(repoName);
            log.info("Creating repository in organization: {}", request.getOrganization());
        } catch (IOException e) {
            // Not an organization, use personal account
            log.info("Creating repository in personal account: {}", request.getOrganization());
            builder = github.createRepository(repoName);
        }

        // Configure repository
        String description = githubPropertiesConfig.getDescription() != null
                ? githubPropertiesConfig.getDescription()
                : "Generated Spring Boot application from OpenAPI specification";

        // IMPORTANT: Don't auto-init as it creates a commit that conflicts with our push
        builder.description(description)
                .private_(githubPropertiesConfig.getConfig().isPrivate())
                .autoInit(false);  // Always false to avoid push conflicts

        try {
            GHRepository repository = builder.create();
            log.info("Repository created successfully: {}", repository.getHtmlUrl());
            return repository;
        } catch (IOException e) {
            // Check if it's a "repository already exists" error
            if (e.getMessage() != null && e.getMessage().contains("name already exists")) {
                log.warn("Repository creation failed because it already exists, attempting to retrieve it");
                // Try one more time to get the existing repository
                try {
                    return github.getRepository(fullRepoName);
                } catch (IOException ex) {
                    throw new CodeGenerationException(
                            "Repository '" + repoName + "' already exists but cannot be accessed. " +
                                    "Please ensure you have write access to this repository or use a different repository name.",
                            ex
                    );
                }
            }
            throw e;
        }
    }

    /**
     * Initializes Git repository and pushes code
     */
    private String initializeAndPush(Path projectDir, GHRepository repository,
                                     CodeGenerationRequest request)
            throws GitAPIException, IOException, URISyntaxException {

        CodeGenerationRequest.GitHubConfig config = request.getGitHubConfig();
        File projectDirFile = projectDir.toFile();
        String branchName = request.getBranchName();

        log.info("Initializing Git repository in: {}", projectDir);

        // Initialize Git repository
        Git git = null;
        try {
            git = Git.init()
                    .setDirectory(projectDirFile)
                    .setInitialBranch(branchName)
                    .call();

            log.info("Git repository initialized with branch: {}", branchName);

            // Create .gitignore if not exists
            createGitIgnore(projectDir);

            // Add all files
            log.info("Adding files to Git...");
            git.add()
                    .addFilepattern(".")
                    .call();

            log.info("Files added successfully");

            // Commit
            log.info("Creating initial commit on branch: {}", branchName);
            git.commit()
                    .setMessage(githubPropertiesConfig.getConfig().getCommit().getMessage())
                    .setAuthor("OpenAPI Code Generator", "codegen@example.com")
                    .call();

            log.info("Initial commit created successfully");

            // Verify branch exists
            String currentBranch = git.getRepository().getBranch();
            log.info("Current branch: {}", currentBranch);

            // Set remote
            log.info("Setting remote origin: {}", repository.getHttpTransportUrl());
            git.remoteAdd()
                    .setName("origin")
                    .setUri(new URIish(repository.getHttpTransportUrl()))
                    .call();

            log.info("Remote added successfully");

            // Push to GitHub
            log.info("Pushing to GitHub - branch: {}, repository: {}", branchName, repository.getFullName());

            try {
                git.push()
                        .setRemote("origin")
                        .setRefSpecs(new RefSpec(branchName + ":" + branchName))
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                                githubPropertiesConfig.getPersonal().getAccessToken(), ""))
                        .setForce(true)
                        .call();

                log.info("Successfully pushed code to GitHub repository");

            } catch (GitAPIException e) {
                log.error("Failed to push to GitHub: {}", e.getMessage(), e);

                // Provide more specific error messages
                String errorMsg = e.getMessage();
                if (errorMsg != null) {
                    if (errorMsg.contains("not authorized") || errorMsg.contains("authentication failed")) {
                        throw new CodeGenerationException(
                                "GitHub authentication failed. Please verify your token has 'repo' scope and is not expired.", e);
                    } else if (errorMsg.contains("protected branch")) {
                        throw new CodeGenerationException(
                                "Cannot push to protected branch '" + branchName + "'. Try using a different branch or disable branch protection.", e);
                    } else if (errorMsg.contains("repository not found")) {
                        throw new CodeGenerationException(
                                "Repository not found or you don't have access. Verify the repository exists and you have write permissions.", e);
                    }
                }

                throw new CodeGenerationException("Failed to push code to GitHub: " + errorMsg, e);
            }

            // Get commit SHA
            String commitSha = git.getRepository()
                    .resolve("HEAD")
                    .getName();

            log.info("Commit SHA: {}", commitSha);
            return commitSha;

        } finally {
            // Always close Git repository
            if (git != null) {
                git.close();
            }
        }
    }

    /**
     * Creates a .gitignore file for Spring Boot projects
     */
    private void createGitIgnore(Path projectDir) throws IOException {
        Path gitIgnorePath = projectDir.resolve(".gitignore");

        if (!Files.exists(gitIgnorePath)) {

            Files.writeString(gitIgnorePath, AppConstants.GIT_IGNORE_CONTENT);
            log.info("Created .gitignore file");
        }
    }

    /**
     * Builds GitHub repository information for response
     */
    private CodeGenerationResponse.GitHubRepositoryInfo buildGitHubRepositoryInfo(
            GHRepository repository, String commitSha, CodeGenerationRequest request)
            throws IOException {

        return CodeGenerationResponse.GitHubRepositoryInfo.builder()
                .repositoryUrl(repository.getHtmlUrl().toString())
                .cloneUrl(repository.getHttpTransportUrl())
                .sshUrl(repository.getSshUrl())
                .fullName(repository.getFullName())
                .commitSha(commitSha)
                .branchName(request.getBranchName())
                .pushSuccessful(true)
                .build();
    }

    /**
     * Deletes a GitHub repository (for cleanup/rollback)
     */
    public void deleteRepository(String token, String fullRepoName) {
        try {
            GitHub github = new GitHubBuilder().withOAuthToken(token).build();
            GHRepository repository = github.getRepository(fullRepoName);
            repository.delete();
            log.info("Deleted GitHub repository: {}", fullRepoName);
        } catch (IOException e) {
            log.error("Failed to delete repository: {}", fullRepoName, e);
        }
    }
}