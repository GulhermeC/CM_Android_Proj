pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password =
                    "pk.eyJ1IjoicnVpdWEiLCJhIjoiY200anplNTNvMDM0ejJqc2htOXZjbTB3NCJ9.4VYtbLOO9eEVlbRMPb1GVg" // Replace with your token
            }
        }}}

// settings.gradle.kts







rootProject.name = "gps"
include(":app")
 