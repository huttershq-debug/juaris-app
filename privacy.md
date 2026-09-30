JUARIS

Datenschutzerklärung (Privacy Policy)

Stand: 15. Oktober 2026

1. Grundsatz und Local-First-Prinzip
Der Schutz Ihrer persönlichen Daten ist das fundamentale Kernprinzip von JUARIS (com.juaris.app). Juaris wurde als Gegenbewegung zu datenhungrigen Cloud-Systemen entwickelt und arbeitet zu 100 % serverlos. Sämtliche Sicherheitsüberprüfungen, Heuristiken, Analysen und Protokolle finden ausschließlich und lokal in der gesicherten Laufzeitumgebung (Sandbox) auf Ihrem Endgerät statt. Es existiert keine Server-Infrastruktur, keine Cloud-Anbindung und kein Telemetrie-Tracking.

2. Erhebung, Verarbeitung und Nutzung von Daten
Da Juaris vollständig offline operiert, werden von uns zu keinem Zeitpunkt persönliche Daten, Metadaten, Nutzungsprofile oder Gerätekennungen erhoben, gespeichert oder an Dritte übertragen.
• Sicherheits-Logs & Datenbanken: Sämtliche Protokolle über blockierte Spam-Anrufe, Phishing-SMS oder E-Mail-Scans verbleiben in einer lokal verschlüsselten Room-Datenbank auf Ihrem Smartphone.
• Dateitresor (Offline-Vault): Lokale Dateien werden mittels hardwaregestützter AES-GCM-Kryptografie isoliert und verlassen Ihr Gerät niemals.

3. Erklärung der sensiblen System-Berechtigungen (Android Scopes)
Zur Gewährleistung der lokalen Schutzfunktionen fordert die App systembedingt sensible Berechtigungen an. Diese werden ausschließlich on-device verarbeitet:
• Anruf-Schutz (CallScreeningService & READ_CONTACTS): Die App analysiert eingehende Rufnummern lokal, um Spam-Anrufe blockieren zu können. Das Telefonbuch wird ausschließlich lokal abgeglichen, um Kontakte von der Filterung auszuschließen. Es findet keine Übertragung statt.
• SMS-Wächter (RECEIVE_SMS / SEND_SMS): Als optionaler Standard-SMS-Wächter analysiert Juaris eingehende Texte lokal auf Phishing-Links.
• Benachrichtigungs-Monitor (NotificationListenerService): Ermöglicht das lokale Scannen eingehender E-Mail- und App-Benachrichtigungen auf psychologische Manipulationstaktiken.
• Akustischer Notfall-Sensor (RECORD_AUDIO): Analysiert flüchtige PCM-Amplituden im RAM, um Gefahrensituationen (Schreie) zu erkennen. Audiodaten werden zu keinem Zeitpunkt dauerhaft gespeichert oder exportiert.

4. Lokales Peer-to-Peer-Netzwerk (Nearby-Schnittstelle)
Juaris nutzt die Google Nearby Connections API (P2P_CLUSTER) für die Bluetooth-Schwarmintelligenz. Diese Schnittstelle wird ausschließlich lokal für den Offline-Peer-to-Peer-Datenabgleich via Bluetooth verwendet. Es werden hierbei keinerlei Daten oder Standortinformationen an Google-Server oder andere Netzwerke übertragen.

5. In-App-Abonnements (Google Play Billing)
Für die Abwicklung des monatlichen Abonnements (€ 1,99/Monat) wird der offizielle Google Play Billing Service genutzt. Die Zahlungsabwicklung und Verwaltung läuft vollständig über die Server der Google Ireland Limited. Wir erhalten zu keinem Zeitpunkt Zugriff auf Ihre Bank- oder Kreditkartendaten.

6. Ihre Rechte und Kontakt
Da wir keine Daten von Ihnen besitzen oder speichern, können wir keine Auskunft, Löschung oder Sperrung von Daten vornehmen. Alle Daten löschen Sie ganz einfach physisch selbst, indem Sie die App deinstallieren oder im Dashboard den Panic-Wipe auslösen.

Verantwortlicher Entwickler:

Benedikt Wolfgang Hütter
Schulgasse 4/15, 2700 Wiener Neustadt, Österreich
E-Mail: support@juaris.com
Web: www.juaris.com

