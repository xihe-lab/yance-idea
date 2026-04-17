plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "yance-lint"

include("yance-common")
include("yance-idea")
include("yance-p3c")
include("yance-lint")
