package com.juaris.app.email

import com.juaris.app.QuantumEngine

class EmailScanWorker {
    fun scanLocalEmailContent(sender: String, subject: String): Boolean {
        val hash = QuantumEngine.generateThreatSignature(subject)
        
        return subject.contains("Invoice", ignoreCase = true) || 
               subject.contains("Bank", ignoreCase = true)
    }
}
