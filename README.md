# Kotlin Compose Desktop Application

A simple desktop application built with Kotlin and Jetpack Compose for Desktop.

## Features

- Modern UI built with Jetpack Compose
- Sales system with sidebar navigation
- Dynamic IP address display
- Material Design components
- Cross-platform desktop support (Windows, macOS, Linux)
- **Hot Reload Support** - Automatic application restart on code changes
- **Automated Builds** - GitHub Actions workflow for building Windows installers (.exe and .msi)

## Requirements

- JDK 11 or higher
- Gradle (included via wrapper)

## How to Run

### Development Mode (Standard)
```bash
./gradlew run
```

### Development Mode with Hot Reload
For automatic application restart when code changes are detected:

**macOS/Linux:**
```bash
./run-dev.sh
```

**Windows:**
```cmd
run-dev.bat
```

**Manual Hot Reload (Alternative):**
```bash
./gradlew run -t --no-daemon
```

> **Note:** Hot reload will automatically restart the application when you save changes to any Kotlin source files. Press `Ctrl+C` to stop the development server.

### Build Distributable Package
```bash
./gradlew createDistributable
```

### Build Native Package
```bash
./gradlew packageDistributionForCurrentOS
```

## Project Structure

```
├── build.gradle.kts          # Build configuration
├── settings.gradle.kts       # Project settings
├── gradle.properties         # Gradle properties
└── src/
    └── main/
        └── kotlin/
            └── Main.kt           # Main application file
```

## Technologies Used

- **Kotlin**: Programming language
- **Jetpack Compose for Desktop**: UI framework
- **Gradle**: Build system
- **Material Design**: UI components and theming
- **GitHub Actions**: CI/CD for automated builds
- **Docker**: Cross-platform installer building
- **WiX Toolset**: MSI installer generation
- **NSIS**: EXE installer generation

## Automated Builds with GitHub Actions

This project includes a GitHub Actions workflow that automatically builds Windows installers (.exe and .msi) when code is pushed to the main branch or when manually triggered.

### Using the Workflow

1. Push changes to the main branch to trigger automatic builds
2. Go to the Actions tab in GitHub to manually trigger a build
3. Download the built installers from the workflow artifacts

For more details, see [GITHUB_ACTIONS_README.md](GITHUB_ACTIONS_README.md).