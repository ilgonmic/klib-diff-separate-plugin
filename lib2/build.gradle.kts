plugins {
    kotlin("js")
}

repositories {
    maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
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
