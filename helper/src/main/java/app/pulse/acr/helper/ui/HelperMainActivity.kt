package app.pulse.acr.helper.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.pulse.acr.helper.service.CallAccessibilityService

class HelperMainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF0F766E),
                    background = Color(0xFFF4EFE6)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HelperHome(
                        accessibilityEnabled = CallAccessibilityService.isEnabled(this),
                        onOpenAccessibility = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onRequestPermissions = {
                            val perms = buildList {
                                add(Manifest.permission.RECORD_AUDIO)
                                add(Manifest.permission.READ_PHONE_STATE)
                                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                                if (Build.VERSION.SDK_INT >= 28) add(Manifest.permission.READ_CALL_LOG)
                            }.toTypedArray()
                            permissionLauncher.launch(perms)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HelperHome(
    accessibilityEnabled: Boolean,
    onOpenAccessibility: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Pulse ACR Helper", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Companion service that watches phone and VoIP call screens, then records with the microphone while a call is active.",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            if (accessibilityEnabled) "Accessibility: enabled" else "Accessibility: off",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Button(onClick = onOpenAccessibility, modifier = Modifier.fillMaxWidth()) {
            Text("Enable Accessibility service")
        }
        OutlinedButton(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
            Text("Grant mic & phone permissions")
        }
    }
}
