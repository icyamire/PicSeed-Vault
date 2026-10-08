import com.icyamire.valut.viewmodel.ValutViewModel
import com.icyamire.valut.viewmodel.WizardStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValutViewModelTest {

    @Test
    fun testInitialState() {
        val viewModel = ValutViewModel()
        val state = viewModel.uiState.value

        assertEquals(WizardStep.MEDIA_SELECTION, state.currentStep)
        assertTrue(state.selectedFiles.isEmpty())
        assertEquals("", state.pin)
        assertEquals("", state.confirmPin)
        assertEquals(24, state.mnemonicWordCount)
        assertNull(state.cryptoResult)
    }

    @Test
    fun testPinValidation() {
        val viewModel = ValutViewModel()

        viewModel.setPin("123")
        assertFalse(viewModel.isPinValid())

        viewModel.setPin("123456")
        viewModel.setConfirmPin("12345")
        assertFalse(viewModel.isPinValid())

        viewModel.setConfirmPin("123456")
        assertTrue(viewModel.isPinValid())
        assertNull(viewModel.uiState.value.pinError)
    }

    @Test
    fun testMnemonicWordCount() {
        val viewModel = ValutViewModel()
        assertEquals(24, viewModel.uiState.value.mnemonicWordCount)

        viewModel.setMnemonicWordCount(12)
        assertEquals(12, viewModel.uiState.value.mnemonicWordCount)

        viewModel.setMnemonicWordCount(24)
        assertEquals(24, viewModel.uiState.value.mnemonicWordCount)
    }

    @Test
    fun testResetWizard() {
        val viewModel = ValutViewModel()
        viewModel.setPin("123456")
        viewModel.setConfirmPin("123456")
        viewModel.setMnemonicWordCount(12)

        viewModel.resetWizard()

        val state = viewModel.uiState.value
        assertEquals(WizardStep.MEDIA_SELECTION, state.currentStep)
        assertTrue(state.selectedFiles.isEmpty())
        assertEquals("", state.pin)
        assertEquals("", state.confirmPin)
        assertEquals(24, state.mnemonicWordCount)
        assertNull(state.cryptoResult)
    }
}
