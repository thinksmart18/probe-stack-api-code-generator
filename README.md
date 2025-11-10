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

Create or edit `application.yml`:

```yaml
codegen:
  output-base-dir: ./generated-projects
  source-template-dir: ./templates
  temp-dir: ./temp
  cleanup-hours: 24
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

codegen:
  # Base directory for generated projects
  output-base-dir: ./generated-projects
  
  # Template directory with additional files to copy
  source-template-dir: ./templates
  
  # Temporary directory for downloads
  temp-dir: ./temp
  
  # Auto-cleanup after N hours (0 = disabled)
  cleanup-hours: 24
  
  # OpenAPI Generator settings
  generator:
    language: spring
    library: spring-boot
    api-package-suffix: api
    model-package-suffix: model
  
  # Additional POM properties to merge
  pom:
    properties:
      maven.compiler.source: 17
      maven.compiler.target: 17
      springdoc-openapi.version: 2.3.0
    
    dependencies:
      - groupId: org.springdoc
        artifactId: springdoc-openapi-starter-webmvc-ui
        version: ${springdoc-openapi.version}
  
  # Additional application properties to merge
  application-properties:
    server.port: 8080
    spring.application.name: generated-service
    springdoc.api-docs.path: /api-docs
```

## Template Directory Structure

Place custom templates and files in the `source-template-dir`:

```
templates/
├── src/
│   └── main/
│       ├── java/
│       │   └── ${basePackagePath}/
│       │       ├── config/
│       │       │   └── CustomConfig.java
│       │       ├── exception/
│       │       │   └── GlobalExceptionHandler.java
│       │       └── util/
│       │           └── UtilityClass.java
│       └── resources/
│           ├── application-dev.properties
│           └── logback-spring.xml
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
  -v $(pwd)/templates:/app/templates \
  -v $(pwd)/generated-projects:/app/generated-projects \
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
│   │       └── application.yml
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

**Solution:** Check that `source-template-dir` exists and contains files. Verify directory permissions.

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
3. **Cleanup**: Configure appropriate `cleanup-hours` based on your usage patterns
4. **Testing**: Always test generated code before deploying to production
5. **Version Control**: Keep generated projects in version control
6. **Monitoring**: Monitor disk space in `output-base-dir`

## Advanced Configuration

### Custom Dependencies

Add custom Maven dependencies in `application.yml`:

```yaml
codegen:
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
codegen:
  application-properties:
    spring.datasource.url: jdbc:postgresql://localhost:5432/mydb
    spring.jpa.hibernate.ddl-auto: validate
    logging.level.root: INFO
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