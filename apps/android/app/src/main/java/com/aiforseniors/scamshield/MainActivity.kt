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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private var status by mutableStateOf("Ready")
    private var vipsText by mutableStateOf("Family contacts not loaded yet")
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
        status = if (granted.values.all { it }) {
            "Permissions are ready"
        } else {
            "Please allow Microphone and Phone"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiUrl = BuildConfig.API_BASE_URL

        setContent {
            SeniorTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BrandBg) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher),
                            contentDescription = "AI For Seniors",
                            modifier = Modifier
                                .size(112.dp)
                                .clip(RoundedCornerShape(28.dp)),
                        )
                        Text(
                            text = "AI For Seniors",
                            style = MaterialTheme.typography.displayLarge,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Message safety & family calls",
                            style = MaterialTheme.typography.bodyLarge,
                            color = BrandMuted,
                            textAlign = TextAlign.Center,
                        )

                        StatusCard(status = status, heard = lastHeard)

                        SectionLabel("Setup")
                        BigActionButton("1. Turn on message protection", onClick = { openNotificationAccess() })
                        BigActionButton("2. Allow microphone & phone", onClick = { requestRuntimePermissions() })
                        BigActionButton("3. Load family contacts", onClick = { refreshVips() })

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(BrandCard)
                                .padding(20.dp),
                        ) {
                            Text(vipsText, style = MaterialTheme.typography.bodyLarge)
                        }

                        SectionLabel("Call family")
                        BigActionButton(
                            "4. Say “Call” + name",
                            onClick = { startCallVipListening() },
                            primary = false,
                        )

                        SectionLabel("Safety check")
                        OutlinedTextField(
                            value = testText,
                            onValueChange = { testText = it },
                            label = { Text("Message to check", fontSize = 20.sp) },
                            textStyle = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandTeal,
                                unfocusedBorderColor = BrandMuted,
                                focusedLabelColor = BrandTeal,
                                cursorColor = BrandTeal,
                            ),
                        )
                        BigActionButton("Check this message", onClick = { runTestClassify() })

                        Text(
                            text = "Connected to care service",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandMuted,
                        )
                        // Keep API URL tiny for debugging without cluttering senior UI
                        Text(
                            text = apiUrl,
                            fontSize = 14.sp,
                            color = BrandMuted.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    private fun openNotificationAccess() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        status = "Find “AI For Seniors” and turn it ON"
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
        status = "Loading family contacts…"
        thread {
            try {
                val list = (application as ScamShieldApp).api.listVips()
                cachedVips = list
                runOnUiThread {
                    vipsText = if (list.isEmpty()) {
                        "No family contacts yet.\nAsk your caregiver to add them."
                    } else {
                        list.joinToString("\n\n") { "• ${it.name}\n  ${it.phone}" }
                    }
                    status = "Loaded ${list.size} family contact(s)"
                }
            } catch (e: Exception) {
                runOnUiThread { status = "Could not load contacts.\n${e.message}" }
            }
        }
    }

    private fun runTestClassify() {
        status = "Checking message…"
        thread {
            try {
                val result = (application as ScamShieldApp).api.classifyScam(testText, "manual")
                runOnUiThread {
                    status = if (result.isScam) {
                        "This message looks unsafe"
                    } else {
                        "This message looks okay"
                    }
                    if (result.isScam) {
                        startActivity(
                            Intent(this, WarnActivity::class.java).apply {
                                putExtra(WarnActivity.EXTRA_WARNING, result.seniorWarning)
                                putExtra(WarnActivity.EXTRA_REASON, result.reason)
                                putExtra(WarnActivity.EXTRA_SOURCE_TEXT, testText)
                            },
                        )
                    } else {
                        Toast.makeText(this, "Not flagged as unsafe", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { status = "Check failed.\n${e.message}" }
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
            status = "Load family contacts first, then try again"
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status = "Voice calling is not available on this phone"
            return
        }
        speech?.destroy()
        speech = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    status = "Listening… say Call, then a name"
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
                    status = "Didn’t catch that — tap and try again"
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
            status = "Please say “Call” then a name"
            return
        }
        val vip = cachedVips.firstOrNull { vip ->
            val n = vip.name.lowercase()
            namePart.contains(n) || n.contains(namePart) ||
                namePart.split(Regex("\\s+")).any { token -> n.contains(token) && token.length > 2 }
        }
        if (vip == null) {
            status = "No match for “$namePart”"
            return
        }
        status = "Calling ${vip.name}…"
        placeCall(vip.phone)
    }

    private fun placeCall(phone: String) {
        val cleaned = phone.filter { it.isDigit() || it == '+' }
        if (cleaned.isEmpty()) {
            status = "That contact has no phone number"
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
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

@androidx.compose.runtime.Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        fontWeight = FontWeight.Bold,
    )
}

@androidx.compose.runtime.Composable
private fun StatusCard(status: String, heard: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BrandCard)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Status", style = MaterialTheme.typography.titleLarge, color = BrandTeal)
        Text(status, style = MaterialTheme.typography.bodyLarge)
        if (heard.isNotBlank()) {
            Text("You said: $heard", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@androidx.compose.runtime.Composable
private fun BigActionButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(SeniorButtonHeight),
        shape = RoundedCornerShape(18.dp),
        colors = if (primary) seniorPrimaryButtonColors() else seniorSecondaryButtonColors(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}
