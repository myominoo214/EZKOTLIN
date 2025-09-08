package security

import java.lang.management.ManagementFactory
import java.security.MessageDigest
import java.io.File
import kotlinx.coroutines.*
import kotlin.system.exitProcess

/**
 * Anti-tampering and debugging protection mechanisms
 * Provides runtime protection against reverse engineering attempts
 */
object AntiTamperingProtection {
    
    private var isProtectionActive = true
    private val protectionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * Initialize all protection mechanisms
     */
    fun initialize() {
        if (!isProtectionActive) return
        
        // Start protection checks
        startDebuggerDetection()
        startIntegrityCheck()
        startEnvironmentCheck()
        startTimingAttackProtection()
    }
    
    /**
     * Detect if a debugger is attached
     */
    private fun startDebuggerDetection() {
        protectionScope.launch {
            while (isProtectionActive) {
                try {
                    // Check for debugger via JVM arguments
                    val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
                    val jvmArgs = runtimeMXBean.inputArguments
                    
                    val suspiciousArgs = listOf(
                        "-agentlib:jdwp",
                        "-Xdebug",
                        "-Xrunjdwp",
                        "-javaagent",
                        "-XX:+UnlockDiagnosticVMOptions",
                        "-XX:+LogVMOutput",
                        "-XX:+TraceClassLoading"
                    )
                    
                    for (arg in jvmArgs) {
                        for (suspicious in suspiciousArgs) {
                            if (arg.contains(suspicious, ignoreCase = true)) {
                                handleTamperingDetected("Debugger detected: $arg")
                                return@launch
                            }
                        }
                    }
                    
                    // Check for debugging via thread count
                    val threadCount = Thread.activeCount()
                    if (threadCount > 50) { // Suspicious thread count
                        handleTamperingDetected("Suspicious thread count: $threadCount")
                        return@launch
                    }
                    
                    // Check for debugging via timing
                    val startTime = System.nanoTime()
                    Thread.sleep(1)
                    val endTime = System.nanoTime()
                    val duration = (endTime - startTime) / 1_000_000 // Convert to milliseconds
                    
                    if (duration > 100) { // If sleep took too long, might be debugged
                        handleTamperingDetected("Timing anomaly detected: ${duration}ms")
                        return@launch
                    }
                    
                } catch (e: Exception) {
                    // Silently continue - don't reveal protection mechanisms
                }
                
                delay(5000) // Check every 5 seconds
            }
        }
    }
    
    /**
     * Check application integrity
     */
    private fun startIntegrityCheck() {
        protectionScope.launch {
            while (isProtectionActive) {
                try {
                    // Get current JAR file
                    val jarPath = AntiTamperingProtection::class.java.protectionDomain
                        .codeSource.location.toURI().path
                    
                    val jarFile = File(jarPath)
                    if (jarFile.exists() && jarFile.isFile) {
                        // Calculate checksum
                        val checksum = calculateFileChecksum(jarFile)
                        
                        // Store expected checksum (in real implementation, this would be encrypted/obfuscated)
                        val expectedChecksum = getExpectedChecksum()
                        
                        if (expectedChecksum.isNotEmpty() && checksum != expectedChecksum) {
                            handleTamperingDetected("File integrity violation")
                            return@launch
                        }
                    }
                    
                } catch (e: Exception) {
                    // Silently continue
                }
                
                delay(30000) // Check every 30 seconds
            }
        }
    }
    
    /**
     * Check for suspicious environment
     */
    private fun startEnvironmentCheck() {
        protectionScope.launch {
            while (isProtectionActive) {
                try {
                    // Check for virtual machine indicators
                    val vmIndicators = listOf(
                        "java.vm.name" to listOf("HotSpot", "OpenJDK"),
                        "os.name" to listOf("Windows", "Mac", "Linux")
                    )
                    
                    // Check for suspicious system properties
                    val suspiciousProps = listOf(
                        "idea.launcher.port",
                        "idea.launcher.bin.path",
                        "eclipse.launcher",
                        "netbeans.home"
                    )
                    
                    for (prop in suspiciousProps) {
                        if (System.getProperty(prop) != null) {
                            handleTamperingDetected("IDE environment detected: $prop")
                            return@launch
                        }
                    }
                    
                    // Check for debugging tools in classpath
                    val classpath = System.getProperty("java.class.path", "")
                    val suspiciousJars = listOf(
                        "idea", "eclipse", "netbeans", "jdb", "jconsole",
                        "visualvm", "jprofiler", "yourkit", "jrebel"
                    )
                    
                    for (jar in suspiciousJars) {
                        if (classpath.contains(jar, ignoreCase = true)) {
                            handleTamperingDetected("Debugging tool detected in classpath: $jar")
                            return@launch
                        }
                    }
                    
                } catch (e: Exception) {
                    // Silently continue
                }
                
                delay(15000) // Check every 15 seconds
            }
        }
    }
    
    /**
     * Protection against timing attacks
     */
    private fun startTimingAttackProtection() {
        protectionScope.launch {
            while (isProtectionActive) {
                try {
                    // Add random delays to make timing analysis harder
                    val randomDelay = (100..1000).random()
                    delay(randomDelay.toLong())
                    
                    // Perform dummy operations to confuse timing analysis
                    val dummy = (1..100).map { it * 2 }.sum()
                    
                } catch (e: Exception) {
                    // Silently continue
                }
                
                delay(10000) // Every 10 seconds
            }
        }
    }
    
    /**
     * Calculate file checksum
     */
    private fun calculateFileChecksum(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = file.readBytes()
            val hash = digest.digest(bytes)
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Get expected checksum (in production, this should be encrypted/obfuscated)
     */
    private fun getExpectedChecksum(): String {
        // In real implementation, this would be encrypted and stored securely
        // For now, return empty to skip integrity check
        return ""
    }
    
    /**
     * Handle tampering detection
     */
    private fun handleTamperingDetected(reason: String) {
        try {
            // Log the incident (in production, send to secure logging service)
            println("Security violation detected: $reason")
            
            // Perform cleanup
            cleanup()
            
            // Exit application
            exitProcess(1)
            
        } catch (e: Exception) {
            // Force exit even if cleanup fails
            Runtime.getRuntime().halt(1)
        }
    }
    
    /**
     * Cleanup sensitive data before exit
     */
    private fun cleanup() {
        try {
            // Clear sensitive data from memory
            System.gc()
            
            // Cancel all protection coroutines
            protectionScope.cancel()
            
            // Mark protection as inactive
            isProtectionActive = false
            
        } catch (e: Exception) {
            // Silently continue with exit
        }
    }
    
    /**
     * Disable protection (for testing purposes only)
     */
    fun disableProtection() {
        isProtectionActive = false
        protectionScope.cancel()
    }
    
    /**
     * Check if protection is active
     */
    fun isActive(): Boolean = isProtectionActive
}