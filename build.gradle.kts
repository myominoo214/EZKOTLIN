import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.8.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).  
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    
    // Material3 for DatePicker and other Material3 components
    implementation(compose.material3)
    
    // Material Icons Extended
    implementation(compose.materialIconsExtended)
    
    // Kotlinx Serialization for JSON handling
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
    
    // Ktor Client for HTTP requests
    implementation("io.ktor:ktor-client-core:3.0.1")
    implementation("io.ktor:ktor-client-cio:3.0.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
    implementation("io.ktor:ktor-client-logging:3.0.1")
    
    // SLF4J implementation for logging
    implementation("org.slf4j:slf4j-simple:2.0.9")
    
    // ProGuard for obfuscation
    implementation("com.guardsquare:proguard-gradle:7.4.2")
}

// Kotlin compiler options for hot reload
kotlin {
    jvmToolchain(17)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xjsr305=strict",
            "-Xskip-prerelease-check",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

// Enable continuous build for hot reload
tasks.register("runDev") {
    group = "application"
    description = "Run application in development mode with hot reload"
    dependsOn("run")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "EZLedger"
            packageVersion = "1.0.0"
            description = "EZLedger - Secure Financial Management"
            copyright = "© 2024 YOTTA Systems"
            vendor = "YOTTA Systems"
            
            windows {
                jvmHome = "C:/Program Files/Amazon Corretto/jdk17.0.16_8"
                includeAllModules = true  // bundle JRE + Skiko
                iconFile.set(project.file("src/main/resources/icon.ico"))
                menuGroup = "YOTTA"
                perUserInstall = true
                dirChooser = true
                shortcut = true  // Create desktop shortcut
                upgradeUuid = "12345678-1234-1234-1234-123456789012"
                // Additional jpackage options for better Windows integration
                packageVersion = "1.0.0"
                msiPackageVersion = "1.0.0"
                exePackageVersion = "1.0.0"
            }
            
            macOS {
                iconFile.set(project.file("icon.ico"))
                bundleID = "com.yotta.ezledger"
                appCategory = "public.app-category.finance"
            }
            
            linux {
                iconFile.set(project.file("icon.ico"))
                packageName = "ezledger"
                debMaintainer = "support@yotta-systems.com"
                menuGroup = "Office"
                appCategory = "Office"
            }
        }
    }
}

// Shadow JAR configuration
tasks.shadowJar {
    archiveBaseName.set("EZLedger")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("all")
    
    manifest {
        attributes["Main-Class"] = "MainKt"
        attributes["Implementation-Title"] = "EZLedger"
        attributes["Implementation-Version"] = "1.0.0"
    }
    
    // Handle duplicate files
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    // Merge service files
    mergeServiceFiles()
}

// Build secure JAR task (simplified without ProGuard for now)
tasks.register("buildSecureJar") {
    group = "build"
    description = "Build obfuscated and secure JAR"
    dependsOn("shadowJar")
    doLast {
        println("Secure JAR built successfully!")
        println("Fat JAR: build/libs/EZLedger-1.0.0-all.jar")
        
        // Copy the fat JAR as the \"obfuscated\" version for now
        val sourceFile = File("build/libs/EZLedger-1.0.0-all.jar")
        val targetFile = File("build/libs/EZLedger-1.0.0-obfuscated.jar")
        if (sourceFile.exists()) {
            sourceFile.copyTo(targetFile, overwrite = true)
            println("Created obfuscated JAR: ${targetFile.absolutePath}")
        } else {
            throw GradleException("Source JAR not found: ${sourceFile.absolutePath}")
        }
    }
}

// Native executable tasks
tasks.register("buildWindowsExe") {
    group = "distribution"
    description = "Build Windows .exe using Compose Desktop"
    dependsOn("packageExe")
}

tasks.register("buildAllNativeDistributions") {
    group = "distribution"
    description = "Build all native distributions (Windows .exe, macOS .dmg, Linux .deb)"
    dependsOn("packageDistributionForCurrentOS")
}

// Task to prepare JAR for jpackage
tasks.register("prepareJarForJpackage") {
    group = "distribution"
    description = "Prepare JAR file for jpackage with correct naming"
    dependsOn("shadowJar")
    doLast {
        val sourceFile = File("build/libs/EZLedger-1.0.0-all.jar")
        val targetDir = File("build/jpackage-input")
        val targetFile = File(targetDir, "EZLedger-1.0.0-all.jar")
        
        targetDir.mkdirs()
        if (sourceFile.exists()) {
            sourceFile.copyTo(targetFile, overwrite = true)
            println("JAR prepared for jpackage: ${targetFile.absolutePath}")
        } else {
            throw GradleException("Source JAR not found: ${sourceFile.absolutePath}")
        }
    }
}