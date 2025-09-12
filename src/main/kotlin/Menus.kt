import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Icon as Material3Icon
import androidx.compose.material3.IconButton as Material3IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.screens.*
import core.services.UserSession
import java.net.InetAddress
import java.net.NetworkInterface
import FontConfig

data class MenuItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

// Theme state management
@Composable
fun rememberThemeState(): MutableState<Boolean> {
    return remember { mutableStateOf(false) } // false = light, true = dark
}

@Composable
fun ThemeChanger(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Material3IconButton(
        onClick = { onThemeChange(!isDarkTheme) },
        modifier = modifier
    ) {
        Material3Icon(
            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
            contentDescription = if (isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme",
            tint = if (isDarkTheme) Color.Yellow else Color.Gray
        )
    }
}

fun getLocalIPAddress(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (!networkInterface.isLoopback && networkInterface.isUp) {
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.isSiteLocalAddress) {
                        return address.hostAddress ?: "Unknown"
                    }
                }
            }
        }
    } catch (e: Exception) {
        return "IP Not Found"
    }
    return "No Network"
}

@Composable
fun SalesPage(
    userSession: UserSession,
    onLogout: () -> Unit
) {
    var selectedItem by remember { mutableStateOf("အရောင်း") }
    val isDarkTheme = rememberThemeState()

    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar
        Sidebar(
            selectedItem = selectedItem,
            onItemSelected = { item: String -> selectedItem = item },
            userSession = userSession,
            onLogout = onLogout,
            isDarkTheme = isDarkTheme.value,
            onThemeChange = { isDarkTheme.value = it }
        )
        
        // Main Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
        ) {
            when (selectedItem) {
                "အရောင်း" -> SalePage()
                "အပတ်စဉ်" -> TermsContent()
                "ထိုးသား" -> UsersContent()
                "စလစ်" -> SlipsContent(onNavigateToSale = { selectedItem = "အရောင်း" })
                "စာရင်း" -> ReportContent()
                "အလျော်စာရင်း" -> WinNumContent()
                "လယ်ဂျာ" -> LedgerContent()
                "ပြင်ဖျက်မှတ်တမ်း" -> EditLogsContent()
                "ဖိုင်ရှင်း" -> CleanFileContent()
                "Setting" -> SettingContent(onLogout = onLogout)
                else -> SalePage()
            }
        }
    }
}

@Composable
fun Sidebar(
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    userSession: UserSession,
    onLogout: () -> Unit,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val menuItems = listOf(
        MenuItem("အရောင်း", Icons.Default.ShoppingCart) { onItemSelected("အရောင်း") },
        MenuItem("အပတ်စဉ်", Icons.Default.Schedule) { onItemSelected("အပတ်စဉ်") },
        MenuItem("ထိုးသား", Icons.Default.People) { onItemSelected("ထိုးသား") },
        MenuItem("စလစ်", Icons.Default.Receipt) { onItemSelected("စလစ်") },
        MenuItem("လယ်ဂျာ", Icons.Default.AccountBalance) { onItemSelected("လယ်ဂျာ") },
        MenuItem("စာရင်း", Icons.Default.Assessment) { onItemSelected("စာရင်း") },
        MenuItem("အလျော်စာရင်း", Icons.Default.EmojiEvents) { onItemSelected("အလျော်စာရင်း") },
        MenuItem("ပြင်ဖျက်မှတ်တမ်း", Icons.Default.History) { onItemSelected("ပြင်ဖျက်မှတ်တမ်း") },
        MenuItem("ဖိုင်ရှင်း", Icons.Default.CleaningServices) { onItemSelected("ဖိုင်ရှင်း") },
        MenuItem("Setting", Icons.Default.Settings) { onItemSelected("Setting") }
    )

    val backgroundColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val selectedColor = if (isDarkTheme) Color(0xFF4CAF50) else Color(0xFF2196F3)
    
    Column(
        modifier = Modifier
            .width(180.dp)
            .fillMaxHeight()
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        // IP Address and Theme Changer in same row
        val ipAddress = remember { getLocalIPAddress() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ipAddress,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontFamily = FontConfig.getFontFamily(ipAddress)
            )
            
            ThemeChanger(
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange
            )
        }
        
        // User Info
        userSession.sessionData?.let { session ->
            Column(
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "User: ${session.accountKey}",
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.7f),
                    fontFamily = FontConfig.getFontFamily(session.accountKey)
                )
                Text(
                    text = "Device: ${session.deviceName}",
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.5f),
                    fontFamily = FontConfig.getFontFamily(session.deviceName)
                )
            }
        }

        // Menu Items
        menuItems.forEach { item ->
            val isSelected = selectedItem == item.title
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { item.onClick() }
                    .background(
                        color = if (isSelected) selectedColor.copy(alpha = 0.1f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = if (isSelected) selectedColor else textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                Text(
                    text = item.title,
                    color = if (isSelected) selectedColor else textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    fontFamily = FontConfig.getFontFamily(item.title)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Logout Button
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isDarkTheme) Color(0xFFD32F2F) else Color(0xFFE53E3E),
                contentColor = Color.White
            )
        ) {
            Icon(
                 imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                 contentDescription = "Logout",
                 modifier = Modifier.size(16.dp)
             )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Logout",
                fontSize = 12.sp,
                fontFamily = FontConfig.getFontFamily("Logout")
            )
        }
    }
}