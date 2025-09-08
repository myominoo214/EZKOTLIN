package core.services
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.print.attribute.standard.PrinterState
import javax.print.attribute.standard.PrinterStateReason
import javax.print.attribute.standard.PrinterStateReasons
import kotlinx.serialization.Serializable

@Serializable
data class PrinterInfo(
    val name: String,
    val status: String,
    val isDefault: Boolean = false
)

object PrinterService {
    
    /**
     * Get all available printers on the system
     */
    fun getAvailablePrinters(): List<PrinterInfo> {
        val printers = mutableListOf<PrinterInfo>()
        
        try {
            // Get all print services
            val printServices = PrintServiceLookup.lookupPrintServices(null, null)
            val defaultPrintService = PrintServiceLookup.lookupDefaultPrintService()
            
            for (printService in printServices) {
                val printerName = printService.name
                val isDefault = printService == defaultPrintService
                val status = getPrinterStatus(printService)
                
                printers.add(
                    PrinterInfo(
                        name = printerName,
                        status = status,
                        isDefault = isDefault
                    )
                )
            }
            
            // Sort printers: default first, then alphabetically
            printers.sortWith(compareByDescending<PrinterInfo> { it.isDefault }.thenBy { it.name })
            
        } catch (e: Exception) {
            println("Error getting printers: ${e.message}")
            // Return a fallback list if system printer detection fails
            printers.add(
                PrinterInfo(
                    name = "Default Printer",
                    status = "Available",
                    isDefault = true
                )
            )
        }
        
        return printers
    }
    
    /**
     * Get the default printer
     */
    fun getDefaultPrinter(): PrinterInfo? {
        return try {
            val defaultPrintService = PrintServiceLookup.lookupDefaultPrintService()
            defaultPrintService?.let {
                PrinterInfo(
                    name = it.name,
                    status = getPrinterStatus(it),
                    isDefault = true
                )
            }
        } catch (e: Exception) {
            println("Error getting default printer: ${e.message}")
            null
        }
    }
    
    /**
     * Get printer status as a human-readable string
     */
    private fun getPrinterStatus(printService: PrintService): String {
        return try {
            val printerState = printService.getAttribute(PrinterState::class.java)
            val printerStateReasons = printService.getAttribute(PrinterStateReasons::class.java)
            
            when {
                printerStateReasons != null && printerStateReasons.isNotEmpty() -> {
                    val reasons = printerStateReasons.map { reason ->
                        val reasonName = reason.key.toString()
                        when {
                            reasonName.contains("MEDIA_JAM") -> "Paper Jam"
                            reasonName.contains("MEDIA_EMPTY") -> "Out of Paper"
                            reasonName.contains("TONER_EMPTY") -> "Out of Toner"
                            reasonName.contains("TONER_LOW") -> "Low Toner"
                            reasonName.contains("STOPPED") || reasonName.contains("OFFLINE") -> "Offline"
                            reasonName.contains("PAUSED") -> "Paused"
                            reasonName.contains("OTHER") -> "Error"
                            else -> reasonName.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                        }
                    }
                    reasons.joinToString(", ")
                }
                printerState != null -> {
                    when (printerState) {
                        PrinterState.IDLE -> "Ready"
                        PrinterState.PROCESSING -> "Printing"
                        PrinterState.STOPPED -> "Stopped"
                        else -> "Unknown"
                    }
                }
                else -> "Available"
            }
        } catch (e: Exception) {
            "Available"
        }
    }
    
    /**
     * Check if a printer with the given name exists
     */
    fun printerExists(printerName: String): Boolean {
        return try {
            val printServices = PrintServiceLookup.lookupPrintServices(null, null)
            printServices.any { it.name.equals(printerName, ignoreCase = true) }
        } catch (e: Exception) {
            false
        }
    }
}