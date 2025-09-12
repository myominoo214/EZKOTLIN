package ui.screens

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.*

data class LotteryEntry(
    val num: String,
    val unit: Int,
    val summary: String,
    val groupId: String
)

class HelperFunctions {
    companion object {
        private var mmlist = mutableListOf<LotteryEntry>()
        private var isValidNum = false
        private var lottTypeIndex = 0
        private var isPP = false
        private var is2DRR = false
        private var isValidFormat = true
        private var invalidString = ""
        
        private val data: HelperData by lazy {
            loadHelperData()
        }
        
        private fun loadHelperData(): HelperData {
            val jsonString = File("src/main/kotlin/pages/helperData.json").readText()
            val jsonElement = Json.parseToJsonElement(jsonString).jsonObject
            
            return HelperData(
                sp = jsonElement["sp"]!!.jsonArray.map { it.jsonPrimitive.content },
                mp = jsonElement["mp"]!!.jsonArray.map { it.jsonPrimitive.content },
                ss = jsonElement["ss"]!!.jsonArray.map { it.jsonPrimitive.content },
                mm = jsonElement["mm"]!!.jsonArray.map { it.jsonPrimitive.content },
                sm = jsonElement["sm"]!!.jsonArray.map { it.jsonPrimitive.content },
                ms = jsonElement["ms"]!!.jsonArray.map { it.jsonPrimitive.content },
                nk = jsonElement["nk"]!!.jsonArray.map { it.jsonPrimitive.content },
                k = jsonElement["k"]!!.jsonArray.map { it.jsonPrimitive.content },
                w = jsonElement["w"]!!.jsonArray.map { it.jsonPrimitive.content },
                include = jsonElement["include"]!!.jsonArray.map { it.jsonPrimitive.content }
            )
        }
        
        private fun generateUUID(): String = UUID.randomUUID().toString()
        
        fun getFrontEven(num: String, unit: Int) {
            val groupId = generateUUID()
            for (item in data.ss) {
                if (item.startsWith(num.first().toString())) {
                    mmlist.add(LotteryEntry(item, unit, "Front Even", groupId))
                }
            }
        }
        
        fun getBackEven(num: String, unit: Int) {
            val groupId = generateUUID()
            for (item in data.ss) {
                if (item.endsWith(num.last().toString())) {
                    mmlist.add(LotteryEntry(item, unit, "Back Even", groupId))
                }
            }
        }
        
        fun getFrontOdd(num: String, unit: Int) {
            val groupId = generateUUID()
            for (item in data.mm) {
                if (item.startsWith(num.first().toString())) {
                    mmlist.add(LotteryEntry(item, unit, "Front Odd", groupId))
                }
            }
        }
        
        fun getBackOdd(num: String, unit: Int) {
            val groupId = generateUUID()
            for (item in data.mm) {
                if (item.endsWith(num.last().toString())) {
                    mmlist.add(LotteryEntry(item, unit, "Back Odd", groupId))
                }
            }
        }
        
        fun getFrontSeries2D(num: String, unit: Int) {
            val groupId = generateUUID()
            for (i in 0..99) {
                val formattedNum = i.toString().padStart(2, '0')
                if (formattedNum.startsWith(num.first().toString())) {
                    mmlist.add(LotteryEntry(formattedNum, unit, "Front Series 2D", groupId))
                }
            }
        }
        
        fun getBackSeries2D(num: String, unit: Int) {
            val groupId = generateUUID()
            for (i in 0..99) {
                val formattedNum = i.toString().padStart(2, '0')
                if (formattedNum.endsWith(num.last().toString())) {
                    mmlist.add(LotteryEntry(formattedNum, unit, "Back Series 2D", groupId))
                }
            }
        }
        
        fun getBreak2D(num: String, unit: Int) {
            val groupId = generateUUID()
            val firstDigit = num.first().toString()
            val lastDigit = num.last().toString()
            
            for (i in 0..99) {
                val formattedNum = i.toString().padStart(2, '0')
                if (formattedNum.contains(firstDigit) || formattedNum.contains(lastDigit)) {
                    mmlist.add(LotteryEntry(formattedNum, unit, "Break 2D", groupId))
                }
            }
        }
        
        fun getDoubleNum(num: String, unit: Int) {
            val groupId = generateUUID()
            for (i in 0..9) {
                val doubleNum = "$i$i"
                mmlist.add(LotteryEntry(doubleNum, unit, "Double Number", groupId))
            }
        }
        
        fun getPower(num: String, unit: Int) {
            val groupId = generateUUID()
            for (item in data.w) {
                mmlist.add(LotteryEntry(item, unit, "Power", groupId))
            }
        }
        
        fun getK(num: String, unit: Int) {
            val groupId = generateUUID()
            for (item in data.k) {
                mmlist.add(LotteryEntry(item, unit, "K", groupId))
            }
        }
        
        fun getSP(num: String, unit: Int) {
            val groupId = generateUUID()
            for (item in data.sp) {
                mmlist.add(LotteryEntry(item, unit, "SP", groupId))
            }
        }
        
        fun getMP(num: String, unit: Int) {
            val groupId = generateUUID()
            for (item in data.mp) {
                mmlist.add(LotteryEntry(item, unit, "MP", groupId))
            }
        }
        
        fun getSS(num: String, unit: Int) {
            val groupId = generateUUID()
            for (item in data.ss) {
                mmlist.add(LotteryEntry(item, unit, "SS", groupId))
            }
        }
        
        fun getMM(num: String, unit: Int) {
            val groupId = generateUUID()
            for (item in data.mm) {
                mmlist.add(LotteryEntry(item, unit, "MM", groupId))
            }
        }
        
        fun getSM(num: String, unit: Int) {
            val groupId = generateUUID()
            for (item in data.sm) {
                mmlist.add(LotteryEntry(item, unit, "SM", groupId))
            }
        }
        
        fun getMS(num: String, unit: Int) {
            val groupId = generateUUID()
            for (item in data.ms) {
                mmlist.add(LotteryEntry(item, unit, "MS", groupId))
            }
        }
        
        fun getNK(num: String, unit: Int) {
            val groupId = generateUUID()
            for (item in data.nk) {
                mmlist.add(LotteryEntry(item, unit, "NK", groupId))
            }
        }
        
        fun getInclude(num: String, unit: Int) {
            val groupId = generateUUID()
            for (i in 0..99) {
                val formattedNum = i.toString().padStart(2, '0')
                if (formattedNum.contains(num)) {
                    mmlist.add(LotteryEntry(formattedNum, unit, "Include", groupId))
                }
            }
        }
        
        fun getAP(num: String, unit: Int) {
            val groupId = generateUUID()
            val digits = num.toCharArray().map { it.toString().toInt() }
            val permutations = generatePermutations(digits)
            
            for (perm in permutations) {
                val permStr = perm.joinToString("") { it.toString().padStart(1, '0') }
                if (permStr.length == 2) {
                    mmlist.add(LotteryEntry(permStr.padStart(2, '0'), unit, "AP", groupId))
                }
            }
        }
        
        fun getAP_(num: String, unit: Int) {
            val groupId = generateUUID()
            val digits = num.toCharArray().map { it.toString().toInt() }
            val permutations = generatePermutations(digits)
            
            for (perm in permutations) {
                val permStr = perm.joinToString("") { it.toString().padStart(1, '0') }
                if (permStr.length == 2 && permStr != num) {
                    mmlist.add(LotteryEntry(permStr.padStart(2, '0'), unit, "AP-", groupId))
                }
            }
        }
        
        fun getR(num: String, unit: Int) {
            val groupId = generateUUID()
            val reversed = num.reversed()
            if (reversed != num) {
                mmlist.add(LotteryEntry(reversed, unit, "R", groupId))
            }
        }
        
        fun getBreak3D(num: String, unit: Int) {
            val groupId = generateUUID()
            val digits = num.toCharArray().map { it.toString() }
            
            for (i in 0..999) {
                val formattedNum = i.toString().padStart(3, '0')
                var hasDigit = false
                for (digit in digits) {
                    if (formattedNum.contains(digit)) {
                        hasDigit = true
                        break
                    }
                }
                if (hasDigit) {
                    mmlist.add(LotteryEntry(formattedNum, unit, "Break 3D", groupId))
                }
            }
        }
        
        fun getRR3D(num: String, unit: Int) {
            val groupId = generateUUID()
            val digits = num.toCharArray().map { it.toString().toInt() }
            val permutations = generatePermutations(digits)
            
            for (perm in permutations) {
                val permStr = perm.joinToString("") { it.toString().padStart(1, '0') }
                if (permStr.length == 3 && permStr != num) {
                    mmlist.add(LotteryEntry(permStr.padStart(3, '0'), unit, "RR 3D", groupId))
                }
            }
        }
        
        fun getR3D(num: String, unit: Int) {
            val groupId = generateUUID()
            val reversed = num.reversed()
            if (reversed != num) {
                mmlist.add(LotteryEntry(reversed, unit, "R 3D", groupId))
            }
        }
        
        fun getFrontSeries3D(num: String, unit: Int) {
            val groupId = generateUUID()
            for (i in 0..999) {
                val formattedNum = i.toString().padStart(3, '0')
                if (formattedNum.startsWith(num.first().toString())) {
                    mmlist.add(LotteryEntry(formattedNum, unit, "Front Series 3D", groupId))
                }
            }
        }
        
        fun getMidSeries(num: String, unit: Int) {
            val groupId = generateUUID()
            if (num.length >= 2) {
                val midDigit = num[1].toString()
                for (i in 0..999) {
                    val formattedNum = i.toString().padStart(3, '0')
                    if (formattedNum.length >= 2 && formattedNum[1].toString() == midDigit) {
                        mmlist.add(LotteryEntry(formattedNum, unit, "Mid Series", groupId))
                    }
                }
            }
        }
        
        fun getBackSeries3D(num: String, unit: Int) {
            val groupId = generateUUID()
            for (i in 0..999) {
                val formattedNum = i.toString().padStart(3, '0')
                if (formattedNum.endsWith(num.last().toString())) {
                    mmlist.add(LotteryEntry(formattedNum, unit, "Back Series 3D", groupId))
                }
            }
        }
        
        fun getTri(num: String, unit: Int) {
            val groupId = generateUUID()
            for (i in 0..9) {
                val triNum = "$i$i$i"
                mmlist.add(LotteryEntry(triNum, unit, "Tri", groupId))
            }
        }
        
        private fun generatePermutations(list: List<Int>): List<List<Int>> {
            if (list.size <= 1) return listOf(list)
            val result = mutableListOf<List<Int>>()
            for (i in list.indices) {
                val rest = list.toMutableList()
                val current = rest.removeAt(i)
                for (perm in generatePermutations(rest)) {
                    result.add(listOf(current) + perm)
                }
            }
            return result
        }
        
        fun checkTypeForCalculation(num: String, unit: Int, customFormat: String, lottType: String) {
            when (customFormat) {
                "" -> {
                    val groupId = generateUUID()
                    mmlist.add(LotteryEntry(num, unit, "Direct", groupId))
                }
                "R" -> getR(num, unit)
                "AP" -> getAP(num, unit)
                "AP-" -> getAP_(num, unit)
                "NK" -> getNK(num, unit)
                "SP" -> getSP(num, unit)
                "MP" -> getMP(num, unit)
                "SS" -> getSS(num, unit)
                "MM" -> getMM(num, unit)
                "SM" -> getSM(num, unit)
                "MS" -> {
                    if (lottType == "3D") getMidSeries(num, unit)
                    else getMS(num, unit)
                }
                "P", "W" -> getPower(num, unit)
                "K" -> getK(num, unit)
                "B" -> {
                    if (lottType == "2D") getBreak2D(num, unit)
                    else getBreak3D(num, unit)
                }
                "N*" -> {
                    if (lottType == "2D") getBackSeries2D(num, unit)
                    else getBackSeries3D(num, unit)
                }
                "*N" -> {
                    if (lottType == "2D") getFrontSeries2D(num, unit)
                    else getFrontSeries3D(num, unit)
                }
                "RR" -> {
                    if (lottType == "3D") getRR3D(num, unit)
                }
                "FS" -> {
                    if (lottType == "3D") getFrontSeries3D(num, unit)
                }
                "BS" -> {
                    if (lottType == "3D") getBackSeries3D(num, unit)
                }
                "T" -> {
                    if (lottType == "3D") getTri(num, unit)
                }
                "FE" -> getFrontEven(num, unit)
                "BE" -> getBackEven(num, unit)
                "FO" -> getFrontOdd(num, unit)
                "BO" -> getBackOdd(num, unit)
                "D" -> getDoubleNum(num, unit)
                "I" -> getInclude(num, unit)
            }
        }
        
        fun type3DSelectNew(format: String): Int {
            isValidNum = true
            return when (format) {
                "" -> 1
                "R" -> 2
                "RR" -> 3
                "AP" -> 4
                "AP-" -> 5
                "B" -> 6
                "FS" -> 7
                "MS" -> 8
                "BS" -> 9
                "T" -> 10
                else -> {
                    isValidNum = false
                    0
                }
            }
        }
        
        fun type2DSelectNew(format: String): Int {
            isValidNum = true
            return when (format) {
                "" -> 1
                "R" -> 2
                "AP" -> 3
                "AP-" -> 4
                "NK" -> 5
                "SP" -> 6
                "MP" -> 7
                "SS" -> 8
                "MM" -> 9
                "SM" -> 10
                "MS" -> 11
                "P", "W" -> 12
                "K" -> 13
                "B" -> 14
                "N*" -> 15
                "*N" -> 16
                "FE" -> 17
                "BE" -> 18
                "FO" -> 19
                "BO" -> 20
                "D", "DP", "D+" -> 21
                "I" -> 22
                else -> {
                    isValidNum = false
                    0
                }
            }
        }
        
        private fun escapeRegex(string: String): String {
            return string.replace(Regex("[.*+?^\${}()|\\[\\]\\\\]"), "\\\\$0")
        }
        
        private fun replaceAll(input: String, searchValue: String, replaceValue: String): String {
            return input.replace(searchValue, replaceValue)
        }
        
        fun getSMS(
            smsText: String,
            lottType: String,
            unitPrice: Int,
            numUnitPrice: Int,
            callback: (List<LotteryEntry>, String) -> Unit
        ) {
            try {
                mmlist.clear()
                isValidFormat = true
                invalidString = ""
                
                var processedText = smsText.uppercase()
                
                // Replace Myanmar characters with English equivalents
                processedText = processedText.replace("ေ", "")
                processedText = processedText.replace("ါ", "")
                processedText = processedText.replace("ာ", "")
                processedText = processedText.replace("ီ", "")
                processedText = processedText.replace("ု", "")
                processedText = processedText.replace("ံ", "")
                processedText = processedText.replace("့", "")
                processedText = processedText.replace("္", "")
                processedText = processedText.replace("်", "")
                
                // Replace Myanmar digits with English digits
                processedText = processedText.replace("၀", "0")
                processedText = processedText.replace("၁", "1")
                processedText = processedText.replace("၂", "2")
                processedText = processedText.replace("၃", "3")
                processedText = processedText.replace("၄", "4")
                processedText = processedText.replace("၅", "5")
                processedText = processedText.replace("၆", "6")
                processedText = processedText.replace("၇", "7")
                processedText = processedText.replace("၈", "8")
                processedText = processedText.replace("၉", "9")
                
                // Replace special characters and patterns
                processedText = processedText.replace("အပ", ".AP.")
                processedText = processedText.replace("နက", ".NK.")
                processedText = processedText.replace("ေရ", ".R.")
                processedText = processedText.replace("ပါဝါ", ".W.")
                processedText = processedText.replace("ပါဝ", ".P.")
                processedText = processedText.replace("ေက", ".K.")
                processedText = processedText.replace("ေဘ", ".B.")
                processedText = processedText.replace("စပ", ".SP.")
                processedText = processedText.replace("မပ", ".MP.")
                processedText = processedText.replace("စစ", ".SS.")
                processedText = processedText.replace("မမ", ".MM.")
                processedText = processedText.replace("စမ", ".SM.")
                processedText = processedText.replace("မစ", ".MS.")
                processedText = processedText.replace("နေ", ".N*.")
                processedText = processedText.replace("ေန", ".*N.")
                
                val lines = processedText.split("\n")
                
                for (currentLine in lines) {
                    if (currentLine.trim().isEmpty()) continue
                    
                    try {
                        var line = currentLine.trim()
                        var isValid = true
                        var numList = mutableListOf<String>()
                        var format = ""
                        var unit = "0"
                        var seriesFormat = ""
                        
                        // Handle special patterns
                        if (line.contains("*")) {
                            val dashOrDot = if (line.contains("-")) "-" else "."
                            val customFormatIndex = line.substring(0, line.indexOf(dashOrDot))
                            val customFormat = line.substring(0, line.indexOf(dashOrDot))
                            seriesFormat = customFormat
                            line = line.replace("*", "")
                            line = line.replace(".", ".$customFormat.")
                        }
                        
                        if (line.contains(".")) {
                            line = line.replace("/", ".R.")
                            line = line.replace("//", ".R.")
                        } else {
                            line = line.replace("/", ".R.")
                            line = line.replace("//", ".R.")
                        }
                        
                        val splitLine = line.split(".")
                        numList.clear()
                        format = ""
                        unit = "0"
                        
                        var currentIndex = 0
                        for (numOrUnitOrFormat in splitLine) {
                            if (numOrUnitOrFormat.isNotEmpty()) {
                                if (numOrUnitOrFormat.trim().all { it.isDigit() }) {
                                    // Number
                                    if (format.isEmpty()) {
                                        if (currentIndex == splitLine.size - 1) {
                                            unit = numOrUnitOrFormat
                                        } else {
                                            numList.add(numOrUnitOrFormat)
                                        }
                                    } else {
                                        // Unit
                                        unit = numOrUnitOrFormat
                                    }
                                } else {
                                    format = numOrUnitOrFormat
                                }
                                
                                if (unit != "0") {
                                    var custom = ""
                                    
                                    // Check invalid num and format
                                    if (lottType == "2D") {
                                        if (format == "P" || format == "+") {
                                            if (numList.isNotEmpty()) {
                                                custom = "D"
                                            }
                                        }
                                        lottTypeIndex = type2DSelectNew(custom + format)
                                    } else if (lottType == "3D") {
                                        lottTypeIndex = type3DSelectNew(format)
                                    }
                                    
                                    // Check valid num
                                    if (format.isEmpty()) {
                                        for (num in numList) {
                                            if (lottType == "2D") {
                                                if (num.length != 2) {
                                                    isValidNum = false
                                                    break
                                                }
                                            } else {
                                                if (num.length != 3) {
                                                    isValidNum = false
                                                    break
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (isValidNum) {
                                        // Check format
                                        when (custom + format) {
                                            "", "R" -> {
                                                // Check num
                                                for (vNum in numList) {
                                                    if (lottType == "2D") {
                                                        if (vNum.length != 2) {
                                                            numList.clear()
                                                            format = ""
                                                            unit = "0"
                                                            isValid = false
                                                            break
                                                        }
                                                    } else {
                                                        if (vNum.length != 3) {
                                                            numList.clear()
                                                            format = ""
                                                            unit = "0"
                                                            isValid = false
                                                            break
                                                        }
                                                    }
                                                }
                                                
                                                if (numList.isNotEmpty()) {
                                                    for (num in numList) {
                                                        var processedNum = num
                                                        if (lottType == "3D" && format.contains("*")) {
                                                            processedNum = seriesFormat
                                                        }
                                                        
                                                        // Change unit
                                                        var adjustedUnit = unit.toInt()
                                                        if (numUnitPrice > 0) {
                                                            if (adjustedUnit > 0) {
                                                                adjustedUnit = (adjustedUnit * numUnitPrice) / unitPrice
                                                            }
                                                        }
                                                        
                                                        checkTypeForCalculation(num, adjustedUnit, format, lottType)
                                                    }
                                                    numList.clear()
                                                    format = ""
                                                    unit = "0"
                                                    isValid = true
                                                }
                                            }
                                            "N*", "*N", "D+", "DP" -> {
                                                for (nn in numList) {
                                                    for (n in nn.toCharArray()) {
                                                        // Change unit
                                                        var adjustedUnit = unit.toInt()
                                                        if (numUnitPrice > 0) {
                                                            if (adjustedUnit > 0) {
                                                                adjustedUnit = (adjustedUnit * numUnitPrice) / unitPrice
                                                            }
                                                        }
                                                        
                                                        checkTypeForCalculation(n.toString(), adjustedUnit, format, lottType)
                                                    }
                                                }
                                                
                                                numList.clear()
                                                format = ""
                                                unit = "0"
                                                isValid = true
                                            }
                                            else -> {
                                                if (numList.isNotEmpty()) {
                                                    for (num in numList) {
                                                        var processedNum = num
                                                        if (lottType == "3D" && format.contains("*")) {
                                                            processedNum = seriesFormat
                                                        }
                                                        
                                                        // Change unit
                                                        var adjustedUnit = unit.toInt()
                                                        if (numUnitPrice > 0) {
                                                            if (adjustedUnit > 0) {
                                                                adjustedUnit = (adjustedUnit * numUnitPrice) / unitPrice
                                                            }
                                                        }
                                                        
                                                        checkTypeForCalculation(num, adjustedUnit, format, lottType)
                                                    }
                                                    numList.clear()
                                                    format = ""
                                                    unit = "0"
                                                    isValid = true
                                                } else {
                                                    // Change unit
                                                    var adjustedUnit = unit.toInt()
                                                    if (numUnitPrice > 0) {
                                                        if (adjustedUnit > 0) {
                                                            adjustedUnit = (adjustedUnit * numUnitPrice) / unitPrice
                                                        }
                                                    }
                                                    
                                                    checkTypeForCalculation("", adjustedUnit, format, lottType)
                                                    numList.clear()
                                                    format = ""
                                                    unit = "0"
                                                    isValid = true
                                                }
                                            }
                                        }
                                    } else {
                                        isValid = false
                                    }
                                }
                            }
                            currentIndex++
                        }
                        
                        // Check if line is invalid
                        if (!isValid) {
                            invalidString += "$currentLine\n"
                        }
                    } catch (e: Exception) {
                        println(e)
                        invalidString += "$currentLine\n"
                    }
                    
                    // Get error string
                    if (!isValidFormat) {
                        invalidString += "$currentLine\n"
                        isValidFormat = false
                    }
                }
                
                if (lottType == "2D" && mmlist.isNotEmpty()) {
                    if (mmlist[0].num.length != 2) {
                        invalidString = "အပါတ္စဥ္ မွားေနသည္။"
                        isValidFormat = false
                        callback(mmlist.toList(), invalidString)
                        return
                    }
                }
                
                if (lottType == "3D" && mmlist.isNotEmpty()) {
                    if (mmlist[0].num.length != 3) {
                        invalidString = "အပါတ္စဥ္ မွားေနသည္။"
                        isValidFormat = false
                        callback(mmlist.toList(), invalidString)
                        return
                    }
                }
                
                callback(mmlist.toList(), invalidString)
            } catch (e: Exception) {
                println(e)
                invalidString = e.toString()
                isValidFormat = false
            }
        }
    }
}