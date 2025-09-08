package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class Item(
    val _id: String,
    val number: String,
    val type: String,
    val amount: Int,
    val slipId: String,
    val termId: Int,
    val userId: Int,
    val summary: String,
    val showSummary: String
)

// UserProfile is defined in SalePage.kt

data class SlipTerm(
    val value: String,
    val label: String
)

data class SlipDetailProps(
    val copy: String,
    val smsCopy: String,
    val slipNumber: String,
    val customerName: String,
    val totalAmount: String,
    val items: List<Item>,
    val status: String,
    val term: List<SlipTerm>,
    val termId: String,
    val phoneNumber: String,
    val userId: String,
    val userRole: String,
    val userAccess: String,
    val onDelete: (Int, Item) -> Unit,
    val slipRefresh: (Int) -> Unit,
    val onUpdate: (Item) -> Unit = {},
    val onShowDeleteConfirmation: (Int, Item, (Int, Item) -> Unit) -> Unit = { _, _, _ -> }
)

@Composable
fun SlipDetailContent(
    props: SlipDetailProps,
    userProfile: UserProfile? = null,
    printWidth: String = "",
    printerName: String = "",
    isTwoColumn: Boolean = false,
    footerText: String = "",
    fontSize: Int = 12,
    showSummary: Boolean = false,
    showPrintTime: Boolean = false,
    showBusinessName: Boolean = false,
    showEmployeeName: Boolean = false,
    showTermName: Boolean = false
) {
    var selectedRowIndex by remember { mutableStateOf(-1) }
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Initialize selected row when items change
    LaunchedEffect(props.items) {
        selectedRowIndex = if (props.items.isNotEmpty()) 0 else -1
    }

    // Handle keyboard navigation
    fun handleKeyEvent(keyEvent: KeyEvent): Boolean {
        if (props.items.isEmpty()) return false

        when (keyEvent.key) {
            Key.DirectionDown -> {
                if (selectedRowIndex < props.items.size - 1) {
                    selectedRowIndex++
                    coroutineScope.launch {
                        listState.animateScrollToItem(selectedRowIndex)
                    }
                }
                return true
            }
            Key.DirectionUp -> {
                if (selectedRowIndex > 0) {
                    selectedRowIndex--
                    coroutineScope.launch {
                        listState.animateScrollToItem(selectedRowIndex)
                    }
                }
                return true
            }
            Key.Enter -> {
                if (selectedRowIndex >= 0 && selectedRowIndex < props.items.size) {
                    handleUpdate(selectedRowIndex, props.items[selectedRowIndex], props.onUpdate)
                }
                return true
            }
            Key.Delete -> {
                if (selectedRowIndex >= 0 && selectedRowIndex < props.items.size) {
                    handleDeleteClick(selectedRowIndex, props.items[selectedRowIndex], props.onShowDeleteConfirmation, props.onDelete)
                }
                return true
            }
        }
        return false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    handleKeyEvent(keyEvent)
                } else false
            }
    ) {
        // Header Information
        Column(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Print Copy [ ${props.copy} ]",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "SMS Copy [ ${props.smsCopy} ]",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ထိုးသား: ${props.customerName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ယူနစ်ပေါင်း : ${props.totalAmount.toString().replace(Regex("(\\d)(?=(\\d{3})+(?!\\d))"), "$1,")}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            // Viber Button
            Button(
                onClick = {
                    val content = generateSlipContent(
                        props.items,
                        props.slipNumber,
                        userProfile,
                        props.term,
                        props.termId,
                        isTwoColumn,
                        showBusinessName,
                        showTermName
                    )
                    clipboardManager.setText(AnnotatedString(content))
                    // TODO: Open Viber app
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    painter = painterResource("viber.png"),
                    contentDescription = "Viber",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Copy Button
            Button(
                onClick = {
                    val content = generateSlipContent(
                        props.items,
                        props.slipNumber,
                        userProfile,
                        props.term,
                        props.termId,
                        isTwoColumn,
                        showBusinessName,
                        showTermName
                    )
                    clipboardManager.setText(AnnotatedString(content))
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    painter = painterResource("copy_icon.svg"),
                    contentDescription = "Copy",
                    tint = Color.Blue,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Print Button
            Button(
                onClick = {
                    // TODO: Implement print functionality
                    props.slipRefresh((Math.random() * 1000).toInt())
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    painter = painterResource("print_icon.svg"),
                    contentDescription = "Print",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            // SMS Button
            Button(
                onClick = {
                    // TODO: Implement SMS functionality
                    props.slipRefresh((Math.random() * 1000).toInt())
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
            ) {
                Icon(
                    painter = painterResource("sms_icon.svg"),
                    contentDescription = "SMS",
                    tint = Color.Blue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Table
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray)
                    .border(1.dp, Color.Gray),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "နံပါတ်",
                    modifier = Modifier
                        .weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ယူနစ်",
                    modifier = Modifier
                        .weight(1.5f),
                    textAlign = TextAlign.End,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "မှတ်ချက်",
                    modifier = Modifier
                        .weight(1.5f),
                    textAlign = TextAlign.End,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (props.userRole == "owner" || (props.userRole == "employee" && props.userAccess == "1")) {
                    Text(
                        text = "",
                        modifier = Modifier
                            .weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Table Body
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(props.items) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (index == selectedRowIndex) Color(0xFFE3F2FD)
                                else if (index % 2 == 0) Color.White else Color(0xFFF5F5F5)
                            )
                            .border(1.dp, Color.Gray)
                            .clickable { selectedRowIndex = index },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.number,
                            modifier = Modifier
                                .weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp
                        )
                        Text(
                            text = item.amount.toString().replace(Regex("(\\d)(?=(\\d{3})+(?!\\d))"), "$1,"),
                            modifier = Modifier
                                .weight(1.5f),
                            textAlign = TextAlign.End,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (item.showSummary == "1") item.summary else "",
                            modifier = Modifier
                                .weight(1.5f),
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp
                        )
                        if (props.userRole == "owner" || (props.userRole == "employee" && props.userAccess == "1")) {
                            Row(
                                modifier = Modifier
                                    .weight(1f),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = { handleUpdate(index, item, props.onUpdate) }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.Blue
                                    )
                                }
                                IconButton(
                                    onClick = { handleDeleteClick(index, item, props.onShowDeleteConfirmation, props.onDelete) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Request focus when component is first composed
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

internal fun handleUpdate(index: Int, item: Item, onUpdate: (Item) -> Unit) {
    // Dispatch modal opening and add slip single update data
    onUpdate(item)
}

internal fun handleDeleteClick(index: Int, item: Item, onShowDeleteConfirmation: (Int, Item, (Int, Item) -> Unit) -> Unit, onDelete: (Int, Item) -> Unit) {
    // Show confirmation dialog before deleting
    onShowDeleteConfirmation(index, item, onDelete)
}

private fun generateSlipContent(
    items: List<Item>,
    slipNumber: String,
    userProfile: UserProfile?,
    terms: List<SlipTerm>,
    termId: String,
    isTwoColumn: Boolean,
    showBusinessName: Boolean,
    showTermName: Boolean
): String {
    val businessName = if (showBusinessName) "${userProfile?.businessName}\n" else ""
    val termName = if (showTermName) {
        "${terms.find { it.value == termId }?.label ?: ""}\n"
    } else ""
    val slipId = "Slip No: $slipNumber\n"
    
    val formattedContent = if (isTwoColumn) {
        val rows = items.map { "${it.number}-${it.amount}" }
        buildString {
            for (i in rows.indices step 2) {
                val row1 = rows.getOrNull(i) ?: ""
                val row2 = rows.getOrNull(i + 1) ?: ""
                appendLine("$row1 | $row2")
            }
        }
    } else {
        items.joinToString("\n") { "${it.number}-${it.amount}" } + "\n"
    }
    
    val totalAmount = items.sumOf { it.amount }
    return "$businessName$termName$slipId$formattedContent\nTotal - ${totalAmount.toString().replace(Regex("(\\d)(?=(\\d{3})+(?!\\d))"), "$1,")}"
}