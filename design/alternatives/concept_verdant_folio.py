#!/usr/bin/env python3
"""Unified Hybrid: Verdant Folio — Option 5's botanical calm + Option 2's square buttons & ruled bottom nav."""

from __future__ import annotations

import argparse
import zipfile
from pathlib import Path
from typing import Any

from fc_core import (
    ALT_DIR,
    ASSETS_DIR,
    GALLERY_H,
    GALLERY_PAD_X,
    GALLERY_W,
    PREVIEWS_DIR,
    SCREEN_GAP_X,
    SCREEN_H,
    SCREEN_TOP_Y,
    SCREEN_W,
    DocBuilder,
    drop_shadow,
    figcraft_roundtrip_batch,
    hex_to_rgba,
    link_flow,
    write_figcraft_zip,
)

FONT_SERIF = "Merriweather"
FONT_SANS = "Nunito Sans"
FONT_MONO = "Liberation Mono"

OUTPUT_FIGCRAFT = ALT_DIR / "verdant_folio_hybrid.figcraft"

THEMES: dict[str, dict[str, str]] = {
    "light": {
        "mode_label": "LIGHT MODE · DAYLIGHT SANCTUARY",
        "title": "Verdant Folio: Daylight Hybrid",
        "subtitle": "Combines Verdant's mineral linen, botanical gouache, and breathing canopy gate with Quiet Folio's square buttons and double-ruled bottom navigation.",
        "font_label": "MERRIWEATHER + NUNITO SANS  //  DAYLIGHT HYBRID",
        "backdrop": "#DFE7DC",
        "boardStroke": "#BAC8B5",
        "canvas": "#EFF3EC",
        "card": "#FAFCF8",
        "cardBorder": "#D0DBCD",
        "plateSproutBg": "#F4F5EE",
        "plateTerrariumBg": "#F7F8F1",
        "sproutImg": "verdant_sprout.png",
        "terrariumImg": "verdant_terrarium.png",
        "warmCard": "#F9F4EA",
        "warmBorder": "#E4D7BF",
        "rule": "#D3DEC9",
        "ruleStrong": "#8FA395",
        "ink": "#1C352B",
        "inkMuted": "#4E685C",
        "navActiveInk": "#2F6F4E",
        "sageTint": "#DCE5D8",
        "mossAccent": "#2F6F4E",
        "mossBtnFill": "#2F6F4E",
        "mossBtnStroke": "#24573D",
        "mossBtnInk": "#FAFCF8",
        "s2CtaFill": "#1C352B",
        "s2CtaInk": "#F6F5EE",
        "terracotta": "#C96846",
        "sunGold": "#DFA952",
        "canopy": "#1C352B",
        "canopyCard": "#244337",
        "canopyStroke": "#396151",
        "canopyCore": "#162A22",
        "haloOuter": "#294B3D",
        "haloMid": "#335C4B",
        "canopyMuted": "#9BB5A8",
        "cream": "#F6F5EE",
        "switchKnobOff": "#FFFFFF",
        "screenStroke": "#C6D2C2",
        "shadowHex": "#15241D",
        "shadowAlpha": "0.18",
        "captions": [
            "01  ONBOARDING · SQUARE CONTROLS",
            "02  SANCTUARY · RULED NAVIGATION",
            "03  CANOPY GATE · SQUARE HOLD RESERVOIR",
            "04  GARDEN APPS · WEEKLY RHYTHM",
        ],
    },
    "dark": {
        "mode_label": "DARK MODE · NOCTURNAL CANOPY",
        "title": "Verdant Folio: Nocturnal Hybrid",
        "subtitle": "Dark-mode companion blending nocturnal moss linen and moonlit botanical medallions with Quiet Folio's crisp architectural buttons and ruled navigation.",
        "font_label": "MERRIWEATHER + NUNITO SANS  //  NOCTURNAL HYBRID",
        "backdrop": "#09120E",
        "boardStroke": "#1C3128",
        "canvas": "#11211A",
        "card": "#182D24",
        "cardBorder": "#274337",
        "plateSproutBg": "#182D24",
        "plateTerrariumBg": "#182D24",
        "sproutImg": "verdant_sprout_dark.png",
        "terrariumImg": "verdant_terrarium_dark.png",
        "warmCard": "#202821",
        "warmBorder": "#3B4737",
        "rule": "#243F34",
        "ruleStrong": "#4A6E5E",
        "ink": "#EEF3EB",
        "inkMuted": "#94AFA2",
        "navActiveInk": "#63B584",
        "sageTint": "#223D31",
        "mossAccent": "#63B584",
        "mossBtnFill": "#2D6D4C",
        "mossBtnStroke": "#63B584",
        "mossBtnInk": "#FAFCF8",
        "s2CtaFill": "#EEF3EB",
        "s2CtaInk": "#11211A",
        "terracotta": "#E28360",
        "sunGold": "#E8B861",
        "canopy": "#0B1712",
        "canopyCard": "#14271F",
        "canopyStroke": "#274337",
        "canopyCore": "#08120E",
        "haloOuter": "#183428",
        "haloMid": "#214636",
        "canopyMuted": "#94AFA2",
        "cream": "#EEF3EB",
        "switchKnobOff": "#94AFA2",
        "screenStroke": "#274337",
        "shadowHex": "#030805",
        "shadowAlpha": "0.45",
        "captions": [
            "01  ONBOARDING · SQUARE CONTROLS",
            "02  SANCTUARY · RULED NAVIGATION",
            "03  MIDNIGHT GATE · SQUARE HOLD RESERVOIR",
            "04  GARDEN APPS · WEEKLY RHYTHM",
        ],
    },
}


def _screen(b: DocBuilder, board_id: int, slot: int, name: str, fill_hex: str, t: dict[str, str], desc: str) -> int:
    sx = GALLERY_PAD_X + slot * (SCREEN_W + SCREEN_GAP_X)
    return b.frame(
        board_id,
        name,
        sx,
        SCREEN_TOP_Y,
        SCREEN_W,
        SCREEN_H,
        fill_hex=fill_hex,
        stroke_hex=t["screenStroke"] if fill_hex == t["canvas"] else t["canopyStroke"],
        stroke_w=1.5,
        radius=36.0,
        clip=True,
        effects=drop_shadow(t["shadowHex"], float(t["shadowAlpha"]), 0.0, 16.0, 32.0),
        annotations=[{"label": desc, "properties": ["fill", "cornerRadius", "stroke"], "category": "hybrid-design"}],
    )


def _status_bar(b: DocBuilder, s: int, t: dict[str, str], on_canopy: bool = False) -> None:
    ink = t["cream"] if on_canopy else t["ink"]
    muted = t["canopyMuted"] if on_canopy else t["inkMuted"]
    dot = t["sunGold"] if on_canopy else t["mossAccent"]
    b.text(s, "Status Time", 26, 16, 60, 18, "9:41", FONT_SANS, "Bold", 14.0, 18.0, ink, tnum=True)
    b.ellipse(s, "Status Dot", 272, 21, 8, 8, fill_hex=dot)
    b.text(s, "Status Spec", 284, 17, 102, 16, "Sanctuary On", FONT_SANS, "Semi Bold", 11.5, 16.0, muted, align_h="right")


def _ruled_bottom_nav(b: DocBuilder, s: int, t: dict[str, str], active_idx: int) -> None:
    """Option 2 signature grounded double-ruled text navigation with single centered dot."""
    b.rect(s, "Nav Canvas BG", 0, 828, SCREEN_W, 87, fill_hex=t["canvas"])
    b.rule(s, "Nav Double Top", 24, 828, 364, t["ruleStrong"], thickness=1.0)
    b.rule(s, "Nav Double Bot", 24, 831, 364, t["rule"], thickness=1.0)
    items = ["Home", "Garden", "Rhythm", "Calm"]
    cw = 364.0 / 4.0
    for idx, label in enumerate(items):
        cx = 24.0 + idx * cw
        act = idx == active_idx
        b.text(
            s,
            f"Nav {label}",
            cx,
            850,
            cw,
            20,
            label,
            FONT_SERIF if act else FONT_SANS,
            "Bold" if act else "Semi Bold",
            13.5 if act else 13.5,
            20.0,
            t["navActiveInk"] if act else t["inkMuted"],
            align_h="center",
        )
        if act:
            b.ellipse(s, "Nav Active Dot", cx + cw / 2.0 - 2.5, 876, 5.0, 5.0, fill_hex=t["navActiveInk"])


def build_board(b: DocBuilder, mode_key: str, y_offset: float) -> tuple[int, list[int]]:
    t = THEMES[mode_key]
    board = b.frame(
        b.page_id,
        f"Verdant Folio — {mode_key.title()} Board",
        0.0,
        y_offset,
        GALLERY_W,
        GALLERY_H,
        fill_hex=t["backdrop"],
        stroke_hex=t["boardStroke"],
        stroke_w=1.5,
        clip=True,
    )

    # Studio Gallery Header (local to board frame)
    hdr = b.group(board, "Gallery Header", 0.0, 0.0, GALLERY_W, 150.0)
    b.text(hdr, "Mode Kicker", GALLERY_PAD_X, 40.0, 620.0, 18.0, f"VERDANT FOLIO HYBRID  //  {t['mode_label']}", FONT_MONO, "Bold", 11.5, 16.0, t["inkMuted"], letter_spacing_pct=5.0)
    b.text(hdr, "Gallery Title", GALLERY_PAD_X, 64.0, 1020.0, 44.0, t["title"], FONT_SERIF, "Bold", 32.0, 38.0, t["ink"])
    b.text(hdr, "Gallery Subtitle", GALLERY_PAD_X, 112.0, 1180.0, 24.0, t["subtitle"], FONT_SANS, "Regular", 14.5, 22.0, t["inkMuted"])

    b.text(hdr, "Font Label", GALLERY_W - GALLERY_PAD_X - 440.0, 42.0, 440.0, 18.0, t["font_label"], FONT_MONO, "Bold", 11.0, 16.0, t["inkMuted"], align_h="right")
    swatches = [(t["canvas"], t["canvas"]), (t["card"], t["card"]), (t["mossAccent"], t["mossAccent"]), (t["terracotta"], t["terracotta"])]
    sw_start_x = GALLERY_W - GALLERY_PAD_X - len(swatches) * 64.0 + 14.0
    for idx, (shex, sname) in enumerate(swatches):
        sx = sw_start_x + idx * 64.0
        b.rect(hdr, f"Swatch {idx}", sx, 68.0, 50.0, 32.0, fill_hex=shex, stroke_hex=t["ruleStrong"], stroke_w=1.0, radius=4.0)
        b.text(hdr, f"Swatch Hex {idx}", sx, 105.0, 50.0, 14.0, sname, FONT_MONO, "Regular", 9.0, 12.0, t["inkMuted"], align_h="center")
    b.rule(hdr, "Header Divider", GALLERY_PAD_X, 148.0, GALLERY_W - 2 * GALLERY_PAD_X, t["rule"], thickness=1.0)

    # Bottom Captions
    ftr = b.group(board, "Gallery Captions", 0.0, SCREEN_TOP_Y + SCREEN_H + 18.0, GALLERY_W, 50.0)
    for idx, cap in enumerate(t["captions"]):
        cx = GALLERY_PAD_X + idx * (SCREEN_W + SCREEN_GAP_X)
        b.text(ftr, f"Caption {idx + 1}", cx, 8.0, SCREEN_W, 24.0, cap, FONT_MONO, "Bold", 11.5, 18.0, t["inkMuted"], align_h="center")

    # =========================================================================
    # SCREEN 1: ONBOARDING (Verdant Botanical + Option 2 Square Buttons & Checkboxes)
    # =========================================================================
    s1 = _screen(b, board, 0, f"{mode_key.title()} 01 Onboarding", t["canvas"], t, "Verdant arched botanical gouache medallion combined with crisp Option 2 square hour buttons and square goal checkboxes.")
    _status_bar(b, s1, t)
    b.rect(s1, "Step Square Badge", 114, 52, 184, 24, fill_hex=t["sageTint"], stroke_hex=t["cardBorder"], stroke_w=1.0, radius=4.0)
    b.text(s1, "Step Badge T", 114, 56, 184, 16, "STEP 2 · GENTLE CHECK-IN", FONT_MONO, "Bold", 10.0, 16.0, t["mossAccent"], align_h="center")
    b.text(s1, "S1 Head", 28, 88, 356, 36, "Tend your quiet hours.", FONT_SERIF, "Bold", 25.0, 32.0, t["ink"], align_h="center")

    b.clipped_image(
        s1,
        "Botanical Sprout Arch",
        ASSETS_DIR / t["sproutImg"],
        96,
        136,
        220,
        214,
        bg_hex=t["plateSproutBg"],
        stroke_hex=t["cardBorder"],
        stroke_w=1.5,
        radius=(110.0, 110.0, 26.0, 26.0),
    )

    b.rect(s1, "Time Cost Card", 24, 364, 364, 94, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=24.0)
    b.text(s1, "Cost Val", 42, 378, 328, 30, "9.4 waking years", FONT_SERIF, "Bold", 23.0, 28.0, t["terracotta"], align_h="center", tnum=True)
    b.text(s1, "Cost Desc", 42, 412, 328, 36, "At 3 hours daily, endless feeds quietly absorb 45 full days out of every year.", FONT_SANS, "Regular", 12.5, 17.0, t["inkMuted"], align_h="center")

    # Option 2 Square Daily Feed Selector Buttons (radius = 6px)
    b.text(s1, "Hour Lbl", 24, 474, 364, 16, "DAILY FEED TIME TODAY", FONT_MONO, "Bold", 10.0, 16.0, t["inkMuted"], align_h="center")
    for idx, hr in enumerate(["1 hr", "2 hrs", "3 hrs", "4+ hrs"]):
        hx = 24.0 + idx * 93.0
        sel = hr == "3 hrs"
        b.rect(
            s1,
            f"Hr SqBtn {idx}",
            hx,
            498,
            85,
            40,
            fill_hex=t["mossBtnFill"] if sel else t["card"],
            stroke_hex=t["mossBtnStroke"] if sel else t["cardBorder"],
            stroke_w=1.4 if sel else 1.2,
            radius=6.0,
        )
        b.text(s1, f"Hr SqT {idx}", hx, 509, 85, 18, hr, FONT_SANS, "Bold", 13.0, 18.0, t["mossBtnInk"] if sel else t["ink"], align_h="center")

    # Goal list with Option 2 crisp square checkboxes & 6px row corners
    b.text(s1, "Goals Q", 24, 556, 364, 16, "WHAT HELPS YOUR MIND FLOURISH?", FONT_MONO, "Bold", 10.0, 16.0, t["inkMuted"])
    goals = [("Deep focus & reading", True), ("Restful sleep before midnight", True), ("Unrushed evenings offline", False)]
    for idx, (gtxt, on) in enumerate(goals):
        gy = 580.0 + idx * 58.0
        b.rect(s1, f"Goal Box {idx}", 24, gy, 364, 48, fill_hex=t["card"], stroke_hex=t["mossAccent"] if on else t["cardBorder"], stroke_w=1.4 if on else 1.0, radius=6.0)
        b.rect(s1, f"Chk Box {idx}", 40, gy + 15, 18, 18, fill_hex=t["canvas"], stroke_hex=t["mossAccent"] if on else t["ruleStrong"], stroke_w=1.5, radius=2.0)
        if on:
            b.rect(s1, f"Chk Fill {idx}", 44, gy + 19, 10, 10, fill_hex=t["mossAccent"], radius=1.0)
        b.text(s1, f"Goal Txt {idx}", 72, gy + 14, 296, 20, gtxt, FONT_SANS, "Bold" if on else "Regular", 14.0, 20.0, t["ink"] if on else t["inkMuted"])

    # Option 2 Square-Cornered Primary CTA (radius = 6px)
    cta1 = b.rect(s1, "S1 Square CTA", 24, 780, 364, 58, fill_hex=t["mossBtnFill"], stroke_hex=t["mossBtnStroke"], stroke_w=1.2, radius=6.0, effects=drop_shadow(t["shadowHex"], 0.25, 0.0, 6.0, 16.0))
    b.text(s1, "S1 Square CTA T", 24, 798, 364, 22, "Grow My Sanctuary", FONT_SANS, "Bold", 15.5, 22.0, t["mossBtnInk"], align_h="center")
    b.text(s1, "S1 Soft Note", 24, 850, 364, 16, "Calm haptics · Zero guilt-tripping", FONT_SANS, "Semi Bold", 11.5, 16.0, t["inkMuted"], align_h="center")

    # =========================================================================
    # SCREEN 2: HOME SANCTUARY (Verdant Hero + Square CTA + Option 2 Ruled Nav)
    # =========================================================================
    s2 = _screen(b, board, 1, f"{mode_key.title()} 02 Sanctuary", t["canvas"], t, "Verdant flourishing terrarium card with crisp 6px Preview button and grounded Option 2 double-ruled bottom navigation.")
    _status_bar(b, s2, t)
    b.text(s2, "S2 Sub", 26, 52, 200, 16, "TUESDAY SANCTUARY" if mode_key == "light" else "TUESDAY EVENING", FONT_MONO, "Bold", 10.0, 16.0, t["inkMuted"])
    b.text(s2, "S2 Title", 26, 70, 210, 32, "Sanctuary", FONT_SERIF, "Bold", 25.0, 30.0, t["ink"])
    b.rect(s2, "Shield Badge Sq", 262, 66, 126, 28, fill_hex=t["sageTint"], stroke_hex=t["cardBorder"], radius=4.0)
    b.ellipse(s2, "Shield Dot", 274, 76, 8, 8, fill_hex=t["mossAccent"])
    b.text(s2, "Shield Badge T", 286, 72, 94, 16, "Shield Ready", FONT_SANS, "Bold", 11.5, 16.0, t["mossAccent"], align_h="center")

    b.rect(
        s2,
        "Hero Terrarium Card",
        22,
        112,
        368,
        310,
        fill_hex=t["plateTerrariumBg"],
        stroke_hex=t["cardBorder"],
        stroke_w=1.2,
        radius=30.0,
        effects=drop_shadow(t["shadowHex"], 0.14 if mode_key == "light" else 0.32, 0.0, 10.0, 24.0),
    )
    b.clipped_image(
        s2,
        "Terrarium Medallion",
        ASSETS_DIR / t["terrariumImg"],
        96,
        122,
        220,
        204,
        bg_hex=t["plateTerrariumBg"],
        stroke_hex=t["cardBorder"],
        stroke_w=1.0,
        radius=26.0,
    )
    b.text(s2, "Hero Health", 38, 336, 336, 34, "88% Flourishing", FONT_SERIF, "Bold", 24.0, 30.0, t["ink"], align_h="center")
    b.rect(s2, "Hero Stage Badge", 104, 376, 204, 26, fill_hex=t["sageTint"], stroke_hex=t["cardBorder"], radius=4.0)
    b.text(s2, "Hero Stage T", 104, 381, 204, 16, "Lush · 42m reclaimed today", FONT_SANS, "Bold", 11.5, 16.0, t["mossAccent"], align_h="center")

    b.rect(s2, "Stat Pebble L", 22, 438, 176, 94, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=24.0)
    b.text(s2, "Stat L Val", 40, 454, 140, 34, "7", FONT_SERIF, "Bold", 28.0, 32.0, t["ink"], tnum=True)
    b.text(s2, "Stat L Lbl", 40, 492, 140, 18, "Mindful pauses", FONT_SANS, "Semi Bold", 12.5, 18.0, t["inkMuted"])

    b.rect(s2, "Stat Pebble R", 214, 438, 176, 94, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=24.0)
    b.text(s2, "Stat R Val", 232, 454, 140, 34, "42m", FONT_SERIF, "Bold", 28.0, 32.0, t["mossAccent"], tnum=True)
    b.text(s2, "Stat R Lbl", 232, 492, 140, 18, "Quiet returned", FONT_SANS, "Semi Bold", 12.5, 18.0, t["inkMuted"])

    b.rect(s2, "Mantra Warm Card", 22, 548, 368, 108, fill_hex=t["warmCard"], stroke_hex=t["warmBorder"], radius=24.0)
    b.text(s2, "Mantra Tag", 40, 566, 332, 14, "YOUR ANCHOR SEED", FONT_MONO, "Bold", 10.0, 14.0, t["terracotta"])
    b.text(s2, "Mantra Body", 40, 588, 332, 48, "“Build things that outlast the feed.”", FONT_SERIF, "Italic", 17.5, 24.0, t["ink"])

    # Option 2 Square-Cornered Preview CTA (radius = 6px)
    test_cta = b.rect(s2, "Preview Square CTA", 22, 684, 368, 58, fill_hex=t["s2CtaFill"], radius=6.0, effects=drop_shadow(t["shadowHex"], 0.24, 0.0, 6.0, 18.0))
    b.text(s2, "Preview Square T", 22, 702, 368, 22, "Preview Canopy Pause", FONT_SANS, "Bold", 15.0, 22.0, t["s2CtaInk"], align_h="center")
    b.text(s2, "Guarded Note", 22, 756, 368, 18, "Gently guarding YouTube · Instagram · TikTok · Reddit", FONT_SANS, "Semi Bold", 11.5, 18.0, t["inkMuted"], align_h="center")

    _ruled_bottom_nav(b, s2, t, 0)

    # =========================================================================
    # SCREEN 3: LIVE YOUTUBE FRICTION GATE (Canopy Halos + Square Buttons & Reservoir)
    # =========================================================================
    s3 = _screen(b, board, 2, f"{mode_key.title()} 03 Canopy Gate", t["canopy"], t, "Verdant canopy concentric breathing halo with Option 2 6px architectural Close CTA and rectangular ink/gold hold reservoir.")
    _status_bar(b, s3, t, on_canopy=True)

    b.rect(s3, "Canopy Badge Sq", 84, 54, 244, 26, fill_hex=t["canopyCard"], stroke_hex=t["canopyStroke"], radius=4.0)
    b.text(s3, "Canopy Badge T", 84, 59, 244, 16, "PAUSE · YOUTUBE · VISIT #4", FONT_MONO, "Bold", 10.0, 16.0, t["sunGold"], align_h="center")

    b.text(s3, "Canopy Head", 28, 98, 356, 68, "Take one slow breath\nbefore entering.", FONT_SERIF, "Bold", 24.0, 31.0, t["cream"], align_h="center")

    cx, cy = 206.0, 290.0
    b.ellipse(s3, "Halo Outer", cx - 120, cy - 120, 240, 240, fill_hex=t["haloOuter"], fill_alpha=0.40)
    b.ellipse(s3, "Halo Mid", cx - 98, cy - 98, 196, 196, fill_hex=t["haloMid"], fill_alpha=0.50)
    b.ellipse(s3, "Hold Track Ring", cx - 80, cy - 80, 160, 160, fill_hex=t["canopyStroke"], inner=0.84)
    b.ellipse(s3, "Hold Gold Arc", cx - 80, cy - 80, 160, 160, fill_hex=t["sunGold"], inner=0.84, sweep_ratio=0.48, effects=drop_shadow(t["sunGold"], 0.38, 0.0, 0.0, 18.0))
    b.ellipse(s3, "Hold Core", cx - 60, cy - 60, 120, 120, fill_hex=t["canopyCore"])
    b.text(s3, "Core Prompt", cx - 56, cy - 36, 112, 16, "BREATHE & HOLD", FONT_MONO, "Bold", 9.0, 14.0, t["canopyMuted"], align_h="center")
    b.text(s3, "Core Timer", cx - 56, cy - 14, 112, 32, "1.9s", FONT_SERIF, "Bold", 28.0, 32.0, t["cream"], align_h="center", tnum=True)
    b.text(s3, "Core Goal", cx - 56, cy + 22, 112, 16, "of 4 seconds", FONT_SANS, "Semi Bold", 11.0, 16.0, t["sunGold"], align_h="center")

    b.rect(s3, "Intention Card", 24, 434, 364, 96, fill_hex=t["canopyCard"], stroke_hex=t["canopyStroke"], radius=24.0)
    b.text(s3, "Intent Kicker", 42, 452, 328, 14, "GENTLY REMEMBERING", FONT_MONO, "Bold", 10.0, 14.0, t["sunGold"], align_h="center")
    b.text(s3, "Intent Quote", 42, 474, 328, 44, "“Protect your sleep and deep work tonight.”", FONT_SERIF, "Italic", 15.5, 22.0, t["cream"], align_h="center")

    # Option 2 Square-Cornered Close YouTube CTA (radius = 6px)
    close_cta = b.rect(s3, "Return Square CTA", 24, 558, 364, 60, fill_hex=t["cream"], radius=6.0, effects=drop_shadow("#050D09", 0.35, 0.0, 8.0, 20.0))
    b.text(s3, "Return Square T", 24, 577, 364, 22, "Return to Quiet (Close YouTube)", FONT_SANS, "Bold", 15.0, 22.0, t["canopy"], align_h="center")

    b.text(s3, "Or Gentle", 24, 640, 364, 16, "OR CONTINUE SLOWLY WITH INTENT", FONT_MONO, "Bold", 10.0, 16.0, t["canopyMuted"], align_h="center")

    # Option 2 Square Ink-Style Hold Reservoir adapted to Verdant's SunGold & Canopy palette (radius = 6px)
    b.rect(s3, "Hold Sq Reservoir", 24, 670, 364, 70, fill_hex=t["canopyCard"], stroke_hex=t["sunGold"], stroke_w=1.5, radius=6.0)
    b.rect(s3, "Hold Sq Fill", 25.5, 671.5, 174, 67, fill_hex=t["sunGold"], fill_alpha=0.28, radius=4.0)
    b.rect(s3, "Hold Sq Edge", 198, 671.5, 2.0, 67, fill_hex=t["sunGold"])
    b.rect(s3, "Hold Sq Plaque", 66, 687, 280, 36, fill_hex=t["canopyCore"], stroke_hex=t["sunGold"], stroke_w=1.0, radius=4.0)
    b.text(s3, "Hold Sq Txt", 66, 696, 280, 18, "Press & hold 4s  ·  1.9s", FONT_MONO, "Bold", 11.5, 18.0, t["cream"], align_h="center", tnum=True)

    b.text(s3, "Canopy Foot", 36, 760, 340, 38, "Lifting your finger resets the breath gently so mindless habit never slips past.", FONT_SANS, "Regular", 12.0, 17.0, t["canopyMuted"], align_h="center")

    # =========================================================================
    # SCREEN 4: GUARDED GARDEN & RHYTHM (Verdant Cards + Square Filter + Ruled Nav)
    # =========================================================================
    s4 = _screen(b, board, 3, f"{mode_key.title()} 04 Garden & Rhythm", t["canvas"], t, "Verdant botanical rows & rhythm bars paired with square duration badge and Option 2 double-ruled bottom navigation.")
    _status_bar(b, s4, t)
    b.text(s4, "S4 Title", 26, 54, 240, 32, "Guarded Garden", FONT_SERIF, "Bold", 24.0, 30.0, t["ink"])
    b.rect(s4, "S4 Square Pill", 296, 56, 92, 26, fill_hex=t["sageTint"], stroke_hex=t["cardBorder"], radius=4.0)
    b.text(s4, "S4 Square T", 296, 61, 92, 16, "4s Breath", FONT_MONO, "Bold", 10.5, 16.0, t["mossAccent"], align_h="center")

    apps = [
        ("YouTube", "4s gentle pause · 5 paused", True, t["terracotta"]),
        ("Instagram", "4s gentle pause · 4 paused", True, t["sunGold"]),
        ("TikTok", "6s gentle pause · 2 paused", True, t["mossAccent"]),
        ("Chrome", "Unprotected browser", False, t["inkMuted"]),
    ]
    for idx, (aname, sub, on, dot_col) in enumerate(apps):
        ay = 104.0 + idx * 66.0
        b.rect(s4, f"GApp {idx}", 22, ay, 368, 56, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=22.0)
        b.ellipse(s4, f"GApp Dot {idx}", 38, ay + 21, 14, 14, fill_hex=dot_col, fill_alpha=0.9 if on else 0.3)
        b.text(s4, f"GApp N {idx}", 64, ay + 10, 210, 20, aname, FONT_SANS, "Bold", 15.0, 20.0, t["ink"])
        b.text(s4, f"GApp S {idx}", 64, ay + 31, 220, 16, sub, FONT_SANS, "Regular", 11.5, 16.0, t["inkMuted"])
        b.rect(s4, f"GApp Sw {idx}", 324, ay + 15, 48, 26, fill_hex=t["mossAccent"] if on else t["sageTint"], radius=13.0)
        b.ellipse(s4, f"GApp K {idx}", 349 if on else 327, ay + 18, 20, 20, fill_hex=t["canvas"] if (on and mode_key == "dark") else (t["switchKnobOff"] if not on else "#FFFFFF"))

    b.rect(s4, "Duel Peb", 22, 380, 368, 96, fill_hex=t["warmCard"], stroke_hex=t["warmBorder"], radius=24.0)
    b.text(s4, "Duel K", 40, 396, 220, 14, "TODAY'S CALM COMPARISON", FONT_MONO, "Bold", 10.0, 14.0, t["terracotta"])
    b.text(s4, "Duel Main", 40, 416, 332, 24, "2 bypasses · Avg scroller: 14", FONT_SERIF, "Bold", 17.5, 22.0, t["ink"])
    b.text(s4, "Duel Sub", 40, 444, 332, 18, "86% fewer impulsive overrides today.", FONT_SANS, "Semi Bold", 12.5, 18.0, t["mossAccent"])

    b.rect(s4, "Rhythm Card", 22, 492, 368, 308, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=26.0)
    b.text(s4, "Rhy Title", 40, 512, 230, 24, "Weekly Rhythm", FONT_SERIF, "Bold", 18.0, 24.0, t["ink"])
    b.text(s4, "Rhy Tot", 272, 515, 100, 18, "4h 54m saved", FONT_SANS, "Bold", 12.0, 18.0, t["mossAccent"], align_h="right")

    bars = [("Mon", 64), ("Tue", 72), ("Wed", 78), ("Thu", 82), ("Fri", 85), ("Sat", 91), ("Sun", 88)]
    for idx, (dlbl, pct) in enumerate(bars):
        bx = 42.0 + idx * 48.0
        bh = round(162.0 * (pct / 100.0), 1)
        by = 720.0 - bh
        b.rect(s4, f"BarBG {idx}", bx, 558, 28, 162, fill_hex=t["canvas"], radius=14.0)
        b.rect(s4, f"BarFG {idx}", bx, by, 28, bh, fill_hex=t["mossAccent"] if pct >= 80 else t["sunGold"], radius=14.0)
        b.text(s4, f"BarPct {idx}", bx, by - 17, 28, 14, f"{pct}", FONT_SANS, "Bold", 10.5, 14.0, t["inkMuted"], align_h="center", tnum=True)
        b.text(s4, f"BarDay {idx}", bx, 730, 28, 16, dlbl, FONT_SANS, "Semi Bold", 11.0, 16.0, t["inkMuted"], align_h="center")

    b.text(s4, "Rhy Note", 40, 760, 332, 18, "Steadily thriving · keep evening hours quiet", FONT_SANS, "Semi Bold", 12.0, 18.0, t["mossAccent"], align_h="center")

    _ruled_bottom_nav(b, s4, t, 1)

    link_flow(b, cta1, s2)
    link_flow(b, test_cta, s3, transition="dissolve", duration_ms=300)
    link_flow(b, close_cta, s2)

    b.add_comment(s2, 206, 860, "Hybrid System", "Option 2 double-ruled bottom navigation & crisp 6px button geometry grounded cleanly inside Option 5's botanical palette.")
    b.add_comment(s3, 206, 705, "Square Hold Reservoir", "Combines Quiet Folio's legible centered hold plaque & rectangular track with Verdant's warm gold & deep forest canopy.")

    return board, [s1, s2, s3, s4]


def build_verdant_folio_hybrid() -> tuple[dict[str, Any], int, int, dict[str, tuple[bytes, int, int]]]:
    b = DocBuilder("Verdant Folio Hybrid (Light + Dark)", "#111A15")
    light_board, light_screens = build_board(b, "light", 0.0)
    dark_board, _dark_screens = build_board(b, "dark", GALLERY_H + 56.0)

    b.nodes[str(b.page_id)]["flows"] = [
        {"name": "Verdant Folio Daylight Flow", "start": light_screens[0], "description": "Botanical onboarding -> Sanctuary with ruled nav -> Square hold reservoir"},
    ]

    light_t, dark_t = THEMES["light"], THEMES["dark"]
    doc: dict[str, Any] = {
        "format": "figcraft",
        "version": 1,
        "imageIndex": b.image_index,
        "name": b.doc_name,
        "pages": [b.page_id],
        "nodes": b.nodes,
        "next_id": b.next_id,
        "styles": [],
        "variables": {
            "collections": [
                {
                    "id": "VariableCollectionId:1",
                    "name": "Verdant Folio",
                    "modes": [
                        {"id": "VariableCollectionId:1/daylight", "name": "Daylight"},
                        {"id": "VariableCollectionId:1/nocturnal", "name": "Nocturnal"},
                    ],
                }
            ],
            "variables": [
                {
                    "id": "VariableID:1",
                    "name": "surface/canvas",
                    "resolvedType": "color",
                    "collection": "VariableCollectionId:1",
                    "values": {
                        "VariableCollectionId:1/daylight": {"type": "color", "value": hex_to_rgba(light_t["canvas"])},
                        "VariableCollectionId:1/nocturnal": {"type": "color", "value": hex_to_rgba(dark_t["canvas"])},
                    },
                    "description": "Primary botanical linen / nocturnal forest canvas",
                },
                {
                    "id": "VariableID:2",
                    "name": "surface/card",
                    "resolvedType": "color",
                    "collection": "VariableCollectionId:1",
                    "values": {
                        "VariableCollectionId:1/daylight": {"type": "color", "value": hex_to_rgba(light_t["card"])},
                        "VariableCollectionId:1/nocturnal": {"type": "color", "value": hex_to_rgba(dark_t["card"])},
                    },
                    "description": "Elevated alabaster / forest pebble card",
                },
                {
                    "id": "VariableID:3",
                    "name": "accent/moss",
                    "resolvedType": "color",
                    "collection": "VariableCollectionId:1",
                    "values": {
                        "VariableCollectionId:1/daylight": {"type": "color", "value": hex_to_rgba(light_t["mossAccent"])},
                        "VariableCollectionId:1/nocturnal": {"type": "color", "value": hex_to_rgba(dark_t["mossAccent"])},
                    },
                    "description": "Primary botanical accent & active ruled-nav ink",
                },
                {
                    "id": "VariableID:4",
                    "name": "shield/canopy",
                    "resolvedType": "color",
                    "collection": "VariableCollectionId:1",
                    "values": {
                        "VariableCollectionId:1/daylight": {"type": "color", "value": hex_to_rgba(light_t["canopy"])},
                        "VariableCollectionId:1/nocturnal": {"type": "color", "value": hex_to_rgba(dark_t["canopy"])},
                    },
                    "description": "YouTube friction overlay background",
                },
                {
                    "id": "VariableID:5",
                    "name": "accent/sunGold",
                    "resolvedType": "color",
                    "collection": "VariableCollectionId:1",
                    "values": {
                        "VariableCollectionId:1/daylight": {"type": "color", "value": hex_to_rgba(light_t["sunGold"])},
                        "VariableCollectionId:1/nocturnal": {"type": "color", "value": hex_to_rgba(dark_t["sunGold"])},
                    },
                    "description": "Sunlit gold hold ring & square bypass reservoir",
                },
            ],
        },
        "current_page": b.page_id,
        "document_colors": [
            hex_to_rgba(light_t["canvas"]),
            hex_to_rgba(light_t["mossAccent"]),
            hex_to_rgba(light_t["canopy"]),
            hex_to_rgba(light_t["sunGold"]),
            hex_to_rgba(dark_t["canvas"]),
            hex_to_rgba(dark_t["mossAccent"]),
            hex_to_rgba(dark_t["canopy"]),
        ],
        "annotation_categories": [{"id": "hybrid-design", "name": "Verdant Folio Hybrid", "color": hex_to_rgba(light_t["mossAccent"])}],
        "comments": b.comments,
    }
    return doc, light_board, dark_board, b.images


def main() -> None:
    parser = argparse.ArgumentParser(description="Build & render unified Verdant Folio Hybrid FigCraft design")
    parser.add_argument("--roundtrip", action="store_true")
    parser.add_argument("--port", type=int, default=47985)
    args = parser.parse_args()

    doc, light_id, dark_id, images = build_verdant_folio_hybrid()
    write_figcraft_zip(doc, images, OUTPUT_FIGCRAFT)
    print(f"Built {OUTPUT_FIGCRAFT.name} ({OUTPUT_FIGCRAFT.stat().st_size} bytes, {len(doc['nodes'])} nodes)")

    if args.roundtrip:
        import json
        import socket
        import subprocess
        import time

        from fc_core import FIGCRAFT_BIN, ROOT_DIR

        PREVIEWS_DIR.mkdir(parents=True, exist_ok=True)
        proc = subprocess.Popen(
            [
                str(FIGCRAFT_BIN),
                "--control",
                str(args.port),
                "--automation-read-root",
                str(ROOT_DIR),
                "--automation-write-root",
                str(ROOT_DIR),
            ],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        time.sleep(2.5)

        def rpc(method: str, params: dict[str, Any], req_id: int) -> dict[str, Any]:
            sock = socket.create_connection(("127.0.0.1", args.port), timeout=45)
            sock.sendall((json.dumps({"id": req_id, "method": method, "params": params}) + "\n").encode("utf-8"))
            buf = b""
            while b"\n" not in buf:
                chunk = sock.recv(65536)
                if not chunk:
                    break
                buf += chunk
            sock.close()
            return json.loads(buf.decode("utf-8"))

        light_png = PREVIEWS_DIR / "verdant_folio_light_gallery.png"
        dark_png = PREVIEWS_DIR / "verdant_folio_dark_gallery.png"
        try:
            r_open = rpc("engine.execute", {"command": "file.open", "params": {"path": str(OUTPUT_FIGCRAFT)}}, 1)
            assert r_open.get("ok"), f"file.open failed: {r_open}"
            r_fonts = rpc("engine.execute", {"command": "text.missingFonts", "params": {}}, 2)
            assert r_fonts.get("ok"), f"text.missingFonts failed: {r_fonts}"
            missing = r_fonts["result"].get("families", [])
            assert not missing, f"Missing fonts: {missing}"

            r_save = rpc("engine.execute", {"command": "file.saveAs", "params": {"path": str(OUTPUT_FIGCRAFT)}}, 3)
            assert r_save.get("ok"), f"file.saveAs failed: {r_save}"

            r_light = rpc("ui.render", {"ids": [light_id], "path": str(light_png), "scale": 1.25}, 4)
            assert r_light.get("ok"), f"Light render failed: {r_light}"
            r_dark = rpc("ui.render", {"ids": [dark_id], "path": str(dark_png), "scale": 1.25}, 5)
            assert r_dark.get("ok"), f"Dark render failed: {r_dark}"
            print(f"[FigCraft OK] Rendered {light_png.name} + {dark_png.name}")
        finally:
            proc.terminate()
            try:
                proc.wait(timeout=4)
            except subprocess.TimeoutExpired:
                proc.kill()

        with zipfile.ZipFile(OUTPUT_FIGCRAFT) as zf:
            names = zf.namelist()
            assert "document.json" in names
            assert "thumbnail.png" in names
            assert zf.testzip() is None
            print(f"Verified {OUTPUT_FIGCRAFT.name} ({len(names)} zip entries)")


if __name__ == "__main__":
    main()
