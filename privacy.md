# Datenschutzerklärung (Privacy Policy)
**Stand: 15. Oktober 2026**

### 1. Grundsatz und Local-First-Prinzip
Der Schutz Ihrer Privatsphäre und Ihrer persönlichen Daten ist das fundamentale Kernprinzip von **JUARIS** (`com.juaris.app`). Juaris wurde als radikale Gegenbewegung zu datenhungrigen Cloud-Systemen entwickelt und arbeitet zu **100 % serverlos**. Sämtliche Sicherheitsüberprüfungen, Heuristiken, Analysen und Protokolle finden ausschließlich lokal im gesicherten, internen App-Speicher direkt auf Ihrem Endgerät statt. **Es existiert keine Server-Infrastruktur, keine Cloud-Anbindung und kein Telemetrie-Tracking.** Ihre Daten verlassen niemals Ihr Gerät. Juaris ist ein voll funktionsfähiges Live-Sicherheitssystem für den täglichen Einsatz im echten Leben.

### 2. Keine Datenerhebung, Verarbeitung und Nutzung
Da Juaris vollständig offline operiert, werden von uns zu keinem Zeitpunkt persönliche Daten, Metadaten, Standortdaten, Nutzungsprofile oder Gerätekennungen erhoben, gespeichert oder an Dritte übertragen. Was die App im echten Leben nicht tut, ist genauso wichtig wie das, was sie tut:
* **Ohne Cloud:** Keine Cloud-Speicherung, kein Cloud-Backup, kein Cloud-Abgleich.
* **Ohne Server:** Keine externen Server, an die Ihr Gerät Daten sendet.
* **Ohne Telemetrie:** Keine Nutzungs-, Diagnose- oder Trackingdaten, die das Gerät verlassen.

Sämtliche Protokolle über abgefangene Spam-Anrufe oder Phishing-SMS verbleiben in einer lokal verschlüsselten Datenbank auf Ihrem Smartphone. Lokale Dateien in Ihrem Dateitresor (Offline-Vault) werden mittels hardwaregestützter AES-GCM-Kryptografie isoliert und verlassen Ihr Gerät im echten Betrieb niemals.

### 3. Erklärung der sensiblen System-Berechtigungen (Android Scopes)
Zur Gewährleistung der lokalen Echtzeit-Schutzfunktionen im Alltag fordert die App systembedingt sensible Berechtigungen an. Diese werden ausschließlich on-device verarbeitet:
* **Anruf-Schutz (`CallScreeningService` & `READ_CONTACTS`):** Die App analysiert eingehende Rufnummern lokal, um Spam-Anrufe blockieren zu können. Das Telefonbuch wird ausschließlich lokal abgeglichen, um Kontakte von der Filterung auszuschließen. Es findet keine Übertragung statt.
* **SMS-Wächter (`RECEIVE_SMS` / `SEND_SMS`):** Als aktiver Standard-SMS-Wächter analysiert Juaris eingehende Texte im echten Betrieb lokal auf Phishing-Links.
* **Benachrichtigungs-Monitor (`NotificationListenerService`):** Ermöglicht das lokale Scannen eingehender E-Mail- und App-Benachrichtigungen (Gmail, GMX, Outlook etc.) auf psychologische Manipulationstaktiken.
* **Akustischer Notfall-Sensor (`RECORD_AUDIO`):** Analysiert flüchtige PCM-Amplituden im Arbeitsspeicher, um Gefahrensituationen (Schreie) zu erkennen. Audiodaten werden zu keinem Zeitpunkt dauerhaft gespeichert oder exportiert.

### 4. Lokales Peer-to-Peer-Netzwerk (Nearby-Schnittstelle)
Die Nearby-Schnittstelle (`P2P_CLUSTER`) wird **ausschließlich lokal für den Offline-Peer-to-Peer-Datenabgleich via Bluetooth** im Juaris-Schwarm genutzt. Es werden hierbei keinerlei Daten, IDs oder Standortinformationen an Google-Server oder andere externe Netze übertragen.

### 5. In-App-Abonnements (Google Play Billing)
Für die Abwicklung des monatlichen Abonnements (1,99 €/Monat) wird der offizielle `Google Play Billing` Service genutzt. Die Zahlungsabwicklung und Verwaltung läuft vollständig über die Server der Google Ireland Limited gemäß deren Datenschutzrichtlinien. Wir erhalten zu keinem Zeitpunkt Zugriff auf Ihre Bank- oder Kreditkartendaten.

### 6. Ihre Rechte und Kontakt
Da wir im echten Betrieb keine Daten von Ihnen besitzen, erheben oder speichern, können wir keine Auskunft, Löschung oder Sichteinschränkung von Daten vornehmen. Alle Daten löschen Sie ganz einfach physisch selbst, indem Sie die App deinstallieren oder im Dashboard den Panic-Wipe auslösen. Sie behalten im echten Leben die volle Kontrolle.

Bei Fragen zum Datenschutz erreichen Sie uns unter: **support@juaris.com**

---

# Impressum
*Angaben gemäß § 5 TMG / ECG*

**Name / Entwickler:**
Benedikt Wolfgang Hütter

**Anschrift:**
Schulgasse 4/15
2700 Wiener Neustadt
Österreich

**Kontakt:**
E-Mail: support@juaris.com
Web: www.juaris.com

**Verantwortlich für den Inhalt:**
Benedikt Wolfgang Hütter

**Design:**
Benedikt Wolfgang Hütter

*© 2026 JUARIS — Hinter JUARIS steht Hutter's IT-Solutions aus Wiener Neustadt.*


