package core.stores

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ui.screens.LedgerItem
import ui.screens.LedgerApiResponseData
import ui.screens.LedgerApiData
import core.services.ApiService
import core.services.UserSession

// Actions for LedgerStore
sealed class LedgerAction {
    data class SetLedgerData(val ledgerData: List<LedgerItem>) : LedgerAction()
    data class AddLedgerItem(val item: LedgerItem) : LedgerAction()
    data class UpdateLedgerItem(val index: Int, val item: LedgerItem) : LedgerAction()
    data class RemoveLedgerItem(val index: Int) : LedgerAction()
    data class FilterByUser(val userId: String) : LedgerAction()
    data class FilterByPrize(val prize: String) : LedgerAction()
    data class SetLoading(val loading: Boolean) : LedgerAction()
    object ClearLedgerData : LedgerAction()
}

// State for LedgerStore
data class LedgerStoreState(
    val ledgerData: List<LedgerItem> = emptyList(),
    val filteredData: List<LedgerItem> = emptyList(),
    val selectedUser: String = "",
    val selectedPrize: String = "",
    val loading: Boolean = false,
    val lastUpdated: Long = 0L
)

// LedgerStore class
class LedgerStore private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: LedgerStore? = null
        
        fun getInstance(): LedgerStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LedgerStore().also { INSTANCE = it }
            }
        }
    }
    
    private val _state = MutableStateFlow(LedgerStoreState())
    val state: StateFlow<LedgerStoreState> = _state.asStateFlow()
    
    fun dispatch(action: LedgerAction) {
        _state.value = reduce(_state.value, action)
    }
    
    private fun reduce(state: LedgerStoreState, action: LedgerAction): LedgerStoreState {
        return when (action) {
            is LedgerAction.SetLedgerData -> {
                val filteredData = applyFilters(action.ledgerData, state.selectedUser, state.selectedPrize)
                state.copy(
                    ledgerData = action.ledgerData,
                    filteredData = filteredData,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            is LedgerAction.AddLedgerItem -> {
                val newLedgerData = state.ledgerData + action.item
                val filteredData = applyFilters(newLedgerData, state.selectedUser, state.selectedPrize)
                state.copy(
                    ledgerData = newLedgerData,
                    filteredData = filteredData,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            is LedgerAction.UpdateLedgerItem -> {
                val newLedgerData = state.ledgerData.toMutableList().apply {
                    if (action.index in indices) {
                        this[action.index] = action.item
                    }
                }
                val filteredData = applyFilters(newLedgerData, state.selectedUser, state.selectedPrize)
                state.copy(
                    ledgerData = newLedgerData,
                    filteredData = filteredData,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            is LedgerAction.RemoveLedgerItem -> {
                val newLedgerData = state.ledgerData.toMutableList().apply {
                    if (action.index in indices) {
                        removeAt(action.index)
                    }
                }
                val filteredData = applyFilters(newLedgerData, state.selectedUser, state.selectedPrize)
                state.copy(
                    ledgerData = newLedgerData,
                    filteredData = filteredData,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            is LedgerAction.FilterByUser -> {
                val filteredData = applyFilters(state.ledgerData, action.userId, state.selectedPrize)
                state.copy(
                    selectedUser = action.userId,
                    filteredData = filteredData
                )
            }
            is LedgerAction.FilterByPrize -> {
                val filteredData = applyFilters(state.ledgerData, state.selectedUser, action.prize)
                state.copy(
                    selectedPrize = action.prize,
                    filteredData = filteredData
                )
            }
            is LedgerAction.SetLoading -> {
                state.copy(loading = action.loading)
            }
            is LedgerAction.ClearLedgerData -> {
                LedgerStoreState()
            }
        }
    }
    
    private fun applyFilters(data: List<LedgerItem>, userId: String, prize: String): List<LedgerItem> {
        var filtered = data
        
        // Apply user filter if specified
        if (userId.isNotEmpty()) {
            // Filter by customer field as a proxy for user filtering
            filtered = filtered.filter { it.customer.contains(userId, ignoreCase = true) }
        }
        
        // Apply prize filter if specified
        if (prize.isNotEmpty()) {
            // Filter by number pattern for prize filtering
            filtered = filtered.filter { it.number.contains(prize) }
        }
        
        return filtered
    }
    
    // Convenience methods for dispatching actions
    fun setLedgerData(ledgerData: List<LedgerItem>) {
        dispatch(LedgerAction.SetLedgerData(ledgerData))
    }
    
    fun addLedgerItem(item: LedgerItem) {
        dispatch(LedgerAction.AddLedgerItem(item))
    }
    
    fun updateLedgerItem(index: Int, item: LedgerItem) {
        dispatch(LedgerAction.UpdateLedgerItem(index, item))
    }
    
    fun removeLedgerItem(index: Int) {
        dispatch(LedgerAction.RemoveLedgerItem(index))
    }
    
    fun filterByUser(userId: String) {
        dispatch(LedgerAction.FilterByUser(userId))
    }
    
    fun filterByPrize(prize: String) {
        dispatch(LedgerAction.FilterByPrize(prize))
    }
    
    fun setLoading(loading: Boolean) {
        dispatch(LedgerAction.SetLoading(loading))
    }
    
    fun clearLedgerData() {
        dispatch(LedgerAction.ClearLedgerData)
    }
    
    // Selector functions
    fun getLedgerData(): List<LedgerItem> {
        return _state.value.ledgerData
    }
    
    fun getFilteredData(): List<LedgerItem> {
        return _state.value.filteredData
    }
    
    fun getSelectedUser(): String {
        return _state.value.selectedUser
    }
    
    fun getSelectedPrize(): String {
        return _state.value.selectedPrize
    }
    
    fun isLoading(): Boolean {
        return _state.value.loading
    }
    
    fun getTotalAmount(): Double {
        return _state.value.filteredData.sumOf { it.totalAmount }
    }
    
    fun getItemCount(): Int {
        return _state.value.filteredData.size
    }
    
    fun getLastUpdated(): Long {
        return _state.value.lastUpdated
    }
    
    // API integration methods
    suspend fun fetchLedgerData(
        termId: String,
        userId: String = "",
        prize: String = "",
        is2D: Boolean = true
    ): Result<List<LedgerItem>> {
        return try {
            setLoading(true)
            
            val userSession = UserSession.getInstance()
            val apiService = ApiService()
            val limit = if (is2D) "100" else "1000"
            
            val url = "/v1/ledger/getLedgers?termId=${termId}&userId=${userId}&prize=${prize}&keyword=&current=1&limit=${limit}"
            
            val response = apiService.get<LedgerApiResponseData>(
                url = "${ApiService.BASE_URL}${url}",
                headers = userSession.getAuthHeaders()
            )
            
            if (response.success && response.data?.code == "200" && response.data.data?.by != null) {
                val ledgerItems = response.data.data.by.map { apiData: LedgerApiData ->
                    LedgerItem(
                        number = apiData.number ?: "",
                        totalAmount = apiData.TotalAmount ?: 0.0,
                        customer = apiData.customer ?: "",
                        amount = apiData.amount ?: 0.0,
                        slipId = apiData.slipId ?: ""
                    )
                }
                
                setLedgerData(ledgerItems)
                filterByUser(userId)
                filterByPrize(prize)
                setLoading(false)
                
                Result.success(ledgerItems)
            } else {
                setLedgerData(emptyList())
                setLoading(false)
                Result.failure(Exception("Failed to fetch ledger data: ${response.message}"))
            }
        } catch (e: Exception) {
            setLedgerData(emptyList())
            setLoading(false)
            Result.failure(e)
        }
    }
    
    suspend fun refreshLedgerData(
        termId: String,
        is2D: Boolean = true
    ): Result<List<LedgerItem>> {
        return fetchLedgerData(
            termId = termId,
            userId = getSelectedUser(),
            prize = getSelectedPrize(),
            is2D = is2D
        )
    }
}