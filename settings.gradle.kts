pluginManagement {
    repositories {
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
        gradlePluginPortal()
        mavenLocal()
    }
}
rootProject.name = "klib-diff"
include("lib1", "lib2")
