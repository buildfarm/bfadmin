# Optimized Bazel configuration for Spring Boot bfadmin application

# Primary target - Run the Spring Boot application
sh_binary(
    name = "bfadmin",
    srcs = ["run_bfadmin.sh"],
    data = [
        "//main:bfadmin_jar",
    ],
)

# Build the application using Maven
alias(
    name = "build",
    actual = "//main:maven_build",
)

# Run tests using Maven
alias(
    name = "test",
    actual = "//main:maven_test",
)

# Access to the built JAR for other uses
alias(
    name = "bfadmin_jar",
    actual = "//main:bfadmin_jar",
)

# Development aliases for convenience
alias(
    name = "jar",
    actual = "//main:jar",
)

# Quick access to modular components for development
alias(
    name = "java_sources",
    actual = "//main/src/main/java:all_java_sources",
)

alias(
    name = "resources",
    actual = "//main/src/main/resources:all_resources",
)