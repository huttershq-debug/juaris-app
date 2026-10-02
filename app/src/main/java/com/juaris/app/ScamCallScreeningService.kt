package com.juaris.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

@RequiresApi(Build.VERSION_CODES.N)
class ScamCallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "JuarisCallScreening"
    }

    // KORREKTUR 1: Zentraler, kontrollierter Service-Scope statt ungebundenem Wildwuchs!
    // Verhindert verwaiste Hintergrund-Threads und Memory Leaks beim Beenden des Dienstes.
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onScreenCall(callDetails: Call.Details) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
                respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
                return
            }
        }

        val phoneNumber = callDetails.handle?.schemeSpecificPart
        val securityEngine = SecurityEngine(applicationContext)
        val db = JuarisDatabase.getDatabase(applicationContext)

        if (phoneNumber.isNullOrEmpty()) {
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            
            respondToCall(callDetails, response)

            serviceScope.launch {
                try {
                    db.securityLogDao().insertLog(
                        SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            status = "BLOCKED",
                            module = "Anruf-Schutz",
                            description = "Unterdrückte/anonyme Nummer blockiert",
                            details = "Keine Rufnummer vom Provider übermittelt"
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Fehler beim Schreiben des anonymen Anrufer-Logs: ${e.message}")
                }
            }
            return
        }

        // KORREKTUR 2: Wir starten die Krypto- und Heuristikprüfung asynchron im Hintergrund,
        // um den Main-Thread NIEMALS mit I/O- oder ContentResolver-Lags zu blockieren!
        serviceScope.launch {
            try {
                // 1. DAS EISERNE GESETZ: Kontakte im Telefonbuch (auf IO-Thread ausgelagert) durchlassen
                if (isContactInPhoneBook(phoneNumber)) {
                    withContext(Dispatchers.Main) {
                        respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
                    }
                    return@launch
                }

                // 2. Blacklist- und Heuristik-Prüfung
                val isExplicitlyBlocked = securityEngine.isNumberBlocked(phoneNumber)
                val isFraudDetected = isLocalFraudDetected(phoneNumber)

                if (isExplicitlyBlocked || isFraudDetected) {
                    val response = CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                        .build()

                    withContext(Dispatchers.Main) {
                        respondToCall(callDetails, response)
                    }

                    val reasonDesc = if (isExplicitlyBlocked) "Nummer steht auf der manuellen Sperrliste" else "Lokale Betrugsheuristik (Risiko-Vorwahl oder Länge)"
                   
                    db.securityLogDao().insertLog(
                        SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            status = "BLOCKED",
                            module = "Anruf-Schutz",
                            description = "Spam-/Betrugsanruf blockiert",
                            details = "Rufnummer: $phoneNumber | Grund: $reasonDesc"
                        )
                    )
                } else {
                    withContext(Dispatchers.Main) {
                        respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fehler im asynchronen Call-Screening-Prozess: ${e.message}")
                // Fallback im Fehlerfall: Anruf zur Sicherheit durchlassen
                withContext(Dispatchers.Main) {
                    respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
                }
            }
        }
    }

    private fun isContactInPhoneBook(phoneNumber: String): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        var cursor: android.database.Cursor? = null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )
            cursor?.use { it.moveToFirst() } ?: false
        } catch (e: Exception) {
            false
        } finally {
            cursor?.close() // Ressourcen explizit freigeben
        }
    }

    private fun isLocalFraudDetected(phoneNumber: String): Boolean {
        val cleanNumber = phoneNumber.replace(Regex("[^\\d+]"), "")

        // Risiko-Vorwahlen (+357 Zypern, +216 Tunesien, +243 Kongo)
        val highRiskPrefixes = listOf("+357", "+216", "+243")
        if (highRiskPrefixes.any { cleanNumber.startsWith(it) }) {
            return true
        }

        if (cleanNumber.length < 4) {
            return true
        }

        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        // KORREKTUR 3: Beendet alle offenen Hänger und Coroutinen sauber beim Zerstören des Services
        serviceJob.cancel()
        Log.d(TAG, "🛑 Juaris Anruf-Schutz sicher heruntergefahren.")
    }
}
