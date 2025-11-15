# Microservice Enhancement System

## Overview

This document describes the comprehensive microservice enhancement system that automatically scans and enhances generated Spring Boot microservices with AI tooling, business logic, and production-grade code quality improvements.

## Features

### 1. AI Tooling Integration

The system automatically integrates advanced AI capabilities into generated microservices:

#### Embabel (Spring AI Extension)
- **Location**: `src/main/java/{basePackage}/config/EmbabelConfiguration.java`
- **Purpose**: Enhanced AI capabilities on top of Spring AI
- **Features**:
  - Context-aware AI interactions
  - Multi-model support
  - Enhanced prompt engineering

#### Agent Communication Protocol (ACPJava)
- **Location**: `src/main/java/{basePackage}/config/ACPConfiguration.java`
- **Purpose**: Enable agent-to-agent communication
- **Features**:
  - Inter-service AI agent communication
  - Protocol-based message exchange
  - Distributed AI capabilities

#### Spring AI Core
- **Location**: `src/main/java/{basePackage}/config/AIConfiguration.java`
- **Purpose**: Core AI functionality
- **Configuration**:
  ```properties
  spring.ai.openai.api-key=${OPENAI_API_KEY}
  spring.ai.openai.model=gpt-4
  spring.ai.openai.temperature=0.7
  ```

### 2. Swagger Analysis and Validation

The `SwaggerAnalysisService` performs comprehensive analysis of OpenAPI/Swagger specifications:

- **Locates Swagger files** in `src/main/resources/`
- **Parses paths and operations** (GET, POST, PUT, DELETE, PATCH)
- **Extracts schema definitions** for model generation
- **Identifies missing implementations**
- **Caches parsed specs** for reuse by other services

**Supported file names**:
- `openapi.yaml` / `openapi.yml`
- `swagger.yaml` / `swagger.yml`
- `api.yaml` / `api.yml`

### 3. Missing Class Generation

The `MissingClassGeneratorService` generates any missing classes based on Swagger analysis:

#### Model Classes
- Generated in `{basePackage}.model` package
- Based on Swagger schema definitions
- Includes getters and setters
- Proper Javadoc comments

#### Repository Interfaces
- Generated in `{basePackage}.repository` package
- Extends `MongoRepository<Model, String>`
- One repository per model
- Ready for MongoDB integration

#### Service Classes
- Generated in `{basePackage}.service` package
- Autowired repository dependencies
- Constructor injection pattern
- Production-ready structure

### 4. Business Logic Enhancement

The `BusinessLogicEnhancerService` implements comprehensive CRUD operations:

#### Create Operation
```java
public Entity create(Entity entity) {
    log.info("Creating new Entity: {}", entity);
    return repository.save(entity);
}
```

#### Read Operations
```java
public Optional<Entity> findById(String id) {
    log.debug("Finding Entity by ID: {}", id);
    return repository.findById(id);
}

public List<Entity> findAll() {
    log.debug("Finding all Entity entities");
    return repository.findAll();
}
```

#### Update Operation
```java
public Entity update(String id, Entity entity) {
    log.info("Updating Entity with ID: {}", id);
    return repository.findById(id).map(existing -> {
        log.debug("Found existing Entity, updating...");
        return repository.save(entity);
    }).orElseGet(() -> {
        log.info("Entity not found, creating new one");
        return repository.save(entity);
    });
}
```

#### Delete Operation
```java
public void delete(String id) {
    log.info("Deleting Entity with ID: {}", id);
    repository.deleteById(id);
}
```

### 5. Code Quality Enhancement

The `CodeQualityEnhancerService` ensures production-grade code quality:

#### Logging
- Adds `@Slf4j` annotation to all classes
- Implements comprehensive logging throughout
- Includes debug, info, warn, and error levels
- Logs method entry/exit for critical operations

#### Exception Handling
Creates a global exception handler (`GlobalExceptionHandler`) with:
- Generic exception handling
- Specific handlers for common exceptions
- Structured error responses
- HTTP status code mapping
- Custom `ResourceNotFoundException`

Example error response:
```json
{
  "timestamp": "2025-11-15T10:30:00",
  "message": "User not found with id: 123",
  "error": "Not Found",
  "status": 404
}
```

#### Documentation
- Class-level Javadoc for all classes
- Method-level Javadoc for all public methods
- Parameter documentation
- Return value documentation
- Usage examples where applicable

### 6. Compile Error Detection and Fixing

The `CompileErrorDetectionService` detects and fixes compilation errors:

#### Detection
- Runs `mvn compile` on the project
- Parses Maven output for errors
- Extracts file path, line number, and error message
- Categorizes errors by type

#### Automatic Fixes
- Missing imports
- Missing package declarations
- Incompatible types
- Override annotation issues

#### Verification
- Re-compiles after fixes
- Reports remaining errors
- Provides fix summaries

## Integration with Code Generation

The enhancement system is integrated into the main code generation flow:

```java
// In CodeGenerationService.generateProject()
1. Download OpenAPI specification
2. Generate code using OpenAPI Generator
3. Enhance project with templates
4. Generate Service and Repository classes
5. *** PERFORM MICROSERVICE ENHANCEMENT ***
6. Update .openapi-generator-ignore
7. Update README
8. Create archive (if requested)
9. Push to GitHub (if enabled)
```

## Usage

### Automatic Enhancement

Enhancement is automatically triggered after code generation:

```java
POST /api/v1/codegen/generate
{
  "openApiSpecUrl": "https://example.com/api/openapi.yaml",
  "groupName": "com.example",
  "artifactId": "my-service",
  "basePackage": "com.example.myservice",
  "version": "1.0.0"
}
```

The response will include enhancement messages:
```json
{
  "generationId": "uuid",
  "status": "SUCCESS",
  "messages": [
    "Generated 45 files from OpenAPI spec",
    "Found Swagger specification: openapi.yaml",
    "Parsed Swagger: 10 paths, 5 schemas",
    "Generated 5 model classes",
    "Generated 5 repository interfaces",
    "Generated 5 service classes",
    "Enhanced business logic in 5 service classes",
    "Enhanced 15 controller classes",
    "Enhanced 5 service classes",
    "Enhanced 5 repository interfaces",
    "Enhanced 5 model classes",
    "Created GlobalExceptionHandler.java",
    "No compile errors detected - project compiles successfully"
  ]
}
```

### Critical Enhancements Only

For faster generation, use critical enhancements only:

```java
microserviceEnhancementService.performCriticalEnhancements(projectDir, request);
```

This performs only:
- Compile error fixes
- Missing class generation from Swagger
- Basic validation

## Generated Project Structure

After enhancement, the generated project will have:

```
my-service/
├── src/main/java/com/example/myservice/
│   ├── api/                         # Controllers (OpenAPI generated)
│   │   └── UserApiController.java
│   ├── model/                       # Models (enhanced)
│   │   └── User.java
│   ├── service/                     # Services (enhanced with CRUD)
│   │   └── UserService.java
│   ├── repository/                  # Repositories (generated)
│   │   └── UserRepository.java
│   ├── config/                      # Configurations (AI + general)
│   │   ├── AIConfiguration.java
│   │   ├── EmbabelConfiguration.java
│   │   └── ACPConfiguration.java
│   └── exception/                   # Exception handling
│       ├── GlobalExceptionHandler.java
│       └── ResourceNotFoundException.java
├── src/main/resources/
│   ├── openapi.yaml                # Original spec
│   └── application.properties      # Enhanced with AI settings
└── pom.xml                         # Enhanced with AI dependencies
```

## Dependencies Added

The enhancement system adds the following dependencies:

```xml
<!-- Spring AI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-core</artifactId>
    <version>1.0.0-M3</version>
</dependency>

<!-- Spring AI OpenAI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai</artifactId>
    <version>1.0.0-M3</version>
</dependency>

<!-- Embabel -->
<dependency>
    <groupId>io.embabel</groupId>
    <artifactId>embabel-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>

<!-- ACP Java -->
<dependency>
    <groupId>io.acp</groupId>
    <artifactId>acp-java-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Configuration

### AI Configuration

Set the following environment variables or application properties:

```properties
# OpenAI API Key (required for AI features)
OPENAI_API_KEY=your-api-key-here

# Or in application.properties
spring.ai.openai.api-key=${OPENAI_API_KEY:your-api-key-here}
spring.ai.openai.model=gpt-4
spring.ai.openai.temperature=0.7

# Embabel
embabel.enabled=true
embabel.context-window=4096

# ACP
acp.enabled=true
acp.agent-id=${spring.application.name}
```

## Best Practices

The enhancement system enforces these production-grade best practices:

1. **Logging**: Every operation is logged for observability
2. **Exception Handling**: Centralized error handling with meaningful responses
3. **Documentation**: Comprehensive Javadoc on all public APIs
4. **Dependency Injection**: Constructor-based injection throughout
5. **Separation of Concerns**: Clear separation of controllers, services, repositories
6. **Database Patterns**: Update-or-insert pattern for entity persistence
7. **Error Messages**: User-friendly, actionable error messages
8. **Monitoring Ready**: Structured logging for APM integration

## Troubleshooting

### Enhancement Fails

If enhancement fails, check the logs for:
```
Error during microservice enhancement
```

Common issues:
- Swagger file not found in resources
- Invalid Swagger/OpenAPI syntax
- Network issues during dependency download

### Compile Errors After Enhancement

Run compile error detection manually:
```bash
cd generated-projects/{uuid}/{service-name}
mvn clean compile
```

The enhancement system will attempt to fix common errors automatically.

## Future Enhancements

Planned improvements:
- Support for JPA/Hibernate repositories
- GraphQL API generation
- Kafka/messaging integration
- OAuth2/JWT security setup
- Docker and Kubernetes manifests
- CI/CD pipeline generation
- Integration test generation

## Architecture

```
MicroserviceEnhancementService (Orchestrator)
├── SwaggerAnalysisService
│   └── Parses OpenAPI specs
├── AIToolingIntegrationService
│   ├── Adds AI dependencies
│   └── Creates AI configuration
├── MissingClassGeneratorService
│   ├── Generates models
│   ├── Generates repositories
│   └── Generates services
├── BusinessLogicEnhancerService
│   └── Implements CRUD operations
├── CodeQualityEnhancerService
│   ├── Adds logging
│   ├── Adds exception handling
│   └── Adds documentation
└── CompileErrorDetectionService
    ├── Detects errors
    └── Applies fixes
```

## Support

For issues or questions about the enhancement system, please contact the ProbeStack development team.
