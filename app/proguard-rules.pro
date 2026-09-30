# =====================================================================
# Juaris ProGuard Rules - Store Ready (Optimized for Android 14+)
# =====================================================================

# Room Database (Verhindert das Entfernen von Entities, DAOs und Modellen)
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
# KORREKTUR 1: Schützt deine konkreten Room-Entitäten und DAOs vor der Namenszerstörung!
-keep class com.juaris.app.SecurityLogEntity { *; }
-keep class com.juaris.app.MeshPostEntity { *; }
-keep class * implements androidx.room.RoomDatabase$Callback

# Kotlin Coroutines
-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class kotlinx.coroutines.** { *; }

# Google Play Billing Client (Absicherung für In-App-Abos)
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Jetpack Compose & Material 3
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class androidx.compose.ui.awt.ComposePanel { *; }
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Verschlüsselung & Security (EncryptedSharedPreferences / Tink)
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Google Play Services Nearby (Pflicht für dein Bluetooth P2P-Mesh)
# KORREKTUR 2: Verhindert, dass R8 die Google Nearby-Schnittstellen unlesbar macht!
-keep class com.google.android.gms.nearby.** { *; }
-dontwarn com.google.android.gms.nearby.**

# BouncyCastle Post-Quantum Kryptografie (Absicherung für Dilithium)
# KORREKTUR 3: Hält deine unknackbare Post-Quanten-Engine voll funktionsfähig!
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Allgemeine Beibehaltung von AndroidX-Komponenten
-dontwarn androidx.core.**
-keep class androidx.core.** { *; }


