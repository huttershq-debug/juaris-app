package com.juaris.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Einziger Speicher fuer alle Einstellungen. Wird von UI, Listener, Call-Screening und Worker
 * gemeinsam genutzt, damit jeder Schalter wirklich wirkt.
 */
object SecurityPrefs {
    private const val FILE = "juaris_prefs"
    private const val KEY_CALL = "call_protection"
    private const val KEY_NOTIFICATION_SCAN = "notification_scan"
    private const val KEY_BLOCKED = "blocked_numbers"
    private const val KEY_SEEN_EVENTS = "seen_calendar_events"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isCallProtectionEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CALL, true)

    fun setCallProtectionEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_CALL, value).apply()
    }

    fun isNotificationScanEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATION_SCAN, true)

    fun setNotificationScanEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATION_SCAN, value).apply()
    }

    fun blockedNumbers(context: Context): List<String> =
        prefs(context).getStringSet(KEY_BLOCKED, emptySet()).orEmpty().toList().sorted()

    @Synchronized
    fun addBlockedNumber(context: Context, raw: String): Boolean {
        val entry = raw.trim()
        if (digitsOf(entry).length < 3) return false
        val set = HashSet<String>(prefs(context).getStringSet(KEY_BLOCKED, emptySet()).orEmpty())
        if (!set.add(entry)) return false
        prefs(context).edit().putStringSet(KEY_BLOCKED, set).apply()
        return true
    }

    @Synchronized
    fun removeBlockedNumber(context: Context, entry: String) {
        val set = HashSet<String>(prefs(context).getStringSet(KEY_BLOCKED, emptySet()).orEmpty())
        if (set.remove(entry)) {
            prefs(context).edit().putStringSet(KEY_BLOCKED, set).apply()
        }
    }

    /** Vergleicht die letzten bis zu 9 Ziffern, damit +43 660 ... und 0660 ... zusammenpassen. */
    fun isNumberBlocked(context: Context, number: String): Boolean {
        val n = digitsOf(number)
        if (n.isEmpty()) return false
        return blockedNumbers(context).any { entry ->
            val e = digitsOf(entry)
            if (e.isEmpty()) {
                false
            } else if (n == e) {
                true
            } else {
                val k = minOf(n.length, e.length, 9)
                k >= 7 && n.takeLast(k) == e.takeLast(k)
            }
        }
    }

    /** true, wenn der Termin heute noch nicht gemeldet wurde. Alte Tage werden dabei verworfen. */
    @Synchronized
    fun markEventNotified(context: Context, dayKey: String, eventId: Long): Boolean {
        val key = "$dayKey:$eventId"
        val today = HashSet<String>(
            prefs(context).getStringSet(KEY_SEEN_EVENTS, emptySet()).orEmpty()
                .filter { it.startsWith("$dayKey:") }
        )
        if (!today.add(key)) return false
        prefs(context).edit().putStringSet(KEY_SEEN_EVENTS, today).apply()
        return true
    }

    fun clearUserData(context: Context) {
        prefs(context).edit().remove(KEY_BLOCKED).remove(KEY_SEEN_EVENTS).apply()
    }

    private fun digitsOf(s: String): String = s.filter { it.isDigit() }
}
