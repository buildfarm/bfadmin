# BUILD file for service implementations

filegroup(
    name = "service_impl",
    srcs = glob(["*.java"]),
    visibility = [
        "//main/src/main/java/tech/aurora/bfadmin/service:__pkg__",
        "//main/src/main/java:__pkg__",
    ],
)

# Individual implementation files for granular access
filegroup(
    name = "admin_service_impl",
    srcs = ["AdminServiceImpl.java"],
    visibility = [
        "//main/src/main/java/tech/aurora/bfadmin/service:__pkg__",
    ],
)

filegroup(
    name = "valkey_service_impl",
    srcs = ["ValkeyServiceImpl.java"],
    visibility = [
        "//main/src/main/java/tech/aurora/bfadmin/service:__pkg__",
    ],
)

# Alias for consistency with parent package naming
filegroup(
    name = "java_sources",
    srcs = glob(["*.java"]),
    visibility = [
        "//main/src/main/java/tech/aurora/bfadmin/service:__pkg__",
    ],
)