package com.aiforseniors.scamshield

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhonelinkLock
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
        listenerOn = isNotificationListenerEnabled()

        setContent {
            SeniorTheme {
                Surface(Modifier.fillMaxSize(), color = BrandBg) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = BrandNavy,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                            Text(
                                "Settings",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy,
                            )
                        }

                        Text(
                            "WhatsApp will not show as its own row here.\n" +
                                "Turn ON this app so it can watch WhatsApp messages:",
                            fontSize = 20.sp,
                            color = BrandInk,
                            lineHeight = 28.sp,
                        )
                        Text(
                            text = if (listenerOn) {
                                "Protection: ON"
                            } else {
                                "Protection: OFF — please enable below"
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
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
            .height(96.dp),
        shape = RoundedCornerShape(18.dp),
        colors = seniorPrimaryButtonColors(),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(detail, fontSize = 16.sp)
            }
        }
    }
}
