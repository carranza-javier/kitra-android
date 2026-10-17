"""Single source of the UI strings in English (default), Spanish and German.

Run `python tools/strings_source.py` to regenerate app/src/main/res/values*/strings.xml.
Each entry: key -> (en, es, de). Use \n for line breaks; escaping for Android is done automatically.
"""
from pathlib import Path

S = {
    "app_name": ("Kitra", "Kitra", "Kitra"),

    # Screenshot notification
    "channel_screenshots": ("Screenshots", "Capturas de pantalla", "Bildschirmfotos"),
    "channel_screenshots_desc": ("Short notice to translate a screenshot you just took",
                                 "Aviso breve para traducir una captura recién hecha",
                                 "Kurzer Hinweis, um ein gerade erstelltes Bildschirmfoto zu übersetzen"),
    "notif_title": ("Screenshot detected", "Captura detectada", "Bildschirmfoto erkannt"),
    "notif_text": ("Tap to translate it", "Pulsa para traducirla", "Tippe, um es zu übersetzen"),
    "notif_title_picker": ("Took a screenshot?", "¿Has hecho una captura?", "Bildschirmfoto gemacht?"),
    "notif_text_picker": ("Tap and choose the image to translate it", "Toca y elige la imagen para traducirla",
                          "Tippe und wähle das Bild zum Übersetzen"),
    "action_translate": ("Translate", "Traducir", "Übersetzen"),
    "shortcut_share": ("Translate", "Traducir", "Übersetzen"),

    # Result sheet
    "translating": ("Translating…", "Traduciendo…", "Wird übersetzt…"),
    "copy": ("Copy", "Copiar", "Kopieren"),
    "copied": ("Copied", "Copiado", "Kopiert"),
    "close": ("Close", "Cerrar", "Schließen"),
    "retry": ("Retry", "Reintentar", "Erneut versuchen"),
    "open_settings": ("Open settings", "Abrir ajustes", "Einstellungen öffnen"),
    "progress_preparing_ocr": ("Preparing text recognition (first time only)…",
                               "Preparando el reconocimiento de texto (solo la primera vez)…",
                               "Texterkennung wird vorbereitet (nur beim ersten Mal)…"),
    "progress_recognizing": ("Reading the screenshot…", "Leyendo la captura…", "Bildschirmfoto wird gelesen…"),
    "progress_downloading_language": ("Downloading %1$s (about 30 MB, first time only)…",
                                      "Descargando el idioma %1$s (unos 30 MB, solo la primera vez)…",
                                      "%1$s wird heruntergeladen (ca. 30 MB, nur beim ersten Mal)…"),
    "notice_already_in_target": ("The text is already in %1$s.", "El texto ya está en %1$s.", "Der Text ist bereits auf %1$s."),
    "attribution_google_translate": ("Powered by Google Translate", "Con la tecnología de Google Translate",
                                     "Bereitgestellt von Google Translate"),

    # Basic mode banner
    "basic_title": ("Basic translation: it may be inaccurate", "Traducción básica: puede ser imprecisa",
                    "Einfache Übersetzung: kann ungenau sein"),
    "basic_no_key": ("Turn on Gemini for accurate translations.", "Activa Gemini para traducciones precisas.",
                     "Aktiviere Gemini für genaue Übersetzungen."),
    "basic_key_invalid": ("Your Gemini key has stopped working. Check it to get accurate translations again.",
                          "Tu clave de Gemini ha dejado de funcionar. Revísala para volver a tener traducciones precisas.",
                          "Dein Gemini-Schlüssel funktioniert nicht mehr. Prüfe ihn, um wieder genaue Übersetzungen zu erhalten."),
    "basic_offline": ("No internet connection. Kitra will use Gemini again as soon as you are back online.",
                      "Sin conexión a internet. Kitra volverá a usar Gemini en cuanto tengas conexión.",
                      "Keine Internetverbindung. Kitra nutzt Gemini wieder, sobald du online bist."),
    "basic_quota": ("Your Gemini key has used up its free quota for now. Kitra will use Gemini again when it renews.",
                    "Tu clave de Gemini ha agotado su cuota gratuita por ahora. Kitra volverá a usar Gemini cuando se renueve.",
                    "Dein Gemini-Schlüssel hat sein kostenloses Kontingent vorerst aufgebraucht. Kitra nutzt Gemini wieder, sobald es erneuert ist."),
    "basic_cloud_failed": ("Gemini did not respond this time. Kitra will try it again on the next translation.",
                           "Gemini no ha respondido esta vez. Kitra lo volverá a intentar en la próxima traducción.",
                           "Gemini hat diesmal nicht geantwortet. Kitra versucht es bei der nächsten Übersetzung erneut."),
    "basic_action_set_up": ("Turn on Gemini", "Activar Gemini", "Gemini aktivieren"),
    "basic_action_fix_key": ("Check my key", "Revisar mi clave", "Meinen Schlüssel prüfen"),

    # Errors
    "error_invalid_key": ("Your Gemini API key is not valid. Check it in Kitra's settings.",
                          "La clave de Gemini API no es válida. Revísala en los ajustes de Kitra.",
                          "Dein Gemini-API-Schlüssel ist ungültig. Prüfe ihn in den Kitra-Einstellungen."),
    "error_key_type_unsupported": ("Google rejected this type of key. Create a new key in Google AI Studio.",
                                   "Google ha rechazado este tipo de clave. Crea una clave nueva en Google AI Studio.",
                                   "Google hat diesen Schlüsseltyp abgelehnt. Erstelle einen neuen Schlüssel in Google AI Studio."),
    "error_quota": ("You have used up your Gemini API quota for now. Wait a bit or check your limits in Google AI Studio.",
                    "Has agotado la cuota de Gemini API por ahora. Espera un poco o revisa tus límites en Google AI Studio.",
                    "Dein Gemini-API-Kontingent ist vorerst aufgebraucht. Warte etwas oder prüfe deine Limits in Google AI Studio."),
    "error_unavailable": ("Gemini is not available right now. Try again in a few seconds.",
                          "Gemini no está disponible ahora mismo. Inténtalo de nuevo en unos segundos.",
                          "Gemini ist gerade nicht verfügbar. Versuche es in ein paar Sekunden erneut."),
    "error_no_connection": ("No internet connection. Check your network and try again.",
                            "Sin conexión a internet. Comprueba la red e inténtalo de nuevo.",
                            "Keine Internetverbindung. Prüfe dein Netzwerk und versuche es erneut."),
    "error_model": ("The configured model does not exist or is not available for your key.",
                    "El modelo configurado no existe o no está disponible para tu clave.",
                    "Das eingestellte Modell existiert nicht oder ist für deinen Schlüssel nicht verfügbar."),
    "error_blocked": ("Gemini declined to process this image.", "Gemini no ha querido procesar esta imagen.",
                      "Gemini hat die Verarbeitung dieses Bildes abgelehnt."),
    "error_empty": ("No translation was received. Try again.", "No se ha recibido ninguna traducción. Inténtalo de nuevo.",
                    "Es wurde keine Übersetzung empfangen. Versuche es erneut."),
    "error_too_large": ("The image is too large to send.", "La imagen es demasiado grande para enviarla.",
                        "Das Bild ist zu groß zum Senden."),
    "error_image": ("The image could not be opened.", "No se ha podido abrir la imagen.", "Das Bild konnte nicht geöffnet werden."),
    "error_unknown": ("Something went wrong while translating. Try again.", "Algo ha fallado al traducir. Inténtalo de nuevo.",
                      "Beim Übersetzen ist etwas schiefgelaufen. Versuche es erneut."),
    "error_no_text": ("No text was found in the image.", "No se ha encontrado texto en la imagen.",
                      "Im Bild wurde kein Text gefunden."),
    "error_unknown_language": ("The language of the text could not be recognised.",
                               "No se ha podido reconocer el idioma del texto.",
                               "Die Sprache des Textes konnte nicht erkannt werden."),
    "error_language_not_supported": ("This language cannot be translated in basic mode. Turn on Gemini to translate it.",
                                     "Ese idioma no se puede traducir en modo básico. Activa Gemini para traducirlo.",
                                     "Diese Sprache kann im einfachen Modus nicht übersetzt werden. Aktiviere Gemini, um sie zu übersetzen."),
    "error_language_not_downloaded": ("No connection, and this language is not on the phone yet. Connect to the internet once to download it.",
                                      "No hay conexión y este idioma todavía no está en el teléfono. Conéctate a internet una vez para descargarlo.",
                                      "Keine Verbindung, und diese Sprache ist noch nicht auf dem Handy. Verbinde dich einmal mit dem Internet, um sie herunterzuladen."),
    "error_download": ("What is needed to translate could not be downloaded. Check your connection and try again.",
                       "No se ha podido descargar lo necesario para traducir. Comprueba la conexión e inténtalo de nuevo.",
                       "Die zum Übersetzen nötigen Daten konnten nicht geladen werden. Prüfe deine Verbindung und versuche es erneut."),

    # Welcome
    "welcome_title": ("Choose how to translate", "Elige cómo traducir", "Wähle, wie übersetzt wird"),
    "welcome_body": ("With your free Gemini key, Kitra translates with the highest accuracy: it understands context, idioms and tone. "
                     "Without a key, Kitra uses a basic mode that works on your phone, but translations may be inaccurate.",
                     "Con tu clave gratuita de Gemini, Kitra traduce con la máxima precisión: entiende el contexto, las expresiones y el tono. "
                     "Sin clave, Kitra usa un modo básico que funciona en tu móvil, pero las traducciones pueden ser poco precisas.",
                     "Mit deinem kostenlosen Gemini-Schlüssel übersetzt Kitra besonders genau: Es versteht Kontext, Redewendungen und Tonfall. "
                     "Ohne Schlüssel nutzt Kitra einen einfachen Modus auf deinem Handy, die Übersetzungen können aber ungenau sein."),
    "welcome_set_up_key": ("Set up my free key", "Configurar mi clave gratuita", "Meinen kostenlosen Schlüssel einrichten"),
    "welcome_basic_mode": ("Continue in basic mode", "Continuar en modo básico", "Im einfachen Modus fortfahren"),
    "welcome_note": ("You can set it up later in the settings.", "Puedes configurarla más adelante en los ajustes.",
                     "Du kannst ihn später in den Einstellungen einrichten."),

    # Key tutorial
    "tutorial_title": ("Turn on Gemini in 3 steps", "Activa Gemini en 3 pasos", "Gemini in 3 Schritten aktivieren"),
    "tutorial_subtitle": ("It is free and takes about two minutes.", "Es gratis y se tarda unos dos minutos.",
                          "Es ist kostenlos und dauert etwa zwei Minuten."),
    "tutorial_step1_title": ("Open Google AI Studio", "Abre Google AI Studio", "Öffne Google AI Studio"),
    "tutorial_step1_text": ("Tap the button and sign in with your Google account.", "Pulsa el botón y entra con tu cuenta de Google.",
                            "Tippe auf die Schaltfläche und melde dich mit deinem Google-Konto an."),
    "tutorial_step1_button": ("Open Google AI Studio", "Abrir Google AI Studio", "Google AI Studio öffnen"),
    "tutorial_step2_title": ("Create your key", "Crea tu clave", "Erstelle deinen Schlüssel"),
    "tutorial_step2_text": ("Tap Create API key and then the copy button.", "Pulsa Create API key y después el botón de copiar.",
                            "Tippe auf Create API key und dann auf die Kopieren-Schaltfläche."),
    "tutorial_step3_title": ("Come back to Kitra", "Vuelve a Kitra", "Kehre zu Kitra zurück"),
    "tutorial_step3_text": ("Kitra detects the key automatically. You only have to confirm.",
                            "Kitra detecta la clave automáticamente. Solo tienes que confirmar.",
                            "Kitra erkennt den Schlüssel automatisch. Du musst nur bestätigen."),
    "tutorial_detected": ("Key found on the clipboard: %1$s", "Clave encontrada en el portapapeles: %1$s",
                          "Schlüssel in der Zwischenablage gefunden: %1$s"),
    "tutorial_use_key": ("Use this key", "Usar esta clave", "Diesen Schlüssel verwenden"),
    "tutorial_manual_hint": ("Not detected? Paste the key here", "¿No aparece? Pega la clave aquí",
                             "Nicht erkannt? Füge den Schlüssel hier ein"),
    "tutorial_manual_button": ("Check and save", "Comprobar y guardar", "Prüfen und speichern"),
    "tutorial_checking": ("Checking the key with Google…", "Comprobando la clave con Google…", "Schlüssel wird bei Google geprüft…"),
    "tutorial_success_title": ("Done! Gemini is on", "¡Listo! Gemini está activado", "Fertig! Gemini ist aktiviert"),
    "tutorial_success_body": ("From now on Kitra translates with Gemini. Your key is stored encrypted on this phone.",
                              "A partir de ahora Kitra traduce con Gemini. Tu clave se guarda cifrada en este teléfono.",
                              "Ab jetzt übersetzt Kitra mit Gemini. Dein Schlüssel wird verschlüsselt auf diesem Handy gespeichert."),
    "tutorial_success_no_quota": ("The key is saved, but it has no free quota left right now. Kitra will use it as soon as it renews.",
                                  "La clave está guardada, pero ahora mismo no le queda cuota gratuita. Kitra la usará en cuanto se renueve.",
                                  "Der Schlüssel ist gespeichert, hat aber gerade kein kostenloses Kontingent mehr. Kitra nutzt ihn, sobald es erneuert ist."),
    "tutorial_continue": ("Continue", "Continuar", "Weiter"),
    "tutorial_later": ("Not now", "Ahora no", "Jetzt nicht"),
    "tutorial_privacy_title": ("Privacy on the free tier", "Privacidad en el nivel gratuito", "Datenschutz in der kostenlosen Stufe"),
    "tutorial_privacy_body": ("When you tap \u201cTranslate\u201d, Kitra sends the screenshot directly to Google with your key. "
                              "On the free tier of the Gemini API, Google may use what you send to improve its products, and human "
                              "reviewers may read it. Do not translate sensitive information with a free key. With billing enabled "
                              "(paid tier), Google does not use it to improve its products.",
                              "Al pulsar «Traducir», Kitra envía la captura directamente a Google con tu clave. "
                              "En el nivel gratuito de Gemini API, Google puede usar lo que envías para mejorar sus productos y "
                              "revisores humanos pueden leerlo. No traduzcas información sensible con una clave gratuita. Con la "
                              "facturación activada (nivel de pago), Google no lo usa para mejorar sus productos.",
                              "Wenn du auf \u201eÜbersetzen\u201c tippst, sendet Kitra das Bildschirmfoto mit deinem Schlüssel direkt an Google. "
                              "In der kostenlosen Stufe der Gemini API kann Google die gesendeten Inhalte zur Verbesserung seiner Produkte "
                              "nutzen, und menschliche Prüfer können sie lesen. Übersetze mit einem kostenlosen Schlüssel keine sensiblen "
                              "Informationen. Mit aktivierter Abrechnung (kostenpflichtige Stufe) nutzt Google sie nicht dafür."),
    "tutorial_illustration_sign_in": ("Sign in with Google", "Iniciar sesión con Google", "Mit Google anmelden"),
    "tutorial_illustration_copy": ("Copy", "Copiar", "Kopieren"),
    "tutorial_illustration_detected": ("Key detected on the clipboard", "Clave detectada en el portapapeles",
                                       "Schlüssel in der Zwischenablage erkannt"),
    "key_error_invalid": ("Google says this key is not valid. Go back to AI Studio, tap the copy button next to your key and come back here.",
                          "Google dice que esta clave no es válida. Vuelve a AI Studio, pulsa el botón de copiar junto a tu clave y vuelve aquí.",
                          "Laut Google ist dieser Schlüssel ungültig. Gehe zurück zu AI Studio, tippe neben deinem Schlüssel auf Kopieren und komm hierher zurück."),
    "key_error_type_unsupported": ("Google rejected this type of key. This is a known Google issue with some new keys. Try creating "
                                   "another key in AI Studio; meanwhile Kitra keeps working in basic mode.",
                                   "Google ha rechazado este tipo de clave. Es un problema conocido de Google con algunas claves nuevas. "
                                   "Prueba a crear otra clave en AI Studio; mientras tanto, Kitra sigue funcionando en modo básico.",
                                   "Google hat diesen Schlüsseltyp abgelehnt. Das ist ein bekanntes Google-Problem mit einigen neuen "
                                   "Schlüsseln. Erstelle in AI Studio einen weiteren Schlüssel; bis dahin funktioniert Kitra im einfachen Modus."),
    "key_error_offline": ("There is no internet connection. Connect and try again.",
                          "No hay conexión a internet. Conéctate y vuelve a intentarlo.",
                          "Keine Internetverbindung. Verbinde dich und versuche es erneut."),
    "key_error_model": ("The key works, but the configured model is not available for it. Reset the model in Advanced.",
                        "La clave funciona, pero el modelo configurado no está disponible para ella. Restablece el modelo en Avanzado.",
                        "Der Schlüssel funktioniert, aber das eingestellte Modell ist dafür nicht verfügbar. Setze das Modell unter Erweitert zurück."),
    "key_error_google": ("Google is not responding right now. Try again in a few minutes.",
                         "Google no responde ahora mismo. Inténtalo de nuevo en unos minutos.",
                         "Google antwortet gerade nicht. Versuche es in ein paar Minuten erneut."),

    # Settings
    "setup_tagline": ("Take a screenshot and tap \u201cTranslate\u201d. That is all.", "Haz una captura y pulsa «Traducir». Nada más.",
                      "Mach ein Bildschirmfoto und tippe auf \u201eÜbersetzen\u201c. Das ist alles."),
    "setup_gemini_title": ("Gemini", "Gemini", "Gemini"),
    "setup_gemini_on": ("Gemini is on: Kitra translates with your key and the highest accuracy.",
                        "Gemini activado: Kitra traduce con tu clave y la máxima precisión.",
                        "Gemini ist aktiviert: Kitra übersetzt mit deinem Schlüssel besonders genau."),
    "setup_gemini_off": ("Basic mode: translations may be inaccurate. Set up your free Gemini key for accurate translations.",
                         "Modo básico: las traducciones pueden ser imprecisas. Configura tu clave gratuita de Gemini para traducciones precisas.",
                         "Einfacher Modus: Übersetzungen können ungenau sein. Richte deinen kostenlosen Gemini-Schlüssel für genaue Übersetzungen ein."),
    "setup_gemini_change": ("Change key", "Cambiar clave", "Schlüssel ändern"),
    "setup_gemini_remove": ("Remove key", "Quitar clave", "Schlüssel entfernen"),
    "setup_lang_title": ("Translate into", "Traducir al", "Übersetzen in"),
    "setup_pack_explainer": ("For basic mode and offline use, Kitra downloads this language once.",
                             "Para el modo básico y sin conexión, Kitra descarga este idioma una vez.",
                             "Für den einfachen Modus und offline lädt Kitra diese Sprache einmalig herunter."),
    "setup_pack_ready": ("Ready to translate offline.", "Listo para traducir sin conexión.", "Bereit für Offline-Übersetzungen."),
    "setup_pack_downloading": ("Downloading the language (about 30 MB)…", "Descargando el idioma (unos 30 MB)…",
                               "Sprache wird heruntergeladen (ca. 30 MB)…"),
    "setup_pack_waiting_wifi": ("It will download when there is Wi‑Fi (about 30 MB).", "Se descargará cuando haya wifi (unos 30 MB).",
                                "Wird bei WLAN heruntergeladen (ca. 30 MB)."),
    "setup_pack_download_now": ("Download now with mobile data", "Descargar ahora con datos", "Jetzt mit mobilen Daten laden"),
    "setup_pack_failed": ("The language could not be downloaded.", "No se ha podido descargar el idioma.",
                          "Die Sprache konnte nicht heruntergeladen werden."),
    "setup_pack_missing": ("It will download the first time it is needed (about 30 MB).",
                           "Se descargará la primera vez que haga falta (unos 30 MB).",
                           "Wird beim ersten Bedarf heruntergeladen (ca. 30 MB)."),
    "setup_detect_title": ("Notice when you take a screenshot", "Aviso al hacer una captura", "Hinweis bei einem Bildschirmfoto"),
    "setup_detect_desc": ("Kitra detects your new screenshots and shows a \u201cTranslate\u201d notice for a few seconds. "
                          "It never saves, moves or deletes your screenshots.",
                          "Kitra detecta tus capturas nuevas y muestra un aviso «Traducir» durante unos segundos. "
                          "No guarda, mueve ni borra tus capturas.",
                          "Kitra erkennt neue Bildschirmfotos und zeigt einige Sekunden lang einen \u201eÜbersetzen\u201c-Hinweis. "
                          "Bildschirmfotos werden nie gespeichert, verschoben oder gelöscht."),
    "setup_detect_title_picker": ("Notice when you save an image", "Aviso al guardar una imagen", "Hinweis beim Speichern eines Bildes"),
    "setup_detect_desc_picker": ("When a new image appears (for example, a screenshot), Kitra shows a \u201cTranslate\u201d notice. "
                                 "Tap it and choose the image: Kitra only sees the image you choose.",
                                 "Cuando aparece una imagen nueva (por ejemplo, una captura), Kitra muestra un aviso «Traducir». "
                                 "Tócalo y elige la imagen: Kitra solo ve la imagen que eliges.",
                                 "Wenn ein neues Bild erscheint (z. B. ein Bildschirmfoto), zeigt Kitra einen \u201eÜbersetzen\u201c-Hinweis. "
                                 "Tippe darauf und wähle das Bild: Kitra sieht nur das gewählte Bild."),
    "setup_perm_needed": ("It needs permission to see your photos (only to detect new screenshots) and to show notifications.",
                          "Necesita permiso para ver tus fotos (solo para detectar capturas nuevas) y para mostrar avisos.",
                          "Benötigt Zugriff auf deine Fotos (nur um neue Bildschirmfotos zu erkennen) und auf Benachrichtigungen."),
    "setup_perm_grant": ("Grant permissions", "Conceder permisos", "Berechtigungen erteilen"),
    "setup_perm_partial": ("You allowed access to only some photos, so Kitra cannot see new screenshots. Choose \u201cAllow all\u201d "
                           "in Photos and videos, or share images with Kitra.",
                           "Has dado acceso solo a algunas fotos, así que Kitra no puede ver las capturas nuevas. Elige «Permitir todo» "
                           "en Fotos y vídeos, o comparte las imágenes con Kitra.",
                           "Du hast nur Zugriff auf einige Fotos erlaubt, daher sieht Kitra keine neuen Bildschirmfotos. Wähle "
                           "\u201eAlle zulassen\u201c bei Fotos und Videos oder teile Bilder mit Kitra."),
    "setup_perm_denied": ("Permission denied. You can turn it on in the system settings, or share any image with Kitra.",
                          "Permiso denegado. Puedes activarlo en los ajustes del sistema, o compartir cualquier imagen con Kitra.",
                          "Berechtigung verweigert. Du kannst sie in den Systemeinstellungen aktivieren oder Bilder mit Kitra teilen."),
    "setup_notif_denied": ("Notifications are off for Kitra. Turn them on in the system settings.",
                           "Los avisos están desactivados para Kitra. Actívalos en los ajustes del sistema.",
                           "Benachrichtigungen sind für Kitra deaktiviert. Aktiviere sie in den Systemeinstellungen."),
    "setup_perm_ok": ("Ready. Take a screenshot to try it.", "Listo. Haz una captura para probarlo.",
                      "Fertig. Mach ein Bildschirmfoto, um es auszuprobieren."),
    "setup_share_title": ("Also from \u201cShare\u201d", "También desde «Compartir»", "Auch über \u201eTeilen\u201c"),
    "setup_share_desc": ("Share any image with Kitra to translate it, even if it is not a screenshot.",
                         "Comparte cualquier imagen con Kitra para traducirla, aunque no sea una captura.",
                         "Teile ein beliebiges Bild mit Kitra, um es zu übersetzen – auch wenn es kein Bildschirmfoto ist."),
    "setup_privacy_title": ("Privacy", "Privacidad", "Datenschutz"),
    "setup_privacy_body": ("• With your Gemini key, the screenshot is sent directly to Google (Gemini API) only when you tap \u201cTranslate\u201d. "
                           "On the free tier, Google may use it to improve its products and human reviewers may read it.\n"
                           "• In basic mode, the screenshot and its text never leave your phone. Google\u2019s on-device components (ML Kit) "
                           "send technical diagnostics to Google, never the image or the text.\n"
                           "• Kitra has no servers of its own and no analytics, and it does not store screenshots or translations.",
                           "• Con tu clave de Gemini, la captura se envía directamente a Google (Gemini API) solo cuando pulsas «Traducir». "
                           "En el nivel gratuito, Google puede usarla para mejorar sus productos y revisores humanos pueden leerla.\n"
                           "• En modo básico, la captura y su texto no salen del teléfono. Los componentes de Google que traducen en el "
                           "teléfono (ML Kit) envían a Google datos técnicos de diagnóstico, nunca la imagen ni el texto.\n"
                           "• Kitra no tiene servidores propios ni analíticas y no guarda capturas ni traducciones.",
                           "• Mit deinem Gemini-Schlüssel wird das Bildschirmfoto nur beim Tippen auf \u201eÜbersetzen\u201c direkt an Google "
                           "(Gemini API) gesendet. In der kostenlosen Stufe kann Google es zur Verbesserung seiner Produkte nutzen, und "
                           "menschliche Prüfer können es lesen.\n"
                           "• Im einfachen Modus verlassen das Bildschirmfoto und sein Text nie dein Handy. Die Google-Komponenten auf dem "
                           "Gerät (ML Kit) senden technische Diagnosedaten an Google, nie das Bild oder den Text.\n"
                           "• Kitra hat keine eigenen Server und keine Analysen und speichert weder Bildschirmfotos noch Übersetzungen."),
    "setup_privacy_policy": ("Read the privacy policy", "Ver la política de privacidad", "Datenschutzerklärung lesen"),
    "setup_advanced": ("Advanced", "Avanzado", "Erweitert"),
    "setup_diagnostics_title": ("Diagnostics", "Diagnóstico", "Diagnose"),
    "setup_diagnostics_desc": ("Which engines are available and which one was used for each translation.",
                               "Qué motores hay disponibles y cuál se ha usado en cada traducción.",
                               "Welche Engines verfügbar sind und welche bei jeder Übersetzung genutzt wurde."),
    "setup_diagnostics_open": ("Open diagnostics", "Abrir diagnóstico", "Diagnose öffnen"),
    "setup_basic_engine_title": ("Basic mode engine", "Motor del modo básico", "Engine des einfachen Modus"),
    "setup_basic_engine_desc": ("Without Gemini, Kitra translates on the phone: with Gemini Nano if the phone has it, otherwise with Google\u2019s on-device translator (ML Kit).",
                                "Sin Gemini, Kitra traduce en el teléfono: con Gemini Nano si el móvil lo tiene; si no, con el traductor de Google en el teléfono (ML Kit).",
                                "Ohne Gemini übersetzt Kitra auf dem Handy: mit Gemini Nano, falls vorhanden, sonst mit Googles Übersetzer auf dem Gerät (ML Kit)."),
    "setup_nano_available": ("Gemini Nano: available on this phone.", "Gemini Nano: disponible en este teléfono.",
                             "Gemini Nano: auf diesem Handy verfügbar."),
    "setup_nano_downloadable": ("Gemini Nano: can be downloaded (managed by the system).",
                                "Gemini Nano: se puede descargar (lo gestiona el sistema).",
                                "Gemini Nano: kann heruntergeladen werden (vom System verwaltet)."),
    "setup_nano_downloading": ("Gemini Nano: downloading…", "Gemini Nano: descargando…", "Gemini Nano: wird heruntergeladen…"),
    "setup_nano_download": ("Download Gemini Nano", "Descargar Gemini Nano", "Gemini Nano herunterladen"),
    "setup_nano_unavailable": ("Gemini Nano: not available on this phone.", "Gemini Nano: no disponible en este teléfono.",
                               "Gemini Nano: auf diesem Handy nicht verfügbar."),
    "setup_packs_title": ("Downloaded languages", "Idiomas descargados", "Heruntergeladene Sprachen"),
    "setup_packs_empty": ("None yet.", "Ninguno todavía.", "Noch keine."),
    "setup_pack_delete": ("Delete", "Borrar", "Löschen"),
    "setup_model": ("Gemini model", "Modelo de Gemini", "Gemini-Modell"),
    "setup_model_desc": ("Default: %1$s. Only change it if you know what you are doing.",
                         "Predeterminado: %1$s. Cámbialo solo si sabes lo que haces.",
                         "Standard: %1$s. Nur ändern, wenn du weißt, was du tust."),
    "setup_legal_title": ("Translation legal notice", "Aviso legal de la traducción", "Rechtlicher Hinweis zur Übersetzung"),
    "setup_legal_body": ("Basic-mode translations use Google Translate (ML Kit).\n\nTHIS SERVICE MAY CONTAIN TRANSLATIONS POWERED BY GOOGLE. "
                         "GOOGLE DISCLAIMS ALL WARRANTIES RELATED TO THE TRANSLATIONS, EXPRESS OR IMPLIED, INCLUDING ANY WARRANTIES OF "
                         "ACCURACY, RELIABILITY, AND ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.",
                         "Las traducciones del modo básico usan Google Translate (ML Kit).\n\nTHIS SERVICE MAY CONTAIN TRANSLATIONS POWERED BY GOOGLE. "
                         "GOOGLE DISCLAIMS ALL WARRANTIES RELATED TO THE TRANSLATIONS, EXPRESS OR IMPLIED, INCLUDING ANY WARRANTIES OF "
                         "ACCURACY, RELIABILITY, AND ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.",
                         "Übersetzungen im einfachen Modus nutzen Google Translate (ML Kit).\n\nTHIS SERVICE MAY CONTAIN TRANSLATIONS POWERED BY GOOGLE. "
                         "GOOGLE DISCLAIMS ALL WARRANTIES RELATED TO THE TRANSLATIONS, EXPRESS OR IMPLIED, INCLUDING ANY WARRANTIES OF "
                         "ACCURACY, RELIABILITY, AND ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT."),
    "setup_debug_fake": ("Fake provider (debug only)", "Proveedor falso (solo depuración)", "Test-Anbieter (nur Debug)"),

    # Diagnostics
    "diag_back": ("← Back", "← Volver", "← Zurück"),
    "diag_yes": ("Yes", "Sí", "Ja"),
    "diag_no": ("No", "No", "Nein"),
    "diag_engines_title": ("Available engines", "Motores disponibles", "Verfügbare Engines"),
    "diag_key": ("Gemini key set up", "Clave de Gemini configurada", "Gemini-Schlüssel eingerichtet"),
    "diag_online": ("Internet connection", "Conexión a internet", "Internetverbindung"),
    "diag_nano": ("Gemini Nano", "Gemini Nano", "Gemini Nano"),
    "diag_nano_downloadable": ("Downloadable", "Descargable", "Herunterladbar"),
    "diag_nano_downloading": ("Downloading", "Descargando", "Wird geladen"),
    "diag_ocr": ("On-device text recognition ready", "Reconocimiento de texto listo", "Texterkennung bereit"),
    "diag_packs": ("Languages for basic mode", "Idiomas del modo básico", "Sprachen für den einfachen Modus"),
    "diag_variant": ("Version", "Versión", "Version"),
    "diag_log_title": ("Engine used in each translation", "Motor usado en cada traducción", "Genutzte Engine pro Übersetzung"),
    "diag_log_empty": ("No translations yet.", "Todavía no hay traducciones.", "Noch keine Übersetzungen."),
    "diag_log_clear": ("Clear history", "Borrar historial", "Verlauf löschen"),
    "diag_reason": ("Basic mode: %1$s", "Modo básico: %1$s", "Einfacher Modus: %1$s"),
    "diag_first_text": ("first text %1$d ms", "primer texto %1$d ms", "erster Text %1$d ms"),
    "diag_total": ("total %1$d ms", "total %1$d ms", "gesamt %1$d ms"),
    "diag_error": ("Error: %1$s", "Error: %1$s", "Fehler: %1$s"),
    "engine_cloud": ("Gemini (cloud)", "Gemini (nube)", "Gemini (Cloud)"),
    "engine_nano": ("Gemini Nano (on device)", "Gemini Nano (en el teléfono)", "Gemini Nano (auf dem Gerät)"),
    "engine_mlkit": ("ML Kit (on device)", "ML Kit (en el teléfono)", "ML Kit (auf dem Gerät)"),
    "engine_fake": ("Fake provider", "Proveedor falso", "Test-Anbieter"),
    "engine_none": ("—", "—", "—"),
    "reason_no_key": ("no key", "sin clave", "kein Schlüssel"),
    "reason_offline": ("offline", "sin conexión", "offline"),
    "reason_quota": ("quota used up", "cuota agotada", "Kontingent aufgebraucht"),
    "reason_key_invalid": ("key not working", "la clave no funciona", "Schlüssel funktioniert nicht"),
    "reason_cloud_failed": ("Gemini failed", "Gemini ha fallado", "Gemini fehlgeschlagen"),
}


def escape(text: str) -> str:
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = text.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    if text.startswith("@") or text.startswith("?"):
        text = "\\" + text
    return text


def write(folder: str, index: int) -> None:
    lines = ['<?xml version="1.0" encoding="utf-8"?>',
             "<!-- Generated by tools/strings_source.py. Edit that file, not this one. -->",
             "<resources>"]
    for key, values in S.items():
        attrs = ' translatable="false"' if key == "app_name" and index == 0 else ""
        if key == "app_name" and index != 0:
            continue
        lines.append(f'    <string name="{key}"{attrs}>{escape(values[index])}</string>')
    lines.append("</resources>")
    out = Path(__file__).resolve().parent.parent / "app/src/main/res" / folder / "strings.xml"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    write("values", 0)
    write("values-es", 1)
    write("values-de", 2)
    print(f"{len(S)} strings x 3 languages")
