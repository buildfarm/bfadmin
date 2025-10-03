#!/bin/bash
# Run the Spring Boot JAR via Bazel

# The JAR file is available in the runfiles under main/target/bfadmin.jar
# Use external properties file from home directory
EXTERNAL_PROPERTIES="$HOME/bfadmin.properties"

if [[ -f "$EXTERNAL_PROPERTIES" ]]; then
    echo "Using external properties file: $EXTERNAL_PROPERTIES"
    exec java -jar "main/target/bfadmin.jar" --spring.config.location="file:$EXTERNAL_PROPERTIES" "$@"
else
    echo "External properties file not found at: $EXTERNAL_PROPERTIES"
    echo "Using internal application.properties"
    exec java -jar "main/target/bfadmin.jar" "$@"
fi