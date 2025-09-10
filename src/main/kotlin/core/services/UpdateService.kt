package core.services

import core.models.UpdateInfo
import core.models.VersionInfo
import core.config.UpdateConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.system.exitProcess

class UpdateService(private val httpClient: HttpClient) {
    companion object {
        private val CURRENT_VERSION = UpdateConfig.CURRENT_VERSION
        private val UPDATE_CHECK_URL = UpdateConfig.UPDATE_CHECK_URL
        private val TEMP_DIR = UpdateConfig.TEMP_DIR
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun checkForUpdates(): UpdateCheckResult {
        return try {
            val response = httpClient.get(UPDATE_CHECK_URL)
            
            if (response.status == HttpStatusCode.OK) {
                val updateInfo: UpdateInfo = response.body()
                val currentVersion = VersionInfo.fromString(CURRENT_VERSION)
                val latestVersion = VersionInfo.fromString(updateInfo.latestVersion)
                
                if (latestVersion.isNewerThan(currentVersion)) {
                    UpdateCheckResult.UpdateAvailable(updateInfo)
                } else {
                    UpdateCheckResult.NoUpdate
                }
            } else {
                UpdateCheckResult.Error("Failed to check for updates: ${response.status}")
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("Update check failed: ${e.message}")
        }
    }
    
    suspend fun downloadAndInstallUpdate(updateInfo: UpdateInfo): Boolean {
        return try {
            // Create temp directory
            val tempDir = File(TEMP_DIR)
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            
            // Download MSI file
            val msiFile = File(tempDir, "installer.msi")
            downloadFile(updateInfo.downloadUrl, msiFile)
            
            // Install update silently
            installMsiSilently(msiFile)
            
            true
        } catch (e: Exception) {
            println("Update installation failed: ${e.message}")
            false
        }
    }
    
    private suspend fun downloadFile(url: String, destinationFile: File) {
        val response = httpClient.get(url)
        
        if (response.status == HttpStatusCode.OK) {
            val channel = response.bodyAsChannel()
            destinationFile.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead > 0) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
        } else {
            throw Exception("Failed to download file: ${response.status}")
        }
    }
    
    private fun installMsiSilently(msiFile: File) {
        val command = mutableListOf(
            "msiexec",
            "/i",
            msiFile.absolutePath
        )
        
        if (UpdateConfig.SILENT_INSTALL) {
            command.add("/qn") // Silent installation
        }
        
        if (UpdateConfig.NO_RESTART) {
            command.add("/norestart") // Don't restart automatically
        }
        
        val processBuilder = ProcessBuilder(command)
        val process = processBuilder.start()
        
        // Wait for installation to complete
        val exitCode = process.waitFor()
        
        if (exitCode == 0) {
            // Installation successful, exit current app
            println("Update installed successfully. Exiting application...")
            
            // Clean up temp file
            msiFile.delete()
            
            // Exit the application
            exitProcess(0)
        } else {
            throw Exception("MSI installation failed with exit code: $exitCode")
        }
    }
}

sealed class UpdateCheckResult {
    object NoUpdate : UpdateCheckResult()
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}