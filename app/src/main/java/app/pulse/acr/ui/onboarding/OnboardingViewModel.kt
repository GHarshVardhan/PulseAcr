package app.pulse.acr.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import app.pulse.acr.PulseApp
import app.pulse.acr.data.settings.SettingsRepository

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    suspend fun acceptConsent() {
        settingsRepository.setConsentAccepted(true)
    }

    suspend fun finishOnboarding() {
        settingsRepository.setConsentAccepted(true)
        settingsRepository.setOnboardingDone(true)
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as PulseApp
                return OnboardingViewModel(app.container.settingsRepository) as T
            }
        }
    }
}
