# Auto-Updater System

This document explains how to set up and use the auto-updater system for EZ Ledger.

## Overview

The auto-updater system automatically checks for updates when the application starts and provides a seamless update experience for users.

## How It Works

1. **App Startup** → Fetches `update.json` from your server
2. **Version Check** → Compares `latestVersion` with current version
3. **User Prompt** → Shows update dialog if newer version available
4. **Download** → Downloads `.msi` file to temp directory
5. **Silent Install** → Runs `msiexec /i installer.msi /qn` in background
6. **App Exit** → Application closes and user reopens updated version

## Setup Instructions

### 1. Configure Update Server

Update the server URL in `src/main/kotlin/core/config/UpdateConfig.kt`:

```kotlin
const val UPDATE_CHECK_URL = "https://your-domain.com/api/update.json"
```

### 2. Create Update Endpoint

Your server should provide an endpoint that returns JSON in this format:

```json
{
  "latestVersion": "1.1.0",
  "downloadUrl": "https://your-server.com/downloads/ez-ledger-1.1.0.msi",
  "releaseNotes": "New features and bug fixes...",
  "mandatory": false,
  "minSupportedVersion": "1.0.0"
}
```

### 3. Host MSI Files

Ensure your MSI installer files are accessible via the `downloadUrl` provided in the JSON response.

### 4. Update Version Number

When building a new version, update the version in:
- `src/main/kotlin/core/config/UpdateConfig.kt`
- `build.gradle.kts` (version property)

## Configuration Options

In `UpdateConfig.kt`, you can customize:

```kotlin
const val CHECK_ON_STARTUP = true      // Enable/disable startup checks
const val AUTO_DOWNLOAD = false        // Auto-download without user prompt
const val SILENT_INSTALL = true        // Silent MSI installation
const val NO_RESTART = true           // Prevent automatic restart
```

## Update JSON Schema

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `latestVersion` | String | Yes | Latest available version (e.g., "1.1.0") |
| `downloadUrl` | String | Yes | Direct download URL for MSI file |
| `releaseNotes` | String | No | Markdown-formatted release notes |
| `mandatory` | Boolean | No | If true, user cannot skip update |
| `minSupportedVersion` | String | No | Minimum version that can update |

## Version Comparison

Versions are compared using semantic versioning (major.minor.patch):
- `1.1.0` > `1.0.9`
- `2.0.0` > `1.9.9`
- `1.0.1` > `1.0.0`

## Security Considerations

1. **HTTPS Only**: Always use HTTPS for update checks and downloads
2. **File Verification**: Consider adding checksum verification for downloaded files
3. **Signed MSI**: Use code-signed MSI files for security
4. **Server Authentication**: Implement proper authentication for your update server

## Testing

### Local Testing

1. Set up a local server with `update.json`
2. Update `UPDATE_CHECK_URL` to point to localhost
3. Create a test MSI with higher version number
4. Run the application to test update flow

### Production Testing

1. Deploy update server with test version
2. Test with a small group of users
3. Monitor logs for any issues
4. Roll out to all users

## Troubleshooting

### Common Issues

1. **Update check fails**
   - Verify server URL is accessible
   - Check network connectivity
   - Validate JSON format

2. **Download fails**
   - Ensure MSI file exists at download URL
   - Check file permissions
   - Verify sufficient disk space

3. **Installation fails**
   - Check MSI file integrity
   - Verify user has admin privileges
   - Review Windows Event Logs

### Logs

Update-related logs are printed to console:
- "No updates available"
- "Update check failed: [error]"
- "Update installed successfully"

## Example Server Implementation

### Node.js/Express Example

```javascript
app.get('/api/update.json', (req, res) => {
  res.json({
    latestVersion: "1.1.0",
    downloadUrl: "https://releases.yourapp.com/ez-ledger-1.1.0.msi",
    releaseNotes: "Bug fixes and improvements",
    mandatory: false
  });
});
```

### Static File Example

Simply host a static `update.json` file on your web server and update it when new versions are released.

## Best Practices

1. **Gradual Rollout**: Release updates to small groups first
2. **Rollback Plan**: Keep previous versions available
3. **User Communication**: Provide clear release notes
4. **Testing**: Thoroughly test update process before release
5. **Monitoring**: Monitor update success rates and errors

## File Structure

```
src/main/kotlin/
├── core/
│   ├── config/
│   │   └── UpdateConfig.kt          # Update configuration
│   ├── models/
│   │   └── UpdateInfo.kt            # Update data models
│   └── services/
│       └── UpdateService.kt         # Update logic
├── ui/
│   └── dialogs/
│       └── UpdateDialog.kt          # Update UI components
└── Main.kt                          # Integration point
```

This auto-updater system provides a professional, user-friendly way to keep your application up to date with minimal user intervention.