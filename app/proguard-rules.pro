# Juaris ProGuard Rules - Store Ready

# Room Database (Verhindert das Entfernen von Entities und DAOs)
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase

# Kotlin Coroutines
-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class kotlinx.coroutines.** { *; }

# Google Play Billing Client
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Jetpack Compose & Material 3
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class androidx.compose.ui.awt.ComposePanel {
    *;
}

# Verschlüsselung & Security (EncryptedSharedPreferences / Tink)
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Allgemeine Beibehaltung von AndroidX-Komponenten
-dontwarn androidx.core.**
-keep class androidx.core.** { *; }


