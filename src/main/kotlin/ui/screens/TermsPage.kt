package ui.screens

import core.services.ApiService
import core.services.UserSession
import data.models.TermsApiResponse
import data.models.TermsApiResponseData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Surface
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ui.screens.BreakAmountSerializer
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.Locale
import java.time.format.DateTimeFormatter
import java.time.LocalTime
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

@Serializable
data class TermData(
    val termId: Int,
    val groupId: String,
    val is2D: String,
    val termName: String,
    val shortName: String,
    @Serializable(with = BreakAmountSerializer::class)
    val breakAmount: Int,
    val winNum: String? = null,
    val unitPrice: Int,
    val isFinished: String,
    val startDate: String,
    val endDate: String,
    val businessId: String,
    val termType: String,
    val shareOption: String? = null,
    val accessUsers: String? = null,
    val last_updated_at: String,
    val created_at: String,
    val deleted: String,
    val deletedAt: String? = null,
    val note: String? = null,
    val tNumbers: Array<String>? = null,
    val active: String
)

@Serializable
data class PaginationData(
    val current: Int,
    val limit: Int,
    val rowsPerPage: Int,
    val total: Int
)

// TermsApiResponse and TermsApiResponseData are now imported from data.models

@Serializable
data class TimeSlot(
    val startTime: String,
    val endTime: String
)

@Serializable
data class AddTermRequest(
    val term: List<TermData>
)

// Helper functions
fun convertDateFormat(dateStr: String): String {
    return try {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val date = LocalDate.parse(dateStr, inputFormatter)
        date.format(outputFormatter)
    } catch (e: Exception) {
        dateStr
    }
}

fun formatDateTimeToMyanmarTimezone(dateTimeString: String): String {
    return try {
        println("[DEBUG] Input dateTimeString: '$dateTimeString'")
        
        val cleanedInput = dateTimeString.trim()
        
        val inputFormatter = if (cleanedInput.contains("AM") || cleanedInput.contains("PM")) {
            DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
        } else {
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        }
        
        val localDateTime = LocalDateTime.parse(cleanedInput, inputFormatter)
        println("[DEBUG] Parsed LocalDateTime: $localDateTime")
        
        val myanmarZone = ZoneId.of("Asia/Yangon")
        val zonedDateTime = localDateTime.atZone(myanmarZone)
        println("[DEBUG] ZonedDateTime in Myanmar: $zonedDateTime")
        
        val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val result = zonedDateTime.format(outputFormatter)
        println("[DEBUG] Final formatted result: '$result'")
        
        result
    } catch (e: Exception) {
        println("[ERROR] Failed to format datetime '$dateTimeString': ${e.message}")
        e.printStackTrace()
        dateTimeString
    }
}

fun formatDateTimeForApi(dateTimeString: String): String {
    return try {
        val inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
        val localDateTime = LocalDateTime.parse(dateTimeString, inputFormatter)
        
        val myanmarZone = ZoneId.of("Asia/Yangon")
        val zonedDateTime = localDateTime.atZone(myanmarZone)
        
        val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:ss")
        zonedDateTime.format(outputFormatter)
    } catch (e: Exception) {
        println("[ERROR] Failed to format datetime for API: ${e.message}")
        dateTimeString
    }
}

fun convertAmPmTo24Hour(dateTimeString: String): String {
    return try {
        println("[DEBUG] Converting AM/PM to 24-hour: '$dateTimeString'")
        
        if (!dateTimeString.contains("AM") && !dateTimeString.contains("PM")) {
            println("[DEBUG] No AM/PM found, returning as is")
            return dateTimeString
        }
        
        val parts = dateTimeString.split(" ")
        if (parts.size < 3) {
            println("[DEBUG] Invalid format, expected 'DD/MM/YYYY HH:MM AM/PM'")
            return dateTimeString
        }
        
        val datePart = parts[0] // DD/MM/YYYY
        val timePart = parts[1] // HH:MM
        val amPmPart = parts[2] // AM/PM
        
        val timeComponents = timePart.split(":")
        if (timeComponents.size != 2) {
            println("[DEBUG] Invalid time format")
            return dateTimeString
        }
        
        var hour = timeComponents[0].toInt()
        val minute = timeComponents[1]
        
        when (amPmPart.uppercase()) {
            "AM" -> {
                if (hour == 12) hour = 0
            }
            "PM" -> {
                if (hour != 12) hour += 12
            }
        }
        
        val result = "$datePart ${String.format("%02d", hour)}:$minute"
        println("[DEBUG] Converted result: '$result'")
        return result
        
    } catch (e: Exception) {
        println("[ERROR] Failed to convert AM/PM to 24-hour: ${e.message}")
        return dateTimeString
    }
}

fun getAllDaysBetweenDates(startDate: String, endDate: String): List<String> {
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val start = LocalDate.parse(startDate, formatter)
        val end = LocalDate.parse(endDate, formatter)
        
        val days = mutableListOf<String>()
        var current = start
        while (!current.isAfter(end)) {
            days.add(current.format(formatter))
            current = current.plusDays(1)
        }
        days
    } catch (e: Exception) {
        println("[ERROR] Failed to get days between dates: ${e.message}")
        emptyList()
    }
}

fun formatTime(time: String, format: String): String {
    return try {
        when (format) {
            "12" -> {
                if (time.contains("AM") || time.contains("PM")) {
                    time
                } else {
                    val timeParts = time.split(":")
                    if (timeParts.size >= 2) {
                        val hour = timeParts[0].toInt()
                        val minute = timeParts[1]
                        when {
                            hour == 0 -> "12:$minute AM"
                            hour < 12 -> "$hour:$minute AM"
                            hour == 12 -> "12:$minute PM"
                            else -> "${hour - 12}:$minute PM"
                        }
                    } else {
                        time
                    }
                }
            }
            "24" -> {
                if (time.contains("AM") || time.contains("PM")) {
                    convertAmPmTo24Hour("01/01/2000 $time").substringAfter(" ")
                } else {
                    time
                }
            }
            else -> time
        }
    } catch (e: Exception) {
        println("[ERROR] Failed to format time: ${e.message}")
        time
    }
}

fun toLocalISOString(dateTimeString: String): String {
    return try {
        if (dateTimeString.isBlank()) {
            return ""
        }
        
        val cleanInput = dateTimeString.trim()
        
        // Try to parse ISO format with milliseconds and Z (yyyy-MM-ddTHH:mm:ss.SSSZ)
        if (cleanInput.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"))) {
            // Parse as UTC instant and convert to system timezone
            val instant = java.time.Instant.parse(cleanInput)
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            
            val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            return zonedDateTime.format(outputFormatter)
        }
        
        // Try to parse ISO format (yyyy-MM-dd HH:mm:ss)
        if (cleanInput.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"))) {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val localDateTime = LocalDateTime.parse(cleanInput, inputFormatter)
            
            val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            return localDateTime.format(outputFormatter)
        }
        
        // Try to parse ISO format without seconds (yyyy-MM-dd HH:mm)
        if (cleanInput.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"))) {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val localDateTime = LocalDateTime.parse(cleanInput, inputFormatter)
            
            val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            return localDateTime.format(outputFormatter)
        }
        
        // If no pattern matches, return empty string
        return ""
        
    } catch (e: Exception) {
        println("[ERROR] Failed to convert to local ISO string '$dateTimeString': ${e.message}")
        return ""
    }
}

fun convertDisplayToISOString(dateTimeString: String): String {
    return try {
        println("[DEBUG] convertDisplayToISOString input: '$dateTimeString'")
        
        if (dateTimeString.isBlank()) {
            println("[DEBUG] convertDisplayToISOString: Input is blank, returning empty string")
            return ""
        }
        
        val cleanInput = dateTimeString.trim()
        println("[DEBUG] convertDisplayToISOString cleanInput: '$cleanInput'")
        
        // Try to parse display format (dd/MM/yyyy h:mm a or dd/MM/yyyy hh:mm a)
        if (cleanInput.matches(Regex("\\d{2}/\\d{2}/\\d{4} \\d{1,2}:\\d{2} (AM|PM)", RegexOption.IGNORE_CASE))) {
            println("[DEBUG] convertDisplayToISOString: Matched AM/PM format")
            // Use flexible pattern that handles both single and double digit hours
            val inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a", Locale.ENGLISH)
            val localDateTime = LocalDateTime.parse(cleanInput, inputFormatter)
            
            val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm")
            val result = localDateTime.format(outputFormatter)
            println("[DEBUG] convertDisplayToISOString AM/PM result: '$result'")
            return result
        }
        
        // Try to parse the new edit form format (yyyy-MM-dd HH:mm)
        if (cleanInput.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"))) {
            println("[DEBUG] convertDisplayToISOString: Matched yyyy-MM-dd HH:mm format, returning as-is")
            // Already in the correct format, just return it
            return cleanInput
        }
        
        println("[DEBUG] convertDisplayToISOString: No pattern matched, returning empty string")
        // If no pattern matches, return empty string
        return ""
        
    } catch (e: Exception) {
        println("[ERROR] Failed to convert display to ISO string '$dateTimeString': ${e.message}")
        return ""
    }
}

fun convertISOToApiFormat(isoString: String): String {
    try {
        if (isoString.isBlank()) {
            return ""
        }
        
        // Parse "yyyy-MM-dd HH:mm" format
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val localDateTime = LocalDateTime.parse(isoString, inputFormatter)
        
        // Convert from system timezone to Yangon timezone and format for API
        val systemZone = ZoneId.systemDefault()
        val yangonZone = ZoneId.of("Asia/Yangon")
        val zonedDateTime = localDateTime.atZone(systemZone).withZoneSameInstant(yangonZone)
        val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return zonedDateTime.format(outputFormatter)
    } catch (e: Exception) {
        println("[ERROR] Failed to convert ISO to API format '$isoString': ${e.message}")
        return isoString
    }
}

fun convertISOToApiFormatFor3D(isoString: String): String {
    try {
        if (isoString.isBlank()) {
            return ""
        }
        
        // Parse "yyyy-MM-dd HH:mm" format
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val localDateTime = LocalDateTime.parse(isoString, inputFormatter)
        
        // Convert from system timezone to Yangon timezone and format for 3D API (YYYY-MM-DD HH:mm)
        val systemZone = ZoneId.systemDefault()
        val yangonZone = ZoneId.of("Asia/Yangon")
        val zonedDateTime = localDateTime.atZone(systemZone).withZoneSameInstant(yangonZone)
        val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return zonedDateTime.format(outputFormatter)
    } catch (e: Exception) {
        println("[ERROR] Failed to convert ISO to 3D API format '$isoString': ${e.message}")
        return isoString
    }
}

fun formatDateTime(dateTimeString: String): String {
    return try {
        println("[DEBUG] Formatting datetime: '$dateTimeString'")
        
        if (dateTimeString.isBlank()) {
            return ""
        }
        
        // Handle different input formats
        val cleanInput = dateTimeString.trim()
        
        // If it's already in the desired format, return as is
        if (cleanInput.matches(Regex("\\d{2}/\\d{2}/\\d{4} \\d{1,2}:\\d{2} (AM|PM)", RegexOption.IGNORE_CASE))) {
            return cleanInput
        }
        
        val yangonZone = ZoneId.of("Asia/Yangon")
        val systemZone = ZoneId.systemDefault()
        
        // Try to parse ISO format with milliseconds and Z (yyyy-MM-ddTHH:mm:ss.SSSZ)
        if (cleanInput.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"))) {
            // Parse as UTC instant and convert to system timezone
            val instant = java.time.Instant.parse(cleanInput)
            val zonedDateTime = instant.atZone(systemZone)
            
            val outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
            return zonedDateTime.format(outputFormatter)
        }
        
        // Try to parse ISO format (yyyy-MM-dd HH:mm:ss)
        if (cleanInput.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"))) {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val localDateTime = LocalDateTime.parse(cleanInput, inputFormatter)
            // Treat the input as already in system timezone (no conversion needed)
            val zonedDateTime = localDateTime.atZone(systemZone)
            
            val outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
            return zonedDateTime.format(outputFormatter)
        }
        
        // Try to parse ISO format without seconds (yyyy-MM-dd HH:mm)
        if (cleanInput.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"))) {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val localDateTime = LocalDateTime.parse(cleanInput, inputFormatter)
            // Treat the input as already in system timezone (no conversion needed)
            val zonedDateTime = localDateTime.atZone(systemZone)
            
            val outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
            return zonedDateTime.format(outputFormatter)
        }
        
        // If no pattern matches, return the original string
        println("[DEBUG] No matching pattern found, returning original: '$cleanInput'")
        return cleanInput
        
    } catch (e: Exception) {
        println("[ERROR] Failed to format datetime '$dateTimeString': ${e.message}")
        return dateTimeString
    }
}

@Composable
fun TermsContent() {
    var terms by remember { mutableStateOf<List<TermData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var currentPage by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var showAddModal by remember { mutableStateOf(false) }
    var showEditModal by remember { mutableStateOf(false) }
    var selectedTerm by remember { mutableStateOf<TermData?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    
    fun loadTerms() {
        isLoading = true
        coroutineScope.launch {
            try {
                val response = ApiService.getTerms(
                    page = currentPage,
                    search = null
                )
                if (response.code == "200") {
                    // Debug: Log the first term's date format
                    if (response.data.by.isNotEmpty()) {
                        val firstTerm = response.data.by.first()
                        println("[DEBUG] API returned startDate: '${firstTerm.startDate}'")
                        println("[DEBUG] API returned endDate: '${firstTerm.endDate}'")
                    }
                    
                    // Convert data.models.TermData to ui.screens.TermData
                    terms = response.data.by.map { apiTerm ->
                        TermData(
                            termId = apiTerm.termId,
                            groupId = apiTerm.groupId,
                            is2D = if (apiTerm.is2D) "1" else "0",
                            termName = apiTerm.termName,
                            shortName = apiTerm.shortName,
                            breakAmount = apiTerm.breakAmount,
                            winNum = apiTerm.winNum,
                            unitPrice = apiTerm.unitPrice.toInt(),
                            isFinished = apiTerm.isFinished,
                            startDate = apiTerm.startDate,
                            endDate = apiTerm.endDate,
                            businessId = "",
                            termType = apiTerm.termType,
                            shareOption = null,
                            accessUsers = "",
                            last_updated_at = "",
                            created_at = "",
                            deleted = "0",
                            deletedAt = null,
                            note = null,
                            tNumbers = null,
                            active = "0"
                        )
                    }
                    totalPages = (response.data.pagination.total + response.data.pagination.limit - 1) / response.data.pagination.limit
                }
            } catch (e: Exception) {
                println("Error loading terms: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(currentPage) {
        loadTerms()
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "အပတ်စဉ်",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = { showAddModal = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Term",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "အသစ်လုပ်မည်",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        

        
        // Terms Table
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "အမည်",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            text = "အမျိုးအစား",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "ဘရိတ်",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "ပေါက်သီး",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "စတင်ချိန်",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "ပြီးဆုံးချိန်",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Divider()
                    
                    // Table Rows
                    LazyColumn {
                        if (terms.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No terms found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(terms) { term ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .clickable {
                                        selectedTerm = term
                                        showEditModal = true
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = term.termName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(2f)
                                )
                                Text(
                                    text = if (term.is2D == "1") "2D" else "3D",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (term.is2D == "1") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = term.breakAmount.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1.5f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = term.winNum ?: "-",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1.5f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = formatDateTime(term.startDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1.5f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = formatDateTime(term.endDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1.5f),
                                    textAlign = TextAlign.Center
                                )
                                Row(
                                    modifier = Modifier.weight(1.5f),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                        onClick = {
                                            selectedTerm = term
                                            showEditModal = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Term",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            
                            if (terms.indexOf(term) < terms.size - 1) {
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            }
                        }
                        }
                    }
                }
            }
        }
        
        // Pagination
        if (totalPages > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { if (currentPage > 1) currentPage-- },
                    enabled = currentPage > 1
                ) {
                    Text("Previous")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    "Page $currentPage of $totalPages",
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Button(
                    onClick = { if (currentPage < totalPages) currentPage++ },
                    enabled = currentPage < totalPages
                ) {
                    Text("Next")
                }
            }
        }
    }
    
    // Add Modal
    if (showAddModal) {
        TermFormModal(
            term = null,
            onDismiss = { showAddModal = false },
            onSave = { termData ->
                coroutineScope.launch {
                    try {
                        // Generate groupId for 3D terms
                        val groupId = (100000 + Random.nextInt(900000)).toString()
                        
                        // Convert AM/PM format to ISO format first, then format for 3D API (YYYY-MM-DD HH:mm format)
                        val isoStartDate = convertDisplayToISOString(termData.startDate)
                        val isoEndDate = convertDisplayToISOString(termData.endDate)
                        val formattedStartDate = convertISOToApiFormatFor3D(isoStartDate)
                        val formattedEndDate = convertISOToApiFormatFor3D(isoEndDate)
                        
                        // Create 3D term data with required payload structure
                        val add3DData = termData.copy(
                            groupId = groupId,
                            is2D = "0",
                            termName = termData.termName,
                            shortName = termData.termName,
                            startDate = formattedStartDate,
                            endDate = formattedEndDate,
                            breakAmount = termData.breakAmount
                        )
                        val request = AddTermRequest(term = listOf(add3DData))
                        println("[DEBUG] 3D Term creation request: $request")
                        val response = ApiService.addTerm(request)
                        if (response.code == "200") {
                             showAddModal = false
                             loadTerms()
                        }
                    } catch (e: Exception) {
                        println("Error adding term: ${e.message}")
                    }
                }
            },
            onSave2D = { termData, timeSlots ->
                coroutineScope.launch {
                    try {
                        // Generate groupId for 2D terms
                         val groupId = (100000 + Random.nextInt(900000)).toString()
                        
                        val days = getAllDaysBetweenDates(termData.startDate, termData.endDate)
                        val termsToCreate = mutableListOf<TermData>()
                        
                        days.forEach { day ->
                            timeSlots.forEach { (startTime, endTime) ->
                                val startDateTime = convertAmPmTo24Hour("$day $startTime")
                                val endDateTime = convertAmPmTo24Hour("$day $endTime")
                                
                                val formattedStartDateTime = formatDateTimeToMyanmarTimezone(startDateTime)
                                val formattedEndDateTime = formatDateTimeToMyanmarTimezone(endDateTime)
                                
                                val newTerm = termData.copy(
                                    groupId = groupId,
                                    is2D = "1",
                                    termName = "${termData.termName}_${Random.nextInt(1000, 9999)}",
                                    shortName = termData.termName,
                                    startDate = formattedStartDateTime,
                                    endDate = formattedEndDateTime,
                                    breakAmount = termData.breakAmount
                                )
                                termsToCreate.add(newTerm)
                            }
                        }
                        
                        println("[DEBUG] 2D Terms creation payload: groupId=$groupId, is2D=1, termCount=${termsToCreate.size}, breakAmount=${termData.breakAmount}")
                        
                        val request = AddTermRequest(term = termsToCreate)
                        val response = ApiService.addTerm(request)
                        if (response.code == "200") {
                              showAddModal = false
                              loadTerms()
                        }
                    } catch (e: Exception) {
                        println("Error adding 2D terms: ${e.message}")
                    }
                }
            }
        )
    }
    
    // Edit Modal
    if (showEditModal && selectedTerm != null) {
        TermFormModal(
            term = selectedTerm,
            onDismiss = {
                showEditModal = false
                selectedTerm = null
            },
            onSave = { termData ->
                coroutineScope.launch {
                    try {
                        println("[DEBUG] Term update - Raw termData.startDate: '${termData.startDate}'")
                        println("[DEBUG] Term update - Raw termData.endDate: '${termData.endDate}'")
                        
                        // Create UpdateTermRequest with properly formatted dates
                        val formattedStartDate = convertISOToApiFormat(termData.startDate)
                        val formattedEndDate = convertISOToApiFormat(termData.endDate)
                        
                        println("[DEBUG] Term update - Formatted startDate: '$formattedStartDate'")
                        println("[DEBUG] Term update - Formatted endDate: '$formattedEndDate'")
                        
                        val updateRequest = ApiService.UpdateTermRequest(
                            termId = termData.termId.toString(),
                            termName = termData.termName,
                            shortName = termData.shortName ?: "",
                            groupId = termData.groupId.toString(),
                            startDate = formattedStartDate,
                            endDate = formattedEndDate,
                            isFinished = termData.isFinished,
                            termType = termData.termType,
                            winNum = termData.winNum.toString(),
                            //is2D = termData.is2D.toIntOrNull() ?: 0,
                            unitPrice = termData.unitPrice.toDouble(),
                            breakAmount = termData.breakAmount
                        )
                        println("[DEBUG] Term update: '${updateRequest}'")
                        println("[DEBUG] Term update - UpdateRequest startDate: '${updateRequest.startDate}'")
                        println("[DEBUG] Term update - UpdateRequest endDate: '${updateRequest.endDate}'")
                        
                        val apiService = ApiService()
                        val response = apiService.updateTerm(updateRequest)
                        
                        println("[DEBUG] Term update - API response: ${response}")
                        
                        if (response.success) {
                            showEditModal = false
                            selectedTerm = null
                            loadTerms()
                        } else {
                            println("[DEBUG] Term update - API response failed: ${response.message}")
                        }
                    } catch (e: Exception) {
                        println("[ERROR] Error updating term: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        )
    }
    

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoDFormLayout(
    breakAmount: String,
    onBreakAmountChange: (String) -> Unit,
    prefix: String,
    onPrefixChange: (String) -> Unit,
    selectedStartDate: String,
    onSelectedStartDateChange: (String) -> Unit,
    selectedEndDate: String,
    onSelectedEndDateChange: (String) -> Unit,
    selectedStartTime: String,
    onSelectedStartTimeChange: (String) -> Unit,
    selectedEndTime: String,
    onSelectedEndTimeChange: (String) -> Unit,
    onShowStartDatePicker: () -> Unit,
    onShowEndDatePicker: () -> Unit,
    onShowStartTimePicker: () -> Unit,
    onShowEndTimePicker: () -> Unit,
    onTimeSlotDataChange: (Int, List<Pair<String, String>>) -> Unit
) {
    var numberOfRows by remember { mutableStateOf("2") }
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("2", "3", "4", "5", "6")
    
    Column {
        // Break Amount, Prefix & Number of Time Slots in same row
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = breakAmount,
                onValueChange = onBreakAmountChange,
                label = { Text("Break Amount") },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 14.sp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            OutlinedTextField(
                value = prefix,
                onValueChange = onPrefixChange,
                label = { Text("Prefix") },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 14.sp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Number of Rows Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = numberOfRows,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Number of Time Slots") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    textStyle = TextStyle(fontSize = 14.sp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                numberOfRows = option
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Date Range
        Row {
            OutlinedTextField(
                value = selectedStartDate,
                onValueChange = {},
                label = { Text("Start Date") },
                readOnly = true,
                modifier = Modifier.weight(1f).clickable { onShowStartDatePicker() },
                trailingIcon = {
                    IconButton(onClick = onShowStartDatePicker) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    }
                },
                textStyle = TextStyle(fontSize = 14.sp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            OutlinedTextField(
                value = selectedEndDate,
                onValueChange = {},
                label = { Text("End Date") },
                readOnly = true,
                modifier = Modifier.weight(1f).clickable { onShowEndDatePicker() },
                trailingIcon = {
                    IconButton(onClick = onShowEndDatePicker) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    }
                },
                textStyle = TextStyle(fontSize = 14.sp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Dynamic Time Slots
        val numSlots = numberOfRows.toIntOrNull() ?: 2
        val timeSlots = remember(numSlots) {
            mutableStateListOf<Pair<String, String>>().apply {
                repeat(numSlots) {
                    add("11:30 PM" to "11:30 PM")
                }
            }
        }
        
        LaunchedEffect(timeSlots.toList()) {
            onTimeSlotDataChange(numSlots, timeSlots.toList())
        }
        
        timeSlots.forEachIndexed { index, (startTime, endTime) ->
            var showStartTimePicker by remember { mutableStateOf(false) }
            var showEndTimePicker by remember { mutableStateOf(false) }
            
            Row {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { newTime ->
                        timeSlots[index] = newTime to endTime
                    },
                    label = { Text("Start Time ${index + 1}") },
                    readOnly = true,
                    modifier = Modifier.weight(1f).clickable {
                        showStartTimePicker = true
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            showStartTimePicker = true
                        }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    textStyle = TextStyle(fontSize = 14.sp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { newTime ->
                        timeSlots[index] = startTime to newTime
                    },
                    label = { Text("End Time ${index + 1}") },
                    readOnly = true,
                    modifier = Modifier.weight(1f).clickable {
                        showEndTimePicker = true
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            showEndTimePicker = true
                        }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    textStyle = TextStyle(fontSize = 14.sp)
                )
            }
            
            // Time pickers for this slot
            if (showStartTimePicker) {
                TimePickerDialog(
                    onTimeSelected = { time ->
                        timeSlots[index] = time to endTime
                        showStartTimePicker = false
                    },
                    onDismiss = { showStartTimePicker = false }
                )
            }
            
            if (showEndTimePicker) {
                TimePickerDialog(
                    onTimeSelected = { time ->
                        timeSlots[index] = startTime to time
                        showEndTimePicker = false
                    },
                    onDismiss = { showEndTimePicker = false }
                )
            }
            
            if (index < timeSlots.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreeDFormLayout(
    termName: String,
    onTermNameChange: (String) -> Unit,
    selectedStartDate: String,
    onSelectedStartDateChange: (String) -> Unit,
    selectedEndDate: String,
    onSelectedEndDateChange: (String) -> Unit,
    breakAmount: String,
    onBreakAmountChange: (String) -> Unit,
    winNum: String,
    onWinNumChange: (String) -> Unit,
    unitPrice: String,
    onUnitPriceChange: (String) -> Unit,
    onShowStartDatePicker: () -> Unit,
    onShowEndDatePicker: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = termName,
            onValueChange = onTermNameChange,
            label = { Text("Term Name") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 14.sp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row {
            OutlinedTextField(
                value = selectedStartDate,
                onValueChange = {},
                label = { Text("Start Date") },
                readOnly = true,
                modifier = Modifier.weight(1f).clickable { onShowStartDatePicker() },
                trailingIcon = {
                    IconButton(onClick = onShowStartDatePicker) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    }
                },
                textStyle = TextStyle(fontSize = 14.sp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            OutlinedTextField(
                value = selectedEndDate,
                onValueChange = {},
                label = { Text("End Date") },
                readOnly = true,
                modifier = Modifier.weight(1f).clickable { onShowEndDatePicker() },
                trailingIcon = {
                    IconButton(onClick = onShowEndDatePicker) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    }
                },
                textStyle = TextStyle(fontSize = 14.sp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = breakAmount,
            onValueChange = onBreakAmountChange,
            label = { Text("Break Amount") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 14.sp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = unitPrice,
            onValueChange = onUnitPriceChange,
            label = { Text("Unit Price") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 14.sp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = winNum,
            onValueChange = onWinNumChange,
            label = { Text("Win Number") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 14.sp)
        )
        
        
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = 23,
        initialMinute = 30,
        is24Hour = false
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            TimePicker(
                state = timePickerState,
                layoutType = TimePickerLayoutType.Vertical
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hour = timePickerState.hour
                    val minute = timePickerState.minute
                    
                    // Since is24Hour = false, the TimePicker already returns 12-hour format
                    // We just need to determine AM/PM based on the hour value
                    val amPm = if (hour < 12) "AM" else "PM"
                    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                    
                    val timeString = String.format("%d:%02d %s", displayHour, minute, amPm)
                    onTimeSelected(timeString)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date") },
        text = {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // DatePicker returns milliseconds for the start of the selected day in local timezone
                        // Convert to LocalDate using system default timezone to get the correct date
                        val instant = java.time.Instant.ofEpochMilli(millis)
                        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        onDateSelected(localDate.format(formatter))
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DateTimePickerDialog(
    onDateTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(true) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    when {
        showDatePicker -> {
            DatePickerDialog(
                onDateSelected = { date ->
                    selectedDate = date
                    showDatePicker = false
                    showTimePicker = true
                },
                onDismiss = onDismiss
            )
        }
        showTimePicker -> {
            TimePickerDialog(
                onTimeSelected = { time ->
                    selectedTime = time
                    showTimePicker = false
                    if (selectedDate.isNotEmpty() && selectedTime.isNotEmpty()) {
                        onDateTimeSelected("$selectedDate $selectedTime")
                    }
                },
                onDismiss = {
                    showTimePicker = false
                    showDatePicker = true
                }
            )
        }
    }
}

@Composable
fun TermFormModal(
    term: TermData?,
    onDismiss: () -> Unit,
    onSave: (TermData) -> Unit,
    onSave2D: ((TermData, List<Pair<String, String>>) -> Unit)? = null
) {
    var termName by remember { mutableStateOf(term?.termName ?: "") }
    var breakAmount by remember { mutableStateOf(term?.breakAmount?.toString() ?: "") }
    var startDate by remember { mutableStateOf(term?.startDate?.let { toLocalISOString(it) } ?: "") }
    var endDate by remember { mutableStateOf(term?.endDate?.let { toLocalISOString(it) } ?: "") }
    var winNum by remember { mutableStateOf(term?.winNum ?: "") }
    var unitPrice by remember { mutableStateOf(term?.unitPrice?.toString() ?: "1") }
    var is2D by remember { mutableStateOf(term?.is2D == "1") }
    var prefix by remember { mutableStateOf("") }
    
    // 2D form states
    var number1 by remember { mutableStateOf("") }
    var number2 by remember { mutableStateOf("") }
    var startTime1 by remember { mutableStateOf("11:30 PM") }
    var endTime1 by remember { mutableStateOf("11:30 PM") }
    var startTime2 by remember { mutableStateOf("11:30 PM") }
    var endTime2 by remember { mutableStateOf("11:30 PM") }
    
    // Date and time picker states for 2D
    var selected2DStartDate by remember { mutableStateOf("") }
    var selected2DEndDate by remember { mutableStateOf("") }
    var selected2DStartTime by remember { mutableStateOf("") }
    var selected2DEndTime by remember { mutableStateOf("") }
    
    // Date picker states for 3D
    var selected3DStartDate by remember { mutableStateOf("") }
    var selected3DEndDate by remember { mutableStateOf("") }
    
    // Picker visibility states for 2D
    var show2DStartDatePicker by remember { mutableStateOf(false) }
    var show2DEndDatePicker by remember { mutableStateOf(false) }
    var show2DStartTimePicker by remember { mutableStateOf(false) }
    var show2DEndTimePicker by remember { mutableStateOf(false) }
    
    // Picker visibility states for 3D
    var show3DStartDatePicker by remember { mutableStateOf(false) }
    var show3DEndDatePicker by remember { mutableStateOf(false) }
    
    // Edit mode date time picker states
    var showEditStartDatePicker by remember { mutableStateOf(false) }
    var showEditEndDatePicker by remember { mutableStateOf(false) }
    
    // Time slot data for 2D
    var numberOfTimeSlots by remember { mutableStateOf(2) }
    var timeSlotPairs by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(600.dp)
                .wrapContentSize(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal=16.dp,vertical=8.dp)
            ) {
                if (term != null) {
                    // Edit mode - simplified form
                    OutlinedTextField(
                        value = termName,
                        onValueChange = { termName = it },
                        label = { Text("Term Name") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 14.sp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = {},
                            label = { Text("Start Date") },
                            readOnly = true,
                            modifier = Modifier.weight(1f).clickable { showEditStartDatePicker = true },
                            trailingIcon = {
                                IconButton(onClick = { showEditStartDatePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = null)
                                }
                            },
                            textStyle = TextStyle(fontSize = 14.sp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = {},
                            label = { Text("End Date") },
                            readOnly = true,
                            modifier = Modifier.weight(1f).clickable { showEditEndDatePicker = true },
                            trailingIcon = {
                                IconButton(onClick = { showEditEndDatePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = null)
                                }
                            },
                            textStyle = TextStyle(fontSize = 14.sp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = breakAmount,
                        onValueChange = { breakAmount = it },
                        label = { Text("Break Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 14.sp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = unitPrice,
                        onValueChange = { unitPrice = it },
                        label = { Text("Unit Price") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 14.sp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = winNum,
                        onValueChange = { winNum = it },
                        label = { Text("Win Number") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 14.sp)
                    )
                } else {
                    // Add mode - full form with 2D/3D toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !is2D,
                                onClick = { is2D = false }
                            )
                            Text("3D")
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = is2D,
                                onClick = { is2D = true }
                            )
                            Text("2D")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (is2D) {
                        TwoDFormLayout(
                            breakAmount = breakAmount,
                            onBreakAmountChange = { breakAmount = it },
                            prefix = prefix,
                            onPrefixChange = { prefix = it },
                            selectedStartDate = selected2DStartDate,
                            onSelectedStartDateChange = { selected2DStartDate = it },
                            selectedEndDate = selected2DEndDate,
                            onSelectedEndDateChange = { selected2DEndDate = it },
                            selectedStartTime = selected2DStartTime,
                            onSelectedStartTimeChange = { selected2DStartTime = it },
                            selectedEndTime = selected2DEndTime,
                            onSelectedEndTimeChange = { selected2DEndTime = it },
                            onShowStartDatePicker = { show2DStartDatePicker = true },
                            onShowEndDatePicker = { show2DEndDatePicker = true },
                            onShowStartTimePicker = { show2DStartTimePicker = true },
                            onShowEndTimePicker = { show2DEndTimePicker = true },
                            onTimeSlotDataChange = { slots, pairs ->
                                numberOfTimeSlots = slots
                                timeSlotPairs = pairs
                            }
                        )
                    } else {
                        ThreeDFormLayout(
                            termName = termName,
                            onTermNameChange = { termName = it },
                            selectedStartDate = selected3DStartDate,
                            onSelectedStartDateChange = { selected3DStartDate = it },
                            selectedEndDate = selected3DEndDate,
                            onSelectedEndDateChange = { selected3DEndDate = it },
                            breakAmount = breakAmount,
                            onBreakAmountChange = { breakAmount = it },
                            winNum = winNum,
                            onWinNumChange = { winNum = it },
                            unitPrice = unitPrice,
                            onUnitPriceChange = { unitPrice = it },
                            onShowStartDatePicker = { show3DStartDatePicker = true },
                            onShowEndDatePicker = { show3DEndDatePicker = true }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (term != null) {
                                // Edit mode submission
                                println("[DEBUG] TermFormModal Edit - startDate variable: '$startDate'")
                                println("[DEBUG] TermFormModal Edit - endDate variable: '$endDate'")
                                
                                val updatedTerm = term.copy(
                                    termName = termName,
                                    breakAmount = breakAmount.toIntOrNull() ?: 0,
                                    startDate = startDate,
                                    endDate = endDate,
                                    unitPrice = unitPrice.toIntOrNull() ?: 0,
                                    winNum = winNum
                                )
                                
                                println("[DEBUG] TermFormModal Edit - updatedTerm.startDate: '${updatedTerm.startDate}'")
                                println("[DEBUG] TermFormModal Edit - updatedTerm.endDate: '${updatedTerm.endDate}'")
                                
                                onSave(updatedTerm)
                            } else {
                                // Add mode submission
                                if (is2D) {
                                    // 2D Form submission
                                    println("[DEBUG] 2D Form validation - breakAmount: '$breakAmount', winNum: '$winNum', prefix: '$prefix', startDate: '$selected2DStartDate', endDate: '$selected2DEndDate'")
                                    
                                    if (breakAmount.isEmpty()  || selected2DStartDate.isEmpty() || selected2DEndDate.isEmpty()) {
                                        println("[DEBUG] 2D validation failed: missing required fields")
                                        return@Button
                                    }
                                    
                                    val formData = TermData(
                                        termId = 0,
                                        groupId = "", // Will be generated in onSave
                                        is2D = "1",
                                        termName = prefix,
                                        shortName = prefix,
                                        breakAmount = breakAmount.toIntOrNull() ?: 0,
                                        winNum = winNum,
                                        unitPrice = 1,
                                        isFinished = "0",
                                        startDate = selected2DStartDate,
                                        endDate = selected2DEndDate,
                                        businessId = "",
                                        termType = "normal",
                                        shareOption = null,
                                        accessUsers = "",
                                        last_updated_at = "",
                                        created_at = "",
                                        deleted = "0",
                                        deletedAt = null,
                                        note = null,
                                        tNumbers = null,
                                        active = "0"
                                    )
                                    
                                    if (onSave2D != null) {
                                        onSave2D(formData, timeSlotPairs)
                                    } else {
                                        onSave(formData)
                                    }
                                } else {
                                    // 3D Form submission
                                    println("[DEBUG] 3D Form validation - termName: '$termName', startDate: '$selected3DStartDate', endDate: '$selected3DEndDate', breakAmount: '$breakAmount', winNum: '$winNum'")
                                    
                                    if (termName.isEmpty()) {
                                        println("[DEBUG] 3D validation failed: termName is empty")
                                        return@Button
                                    }
                                    if (selected3DStartDate.isEmpty()) {
                                        println("[DEBUG] 3D validation failed: startDate is empty")
                                        return@Button
                                    }
                                    if (selected3DEndDate.isEmpty()) {
                                        println("[DEBUG] 3D validation failed: endDate is empty")
                                        return@Button
                                    }
                                    if (breakAmount.isEmpty()) {
                                        println("[DEBUG] 3D validation failed: breakAmount is empty")
                                        return@Button
                                    }
                                    // if (winNum.isEmpty()) {
                                    //     println("[DEBUG] 3D validation failed: winNum is empty")
                                    //     return@Button
                                    // }
                                    
                                    val termData = TermData(
                                        termId = 0,
                                        groupId = "", // Will be generated in onSave
                                        is2D = "0",
                                        termName = termName,
                                        shortName = termName,
                                        breakAmount = breakAmount.toIntOrNull() ?: 0,
                                        winNum = winNum,
                                        unitPrice = unitPrice.toIntOrNull() ?: 0,
                                        isFinished = "0",
                                        startDate = selected3DStartDate,
                                        endDate = selected3DEndDate,
                                        businessId = "",
                                        termType = "normal",
                                        shareOption = null,
                                        accessUsers = "",
                                        last_updated_at = "",
                                        created_at = "",
                                        deleted = "0",
                                        deletedAt = null,
                                        note = null,
                                        tNumbers = null,
                                        active = "0"
                                    )
                                    
                                    println("[DEBUG] Calling onSave for 3D form data: $termData")
                                    onSave(termData)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        )
                    ) {
                        Text(if (term != null) "Update" else "Submit")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
    
    // Date Pickers for 2D
    if (show2DStartDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                selected2DStartDate = date
                show2DStartDatePicker = false
            },
            onDismiss = { show2DStartDatePicker = false }
        )
    }
    
    if (show2DEndDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                selected2DEndDate = date
                show2DEndDatePicker = false
            },
            onDismiss = { show2DEndDatePicker = false }
        )
    }
    
    // Time Pickers for 2D
    if (show2DStartTimePicker) {
        TimePickerDialog(
            onTimeSelected = { time ->
                selected2DStartTime = time
                show2DStartTimePicker = false
            },
            onDismiss = { show2DStartTimePicker = false }
        )
    }
    
    if (show2DEndTimePicker) {
        TimePickerDialog(
            onTimeSelected = { time ->
                selected2DEndTime = time
                show2DEndTimePicker = false
            },
            onDismiss = { show2DEndTimePicker = false }
        )
    }
    
    // Date Time Pickers for 3D
    if (show3DStartDatePicker) {
        DateTimePickerDialog(
            onDateTimeSelected = { dateTime ->
                selected3DStartDate = dateTime
                show3DStartDatePicker = false
            },
            onDismiss = { show3DStartDatePicker = false }
        )
    }
    
    if (show3DEndDatePicker) {
        DateTimePickerDialog(
            onDateTimeSelected = { dateTime ->
                selected3DEndDate = dateTime
                show3DEndDatePicker = false
            },
            onDismiss = { show3DEndDatePicker = false }
        )
    }
    
    // Edit mode date time pickers
    if (showEditStartDatePicker) {
        DateTimePickerDialog(
            onDateTimeSelected = { dateTime ->
                startDate = convertDisplayToISOString(dateTime)
                showEditStartDatePicker = false
            },
            onDismiss = { showEditStartDatePicker = false }
        )
    }
    
    if (showEditEndDatePicker) {
        DateTimePickerDialog(
            onDateTimeSelected = { dateTime ->
                endDate = convertDisplayToISOString(dateTime)
                showEditEndDatePicker = false
            },
            onDismiss = { showEditEndDatePicker = false }
        )
    }
}