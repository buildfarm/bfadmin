# BUILD file for Java source organization

# Main application entry point
filegroup(
    name = "bfadmin_main",
    srcs = ["tech/aurora/bfadmin/BuildfarmAdmin.java"],
    visibility = ["//main:__pkg__"],
)

# All Java sources (for Maven compatibility)
filegroup(
    name = "all_java_sources",
    srcs = glob(["**/*.java"]),
    visibility = [
        "//main/src:__pkg__",
        "//:__pkg__",
    ],
)

# Package-specific source groups
filegroup(
    name = "config_sources",
    srcs = ["//main/src/main/java/tech/aurora/bfadmin/config:java_sources"],
    visibility = ["//main:__pkg__"],
)

filegroup(
    name = "controller_sources",
    srcs = ["//main/src/main/java/tech/aurora/bfadmin/controller:java_sources"],
    visibility = ["//main:__pkg__"],
)

filegroup(
    name = "model_sources",
    srcs = ["//main/src/main/java/tech/aurora/bfadmin/model:java_sources"],
    visibility = ["//main:__pkg__"],
)

filegroup(
    name = "service_sources",
    srcs = ["//main/src/main/java/tech/aurora/bfadmin/service:java_sources"],
    visibility = ["//main:__pkg__"],
)