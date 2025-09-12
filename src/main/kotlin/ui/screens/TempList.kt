package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.res.painterResource
// import androidx.compose.material.icons.filled.DeleteSweep // Not available in desktop Compose
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.lifecycle.ViewModel // Not available in desktop Compose
// import androidx.lifecycle.viewmodel.compose.viewModel // Not available in desktop Compose
import core.stores.TempListStore
import core.stores.rememberTempListStore
import androidx.compose.runtime.collectAsState

// Data Models
data class TempListItem(
    val number: String,
    val amount: String,
    val summary: String = "",
    val showSummary: String = "0",
    val groupId: String? = null
)

// Action Buttons Component
@Composable
fun ActionButtons(
    onDelete: () -> Unit,
    onDeleteGroup: () -> Unit,
    listType: String = "SELL",
    modifier: Modifier = Modifier
) {
    val iconTint = if (listType == "BUY") Color.White else MaterialTheme.colorScheme.error
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Item",
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(
            onClick = onDeleteGroup,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource("remove-all-icon.png"),
                contentDescription = "Delete Group",
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// Row Component
@Composable
fun TempListRow(
    item: TempListItem,
    index: Int,
    onDelete: () -> Unit,
    onDeleteGroup: () -> Unit,
    listType: String = "SELL",
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        listType == "BUY" -> Color.Red
        index % 2 == 0 -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    
    val textColor = if (listType == "BUY") Color.White else MaterialTheme.colorScheme.onSurface
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(backgroundColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Number Column
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.number,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        
        // Amount Column
        Box(
            modifier = Modifier
                .weight(1.7f)
                .fillMaxHeight()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = item.amount,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        
        // Summary Column
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (item.showSummary == "1") item.summary else "",
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        
        // Actions Column
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            ActionButtons(
                onDelete = onDelete,
                onDeleteGroup = onDeleteGroup,
                listType = listType,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// Header Component
@Composable
fun TempListHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "နံပါတ်", // Myanmar: Number
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        Text(
            text = "ယူနစ်", // Myanmar: Unit
            modifier = Modifier
                .weight(1.7f)
                .padding(8.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        Text(
            text = "Summary",
            modifier = Modifier
                .weight(1.3f)
                .padding(8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        Text(
            text = "",
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// Main TempList Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempList(
    store: TempListStore = rememberTempListStore()
) {
    val state by store.state.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll to bottom when items are added
    LaunchedEffect(state.items.size) {
        if (state.items.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(state.items.size - 1)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
    ) {
        // Table Header
        TempListHeader(
            modifier = Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            )
        )
        
        // Table Content
        if (state.items.isNotEmpty()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (state.listType == "BUY") 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        else 
                            MaterialTheme.colorScheme.surface
                    )
            ) {
                itemsIndexed(
                    items = state.items,
                    key = { index, item -> "${item.groupId ?: "item"}_${index}_${item.number}_${item.amount}" }
                ) { index, item ->
                    TempListRow(
                        item = item,
                        index = index,
                        onDelete = { store.deleteItem(index) },
                        onDeleteGroup = { store.deleteGroup(index) },
                        listType = state.listType
                    )
                }
            }
        } else {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

// Preview for development
@Composable
fun TempListPreview() {
    MaterialTheme {
        val store = rememberTempListStore()
        
        // Add sample data
        LaunchedEffect(Unit) {
            store.addItem(TempListItem("123", "1000", "Sample Summary", "1", "group1"))
            store.addItem(TempListItem("456", "2000", "Another Summary", "1", "group2"))
            store.addItem(TempListItem("789", "3000", "", "0", "group3"))
        }
        
        TempList(store)
    }
}