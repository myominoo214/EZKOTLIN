package core.services
import kotlinx.serialization.Serializable
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.print.*
import java.text.SimpleDateFormat
import java.util.*
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.*

@Serializable
data class PrintItem(
    val text: String,
    val alignment: TextAlignment = TextAlignment.LEFT,
    val fontSize: Int = 12,
    val isBold: Boolean = false,
    val isUnderline: Boolean = false
)

@Serializable
data class PrintReceipt(
    val header: List<PrintItem> = emptyList(),
    val body: List<PrintItem> = emptyList(),
    val footer: List<PrintItem> = emptyList()
)

enum class TextAlignment {
    LEFT, CENTER, RIGHT
}

enum class PaperSize(val widthMm: Int, val charactersPerLine58mm: Int, val charactersPerLine80mm: Int) {
    MM_58(58, 32, 48),
    MM_80(80, 48, 64)
}

object PosPrinterService {
    
    private const val POINTS_PER_MM = 2.83465f
    
    /**
     * Print receipt using the selected printer and paper size from settings
     */
    fun printReceipt(
        receipt: PrintReceipt,
        printerName: String = "Default Printer",
        paperWidthMm: Int = 58
    ): Boolean {
        return try {
            val printService = findPrintService(printerName)
            if (printService == null) {
                println("Printer '$printerName' not found")
                return false
            }
            
            val paperSize = if (paperWidthMm == 80) PaperSize.MM_80 else PaperSize.MM_58
            val printJob = PrinterJob.getPrinterJob()
            printJob.printService = printService
            
            printJob.setPrintable(ReceiptPrintable(receipt, paperSize))
            
            val attributes = HashPrintRequestAttributeSet()
            attributes.add(OrientationRequested.PORTRAIT)
            
            printJob.print(attributes)
            true
        } catch (e: Exception) {
            println("Error printing receipt: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Print simple text with automatic formatting
     */
    fun printText(
        text: String,
        printerName: String = "Default Printer",
        paperWidthMm: Int = 58,
        fontSize: Int = 12,
        alignment: TextAlignment = TextAlignment.LEFT
    ): Boolean {
        val receipt = PrintReceipt(
            body = listOf(
                PrintItem(
                    text = text,
                    alignment = alignment,
                    fontSize = fontSize
                )
            )
        )
        return printReceipt(receipt, printerName, paperWidthMm)
    }
    
    /**
     * Create a formatted receipt for sales/betting slips
     */
    fun createSalesReceipt(
        businessName: String = "",
        items: List<String>,
        total: String = "",
        footerText: String = "",
        paperWidthMm: Int = 58
    ): PrintReceipt {
        val header = mutableListOf<PrintItem>()
        val body = mutableListOf<PrintItem>()
        val footer = mutableListOf<PrintItem>()
        
        // Header
        if (businessName.isNotEmpty()) {
            header.add(
                PrintItem(
                    text = businessName,
                    alignment = TextAlignment.CENTER,
                    fontSize = 14,
                    isBold = true
                )
            )
            header.add(PrintItem(text = "", fontSize = 8)) // Empty line
        }
        
        // Date and time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        header.add(
            PrintItem(
                text = dateFormat.format(Date()),
                alignment = TextAlignment.CENTER,
                fontSize = 10
            )
        )
        
        // Separator line
        val separatorLength = if (paperWidthMm == 80) 48 else 32
        header.add(
            PrintItem(
                text = "-".repeat(separatorLength),
                alignment = TextAlignment.CENTER,
                fontSize = 10
            )
        )
        
        // Body - Items
        items.forEach { item ->
            body.add(
                PrintItem(
                    text = item,
                    alignment = TextAlignment.LEFT,
                    fontSize = 10
                )
            )
        }
        
        // Total
        if (total.isNotEmpty()) {
            body.add(
                PrintItem(
                    text = "-".repeat(separatorLength),
                    alignment = TextAlignment.CENTER,
                    fontSize = 10
                )
            )
            body.add(
                PrintItem(
                    text = total,
                    alignment = TextAlignment.RIGHT,
                    fontSize = 12,
                    isBold = true
                )
            )
        }
        
        // Footer
        if (footerText.isNotEmpty()) {
            footer.add(PrintItem(text = "", fontSize = 8)) // Empty line
            footer.add(
                PrintItem(
                    text = footerText,
                    alignment = TextAlignment.CENTER,
                    fontSize = 10
                )
            )
        }
        
        return PrintReceipt(header, body, footer)
    }
    
    /**
     * Find print service by name
     */
    private fun findPrintService(printerName: String): PrintService? {
        return try {
            val printServices = PrintServiceLookup.lookupPrintServices(null, null)
            
            // First try exact match
            var service = printServices.find { it.name.equals(printerName, ignoreCase = true) }
            
            // If not found and looking for "Default Printer", get system default
            if (service == null && printerName.equals("Default Printer", ignoreCase = true)) {
                service = PrintServiceLookup.lookupDefaultPrintService()
            }
            
            service
        } catch (e: Exception) {
            println("Error finding print service: ${e.message}")
            null
        }
    }
    
    /**
     * Get maximum characters per line for given paper width
     */
    fun getMaxCharactersPerLine(paperWidthMm: Int, fontSize: Int = 12): Int {
        return when (paperWidthMm) {
            80 -> when {
                fontSize <= 10 -> 64
                fontSize <= 12 -> 48
                else -> 42
            }
            else -> when { // 58mm
                fontSize <= 10 -> 48
                fontSize <= 12 -> 32
                else -> 28
            }
        }
    }
    
    /**
     * Word wrap text to fit paper width
     */
    fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        if (text.length <= maxCharsPerLine) return listOf(text)
        
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = ""
        
        for (word in words) {
            if ((currentLine + word).length <= maxCharsPerLine) {
                currentLine += if (currentLine.isEmpty()) word else " $word"
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = word
                } else {
                    // Word is longer than max chars, split it
                    lines.add(word.substring(0, maxCharsPerLine))
                    currentLine = word.substring(maxCharsPerLine)
                }
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        return lines
    }
}

/**
 * Printable implementation for receipts
 */
private class ReceiptPrintable(
    private val receipt: PrintReceipt,
    private val paperSize: PaperSize
) : Printable {
    
    override fun print(graphics: Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
        if (pageIndex > 0) return Printable.NO_SUCH_PAGE
        
        val g2d = graphics as Graphics2D
        g2d.translate(pageFormat.imageableX, pageFormat.imageableY)
        
        var yPosition = 20f
        val pageWidth = pageFormat.imageableWidth.toFloat()
        val maxCharsPerLine = PosPrinterService.getMaxCharactersPerLine(paperSize.widthMm)
        
        // Print header
        yPosition = printSection(g2d, receipt.header, yPosition, pageWidth, maxCharsPerLine)
        
        // Print body
        yPosition = printSection(g2d, receipt.body, yPosition, pageWidth, maxCharsPerLine)
        
        // Print footer
        printSection(g2d, receipt.footer, yPosition, pageWidth, maxCharsPerLine)
        
        return Printable.PAGE_EXISTS
    }
    
    private fun printSection(
        g2d: Graphics2D,
        items: List<PrintItem>,
        startY: Float,
        pageWidth: Float,
        maxCharsPerLine: Int
    ): Float {
        var yPosition = startY
        
        for (item in items) {
            val font = Font(
                Font.MONOSPACED,
                if (item.isBold) Font.BOLD else Font.PLAIN,
                item.fontSize
            )
            g2d.font = font
            
            val fontMetrics = g2d.fontMetrics
            val lineHeight = fontMetrics.height.toFloat()
            
            // Wrap text if necessary
            val lines = PosPrinterService.wrapText(item.text, maxCharsPerLine)
            
            for (line in lines) {
                val textWidth = fontMetrics.stringWidth(line).toFloat()
                
                val xPosition = when (item.alignment) {
                    TextAlignment.LEFT -> 0f
                    TextAlignment.CENTER -> (pageWidth - textWidth) / 2
                    TextAlignment.RIGHT -> pageWidth - textWidth
                }
                
                g2d.drawString(line, xPosition, yPosition)
                
                if (item.isUnderline) {
                    g2d.drawLine(
                        xPosition.toInt(),
                        (yPosition + 2).toInt(),
                        (xPosition + textWidth).toInt(),
                        (yPosition + 2).toInt()
                    )
                }
                
                yPosition += lineHeight
            }
        }
        
        return yPosition + 10f // Add some spacing after section
    }
}