package ui.screens

import data.models.UserOption
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import core.services.ApiService
import core.services.UserSession
import kotlinx.serialization.Serializable

// Data Models
@Serializable
data class BettedAndRemainder(
    val remainderAmount: Int? = null,
    val bettedAmount: Int? = null
)

@Serializable
data class BettedAndRemainderResponse(
    val code: String,
    val status: String,
    val message: String,
    val data: BettedAndRemainder
)

// Main BetAndRemining Composable
@Composable
fun BetAndRemining(
    number: String,
    user: UserOption?,
    termId: String?
) {
    var bettedAndRemainder by remember { mutableStateOf(BettedAndRemainder()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Fetch data when number changes
    LaunchedEffect(number, user, termId) {
        println("[DEBUG] BetAndRemining LaunchedEffect triggered - number: $number, user: ${user?.value}, termId: $termId")
        if (user != null && termId != null && number.isNotEmpty()) {
            println("[DEBUG] BetAndRemining starting API call")
            isLoading = true
            error = null
            
            coroutineScope.launch {
                try {
                    val apiService = ApiService()
                    val userSession = UserSession.getInstance()
                    val url = "${ApiService.BASE_URL}/v1/ledger/getBettedAndRemainderAmount?termId=$termId&userId=${user.value}&number=$number"
                    println("[DEBUG] BetAndRemining API URL: $url")
                    
                    val response = apiService.get<BettedAndRemainderResponse>(
                        url = url,
                        headers = userSession.getAuthHeaders()
                    )
                    
                    println("[DEBUG] BetAndRemining API response - success: ${response.success}, data: ${response.data}")
                    if (response.success && response.data != null) {
                        bettedAndRemainder = response.data.data
                        println("[DEBUG] BetAndRemining data set: $bettedAndRemainder")
                    } else {
                        error = response.message ?: "Failed to fetch data"
                        println("[DEBUG] BetAndRemining API error: $error")
                    }
                } catch (e: Exception) {
                    error = "Error: ${e.message}"
                    println("[DEBUG] BetAndRemining exception: ${e.message}")
                    e.printStackTrace()
                } finally {
                    isLoading = false
                    println("[DEBUG] BetAndRemining loading finished")
                }
            }
        } else {
            println("[DEBUG] BetAndRemining conditions not met - user: $user, termId: $termId, number: $number")
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Table
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 256.dp)
        ) {
            // Table with border
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE5E7EB))
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F6))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Number",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "ကျံ",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "ထိုးပြီးယူနစ်",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "ထိုးသား",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Content
                when {
                    isLoading -> {
                        println("[DEBUG] BetAndRemining UI: Showing loading state")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    error != null -> {
                        println("[DEBUG] BetAndRemining UI: Showing error state - $error")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = error!!,
                                color = Color.Red
                            )
                        }
                    }
                    
                    else -> {
                        println("[DEBUG] BetAndRemining UI: Showing data - remainderAmount: ${bettedAndRemainder.remainderAmount}, bettedAmount: ${bettedAndRemainder.bettedAmount}")
                        // Data row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE5E7EB))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = number,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = bettedAndRemainder.remainderAmount?.toInt()?.toString() ?: "-",
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = bettedAndRemainder.bettedAmount?.toInt()?.toString() ?: "-",
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = user?.label ?: "-",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}