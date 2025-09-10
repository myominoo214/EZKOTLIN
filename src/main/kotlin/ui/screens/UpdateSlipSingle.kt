package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import core.config.CompactOutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSlipSingle(
    initialNumber: String = "",
    initialAmount: String = "",
    onUpdate: (String, String) -> Unit
) {
    var number by remember { mutableStateOf(initialNumber) }
    var amount by remember { mutableStateOf(initialAmount) }

    // Update state when initial values change
    LaunchedEffect(initialNumber, initialAmount) {
        number = initialNumber
        amount = initialAmount
    }

    val handleUpdate = {
        onUpdate(number, amount)
        number = ""
        amount = ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Update Slip",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Input fields and button in a row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number input field
            CompactOutlinedTextField(
                value = number,
                onValueChange = { number = it },
                label = { Text("Number") },
                placeholder = { Text("Number") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text
                )
            )

            // Amount input field
            CompactOutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                placeholder = { Text("Amount") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            // Update button
            Button(
                onClick = handleUpdate,
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Update",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}