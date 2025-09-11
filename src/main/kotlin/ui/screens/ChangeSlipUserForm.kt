package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import core.services.ApiService
import core.services.UserSession
import data.models.ApiResponse
import core.config.CompactOutlinedTextField

@Serializable
data class Agent(
    val label: String,
    val value: String,
    val userType: String? = null
)

@Serializable
data class UpdateSlipUserResponse(
    val newSlipId: Int
)

@Serializable
data class UpdateSlipUserPayload(
    val slipId: String,
    val termId: String,
    val userId: String,
    val uid: String,
    val userType: String?
)

@Serializable
data class SlipData(
    val slipId: String,
    val userId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeSlipUserForm(
    agentList: List<Agent>,
    defaultUser: String?,
    termId: String,
    slip: SlipData,
    onRefreshSlip: () -> Unit,
    onCloseModal: () -> Unit
) {
    var agents by remember { mutableStateOf<List<Agent>>(emptyList()) }
    var selectedAgent by remember { mutableStateOf(defaultUser ?: "") }
    var loading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize agents list
    LaunchedEffect(agentList) {
        if (agentList.isNotEmpty()) {
            val filteredAgents = agentList.filter { it.value.isNotEmpty() }
            agents = filteredAgents
            if (defaultUser != null) {
                selectedAgent = defaultUser
            }
        }
    }

    // Show snackbar when needed
    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(
                message = snackbarMessage,
                duration = SnackbarDuration.Short
            )
            showSnackbar = false
        }
    }

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    val handleFormSubmit: () -> Unit = {
        scope.launch {
            try {
                loading = true
                val selectedAgentData = agentList.find { it.value == selectedAgent }
                
                val payload = UpdateSlipUserPayload(
                    slipId = slip.slipId,
                    termId = termId,
                    userId = selectedAgent,
                    uid = slip.userId,
                    userType = selectedAgentData?.userType
                )

                val response = client.put("${ApiService.BASE_URL}/v1/slip/updateUserChangeBySlipId") {
                    contentType(ContentType.Application.Json)
                    UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                        header(key, value)
                    }
                    setBody(payload)
                }

                val apiResponse = response.body<ApiResponse<UpdateSlipUserResponse>>()
                
                if (apiResponse.code == "200") {
                    onRefreshSlip()
                    snackbarMessage = apiResponse.message.orEmpty().ifEmpty { "Updated successfully" }
                    isError = false
                } else {
                    snackbarMessage = apiResponse.message.orEmpty().ifEmpty { "Update failed" }
                    isError = true
                }
                showSnackbar = true
                onCloseModal()
            } catch (e: Exception) {
                snackbarMessage = "Error: ${e.message}"
                isError = true
                showSnackbar = true
            } finally {
                loading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Agent List",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Agent Selection
            if (agents.isNotEmpty() && selectedAgent.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    CompactOutlinedTextField(
                        value = agents.find { it.value == selectedAgent }?.label ?: "Select an agent",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Select Agent") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        agents.forEach { agent ->
                            DropdownMenuItem(
                                text = { Text(agent.label) },
                                onClick = {
                                    selectedAgent = agent.value
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No agents available.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Update Button
            Button(
                onClick = handleFormSubmit,
                enabled = !loading && selectedAgent.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Update",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}