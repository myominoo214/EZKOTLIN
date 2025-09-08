# Windows .exe Build Guide for Kotlin/Compose Desktop

This guide explains how to generate Windows .exe files from this Kotlin/Compose Desktop project using multiple approaches.

## 🚀 Quick Start

### Method 1: GitHub Actions (Recommended)

The easiest way to build Windows .exe files is using GitHub Actions:

1. **Push to main branch** or **create a pull request**
2. **Manual trigger**: Go to Actions tab → "Build Windows Installers" → "Run workflow"
3. **Download artifacts** from the completed workflow run

### Method 2: Local Build with Compose Desktop

```bash
# Build Windows .exe using Compose Desktop (requires Windows or cross-compilation)
./gradlew packageExe

# Or use the custom task
./gradlew buildWindowsExe
```

### Method 3: Manual jpackage (Advanced)

```bash
# 1. Build the fat JAR
./gradlew shadowJar

# 2. Use jpackage to create .exe (Windows only)
jpackage \
  --input build/libs \
  --name "EZLedger" \
  --main-jar "EZLedger-1.0.0-all.jar" \
  --main-class "MainKt" \
  --type exe \
  --dest dist \
  --app-version "1.0.0" \
  --description "EZLedger - Secure Financial Management" \
  --vendor "YOTTA Systems" \
  --copyright "© 2024 YOTTA Systems" \
  --win-dir-chooser \
  --win-menu \
  --win-shortcut
```

## 📋 Available Build Methods

### 1. GitHub Actions Workflow

**File**: `.github/workflows/build-installers.yml`

**Features**:
- ✅ Native Windows .exe using jpackage
- ✅ Legacy NSIS installer support
- ✅ Automatic artifact upload
- ✅ Cross-platform builds
- ✅ Version parameterization

**Outputs**:
- `windows-exe`: Native Windows executable
- `nsis-installer`: NSIS-based installer
- `portable-distribution`: Portable ZIP/TAR.GZ

### 2. Compose Desktop Native Distributions

**Gradle Tasks**:
```bash
./gradlew packageExe              # Windows .exe
./gradlew packageMsi              # Windows .msi
./gradlew packageDmg              # macOS .dmg
./gradlew packageDeb              # Linux .deb
./gradlew packageDistributionForCurrentOS  # Current platform
```

### 3. Custom Gradle Tasks

```bash
./gradlew buildWindowsExe         # Build Windows .exe
./gradlew buildAllNativeDistributions  # All platforms
./gradlew prepareJarForJpackage   # Prepare JAR for manual jpackage
```

## 🛠️ Requirements

### For GitHub Actions
- ✅ No local requirements
- ✅ Automatic Windows runner setup
- ✅ JDK 17 automatically installed

### For Local Builds

**Windows**:
- JDK 17+ with jpackage
- Gradle 8.4+
- Windows 10+ (for .exe generation)

**Cross-platform**:
- Docker (for NSIS installer)
- Wine (optional, for NSIS)

## 📁 Output Locations

### GitHub Actions Artifacts
- **windows-exe**: `dist/*.exe`
- **nsis-installer**: `installer-dist/*.exe`
- **portable-distribution**: `secure-dist/*.zip`, `secure-dist/*.tar.gz`

### Local Builds
- **Compose Desktop**: `build/compose/binaries/main/exe/`
- **Manual jpackage**: `dist/`
- **Shadow JAR**: `build/libs/EZLedger-1.0.0-all.jar`

## 🔧 Configuration

### Application Metadata

```kotlin
// build.gradle.kts
compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            packageName = "EZLedger"
            packageVersion = "1.0.0"
            description = "EZLedger - Secure Financial Management"
            copyright = "© 2024 YOTTA Systems"
            vendor = "YOTTA Systems"
            
            windows {
                iconFile.set(project.file("icon.ico"))
                menuGroup = "YOTTA"
                perUserInstall = true
                dirChooser = true
            }
        }
    }
}
```

### Version Management

```bash
# GitHub Actions with custom version
gh workflow run "Build Windows Installers" -f version=2.0.0
```

## 🚨 Troubleshooting

### Common Issues

1. **UnsatisfiedLinkError with Skiko**
   ```bash
   ./fix-skiko-issue.sh
   ```

2. **Missing icon.ico**
   - Ensure `icon.ico` exists in project root
   - Or update `iconFile.set()` path in build.gradle.kts

3. **jpackage not found**
   - Use JDK 17+ (includes jpackage)
   - Verify: `jpackage --version`

4. **Cross-compilation issues**
   - Use GitHub Actions for cross-platform builds
   - Or use Docker-based approach

### Debug Commands

```bash
# Check Java version
java -version

# Verify jpackage
jpackage --version

# List available Gradle tasks
./gradlew tasks --group=distribution

# Build with debug info
./gradlew packageExe --info
```

## 📚 Additional Resources

- [Compose Desktop Documentation](https://github.com/JetBrains/compose-multiplatform)
- [jpackage Documentation](https://docs.oracle.com/en/java/javase/17/jpackage/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [NSIS Documentation](https://nsis.sourceforge.io/Docs/)

## 🎯 Best Practices

1. **Use GitHub Actions** for production builds
2. **Test locally** with Compose Desktop tasks
3. **Version your releases** using semantic versioning
4. **Include proper icons** for professional appearance
5. **Sign your executables** for Windows SmartScreen compatibility
6. **Test on target Windows versions** before release

---

**Need help?** Check the [TROUBLESHOOTING.md](TROUBLESHOOTING.md) file or create an issue.