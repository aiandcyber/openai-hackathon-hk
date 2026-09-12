package com.aiforseniors.scamshield

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlin.concurrent.thread

/**
 * Reads WhatsApp notification title/text and POSTs to /api/classify-scam.
 * On isScam, opens [WarnActivity]. Requires Notification access in system settings.
 */
class WhatsAppNotificationListener : NotificationListenerService() {

    private val recent = LinkedHashMap<String, Long>()

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

        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = (
            extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            )?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return

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
                val result = app.api.classifyScam(payload, source = "whatsapp")
                if (result.isScam) {
                    val intent = Intent(this, WarnActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra(WarnActivity.EXTRA_WARNING, result.seniorWarning.ifBlank {
                            "This message looks unsafe. Do not share codes or tap links."
                        })
                        putExtra(WarnActivity.EXTRA_REASON, result.reason)
                        putExtra(WarnActivity.EXTRA_SOURCE_TEXT, payload.take(400))
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "classify failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "WaNotifyListener"
    }
}
