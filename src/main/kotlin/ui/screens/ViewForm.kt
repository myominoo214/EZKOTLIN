package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.builtins.ListSerializer
import java.text.NumberFormat
import java.util.*
import kotlin.math.round
import core.services.ApiService
import core.services.UserSession
import ui.screens.LedgerApiResponseData

// Custom serializer to handle breakAmount as either Int or decimal string
object BreakAmountSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BreakAmount", PrimitiveKind.INT)
    
    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }
    
    override fun deserialize(decoder: Decoder): Int {
        return when (decoder) {
            is JsonDecoder -> {
                val element = decoder.decodeJsonElement()
                when {
                    element.jsonPrimitive.isString -> {
                        // Handle decimal string like "1000.00"
                        val stringValue = element.jsonPrimitive.content
                        try {
                            // Parse as double first, then convert to int to handle decimal strings
                            stringValue.toDouble().toInt()
                        } catch (e: NumberFormatException) {
                            // Fallback to 0 if parsing fails
                            0
                        }
                    }
                    else -> {
                        // Handle regular integer
                        element.jsonPrimitive.content.toInt()
                    }
                }
            }
            else -> decoder.decodeInt()
        }
    }
}

@Serializable
data class SubOwner(
    val userId: String,
    val name: String
)

@Serializable
data class LedgerData(
    val number: String,
    val totalAmount: Int
)

@Serializable
data class CutOrDivideData(
    val cutOrDivideKey: String,
    val type: String,
    @Serializable(with = BreakAmountSerializer::class)
    val breakAmount: Int,
    val slipId: String? = null,
    val userId: String? = null,
    val unit: Int = 0
)

@Serializable
data class SubOwnerOption(
    val value: String,
    val label: String
)

// ApiResponse is defined in ApiService.kt

@Serializable
data class SubOwnersResponse(
    val by: List<SubOwner>
)

@Serializable
data class LedgerResponse(
    val by: List<LedgerDataResponse>
)

@Serializable
data class LedgerDataResponse(
    val number: String,
    val TotalAmount: Int
)

// TableCell is defined in DynamicTable.kt

data class InternalTableCell(
    val n: String,
    val u: Int,
    val remainingUnit: Int = 0
)

data class CalculateCutDataResult(
    val cutTableData: List<List<List<InternalTableCell>>>,
    val remainingTableData: Any, // Can be List<List<List<InternalTableCell>>> for 2D or List<InternalTableCell> for 3D
    val remainingTableDataTmp: List<List<List<InternalTableCell>>>
)

// Helper functions to convert between InternalTableCell and TableCell
fun InternalTableCell.toTableCell(): TableCell = TableCell(n = this.n, u = this.u.toString())
fun List<List<List<InternalTableCell>>>.toTableCellList(): List<List<List<TableCell>>> = 
    this.map { it.map { row -> row.map { cell -> cell.toTableCell() } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewForm(
    is2D: Boolean,
    breakAmount: Int,
    termId: String,
    termName: String,
    setBreakAmount: (Int) -> Unit,
    handleRefreshLedger: () -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var subOwners by remember { mutableStateOf<List<SubOwner>>(emptyList()) }
    var subOwnersOpt by remember { mutableStateOf<List<SubOwnerOption>>(emptyList()) }
    var subOwnersVal by remember { mutableStateOf("all") }
    var updatedId by remember { mutableStateOf("") }
    var disableBuy by remember { mutableStateOf(false) }
    var ledgerData by remember { mutableStateOf<List<LedgerDataResponse>>(emptyList()) }
    var tableData by remember { mutableStateOf<List<List<List<TableCell>>>>(emptyList()) }
    var tableHeader by remember { mutableStateOf<List<Int>>(emptyList()) }
    var tmpCODKey by remember { mutableStateOf<List<String>>(emptyList()) }
    var cutOrDivideData by remember { mutableStateOf<List<CutOrDivideData>>(emptyList()) }
    var current by remember { mutableStateOf(0) }
    var refresh by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val apiService = remember { ApiService() }
    val userSession = remember { UserSession.getInstance() }
    
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }
    
    // Data fetching functions
    suspend fun getCutOrDivide() {
        try {
            val response = apiService.get<List<CutOrDivideData>>(
                url = "${ApiService.BASE_URL}/v1/ledger/getCutOrDivide?termId=$termId",
                headers = userSession.getAuthHeaders()
            )
            println("url ==>> ${ApiService.BASE_URL}/v1/ledger/getCutOrDivide?termId=$termId")
            println("CutOrDivide Response: ${response}")
            if (response.success && response.data != null) {
                cutOrDivideData = response.data
            } else {
                cutOrDivideData = emptyList()
            }
        } catch (e: Exception) {
            println("Error fetching CutOrDivide data: ${e.message}")
            cutOrDivideData = emptyList()
        }
    }
    
    suspend fun fetchLedgerData() {
        try {
            val limit = if (is2D) "100" else "1000"
            val url = "${ApiService.BASE_URL}/v1/ledger/getLedgers?termId=$termId&limit=$limit"
            val response = apiService.get<LedgerApiResponseData>(
                url = url,
                headers = userSession.getAuthHeaders()
            )
            if (response.success && response.data?.code == "200" && response.data.data?.by != null) {
                val cappedData = response.data.data.by.map { item ->
                    LedgerDataResponse(
                        number = item.number ?: "",
                        TotalAmount = when {
                            (item.TotalAmount ?: 0.0).toInt() > breakAmount -> breakAmount
                            (item.TotalAmount ?: 0.0).toInt() < 0 -> 0
                            else -> (item.TotalAmount ?: 0.0).toInt()
                        }
                    )
                }
                ledgerData = cappedData
                println("[DEBUG] Fetched ${ledgerData.size} ledger data items")
            } else {
                ledgerData = emptyList()
            }
        } catch (e: Exception) {
            println("Error fetching ledger data: ${e.message}")
            ledgerData = emptyList()
        }
    }
    
    suspend fun fetchSlipDetail(slipId: String?, termId: String, userId: String?): List<LedgerDataResponse> {
        return try {
            if (slipId.isNullOrEmpty() || userId.isNullOrEmpty()) {
                return emptyList()
            }
            
            val response = apiService.getSlipDetails(
                slipId = slipId,
                termId = termId,
                userId = userId
            )
            println("getSlipDetails Respone ==>> ${response}")
            if (response.success) {
                // Parse the slip details from JsonElement
                val slipDetails = response.data?.data?.let { jsonElement ->
                    try {
                        val lenientJson = kotlinx.serialization.json.Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                            encodeDefaults = false
                        }
                        lenientJson.decodeFromJsonElement(
                            kotlinx.serialization.builtins.ListSerializer(ApiService.SlipDetailResponseData.serializer()),
                            jsonElement
                        )
                    } catch (e: Exception) {
                        println("[DEBUG] Failed to parse slip details: ${e.message}")
                        emptyList<ApiService.SlipDetailResponseData>()
                    }
                } ?: emptyList()
                
                // Convert slip details to ledger format (number and TotalAmount)
                slipDetails.map { slip ->
                    LedgerDataResponse(
                        number = slip.number,
                        TotalAmount = slip.amount * -1  // Multiply by -1 as in JavaScript
                    )
                }
            } else {
                emptyList()
            }
        } catch (error: Exception) {
            println("Error fetching slip detail: ${error.message}")
            emptyList()
        }
    }
    
    suspend fun fetchSubOwners() {
        try {
            val response = apiService.get<SubOwnersResponse>(
                url = "${ApiService.BASE_URL}/v1/account/getSubOwners?limit=1000",
                headers = userSession.getAuthHeaders()
            )
            if (response.success && response.data != null) {
                subOwners = response.data.by
                val tmpData = subOwners.map { subOwner ->
                    SubOwnerOption(
                        value = subOwner.userId,
                        label = subOwner.name
                    )
                }.toMutableList()
                tmpData.add(0, SubOwnerOption("all", "All"))
                subOwnersOpt = tmpData
                if (tmpData.isNotEmpty() && updatedId.isEmpty()) {
                    subOwnersVal = "all"
                }
            }
        } catch (e: Exception) {
            println("Error fetching sub owners: ${e.message}")
        } finally {
            loading = false
        }
    }
    

    
    fun calculateUnitCut(unit: Int, cutUnit: Int, cutBreak: Int): Int {
        if (unit != 0 && unit <= cutUnit) {
            return 0
        }
        if (unit > 0) {
            val first = unit - cutUnit
            val second = first - cutBreak
            
            val firstResult: Int
            val secondResult: Int
            
            if (second < cutBreak) {
                if (second < 0) {
                    firstResult = first
                    secondResult = cutUnit
                } else {
                    firstResult = cutBreak
                    secondResult = second + cutUnit
                }
            } else {
                firstResult = cutBreak
                secondResult = second + cutUnit
            }
            
            return firstResult
        }
        return 0
    }
    
    fun calculatePercentage(x: Int, y: Int): Double {
        return (x.toDouble() / y.toDouble()) * 100.0
    }
    
    fun calculateUnit(u: Int, p: Double): Int {
        return kotlin.math.round((u.toDouble() * p) / 100.0).toInt()
    }
    
    fun distributeBreakAmount(objects: MutableList<CutOrDivideData>, totalBreakAmount: Int, termId: String): List<CutOrDivideData> {
        // Calculate the current sum of breakAmount for objects without slipId
        val currentTotal = objects.filter { it.slipId.isNullOrEmpty() }
            .sumOf { it.breakAmount }
        
        println("currentTotal: $currentTotal")
        println("totalBreakAmount: $totalBreakAmount")
        
        // Calculate the remaining amount
        if (totalBreakAmount > currentTotal) {
            val remainingAmount = totalBreakAmount - currentTotal
            // If there's a remaining amount, add a new object
            if (remainingAmount > 0) {
                objects.add(
                    CutOrDivideData(
                        cutOrDivideKey = "", // Will be set later
                        type = "divide",
                        breakAmount = remainingAmount,
                        slipId = null,
                        userId = null,
                        unit = 0
                    )
                )
            }
        }
        
        return objects
    }
    
    suspend fun calculateCutData(
        cutData: List<CutOrDivideData>,
        ledgerData: List<LedgerDataResponse>,
        is2D: Boolean
    ): CalculateCutDataResult {
        val cutTableData = mutableListOf<List<List<InternalTableCell>>>()
        var remainingTableData: Any = emptyList<InternalTableCell>()
        val remainingTableDataTmp = mutableListOf<List<List<InternalTableCell>>>()
        
        for (i in cutData.indices) {
            val cutItem = cutData[i]
            
            if (!cutItem.slipId.isNullOrEmpty()) {
                // Fetch slip detail data
                val tmpLedgerData = fetchSlipDetail(cutItem.slipId, termId, cutItem.userId)
                
                if (is2D) {
                    val row2D = (0 until 10).map { rowIndex ->
                        (0 until 10).map { colIndex ->
                            val n = String.format("%02d", rowIndex + colIndex * 10)
                            val ledger = tmpLedgerData.find { it.number == n }
                            val ledgerUnit = ledger?.TotalAmount ?: 0
                            
                            val orgLedger = tmpLedgerData.find { it.number == n }
                            val orgLedgerUnit = orgLedger?.TotalAmount ?: 0
                            
                            // Calculate Remaining Unit
                            val remainingUnit = orgLedgerUnit - ledgerUnit
                            
                            InternalTableCell(
                                n = n,
                                u = orgLedgerUnit,
                                remainingUnit = ledgerUnit
                            )
                        }
                    }
                    
                    cutTableData.add(row2D)
                    remainingTableDataTmp.add(
                        row2D.map { row ->
                            row.map { cell ->
                                InternalTableCell(
                                    n = cell.n,
                                    u = maxOf(cell.remainingUnit, 0)
                                )
                            }
                        }
                    )
                } else {
                    val row3D = (0 until 100).map { rowIndex ->
                        (0 until 10).map { colIndex ->
                            val n = String.format("%03d", rowIndex + colIndex * 100)
                            val ledger = ledgerData.find { it.number == n }
                            val ledgerUnit = ledger?.TotalAmount ?: 0
                            
                            val orgLedger = tmpLedgerData.find { it.number == n }
                            val orgLedgerUnit = orgLedger?.let { it.TotalAmount * -1 } ?: 0
                            
                            InternalTableCell(
                                n = n,
                                u = orgLedgerUnit,
                                remainingUnit = ledgerUnit
                            )
                        }
                    }
                    
                    cutTableData.add(row3D)
                    remainingTableDataTmp.add(
                        row3D.map { row ->
                            row.map { cell ->
                                InternalTableCell(
                                    n = cell.n,
                                    u = maxOf(cell.remainingUnit, 0)
                                )
                            }
                        }
                    )
                    remainingTableData = row3D.flatten().map { cell ->
                        InternalTableCell(
                            n = cell.n,
                            u = maxOf(cell.remainingUnit, 0)
                        )
                    }
                }
            } else {
                // Calculate cut data without slip
                if (is2D) {
                    val row2D = (0 until 10).map { rowIndex ->
                        (0 until 10).map { colIndex ->
                            val n = String.format("%02d", rowIndex + colIndex * 10)
                            val ledger = ledgerData.find { it.number == n }
                            val ledgerUnit = ledger?.TotalAmount ?: 0
                            val cutUnit = cutItem.unit
                            val breakAmount = cutItem.breakAmount
                            
                            // Calculate Cut Unit
                            val cutValue = calculateUnitCut(ledgerUnit, cutUnit, breakAmount)
                            
                            // Calculate Remaining Unit
                            val remainingUnit = ledgerUnit - cutValue
                            
                            InternalTableCell(
                                n = n,
                                u = cutValue,
                                remainingUnit = remainingUnit
                            )
                        }
                    }
                    
                    cutTableData.add(row2D)
                    remainingTableDataTmp.add(
                        row2D.map { row ->
                            row.map { cell ->
                                InternalTableCell(
                                    n = cell.n,
                                    u = maxOf(cell.remainingUnit, 0)
                                )
                            }
                        }
                    )
                } else {
                    val row3D = (0 until 100).map { rowIndex ->
                        (0 until 10).map { colIndex ->
                            val n = String.format("%03d", rowIndex + colIndex * 100)
                            val ledger = ledgerData.find { it.number == n }
                            val ledgerUnit = ledger?.TotalAmount ?: 0
                            val cutUnit = cutItem.unit
                            val breakAmount = cutItem.breakAmount
                            
                            // Calculate Cut Unit
                            val cutValue = calculateUnitCut(ledgerUnit, cutUnit, breakAmount)
                            
                            // Calculate Remaining Unit
                            val remainingUnit = ledgerUnit - cutValue
                            
                            InternalTableCell(
                                n = n,
                                u = cutValue,
                                remainingUnit = remainingUnit
                            )
                        }
                    }
                    
                    cutTableData.add(row3D)
                    remainingTableDataTmp.add(
                        row3D.map { row ->
                            row.map { cell ->
                                InternalTableCell(
                                    n = cell.n,
                                    u = maxOf(cell.remainingUnit, 0)
                                )
                            }
                        }
                    )
                    remainingTableData = row3D.flatten().map { cell ->
                        InternalTableCell(
                            n = cell.n,
                            u = maxOf(cell.remainingUnit, 0)
                        )
                    }
                }
            }
        }
        
        return CalculateCutDataResult(
            cutTableData = cutTableData,
            remainingTableData = remainingTableData,
            remainingTableDataTmp = remainingTableDataTmp
        )
    }
    
    suspend fun calculateDivideData(
        divideData: List<CutOrDivideData>,
        remainingTableData: Any,
        is2D: Boolean,
        breakAmount: Int,
        termId: String
    ): List<List<List<InternalTableCell>>> {
        val tmpData = mutableListOf<List<List<InternalTableCell>>>()
        
        // Distribute break amount
        val distributedDivideData = distributeBreakAmount(
            divideData.toMutableList(),
            breakAmount,
            termId
        )
        
        println("Calculating divide data, breakAmount: $breakAmount")
        
        for (i in distributedDivideData.indices) {
            val divideItem = distributedDivideData[i]
            
            if (!divideItem.slipId.isNullOrEmpty()) {
                // Fetch slip detail data
                val tmpLedgerData = fetchSlipDetail(divideItem.slipId, termId, divideItem.userId)
                
                if (is2D) {
                    val row2D = (0 until 10).map { rowIndex ->
                        (0 until 10).map { colIndex ->
                            val n = String.format("%02d", rowIndex + colIndex * 10)
                            val ledger = tmpLedgerData.find { it.number == n }
                            val ledgerUnit = ledger?.TotalAmount ?: 0
                            
                            InternalTableCell(n = n, u = ledgerUnit, remainingUnit = 0)
                        }
                    }
                    tmpData.add(row2D)
                } else {
                    val row3D = (0 until 100).map { rowIndex ->
                        (0 until 10).map { colIndex ->
                            val n = String.format("%03d", rowIndex + colIndex * 100)
                            val ledger = tmpLedgerData.find { it.number == n }
                            val ledgerUnit = ledger?.TotalAmount ?: 0
                            
                            InternalTableCell(n = n, u = ledgerUnit, remainingUnit = 0)
                        }
                    }
                    tmpData.add(row3D)
                }
            } else {
                // Calculate divide data without slip
                if (is2D) {
                    val row2D = (0 until 10).map { rowIndex ->
                        (0 until 10).map { colIndex ->
                            val n = String.format("%02d", rowIndex + colIndex * 10)
                            
                            // Find the ledger data from remainingTableData
                            val ledger = when (remainingTableData) {
                                is List<*> -> {
                                    if (remainingTableData.isNotEmpty() && remainingTableData[0] is List<*>) {
                                        // 2D structure: List<List<InternalTableCell>>
                                        @Suppress("UNCHECKED_CAST")
                                        val tableData = remainingTableData as List<List<InternalTableCell>>
                                        tableData.flatten().find { it.n == n }
                                    } else {
                                        // 1D structure: List<InternalTableCell>
                                        @Suppress("UNCHECKED_CAST")
                                        val tableData = remainingTableData as List<InternalTableCell>
                                        tableData.find { it.n == n }
                                    }
                                }
                                else -> null
                            }
                            
                            val originalUnit = ledger?.u ?: 0
                            
                            // Calculate divide amount based on breakAmount percentage
                            val dividedUnit = calculateUnit(
                                originalUnit,
                                calculatePercentage(divideItem.breakAmount, breakAmount)
                            )
                            
InternalTableCell(n = n, u = dividedUnit, remainingUnit = 0)
                        }
                    }
                    tmpData.add(row2D)
                } else {
                    val row3D = (0 until 100).map { rowIndex ->
                        (0 until 10).map { colIndex ->
                            val n = String.format("%03d", rowIndex + colIndex * 100)
                            
                            // Find the ledger data from remainingTableData
                            val ledger = when (remainingTableData) {
                                is List<*> -> {
                                    if (remainingTableData.isNotEmpty() && remainingTableData[0] is List<*>) {
                                        // 2D structure: List<List<InternalTableCell>>
                                        @Suppress("UNCHECKED_CAST")
                                        val tableData = remainingTableData as List<List<InternalTableCell>>
                                        tableData.flatten().find { it.n == n }
                                    } else {
                                        // 1D structure: List<InternalTableCell>
                                        @Suppress("UNCHECKED_CAST")
                                        val tableData = remainingTableData as List<InternalTableCell>
                                        tableData.find { it.n == n }
                                    }
                                }
                                else -> null
                            }
                            
                            val originalUnit = ledger?.u ?: 0
                            
                            // Calculate divide amount based on breakAmount percentage
                            val p = calculatePercentage(divideItem.breakAmount, breakAmount)
                            val dividedUnit = calculateUnit(originalUnit, p)
                            
                            InternalTableCell(n = n, u = dividedUnit, remainingUnit = 0)
                        }
                    }
                    tmpData.add(row3D)
                }
            }
        }
        
        return tmpData
    }
    
    fun generateTableData(data: List<LedgerDataResponse>, is2D: Boolean): List<List<TableCell>> {
        return if (is2D) {
            (0 until 10).map { rowIndex ->
                (0 until 10).map { colIndex ->
                    val n = (rowIndex + colIndex * 10).toString().padStart(2, '0')
                    val ledger = data.find { it.number == n }
                    TableCell(n, (ledger?.TotalAmount ?: 0).toString())
                }
            }
        } else {
            (0 until 100).map { rowIndex ->
                (0 until 10).map { colIndex ->
                    val n = (rowIndex + colIndex * 100).toString().padStart(3, '0')
                    val ledger = data.find { it.number == n }
                    TableCell(n, (ledger?.TotalAmount ?: 0).toString())
                }
            }
        }
    }
    
    fun getShareText(twoColumn: Boolean = false): String {
        var total = 0
        var copyData = ""
        val allCells = mutableListOf<TableCell>()
        
        // Flatten all cells into a single array
        if (tableData.isNotEmpty() && current < tableData.size) {
            tableData[current].forEach { row ->
                allCells.addAll(row)
            }
        }
        
        // Sort all cells by 'n'
        allCells.sortBy { it.n.toIntOrNull() ?: 0 }
        
        if (twoColumn) {
            allCells.forEachIndexed { index, cell ->
                if (index % 2 == 0) {
                    copyData += "${cell.n}=${cell.u} | "
                } else {
                    copyData += "${cell.n}=${cell.u}\n"
                }
                total += cell.u.toIntOrNull() ?: 0
            }
        } else {
            allCells.forEach { cell ->
            copyData += "${cell.n}=${cell.u}\n"
            total += cell.u.toIntOrNull() ?: 0
        }
        }
        
        val percentage = if (tableHeader.isNotEmpty() && current < tableHeader.size) {
            total.toDouble() / tableHeader[current]
        } else 0.0
        
        val txtHeader = "$termName\n${Date()}\n--------------------------\n"
        val breakAmountText = if (tableHeader.isNotEmpty() && current < tableHeader.size) {
            tableHeader[current]
        } else breakAmount
        
        return "${txtHeader}${copyData.trim()}\n\n${formatter.format(breakAmountText)} B\nTotal=${formatter.format(total)}(${String.format("%.2f", percentage)}%)"
    }
    

    
    suspend fun handleBuy() {
        if (subOwnersVal == "all") {
            snackbarHostState.showSnackbar("Please select a user")
            return
        }
        
        loading = true
        try {
            val allCells = mutableListOf<TableCell>()
            if (tableData.isNotEmpty() && current < tableData.size) {
                tableData[current].forEach { row ->
                    allCells.addAll(row)
                }
            }
            
            allCells.sortBy { it.n.toIntOrNull() ?: 0 }
            
            val payloadData = allCells.map { cell ->
                mapOf(
                    "number" to cell.n,
                    "amount" to cell.u,
                    "summary" to cell.n,
                    "showSummary" to true,
                    "groupId" to (100000..999999).random(),
                    "delete" to false
                )
            }
            
            val payload = mapOf(
                "termId" to termId,
                "status" to "BUY",
                "ledger" to payloadData,
                "userId" to subOwnersVal,
                "cutOrDivideKey" to (tmpCODKey.getOrNull(current) ?: (100000..999999).random().toString())
            )
            
            // Simulate API call
            // val response = apiService.addSlips(payload)
            // Handle success
            
            val reduceBreakAmount = if (tableHeader.isNotEmpty() && current < tableHeader.size) {
                tableHeader[current]
            } else 0
            
            setBreakAmount(breakAmount - reduceBreakAmount)
            snackbarHostState.showSnackbar("Purchase successful")
            
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Error: ${e.message}")
        } finally {
            loading = false
        }
    }
    
    suspend fun handleDeleteSlip() {
        loading = true
        try {
            // Simulate API call for deletion
            snackbarHostState.showSnackbar("Slip deleted successfully")
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Error deleting slip")
        } finally {
            loading = false
        }
    }
    
    // Initial data loading
    LaunchedEffect(termId, breakAmount, refresh) {
        scope.launch {
            loading = true
            fetchSubOwners()
            getCutOrDivide()
            fetchLedgerData()
            
            // Process data similar to JavaScript implementation
            if (cutOrDivideData.isNotEmpty() && ledgerData.isNotEmpty()) {
                // Separate cut and divide data
                val apiCutData = cutOrDivideData.filter { it.type == "cut" }
                val apiDivideData = cutOrDivideData.filter { it.type == "divide" }
                
                if (apiCutData.isNotEmpty()) {
                    // Calculate cut data first
                    tmpCODKey = mutableListOf(apiCutData[0].cutOrDivideKey)
                    
                    val cutResult = calculateCutData(apiCutData, ledgerData, is2D)
                    val boughtCutData = apiCutData.filter { !it.slipId.isNullOrEmpty() }
                    val totalCutBreakAmount = apiCutData.sumOf { it.breakAmount }
                    val remainingBreakAmount = if (boughtCutData.isNotEmpty()) breakAmount else breakAmount - totalCutBreakAmount
                    
                    if (apiDivideData.isNotEmpty()) {
                        // Call calculateDivideData if divide data exists
                        val divideResult = calculateDivideData(
                            apiDivideData,
                            cutResult.remainingTableData,
                            is2D,
                            remainingBreakAmount,
                            termId
                        )
                        
                        // Convert divide result to TableCell format
                        val divideTableData = divideResult.map { table ->
                            table.map { row ->
                                row.map { cell ->
                                    TableCell(cell.n, cell.u.toString())
                                }
                            }
                        }
                        
                        // Set table data from divide results
                        tableData = divideTableData
                        tableHeader = apiDivideData.map { it.breakAmount }
                        tmpCODKey = apiDivideData.map { it.cutOrDivideKey }
                    } else {
                        // No divide data, use remaining cut data
                        tableHeader = mutableListOf(remainingBreakAmount)
                        
                        // Convert remaining table data to TableCell format
                        val remainingTableData = cutResult.remainingTableDataTmp.map { table ->
                            table.map { row ->
                                row.map { cell ->
                                    TableCell(cell.n, cell.u.toString())
                                }
                            }
                        }
                        tableData = remainingTableData
                    }
                } else if (apiDivideData.isNotEmpty() && ledgerData.isNotEmpty()) {
                    // Only divide data exists
                    tmpCODKey = emptyList()
                    tableHeader = emptyList()
                    tableData = emptyList()
                    
                    // Transform ledger data for divide calculation
                    val transformedData = ledgerData.map { item ->
                        InternalTableCell(n = item.number, u = item.TotalAmount)
                    }
                    
                    val divideResult = calculateDivideData(
                        apiDivideData,
                        transformedData,
                        is2D,
                        breakAmount,
                        termId
                    )
                    
                    // Convert divide result to TableCell format
                    val divideTableData = divideResult.map { table ->
                        table.map { row ->
                            row.map { cell ->
                                TableCell(cell.n, cell.u.toString())
                            }
                        }
                    }
                    
                    tableData = divideTableData
                    tableHeader = apiDivideData.map { it.breakAmount }
                    tmpCODKey = apiDivideData.map { it.cutOrDivideKey }
                }
            } else if (ledgerData.isNotEmpty()) {
                // Default case: single table with breakAmount
                val rowData = generateTableData(ledgerData, is2D)
                tmpCODKey = listOf("")
                tableHeader = listOf(breakAmount)
                tableData = listOf(rowData)
            }
            
            println("[DEBUG] ViewForm - ledgerData size: ${ledgerData.size}")
            println("[DEBUG] ViewForm - tableData size: ${tableData.size}")
            if (tableData.isNotEmpty() && tableData[0].isNotEmpty()) {
                println("[DEBUG] ViewForm - first table row count: ${tableData[0].size}")
                println("[DEBUG] ViewForm - first row size: ${tableData[0][0].size}")
                println("[DEBUG] ViewForm - sample cell: ${tableData[0][0].getOrNull(0)}")
            }
            
            loading = false
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp,vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top control panel
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    // Left side - Sub Owner selection and Buy button
                    Row(
                        modifier = Modifier.weight(0.4f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sub Owner Dropdown
                        ExposedDropdownMenuBox(
                            expanded = showDropdown,
                            onExpandedChange = { showDropdown = !showDropdown },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = subOwnersOpt.find { it.value == subOwnersVal }?.label ?: "Select Sub Owner",
                                onValueChange = { },
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .height(46.dp)
                            )
                            
                            ExposedDropdownMenu(
                                expanded = showDropdown,
                                onDismissRequest = { showDropdown = false }
                            ) {
                                subOwnersOpt.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            subOwnersVal = option.value
                                            showDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Buy/Delete button
                        val currentCOD = cutOrDivideData.find { 
                            it.cutOrDivideKey == tmpCODKey.getOrNull(current) && it.slipId != null 
                        }
                        
                        if (currentCOD == null) {
                            Button(
                                onClick = {
                                    scope.launch { handleBuy() }
                                },
                                enabled = !disableBuy && !loading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Buy")
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    scope.launch { handleDeleteSlip() }
                                },
                                enabled = !loading,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    }
                    
                    // Right side - Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Viber Button
                        Button(
                            onClick = {
                                val message = getShareText()
                                clipboardManager.setText(AnnotatedString(message))
                                scope.launch {
                                    snackbarHostState.showSnackbar("Copied for Viber")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                painter = painterResource("viber.png"),
                                contentDescription = "Viber",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Viber")
                        }
                        
                        // Telegram Button
                        Button(
                            onClick = {
                                val message = getShareText()
                                clipboardManager.setText(AnnotatedString(message))
                                scope.launch {
                                    snackbarHostState.showSnackbar("Copied for Telegram")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                painter = painterResource("telegram.png"),
                                contentDescription = "Telegram",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Telegram")
                        }
                        
                        // Share Button
                        Button(
                            onClick = {
                                val message = getShareText()
                                clipboardManager.setText(AnnotatedString(message))
                                scope.launch {
                                    snackbarHostState.showSnackbar("Copied to clipboard!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(
                                painter = painterResource("copy_icon.svg"),
                                contentDescription = "Copy",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy")
                        }
                        
                        // PDF Button
                        Button(
                            onClick = {
                                val message = getShareText(true)
                                // TODO: Implement PDF generation
                                scope.launch {
                                    snackbarHostState.showSnackbar("PDF generation not implemented yet")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF")
                        }
                    }
                }
            
            // Table section
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (tableData.isNotEmpty()) {
                DynamicTable(
                    totalTable = tableData,
                    tableHeader = tableHeader,
                    setCurrent = { current = it },
                    currentT = current
                )
            }
        }
    }
}