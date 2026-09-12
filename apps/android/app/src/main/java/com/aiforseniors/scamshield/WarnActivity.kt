package com.aiforseniors.scamshield

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            SeniorTheme {
                WarnScreen(
                    warning = warning,
                    reason = reason,
                    source = source,
                    onDismiss = { finish() },
                )
            }
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
            .background(BrandWarn)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "AI For Seniors",
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Please be careful",
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 54.sp,
        )
        Text(
            text = warning.ifBlank {
                "Do not tap links or share codes. Call your family."
            },
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 30.sp,
            lineHeight = 38.sp,
            textAlign = TextAlign.Center,
        )
        if (reason.isNotBlank()) {
            Text(
                text = reason,
                color = androidx.compose.ui.graphics.Color(0xFFFFE4E6),
                fontSize = 24.sp,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (source.isNotBlank()) {
            Text(
                text = source,
                color = androidx.compose.ui.graphics.Color(0xFFFFE4E6),
                fontSize = 22.sp,
                lineHeight = 30.sp,
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
            shape = RoundedCornerShape(20.dp),
            colors = seniorWarnButtonColors(),
        ) {
            Text(
                text = "I understand",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
