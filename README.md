# OpenAPI Code Generator Service

A Spring Boot microservice that automates the generation of Spring Boot application code from OpenAPI specifications.

## Features

- **OpenAPI Code Generation**: Generates Spring Boot projects from OpenAPI 3.0 specifications
- **Customizable Templates**: Copy additional Java code and configuration files from templates
- **POM Merging**: Automatically merges Maven dependencies, properties, and plugins
- **Properties Merging**: Merges application properties into generated projects
- **Placeholder Replacement**: Updates placeholders in generated files (base packages, artifact IDs, etc.)
- **Archive Support**: Option to return generated projects as ZIP archives
- **Automatic Cleanup**: Scheduled cleanup of old generated projects
- **GitHub Integration**: Supports private repositories with GitHub tokens

## Prerequisites

- Java 17 or higher
- Maven 3.6+ 
- 2GB RAM minimum

## Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd openapi-code-generator
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

## API Usage

### Generate Code

**Endpoint:** `POST /api/v1/codegen/generate`

**Request Body:**
```json
{
  "openApiSpecUrl": "https://example.com/api/openapi.yaml",
  "groupName": "com.example",
  "artifactId": "my-service",
  "basePackage": "com.example.myservice",
  "githubToken": "ghp_xxxxx",
  "version": "1.0.0",
  "returnAsArchive": false
}
```

**Response:**
```json
{
  "generationId": "550e8400-e29b-41d4-a716-446655440000",
  "projectPath": "/path/to/generated-projects/550e8400.../my-service",
  "archivePath": null,
  "status": "SUCCESS",
  "timestamp": "2025-10-20T10:30:00",
  "generatedFiles": ["file1.java", "file2.java", "..."],
  "messages": [
    "Downloaded OpenAPI specification",
    "Generated 45 files from OpenAPI spec",
    "Merged POM configuration",
    "Replaced placeholders in 42 files"
  ]
}
```

### Download Generated Project

**Endpoint:** `GET /api/v1/codegen/download/{generationId}`

Returns the generated project as a ZIP file.

### Health Check

**Endpoint:** `GET /api/v1/codegen/health`

**Response:**
```json
{
  "status": "UP",
  "message": "Code Generation Service is running"
}
```

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

## Project Structure

```
openapi-code-generator/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/codegen/openapi/
│   │   │       ├── config/
│   │   │       │   └── CodeGeneratorConfig.java
│   │   │       ├── controller/
│   │   │       │   └── CodeGenerationController.java
│   │   │       ├── dto/
│   │   │       │   ├── CodeGenerationRequest.java
│   │   │       │   └── CodeGenerationResponse.java
│   │   │       ├── exception/
│   │   │       │   ├── CodeGenerationException.java
│   │   │       │   └── GlobalExceptionHandler.java
│   │   │       ├── scheduler/
│   │   │       │   └── CleanupScheduler.java
│   │   │       ├── service/
│   │   │       │   ├── CodeGenerationService.java
│   │   │       │   ├── FileOperationsService.java
│   │   │       │   ├── OpenApiGeneratorService.java
│   │   │       │   ├── PomMergeService.java
│   │   │       │   ├── PropertiesMergeService.java
│   │   │       │   ├── SpecificationDownloadService.java
│   │   │       │   └── TemplateProcessingService.java
│   │   │       └── CodeGeneratorApplication.java
│   │   └── resources/
│   │       └── application.yaml
│   └── test/
│       └── java/
│           └── com/codegen/openapi/
│               ├── controller/
│               │   └── CodeGenerationControllerTest.java
│               └── service/
│                   ├── CodeGenerationServiceTest.java
│                   ├── FileOperationsServiceTest.java
│                   └── TemplateProcessingServiceTest.java
├── templates/
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

## Example Usage with cURL

```bash
# Generate code from public OpenAPI spec
curl -X POST http://localhost:8080/api/v1/codegen/generate \
  -H "Content-Type: application/json" \
  -d '{
    "openApiSpecUrl": "https://petstore3.swagger.io/api/v3/openapi.json",
    "groupName": "com.example",
    "artifactId": "petstore-api",
    "basePackage": "com.example.petstore",
    "version": "1.0.0",
    "returnAsArchive": true
  }'

# Download generated project
curl -O -J http://localhost:8080/api/v1/codegen/download/{generationId}
```

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