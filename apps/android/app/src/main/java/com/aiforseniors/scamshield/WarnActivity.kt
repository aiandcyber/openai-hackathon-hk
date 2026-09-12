package com.aiforseniors.scamshield

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class WarnActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var pendingSpeak: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val warning = intent.getStringExtra(EXTRA_WARNING).orEmpty()
        val reason = intent.getStringExtra(EXTRA_REASON).orEmpty()
        val source = intent.getStringExtra(EXTRA_SOURCE_TEXT).orEmpty()
        pendingSpeak = warning
        tts = TextToSpeech(this, this)

        setContent {
            WarnScreen(
                warning = warning,
                reason = reason,
                source = source,
                onDismiss = { finish() },
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            pendingSpeak?.let { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "warn") }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_WARNING = "warning"
        const val EXTRA_REASON = "reason"
        const val EXTRA_SOURCE_TEXT = "source_text"
    }
}

@Composable
private fun WarnScreen(
    warning: String,
    reason: String,
    source: String,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFB91C1C))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "SCAM ALERT",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = warning.ifBlank { "Do not tap links or share codes. Call your family." },
            color = Color.White,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
        )
        if (reason.isNotBlank()) {
            Text(text = reason, color = Color(0xFFFFE4E6), fontSize = 16.sp, textAlign = TextAlign.Center)
        }
        if (source.isNotBlank()) {
            Text(text = source, color = Color(0xFFFFE4E6), fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFB91C1C)),
        ) {
            Text("I understand", fontSize = 18.sp)
        }
    }
}
