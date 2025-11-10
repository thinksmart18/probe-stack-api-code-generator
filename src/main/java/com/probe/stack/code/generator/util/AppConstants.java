package com.probe.stack.code.generator.util;

public class AppConstants {

    public static String README_MD_CONTENT = """
                # %s
                
                Generated Spring Boot application from OpenAPI specification.
                
                ## Project Details
                - **Group ID**: %s
                - **Artifact ID**: %s
                - **Version**: %s
                - **Base Package**: %s
                
                ## Building the Project
                ```bash
                mvn clean install
                ```
                
                ## Running the Application
                ```bash
                mvn spring-boot:run
                ```
                
                ## API Documentation
                Once the application is running, access the Swagger UI at:
                - http://localhost:8080/swagger-ui.html
                
                ## API Docs (OpenAPI)
                - http://localhost:8080/api-docs
                """;
    public static String GIT_IGNORE_CONTENT = """
                    # Compiled class files
                    *.class
                    
                    # Log files
                    *.log
                    
                    # Package Files
                    *.jar
                    *.war
                    *.nar
                    *.ear
                    *.zip
                    *.tar.gz
                    *.rar
                    
                    # Maven
                    target/
                    pom.xml.tag
                    pom.xml.releaseBackup
                    pom.xml.versionsBackup
                    pom.xml.next
                    release.properties
                    dependency-reduced-pom.xml
                    buildNumber.properties
                    .mvn/timing.properties
                    .mvn/wrapper/maven-wrapper.jar
                    
                    # IDE
                    .idea/
                    *.iws
                    *.iml
                    *.ipr
                    .vscode/
                    .classpath
                    .project
                    .settings/
                    
                    # OS
                    .DS_Store
                    Thumbs.db
                    
                    # Application
                    application-local.properties
                    application-local.yml
                    """;
}
