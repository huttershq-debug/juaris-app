package com.juaris.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Testlauf der heiligen Dreifaltigkeit beim Start im Hintergrund
        testSecurityEngine()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JuarisMainScreen()
                }
            }
        }
    }

    private fun testSecurityEngine() {
        // 1. Test Anruf
        val callTest = SecurityEngine.analyzeIncomingCall("+43123456789")
        Log.d("JuarisTest", "Call Result: $callTest")

        // 2. Test SMS
        val smsTest = SecurityEngine.analyzeIncomingSms("+43660123456", "Dein Konto gesperrt!")
        Log.d("JuarisTest", "SMS Result: $smsTest")

        // 3. Test E-Mail
        val emailTest = SecurityEngine.analyzeIncomingEmail("info@bank.com", "Gewinn!", "Du hast gewonnen")
        Log.d("JuarisTest", "Email Result: $emailTest")
    }
}

@Composable
fun JuarisMainScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.status_protected),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        ProtectionItem(title = stringResource(R.string.call_protection_title))
        ProtectionItem(title = stringResource(R.string.sms_protection_title))
        ProtectionItem(title = stringResource(R.string.email_protection_title))
    }
}

@Composable
fun ProtectionItem(title: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        // HIER WAR DER FEHLER: modifier.padding -> Korrigiert zu Modifier.padding
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
    }
}
