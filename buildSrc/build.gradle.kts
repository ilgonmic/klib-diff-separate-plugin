buildscript {
    dependencies {

        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.255-SNAPSHOT")
    }

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.255-SNAPSHOT")
}