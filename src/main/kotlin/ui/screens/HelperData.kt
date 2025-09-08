package ui.screens

/**
 * Helper data class containing predefined number arrays for lottery calculations
 */
data class HelperData(
    val sp: List<String> = listOf("00", "22", "44", "66", "88"),
    val mp: List<String> = listOf("11", "33", "55", "77", "99"),
    val ss: List<String> = listOf(
        "00", "02", "04", "06", "08", "20", "22", "24", "26", "28",
        "40", "42", "44", "46", "48", "60", "62", "64", "66", "68",
        "80", "82", "84", "86", "88"
    ),
    val mm: List<String> = listOf(
        "11", "13", "15", "17", "19", "31", "33", "35", "37", "39",
        "51", "53", "55", "57", "59", "71", "73", "75", "77", "79",
        "91", "93", "95", "97", "99"
    ),
    val sm: List<String> = listOf(
        "01", "03", "05", "07", "09", "21", "23", "25", "27", "29",
        "41", "43", "45", "47", "49", "61", "63", "65", "67", "69",
        "81", "83", "85", "87", "89"
    ),
    val ms: List<String> = listOf(
        "10", "12", "14", "16", "18", "30", "32", "34", "36", "38",
        "50", "52", "54", "56", "58", "70", "72", "74", "76", "78",
        "90", "92", "94", "96", "98"
    ),
    val nk: List<String> = listOf(
        "01", "09", "10", "12", "21", "23", "32", "34", "43", "45",
        "54", "56", "65", "67", "76", "78", "87", "89", "98", "90"
    ),
    val k: List<String> = listOf(
        "07", "18", "24", "35", "69", "70", "81", "42", "53", "96"
    ),
    val w: List<String> = listOf(
        "05", "16", "27", "38", "49", "50", "61", "72", "83", "94"
    ),
    val include: List<String> = listOf(
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
    )
) {
    companion object {
        /**
         * Default instance of HelperData with predefined values
         */
        val default = HelperData()
        
        /**
         * Get SP (Small Pair) numbers
         */
        fun getSP(): List<String> = default.sp
        
        /**
         * Get MP (Medium Pair) numbers
         */
        fun getMP(): List<String> = default.mp
        
        /**
         * Get SS (Small-Small) numbers
         */
        fun getSS(): List<String> = default.ss
        
        /**
         * Get MM (Medium-Medium) numbers
         */
        fun getMM(): List<String> = default.mm
        
        /**
         * Get SM (Small-Medium) numbers
         */
        fun getSM(): List<String> = default.sm
        
        /**
         * Get MS (Medium-Small) numbers
         */
        fun getMS(): List<String> = default.ms
        
        /**
         * Get NK numbers
         */
        fun getNK(): List<String> = default.nk
        
        /**
         * Get K numbers
         */
        fun getK(): List<String> = default.k
        
        /**
         * Get W (Win) numbers
         */
        fun getW(): List<String> = default.w
        
        /**
         * Get include numbers
         */
        fun getInclude(): List<String> = default.include
    }
}