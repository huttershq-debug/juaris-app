package com.juaris.app

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [SecurityLogEntity::class],
    version = 3,
    exportSchema = false
)
abstract class JuarisDatabase : RoomDatabase() {

    abstract fun securityLogDao(): SecurityLogDao

    companion object {
        private const val TAG = "JuarisDb"
        private const val DB_NAME = "juaris_security_db"

        @Volatile
        private var instance: JuarisDatabase? = null

        /** true, wenn die Datenbank verschluesselt auf dem Geraet liegt. */
        @Volatile
        var isEncrypted: Boolean = false
            private set

        /** false, wenn nur ein fluechtiger Speicher im RAM verfuegbar war. */
        @Volatile
        var isPersistent: Boolean = false
            private set

        /**
         * Wirft nie. Nicht auf dem Main-Thread aufrufen (Keystore und native Bibliothek).
         * Reihenfolge: verschluesselt -> Schluessel/DB zuruecksetzen und erneut -> RAM-Datenbank.
         */
        fun getDatabase(context: Context): JuarisDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(ctx: Context): JuarisDatabase {
            openEncrypted(ctx)?.let {
                isEncrypted = true
                isPersistent = true
                return it
            }
            Log.w(TAG, "Setze Schluessel und Datenbank zurueck und versuche es erneut.")
            KeystoreVault.reset(ctx)
            ctx.deleteDatabase(DB_NAME)
            openEncrypted(ctx)?.let {
                isEncrypted = true
                isPersistent = true
                return it
            }
            Log.e(TAG, "Verschluesselter Speicher nicht verfuegbar. Nutze fluechtigen Speicher.")
            isEncrypted = false
            isPersistent = false
            return Room.inMemoryDatabaseBuilder(ctx, JuarisDatabase::class.java).build()
        }

        private fun openEncrypted(ctx: Context): JuarisDatabase? {
            var db: JuarisDatabase? = null
            return try {
                System.loadLibrary("sqlcipher")
                val passphrase = KeystoreVault.getOrCreatePassphrase(ctx)
                db = Room.databaseBuilder(ctx, JuarisDatabase::class.java, DB_NAME)
                    .openHelperFactory(SupportOpenHelperFactory(passphrase))
                    .fallbackToDestructiveMigration()
                    .build()
                db.openHelper.writableDatabase // erzwingt das Oeffnen und die Schema-Pruefung jetzt
                db
            } catch (t: Throwable) {
                Log.e(TAG, "Verschluesselte DB nicht verfuegbar: ${t.javaClass.simpleName}")
                try {
                    db?.close()
                } catch (e: Throwable) {
                    // ignorieren
                }
                null
            }
        }
    }
}
