import os
import re
import xml.etree.ElementTree as ET

def clean_strings_xml(file_path):
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()

        # 1. Prozentzeichen korrigieren (Zeile für Zeile, außer bei formatted="false")
        lines = content.splitlines()
        new_lines = []
        for line in lines:
            if "<string" in line and 'formatted="false"' not in line:
                # Ersetzt einzelne % durch %%, sofern es kein gültiger Formatbezeichner ist (%s, %d, %1$s etc.)
                updated_line = re.sub(r'(?<!%)\%(?![0-9]+\$[ds]|[%dsdf])', '%%', line)
            else:
                updated_line = line
            new_lines.append(updated_line)
        content_fixed_pct = "\n".join(new_lines) + "\n"

        # Temporäre Datei schreiben für den XML-Parser
        temp_path = file_path + ".tmp"
        with open(temp_path, "w", encoding="utf-8") as f:
            f.write(content_fixed_pct)

        # 2. XML einlesen und Duplikate entfernen
        tree = ET.parse(temp_path)
        root = tree.getroot()
       
        seen_names = set()
        new_children = []
       
        for child in root:
            if child.tag == 'string':
                name = child.get('name')
                if name in seen_names:
                    print(f" [Duplikat entfernt] {name}")
                    continue
                seen_names.add(name)
            new_children.append(child)
           
        root[:] = new_children
       
        # Datei sauber mit UTF-8 und XML-Deklaration zurückschreiben
        tree.write(file_path, encoding="utf-8", xml_declaration=True)
        
        # Temporäre Datei aufräumen
        if os.path.exists(temp_path):
            os.remove(temp_path)
            
        print(f"[Erfolgreich] {file_path} bereinigt (Duplikate & Prozentzeichen).\n")
    except Exception as e:
        print(f"[Fehler] Konnte {file_path} nicht verarbeiten: {e}")
        if os.path.exists(file_path + ".tmp"):
            os.remove(file_path + ".tmp")

def main():
    res_dir = "app/src/main/res"
    if not os.path.exists(res_dir):
        print("Fehler: 'app/src/main/res' wurde nicht gefunden. Bitte im Root-Verzeichnis des Projekts ausführen!")
        return
       
    print("Starte automatische Bereinigung aller strings.xml-Dateien...\n")
   
    # Durchsuche alle Unterordner (values, values-de, values-ar, values-id, etc.)
    for root, dirs, files in os.walk(res_dir):
        if "values" in os.path.basename(root):
            for file in files:
                if file == "strings.xml":
                    full_path = os.path.join(root, file)
                    print(f"Verarbeite: {full_path}")
                    clean_strings_xml(full_path)

    print("Fertig! Alle Sprachdateien wurden von Duplikaten befreit und auf korrekte Prozentzeichen geprüft.")

if __name__ == "__main__":
    main()



