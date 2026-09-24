package com.petbreath.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.petbreath.app.data.settings.AppSettings
import com.petbreath.app.ui.navigation.DeepLinkRequest
import com.petbreath.app.ui.navigation.PetBreathNavHost
import com.petbreath.app.ui.theme.PetBreathTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val deepLink = MutableStateFlow<DeepLinkRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        val settingsRepository = appContainer.settingsRepository

        setContent {
            val settings: AppSettings? by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val pendingDeepLink by deepLink.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            val current = settings
            PetBreathTheme(themeMode = current?.themeMode ?: com.petbreath.app.data.settings.ThemeMode.SYSTEM) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Wait for stored settings before choosing the start screen to avoid a flash of onboarding.
                    if (current != null) {
                        PetBreathNavHost(
                            showOnboarding = !current.disclaimerAccepted,
                            onAcceptDisclaimer = { scope.launch { settingsRepository.acceptDisclaimer() } },
                            deepLink = pendingDeepLink,
                            onDeepLinkHandled = { deepLink.value = null },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val petId = intent?.getLongExtra(EXTRA_PET_ID, -1L) ?: -1L
        val destination = intent?.getStringExtra(EXTRA_DESTINATION)
        if (petId > 0 && destination != null) deepLink.value = DeepLinkRequest(petId, destination)
    }

    companion object {
        const val EXTRA_PET_ID = "com.petbreath.app.extra.PET_ID"
        const val EXTRA_DESTINATION = "com.petbreath.app.extra.DESTINATION"
        const val DESTINATION_MEASURE = "measure"
        const val DESTINATION_MEDICATIONS = "medications"
    }
}
