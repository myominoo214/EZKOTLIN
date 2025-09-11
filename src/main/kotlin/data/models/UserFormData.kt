package data.models

import kotlinx.serialization.Serializable
import data.models.BetType

// User types enum
enum class UserType {
    AGENT,
    USER,
    SUB_OWNER,
    EMPLOYEE
}

// Bet type enum
// BetType enum is already defined in CommonModels.kt

// Agent option for dropdown
@Serializable
data class AgentOption(
    val value: String,
    val label: String
)

// User form data class
@Serializable
data class UserFormData(
    val name: String = "",
    val password: String = "",
    val phoneNumber: String = "",
    val partner: Boolean = false,
    val inviteKey: String = "",
    val agentId: String? = null,
    val discount2D: String = "",
    val discount3D: String = "",
    val prize2D: String = "",
    val prize3D: String = "",
    val tPrize: String = "",
    val breakLimit2D: String = "",
    val breakLimit3D: String = "",
    val unitPrice: String = "",
    val hotBreak: String = "",
    val hotPercentage: String = "",
    val hotBreak3D: String = "",
    val hotPercentage3D: String = "",
    val betType: BetType = BetType.PERCENTAGE,
    val betType3D: BetType = BetType.PERCENTAGE,
    val access2D: Boolean = true,
    val access3D: Boolean = true,
    val host: Boolean = false,
    val breakAccess: Boolean = false,
    val hotAccess: Boolean = false
)

// Form validation state
data class FormValidationState(
    val nameError: String? = null,
    val passwordError: String? = null,
    val phoneNumberError: String? = null,
    val discount2DError: String? = null,
    val discount3DError: String? = null,
    val prize2DError: String? = null,
    val prize3DError: String? = null,
    val tPrizeError: String? = null,
    val breakLimit2DError: String? = null,
    val breakLimit3DError: String? = null,
    val unitPriceError: String? = null,
    val hotBreakError: String? = null,
    val hotPercentageError: String? = null,
    val hotBreak3DError: String? = null,
    val hotPercentage3DError: String? = null,
    val agentIdError: String? = null
) {
    val hasErrors: Boolean
        get() = listOf(
            nameError, passwordError, phoneNumberError, discount2DError,
            discount3DError, prize2DError, prize3DError, tPrizeError,
            breakLimit2DError, breakLimit3DError, unitPriceError,
            hotBreakError, hotPercentageError, hotBreak3DError,
            hotPercentage3DError, agentIdError
        ).any { it != null }
}

// API request models
@Serializable
data class CreateUserRequest(
    val name: String,
    val password: String,
    val phoneNumber: String,
    val partner: Boolean = false,
    val agentId: String? = null,
    val discount2D: Int? = null,
    val discount3D: Int? = null,
    val prize2D: Int? = null,
    val prize3D: Int? = null,
    val tPrize: Int? = null,
    val breakLimit2D: Int? = null,
    val breakLimit3D: Int? = null,
    val unitPrice: Int? = null,
    val hotBreak: Int? = null,
    val hotPercentage: Int? = null,
    val hotBreak3D: Int? = null,
    val hotPercentage3D: Int? = null,
    val betType: String? = null,
    val betType3D: String? = null,
    val userAccess: String? = null,
    val host: Boolean? = null,
    val breakAccess: Boolean? = null,
    val hotAccess: Boolean? = null
)

@Serializable
data class UpdateUserRequest(
    val id: String,
    val name: String,
    val password: String?,
    val phoneNumber: String,
    val partner: Boolean = false,
    val agentId: String? = null,
    val discount2D: Int? = null,
    val discount3D: Int? = null,
    val prize2D: Int? = null,
    val prize3D: Int? = null,
    val tPrize: Int? = null,
    val breakLimit2D: Int? = null,
    val breakLimit3D: Int? = null,
    val unitPrice: Int? = null,
    val hotBreak: Int? = null,
    val hotPercentage: Int? = null,
    val hotBreak3D: Int? = null,
    val hotPercentage3D: Int? = null,
    val betType: String? = null,
    val betType3D: String? = null,
    val userAccess: String? = null,
    val host: Boolean? = null,
    val breakAccess: Boolean? = null,
    val hotAccess: Boolean? = null
)

@Serializable
data class UserResponse(
    val success: Boolean,
    val message: String? = null,
    val data: UserFormUserData? = null
)

@Serializable
data class UserFormUserData(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val partner: Boolean = false,
    val inviteKey: String? = null,
    val agentId: String? = null,
    val discount2D: Int? = null,
    val discount3D: Int? = null,
    val prize2D: Int? = null,
    val prize3D: Int? = null,
    val tPrize: Int? = null,
    val breakLimit2D: Int? = null,
    val breakLimit3D: Int? = null,
    val unitPrice: Int? = null,
    val hotBreak: Int? = null,
    val hotPercentage: Int? = null,
    val hotBreak3D: Int? = null,
    val hotPercentage3D: Int? = null,
    val betType: String? = null,
    val betType3D: String? = null,
    val userAccess: String? = null,
    val host: Boolean? = null,
    val breakAccess: Boolean? = null,
    val hotAccess: Boolean? = null
)

@Serializable
data class AgentListResponse(
    val success: Boolean,
    val data: List<AgentData>? = null
)

@Serializable
data class AgentData(
    val id: String,
    val name: String
)