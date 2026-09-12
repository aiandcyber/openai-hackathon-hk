package com.aiforseniors.scamshield

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var listening by mutableStateOf(false)
    private var activeCall by mutableStateOf<Vip?>(null)
    private var speech: SpeechRecognizer? = null
    private val vips: List<Vip> = DefaultVips.list
    private val listenHandler = Handler(Looper.getMainLooper())
    private var wantContinuousListen = true

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted[Manifest.permission.RECORD_AUDIO] == true) {
            scheduleListen(delayMs = 300)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRuntimePermissionsIfNeeded()

        setContent {
            SeniorTheme {
                Surface(Modifier.fillMaxSize(), color = BrandBg) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HeaderRow(
                            onSettings = {
                                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                            },
                        )

                        StatusCard(Modifier = Modifier.weight(1f))

                        MessageGuardianCard(on = true, modifier = Modifier.weight(1f))

                        if (activeCall == null) {
                            CallSomeoneCard(
                                listening = listening,
                                names = vips.joinToString(" · ") { it.name },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            CallingCard(
                                vip = activeCall!!,
                                onEnd = { endCall() },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        wantContinuousListen = activeCall == null
        if (wantContinuousListen) scheduleListen(delayMs = 400)
    }

    override fun onPause() {
        wantContinuousListen = false
        stopListening()
        super.onPause()
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
        if (need.isNotEmpty()) permissionLauncher.launch(need.toTypedArray())
    }

    private fun scheduleListen(delayMs: Long = 600) {
        listenHandler.removeCallbacksAndMessages(null)
        listenHandler.postDelayed({ startContinuousListening() }, delayMs)
    }

    private fun stopListening() {
        listenHandler.removeCallbacksAndMessages(null)
        try {
            speech?.cancel()
            speech?.destroy()
        } catch (_: Exception) {
        }
        speech = null
        listening = false
    }

    /** Always-on mic loop: keeps restarting until a “Call &lt;name&gt;” match. */
    private fun startContinuousListening() {
        if (!wantContinuousListen || activeCall != null) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestRuntimePermissionsIfNeeded()
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            listening = false
            return
        }

        try {
            speech?.cancel()
            speech?.destroy()
        } catch (_: Exception) {
        }

        listening = true
        speech = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                }

                override fun onResults(results: Bundle?) {
                    val heardList = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                    Log.d(TAG, "heard: $heardList")
                    val matched = heardList.any { handleVoiceCommand(it) }
                    if (!matched && wantContinuousListen && activeCall == null) {
                        scheduleListen(delayMs = 400)
                    }
                }

                override fun onError(error: Int) {
                    // Timeouts / no-match are normal in always-listen mode — just restart.
                    Log.d(TAG, "speech error $error")
                    if (wantContinuousListen && activeCall == null) {
                        scheduleListen(delayMs = 500)
                    } else {
                        listening = false
                    }
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
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            try {
                sr.startListening(intent)
            } catch (e: Exception) {
                Log.w(TAG, "startListening failed", e)
                scheduleListen(delayMs = 1000)
            }
        }
    }

    /** @return true if a call was started */
    private fun handleVoiceCommand(heard: String): Boolean {
        val normalized = heard.lowercase().replace(Regex("[^a-z0-9\\s+]"), " ").replace(Regex("\\s+"), " ").trim()
        // "call Martin", "calling Martin", "phone Martin", "dial Martin"
        val callMatch = Regex("""\b(?:call(?:ing)?|phone|dial(?:ling|ing)?|ring)\s+(?:to\s+)?(.+)$""")
            .find(normalized) ?: return false
        val namePart = callMatch.groupValues.getOrNull(1)?.trim().orEmpty()
        if (namePart.isBlank()) return false

        val vip = matchVip(namePart)
        if (vip == null) {
            Toast.makeText(this, "No match for $namePart", Toast.LENGTH_SHORT).show()
            return false
        }
        placeCall(vip)
        return true
    }

    private fun matchVip(spoken: String): Vip? {
        val cleaned = spoken.lowercase().trim()
        val aliases = mapOf(
            "martin" to listOf("martin", "martyn", "martine", "marten"),
            "amir" to listOf("amir", "ameer", "ameir", "emir"),
            "tim" to listOf("tim", "timothy"),
        )
        return vips.firstOrNull { vip ->
            val keys = aliases[vip.id] ?: listOf(vip.name.lowercase())
            keys.any { key ->
                cleaned == key || cleaned.contains(key) ||
                    cleaned.split(" ").any { token -> token == key }
            }
        }
    }

    private fun placeCall(vip: Vip) {
        val cleaned = vip.phone.filter { it.isDigit() || it == '+' }
        if (cleaned.isEmpty()) {
            Toast.makeText(this, "No number for ${vip.name}", Toast.LENGTH_LONG).show()
            return
        }
        wantContinuousListen = false
        stopListening()
        val uri = Uri.parse("tel:$cleaned")
        val dial = Intent(Intent.ACTION_DIAL, uri)
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    startActivity(Intent(Intent.ACTION_CALL, uri))
                    activeCall = vip
                    return
                } catch (e: Exception) {
                    Log.w(TAG, "ACTION_CALL failed, opening dialer", e)
                }
            } else {
                requestRuntimePermissionsIfNeeded()
            }
            startActivity(dial)
            activeCall = vip
        } catch (e: Exception) {
            Log.e(TAG, "cannot start call", e)
            Toast.makeText(this, "Could not call ${vip.name}. Check Phone permission.", Toast.LENGTH_LONG).show()
            activeCall = null
            wantContinuousListen = true
            scheduleListen(delayMs = 500)
        }
    }

    private fun endCall() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val tm = getSystemService(TELECOM_SERVICE) as TelecomManager
                @Suppress("DEPRECATION")
                tm.endCall()
            }
        } catch (_: Exception) {
            // UI still resets
        }
        activeCall = null
        wantContinuousListen = true
        scheduleListen(delayMs = 500)
    }

    override fun onDestroy() {
        wantContinuousListen = false
        stopListening()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CallListen"
    }
}

@androidx.compose.runtime.Composable
private fun HeaderRow(onSettings: () -> Unit) {
    val date = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH))
        .uppercase(Locale.ENGLISH)
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = date,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandMuted,
                letterSpacing = 0.6.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$greeting, Mary",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = BrandInk,
                lineHeight = 46.sp,
            )
        }
        IconButton(onClick = onSettings) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = BrandMuted,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun StatusCard(modifier: Modifier = Modifier) {
    HomeCard(modifier = modifier) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIcon(Icons.Filled.Shield, filled = false)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("You're protected", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BrandInk)
                Spacer(Modifier.height(4.dp))
                Text(
                    "AI For Seniors is watching with you.",
                    fontSize = 22.sp,
                    color = BrandMuted,
                    lineHeight = 28.sp,
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MessageGuardianCard(on: Boolean, modifier: Modifier = Modifier) {
    HomeCard(modifier = modifier) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SoftSquareIcon(Icons.Filled.ChatBubble)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Message Guardian", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BrandInk)
                Spacer(Modifier.height(4.dp))
                Text("Watching WhatsApp", fontSize = 22.sp, color = BrandMuted)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(BrandMint)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = if (on) "On" else "Off",
                    color = BrandGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun CallSomeoneCard(
    listening: Boolean,
    names: String,
    modifier: Modifier = Modifier,
) {
    HomeCard(modifier = modifier) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIcon(Icons.Filled.Mic, filled = true)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (listening) "Listening…" else "Call Someone",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandInk,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Say “Call” then a name",
                    fontSize = 22.sp,
                    color = BrandMuted,
                    lineHeight = 28.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Family: $names",
                    fontSize = 20.sp,
                    color = BrandMuted,
                    lineHeight = 26.sp,
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun CallingCard(
    vip: Vip,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeCard(
        modifier = modifier.border(2.dp, BrandGreen, RoundedCornerShape(22.dp)),
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(72.dp)
                        .border(3.dp, BrandGreen, CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(BrandGreenSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = vip.name.first().uppercaseChar().toString(),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Calling ${vip.name}…",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandInk,
                    )
                    Text(
                        text = vip.phone,
                        fontSize = 22.sp,
                        color = BrandMuted,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = seniorWarnButtonColors(),
            ) {
                Icon(Icons.Filled.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text("End call", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun HomeCard(
    modifier: Modifier = Modifier,
    content: @androidx.compose.runtime.Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(22.dp))
            .background(BrandCard)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        content()
    }
}

@androidx.compose.runtime.Composable
private fun CircleIcon(icon: ImageVector, filled: Boolean) {
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .then(
                if (filled) {
                    Modifier.background(BrandGreen)
                } else {
                    Modifier.border(3.dp, BrandGreen, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (filled) Color.White else BrandGreen,
            modifier = Modifier.size(36.dp),
        )
    }
}

@androidx.compose.runtime.Composable
private fun SoftSquareIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BrandMint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(36.dp))
    }
}
