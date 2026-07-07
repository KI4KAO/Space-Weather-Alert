#!/usr/bin/env python3
"""
Prepare bundled assets at build time so the app never depends on network at first run:
  - splash image  <- https://ki4kao.net/app1/spaceweather.png  -> res/drawable-nodpi/splash.png
  - launcher icons (all densities) generated from the same PNG
  - notification sound <- https://ki4kao.net/app1/not.wav       -> res/raw/not.wav

Every step has a fallback so the build cannot break if an asset is briefly unavailable.
"""
import os
import struct
import urllib.request

from PIL import Image, ImageDraw

RES = os.path.join("app", "src", "main", "res")
SPLASH_URL = "https://ki4kao.net/app1/spaceweather.png"
SOUND_URL = "https://ki4kao.net/app1/not.wav"

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def fetch(url, timeout=30):
    req = urllib.request.Request(url, headers={"User-Agent": "ci-build/1.0"})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.read()


def load_source():
    try:
        data = fetch(SPLASH_URL)
        tmp = "_src.png"
        with open(tmp, "wb") as f:
            f.write(data)
        img = Image.open(tmp).convert("RGBA")
        print("splash: downloaded", img.size)
        return img
    except Exception as e:
        print("splash download failed, using placeholder:", e)
        img = Image.new("RGBA", (512, 512), (4, 6, 10, 255))
        d = ImageDraw.Draw(img)
        d.ellipse((136, 136, 376, 376), outline=(0, 246, 255, 255), width=10)
        d.ellipse((196, 196, 316, 316), fill=(255, 176, 0, 255))
        return img


def circular(img):
    size = img.size
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size[0], size[1]), fill=255)
    out = Image.new("RGBA", size, (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def main():
    src = load_source()

    # splash (nodpi, keep full resolution but cap to 512 for size)
    os.makedirs(os.path.join(RES, "drawable-nodpi"), exist_ok=True)
    splash = src.copy()
    splash.thumbnail((512, 512), Image.LANCZOS)
    splash.save(os.path.join(RES, "drawable-nodpi", "splash.png"))

    # square base for icons
    base = src.copy()
    side = max(base.size)
    canvas = Image.new("RGBA", (side, side), (4, 6, 10, 255))
    canvas.paste(base, ((side - base.size[0]) // 2, (side - base.size[1]) // 2), base)

    for folder, px in DENSITIES.items():
        os.makedirs(os.path.join(RES, folder), exist_ok=True)
        sq = canvas.resize((px, px), Image.LANCZOS)
        sq.save(os.path.join(RES, folder, "ic_launcher.png"))
        circular(sq).save(os.path.join(RES, folder, "ic_launcher_round.png"))
        print("icons:", folder, px)

    # notification sound
    os.makedirs(os.path.join(RES, "raw"), exist_ok=True)
    dst = os.path.join(RES, "raw", "not.wav")
    try:
        data = fetch(SOUND_URL)
        if len(data) < 44 or data[:4] != b"RIFF":
            raise ValueError("not a WAV")
        with open(dst, "wb") as f:
            f.write(data)
        print("sound: downloaded", len(data), "bytes")
    except Exception as e:
        print("sound download failed, writing short beep fallback:", e)
        write_beep(dst)


def write_beep(path):
    """Write a short 440Hz beep WAV as a fallback notification sound."""
    import math
    sr = 22050
    dur = 0.35
    n = int(sr * dur)
    frames = bytearray()
    for i in range(n):
        v = int(32767 * 0.4 * math.sin(2 * math.pi * 740 * i / sr)
                * math.exp(-3.0 * i / n))
        frames += struct.pack("<h", v)
    data_size = len(frames)
    with open(path, "wb") as f:
        f.write(b"RIFF")
        f.write(struct.pack("<I", 36 + data_size))
        f.write(b"WAVE")
        f.write(b"fmt ")
        f.write(struct.pack("<IHHIIHH", 16, 1, 1, sr, sr * 2, 2, 16))
        f.write(b"data")
        f.write(struct.pack("<I", data_size))
        f.write(frames)


if __name__ == "__main__":
    main()
