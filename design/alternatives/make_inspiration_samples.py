#!/usr/bin/env python3
"""Generate 3 serene 512x320 scenic editorial sample photos for Verdant Folio's Inspiration Vault."""

from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path

ASSETS_DIR = Path(__file__).resolve().parent / "assets"


def clamp(v: float, lo: float = 0.0, hi: float = 255.0) -> int:
    return int(max(lo, min(hi, round(v))))


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def lerp_rgb(c1: tuple[int, int, int], c2: tuple[int, int, int], t: float) -> tuple[float, float, float]:
    return (lerp(c1[0], c2[0], t), lerp(c1[1], c2[1], t), lerp(c1[2], c2[2], t))


def write_png_rgb(path: Path, w: int, h: int, rgb: bytearray) -> None:
    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    stride = w * 3
    raw = bytearray(h * (1 + stride))
    for y in range(h):
        raw[y * (1 + stride)] = 0
        raw[y * (1 + stride) + 1 : (y + 1) * (1 + stride)] = rgb[y * stride : (y + 1) * stride]
    ihdr = struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)
    comp = zlib.compress(bytes(raw), level=6)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", comp) + chunk(b"IEND", b""))


def make_alpine_sunrise(w: int = 512, h: int = 320) -> None:
    """Misty mountain ridges under a glowing golden-apricot sunrise and serene lake reflection."""
    out = bytearray(w * h * 3)
    water_y = int(h * 0.72)

    sky_top = (36, 72, 62)
    sky_mid = (132, 160, 134)
    sky_bot = (242, 206, 146)
    sun_col = (255, 232, 178)

    ridges = [
        (0.45, 22.0, (82, 114, 98), 0.11, 0.7),
        (0.55, 26.0, (56, 89, 74), 0.16, 2.3),
        (0.64, 18.0, (32, 62, 50), 0.22, 4.1),
        (0.71, 9.0, (20, 42, 34), 0.30, 1.2),
    ]

    sun_x, sun_y = w * 0.64, h * 0.39
    for y in range(h):
        row = y * w * 3
        is_water = y >= water_y
        sample_y = (water_y - (y - water_y) * 1.15) if is_water else float(y)
        sy_clamped = max(0.0, min(float(water_y), sample_y))
        t_sky = sy_clamped / water_y

        for x in range(w):
            # Layered warm sunrise sky gradient
            if t_sky < 0.55:
                r, g, b = lerp_rgb(sky_top, sky_mid, t_sky / 0.55)
            else:
                r, g, b = lerp_rgb(sky_mid, sky_bot, (t_sky - 0.55) / 0.45)

            # Glowing sun + solar bloom
            dx = x - sun_x
            dy = sy_clamped - sun_y
            d_sun = math.hypot(dx, dy)
            glow = math.exp(-d_sun / 75.0) * 0.55
            r = lerp(r, sun_col[0], glow)
            g = lerp(g, sun_col[1], glow)
            b = lerp(b, sun_col[2], glow)
            if d_sun < 26.0:
                edge = min(1.0, (26.0 - d_sun) / 3.0)
                r = lerp(r, 255.0, edge * 0.92)
                g = lerp(g, 240.0, edge * 0.92)
                b = lerp(b, 198.0, edge * 0.92)

            # Mountain ridges
            xn = x / w * 8.0
            for base_f, amp, col, mist_amt, ph in ridges:
                ry = (
                    h * base_f
                    - amp * math.sin(xn * 0.8 + ph)
                    - amp * 0.48 * math.cos(xn * 1.9 + ph * 1.7)
                    - amp * 0.22 * math.sin(xn * 4.3 + ph)
                )
                if sy_clamped >= ry:
                    # Atmospheric morning mist at ridge base
                    dist_down = min(1.0, max(0.0, (water_y - sy_clamped) / 46.0))
                    r, g, b = lerp_rgb((col[0], col[1], col[2]), (220, 198, 152), (1.0 - dist_down) * mist_amt)

            if is_water:
                # Shimmering horizontal lake reflection ripples
                depth = (y - water_y) / (h - water_y)
                ripple = 0.5 + 0.5 * math.sin(y * 0.85 + math.sin(x * 0.08 + y * 0.2) * 2.5)
                r, g, b = lerp_rgb((clamp(r), clamp(g), clamp(b)), (24, 48, 39), 0.42 + depth * 0.28)
                # Golden sun shimmer path across ripples
                shimmer = math.exp(-(abs(x - sun_x) / (26.0 + depth * 40.0)) ** 2) * ripple * 0.65
                r = lerp(r, 248.0, shimmer)
                g = lerp(g, 212.0, shimmer)
                b = lerp(b, 146.0, shimmer)

            idx = row + x * 3
            out[idx] = clamp(r)
            out[idx + 1] = clamp(g)
            out[idx + 2] = clamp(b)

    write_png_rgb(ASSETS_DIR / "photo_alpine_sunrise.png", w, h, out)
    print("Generated photo_alpine_sunrise.png")


def make_cedar_grove(w: int = 512, h: int = 320) -> None:
    """Tall misty redwood trunks with glowing diagonal morning sunbeams and ferns."""
    out = bytearray(w * h * 3)
    bg_top = (28, 56, 44)
    bg_bot = (132, 162, 134)

    for y in range(h):
        ty = y / h
        base_r, base_g, base_b = lerp_rgb(bg_top, bg_bot, ty ** 0.9)
        row = y * w * 3
        for x in range(w):
            u = x * 0.76 - y * 0.65
            beam = (
                0.58 * math.exp(-((u - 80.0) / 42.0) ** 2)
                + 0.48 * math.exp(-((u - 210.0) / 56.0) ** 2)
                + 0.38 * math.exp(-((u + 35.0) / 46.0) ** 2)
            ) * (1.0 - ty * 0.30)
            r = lerp(base_r, 248.0, beam * 0.72)
            g = lerp(base_g, 224.0, beam * 0.72)
            b = lerp(base_b, 168.0, beam * 0.72)

            hill = h * 0.80 + 16.0 * math.sin(x * 0.014 + 0.8) + 5.0 * math.cos(x * 0.04)
            if y >= hill:
                # Forest floor fern tufts
                tuft = 6.0 * abs(math.sin(x * 0.11))
                if y >= hill - tuft * 0.4:
                    r, g, b = lerp_rgb((22, 48, 36), (clamp(r), clamp(g), clamp(b)), 0.18)

            idx = row + x * 3
            out[idx] = clamp(r)
            out[idx + 1] = clamp(g)
            out[idx + 2] = clamp(b)

    # Pure clean towering redwood trunks in far haze + foreground depth
    trees = [
        (46, 10, (54, 86, 68)),
        (114, 7, (64, 96, 77)),
        (234, 9, (52, 84, 66)),
        (338, 8, (60, 92, 74)),
        (438, 11, (48, 78, 62)),
        (82, 15, (23, 46, 35)),
        (184, 21, (17, 36, 27)),
        (292, 13, (26, 51, 39)),
        (398, 18, (19, 39, 30)),
    ]
    for tx, half_w, col in trees:
        for y in range(h):
            row = y * w * 3
            taper = half_w * (0.72 + 0.32 * (y / h))
            sway = 2.5 * math.sin(y * 0.012 + tx)
            cx = tx + sway
            x0 = max(0, int(cx - taper - 2))
            x1 = min(w, int(cx + taper + 3))
            # Subtle mist fade near bottom of distant trees
            mist = max(0.0, (y - h * 0.65) / (h * 0.35)) * (0.35 if col[0] > 40 else 0.10)
            for x in range(x0, x1):
                dist = abs(x - cx)
                edge = max(0.0, min(1.0, (taper - dist) * 0.85))
                if edge > 0:
                    eff = edge * (1.0 - mist)
                    idx = row + x * 3
                    out[idx] = clamp(lerp(out[idx], col[0], eff))
                    out[idx + 1] = clamp(lerp(out[idx + 1], col[1], eff))
                    out[idx + 2] = clamp(lerp(out[idx + 2], col[2], eff))

    write_png_rgb(ASSETS_DIR / "photo_cedar_grove.png", w, h, out)
    print("Generated photo_cedar_grove.png")


def make_coastal_horizon(w: int = 512, h: int = 320) -> None:
    """Warm golden-terracotta sunset over a calm eucalyptus ocean & smooth coastal headland."""
    out = bytearray(w * h * 3)
    horizon_y = int(h * 0.58)
    sun_x, sun_y = w * 0.34, horizon_y - 34.0

    for y in range(h):
        row = y * w * 3
        is_sea = y >= horizon_y
        ty = y / horizon_y if not is_sea else (y - horizon_y) / (h - horizon_y)

        for x in range(w):
            if not is_sea:
                if ty < 0.52:
                    r, g, b = lerp_rgb((44, 66, 70), (146, 134, 118), ty / 0.52)
                else:
                    r, g, b = lerp_rgb((146, 134, 118), (235, 174, 126), (ty - 0.52) / 0.48)
                d_sun = math.hypot(x - sun_x, y - sun_y)
                glow = math.exp(-d_sun / 82.0) * 0.55
                r = lerp(r, 250.0, glow)
                g = lerp(g, 206.0, glow)
                b = lerp(b, 140.0, glow)
                if d_sun < 28.0:
                    e = min(1.0, (28.0 - d_sun) / 3.0)
                    r, g, b = lerp(r, 252.0, e), lerp(g, 224.0, e), lerp(b, 168.0, e)
            else:
                r, g, b = lerp_rgb((66, 94, 86), (22, 42, 36), ty ** 0.75)
                wave = 0.5 + 0.5 * math.sin(y * 1.15 + math.sin(x * 0.06 + y * 0.15) * 3.0)
                sun_track = math.exp(-((x - sun_x) / (28.0 + ty * 74.0)) ** 2) * wave * 0.78
                r = lerp(r, 245.0, sun_track)
                g = lerp(g, 190.0, sun_track)
                b = lerp(b, 124.0, sun_track)

            # Smooth tapering coastal headland silhouette descending cleanly to horizon
            taper = max(0.0, min(1.0, (x - w * 0.40) / (w * 0.18)))
            head_y = horizon_y - taper * (44.0 * math.exp(-((x - w * 0.90) / 155.0) ** 2) + 4.0 * (1.0 + math.sin(x * 0.045)))
            if head_y <= y <= horizon_y + 1:
                r, g, b = (28, 48, 42)

            idx = row + x * 3
            out[idx] = clamp(r)
            out[idx + 1] = clamp(g)
            out[idx + 2] = clamp(b)

    write_png_rgb(ASSETS_DIR / "photo_coastal_horizon.png", w, h, out)
    print("Generated photo_coastal_horizon.png")


if __name__ == "__main__":
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    make_alpine_sunrise()
    make_cedar_grove()
    make_coastal_horizon()
