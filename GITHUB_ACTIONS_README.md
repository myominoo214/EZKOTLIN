# GitHub Actions Workflow for Building Windows Installers

This project includes a GitHub Actions workflow that automatically builds and packages the Kotlin Compose application into self-contained Windows installers (.exe and .msi) and portable distributions.

## Workflow Overview

The workflow is defined in `.github/workflows/build-installers.yml` and performs the following steps:

1. Checks out the repository code
2. Sets up JDK 17 for building the application
3. Builds the application JAR using Gradle
4. Sets up Docker for cross-platform building
5. Creates a secure Windows distribution with bundled JRE
6. Builds an MSI installer using WiX Toolset
7. Builds an EXE installer using NSIS
8. Uploads all artifacts (MSI, EXE, and portable distributions) for download

## Triggering the Workflow

The workflow can be triggered in three ways:

1. **Automatically on push to main branch**
2. **Automatically on pull requests to main branch**
3. **Manually via workflow_dispatch**

To trigger the workflow manually:

1. Go to the "Actions" tab in your GitHub repository
2. Select "Build Windows Installers" from the workflows list
3. Click "Run workflow"
4. Optionally specify a custom version number (defaults to 1.0.0)
5. Click "Run workflow" to start the build

## Accessing Build Artifacts

After the workflow completes successfully, you can download the build artifacts:

1. Go to the completed workflow run
2. Scroll down to the "Artifacts" section
3. Download any of the following artifacts:
   - **msi-installer**: Contains the Windows MSI installer
   - **exe-installer**: Contains the Windows EXE installer
   - **portable-distribution**: Contains portable ZIP and TAR.GZ archives

## Customizing the Workflow

### Changing the Version Number

The version number can be customized in several ways:

1. **For manual runs**: Enter a custom version when triggering the workflow
2. **For all builds**: Edit the version in `build.gradle.kts`
3. **For specific installers**: Edit the version in the respective build scripts

### Modifying Build Parameters

To customize the build process:

1. Edit `.github/workflows/build-installers.yml` to change workflow steps
2. Modify `build-secure-windows.sh`, `build-msi-installer.sh`, or `build-nsis-installer.sh` to change installer properties
3. Update `installer.wxs` (for MSI) or `installer.nsi` (for EXE) to change installer behavior

## Troubleshooting

### Common Issues

1. **Docker errors**: Ensure Docker is properly configured in the workflow
2. **Build failures**: Check the build logs for specific error messages
3. **Missing artifacts**: Verify that all build scripts completed successfully

### Workflow Logs

To view detailed logs for troubleshooting:

1. Go to the workflow run
2. Click on the job name "Build Windows Installers"
3. Expand the step that failed to see detailed logs

## Security Considerations

The workflow builds secure, self-contained Windows executables with:

- Bundled JRE (no external Java dependencies)
- Application obfuscation for protection
- Anti-tampering measures
- Proper Windows integration

## Additional Resources

- See `MSI-INSTALLER-README.md` for details on the MSI installer
- Check the build scripts for additional configuration options