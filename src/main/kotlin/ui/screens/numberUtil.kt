package ui.screens

import kotlin.math.pow

data class NumberEntry(
    val number: String,
    val amount: Int,
    val summary: String,
    val showSummary: String,
    val groupId: String,
    val delete: Boolean = false
)

class NumberUtil {
    
    companion object {
        
        fun processPatternR(match: String, unitPrice: Int, uniqueID: String): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            val reversedMatch = match.reversed()
            
            entries.add(
                NumberEntry(
                    number = match,
                    amount = unitPrice,
                    summary = "${match}R",
                    showSummary = "1",
                    groupId = uniqueID
                )
            )
            
            if (reversedMatch != match) {
                entries.add(
                    NumberEntry(
                        number = reversedMatch,
                        amount = unitPrice,
                        summary = "${match}R",
                        showSummary = "0",
                        groupId = uniqueID
                    )
                )
            }
            
            return entries
        }
        
        fun processPatternRUnit(number: String, unitPrice: Int, uniqueID: String): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            val reversedNumber = number.reversed()
            
            // Calculate total price based on number of digits
            val totalPrice = when (number.length) {
                2 -> unitPrice * 2
                3 -> unitPrice * 6
                else -> unitPrice
            }
            
            // Generate all permutations
            val permutations = getPermutations(number).toSet().toList()
            
            permutations.forEachIndexed { index, perm ->
                entries.add(
                    NumberEntry(
                        number = perm,
                        amount = unitPrice,
                        summary = "${number}R",
                        showSummary = if (index == 0) "1" else "0",
                        groupId = uniqueID
                    )
                )
            }
            
            return entries
        }
        
        fun processPatternRUnit2(number: String, unitPrice: Int, uniqueID: String): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            val reversedNumber = number.reversed()
            
            entries.add(
                NumberEntry(
                    number = number,
                    amount = unitPrice,
                    summary = "${number}R",
                    showSummary = "1",
                    groupId = uniqueID
                )
            )
            
            if (reversedNumber != number) {
                entries.add(
                    NumberEntry(
                        number = reversedNumber,
                        amount = unitPrice,
                        summary = "${number}R",
                        showSummary = "0",
                        groupId = uniqueID
                    )
                )
            }
            
            return entries
        }
        
        fun processStarN(match: String, unitPrice: Int, uniqueID: String): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            
            for (i in 0..9) {
                val number = "$i$match"
                entries.add(
                    NumberEntry(
                        number = number,
                        amount = unitPrice,
                        summary = "*$match",
                        showSummary = if (entries.isEmpty()) "1" else "0",
                        groupId = uniqueID
                    )
                )
            }
            
            return entries
        }
        
        fun processNStar(match: String, unitPrice: Int, uniqueID: String): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            
            for (i in 0..9) {
                val number = "$match$i"
                entries.add(
                    NumberEntry(
                        number = number,
                        amount = unitPrice,
                        summary = "$match*",
                        showSummary = if (entries.isEmpty()) "1" else "0",
                        groupId = uniqueID
                    )
                )
            }
            
            return entries
        }
        
        fun processTRule(uniqueID: String, unitPrice: Int): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            val tNumbers = listOf("000", "111", "222", "333", "444", "555", "666", "777", "888", "999")
            
            tNumbers.forEachIndexed { index, number ->
                entries.add(
                    NumberEntry(
                        number = number,
                        amount = unitPrice,
                        summary = "T",
                        showSummary = if (index == 0) "1" else "0",
                        groupId = uniqueID
                    )
                )
            }
            
            return entries
        }
        
        fun processDoubleNStarRule(digit: String, uniqueID: String, unitPrice: Int): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            
            for (i in 0..9) {
                val number = digit + i.toString()
                entries.add(
                    NumberEntry(
                        number = number,
                        amount = unitPrice,
                        summary = "$digit*",
                        showSummary = if (entries.isEmpty()) "1" else "0",
                        groupId = uniqueID
                    )
                )
            }
            
            return entries
        }
        
        fun processStartDoubleNRule(digit: String, uniqueID: String, unitPrice: Int): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            
            for (i in 0..9) {
                val number = "${i}$digit"
                entries.add(
                    NumberEntry(
                        number = number,
                        amount = unitPrice,
                        summary = "*$digit",
                        showSummary = if (entries.isEmpty()) "1" else "0",
                        groupId = uniqueID
                    )
                )
            }
            
            return entries
        }
        
        fun processNStarNRule(digit: String, digit2: String, uniqueID: String, unitPrice: Int): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            
            for (i in 0..9) {
                val number = "$digit$i$digit2"
                entries.add(
                    NumberEntry(
                        number = number,
                        amount = unitPrice,
                        summary = "$digit*$digit2",
                        showSummary = if (entries.isEmpty()) "1" else "0",
                        groupId = uniqueID
                    )
                )
            }
            
            return entries
        }
        
        fun process3DRRule(digits: String, uniqueID: String, unitPrice: String): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            
            // Parse unitPrice to handle single value or range
            val minPrice: Int
            val maxPrice: Int
            val uniqueR: Boolean
            
            if (Regex("[Rr/]").containsMatchIn(unitPrice)) {
                val prices = unitPrice.split(Regex("[Rr/]")).map { it.toIntOrNull() ?: 0 }
                minPrice = prices.getOrNull(0) ?: 0
                maxPrice = prices.getOrNull(1) ?: minPrice
                uniqueR = true
            } else {
                minPrice = unitPrice.toIntOrNull() ?: 0
                maxPrice = minPrice
                uniqueR = false
            }
            
            // Generate unique permutations
            val permutations = getPermutations(digits).toSet().toList()
            
            // Create entries
            permutations.forEachIndexed { index, number ->
                entries.add(
                    NumberEntry(
                        number = number,
                        amount = if (index == 0) minPrice else maxPrice,
                        summary = if (uniqueR) number else "${digits}R",
                        showSummary = if (uniqueR) "1" else if (index == 0) "1" else "0",
                        groupId = uniqueID
                    )
                )
            }
            
            return entries
        }
        
        fun process3DRRule2(digits: String, uniqueID: String, unitPrice: Int): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            
            // Generate unique permutations
            val permutations = getPermutations(digits).toSet().toList()
            
            // Create entries
            permutations.forEach { number ->
                if (number != digits) {
                    entries.add(
                        NumberEntry(
                            number = number,
                            amount = unitPrice,
                            summary = number,
                            showSummary = "1",
                            groupId = uniqueID
                        )
                    )
                }
            }
            
            return entries
        }
        
        fun process3DRRRule(digits: String, uniqueID: String, unitPrice: Int): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            
            val permutations = getPermutations(digits).toSet().toList()
            
            permutations
                .filter { it != digits }
                .forEachIndexed { index, number ->
                    entries.add(
                        NumberEntry(
                            number = number,
                            amount = unitPrice,
                            summary = if (index == 0) "$digits ကျံR" else "",
                            showSummary = if (index == 0) "1" else "0",
                            groupId = uniqueID
                        )
                    )
                }
            
            return entries
        }
        
        fun processBreakRule(digit: String, uniqueID: String, unitPrice: Int): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            
            for (i in 0..999) {
                val number = i.toString().padStart(3, '0')
                val sum = number.map { it.toString().toInt() }.sum()
                
                if (sum % 10 == digit.toInt()) {
                    entries.add(
                        NumberEntry(
                            number = number,
                            amount = unitPrice,
                            summary = "$digit ဘရိတ်",
                            showSummary = if (entries.isEmpty()) "1" else "0",
                            groupId = uniqueID
                        )
                    )
                }
            }
            
            return entries
        }
        
        fun processNP(digit: String, uniqueID: String, unitPrice: Int): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            
            for (i in 0..999) {
                val num = i.toString().padStart(3, '0')
                if (num.contains(digit)) {
                    entries.add(
                        NumberEntry(
                            number = num,
                            amount = unitPrice,
                            summary = "${digit}ပါ",
                            showSummary = if (entries.isEmpty()) "1" else "0",
                            groupId = uniqueID
                        )
                    )
                }
            }
            
            return entries
        }
        
        fun generateCombinations(input: String): List<String> {
            val result = mutableListOf<String>()
            
            fun generate(curr: String, remainingLength: Int) {
                if (curr.length == 3) {
                    result.add(curr)
                    return
                }
                
                for (i in input.indices) {
                    generate(curr + input[i], remainingLength)
                }
            }
            
            generate("", 3)
            return result
        }
        
        fun processAPMinus(digit: String, uniqueID: String, unitPrice: Int): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            val arr = generateCombinations(digit)
            
            arr.forEach { value ->
                val chars = value.toCharArray()
                if (!(chars[0] == chars[1] && chars[1] == chars[2] && chars[0] == chars[2])) {
                    entries.add(
                        NumberEntry(
                            number = value,
                            amount = unitPrice,
                            summary = "${digit}အပြီး-",
                            showSummary = if (entries.isEmpty()) "1" else "0",
                            groupId = uniqueID
                        )
                    )
                }
            }
            
            // Remove duplicates
            val seen = mutableSetOf<String>()
            return entries.filter { entry ->
                if (seen.contains(entry.number)) {
                    false
                } else {
                    seen.add(entry.number)
                    true
                }
            }
        }
        
        fun processAP(digit: String, uniqueID: String, unitPrice: Int): List<NumberEntry> {
            val entries = mutableListOf<NumberEntry>()
            val arr = generateCombinations(digit)
            
            arr.forEach { value ->
                entries.add(
                    NumberEntry(
                        number = value,
                        amount = unitPrice,
                        summary = "${digit}အပြီး",
                        showSummary = if (entries.isEmpty()) "1" else "0",
                        groupId = uniqueID
                    )
                )
            }
            
            // Remove duplicates
            val seen = mutableSetOf<String>()
            return entries.filter { entry ->
                if (seen.contains(entry.number)) {
                    false
                } else {
                    seen.add(entry.number)
                    true
                }
            }
        }
        
        // Helper function to generate permutations
        private fun getPermutations(str: String): List<String> {
            if (str.length <= 1) return listOf(str)
            
            val permutations = mutableListOf<String>()
            val smallerPermutations = getPermutations(str.substring(1))
            
            for (perm in smallerPermutations) {
                for (i in 0..perm.length) {
                    val newPerm = perm.substring(0, i) + str[0] + perm.substring(i)
                    permutations.add(newPerm)
                }
            }
            
            return permutations
        }
        
        // Helper function to parse unit price
        private fun parseUnitPrice(unitPrice: String): Triple<Int, Int, Boolean> {
            return if (Regex("[Rr/]").containsMatchIn(unitPrice)) {
                val parts = unitPrice.split(Regex("[Rr/]")).map { it.toInt() }
                Triple(parts[0], parts[1], true)
            } else {
                val price = unitPrice.toInt()
                Triple(price, price, false)
            }
        }
    }
}