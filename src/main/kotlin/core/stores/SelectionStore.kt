package core.stores

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ui.screens.TermOption
import data.models.UserOption

// Actions for SelectionStore
sealed class SelectionAction {
    data class SetSelectedTerm(val term: TermOption?) : SelectionAction()
    data class SetSelectedUser(val user: UserOption?) : SelectionAction()
    data class SetTermOptions(val options: List<TermOption>) : SelectionAction()
    data class SetUserOptions(val options: List<UserOption>) : SelectionAction()
    object ClearSelection : SelectionAction()
    object ClearTermSelection : SelectionAction()
    object ClearUserSelection : SelectionAction()
}

// State for SelectionStore
data class SelectionStoreState(
    val selectedTerm: TermOption? = null,
    val selectedUser: UserOption? = null,
    val termOptions: List<TermOption> = emptyList(),
    val userOptions: List<UserOption> = emptyList(),
    val lastUpdated: Long = 0L
)

// SelectionStore class
class SelectionStore private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: SelectionStore? = null
        
        fun getInstance(): SelectionStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SelectionStore().also { INSTANCE = it }
            }
        }
    }
    
    private val _state = MutableStateFlow(SelectionStoreState())
    val state: StateFlow<SelectionStoreState> = _state.asStateFlow()
    
    fun dispatch(action: SelectionAction) {
        println("[DEBUG] SelectionStore - Dispatching action: $action")
        _state.value = reduce(_state.value, action)
        println("[DEBUG] SelectionStore - New state: selectedTerm=${_state.value.selectedTerm?.label}, selectedUser=${_state.value.selectedUser?.label}")
    }
    
    private fun reduce(state: SelectionStoreState, action: SelectionAction): SelectionStoreState {
        return when (action) {
            is SelectionAction.SetSelectedTerm -> {
                state.copy(
                    selectedTerm = action.term,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            
            is SelectionAction.SetSelectedUser -> {
                state.copy(
                    selectedUser = action.user,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            
            is SelectionAction.SetTermOptions -> {
                state.copy(
                    termOptions = action.options,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            
            is SelectionAction.SetUserOptions -> {
                state.copy(
                    userOptions = action.options,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            
            is SelectionAction.ClearSelection -> {
                state.copy(
                    selectedTerm = null,
                    selectedUser = null,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            
            is SelectionAction.ClearTermSelection -> {
                state.copy(
                    selectedTerm = null,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            
            is SelectionAction.ClearUserSelection -> {
                state.copy(
                    selectedUser = null,
                    lastUpdated = System.currentTimeMillis()
                )
            }
        }
    }
    
    // Action dispatchers
    fun setSelectedTerm(term: TermOption?) {
        dispatch(SelectionAction.SetSelectedTerm(term))
    }
    
    fun setSelectedUser(user: UserOption?) {
        dispatch(SelectionAction.SetSelectedUser(user))
    }
    
    fun setTermOptions(options: List<TermOption>) {
        dispatch(SelectionAction.SetTermOptions(options))
    }
    
    fun setUserOptions(options: List<UserOption>) {
        dispatch(SelectionAction.SetUserOptions(options))
    }
    
    fun clearSelection() {
        dispatch(SelectionAction.ClearSelection)
    }
    
    fun clearTermSelection() {
        dispatch(SelectionAction.ClearTermSelection)
    }
    
    fun clearUserSelection() {
        dispatch(SelectionAction.ClearUserSelection)
    }
    
    // Selector functions
    fun getSelectedTerm(): TermOption? {
        return _state.value.selectedTerm
    }
    
    fun getSelectedUser(): UserOption? {
        return _state.value.selectedUser
    }
    
    fun getTermOptions(): List<TermOption> {
        return _state.value.termOptions
    }
    
    fun getUserOptions(): List<UserOption> {
        return _state.value.userOptions
    }
    
    fun getLastUpdated(): Long {
        return _state.value.lastUpdated
    }
}

@Composable
fun rememberSelectionStore(): SelectionStore {
    return remember { SelectionStore.getInstance() }
}