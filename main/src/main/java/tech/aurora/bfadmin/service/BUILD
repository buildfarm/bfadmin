# BUILD file for service interfaces and implementations

filegroup(
    name = "service",
    srcs = glob([
        "*.java",
        "impl/*.java",
    ]),
    visibility = [
        "//main/src/main/java:__pkg__",
        "//main/src/main/java/tech/aurora/bfadmin/controller:__pkg__",
    ],
)

# Alias for consistency
filegroup(
    name = "java_sources",
    srcs = glob([
        "*.java",
        "impl/*.java",
    ]),
    visibility = ["//main/src/main/java:__pkg__"],
)

# Service interfaces only
filegroup(
    name = "interfaces",
    srcs = glob(["*.java"]),
    visibility = ["//main/src/main/java:__pkg__"],
)

# Service implementations only
filegroup(
    name = "implementations",
    srcs = ["//main/src/main/java/tech/aurora/bfadmin/service/impl:service_impl"],
    visibility = ["//main/src/main/java:__pkg__"],
)