package com.probe.stack.code.generator.exception;

/**
 * Exception thrown when code generation fails
 */
public class CodeGenerationException extends RuntimeException {
    
    public CodeGenerationException(String message) {
        super(message);
    }
    
    public CodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Exception thrown when OpenAPI spec download or parsing fails
 */
class SpecificationDownloadException extends CodeGenerationException {
    
    public SpecificationDownloadException(String message) {
        super(message);
    }
    
    public SpecificationDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Exception thrown when file operations fail
 */
class FileOperationException extends CodeGenerationException {
    
    public FileOperationException(String message) {
        super(message);
    }
    
    public FileOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Exception thrown when template processing fails
 */
class TemplateProcessingException extends CodeGenerationException {
    
    public TemplateProcessingException(String message) {
        super(message);
    }
    
    public TemplateProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}