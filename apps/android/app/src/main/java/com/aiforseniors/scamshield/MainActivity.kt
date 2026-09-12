package com.aiforseniors.scamshield

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private var status by mutableStateOf("Ready")
    private var vipsText by mutableStateOf("(not loaded)")
    private var apiUrl by mutableStateOf(BuildConfig.API_BASE_URL)
    private var lastHeard by mutableStateOf("")
    private var testText by mutableStateOf(
        "URGENT: Your HSBC account will be locked. Send OTP now.",
    )
    private var speech: SpeechRecognizer? = null
    private var cachedVips: List<Vip> = emptyList()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        status = if (granted.values.all { it }) "Permissions OK" else "Grant mic + phone permissions"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiUrl = BuildConfig.API_BASE_URL

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Scam Shield", style = MaterialTheme.typography.headlineMedium)
                        Text("API: $apiUrl", style = MaterialTheme.typography.bodySmall)
                        Text(status, style = MaterialTheme.typography.bodyMedium)
                        if (lastHeard.isNotBlank()) {
                            Text("Heard: $lastHeard", style = MaterialTheme.typography.bodySmall)
                        }

                        Button(onClick = { openNotificationAccess() }, Modifier.fillMaxWidth()) {
                            Text("1. Enable Notification access")
                        }
                        Button(onClick = { requestRuntimePermissions() }, Modifier.fillMaxWidth()) {
                            Text("2. Grant mic + call permissions")
                        }
                        Button(onClick = { refreshVips() }, Modifier.fillMaxWidth()) {
                            Text("3. Load VIP list from API")
                        }
                        Text(vipsText, style = MaterialTheme.typography.bodySmall)

                        Button(onClick = { startCallVipListening() }, Modifier.fillMaxWidth()) {
                            Text("4. Say “Call [VIP name]”")
                        }

                        OutlinedTextField(
                            value = testText,
                            onValueChange = { testText = it },
                            label = { Text("Test classify text") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                        )
                        Button(onClick = { runTestClassify() }, Modifier.fillMaxWidth()) {
                            Text("Test classify (no WhatsApp needed)")
                        }
                    }
                }
            }
        }
    }

    private fun openNotificationAccess() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        status = "Turn on Scam Shield in Notification access"
    }

    private fun requestRuntimePermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.POST_NOTIFICATIONS,
            ),
        )
    }

    private fun refreshVips() {
        status = "Loading VIPs…"
        thread {
            try {
                val list = (application as ScamShieldApp).api.listVips()
                cachedVips = list
                runOnUiThread {
                    vipsText = if (list.isEmpty()) {
                        "No VIPs yet — add some on /caregiver"
                    } else {
                        list.joinToString("\n") { "${it.name} — ${it.phone}" }
                    }
                    status = "Loaded ${list.size} VIP(s)"
                }
            } catch (e: Exception) {
                runOnUiThread { status = "VIP load failed: ${e.message}" }
            }
        }
    }

    private fun runTestClassify() {
        status = "Classifying…"
        thread {
            try {
                val result = (application as ScamShieldApp).api.classifyScam(testText, "manual")
                runOnUiThread {
                    status = "mode=${result.mode} isScam=${result.isScam}"
                    if (result.isScam) {
                        startActivity(
                            Intent(this, WarnActivity::class.java).apply {
                                putExtra(WarnActivity.EXTRA_WARNING, result.seniorWarning)
                                putExtra(WarnActivity.EXTRA_REASON, result.reason)
                                putExtra(WarnActivity.EXTRA_SOURCE_TEXT, testText)
                            },
                        )
                    } else {
                        Toast.makeText(this, "Not flagged as scam", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { status = "Classify failed: ${e.message}" }
            }
        }
    }

    private fun startCallVipListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestRuntimePermissions()
            return
        }
        if (cachedVips.isEmpty()) {
            refreshVips()
            status = "Load VIPs first, then try again"
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status = "Speech recognition not available on this device"
            return
        }
        speech?.destroy()
        speech = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    status = "Listening… say Call [name]"
                }

                override fun onResults(results: Bundle?) {
                    val heard = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    lastHeard = heard
                    handleVoiceCommand(heard)
                }

                override fun onError(error: Int) {
                    status = "Speech error $error — tap again"
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            sr.startListening(intent)
        }
    }

    private fun handleVoiceCommand(heard: String) {
        val normalized = heard.lowercase().trim()
        val callMatch = Regex("""\bcall\s+(.+)$""").find(normalized)
        val namePart = callMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (namePart.isEmpty()) {
            status = "Say “Call” then a VIP name"
            return
        }
        val vip = cachedVips.firstOrNull { vip ->
            val n = vip.name.lowercase()
            namePart.contains(n) || n.contains(namePart) ||
                namePart.split(Regex("\\s+")).any { token -> n.contains(token) && token.length > 2 }
        }
        if (vip == null) {
            status = "No VIP matched “$namePart”"
            return
        }
        status = "Calling ${vip.name}…"
        placeCall(vip.phone)
    }

    private fun placeCall(phone: String) {
        val cleaned = phone.filter { it.isDigit() || it == '+' }
        if (cleaned.isEmpty()) {
            status = "VIP has empty phone"
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Fall back to dialer UI without CALL_PHONE
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")))
            return
        }
        startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleaned")))
    }

    override fun onDestroy() {
        speech?.destroy()
        super.onDestroy()
    }
}
