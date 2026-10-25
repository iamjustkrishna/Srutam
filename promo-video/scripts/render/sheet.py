"""Tile frames into a contact sheet, labelled with the end of each file name.

    python scripts/render/sheet.py out.png frame1.png frame2.png ...

Portrait frames go 6 to a row, landscape 3 to a row.
"""
import sys

from PIL import Image, ImageDraw, ImageFont

out, files = sys.argv[1], sys.argv[2:]
w0, h0 = Image.open(files[0]).size
portrait = h0 > w0
tw, th, cols = (324, 576, 6) if portrait else (640, 360, 3)
try:
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 18)
except OSError:
    font = ImageFont.load_default()
rows = (len(files) + cols - 1) // cols
sheet = Image.new("RGB", (cols * tw + (cols - 1) * 6, rows * th + (rows - 1) * 6), "white")
for n, f in enumerate(files):
    im = Image.open(f).convert("RGB").resize((tw, th))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 70, 24], fill="black")
    d.text((4, 2), f.rsplit("-", 1)[-1].rsplit(".", 1)[0], font=font, fill="yellow")
    sheet.paste(im, ((n % cols) * (tw + 6), (n // cols) * (th + 6)))
sheet.save(out)
print(out)
