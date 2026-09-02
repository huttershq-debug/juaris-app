package com.localshield.v5

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

        // 2. Prüfen, ob die Nummer dauerhaft auf dem Gerät gesperrt ist
        if (isNumberBlockedPermanently(applicationContext, phoneNumber)) {
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

    private fun isNumberBlockedPermanently(context: Context, number: String): Boolean {
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

        // B) Abgleich mit dem dauerhaften lokalen Speicher des Handys
        val prefs = context.getSharedPreferences("LocalShieldPrefs", Context.MODE_PRIVATE)
        val blockedSet = prefs.getStringSet("blocked_numbers", mutableSetOf()) ?: mutableSetOf()

        return blockedSet.contains(number)
    }
}
