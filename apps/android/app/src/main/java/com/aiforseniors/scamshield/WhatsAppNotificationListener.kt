package com.aiforseniors.scamshield

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected. API=${BuildConfig.API_BASE_URL}")
        toast("WhatsApp protection ON\nAPI ${BuildConfig.API_BASE_URL}")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            handle(sbn)
        } catch (e: Exception) {
            Log.w(TAG, "notification handle failed", e)
            toast("WhatsApp watch error: ${e.message}")
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
        val ticker = n.tickerText?.toString()?.trim().orEmpty()
        Log.i(TAG, "whatsapp notif title='$title' text='$text' ticker='$ticker'")

        val body = text.ifEmpty {
            // Fallback: "Name: message" ticker / title-only noise
            when {
                ticker.contains(':') -> ticker.substringAfter(':').trim()
                title.contains(':') -> title.substringAfter(':').trim()
                else -> ""
            }
        }

        if (body.isEmpty()) {
            Log.w(TAG, "empty WhatsApp body — enable message preview in WhatsApp notification settings")
            toast("WhatsApp seen, but text was hidden.\nTurn ON message preview in WhatsApp notifications.")
            return
        }
        if (body.equals("Checking for new messages", ignoreCase = true)) return
        if (body.equals("Incoming voice call", ignoreCase = true)) return
        if (body.equals("Incoming video call", ignoreCase = true)) return

        val key = "$title|$body"
        val now = System.currentTimeMillis()
        synchronized(recent) {
            recent.entries.removeIf { now - it.value > 60_000 }
            if (recent.containsKey(key)) return
            recent[key] = now
        }

        val payload = if (title.isNotEmpty() && !title.contains(body)) "$title: $body" else body
        toast("Checking WhatsApp…")
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
                } else {
                    toast("Checked — looks OK")
                }
            } catch (e: Exception) {
                Log.e(TAG, "classify failed (check API_BASE_URL / server)", e)
                toast(
                    "Scam check failed — cannot reach ${BuildConfig.API_BASE_URL}\n" +
                        "Keep USB connected (adb reverse) or same Wi‑Fi. Server must be running.",
                )
            }
        }
    }

    private fun toast(msg: String) {
        mainHandler.post {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
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
        val fromCompat = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(n)
            ?.messages
            ?.lastOrNull()
            ?.text
            ?.toString()
            ?.trim()
            .orEmpty()
        if (fromCompat.isNotEmpty()) return fromCompat

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) return text

        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        if (big.isNotEmpty()) return big

        val sub = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()
        if (sub.isNotEmpty()) return sub

        @Suppress("DEPRECATION")
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        val fromLines = lines?.mapNotNull { it?.toString()?.trim() }?.lastOrNull().orEmpty()
        if (fromLines.isNotEmpty()) return fromLines

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
