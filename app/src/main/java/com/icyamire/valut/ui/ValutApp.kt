package com.icyamire.valut.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.icyamire.valut.ui.navigation.NavRoute
import com.icyamire.valut.ui.screens.MediaSelectionScreen
import com.icyamire.valut.ui.screens.PinSetupScreen
import com.icyamire.valut.ui.screens.ProcessingScreen
import com.icyamire.valut.ui.screens.ResultScreen
import com.icyamire.valut.viewmodel.ValutViewModel
import com.icyamire.valut.viewmodel.WizardStep

@Composable
fun ValutApp(
    viewModel: ValutViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(NavRoute.MediaSelection)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            },
        ) { key ->
            NavEntry(key) {
                when (key) {
                    is NavRoute.MediaSelection -> {
                        viewModel.setStep(WizardStep.MEDIA_SELECTION)
                        MediaSelectionScreen(
                            viewModel = viewModel,
                            selectedFiles = uiState.selectedFiles,
                            onNextStep = {
                                viewModel.setStep(WizardStep.PIN_SETUP)
                                backStack.add(NavRoute.PinSetup)
                            },
                        )
                    }

                    is NavRoute.PinSetup -> {
                        viewModel.setStep(WizardStep.PIN_SETUP)
                        PinSetupScreen(
                            viewModel = viewModel,
                            pin = uiState.pin,
                            confirmPin = uiState.confirmPin,
                            isPinVisible = uiState.isPinVisible,
                            isConfirmPinVisible = uiState.isConfirmPinVisible,
                            mnemonicWordCount = uiState.mnemonicWordCount,
                            pinError = uiState.pinError,
                            onBack = {
                                viewModel.setStep(WizardStep.MEDIA_SELECTION)
                                if (backStack.size > 1) {
                                    backStack.removeAt(backStack.lastIndex)
                                }
                            },
                            onGenerate = {
                                viewModel.setStep(WizardStep.PROCESSING)
                                backStack.add(NavRoute.Processing)
                            },
                        )
                    }

                    is NavRoute.Processing -> {
                        viewModel.setStep(WizardStep.PROCESSING)
                        ProcessingScreen(
                            viewModel = viewModel,
                            isProcessing = uiState.isProcessing,
                            progress = uiState.processingProgress,
                            statusText = uiState.processingStatus,
                            errorText = uiState.processingError,
                            cryptoResult = uiState.cryptoResult,
                            onComplete = {
                                if (backStack.lastOrNull() is NavRoute.Processing) {
                                    viewModel.setStep(WizardStep.RESULT)
                                    backStack.add(NavRoute.Result)
                                }
                            },
                        )
                    }

                    is NavRoute.Result -> {
                        viewModel.setStep(WizardStep.RESULT)
                        ResultScreen(
                            cryptoResult = uiState.cryptoResult,
                            selectedFiles = uiState.selectedFiles,
                            pinLength = uiState.pin.length,
                            mnemonicWordCount = uiState.mnemonicWordCount,
                            onRestart = {
                                viewModel.resetWizard()
                                while (backStack.size > 1) {
                                    backStack.removeAt(backStack.lastIndex)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
