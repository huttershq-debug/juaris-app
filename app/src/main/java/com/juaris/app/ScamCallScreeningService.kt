package com.juaris.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.N)
class ScamCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // 1. Richtung prüfen (Nur eingehende Anrufe filtern, ab API 29)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
                respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
                return
            }
        }

        val phoneNumber = callDetails.handle?.schemeSpecificPart
        val securityEngine = SecurityEngine(applicationContext)
        val db = JuarisDatabase.getDatabase(applicationContext)

        // Sicherer Hintergrund-Scope, der nur für die Dauer des Aufrufs lebt
        val localScope = CoroutineScope(Dispatchers.IO)

        if (phoneNumber.isNullOrEmpty()) {
            // Anonyme/Unterdrückte Nummern abwehren
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            
            // ERST die Antwort an das System senden, um Lags zu verhindern!
            respondToCall(callDetails, response)

            // Danach das Log sicher im IO-Thread abspeichern
            localScope.launch {
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

        // 2. DAS EISERNE GESETZ: Kontakte im Telefonbuch IMMER durchlassen!
        if (isContactInPhoneBook(phoneNumber)) {
            respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
            return
        }

        // 3. Blacklist- und Heuristik-Prüfung
        val isExplicitlyBlocked = securityEngine.isNumberBlocked(phoneNumber)
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
           
            localScope.launch {
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
        // Sicherheits-Check: Ohne Kontakte-Berechtigung überspringen, um Abstürze zu verhindern
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
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

        // Risiko-Vorwahlen (+357 Zypern, +216 Tunesien, +243 Kongo)
        val highRiskPrefixes = listOf("+357", "+216", "+243")
        if (highRiskPrefixes.any { cleanNumber.startsWith(it) }) {
            return true
        }

        // Unnatürlich kurze Nummern blockieren (z.B. manipulierte Ping-Anrufe)
        if (cleanNumber.length < 4) {
            return true
        }

        return false
    }
}

