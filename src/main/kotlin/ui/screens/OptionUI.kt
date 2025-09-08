package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.random.Random

// Helper function to generate random key
fun generateRandomKey(): Int {
    return Random.nextInt(100000, 999999)
}

data class CutOrDivideInput(
    val termId: String,
    var breakAmount: Int = 0,
    val slipId: String? = null,
    val cutOrDivideKey: Int,
    val type: String,
    val operator: String = "",
    var unit: Int = 0,
    val isNew: Boolean = true,
    var update: Boolean = false
)

data class OptionUIState(
    val loading: Boolean = false,
    val activeDivide: Boolean = false,
    val activeCut: Boolean = false,
    val remaining: Int = 0,
    val cutInputs: List<CutOrDivideInput> = emptyList(),
    val divideInputs: List<CutOrDivideInput> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionUI(
    termId: String,
    breakAmount: Int,
    onCloseModal: () -> Unit,
    onSaveSuccess: () -> Unit = {}
) {
    var state by remember {
        mutableStateOf(
            OptionUIState(
                remaining = breakAmount,
                cutInputs = listOf(
                    CutOrDivideInput(
                        termId = termId,
                        cutOrDivideKey = generateRandomKey(),
                        type = "cut"
                    )
                ),
                divideInputs = (1..5).map {
                    CutOrDivideInput(
                        termId = termId,
                        cutOrDivideKey = generateRandomKey(),
                        type = "divide"
                    )
                }
            )
        )
    }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize data on first composition
    LaunchedEffect(termId) {
        // Fetch existing cut/divide data
        // This would be replaced with actual API call
        // fetchCutOrDivideData(termId)
    }

    fun generateRandomKey(): Int {
        return floor(100000 + Random.nextDouble() * 900000).toInt()
    }

    fun handleAddDivideInput() {
        if (!state.activeDivide) return
        
        val newInput = CutOrDivideInput(
            termId = termId,
            cutOrDivideKey = generateRandomKey(),
            type = "divide"
        )
        state = state.copy(
            divideInputs = state.divideInputs + newInput
        )
    }

    fun handleChangeDivide(index: Int, newValue: String) {
        val newVal = newValue.toIntOrNull() ?: 0
        
        if (breakAmount == 0) return
        
        val updatedInputs = state.divideInputs.toMutableList()
        
        val currentTotal = state.cutInputs
            .filter { it.slipId.isNullOrEmpty() }
            .sumOf { it.breakAmount } +
            state.divideInputs
                .filterIndexed { i, divide -> !divide.slipId.isNullOrEmpty() && i != index }
                .sumOf { it.breakAmount }
        
        val finalVal = if (currentTotal + newVal > breakAmount) {
            breakAmount - currentTotal
        } else newVal
        
        updatedInputs[index] = updatedInputs[index].copy(
            breakAmount = finalVal,
            update = true
        )
        
        state = state.copy(
            divideInputs = updatedInputs,
            remaining = breakAmount - (currentTotal + finalVal)
        )
    }

    fun handleChangeCut(index: Int, newValue: String, inputName: String) {
        val newVal = newValue.toIntOrNull() ?: 0
        
        if (breakAmount == 0) return
        
        val updatedInputs = state.cutInputs.toMutableList()
        
        when (inputName) {
            "breakAmount" -> {
                val currentTotal = state.cutInputs
                    .filter { it.slipId.isNullOrEmpty() }
                    .filterIndexed { i, _ -> i != index }
                    .sumOf { it.breakAmount } +
                    state.divideInputs
                        .filter { it.slipId.isNullOrEmpty() }
                        .sumOf { it.breakAmount }
                
                val finalVal = if (currentTotal + newVal > breakAmount) {
                    breakAmount - currentTotal
                } else newVal
                
                updatedInputs[index] = updatedInputs[index].copy(
                    breakAmount = finalVal,
                    update = true
                )
                
                state = state.copy(
                    cutInputs = updatedInputs,
                    remaining = breakAmount - (currentTotal + finalVal)
                )
            }
            "unit" -> {
                val finalVal = if (newVal > breakAmount) breakAmount else newVal
                updatedInputs[index] = updatedInputs[index].copy(
                    unit = finalVal,
                    update = true
                )
                state = state.copy(cutInputs = updatedInputs)
            }
        }
    }

    fun handleSave() {
        scope.launch {
            try {
                state = state.copy(loading = true)
                
                val divideSaveData = state.divideInputs.filter { 
                    (it.slipId.isNullOrEmpty()) && (it.breakAmount > 0 || it.update) 
                }
                val cutSaveData = state.cutInputs.filter { 
                    (it.slipId.isNullOrEmpty()) && (it.breakAmount > 0 || it.update) 
                }
                
                // Process divide data
                divideSaveData.forEach { divide ->
                    when {
                        divide.isNew -> {
                            // API call to create new divide
                            // axiosPrivate.post("/v1/ledger/setCutOrDivide", payload)
                        }
                        divide.update && divide.breakAmount > 0 -> {
                            // API call to update divide
                            // axiosPrivate.put("/v1/ledger/updateCutOrDivide", payload)
                        }
                        divide.update && divide.breakAmount == 0 -> {
                            // API call to delete divide
                            // axiosPrivate.delete("/v1/ledger/delCutOrDivide", payload)
                        }
                    }
                }
                
                // Process cut data
                cutSaveData.forEach { cut ->
                    when {
                        cut.isNew -> {
                            // API call to create new cut
                        }
                        cut.update && cut.breakAmount > 0 -> {
                            // API call to update cut
                        }
                        cut.update && cut.breakAmount == 0 -> {
                            // API call to delete cut
                        }
                    }
                }
                
                onSaveSuccess()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error saving data: ${e.message}")
            } finally {
                state = state.copy(loading = false)
                onCloseModal()
            }
        }
    }

    fun handleRefresh() {
        scope.launch {
            try {
                state = state.copy(loading = true)
                
                // Delete all existing data
                val allData = state.divideInputs + state.cutInputs
                allData.filter { it.breakAmount > 0 || it.update }.forEach { item ->
                    // API call to delete
                    // axiosPrivate.delete("/v1/ledger/delCutOrDivide", payload)
                }
                
                // Reset state
                state = state.copy(
                    activeDivide = false,
                    activeCut = false,
                    cutInputs = listOf(
                        CutOrDivideInput(
                            termId = termId,
                            cutOrDivideKey = generateRandomKey(),
                            type = "cut"
                        )
                    ),
                    divideInputs = (1..5).map {
                        CutOrDivideInput(
                            termId = termId,
                            cutOrDivideKey = generateRandomKey(),
                            type = "divide"
                        )
                    },
                    remaining = breakAmount,
                    loading = false
                )
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error refreshing data: ${e.message}")
                state = state.copy(loading = false)
            }
        }
    }

    fun handleActiveCut() {
        if (state.activeCut) {
            val updatedCutInputs = state.cutInputs.map { it.copy(breakAmount = 0) }
            val totalDivideAmount = state.divideInputs
                .filter { it.slipId.isNullOrEmpty() }
                .sumOf { it.breakAmount }
            
            state = state.copy(
                activeCut = false,
                cutInputs = updatedCutInputs,
                remaining = breakAmount - totalDivideAmount
            )
        } else {
            val totalAllocatedBreak = state.cutInputs
                .filter { it.slipId.isNullOrEmpty() }
                .sumOf { it.breakAmount } +
                state.divideInputs
                    .filter { it.slipId.isNullOrEmpty() }
                    .sumOf { it.breakAmount }
            
            state = state.copy(
                activeCut = true,
                remaining = breakAmount - totalAllocatedBreak
            )
        }
    }

    fun handleActiveDivide() {
        if (state.activeDivide) {
            val updatedDivideInputs = state.divideInputs.map { it.copy(breakAmount = 0) }
            val totalCutAmount = state.cutInputs.sumOf { it.breakAmount }
            
            state = state.copy(
                activeDivide = false,
                divideInputs = updatedDivideInputs,
                remaining = breakAmount - totalCutAmount
            )
        } else {
            val totalAllocatedBreak = state.cutInputs.sumOf { it.breakAmount } +
                state.divideInputs
                    .filter { it.slipId.isNullOrEmpty() }
                    .sumOf { it.breakAmount }
            
            state = state.copy(
                activeDivide = true,
                remaining = breakAmount - totalAllocatedBreak
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (state.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Remaining amount display
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "မူရင်းဘရိတ်",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = state.remaining.toString(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cut Section
                    item {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = state.activeCut,
                                        onCheckedChange = { handleActiveCut() }
                                    )
                                    Text(
                                        text = "Cut",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                state.cutInputs
                                    .sortedBy { it.slipId != null && it.slipId.isNotEmpty() }
                                    .forEachIndexed { index, input ->
                                        if (input.type == "cut") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = if (input.breakAmount > 0) input.breakAmount.toString() else "",
                                                    onValueChange = { value ->
                                                        handleChangeCut(index, value, "breakAmount")
                                                    },
                                                    label = { Text("Break") },
                                                    enabled = (input.slipId.isNullOrEmpty()) && state.activeCut,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(2f)
                                                )
                                                
                                                Text(
                                                    text = "if unit>=",
                                                    modifier = Modifier.weight(1f),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                
                                                OutlinedTextField(
                                                    value = if (input.unit > 0) input.unit.toString() else "",
                                                    onValueChange = { value ->
                                                        handleChangeCut(index, value, "unit")
                                                    },
                                                    label = { Text("Unit") },
                                                    enabled = (input.slipId.isNullOrEmpty()) && state.activeCut,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(2f)
                                                )
                                            }
                                        }
                                    }
                            }
                        }
                    }

                    // Divide Section
                    item {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = state.activeDivide,
                                            onCheckedChange = { handleActiveDivide() }
                                        )
                                        Text(
                                            text = "Divide",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    
                                    FloatingActionButton(
                                        onClick = { handleAddDivideInput() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Divide Input"
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                state.divideInputs
                                    .sortedBy { it.slipId != null }
                                    .forEachIndexed { index, input ->
                                        if (input.type == "divide") {
                                            OutlinedTextField(
                                                value = if (input.breakAmount > 0) input.breakAmount.toString() else "",
                                                onValueChange = { value ->
                                                    handleChangeDivide(index, value)
                                                },
                                                label = { Text("Break Amount ${index + 1}") },
                                                enabled = (input.slipId.isNullOrEmpty()) && state.activeDivide,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { handleSave() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                    
                    OutlinedButton(
                        onClick = { onCloseModal() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = { 
                            // Show confirmation dialog before refresh
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Divide ရော Cut ရော အကုန်ဖျက်မှာသေချာပါသလား?",
                                    actionLabel = "Yes",
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    handleRefresh()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refresh")
                    }
                }
            }
        }
    }
}