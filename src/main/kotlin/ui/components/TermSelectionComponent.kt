package ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*
import data.models.*

// Function to group terms by groupId and create group labels
fun groupTermsByGroupId(termOptions: List<TermOption>): List<GroupedTermOption> {
    if (termOptions.isEmpty()) return emptyList()
    
    // Group terms by GroupId
    val groupedTerms = termOptions.groupBy { it.groupId.ifEmpty { "ungrouped" } }
    
    // Convert to grouped options format
    return groupedTerms.map { (groupId, terms) ->
        val groupLabel = if (groupId == "ungrouped") {
            "Ungrouped Terms"
        } else {
            val firstTerm = terms.firstOrNull()
            if (firstTerm?.startDate?.isNotEmpty() == true && firstTerm.endDate.isNotEmpty()) {
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val startDates = terms.mapNotNull { 
                        try { dateFormat.parse(it.startDate) } catch (e: Exception) { null }
                    }
                    val endDates = terms.mapNotNull { 
                        try { dateFormat.parse(it.endDate) } catch (e: Exception) { null }
                    }
                    
                    if (startDates.isNotEmpty() && endDates.isNotEmpty()) {
                        val minStart = startDates.minOrNull()!!
                        val maxEnd = endDates.maxOrNull()!!
                        
                        val calendar = Calendar.getInstance()
                        calendar.time = minStart
                        val startDay = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
                        
                        calendar.time = maxEnd
                        val endDay = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
                        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
                        val year = calendar.get(Calendar.YEAR)
                        
                        "$startDay TO $endDay/$month/$year"
                    } else {
                        "Group $groupId"
                    }
                } catch (e: Exception) {
                    "Group $groupId"
                }
            } else {
                "Group $groupId"
            }
        }
        
        GroupedTermOption(
            groupId = groupId,
            groupLabel = groupLabel,
            terms = terms
        )
    }
}

@Composable
fun TermSelectionDialog(
    termOptions: List<TermOption>,
    selectedTerms: List<TermOption>,
    onTermsSelected: (List<TermOption>) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    selectionMode: SelectionMode = SelectionMode.MULTI
) {
    var tempSelectedTerms by remember { mutableStateOf(selectedTerms) }
    val groupedTerms = remember(termOptions) { groupTermsByGroupId(termOptions) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp),
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
                        text = "အပါတ်စဉ်များရွေးချယ်ပါ",
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Loading terms...")
                            }
                        }
                    }
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onRetry) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    groupedTerms.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No terms available")
                        }
                    }
                    else -> {
                        // Grouped terms list with accordion
                        LazyColumn(
                            modifier = Modifier.weight(1f).heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(groupedTerms) { index, group ->
                                val backgroundColor = if (index % 2 == 0) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                                GroupedTermAccordion(
                                    group = group,
                                    selectedTerms = tempSelectedTerms,
                                    selectionMode = selectionMode,
                                    backgroundColor = backgroundColor,
                                    onTermSelectionChanged = { term, isSelected ->
                                        tempSelectedTerms = when (selectionMode) {
                                            SelectionMode.SINGLE -> {
                                                if (isSelected) {
                                                    listOf(term)
                                                } else {
                                                    emptyList()
                                                }
                                            }
                                            SelectionMode.MULTI -> {
                                                if (isSelected) {
                                                    tempSelectedTerms + term
                                                } else {
                                                    tempSelectedTerms.filter { it.termId != term.termId }
                                                }
                                            }
                                        }
                                    }
                                )
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
                                onClick = { onTermsSelected(tempSelectedTerms) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    when (selectionMode) {
                                        SelectionMode.SINGLE -> "Select"
                                        SelectionMode.MULTI -> "Select (${tempSelectedTerms.size})"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermSelectionDropdown(
    termOptions: List<TermOption>,
    selectedTerm: TermOption?,
    onTermSelected: (TermOption) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "အပါတ်စဉ် ရွေးပါ",
    placeholder: String = "အပါတ်စဉ်",
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded && !isLoading }
        ) {
            OutlinedTextField(
                value = selectedTerm?.let { 
                    if (it.winNum != null) "${it.termName}(${it.winNum})" else it.termName 
                } ?: "",
                onValueChange = { },
                readOnly = true,
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                trailingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                enabled = !isLoading
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                termOptions.forEach { term ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (term.winNum != null) "${term.termName}(${term.winNum})" else term.termName
                            )
                        },
                        onClick = {
                            onTermSelected(term)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        // Error message
        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun GroupedTermAccordion(
    group: GroupedTermOption,
    selectedTerms: List<TermOption>,
    selectionMode: SelectionMode,
    backgroundColor: Color,
    onTermSelectionChanged: (TermOption, Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val selectedCount = group.terms.count { term -> selectedTerms.any { it.termId == term.termId } }
    val isSingleChild = group.terms.size == 1
    val singleTerm = if (isSingleChild) group.terms.first() else null
    val isGroupSelected = if (isSingleChild && singleTerm != null) {
        selectedTerms.any { it.termId == singleTerm.termId }
    } else {
        selectedCount == group.terms.size && group.terms.isNotEmpty()
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column {
            // Group header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (isSingleChild && singleTerm != null) {
                            onTermSelectionChanged(singleTerm, !isGroupSelected)
                        } else {
                            isExpanded = !isExpanded
                        }
                    }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show checkbox for multi-select or radio button for single-select
                    when (selectionMode) {
                        SelectionMode.MULTI -> {
                            Checkbox(
                                checked = isGroupSelected,
                                onCheckedChange = { checked ->
                                    if (isSingleChild && singleTerm != null) {
                                        onTermSelectionChanged(singleTerm, checked)
                                    } else {
                                        // For multi-child groups, select/deselect all terms
                                        group.terms.forEach { term ->
                                            onTermSelectionChanged(term, checked)
                                        }
                                    }
                                }
                            )
                        }
                        SelectionMode.SINGLE -> {
                            if (isSingleChild && singleTerm != null) {
                                RadioButton(
                                    selected = isGroupSelected,
                                    onClick = {
                                        onTermSelectionChanged(singleTerm, !isGroupSelected)
                                    }
                                )
                            }
                            // For multi-child groups in single mode, don't show selection control at group level
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(
                            text = if (isSingleChild && singleTerm != null) {
                                if (singleTerm.winNum != null) "${singleTerm.termName}(${singleTerm.winNum})" else singleTerm.termName
                            } else group.groupLabel,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        if (!isSingleChild) {
                            Text(
                                text = "${selectedCount}/${group.terms.size} selected",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Only show expand/collapse icon for multi-child groups
                if (!isSingleChild) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
            
            // Group content (terms) - only for multi-child groups
            if (!isSingleChild && isExpanded) {
                Divider()
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    group.terms.forEach { term ->
                        val termIsSelected = selectedTerms.any { it.termId == term.termId }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTermSelectionChanged(term, !termIsSelected)
                                }
                                .padding(start = 24.dp, top = 8.dp, bottom = 8.dp, end = 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (selectionMode) {
                                SelectionMode.MULTI -> {
                                    Checkbox(
                                        checked = termIsSelected,
                                        onCheckedChange = { checked ->
                                            onTermSelectionChanged(term, checked)
                                        }
                                    )
                                }
                                SelectionMode.SINGLE -> {
                                    RadioButton(
                                        selected = termIsSelected,
                                        onClick = {
                                            onTermSelectionChanged(term, !termIsSelected)
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (term.winNum != null) "${term.termName}(${term.winNum})" else term.termName,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}