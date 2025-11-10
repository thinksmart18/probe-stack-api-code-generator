# Multi-stage build for OpenAPI Code Generator Service

# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN ./mvnw clean package -DskipTests && \
    mkdir -p target/dependency && \
    (cd target/dependency; jar -xf ../*.jar)

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

# Add labels
LABEL maintainer="your-email@example.com"
LABEL description="OpenAPI Code Generator Microservice"
LABEL version="1.0.0"

# Create app user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy application from build stage
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

# Create required directories
RUN mkdir -p /app/generated-projects /app/templates /app/temp && \
    chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/codegen/health || exit 1

# Set JVM options
ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC"

# Run application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -cp /app:/app/lib/* com.codegen.openapi.CodeGeneratorApplication"]