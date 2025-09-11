package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ui.screens.Ledger
import ui.screens.ViewForm
import ui.screens.OptionUI
import ui.components.TermSelectionDropdown
import data.models.TermOption
import data.models.UserOption as ComponentUserOption
import ui.components.fetchUsers
import core.services.ApiService
import core.services.UserSession
import kotlinx.coroutines.launch
import data.models.TermsApiResponse

@Composable
fun LedgerContent() {
    var userList by remember { mutableStateOf<List<ComponentUserOption>>(emptyList()) }
    var selectedTerm by remember { mutableStateOf<TermOption?>(null) }
    var refreshTermsTrigger by remember { mutableStateOf(0) }
    var showViewModal by remember { mutableStateOf(false) }
    var showOptionsModal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiService() }
    
    // Convert ComponentUserOption to UserOption
    val convertedUserList = remember(userList) {
        userList.map { componentUser ->
            ComponentUserOption(
                value = componentUser.value,
                label = componentUser.label
            )
        }
    }
    
    // Function to fetch active terms
    val onFetchActiveTerms: () -> Unit = {
        refreshTermsTrigger++
    }
    
    // Fetch users on component initialization
    LaunchedEffect(Unit) {
        fetchUsers(
            scope = scope,
            apiService = apiService,
            onUsersLoaded = { users ->
                userList = users
            },
            onUserSelected = { /* Not used in this context */ }
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TermSelectionComponent
        TermSelectionComponent(
            selectedTerm = selectedTerm,
            onTermSelected = { selectedTerm = it },
            refreshTrigger = refreshTermsTrigger,
            modifier = Modifier.weight(1f)
        )
        
        // LedgerView
         Box(
             modifier = Modifier.weight(1.5f)
         ) {
             Ledger(
                 termId = selectedTerm?.termId?.toString() ?: "",
                 termName = selectedTerm?.termName ?: "No Term Selected",
                 apiBreakAmount = selectedTerm?.breakAmount ?: 0,
                 is2D = selectedTerm?.is2D ?: false,
                 userList = convertedUserList,
                 onFetchActiveTerms = onFetchActiveTerms,
                 onSetLedgerData = { },
                 onRefreshLedgerParent = { },
                 onNavigateToBuy = { },
                 onShowViewModal = { showViewModal = it },
                 onShowOptionsModal = { showOptionsModal = it }
             )
         }
         Box(
             modifier = Modifier.weight(1.5f)
         ) {
         }
    }
    
    // View Modal - rendered outside the Row layout to avoid flex constraints
    if (showViewModal) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showViewModal = false }
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.98f)
                    .fillMaxHeight(1f)
                    .clickable(enabled = false) { },
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                ViewForm(
                    is2D = selectedTerm?.is2D ?: false,
                    breakAmount = selectedTerm?.breakAmount ?: 0,
                    termId = selectedTerm?.termId?.toString() ?: "",
                    termName = selectedTerm?.termName ?: "No Term Selected",
                    setBreakAmount = { },
                    handleRefreshLedger = { refreshTermsTrigger++ }
                )
            }
        }
    }
    
    // Options Modal
    if (showOptionsModal) {
        Dialog(
            onDismissRequest = { showOptionsModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .widthIn(min = 300.dp, max = 600.dp)
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                OptionUI(
                    termId = selectedTerm?.termId?.toString() ?: "",
                    breakAmount = selectedTerm?.breakAmount ?: 0,
                    onCloseModal = { showOptionsModal = false }
                )
            }
        }
    }
}

@Composable
fun TermSelectionComponent(
    selectedTerm: TermOption?,
    onTermSelected: (TermOption?) -> Unit,
    refreshTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    var termOptions by remember { mutableStateOf<List<TermOption>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Function to fetch terms
    suspend fun fetchTerms() {
        isLoading = true
        errorMessage = null
        try {
            val apiService = ApiService()
            val userSession = UserSession.getInstance()
            val response = apiService.get<TermsApiResponse>(
                url = "${ApiService.BASE_URL}/v1/term/getTerms?current=1&limit=1000",
                headers = userSession.getAuthHeaders()
            )
            if (response.success && response.data != null) {
                val activeTerms = response.data.data.by.filter { it.isFinished == "0" }
                termOptions = activeTerms.map { termData ->
                    TermOption(
                        termId = termData.termId,
                        termName = termData.termName,
                        shortName = termData.shortName,
                        groupId = termData.groupId,
                        startDate = termData.startDate,
                        endDate = termData.endDate,
                        isFinished = termData.isFinished,
                        termType = termData.termType,
                        winNum = termData.winNum,
                        is2D = termData.is2D,
                        unitPrice = termData.unitPrice,
                        breakAmount = termData.breakAmount
                    )
                }
            } else {
                errorMessage = response.message ?: "Failed to fetch terms"
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load terms: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    
    // Fetch terms on component initialization
    LaunchedEffect(Unit) {
        fetchTerms()
    }
    
    // Fetch terms when refresh trigger changes
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            fetchTerms()
        }
    }
    
    Card(
        modifier = modifier.fillMaxHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                TermSelectionDropdown(
                    termOptions = termOptions,
                    selectedTerm = selectedTerm,
                    onTermSelected = onTermSelected,
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoading,
                    errorMessage = errorMessage
                )
                
                if (selectedTerm != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Selected Term Details",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Name: ${selectedTerm!!.termName}")
                            Text("Type: ${selectedTerm!!.termType}")
                            Text("Period: ${selectedTerm!!.startDate} - ${selectedTerm!!.endDate}")
                            Text("Status: ${if (selectedTerm!!.isFinished == "0") "Active" else "Finished"}")
                        }
                    }
                }
            }
        }
    }
}