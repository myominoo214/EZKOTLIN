package core.models

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String? = null,
    val mandatory: Boolean = false,
    val minSupportedVersion: String? = null
)

@Serializable
data class VersionInfo(
    val major: Int,
    val minor: Int,
    val patch: Int
) {
    companion object {
        fun fromString(version: String): VersionInfo {
            val parts = version.split(".")
            return VersionInfo(
                major = parts.getOrNull(0)?.toIntOrNull() ?: 0,
                minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
                patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            )
        }
    }
    
    override fun toString(): String = "$major.$minor.$patch"
    
    fun isNewerThan(other: VersionInfo): Boolean {
        return when {
            major > other.major -> true
            major < other.major -> false
            minor > other.minor -> true
            minor < other.minor -> false
            patch > other.patch -> true
            else -> false
        }
    }
}