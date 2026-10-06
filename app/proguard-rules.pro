# =====================================================================
# Juaris ProGuard Rules - Store Ready (Optimized for Android 14+)
# =====================================================================

# Allgemeine Android-Attribute sichern
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, SourceFile, LineNumberTable, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# =====================================================================
# 1. ROOM DATABASE & ENITIES (Alle Entities & DAOs pauschal schützen)
# =====================================================================
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.RoomDatabase$Callback

# Schützt alle Klassen, die als Room Entities oder DAOs dienen
-keep class com.juaris.app.** { 
    @androidx.room.Entity *;
    @androidx.room.Dao *;
    <init>();
}
-keep interface com.juaris.app.**Dao { *; }
-keep class com.juaris.app.SecurityLogEntity { *; }
-keep class com.juaris.app.MeshPostEntity { *; }
-keep interface com.juaris.app.SecurityLogDao { *; }
-keep interface com.juaris.app.MeshDao { *; }

# =====================================================================
# 2. SERIALISIERUNG & DATENMODELLE (Verhindert Start-Crashes bei JSON/Prefs)
# =====================================================================
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private !static !transient <fields>;
    private !static !transient <methods>;
    private <methods>;
}

# Schutz für Kotlinx Serialization / Gson / Moshi Modelle
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# =====================================================================
# 3. ARCHITECTURE COMPONENTS (ViewModels & Lifecycle)
# =====================================================================
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# =====================================================================
# 4. NATIVE BIBLIOTHEKEN & SICHERHEIT (SQLCipher, TFLite, Krypto)
# =====================================================================
-keep class net.zetetic.database.sqlcipher.** { *; }
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**
-dontwarn androidx.sqlite.db.**

-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

-keepclassmembers class * {
    @native <methods>;
}

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }

# Google Play Billing Client (In-App-Abos)
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

# Google Play Services Nearby (Bluetooth P2P-Mesh)
-keep class com.google.android.gms.nearby.** { *; }
-dontwarn com.google.android.gms.nearby.**

# BouncyCastle Post-Quantum Kryptografie (Dilithium)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Allgemeine Beibehaltung von AndroidX-Komponenten
-dontwarn androidx.core.**
-keep class androidx.core.** { *; }
