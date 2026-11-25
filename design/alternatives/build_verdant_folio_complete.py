#!/usr/bin/env python3
"""Build & render the complete 16-screen RotGuard FigCraft blueprint with inline celebration & clean copy."""

from __future__ import annotations

import argparse
import json
import socket
import subprocess
import time
import zipfile
from typing import Any

from fc_core import (
    ASSETS_DIR,
    FIGCRAFT_BIN,
    GALLERY_PAD_X,
    GALLERY_W,
    PREVIEWS_DIR,
    ROOT_DIR,
    SCREEN_GAP_X,
    SCREEN_H,
    SCREEN_W,
    DocBuilder,
    drop_shadow,
    hex_to_rgba,
    link_flow,
    write_figcraft_zip,
)
from vf_tokens import (
    FONT_MONO,
    FONT_SANS,
    FONT_SERIF,
    ROW_CAPTION_Y,
    ROW_H,
    ROW_SCREEN_Y,
    ROW_W,
    THEMES,
    create_screen,
    draw_inline_celebration_art,
    draw_ruled_bottom_nav,
    draw_square_cta,
    draw_square_hold_reservoir,
    draw_status_bar,
)

OUTPUT_FIGCRAFT = ROOT_DIR / "design" / "verdant-folio-complete.figcraft"


def _row_frame(
    b: DocBuilder,
    parent_id: int,
    name: str,
    y: float,
    backdrop_hex: str,
    kicker: str,
    muted_hex: str,
    captions: list[str],
) -> int:
    row = b.frame(parent_id, name, 0.0, y, ROW_W, ROW_H, fill_hex=backdrop_hex, clip=True)
    b.text(row, f"{name} Kicker", GALLERY_PAD_X, 12.0, 1500.0, 20.0, kicker, FONT_MONO, "Bold", 11.5, 18.0, muted_hex, letter_spacing_pct=4.0)
    for idx, cap in enumerate(captions):
        cx = GALLERY_PAD_X + idx * (SCREEN_W + SCREEN_GAP_X)
        b.text(row, f"{name} Cap {idx + 1}", cx, ROW_CAPTION_Y, SCREEN_W, 22.0, cap, FONT_MONO, "Bold", 11.0, 18.0, muted_hex, align_h="center")
    return row


def build_home_screen(b: DocBuilder, parent: int, slot: int, y: float, mode: str) -> tuple[int, int]:
    t = THEMES[mode]
    s = create_screen(b, parent, slot, y, f"05 Home ({mode.title()})", t["canvas"], t, "Home screen with Focus Health score, daily stats, inspiration thumbnail, and double-ruled bottom nav.")
    draw_status_bar(b, s, t)
    b.text(s, "S5 Sub", 26, 52, 200, 16, "TODAY'S FOCUS", FONT_MONO, "Bold", 10.0, 16.0, t["inkMuted"])
    b.text(s, "S5 Title", 26, 70, 210, 32, "RotGuard", FONT_SERIF, "Bold", 25.0, 30.0, t["ink"])
    b.rect(s, "Shield Sq", 262, 66, 126, 28, fill_hex=t["sageTint"], stroke_hex=t["cardBorder"], radius=4.0)
    b.ellipse(s, "Shield Dot", 274, 76, 8, 8, fill_hex=t["mossAccent"])
    b.text(s, "Shield T", 286, 72, 94, 16, "Blocker On", FONT_SANS, "Bold", 11.5, 16.0, t["mossAccent"], align_h="center")

    b.rect(s, "Hero Card", 22, 112, 368, 302, fill_hex=t["plateTerrariumBg"], stroke_hex=t["cardBorder"], stroke_w=1.2, radius=28.0, effects=drop_shadow(t["shadowHex"], 0.14 if mode == "light" else 0.32, 0.0, 10.0, 24.0))
    b.clipped_image(s, "Hero Visual", ASSETS_DIR / t["terrariumImg"], 96, 120, 220, 198, bg_hex=t["plateTerrariumBg"], stroke_hex=t["cardBorder"], stroke_w=1.0, radius=24.0)
    b.text(s, "Hero Score", 38, 330, 336, 32, "88% Focus Health", FONT_SERIF, "Bold", 23.0, 30.0, t["ink"], align_h="center")
    b.rect(s, "Hero Stage", 104, 368, 204, 26, fill_hex=t["sageTint"], stroke_hex=t["cardBorder"], radius=4.0)
    b.text(s, "Hero Stage T", 104, 373, 204, 16, "Great · 42m saved today", FONT_SANS, "Bold", 11.5, 16.0, t["mossAccent"], align_h="center")

    b.rect(s, "Stat L", 22, 430, 176, 92, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=22.0)
    b.text(s, "Stat LV", 40, 446, 140, 32, "7", FONT_SERIF, "Bold", 28.0, 32.0, t["ink"], tnum=True)
    b.text(s, "Stat LL", 40, 484, 140, 18, "Urges stopped", FONT_SANS, "Semi Bold", 12.5, 18.0, t["inkMuted"])

    b.rect(s, "Stat R", 214, 430, 176, 92, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=22.0)
    b.text(s, "Stat RV", 232, 446, 140, 32, "42m", FONT_SERIF, "Bold", 28.0, 32.0, t["mossAccent"], tnum=True)
    b.text(s, "Stat RL", 232, 484, 140, 18, "Time saved", FONT_SANS, "Semi Bold", 12.5, 18.0, t["inkMuted"])

    b.rect(s, "Mantra Card", 22, 538, 368, 114, fill_hex=t["warmCard"], stroke_hex=t["warmBorder"], radius=24.0)
    b.text(s, "Mantra K", 40, 554, 240, 14, "YOUR INSPIRATION + REMINDER", FONT_MONO, "Bold", 9.5, 14.0, t["terracotta"])
    b.text(s, "Mantra B", 40, 578, 250, 46, "“Build things that outlast\nthe feed.”", FONT_SERIF, "Italic", 15.5, 22.0, t["ink"])
    b.clipped_image(s, "Home Photo Mini", ASSETS_DIR / "photo_alpine_sunrise.png", 306, 554, 68, 82, stroke_hex=t["warmBorder"], radius=6.0)

    test_cta = draw_square_cta(b, s, "Test Blocker CTA", 22, 678, 368, 58, "Test Blocker Screen", t["s2CtaFill"], t["s2CtaInk"], shadow_hex=t["shadowHex"])
    b.text(s, "Home Foot", 22, 750, 368, 18, "Guarding YouTube, Instagram, TikTok, and Reddit", FONT_SANS, "Semi Bold", 11.0, 18.0, t["inkMuted"], align_h="center")
    draw_ruled_bottom_nav(b, s, t, 0)
    return s, test_cta


def build_blocker_opening_screen(b: DocBuilder, parent: int, slot: int, y: float, mode: str) -> tuple[int, int]:
    t = THEMES[mode]
    s = create_screen(b, parent, slot, y, f"09 Blocker Opening ({mode.title()})", t["canopy"], t, "Blocker screen showing user's Inspiration photo and opening prompt before press-and-hold begins.")
    draw_status_bar(b, s, t, on_canopy=True)
    b.rect(s, "Open Sq Tag", 96, 52, 220, 26, fill_hex=t["canopyCard"], stroke_hex=t["canopyStroke"], radius=4.0)
    b.text(s, "Open Sq Tag T", 96, 57, 220, 16, "YOUTUBE  ·  ATTEMPT #4 TODAY", FONT_MONO, "Bold", 9.5, 16.0, t["sunGold"], align_h="center")

    b.text(s, "P1 Head", 28, 92, 356, 62, "Pause before opening\nYouTube.", FONT_SERIF, "Bold", 23.0, 29.0, t["cream"], align_h="center")

    b.rect(s, "Why Photo Frame", 34, 170, 344, 240, fill_hex=t["canopyCard"], stroke_hex=t["sunGold"], stroke_alpha=0.55, stroke_w=1.2, radius=(84.0, 84.0, 20.0, 20.0))
    b.clipped_image(s, "User Why Photo", ASSETS_DIR / "photo_alpine_sunrise.png", 44, 180, 324, 174, stroke_hex=t["canopyStroke"], radius=(74.0, 74.0, 10.0, 10.0))
    b.text(s, "Why Kicker", 52, 364, 308, 14, "INSPIRATION  ·  MORNING TRAIL", FONT_MONO, "Bold", 9.5, 14.0, t["sunGold"], align_h="center")
    b.text(s, "Why Caption", 52, 381, 308, 20, "“Protect early mornings for trails, not feeds.”", FONT_SERIF, "Italic", 12.5, 18.0, t["cream"], align_h="center")

    b.rect(s, "P1 Mantra Box", 34, 426, 344, 62, fill_hex=t["canopyCard"], stroke_hex=t["canopyStroke"], radius=18.0)
    b.text(s, "P1 Mantra", 50, 446, 312, 22, "“Build things that outlast the feed.”", FONT_SERIF, "Italic", 14.0, 20.0, t["cream"], align_h="center")

    close_cta = draw_square_cta(b, s, "P1 Close CTA", 24, 514, 364, 60, "Close YouTube", t["cream"], t["canopy"], shadow_hex="#040A07")

    b.text(s, "P1 Or", 24, 594, 364, 16, "OR PRESS & HOLD TO OPEN YOUTUBE", FONT_MONO, "Bold", 10.0, 16.0, t["canopyMuted"], align_h="center")
    draw_square_hold_reservoir(b, s, 24, 624, 364, 70, 0.0, "Press & hold 4s to open", t["canopyCard"], t["sunGold"], t["canopyCore"], t["cream"])

    b.text(s, "P1 Spec Foot", 32, 718, 348, 36, "Take a slow breath before deciding.", FONT_SANS, "Regular", 12.0, 17.0, t["canopyMuted"], align_h="center")
    return s, close_cta


def build_blocker_midhold_screen(b: DocBuilder, parent: int, slot: int, y: float, mode: str) -> tuple[int, int]:
    t = THEMES[mode]
    s = create_screen(b, parent, slot, y, f"10 Blocker Mid-Hold ({mode.title()})", t["canopy"], t, "Blocker silently transitions to a second-thought prompt while user is holding before timer expires.")
    draw_status_bar(b, s, t, on_canopy=True)
    b.rect(s, "P2 Badge", 96, 52, 220, 26, fill_hex=t["canopyCard"], stroke_hex=t["sunGold"], stroke_w=1.2, radius=4.0)
    b.text(s, "P2 Badge T", 96, 57, 220, 16, "YOUTUBE  ·  1.4s REMAINING", FONT_MONO, "Bold", 9.5, 16.0, t["sunGold"], align_h="center", tnum=True)

    b.rect(s, "P1 Strip", 28, 92, 356, 56, fill_hex=t["canopyCard"], stroke_hex=t["canopyStroke"], radius=14.0)
    b.clipped_image(s, "Thumb Mini", ASSETS_DIR / "photo_alpine_sunrise.png", 38, 100, 52, 40, stroke_hex=t["canopyStroke"], radius=4.0)
    b.text(s, "Thumb Kicker", 102, 100, 268, 14, "INSPIRATION  ·  MORNING TRAIL", FONT_MONO, "Bold", 9.0, 14.0, t["sunGold"])
    b.text(s, "Thumb Sub", 102, 118, 268, 18, "Protect early mornings for trails, not feeds.", FONT_SANS, "Semi Bold", 11.5, 16.0, t["cream"])

    cx, cy = 206.0, 246.0
    b.ellipse(s, "Halo Outer", cx - 94, cy - 94, 188, 188, fill_hex=t["haloOuter"], fill_alpha=0.45)
    b.ellipse(s, "Halo Mid", cx - 78, cy - 78, 156, 156, fill_hex=t["haloMid"], fill_alpha=0.55)
    b.ellipse(s, "Track Ring", cx - 64, cy - 64, 128, 128, fill_hex=t["canopyStroke"], inner=0.82)
    b.ellipse(s, "Hold Gold 65", cx - 64, cy - 64, 128, 128, fill_hex=t["sunGold"], inner=0.82, sweep_ratio=0.65, effects=drop_shadow(t["sunGold"], 0.42, 0.0, 0.0, 18.0))
    b.ellipse(s, "Hold Core", cx - 48, cy - 48, 96, 96, fill_hex=t["canopyCore"])
    b.text(s, "Core Time", cx - 46, cy - 18, 92, 30, "2.6s", FONT_SERIF, "Bold", 25.0, 28.0, t["cream"], align_h="center", tnum=True)
    b.text(s, "Core Sub", cx - 46, cy + 16, 92, 14, "1.4s LEFT", FONT_MONO, "Bold", 9.5, 14.0, t["sunGold"], align_h="center", tnum=True)

    b.rect(s, "P2 Shift Card", 24, 356, 364, 134, fill_hex=t["canopyCard"], stroke_hex=t["sunGold"], stroke_w=1.4, radius=24.0)
    b.text(s, "P2 Card K", 42, 372, 328, 14, "ONE LAST THOUGHT", FONT_MONO, "Bold", 10.0, 14.0, t["sunGold"], align_h="center")
    b.text(s, "P2 Card Head", 40, 394, 332, 54, "Still want YouTube?\nLift your thumb to close it.", FONT_SERIF, "Bold", 18.0, 24.0, t["cream"], align_h="center")
    b.text(s, "P2 Card Sub", 42, 456, 328, 18, "Letting go resets the timer and keeps your streak.", FONT_SANS, "Semi Bold", 12.0, 18.0, t["canopyMuted"], align_h="center")

    close_cta = draw_square_cta(b, s, "P2 Close CTA", 24, 518, 364, 60, "Close YouTube", t["cream"], t["canopy"], shadow_hex="#040A07")

    b.text(s, "P2 Hold Caption", 24, 598, 364, 16, "KEEP HOLDING TO OPEN  ·  RELEASE TO CLOSE", FONT_MONO, "Bold", 9.5, 16.0, t["sunGold"], align_h="center")
    draw_square_hold_reservoir(b, s, 24, 626, 364, 70, 0.65, "Keep holding... 1.4s left", t["canopyCard"], t["sunGold"], t["canopyCore"], t["cream"])

    b.text(s, "P2 Foot", 34, 718, 344, 36, "Release anytime to return to your day.", FONT_SANS, "Regular", 12.0, 17.0, t["canopyMuted"], align_h="center")
    return s, close_cta


def build_inline_celebration_screen(b: DocBuilder, parent: int, slot: int, y: float, mode: str) -> tuple[int, int]:
    """Full-screen inline celebration directly on the blocker canvas right after closing YouTube."""
    t = THEMES[mode]
    is_dark = mode == "dark"
    s = create_screen(b, parent, slot, y, f"11 Inline Celebration ({mode.title()})", t["canopy"], t, "Full-screen inline aurora bloom celebration played smoothly right when user taps Close YouTube (no modal).")
    draw_status_bar(b, s, t, on_canopy=True)

    b.text(s, "Cele Kicker", 28, 78, 356, 16, "MOMENT RECLAIMED", FONT_MONO, "Bold", 10.5, 16.0, t["sunGold"], align_h="center", letter_spacing_pct=6.0)

    # Full-screen open radial sunrise aurora, rising embers & glowing geometric check orb
    draw_inline_celebration_art(b, s, 206.0, 266.0, t, is_dark)

    b.text(s, "Cele Headline", 28, 412, 356, 76, "Nice choice.\nYouTube closed.", FONT_SERIF, "Bold", 30.0, 36.0, t["cream"], align_h="center")
    b.text(s, "Cele Sub", 40, 498, 332, 22, "You chose what matters over autopilot.", FONT_SANS, "Regular", 14.5, 20.0, t["canopyMuted"], align_h="center")

    # Inline translucent reward stat strip (blends softly into canopy — not a modal box)
    b.rect(s, "Reward Strip", 40, 554, 332, 74, fill_hex=t["canopyCard"], fill_alpha=0.72, stroke_hex=t["sunGold"], stroke_alpha=0.38, radius=6.0)
    b.text(s, "Rew Val L", 40, 566, 165, 28, "+5 min", FONT_SERIF, "Bold", 21.0, 26.0, t["sunGold"], align_h="center", tnum=True)
    b.text(s, "Rew Lbl L", 40, 598, 165, 16, "Estimated saved", FONT_SANS, "Semi Bold", 11.5, 16.0, t["canopyMuted"], align_h="center")
    b.vrule(s, "Rew Divider", 206, 568, 46, t["canopyStroke"])
    b.text(s, "Rew Val R", 207, 566, 165, 28, "8 today", FONT_SERIF, "Bold", 21.0, 26.0, t["cream"], align_h="center", tnum=True)
    b.text(s, "Rew Lbl R", 207, 598, 165, 16, "Urges stopped", FONT_SANS, "Semi Bold", 11.5, 16.0, t["canopyMuted"], align_h="center")

    # Quiet inspiration echo
    b.clipped_image(s, "Echo Thumb", ASSETS_DIR / "photo_alpine_sunrise.png", 92, 666, 44, 36, stroke_hex=t["canopyStroke"], radius=4.0)
    b.text(s, "Echo Copy", 148, 675, 190, 20, "“Back to what matters.”", FONT_SERIF, "Italic", 13.5, 18.0, t["cream"])

    # Delicate inline auto-dismiss indicator
    b.rect(s, "AutoTrack BG", 138, 790, 136, 4, fill_hex=t["canopyStroke"], radius=2.0)
    b.rect(s, "AutoTrack FG", 138, 790, 96, 4, fill_hex=t["sunGold"], radius=2.0)
    b.text(s, "Returning Note", 28, 804, 356, 16, "Returning home...", FONT_SANS, "Semi Bold", 12.0, 16.0, t["canopyMuted"], align_h="center")
    return s, s


def build_complete_document() -> tuple[dict[str, Any], int, int, int, int, int, dict[str, tuple[bytes, int, int]]]:
    t = THEMES["light"]
    dark_t = THEMES["dark"]
    b = DocBuilder("RotGuard — Complete 16-Screen Architecture", "#0D1712")

    # =========================================================================
    # MASTER 12-SCREEN LIGHT MODE BOARD (1932 x 3200)
    # =========================================================================
    master = b.frame(b.page_id, "Complete App Blueprint (Light Mode)", 0.0, 0.0, GALLERY_W, 3200.0, fill_hex=t["backdrop"], clip=True)
    hdr = b.group(master, "Master Header", 0.0, 0.0, GALLERY_W, 148.0)
    b.text(hdr, "Master Kicker", GALLERY_PAD_X, 32.0, 920.0, 18.0, "ROTGUARD  //  ALL 12 APP SCREENS + 4 DARK MODE COMPANION SCREENS", FONT_MONO, "Bold", 11.5, 16.0, t["inkMuted"], letter_spacing_pct=4.0)
    b.text(hdr, "Master Title", GALLERY_PAD_X, 54.0, 1080.0, 44.0, "RotGuard: Complete Screen Gallery", FONT_SERIF, "Bold", 33.0, 38.0, t["ink"])
    b.text(hdr, "Master Sub", GALLERY_PAD_X, 102.0, 1280.0, 22.0, "Plain-English controls (Home, Apps, Insights, Settings), custom Inspiration photos, rotating blocker prompts, and inline post-close celebration.", FONT_SANS, "Regular", 14.5, 22.0, t["inkMuted"])

    swatches = [(t["canvas"], "#EFF3EC"), (t["mossAccent"], "#2F6F4E"), (t["terracotta"], "#C96846"), (t["canopy"], "#1C352B"), (t["sunGold"], "#DFA952")]
    sw_start = GALLERY_W - GALLERY_PAD_X - len(swatches) * 60.0 + 10.0
    for idx, (sh, sn) in enumerate(swatches):
        sx = sw_start + idx * 60.0
        b.rect(hdr, f"MSwatch {idx}", sx, 52.0, 48.0, 30.0, fill_hex=sh, stroke_hex=t["ruleStrong"], radius=4.0)
        b.text(hdr, f"MSwatch T {idx}", sx, 86.0, 48.0, 14.0, sn, FONT_MONO, "Regular", 8.5, 12.0, t["inkMuted"], align_h="center")
    b.rule(hdr, "Master Rule", GALLERY_PAD_X, 136.0, GALLERY_W - 2 * GALLERY_PAD_X, t["rule"], thickness=1.0)

    # ROW 1: ONBOARDING FLOW (01 - 04)
    row1 = _row_frame(
        b,
        master,
        "Row 1 Onboarding",
        152.0,
        t["backdrop"],
        "ROW 01  //  ONBOARDING FLOW: WELCOME -> SCROLL SIMULATOR -> GOALS -> INSPIRATION & PLEDGE",
        t["inkMuted"],
        ["01  WELCOME · INTRO", "02  SCROLL SIMULATOR · PULSES", "03  TIME AUDIT & GOALS", "04  INSPIRATION & PLEDGE"],
    )

    # 01 Welcome
    s1 = create_screen(b, row1, 0, ROW_SCREEN_Y, "01 Welcome", t["canvas"], t, "Clean welcome screen explaining press-and-hold blocking, inspiration photos, and local privacy.")
    draw_status_bar(b, s1, t)
    b.rect(s1, "S1 Badge", 128, 52, 156, 24, fill_hex=t["sageTint"], stroke_hex=t["cardBorder"], radius=4.0)
    b.text(s1, "S1 Badge T", 128, 56, 156, 16, "STEP 01 · WELCOME", FONT_MONO, "Bold", 10.0, 16.0, t["mossAccent"], align_h="center")
    b.clipped_image(s1, "S1 Hero", ASSETS_DIR / t["terrariumImg"], 100, 96, 212, 198, bg_hex=t["plateTerrariumBg"], stroke_hex=t["cardBorder"], stroke_w=1.5, radius=(106.0, 106.0, 26.0, 26.0))
    b.text(s1, "S1 Headline", 28, 314, 356, 68, "Reclaim your\nwaking hours.", FONT_SERIF, "Bold", 28.0, 33.0, t["ink"], align_h="center")
    b.text(s1, "S1 Sub", 36, 392, 340, 42, "Adds calm physical friction when you open distracting apps, anchored to your real goals.", FONT_SANS, "Regular", 13.5, 19.0, t["inkMuted"], align_h="center")
    promises = [
        ("Press-and-Hold Pause", "A 4-second physical hold stops autopilot taps.", t["mossAccent"]),
        ("Your Inspiration Photos", "See personal reminder photos on every blocker.", t["terracotta"]),
        ("Private on Your Phone", "Zero accounts needed. All history stays private.", t["sunGold"]),
    ]
    for idx, (pt, ps, pdot) in enumerate(promises):
        py = 462.0 + idx * 78.0
        b.rect(s1, f"Prom Card {idx}", 24, py, 364, 66, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=20.0)
        b.ellipse(s1, f"Prom Dot {idx}", 42, py + 26, 14, 14, fill_hex=pdot)
        b.text(s1, f"Prom T {idx}", 70, py + 13, 298, 18, pt, FONT_SANS, "Bold", 14.0, 18.0, t["ink"])
        b.text(s1, f"Prom S {idx}", 70, py + 34, 298, 18, ps, FONT_SANS, "Regular", 12.0, 16.0, t["inkMuted"])
    cta1 = draw_square_cta(b, s1, "S1 Get Started", 24, 780, 364, 58, "Get Started", t["mossBtnFill"], t["mossBtnInk"], t["mossBtnStroke"], shadow_hex=t["shadowHex"])
    b.text(s1, "S1 Note", 24, 850, 364, 16, "Takes 90 seconds to set up", FONT_SANS, "Semi Bold", 11.5, 16.0, t["inkMuted"], align_h="center")

    # 02 Scroll Simulator
    s2 = create_screen(b, row1, 1, ROW_SCREEN_Y, "02 Scroll Simulator", t["canvas"], t, "Interactive 5-stage scroll simulator with botanical visual and gentle-to-sharp tactile pulse feedback.")
    draw_status_bar(b, s2, t)
    b.rect(s2, "S2 Badge", 108, 52, 196, 24, fill_hex=t["sageTint"], stroke_hex=t["cardBorder"], radius=4.0)
    b.text(s2, "S2 Badge T", 108, 56, 196, 16, "STEP 02 · SCROLL SIMULATOR", FONT_MONO, "Bold", 10.0, 16.0, t["mossAccent"], align_h="center")
    b.text(s2, "S2 Head", 28, 88, 356, 62, "See what endless\nscrolling costs.", FONT_SERIF, "Bold", 24.0, 29.0, t["ink"], align_h="center")
    b.clipped_image(s2, "S2 Sprout", ASSETS_DIR / t["sproutImg"], 106, 162, 200, 192, bg_hex=t["plateSproutBg"], stroke_hex=t["cardBorder"], stroke_w=1.5, radius=(100.0, 100.0, 22.0, 22.0))
    b.rect(s2, "S2 Stage Card", 24, 372, 364, 146, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=22.0)
    b.rect(s2, "S2 Stage Pill", 42, 390, 152, 22, fill_hex=t["terracottaSoft"], radius=4.0)
    b.text(s2, "S2 Stage Pill T", 42, 393, 152, 16, "STAGE 4 · HIGH DRAIN", FONT_MONO, "Bold", 9.5, 16.0, t["terracotta"], align_h="center")
    b.text(s2, "S2 Reels", 220, 392, 150, 18, "150+ Reels / Day", FONT_SANS, "Bold", 13.5, 18.0, t["ink"], align_h="right")
    b.text(s2, "S2 Stage Copy", 42, 424, 328, 44, "Rapid short-form video wears down focus, leaving quiet everyday work feeling flat.", FONT_SANS, "Regular", 13.0, 18.0, t["inkMuted"])
    b.rect(s2, "S2 Pulse Tag", 42, 478, 328, 26, fill_hex=t["sageTint"], radius=4.0)
    b.text(s2, "S2 Pulse T", 42, 483, 328, 16, "PULSE: RAPID TRIPLE VIBRATION", FONT_MONO, "Bold", 10.0, 16.0, t["mossAccent"], align_h="center")

    b.text(s2, "S2 Tap Q", 24, 540, 364, 16, "TAP ANY STAGE TO FEEL THE PULSE", FONT_MONO, "Bold", 10.0, 16.0, t["inkMuted"], align_h="center")
    for idx, lbl in enumerate(["0", "50", "100", "150", "200+"]):
        kx = 24.0 + idx * 74.5
        sel = lbl == "150"
        b.rect(s2, f"Sim Sq {idx}", kx, 566, 66, 44, fill_hex=t["terracotta"] if sel else t["card"], stroke_hex=t["terracotta"] if sel else t["cardBorder"], stroke_w=1.4, radius=6.0)
        b.text(s2, f"Sim Sq T {idx}", kx, 579, 66, 18, lbl, FONT_MONO, "Bold", 12.5, 18.0, "#FFFFFF" if sel else t["ink"], align_h="center", tnum=True)

    b.text(s2, "S2 Calm Desc", 34, 636, 344, 42, "Notice how sharp Stage 4 feels compared to the calm tap at Stage 0.", FONT_SANS, "Regular", 12.5, 17.5, t["inkMuted"], align_h="center")
    cta2 = draw_square_cta(b, s2, "S2 Continue", 24, 780, 364, 58, "Continue", t["mossBtnFill"], t["mossBtnInk"], t["mossBtnStroke"], shadow_hex=t["shadowHex"])
    b.text(s2, "S2 Foot", 24, 850, 364, 16, "Gentle vibration feedback enabled", FONT_SANS, "Semi Bold", 11.5, 16.0, t["inkMuted"], align_h="center")

    # 03 Time Audit & Focus Goals
    s3 = create_screen(b, row1, 2, ROW_SCREEN_Y, "03 Time Audit & Goals", t["canvas"], t, "Waking years calculator, square daily selectors, goal checkboxes, and personal reminder input.")
    draw_status_bar(b, s3, t)
    b.rect(s3, "S3 Badge", 102, 52, 208, 24, fill_hex=t["sageTint"], stroke_hex=t["cardBorder"], radius=4.0)
    b.text(s3, "S3 Badge T", 102, 56, 208, 16, "STEP 03 · TIME AUDIT & GOALS", FONT_MONO, "Bold", 10.0, 16.0, t["mossAccent"], align_h="center")
    b.text(s3, "S3 Head", 28, 86, 356, 34, "Reclaim your waking years.", FONT_SERIF, "Bold", 23.0, 30.0, t["ink"], align_h="center")

    b.rect(s3, "S3 Loss Card", 24, 134, 364, 98, fill_hex=t["warmCard"], stroke_hex=t["warmBorder"], radius=22.0)
    b.text(s3, "S3 Loss K", 42, 148, 328, 14, "AT 3 HOURS DAILY", FONT_MONO, "Bold", 10.0, 14.0, t["terracotta"], align_h="center")
    b.text(s3, "S3 Loss V", 42, 166, 328, 30, "9.4 waking years", FONT_SERIF, "Bold", 24.0, 28.0, t["terracotta"], align_h="center", tnum=True)
    b.text(s3, "S3 Loss D", 42, 200, 328, 18, "45 full days each year spent inside feeds", FONT_SANS, "Regular", 12.5, 18.0, t["inkMuted"], align_h="center")

    b.text(s3, "S3 Hr Lbl", 24, 248, 364, 14, "DAILY SCROLL HOURS", FONT_MONO, "Bold", 10.0, 14.0, t["inkMuted"])
    for idx, hr in enumerate(["1 hr", "2 hrs", "3 hrs", "4+ hrs"]):
        hx = 24.0 + idx * 93.0
        sel = hr == "3 hrs"
        b.rect(s3, f"S3 Hr {idx}", hx, 270, 85, 38, fill_hex=t["mossBtnFill"] if sel else t["card"], stroke_hex=t["mossBtnStroke"] if sel else t["cardBorder"], radius=6.0)
        b.text(s3, f"S3 Hr T {idx}", hx, 280, 85, 18, hr, FONT_SANS, "Bold", 12.5, 18.0, t["mossBtnInk"] if sel else t["ink"], align_h="center")

    b.text(s3, "S3 Goals Lbl", 24, 326, 364, 14, "YOUR FOCUS GOALS", FONT_MONO, "Bold", 10.0, 14.0, t["inkMuted"])
    for idx, (gtxt, on) in enumerate([("Deep work & quiet reading", True), ("Sleep peacefully before midnight", True), ("Create & ship real projects", False)]):
        gy = 348.0 + idx * 54.0
        b.rect(s3, f"S3 G {idx}", 24, gy, 364, 46, fill_hex=t["card"], stroke_hex=t["mossAccent"] if on else t["cardBorder"], stroke_w=1.4 if on else 1.0, radius=6.0)
        b.rect(s3, f"S3 C {idx}", 38, gy + 14, 18, 18, fill_hex=t["canvas"], stroke_hex=t["mossAccent"] if on else t["ruleStrong"], radius=2.0)
        if on:
            b.rect(s3, f"S3 CF {idx}", 42, gy + 18, 10, 10, fill_hex=t["mossAccent"], radius=1.0)
        b.text(s3, f"S3 GT {idx}", 68, gy + 13, 300, 20, gtxt, FONT_SANS, "Bold" if on else "Regular", 13.5, 20.0, t["ink"] if on else t["inkMuted"])

    b.text(s3, "S3 Mant Lbl", 24, 524, 364, 14, "PERSONAL REMINDER", FONT_MONO, "Bold", 10.0, 14.0, t["inkMuted"])
    b.rect(s3, "S3 Mant Input", 24, 546, 364, 56, fill_hex=t["card"], stroke_hex=t["ruleStrong"], stroke_w=1.2, radius=6.0)
    b.text(s3, "S3 Mant Val", 40, 563, 332, 22, "Build things that outlast the feed.", FONT_SERIF, "Italic", 14.5, 20.0, t["ink"])

    b.text(s3, "S3 Preview Foot", 34, 622, 344, 36, "Your reminder and photos appear whenever a blocked app opens.", FONT_SANS, "Regular", 12.0, 17.0, t["inkMuted"], align_h="center")
    cta3 = draw_square_cta(b, s3, "S3 Plan CTA", 24, 780, 364, 58, "Save My Focus Plan", t["mossBtnFill"], t["mossBtnInk"], t["mossBtnStroke"], shadow_hex=t["shadowHex"])
    b.text(s3, "S3 Foot", 24, 850, 364, 16, "Step 3 of 4 · Change anytime", FONT_SANS, "Semi Bold", 11.5, 16.0, t["inkMuted"], align_h="center")

    # 04 Inspiration & Pledge
    s4 = create_screen(b, row1, 3, ROW_SCREEN_Y, "04 Inspiration & Pledge", t["canvas"], t, "Pick Inspiration photos for the blocker screen, confirm app permissions, and seal pledge with 3s hold.")
    draw_status_bar(b, s4, t)
    b.rect(s4, "S4 Badge", 96, 52, 220, 24, fill_hex=t["sageTint"], stroke_hex=t["cardBorder"], radius=4.0)
    b.text(s4, "S4 Badge T", 96, 56, 220, 16, "STEP 04 · INSPIRATION & PLEDGE", FONT_MONO, "Bold", 10.0, 16.0, t["mossAccent"], align_h="center")
    b.text(s4, "S4 Head", 28, 86, 356, 34, "Add your inspiration.", FONT_SERIF, "Bold", 23.0, 29.0, t["ink"], align_h="center")

    b.rect(s4, "Why Card", 24, 134, 364, 198, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=22.0)
    b.text(s4, "Why Card K", 40, 150, 332, 14, "INSPIRATION PHOTOS", FONT_MONO, "Bold", 9.5, 14.0, t["terracotta"])
    b.text(s4, "Why Card S", 40, 168, 332, 18, "Photos show on your pause screen to remind you why.", FONT_SANS, "Regular", 12.5, 18.0, t["inkMuted"])

    b.clipped_image(s4, "Pick 1", ASSETS_DIR / "photo_alpine_sunrise.png", 40, 196, 102, 86, stroke_hex=t["mossAccent"], stroke_w=1.5, radius=6.0)
    b.text(s4, "Pick 1 Lbl", 40, 288, 102, 14, "Morning Trail", FONT_SANS, "Bold", 11.0, 14.0, t["ink"], align_h="center")
    b.clipped_image(s4, "Pick 2", ASSETS_DIR / "photo_cedar_grove.png", 155, 196, 102, 86, stroke_hex=t["mossAccent"], stroke_w=1.5, radius=6.0)
    b.text(s4, "Pick 2 Lbl", 155, 288, 102, 14, "Redwood Path", FONT_SANS, "Bold", 11.0, 14.0, t["ink"], align_h="center")

    b.rect(s4, "Add Photo Slot", 270, 196, 102, 86, fill_hex=t["canvas"], stroke_hex=t["ruleStrong"], stroke_w=1.4, dashes=[5.0, 5.0], radius=6.0)
    b.text(s4, "Add Plus", 270, 214, 102, 24, "+", FONT_SANS, "Bold", 22.0, 24.0, t["mossAccent"], align_h="center")
    b.text(s4, "Add Txt", 270, 244, 102, 16, "Add Photo", FONT_SANS, "Bold", 11.5, 16.0, t["mossAccent"], align_h="center")
    b.text(s4, "Pick 3 Lbl", 270, 288, 102, 14, "From Gallery", FONT_SANS, "Regular", 11.0, 14.0, t["inkMuted"], align_h="center")

    b.rect(s4, "Perm Box", 24, 348, 364, 126, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=22.0)
    b.text(s4, "Perm K", 40, 364, 332, 14, "APP PAUSE PERMISSIONS", FONT_MONO, "Bold", 9.5, 14.0, t["inkMuted"])
    for idx, (pname, psub) in enumerate([("Detect Blocked Apps", "Knows when blocked apps open"), ("Show Pause Screen", "Displays calm screen over YouTube")]):
        py = 388.0 + idx * 42.0
        b.text(s4, f"P N {idx}", 40, py, 210, 18, pname, FONT_SANS, "Bold", 13.5, 18.0, t["ink"])
        b.text(s4, f"P S {idx}", 40, py + 17, 224, 14, psub, FONT_SANS, "Regular", 11.0, 14.0, t["inkMuted"])
        b.rect(s4, f"P B {idx}", 286, py + 4, 86, 22, fill_hex=t["sageTint"], radius=4.0)
        b.text(s4, f"P T {idx}", 286, py + 7, 86, 14, "READY", FONT_MONO, "Bold", 10.0, 14.0, t["mossAccent"], align_h="center")

    b.text(s4, "Pledge Ask", 36, 540, 340, 42, "Press & hold for 3 seconds below to finish setup and try the pause screen:", FONT_SANS, "Semi Bold", 13.0, 18.5, t["ink"], align_h="center")
    draw_square_hold_reservoir(b, s4, 24, 608, 364, 72, 0.70, "HOLD 3s TO FINISH  ·  2.1s", t["card"], t["mossAccent"], t["canvas"], t["mossAccent"])
    b.text(s4, "Pledge Haptic", 34, 700, 344, 18, "Gentle vibration builds as the bar fills", FONT_SANS, "Regular", 11.5, 16.0, t["inkMuted"], align_h="center")

    # =========================================================================
    # ROW 2: HOME, APPS, INSIGHTS & INSPIRATION (05 - 08)
    # =========================================================================
    row2 = _row_frame(
        b,
        master,
        "Row 2 Core Apps",
        1168.0,
        t["backdrop"],
        "ROW 02  //  CORE SCREENS: HOME, APPS, INSIGHTS & INSPIRATION",
        t["inkMuted"],
        ["05  HOME · FOCUS DASHBOARD", "06  APPS · BLOCKED APPS", "07  INSIGHTS · DUEL & TREND", "08  INSPIRATION · PERSONAL PHOTOS"],
    )

    s5, test_cta = build_home_screen(b, row2, 0, ROW_SCREEN_Y, "light")

    # 06 Apps — Blocked Apps
    s6 = create_screen(b, row2, 1, ROW_SCREEN_Y, "06 Apps — Blocked Apps", t["canvas"], t, "Clean app selection list with square hold duration buttons and simple toggles.")
    draw_status_bar(b, s6, t)
    b.text(s6, "S6 Title", 26, 52, 240, 30, "Blocked Apps", FONT_SERIF, "Bold", 24.0, 30.0, t["ink"])
    b.rect(s6, "S6 Search", 22, 96, 368, 44, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=6.0)
    b.text(s6, "S6 Search T", 38, 108, 336, 20, "Search installed apps (YouTube, Chrome...)", FONT_SANS, "Regular", 13.0, 20.0, t["inkMuted"])

    b.text(s6, "S6 Preset K", 24, 156, 364, 14, "HOLD DURATION TO UNLOCK", FONT_MONO, "Bold", 10.0, 14.0, t["inkMuted"])
    for idx, dur in enumerate(["3s", "4s", "6s", "10s"]):
        dx = 22.0 + idx * 94.0
        sel = dur == "4s"
        b.rect(s6, f"S6 Dur {idx}", dx, 178, 86, 36, fill_hex=t["mossBtnFill"] if sel else t["card"], stroke_hex=t["mossBtnStroke"] if sel else t["cardBorder"], radius=6.0)
        b.text(s6, f"S6 Dur T {idx}", dx, 187, 86, 18, dur, FONT_MONO, "Bold", 12.0, 18.0, t["mossBtnInk"] if sel else t["ink"], align_h="center")

    apps = [
        ("YouTube", "4s hold · 5 closed today", True, t["terracotta"]),
        ("Instagram", "4s hold · 4 closed today", True, t["sunGold"]),
        ("TikTok", "6s hold · 2 closed today", True, t["mossAccent"]),
        ("Reddit", "4s hold · 3 closed today", True, t["terracotta"]),
        ("Chrome", "Not blocked", False, t["inkMuted"]),
    ]
    for idx, (aname, sub, on, dot_col) in enumerate(apps):
        ay = 232.0 + idx * 72.0
        b.rect(s6, f"S6 App {idx}", 22, ay, 368, 60, fill_hex=t["card"], stroke_hex=t["mossAccent"] if idx == 0 else t["cardBorder"], stroke_w=1.4 if idx == 0 else 1.0, radius=22.0)
        b.ellipse(s6, f"S6 Dot {idx}", 38, ay + 23, 14, 14, fill_hex=dot_col, fill_alpha=0.9 if on else 0.3)
        b.text(s6, f"S6 N {idx}", 64, ay + 12, 210, 20, aname, FONT_SANS, "Bold", 15.0, 20.0, t["ink"])
        b.text(s6, f"S6 S {idx}", 64, ay + 34, 220, 16, sub, FONT_SANS, "Regular", 11.5, 16.0, t["inkMuted"])
        b.rect(s6, f"S6 Sw {idx}", 324, ay + 17, 48, 26, fill_hex=t["mossAccent"] if on else t["sageTint"], radius=13.0)
        b.ellipse(s6, f"S6 K {idx}", 349 if on else 327, ay + 20, 20, 20, fill_hex=t["switchKnobOff"] if not on else "#FFFFFF")

    b.text(s6, "S6 Note", 22, 610, 368, 36, "Opening any blocked app shows a pause screen with your Inspiration photo.", FONT_SANS, "Semi Bold", 12.0, 17.0, t["inkMuted"], align_h="center")
    draw_ruled_bottom_nav(b, s6, t, 1)

    # 07 Insights
    s7 = create_screen(b, row2, 2, ROW_SCREEN_Y, "07 Insights", t["canvas"], t, "Daily Focus Duel comparison, 7-day saved time trend, and blocked apps breakdown.")
    draw_status_bar(b, s7, t)
    b.text(s7, "S7 Title", 26, 52, 240, 30, "Insights", FONT_SERIF, "Bold", 24.0, 30.0, t["ink"])
    b.rect(s7, "S7 Streak", 282, 54, 108, 26, fill_hex=t["sageTint"], stroke_hex=t["cardBorder"], radius=4.0)
    b.text(s7, "S7 Streak T", 282, 59, 108, 16, "7D STREAK", FONT_MONO, "Bold", 10.0, 16.0, t["mossAccent"], align_h="center")

    b.rect(s7, "S7 Duel Card", 22, 100, 368, 148, fill_hex=t["warmCard"], stroke_hex=t["warmBorder"], radius=24.0)
    b.text(s7, "S7 Duel K", 40, 116, 260, 14, "TODAY'S FOCUS DUEL", FONT_MONO, "Bold", 10.0, 14.0, t["terracotta"])
    b.text(s7, "S7 Duel Verdict", 40, 136, 332, 26, "86% fewer bypasses today", FONT_SERIF, "Bold", 19.0, 24.0, t["ink"])
    b.rect(s7, "You Box", 40, 174, 154, 58, fill_hex=t["card"], stroke_hex=t["mossAccent"], radius=6.0)
    b.text(s7, "You N", 40, 182, 154, 24, "2", FONT_SERIF, "Bold", 21.0, 24.0, t["mossAccent"], align_h="center", tnum=True)
    b.text(s7, "You L", 40, 208, 154, 14, "YOUR BYPASSES", FONT_MONO, "Bold", 9.0, 14.0, t["inkMuted"], align_h="center")
    b.rect(s7, "Avg Box", 218, 174, 154, 58, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=6.0)
    b.text(s7, "Avg N", 218, 182, 154, 24, "14", FONT_SERIF, "Bold", 21.0, 24.0, t["terracotta"], align_h="center", tnum=True)
    b.text(s7, "Avg L", 218, 208, 154, 14, "AVG SCROLLER", FONT_MONO, "Bold", 9.0, 14.0, t["inkMuted"], align_h="center")

    b.rect(s7, "S7 Chart Card", 22, 266, 368, 280, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=26.0)
    b.text(s7, "S7 Chart Title", 40, 284, 200, 22, "7-Day Focus Health", FONT_SERIF, "Bold", 16.5, 22.0, t["ink"])
    b.text(s7, "S7 Chart Tot", 240, 287, 130, 18, "4h 54m saved", FONT_SANS, "Bold", 12.0, 18.0, t["mossAccent"], align_h="right")
    for idx, (dlbl, pct) in enumerate([("Mon", 64), ("Tue", 72), ("Wed", 78), ("Thu", 82), ("Fri", 85), ("Sat", 91), ("Sun", 88)]):
        bx = 42.0 + idx * 48.0
        bh = round(148.0 * (pct / 100.0), 1)
        by = 486.0 - bh
        b.rect(s7, f"S7BG {idx}", bx, 338, 28, 148, fill_hex=t["canvas"], radius=14.0)
        b.rect(s7, f"S7FG {idx}", bx, by, 28, bh, fill_hex=t["mossAccent"] if pct >= 80 else t["sunGold"], radius=14.0)
        b.text(s7, f"S7Val {idx}", bx, by - 16, 28, 14, f"{pct}", FONT_SANS, "Bold", 10.0, 14.0, t["inkMuted"], align_h="center", tnum=True)
        b.text(s7, f"S7Day {idx}", bx, 496, 28, 16, dlbl, FONT_SANS, "Semi Bold", 11.0, 16.0, t["inkMuted"], align_h="center")

    b.rect(s7, "S7 Top Apps", 22, 564, 368, 228, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=24.0)
    b.text(s7, "S7 Top K", 40, 582, 332, 16, "MOST CLOSED TODAY", FONT_MONO, "Bold", 10.0, 14.0, t["inkMuted"])
    for idx, (aname, sub, saved) in enumerate([("YouTube", "5 closed · 1 opened", "25m saved"), ("Instagram", "4 closed · 1 opened", "20m saved"), ("Reddit", "3 closed · 0 opened", "15m saved")]):
        ry = 614.0 + idx * 54.0
        b.text(s7, f"S7 AN {idx}", 40, ry, 180, 20, aname, FONT_SANS, "Bold", 14.5, 20.0, t["ink"])
        b.text(s7, f"S7 AS {idx}", 40, ry + 20, 210, 16, sub, FONT_SANS, "Regular", 11.5, 16.0, t["inkMuted"])
        b.text(s7, f"S7 AV {idx}", 256, ry + 8, 116, 18, saved, FONT_MONO, "Bold", 11.5, 18.0, t["mossAccent"], align_h="right", tnum=True)

    draw_ruled_bottom_nav(b, s7, t, 2)

    # 08 Inspiration (renamed from Blocker Photos; zero internal rules exposed)
    s8 = create_screen(b, row2, 3, ROW_SCREEN_Y, "08 Inspiration", t["canvas"], t, "Dedicated Inspiration screen to manage personal reminder photos and quote preferences with zero jargon.")
    draw_status_bar(b, s8, t)
    b.text(s8, "S8 Title", 26, 52, 220, 30, "Inspiration", FONT_SERIF, "Bold", 24.0, 30.0, t["ink"])
    b.rect(s8, "S8 Tag", 292, 54, 98, 26, fill_hex=t["sageTint"], stroke_hex=t["cardBorder"], radius=4.0)
    b.text(s8, "S8 Tag T", 292, 59, 98, 16, "3 PHOTOS", FONT_MONO, "Bold", 9.5, 16.0, t["mossAccent"], align_h="center")

    b.rect(s8, "Insp Photos Card", 22, 98, 368, 344, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=24.0)
    b.text(s8, "IPC Head", 40, 114, 230, 16, "YOUR PHOTOS", FONT_MONO, "Bold", 10.0, 14.0, t["terracotta"])
    b.rect(s8, "IPC Rot Sw", 326, 112, 46, 24, fill_hex=t["mossAccent"], radius=12.0)
    b.ellipse(s8, "IPC Rot K", 350, 115, 18, 18, fill_hex="#FFFFFF")
    b.text(s8, "IPC Sub", 40, 134, 332, 16, "Shown on the pause screen to remind you why to stop.", FONT_SANS, "Regular", 11.5, 16.0, t["inkMuted"])

    tiles = [
        ("Morning Trail", "photo_alpine_sunrise.png"),
        ("Redwood Path", "photo_cedar_grove.png"),
        ("Slow Evenings", "photo_coastal_horizon.png"),
    ]
    for idx, (tlbl, imgf) in enumerate(tiles):
        col_i = idx % 2
        row_i = idx // 2
        tx = 40.0 + col_i * 172.0
        ty = 162.0 + row_i * 134.0
        b.clipped_image(s8, f"InspImg {idx}", ASSETS_DIR / imgf, tx, ty, 160, 102, stroke_hex=t["cardBorder"], radius=6.0)
        b.text(s8, f"InspLbl {idx}", tx, ty + 106, 160, 16, tlbl, FONT_SANS, "Bold", 12.0, 16.0, t["ink"])

    ax, ay = 212.0, 296.0
    b.rect(s8, "Insp Add Box", ax, ay, 160, 102, fill_hex=t["canvas"], stroke_hex=t["ruleStrong"], stroke_w=1.4, dashes=[5.0, 5.0], radius=6.0)
    b.text(s8, "Insp Add +", ax, ay + 24, 160, 26, "+", FONT_SANS, "Bold", 24.0, 26.0, t["mossAccent"], align_h="center")
    b.text(s8, "Insp Add T", ax, ay + 54, 160, 18, "Add Your Photo", FONT_SANS, "Bold", 12.5, 18.0, t["mossAccent"], align_h="center")
    b.text(s8, "Insp Add Sub", ax, ay + 106, 160, 16, "From Phone Gallery", FONT_SANS, "Regular", 11.0, 16.0, t["inkMuted"])

    # Clean Reminder & Prompt Settings Card (zero leaky rules/numbers!)
    b.rect(s8, "Insp Rem Card", 22, 458, 368, 272, fill_hex=t["warmCard"], stroke_hex=t["warmBorder"], radius=24.0)
    b.text(s8, "IRem K", 40, 476, 332, 14, "YOUR REMINDER MESSAGE", FONT_MONO, "Bold", 10.0, 14.0, t["terracotta"])
    b.rect(s8, "IRem Input", 40, 500, 332, 60, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=6.0)
    b.text(s8, "IRem Quote", 54, 519, 304, 22, "“Build things that outlast the feed.”", FONT_SERIF, "Italic", 14.0, 20.0, t["ink"])

    b.text(s8, "RowA T", 40, 584, 250, 18, "Show photos on pause screen", FONT_SANS, "Bold", 13.5, 18.0, t["ink"])
    b.text(s8, "RowA S", 40, 604, 250, 16, "Rotates through your saved collection", FONT_SANS, "Regular", 11.5, 16.0, t["inkMuted"])
    b.rect(s8, "RowA Sw", 326, 588, 46, 24, fill_hex=t["mossAccent"], radius=12.0)
    b.ellipse(s8, "RowA K", 350, 591, 18, 18, fill_hex="#FFFFFF")

    b.rule(s8, "IRem Div", 40, 636, 332, t["warmBorder"])
    b.text(s8, "RowB T", 40, 654, 250, 18, "Thoughtful daily prompts", FONT_SANS, "Bold", 13.5, 18.0, t["ink"])
    b.text(s8, "RowB S", 40, 674, 250, 16, "Fresh pause reflections every time", FONT_SANS, "Regular", 11.5, 16.0, t["inkMuted"])
    b.rect(s8, "RowB Sw", 326, 658, 46, 24, fill_hex=t["mossAccent"], radius=12.0)
    b.ellipse(s8, "RowB K", 350, 661, 18, 18, fill_hex="#FFFFFF")

    preview_insp_cta = draw_square_cta(b, s8, "Preview Insp CTA", 22, 748, 368, 56, "Preview Pause Screen", t["s2CtaFill"], t["s2CtaInk"], shadow_hex=t["shadowHex"])
    draw_ruled_bottom_nav(b, s8, t, 3)

    # =========================================================================
    # ROW 3: LIVE BLOCKER STORYBOARD (OPENING -> MID-HOLD -> INLINE CELEBRATION) + SETTINGS
    # =========================================================================
    row3 = _row_frame(
        b,
        master,
        "Row 3 Blocker Celebration Settings",
        2180.0,
        t["backdrop"],
        "ROW 03  //  LIVE BLOCKER FLOW: OPENING PHOTO -> MID-HOLD PROMPT -> INLINE CELEBRATION ON CLOSE + SETTINGS",
        t["inkMuted"],
        ["09  BLOCKER · OPENING PHOTO", "10  BLOCKER · MID-HOLD PROMPT", "11  CLOSE CELEBRATION (INLINE)", "12  SETTINGS · PREFERENCES"],
    )

    s9, close_p1 = build_blocker_opening_screen(b, row3, 0, ROW_SCREEN_Y, "light")
    s10, close_p2 = build_blocker_midhold_screen(b, row3, 1, ROW_SCREEN_Y, "light")
    s11, _ = build_inline_celebration_screen(b, row3, 2, ROW_SCREEN_Y, "light")

    # 12 Settings (zero internal technical jargon!)
    s12 = create_screen(b, row3, 3, ROW_SCREEN_Y, "12 Settings", t["canvas"], t, "Plain-language permission statuses, Light/Dark/System square buttons, and Inspiration link.")
    draw_status_bar(b, s12, t)
    b.text(s12, "S12 Title", 26, 52, 240, 30, "Settings", FONT_SERIF, "Bold", 24.0, 30.0, t["ink"])

    b.rect(s12, "S12 Perm Card", 22, 98, 368, 188, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=24.0)
    b.text(s12, "S12 Perm K", 40, 114, 332, 14, "APP PAUSE STATUS", FONT_MONO, "Bold", 9.5, 14.0, t["inkMuted"])
    for idx, (dn, ds) in enumerate([("Detect Blocked Apps", "ON"), ("Show Pause Screen", "ON"), ("Screen Time Insights", "ON")]):
        dy = 142.0 + idx * 46.0
        b.text(s12, f"DN {idx}", 40, dy + 3, 210, 18, dn, FONT_SANS, "Bold", 13.5, 18.0, t["ink"])
        b.rect(s12, f"DB {idx}", 298, dy, 74, 24, fill_hex=t["sageTint"], radius=4.0)
        b.text(s12, f"DT {idx}", 298, dy + 4, 74, 16, ds, FONT_MONO, "Bold", 10.0, 16.0, t["mossAccent"], align_h="center")

    b.rect(s12, "Theme Card", 22, 302, 368, 118, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=22.0)
    b.text(s12, "Theme K", 40, 318, 332, 14, "APPEARANCE", FONT_MONO, "Bold", 9.5, 14.0, t["inkMuted"])
    for idx, mlbl in enumerate(["Light", "Dark", "System"]):
        mx = 40.0 + idx * 114.0
        sel = mlbl == "Light"
        b.rect(s12, f"Mode Btn {idx}", mx, 344, 104, 42, fill_hex=t["mossBtnFill"] if sel else t["canvas"], stroke_hex=t["mossBtnStroke"] if sel else t["cardBorder"], radius=6.0)
        b.text(s12, f"Mode T {idx}", mx, 355, 104, 20, mlbl, FONT_SANS, "Bold", 13.0, 20.0, t["mossBtnInk"] if sel else t["ink"], align_h="center")

    b.rect(s12, "Calib Card", 22, 436, 368, 282, fill_hex=t["card"], stroke_hex=t["cardBorder"], radius=24.0)
    b.text(s12, "Calib K", 40, 452, 332, 14, "PREFERENCES", FONT_MONO, "Bold", 9.5, 14.0, t["inkMuted"])
    for idx, (cn, cv) in enumerate([("Inspiration", "3 photos added"), ("Default Hold Time", "4 seconds"), ("Vibration Feedback", "On · Escalates gently"), ("Focus Reminder", "Build things that outlast the feed.")]):
        cy_row = 482.0 + idx * 58.0
        b.text(s12, f"CN {idx}", 40, cy_row, 332, 18, cn, FONT_SANS, "Bold", 14.0, 18.0, t["ink"])
        b.text(s12, f"CV {idx}", 40, cy_row + 20, 332, 16, cv, FONT_SANS, "Semi Bold", 12.0, 16.0, t["mossAccent"])
        if idx < 3:
            b.rule(s12, f"CR {idx}", 40, cy_row + 46, 332, t["rule"])

    replay_cta = draw_square_cta(b, s12, "Replay CTA", 22, 742, 368, 56, "Replay Onboarding", t["sageTint"], t["ink"], stroke_hex=t["cardBorder"], shadow_hex=t["shadowHex"])
    draw_ruled_bottom_nav(b, s12, t, 3)

    # =========================================================================
    # ROW 4: DARK MODE COMPANION BOARD (HOME, OPENING, MID-HOLD, INLINE CELEBRATION)
    # =========================================================================
    dark_board = b.frame(b.page_id, "Dark Mode Companion Board", 0.0, 3264.0, GALLERY_W, 1140.0, fill_hex=dark_t["backdrop"], stroke_hex=dark_t["boardStroke"], stroke_w=1.5, clip=True)
    dhdr = b.group(dark_board, "Dark Header", 0.0, 0.0, GALLERY_W, 130.0)
    b.text(dhdr, "DK Kicker", GALLERY_PAD_X, 26.0, 920.0, 18.0, "ROTGUARD  //  DARK MODE COMPANION SCREENS", FONT_MONO, "Bold", 11.5, 16.0, dark_t["inkMuted"], letter_spacing_pct=4.0)
    b.text(dhdr, "DK Title", GALLERY_PAD_X, 48.0, 1080.0, 40.0, "Dark Mode: Home, Blocker Opening, Mid-Hold & Inline Celebration", FONT_SERIF, "Bold", 28.0, 36.0, dark_t["ink"])
    b.text(dhdr, "DK Sub", GALLERY_PAD_X, 92.0, 1180.0, 20.0, "Dark theme across Home, Blocker Opening Photo, Mid-Hold Second Prompt, and Inline Close Celebration.", FONT_SANS, "Regular", 14.0, 20.0, dark_t["inkMuted"])
    b.rule(dhdr, "DK Rule", GALLERY_PAD_X, 124.0, GALLERY_W - 2 * GALLERY_PAD_X, dark_t["rule"], thickness=1.0)

    dark_y = 148.0
    d_caps = [
        "05 DARK  ·  HOME DASHBOARD",
        "09 DARK  ·  BLOCKER OPENING PHOTO",
        "10 DARK  ·  BLOCKER MID-HOLD PROMPT",
        "11 DARK  ·  CLOSE CELEBRATION (INLINE)",
    ]
    for idx, dcap in enumerate(d_caps):
        b.text(dark_board, f"Dark Cap {idx + 1}", GALLERY_PAD_X + idx * (SCREEN_W + SCREEN_GAP_X), dark_y + 936.0, SCREEN_W, 22.0, dcap, FONT_MONO, "Bold", 11.0, 18.0, dark_t["inkMuted"], align_h="center")

    d5, d5_cta = build_home_screen(b, dark_board, 0, dark_y, "dark")
    d9, d9_close = build_blocker_opening_screen(b, dark_board, 1, dark_y, "dark")
    d10, d10_close = build_blocker_midhold_screen(b, dark_board, 2, dark_y, "dark")
    d11, _ = build_inline_celebration_screen(b, dark_board, 3, dark_y, "dark")

    # Flows linking Close YouTube straight into the Inline Celebration!
    link_flow(b, cta1, s2)
    link_flow(b, cta2, s3)
    link_flow(b, cta3, s4)
    link_flow(b, test_cta, s9, transition="dissolve", duration_ms=280)
    link_flow(b, preview_insp_cta, s9, transition="dissolve", duration_ms=260)
    link_flow(b, close_p1, s11, transition="dissolve", duration_ms=320)
    link_flow(b, close_p2, s11, transition="dissolve", duration_ms=320)
    link_flow(b, replay_cta, s1)
    link_flow(b, d5_cta, d9, transition="dissolve", duration_ms=280)
    link_flow(b, d9_close, d11, transition="dissolve", duration_ms=320)
    link_flow(b, d10_close, d11, transition="dissolve", duration_ms=320)

    b.nodes[str(b.page_id)]["flows"] = [
        {"name": "End-to-End RotGuard Flow", "start": s1, "description": "Welcome -> Simulator -> Goals -> Inspiration & Pledge -> Home -> Blocker -> Inline Celebration"},
    ]

    b.add_comment(s8, 206, 240, "Inspiration Manager", "Renamed to plain 'Inspiration' for managing reminder photos without surfacing internal deck rules.")
    b.add_comment(s9, 206, 280, "Opening Blocker", "Displays one of the user's Inspiration photos alongside one of 20 rotating prompts.")
    b.add_comment(s10, 206, 420, "Mid-Hold Transition", "Prompt silently updates while holding before timer ends—no technical rule leaks shown.")
    b.add_comment(s11, 206, 270, "Inline Close Celebration", "Full-screen radial aurora bloom + rising embers celebrates when user chooses Close YouTube.")

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
                    "name": "RotGuard Theme",
                    "modes": [
                        {"id": "VariableCollectionId:1/light", "name": "Light"},
                        {"id": "VariableCollectionId:1/dark", "name": "Dark"},
                    ],
                }
            ],
            "variables": [
                {
                    "id": f"VariableID:{i + 1}",
                    "name": vname,
                    "resolvedType": "color",
                    "collection": "VariableCollectionId:1",
                    "values": {
                        "VariableCollectionId:1/light": {"type": "color", "value": hex_to_rgba(t[key])},
                        "VariableCollectionId:1/dark": {"type": "color", "value": hex_to_rgba(dark_t[key])},
                    },
                    "description": vdesc,
                }
                for i, (vname, key, vdesc) in enumerate(
                    [
                        ("surface/canvas", "canvas", "Primary background"),
                        ("surface/card", "card", "Card surface"),
                        ("accent/primary", "mossAccent", "Primary botanical green accent"),
                        ("shield/background", "canopy", "Blocker overlay background"),
                        ("accent/gold", "sunGold", "Hold ring & celebration gold"),
                        ("accent/alert", "terracotta", "Alert & stat accent"),
                    ]
                )
            ],
        },
        "current_page": b.page_id,
        "document_colors": [hex_to_rgba(t["canvas"]), hex_to_rgba(t["mossAccent"]), hex_to_rgba(t["canopy"]), hex_to_rgba(t["sunGold"]), hex_to_rgba(t["terracotta"]), hex_to_rgba(dark_t["canvas"]), hex_to_rgba(dark_t["mossAccent"])],
        "annotation_categories": [{"id": "rotguard-design", "name": "RotGuard Spec", "color": hex_to_rgba(t["mossAccent"])}],
        "comments": b.comments,
    }
    return doc, master, row1, row2, row3, dark_board, b.images


def main() -> None:
    parser = argparse.ArgumentParser(description="Build & render complete 16-screen RotGuard FigCraft archive")
    parser.add_argument("--roundtrip", action="store_true")
    parser.add_argument("--port", type=int, default=47987)
    args = parser.parse_args()

    doc, master_id, r1_id, r2_id, r3_id, dark_id, images = build_complete_document()
    write_figcraft_zip(doc, images, OUTPUT_FIGCRAFT)
    print(f"Built {OUTPUT_FIGCRAFT.name} ({OUTPUT_FIGCRAFT.stat().st_size} bytes, {len(doc['nodes'])} nodes, {len(doc['comments'])} comments)")

    if args.roundtrip:
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
            sock = socket.create_connection(("127.0.0.1", args.port), timeout=60)
            sock.sendall((json.dumps({"id": req_id, "method": method, "params": params}) + "\n").encode("utf-8"))
            buf = b""
            while b"\n" not in buf:
                chunk = sock.recv(65536)
                if not chunk:
                    break
                buf += chunk
            sock.close()
            return json.loads(buf.decode("utf-8"))

        renders = [
            (master_id, PREVIEWS_DIR / "vf_complete_master_12.png", 0.8),
            (r1_id, PREVIEWS_DIR / "vf_row1_onboarding.png", 1.25),
            (r2_id, PREVIEWS_DIR / "vf_row2_sanctuary_celebration.png", 1.25),
            (r3_id, PREVIEWS_DIR / "vf_row3_blocker_vault.png", 1.25),
            (dark_id, PREVIEWS_DIR / "vf_row4_nocturnal_dark.png", 1.25),
        ]
        try:
            r_open = rpc("engine.execute", {"command": "file.open", "params": {"path": str(OUTPUT_FIGCRAFT)}}, 1)
            assert r_open.get("ok"), f"file.open failed: {r_open}"
            r_fonts = rpc("engine.execute", {"command": "text.missingFonts", "params": {}}, 2)
            assert r_fonts.get("ok"), f"text.missingFonts failed: {r_fonts}"
            missing = r_fonts["result"].get("families", [])
            assert not missing, f"Missing fonts: {missing}"

            r_save = rpc("engine.execute", {"command": "file.saveAs", "params": {"path": str(OUTPUT_FIGCRAFT)}}, 3)
            assert r_save.get("ok"), f"file.saveAs failed: {r_save}"

            for idx, (nid, png_path, scale) in enumerate(renders, start=10):
                r_ren = rpc("ui.render", {"ids": [nid], "path": str(png_path), "scale": scale}, idx)
                assert r_ren.get("ok"), f"ui.render failed for {png_path.name}: {r_ren}"
                print(f"[FigCraft OK] {png_path.name} ({png_path.stat().st_size} bytes)")
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
