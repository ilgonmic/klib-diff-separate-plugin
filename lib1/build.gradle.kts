plugins {
    kotlin("js")
}

apply<MyPlugin>()

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":lib2"))
}

kotlin {
    js(IR) {
        nodejs {

        }
    }
}