package core.services

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.io.IOException

// Import SettingsData from LoginPage
@Serializable
data class SettingsData(
    val enablePrinting: String = "0",
    val enableSong: String = "0",
    val isTwoColumn: String = "0",
    val printWidth: String = "0",
    val showBusinessName: String = "0",
    val showEmployeeName: String = "0",
    val showTermName: String = "0",
    val showPrintTime: String = "0"
) {
    // Helper properties to convert string values to boolean
    val enablePrintingBool: Boolean get() = enablePrinting == "1"
    val enableSongBool: Boolean get() = enableSong == "1"
    val isTwoColumnBool: Boolean get() = isTwoColumn == "1"
    val showBusinessNameBool: Boolean get() = showBusinessName == "1"
    val showEmployeeNameBool: Boolean get() = showEmployeeName == "1"
    val showTermNameBool: Boolean get() = showTermName == "1"
    val showPrintTimeBool: Boolean get() = showPrintTime == "1"
    
    // Helper property to extract numeric value from printWidth
    val printWidthNumeric: Int get() = printWidth.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
}

@Serializable
data class SettingsApiResponse(
    val code: String,
    val message: String? = null,
    val data: SettingsData? = null
)

@Serializable
data class UserProfileData(
    val userId: Int? = null,
    val name: String? = null,
    val profile: String? = null,
    val phoneNumber: String? = null,
    val suspended: Int? = null,
    val clientId: String? = null,
    val businessId: String? = null,
    val discount2D: Int? = null,
    val discount3D: Int? = null,
    val userType: String? = null,
    val shareId: Int? = null,
    val prize2D: Int? = null,
    val prize3D: Int? = null,
    val tPrize: Int? = null,
    val hotBreak: Int? = null,
    val hotPercentage: String? = null,
    val unitPrice: Int? = null,
    val secretKey: String? = null,
    val shopName: String? = null,
    val alertMessage: String? = null,
    val breakLimit: Int? = null,
    val betType: String? = null,
    val phoneNumber1: String? = null,
    val phoneNumber2: String? = null,
    val phoneNumber3: String? = null,
    val lastLoggedDate: String? = null,
    val createdDate: String? = null,
    val last_updated_at: String? = null,
    val hotBreak3D: Int? = null,
    val hotPercentage3D: String? = null,
    val breakLimit3D: Int? = null,
    val auto: String? = null,
    val inviteKey: String? = null,
    val apkUrl: String? = null,
    val useWallet: String? = null,
    val userAccess: String? = null,
    val visionCount: Int? = null,
    val breakAccess: String? = null,
    val hotAccess: String? = null,
    val eUniqueSlipId: String? = null,
    val extraPercent: Int? = null,
    val deviceLimit: Int? = null,
    val callbackUrl: String? = null,
    val appUserId: String? = null,
    val businessName: String? = null
)

@Serializable
data class UserProfileApiResponse(
    val code: Int? = null,
    val message: String? = null,
    val data: UserProfileData? = null
)

@Serializable
data class UserSessionData(
    val token: String,
    val accountKey: String,
    val deviceName: String,
    val loginTime: Long = System.currentTimeMillis()
)

@Serializable
data class LocalSettingsData(
    val footerText: String = "",
    val fontSize: String = "12",
    val selectedPrinter: String = "Default Printer",
    val printWidth: Int = 58,
    val showSummary: Boolean = false,
    val sendSMS: Boolean = false,
    val isPrintingEnabled: Boolean = false,
    val isAllowExtra: Boolean = false
)

class UserSession {
    private var _sessionData: UserSessionData? = null
    private var _userProfileData: UserProfileData? = null
    private var _settingsData: SettingsData? = null
    private var _localSettingsData: LocalSettingsData = LocalSettingsData()
    
    val sessionData: UserSessionData?
        get() = _sessionData
    
    val userProfileData: UserProfileData?
        get() = _userProfileData
    
    val settingsData: SettingsData?
        get() = _settingsData
    
    val localSettingsData: LocalSettingsData
        get() = _localSettingsData
    
    val isLoggedIn: Boolean
        get() = _sessionData != null
    
    val authToken: String?
        get() = _sessionData?.token
    
    fun login(token: String, accountKey: String, deviceName: String) {
        _sessionData = UserSessionData(
            token = token,
            accountKey = accountKey,
            deviceName = deviceName
        )
    }
    
    fun setUserProfile(userProfileData: UserProfileData) {
        _userProfileData = userProfileData
    }
    
    fun setSettings(settingsData: SettingsData) {
        _settingsData = settingsData
    }
    
    fun setLocalSettings(localSettingsData: LocalSettingsData) {
        _localSettingsData = localSettingsData
        saveLocalSettings()
    }
    
    fun updateFooterText(footerText: String) {
        _localSettingsData = _localSettingsData.copy(footerText = footerText)
        saveLocalSettings()
    }
    
    fun updateFontSize(fontSize: String) {
        _localSettingsData = _localSettingsData.copy(fontSize = fontSize)
        saveLocalSettings()
    }
    
    fun updateSelectedPrinter(selectedPrinter: String) {
        _localSettingsData = _localSettingsData.copy(selectedPrinter = selectedPrinter)
        saveLocalSettings()
    }
    
    fun updatePrintWidth(printWidth: Int) {
        _localSettingsData = _localSettingsData.copy(printWidth = printWidth)
        saveLocalSettings()
    }
    
    fun updateShowSummary(showSummary: Boolean) {
        _localSettingsData = _localSettingsData.copy(showSummary = showSummary)
        saveLocalSettings()
    }
    
    fun updateSendSMS(sendSMS: Boolean) {
        _localSettingsData = _localSettingsData.copy(sendSMS = sendSMS)
        saveLocalSettings()
    }
    
    fun updateIsPrintingEnabled(isPrintingEnabled: Boolean) {
        _localSettingsData = _localSettingsData.copy(isPrintingEnabled = isPrintingEnabled)
        saveLocalSettings()
    }
    
    fun updateIsAllowExtra(isAllowExtra: Boolean) {
        _localSettingsData = _localSettingsData.copy(isAllowExtra = isAllowExtra)
        saveLocalSettings()
    }
    
    private fun saveLocalSettings() {
        try {
            val json = Json.encodeToString(_localSettingsData)
            File(LOCAL_SETTINGS_FILE).writeText(json)
        } catch (e: IOException) {
            println("Error saving local settings: ${e.message}")
        }
    }
    
    private fun loadLocalSettings() {
        try {
            val file = File(LOCAL_SETTINGS_FILE)
            if (file.exists()) {
                val json = file.readText()
                _localSettingsData = Json.decodeFromString<LocalSettingsData>(json)
            }
        } catch (e: Exception) {
            println("Error loading local settings: ${e.message}")
            _localSettingsData = LocalSettingsData() // Use default if loading fails
        }
    }
    
    fun logout() {
        _sessionData = null
        _userProfileData = null
        _settingsData = null
        // Keep _localSettingsData unchanged to persist until app restart
    }
    
    fun getAuthHeaders(): Map<String, String> {
        return if (authToken != null) {
            mapOf("Authorization" to "Bearer $authToken")
        } else {
            emptyMap()
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: UserSession? = null
        private const val LOCAL_SETTINGS_FILE = "local_settings.json"
        
        fun getInstance(): UserSession {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserSession().also { 
                    INSTANCE = it
                    it.loadLocalSettings()
                }
            }
        }
    }
}