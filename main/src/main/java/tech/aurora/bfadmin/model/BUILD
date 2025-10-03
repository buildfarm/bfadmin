# BUILD file for data model classes

filegroup(
    name = "model",
    srcs = glob(["*.java"]),
    visibility = [
        "//main/src/main/java:__pkg__",
        "//main/src/main/java/tech/aurora/bfadmin/controller:__pkg__",
        "//main/src/main/java/tech/aurora/bfadmin/service:__pkg__",
    ],
)

# Alias for consistency
filegroup(
    name = "java_sources",
    srcs = glob(["*.java"]),
    visibility = ["//main/src/main/java:__pkg__"],
)