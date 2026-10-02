package com.juaris.app

import android.os.Build
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class JuarisBiometricManager(private val activity: FragmentActivity) {

    fun authenticateUser(
        title: String = "Juaris Future-Proof Vault",
        subtitle: String = "Biometrische Verifizierung (Fingerabdruck, Gesicht, Sensor oder PIN)",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)
       
        // ZUKUNFTSSICHER: Kombiniert Strong-Biometrics, Weak-Biometrics und Device-Credentials (PIN/Muster)
        val allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        // CRITICAL FIX: Vor Android 11 (API 30) gab es BIOMETRIC_WEAK in dieser Kombination nicht nativ im canAuthenticate-Check.
        // Wir passen das Prüf-Flag für ältere APIs an, um System-Fehlinterpretationen zu vermeiden.
        val checkAuthenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            allowedAuthenticators
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        when (biometricManager.canAuthenticate(checkAuthenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(activity)
                val biometricPrompt = BiometricPrompt(activity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            onError(errString.toString())
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Toast.makeText(activity, "Biometrie nicht erkannt. Bitte erneut versuchen.", Toast.LENGTH_SHORT).show()
                        }
                    })

                val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    promptInfoBuilder.setAllowedAuthenticators(allowedAuthenticators)
                } else {
                    // Abwärtskompatibler, stabiler Fallback für ältere Android-Versionen vor API 30
                    @Suppress("DEPRECATION")
                    promptInfoBuilder.setDeviceCredentialAllowed(true)
                }

                try {
                    val promptInfo = promptInfoBuilder.build()
                    
                    // ARCHITEKTUR-TIPP FÜR DIE ZUKUNFT:
                    // Um absolute Immunität gegen RAM-Injektionen (z.B. via Frida) zu erreichen,
                    // sollte hier langfristig das 'CryptoObject' des verschlüsselten Tresors übergeben werden:
                    // biometricPrompt.authenticate(promptInfo, cryptoObject)
                    
                    biometricPrompt.authenticate(promptInfo)
                } catch (e: Exception) {
                    onError("Fehler bei der Initialisierung der Biometrie-UI: ${e.message}")
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                onError("Keine biometrische Hardware oder Sicherung auf diesem Gerät verfügbar.")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onError("Keine Sicherheitsmethoden (Biometrie oder PIN) im System eingerichtet. Bitte in den Android-Einstellungen hinterlegen.")
            }
            else -> {
                onError("Biometrische Authentifizierung aktuell vom System eingeschränkt.")
            }
        }
    }
}
