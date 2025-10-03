# BUILD file for static resources and templates

# Application properties and configuration files
filegroup(
    name = "config_files",
    srcs = glob([
        "*.properties", 
        "*.xml",
        "*.yml",
        "*.yaml",
    ]),
    visibility = ["//main:__pkg__"],
)

# Static web assets (CSS, JS, images)
filegroup(
    name = "static_assets",
    srcs = glob([
        "static/**/*",
    ]),
    visibility = ["//main:__pkg__"],
)

# Thymeleaf templates
filegroup(
    name = "templates",
    srcs = glob([
        "templates/**/*",
    ]),
    visibility = ["//main:__pkg__"],
)

# All resources combined
filegroup(
    name = "all_resources",
    srcs = [
        ":config_files",
        ":static_assets", 
        ":templates",
    ],
    visibility = [
        "//main:__pkg__",
        "//main/src:__pkg__",
        "//:__pkg__",
    ],
)