# Main BUILD file for the Maven-based Spring Boot application

# Maven build wrapper
sh_binary(
    name = "maven_build",
    srcs = ["mvnw"],
    args = ["clean", "package", "-DskipTests"],
    data = [
        "pom.xml",
        "mvn/wrapper/maven-wrapper.jar",
        "mvn/wrapper/maven-wrapper.properties", 
        "mvn/wrapper/MavenWrapperDownloader.java",
        "//main/src:all_sources",
    ],
    visibility = ["//:__pkg__"],
)

# Maven test runner
sh_binary(
    name = "maven_test",
    srcs = ["mvnw"],
    args = ["test"],
    data = [
        "pom.xml",
        "mvn/wrapper/maven-wrapper.jar",
        "mvn/wrapper/maven-wrapper.properties",
        "mvn/wrapper/MavenWrapperDownloader.java",
        "//main/src:all_sources",
    ],
    visibility = ["//:__pkg__"],
)

# Reference to the built JAR (created by Maven)
java_import(
    name = "bfadmin_jar",
    jars = ["target/bfadmin.jar"],
    visibility = ["//visibility:public"],
)

# Convenience alias for the JAR
alias(
    name = "jar",
    actual = ":bfadmin_jar",
    visibility = ["//visibility:public"],
)