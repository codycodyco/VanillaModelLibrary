plugins {
    java
}

group = "dev.cody"
version = "0.2.2"
description = "Installs and selects the vanillamobs static models through RP Engine."

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://api.modrinth.com/maven")
}

val paperApiVersion = "26.2.build.134-stable"
val rpEngineVersion = "0.2.20"

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("maven.modrinth:rp-engine:$rpEngineVersion")
    compileOnly("com.google.code.gson:gson:2.11.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveBaseName = "VanillaModelLibrary"
    // Deliberately no vanilla textures and no content folder in the jar:
    // assets are delivered as a separate, versioned package.
}

dependencies {
    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
    testImplementation("com.google.code.gson:gson:2.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.0")
}

tasks.test {
    useJUnitPlatform()
}
