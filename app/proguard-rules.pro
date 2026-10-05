# =====================================================================
# Juaris ProGuard Rules - Store Ready (Optimized for Android 14+)
# =====================================================================

# Allgemeine Android-Attribute sichern
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*,SourceFile,LineNumberTable

# Room Database (Verhindert das Entfernen von Entities, DAOs und Modellen)
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep class com.juaris.app.SecurityLogEntity { *; }
-keep class com.juaris.app.MeshPostEntity { *; }
-keep class * implements androidx.room.RoomDatabase$Callback

# KORREKTUR 1: Schützt deine Datenzugriffs-Schnittstellen (DAOs) vor der Zerstörung!
# Ohne diese Regeln kann Room die SQL-Befehle im Release-Build nicht mehr zuordnen.
-keep interface com.juaris.app.SecurityLogDao { *; }
-keep interface com.juaris.app.MeshDao { *; }

# KORREKTUR 2: Schutz für die militärische SQLCipher-Datenbankverschlüsselung!
# Verhindert, dass R8 die nativen C/C++ Krypto-Treiber (.so-Dateien) im Release-Build beschädigt.
-keep class net.zetetic.database.sqlcipher.** { *; }
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**
-dontwarn androidx.sqlite.db.**

# KORREKTUR 3: Schutz für das On-Device KI-Modell (TensorFlow Lite)!
# Sichert die mathematischen Strukturen, mit denen die NPU gefüttert wird.
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Kotlin Coroutines (Krig korrigiert: einfache Klammern)
-keep class kotlinx.coroutines.** { *; }

# Google Play Billing Client (Absicherung für In-App-Abos)
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Jetpack Compose & Material 3
-keepclassmembers class androidx.compose.ui.awt.ComposePanel { *; }
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Verschlüsselung & Security (EncryptedSharedPreferences / Tink)
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Google Play Services Nearby (Pflicht für dein Bluetooth P2P-Mesh)
-keep class com.google.android.gms.nearby.** { *; }
-dontwarn com.google.android.gms.nearby.**

# BouncyCastle Post-Quantum Kryptografie (Absicherung für Dilithium)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Allgemeine Beibehaltung von AndroidX-Komponenten
-dontwarn androidx.core.**
-keep class androidx.core.** { *; }

