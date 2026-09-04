package com.juaris.app

import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class ScamCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart
        val responseBuilder = CallResponse.Builder()

        // 1. Unterdrückte oder anonyme Nummern sofort blockieren
        if (phoneNumber == null) {
            blockCall(responseBuilder)
            respondToCall(callDetails, responseBuilder.build())
            return
        }

        // 2. Prüfen, ob die Nummer dauerhaft gesperrt ist oder in der Liste steht
        if (isNumberBlockedPermanently(phoneNumber) || checkNumberLocally(phoneNumber)) {
            blockCall(responseBuilder)
        } else {
            responseBuilder.setDisallowCall(false)
        }

        respondToCall(callDetails, responseBuilder.build())
    }

    private fun blockCall(builder: CallResponse.Builder) {
        builder.setDisallowCall(true)
        builder.setRejectCall(true)
        builder.setSkipCallLog(false)
        builder.setSkipNotification(true)
    }

    private fun isNumberBlockedPermanently(number: String): Boolean {
        // A) Feste Liste bekannter internationaler Betrugs-Vorwahlen
        val suspiciousPrefixes = listOf(
            "+243", // DR Kongo
            "+225", // Elfenbeinküste
            "+232", // Sierra Leone
            "+375", // Belarus
            "+882", // Satelliten
            "+881" // Satelliten
        )

        for (prefix in suspiciousPrefixes) {
            if (number.startsWith(prefix)) {
                return true
            }
        }
        return false
    }

    private fun checkNumberLocally(number: String): Boolean {
        // Test-Liste fuer bekannte Betrugsnummern
        val localScamDatabase = listOf("+43123456789", "+49190123456")
        if (localScamDatabase.contains(number)) {
            return true
        }

        // B) Abgleich mit dem dauerhaften lokalen Speicher des Handys
        val prefs = getSharedPreferences("LocalShieldPrefs", Context.MODE_PRIVATE)
        val blockedSet = prefs.getStringSet("blocked_numbers", mutableSetOf()) ?: mutableSetOf()

        return blockedSet.contains(number)
    }
}

