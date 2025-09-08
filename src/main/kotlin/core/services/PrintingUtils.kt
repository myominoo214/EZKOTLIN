package core.services
/**
 * Utility functions for printing that integrate with UserSession settings
 */

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PrintingUtils {
    
    /**
     * Print receipt using settings from UserSession
     */
    suspend fun printReceiptWithSettings(
        receipt: PrintReceipt,
        userSession: UserSession = UserSession.getInstance()
    ): Boolean = withContext(Dispatchers.IO) {
        val settings = userSession.localSettingsData
        return@withContext PosPrinterService.printReceipt(
            receipt = receipt,
            printerName = settings.selectedPrinter,
            paperWidthMm = settings.printWidth
        )
    }
    
    /**
     * Print simple text using settings from UserSession
     */
    suspend fun printTextWithSettings(
        text: String,
        alignment: TextAlignment = TextAlignment.LEFT,
        fontSize: Int? = null,
        userSession: UserSession = UserSession.getInstance()
    ): Boolean = withContext(Dispatchers.IO) {
        val settings = userSession.localSettingsData
        val actualFontSize = fontSize ?: settings.fontSize.toIntOrNull() ?: 12
        
        return@withContext PosPrinterService.printText(
            text = text,
            printerName = settings.selectedPrinter,
            paperWidthMm = settings.printWidth,
            fontSize = actualFontSize,
            alignment = alignment
        )
    }
    
    /**
     * Create and print a sales receipt with business information
     */
    suspend fun printSalesReceiptWithSettings(
        items: List<String>,
        total: String = "",
        businessName: String = "",
        userSession: UserSession = UserSession.getInstance()
    ): Boolean = withContext(Dispatchers.IO) {
        val settings = userSession.localSettingsData
        
        val receipt = PosPrinterService.createSalesReceipt(
            businessName = businessName,
            items = items,
            total = total,
            footerText = settings.footerText,
            paperWidthMm = settings.printWidth
        )
        
        return@withContext PosPrinterService.printReceipt(
            receipt = receipt,
            printerName = settings.selectedPrinter,
            paperWidthMm = settings.printWidth
        )
    }
    
    /**
     * Create a betting slip receipt
     */
    suspend fun printBettingSlipWithSettings(
        slipNumber: String,
        betItems: List<String>,
        totalAmount: String,
        businessName: String = "",
        userSession: UserSession = UserSession.getInstance()
    ): Boolean = withContext(Dispatchers.IO) {
        val settings = userSession.localSettingsData
        
        val header = mutableListOf<PrintItem>()
        val body = mutableListOf<PrintItem>()
        val footer = mutableListOf<PrintItem>()
        
        // Business name
        if (businessName.isNotEmpty()) {
            header.add(
                PrintItem(
                    text = businessName,
                    alignment = TextAlignment.CENTER,
                    fontSize = 14,
                    isBold = true
                )
            )
        }
        
        // Slip number
        header.add(
            PrintItem(
                text = "Slip #: $slipNumber",
                alignment = TextAlignment.CENTER,
                fontSize = 12,
                isBold = true
            )
        )
        
        // Date and time
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        header.add(
            PrintItem(
                text = dateFormat.format(java.util.Date()),
                alignment = TextAlignment.CENTER,
                fontSize = 10
            )
        )
        
        // Separator
        val separatorLength = PosPrinterService.getMaxCharactersPerLine(settings.printWidth, 10)
        header.add(
            PrintItem(
                text = "=".repeat(separatorLength),
                alignment = TextAlignment.CENTER,
                fontSize = 10
            )
        )
        
        // Bet items
        betItems.forEach { item ->
            body.add(
                PrintItem(
                    text = item,
                    alignment = TextAlignment.LEFT,
                    fontSize = 10
                )
            )
        }
        
        // Total
        if (totalAmount.isNotEmpty()) {
            body.add(
                PrintItem(
                    text = "-".repeat(separatorLength),
                    alignment = TextAlignment.CENTER,
                    fontSize = 10
                )
            )
            body.add(
                PrintItem(
                    text = "Total: $totalAmount",
                    alignment = TextAlignment.RIGHT,
                    fontSize = 12,
                    isBold = true
                )
            )
        }
        
        // Footer
        if (settings.footerText.isNotEmpty()) {
            footer.add(PrintItem(text = "", fontSize = 8)) // Empty line
            footer.add(
                PrintItem(
                    text = settings.footerText,
                    alignment = TextAlignment.CENTER,
                    fontSize = 10
                )
            )
        }
        
        val receipt = PrintReceipt(header, body, footer)
        
        return@withContext PosPrinterService.printReceipt(
            receipt = receipt,
            printerName = settings.selectedPrinter,
            paperWidthMm = settings.printWidth
        )
    }
    
    /**
     * Test print to verify printer connectivity and settings
     */
    suspend fun testPrintWithSettings(
        userSession: UserSession = UserSession.getInstance()
    ): Boolean = withContext(Dispatchers.IO) {
        val settings = userSession.localSettingsData
        
        val testReceipt = PrintReceipt(
            header = listOf(
                PrintItem(
                    text = "PRINTER TEST",
                    alignment = TextAlignment.CENTER,
                    fontSize = 14,
                    isBold = true
                )
            ),
            body = listOf(
                PrintItem(
                    text = "Printer: ${settings.selectedPrinter}",
                    alignment = TextAlignment.LEFT,
                    fontSize = 10
                ),
                PrintItem(
                    text = "Paper Width: ${settings.printWidth}mm",
                    alignment = TextAlignment.LEFT,
                    fontSize = 10
                ),
                PrintItem(
                    text = "Font Size: ${settings.fontSize}",
                    alignment = TextAlignment.LEFT,
                    fontSize = 10
                ),
                PrintItem(
                    text = "Date: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(java.util.Date())}",
                    alignment = TextAlignment.LEFT,
                    fontSize = 10
                )
            ),
            footer = listOf(
                PrintItem(
                    text = settings.footerText.ifEmpty { "Test completed successfully" },
                    alignment = TextAlignment.CENTER,
                    fontSize = 10
                )
            )
        )
        
        return@withContext PosPrinterService.printReceipt(
            receipt = testReceipt,
            printerName = settings.selectedPrinter,
            paperWidthMm = settings.printWidth
        )
    }
    
    /**
     * Get formatted text that fits the current paper width setting
     */
    fun formatTextForPaper(
        text: String,
        userSession: UserSession = UserSession.getInstance()
    ): List<String> {
        val settings = userSession.localSettingsData
        val fontSize = settings.fontSize.toIntOrNull() ?: 12
        val maxChars = PosPrinterService.getMaxCharactersPerLine(settings.printWidth, fontSize)
        return PosPrinterService.wrapText(text, maxChars)
    }
    
    /**
     * Create a separator line for the current paper width
     */
    fun createSeparatorLine(
        character: String = "-",
        userSession: UserSession = UserSession.getInstance()
    ): String {
        val settings = userSession.localSettingsData
        val fontSize = settings.fontSize.toIntOrNull() ?: 12
        val maxChars = PosPrinterService.getMaxCharactersPerLine(settings.printWidth, fontSize)
        return character.repeat(maxChars)
    }
}