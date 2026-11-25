#!/usr/bin/env python3
"""Six more calm 512x320 stock Inspiration scenes, same procedural style as make_inspiration_samples.py.

Writes PNGs into the app's drawable-nodpi (pure Python, no PIL); then `cwebp -q 86 X.png -o X.webp` and delete the PNG.
"""

from __future__ import annotations

import math
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from make_inspiration_samples import clamp, lerp, lerp_rgb, write_png_rgb  # noqa: E402

OUT_DIR = Path(__file__).resolve().parents[2] / "app/src/main/res/drawable-nodpi"
W, H = 512, 320


def smooth(t: float) -> float:
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def ridge(x: float, seed: float, base: float, amp: float, freq: float) -> float:
    """Soft mountain/hill silhouette height (in px from top) at column x."""
    return base + amp * (
        0.55 * math.sin(x * freq + seed)
        + 0.3 * math.sin(x * freq * 2.3 + seed * 1.7)
        + 0.15 * math.sin(x * freq * 5.1 + seed * 0.6)
    )


def render(name: str, shade) -> None:
    out = bytearray(W * H * 3)
    rng = random.Random(name)
    grain = [rng.uniform(-3.5, 3.5) for _ in range(W * H)]
    for y in range(H):
        for x in range(W):
            r, g, b = shade(x, y)
            n = grain[y * W + x]
            i = (y * W + x) * 3
            out[i], out[i + 1], out[i + 2] = clamp(r + n), clamp(g + n), clamp(b + n)
    write_png_rgb(OUT_DIR / f"{name}.png", W, H, out)
    print(f"Generated {name}.png")


def misty_lake(x: int, y: int):
    sky = lerp_rgb((236, 205, 196), (176, 196, 205), y / 150)
    far = ridge(x, 1.0, 132, 10, 0.018)
    near = ridge(x, 4.0, 158, 14, 0.031)
    color = sky
    if y > far:
        color = lerp_rgb((150, 170, 172), sky, 0.45)
    if y > near:
        color = (92, 118, 112)
    if y > 186:  # lake with reflection and mist
        t = (y - 186) / (H - 186)
        refl = lerp_rgb((176, 196, 205), (120, 150, 156), t)
        ripple = 6 * math.sin(y * 0.9 + x * 0.04)
        color = (refl[0] + ripple, refl[1] + ripple, refl[2] + ripple)
    mist = math.exp(-((y - 175) / 22) ** 2) * 0.55
    return lerp_rgb(tuple(int(c) for c in color), (240, 240, 236), mist)


def desert_dunes(x: int, y: int):
    sky = lerp_rgb((246, 214, 168), (233, 160, 120), y / 170)
    sun_d = math.hypot(x - 360, y - 92)
    if sun_d < 30:
        sky = lerp_rgb((255, 238, 205), sky, sun_d / 30)
    color = sky
    for k, (base, amp, freq, light, dark) in enumerate([
        (178, 16, 0.012, (224, 158, 104), (196, 124, 82)),
        (214, 20, 0.017, (214, 140, 88), (176, 102, 66)),
        (258, 22, 0.022, (198, 120, 74), (150, 84, 54)),
    ]):
        top = ridge(x, k * 2.1 + 0.3, base, amp, freq)
        if y > top:
            slope = math.cos(x * freq + k * 2.1 + 0.3)
            color = lerp_rgb(light, dark, smooth(0.5 + 0.5 * slope))
    return color


def starry_meadow(x: int, y: int):
    sky = lerp_rgb((22, 34, 58), (58, 72, 104), y / 220)
    rnd = random.Random(x * 7919 + y * 104729)
    if y < 200 and rnd.random() < 0.0035:
        sky = (238, 236, 214)
    moon_d = math.hypot(x - 110, y - 70)
    if moon_d < 18:
        sky = (240, 232, 200)
    elif moon_d < 60:
        sky = lerp_rgb((84, 96, 128), sky, smooth(moon_d / 60))
    hill = ridge(x, 2.5, 238, 12, 0.014)
    if y > hill:
        return lerp_rgb((34, 58, 44), (20, 36, 28), (y - hill) / 80)
    return sky


def rolling_hills(x: int, y: int):
    sky = lerp_rgb((198, 226, 236), (236, 242, 232), y / 160)
    cloud = sum(
        math.exp(-(((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2))
        for cx, cy, rx, ry in [(120, 60, 60, 14), (160, 52, 40, 12), (380, 80, 70, 13)]
    )
    color = lerp_rgb(sky, (252, 252, 248), min(1.0, cloud))
    for k, (base, amp, freq, c) in enumerate([
        (168, 14, 0.010, (150, 186, 140)),
        (202, 16, 0.014, (112, 160, 104)),
        (246, 18, 0.018, (78, 134, 80)),
        (290, 12, 0.024, (58, 108, 62)),
    ]):
        if y > ridge(x, k * 1.9 + 0.8, base, amp, freq):
            color = c
    return color


def snow_peaks(x: int, y: int):
    sky = lerp_rgb((176, 204, 228), (226, 234, 240), y / 180)
    color = sky
    peak = 120 + 70 * abs(((x / 128.0) % 2) - 1) + 8 * math.sin(x * 0.07)
    if y > peak:
        depth = y - peak
        rock = lerp_rgb((112, 132, 152), (84, 100, 120), smooth(depth / 120))
        snow = depth < 26 + 10 * math.sin(x * 0.05)
        color = (238, 242, 246) if snow else rock
    near = ridge(x, 3.3, 262, 10, 0.02)
    if y > near:
        color = lerp_rgb((214, 224, 232), (186, 200, 212), (y - near) / 60)
    return color


def lavender_field(x: int, y: int):
    sky = lerp_rgb((250, 206, 160), (214, 170, 196), y / 140)
    sun_d = math.hypot(x - 256, y - 128)
    if sun_d < 26:
        sky = lerp_rgb((255, 236, 200), sky, sun_d / 26)
    horizon = 140
    if y <= horizon:
        return sky
    t = (y - horizon) / (H - horizon)
    # rows converge to the vanishing point at (256, horizon)
    spread = (x - 256) / max(1.0, (y - horizon) * 0.9)
    row = 0.5 + 0.5 * math.cos(spread * 9.0)
    lav = lerp_rgb((150, 120, 182), (112, 82, 150), t)
    soil = lerp_rgb((112, 120, 84), (78, 88, 60), t)
    return lerp_rgb(soil, lav, smooth(row * 1.3))


if __name__ == "__main__":
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for scene, fn in [
        ("photo_misty_lake", misty_lake),
        ("photo_desert_dunes", desert_dunes),
        ("photo_starry_meadow", starry_meadow),
        ("photo_rolling_hills", rolling_hills),
        ("photo_snow_peaks", snow_peaks),
        ("photo_lavender_field", lavender_field),
    ]:
        render(scene, fn)
