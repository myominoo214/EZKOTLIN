package core.stores

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ui.screens.TempListItem

// Actions
sealed class TempListAction {
    data class AddItem(val item: TempListItem) : TempListAction()
    data class DeleteItem(val index: Int) : TempListAction()
    data class DeleteGroup(val index: Int) : TempListAction()
    data class SetListType(val type: String) : TempListAction()
    object ClearList : TempListAction()
    data class UpdateItems(val items: List<TempListItem>) : TempListAction()
    data class AddItems(val items: List<TempListItem>) : TempListAction()
}

// State
data class TempListState(
    val items: List<TempListItem> = emptyList(),
    val listType: String = "SELL" // "BUY" or "SELL"
)

// Store
class TempListStore {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _state = MutableStateFlow(TempListState())
    val state: StateFlow<TempListState> = _state.asStateFlow()
    
    companion object {
        @Volatile
        private var INSTANCE: TempListStore? = null
        
        fun getInstance(): TempListStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TempListStore().also { INSTANCE = it }
            }
        }
    }
    
    fun dispatch(action: TempListAction) {
        println("[DEBUG] TempListStore - Dispatching action: $action")
        println("[DEBUG] TempListStore - Current items count before: ${_state.value.items.size}")
        _state.value = reduce(_state.value, action)
        println("[DEBUG] TempListStore - Current items count after: ${_state.value.items.size}")
        println("[DEBUG] TempListStore - Current items: ${_state.value.items}")
    }
    
    private fun reduce(state: TempListState, action: TempListAction): TempListState {
        return when (action) {
            is TempListAction.AddItem -> {
                val newItems = state.items + action.item
                state.copy(items = newItems)
            }
            
            is TempListAction.DeleteItem -> {
                val newItems = state.items.toMutableList()
                if (action.index in newItems.indices) {
                    newItems.removeAt(action.index)
                }
                state.copy(items = newItems)
            }
            
            is TempListAction.DeleteGroup -> {
                if (action.index in state.items.indices) {
                    val groupId = state.items[action.index].groupId
                    if (groupId != null) {
                        val newItems = state.items.filter { it.groupId != groupId }
                        state.copy(items = newItems)
                    } else {
                        // If no groupId, just delete the single item
                        val newItems = state.items.toMutableList()
                        newItems.removeAt(action.index)
                        state.copy(items = newItems)
                    }
                } else {
                    state
                }
            }
            
            is TempListAction.SetListType -> {
                state.copy(listType = action.type)
            }
            
            is TempListAction.ClearList -> {
                state.copy(items = emptyList())
            }
            
            is TempListAction.UpdateItems -> {
                state.copy(items = action.items)
            }
            
            is TempListAction.AddItems -> {
                println("[DEBUG] TempListStore - Adding ${action.items.size} items")
                val newItems = state.items + action.items
                println("[DEBUG] TempListStore - New items list size: ${newItems.size}")
                state.copy(items = newItems)
            }
        }
    }
    
    // Convenience methods for common operations
    fun addItem(item: TempListItem) {
        dispatch(TempListAction.AddItem(item))
    }
    
    fun deleteItem(index: Int) {
        dispatch(TempListAction.DeleteItem(index))
    }
    
    fun deleteGroup(index: Int) {
        dispatch(TempListAction.DeleteGroup(index))
    }
    
    fun setListType(type: String) {
        dispatch(TempListAction.SetListType(type))
    }
    
    fun clearList() {
        dispatch(TempListAction.ClearList)
    }
    
    fun updateItems(items: List<TempListItem>) {
        dispatch(TempListAction.UpdateItems(items))
    }
    
    fun addItems(items: List<TempListItem>) {
        dispatch(TempListAction.AddItems(items))
    }
    
    // Selectors
    fun getItems(): List<TempListItem> = _state.value.items
    fun getListType(): String = _state.value.listType
    fun getItemCount(): Int = _state.value.items.size
    fun getTotalAmount(): Int = _state.value.items.sumOf { it.amount.toIntOrNull() ?: 0 }
}

// Hook for Compose components
@androidx.compose.runtime.Composable
fun rememberTempListStore(): TempListStore {
    return androidx.compose.runtime.remember { TempListStore.getInstance() }
}