package com.example.localshield

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class ScamCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""

        // LOKALER CHECK: Alles läuft zu 100% auf dem Gerät
        val isScamDetected = checkNumberLocally(phoneNumber)

        val responseBuilder = CallResponse.Builder()

        if (isScamDetected) {
            // Anruf abfangen, bevor er klingelt
            responseBuilder.setDisallowCall(true)
            responseBuilder.setRejectCall(true)
            responseBuilder.setSkipCallLog(true)
            responseBuilder.setSkipNotification(true)
        } else {
            responseBuilder.setDisallowCall(false)
        }

        respondToCall(callDetails, responseBuilder.build())
    }

    private fun checkNumberLocally(number: String): Boolean {
        // Test-Liste für bekannte Betrugsnummern
        val localScamDatabase = listOf("+43123456789", "+49190123456")
        return localScamDatabase.contains(number)
    }
}