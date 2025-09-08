package ui.components

import data.models.UserOption
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableUserDropdown(
    userOptions: List<UserOption>,
    selectedUser: UserOption?,
    onUserSelected: (UserOption) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "ထိုးသား ရွေးပါ",
    placeholder: String = "ထိုးသား",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    focusRequester: FocusRequester? = null
) {


    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    
    // Focus requesters
    val internalFocusRequester = remember { FocusRequester() }
    val actualFocusRequester = focusRequester ?: internalFocusRequester
    val lazyColumnFocusRequester = remember { FocusRequester() }
    
    // State variables
    var expanded by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var selectedIndex by remember { mutableStateOf(-1) }
    var textFieldWidth by remember { mutableStateOf(0.dp) }
    var textFieldHeight by remember { mutableStateOf(0.dp) }
    
    // Initialize textFieldValue based on selectedUser when component first loads
    LaunchedEffect(selectedUser) {
        if (selectedUser != null && selectedUser.label.isNotEmpty()) {
            textFieldValue = TextFieldValue(selectedUser.label)
        } else {
            textFieldValue = TextFieldValue("")
        }
    }
    
    val searchText = textFieldValue.text
    
    // Filter users based on search text
    val filteredUsers = remember(userOptions, searchText) {
        if (searchText.isEmpty()) {
            userOptions
        } else {
            userOptions.filter { user ->
                user.label.contains(searchText, ignoreCase = true)
            }
        }
    }
    

    
    // Reset selection when filtered users change
    LaunchedEffect(filteredUsers) {
        if (filteredUsers.isEmpty()) {
            selectedIndex = -1
        }
    }
    
    // Focus LazyColumn when dropdown expands for keyboard navigation
    LaunchedEffect(expanded, filteredUsers) {
        if (expanded && filteredUsers.isNotEmpty()) {
            delay(200) // Longer delay to ensure LazyColumn is fully rendered
            try {
                lazyColumnFocusRequester.requestFocus()
            } catch (e: IllegalStateException) {
                // Ignore focus request if LazyColumn is not ready
                println("LazyColumn focus request failed: ${e.message}")
            }
        }
    }
    
    val listState = rememberLazyListState()
    
    // Scroll to selected item
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0 && expanded) {
            listState.animateScrollToItem(selectedIndex)
        }
    }
    
    fun handleKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.type == KeyEventType.KeyUp || keyEvent.type == KeyEventType.KeyDown) {
            when (keyEvent.key) {
                Key.DirectionDown -> {
                    if (!expanded) {
                        expanded = true
                        return true
                    } else if (filteredUsers.isNotEmpty()) {
                        selectedIndex = if (selectedIndex == -1) 0 else (selectedIndex + 1).coerceAtMost(filteredUsers.size - 1)
                        return true
                    }
                    return false
                }
                Key.DirectionUp -> {
                    if (expanded && filteredUsers.isNotEmpty()) {
                        selectedIndex = if (selectedIndex == -1) filteredUsers.size - 1 else (selectedIndex - 1).coerceAtLeast(0)
                        return true
                    }
                    return false
                }
                Key.Enter -> {
                    if (!expanded) {
                        expanded = true
                        return true
                    } else if (selectedIndex >= 0 && selectedIndex < filteredUsers.size) {
                        onUserSelected(filteredUsers[selectedIndex])
                        expanded = false
                        textFieldValue = TextFieldValue("")
                        selectedIndex = -1
                        focusManager.clearFocus()
                        return true
                    }
                    return false
                }
                Key.Escape -> {
                    if (expanded) {
                        expanded = false
                        textFieldValue = TextFieldValue("")
                        selectedIndex = -1
                        focusManager.clearFocus()
                        return true
                    }
                    return false
                }
            }
        }
        return false
    }
    
    Column(modifier = modifier) {
        Box(
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Clear text field when Box is clicked if user is selected
                if (selectedUser != null && selectedUser.label.isNotEmpty()) {
                    textFieldValue = TextFieldValue("")
                    onUserSelected(UserOption("", ""))
                }
                if (!expanded) {
                    expanded = true
                }
                actualFocusRequester.requestFocus()
            }
        ) {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { value ->
                    textFieldValue = value
                    if (!expanded) {
                        expanded = true
                    }
                    // selectedIndex will be updated by LaunchedEffect when filteredUsers changes
                },
                singleLine = true,
                readOnly = false,
                label = { Text(label) },
                placeholder = { 
                    Text(
                        text = if (selectedUser == null) placeholder else "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ) 
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (!expanded) {
                            expanded = true
                            textFieldValue = TextFieldValue("")
                            selectedIndex = -1
                        } else if (selectedIndex >= 0 && selectedIndex < filteredUsers.size) {
                            onUserSelected(filteredUsers[selectedIndex])
                            expanded = false
                            textFieldValue = TextFieldValue("")
                            selectedIndex = -1
                            focusManager.clearFocus()
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(actualFocusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            // Clear text field immediately when focused if user is selected
                            if (selectedUser != null && selectedUser.label.isNotEmpty()) {
                                textFieldValue = TextFieldValue("")
                                onUserSelected(UserOption("", ""))
                            }
                            if (!expanded) {
                                expanded = true
                                selectedIndex = -1
                            }
                        }
                    }
                    .onKeyEvent { keyEvent ->
                        handleKeyEvent(keyEvent)
                    }
                    .onGloballyPositioned { coordinates ->
                        textFieldWidth = with(density) { coordinates.size.width.toDp() }
                        textFieldHeight = with(density) { coordinates.size.height.toDp() }
                    }
            )
            
            // Dropdown menu
            if (expanded) {
        
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, with(density) { textFieldHeight.roundToPx() }),
                    onDismissRequest = {
                        expanded = false
                        textFieldValue = TextFieldValue("")
                        selectedIndex = -1
                    },
                    properties = PopupProperties(
                        focusable = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    // Keep text field focused for typing and filtering
                    
                    Card(
                        modifier = Modifier
                            .width(textFieldWidth)
                            .heightIn(max = 300.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        if (filteredUsers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (searchText.isEmpty()) "No users available" else "No users found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (searchText.isNotEmpty()) {
                                        Text(
                                            text = "Try adjusting your search",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(lazyColumnFocusRequester)
                                    .focusable()
                                    .onKeyEvent { keyEvent ->
                                        handleKeyEvent(keyEvent)
                                    }
                            ) {
                                itemsIndexed(filteredUsers) { index, user ->
                                    val isSelected = index == selectedIndex
                                    val isCurrentlySelected = selectedUser?.value == user.value
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isCurrentlySelected -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                onUserSelected(user)
                                                expanded = false
                                                textFieldValue = TextFieldValue("")
                                                selectedIndex = -1
                                                focusManager.clearFocus()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = user.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isCurrentlySelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    isCurrentlySelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (user.userType.isNotEmpty()) {
                                                Text(
                                                    text = user.userType,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = when {
                                                        isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                                        isCurrentlySelected -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
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