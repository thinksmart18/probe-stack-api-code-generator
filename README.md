# Probe Stack API Code Generator

A Spring Boot microservice that automates the generation of complete Spring Boot applications from OpenAPI specifications. This service generates production-ready Spring Boot projects with REST controllers, services, repositories, and MongoDB integration.

## Features

- **OpenAPI Code Generation**: Generates Spring Boot projects from OpenAPI 3.0 specifications
- **Service Layer Generation**: Automatically creates service and repository classes for MongoDB
- **Customizable Templates**: Copy additional Java code and configuration files from templates
- **POM Merging**: Automatically merges Maven dependencies, properties, and plugins
- **Properties Merging**: Merges application properties into generated projects
- **Placeholder Replacement**: Updates placeholders in generated files (base packages, artifact IDs, etc.)
- **Archive Support**: Option to return generated projects as ZIP archives
- **GitHub Integration**: Create repositories and push generated code to GitHub
- **File Upload Support**: Upload OpenAPI specs directly or provide URLs
- **Automatic Cleanup**: Scheduled cleanup of old generated projects

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- 2GB RAM minimum

## Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd probe-stack-api-code-generator
mvn clean install
```

### 2. Configure

Create or edit `application.yaml`:

```yaml
probe:
  stack:
    generator:
      directories:
        output-base: ./generated-projects
        template-config: ./codegen_config
        temp: ./temp
      cleanup:
        hours: 24
        cron: "0 0 * * * *"
```

### 3. Run

```bash
mvn spring-boot:run
```

The service will start on `http://localhost:8080`

---

## API Documentation

## Main API: `/api/v1/codegen/generate`

The primary endpoint for generating Spring Boot projects from OpenAPI specifications.

### Endpoint Details

**Method:** `POST`
**URL:** `/api/v1/codegen/generate`
**Content-Type:** `application/json`
**Controller:** `CodeGenerationController.java:46`

### What This API Does

This API performs the complete code generation workflow:

1. **Downloads/Processes OpenAPI Specification** - Fetches the spec from URL or processes raw content
2. **Generates REST Controllers** - Creates Spring Boot REST controllers with all endpoints defined in OpenAPI spec
3. **Generates Model Classes** - Creates POJOs for all schemas/models in the spec
4. **Generates Service Classes** - Creates service layer for business logic with MongoDB integration
5. **Generates Repository Classes** - Creates Spring Data MongoDB repository interfaces
6. **Enhances with Templates** - Copies custom templates (configs, utilities, exception handlers, etc.)
7. **Merges POM Configuration** - Adds required dependencies and plugins to pom.xml
8. **Merges Application Properties** - Adds application configuration
9. **Updates README** - Generates project documentation
10. **Creates Archive (Optional)** - Packages project as ZIP file
11. **Pushes to GitHub (Optional)** - Creates GitHub repository and pushes code

### Request Body

#### Required Fields

| Field | Type | Description | Validation | Example |
|-------|------|-------------|------------|---------|
| `groupName` | String | Maven group ID | Must match pattern: `^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$` | `"com.example"` |
| `artifactId` | String | Maven artifact ID | Must match pattern: `^[a-z][a-z0-9-]*$` | `"my-service"` |
| `basePackage` | String | Base Java package | Must match pattern: `^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$` | `"com.example.myservice"` |

#### OpenAPI Specification (Choose One)

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `openApiSpecUrl` | String | URL to OpenAPI spec (YAML or JSON) | `"https://api.example.com/openapi.yaml"` |
| `specContent` | String | Raw OpenAPI spec content | `"openapi: 3.0.0\ninfo:\n  title: My API..."` |
| `specContentType` | String | Type when using specContent: `"json"` or `"yaml"` | `"yaml"` |

#### Optional Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `version` | String | `"1.0.0"` | Project version |
| `returnAsArchive` | Boolean | `false` | Return project as ZIP archive |
| `githubToken` | String | `null` | GitHub personal access token for private repos |
| `organization` | String | `null` | GitHub organization or username |
| `branchName` | String | `"main"` | Initial branch name |
| `repositoryName` | String | `null` | GitHub repository name (defaults to artifactId) |
| `gitHubConfig` | Object | `null` | GitHub integration configuration |

#### GitHub Configuration Object

When `gitHubConfig.enabled` is `true`, the service will create a GitHub repository and push the generated code.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | Boolean | `false` | Enable GitHub integration |
| `description` | String | `null` | Repository description |
| `isPrivate` | Boolean | `true` | Create private repository |
| `commitMessage` | String | `"Initial commit..."` | Commit message |
| `autoInit` | Boolean | `false` | Initialize with README |

### Request Examples

#### Example 1: Basic Request (URL-based)

```json
{
  "openApiSpecUrl": "https://petstore3.swagger.io/api/v3/openapi.json",
  "groupName": "com.example",
  "artifactId": "petstore-api",
  "basePackage": "com.example.petstore"
}
```

#### Example 2: With Raw Spec Content

```json
{
  "specContent": "openapi: 3.0.0\ninfo:\n  title: Sample API\n  version: 1.0.0\npaths:\n  /users:\n    get:\n      summary: Get users\n      responses:\n        '200':\n          description: Success",
  "specContentType": "yaml",
  "groupName": "com.mycompany",
  "artifactId": "sample-api",
  "basePackage": "com.mycompany.sample",
  "version": "2.0.0"
}
```

#### Example 3: With Archive and GitHub Integration

```json
{
  "openApiSpecUrl": "https://example.com/api/openapi.yaml",
  "groupName": "com.acme",
  "artifactId": "order-service",
  "basePackage": "com.acme.order",
  "version": "1.0.0",
  "returnAsArchive": true,
  "githubToken": "ghp_xxxxxxxxxxxxxxxxxxxx",
  "organization": "my-org",
  "repositoryName": "order-service-api",
  "branchName": "main",
  "gitHubConfig": {
    "enabled": true,
    "description": "Order Management Service API",
    "isPrivate": true,
    "commitMessage": "Initial commit - Generated from OpenAPI spec",
    "autoInit": false
  }
}
```

#### Example 4: Private GitHub Repository Spec

```json
{
  "openApiSpecUrl": "https://raw.githubusercontent.com/my-org/api-specs/main/openapi.yaml",
  "githubToken": "ghp_xxxxxxxxxxxxxxxxxxxx",
  "groupName": "com.enterprise",
  "artifactId": "customer-api",
  "basePackage": "com.enterprise.customer"
}
```

### Response Body

#### Success Response

| Field | Type | Description |
|-------|------|-------------|
| `generationId` | String | Unique identifier for this generation (UUID) |
| `projectPath` | String | Absolute path to generated project directory |
| `archivePath` | String | Path to ZIP archive (if `returnAsArchive` was true) |
| `status` | Enum | Generation status: `SUCCESS`, `PARTIAL_SUCCESS`, or `FAILED` |
| `timestamp` | DateTime | When generation completed (ISO 8601 format) |
| `generatedFiles` | Array<String> | List of all generated file paths |
| `messages` | Array<String> | Informational messages about the generation process |
| `errorMessage` | String | Error description (only present if status is FAILED) |
| `gitHubRepositoryInfo` | Object | GitHub repository details (if GitHub integration enabled) |

#### GitHub Repository Info Object

| Field | Type | Description |
|-------|------|-------------|
| `repositoryUrl` | String | GitHub repository web URL |
| `cloneUrl` | String | HTTPS clone URL |
| `sshUrl` | String | SSH clone URL |
| `fullName` | String | Repository full name (org/repo) |
| `commitSha` | String | Initial commit SHA |
| `branchName` | String | Branch name |
| `pushSuccessful` | Boolean | Whether push was successful |

#### Response Example (Success)

```json
{
  "generationId": "a3f7b2c1-8e4d-4f9a-b1c2-3d4e5f6a7b8c",
  "projectPath": "/home/user/generated-projects/a3f7b2c1-8e4d-4f9a-b1c2-3d4e5f6a7b8c/petstore-api",
  "archivePath": "/home/user/generated-projects/a3f7b2c1-8e4d-4f9a-b1c2-3d4e5f6a7b8c/petstore-api.zip",
  "status": "SUCCESS",
  "timestamp": "2025-11-12T14:30:45.123",
  "generatedFiles": [
    "src/main/java/com/example/petstore/api/PetApi.java",
    "src/main/java/com/example/petstore/api/StoreApi.java",
    "src/main/java/com/example/petstore/model/Pet.java",
    "src/main/java/com/example/petstore/model/Order.java",
    "src/main/java/com/example/petstore/service/PetService.java",
    "src/main/java/com/example/petstore/service/impl/PetServiceImpl.java",
    "src/main/java/com/example/petstore/repository/PetRepository.java",
    "pom.xml",
    "README.md"
  ],
  "messages": [
    "Downloaded OpenAPI specification from URL",
    "Generated 12 REST controller methods",
    "Generated 8 model classes",
    "Generated 4 service interfaces",
    "Generated 4 service implementations",
    "Generated 4 MongoDB repositories",
    "Copied 15 template files",
    "Merged POM dependencies and plugins",
    "Merged application properties",
    "Updated README with API documentation",
    "Created ZIP archive"
  ],
  "errorMessage": null,
  "gitHubRepositoryInfo": {
    "repositoryUrl": "https://github.com/my-org/petstore-api",
    "cloneUrl": "https://github.com/my-org/petstore-api.git",
    "sshUrl": "git@github.com:my-org/petstore-api.git",
    "fullName": "my-org/petstore-api",
    "commitSha": "f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6",
    "branchName": "main",
    "pushSuccessful": true
  }
}
```

#### Response Example (Error)

```json
{
  "generationId": null,
  "projectPath": null,
  "archivePath": null,
  "status": "FAILED",
  "timestamp": "2025-11-12T14:35:22.456",
  "generatedFiles": null,
  "messages": null,
  "errorMessage": "Invalid OpenAPI specification: Unable to parse YAML content at line 15",
  "gitHubRepositoryInfo": null
}
```

### HTTP Status Codes

| Status Code | Description | Response Status |
|-------------|-------------|-----------------|
| `200 OK` | Generation successful or partially successful | `SUCCESS` or `PARTIAL_SUCCESS` |
| `400 Bad Request` | Invalid request (validation failed) | `FAILED` |
| `500 Internal Server Error` | Generation failed due to internal error | `FAILED` |

### Error Scenarios

| Error | HTTP Status | Error Message Example |
|-------|-------------|----------------------|
| Missing required field | 400 | `"Group name is required"` |
| Invalid format | 400 | `"Invalid package name format"` |
| Both URL and content provided | 400 | `"Provide either openApiSpecUrl or specContent, not both"` |
| Invalid OpenAPI spec | 400 | `"Invalid OpenAPI specification: ..."` |
| Unable to download spec | 400 | `"Failed to download OpenAPI specification from URL"` |
| GitHub token invalid | 400 | `"Invalid GitHub token or insufficient permissions"` |
| Template processing error | 500 | `"Failed to process templates: ..."` |

---

## Additional Endpoints

### Upload API: `/api/v1/codegen/generate/upload`

**Method:** `POST`
**Content-Type:** `multipart/form-data`
**Controller:** `CodeGenerationController.java:72`

Upload OpenAPI specification as a file instead of URL/content.

#### Request Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `file` | MultipartFile | OpenAPI spec file (YAML or JSON) |
| `request` | String | JSON string with other request parameters |

#### cURL Example

```bash
curl -X POST http://localhost:8080/api/v1/codegen/generate/upload \
  -F "file=@openapi.yaml" \
  -F 'request={"groupName":"com.example","artifactId":"my-api","basePackage":"com.example.myapi"}'
```

---

### Download API: `/api/v1/codegen/download/{generationId}`

**Method:** `GET`
**Controller:** `CodeGenerationController.java:100`

Download the generated project as a ZIP archive.

#### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `generationId` | String | Generation ID from generate response |

#### Response

- **Success (200)**: Returns ZIP file with headers:
  - `Content-Disposition: attachment; filename=<artifact-id>.zip`
  - `Content-Type: application/octet-stream`
- **Not Found (404)**: Generation ID not found
- **Error (500)**: Internal server error

#### cURL Example

```bash
curl -O -J http://localhost:8080/api/v1/codegen/download/a3f7b2c1-8e4d-4f9a-b1c2-3d4e5f6a7b8c
```

---

## Generated Project Structure

The generated Spring Boot project includes:

```
my-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/myservice/
│   │   │       ├── api/              # REST Controllers (from OpenAPI)
│   │   │       ├── model/            # Model/DTO classes (from OpenAPI)
│   │   │       ├── service/          # Service interfaces
│   │   │       │   └── impl/         # Service implementations
│   │   │       ├── repository/       # MongoDB repositories
│   │   │       ├── config/           # Configuration classes (from templates)
│   │   │       ├── exception/        # Exception handlers (from templates)
│   │   │       └── util/             # Utility classes (from templates)
│   │   └── resources/
│   │       ├── application.yml       # Application configuration
│   │       └── application-dev.yml   # Dev profile configuration
│   └── test/
│       └── java/
│           └── com/example/myservice/
├── pom.xml                           # Maven configuration
├── README.md                         # Project documentation
└── Dockerfile                        # Docker configuration (if in templates)
```

### Key Generated Components

1. **REST Controllers** (`api/` package)
   - Generated from OpenAPI paths
   - Spring `@RestController` annotations
   - Request/Response mappings
   - Validation annotations

2. **Model Classes** (`model/` package)
   - Generated from OpenAPI schemas
   - MongoDB `@Document` annotations
   - Jakarta Bean Validation
   - Lombok annotations

3. **Service Layer** (`service/` package)
   - Service interfaces for business logic
   - Implementation classes with MongoDB integration
   - `@Service` annotated
   - Repository injection

4. **Repositories** (`repository/` package)
   - Extends `MongoRepository<Entity, ID>`
   - Spring Data MongoDB integration
   - Custom query methods

## cURL Examples

### Example 1: Generate from Public OpenAPI Spec

```bash
curl -X POST http://localhost:8080/api/v1/codegen/generate \
  -H "Content-Type: application/json" \
  -d '{
    "openApiSpecUrl": "https://petstore3.swagger.io/api/v3/openapi.json",
    "groupName": "com.example",
    "artifactId": "petstore-api",
    "basePackage": "com.example.petstore",
    "version": "1.0.0"
  }'
```

### Example 2: Generate with Archive

```bash
curl -X POST http://localhost:8080/api/v1/codegen/generate \
  -H "Content-Type: application/json" \
  -d '{
    "openApiSpecUrl": "https://api.example.com/openapi.yaml",
    "groupName": "com.mycompany",
    "artifactId": "order-service",
    "basePackage": "com.mycompany.order",
    "version": "2.0.0",
    "returnAsArchive": true
  }' | jq
```

### Example 3: Generate with GitHub Integration

```bash
curl -X POST http://localhost:8080/api/v1/codegen/generate \
  -H "Content-Type: application/json" \
  -d '{
    "openApiSpecUrl": "https://example.com/api/openapi.yaml",
    "groupName": "com.enterprise",
    "artifactId": "payment-api",
    "basePackage": "com.enterprise.payment",
    "githubToken": "ghp_xxxxxxxxxxxxxxxxxxxx",
    "organization": "my-org",
    "gitHubConfig": {
      "enabled": true,
      "description": "Payment Service API",
      "isPrivate": true,
      "commitMessage": "Initial commit from code generator"
    }
  }' | jq
```

### Example 4: Upload OpenAPI File

```bash
curl -X POST http://localhost:8080/api/v1/codegen/generate/upload \
  -F "file=@/path/to/openapi.yaml" \
  -F 'request={
    "groupName": "com.example",
    "artifactId": "user-api",
    "basePackage": "com.example.user",
    "version": "1.0.0",
    "returnAsArchive": true
  }'
```

### Example 5: Download Generated Project

```bash
# First, generate the project and capture the generationId
GENERATION_ID=$(curl -X POST http://localhost:8080/api/v1/codegen/generate \
  -H "Content-Type: application/json" \
  -d '{
    "openApiSpecUrl": "https://petstore3.swagger.io/api/v3/openapi.json",
    "groupName": "com.example",
    "artifactId": "petstore-api",
    "basePackage": "com.example.petstore"
  }' | jq -r '.generationId')

# Then download the project
curl -O -J http://localhost:8080/api/v1/codegen/download/$GENERATION_ID
```

---

## Configuration

### Application Properties

```yaml
server:
  port: 8080

spring:
  application:
    name: probe-stack-api-code-generator

probe:
  stack:
    generator:
      # -----------------------------------------------------------------------
      # Directory Configuration
      # -----------------------------------------------------------------------
      directories:
        # Base directory for generated projects
        output-base: ./generated-projects
        # Template configuration directory structure
        template-config: ./codegen_config
        # Temporary directory for downloads
        temp: ./temp

      # -----------------------------------------------------------------------
      # Cleanup Configuration
      # -----------------------------------------------------------------------
      cleanup:
        # Cleanup generated files after specified hours (0 = no cleanup)
        hours: 24
        # Cron expression for cleanup scheduler (default: every hour at minute 0)
        cron: "0 0 * * * *"

      # -----------------------------------------------------------------------
      # Template Configuration
      # -----------------------------------------------------------------------
      templates:
        java-source-dir: java_code/src/main/java
        resources-dir: java_code/src/main/resources
        pom-template: maven_config

      # -----------------------------------------------------------------------
      # OpenAPI Generator Configuration
      # -----------------------------------------------------------------------
      openapi:
        generator:
          language: spring
          library: spring-boot
          api-package-suffix: api
          model-package-suffix: model

      # -----------------------------------------------------------------------
      # HTTP Client Configuration
      # -----------------------------------------------------------------------
      http:
        connect-timeout-ms: 30000
        read-timeout-ms: 30000
        user-agent: OpenAPI-Code-Generator/1.0

      # -----------------------------------------------------------------------
      # File Configuration
      # -----------------------------------------------------------------------
      files:
        default-archive-name: generated-project.zip
        text-file-extensions:
          - .java
          - .xml
          - .properties
          - .yml
          - .yaml
          - .md

      # -----------------------------------------------------------------------
      # GitHub Integration Configuration
      # -----------------------------------------------------------------------
      github:
        version: 1.0.0
        description: Generated Spring Boot application from OpenAPI specification
        push:
          enabled: true
        config:
          is-private: true
          commit:
            message: "Initial Commit: Generated {app_name}"
        personal:
          access-token: "${github_personal_access_token}"

      # -----------------------------------------------------------------------
      # Default POM Configuration (Fallback)
      # -----------------------------------------------------------------------
      pom:
        properties:
          maven.compiler.source: 17
          maven.compiler.target: 17
          springdoc-openapi.version: 2.3.0
        dependencies:
          - groupId: org.springdoc
            artifactId: springdoc-openapi-starter-webmvc-ui
            version: ${springdoc-openapi.version}
          - groupId: org.springframework.boot
            artifactId: spring-boot-starter-validation

      # -----------------------------------------------------------------------
      # Default Application Properties (Fallback)
      # -----------------------------------------------------------------------
      application-properties:
        server.port: 8080
        spring.application.name: generated-service
        springdoc.api-docs.path: /api-docs
        springdoc.swagger-ui.path: /swagger-ui.html
```

### Environment Variables

The application supports the following environment variables to override default configuration:

| Environment Variable | Description | Default Value | Configuration Property |
|---------------------|-------------|---------------|----------------------|
| `CODEGEN_OUTPUT_DIR` | Base directory for generated projects | `./generated-projects` | `probe.stack.generator.directories.output-base` |
| `CODEGEN_TEMPLATE_DIR` | Template configuration directory | `./codegen_config` | `probe.stack.generator.directories.template-config` |
| `CODEGEN_TEMP_DIR` | Temporary directory for downloads | `./temp` | `probe.stack.generator.directories.temp` |
| `CODEGEN_CLEANUP_HOURS` | Auto-cleanup after N hours (0 = disabled) | `24` | `probe.stack.generator.cleanup.hours` |
| `github_personal_access_token` | GitHub Personal Access Token for private repositories | - | `probe.stack.generator.github.personal.access-token` |

**Example usage:**

```bash
# Set environment variables
export CODEGEN_OUTPUT_DIR=/var/app/generated
export CODEGEN_TEMPLATE_DIR=/var/app/templates
export CODEGEN_TEMP_DIR=/tmp/codegen
export CODEGEN_CLEANUP_HOURS=48
export github_personal_access_token=ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Run the application
mvn spring-boot:run
```

**Docker usage:**

```bash
docker run -p 8080:8080 \
  -e CODEGEN_OUTPUT_DIR=/app/output \
  -e CODEGEN_TEMPLATE_DIR=/app/config \
  -e github_personal_access_token=ghp_xxxxx \
  -v $(pwd)/config:/app/config \
  -v $(pwd)/output:/app/output \
  openapi-code-generator
```

## Template Directory Structure

Place custom templates and files in the `directories.template-config` directory:

```
codegen_config/
├── java_code/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── ${basePackagePath}/
│           │       ├── config/
│           │       │   └── CustomConfig.java
│           │       ├── exception/
│           │       │   └── GlobalExceptionHandler.java
│           │       └── util/
│           │           └── UtilityClass.java
│           └── resources/
│               ├── application-dev.properties
│               └── logback-spring.xml
├── maven_config/
│   └── pom.xml
├── Dockerfile
└── README.md
```

### Placeholder Variables

Use these placeholders in your template files:

- `${basePackage}` - Base package (e.g., com.example.myservice)
- `${groupId}` - Maven group ID
- `${artifactId}` - Maven artifact ID
- `${version}` - Project version
- `${projectName}` - Title-cased artifact ID
- `${apiPackage}` - API package (basePackage.api)
- `${modelPackage}` - Model package (basePackage.model)
- `${servicePackage}` - Service package (basePackage.service)
- `${configPackage}` - Config package (basePackage.config)
- `${basePackagePath}` - Package path with slashes

## Docker Support

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Create directories
RUN mkdir -p /app/generated-projects /app/templates /app/temp

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Build and Run

```bash
# Build image
docker build -t openapi-code-generator .

# Run container
docker run -p 8080:8080 \
  -e CODEGEN_TEMPLATE_DIR=/app/config \
  -e CODEGEN_OUTPUT_DIR=/app/output \
  -v $(pwd)/codegen_config:/app/config \
  -v $(pwd)/generated-projects:/app/output \
  openapi-code-generator
```

## Code Generator Service Structure

```
probe-stack-api-code-generator/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/probe/stack/code/generator/
│   │   │       ├── component/
│   │   │       │   ├── CodeGenerationOrchestrator.java
│   │   │       │   └── ExistingControllerEnhancer.java
│   │   │       ├── config/
│   │   │       │   ├── CodeGeneratorConfig.java
│   │   │       │   └── GitHubConfig.java
│   │   │       ├── controller/
│   │   │       │   └── CodeGenerationController.java
│   │   │       ├── dto/
│   │   │       │   ├── CodeGenerationRequest.java
│   │   │       │   └── CodeGenerationResponse.java
│   │   │       ├── exception/
│   │   │       │   ├── CodeGenerationException.java
│   │   │       │   └── GlobalExceptionHandler.java
│   │   │       ├── parser/
│   │   │       │   └── ControllerMetadataExtractor.java
│   │   │       ├── scheduler/
│   │   │       │   └── CleanupScheduler.java
│   │   │       ├── service/
│   │   │       │   ├── CodeGenerationService.java
│   │   │       │   ├── DownloadService.java
│   │   │       │   ├── FileOperationsService.java
│   │   │       │   ├── GitHubService.java
│   │   │       │   ├── MultipartRequestService.java
│   │   │       │   ├── OpenApiGeneratorService.java
│   │   │       │   ├── PomCustomizationService.java
│   │   │       │   ├── PomMergeService.java
│   │   │       │   ├── PropertiesMergeService.java
│   │   │       │   ├── RequestValidationService.java
│   │   │       │   ├── SpecificationDownloadService.java
│   │   │       │   ├── TemplateEnhancementService.java
│   │   │       │   └── TemplateProcessingService.java
│   │   │       ├── util/
│   │   │       │   ├── AppConstants.java
│   │   │       │   └── ControllerPathScanner.java
│   │   │       └── ProbeStackApiCodeGeneratorApplication.java
│   │   └── resources/
│   │       └── application.yaml
│   └── test/
│       └── java/
│           └── com/probe/stack/code/generator/
│               └── (test files)
├── templates/
├── generated-projects/
├── pom.xml
└── README.md
```

## Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test

```bash
mvn test -Dtest=CodeGenerationServiceTest
```

### Test Coverage

```bash
mvn jacoco:report
```

Report available at: `target/site/jacoco/index.html`

---

## Troubleshooting

### Common Issues

**Issue:** Generated code compilation errors

**Solution:** Ensure your OpenAPI spec is valid. Use online validators like [Swagger Editor](https://editor.swagger.io/)

---

**Issue:** Template files not being copied

**Solution:** Check that `probe.stack.generator.directories.template-config` exists and contains files. Verify directory permissions and ensure the directory structure follows the expected layout (java_code/, maven_config/, etc.).

---

**Issue:** Out of memory errors

**Solution:** Increase JVM heap size:
```bash
java -Xmx2g -jar openapi-code-generator.jar
```

---

**Issue:** GitHub private repo access denied

**Solution:** Ensure GitHub token has `repo` scope and is valid.

## Best Practices

1. **OpenAPI Specs**: Use OpenAPI 3.0+ specifications for best compatibility
2. **Templates**: Keep templates generic and use placeholders extensively
3. **Cleanup**: Configure appropriate `probe.stack.generator.cleanup.hours` based on your usage patterns
4. **Testing**: Always test generated code before deploying to production
5. **Version Control**: Keep generated projects in version control
6. **Monitoring**: Monitor disk space in `probe.stack.generator.directories.output-base`
7. **Environment Variables**: Use environment variables for sensitive data like GitHub tokens
8. **HTTP Timeouts**: Adjust `probe.stack.generator.http` settings for large OpenAPI specs

## Advanced Configuration

### Custom Dependencies

Add custom Maven dependencies in `application.yaml`:

```yaml
probe:
  stack:
    generator:
      pom:
        dependencies:
          - groupId: io.jsonwebtoken
            artifactId: jjwt-api
            version: 0.11.5
          - groupId: org.mapstruct
            artifactId: mapstruct
            version: 1.5.5.Final
```

### Custom Properties

```yaml
probe:
  stack:
    generator:
      application-properties:
        spring.datasource.url: jdbc:postgresql://localhost:5432/mydb
        spring.jpa.hibernate.ddl-auto: validate
        logging.level.root: INFO
```

### HTTP Client Configuration

Configure HTTP timeouts and user agent for OpenAPI spec downloads:

```yaml
probe:
  stack:
    generator:
      http:
        connect-timeout-ms: 60000
        read-timeout-ms: 60000
        user-agent: Custom-Agent/2.0
```

### GitHub Integration

Configure GitHub integration for automatic repository creation and push:

```yaml
probe:
  stack:
    generator:
      github:
        version: 1.0.0
        description: Custom generated application
        push:
          enabled: true
        config:
          is-private: false
          commit:
            message: "Initial commit for {app_name}"
        personal:
          access-token: "${github_personal_access_token}"
```

### Cleanup Configuration

Configure automatic cleanup of old generated projects:

```yaml
probe:
  stack:
    generator:
      cleanup:
        # Cleanup files older than 48 hours
        hours: 48
        # Run cleanup every day at 2 AM
        cron: "0 0 2 * * *"
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For issues and questions:
- GitHub Issues: [Project Issues](https://github.com/your-org/openapi-code-generator/issues)
- Email: support@example.com

## Changelog

### Version 1.0.0
- Initial release
- OpenAPI 3.0 support
- Spring Boot 3.2 generation
- Template processing
- POM and properties merging
- Automatic cleanup