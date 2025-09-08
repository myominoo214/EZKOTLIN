package core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import core.services.PrintItem
import core.services.TextAlignment
import core.services.PosPrinterService
import core.services.PaperSize

// Data classes for betting slip items
@Serializable
data class BettingSlipItem(
    val number: String,
    val amount: String,
    val summary: String? = null,
    val showSummary: String = "0"
)

data class PrintConfiguration(
    val list: List<BettingSlipItem>,
    val isTwoColumn: Boolean = false,
    val printerName: String = "XP-58",
    val showBusinessName: Boolean = false,
    val businessName: String = "",
    val showEmployeeName: Boolean = false,
    val employeeName: String = "",
    val showTermName: Boolean = false,
    val termName: String = "",
    val showPrintTime: Boolean = true,
    val footerText: String = "",
    val fontSize: Int = 12,
    val slipId: String,
    val printWidth: PaperSize = PaperSize.MM_58, // 58mm or 80mm
    val copy: Int? = null,
    val termId: String,
    val userId: String,
    val businessId: String,
    val showSummary: Boolean = false
)

// Paper size configurations are now handled by PosPrinterService.PaperSize enum

/**
 * Print betting slip function for Kotlin Compose Desktop
 * Supports both 80mm and 58mm paper sizes
 */
fun printBettingSlip(config: PrintConfiguration) {
    if (config.list.isEmpty()) {
        println("Error: No items to print!")
        return
    }
    
    val filteredList = if (config.showSummary) {
        config.list.filter { it.showSummary == "1" }
    } else {
        config.list
    }
    
    val rows = if (config.showSummary) {
        filteredList.map { "${it.summary}-${it.amount}" }
    } else {
        filteredList.map { "${it.number}-${it.amount}" }
    }
    
    println("ROWS => $rows")
    
    // Format content based on column layout
    val formattedItems = if (config.isTwoColumn) {
        val twoColumnItems = mutableListOf<String>()
        for (i in rows.indices step 2) {
            val row1 = rows.getOrNull(i) ?: ""
            val row2 = rows.getOrNull(i + 1) ?: ""
            twoColumnItems.add("$row1 | $row2")
        }
        twoColumnItems
    } else {
        rows
    }
    
    val totalAmount = config.list.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    
    println("employeeName: ${config.employeeName}")
    
    // Update print count via API (coroutine for async operation)
    CoroutineScope(Dispatchers.IO).launch {
        updatePrintCount(
            slipId = config.slipId,
            termId = config.termId,
            userId = config.userId,
            businessId = config.businessId
        )
    }
    
    // Send to printer using PosPrinterService
    val printSuccess = sendToPrinter(config, formattedItems, totalAmount)
    
    if (printSuccess) {
        println("Print job sent successfully")
    } else {
        println("Failed to send print job")
    }
}

/**
 * Update print count via API
 */
suspend fun updatePrintCount(
    slipId: String,
    termId: String,
    userId: String,
    businessId: String
) {
    try {
        // TODO: Implement actual API call using HTTP client
        // This would replace the axiosPrivate.put call from JavaScript
        println("Updating print count for slip: $slipId")
        
        // Example structure for API call:
        // val requestBody = mapOf(
        //     "slipId" to slipId,
        //     "termId" to termId,
        //     "userId" to userId,
        //     "businessId" to businessId
        // )
        // httpClient.put("/v1/slip/setPrintCount", requestBody)
        
    } catch (e: Exception) {
        println("Error updating print count: ${e.message}")
    }
}

/**
 * Send print data to printer using PosPrinterService
 */
fun sendToPrinter(config: PrintConfiguration, formattedItems: List<String>, totalAmount: Double): Boolean {
    return try {
        // Create receipt using PosPrinterService
        val businessName = if (config.showBusinessName) config.businessName else ""
        val footerText = config.footerText
        
        val receipt = PosPrinterService.createSalesReceipt(
            businessName = businessName,
            items = formattedItems,
            total = "Total = $totalAmount",
            footerText = footerText,
            paperWidthMm = config.printWidth.widthMm
        )
        
        // Add additional header information if needed
        val headerItems = mutableListOf<PrintItem>()
        
        if (config.showTermName && config.termName.isNotEmpty()) {
            headerItems.add(
                PrintItem(
                     text = config.termName,
                     alignment = TextAlignment.CENTER,
                    fontSize = config.fontSize,
                    isBold = true
                )
            )
        }
        
        if (config.showEmployeeName && config.employeeName.isNotEmpty()) {
            headerItems.add(
                PrintItem(
                     text = config.employeeName,
                     alignment = TextAlignment.CENTER,
                    fontSize = config.fontSize
                )
            )
        }
        
        // Slip ID
        val slipText = if (config.copy != null) {
            "Slip No = ${config.slipId} copy[${config.copy}]"
        } else {
            "Slip No = ${config.slipId}"
        }
        headerItems.add(
            PrintItem(
                 text = slipText,
                 alignment = TextAlignment.LEFT,
                fontSize = config.fontSize
            )
        )
        
        // Print time
        if (config.showPrintTime) {
            val currentTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yy/MM/dd hh:mm:ss a")
            headerItems.add(
                PrintItem(
                     text = currentTime.format(formatter),
                     alignment = TextAlignment.LEFT,
                    fontSize = config.fontSize
                )
            )
        }
        
        // Create final receipt with custom header
        val finalReceipt = receipt.copy(
            header = headerItems + receipt.header
        )
        
        // Print using PosPrinterService
        PosPrinterService.printReceipt(
            receipt = finalReceipt,
            printerName = config.printerName,
            paperWidthMm = config.printWidth.widthMm
        )
        
    } catch (e: Exception) {
        println("Error sending to printer: ${e.message}")
        e.printStackTrace()
        false
    }
}

/**
 * Example usage function
 */
fun exampleUsage() {
    val sampleItems = listOf(
        BettingSlipItem(number = "123", amount = "1000"),
        BettingSlipItem(number = "456", amount = "2000"),
        BettingSlipItem(number = "789", amount = "1500")
    )
    
    val config58mm = PrintConfiguration(
        list = sampleItems,
        isTwoColumn = false,
        printerName = "XP-58",
        showBusinessName = true,
        businessName = "Sample Business",
        showEmployeeName = true,
        employeeName = "John Doe",
        showTermName = true,
        termName = "Terminal 1",
        showPrintTime = true,
        footerText = "Thank you!",
        fontSize = 12,
        slipId = "SLIP001",
        printWidth = PaperSize.MM_58,
        termId = "TERM001",
        userId = "USER001",
        businessId = "BIZ001"
    )
    
    val config80mm = config58mm.copy(
        printWidth = PaperSize.MM_80,
        isTwoColumn = true
    )
    
    // Print on 58mm paper
    println("Printing on 58mm paper:")
    printBettingSlip(config58mm)
    
    // Print on 80mm paper
    println("\nPrinting on 80mm paper:")
    printBettingSlip(config80mm)
}