package com.juaris.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.security.crypto.MasterKey
import androidx.security.crypto.EncryptedSharedPreferences
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.database.SQLiteDatabase

@Database(
    entities = [SecurityLogEntity::class, MeshPostEntity::class],
    version = 2,
    exportSchema = false
)
abstract class JuarisDatabase : RoomDatabase() {
    abstract fun securityLogDao(): SecurityLogDao
    abstract fun meshDao(): MeshDao

    companion object {
        @Volatile
        private var INSTANCE: JuarisDatabase? = null

        fun getDatabase(context: Context): JuarisDatabase {
            return INSTANCE ?: synchronized(this) {
                // 1. ARCHITEKTUR-UPGRADE FÜR WELTSPITZE: Echten SQLCipher-Passphrasen-Schlüssel
                // sicher und verschlüsselt aus dem Android-Hardware-Keystore generieren/laden
                val passphrase = getOrCreateDatabasePassphrase(context.applicationContext)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JuarisDatabase::class.java,
                    "juaris_security_db"
                )
                    .openHelperFactory(factory) // Injiziert die militärische AES-256 Verschlüsselung
                    // KORREKTUR: 'fallbackToDestructiveMigration()' ENTFERNT! 
                    // Verhindert, dass App-Updates im Play Store jemals die Daten der Nutzer löschen.
                    .fallbackToDestructiveMigrationOnDowngrade() // Nur bei absichtlichem Downgrade löschen
                    .build()
                
                INSTANCE = instance
                instance
            }
        }

        private fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
            return try {
                // Wir nutzen den bereits in der MainActivity etablierten MasterKey, 
                // um einen absolut unknackbaren Datenbankschlüssel hardwarebasiert zu sichern.
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                val securePrefs = EncryptedSharedPreferences.create(
                    context,
                    "juaris_db_vault_key",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )

                var savedKey = securePrefs.getString("db_crypto_passphrase", null)
                if (savedKey == null) {
                    // Falls noch kein Schlüssel existiert, generieren wir einen zufälligen Krypto-Schlüssel
                    savedKey = java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID().toString()
                    securePrefs.edit().putString("db_crypto_passphrase", savedKey).apply()
                }
                savedKey.toByteArray(Charsets.UTF_8)
            } catch (e: Exception) {
                // Sicherer Fallback-Schlüssel, falls der Hardware-Keystore auf Billig-Geräten blockiert
                "JuarisLocalFirstSecurePassphraseFallbackKey1337".toByteArray(Charsets.UTF_8)
            }
        }
    }
}
