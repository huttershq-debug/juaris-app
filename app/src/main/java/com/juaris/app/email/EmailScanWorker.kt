package com.juaris.app

class EmailScanWorker {
    fun scanLocalEmailContent(sender: String, subject: String): Boolean {
        val hash = QuantumEngine.generateSecureHash(subject)
        val isThreat = subject.contains("Invoice", ignoreCase = true) || subject.contains("Bank", ignoreCase = true)
        
        if (isThreat) {
            JuarisEventBus.postEvent("[!] E-Mail-Bedrohung erkannt: $sender (Quantum-Hash: ${hash.take(8)})")
            return true
        }
        return false
    }
}
