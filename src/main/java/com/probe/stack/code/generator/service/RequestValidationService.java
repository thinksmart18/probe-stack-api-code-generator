package com.probe.stack.code.generator.service;

import com.probe.stack.code.generator.dto.CodeGenerationRequest;
import com.probe.stack.code.generator.exception.CodeGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for validating code generation requests
 */
@Slf4j
@Service
public class RequestValidationService {
    
    /**
     * Validates that the request has either URL or content
     *
     * @param request Code generation request
     * @throws CodeGenerationException if validation fails
     */
    public void validateSpecificationSource(CodeGenerationRequest request) {
        boolean hasUrl = request.getOpenApiSpecUrl() != null && 
                        !request.getOpenApiSpecUrl().isEmpty();
        boolean hasContent = request.getSpecContent() != null && 
                            !request.getSpecContent().isEmpty();
        
        if (!hasUrl && !hasContent) {
            log.error("Request missing both URL and content");
            throw new CodeGenerationException(
                "Either 'openApiSpecUrl' or 'specContent' must be provided"
            );
        }
        
        if (hasUrl && hasContent) {
            log.warn("Request has both URL and content, URL will be ignored");
        }
        
        log.debug("Specification source validation passed - hasUrl: {}, hasContent: {}", 
                hasUrl, hasContent);
    }
    
    /**
     * Validates content type when raw content is provided
     *
     * @param request Code generation request
     */
    public void validateContentType(CodeGenerationRequest request) {
        if (request.getSpecContent() != null && !request.getSpecContent().isEmpty()) {
            if (request.getSpecContentType() == null || request.getSpecContentType().isEmpty()) {
                // Auto-detect if not provided
                String content = request.getSpecContent().trim();
                String detectedType = content.startsWith("{") || content.startsWith("[") 
                    ? "json" 
                    : "yaml";
                request.setSpecContentType(detectedType);
                log.info("Auto-detected content type: {}", detectedType);
            }
        }
    }
    
    /**
     * Validates GitHub configuration
     *
     * @param request Code generation request
     */
    public void validateGitHubConfig(CodeGenerationRequest request) {
        if (request.getGitHubConfig() != null && request.getGitHubConfig().isEnabled()) {
            if (request.getGithubToken() == null || request.getGithubToken().isEmpty()) {
                throw new CodeGenerationException(
                    "GitHub token is required when GitHub integration is enabled"
                );
            }
            
            if (request.getOrganization() == null ||
                request.getOrganization().isEmpty()) {
                throw new CodeGenerationException(
                    "GitHub organization/username is required when GitHub integration is enabled"
                );
            }
            
            log.debug("GitHub configuration validation passed");
        }
    }
    
    /**
     * Performs all validations
     *
     * @param request Code generation request
     */
    public void validateRequest(CodeGenerationRequest request) {
        validateSpecificationSource(request);
        validateContentType(request);
        validateGitHubConfig(request);
    }
}