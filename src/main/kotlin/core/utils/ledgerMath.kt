package core.utils

/**
 * ledgerMath.kt
 *
 * Turn raw ledger rows → statement summary rows that look like:
 * [
 *   {
 *     SubTotalAmount:              "540.000000",
 *     TotalAmountWithDiscount:     "600.00",
 *     TotalAmountWithPrize:        "0.000000",
 *     TotalAmountWithoutDiscount:  "540.00",
 *     TotalTAmountWithoutPrize:    "0",
 *     TotalUnitWithDiscount:       "600.00",
 *     TotalWinAmountWithoutPrize:  "0",
 *     customer:                    "@BUY",
 *     discountAmount:              "60.00",
 *     termId:                      "38",
 *     termName:                    "13TH",
 *     userId:                      "28"
 *   },
 *   …
 * ]
 */

data class LedgerRow(
    val number: String? = null,
    val winNum: String? = null,
    val tNumbers: List<String>? = null,
    val amount: Int = 0,
    val tUnitPrice: Int? = null,
    val discount2D: Int? = null,
    val discount3D: Int? = null,
    val agentDisount2D: Int? = null,
    val agentDisount3D: Int? = null,
    val prize2D: Int = 0,
    val prize3D: Int = 0,
    val tPrize: Int? = null,
    val name: String? = null,
    val customer: String? = null,
    val userId: String = "",
    val termId: String = "",
    val termName: String = ""
)

data class StatementSummary(
    val SubTotalAmount: String,
    val TotalAmountWithDiscount: String,
    val TotalAmountWithPrize: String,
    val TotalAmountWithoutDiscount: String,
    val TotalTAmountWithoutPrize: String,
    val TotalUnitWithDiscount: String,
    val TotalWinAmountWithoutPrize: String,
    val customer: String,
    val discountAmount: String,
    val termId: String,
    val termName: String,
    val userId: String
)

private data class BucketGroup(
    val customer: String,
    val userId: String,
    val termId: String,
    val termName: String,
    var discountAmount: Int = 0,
    var totalAmountWithDiscount: Int = 0,
    var totalUnitWithDiscount: Int = 0,
    var totalAmountWithoutDiscount: Int = 0,
    var totalWinAmountWithoutPrize: Int = 0,
    var totalTAmountWithoutPrize: Int = 0,
    var totalAmountWithPrize: Int = 0,
    var subTotalAmount: Int = 0
)

/**
 * Helper function to remove start prefix from input string
 */
fun removeStartPrefix(input: String, prefix: String): String {
    if (input == prefix) return input
    return if (input.startsWith(prefix)) {
        input.substring(prefix.length)
    } else {
        input
    }
}

/**
 * Aggregate statement report from raw ledger rows
 *
 * @param rows - raw rows from the API
 * @param role - "owner" | "agent" | "user" | …
 * @param currentUser - logged-in userId (needed for agent logic)
 * @param name - name prefix to remove
 * @param userAgentDiscount - whether to use agent discount
 * @returns aggregated & formatted summary
 */
fun aggregateStatementReport(
    rows: List<LedgerRow>,
    role: String = "user",
    currentUser: String? = null,
    name: String = "",
    userAgentDiscount: Boolean = false
): List<StatementSummary> {
    
    // Helper: cut trailing " - X" suffixes and handle nulls
    fun normalize(value: String?): String {
        return (value ?: "∅").split(" - ")[0]
    }
    
    val bucket = mutableMapOf<String, BucketGroup>()
    
    rows.forEach { row ->
        // Single-row calculations (mirror SQL)
        val digits = (row.number ?: "").length
        val is2D = digits == 2
        val is3D = digits == 3
        val isHit = row.number == row.winNum
        val isTHit = is3D && row.tNumbers?.contains(row.number) == true
        
        val rate = when {
            is2D -> if (userAgentDiscount) (row.agentDisount2D?.toDouble() ?: 0.0) / 100 else (row.discount2D?.toDouble() ?: 0.0) / 100
            is3D -> if (userAgentDiscount) (row.agentDisount3D?.toDouble() ?: 0.0) / 100 else (row.discount3D?.toDouble() ?: 0.0) / 100
            else -> 0.0
        }
        
        val amtValue = row.amount * (row.tUnitPrice?.toDouble() ?: 1.0)
        val discAmount = amtValue * rate
        val winPrize = if (isHit) {
            (if (is2D) row.prize2D.toDouble() else row.prize3D.toDouble()) * row.amount * (row.tUnitPrice?.toDouble() ?: 1.0)
        } else 0.0
        val tPrize = if (isTHit) {
            (row.tPrize?.toDouble() ?: 0.0) * row.amount * (row.tUnitPrice?.toDouble() ?: 1.0)
        } else 0.0
        val totalPrize = winPrize + tPrize
        
        val subTotal = (
            row.amount
            - if (is2D) {
                row.amount * (if (userAgentDiscount) (row.agentDisount2D?.toDouble() ?: 0.0) else (row.discount2D?.toDouble() ?: 0.0)) / 100
            } else {
                row.amount * (if (userAgentDiscount) (row.agentDisount3D?.toDouble() ?: 0.0) else (row.discount3D?.toDouble() ?: 0.0)) / 100
            }
            - if (isHit) (if (is2D) row.prize2D.toDouble() else row.prize3D.toDouble()) * row.amount else 0.0
            - if (isTHit) (row.tPrize?.toDouble() ?: 0.0) * row.amount else 0.0
        ) * (row.tUnitPrice?.toDouble() ?: 1.0)
        
        // Choose name vs customer depending on role
        val displayName = if (role == "owner" || (role == "agent" && currentUser != row.userId)) {
            removeStartPrefix(normalize(row.name), name)
        } else {
            normalize(row.customer)
        }
        
        val key = "${displayName}|${row.termId}|${row.userId}"
        
        if (!bucket.containsKey(key)) {
            bucket[key] = BucketGroup(
                customer = displayName,
                userId = row.userId,
                termId = row.termId,
                termName = row.termName
            )
        }
        
        val g = bucket[key]!!
        g.discountAmount += discAmount.toInt()
        g.totalAmountWithDiscount += amtValue.toInt()
        g.totalUnitWithDiscount += row.amount
        g.totalAmountWithoutDiscount += (amtValue - discAmount).toInt()
        g.totalWinAmountWithoutPrize += if (isHit) row.amount else 0
        g.totalTAmountWithoutPrize += if (isTHit) row.amount else 0
        g.totalAmountWithPrize += totalPrize.toInt()
        g.subTotalAmount += subTotal.toInt()
    }
    
    // Final MySQL-style formatting & stable sort
    return bucket.values
        .map { g ->
            StatementSummary(
                SubTotalAmount = String.format("%.6f", g.subTotalAmount.toDouble()),
                TotalAmountWithDiscount = String.format("%.2f", g.totalAmountWithDiscount.toDouble()),
                TotalAmountWithPrize = String.format("%.6f", g.totalAmountWithPrize.toDouble()),
                TotalAmountWithoutDiscount = String.format("%.2f", g.totalAmountWithoutDiscount.toDouble()),
                TotalTAmountWithoutPrize = g.totalTAmountWithoutPrize.toString(),
                TotalUnitWithDiscount = String.format("%.2f", g.totalUnitWithDiscount.toDouble()),
                TotalWinAmountWithoutPrize = g.totalWinAmountWithoutPrize.toString(),
                customer = g.customer,
                discountAmount = String.format("%.2f", g.discountAmount.toDouble()),
                termId = g.termId,
                termName = g.termName,
                userId = g.userId
            )
        }
        .sortedBy { it.customer }
}