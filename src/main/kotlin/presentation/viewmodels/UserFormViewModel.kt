package presentation.viewmodels

import data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import core.services.UserApiService

class UserFormViewModel(private val userApiService: UserApiService, private val scope: CoroutineScope) {
    
    private val _uiState = MutableStateFlow(UserFormUiState())
    val uiState: StateFlow<UserFormUiState> = _uiState.asStateFlow()
    
    private val _agentOptions = MutableStateFlow<List<AgentOption>>(emptyList())
    val agentOptions: StateFlow<List<AgentOption>> = _agentOptions.asStateFlow()
    
    init {
        loadAgentOptions()
    }
    
    fun loadAgentOptions() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            userApiService.getAgentList()
                .onSuccess { response ->
                    if (response.success && response.data != null) {
                        _agentOptions.value = response.data.map { agent ->
                            AgentOption(value = agent.id, label = agent.name)
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to load agents: ${error.message}"
                    )
                }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    fun createUser(userData: UserFormData, userType: UserType) {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            userApiService.createUser(userData, userType)
                .onSuccess { response ->
                    if (response.success) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "User created successfully",
                            shouldClose = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = response.message ?: "Failed to create user"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to create user: ${error.message}"
                    )
                }
        }
    }
    
    fun updateUser(userId: String, userData: UserFormData) {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            userApiService.updateUser(userId, userData)
                .onSuccess { response ->
                    if (response.success) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "User updated successfully",
                            shouldClose = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = response.message ?: "Failed to update user"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to update user: ${error.message}"
                    )
                }
        }
    }
    
    fun deleteUser(userId: String) {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            userApiService.deleteUser(userId)
                .onSuccess { response ->
                    if (response.success) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "User deleted successfully",
                            shouldClose = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = response.message ?: "Failed to delete user"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to delete user: ${error.message}"
                    )
                }
        }
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null,
            shouldClose = false
        )
    }
    
    fun resetShouldClose() {
        _uiState.value = _uiState.value.copy(shouldClose = false)
    }
}

data class UserFormUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val shouldClose: Boolean = false
)