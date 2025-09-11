package ui.screens

import ui.screens.SalePageState
import ui.screens.SalePageViewModel
import core.stores.SelectionStoreState
import ui.screens.TermOption
import ui.components.SearchableTermDropdown
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.components.SearchableUserDropdown
import core.stores.rememberTempListStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import core.config.CompactOutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalePageLeftContent(
    state: SalePageState,
    selectionState: SelectionStoreState,
    viewModel: SalePageViewModel,
    onOpenBetAndReminingModal: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    println("=== SALE PAGE LEFT CONTENT COMPOSABLE CALLED ===")
    val tempListStore = rememberTempListStore()

    // Left Section - Main Controls (40%)
    Column(
        modifier = modifier.fillMaxHeight().padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // Term Selection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Convert SalePage TermOption to SearchableDropdown TermOption format
                val searchableTermOptions = selectionState.termOptions.map { termOption ->
                    data.models.TermOption(
                        termId = termOption.value.toIntOrNull() ?: 0,
                        termName = termOption.label,
                        shortName = termOption.label,
                        groupId = "1",
                        startDate = "",
                        endDate = "",
                        isFinished = "0",
                        termType = "regular",
                        winNum = null,
                        is2D = true,
                        unitPrice = 1.0,
                        breakAmount = 0
                    )
                }
                
                val selectedSearchableTerm = selectionState.selectedTerm?.let { selected ->
                    searchableTermOptions.find { it.termId.toString() == selected.value }
                }

                SearchableTermDropdown(
                    termOptions = searchableTermOptions,
                    selectedTerm = selectedSearchableTerm,
                    onTermSelected = { selectedTerm ->
                        // Convert back to SalePage TermOption format
                        val salePageTermOption = TermOption(
                            value = selectedTerm.termId.toString(),
                            label = selectedTerm.termName
                        )
                        viewModel.updateSelectedTerm(salePageTermOption)
                        // Focus user dropdown after term selection (like JavaScript version)
                        // Add delay to ensure focus happens after state updates
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(200)
                            viewModel.focusUserSelect()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = "အပါတ်စဉ်",
                    focusRequester = viewModel.termFocusRequester
                )
                
                // Auto-focus on initial load
                LaunchedEffect(Unit) {
                    viewModel.termFocusRequester.requestFocus()
                }
                
                IconButton(
                    onClick = { /* Navigate to slip page */ }
                ) {
                    Icon(Icons.Default.List, contentDescription = "Slips")
                }
            }
            
            // User Selection
            println("[DEBUG] About to render SearchableUserDropdown with ${selectionState.userOptions.size} userOptions")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchableUserDropdown(
                    userOptions = selectionState.userOptions,
                    selectedUser = selectionState.selectedUser,
                    onUserSelected = { userOption -> 
                        viewModel.updateSelectedUser(userOption)
                        // Focus number input after user selection
                        viewModel.focusNumberInputAfterUserSelection()
                    },
                    modifier = Modifier.weight(1f),
                    label = "ထိုးသား",
                    placeholder = "ထိုးသား",
                    focusRequester = viewModel.userFocusRequester
                )
                
                // Connection Status Indicator
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (state.connectStatus) Color.Green else Color.Red,
                            shape = CircleShape
                        )
                )
                
                IconButton(
                    onClick = { /* Navigate to slip page */ }
                ) {
                    Icon(Icons.Default.List, contentDescription = "List")
                }
            }
            

            
            // Betted Units Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ထိုးပီးယူနစ်",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(120.dp)
                )
                
                CompactOutlinedTextField(
                    value = state.bettedUnits,
                    onValueChange = { },
                    enabled = false,
                    textStyle = LocalTextStyle.current.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(),
                    modifier = Modifier,
                    shape = RoundedCornerShape(8.dp)
                 )
            }
            
            // 2D/3D View Section
            if (selectionState.selectedTerm != null) {
                println("[DEBUG] SalePageLeftContent: selectedTerm is not null, is2D: ${state.is2D}")
                if (state.is2D) {
                    println("[DEBUG] SalePageLeftContent: Rendering TwoDView")
                    TwoDView(
                        termId = selectionState.selectedTerm?.value,
                        user = state.userList.find { it.userId == selectionState.selectedUser?.value },
                        unitPrice = state.unitPrice,
                        breakAmount = state.breakAmount,
                        playFailSong = { /* Handle fail song */ },
                        playSuccessSong = { /* Handle success song */ },
                        playDuplicateSong = { /* Handle duplicate song */ },
                        numberInput = state.numberInput,
                        apiUserData = state.apiUserData,
                        onUserSelectionChanged = { viewModel.focusNumberInputAfterUserSelection() },
                        onOpenBetAndReminingModal = onOpenBetAndReminingModal
                    )
                } else {
                    println("[DEBUG] SalePageLeftContent: Rendering ThreeDView")
                    ThreeDView(
                        unitPrice = state.unitPrice,
                        breakAmount = state.breakAmount,
                        playFailSong = { /* Handle fail song */ },
                        playSuccessSong = { /* Handle success song */ },
                        playDuplicateSong = { /* Handle duplicate song */ },
                        termId = selectionState.selectedTerm?.value,
                        user = state.userList.find { it.userId == selectionState.selectedUser?.value },
                        numberInput = state.numberInput,
                        apiUserData = state.apiUserData,
                        onUserSelectionChanged = { viewModel.focusNumberInputAfterUserSelection() },
                        onOpenBetAndReminingModal = onOpenBetAndReminingModal
                    )
                }
            }else{
                println("[DEBUG] SalePageLeftContent: selectedTerm is null, rendering default TwoDView")
                TwoDView(
                        termId = selectionState.selectedTerm?.value,
                        user = state.userList.find { it.userId == selectionState.selectedUser?.value },
                        unitPrice = state.unitPrice,
                        breakAmount = state.breakAmount,
                        playFailSong = { /* Handle fail song */ },
                        playSuccessSong = { /* Handle success song */ },
                        playDuplicateSong = { /* Handle duplicate song */ },
                        numberInput = state.numberInput,
                        apiUserData = state.apiUserData,
                        onUserSelectionChanged = { viewModel.focusNumberInputAfterUserSelection() },
                        onOpenBetAndReminingModal = onOpenBetAndReminingModal
                    )
            }
            

            
            // Checkboxes
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.sendSMS,
                        onCheckedChange = { viewModel.toggleSendSMS() }
                    )
                    Text(
                        text = "SMS",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.isPrintingEnabled,
                        onCheckedChange = { viewModel.togglePrinting() }
                    )
                    Text(
                        text = "ပရင့်ထုတ်",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                if (state.userProfile?.userType == "owner" || state.userProfile?.userType == "employee") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.isAllowExtra,
                            onCheckedChange = { viewModel.toggleAllowExtra() }
                        )
                        Text(
                            text = "ကျွံခွင့်ပြု",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Button(
                    onClick = { viewModel.openModal("total-message") },
                    modifier = Modifier.weight(1f),
                    enabled = !state.apiCalling
                ) {
                    Text("မက်ဆေ့ပေါင်း")
                }
                
                OutlinedButton(
                    onClick = { 
                        viewModel.clearList()
                        tempListStore.clearList()
                        tempListStore.setListType("SELL")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !state.apiCalling
                ) {
                    Text("Clear")
                }
            }
            
            // Buy/Save Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (state.userProfile?.userType == "owner" || state.userProfile?.userType == "employee") {
                    Button(
                        onClick = { viewModel.handleBuy() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.apiCalling && state.list.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Buy")
                    }
                    
                    Button(
                        onClick = { viewModel.handleDirectBuy() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.apiCalling && state.list.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9C27B0)
                        )
                    ) {
                        Text("D Buy")
                    }
                }
                
                Button(
                    onClick = { viewModel.handleSave(false) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.apiCalling,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Save")
                }
            }
        } // Close left Column
}