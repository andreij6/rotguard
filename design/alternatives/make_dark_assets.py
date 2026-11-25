#!/usr/bin/env python3
"""Generate dark-mode archival woodcut & botanical medallion PNGs using pure Python stdlib."""

from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path

ASSETS_DIR = Path(__file__).resolve().parent / "assets"


def read_png_rgb(path: Path) -> tuple[int, int, bytearray]:
    raw = path.read_bytes()
    pos = 8
    w = h = ctype = 0
    idat = bytearray()
    while pos < len(raw):
        ln = struct.unpack(">I", raw[pos : pos + 4])[0]
        kind = raw[pos + 4 : pos + 8]
        chunk = raw[pos + 8 : pos + 8 + ln]
        if kind == b"IHDR":
            w, h, bit_depth, ctype = struct.unpack(">IIBB", chunk[:10])
            assert bit_depth == 8 and ctype in (2, 6), f"Unsupported PNG format: {path.name}"
        elif kind == b"IDAT":
            idat.extend(chunk)
        pos += 12 + ln

    bpp = 4 if ctype == 6 else 3
    dec = zlib.decompress(bytes(idat))
    stride = w * bpp
    out = bytearray(w * h * 3)
    prev = bytearray(stride)

    for y in range(h):
        row_off = y * (1 + stride)
        f = dec[row_off]
        cur = bytearray(dec[row_off + 1 : row_off + 1 + stride])
        if f == 1:
            for i in range(bpp, stride):
                cur[i] = (cur[i] + cur[i - bpp]) & 0xFF
        elif f == 2:
            for i in range(stride):
                cur[i] = (cur[i] + prev[i]) & 0xFF
        elif f == 3:
            for i in range(stride):
                a = cur[i - bpp] if i >= bpp else 0
                cur[i] = (cur[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif f == 4:
            for i in range(stride):
                a = cur[i - bpp] if i >= bpp else 0
                b = prev[i]
                c = prev[i - bpp] if i >= bpp else 0
                pa = abs(b - c)
                pb = abs(a - c)
                pc = abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                cur[i] = (cur[i] + pr) & 0xFF
        prev = cur

        dst_off = y * w * 3
        if bpp == 3:
            out[dst_off : dst_off + w * 3] = cur
        else:
            for x in range(w):
                si = x * 4
                di = dst_off + x * 3
                out[di : di + 3] = cur[si : si + 3]

    return w, h, out


def write_png_rgb(path: Path, w: int, h: int, rgb: bytearray) -> None:
    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    stride = w * 3
    raw_scanlines = bytearray(h * (1 + stride))
    for y in range(h):
        raw_scanlines[y * (1 + stride)] = 0
        raw_scanlines[y * (1 + stride) + 1 : (y + 1) * (1 + stride)] = rgb[y * stride : (y + 1) * stride]

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)
    comp = zlib.compress(bytes(raw_scanlines), level=6)
    png_bytes = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", comp) + chunk(b"IEND", b"")
    path.write_bytes(png_bytes)


def sample_corner_median(w: int, h: int, rgb: bytearray) -> tuple[int, int, int]:
    pts = [(10, 10), (w - 11, 10), (10, h - 11), (w - 11, h - 11), (w // 2, 8)]
    rs, gs, bs = [], [], []
    for x, y in pts:
        idx = (y * w + x) * 3
        rs.append(rgb[idx])
        gs.append(rgb[idx + 1])
        bs.append(rgb[idx + 2])
    rs.sort()
    gs.sort()
    bs.sort()
    return rs[2], gs[2], bs[2]


def make_folio_dark_etching(src_name: str, dst_name: str) -> None:
    """Invert fine-line woodcut etching into ivory + moonlit indigo strokes on #1D1B18 plate."""
    w, h, rgb = read_png_rgb(ASSETS_DIR / src_name)
    br, bg, bb = sample_corner_median(w, h, rgb)
    bg_luma = 0.299 * br + 0.587 * bg + 0.114 * bb

    plate = (0x1D, 0x1B, 0x18)
    ivory = (0xEA, 0xE3, 0xD4)
    indigo = (0x7F, 0xA6, 0xD4)

    out = bytearray(w * h * 3)
    for i in range(0, len(rgb), 3):
        r, g, b = rgb[i], rgb[i + 1], rgb[i + 2]
        luma = 0.299 * r + 0.587 * g + 0.114 * b
        # Noise-gated ink opacity relative to paper background
        ink = (bg_luma - luma) / max(bg_luma * 0.80, 1.0)
        if ink <= 0.055:
            ink = 0.0
        else:
            ink = min(1.0, (ink - 0.055) / 0.88)
            ink = ink ** 0.78

        # Preserve cool indigo accents vs neutral charcoal ink lines
        blue_tint = max(0.0, min(1.0, (b - r - 6) / 36.0))
        ir = ivory[0] * (1.0 - blue_tint) + indigo[0] * blue_tint
        ig = ivory[1] * (1.0 - blue_tint) + indigo[1] * blue_tint
        ib = ivory[2] * (1.0 - blue_tint) + indigo[2] * blue_tint

        out[i] = int(plate[0] * (1.0 - ink) + ir * ink)
        out[i + 1] = int(plate[1] * (1.0 - ink) + ig * ink)
        out[i + 2] = int(plate[2] * (1.0 - ink) + ib * ink)

    write_png_rgb(ASSETS_DIR / dst_name, w, h, out)
    print(f"Generated {dst_name} ({w}x{h})")


def make_verdant_dark_botanical(src_name: str, dst_name: str, lift_dark: bool = True) -> None:
    """Matte botanical gouache onto a moonlit canopy radial glow (#2B4E3E -> #182D24) with neighbor defringe."""
    w, h, rgb = read_png_rgb(ASSETS_DIR / src_name)
    br, bg, bb = sample_corner_median(w, h, rgb)

    edge_col = (0x18, 0x2D, 0x24)
    glow_col = (0x2E, 0x54, 0x43)

    # Pass 1: Compute continuous matte alpha with conservative background cut so paper haze stays 0
    alpha = [0.0] * (w * h)
    for y in range(h):
        row_off = y * w * 3
        p_off = y * w
        for x in range(w):
            idx = row_off + x * 3
            r, g, b = rgb[idx], rgb[idx + 1], rgb[idx + 2]
            cdist = math.sqrt((r - br) ** 2 + (g - bg) ** 2 + (b - bb) ** 2)
            sat = max(r, g, b) - min(r, g, b)
            # Treat unsaturated pale rim pixels up to higher cdist as partial fringe rather than solid foreground
            eff_dist = cdist + sat * 0.45
            if eff_dist <= 22.0:
                a = 0.0
            elif eff_dist >= 64.0:
                a = 1.0
            else:
                a = ((eff_dist - 22.0) / 42.0) ** 1.15
            alpha[p_off + x] = a

    # Pass 2: Gentle 1px choke on soft perimeter alpha to strip JPEG halo compression rings
    choked = alpha[:]
    for y in range(1, h - 1):
        row = y * w
        for x in range(1, w - 1):
            a = alpha[row + x]
            if 0.0 < a < 0.92:
                nbr_min = min(alpha[row - w + x], alpha[row + w + x], alpha[row + x - 1], alpha[row + x + 1])
                choked[row + x] = min(a, nbr_min * 0.65 + a * 0.35)
                if choked[row + x] < 0.06:
                    choked[row + x] = 0.0

    # Pass 3: Replace partial boundary RGB with pure nearest opaque neighbor pigment (zero white matte!)
    out = bytearray(w * h * 3)
    cx, cy, max_r = w * 0.5, h * 0.5, w * 0.46

    for y in range(h):
        dy = y - cy
        row = y * w
        row_off = y * w * 3
        for x in range(w):
            dx = x - cx
            dist_center = math.hypot(dx, dy)
            glow = max(0.0, 1.0 - (dist_center / max_r) ** 1.45)
            bgr = edge_col[0] + (glow_col[0] - edge_col[0]) * glow
            bgg = edge_col[1] + (glow_col[1] - edge_col[1]) * glow
            bgb = edge_col[2] + (glow_col[2] - edge_col[2]) * glow

            idx = row_off + x * 3
            a = choked[row + x]
            if a <= 0.001:
                out[idx] = int(bgr)
                out[idx + 1] = int(bgg)
                out[idx + 2] = int(bgb)
                continue

            if a >= 0.95:
                fr, fg, fb = float(rgb[idx]), float(rgb[idx + 1]), float(rgb[idx + 2])
            else:
                # Search up to radius 3 for clean opaque interior color
                best_d2 = 999
                sr, sg, sb = float(rgb[idx]), float(rgb[idx + 1]), float(rgb[idx + 2])
                found = False
                for oy in range(max(0, y - 3), min(h, y + 4)):
                    orow = oy * w
                    for ox in range(max(0, x - 3), min(w, x + 4)):
                        if alpha[orow + ox] >= 0.95:
                            d2 = (ox - x) ** 2 + (oy - y) ** 2
                            if d2 < best_d2:
                                best_d2 = d2
                                oidx = (orow + ox) * 3
                                sr, sg, sb = float(rgb[oidx]), float(rgb[oidx + 1]), float(rgb[oidx + 2])
                                found = True
                if found:
                    fr, fg, fb = sr, sg, sb
                else:
                    fr = max(0.0, min(255.0, (rgb[idx] - (1.0 - a) * br) / max(a, 0.25)))
                    fg = max(0.0, min(255.0, (rgb[idx + 1] - (1.0 - a) * bg) / max(a, 0.25)))
                    fb = max(0.0, min(255.0, (rgb[idx + 2] - (1.0 - a) * bb) / max(a, 0.25)))

            if lift_dark:
                luma = 0.299 * fr + 0.587 * fg + 0.114 * fb
                shadow_boost = 1.0 + 0.24 * max(0.0, 1.0 - luma / 145.0)
                fr = min(255.0, fr * shadow_boost + 6.0)
                fg = min(255.0, fg * shadow_boost + 10.0)
                fb = min(255.0, fb * shadow_boost + 6.0)

            out[idx] = int(bgr * (1.0 - a) + fr * a)
            out[idx + 1] = int(bgg * (1.0 - a) + fg * a)
            out[idx + 2] = int(bgb * (1.0 - a) + fb * a)

    write_png_rgb(ASSETS_DIR / dst_name, w, h, out)
    print(f"Generated {dst_name} ({w}x{h})")


if __name__ == "__main__":
    make_folio_dark_etching("folio_hourglass.png", "folio_hourglass_dark.png")
    make_folio_dark_etching("folio_stillwater.png", "folio_stillwater_dark.png")
    make_verdant_dark_botanical("verdant_sprout.png", "verdant_sprout_dark.png")
    make_verdant_dark_botanical("verdant_terrarium.png", "verdant_terrarium_dark.png")
