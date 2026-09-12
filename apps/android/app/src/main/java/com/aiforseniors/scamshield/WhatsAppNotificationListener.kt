package com.aiforseniors.scamshield

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.concurrent.thread

/**
 * Reads WhatsApp notification title/text and POSTs to /api/classify-scam.
 * On isScam, opens [WarnActivity]. Requires Notification access in system settings.
 */
class WhatsAppNotificationListener : NotificationListenerService() {

    private val recent = LinkedHashMap<String, Long>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected. API=${BuildConfig.API_BASE_URL}")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            handle(sbn)
        } catch (e: Exception) {
            Log.w(TAG, "notification handle failed", e)
        }
    }

    private fun handle(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") return

        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (n.category == Notification.CATEGORY_CALL) return

        val title = extractTitle(n)
        val text = extractBody(n)
        Log.i(TAG, "whatsapp notif title='$title' text='$text'")

        if (text.isEmpty()) {
            Log.w(TAG, "empty WhatsApp body — enable message preview in WhatsApp notification settings")
            return
        }
        // Skip "Checking for new messages…" / pure typing noise
        if (text.equals("Checking for new messages", ignoreCase = true)) return

        val key = "$title|$text"
        val now = System.currentTimeMillis()
        synchronized(recent) {
            recent.entries.removeIf { now - it.value > 60_000 }
            if (recent.containsKey(key)) return
            recent[key] = now
        }

        val payload = if (title.isNotEmpty()) "$title: $text" else text
        thread(name = "classify-whatsapp") {
            try {
                val app = application as ScamShieldApp
                Log.i(TAG, "POST classify → ${BuildConfig.API_BASE_URL} payload=${payload.take(80)}")
                val result = app.api.classifyScam(payload, source = "whatsapp")
                Log.i(TAG, "classify result isScam=${result.isScam} mode=${result.mode}")
                val short = payload.replace('\n', ' ').take(80)
                ProtectionLog.record(
                    applicationContext,
                    isScam = result.isScam,
                    summary = if (result.isScam) {
                        "Warning: $short"
                    } else {
                        "Checked OK: $short"
                    },
                )
                if (result.isScam) {
                    val intent = Intent(this, WarnActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra(
                            WarnActivity.EXTRA_WARNING,
                            result.seniorWarning.ifBlank {
                                "This message looks unsafe. Do not share codes or tap links."
                            },
                        )
                        putExtra(WarnActivity.EXTRA_REASON, result.reason)
                        putExtra(WarnActivity.EXTRA_SOURCE_TEXT, payload.take(400))
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "classify failed (check API_BASE_URL / server)", e)
                try {
                    android.os.Handler(mainLooper).post {
                        Toast.makeText(
                            applicationContext,
                            "Scam check failed — cannot reach ${BuildConfig.API_BASE_URL}\nTurn off phone VPN. Same Wi-Fi as laptop.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun extractTitle(n: Notification): String {
        val extras = n.extras
        return extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
            .ifEmpty {
                extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim().orEmpty()
            }
    }

    private fun extractBody(n: Notification): String {
        val extras = n.extras
        // Prefer MessagingStyle last message (common for WhatsApp)
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
        val fromStyle = style?.messages?.lastOrNull()?.text?.toString()?.trim().orEmpty()
        if (fromStyle.isNotEmpty()) return fromStyle

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) return text

        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        if (big.isNotEmpty()) return big

        @Suppress("DEPRECATION")
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        val fromLines = lines?.mapNotNull { it?.toString()?.trim() }?.lastOrNull().orEmpty()
        if (fromLines.isNotEmpty()) return fromLines

        // Some builds stash CharSequence in EXTRA_MESSAGES bundle list
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages != null) {
            for (i in messages.indices.reversed()) {
                val b = messages[i] as? Bundle ?: continue
                val t = b.getCharSequence("text")?.toString()?.trim().orEmpty()
                if (t.isNotEmpty()) return t
            }
        }
        return ""
    }

    companion object {
        private const val TAG = "WaNotifyListener"
    }
}
