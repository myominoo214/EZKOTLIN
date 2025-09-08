package ui.screens

import data.models.UserTableData
import data.models.UserFormData
import data.models.UserData
import data.models.UsersListResponse
import data.models.displayHotBreakPercentage
import data.models.displayAgentName
import data.models.displayUserType
import core.services.UserApiService
import presentation.viewmodels.UserFormViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.getValue
import io.ktor.client.HttpClient
import data.models.UserOption
import ui.components.fetchUsers
import data.models.UsersPageState
import core.services.ApiService
import core.services.UserSession
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersContent() {
    var pageState by remember { mutableStateOf(UsersPageState()) }
    var showUserForm by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<UserTableData?>(null) }
    
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiService() }
    val httpClient = remember { HttpClient() }
    val userApiService = remember { UserApiService(httpClient) }
    val viewModel = remember { UserFormViewModel(userApiService, scope) }
    val clipboardManager = LocalClipboardManager.current
    
    // Fetch users data
    fun fetchUsersData() {
        scope.launch {
            pageState = pageState.copy(isLoading = true, errorMessage = null)
            try {
                val userSession = UserSession.getInstance()
                val response = apiService.get<UsersListResponse>(
                    url = "${ApiService.BASE_URL}/v1/account/getUserLists?keyword=${pageState.searchText}&current=1&limit=10000",
                    headers = userSession.getAuthHeaders()
                )
                println("getUserList response: $response")
                // Debug: Print raw JSON response
                try {
                    val rawResponse = apiService.getString(
                        url = "${ApiService.BASE_URL}/v1/account/getUserLists?keyword=${pageState.searchText}&current=1&limit=10000",
                        headers = userSession.getAuthHeaders()
                    )
                    println("[DEBUG] Raw JSON response: $rawResponse")
                } catch (e: Exception) {
                    println("[DEBUG] Failed to get raw response: ${e.message}")
                }
                // Debug: Print first user's raw data if available
                if (response.success && response.data?.data?.by?.isNotEmpty() == true) {
                    val firstUser = response.data.data.by.first()
                    println("[DEBUG] First user raw data: $firstUser")
                    println("[DEBUG] First user discount2d: ${firstUser.discount2D}")
                    println("[DEBUG] First user phone: ${firstUser.phone}")
                }
                if (response.success && response.data != null) {
                    val usersData = response.data!!
                    if (usersData.code == "200") {
                        pageState = pageState.copy(
                            users = usersData.data.by,
                            pagination = usersData.data.pagination,
                            isLoading = false
                        )
                    } else {
                        pageState = pageState.copy(
                            isLoading = false,
                            errorMessage = "Failed to load users: ${usersData.message}"
                        )
                    }
                } else {
                    pageState = pageState.copy(
                        isLoading = false,
                        errorMessage = "Failed to load users: ${response.message ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                pageState = pageState.copy(
                    isLoading = false,
                    errorMessage = "Error loading users: ${e.message}"
                )
            }
        }
    }
    
    // Load users on component mount and when search/pagination changes
    LaunchedEffect(pageState.searchText, pageState.currentPage, pageState.pageLimit) {
        fetchUsersData()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Header with Add User button and item count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ထိုးသား",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "စုစုပေါင်း ${pageState.pagination.total} ယောက်",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(
                onClick = {
                    selectedUser = null
                    showUserForm = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add User",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add User")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Error message
        pageState.errorMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Data table
        if (pageState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            UsersDataTable(
                users = pageState.users,
                onEditUser = { user ->
                    selectedUser = user
                    showUserForm = true
                },
                onDeleteUser = { user ->
                    scope.launch {
                        try {
                            viewModel.deleteUser(user.id)
                            fetchUsersData() // Refresh the list
                        } catch (e: Exception) {
                            pageState = pageState.copy(
                                errorMessage = "Failed to delete user: ${e.message}"
                            )
                        }
                    }
                },
                onCopyToClipboard = { text ->
                    clipboardManager.setText(AnnotatedString(text))
                }
            )
        }
    }
    
    // User Form Dialog
    if (showUserForm) {
        Dialog(
            onDismissRequest = { showUserForm = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                UserForm(
                    merchantDetail = selectedUser?.let { user ->
                        UserData(
                            id = user.id.toIntOrNull(),
                            name = user.name,
                            userType = user.userType
                        )
                    },
                    onClose = {
                        showUserForm = false
                        fetchUsersData() // Refresh the list
                    },
                    userApiService = userApiService,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun UsersDataTable(
    users: List<UserTableData>,
    onEditUser: (UserTableData) -> Unit,
    onDeleteUser: (UserTableData) -> Unit,
    onCopyToClipboard: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Name
                Text(
                    text = "အမည်",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1.5f)
                )
                
                // 2D-3D Discount
                Text(
                    text = "ကော်",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                
                // 2D-3D Prize
                Text(
                    text = "အဆ",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                
                // Phone
                Text(
                    text = "ဖုန်းနံပါတ်",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1.2f)
                )
                
                // 2D Hot
                Text(
                    text = "2D Hot (B-%)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                
                // 3D Hot
                Text(
                    text = "3D Hot (B-%)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                
                // Agent
                Text(
                    text = "Agent",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center
                )
                
                // User Type
                Text(
                    text = "အမျိုးအစား",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1.2f),
                    textAlign = TextAlign.Center
                )
                
                // Actions
                Text(
                    text = "Actions",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
            
            Divider()
            
            // Table Content
            if (users.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No users found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    items(users) { user ->
                        UserTableRow(
                            user = user,
                            onEdit = { onEditUser(user) },
                            onDelete = { onDeleteUser(user) },
                            onCopyToClipboard = onCopyToClipboard
                        )
                        if (user != users.last()) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserTableRow(
    user: UserTableData,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyToClipboard: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name
        Text(
            text = user.name,
            modifier = Modifier
                .weight(1.5f)
                .clickable { onCopyToClipboard(user.name) },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
        
        // 2D-3D Discount
        Text(
            text = "${user.discount2D}-${user.discount3D}",
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // 2D-3D Prize
        Text(
            text = "${user.prize2D}-${user.prize3D}",
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // Phone
        Text(
            text = user.phone?.ifEmpty { "-" } ?: "-",
            modifier = Modifier
                .weight(1.2f)
                .clickable { user.phone?.let { if (it.isNotEmpty()) onCopyToClipboard(it) } },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 2D Hot (B-%)
        Text(
            text = displayHotBreakPercentage(user.hotBreak2D, user.hotPercentage2D),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // 3D Hot (B-%)
        Text(
            text = displayHotBreakPercentage(user.hotBreak3D, user.hotPercentage3D),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // Agent
        Text(
            text = displayAgentName(user.agentName, user.userType),
            modifier = Modifier.weight(1.2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // User Type
        Text(
            text = displayUserType(user.userType, user.inviteKey),
            modifier = Modifier.weight(1.2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        // Actions
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit User",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete User",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}