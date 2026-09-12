package com.aiforseniors.scamshield

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ProtectionEvent(
    val atMs: Long,
    val isScam: Boolean,
    val summary: String,
)

/**
 * Tiny on-device log of recent WhatsApp checks for the senior home screen.
 */
object ProtectionLog {
    private const val PREFS = "protection_log"
    private const val KEY = "events"
    private const val MAX = 20

    fun record(context: Context, isScam: Boolean, summary: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        val next = JSONArray()
        next.put(
            JSONObject()
                .put("atMs", System.currentTimeMillis())
                .put("isScam", isScam)
                .put("summary", summary.take(120)),
        )
        for (i in 0 until arr.length()) {
            if (next.length() >= MAX) break
            next.put(arr.getJSONObject(i))
        }
        prefs.edit().putString(KEY, next.toString()).apply()
    }

    fun recent(context: Context, limit: Int = 5): List<ProtectionEvent> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        val out = ArrayList<ProtectionEvent>(minOf(limit, arr.length()))
        for (i in 0 until minOf(limit, arr.length())) {
            val o = arr.getJSONObject(i)
            out.add(
                ProtectionEvent(
                    atMs = o.optLong("atMs"),
                    isScam = o.optBoolean("isScam"),
                    summary = o.optString("summary"),
                ),
            )
        }
        return out
    }

    fun todayCounts(context: Context): Pair<Int, Int> {
        val start = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        var checks = 0
        var blocked = 0
        for (e in recent(context, MAX)) {
            if (e.atMs < start) continue
            checks++
            if (e.isScam) blocked++
        }
        return checks to blocked
    }
}
