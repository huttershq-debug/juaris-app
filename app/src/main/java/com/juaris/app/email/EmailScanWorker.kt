
Benedikt Wolfgang Hütter <hutters.hq@gmail.com>
20:47 (vor 0 Minuten)
an mich

package com.juaris.app.email

import com.juaris.app.QuantumEngine

class EmailScanWorker {
    fun scanLocalEmailContent(sender: String, subject: String): Boolean {
        // Korrekter Methodenaufruf passend zur QuantumEngine
        val hash = QuantumEngine.generateThreatSignature(subject)
        
        return subject.contains("Invoice", ignoreCase = true) || 
               subject.contains("Bank", ignoreCase = true)
    }
}
