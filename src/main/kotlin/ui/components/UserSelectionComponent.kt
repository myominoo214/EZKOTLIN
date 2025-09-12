package ui.components

import core.services.ApiService
import core.services.UserSession
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import data.models.*
import core.config.CompactOutlinedTextField

@Serializable
data class UsersApiResponse(
    val success: Boolean? = null,
    val data: UsersApiResponseData? = null,
    val message: String? = null,
    // Alternative response structure - direct array
    val users: List<UserData>? = null,
    // Alternative response structure - different field names
    val result: UsersApiResponseData? = null,
    val status: String? = null
)

@Serializable
data class PaginationData(
    val current: Int? = null,
    val limit: Int? = null,
    val total: Int? = null
)

// Function to fetch users from API
fun fetchUsers(
    scope: CoroutineScope,
    apiService: ApiService,
    onUsersLoaded: (List<UserOption>) -> Unit,
    onUserSelected: (String) -> Unit
) {
    scope.launch {
        try {
            println("[DEBUG] Starting fetchUsers API call...")
            val userSession = UserSession.getInstance()
            val response = apiService.get<UsersApiResponse>(
                url = "${ApiService.BASE_URL}/v1/account/getUserLists?current=1&limit=1000",
                headers = userSession.getAuthHeaders()
            )
            println("[DEBUG] API Response - Success: ${response.success}, Data: ${response.data}")
            println("[DEBUG] Raw response data structure: ${response.data?.data?.by}")
            if (response.data?.data?.by?.isNotEmpty() == true) {
                println("[DEBUG] First user object: ${response.data?.data?.by?.get(0)}")
            }
            // Check if we have a successful response with data
            if (response.success != false && response.data?.data?.by != null) {
                val apiUsers = response.data?.data?.by ?: emptyList()
                val userList = mutableListOf<UserOption>()
                // Add "All" option at the beginning
                userList.add(UserOption("", "All", ""))
                // Add users from API
                userList.addAll(apiUsers.map { userData ->
                    val userId = userData.userId ?: userData.id ?: 0
                    val userName = userData.userName ?: userData.name ?: userData.username ?: userData.email ?: "Unknown User"
                    val userType = userData.userType ?: userData.type ?: "user"
                    UserOption(
                        value = userId.toString(),
                        label = userName,
                        userType = userType
                    )
                })
                onUsersLoaded(userList)
                onUserSelected(userList.firstOrNull()?.value ?: "")
                println("[DEBUG] Successfully loaded ${userList.size} users from API")
            } else {
                println("[DEBUG] API call failed or no data, using fallback users. Message: ${response.message}")
                // Fallback to sample users if API fails
                val fallbackUsers = listOf(
                    UserOption("", "All"),
                    UserOption("1", "User 1", "user"),
                    UserOption("2", "Agent 1", "agent")
                )
                onUsersLoaded(fallbackUsers)
                onUserSelected(fallbackUsers.firstOrNull()?.value ?: "")
            }
        } catch (e: Exception) {
            println("[DEBUG] Exception in fetchUsers: ${e.message}")
            e.printStackTrace()
            // Fallback to sample users if API fails
            val fallbackUsers = listOf(
                UserOption("", "All"),
                UserOption("1", "User 1", "user"),
                UserOption("2", "Agent 1", "agent")
            )
            onUsersLoaded(fallbackUsers)
            onUserSelected(fallbackUsers.firstOrNull()?.value ?: "")
        }
    }
}

@Composable
fun UserSelectionDialog(
    users: List<UserOption>,
    selectedUser: UserOption?,
    onUserSelected: (UserOption) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var tempSelectedUser by remember { mutableStateOf(selectedUser) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select User",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMessage != null -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onDismiss) {
                                Text("Retry")
                            }
                        }
                    }
                    else -> {
                        // User list
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(users) { index, user ->
                                val backgroundColor = if (index % 2 == 0) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                                val isSelected = tempSelectedUser?.value == user.value
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(backgroundColor)
                                        .clickable {
                                            tempSelectedUser = user
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            tempSelectedUser = user
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = user.label,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        if (user.userType.isNotEmpty()) {
                                            Text(
                                                text = user.userType,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    tempSelectedUser?.let { onUserSelected(it) }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = tempSelectedUser != null
                            ) {
                                Text("Select")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSelectionDropdown(
    users: List<UserOption>,
    selectedUser: String,
    onUserSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            CompactOutlinedTextField(
                value = users.find { it.value == selectedUser }?.label ?: "",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth()
                    .height(56.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                users.forEach { user ->
                    DropdownMenuItem(
                        text = { Text(user.label, fontSize = 14.sp) },
                        onClick = {
                            onUserSelected(user.value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}