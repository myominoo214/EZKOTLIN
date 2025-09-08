package data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.roundToInt

// Custom serializer to handle string to int conversion
object StringToIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StringToInt", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: Decoder): Int {
        val string = decoder.decodeString()
        return try {
            string.toDoubleOrNull()?.roundToInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }
}

// Enhanced User data model for the users table
@Serializable
data class UserTableData(
    val id: String = "",
    val name: String = "",
    @SerialName("phoneNumber")
    val phone: String? = null,
    val userType: String = "", // agent, user, sub-owner, employee, owner
    @SerialName("discount2D")
    val discount2D: Int = 0,
    @SerialName("discount3D")
    val discount3D: Int = 0,
    @SerialName("prize2D")
    val prize2D: Int = 0,
    @SerialName("prize3D")
    val prize3D: Int = 0,
    @SerialName("hotBreak")
    @Serializable(with = StringToIntSerializer::class)
    val hotBreak2D: Int = 0,
    @SerialName("hotPercentage")
    @Serializable(with = StringToIntSerializer::class)
    val hotPercentage2D: Int = 0,
    @SerialName("hotBreak3D")
    @Serializable(with = StringToIntSerializer::class)
    val hotBreak3D: Int = 0,
    @SerialName("hotPercentage3D")
    @Serializable(with = StringToIntSerializer::class)
    val hotPercentage3D: Int = 0,
    val agentName: String = "",
    val agentId: String = "",
    val inviteKey: String? = null,
    val lastLoggedDate: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

// Pagination data for the users table
@Serializable
data class UsersPaginationData(
    val total: Int = 0,
    val current: Int = 1,
    val limit: Int = 100,
    val totalPages: Int = 0,
    val from: Int = 0,
    val to: Int = 0
)

// API response for users list
@Serializable
data class UsersListResponse(
    val code: String = "",
    val message: String = "",
    val data: UsersListData = UsersListData()
)

@Serializable
data class UsersListData(
    val by: List<UserTableData> = emptyList(),
    val pagination: UsersPaginationData = UsersPaginationData()
)

// UI state for the users page
data class UsersPageState(
    val users: List<UserTableData> = emptyList(),
    val pagination: UsersPaginationData = UsersPaginationData(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchText: String = "",
    val currentPage: Int = 1,
    val pageLimit: Int = 100,
    val showUserForm: Boolean = false,
    val selectedUser: UserTableData? = null
)

// Column definition for the data table
data class TableColumn(
    val header: String,
    val accessor: String,
    val width: Int = 100,
    val copyable: Boolean = false,
    val sortable: Boolean = false
)

// User type display mapping
fun displayUserType(userType: String, inviteKey: String?): String {
    return when (userType) {
        "agent" -> "ကိုယ်စားလှယ်"
        "employee" -> "ဝန်ထမ်း"
        "sub-owner" -> "ဒိုင်"
        "owner" -> "ပိုင်ရှင်"
        "user" -> if (inviteKey != null) "Partner" else "ထိုးသား"
        else -> "ထိုးသား"
    }
}

// Hot break/percentage display helper
fun displayHotBreakPercentage(hotBreak: Int, hotPercentage: Int): String {
    return if (hotBreak > 0) "${hotBreak}B" else "${hotPercentage}%"
}

// Agent display helper
fun displayAgentName(agentName: String, userType: String): String {
    return if (userType == "user") agentName else ""
}

// Table columns definition
val usersTableColumns = listOf(
    TableColumn("အမည်", "name", 120, true),
    TableColumn("ကော်(2D-3D)", "discount", 80),
    TableColumn("ဆ(2D-3D)", "prize", 80),
    TableColumn("ဖုန်း", "phone", 100, true),
    TableColumn("2D Hot(B-%)", "hotBreak2D", 80),
    TableColumn("3D Hot(B-%)", "hotBreak3D", 80),
    TableColumn("Agent", "agentName", 100),
    TableColumn("အမျိုးအစား", "userType", 100),
    TableColumn("Actions", "actions", 80)
)