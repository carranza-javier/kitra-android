"""Writes the Google Play store listing texts (play/listing/<locale>/*.txt) and checks Play's length limits.

Play limits: title 30, short description 80, full description 4000 characters.
"Gemini" is only used to describe the feature ("works with your Gemini key"), never as branding.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "play" / "listing"

LISTING = {
    "es-ES": {
        "title": "Kitra: traduce tus capturas",
        "short_description": "Haz una captura, pulsa «Traducir» y lee la traducción. Sin copiar ni pegar.",
        "full_description": """Kitra traduce cualquier texto que veas en la pantalla con el mínimo esfuerzo: haz una captura como siempre, pulsa «Traducir» en el aviso que aparece y lee la traducción en una ventana ligera encima de lo que estabas leyendo. Copia, cierra y sigue.

Sin abrir otra app, sin copiar y pegar, sin seleccionar texto y sin elegir el idioma cada vez.

CÓMO FUNCIONA
• Haz una captura con los botones de siempre.
• Aparece un aviso «Traducir» durante unos segundos.
• Púlsalo: la traducción aparece al momento, párrafo a párrafo.
• También puedes compartir cualquier imagen con Kitra desde la galería u otra app.

TRADUCCIONES NATURALES
Kitra funciona con tu clave gratuita de Gemini: la traducción entiende el contexto, las expresiones y el tono, e ignora los menús, botones, banners y barras de la pantalla. Ideal para leer prensa, mensajes, menús o webs en alemán, francés, italiano, inglés, chino y muchos otros idiomas.

Conseguir la clave es gratis y lleva unos dos minutos: Kitra te guía en tres pasos y la detecta sola cuando la copias.

MODO BÁSICO, SIN CLAVE
¿Sin clave o sin conexión? Kitra traduce igualmente en tu teléfono con un modo básico. Es menos preciso, y Kitra siempre te avisa cuando lo usa.

PRIVACIDAD
• Con tu clave, la captura se envía directamente a Google solo cuando pulsas «Traducir». En el nivel gratuito, Google puede usar ese contenido para mejorar sus productos.
• En modo básico, la captura y su texto no salen del teléfono.
• Kitra no tiene servidores propios, ni cuentas, ni anuncios, ni analíticas, y no guarda tus capturas ni tus traducciones.
• Kitra nunca borra, mueve ni modifica tus capturas.

Kitra está disponible en español, alemán e inglés, y traduce a español, inglés, francés, alemán, italiano, portugués, catalán, neerlandés, polaco, japonés, chino y coreano.""",
    },
    "de-DE": {
        "title": "Kitra: Screenshots übersetzen",
        "short_description": "Bildschirmfoto machen, „Übersetzen“ tippen, lesen. Ohne Kopieren und Einfügen.",
        "full_description": """Kitra übersetzt jeden Text auf deinem Bildschirm mit minimalem Aufwand: Mach wie gewohnt ein Bildschirmfoto, tippe im Hinweis auf „Übersetzen“ und lies die Übersetzung in einem schlanken Fenster über dem, was du gerade liest. Kopieren, schließen, weiterlesen.

Keine andere App öffnen, nichts kopieren und einfügen, keinen Text markieren und nicht jedes Mal die Sprache wählen.

SO FUNKTIONIERT ES
• Mach ein Bildschirmfoto mit den gewohnten Tasten.
• Einige Sekunden lang erscheint ein Hinweis „Übersetzen“.
• Tippe darauf: Die Übersetzung erscheint sofort, Absatz für Absatz.
• Du kannst auch jedes Bild aus der Galerie oder einer anderen App mit Kitra teilen.

NATÜRLICHE ÜBERSETZUNGEN
Kitra funktioniert mit deinem kostenlosen Gemini-Schlüssel: Die Übersetzung versteht Kontext, Redewendungen und Tonfall und ignoriert Menüs, Schaltflächen, Banner und Leisten auf dem Bildschirm. Ideal für Zeitungen, Nachrichten, Speisekarten oder Websites auf Französisch, Italienisch, Englisch, Spanisch, Chinesisch und vielen anderen Sprachen.

Der Schlüssel ist kostenlos und in etwa zwei Minuten eingerichtet: Kitra führt dich in drei Schritten durch und erkennt ihn automatisch, sobald du ihn kopierst.

EINFACHER MODUS, OHNE SCHLÜSSEL
Kein Schlüssel oder keine Verbindung? Kitra übersetzt trotzdem direkt auf deinem Handy im einfachen Modus. Er ist weniger genau, und Kitra weist dich immer darauf hin.

DATENSCHUTZ
• Mit deinem Schlüssel wird das Bildschirmfoto nur beim Tippen auf „Übersetzen“ direkt an Google gesendet. In der kostenlosen Stufe kann Google diese Inhalte zur Verbesserung seiner Produkte nutzen.
• Im einfachen Modus verlassen das Bildschirmfoto und sein Text dein Handy nicht.
• Kitra hat keine eigenen Server, keine Konten, keine Werbung und keine Analysen und speichert weder deine Bildschirmfotos noch deine Übersetzungen.
• Kitra löscht, verschiebt oder verändert deine Bildschirmfotos nie.

Kitra ist auf Deutsch, Spanisch und Englisch verfügbar und übersetzt ins Deutsche, Englische, Französische, Spanische, Italienische, Portugiesische, Katalanische, Niederländische, Polnische, Japanische, Chinesische und Koreanische.""",
    },
    "en-US": {
        "title": "Kitra: Translate screenshots",
        "short_description": "Take a screenshot, tap “Translate” and read it. No copying, no pasting.",
        "full_description": """Kitra translates any text on your screen with minimal effort: take a screenshot as usual, tap “Translate” on the notification that appears and read the translation in a lightweight sheet on top of what you were reading. Copy, close and carry on.

No switching apps, no copying and pasting, no selecting text and no choosing the language every time.

HOW IT WORKS
• Take a screenshot with the usual buttons.
• A “Translate” notification appears for a few seconds.
• Tap it: the translation appears right away, paragraph by paragraph.
• You can also share any image with Kitra from your gallery or another app.

NATURAL TRANSLATIONS
Kitra works with your free Gemini key: translations understand context, idioms and tone, and ignore the menus, buttons, banners and bars on the screen. Great for reading news, messages, menus or websites in German, French, Italian, Spanish, Chinese and many other languages.

Getting the key is free and takes about two minutes: Kitra guides you in three steps and detects the key automatically when you copy it.

BASIC MODE, NO KEY NEEDED
No key or no connection? Kitra still translates on your phone with a basic mode. It is less accurate, and Kitra always tells you when it is being used.

PRIVACY
• With your key, the screenshot is sent directly to Google only when you tap “Translate”. On the free tier, Google may use that content to improve its products.
• In basic mode, the screenshot and its text never leave your phone.
• Kitra has no servers of its own, no accounts, no ads and no analytics, and it does not store your screenshots or translations.
• Kitra never deletes, moves or modifies your screenshots.

Kitra is available in English, Spanish and German, and translates into English, Spanish, French, German, Italian, Portuguese, Catalan, Dutch, Polish, Japanese, Chinese and Korean.""",
    },
}

LIMITS = {"title": 30, "short_description": 80, "full_description": 4000}

if __name__ == "__main__":
    for locale, fields in LISTING.items():
        folder = ROOT / locale
        folder.mkdir(parents=True, exist_ok=True)
        for field, text in fields.items():
            assert len(text) <= LIMITS[field], f"{locale}/{field}: {len(text)} > {LIMITS[field]}"
            (folder / f"{field}.txt").write_text(text + "\n", encoding="utf-8")
            print(f"{locale:6} {field:18} {len(text):5}/{LIMITS[field]}")
