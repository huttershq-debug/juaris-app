import os
import xml.etree.ElementTree as ET

def clean_strings_xml(file_path):
    try:
        # XML einlesen (mit erhaltem Encoding)
        tree = ET.parse(file_path)
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
        print(f"[Erfolgreich] {file_path} bereinigt.\n")
    except Exception as e:
        print(f"[Fehler] Konnte {file_path} nicht verarbeiten: {e}")

def main():
    res_dir = "app/src/main/res"
    if not os.path.exists(res_dir):
        print("Fehler: 'app/src/main/res' wurde nicht gefunden. Bitte im Root-Verzeichnis des Projekts ausführen!")
        return
        
    print("Starte automatische Bereinigung aller strings.xml-Dateien...\n")
    
    # Durchsuche alle Unterordner (values, values-de, values-fr, etc.)
    for root, dirs, files in os.walk(res_dir):
        if "values" in os.path.basename(root):
            for file in files:
                if file == "strings.xml":
                    full_path = os.path.join(root, file)
                    print(f"Verarbeite: {full_path}")
                    clean_strings_xml(full_path)

    print("Fertig! Alle Sprachdateien wurden von Duplikaten befreit.")

if __name__ == "__main__":
    main()

