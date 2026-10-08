package com.icyamire.valut.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icyamire.valut.crypto.DeterministicSeedEngine
import com.icyamire.valut.model.CryptoResult
import com.icyamire.valut.model.SelectedMediaFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class WizardStep {
    MEDIA_SELECTION,
    PIN_SETUP,
    PROCESSING,
    RESULT
}

data class ValutUiState(
    val currentStep: WizardStep = WizardStep.MEDIA_SELECTION,
    val selectedFiles: List<SelectedMediaFile> = emptyList(),
    val pin: String = "",
    val confirmPin: String = "",
    val isPinVisible: Boolean = false,
    val isConfirmPinVisible: Boolean = false,
    val mnemonicWordCount: Int = 24, // 12 or 24
    val pinError: String? = null,
    val isProcessing: Boolean = false,
    val processingStatus: String = "",
    val processingProgress: Float = 0f,
    val cryptoResult: CryptoResult? = null,
    val processingError: String? = null
)

class ValutViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ValutUiState())
    val uiState: StateFlow<ValutUiState> = _uiState.asStateFlow()

    private val seedEngine = DeterministicSeedEngine()

    fun addFilesFromUris(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val newFiles = uris.map { uri ->
            getMediaFileFromUri(context, uri)
        }
        _uiState.update { state ->
            val existingUris = state.selectedFiles.map { it.uri }.toSet()
            val filteredNew = newFiles.filter { it.uri !in existingUris }
            state.copy(selectedFiles = state.selectedFiles + filteredNew)
        }
    }

    fun removeFile(index: Int) {
        _uiState.update { state ->
            if (index in state.selectedFiles.indices) {
                val updated = state.selectedFiles.toMutableList()
                updated.removeAt(index)
                state.copy(selectedFiles = updated)
            } else state
        }
    }

    fun moveFileUp(index: Int) {
        if (index <= 0) return
        _uiState.update { state ->
            if (index in state.selectedFiles.indices) {
                val updated = state.selectedFiles.toMutableList()
                val temp = updated[index]
                updated[index] = updated[index - 1]
                updated[index - 1] = temp
                state.copy(selectedFiles = updated)
            } else state
        }
    }

    fun moveFileDown(index: Int) {
        _uiState.update { state ->
            if (index in 0 until state.selectedFiles.lastIndex) {
                val updated = state.selectedFiles.toMutableList()
                val temp = updated[index]
                updated[index] = updated[index + 1]
                updated[index + 1] = temp
                state.copy(selectedFiles = updated)
            } else state
        }
    }

    fun clearAllFiles() {
        _uiState.update { it.copy(selectedFiles = emptyList()) }
    }

    fun setPin(pin: String) {
        _uiState.update {
            val newState = it.copy(pin = pin, pinError = null)
            validatePins(newState)
        }
    }

    fun setConfirmPin(confirmPin: String) {
        _uiState.update {
            val newState = it.copy(confirmPin = confirmPin, pinError = null)
            validatePins(newState)
        }
    }

    fun togglePinVisibility() {
        _uiState.update { it.copy(isPinVisible = !it.isPinVisible) }
    }

    fun toggleConfirmPinVisibility() {
        _uiState.update { it.copy(isConfirmPinVisible = !it.isConfirmPinVisible) }
    }

    fun setMnemonicWordCount(count: Int) {
        require(count == 12 || count == 24)
        _uiState.update { it.copy(mnemonicWordCount = count) }
    }

    private fun validatePins(state: ValutUiState): ValutUiState {
        var error: String? = null
        if (state.pin.isNotEmpty() && state.pin.length < 4) {
            error = "PIN must be at least 4 digits/characters"
        } else if (state.confirmPin.isNotEmpty() && state.pin != state.confirmPin) {
            error = "PINs do not match"
        }
        return state.copy(pinError = error)
    }

    fun isPinValid(): Boolean {
        val state = _uiState.value
        return state.pin.length >= 4 && state.pin == state.confirmPin
    }

    fun startSeedProcessing(context: Context, onComplete: () -> Unit) {
        val state = _uiState.value
        if (state.isProcessing) return

        if (state.selectedFiles.isEmpty()) {
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    processingError = "No media files selected.",
                    processingStatus = "Error encountered"
                )
            }
            return
        }

        if (!isPinValid()) {
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    processingError = "Invalid PIN. Minimum 4 digits required and PINs must match.",
                    processingStatus = "Error encountered"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processingProgress = 0.1f,
                    processingStatus = "Reading file streams...",
                    processingError = null,
                    currentStep = WizardStep.PROCESSING
                )
            }

            try {
                delay(300)
                _uiState.update {
                    it.copy(
                        processingProgress = 0.4f,
                        processingStatus = "Computing SHA-256 & PBKDF2 hash..."
                    )
                }

                val uris = state.selectedFiles.map { Uri.parse(it.uri) }
                val pin = state.pin
                val wordCount = state.mnemonicWordCount

                val result = withContext(Dispatchers.IO) {
                    seedEngine.generateFromUris(
                        context = context.applicationContext,
                        uris = uris,
                        pin = pin,
                        mnemonicWordCount = wordCount
                    )
                }

                _uiState.update {
                    it.copy(
                        processingProgress = 0.8f,
                        processingStatus = "Deriving multi-chain keys (BTC, ETH, XMR)..."
                    )
                }
                delay(300)

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingProgress = 1.0f,
                        processingStatus = "Done!",
                        cryptoResult = result,
                        currentStep = WizardStep.RESULT
                    )
                }
                onComplete()
            } catch (e: Exception) {
                android.util.Log.e("ValutViewModel", "Seed generation error", e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingError = e.localizedMessage ?: "Failed to derive keys from selected files.",
                        processingStatus = "Error encountered"
                    )
                }
            }
        }
    }

    fun setStep(step: WizardStep) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun resetWizard() {
        _uiState.update { ValutUiState() }
    }

    private fun getMediaFileFromUri(context: Context, uri: Uri): SelectedMediaFile {
        var name = "File_${System.currentTimeMillis()}"
        var size = 0L
        val mimeType = context.contentResolver.getType(uri)

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { name = it }
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) { }

        return SelectedMediaFile(
            uri = uri.toString(),
            name = name,
            size = size,
            mimeType = mimeType
        )
    }
}
