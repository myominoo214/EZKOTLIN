package ui.screens

import java.time.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val userId: String,
    val name: String,
    val userType: String,
    val phoneNumber: String? = null,
    val discount2D: Int = 0,
    val discount3D: Int = 0
)

@Serializable
data class ApiUserData(
    val userId: String,
    val discount2D: Double,
    val discount3D: Double
)

data class LedgerItem(
    val number: String,
    val totalAmount: Double,
    val customer: String = "",
    val amount: Double = 0.0,
    val slipId: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Serializable
data class LedgerApiData(
    val number: String? = null,
    val TotalAmount: Double? = null,
    val customer: String? = null,
    val amount: Double? = null,
    val slipId: String? = null
)

@Serializable
data class LedgerApiDataWrapper(
    val by: List<LedgerApiData>? = null
)

@Serializable
data class LedgerApiResponseData(
    val code: String,
    val status: String,
    val message: String,
    val data: LedgerApiDataWrapper? = null
)

@Serializable
data class PrizeApiResponseData(
    val code: String,
    val status: String,
    val message: String,
    val data: List<String>? = null
)

@Serializable
data class CustomerApiData(
    val customer: String? = null,
    val amount: Double? = null,
    val number: String? = null,
    val createdAt: String? = null,
    val slipId: String? = null
)

@Serializable
data class CustomerApiDataWrapper(
    val by: List<CustomerApiData>? = null
)

@Serializable
data class CustomerApiResponseData(
    val code: String,
    val status: String,
    val message: String,
    val data: CustomerApiDataWrapper? = null
)

// UserOption moved to data.models.CommonModels

data class LedgerState(
    val breakAmount: Int = 0,
    val tempBreakAmount: String = "0",
    val showSummary: Boolean = true,
    val refreshLedger: Boolean = false,
    val ledgerData: List<LedgerItem> = emptyList(),
    val ledgerRowDetail: List<LedgerItem> = emptyList(),
    val ledgerRow: List<LedgerItem> = emptyList(),
    val extraData: List<LedgerItem> = emptyList(),
    val unitData: List<LedgerItem> = emptyList(),
    val selectedUser: String = "",
    val prizes: List<String> = emptyList(),
    val selectedPrize: String = "",
    val copiedUnit: Boolean = false,
    val copiedExtra: Boolean = false,
    val temporaryBreakAmountCheck: Boolean = false,
    val loading: Boolean = false,
    val showLedgerDetail: Boolean = false,
    val showViewModal: Boolean = false,
    val showOptionsModal: Boolean = false,
    val is2D: Boolean = true
)

data class FormatData(
    val amount: String,
    val delete: Boolean = false,
    val groupId: Int,
    val number: String,
    val showSummary: String = "1",
    val summary: String
)