package core.services

import core.services.ApiService
import data.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class UserApiService(private val httpClient: HttpClient) {
    
    private val baseUrl = ApiService.BASE_URL // Use the same base URL as main ApiService
    
    suspend fun createUser(
        userData: UserFormData,
        userType: UserType
    ): Result<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val endpoint = when (userType) {
                UserType.AGENT -> "/v1/account/createPartner"
                UserType.USER -> "/v1/account/createUser"
                UserType.SUB_OWNER -> "/v1/account/createSubOwner"
                UserType.EMPLOYEE -> "/v1/account/createEmployee"
            }
            
            val request = CreateUserRequest(
                name = userData.name,
                password = userData.password,
                phoneNumber = userData.phoneNumber,
                partner = userData.partner,
                agentId = userData.agentId,
                discount2D = userData.discount2D.toIntOrNull(),
                discount3D = userData.discount3D.toIntOrNull(),
                prize2D = userData.prize2D.toIntOrNull(),
                prize3D = userData.prize3D.toIntOrNull(),
                tPrize = userData.tPrize.toIntOrNull(),
                breakLimit2D = userData.breakLimit2D.toDoubleOrNull(),
                breakLimit3D = userData.breakLimit3D.toDoubleOrNull(),
                unitPrice = userData.unitPrice.toDoubleOrNull(),
                hotBreak = if (userData.betType == BetType.AMOUNT) userData.hotBreak.toIntOrNull() else null,
                hotPercentage = if (userData.betType == BetType.PERCENTAGE) userData.hotPercentage.toIntOrNull() else null,
                hotBreak3D = if (userData.betType3D == BetType.AMOUNT) userData.hotBreak3D.toIntOrNull() else null,
                hotPercentage3D = if (userData.betType3D == BetType.PERCENTAGE) userData.hotPercentage3D.toIntOrNull() else null,
                betType = if (userData.betType == BetType.AMOUNT) "amount" else "percentage",
                betType3D = if (userData.betType3D == BetType.AMOUNT) "amount" else "percentage",
                userAccess = buildString {
                    if (userData.access2D) append("2D")
                    if (userData.access3D) {
                        if (isNotEmpty()) append(",")
                        append("3D")
                    }
                },
                host = userData.host,
                breakAccess = userData.breakAccess,
                hotAccess = userData.hotAccess
            )
            
            val response: UserResponse = httpClient.post("$baseUrl$endpoint") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUser(
        userId: String,
        userData: UserFormData
    ): Result<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateUserRequest(
                id = userId,
                name = userData.name,
                password = if (userData.password.isNotBlank()) userData.password else null,
                phoneNumber = userData.phoneNumber,
                partner = userData.partner,
                agentId = userData.agentId,
                discount2D = userData.discount2D.toIntOrNull(),
                discount3D = userData.discount3D.toIntOrNull(),
                prize2D = userData.prize2D.toIntOrNull(),
                prize3D = userData.prize3D.toIntOrNull(),
                tPrize = userData.tPrize.toIntOrNull(),
                breakLimit2D = userData.breakLimit2D.toDoubleOrNull(),
                breakLimit3D = userData.breakLimit3D.toDoubleOrNull(),
                unitPrice = userData.unitPrice.toDoubleOrNull(),
                hotBreak = if (userData.betType == BetType.AMOUNT) userData.hotBreak.toIntOrNull() else null,
                hotPercentage = if (userData.betType == BetType.PERCENTAGE) userData.hotPercentage.toIntOrNull() else null,
                hotBreak3D = if (userData.betType3D == BetType.AMOUNT) userData.hotBreak3D.toIntOrNull() else null,
                hotPercentage3D = if (userData.betType3D == BetType.PERCENTAGE) userData.hotPercentage3D.toIntOrNull() else null,
                betType = if (userData.betType == BetType.AMOUNT) "amount" else "percentage",
                betType3D = if (userData.betType3D == BetType.AMOUNT) "amount" else "percentage",
                userAccess = buildString {
                    if (userData.access2D) append("2D")
                    if (userData.access3D) {
                        if (isNotEmpty()) append(",")
                        append("3D")
                    }
                },
                host = userData.host,
                breakAccess = userData.breakAccess,
                hotAccess = userData.hotAccess
            )
            
            val response: UserResponse = httpClient.put("$baseUrl/v1/account/updateUser") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteUser(userId: String): Result<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val response: UserResponse = httpClient.delete("$baseUrl/v1/account/deleteUser/$userId").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAgentList(): Result<AgentListResponse> = withContext(Dispatchers.IO) {
        try {
            val response: AgentListResponse = httpClient.get("$baseUrl/v1/account/agents").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserProfile(): Result<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val response: UserResponse = httpClient.get("$baseUrl/v1/account/profile").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}