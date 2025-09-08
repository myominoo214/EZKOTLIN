import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val onClick: () -> Unit
)

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

    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar
        Sidebar(
            selectedItem = selectedItem,
            onItemSelected = { item: String -> selectedItem = item },
            userSession = userSession,
            onLogout = onLogout
        )
        
        // Main Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
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
    onLogout: () -> Unit
) {
    val menuItems = listOf(
        MenuItem("အရောင်း") { onItemSelected("အရောင်း") },
        MenuItem("အပတ်စဉ်") { onItemSelected("အပတ်စဉ်") },
        MenuItem("ထိုးသား") { onItemSelected("ထိုးသား") },
        MenuItem("စလစ်") { onItemSelected("စလစ်") },
        MenuItem("လယ်ဂျာ") { onItemSelected("လယ်ဂျာ") },
        MenuItem("စာရင်း") { onItemSelected("စာရင်း") },
        MenuItem("အလျော်စာရင်း") { onItemSelected("အလျော်စာရင်း") },
        MenuItem("ပြင်ဖျက်မှတ်တမ်း") { onItemSelected("ပြင်ဖျက်မှတ်တမ်း") },
        MenuItem("ဖိုင်ရှင်း") { onItemSelected("ဖိုင်ရှင်း") },
        MenuItem("Setting") { onItemSelected("Setting") }
    )

    Column(
        modifier = Modifier
            .width(150.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colors.surface)
            .padding(8.dp)
    ) {
        // Header - User Info
        val ipAddress = remember { getLocalIPAddress() }
        Column(
            modifier = Modifier.padding(bottom = 1.dp)
        ) {
            Text(
                text = ipAddress,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface,
                fontFamily = FontConfig.getFontFamily(ipAddress)
            )
            
            userSession.sessionData?.let { session ->
                Text(
                    text = "User: ${session.accountKey}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    fontFamily = FontConfig.getFontFamily(session.accountKey)
                )
                Text(
                    text = "Device: ${session.deviceName}",
                    fontSize = 8.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    fontFamily = FontConfig.getFontFamily(session.deviceName)
                )
            }
        }

        // Menu Items
        menuItems.forEach { item ->
            val isSelected = selectedItem == item.title
            
            Text(
                text = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { item.onClick() }
                    .background(
                        color = if (isSelected) Color(0xFF2196F3).copy(alpha = 0.1f) else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                color = if (isSelected) Color(0xFF2196F3) else MaterialTheme.colors.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                fontFamily = FontConfig.getFontFamily(item.title)
            )
            
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
                backgroundColor = Color(0xFFE53E3E),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Logout",
                fontSize = 12.sp,
                fontFamily = FontConfig.getFontFamily("Logout")
            )
        }
    }
}