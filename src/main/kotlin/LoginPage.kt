import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.coroutines.delay
import java.net.InetAddress
import core.services.ApiService
import core.services.SettingsApiResponse
import core.services.UserSession
import core.services.UserProfileApiResponse
import java.io.File
import core.config.CompactOutlinedTextField

@Serializable
data class LoginRequest(
    val accountKey: String,
    val password: String,
    val deviceName: String
)

@Serializable
data class LoginResponse(
    val code: Int,
    val message: String? = null,
    val data: LoginData? = null
)

@Serializable
data class LoginData(
    val token: String? = null
)



fun getDeviceInfo(): String {
    return try {
        val hostname = InetAddress.getLocalHost().hostName
        val serialNumber = getSerialNumber()
        "$serialNumber-$hostname"
    } catch (e: Exception) {
        "Unknown-Device-${System.currentTimeMillis()}"
    }
}

fun getSerialNumber(): String {
    return try {
        // Try to get macOS serial number
        val process = ProcessBuilder("system_profiler", "SPHardwareDataType")
            .redirectErrorStream(true)
            .start()
        
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        
        val serialRegex = "Serial Number \\(system\\): (.+)".toRegex()
        val matchResult = serialRegex.find(output)
        matchResult?.groupValues?.get(1)?.trim() ?: "Unknown-Serial"
    } catch (e: Exception) {
        "Unknown-Serial"
    }
}

@Composable
fun LoginPage(
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService() }
    val snackbarHostState = remember { SnackbarHostState() }
    val userSession = remember { UserSession.getInstance() }
    
    fun performLogin() {
        when {
            username.isEmpty() -> errorMessage = "Please enter account key"
            password.isEmpty() -> errorMessage = "Please enter password"
            isLoading -> return
            else -> {
                isLoading = true
                errorMessage = ""
                
                coroutineScope.launch {
                    try {
                        val loginRequest = LoginRequest(
                            accountKey = username,
                            password = password,
                            deviceName = getDeviceInfo()
                        )
                        
                        val response = apiService.post<LoginRequest, LoginResponse>(
                            url = "${ApiService.BASE_URL}/v1/account/login",
                            body = loginRequest
                        )
                        
                        if (response.data?.code == 200) {
                            // Store login response data in session
                            response.data?.data?.token?.let { token ->
                                userSession.login(
                                    token = token,
                                    accountKey = username,
                                    deviceName = getDeviceInfo()
                                )
                                
                                // Call getUserProfile API after successful login
                                try {
                                    val profileResponse = apiService.get<UserProfileApiResponse>(
                                        url = "${ApiService.BASE_URL}/v1/account/getUserProfile",
                                        headers = userSession.getAuthHeaders()
                                    )
                                    
                                    if (profileResponse.data?.code == 200 && profileResponse.data?.data != null) {
                                        userSession.setUserProfile(profileResponse.data.data)
                                        //println("[DEBUG] User profile loaded successfully: ${profileResponse.data.data}")
                                    } else {
                                        //println("[DEBUG] Failed to load user profile: ${profileResponse.data?.message}")
                                    }
                                } catch (e: Exception) {
                                    println("[DEBUG] Error loading user profile: ${e.message}")
                                    // Don't fail login if profile loading fails
                                }
                                
                                // Call getSettingByUserId API after successful login
                                try {
                                    val settingsResponse = apiService.get<SettingsApiResponse>(
                                        url = "${ApiService.BASE_URL}/v1/account/getSettingByUserId",
                                        headers = userSession.getAuthHeaders()
                                    )
                                    
                                    if (settingsResponse.data?.code == "200" && settingsResponse.data?.data != null) {
                                        userSession.setSettings(settingsResponse.data.data)
                                        println("[DEBUG] User settings loaded successfully")
                                    } else {
                                        println("[DEBUG] Failed to load user settings - Code: ${settingsResponse.data?.code}, Message: ${settingsResponse.data?.message}")
                                    }
                                } catch (e: Exception) {
                                    println("[DEBUG] Error loading user settings: ${e.message}")
                                    // Don't fail login if settings loading fails
                                }
                            }
                            onLoginSuccess()
                        } else {
                            errorMessage = response.data?.message ?: "Login failed"
                            showSnackbar = true
                        }
                    } catch (e: Exception) {
                        errorMessage = "Login failed: ${e.message}"
                        showSnackbar = true
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }
    
    // Auto-focus username field on start
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100) // Small delay to ensure UI is composed
        usernameFocusRequester.requestFocus()
    }
    
    // Show snackbar when there's an error
    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
            showSnackbar = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                CompactOutlinedTextField(
                    value = username,
                    onValueChange = { 
                        username = it
                    },
                    label = { Text("အမည်") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(usernameFocusRequester),
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    )
                )

                CompactOutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                    },
                    label = { Text("စကားဝှက်") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester),
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { performLogin() }
                    ),
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Filled.Star
                        else Icons.Filled.Lock

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = "Toggle password visibility")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { performLogin() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Login", fontSize = 16.sp)
                    }
                }
            }
        }
        }
    }
}