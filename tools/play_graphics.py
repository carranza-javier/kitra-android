"""Generates the Google Play graphics from the app's own launcher icon vector.

Usage:
  python tools/play_graphics.py <Roboto.ttf> <captures_dir>
where <captures_dir> contains store_es/, store_de/, store_en/ with 1_welcome.png, 2_tutorial.png,
3_notification.png and 4_result.png (raw 1080x2400 emulator screenshots).

Outputs (play/graphics/):
  icon-512.png                      512x512, 32-bit PNG
  feature-graphic-<lang>.png        1024x500, 24-bit PNG (no alpha)
  screenshots/<lang>/0N-*.png       1080x1920 (9:16) framed phone screenshots with a caption
"""
import re
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "play" / "graphics"
BRAND = (31, 94, 255)        # @color/kitra_brand
BRAND_DARK = (18, 52, 160)
TINT = (238, 242, 255)
INK = (20, 27, 52)

# Translate glyph of app/src/main/res/drawable/ic_launcher_foreground.xml (48x48 units).
GLYPH = ("M25.74,30.14l-5.08,-5.02 0.06,-0.06c3.48,-3.88 5.96,-8.34 7.42,-13.06H34V8h-14V4h-4v4H2v3.98h22.34"
         "C23,15.84 20.88,19.5 18,22.7 16.14,20.64 14.6,18.38 13.38,16h-4c1.46,3.26 3.46,6.34 5.96,9.12"
         "L5.16,35.16 8,38l10,-10 6.22,6.22 1.52,-4.08zM37,20h-4L24,44h4l2.24,-6h9.5L42,44h4l-9,-24z"
         "M31.76,34l3.24,-8.66L38.24,34h-6.48z")


def parse_path(d):
    """Minimal SVG path parser (M L H V C Z, absolute and relative) -> list of polygons."""
    tokens = re.findall(r"[MmLlHhVvCcZz]|-?\d*\.?\d+", d)
    polys, cur, x, y, cmd, i = [], [], 0.0, 0.0, None, 0
    def num():
        nonlocal i
        v = float(tokens[i]); i += 1; return v
    while i < len(tokens):
        if re.match(r"[A-Za-z]", tokens[i]):
            cmd = tokens[i]; i += 1
            if cmd in "Zz":
                if cur: polys.append(cur); cur = []
                continue
        rel = cmd.islower()
        c = cmd.upper()
        if c == "M":
            nx, ny = num(), num()
            if rel: nx, ny = x + nx, y + ny
            if cur: polys.append(cur)
            cur = [(nx, ny)]; x, y = nx, ny
            cmd = "l" if rel else "L"  # implicit lineto after moveto
        elif c == "L":
            nx, ny = num(), num()
            if rel: nx, ny = x + nx, y + ny
            cur.append((nx, ny)); x, y = nx, ny
        elif c == "H":
            nx = num(); x = x + nx if rel else nx; cur.append((x, y))
        elif c == "V":
            ny = num(); y = y + ny if rel else ny; cur.append((x, y))
        elif c == "C":
            pts = [num() for _ in range(6)]
            if rel: pts = [pts[k] + (x if k % 2 == 0 else y) for k in range(6)]
            x0, y0 = x, y
            for s in range(1, 17):
                t = s / 16
                bx = (1-t)**3*x0 + 3*(1-t)**2*t*pts[0] + 3*(1-t)*t**2*pts[2] + t**3*pts[4]
                by = (1-t)**3*y0 + 3*(1-t)**2*t*pts[1] + 3*(1-t)*t**2*pts[3] + t**3*pts[5]
                cur.append((bx, by))
            x, y = pts[4], pts[5]
    if cur: polys.append(cur)
    return polys


def glyph_mask(size_px):
    """Even-odd filled glyph, anti-aliased by 4x supersampling."""
    ss = 4
    big = size_px * ss
    mask = Image.new("L", (big, big), 0)
    for poly in parse_path(GLYPH):
        layer = Image.new("L", (big, big), 0)
        ImageDraw.Draw(layer).polygon([(px / 48 * big, py / 48 * big) for px, py in poly], fill=255)
        mask = ImageChops.logical_xor(mask.convert("1"), layer.convert("1")).convert("L")
    return mask.resize((size_px, size_px), Image.LANCZOS)


def gradient(w, h, top, bottom):
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for yy in range(h):
        t = yy / max(1, h - 1)
        d.line([(0, yy), (w, yy)], fill=tuple(int(top[k] + (bottom[k] - top[k]) * t) for k in range(3)))
    return img


def font(path, size, weight=700):
    f = ImageFont.truetype(path, size)
    # Set the variable-font axes by name (Roboto exposes Weight and Width).
    values = []
    for axis in f.get_variation_axes():
        name = axis.get("name", b"")
        values.append(weight if name in (b"Weight", "Weight") else axis["default"] if name not in (b"Width", "Width") else 100)
    f.set_variation_by_axes(values)
    return f


def icon_512():
    img = gradient(512, 512, BRAND, BRAND_DARK).convert("RGBA")
    g = 270
    img.paste((255, 255, 255, 255), ((512 - g) // 2, (512 - g) // 2), glyph_mask(g))
    img.save(OUT / "icon-512.png")


TAGLINES = {
    "es": ("Captura. Un toque. Traducido.", "Traducciones naturales de lo que estás leyendo"),
    "de": ("Screenshot. Ein Tipp. Übersetzt.", "Natürliche Übersetzungen von allem, was du liest"),
    "en": ("Screenshot. One tap. Translated.", "Natural translations of whatever you are reading"),
}


def feature_graphic(lang, fpath):
    img = gradient(1024, 500, BRAND, BRAND_DARK)
    d = ImageDraw.Draw(img)
    tile = Image.new("RGBA", (220, 220), (0, 0, 0, 0))
    ImageDraw.Draw(tile).rounded_rectangle([0, 0, 219, 219], radius=52, fill=(255, 255, 255, 255))
    gm = glyph_mask(140)
    tile.paste(BRAND + (255,), (40, 40), gm)
    img.paste(tile, (80, 140), tile)
    d.text((350, 120), "Kitra", font=font(fpath, 120, 800), fill="white")
    d.text((354, 268), TAGLINES[lang][0], font=font(fpath, 44, 600), fill="white")
    d.text((354, 330), TAGLINES[lang][1], font=font(fpath, 28, 400), fill=(214, 224, 255))
    img.save(OUT / f"feature-graphic-{lang}.png")


CAPTIONS = {
    # Recommended upload order. Captions never use the "Gemini" name.
    "es": [("3_notification", "Haz una captura y pulsa «Traducir»"),
           ("4_result", "Traducción natural, al momento"),
           ("1_welcome", "Elige cómo traducir"),
           ("2_tutorial", "Tu clave gratuita en 3 pasos")],
    "de": [("3_notification", "Screenshot machen, „Übersetzen“ tippen"),
           ("4_result", "Natürliche Übersetzung, sofort"),
           ("1_welcome", "Wähle, wie übersetzt wird"),
           ("2_tutorial", "Dein kostenloser Schlüssel in 3 Schritten")],
    "en": [("3_notification", "Take a screenshot, tap “Translate”"),
           ("4_result", "Natural translation, instantly"),
           ("1_welcome", "Choose how to translate"),
           ("2_tutorial", "Your free key in 3 steps")],
}


def framed(src, caption, dst, fpath):
    W, H = 1080, 1920
    canvas = Image.new("RGB", (W, H), TINT)
    d = ImageDraw.Draw(canvas)
    f = font(fpath, 62, 700)
    # Wrap the caption in up to two lines.
    words, lines, line = caption.split(), [], ""
    for w in words:
        test = (line + " " + w).strip()
        if d.textlength(test, font=f) <= W - 120: line = test
        else: lines.append(line); line = w
    lines.append(line)
    y = 90
    for ln in lines:
        d.text(((W - d.textlength(ln, font=f)) / 2, y), ln, font=f, fill=INK)
        y += 80
    shot = Image.open(src).convert("RGB")
    sh = H - y - 120
    sw = int(shot.width * sh / shot.height)
    shot = shot.resize((sw, sh), Image.LANCZOS)
    radius = 44
    mask = Image.new("L", (sw, sh), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, sw - 1, sh - 1], radius=radius, fill=255)
    x0, y0 = (W - sw) // 2, y + 50
    shadow = Image.new("L", (W, H), 0)
    ImageDraw.Draw(shadow).rounded_rectangle([x0 + 8, y0 + 16, x0 + sw + 8, y0 + sh + 16], radius=radius, fill=110)
    shadow = shadow.filter(ImageFilter.GaussianBlur(22))
    canvas.paste((150, 160, 200), (0, 0), shadow)
    border = Image.new("RGB", (sw + 16, sh + 16), INK)
    bmask = Image.new("L", (sw + 16, sh + 16), 0)
    ImageDraw.Draw(bmask).rounded_rectangle([0, 0, sw + 15, sh + 15], radius=radius + 8, fill=255)
    canvas.paste(border, (x0 - 8, y0 - 8), bmask)
    canvas.paste(shot, (x0, y0), mask)
    canvas.save(dst)


def main():
    fpath, captures = sys.argv[1], Path(sys.argv[2])
    (OUT / "screenshots").mkdir(parents=True, exist_ok=True)
    icon_512()
    for lang in ("es", "de", "en"):
        feature_graphic(lang, fpath)
        out_dir = OUT / "screenshots" / lang
        out_dir.mkdir(parents=True, exist_ok=True)
        for n, (name, caption) in enumerate(CAPTIONS[lang], 1):
            framed(captures / f"store_{lang}" / f"{name}.png", caption, out_dir / f"0{n}-{name.split('_', 1)[1]}.png", fpath)
    print("ok")


if __name__ == "__main__":
    main()
