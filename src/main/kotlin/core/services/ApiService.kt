package core.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.SerialName
import ui.screens.BreakAmountSerializer
import data.models.TermsApiResponse
import data.models.TermsApiResponseData
import data.models.TermData
import ui.screens.AddTermRequest
import data.models.*

/**
 * API Response wrapper class
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val statusCode: Int = 200
)

/**
 * API Exception class for handling HTTP errors
 */
class ApiException(message: String, val statusCode: Int) : Exception(message)

/**
 * Main API Service class for handling HTTP requests using Ktor Client
 */
class ApiService {
    companion object {
        const val BASE_URL = "https://api.easiledger.app"
        
        // Terms API methods
        suspend fun getTerms(page: Int = 1, search: String? = null): TermsApiResponse {
            val apiService = ApiService()
            val userSession = UserSession.getInstance()
            val searchParam = if (search.isNullOrBlank()) "" else "&keyword=$search"
            val url = "${BASE_URL}/v1/term/getTerms?current=$page&limit=30$searchParam"
            val response = apiService.get<TermsApiResponse>(
                url = url,
                headers = userSession.getAuthHeaders()
            )
            return if (response.success && response.data != null) {
                response.data
            } else {
                TermsApiResponse(code = "500", status = "error", message = response.message ?: "Failed to fetch terms", data = TermsApiResponseData(by = emptyList(), pagination = data.models.PaginationData(page = 1, limit = 30, total = 0)))
            }
        }
        
        suspend fun addTerm(request: AddTermRequest): TermsApiResponse {
            val apiService = ApiService()
            val userSession = UserSession.getInstance()
            val response = apiService.post<AddTermRequest, TermsApiResponse>(
                url = "${BASE_URL}/v1/term/addTerm",
                body = request,
                headers = userSession.getAuthHeaders()
            )
            return if (response.success && response.data != null) {
                response.data
            } else {
                TermsApiResponse(code = "500", status = "error", message = response.message ?: "Failed to add term", data = TermsApiResponseData(by = emptyList(), pagination = data.models.PaginationData(page = 1, limit = 30, total = 0)))
            }
        }
        
        // Old updateTerm function removed to prevent conflicts with Boolean is2D
        
        suspend fun deleteTerm(termId: Int): TermsApiResponse {
            val apiService = ApiService()
            val userSession = UserSession.getInstance()
            val response = apiService.deleteForString(
                url = "${BASE_URL}/v1/term/deleteTerm/$termId",
                headers = userSession.getAuthHeaders()
            )
            return if (response.success) {
                TermsApiResponse(code = "200", status = "success", message = "Term deleted successfully", data = TermsApiResponseData(by = emptyList(), pagination = data.models.PaginationData(page = 1, limit = 30, total = 0)))
            } else {
                TermsApiResponse(code = "500", status = "error", message = response.message ?: "Failed to delete term", data = TermsApiResponseData(by = emptyList(), pagination = data.models.PaginationData(page = 1, limit = 30, total = 0)))
            }
        }
    }
    
    val client = HttpClient(CIO) {
        engine {
            pipelining = true  // Enable HTTP pipelining for parallel API calls
        }
        
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            })
        }
        
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }
        
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    /**
     * GET request with typed response
     */
    suspend inline fun <reified T> get(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse<T> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.get(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            if (response.status.isSuccess()) {
                val data: T = response.body()
                ApiResponse(success = true, data = data, statusCode = response.status.value)
            } else {
                val errorMessage = response.bodyAsText()
                ApiResponse(
                    success = false,
                    message = "HTTP ${response.status.value}: $errorMessage",
                    statusCode = response.status.value
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Network error: ${e.message}",
                statusCode = 0
            )
        }
    }

    /**
     * GET request with string response
     */
    suspend fun getString(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse<String> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.get(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            val responseText = response.bodyAsText()
            
            if (response.status.isSuccess()) {
                ApiResponse(success = true, data = responseText, statusCode = response.status.value)
            } else {
                ApiResponse(
                    success = false,
                    message = "HTTP ${response.status.value}: $responseText",
                    statusCode = response.status.value
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Network error: ${e.message}",
                statusCode = 0
            )
        }
    }

    /**
     * POST request with typed request and response
     */
    suspend inline fun <reified T, reified R> post(
        url: String,
        body: T,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse<R> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.post(url) {
                setBody(body)
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            if (response.status.isSuccess()) {
                val data: R = response.body()
                ApiResponse(success = true, data = data, statusCode = response.status.value)
            } else {
                val errorMessage = response.bodyAsText()
                ApiResponse(
                    success = false,
                    message = "HTTP ${response.status.value}: $errorMessage",
                    statusCode = response.status.value
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Network error: ${e.message}",
                statusCode = 0
            )
        }
    }

    /**
     * POST request with string response
     */
    suspend inline fun <reified T> postForString(
        url: String,
        body: T,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse<String> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.post(url) {
                setBody(body)
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            val responseText = response.bodyAsText()
            
            if (response.status.isSuccess()) {
                ApiResponse(success = true, data = responseText, statusCode = response.status.value)
            } else {
                ApiResponse(
                    success = false,
                    message = "HTTP ${response.status.value}: $responseText",
                    statusCode = response.status.value
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Network error: ${e.message}",
                statusCode = 0
            )
        }
    }

    /**
     * PUT request with typed request and response
     */
    suspend inline fun <reified T, reified R> put(
        url: String,
        body: T,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse<R> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.put(url) {
                setBody(body)
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            if (response.status.isSuccess()) {
                val data: R = response.body()
                ApiResponse(success = true, data = data, statusCode = response.status.value)
            } else {
                val errorMessage = response.bodyAsText()
                ApiResponse(
                    success = false,
                    message = "HTTP ${response.status.value}: $errorMessage",
                    statusCode = response.status.value
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Network error: ${e.message}",
                statusCode = 0
            )
        }
    }

    /**
     * PUT request with string response
     */
    suspend inline fun <reified T> putForString(
        url: String,
        body: T,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse<String> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.put(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            val responseText = response.bodyAsText()
            
            if (response.status.isSuccess()) {
                ApiResponse(success = true, data = responseText, statusCode = response.status.value)
            } else {
                ApiResponse(
                    success = false,
                    message = "HTTP ${response.status.value}: $responseText",
                    statusCode = response.status.value
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Network error: ${e.message}",
                statusCode = 0
            )
        }
    }

    /**
     * DELETE request with typed response
     */
    suspend inline fun <reified T> delete(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse<T> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.delete(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            if (response.status.isSuccess()) {
                val data: T = response.body()
                ApiResponse(success = true, data = data, statusCode = response.status.value)
            } else {
                val errorMessage = response.bodyAsText()
                ApiResponse(
                    success = false,
                    message = "HTTP ${response.status.value}: $errorMessage",
                    statusCode = response.status.value
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Network error: ${e.message}",
                statusCode = 0
            )
        }
    }

    /**
     * DELETE request with request body and typed response
     */
    suspend inline fun <reified T, reified R> deleteWithBody(
        url: String,
        body: T,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse<R> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.delete(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            if (response.status.isSuccess()) {
                val data: R = response.body()
                ApiResponse(success = true, data = data, statusCode = response.status.value)
            } else {
                val errorMessage = response.bodyAsText()
                ApiResponse(
                    success = false,
                    message = "HTTP ${response.status.value}: $errorMessage",
                    statusCode = response.status.value
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Network error: ${e.message}",
                statusCode = 0
            )
        }
    }

    /**
     * DELETE request with request body and string response
     */
    suspend inline fun <reified T> deleteWithBodyForString(
        url: String,
        body: T,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse<String> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.delete(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            val responseText = response.bodyAsText()
            
            if (response.status.isSuccess()) {
                ApiResponse(success = true, data = responseText, statusCode = response.status.value)
            } else {
                ApiResponse(
                    success = false,
                    message = "HTTP ${response.status.value}: $responseText",
                    statusCode = response.status.value
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Network error: ${e.message}",
                statusCode = 0
            )
        }
    }

    /**
     * DELETE request with string response
     */
    suspend fun deleteForString(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): ApiResponse<String> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.delete(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            val responseText = response.bodyAsText()
            
            if (response.status.isSuccess()) {
                ApiResponse(success = true, data = responseText, statusCode = response.status.value)
            } else {
                ApiResponse(
                    success = false,
                    message = "HTTP ${response.status.value}: $responseText",
                    statusCode = response.status.value
                )
            }
        } catch (e: Exception) {
            ApiResponse(
                success = false,
                message = "Network error: ${e.message}",
                statusCode = 0
            )
        }
    }

    // Slip-related API methods
    @Serializable
    data class DeleteSlipItemRequest(
        val _id: String,
        val slipId: String,
        val termId: Int,
        val userId: Int
    )
    
    @Serializable
    data class SlipResponse(
        val success: Boolean = true,
        val code: String? = null,
        val data: JsonElement? = null, // Can be SlipResponseData for slips or List<SlipDetailResponseData> for slip details
        val message: String? = null,
        val phoneNumber: String? = null
    )
    
    @Serializable
    data class SlipResponseData(
        val by: List<SlipData>? = null,
        val pagination: PaginationData? = null
    )
    
    @Serializable
    data class SlipDetailResponseData(
        val _id: String,
        val number: String,
        val type: String? = null,
        val amount: Int,
        val slipId: String,
        val termId: Int,
        val userId: Int,
        val summary: String? = null,
        val showSummary: String? = null,
        val groupId: String? = null,
        val smsCopy: Int? = null,
        val copy: Int? = null
    )
    
    @Serializable
    data class SlipData(
        val _id: String? = null,
        val slipId: String,
        val termId: Int,
        val userId: Int,
        val totalAmount: String,
        val status: String? = null,
        val customer: String = "",
        val deviceName: String? = null,
        val creator: String? = null,
        val createdAt: String,
        val user: SlipUser,
        val remark: String? = null,
        val copy: Int? = null,
        val smsCopy: Int? = null
    )
    
    @Serializable
    data class SlipUser(
        val id: Int,
        val name: String,
        val userType: String? = null
    )
    
    @Serializable
    data class PaginationData(
        val total: Int = 0,
        val current: Int = 1,
        val pageSize: Int = 30,
        val limit: Int = 30,
        val rowsPerPage: Int = 4,
        val totalSlipAmount: String = "0"
    )
    
    suspend fun getSlips(termId: String, userId: String, page: Int, limit: Int): ApiResponse<SlipResponse> {
        val url = if (userId.isBlank()) {
            "$BASE_URL/v1/slip/getSlips?termId=$termId&current=$page&limit=$limit"
        } else {
            "$BASE_URL/v1/slip/getSlips?termId=$termId&userId=$userId&current=$page&limit=$limit"
        }
         println("[DEBUG] getSlips Url ${url}")
        return get<SlipResponse>(url, mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}"))
    }
    
    suspend fun getSlipDetails(slipId: String, termId: String, userId: String): ApiResponse<SlipResponse> {
        val url = "$BASE_URL/v1/slip/getSlipDetail?slipId=$slipId&termId=$termId&userId=$userId"
        return get<SlipResponse>(url, mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}"))
    }
    
    suspend fun getStatements(termId: String, userId: String): ApiResponse<SlipResponse> {
        val url = "$BASE_URL/v1/report/getStatementByTermId?termId=$termId&userId=$userId"
        return get<SlipResponse>(url, mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}"))
    }
    
    @Serializable
    data class StatementData(
        val TotalUnitWithDiscount: Double = 0.0,
        val TotalAmountWithoutDiscount: Double = 0.0,
        val TotalWinAmountWithoutPrize: Double = 0.0,
        val TotalAmountWithPrize: Double = 0.0,
        val SubTotalAmount: Double = 0.0
    )
    
    suspend fun deleteSlipItem(itemId: String, slipId: String, termId: Int, userId: Int): ApiResponse<SlipResponse> {
        val request = DeleteSlipItemRequest(
            _id = itemId,
            slipId = slipId,
            termId = termId,
            userId = userId
        )
        val url = "$BASE_URL/v1/slip/deleteSlip"
        return deleteWithBody<DeleteSlipItemRequest, SlipResponse>(
            url = url,
            body = request,
            headers = mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}")
        )
    }
    
    suspend fun deleteSlip(slipId: String, termId: String, userId: String): ApiResponse<SlipResponse> {
        val request = mapOf(
            "slipId" to slipId,
            "termId" to termId,
            "userId" to userId
        )
        val url = "$BASE_URL/v1/slip/deleteSlip"
        return deleteWithBody<Map<String, String>, SlipResponse>(
            url = url,
            body = request,
            headers = mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}")
        )
    }

    @Serializable
    data class UpdateSlipItem(
        val _id: String = "",
        val number: String,
        val amount: Int,
        val showSummary: String = "1",
        val groupId: String? = null,
        val summary: String = ""
    )

    @Serializable
    data class UpdateSlipsRequest(
        val termId: Int,
        val userId: Int,
        val slipId: String,
        val ledger: List<UpdateSlipItem>
    )

    suspend fun updateSlips(request: UpdateSlipsRequest): ApiResponse<SlipResponse> {
        val url = "$BASE_URL/v1/slip/updateSlips"
        return put<UpdateSlipsRequest, SlipResponse>(
            url = url,
            body = request,
            headers = mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}")
        )
    }

    // Settings-related API methods
    @Serializable
    data class UpdateSettingsRequest(
        val userId: Int,
        val enablePrinting: String,
        val enableSong: String,
        val isTwoColumn: String,
        val printWidth: String,
        val showBusinessName: String,
        val showEmployeeName: String,
        val showTermName: String,
        val showPrintTime: String
    )

    @Serializable
    data class ChangePasswordRequest(
        val userId: Int,
        val oldPassword: String,
        val newPassword: String
    )

    @Serializable
    data class UpdateUserRequest(
        val userId: Int,
        val phoneNumber1: String,
        val phoneNumber2: String? = null,
        val phoneNumber3: String? = null
    )

    @Serializable
    data class SendNotificationRequest(
        val title: String,
        val description: String
    )

    @Serializable
    data class UpdateMarqueeRequest(
        val userId: Int,
        val alertMessage: String
    )

    @Serializable
    data class UpdateFooterTextRequest(
        val userId: Int,
        val footerText: String
    )

    @Serializable
    data class UpdateFontSizeRequest(
        val userId: Int,
        val fontSize: Int
    )

    suspend fun updateSettings(request: UpdateSettingsRequest): ApiResponse<String> {
        val url = "$BASE_URL/v1/account/updateSettings"
        return postForString(
            url = url,
            body = request,
            headers = mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}")
        )
    }

    suspend fun changePassword(request: ChangePasswordRequest): ApiResponse<String> {
        val url = "$BASE_URL/v1/account/changePassword"
        return putForString(
            url = url,
            body = request,
            headers = mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}")
        )
    }

    suspend fun updateUser(request: UpdateUserRequest): ApiResponse<String> {
        val url = "$BASE_URL/v1/account/updateUser"
        return putForString(
            url = url,
            body = request,
            headers = mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}")
        )
    }

    suspend fun sendNotification(request: SendNotificationRequest): ApiResponse<String> {
        val url = "$BASE_URL/v1/account/sendNoti"
        return postForString(
            url = url,
            body = request,
            headers = mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}")
        )
    }

    suspend fun updateMarquee(request: UpdateMarqueeRequest): ApiResponse<String> {
        val url = "$BASE_URL/v1/account/updateAlertMessage"
        return putForString(
            url = url,
            body = request,
            headers = mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}")
        )
    }

    suspend fun updateFooterText(request: UpdateFooterTextRequest): ApiResponse<String> {
        val url = "$BASE_URL/v1/account/updateFooterText"
        return putForString(
            url = url,
            body = request,
            headers = mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}")
        )
    }

    suspend fun updateFontSize(request: UpdateFontSizeRequest): ApiResponse<String> {
        val url = "$BASE_URL/v1/account/updateFontSize"
        return putForString(
            url = url,
            body = request,
            headers = mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}")
        )
    }

    @Serializable
    data class UpdateTermRequest(
        val termId: String,
        val termName: String,
        val shortName: String,
        val groupId: String,
        val startDate: String,
        val endDate: String,
        val isFinished: String,
        val termType: String,
        val winNum: String,
        //val is2D: Int,
        val unitPrice: Double,
        @Serializable(with = BreakAmountSerializer::class)
        val breakAmount: Int
    )

    suspend fun updateTerm(request: UpdateTermRequest): ApiResponse<String> {
        val url = "$BASE_URL/v1/term/updateTerm"
        return putForString(
            url = url,
            body = request,
            headers = mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}")
        )
    }

    @Serializable
    data class AlertMessageResponse(
        val code: String? = null,
        val status: String? = null,
        val message: String? = null,
        val data: AlertMessageData? = null
    )

    @Serializable
    data class AlertMessageData(
        val alertMessage: String? = null
    )

    suspend fun getAlertMessage(): ApiResponse<AlertMessageResponse> {
        val url = "$BASE_URL/v1/account/getAlertMessage"
        return get<AlertMessageResponse>(
            url = url,
            headers = mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}")
        )
    }

    @Serializable
    data class EncryptionRequest(
        val text: String
    )

    suspend fun getEncryptionInfo(request: EncryptionRequest): ApiResponse<String> {
        val url = "$BASE_URL/v1/encrypt"
        return post<EncryptionRequest, String>(
            url = url,
            body = request,
            headers = mapOf("Authorization" to "Bearer ${UserSession.getInstance().authToken}")
        )
    }

    suspend fun getUserProfile(headers: Map<String, String> = emptyMap()): ApiResponse<UserProfileApiResponse> {
        return try {
            val response = get<UserProfileApiResponse>(
                url = "$BASE_URL/v1/account/getUserProfile",
                headers = headers
            )
            response
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Unknown error occurred")
        }
    }

    // User Management API Methods
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
            
            val response: UserResponse = client.post("$BASE_URL$endpoint") {
                contentType(ContentType.Application.Json)
                setBody(request)
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
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
            
            val response: UserResponse = client.put("$BASE_URL/v1/account/updateUser") {
                contentType(ContentType.Application.Json)
                setBody(request)
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }.body()
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteUser(userId: String): Result<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val response: UserResponse = client.delete("$BASE_URL/v1/account/deleteUser/$userId") {
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAgentList(): Result<AgentListResponse> = withContext(Dispatchers.IO) {
        try {
            val response: AgentListResponse = client.get("$BASE_URL/v1/account/agents") {
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserProfileResult(): Result<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val response: UserResponse = client.get("$BASE_URL/v1/account/profile") {
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Sale API Methods
    suspend fun getActiveTerms(current: Int = 1, limit: Int = 100000): ui.screens.ApiResponse<List<ui.screens.SaleTerm>> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$BASE_URL/v1/term/getActiveTerms") {
                parameter("current", current)
                parameter("limit", limit)
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            // Try direct array format first, fallback to nested response
            try {
                response.body<ui.screens.ApiResponse<List<ui.screens.SaleTerm>>>()
            } catch (directError: Exception) {
                // Fallback to nested response format
                val nestedResponse = response.body<ui.screens.NestedApiResponse<List<ui.screens.SaleTerm>>>()
                ui.screens.ApiResponse(
                    code = nestedResponse.code,
                    message = nestedResponse.message,
                    data = nestedResponse.data.by
                )
            }
        } catch (e: Exception) {
            println("Error fetching active terms: ${e.message}")
            // Fallback to mock data if API fails
            ui.screens.ApiResponse(
                code = "200",
                message = "Success (Fallback)",
                data = listOf(
                    ui.screens.SaleTerm(
                        termId = "1",
                        termName = "Morning Term",
                        unitPrice = 100.0,
                        breakAmount = 1000.0,
                        is2D = "1",
                        termType = "regular"
                    ),
                    ui.screens.SaleTerm(
                        termId = "2",
                        termName = "Evening Term",
                        unitPrice = 100.0,
                        breakAmount = 1500.0,
                        is2D = "0",
                        termType = "regular"
                    )
                )
            )
        }
    }
    
    suspend fun getSaleUserProfile(): ui.screens.ApiResponse<ui.screens.UserProfile> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$BASE_URL/v1/account/getUserProfile") {
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            response.body<ui.screens.ApiResponse<ui.screens.UserProfile>>()
        } catch (e: Exception) {
            println("Error fetching user profile: ${e.message}")
            ui.screens.ApiResponse(
                code = "200",
                message = "Success (Fallback)",
                data = ui.screens.UserProfile(
                    businessName = "Default Business",
                    name = "Default User",
                    userType = "user"
                )
            )
        }
    }
    
    suspend fun getUserLists(current: Int = 1, limit: Int = 100000): ui.screens.ApiResponse<List<ui.screens.User>> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$BASE_URL/v1/account/getUserLists") {
                parameter("current", current)
                parameter("limit", limit)
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            val nestedResponse = response.body<ui.screens.NestedApiResponse<List<ui.screens.User>>>()
            ui.screens.ApiResponse(
                code = nestedResponse.code,
                message = nestedResponse.message,
                data = nestedResponse.data.by
            )
        } catch (e: Exception) {
            println("Error fetching user lists: ${e.message}")
            ui.screens.ApiResponse(
                code = "200",
                message = "Success (Fallback)",
                data = listOf(
                    ui.screens.User(
                        userId = "1",
                        name = "Default User",
                        userType = "user",
                        discount2D = 0,
                        discount3D = 0
                    )
                )
            )
        }
    }
    
    suspend fun getApiUserData(current: Int = 1, limit: Int = 100000): ui.screens.ApiResponse<List<ui.screens.ApiUserData>> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$BASE_URL/v1/account/getUserLists") {
                parameter("current", current)
                parameter("limit", limit)
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            val nestedResponse = response.body<ui.screens.NestedApiResponse<List<ui.screens.User>>>()
            // Convert User data to ApiUserData format
            val apiUserData = nestedResponse.data.by.map { user ->
                ui.screens.ApiUserData(
                    userId = user.userId,
                    discount2D = 0.0, // Default values since this data isn't in user lists
                    discount3D = 0.0
                )
            }
            ui.screens.ApiResponse(
                code = nestedResponse.code,
                message = nestedResponse.message,
                data = apiUserData
            )
        } catch (e: Exception) {
            println("Error fetching API user data: ${e.message}")
            ui.screens.ApiResponse(
                code = "200",
                message = "Success (Fallback)",
                data = listOf(
                    ui.screens.ApiUserData(userId = "1", discount2D = 0.1, discount3D = 0.15)
                )
            )
        }
    }
    
    suspend fun getBettedTotalUnits(termId: String, userId: String): ui.screens.ApiResponse<ui.screens.BettedUnitsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$BASE_URL/v1/ledger/getBettedTotalUnits") {
                parameter("termId", termId)
                parameter("userId", userId)
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            response.body<ui.screens.ApiResponse<ui.screens.BettedUnitsResponse>>()
        } catch (e: Exception) {
            println("Error fetching betted total units: ${e.message}")
            ui.screens.ApiResponse(
                code = "200",
                message = "Success (Fallback)",
                data = ui.screens.BettedUnitsResponse(totalUnits = 0)
            )
        }
    }
    
    suspend fun buySlip(payload: ui.screens.SlipPayload): ui.screens.ApiResponse<ui.screens.SlipResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.post("$BASE_URL/v1/ledger/buySlip") {
                contentType(ContentType.Application.Json)
                setBody(payload)
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            response.body<ui.screens.ApiResponse<ui.screens.SlipResponse>>()
        } catch (e: Exception) {
            println("Error buying slip: ${e.message}")
            ui.screens.ApiResponse(
                code = "500",
                message = "Error: ${e.message}",
                data = ui.screens.SlipResponse(slipId = "", ledger = emptyList())
            )
        }
    }
    
    suspend fun saveSlip(payload: ui.screens.SlipPayload): ui.screens.ApiResponse<ui.screens.SlipResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.post("$BASE_URL/v1/ledger/saveSlip") {
                contentType(ContentType.Application.Json)
                setBody(payload)
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            response.body<ui.screens.ApiResponse<ui.screens.SlipResponse>>()
        } catch (e: Exception) {
            println("Error saving slip: ${e.message}")
            ui.screens.ApiResponse(
                code = "500",
                message = "Error: ${e.message}",
                data = ui.screens.SlipResponse(slipId = "", ledger = emptyList())
            )
        }
    }
    
    suspend fun addSlips(payload: ui.screens.SlipPayload): ui.screens.ApiResponse<ui.screens.SlipResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.post("$BASE_URL/v1/ledger/addSlips") {
                contentType(ContentType.Application.Json)
                setBody(payload)
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            response.body<ui.screens.ApiResponse<ui.screens.SlipResponse>>()
        } catch (e: Exception) {
            println("Error adding slips: ${e.message}")
            ui.screens.ApiResponse(
                code = "500",
                message = "Error: ${e.message}",
                data = ui.screens.SlipResponse(slipId = "", ledger = emptyList())
            )
        }
    }
    
    suspend fun addSlipsWithHotBreak(payload: ui.screens.SlipPayload): ui.screens.ApiResponse<ui.screens.SlipResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.post("$BASE_URL/v1/ledger/addSlipsWithHotBreak") {
                contentType(ContentType.Application.Json)
                setBody(payload)
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            response.body<ui.screens.ApiResponse<ui.screens.SlipResponse>>()
        } catch (e: Exception) {
            println("Error adding slips with hot break: ${e.message}")
            ui.screens.ApiResponse(
                code = "500",
                message = "Error: ${e.message}",
                data = ui.screens.SlipResponse(slipId = "", ledger = emptyList())
            )
        }
    }
    
    suspend fun saveSlips(payload: ui.screens.SlipPayload): ui.screens.ApiResponse<ui.screens.SlipResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.post("$BASE_URL/v1/ledger/saveSlips") {
                contentType(ContentType.Application.Json)
                setBody(payload)
                UserSession.getInstance().getAuthHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
            response.body<ui.screens.ApiResponse<ui.screens.SlipResponse>>()
        } catch (e: Exception) {
            println("Error saving slips: ${e.message}")
            ui.screens.ApiResponse(
                code = "500",
                message = "Error: ${e.message}",
                data = ui.screens.SlipResponse(slipId = "", ledger = emptyList())
            )
        }
    }

    /**
     * Close the HTTP client when done
     */
    fun close() {
        client.close()
    }
    
}

/**
 * Example usage and data models
 */

// Example data models
@Serializable
data class User(
    val id: Int? = null,
    val name: String,
    val email: String
)

@Serializable
data class Product(
    val id: Int? = null,
    val name: String,
    val price: Double,
    val description: String? = null
)

/**
 * Example API client class using Ktor Client
 */
class ExampleApiClient {
    private val apiService = ApiService()
    private val baseUrl = ApiService.BASE_URL
    
    // Example GET request for list of users
    suspend fun getUsers(): ApiResponse<List<User>> {
        return apiService.get<List<User>>(
            url = "$baseUrl/users",
            headers = mapOf("Authorization" to "Bearer your-token")
        )
    }
    
    // Example GET request for single user
    suspend fun getUser(userId: Int): ApiResponse<User> {
        return apiService.get<User>(
            url = "$baseUrl/users/$userId",
            headers = mapOf("Authorization" to "Bearer your-token")
        )
    }
    
    // Example POST request
    suspend fun createUser(user: User): ApiResponse<User> {
        return apiService.post<User, User>(
            url = "$baseUrl/users",
            body = user,
            headers = mapOf("Authorization" to "Bearer your-token")
        )
    }
    
    // Example PUT request
    suspend fun updateUser(userId: Int, user: User): ApiResponse<User> {
        return apiService.put<User, User>(
            url = "$baseUrl/users/$userId",
            body = user,
            headers = mapOf("Authorization" to "Bearer your-token")
        )
    }
    
    // Example DELETE request
    suspend fun deleteUser(userId: Int): ApiResponse<String> {
        return apiService.deleteForString(
            url = "$baseUrl/users/$userId",
            headers = mapOf("Authorization" to "Bearer your-token")
        )
    }
    
    // Example with query parameters
    suspend fun searchUsers(query: String, limit: Int = 10): ApiResponse<List<User>> {
        return apiService.get<List<User>>(
            url = "$baseUrl/users?q=$query&limit=$limit",
            headers = mapOf("Authorization" to "Bearer your-token")
        )
    }
    
    // Clean up resources
    fun close() {
        apiService.close()
    }
}

/**
 * Usage example in a Composable or ViewModel
 */
/*
class UserViewModel {
    private val apiClient = ExampleApiClient()
    
    suspend fun loadUsers() {
        val response = apiClient.getUsers()
        if (response.success) {
            // Handle successful response
            val users = response.data
            println("Loaded ${users?.size} users")
        } else {
            // Handle error
            println("Error: ${response.message}")
        }
    }
    
    suspend fun createNewUser(name: String, email: String) {
        val newUser = User(name = name, email = email)
        val response = apiClient.createUser(newUser)
        if (response.success) {
            println("User created: ${response.data}")
        } else {
            println("Failed to create user: ${response.message}")
        }
    }
    
    suspend fun searchForUsers(searchQuery: String) {
        val response = apiClient.searchUsers(searchQuery, limit = 20)
        if (response.success) {
            println("Found ${response.data?.size} users matching '$searchQuery'")
        } else {
            println("Search failed: ${response.message}")
        }
    }
    
    // Don't forget to clean up
    fun cleanup() {
        apiClient.close()
    }
}
*/