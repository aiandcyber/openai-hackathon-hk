package com.aiforseniors.scamshield

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Caregiver / setup only — not the senior home screen.
 * WhatsApp itself does not appear in Notification access; enable
 * “AI For Seniors — WhatsApp protection” so this app can read WhatsApp alerts.
 */
class SettingsActivity : ComponentActivity() {
    private var listenerOn by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        listenerOn = isNotificationListenerEnabled()

        setContent {
            SeniorTheme {
                Surface(Modifier.fillMaxSize(), color = BrandBg) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = BrandInk,
                                    modifier = Modifier.size(34.dp),
                                )
                            }
                            Text(
                                "Settings",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandInk,
                            )
                        }

                        Text(
                            "WhatsApp will not show as its own row here.\n" +
                                "Turn ON this app so it can watch WhatsApp messages:",
                            fontSize = 22.sp,
                            color = BrandInk,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = if (listenerOn) {
                                "Protection: ON"
                            } else {
                                "Protection: OFF — please enable below"
                            },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (listenerOn) BrandOk else BrandWarn,
                        )

                        SettingsAction(
                            icon = Icons.Filled.NotificationsActive,
                            label = "Open WhatsApp protection",
                            detail = "Find “AI For Seniors — WhatsApp protection” and turn it ON",
                            onClick = {
                                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
                        )
                        SettingsAction(
                            icon = Icons.Filled.PhonelinkLock,
                            label = "App permissions",
                            detail = "Microphone & Phone for calling family",
                            onClick = {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        android.net.Uri.fromParts("package", packageName, null),
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        listenerOn = isNotificationListenerEnabled()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?: return false
        if (flat.isEmpty()) return false
        val cn = ComponentName(this, WhatsAppNotificationListener::class.java)
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(flat)
        while (splitter.hasNext()) {
            val component = ComponentName.unflattenFromString(splitter.next())
            if (component != null && component == cn) return true
        }
        return false
    }
}

@androidx.compose.runtime.Composable
private fun SettingsAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandGreen,
            contentColor = Color.White,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    label,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    detail,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.92f),
                    lineHeight = 24.sp,
                )
            }
        }
    }
}
