package com.juaris.app

import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class ScamCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        // Nur eingehende Anrufe prüfen (API-Check für getCallDirection ab API 29)
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
            // Unterdrückte / private Nummern ohne Kennung hart abwehren und loggen
            val response = CallResponse.Builder()
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            respondToCall(callDetails, response)

            scope.launch {
                db.securityLogDao().insertLog(
                    SecurityLogEntity(
                        timestamp = System.currentTimeMillis(),
                        status = "BLOCKED",
                        module = "Anruf-Schutz",
                        description = "Unterdrückte/anonyme Nummer blockiert",
                        details = "Keine Rufnummer vom Provider übermittelt"
                    )
                )
            }
            return
        }

        // 1. DAS EISERNE GESETZ: Echte Kontakte aus dem Telefonbuch MÜSSEN immer durchkommen!
        if (isContactInPhoneBook(phoneNumber)) {
            respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
            return
        }

        // 2. Blacklist-Prüfung über die SecurityEngine
        val isExplicitlyBlocked = securityEngine.isNumberBlocked(phoneNumber)

        // 3. Lokale Betrugsheuristik (Risiko-Vorwahlen, unnatürliche Länge)
        val isFraudDetected = isLocalFraudDetected(phoneNumber)

        if (isExplicitlyBlocked || isFraudDetected) {
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()

            respondToCall(callDetails, response)

            val reasonDesc = if (isExplicitlyBlocked) "Nummer steht auf der manuellen Sperrliste" else "Lokale Betrugsheuristik (Risiko-Vorwahl oder Länge)"
            
            scope.launch {
                db.securityLogDao().insertLog(
                    SecurityLogEntity(
                        timestamp = System.currentTimeMillis(),
                        status = "BLOCKED",
                        module = "Anruf-Schutz",
                        description = "Spam-/Betrugsanruf blockiert",
                        details = "Rufnummer: $phoneNumber | Grund: $reasonDesc"
                    )
                )
            }
        } else {
            // Unbekannt, aber unauffällig -> normal durchlassen
            respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
        }
    }

    private fun isContactInPhoneBook(phoneNumber: String): Boolean {
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )
            cursor?.use { it.moveToFirst() } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun isLocalFraudDetected(phoneNumber: String): Boolean {
        val cleanNumber = phoneNumber.replace(Regex("[^\\d+]"), "")

        // Lokale Heuristik für bekannte Risiko-Vorwahlen (z.B. Zypern +357, Tunesien +216, Kongo +243)
        val highRiskPrefixes = listOf("+357", "+216", "+243")
        if (highRiskPrefixes.any { cleanNumber.startsWith(it) }) {
            return true
        }

        // Manipulierte oder unnatürlich kurze Nummern abfangen
        if (cleanNumber.length < 4) {
            return true
        }

        return false
    }
}

