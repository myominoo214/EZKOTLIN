package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.services.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoPage() {
    val userSession = UserSession.getInstance()
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Software Information & Contact Us Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Software Information Section
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "YOTTA Lottery Management System",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Version 1.0.0",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Professional lottery management solution for businesses of all sizes. Manage 2D/3D lottery operations with advanced features and real-time analytics.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Contact Information Section
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Contact Us",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    ContactInfoItem(
                        label = "Email",
                        value = "support@yotta-lottery.com"
                    )
                    
                    ContactInfoItem(
                        label = "Phone",
                        value = "+1 (555) 123-4567"
                    )
                    
                    ContactInfoItem(
                        label = "Address",
                        value = "123 Lottery Street\nGaming City, GC 12345\nUnited States"
                    )
                    
                    ContactInfoItem(
                        label = "Business Hours",
                        value = "Monday - Friday: 9:00 AM - 6:00 PM\nSaturday: 10:00 AM - 4:00 PM\nSunday: Closed"
                    )
                }
            }
        }
        
        
        // Pricing Plans Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Free Plan Card
            PricingCard(
                title = "Free Plan",
                subtitle = "Mobile App",
                price = "Free",
                features = listOf(
                    "Mobile application access",
                    "Basic lottery features",
                    "Download from app store",
                    "Community support"
                ),
                buttonText = "Download App",
                backgroundColor = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            
            // Device Plan Card
            PricingCard(
                title = "Device Plan",
                subtitle = "For Small to Medium Businesses",
                price = "Starting from $30/year",
                features = listOf(
                    "1 device - 500,000 MMK ($30/year)",
                    "ထပ်တိုး 1လုံးအတွက် 350,000 MMK",
                    "2-3 devices ($50/year)",
                    "4-7 devices ($150/year)",
                    "8-15 devices ($250/year)",
                    "30 Devices - 2,500,000 MMK ($200/year)",
                    "30-100 Devices - 2,500,000 MMK ($300/year)",
                    "100-200 Devices - 2,500,000 MMK ($500/year)"
                ),
                buttonText = "Choose Device Plan",
                backgroundColor = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
            
            // Unlimited Plan Card
            PricingCard(
                title = "Unlimited Plan",
                subtitle = "For Enterprise",
                price = "2,500,000 MMK ($200/year)",
                features = listOf(
                    "Unlimited devices",
                    "Premium support",
                    "Advanced analytics",
                    "Custom integrations",
                    "Priority updates",
                    "Dedicated account manager"
                ),
                buttonText = "Choose Unlimited",
                backgroundColor = Color(0xFF9C27B0),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ContactInfoItem(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun PricingCard(
    title: String,
    subtitle: String,
    price: String,
    features: List<String>,
    buttonText: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Plan Title
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Plan Subtitle
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Price
            Text(
                text = price,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Features List
            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✓",
                        color = Color(0xFF4CAF50),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = feature,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Action Button
            Button(
                onClick = { /* Handle plan selection */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = buttonText,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}