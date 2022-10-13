plugins {
    kotlin("js")
}

apply<MyPlugin>()

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    js(IR) {
        binaries.executable()
        nodejs {

        }
    }
}