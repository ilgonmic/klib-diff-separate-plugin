plugins {
    kotlin("js")
}

apply<MyPlugin>()

repositories {
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

extensions.getByName<KlibDiffExtension>(KlibDiffExtension.NAME).also {
    it.threshold.set(1)
}