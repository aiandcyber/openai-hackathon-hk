package com.aiforseniors.scamshield

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    private var showCheckDialog by mutableStateOf(false)
    private var testText by mutableStateOf(
        "URGENT: Your HSBC account will be locked. Send OTP now.",
    )
    private var speech: SpeechRecognizer? = null
    private val vips: List<Vip> = DefaultVips.list

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* granted silently for senior home */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRuntimePermissionsIfNeeded()

        setContent {
            SeniorTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BrandBg) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Header
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "AI For Seniors",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandNavy,
                                )
                                Text(
                                    text = "Call family · Stay safe",
                                    fontSize = 18.sp,
                                    color = BrandMuted,
                                )
                            }
                            IconButton(
                                onClick = {
                                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                    tint = BrandNavy,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }

                        // Call family — main content
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                        ) {
                            Text(
                                text = "Call family",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandInk,
                            )
                            vips.forEach { vip ->
                                IconTextButton(
                                    icon = Icons.Filled.Call,
                                    label = "Call ${vip.name}",
                                    onClick = { placeCall(vip.phone) },
                                    primary = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f, fill = false)
                                        .height(72.dp),
                                )
                            }
                            IconTextButton(
                                icon = Icons.Filled.Mic,
                                label = "Say “Call” + name",
                                onClick = { startCallVipListening() },
                                primary = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp),
                            )
                        }

                        // Safety check
                        IconTextButton(
                            icon = Icons.Filled.Security,
                            label = "Check a message",
                            onClick = { showCheckDialog = true },
                            primary = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                        )
                    }

                    if (showCheckDialog) {
                        AlertDialog(
                            onDismissRequest = { showCheckDialog = false },
                            title = {
                                Text("Check a message", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            },
                            text = {
                                OutlinedTextField(
                                    value = testText,
                                    onValueChange = { testText = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    textStyle = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showCheckDialog = false
                                        runTestClassify()
                                    },
                                ) {
                                    Text("Check now", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCheckDialog = false }) {
                                    Text("Cancel", fontSize = 20.sp)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    private fun requestRuntimePermissionsIfNeeded() {
        val need = buildList {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.CALL_PHONE)
            }
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (need.isNotEmpty()) {
            permissionLauncher.launch(need.toTypedArray())
        }
    }

    private fun runTestClassify() {
        thread {
            try {
                val result = (application as ScamShieldApp).api.classifyScam(testText, "manual")
                runOnUiThread {
                    if (result.isScam) {
                        startActivity(
                            Intent(this, WarnActivity::class.java).apply {
                                putExtra(WarnActivity.EXTRA_WARNING, result.seniorWarning)
                                putExtra(WarnActivity.EXTRA_REASON, result.reason)
                                putExtra(WarnActivity.EXTRA_SOURCE_TEXT, testText)
                            },
                        )
                    } else {
                        Toast.makeText(this, "This message looks okay", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Could not check message", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startCallVipListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestRuntimePermissionsIfNeeded()
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Voice calling not available", Toast.LENGTH_LONG).show()
            return
        }
        speech?.destroy()
        speech = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Toast.makeText(this@MainActivity, "Listening…", Toast.LENGTH_SHORT).show()
                }

                override fun onResults(results: Bundle?) {
                    val heard = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    handleVoiceCommand(heard)
                }

                override fun onError(error: Int) {
                    Toast.makeText(this@MainActivity, "Please try again", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Say Call then a name", Toast.LENGTH_LONG).show()
            return
        }
        val vip = vips.firstOrNull { vip ->
            val n = vip.name.lowercase()
            namePart.contains(n) || n.contains(namePart) ||
                namePart.split(Regex("\\s+")).any { token -> n.contains(token) && token.length > 2 }
        }
        if (vip == null) {
            Toast.makeText(this, "No match for $namePart", Toast.LENGTH_LONG).show()
            return
        }
        placeCall(vip.phone)
    }

    private fun placeCall(phone: String) {
        val cleaned = phone.filter { it.isDigit() || it == '+' }
        if (cleaned.isEmpty()) return
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
private fun IconTextButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = if (primary) seniorPrimaryButtonColors() else seniorSecondaryButtonColors(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
            )
        }
    }
}
