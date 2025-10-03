# BUILD file for main source directories

# Group all source files for easy reference by Maven targets
filegroup(
    name = "all_sources",
    srcs = glob([
        "main/java/**/*.java",
        "main/resources/**/*",
    ]),
    visibility = ["//main:__pkg__"],
)

# Convenience alias for Java sources only
filegroup(
    name = "java_sources", 
    srcs = glob(["main/java/**/*.java"]),
    visibility = ["//main:__pkg__"],
)

# Convenience alias for resources only
filegroup(
    name = "resources",
    srcs = glob(["main/resources/**/*"]),
    visibility = ["//main:__pkg__"],
)