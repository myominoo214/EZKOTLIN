package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.*

data class TableCell(
    val n: String,
    val u: String
)

@Composable
fun DynamicTable(
    totalTable: List<List<List<TableCell>>>,
    tableHeader: List<Int>,
    setCurrent: (Int) -> Unit,
    currentT: Int = 0
) {
    var currentIndex by remember { mutableStateOf(currentT) }
    var tableData by remember { mutableStateOf(totalTable.getOrNull(currentIndex) ?: emptyList()) }
    
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }
    
    fun calculateNextTable() {
        if (currentIndex < totalTable.size - 1) {
            val newIndex = currentIndex + 1
            currentIndex = newIndex
            setCurrent(newIndex)
            tableData = totalTable[newIndex]
        }
    }
    
    fun calculatePreviousTable() {
        if (currentIndex > 0) {
            val newIndex = currentIndex - 1
            currentIndex = newIndex
            setCurrent(newIndex)
            tableData = totalTable[newIndex]
        }
    }
    
    fun calculateTotalAndPercentage(): Pair<Int, Double> {
        var total = 0
        totalTable.getOrNull(currentIndex)?.forEach { row ->
            row.forEach { cell ->
                total += cell.u.toIntOrNull() ?: 0
            }
        }
        val totalPercentage = if (tableHeader.getOrNull(currentIndex) != null && tableHeader[currentIndex] > 0) {
            total.toDouble() / tableHeader[currentIndex]
        } else 0.0
        return Pair(total, totalPercentage)
    }
    
    LaunchedEffect(totalTable, tableHeader) {
        tableData = totalTable.getOrNull(currentIndex) ?: emptyList()
        println("[DEBUG] DynamicTable - totalTable size: ${totalTable.size}")
        println("[DEBUG] DynamicTable - currentIndex: $currentIndex")
        println("[DEBUG] DynamicTable - tableData size: ${tableData.size}")
        if (tableData.isNotEmpty()) {
            println("[DEBUG] DynamicTable - first row size: ${tableData[0].size}")
            println("[DEBUG] DynamicTable - sample cell: ${tableData[0].getOrNull(0)}")
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        val (total, totalPercentage) = calculateTotalAndPercentage()
        Text(
            text = "${formatter.format(tableHeader.getOrNull(currentIndex) ?: 0)}B - Total: ${formatter.format(total)}(${String.format("%.2f", totalPercentage * 100)}%)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Navigation Controls
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { calculatePreviousTable() },
                enabled = currentIndex > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("<")
            }
            
            Text(
                text = "${currentIndex + 1} of ${totalTable.size}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = { calculateNextTable() },
                enabled = currentIndex < totalTable.size - 1,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(">")
            }
        }
        
        // Table
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RectangleShape
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                // Table Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        repeat(10) { index ->
                            Text(
                                text = "N",
                                modifier = Modifier
                                    .width(35.dp)
                                    .height(30.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                                    .padding(2.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "U",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                                    .padding(2.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                // Table Body
                itemsIndexed(tableData) { rowIndex, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (rowIndex % 2 == 0) 
                                    MaterialTheme.colorScheme.surface 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                    ) {
                        row.forEach { cell ->
                            Box(
                                modifier = Modifier
                                    .width(35.dp)
                                    .height(30.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cell.n,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cell.u,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}