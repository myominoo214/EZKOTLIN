package data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Custom serializer for Boolean fields that come as "0" or "1" strings
object BooleanAsStringSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BooleanAsString", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeString(if (value) "1" else "0")
    }
    
    override fun deserialize(decoder: Decoder): Boolean {
        return when (val string = decoder.decodeString()) {
            "1", "true" -> true
            "0", "false" -> false
            else -> throw IllegalArgumentException("Unknown boolean value: $string")
        }
    }
}

// Term related models
@Serializable
data class TermOption(
    val termId: Int,
    val termName: String,
    val shortName: String,
    val groupId: String,
    val startDate: String,
    val endDate: String,
    val isFinished: String,
    val termType: String,
    val winNum: String? = null,
    @Serializable(with = BooleanAsStringSerializer::class)
    val is2D: Boolean = true,
    val unitPrice: Int = 1,
    val breakAmount: Int = 0
)

@Serializable
data class GroupedTermOption(
    val groupId: String,
    val groupLabel: String,
    val terms: List<TermOption>
)

@Serializable
data class TermData(
    val termId: Int,
    val termName: String,
    val shortName: String,
    val groupId: String,
    val startDate: String,
    val endDate: String,
    val isFinished: String,
    val termType: String,
    val winNum: String? = null,
    @Serializable(with = BooleanAsStringSerializer::class)
    val is2D: Boolean = true,
    val unitPrice: Int = 1,
    val breakAmount: Int = 0
)

@Serializable
data class TermsApiResponseData(
    val by: List<TermData>,
    val pagination: PaginationData
)

@Serializable
data class TermsApiResponse(
    val code: String,
    val status: String,
    val message: String,
    val data: TermsApiResponseData
)

// User related models
@Serializable
data class UserOption(
    val value: String,
    val label: String,
    val userType: String = ""
)

@Serializable
data class UserData(
    val userId: Int? = null,
    val userName: String? = null,
    val userType: String? = null,
    val isActive: Boolean = true,
    // Alternative field names that might be in the API response
    val id: Int? = null,
    val name: String? = null,
    val type: String? = null,
    val email: String? = null,
    val username: String? = null
)

@Serializable
data class UsersApiResponseData(
    val by: List<UserData>,
    val pagination: PaginationData? = null
)

@Serializable
data class UsersApiResponse(
    val code: String,
    val status: String,
    val message: String,
    val data: UsersApiResponseData
)

// Common models
@Serializable
data class PaginationData(
    val page: Int = 1,
    val limit: Int = 10,
    val total: Int = 0
)

@Serializable
data class TimeSlot(
    val startTime: String,
    val endTime: String
)

// Enums
enum class SelectionMode {
    SINGLE,
    MULTI
}

enum class BetType {
    PERCENTAGE,
    AMOUNT
}

// API Response models
@Serializable
data class ApiResponse<T>(
    val code: String,
    val status: String,
    val message: String,
    val data: T
)

@Serializable
data class LedgerRow(
    val id: String,
    val date: String,
    val description: String,
    val amount: Int,
    val balance: Int
)

@Serializable
data class StatementSummary(
    val totalIncome: Int,
    val totalExpense: Int,
    val netAmount: Int,
    val period: String
)

@Serializable
data class LotteryEntry(
    val id: String,
    val numbers: List<Int>,
    val date: String,
    val amount: Int
)

@Serializable
data class PrinterInfo(
    val name: String,
    val status: String,
    val isDefault: Boolean
)