plugins {
    kotlin("js")
}

apply<MyPlugin>()

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
}

kotlin {
    js(IR) {
        nodejs {

        }
    }
}