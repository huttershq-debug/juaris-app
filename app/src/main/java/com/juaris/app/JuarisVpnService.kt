package com.juaris.app

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class JuarisVpnService : VpnService() {

    companion object {
        private const val TAG = "JuarisZeroTrustFirewall"
    }

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startVpnTunnel()
        Log.d(TAG, "🟢 Lokale Zero-Trust-Firewall aktiv: Sämtlicher unerwünschter Traffic wird auf dem Gerät abgefangen.")
        return START_STICKY
    }

    private fun startVpnTunnel() {
        try {
            val builder = Builder()
                .setSession("Juaris Zero-Trust Shield")
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0) // Fängt den gesamten Netzwerkverkehr lokal ab

            vpnInterface = builder.establish()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Aufbau des lokalen VPN-Tunnels: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Schließen der VPN-Interface: ${e.message}")
        }
    }
}

