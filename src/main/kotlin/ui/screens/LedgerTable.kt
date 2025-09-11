package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.launch
import java.text.NumberFormat
import kotlin.math.max
import kotlin.math.min

data class PreparedDataItem(
    val number: String,
    val unit: Int,
    val extra: Int,
    val original: LedgerItem
)

enum class SortColumn { NUMBER, UNIT }
enum class SortOrder { ASC, DESC }

data class ColumnWidths(
    val number: Float = 100f,
    val unit: Float = 150f,
    val extra: Float = 150f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerTableComponent(
    modifier: Modifier = Modifier,
    is2D: Boolean,
    ledgerData: List<LedgerItem>,
    breakAmount: Int,
    handleRowClick: (LedgerItem) -> Unit,
    setExtraData: (List<FormatData>) -> Unit,
    setUnitData: (List<FormatData>) -> Unit,
    toTop: String,
    tempBreakAmount: Int,
    isTempBreak: Boolean
) {
    var sortColumn by remember { mutableStateOf(SortColumn.UNIT) }
    var sortOrder by remember { mutableStateOf(SortOrder.DESC) }
    var columnWidths by remember { mutableStateOf(ColumnWidths()) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Generate number list
    val numberList = remember(is2D) {
        if (is2D) {
            (0..99).map { it.toString().padStart(2, '0') }
        } else {
            (0..999).map { it.toString().padStart(3, '0') }
        }
    }
    
    // Prepare data
    val preparedData = remember(ledgerData, breakAmount, numberList, isTempBreak, tempBreakAmount) {
        val effectiveBreakAmount = if (isTempBreak) tempBreakAmount else breakAmount
        
        val prepared = numberList.map { number ->
            val ledger = ledgerData.find { it.number == number }
            val totalAmount = ledger?.totalAmount ?: 0
            PreparedDataItem(
                number = number,
                unit = min(totalAmount, effectiveBreakAmount),
                extra = max(totalAmount - effectiveBreakAmount, 0),
                original = ledger ?: LedgerItem(number, 0)
            )
        }
        
        val extraDataArray = prepared
            .filter { it.extra > 0 }
            .map { item ->
                FormatData(
                    amount = item.extra.toString(),
                    delete = false,
                    groupId = (Math.random() * 1000000).toInt(),
                    number = item.number,
                    showSummary = "1",
                    summary = item.extra.toString()
                )
            }
        
        val unitDataArray = prepared
            .filter { it.unit > 0 }
            .map { item ->
                FormatData(
                    amount = item.unit.toString(),
                    delete = false,
                    groupId = (Math.random() * 1000000).toInt(),
                    number = item.number,
                    showSummary = "1",
                    summary = item.unit.toString()
                )
            }
        
        setUnitData(unitDataArray)
        setExtraData(extraDataArray)
        prepared
    }
    
    // Sorted data
    val sortedData = remember(preparedData, sortColumn, sortOrder) {
        preparedData.sortedWith { a, b ->
            val valueA = when (sortColumn) {
                SortColumn.NUMBER -> a.number.toInt()
                SortColumn.UNIT -> (a.unit + a.extra).toInt()
            }
            val valueB = when (sortColumn) {
                SortColumn.NUMBER -> b.number.toInt()
                SortColumn.UNIT -> (b.unit + b.extra).toInt()
            }
            when (sortOrder) {
                SortOrder.ASC -> valueA.compareTo(valueB)
                SortOrder.DESC -> valueB.compareTo(valueA)
            }
        }
    }
    
    // Scroll to top when requested
    LaunchedEffect(toTop) {
        if (toTop == "top") {
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        }
    }
    
    // Handle sorting
    fun handleSort(column: SortColumn) {
        if (sortColumn == column) {
            sortOrder = if (sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
        } else {
            sortColumn = column
            sortOrder = SortOrder.ASC
        }
    }
    
    // Get row background color based on total amount
    fun getRowBackgroundColor(totalAmount: Double): Color {
        return when {
            totalAmount >= breakAmount -> Color(0xFFDC2626) // red-600
            totalAmount >= breakAmount * 0.7 -> Color(0xFF9333EA) // purple-600
            totalAmount >= breakAmount * 0.4 -> Color(0xFF16A34A) // green-600
            else -> Color.White
        }
    }
    
    fun getRowTextColor(totalAmount: Double): Color {
        return when {
            totalAmount >= breakAmount * 0.4 -> Color.White
            else -> Color.Black
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(500.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RectangleShape
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFDBEAFE)) // blue-100
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Number column header
                Row(
                    modifier = Modifier
                        .width(columnWidths.number.dp)
                        .clickable { handleSort(SortColumn.NUMBER) }
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Num",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (sortColumn == SortColumn.NUMBER) {
                        Icon(
                            imageVector = if (sortOrder == SortOrder.ASC) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Unit column header
                Row(
                    modifier = Modifier
                        .width(columnWidths.unit.dp)
                        .clickable { handleSort(SortColumn.UNIT) }
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unit",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (sortColumn == SortColumn.UNIT) {
                        Icon(
                            imageVector = if (sortOrder == SortOrder.ASC) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Extra column header
                Text(
                    text = "Extra",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .width(columnWidths.extra.dp)
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
            
            Divider()
            
            // Table content
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(sortedData) { index, data ->
                    val backgroundColor = getRowBackgroundColor(data.original.totalAmount.toDouble())
                    val textColor = getRowTextColor(data.original.totalAmount.toDouble())
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor)
                            .clickable { handleRowClick(data.original) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Number cell
                        Text(
                            text = data.number,
                            color = textColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(columnWidths.number.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        
                        // Unit cell
                        Text(
                            text = NumberFormat.getInstance().format(data.unit.toInt()),
                            color = textColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .width(columnWidths.unit.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        
                        // Extra cell
                        Text(
                            text = NumberFormat.getInstance().format(data.extra.toInt()),
                            color = textColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .width(columnWidths.extra.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    if (index < sortedData.size - 1) {
                        Divider(color = Color.Gray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}