package ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalTextStyle
// Preview not available in desktop Compose
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.components.CustomTextField
// Removed Android lifecycle imports for desktop compatibility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import core.utils.AudioPlayer
import kotlinx.coroutines.SupervisorJob
import kotlin.math.roundToInt
import kotlin.random.Random
// User and ApiUserData are available from the same package
import core.stores.TempListStore
import core.stores.rememberTempListStore
import data.models.UserOption

// Data Models
data class ThreeDEntry(
    val number: String,
    val amount: Int,
    val summary: String,
    val showSummary: String,
    val groupId: Int,
    val delete: Boolean = false
)

data class ThreeDViewState(
    val autoIncrement3D: Boolean = false,
    val number3D: String = "",
    val tmpNumber3D: String = "",
    val unitPrice3D: TextFieldValue = TextFieldValue(""),
    val remainingUnit: String = "",
    val number3DError: String? = null,
    val unitPriceError: String? = null,
    val payMoney: String = "",
    val remainAmount: Double = 0.0,
    val totalUnit: Int = 0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val isModalOpen: Boolean = false,
    val hotList: List<String> = emptyList(),
    val listItems: List<ThreeDEntry> = emptyList()
)

// Pattern Processing Functions
object ThreeDPatternProcessor {
    
    fun processBreakRule(digit: String, groupId: Int, amount: Int): List<ThreeDEntry> {
        val entries = mutableListOf<ThreeDEntry>()
        val targetSum = digit.toInt()
        
        for (i in 0..999) {
            val numStr = i.toString().padStart(3, '0')
            val digitSum = numStr.map { it.digitToInt() }.sum()
            
            if (digitSum % 10 == targetSum) {
                entries.add(
                    ThreeDEntry(
                        number = numStr,
                        amount = amount,
                        summary = "${digit}B",
                        showSummary = if (entries.isEmpty()) "1" else "0",
                        groupId = groupId
                    )
                )
            }
        }
        
        return entries
    }
    
    fun processTRule(groupId: Int, amount: Int): List<ThreeDEntry> {
        val entries = mutableListOf<ThreeDEntry>()
        val tNumbers = listOf("000", "111", "222", "333", "444", "555", "666", "777", "888", "999")
        
        tNumbers.forEachIndexed { index, number ->
            entries.add(
                ThreeDEntry(
                    number = number,
                    amount = amount,
                    summary = "T",
                    showSummary = if (index == 0) "1" else "0",
                    groupId = groupId
                )
            )
        }
        
        return entries
    }
    
    fun processDoubleNStarRule(digits: String, groupId: Int, amount: Int): List<ThreeDEntry> {
        val entries = mutableListOf<ThreeDEntry>()
        val firstDigit = digits[0]
        val secondDigit = digits[1]
        
        for (i in 0..9) {
            val number = "${firstDigit}${secondDigit}${i}"
            entries.add(
                ThreeDEntry(
                    number = number,
                    amount = amount,
                    summary = "${digits}*",
                    showSummary = if (entries.isEmpty()) "1" else "0",
                    groupId = groupId
                )
            )
        }
        
        return entries
    }
    
    fun processNStarNRule(firstDigit: String, lastDigit: String, groupId: Int, amount: Int): List<ThreeDEntry> {
        val entries = mutableListOf<ThreeDEntry>()
        
        for (i in 0..9) {
            val number = "${firstDigit}${i}${lastDigit}"
            entries.add(
                ThreeDEntry(
                    number = number,
                    amount = amount,
                    summary = "${firstDigit}*${lastDigit}",
                    showSummary = if (entries.isEmpty()) "1" else "0",
                    groupId = groupId
                )
            )
        }
        
        return entries
    }
    
    fun processStartDoubleNRule(digits: String, groupId: Int, amount: Int): List<ThreeDEntry> {
        val entries = mutableListOf<ThreeDEntry>()
        val secondDigit = digits[0]
        val thirdDigit = digits[1]
        
        for (i in 0..9) {
            val number = "${i}${secondDigit}${thirdDigit}"
            entries.add(
                ThreeDEntry(
                    number = number,
                    amount = amount,
                    summary = "*${digits}",
                    showSummary = if (entries.isEmpty()) "1" else "0",
                    groupId = groupId
                )
            )
        }
        
        return entries
    }
    
    fun process3DRRule(number: String, groupId: Int, amount: Int): List<ThreeDEntry> {
        val entries = mutableListOf<ThreeDEntry>()
        val permutations = generatePermutations(number)
        
        permutations.forEachIndexed { index, perm ->
            entries.add(
                ThreeDEntry(
                    number = perm,
                    amount = amount,
                    summary = "${number}R",
                    showSummary = if (index == 0) "1" else "0",
                    groupId = groupId
                )
            )
        }
        
        return entries
    }
    
    fun process3DRRRule(number: String, groupId: Int, amount: Int): List<ThreeDEntry> {
        val entries = mutableListOf<ThreeDEntry>()
        val permutations = generatePermutations(number)
        
        // Filter out the original number, only include different permutations
        val filteredPermutations = permutations.filter { it != number }
        
        filteredPermutations.forEachIndexed { index, perm ->
            entries.add(
                ThreeDEntry(
                    number = perm,
                    amount = amount,
                    summary = if (index == 0) "${number}RR" else "",
                    showSummary = if (index == 0) "1" else "0",
                    groupId = groupId
                )
            )
        }
        
        return entries
    }
    
    fun process3DRRule2(tmpNumber: String, groupId: Int, amount: Int): List<ThreeDEntry> {
        return process3DRRule(tmpNumber, groupId, amount)
    }
    
    fun processNP(digit: String, groupId: Int, amount: Int): List<ThreeDEntry> {
        val entries = mutableListOf<ThreeDEntry>()
        
        for (i in 0..999) {
            val numStr = i.toString().padStart(3, '0')
            if (numStr.contains(digit)) {
                entries.add(
                    ThreeDEntry(
                        number = numStr,
                        amount = amount,
                        summary = "${digit}P",
                        showSummary = if (entries.isEmpty()) "1" else "0",
                        groupId = groupId
                    )
                )
            }
        }
        
        return entries
    }
    
    fun processAP(digits: String, groupId: Int, amount: Int): List<ThreeDEntry> {
        val entries = mutableListOf<ThreeDEntry>()
        val combinations = NumberUtil.generateCombinations(digits)
        
        // Remove duplicates
        val seen = mutableSetOf<String>()
        val uniqueCombinations = combinations.filter { combination ->
            if (seen.contains(combination)) {
                false
            } else {
                seen.add(combination)
                true
            }
        }
        
        uniqueCombinations.forEachIndexed { index, combination ->
            entries.add(
                ThreeDEntry(
                    number = combination,
                    amount = amount,
                    summary = "${digits}အပြီး",
                    showSummary = if (index == 0) "1" else "0",
                    groupId = groupId
                )
            )
        }
        
        return entries
    }
    
    fun processAPMinus(digits: String, groupId: Int, amount: Int): List<ThreeDEntry> {
        val entries = mutableListOf<ThreeDEntry>()
        val combinations = NumberUtil.generateCombinations(digits)
        
        combinations.forEach { combination ->
            val chars = combination.toCharArray()
            // Skip combinations where all digits are the same
            if (!(chars[0] == chars[1] && chars[1] == chars[2] && chars[0] == chars[2])) {
                entries.add(
                    ThreeDEntry(
                        number = combination,
                        amount = amount,
                        summary = "${digits}အပြီး-",
                        showSummary = if (entries.isEmpty()) "1" else "0",
                        groupId = groupId
                    )
                )
            }
        }
        
        // Remove duplicates
        val seen = mutableSetOf<String>()
        return entries.filter { entry ->
            if (seen.contains(entry.number)) {
                false
            } else {
                seen.add(entry.number)
                true
            }
        }
    }
    
    private fun generatePermutations(number: String): List<String> {
        val chars = number.toCharArray()
        val permutations = mutableSetOf<String>()
        generateAllPermutations(chars, 0, permutations)
        return permutations.toList()
    }
    
    private fun generateAllPermutations(chars: CharArray, index: Int, result: MutableSet<String>) {
        if (index == chars.size) {
            result.add(String(chars))
            return
        }
        
        for (i in index until chars.size) {
            // Swap
            val temp = chars[index]
            chars[index] = chars[i]
            chars[i] = temp
            
            // Recurse
            generateAllPermutations(chars, index + 1, result)
            
            // Backtrack
            chars[i] = chars[index]
            chars[index] = temp
        }
    }
}

// ViewModel
class ThreeDViewModel {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _state = MutableStateFlow(ThreeDViewState())
    val state: StateFlow<ThreeDViewState> = _state.asStateFlow()
    
    val numberFocusRequester = FocusRequester()
    val unitFocusRequester = FocusRequester()
    val payFocusRequester = FocusRequester()
    
    fun updateNumber3D(value: String) {
        _state.value = _state.value.copy(
            number3D = value,
            number3DError = null
        )
    }
    
    fun updateUnitPrice3D(value: TextFieldValue) {
        _state.value = _state.value.copy(
            unitPrice3D = value,
            unitPriceError = null
        )
    }
    
    fun selectAllUnitText() {
        val currentValue = _state.value.unitPrice3D
        _state.value = _state.value.copy(
            unitPrice3D = currentValue.copy(
                selection = TextRange(0, currentValue.text.length)
            )
        )
    }
    
    fun updatePayMoney(value: String, unitPrice: Double, tempListStore: TempListStore? = null) {
        val currentState = _state.value
        val totalAmount = tempListStore?.getTotalAmount()?.toDouble()?.times(unitPrice) ?: (currentState.totalUnit * unitPrice)
        val discountAmount = (totalAmount * currentState.discount)
        val finalAmount = totalAmount - discountAmount
        val remainAmount = finalAmount - (value.toDoubleOrNull() ?: 0.0)
        
        _state.value = currentState.copy(
            payMoney = value,
            remainAmount = remainAmount
        )
    }
    
    fun toggleAutoIncrement(enabled: Boolean) {
        _state.value = _state.value.copy(autoIncrement3D = enabled)
        if (enabled) {
            focusUnitInput()
        }
    }
    
    fun validateNumber3D(): Boolean {
        val number = _state.value.number3D.trim()
        if (number.isEmpty()) {
            _state.value = _state.value.copy(number3DError = "Number input cannot be empty!")
            return false
        }
        
        val rules = if (number.matches(Regex("^(\\d+)(\\.)$")) || number.matches(Regex("^(\\d+)(\\.\\.)$"))) {
            listOf(number)
        } else {
            number.split(".")
        }
        
        val validationPatterns = listOf(
            Regex("^\\*(\\d)(\\d)$"),       // Rule: *NN
            Regex("^(\\d)\\*(\\d)$"),       // Rule: N*N
            Regex("^(\\d)(\\d)\\*$"),       // Rule: NN*
            Regex("^T$", RegexOption.IGNORE_CASE),            // Rule: T
            Regex("^(\\d{3})[rR/]$", RegexOption.IGNORE_CASE),      // Rule: R
            Regex("^(\\d{3})rr$", RegexOption.IGNORE_CASE),     // Rule: RR
            Regex("^(\\d{3})$"),        // Matches exactly 3 digits
            Regex("^(\\d)[b-]$", RegexOption.IGNORE_CASE),      // Matches Break (1b or 1B)
            Regex("^(\\d)p$", RegexOption.IGNORE_CASE),         // Matches Break (np or NP)
            Regex("^(\\d+)(\\.)$", RegexOption.IGNORE_CASE),     // Matches Break (ap or AP)
            Regex("^(\\d+)(\\.)(\\.)$", RegexOption.IGNORE_CASE),      // Matches Break (ap or AP-)
            Regex("^[r/]$", RegexOption.IGNORE_CASE)
        )
        
        val isValid = rules.all { rule ->
            validationPatterns.any { pattern -> pattern.matches(rule.trim()) }
        }
        
        if (!isValid) {
            _state.value = _state.value.copy(number3DError = "Invalid input! Please enter valid 3D rules.")
        }
        
        return isValid
    }
    
    fun validateUnitInput(): Boolean {
        val unit = _state.value.unitPrice3D.text.trim()
        if (unit.isEmpty()) {
            _state.value = _state.value.copy(unitPriceError = "Unit price cannot be empty!")
            return false
        }
        
        val isValid = when {
            unit.matches(Regex("^(\\d+)(r|/)(\\d+)$", RegexOption.IGNORE_CASE)) -> true
            unit.matches(Regex("^\\d+(\\+\\d+)*$")) -> true
            _state.value.number3D.matches(Regex("^\\d{3}$")) && 
                (unit.matches(Regex("^[/rR]\\d+$")) || unit.matches(Regex("^\\d+[rR/]$"))) -> true
            unit.toDoubleOrNull() != null -> true
            else -> false
        }
        
        if (!isValid) {
            _state.value = _state.value.copy(unitPriceError = "Unit price must be a valid number!")
        }
        
        return isValid
    }
    
    fun processNumberEntry(tempListStore: TempListStore? = null) {
        val currentState = _state.value
        
        // Early validation with user feedback
        if (currentState.number3D.trim().isEmpty() || currentState.unitPrice3D.text.trim().isEmpty()) {
            _state.value = currentState.copy(
                number3DError = if (currentState.number3D.trim().isEmpty()) "Please enter both number and unit price" else null,
                unitPriceError = if (currentState.unitPrice3D.text.trim().isEmpty()) "Please enter both number and unit price" else null
            )
            AudioPlayer.playFailSong()
            focusNumberInput()
            return
        }
        
        if (!validateNumber3D() || !validateUnitInput()) {
            focusNumberInput()
            AudioPlayer.playFailSong()
            return
        }
        
        try {
            val formattedEntries = processPatterns(currentState.number3D, currentState.unitPrice3D.text)
            
            // Add entries to global TempList store
            tempListStore?.let { store ->
                val tempListItems = formattedEntries.map { entry ->
                    TempListItem(
                        number = entry.number,
                        amount = entry.amount.toString(),
                        summary = entry.summary,
                        showSummary = entry.showSummary,
                        groupId = entry.groupId.toString()
                    )
                }
                store.addItems(tempListItems)
            }
            
            val newListItems = currentState.listItems + formattedEntries
            val newTotalUnit = newListItems.sumOf { it.amount }
            
            _state.value = currentState.copy(
                listItems = newListItems,
                totalUnit = newTotalUnit,
                number3D = if (currentState.autoIncrement3D) incrementNumber() else "",
                tmpNumber3D = if (!currentState.autoIncrement3D) currentState.number3D else currentState.tmpNumber3D,
                remainingUnit = "", // Clear ကျံယူနစ်[F2] TextField after successful processing
                number3DError = null,
                unitPriceError = null
            )
            
            checkHotNumbers(formattedEntries)
            
            if (currentState.autoIncrement3D) {
                focusUnitInput()
            } else {
                focusNumberInput()
            }
        } catch (e: Exception) {
            _state.value = currentState.copy(
                number3DError = "Error processing entry: ${e.message}"
            )
            focusNumberInput()
            AudioPlayer.playFailSong()
        }
    }
    
    private fun processPatterns(number: String, unit3D: String): List<ThreeDEntry> {
        val entries = mutableListOf<ThreeDEntry>()
        val uniqueID = Random.nextInt(100000, 999999)
        val amount = unit3D.split("+").mapNotNull { it.toIntOrNull() }.sum()
        
        val rules = if (number.matches(Regex("^(\\d+)(\\.)$")) || number.matches(Regex("^(\\d+)(\\.\\.)$"))) {
            listOf(number)
        } else {
            number.split(".")
        }
        
        rules.forEach { rule ->
            when {
                // Break rule (NB) - First in JS
                rule.matches(Regex("^(\\d)[b-]$", RegexOption.IGNORE_CASE)) -> {
                    val digit = rule.substring(0, 1)
                    entries.addAll(ThreeDPatternProcessor.processBreakRule(digit, uniqueID, amount))
                }
                
                // T rule - Second in JS
                rule.equals("T", ignoreCase = true) -> {
                    entries.addAll(ThreeDPatternProcessor.processTRule(uniqueID, amount))
                }
                
                // NN* rule - Third in JS
                rule.matches(Regex("^(\\d)(\\d)\\*$")) -> {
                    val digits = rule.substring(0, 2)
                    entries.addAll(ThreeDPatternProcessor.processDoubleNStarRule(digits, uniqueID, amount))
                }
                
                // N*N rule - Fourth in JS
                rule.matches(Regex("^(\\d)\\*(\\d)$")) -> {
                    val firstDigit = rule.substring(0, 1)
                    val lastDigit = rule.substring(2, 3)
                    entries.addAll(ThreeDPatternProcessor.processNStarNRule(firstDigit, lastDigit, uniqueID, amount))
                }
                
                // *NN rule - Fifth in JS
                rule.matches(Regex("^\\*(\\d)(\\d)$")) -> {
                    val digits = rule.substring(1, 3)
                    entries.addAll(ThreeDPatternProcessor.processStartDoubleNRule(digits, uniqueID, amount))
                }
                
                // R rule (NNNR) - Sixth in JS
                rule.matches(Regex("^(\\d{3})[rR/]$")) -> {
                    val baseNumber = rule.substring(0, 3)
                    entries.addAll(ThreeDPatternProcessor.process3DRRule(baseNumber, uniqueID, amount))
                }
                
                // RR rule (NNNRR) - Seventh in JS
                rule.matches(Regex("^(\\d{3})rr$", RegexOption.IGNORE_CASE)) -> {
                    val baseNumber = rule.substring(0, 3)
                    entries.addAll(ThreeDPatternProcessor.process3DRRRule(baseNumber, uniqueID, amount))
                }
                
                // NP rule - Eighth in JS
                rule.matches(Regex("^(\\d)p$", RegexOption.IGNORE_CASE)) -> {
                    val digit = rule.substring(0, 1)
                    entries.addAll(ThreeDPatternProcessor.processNP(digit, uniqueID, amount))
                }
                
                // AP rule (NNN.) - Ninth in JS
                rule.matches(Regex("^(\\d+)(\\.)$", RegexOption.IGNORE_CASE)) -> {
                    val digits = rule.substring(0, rule.length - 1)
                    entries.addAll(ThreeDPatternProcessor.processAP(digits, uniqueID, amount))
                }
                
                // AP- rule (NNN..) - Tenth in JS
                rule.matches(Regex("^(\\d+)(\\.)(\\.)$", RegexOption.IGNORE_CASE)) -> {
                    val digits = rule.substring(0, rule.length - 2)
                    entries.addAll(ThreeDPatternProcessor.processAPMinus(digits, uniqueID, amount))
                }
                
                // Handle unit price with R pattern (e.g., "100r50" or "100/50") - check this first
                unit3D.matches(Regex("^(\\d+)(r|/|\\.)(\\d+)$", RegexOption.IGNORE_CASE)) -> {
                    val uPrice = unit3D.replace(".", "r")
                    if (rule.matches(Regex("^\\d{3}$"))) {
                        entries.addAll(NumberUtil.process3DRRule(rule, uniqueID.toString(), uPrice).map {
                            ThreeDEntry(
                                number = it.number,
                                amount = it.amount.toInt(),
                                summary = it.summary,
                                showSummary = it.showSummary,
                                groupId = it.groupId.toInt()
                            )
                        })
                    }
                    val match2 = Regex("^(\\d{3})[rR/]$", RegexOption.IGNORE_CASE).find(rule)
                    if (match2 != null) {
                        entries.addAll(NumberUtil.process3DRRule(match2.groupValues[1], uniqueID.toString(), uPrice).map {
                            ThreeDEntry(
                                number = it.number,
                                amount = it.amount.toInt(),
                                summary = it.summary,
                                showSummary = it.showSummary,
                                groupId = it.groupId.toInt()
                            )
                        })
                    }
                }
                
                // Handle R rule with just "/" or "r"
                rule.matches(Regex("^[r/]$", RegexOption.IGNORE_CASE)) -> {
                    val tmpNumber = _state.value.tmpNumber3D
                    if (tmpNumber.isNotEmpty()) {
                        entries.addAll(ThreeDPatternProcessor.process3DRRule2(tmpNumber, uniqueID, amount))
                    } else {
                        throw IllegalStateException("No temporary 3D number available for R rule")
                    }
                }
                
                // Handle multiple numbers separated by dots (e.g., "123.456.789")
                rule.matches(Regex("^(\\d{3}\\.)+\\d{3}$")) -> {
                    val numbers = rule.split(".")
                    numbers.forEach { num ->
                        entries.add(
                            ThreeDEntry(
                                number = num,
                                amount = amount,
                                summary = num,
                                showSummary = "1",
                                groupId = uniqueID
                            )
                        )
                    }
                }
                
                // Handle unit price with addition pattern and 3-digit rule
                unit3D.matches(Regex("^\\d+(\\+\\d+)*$")) && rule.matches(Regex("^\\d{3}$")) -> {
                    _state.value = _state.value.copy(tmpNumber3D = rule)
                    entries.add(
                        ThreeDEntry(
                            number = rule,
                            amount = amount,
                            summary = rule,
                            showSummary = "1",
                            groupId = uniqueID
                        )
                    )
                }
                
                // Handle R rule in unit price (e.g., "/50" or "r50" or "50r")
                rule.matches(Regex("^\\d{3}$")) && 
                (unit3D.matches(Regex("^[/rR]\\d+$")) || unit3D.matches(Regex("^\\d+[rR/]$"))) -> {
                    val cleanUnit = unit3D.replace(Regex("[/Rr]"), "")
                    val amountValue = cleanUnit.toIntOrNull() ?: amount
                    entries.addAll(ThreeDPatternProcessor.process3DRRule(rule, uniqueID, amountValue))
                }
                
                // Direct 3-digit number (default case)
                rule.matches(Regex("^\\d{3}$")) -> {
                    _state.value = _state.value.copy(tmpNumber3D = rule)
                    entries.add(
                        ThreeDEntry(
                            number = rule,
                            amount = amount,
                            summary = rule,
                            showSummary = "1",
                            groupId = uniqueID
                        )
                    )
                }
                
                // Unsupported rule
                else -> {
                    throw IllegalArgumentException("Rule \"$rule\" is not yet supported.")
                }
            }
        }
        
        return entries
    }
    
    private fun incrementNumber(): String {
        val current = _state.value.number3D.toIntOrNull() ?: 0
        val existingNumbers = _state.value.listItems.map { it.number.toIntOrNull() ?: -1 }.toSet()
        
        var next = (current + 1) % 1000
        while (existingNumbers.contains(next)) {
            next = (next + 1) % 1000
        }
        
        return next.toString().padStart(3, '0')
    }
    
    private fun checkHotNumbers(entries: List<ThreeDEntry>) {
        val hotNumbersSet = _state.value.hotList.toSet()
        val hasHot = entries.any { hotNumbersSet.contains(it.number) }
        
        // Sound playing would be handled by the UI layer
        if (hasHot) {
            AudioPlayer.playDuplicateSong()
        } else {
            AudioPlayer.playSuccessSong()
        }
    }
    
    fun focusNumberInput() {
        scope.launch {
            numberFocusRequester.requestFocus()
        }
    }
    
    fun focusUnitInput() {
        scope.launch {
            unitFocusRequester.requestFocus()
        }
    }
    
    fun focusPayInput() {
        scope.launch {
            payFocusRequester.requestFocus()
        }
    }
    
    fun openModal() {
        _state.value = _state.value.copy(isModalOpen = true)
    }
    
    fun closeModal() {
        _state.value = _state.value.copy(isModalOpen = false)
    }
    
    fun updateDiscount(user: User?, apiUserData: List<ApiUserData>) {
        val discount = user?.let { u ->
            apiUserData.find { it.userId == u.userId }?.discount3D ?: 0.0
        } ?: 0.0
        
        _state.value = _state.value.copy(discount = discount)
    }
}

@Composable
fun ThreeDView(
    unitPrice: Double,
    breakAmount: Double,
    playFailSong: () -> Unit,
    playSuccessSong: () -> Unit,
    playDuplicateSong: () -> Unit,
    termId: String?,
    user: User?,
    numberInput: String?,
    apiUserData: List<ApiUserData>,
    onUserSelectionChanged: (() -> Unit)? = null,
    onOpenBetAndReminingModal: (String) -> Unit = {},
    viewModel: ThreeDViewModel = remember { ThreeDViewModel() }
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val tempListStore = rememberTempListStore()
    val tempListState by tempListStore.state.collectAsState()
    
    // Update discount when user changes
    LaunchedEffect(user, apiUserData) {
        viewModel.updateDiscount(user, apiUserData)
    }
    
    // Focus number input when numberInput is "3D"
    LaunchedEffect(numberInput) {
        if (numberInput == "3D") {
            viewModel.focusNumberInput()
        }
    }
    
    // Focus number input when user is selected
    LaunchedEffect(user) {
        if (user != null) {
            onUserSelectionChanged?.invoke()
            viewModel.focusNumberInput()
            println("[DEBUG] ThreeDView: User selected, focusing number input")
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Input Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Number Input with Checkbox
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = state.autoIncrement3D,
                    onCheckedChange = viewModel::toggleAutoIncrement
                )
                
                OutlinedTextField(
                    value = state.number3D,
                    onValueChange = viewModel::updateNumber3D,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(viewModel.numberFocusRequester)
                        .onKeyEvent { keyEvent ->
                            when {
                                keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown -> {
                                    viewModel.processNumberEntry(tempListStore)
                                    true
                                }
                                keyEvent.key == Key.F2 && keyEvent.type == KeyEventType.KeyDown -> {
                                    println("[DEBUG] F2 key pressed in ThreeDView")
                                    if (user == null) {
                                        println("[DEBUG] Cannot open modal - no user selected")
                                        // TODO: Show toast message "ဒိုင်ရွေးပါ" (Please select user)
                                    } else if (termId == null) {
                                        println("[DEBUG] Cannot open modal - no term selected")
                                        // TODO: Show toast message "အပတ်စဉ်ရွေးပါ" (Please select term)
                                    } else {
                                         println("[DEBUG] Opening BetAndRemining modal - user: ${user.name}, termId: $termId, number: ${state.number3D}")
                                         onOpenBetAndReminingModal(state.number3D)
                                     }
                                    true
                                }
                                else -> false
                            }
                        },
                    enabled = !state.autoIncrement3D,
                    singleLine = true,
                    isError = state.number3DError != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { viewModel.focusUnitInput() }
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    shape = RoundedCornerShape(8.dp)
                 )
            }
            
            // Unit Input
            OutlinedTextField(
                value = state.unitPrice3D,
                onValueChange = viewModel::updateUnitPrice3D,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(viewModel.unitFocusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            viewModel.selectAllUnitText()
                        }
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) {
                            viewModel.processNumberEntry(tempListStore)
                            true
                        } else false
                    },
                isError = state.unitPriceError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.processNumberEntry(tempListStore) }
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                shape = RoundedCornerShape(8.dp)
              )
        }
        
        // Calculation Display Section
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Remaining Unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ကျံယူနစ်[F2]",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.width(100.dp)
                )
                CustomTextField(
                    value = state.remainingUnit,
                    onValueChange = { },
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    borderColor = androidx.compose.ui.graphics.Color.Black,
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Black,
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            // Total Unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ယူနစ်ပေါင်း",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.width(100.dp)
                )
                val totalUnits = tempListState.items.sumOf { it.amount.toIntOrNull() ?: 0 }
                CustomTextField(
                    value = totalUnits.toString(),
                    onValueChange = { },
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    borderColor = androidx.compose.ui.graphics.Color.Black,
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Black,
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            // Discount Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ကော်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.width(100.dp)
                )
                val totalUnits = tempListState.items.sumOf { it.amount.toIntOrNull() ?: 0 }
                val discountAmount = (((totalUnits.toDouble() * unitPrice) * state.discount)).roundToInt()
                CustomTextField(
                    value = discountAmount.toString(),
                    onValueChange = { },
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    borderColor = androidx.compose.ui.graphics.Color.Black,
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Black,
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            // Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ငွေပေါင်း",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.width(100.dp)
                )
                val totalUnits = tempListState.items.sumOf { it.amount.toIntOrNull() ?: 0 }
                val totalAmount = totalUnits.toDouble() * unitPrice
                val discountAmount = (((totalUnits.toDouble() * unitPrice) * state.discount)).roundToInt()
                val finalAmount = totalAmount - discountAmount
                CustomTextField(
                    value = finalAmount.roundToInt().toString(),
                    onValueChange = { },
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    borderColor = androidx.compose.ui.graphics.Color.Black,
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Black,
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            // Pay Money
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ပေးငွေ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.width(100.dp)
                )
                CustomTextField(
                    value = state.payMoney,
                    onValueChange = { value -> viewModel.updatePayMoney(value, unitPrice, tempListStore) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(viewModel.payFocusRequester),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    borderColor = androidx.compose.ui.graphics.Color.Black,
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Black,
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            // Remain Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ပမာဏ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.width(100.dp)
                )
                CustomTextField(
                    value = state.remainAmount.toString(),
                    onValueChange = { },
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    borderColor = androidx.compose.ui.graphics.Color.Black,
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Black,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
        
        // Modal for BetAndRemining
        if (state.isModalOpen) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { viewModel.closeModal() }
                        ) {
                            Text("Close")
                        }
                    }
                    
                    // BetAndRemining table component for 3D
                    if (user != null && termId != null && state.number3D.isNotEmpty()) {
                        val userOption = UserOption(user.userId, user.name)
                        
                        BetAndRemining(
                            number = state.number3D,
                            user = userOption,
                            termId = termId
                        )
                    } else {
                        Text(
                            text = "Please select a user, term, and enter a number to view bet and remaining data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Keyboard shortcuts
    LaunchedEffect(Unit) {
        // Global keyboard shortcuts would be handled here
        // This would need proper key event handling implementation
    }
}
