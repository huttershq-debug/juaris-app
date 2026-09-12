package com.juaris.app

import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService

class ScamCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // Nur eingehende Anrufe prüfen
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
            return
        }

        val phoneNumber = callDetails.handle?.schemeSpecificPart
        if (phoneNumber.isNullOrEmpty()) {
            // Unterdrückte / private Nummern ohne Kennung behandeln
            val response = CallResponse.Builder()
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            respondToCall(callDetails, response)
            return
        }

        // 1. DAS EISERNE GESETZ: Echte Kontakte aus dem Telefonbuch MÜSSEN immer durchkommen!
        if (isContactInPhoneBook(phoneNumber)) {
            respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
            return
        }

        // 2. LOKALE BETRUGSERKENNUNG (100% On-Device, ohne Cloud)
        if (isLocalFraudDetected(phoneNumber)) {
            // Zero-Ring-Drop: Kein Klingeln, kein Ton, wird sofort hart abgewehrt
            val response = CallResponse.Builder()
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
                .build()
            respondToCall(callDetails, response)
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

        // Lokale Heuristik für bekannte Risiko-Vorwahlen (z.B. Zypern +357 oder ähnliche Spam-Fallen)
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

