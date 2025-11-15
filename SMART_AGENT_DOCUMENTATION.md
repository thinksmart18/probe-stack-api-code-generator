# Smart Agent Functionality - Documentation

## Overview

The Smart Agent functionality enhances the Probe Stack API Code Generator with intelligent request tracking, persistence, and lifecycle management. All code generation requests are automatically tracked in MongoDB, enabling features like request history, duplicate detection, retry mechanisms, and comprehensive audit trails.

## Architecture

### Components

1. **CodeGenerationRequestEntity** - MongoDB document representing a code generation request
2. **CodeGenerationRequestRepository** - Spring Data MongoDB repository for data access
3. **SmartAgentService** - Business logic for request persistence and lifecycle management
4. **SmartAgentController** - REST endpoints for querying and managing requests
5. **Enhanced CodeGenerationService** - Integrated with Smart Agent for automatic tracking

### Data Flow

```
Request → Controller → CodeGenerationService
                          ↓
                    SmartAgentService.saveOrUpdateRequest()
                          ↓
                    MongoDB (Request Persisted)
                          ↓
                    Code Generation Process
                          ↓
                    SmartAgentService.updateWithResponse()
                          ↓
                    MongoDB (Results Stored)
```

## Features

### 1. Automatic Request Tracking

Every code generation request is automatically persisted to MongoDB with:
- Unique generation ID
- Request parameters (artifactId, groupName, basePackage, etc.)
- Timestamps (created, updated, completed)
- Status tracking (PENDING → SUCCESS/FAILED)
- Audit trail information

### 2. Upsert Capability

The Smart Agent implements intelligent upsert logic:

```java
// If request exists → Update existing record
Optional<CodeGenerationRequestEntity> existing = repository.findByGenerationId(generationId);
if (existing.isPresent()) {
    entity = existing.get();
    updateEntityFromRequest(entity, request);
    entity.incrementRetryCount();
}
// If request doesn't exist → Create new record
else {
    entity = createEntityFromRequest(request, generationId);
}
```

This enables:
- Request reprocessing
- Tracking retry attempts
- Maintaining history of updates

### 3. Comprehensive Logging

All operations include detailed logging:
- Request persistence (success/failure)
- Database operations
- Error conditions with stack traces
- Audit trail events

Example log entries:
```
Smart Agent: Processing request for generationId: abc-123, artifactId: my-service
Smart Agent: Found existing request with ID: 507f1f77bcf86cd799439011. Updating record.
Smart Agent: Successfully persisted request. Document ID: 507f1f77bcf86cd799439011
```

### 4. Robust Exception Handling

Smart Agent service handles multiple exception scenarios:

```java
try {
    // Database operation
} catch (DataAccessException e) {
    log.error("Smart Agent: Database error...", e);
    throw new CodeGenerationException("Failed to persist request: " + e.getMessage(), e);
} catch (Exception e) {
    log.error("Smart Agent: Unexpected error...", e);
    throw new CodeGenerationException("Unexpected error: " + e.getMessage(), e);
}
```

Exception types:
- `DataAccessException` - MongoDB connectivity/query errors
- `CodeGenerationException` - Business logic errors
- Generic exceptions - Unexpected errors

### 5. Request Lifecycle Management

Requests progress through well-defined states:

| Status | Description |
|--------|-------------|
| PENDING | Request created, generation not started |
| SUCCESS | Code generation completed successfully |
| PARTIAL_SUCCESS | Generation completed with warnings |
| FAILED | Code generation failed |

### 6. Soft Delete (Archive)

Requests can be archived instead of deleted:
- Archived requests are excluded from active queries
- Retained for audit and compliance purposes
- Can be retrieved for historical analysis

## Database Schema

### Collection: `code_generation_requests`

```javascript
{
    _id: ObjectId("..."),
    generationId: "unique-uuid",           // Unique, indexed
    artifactId: "my-service",              // Indexed
    groupName: "com.example",
    basePackage: "com.example.myservice",
    version: "1.0.0",
    openApiSpecUrl: "https://...",
    specContentType: "yaml",
    organization: "my-org",
    repositoryName: "my-repo",
    branchName: "main",
    status: "SUCCESS",
    projectPath: "/path/to/project",
    archivePath: "/path/to/archive.zip",
    githubRepositoryUrl: "https://github.com/...",
    githubCloneUrl: "https://github.com/.../clone",
    githubCommitSha: "abc123...",
    githubPushSuccessful: true,
    generatedFiles: ["file1.java", "file2.java"],
    messages: ["Info message 1", "Warning message 2"],
    errorMessage: null,
    createdAt: ISODate("2024-01-15T10:00:00Z"),
    updatedAt: ISODate("2024-01-15T10:05:00Z"),
    completedAt: ISODate("2024-01-15T10:05:00Z"),
    retryCount: 0,
    archived: false,
    requestedBy: "user@example.com"
}
```

### Indexes

- `generationId` (unique)
- `artifactId`
- `status`
- `organization`
- Compound index on `createdAt` + `archived` (for cleanup queries)

## API Endpoints

### Smart Agent Controller

#### 1. Get Request by Generation ID

```http
GET /api/v1/smart-agent/requests/{generationId}
```

**Response (200 OK):**
```json
{
    "id": "507f1f77bcf86cd799439011",
    "generationId": "abc-123-def-456",
    "artifactId": "my-service",
    "status": "SUCCESS",
    "createdAt": "2024-01-15T10:00:00",
    "completedAt": "2024-01-15T10:05:00",
    ...
}
```

**Response (404 Not Found):** Request not found

#### 2. Archive Request

```http
PUT /api/v1/smart-agent/requests/{generationId}/archive
```

**Response (200 OK):**
```json
{
    "success": true,
    "message": "Request archived successfully"
}
```

#### 3. Health Check

```http
GET /api/v1/smart-agent/health
```

**Response (200 OK):**
```json
{
    "status": "UP",
    "message": "Smart Agent service is running"
}
```

## Configuration

### MongoDB Configuration

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/probe-stack-generator}
      database: ${MONGODB_DATABASE:probe-stack-generator}
      auto-index-creation: true
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| MONGODB_URI | MongoDB connection URI | mongodb://localhost:27017/probe-stack-generator |
| MONGODB_DATABASE | Database name | probe-stack-generator |

## Usage Examples

### Example 1: Generate Code with Automatic Tracking

```bash
curl -X POST http://localhost:8080/api/v1/codegen/generate \
  -H "Content-Type: application/json" \
  -d '{
    "artifactId": "my-service",
    "groupName": "com.example",
    "basePackage": "com.example.myservice",
    "openApiSpecUrl": "https://example.com/api/openapi.yaml"
  }'
```

Response includes `generationId` which can be used to query request status.

### Example 2: Check Request Status

```bash
curl http://localhost:8080/api/v1/smart-agent/requests/abc-123-def-456
```

### Example 3: Archive Old Request

```bash
curl -X PUT http://localhost:8080/api/v1/smart-agent/requests/abc-123-def-456/archive
```

## Repository Query Methods

The `CodeGenerationRequestRepository` provides rich query capabilities:

```java
// Find by generation ID
Optional<CodeGenerationRequestEntity> findByGenerationId(String generationId);

// Find by artifact ID
List<CodeGenerationRequestEntity> findByArtifactId(String artifactId);

// Find by status
List<CodeGenerationRequestEntity> findByStatus(String status);

// Find failed requests
List<CodeGenerationRequestEntity> findFailedRequests();

// Find successful GitHub pushes
List<CodeGenerationRequestEntity> findSuccessfulGithubPushes();

// Count by status
long countByStatus(String status);

// Cleanup old archived requests
long deleteArchivedRequestsOlderThan(LocalDateTime createdAt);
```

## Best Practices

### 1. Error Handling

Always wrap Smart Agent calls in try-catch to prevent generation failures:

```java
try {
    smartAgentService.saveOrUpdateRequest(request, generationId);
} catch (CodeGenerationException e) {
    log.error("Failed to persist request", e);
    // Continue with generation even if persistence fails
}
```

### 2. Logging

Use structured logging with meaningful context:

```java
log.info("Smart Agent: Processing request for generationId: {}, artifactId: {}",
        generationId, request.getArtifactId());
```

### 3. Data Retention

Implement scheduled cleanup of old archived requests:

```java
@Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
public void cleanupOldRequests() {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
    long deleted = repository.deleteArchivedRequestsOlderThan(cutoffDate);
    log.info("Cleaned up {} archived requests older than {}", deleted, cutoffDate);
}
```

### 4. Monitoring

Monitor key metrics:
- Request creation rate
- Success/failure ratio
- Average generation time
- MongoDB connection health
- Retry counts

## Troubleshooting

### Issue: Requests Not Being Persisted

**Symptoms:** No documents in MongoDB collection

**Possible Causes:**
1. MongoDB not running
2. Connection string incorrect
3. Network connectivity issues
4. Authentication failures

**Solution:**
```bash
# Check MongoDB connection
mongo mongodb://localhost:27017/probe-stack-generator

# Verify logs for connection errors
grep "MongoDB" logs/codegen.log
```

### Issue: Duplicate Key Error

**Symptoms:** Error about duplicate `generationId`

**Cause:** UUID collision (extremely rare) or manual data manipulation

**Solution:**
```java
// Regenerate UUID if collision detected
String generationId = UUID.randomUUID().toString();
while (repository.existsByGenerationId(generationId)) {
    generationId = UUID.randomUUID().toString();
}
```

### Issue: High Retry Counts

**Symptoms:** Many requests with `retryCount > 10`

**Cause:** Persistent errors in code generation

**Solution:**
1. Query failed requests: `findFailedRequests()`
2. Analyze error messages
3. Fix root cause
4. Archive failed requests

## Performance Considerations

### Indexing

Ensure proper indexes for common queries:
```javascript
db.code_generation_requests.createIndex({ "generationId": 1 }, { unique: true })
db.code_generation_requests.createIndex({ "artifactId": 1 })
db.code_generation_requests.createIndex({ "status": 1 })
db.code_generation_requests.createIndex({ "createdAt": 1, "archived": 1 })
```

### Connection Pooling

Configure MongoDB connection pool for high throughput:
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/probe-stack-generator?maxPoolSize=50&minPoolSize=10
```

### Async Operations

For high-volume scenarios, consider async persistence:
```java
@Async
public CompletableFuture<CodeGenerationRequestEntity> saveOrUpdateRequestAsync(
        CodeGenerationRequest request, String generationId) {
    // ... implementation
}
```

## Future Enhancements

1. **Analytics Dashboard** - Visualize request trends and success rates
2. **Webhook Notifications** - Notify external systems on request completion
3. **Request Queuing** - Queue large batches of generation requests
4. **Advanced Search** - Full-text search across request metadata
5. **Export Functionality** - Export request history to CSV/JSON
6. **Request Scheduling** - Schedule code generation for future execution
7. **Multi-tenancy** - Isolate requests by tenant/organization

## Support

For issues or questions about Smart Agent functionality:
- Check logs: `logs/codegen.log`
- Review this documentation
- Check MongoDB connection and health
- Verify configuration in `application.yaml`

## Summary

The Smart Agent functionality provides enterprise-grade request tracking and management for the Probe Stack API Code Generator. With automatic persistence, comprehensive logging, robust error handling, and rich query capabilities, it enables:

- Complete audit trails
- Request reprocessing
- Historical analysis
- Failure investigation
- Compliance and reporting

All while maintaining the simplicity and performance of the core code generation service.
