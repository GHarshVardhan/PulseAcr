package app.pulse.acr.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.pulse.acr.R
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory)
) {
    var step by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* continue either way; settings screen can retry */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        AnimatedContent(targetState = step, label = "onboarding") { current ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Pulse ACR",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                when (current) {
                    0 -> {
                        Text("Automatic call recorder", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Keep a private library of your calls with auto-record, search, stars, and a companion helper for stronger call detection.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { step = 1 },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Continue") }
                    }

                    1 -> {
                        Text(stringResource(R.string.consent_title), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.consent_body), style = MaterialTheme.typography.bodyLarge)
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.acceptConsent()
                                    step = 2
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("I understand") }
                    }

                    2 -> {
                        Text("Permissions", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Pulse needs microphone, phone state, and notifications to record calls automatically.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        PermissionCard("Microphone", "Capture call audio via the device mic")
                        PermissionCard("Phone", "Detect when calls start and end")
                        PermissionCard("Notifications", "Show an ongoing recording indicator")
                        Button(
                            onClick = {
                                val perms = buildList {
                                    add(Manifest.permission.RECORD_AUDIO)
                                    add(Manifest.permission.READ_PHONE_STATE)
                                    add(Manifest.permission.READ_CONTACTS)
                                    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                                    if (Build.VERSION.SDK_INT >= 28) add(Manifest.permission.READ_CALL_LOG)
                                }.toTypedArray()
                                permissionLauncher.launch(perms)
                                step = 3
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Grant permissions") }
                        TextButton(onClick = { step = 3 }) { Text("Skip for now") }
                    }

                    else -> {
                        Text("Helper app", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Install Pulse ACR Helper and enable its Accessibility service for better call detection (including many VoIP apps). You can also ignore battery optimizations for both apps.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Open Accessibility settings") }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}")
                                )
                                runCatching { context.startActivity(intent) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Disable battery optimization") }
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.finishOnboarding()
                                    onFinished()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Enter library") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
