package ui.screens

// Data models and stores
import core.config.CompactOutlinedTextField
import core.stores.LedgerStore
import core.stores.TempListStore
import core.stores.rememberTempListStore
import data.models.UserOption

// Utils
import core.utils.AudioPlayer

// UI components
import ui.components.CustomTextField

// Compose imports
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalTextStyle

// Coroutines
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

// Kotlin standard library
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

// Data Models
@Stable
data class BetEntry(
    val number: String,
    val amount: Int,
    val summary: String,
    val showSummary: String,
    val groupId: Int,
    val delete: Boolean = false
)

// User and ApiUserData are defined in SalePage.kt

@Stable
data class TwoDViewState(
    val selected2DType: String = "",
    val autoIncrement2D: Boolean = false,
    val number2D: String = "",
    val tmpNumber2D: String = "",
    val unit2D: TextFieldValue = TextFieldValue(""),
    val remainingUnitInput: String = "",
    val number2DError: String? = null,
    val unitPriceError: String? = null,
    val payMoney: String = "",
    val remainAmount: Int = 0,
    val discount: Int = 0,
    val totalAmount: Int = 0,
    val isModalOpen: Boolean = false,
    val hotList: List<String> = emptyList()
)

// Pattern Definitions
@Immutable
object TwoDPatterns {
    val patterns = mapOf(
        "sp" to listOf("00", "22", "44", "66", "88"),
        "+*" to listOf("00", "22", "44", "66", "88"),
        "mp" to listOf("11", "33", "55", "77", "99"),
        "-*" to listOf("11", "33", "55", "77", "99"),
        "ss" to listOf("00", "02", "04", "06", "08", "20", "22", "24", "26", "28", "40", "42", "44", "46", "48", "60", "62", "64", "66", "68", "80", "82", "84", "86", "88"),
        "++" to listOf("00", "02", "04", "06", "08", "20", "22", "24", "26", "28", "40", "42", "44", "46", "48", "60", "62", "64", "66", "68", "80", "82", "84", "86", "88"),
        "mm" to listOf("11", "13", "15", "17", "19", "31", "33", "35", "37", "39", "51", "53", "55", "57", "59", "71", "73", "75", "77", "79", "91", "93", "95", "97", "99"),
        "--" to listOf("11", "13", "15", "17", "19", "31", "33", "35", "37", "39", "51", "53", "55", "57", "59", "71", "73", "75", "77", "79", "91", "93", "95", "97", "99"),
        "sm" to listOf("01", "03", "05", "07", "09", "21", "23", "25", "27", "29", "41", "43", "45", "47", "49", "61", "63", "65", "67", "69", "81", "83", "85", "87", "89"),
        "+-" to listOf("01", "03", "05", "07", "09", "21", "23", "25", "27", "29", "41", "43", "45", "47", "49", "61", "63", "65", "67", "69", "81", "83", "85", "87", "89"),
        "ms" to listOf("10", "12", "14", "16", "18", "30", "32", "34", "36", "38", "50", "52", "54", "56", "58", "70", "72", "74", "76", "78", "90", "92", "94", "96", "98"),
        "-+" to listOf("10", "12", "14", "16", "18", "30", "32", "34", "36", "38", "50", "52", "54", "56", "58", "70", "72", "74", "76", "78", "90", "92", "94", "96", "98"),
        "nk" to listOf("01", "09", "10", "12", "21", "23", "32", "34", "43", "45", "54", "56", "65", "67", "76", "78", "87", "89", "98", "90"),
        "nn" to listOf("01", "09", "10", "12", "21", "23", "32", "34", "43", "45", "54", "56", "65", "67", "76", "78", "87", "89", "98", "90"),
        "k" to listOf("07", "18", "24", "35", "69", "70", "81", "42", "53", "96"),
        "n" to listOf("07", "18", "24", "35", "69", "70", "81", "42", "53", "96"),
        "w" to listOf("05", "16", "27", "38", "49", "50", "61", "72", "83", "94"),
        "p" to listOf("00", "11", "22", "33", "44", "55", "66", "77", "88", "99"),
        "**" to listOf("00", "11", "22", "33", "44", "55", "66", "77", "88", "99"),
        "include" to listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
    )
    
    val aliasMap = mapOf(
        "+*" to "sp",
        "-*" to "mp",
        "++" to "ss",
        "--" to "mm",
        "+-" to "sm",
        "-+" to "ms",
        "nn" to "nk",
        "n" to "k",
        "**" to "p"
    )
}

// ViewModel
class TwoDViewModel {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _state = MutableStateFlow(TwoDViewState())
    val state: StateFlow<TwoDViewState> = _state.asStateFlow()
    
    val numberFocusRequester = FocusRequester()
    val unitFocusRequester = FocusRequester()
    val payFocusRequester = FocusRequester()
    
    // Debouncing for input validation
    private var validationJob: Job? = null
    private val validationCache = mutableMapOf<String, Boolean>()
    
    // Cached compiled regex patterns for better performance
    private val cachedPatterns = mapOf(
        "groupRPattern" to Regex("^(\\d{2}\\.)+\\d{2}(\\.)?[rR/]$"),
        "dotPattern" to Regex("^(\\d+)(\\.)$"),
        "doubleDotPattern" to Regex("^(\\d+)(\\.\\.)$"),
        "groupDotPattern" to Regex("^(\\d{2}\\.)+\\d{2}\\.$")
    )
    
    fun updateNumber2D(value: String) {
        // Ensure no input filtering - accept all characters including dots and slashes
        _state.value = _state.value.copy(
            number2D = value,
            number2DError = null
        )
    }
    
    fun updateUnitPrice2D(value: TextFieldValue) {
        _state.value = _state.value.copy(
            unit2D = value,
            unitPriceError = null
        )
        
        // Debounced validation to prevent excessive processing
        validationJob?.cancel()
        validationJob = scope.launch {
            delay(300) // Wait 300ms before validating
            validateUnitInputDebounced(value.text)
        }
    }
    
    private suspend fun validateUnitInputDebounced(unitText: String) {
        // Check cache first
        val cacheKey = "unit_$unitText"
        if (validationCache.containsKey(cacheKey)) {
            return
        }
        
        // Perform validation and cache result
        val isValid = unitText.isNotEmpty() && unitText.split("+").all { 
            it.toIntOrNull() != null && it.toInt() > 0 
        }
        validationCache[cacheKey] = isValid
        
        // Limit cache size to prevent memory issues
        if (validationCache.size > 100) {
            validationCache.clear()
        }
    }
    
    fun selectAllUnitText() {
        val currentState = _state.value
        val text = currentState.unit2D.text
        _state.value = currentState.copy(
            unit2D = currentState.unit2D.copy(
                selection = TextRange(0, text.length)
            )
        )
    }
    
    fun updatePayMoney(value: String, unitPrice: Int, tempListStore: TempListStore? = null) {
        val currentState = _state.value
        val totalUnit = tempListStore?.getTotalAmount()?.toInt() ?: 0
        val totalAmount = totalUnit * unitPrice
        val discountAmount = totalAmount * currentState.discount / 100
        val finalAmount = totalAmount - discountAmount
        val remainAmount = finalAmount - (value.toIntOrNull() ?: 0)
        
        _state.value = currentState.copy(
            payMoney = value,
            remainAmount = remainAmount
        )
    }
    
    fun toggleAutoIncrement(enabled: Boolean) {
        _state.value = _state.value.copy(autoIncrement2D = enabled)
        if (enabled) {
            focusUnitInput()
        }
    }
    
    fun handleAutoIncrementChange(checked: Boolean) {
        _state.value = _state.value.copy(autoIncrement2D = checked)
        
        if (checked) {
            _state.value = _state.value.copy(number2D = "00")
            focusUnitInput()
        }
    }
    
    fun validateNumber2D(): Boolean {
        val number = _state.value.number2D.trim()
        if (number.isEmpty()) {
            _state.value = _state.value.copy(number2DError = "Number is required")
            return false
        }
        
        // Combine all valid patterns into a validation array
        val validationPatterns = listOf(
            Regex("^(\\d{2})(r|/)$", RegexOption.IGNORE_CASE), // R pattern
            Regex("^\\*(\\d)$"),         // *N pattern
            Regex("^(\\d)\\*$"),         // N* pattern
            Regex("^(\\d)[b-]$", RegexOption.IGNORE_CASE),          // B pattern
            Regex("^(\\d)PX\\+$", RegexOption.IGNORE_CASE),       // PX+ pattern
            Regex("^(\\d)\\+$", RegexOption.IGNORE_CASE),         // N+ pattern same as px+
            Regex("^(\\d+)(\\.)$"), // . pattern
            Regex("^(\\d+)(\\.\\.)$"), // .. pattern
            Regex("^\\d{2}$"),          // two digit pattern
            Regex("^s([0-9])$", RegexOption.IGNORE_CASE),       // sN pattern
            Regex("^[0-9]s$", RegexOption.IGNORE_CASE),         // Ns pattern
            Regex("^m([0-9])$", RegexOption.IGNORE_CASE),       // MN pattern
            Regex("^[0-9]m$", RegexOption.IGNORE_CASE),         // NM pattern
            Regex("^(\\d+)\\+(\\d+)([R/]?)$", RegexOption.IGNORE_CASE), // 123+456R (3-4 digits + 'R')
            Regex("^(\\d{2}\\.)+\\d{2}$"), // Group pattern without trailing dot
            Regex("^(\\d{2}\\.)+\\d{2}\\.$"), // Group pattern with trailing dot
            Regex("^(\\d{2}\\.)+\\d{2}\\/$"), // Group pattern with trailing slash
            Regex("^(\\d{2}\\.)+\\d{2}\\./$") // Group pattern with trailing dot and slash
        )
        
        val isValid = validationPatterns.any { pattern -> number.matches(pattern) } ||
                     TwoDPatterns.patterns.containsKey(number.lowercase()) // Pattern keys
        
        if (!isValid) {
            _state.value = _state.value.copy(number2DError = "Invalid number format")
        }
        
        return isValid
    }
    
    fun validateUnitInput(): Boolean {
        val unit = _state.value.unit2D.text.trim()
        if (unit.isEmpty()) {
            _state.value = _state.value.copy(unitPriceError = "Unit price is required")
            return false
        }
        
        // Validate unit price format (can include + for multiple values)
        val isValid = unit.matches(Regex("^\\d+(\\+\\d+)*$"))
        
        if (!isValid) {
            _state.value = _state.value.copy(unitPriceError = "Invalid unit price format")
        }
        
        return isValid
    }
    
    fun processNumberEntry(tempListStore: TempListStore? = null) {
        val currentState = _state.value
        
        // Handle special case: if only "/" is entered, use previous number's reverse
        if (currentState.number2D == "/" && currentState.tmpNumber2D.isNotEmpty() && currentState.tmpNumber2D.length == 2) {
            val reversedNumber = currentState.tmpNumber2D.reversed()
            _state.value = currentState.copy(
                number2D = reversedNumber,
                tmpNumber2D = reversedNumber
            )
            // Process the reversed number
            if (validateNumber2D() && validateUnitInput()) {
                val formattedEntries = processPatterns(reversedNumber, currentState.unit2D.text)
                
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
                
                // Update state
                _state.value = currentState.copy(
                    number2D = if (currentState.autoIncrement2D) incrementNumber(tempListStore) else "",
                    tmpNumber2D = reversedNumber,
                    remainingUnitInput = "" // Clear ကျံယူနစ်[F2] TextField after successful processing
                )
                
                // Check for hot numbers and play appropriate sound
                checkHotNumbers(formattedEntries)
                
                if (currentState.autoIncrement2D) {
                    focusUnitInput()
                } else {
                    focusNumberInput()
                    AudioPlayer.playFailSong()
                }
            } else {
                focusNumberInput()
                AudioPlayer.playFailSong()
            }
            return
        }
        
        if (validateNumber2D() && validateUnitInput()) {
            val formattedEntries = processPatterns(currentState.number2D, currentState.unit2D.text)
            
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
            
            // Update state and store current number as tmpNumber2D for future reference
            _state.value = currentState.copy(
                number2D = if (currentState.autoIncrement2D) incrementNumber(tempListStore) else "",
                tmpNumber2D = currentState.number2D,
                remainingUnitInput = "" // Clear ကျံယူနစ်[F2] TextField after successful processing
            )
            
            // Check for hot numbers and play appropriate sound
            checkHotNumbers(formattedEntries)
            
            if (currentState.autoIncrement2D) {
                focusUnitInput()
            } else {
                focusNumberInput()
            }
        } else {
            focusNumberInput()
            AudioPlayer.playFailSong()
        }
    }
    
    private fun processPatterns(number: String, unit2D: String): List<BetEntry> {
        val entries = mutableListOf<BetEntry>()
        val amount = unit2D.split("+").sumOf { it.toIntOrNull() ?: 0 }
        
        // Handle group pattern ending with /, r, or R (optionally with a dot before)
        val groupRPattern = cachedPatterns["groupRPattern"]!!
        val rules = when {
            groupRPattern.matches(number) -> {
                // Remove the trailing r/R/ and optional dot, then split
                val cleanRule = if (number.substring(number.length - 2, number.length - 1) == ".") {
                    number.substring(0, number.length - 2) // Remove . and suffix
                } else {
                    number.substring(0, number.length - 1) // Remove just the suffix
                }
                // For group patterns ending with '/', generate both original and reversed for each number
                val numbers = cleanRule.split(".")
                val expandedRules = mutableListOf<String>()
                numbers.forEach { num ->
                    expandedRules.add(num) // Add original number
                    val reversed = num.reversed()
                    if (num != reversed) {
                        expandedRules.add(reversed) // Add reversed number if different
                    }
                }
                expandedRules
            }
            number.matches(Regex("^(\\d+)(\\.)$")) || number.matches(Regex("^(\\d+)(\\.\\.)$")) -> {
                listOf(number)
            }
            number.matches(Regex("^(\\d{2}\\.)+\\d{2}\\.$")) -> {
                // Handle group pattern with trailing dot - remove the trailing dot before splitting
                number.replace("\\.$".toRegex(), "").split(".")
            }
            else -> {
                number.split(".")
            }
        }
        
        rules.forEach { rule ->
            val uniqueID = Random.nextInt(100000, 999999)
            
            // Pattern Matching
            val matchR = Regex("^(\\d{2})(r|/)$", RegexOption.IGNORE_CASE).find(rule)
            val matchStarN = Regex("^\\*(\\d)$").find(rule)
            val matchNStar = Regex("^(\\d)\\*$").find(rule)
            val matchNB = Regex("^(\\d)[b-]$", RegexOption.IGNORE_CASE).find(rule)
            val matchPXPlus = Regex("^(\\d)PX\\+$", RegexOption.IGNORE_CASE).find(rule)
            val matchPlus = Regex("^(\\d)\\+$", RegexOption.IGNORE_CASE).find(rule)
            val matchDot = Regex("^(\\d+)(\\.)$").find(rule)
            val matchDotDash = Regex("^(\\d+)(\\.\\.)$").find(rule)
            val matchSN = Regex("^s([0-9])$", RegexOption.IGNORE_CASE).find(rule)
            val matchNs = Regex("^([0-9])s$", RegexOption.IGNORE_CASE).find(rule)
            val matchMN = Regex("^m([0-9])$", RegexOption.IGNORE_CASE).find(rule)
            val matchNM = Regex("^([0-9])m$", RegexOption.IGNORE_CASE).find(rule)
            val matchPlusPattern = Regex("^(\\d+)\\+(\\d+)([R/]?)$", RegexOption.IGNORE_CASE).find(rule)
            
            when {
                matchR != null -> {
                    // R pattern (အပြန်)
                    val baseNumber = matchR.groupValues[1]
                    val reversedNumber = baseNumber.reversed()
                    entries.add(BetEntry(baseNumber, amount, "${baseNumber}r", if (entries.isEmpty()) "1" else "0", uniqueID))
                    if (baseNumber != reversedNumber) {
                        entries.add(BetEntry(reversedNumber, amount, "${baseNumber}r", "0", uniqueID))
                    }
                }
                
                matchStarN != null -> {
                    // *N pattern (နောက်ထိပ်စီး)
                    val digit = matchStarN.groupValues[1]
                    for (i in 0..9) {
                        entries.add(BetEntry("${i}${digit}", amount, "*${digit}", if (entries.isEmpty()) "1" else "0", uniqueID))
                    }
                }
                
                matchNStar != null -> {
                    // N* pattern (ရှေ့ထိပ်စီး)
                    val digit = matchNStar.groupValues[1]
                    for (i in 0..9) {
                        entries.add(BetEntry("${digit}${i}", amount, "${digit}*", if (entries.isEmpty()) "1" else "0", uniqueID))
                    }
                }
                
                matchNB != null -> {
                    // NB pattern (ဘရိတ်)
                    val endSum = matchNB.groupValues[1].toInt()
                    for (i in 0..99) {
                        val digits = i.toString().map { it.toString().toInt() }
                        val digitSum = digits.sum()
                        if (digitSum % 10 == endSum) {
                            entries.add(BetEntry(
                                i.toString().padStart(2, '0'),
                                amount,
                                "${endSum}B",
                                if (entries.isEmpty()) "1" else "0",
                                uniqueID
                            ))
                        }
                    }
                }
                
                matchPXPlus != null -> {
                    // PX+ pattern
                    val requiredDigit = matchPXPlus.groupValues[1]
                    for (i in 0..99) {
                        val numStr = i.toString().padStart(2, '0')
                        if (numStr.contains(requiredDigit)) {
                            entries.add(BetEntry(
                                numStr,
                                amount,
                                "${requiredDigit}ပါ",
                                if (entries.isEmpty()) "1" else "0",
                                uniqueID
                            ))
                        }
                    }
                }
                
                matchPlus != null -> {
                    // N+ pattern (same as PX+)
                    val requiredDigit = matchPlus.groupValues[1]
                    for (i in 0..99) {
                        val numStr = i.toString().padStart(2, '0')
                        if (numStr.contains(requiredDigit)) {
                            entries.add(BetEntry(
                                numStr,
                                amount,
                                "${requiredDigit}ပါ",
                                if (entries.isEmpty()) "1" else "0",
                                uniqueID
                            ))
                        }
                    }
                }
                
                matchSN != null -> {
                    // sN pattern (ရှေ့စုံ)
                    val digit = matchSN.groupValues[1].toInt()
                    for (i in 0..99) {
                        val numStr = i.toString().padStart(2, '0')
                        val firstDigit = numStr[0].toString().toInt()
                        if (firstDigit % 2 == digit % 2) {
                            entries.add(BetEntry(
                                numStr,
                                amount,
                                "s${digit}",
                                if (entries.isEmpty()) "1" else "0",
                                uniqueID
                            ))
                        }
                    }
                }
                
                matchNs != null -> {
                    // Ns pattern (နောက်စုံ)
                    val digit = matchNs.groupValues[1].toInt()
                    for (i in 0..99) {
                        val numStr = i.toString().padStart(2, '0')
                        val lastDigit = numStr[1].toString().toInt()
                        if (lastDigit % 2 == digit % 2) {
                            entries.add(BetEntry(
                                numStr,
                                amount,
                                "${digit}s",
                                if (entries.isEmpty()) "1" else "0",
                                uniqueID
                            ))
                        }
                    }
                }
                
                matchMN != null -> {
                    // mN pattern (ရှေ့မ)
                    val digit = matchMN.groupValues[1].toInt()
                    for (i in 0..99) {
                        val numStr = i.toString().padStart(2, '0')
                        val firstDigit = numStr[0].toString().toInt()
                        if (firstDigit % 2 != digit % 2) {
                            entries.add(BetEntry(
                                numStr,
                                amount,
                                "m${digit}",
                                if (entries.isEmpty()) "1" else "0",
                                uniqueID
                            ))
                        }
                    }
                }
                
                matchNM != null -> {
                    // NM pattern (နောက်မ)
                    val digit = matchNM.groupValues[1].toInt()
                    for (i in 0..99) {
                        val numStr = i.toString().padStart(2, '0')
                        val lastDigit = numStr[1].toString().toInt()
                        if (lastDigit % 2 != digit % 2) {
                            entries.add(BetEntry(
                                numStr,
                                amount,
                                "${digit}m",
                                if (entries.isEmpty()) "1" else "0",
                                uniqueID
                            ))
                        }
                    }
                }
                
                matchDot != null -> {
                    // Dot pattern (အပြီးပေါက်)
                    val digits = matchDot.groupValues[1].toCharArray().map { it.toString() }
                    digits.forEach { firstDigit ->
                        digits.forEach { secondDigit ->
                            entries.add(BetEntry(
                                "${firstDigit}${secondDigit}",
                                amount,
                                "${matchDot.groupValues[1]} ${matchDot.groupValues[2]}",
                                if (entries.isEmpty()) "1" else "0",
                                uniqueID
                            ))
                        }
                    }
                }
                
                matchDotDash != null -> {
                    // Dot dash pattern (..)
                    val digits = matchDotDash.groupValues[1].toCharArray().map { it.toString() }
                    digits.forEach { firstDigit ->
                        digits.forEach { secondDigit ->
                            if (firstDigit != secondDigit) {
                                entries.add(BetEntry(
                                    "${firstDigit}${secondDigit}",
                                    amount,
                                    "${matchDotDash.groupValues[1]} ${matchDotDash.groupValues[2]}",
                                    if (entries.isEmpty()) "1" else "0",
                                    uniqueID
                                ))
                            }
                        }
                    }
                }
                
                matchPlusPattern != null -> {
                    // Plus pattern (N+N)
                    val firstGroup = matchPlusPattern.groupValues[1].toCharArray().map { it.toString() }
                    val secondGroup = matchPlusPattern.groupValues[2].toCharArray().map { it.toString() }
                    val modifier = matchPlusPattern.groupValues[3]
                    
                    val numberCombinations = mutableSetOf<String>()
                    
                    // Create combinations from first and second groups
                    firstGroup.forEach { firstDigit ->
                        secondGroup.forEach { secondDigit ->
                            numberCombinations.add("${firstDigit}${secondDigit}")
                        }
                    }
                    
                    // If modifier is R or /, add reverse combinations
                    if (modifier.uppercase() == "R" || modifier == "/") {
                        firstGroup.forEach { firstDigit ->
                            secondGroup.forEach { secondDigit ->
                                numberCombinations.add("${secondDigit}${firstDigit}")
                            }
                        }
                    }
                    
                    // Add all combinations to entries
                    numberCombinations.forEachIndexed { index, num ->
                        entries.add(BetEntry(
                            num,
                            amount,
                            rule,
                            if (index == 0) "1" else "0",
                            uniqueID
                        ))
                    }
                }
                
                // R unit patterns (r100, 12r34, etc.)
                rule.matches(Regex("^r\\d+$", RegexOption.IGNORE_CASE)) -> {
                    val rMatch = Regex("^r(\\d+)$", RegexOption.IGNORE_CASE).find(rule)
                    if (rMatch != null) {
                        val number = rMatch.groupValues[1]
                        val convertedUnitPrice = "${number}r${number}"
                        val numberEntries = NumberUtil.processPatternRUnit(number, amount, uniqueID.toString())
                        numberEntries.forEach { entry ->
                            entries.add(BetEntry(
                                entry.number,
                                entry.amount.toInt(),
                                "${number}R",
                                entry.showSummary,
                                uniqueID
                            ))
                        }
                    }
                }
                
                rule.matches(Regex("^\\d+r\\d+$", RegexOption.IGNORE_CASE)) -> {
                    val numberEntries = NumberUtil.processPatternRUnit(rule.replace("r", "", ignoreCase = true), amount, uniqueID.toString())
                    numberEntries.forEach { entry ->
                        entries.add(BetEntry(
                            entry.number,
                            entry.amount.toInt(),
                            "${rule}R",
                            entry.showSummary,
                            uniqueID
                        ))
                    }
                }
                
                // Pattern keys
                TwoDPatterns.patterns.containsKey(rule.lowercase()) -> {
                    val key = TwoDPatterns.aliasMap[rule.lowercase()] ?: rule.lowercase()
                    TwoDPatterns.patterns[key]?.forEachIndexed { index, num ->
                        entries.add(BetEntry(num, amount, key, if (index == 0) "1" else "0", uniqueID))
                    }
                }
                
                // Two digit number
                rule.matches(Regex("^\\d{2}$")) -> {
                    entries.add(BetEntry(rule, amount, rule, "1", uniqueID))
                }
                
                // Group pattern
                rule.matches(Regex("^(\\d{2}\\.)+\\d{2}$")) || rule.matches(Regex("^(\\d{2}\\.)+\\d{2}\\.$")) -> {
                    val cleanRule = rule.replace("\\.$".toRegex(), "")
                    val numbers = cleanRule.split(".")
                    numbers.forEachIndexed { index, num ->
                        entries.add(BetEntry(
                            num,
                            amount,
                            cleanRule,
                            if (index == 0) "1" else "0",
                            uniqueID
                        ))
                    }
                }
            }
        }
        
        return entries
    }
    
    private fun incrementNumber(tempListStore: TempListStore? = null): String {
        val current = _state.value.number2D.toIntOrNull() ?: 0
        val existingNumbers = tempListStore?.getItems()?.map { it.number.toIntOrNull() ?: -1 }?.toSet() ?: emptySet()
        
        var next = (current + 1) % 100
        while (existingNumbers.contains(next)) {
            next = (next + 1) % 100
        }
        
        return next.toString().padStart(2, '0')
    }
    
    private fun checkHotNumbers(entries: List<BetEntry>) {
        val hotNumbersSet = _state.value.hotList.toSet()
        val hasHot = entries.any { hotNumbersSet.contains(it.number) }
        
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
        println("[DEBUG] TwoDViewModel: openModal() called")
        _state.value = _state.value.copy(isModalOpen = true)
        println("[DEBUG] TwoDViewModel: isModalOpen set to ${_state.value.isModalOpen}")
    }
    
    fun closeModal() {
        println("[DEBUG] TwoDViewModel: closeModal() called")
        _state.value = _state.value.copy(isModalOpen = false)
        println("[DEBUG] TwoDViewModel: isModalOpen set to ${_state.value.isModalOpen}")
    }
    
    fun updateDiscount(user: User?, apiUserData: List<ApiUserData>, is2D: Boolean = true) {
        val discount = user?.let { u ->
            val userData = apiUserData.find { it.userId == u.userId }
            val discountValue = if (is2D) {
                userData?.discount2D ?: 0
            } else {
                userData?.discount3D ?: 0
            }
            discountValue
        } ?: 0
        
        _state.value = _state.value.copy(discount = discount)
    }
    
    fun calculateRemainingUnit(ledgerStore: LedgerStore, breakAmount: Int, tempListStore: TempListStore? = null) {
        val currentState = _state.value
        val unitValue = currentState.number2D
        
        println("[DEBUG] calculateRemainingUnit called - unitValue: '$unitValue', number2D: '${currentState.number2D}'")
        
        // Check if unit input matches 2-digit pattern
        if (unitValue.matches(Regex("^\\d{2}$"))) {
            val number2D = currentState.number2D
            
            // Find total ledger unit for the current number from LedgerStore
            val ledgerStoreState = ledgerStore.state.value
            println("[DEBUG] LedgerStore filteredData size: ${ledgerStoreState.filteredData.size}")
            
            val totalLedgerUnit = ledgerStoreState.filteredData
                .find { it.number == number2D }
                ?.totalAmount ?: 0
            
            println("[DEBUG] Found totalLedgerUnit: $totalLedgerUnit for number2D: $number2D")
            
            // Calculate total list unit from temp store for the current number
            val totalListUnit = tempListStore?.getItems()
                ?.filter { it.number == number2D }
                ?.sumOf { it.amount.toIntOrNull() ?: 0 } ?: 0
            
            println("[DEBUG] TempListStore items for number $number2D: ${tempListStore?.getItems()?.filter { it.number == number2D }}")
            println("[DEBUG] Found totalListUnit: $totalListUnit for number2D: $number2D")
            
            // Calculate remaining unit
            val remain = breakAmount.toInt() - (totalLedgerUnit.toInt() + totalListUnit.toInt())
            
            println("[DEBUG] Calculation: breakAmount($breakAmount) - totalLedgerUnit($totalLedgerUnit) - totalListUnit($totalListUnit) = $remain")
            
            _state.value = currentState.copy(
                remainingUnitInput = remain.toString()
            )
            
            println("[DEBUG] Updated remainingUnitInput to: ${remain}")
        } else {
            println("[DEBUG] Unit value '$unitValue' does not match 2-digit pattern")
        }
    }
}

@Composable
fun TwoDView(
    termId: String?,
    user: User?,
    unitPrice: Int,
    breakAmount: Int,
    playFailSong: () -> Unit,
    playSuccessSong: () -> Unit,
    playDuplicateSong: () -> Unit,
    numberInput: String?,
    apiUserData: List<ApiUserData>,
    onUserSelectionChanged: (() -> Unit)? = null,
    onOpenBetAndReminingModal: (String) -> Unit = {},
    viewModel: TwoDViewModel = remember { TwoDViewModel() }
) {
    // Cache expensive state calculations
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val tempListStore = rememberTempListStore()
    val tempListState by tempListStore.state.collectAsState()
    val ledgerStore = remember { LedgerStore.getInstance() }
    
    // Derived state to avoid unnecessary recompositions
    val isUserSelected by remember { derivedStateOf { user != null } }
    val totalCalculation by remember { derivedStateOf { 
        state.totalAmount + state.remainAmount 
    }}
    val hasValidInput by remember { derivedStateOf {
        state.number2D.isNotEmpty() && state.unit2D.text.isNotEmpty()
    }}
    val discountedAmount by remember { derivedStateOf {
        if (state.discount > 0) {
            (state.totalAmount * (100 - state.discount) / 100)
        } else state.totalAmount
    }}
    
    // Update discount when user changes - optimized with key
    LaunchedEffect(key1 = user?.userId, key2 = apiUserData.size) {
        viewModel.updateDiscount(user, apiUserData, true) // TwoDView always uses discount2D
    }
    
    // Focus number input when user is selected - optimized with key
    LaunchedEffect(key1 = user?.userId) {
        if (user != null) {
            onUserSelectionChanged?.invoke()
            viewModel.focusNumberInput()
        }
    }
    
    // Focus number input when numberInput is "2D" - optimized with key
    LaunchedEffect(key1 = numberInput) {
        if (numberInput == "2D") {
            viewModel.focusNumberInput()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
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
                    checked = state.autoIncrement2D,
                    onCheckedChange = viewModel::handleAutoIncrementChange
                )
                
                CompactOutlinedTextField(
                    value = state.number2D,
                    onValueChange = viewModel::updateNumber2D,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(viewModel.numberFocusRequester)
                        .onKeyEvent { keyEvent ->
                        when {
                            keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown -> {
                                if (viewModel.validateNumber2D()) {
                                    viewModel.calculateRemainingUnit(ledgerStore, breakAmount, tempListStore)
                                    viewModel.focusUnitInput()
                                }else{
                                    AudioPlayer.playFailSong()
                                }
                                true
                            }
                            keyEvent.key == Key.F2 && keyEvent.type == KeyEventType.KeyDown -> {
                                println("[DEBUG] F2 key pressed in TwoDView")
                                if (user == null) {
                                    println("[DEBUG] Cannot open modal - no user selected")
                                    // TODO: Show toast message "ဒိုင်ရွေးပါ" (Please select user)
                                } else if (termId == null) {
                                    println("[DEBUG] Cannot open modal - no term selected")
                                    // TODO: Show toast message "အပတ်စဉ်ရွေးပါ" (Please select term)
                                } else {
                                    println("[DEBUG] Opening BetAndRemining modal - user: ${user.name}, termId: $termId, number: ${state.number2D}")
                                    onOpenBetAndReminingModal(state.number2D)
                                }
                                true
                            }
                            else -> false
                        }
                    },
                    enabled = !state.autoIncrement2D,
                    isError = state.number2DError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { 
                            println("Number Input: ${state.number2D}")
                            if (viewModel.validateNumber2D()) {
                                viewModel.calculateRemainingUnit(ledgerStore, breakAmount, tempListStore)
                                viewModel.focusUnitInput()
                            }else{
                                AudioPlayer.playFailSong()
                            }
                            //viewModel.focusUnitInput() 
                        }
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            // Unit Input
            CompactOutlinedTextField(
                value = state.unit2D,
                onValueChange = viewModel::updateUnitPrice2D,
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
                    value = state.remainingUnitInput,
                    onValueChange = { },
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    borderColor = Color.Black,
                    focusedBorderColor = Color.Black,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
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
                CustomTextField(
                    value = tempListStore.getTotalAmount().toString(),
                    onValueChange = { },
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    borderColor = Color.Black,
                    focusedBorderColor = Color.Black,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
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
                val discountAmount = (((tempListStore.getTotalAmount() * unitPrice) * state.discount).toDouble()).roundToInt()
                CustomTextField(
                    value = discountAmount.toString(),
                    onValueChange = { },
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    borderColor = Color.Black,
                    focusedBorderColor = Color.Black,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
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
                    borderColor = Color.Black,
                    focusedBorderColor = Color.Black,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
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
                    borderColor = Color.Black,
                    focusedBorderColor = Color.Black,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
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
                    borderColor = Color.Black,
                    focusedBorderColor = Color.Black,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
            }
        }
        
    }
    
    // Modal overlay for BetAndRemining
    if (state.isModalOpen) {
        println("[DEBUG] TwoDView: Rendering modal overlay - isModalOpen: ${state.isModalOpen}")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.6f),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Header with title and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bet and Remaining Data",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { viewModel.closeModal() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Close")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // BetAndRemining table component
                    if (user != null && termId != null && state.number2D.isNotEmpty()) {
                        val userOption = UserOption(user.userId, user.name)
                        println("[DEBUG] TwoDView: Rendering BetAndRemining component - number: ${state.number2D}, user: ${user.name}, termId: $termId")
                        
                        BetAndRemining(
                            number = state.number2D,
                            user = userOption,
                            termId = termId
                        )
                    } else {
                        println("[DEBUG] TwoDView: Cannot render BetAndRemining - user: $user, termId: $termId, number2D: ${state.number2D}")
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
    } else {
        println("[DEBUG] TwoDView: Modal not open - isModalOpen: ${state.isModalOpen}")
    }
    
    // Keyboard shortcuts
    LaunchedEffect(Unit) {
        // Handle global keyboard shortcuts
        // This would need to be implemented with proper key event handling
    }
}
