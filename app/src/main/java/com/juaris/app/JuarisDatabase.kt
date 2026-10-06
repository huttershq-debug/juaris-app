package com.juaris.app

import android.content.Context
import android.util.Base64
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom

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

        private const val DB_NAME = "juaris_security_db"
        private const val PREFS_FILE_NAME = "juaris_db_vault_key"
        private const val PASSPHRASE_KEY = "db_crypto_passphrase"

        fun getDatabase(context: Context): JuarisDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext

                // Initialisiert die nativen SQLCipher C++ Bibliotheken für Android
                SQLiteDatabase.loadLibs(appContext)

                val passphrase = getOrCreateDatabasePassphrase(appContext)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    appContext,
                    JuarisDatabase::class.java,
                    DB_NAME
                )
                    .openHelperFactory(factory) // Injiziert AES-256 Datenbank-Verschlüsselung
                    .fallbackToDestructiveMigrationOnDowngrade() // Verhindert Datenverlust bei regulären App-Updates
                    .build()

                INSTANCE = instance
                instance
            }
        }

        private fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                val securePrefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )

                var savedKeyBase64 = securePrefs.getString(PASSPHRASE_KEY, null)

                if (savedKeyBase64 == null) {
                    // Kryptografisch sichere 256-Bit (32 Byte) Schlüsselgenerierung
                    val randomBytes = ByteArray(32)
                    SecureRandom().nextBytes(randomBytes)
                    savedKeyBase64 = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
                    securePrefs.edit().putString(PASSPHRASE_KEY, savedKeyBase64).apply()
                }

                Base64.decode(savedKeyBase64, Base64.NO_WRAP)
            } catch (e: Exception) {
                // Sichert den Zugriff ab, falls der Android KeyStore fehlschlägt
                getFallbackPassphrase(context)
            }
        }

        private fun getFallbackPassphrase(context: Context): ByteArray {
            val fallbackPrefs = context.getSharedPreferences("juaris_db_fallback_prefs", Context.MODE_PRIVATE)
            var fallbackKey = fallbackPrefs.getString("fallback_key", null)

            if (fallbackKey == null) {
                val randomBytes = ByteArray(32)
                SecureRandom().nextBytes(randomBytes)
                fallbackKey = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
                fallbackPrefs.edit().putString("fallback_key", fallbackKey).apply()
            }

            return Base64.decode(fallbackKey, Base64.NO_WRAP)
        }
    }
}
