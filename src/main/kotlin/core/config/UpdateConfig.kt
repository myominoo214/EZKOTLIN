package core.config

object UpdateConfig {
    // TODO: Update this URL to point to your actual update server
    const val UPDATE_CHECK_URL = "https://your-server.com/api/update.json"
    
    // TODO: This should be automatically updated during build process
    const val CURRENT_VERSION = "1.0.0"
    
    // Update check settings
    const val CHECK_ON_STARTUP = true
    const val AUTO_DOWNLOAD = false // Set to true for automatic downloads
    
    // Temp directory for downloads
    const val TEMP_DIR = "temp"
    
    // MSI installer settings
    const val SILENT_INSTALL = true
    const val NO_RESTART = true
}