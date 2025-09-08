package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.Divider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*
import core.services.ApiService
import core.services.UserSession

@Serializable
data class Term(
    val termId: String,
    val termName: String,
    val isFinished: String,
    val startDate: String?,
    val endDate: String?,
    val groupId: String?,
    val termType: String?
)

@Serializable
data class CleanFileTermGroup(
    val groupId: String,
    val label: String,
    val terms: List<Term>,
    val maxEndDate: String?
)

@Serializable
data class CleanFileTermsApiResponseData(
    val by: List<Term>,
    val pagination: CleanFilePaginationData
)

@Serializable
data class CleanFilePaginationData(
    val current: Int,
    val limit: Int,
    val rowsPerPage: Int,
    val total: Int
)

@Serializable
data class CleanFileTermsApiResponse(
    val code: String,
    val status: String,
    val message: String,
    val data: CleanFileTermsApiResponseData
)

@Serializable
data class UpdateTermRequest(
    val termIds: String,
    val isFinished: String
)

@Serializable
data class DeleteTermRequest(
    val termId: String
)

class CleanFileRepository {
    private val apiService = ApiService()
    private val userSession = UserSession.getInstance()
    
    suspend fun getTerms(): List<Term> {
        return try {
            val response = apiService.get<CleanFileTermsApiResponse>(
                url = "${ApiService.BASE_URL}/v1/term/getTerms?keyword=&current=1&limit=10000000",
                headers = userSession.getAuthHeaders()
            )
            if (response.success && response.data != null) {
                response.data.data.by.filter { it.termType != "born" }
            } else {
                println("Error fetching terms: ${response.message}")
                emptyList()
            }
        } catch (e: Exception) {
            println("Error fetching terms: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun updateTermStatus(termIds: String, isFinished: String): Boolean {
        return try {
            val response = apiService.putForString(
                url = "${ApiService.BASE_URL}/v1/term/isFinished",
                body = mapOf(
                    "termIds" to termIds,
                    "isFinished" to isFinished
                ),
                headers = userSession.getAuthHeaders()
            )
            response.success
        } catch (e: Exception) {
            false
        }
    }
    
    
    suspend fun deleteTerm(termId: String): Boolean {
        return try {
            val request = DeleteTermRequest(termId = termId)
            val response = apiService.deleteWithBodyForString(
                url = "${ApiService.BASE_URL}/v1/term/cleanFileByTermId",
                body = request,
                headers = userSession.getAuthHeaders()
            )
            response.success
        } catch (e: Exception) {
            println("Error deleting term: ${e.message}")
            false
        }
    }
}

@Composable
fun CleanFileContent() {
    var terms by remember { mutableStateOf<List<Term>>(emptyList()) }
    var groupedTerms = remember { mutableStateListOf<CleanFileTermGroup>() }
    var loading by remember { mutableStateOf(false) }
    var loadingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var termToDelete by remember { mutableStateOf<Term?>(null) }
    var groupToDelete by remember { mutableStateOf<CleanFileTermGroup?>(null) }
    val optimisticUpdates = remember { mutableStateMapOf<String, String>() } // termId to isFinished status
 // Force recomposition when optimistic updates change
    var refreshCounter by remember { mutableStateOf(0) } // Force recomposition
    
    // Force UI recomposition by creating a simple state that changes with refreshCounter
    var uiForceUpdate by remember { mutableStateOf(0) }
    
    // Update uiForceUpdate whenever refreshCounter changes
    LaunchedEffect(refreshCounter) {
        uiForceUpdate++
        println("UI force update triggered: $uiForceUpdate")
    }
    
    val repository = remember { CleanFileRepository() }
    val scope = rememberCoroutineScope()
    
    // Load terms on first composition
    LaunchedEffect(Unit) {
        loading = true
        optimisticUpdates.clear() // Clear any existing optimistic updates
        try {
            val loadedTerms = repository.getTerms()
            terms = loadedTerms
            groupedTerms.clear()
            groupedTerms.addAll(groupTermsByDateRange(loadedTerms))
        } catch (e: Exception) {
            println("Error loading terms: ${e.message}")
        } finally {
            loading = false
        }
    }
    
    // Trigger recomposition when refreshCounter changes
    LaunchedEffect(refreshCounter) {
        if (refreshCounter > 0) {
            println("LaunchedEffect triggered for refreshCounter: $refreshCounter")
            // Force recomposition by updating a dummy state
            println("Refresh triggered successfully")
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Delete") },
            text = {
                Text(
                    when {
                        termToDelete != null -> "Are you sure you want to delete '${termToDelete!!.termName}'? This action cannot be undone!"
                        groupToDelete != null -> "Are you sure you want to delete all ${groupToDelete!!.terms.size} terms in '${groupToDelete!!.label}'? This action cannot be undone!"
                        else -> "Are you sure you want to delete this item?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            when {
                                termToDelete != null -> {
                                    if (repository.deleteTerm(termToDelete!!.termId)) {
                                        refreshTermsList(
                                            repository = repository,
                                            onTermsUpdate = { loadedTerms -> terms = loadedTerms },
                                            onGroupedTermsUpdate = { loadedGroupedTerms -> 
                                                groupedTerms.clear()
                                                groupedTerms.addAll(loadedGroupedTerms)
                                            },
                                            onLoadingUpdate = { isLoading -> loading = isLoading }
                                        )
                                        // Clear optimistic updates after successful refresh
                                                            optimisticUpdates.clear()
                                                            uiForceUpdate++ // Force recomposition
                                    }
                                }
                                groupToDelete != null -> {
                                    val termIds = groupToDelete!!.terms.map { it.termId }.joinToString(",")
                                    if (repository.deleteTerm(termIds)) {
                                        refreshTermsList(
                                            repository = repository,
                                            onTermsUpdate = { loadedTerms -> terms = loadedTerms },
                                            onGroupedTermsUpdate = { loadedGroupedTerms -> 
                                                groupedTerms.clear()
                                                groupedTerms.addAll(loadedGroupedTerms)
                                            },
                                            onLoadingUpdate = { isLoading -> loading = isLoading }
                                        )
                                        // Clear optimistic updates after successful refresh
                                        optimisticUpdates.clear()
                                    }
                                }
                            }
                            showDeleteDialog = false
                            termToDelete = null
                            groupToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ဖိုင်ရှင်း",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Button(
                    //     onClick = {
                    //         scope.launch {
                    //             loading = true
                    //             optimisticUpdates.clear()
                    //             loadingIds = emptySet()
                    //             try {
                    //                 val loadedTerms = repository.getTerms()
                    //                 terms = loadedTerms
                    //                 groupedTerms.clear()
                    //                 groupedTerms.addAll(groupTermsByDateRange(loadedTerms))
                    //                 refreshCounter++ // Force UI recomposition
                    //             } catch (e: Exception) {
                    //                 println("Error refreshing terms: ${e.message}")
                    //             } finally {
                    //                 loading = false
                    //             }
                    //         }
                    //     },
                    //     enabled = !loading
                    // ) {
                    //     if (loading) {
                    //         CircularProgressIndicator(
                    //             modifier = Modifier.size(16.dp),
                    //             strokeWidth = 2.dp,
                    //             color = MaterialTheme.colorScheme.onPrimary
                    //         )
                    //     } else {
                    //         Text("Refresh")
                    //     }
                    // }
                }
                
                if (loading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (groupedTerms.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📋",
                            fontSize = 48.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "No terms available",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Please check back later",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Use TermGroupCard component with better optimistic update handling
                        groupedTerms.forEach { group ->
                            key(group.groupId, uiForceUpdate, optimisticUpdates.size) {
                                TermGroupCard(
                                    group = group,
                                    loadingIds = loadingIds,
                                    groupedTerms = groupedTerms,
                                    optimisticUpdates = optimisticUpdates,
                                    uiForceUpdate = uiForceUpdate,
                                onGroupCheckboxChange = { selectedGroup, isChecked ->
                                    println("Checkbox clicked for group: ${selectedGroup.groupId}")
                                    scope.launch {
                                        val currentGroup = groupedTerms.find { it.groupId == selectedGroup.groupId }
                                        
                                        if (currentGroup != null) {
                                            val newStatus = if (isChecked) "1" else "0"
                                            val termIds = currentGroup.terms.map { it.termId }
                                            
                                            // Apply optimistic updates immediately
                                            termIds.forEach { termId ->
                                                optimisticUpdates[termId] = newStatus
                                            }
                                            uiForceUpdate++ // Force recomposition
                                            
                                            // Force immediate recomposition by updating state
                                            refreshCounter++
                                            
                                            println("Calling updateTermStatus for termIds: ${termIds.joinToString(",")} with status: $newStatus")
                                            val success = repository.updateTermStatus(
                                                termIds.joinToString(","),
                                                newStatus
                                            )
                                            println("updateTermStatus returned: $success")
                                            
                                            if (success) {
                                                println("Update successful, starting automatic refresh...")
                                                kotlinx.coroutines.delay(500)
                                                
                                                loading = true
                                                loadingIds = emptySet()
                                                
                                                try {
                                                    println("Fetching fresh terms...")
                                                    val loadedTerms = repository.getTerms()
                                                    terms = loadedTerms
                                                    groupedTerms.clear()
                                                    groupedTerms.addAll(groupTermsByDateRange(loadedTerms))
                                                    refreshCounter++
                                                    // Clear optimistic updates AFTER fresh data is loaded
                                                    optimisticUpdates.clear()
                                                    uiForceUpdate++
                                                    println("Automatic refresh completed successfully")
                                                } catch (e: Exception) {
                                                    println("Error refreshing terms: ${e.message}")
                                                } finally {
                                                    loading = false
                                                }
                                            } else {
                                                // Revert optimistic updates on failure
                                                termIds.forEach { termId ->
                                                    optimisticUpdates.remove(termId)
                                                }
                                                uiForceUpdate++ // Force recomposition
                                                println("API call failed, reverted optimistic updates")
                                            }
                                        }
                                    }
                                },
                                    onGroupDelete = { selectedGroup ->
                                        groupToDelete = selectedGroup
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TermGroupCard(
    group: CleanFileTermGroup,
    loadingIds: Set<String>,
    groupedTerms: List<CleanFileTermGroup>,
    optimisticUpdates: androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>,
    uiForceUpdate: Int,
    onGroupCheckboxChange: (CleanFileTermGroup, Boolean) -> Unit,
    onGroupDelete: (CleanFileTermGroup) -> Unit
) {
    // Use the current group data instead of the original group parameter
    val currentGroup = groupedTerms.find { it.groupId == group.groupId } ?: group
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Group Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isLoading = currentGroup.terms.any { loadingIds.contains(it.termId) }
                    
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        // Calculate checkbox state with reactive optimistic update handling
                        val finalChecked by derivedStateOf {
                            currentGroup.terms.all { term ->
                                val optimisticStatus = optimisticUpdates[term.termId]
                                if (optimisticStatus != null) {
                                    optimisticStatus == "1"
                                } else {
                                    term.isFinished == "1"
                                }
                            }
                        }
                        

                        
                        Checkbox(
                            checked = finalChecked,
                            onCheckedChange = { isChecked ->
                                if (!isLoading) {
                                    onGroupCheckboxChange(group, isChecked)
                                }
                            },
                            enabled = !isLoading
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = group.label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLoading) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                
                IconButton(
                    onClick = { onGroupDelete(group) }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete group",
                        tint = Color.Red
                    )
                }
            }
            
            // Group Terms
            Column {
                currentGroup.terms.forEachIndexed { index, term ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 64.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Show loading indicator if this term is being updated
                            val isLoading = loadingIds.contains(term.termId)
                            
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            
                            Text(
                                text = term.termName,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Status badge - use optimistic updates if available
                            val currentTerm = currentGroup.terms.find { it.termId == term.termId }
                            val displayStatus by derivedStateOf {
                                optimisticUpdates[term.termId] ?: currentTerm?.isFinished ?: term.isFinished
                            }
                            
                            Surface(
                                color = if (displayStatus == "1") {
                                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                                } else {
                                    Color(0xFFFFC107).copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (displayStatus == "1") "Finished" else "Active",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (displayStatus == "1") {
                                        Color(0xFF2E7D32)
                                    } else {
                                        Color(0xFFF57C00)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper functions
fun isGroupAllFinished(group: CleanFileTermGroup): Boolean {
    return group.terms.all { it.isFinished == "1" }
}

fun isGroupAnyFinished(group: CleanFileTermGroup): Boolean {
    return group.terms.any { it.isFinished == "1" }
}

suspend fun loadTerms(repository: CleanFileRepository, onResult: (List<Term>) -> Unit) {
    val terms = repository.getTerms()
    onResult(terms)
}

suspend fun refreshTermsList(
    repository: CleanFileRepository,
    onTermsUpdate: (List<Term>) -> Unit,
    onGroupedTermsUpdate: (List<CleanFileTermGroup>) -> Unit,
    onLoadingUpdate: ((Boolean) -> Unit)? = null
) {
    onLoadingUpdate?.invoke(true)
    try {
        val loadedTerms = repository.getTerms()
        onTermsUpdate(loadedTerms)
        val groupedTerms = groupTermsByDateRange(loadedTerms)
        onGroupedTermsUpdate(groupedTerms)
    } catch (e: Exception) {
        println("Error refreshing terms list: ${e.message}")
    } finally {
        onLoadingUpdate?.invoke(false)
    }
}

fun groupTermsByDateRange(terms: List<Term>): List<CleanFileTermGroup> {
    val grouped = terms.groupBy { it.groupId ?: "ungrouped" }
    
    return grouped.map { (groupId, groupTerms) ->
        val label = if (groupId == "ungrouped") {
            "Ungrouped Terms"
        } else {
            generateGroupLabel(groupTerms)
        }
        
        val maxEndDate = groupTerms.mapNotNull { it.endDate }.maxOrNull()
        
        CleanFileTermGroup(
            groupId = groupId,
            label = label,
            terms = groupTerms,
            maxEndDate = maxEndDate
        )
    }.sortedByDescending { group ->
        group.maxEndDate?.let { parseDate(it) }
    }
}

fun generateGroupLabel(terms: List<Term>): String {
    val firstTerm = terms.firstOrNull()
    return if (firstTerm?.startDate != null && firstTerm.endDate != null) {
        val startDates = terms.mapNotNull { it.startDate?.let { date -> parseDate(date) } }
        val endDates = terms.mapNotNull { it.endDate?.let { date -> parseDate(date) } }
        
        if (startDates.isNotEmpty() && endDates.isNotEmpty()) {
            val minStart = startDates.minOrNull()
            val maxEnd = endDates.maxOrNull()
            
            if (minStart != null && maxEnd != null) {
                val startDay = SimpleDateFormat("dd", Locale.getDefault()).format(minStart)
                val endDay = SimpleDateFormat("dd", Locale.getDefault()).format(maxEnd)
                val month = SimpleDateFormat("MM", Locale.getDefault()).format(maxEnd)
                val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(maxEnd)
                "$startDay TO $endDay/$month/$year"
            } else {
                "Group ${terms.first().groupId}"
            }
        } else {
            "Group ${terms.first().groupId}"
        }
    } else {
        "Group ${terms.first().groupId}"
    }
}

fun parseDate(dateString: String): Date? {
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
    } catch (e: Exception) {
        null
    }
}