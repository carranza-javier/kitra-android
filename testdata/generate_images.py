"""Genera capturas de prueba (1080x2400) con texto ficticio en alemán, francés, italiano e inglés.

Incluyen barra de estado, botones y un banner de cookies para comprobar que Kitra ignora la interfaz.
Sin datos personales. Uso: python generate_images.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

FONT = "C:/Windows/Fonts/arial.ttf"
OUT = Path(__file__).parent

SAMPLES = {
    "de": ("Zürcher Tagblatt", "Stadtrat will Velowege bis 2030 verdoppeln",
           ["Die Stadt Zürich plant, ihr Velonetz in den nächsten Jahren",
            "massiv auszubauen. Kritiker sprechen von einem Schildbürgerstreich,",
            "Befürworter hingegen von einem längst überfälligen Schritt.",
            "", "Der Entscheid fällt im Frühling."],
           "Wir verwenden Cookies. Alle akzeptieren"),
    "fr": ("La Gazette de Lausanne", "Le Léman bat un record de chaleur en octobre",
           ["Les baigneurs n'en croyaient pas leurs yeux : l'eau du lac a",
            "atteint 19 degrés ce week-end. « C'est du jamais vu »,",
            "s'exclame un pêcheur qui a l'habitude de tirer le diable par la queue.",
            "", "Les experts appellent à la prudence."],
           "Nous utilisons des cookies. Tout accepter"),
    "it": ("Corriere del Ticino", "Lugano, il nuovo ponte apre in anticipo",
           ["I lavori si sono conclusi con tre mesi di anticipo sul previsto.",
            "Il sindaco ha parlato di un risultato che non si vedeva da anni:",
            "«Abbiamo preso due piccioni con una fava».",
            "", "L'inaugurazione è prevista per sabato."],
           "Utilizziamo i cookie. Accetta tutto"),
    "en": ("The Morning Ledger", "Local bakery wins national bread award",
           ["After years of burning the midnight oil, the small family bakery",
            "has finally taken home the top prize. The owners say they are",
            "over the moon and plan to keep their prices as they are.",
            "", "Queues are expected this weekend."],
           "We use cookies. Accept all"),
}


def make(code, site, headline, body, cookie):
    img = Image.new("RGB", (1080, 2400), "white")
    d = ImageDraw.Draw(img)
    small, mid, big, head = (ImageFont.truetype(FONT, s) for s in (34, 40, 46, 64))
    # Barra de estado e interfaz que debe ignorarse.
    d.rectangle([0, 0, 1080, 90], fill="#202124")
    d.text((40, 25), "10:42", font=small, fill="white")
    d.text((900, 25), "87 %", font=small, fill="white")
    d.rectangle([0, 90, 1080, 230], fill="#0b3d91")
    d.text((40, 130), site, font=big, fill="white")
    d.text((960, 130), "\u2630", font=big, fill="white")
    # Contenido principal.
    y = 300
    for line in wrap(headline, head, d, 1000):
        d.text((40, y), line, font=head, fill="black")
        y += 80
    y += 40
    # Los párrafos se ajustan al ancho, como haría una app real (sin texto cortado en el borde).
    for paragraph in " ".join(body).split("  "):
        for line in wrap(paragraph, mid, d, 1000):
            d.text((40, y), line, font=mid, fill="#202124")
            y += 60
        y += 40
    # Banner de cookies y barra de navegación.
    d.rectangle([0, 1950, 1080, 2250], fill="#f1f3f4")
    d.text((40, 2000), cookie, font=mid, fill="black")
    d.rounded_rectangle([40, 2100, 400, 2200], radius=20, fill="#0b3d91")
    d.text((90, 2125), "OK", font=mid, fill="white")
    d.rectangle([0, 2300, 1080, 2400], fill="#202124")
    img.save(OUT / f"screenshot_{code}.png")


def wrap(text, font, draw, width):
    words, lines, cur = text.split(), [], ""
    for w in words:
        test = f"{cur} {w}".strip()
        if draw.textlength(test, font=font) <= width:
            cur = test
        else:
            lines.append(cur)
            cur = w
    return lines + [cur]


if __name__ == "__main__":
    for code, args in SAMPLES.items():
        make(code, *args)
    print("ok")
