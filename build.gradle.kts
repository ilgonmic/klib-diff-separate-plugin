plugins {
    kotlin("js")
}

apply<klib.director.KlibDirectorPlugin>()

repositories {
    maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":lib1"))
    testImplementation(kotlin("test"))
}

kotlin {
    js(IR) {
        binaries.executable()
        nodejs {

        }
    }
}
