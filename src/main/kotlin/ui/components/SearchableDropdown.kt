package ui.components

// Import the TermOption from TermSelectionComponent
import data.models.TermOption
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.launch
import core.config.CompactOutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableTermDropdown(
    termOptions: List<TermOption>,
    selectedTerm: TermOption?,
    onTermSelected: (TermOption) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "အပါတ်စဉ် ရွေးပါ",
    placeholder: String = "အပါတ်စဉ်",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    focusRequester: FocusRequester? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var textFieldWidth by remember { mutableStateOf(0.dp) }
    var textFieldHeight by remember { mutableStateOf(0.dp) }
    val internalFocusRequester = remember { FocusRequester() }
    val actualFocusRequester = focusRequester ?: internalFocusRequester
    val popupFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    // Filter terms based on search text
    val filteredTerms = remember(termOptions, searchText) {
        if (searchText.isEmpty()) {
            termOptions
        } else {
            termOptions.filter { term ->
                term.termName.contains(searchText, ignoreCase = true) ||
                term.shortName.contains(searchText, ignoreCase = true) ||
                (term.winNum?.contains(searchText, ignoreCase = true) == true)
            }
        }
    }
    
    // Reset selected index when filtered terms change and auto-select current term
    LaunchedEffect(filteredTerms, expanded) {
        if (expanded) {
            if (filteredTerms.isNotEmpty()) {
                // Find the index of the currently selected term
                val currentTermIndex = selectedTerm?.let { currentTerm ->
                    filteredTerms.indexOfFirst { it.termId == currentTerm.termId }
                } ?: -1
                
                // If current term is found in filtered list, select it; otherwise don't auto-select
                selectedIndex = if (currentTermIndex >= 0) currentTermIndex else -1
            } else {
                selectedIndex = -1
            }
        }
    }
    
    // Auto-focus on component startup
    LaunchedEffect(Unit) {
        actualFocusRequester.requestFocus()
    }
    
    // Handle keyboard navigation
    fun handleKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.type != KeyEventType.KeyDown) return false
        
        when (keyEvent.key) {
            Key.DirectionDown -> {
                if (expanded && filteredTerms.isNotEmpty()) {
                    selectedIndex = if (selectedIndex >= filteredTerms.size - 1) 0 else selectedIndex + 1
                    coroutineScope.launch {
                        listState.animateScrollToItem(selectedIndex)
                    }
                    return true
                } else if (!expanded) {
                    expanded = true
                    // Find the index of the currently selected term
                    val currentTermIndex = selectedTerm?.let { currentTerm ->
                        filteredTerms.indexOfFirst { it.termId == currentTerm.termId }
                    } ?: -1
                    selectedIndex = if (currentTermIndex >= 0) currentTermIndex else 0
                    return true
                }
            }
            Key.DirectionUp -> {
                if (expanded && filteredTerms.isNotEmpty()) {
                    selectedIndex = if (selectedIndex <= 0) filteredTerms.size - 1 else selectedIndex - 1
                    coroutineScope.launch {
                        listState.animateScrollToItem(selectedIndex)
                    }
                    return true
                } else if (!expanded) {
                    expanded = true
                    // Find the index of the currently selected term
                    val currentTermIndex = selectedTerm?.let { currentTerm ->
                        filteredTerms.indexOfFirst { it.termId == currentTerm.termId }
                    } ?: -1
                    selectedIndex = if (currentTermIndex >= 0) currentTermIndex else (filteredTerms.size - 1)
                    return true
                }
            }
            Key.Enter -> {
                if (!expanded) {
                    expanded = true
                    if (filteredTerms.isNotEmpty()) {
                        // Find the index of the currently selected term
                        val currentTermIndex = selectedTerm?.let { currentTerm ->
                            filteredTerms.indexOfFirst { it.termId == currentTerm.termId }
                        } ?: -1
                        selectedIndex = if (currentTermIndex >= 0) currentTermIndex else 0
                    } else {
                        selectedIndex = -1
                    }
                    return true
                } else if (selectedIndex >= 0 && selectedIndex < filteredTerms.size) {
                    onTermSelected(filteredTerms[selectedIndex])
                    expanded = false
                    searchText = ""
                    selectedIndex = -1
                    focusManager.clearFocus()
                    return true
                }
            }
            Key.Escape -> {
                if (expanded) {
                    expanded = false
                    searchText = ""
                    selectedIndex = -1
                    focusManager.clearFocus()
                    return true
                }
            }
        }
        return false
    }
    
    Column(modifier = modifier) {
        Box {
            CompactOutlinedTextField(
                value = if (expanded) searchText else (selectedTerm?.let { 
                    if (it.winNum != null) "${it.termName}(${it.winNum})" else it.termName 
                } ?: ""),
                onValueChange = { value ->
                    println("[DEBUG] onValueChange called - value: $value, expanded: $expanded")
                    if (expanded) {
                        searchText = value
                        selectedIndex = if (filteredTerms.isNotEmpty()) 0 else -1
                    } else {
                        // If not expanded, open the dropdown when user tries to type
                        println("[DEBUG] Opening dropdown from onValueChange")
                        expanded = true
                        searchText = value
                        selectedIndex = if (filteredTerms.isNotEmpty()) 0 else -1
                    }
                }, 
                readOnly = false,
                trailingIcon = {
                    Row {
                        if (expanded && searchText.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchText = ""
                                    selectedIndex = if (filteredTerms.isNotEmpty()) {
                                          filteredTerms.indexOfFirst { it.termId == selectedTerm?.termId }.takeIf { it >= 0 } ?: 0
                                      } else -1
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    if (!expanded) {
                                        expanded = true
                                        searchText = ""
                                        selectedIndex = if (filteredTerms.isNotEmpty()) {
                                     filteredTerms.indexOfFirst { it.termId == selectedTerm?.termId }.takeIf { it >= 0 } ?: 0
                                  } else -1
                                        actualFocusRequester.requestFocus()
                                    } else {
                                        expanded = false
                                        focusManager.clearFocus()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                                    contentDescription = if (expanded) "Collapse" else "Expand"
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(actualFocusRequester)
                    .onGloballyPositioned { coordinates ->
                        textFieldWidth = with(density) { coordinates.size.width.toDp() }
                        textFieldHeight = with(density) { coordinates.size.height.toDp() }
                    }
                    // .onFocusChanged { focusState ->
                    //     println("[DEBUG] Focus changed - hasFocus: ${focusState.hasFocus}, expanded: $expanded")
                    //     if (focusState.hasFocus && !expanded) {
                    //         println("[DEBUG] Opening dropdown on focus")
                    //         expanded = true
                    //         searchText = ""
                    //         selectedIndex = if (filteredTerms.isNotEmpty()) {
                    //               filteredTerms.indexOfFirst { it.termId == selectedTerm?.termId }.takeIf { it >= 0 } ?: 0
                    //           } else -1
                    //     }
                    // }
                    .onPreviewKeyEvent { keyEvent ->
                        handleKeyEvent(keyEvent)
                    },
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.None
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // Let onKeyEvent handle Enter key functionality
                        // This prevents conflicts between KeyboardActions and onKeyEvent
                    }
                )
            )
            
            // Dropdown menu
            if (expanded) {
        
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, with(density) { (textFieldHeight + 4.dp).roundToPx() }),
                    onDismissRequest = {
                        expanded = false
                        searchText = ""
                        selectedIndex = -1
                    },
                    properties = PopupProperties(
                        focusable = true,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    // Transfer focus to popup when it opens
                    LaunchedEffect(expanded, filteredTerms) {
                        if (expanded && filteredTerms.isNotEmpty()) {
                            focusManager.clearFocus()
                            // Delay focus request to ensure LazyColumn is composed
                            kotlinx.coroutines.delay(50)
                            popupFocusRequester.requestFocus()
                        }
                    }
                    
                    Card(
                        modifier = Modifier
                            .width(textFieldWidth)
                            .heightIn(max = 300.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(4.dp)
                            ),
                        shape = RoundedCornerShape(4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        if (filteredTerms.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No terms found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(popupFocusRequester)
                                    .focusable()
                                    .onKeyEvent { keyEvent ->
                                        handleKeyEvent(keyEvent)
                                    }
                            ) {
                                itemsIndexed(filteredTerms) { index, term ->
                                    val isSelected = index == selectedIndex
                                    val isCurrentlySelected = selectedTerm?.termId == term.termId
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                when {
                                                    isSelected -> Color(0xFF006400) // Dark green
                                                    isCurrentlySelected -> MaterialTheme.colorScheme.secondaryContainer
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                onTermSelected(term)
                                                expanded = false
                                                searchText = ""
                                                selectedIndex = -1
                                                focusManager.clearFocus()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (term.winNum != null) "${term.termName}(${term.winNum})" else term.termName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCurrentlySelected) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> Color.White // White text on dark green background
                                                isCurrentlySelected -> MaterialTheme.colorScheme.onSecondaryContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
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