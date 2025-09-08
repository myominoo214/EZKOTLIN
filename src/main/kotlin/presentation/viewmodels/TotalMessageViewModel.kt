package ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Base64

data class TotalMessageUiState(
    val selectedAmount: String = "1",
    val findText: String = "",
    val replaceText: String = "",
    val parseMessage: String = "",
    val errorMessage: String = "",
    val uploadedImageBase64: String? = null,
    val isProcessing: Boolean = false,
    val isError: Boolean = false,
    val isDragOver: Boolean = false,
    val userProfile: UserProfile? = null
)

class TotalMessageViewModel {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _uiState = MutableStateFlow(TotalMessageUiState())
    val uiState: StateFlow<TotalMessageUiState> = _uiState.asStateFlow()
    
    fun updateSelectedAmount(amount: String) {
        _uiState.value = _uiState.value.copy(selectedAmount = amount)
    }
    
    fun updateFindText(text: String) {
        _uiState.value = _uiState.value.copy(findText = text)
    }
    
    fun updateReplaceText(text: String) {
        _uiState.value = _uiState.value.copy(replaceText = text)
    }
    
    fun updateParseMessage(text: String) {
        _uiState.value = _uiState.value.copy(parseMessage = text)
    }
    
    fun updateErrorMessage(text: String, isError: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            errorMessage = text,
            isError = isError
        )
    }
    
    fun setDragOver(isDragOver: Boolean) {
        _uiState.value = _uiState.value.copy(isDragOver = isDragOver)
    }
    
    fun uploadImage(imageBytes: ByteArray) {
        try {
            val base64String = Base64.getEncoder().encodeToString(imageBytes)
            _uiState.value = _uiState.value.copy(
                uploadedImageBase64 = base64String,
                errorMessage = "",
                isError = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to upload image: ${e.message}",
                isError = true
            )
        }
    }
    
    fun removeImage() {
        _uiState.value = _uiState.value.copy(uploadedImageBase64 = null)
    }
    
    fun performFindReplace() {
        val currentState = _uiState.value
        if (currentState.findText.isNotEmpty()) {
            val updatedText = currentState.parseMessage.replace(
                currentState.findText,
                currentState.replaceText
            )
            _uiState.value = currentState.copy(
                parseMessage = updatedText,
                findText = "",
                replaceText = ""
            )
        }
    }
    
    fun extractTextFromImage(
        visionApiCall: suspend (String) -> Result<String>
    ) {
        val currentState = _uiState.value
        
        if (currentState.uploadedImageBase64 == null) {
            _uiState.value = currentState.copy(
                errorMessage = "Please upload an image first",
                isError = true
            )
            return
        }
        
        scope.launch {
            _uiState.value = currentState.copy(
                isProcessing = true,
                errorMessage = "Processing image...",
                isError = false
            )
            
            try {
                val result = visionApiCall(currentState.uploadedImageBase64)
                
                result.fold(
                    onSuccess = { extractedText ->
                        // Process the response data - remove lines starting with "B "
                        val lines = extractedText.split("\n")
                        val filteredLines = lines.filter { line ->
                            !line.trim().startsWith("B ")
                        }
                        val processedText = filteredLines.joinToString("\n")
                        
                        _uiState.value = _uiState.value.copy(
                            parseMessage = processedText,
                            errorMessage = "Text extracted successfully!",
                            isError = false,
                            isProcessing = false
                        )
                        
                        // Update vision count if user profile exists
                        _uiState.value.userProfile?.let { profile ->
                            _uiState.value = _uiState.value.copy(
                                userProfile = profile.copy(
                                    visionCount = profile.visionCount + 1
                                )
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Error: ${error.message ?: "Failed to extract text from image"}",
                            isError = true,
                            isProcessing = false
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error: ${e.message ?: "Failed to extract text from image"}",
                    isError = true,
                    isProcessing = false
                )
            }
        }
    }
    
    fun setUserProfile(userProfile: UserProfile) {
        _uiState.value = _uiState.value.copy(userProfile = userProfile)
    }
    
    fun resetForm() {
        _uiState.value = TotalMessageUiState(
            userProfile = _uiState.value.userProfile
        )
    }
    
    fun validateAndSubmit(): Triple<String, String, String>? {
        val currentState = _uiState.value
        
        return if (currentState.parseMessage.isNotEmpty()) {
            Triple(
                currentState.selectedAmount,
                currentState.parseMessage,
                currentState.errorMessage
            )
        } else {
            _uiState.value = currentState.copy(
                errorMessage = "Please provide a message to parse",
                isError = true
            )
            null
        }
    }
}