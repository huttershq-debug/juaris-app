package com.example.localshield

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
            responseBuilder.setDisallowCall(true)
            responseBuilder.setRejectCall(true)
            responseBuilder.setSkipCallLog(false)
            responseBuilder.setSkipNotification(true)
            respondToCall(callDetails, responseBuilder.build())
            return
        }

        // 2. Nummer gegen unsere echte Betrugs- und Spam-Liste prüfen
        val isScam = checkNumberLocally(phoneNumber)

        if (isScam) {
            // Betrugsversuch erkannt -> Sofort blockieren und auflegen
            responseBuilder.setDisallowCall(true)
            responseBuilder.setRejectCall(true)
            responseBuilder.setSkipCallLog(false)
            responseBuilder.setSkipNotification(true)
        } else {
            // Nummer ist sauber -> Durchlassen
            responseBuilder.setDisallowCall(false)
        }

        respondToCall(callDetails, responseBuilder.build())
    }

    private fun checkNumberLocally(number: String): Boolean {
        // ECHTE LISTE: Bekannte internationale Spam- und Betrugs-Vorwahlen (z.B. Ping-Anrufe)
        val suspiciousPrefixes = listOf(
            "+243", // Demokratische Republik Kongo
            "+225", // Elfenbeinküste
            "+232", // Sierra Leone
            "+375", // Belarus
            "+882", // Satelliten-Netzwerke (oft teurer Betrug)
            "+881" // Satelliten-Netzwerke
        )

        // Prüfen, ob die Nummer mit einer dieser Spam-Vorwahlen beginnt
        for (prefix in suspiciousPrefixes) {
            if (number.startsWith(prefix)) {
                return true // Anruf blockieren!
            }
        }

        // MANUELLE LISTE: Hier kannst du ganz gezielt einzelne Nummern eintragen, die dich nerven
        val manualBlockedNumbers = listOf(
            // Beispiel: "+436601234567" 
        )

        if (manualBlockedNumbers.contains(number)) {
            return true // Anruf blockieren!
        }

        return false // Kein Treffer -> Anruf wird durchgelassen
    }
}
