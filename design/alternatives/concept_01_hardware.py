"""Option 1: OP-Focus — Tactile Hardware Detent (Teenage Engineering / Braun / Nothing OS)."""

from __future__ import annotations

import math
from pathlib import Path
from typing import Any

from fc_core import (
    ALT_DIR,
    SCREEN_H,
    SCREEN_TOP_Y,
    SCREEN_W,
    DocBuilder,
    drop_shadow,
    link_flow,
    screen_x,
    setup_gallery_chrome,
)

C = {
    "backdrop": "#DBDCD6",
    "aluminum": "#ECECE7",
    "surface": "#F4F4F0",
    "bay": "#E0E0D9",
    "navBg": "#E3E3DC",
    "oled": "#141517",
    "oledHi": "#1E2024",
    "oledBorder": "#2D3038",
    "ink": "#141413",
    "muted": "#66665F",
    "hairline": "#C5C5BD",
    "orange": "#FF4F00",
    "orangeLip": "#B83900",
    "amber": "#FFB800",
    "green": "#00B359",
    "greenSoft": "#13291D",
    "red": "#E63229",
    "white": "#FFFFFF",
    "oledText": "#F5F5F0",
    "oledMuted": "#8C919B",
}

FONT_SANS = "Inter"
FONT_MONO = "Liberation Mono"

OUTPUT_FIGCRAFT = ALT_DIR / "01_hardware_detent.figcraft"


def _screen(b: DocBuilder, slot: int, name: str, fill_hex: str, desc: str) -> int:
    return b.frame(
        b.page_id,
        name,
        screen_x(slot),
        SCREEN_TOP_Y,
        SCREEN_W,
        SCREEN_H,
        fill_hex=fill_hex,
        stroke_hex="#A9A9A1" if fill_hex == C["aluminum"] else "#2A2D34",
        stroke_w=2.0,
        radius=36.0,
        clip=True,
        effects=drop_shadow("#000000", 0.22, 0.0, 18.0, 36.0),
        annotations=[{"label": desc, "properties": ["fill", "stroke", "cornerRadius"], "category": "aesthetic-system"}],
    )


def _status_bar(b: DocBuilder, s: int, dark: bool = False) -> None:
    ink = C["oledText"] if dark else C["ink"]
    muted = C["oledMuted"] if dark else C["muted"]
    led = C["orange"] if dark else C["green"]
    b.text(s, "Status Time", 24, 15, 64, 16, "09:41", FONT_MONO, "Bold", 11.5, 16.0, ink, tnum=True)
    b.ellipse(s, "Status LED", 252, 19, 7, 7, fill_hex=led)
    b.text(s, "Status Spec", 264, 16, 124, 14, "SHIELD:ARMED  94%", FONT_MONO, "Bold", 9.5, 14.0, muted, align_h="right", tnum=True)
    b.rule(s, "Top Hairline", 20, 40, 372, C["oledBorder"] if dark else C["hairline"])


def _screws(b: DocBuilder, parent: int, x: float, y: float, w: float, h: float, col: str) -> None:
    for sx, sy in [(x + 8, y + 8), (x + w - 12, y + 8), (x + 8, y + h - 12), (x + w - 12, y + h - 12)]:
        b.ellipse(parent, "Corner Rivet", sx, sy, 4, 4, fill_hex=col)


def _nav_bar(b: DocBuilder, s: int, active_idx: int) -> None:
    b.rect(s, "Nav Deck", 0, 826, SCREEN_W, 89, fill_hex=C["navBg"])
    b.rule(s, "Nav Top Rule", 0, 826, SCREEN_W, C["hairline"])
    items = ["01 HOME", "02 APPS", "03 DATA", "04 SYS"]
    col_w = SCREEN_W / 4.0
    for idx, label in enumerate(items):
        cx = idx * col_w
        active = idx == active_idx
        if active:
            b.rect(s, "Nav Active Indicator", cx + 22, 826, col_w - 44, 3.5, fill_hex=C["orange"])
        b.text(
            s,
            f"Nav {label}",
            cx,
            848,
            col_w,
            18,
            label,
            FONT_MONO,
            "Bold",
            11.0,
            16.0,
            C["ink"] if active else C["muted"],
            align_h="center",
        )


def _hardware_button(
    b: DocBuilder,
    s: int,
    name: str,
    x: float,
    y: float,
    w: float,
    h: float,
    label: str,
    bg_hex: str,
    lip_hex: str,
    ink_hex: str,
    font_family: str = FONT_SANS,
) -> int:
    b.rect(s, f"{name} Lip", x, y + 4, w, h, fill_hex=lip_hex, radius=10.0)
    cap = b.rect(s, f"{name} Cap", x, y, w, h - 2, fill_hex=bg_hex, radius=10.0)
    b.text(s, f"{name} Txt", x, y + (h - 2 - 20) / 2, w, 20, label, font_family, "Bold", 14.0, 20.0, ink_hex, align_h="center")
    return cap


def build_concept_01() -> tuple[dict[str, Any], list[int], dict[str, tuple[bytes, int, int]]]:
    b = DocBuilder("Option 01 — OP-Focus (Hardware Detent)", C["backdrop"])
    setup_gallery_chrome(
        b,
        "CONCEPT 01  //  TACTILE INDUSTRIAL INSTRUMENTATION",
        "OP-Focus: Hardware Detent",
        "Anodized aluminum chassis + high-contrast OLED telemetry bays. Replaces soft wellness clichés with machined rotary detents and physical interlocks.",
        "INTER + LIBERATION MONO  //  LIGHT STUDIO + DARK INTERLOCK",
        C["backdrop"],
        C["ink"],
        C["muted"],
        "#BEBEB5",
        [(C["aluminum"], "#ECECE7"), (C["oled"], "#141517"), (C["orange"], "#FF4F00"), (C["green"], "#00B359")],
        [
            "01  ONBOARDING · LOAD CALIBRATION",
            "02  HOME · CLARITY INSTRUMENT",
            "03  YOUTUBE GATE · ROTARY INTERLOCK",
            "04  APPS & 7-DAY LED TELEMETRY",
        ],
        header_font=FONT_SANS,
        body_font=FONT_SANS,
        mono_font=FONT_MONO,
    )

    # =========================================================================
    # SCREEN 1: ONBOARDING — DOPAMINE LOAD CALIBRATION
    # =========================================================================
    s1 = _screen(b, 0, "01 Onboarding — Load Calibration", C["aluminum"], "Stepped hardware VU analyzer pairing dopamine load with haptic detent profiles.")
    _status_bar(b, s1)
    b.text(s1, "S1 Unit", 24, 54, 240, 16, "UNIT 01 // ATTENTION DRAG", FONT_MONO, "Bold", 10.5, 16.0, C["muted"])
    b.rect(s1, "S1 Step Pill", 316, 52, 72, 20, fill_hex=C["oled"], radius=4.0)
    b.text(s1, "S1 Step Txt", 316, 55, 72, 14, "STEP 02", FONT_MONO, "Bold", 10.0, 14.0, C["orange"], align_h="center")
    b.text(s1, "S1 Title", 24, 78, 364, 34, "Calibrate attention drag.", FONT_SANS, "Bold", 25.0, 32.0, C["ink"])

    # OLED Spectrum Analyzer Bay
    b.rect(s1, "S1 OLED", 20, 124, 372, 286, fill_hex=C["oled"], stroke_hex=C["oledBorder"], radius=14.0)
    _screws(b, s1, 20, 124, 372, 286, "#343740")
    b.ellipse(s1, "OLED Rec Dot", 38, 143, 8, 8, fill_hex=C["orange"])
    b.text(s1, "OLED Header L", 52, 139, 170, 16, "SIGNAL STRESS MONITOR", FONT_MONO, "Bold", 10.0, 16.0, C["oledMuted"])
    b.text(s1, "OLED Header R", 242, 139, 130, 16, "STAGE 04 · CLIPPING", FONT_MONO, "Bold", 10.0, 16.0, C["orange"], align_h="right")
    b.rule(s1, "OLED Grid Line", 38, 164, 336, C["oledBorder"])

    # 22-bar VU frequency meter
    levels = [32, 42, 48, 40, 56, 62, 52, 68, 74, 60, 78, 86, 72, 88, 92, 80, 94, 96, 84, 98, 92, 88]
    for idx, pct in enumerate(levels):
        bx = 38.0 + idx * 15.3
        col = C["green"] if idx < 8 else (C["amber"] if idx < 15 else (C["orange"] if idx < 19 else C["red"]))
        bh = round(84.0 * (pct / 100.0), 1)
        by = 264.0 - bh
        b.rect(s1, f"VU Track {idx}", bx, 180, 10, 84, fill_hex="#202329", radius=2.0)
        b.rect(s1, f"VU Fill {idx}", bx, by, 10, bh, fill_hex=col, radius=2.0)

    b.rect(s1, "OLED Baseline", 38, 276, 336, 1.0, fill_hex=C["oledBorder"], dashes=[4.0, 4.0])

    # 3 metrics inside OLED
    metrics_s1 = [("LIFE TAX", "9.4 YRS", C["orange"]), ("DAILY LOAD", "150+ / d", C["oledText"]), ("HAPTIC", "STUTTER", C["amber"])]
    for idx, (mlbl, mval, mcol) in enumerate(metrics_s1):
        mx = 38.0 + idx * 116.0
        b.text(s1, f"M Lbl {idx}", mx, 290, 104, 14, mlbl, FONT_MONO, "Bold", 9.5, 14.0, C["oledMuted"])
        b.text(s1, f"M Val {idx}", mx, 308, 108, 24, mval, FONT_MONO, "Bold", 19.0, 24.0, mcol, tnum=True)

    b.rect(s1, "OLED Note Bay", 36, 348, 340, 44, fill_hex=C["oledHi"], stroke_hex=C["oledBorder"], radius=6.0)
    b.text(s1, "OLED Note Txt", 48, 355, 316, 30, "Rapid feed switching depresses baseline focus for 45+ mins. Tap keys below to feel detent.", FONT_SANS, "Regular", 11.5, 15.0, C["oledMuted"])

    # Daily Reel Input Keycaps
    b.rect(s1, "Input Bay", 20, 426, 372, 104, fill_hex=C["bay"], stroke_hex=C["hairline"], radius=12.0)
    b.text(s1, "Input Kicker", 34, 440, 344, 14, "DAILY SHORT-VIDEO INTAKE (REELS / SHORTS)", FONT_MONO, "Bold", 10.0, 14.0, C["muted"])
    for idx, val in enumerate(["0", "50", "100", "150", "200+"]):
        kx = 34.0 + idx * 70.0
        sel = val == "150"
        b.rect(s1, f"Key Lip {val}", kx, 469, 62, 42, fill_hex=C["orangeLip"] if sel else "#BEBEB6", radius=8.0)
        b.rect(s1, f"Key {val}", kx, 466, 62, 40, fill_hex=C["orange"] if sel else C["surface"], stroke_hex=None if sel else C["hairline"], radius=8.0)
        b.text(s1, f"Key T {val}", kx, 478, 62, 18, val, FONT_MONO, "Bold", 13.0, 18.0, C["white"] if sel else C["ink"], align_h="center", tnum=True)

    # Protected Goals 2x2 Key Matrix
    b.rect(s1, "Goals Bay", 20, 546, 372, 212, fill_hex=C["surface"], stroke_hex=C["hairline"], radius=12.0)
    b.text(s1, "Goals Kicker", 34, 562, 344, 14, "SELECT ANCHOR DIRECTIVES", FONT_MONO, "Bold", 10.0, 14.0, C["muted"])
    goals = [("DEEP WORK", True), ("SLEEP < 12AM", True), ("BUILD & SHIP", False), ("REAL PRESENCE", False)]
    for idx, (gname, active) in enumerate(goals):
        col_i = idx % 2
        row_i = idx // 2
        gx = 34.0 + col_i * 176.0
        gy = 590.0 + row_i * 76.0
        b.rect(s1, f"Goal Key {idx}", gx, gy, 168, 64, fill_hex=C["oled"] if active else C["bay"], stroke_hex=None if active else C["hairline"], radius=10.0)
        b.ellipse(s1, f"Goal Dot {idx}", gx + 14, gy + 15, 8, 8, fill_hex=C["orange"] if active else "#A8A8A0")
        b.text(s1, f"Goal State {idx}", gx + 28, gy + 12, 126, 14, "LOCKED" if active else "IDLE", FONT_MONO, "Bold", 9.5, 14.0, C["orange"] if active else C["muted"])
        b.text(s1, f"Goal Name {idx}", gx + 14, gy + 34, 140, 18, gname, FONT_MONO, "Bold", 12.5, 18.0, C["white"] if active else C["ink"])

    cta1 = _hardware_button(b, s1, "S1 CTA", 20, 796, 372, 62, "ENGAGE INTERLOCK SHIELD  ->", C["orange"], C["orangeLip"], C["white"], FONT_MONO)
    b.text(s1, "S1 Foot", 20, 868, 372, 16, "STEP 02 OF 07  //  TACTILE HAPTICS ACTIVE", FONT_MONO, "Regular", 10.0, 16.0, C["muted"], align_h="center")

    # =========================================================================
    # SCREEN 2: HOME — CLARITY INSTRUMENT
    # =========================================================================
    s2 = _screen(b, 1, "02 Home — Clarity Instrument", C["aluminum"], "High-contrast dark OLED clarity gauge atop matte aluminum telemetry slots.")
    _status_bar(b, s2)
    b.text(s2, "Brand Lbl", 24, 54, 210, 24, "ROTGUARD // MK-II", FONT_SANS, "Bold", 19.0, 24.0, C["ink"])
    b.rect(s2, "Armed Badge", 272, 54, 116, 26, fill_hex=C["oled"], radius=6.0)
    b.ellipse(s2, "Armed LED", 283, 63, 8, 8, fill_hex=C["green"])
    b.text(s2, "Armed Txt", 296, 59, 84, 16, "ARMED", FONT_MONO, "Bold", 10.5, 16.0, C["green"], align_h="center")

    # Hero OLED Module
    b.rect(s2, "S2 OLED", 20, 96, 372, 232, fill_hex=C["oled"], stroke_hex=C["oledBorder"], radius=16.0)
    _screws(b, s2, 20, 96, 372, 232, "#343740")
    b.text(s2, "Clarity Lbl", 38, 114, 180, 14, "BRAIN CLARITY INDEX", FONT_MONO, "Bold", 10.0, 14.0, C["oledMuted"])
    b.text(s2, "Clarity Stage", 232, 114, 140, 14, "STAGE 01 · OPTIMAL", FONT_MONO, "Bold", 10.0, 14.0, C["green"], align_h="right")
    b.text(s2, "Clarity Num", 38, 146, 190, 70, "88%", FONT_SANS, "Bold", 60.0, 68.0, C["oledText"], tnum=True)
    b.text(s2, "Clarity Sub", 40, 220, 200, 16, "+14% vs 7-day rolling avg", FONT_MONO, "Regular", 10.5, 16.0, C["oledMuted"])

    # Right rotary gauge inside OLED
    b.ellipse(s2, "Mini Ring BG", 272, 144, 96, 96, fill_hex="#23262C", inner=0.76)
    b.ellipse(s2, "Mini Ring FG", 272, 144, 96, 96, fill_hex=C["green"], inner=0.76, sweep_ratio=0.88)
    b.text(s2, "Mini Ring Txt", 272, 183, 96, 18, "88 / 100", FONT_MONO, "Bold", 11.5, 18.0, C["oledText"], align_h="center", tnum=True)

    # Segmented LED meter inside OLED
    for idx in range(22):
        lx = 38.0 + idx * 15.3
        on = idx < 19
        b.rect(s2, f"Clarity Seg {idx}", lx, 260, 10, 14, fill_hex=C["green"] if on else "#252830", radius=2.0)
    b.text(s2, "OLED Foot", 38, 290, 336, 14, "OVERLAY SERVICE ACTIVE  //  ACCESSIBILITY OK", FONT_MONO, "Bold", 9.5, 14.0, C["oledMuted"])

    # 3 Recessed Telemetry Bays
    stats_s2 = [("07", "URGES STOPPED"), ("42m", "TIME SAVED"), ("04", "APPS LOCKED")]
    for idx, (val, lbl) in enumerate(stats_s2):
        sx = 20.0 + idx * 128.0
        b.rect(s2, f"Bay {idx}", sx, 346, 116, 92, fill_hex=C["bay"], stroke_hex=C["hairline"], radius=10.0)
        b.text(s2, f"Bay Val {idx}", sx + 12, 362, 92, 30, val, FONT_MONO, "Bold", 26.0, 30.0, C["ink"], tnum=True)
        b.rule(s2, f"Bay Rule {idx}", sx + 12, 400, 92, C["hairline"])
        b.text(s2, f"Bay Lbl {idx}", sx + 12, 410, 92, 14, lbl, FONT_MONO, "Bold", 9.0, 14.0, C["muted"])

    # Mantra Plate
    b.rect(s2, "Mantra Plate", 20, 456, 372, 110, fill_hex=C["surface"], stroke_hex=C["hairline"], radius=12.0)
    _screws(b, s2, 20, 456, 372, 110, "#B8B8AF")
    b.text(s2, "Mantra Tag", 40, 476, 332, 14, "ANCHOR_DIRECTIVE // ACTIVE", FONT_MONO, "Bold", 10.0, 14.0, C["orange"])
    b.text(s2, "Mantra Copy", 40, 500, 332, 44, "\"Build things that outlast the feed.\"", FONT_SANS, "Semi Bold", 17.0, 22.0, C["ink"])

    # Guarded Strip
    b.rect(s2, "Watch Strip", 20, 584, 372, 82, fill_hex=C["bay"], stroke_hex=C["hairline"], radius=10.0)
    b.text(s2, "Watch Lbl", 34, 598, 344, 14, "INTERCEPT QUEUE  //  PRESS-HOLD LOCK", FONT_MONO, "Bold", 10.0, 14.0, C["muted"])
    for idx, tag in enumerate(["YT · 4s", "IG · 4s", "TT · 6s", "RD · 4s"]):
        tx = 34.0 + idx * 87.0
        b.rect(s2, f"Tag {idx}", tx, 622, 80, 28, fill_hex=C["surface"], stroke_hex=C["hairline"], radius=6.0)
        b.text(s2, f"Tag T {idx}", tx, 628, 80, 16, tag, FONT_MONO, "Bold", 10.5, 16.0, C["ink"], align_h="center", tnum=True)

    test_cta = _hardware_button(b, s2, "Test Shield CTA", 20, 690, 372, 62, "TRIGGER TEST INTERLOCK  ->", C["oled"], "#040405", C["white"], FONT_MONO)
    b.text(s2, "Test Note", 20, 764, 372, 16, "Previews exact Android Accessibility overlay", FONT_SANS, "Regular", 11.5, 16.0, C["muted"], align_h="center")
    _nav_bar(b, s2, 0)

    # =========================================================================
    # SCREEN 3: LIVE YOUTUBE FRICTION GATE — DARK ANODIZED ROTARY INTERLOCK
    # =========================================================================
    s3 = _screen(b, 2, "03 Live Friction — Rotary Interlock", "#121316", "Full-screen hardware safety interlock with machined 24-tick rotary press-and-hold encoder.")
    _status_bar(b, s3, dark=True)
    b.rect(s3, "Gate Banner", 20, 56, 372, 34, fill_hex="#26130F", stroke_hex=C["orange"], radius=6.0)
    b.text(s3, "Gate Banner Txt", 20, 64, 372, 18, "! INTERLOCK TRIPPED  //  YOUTUBE  //  #04", FONT_MONO, "Bold", 10.5, 18.0, C["orange"], align_h="center", tnum=True)
    b.text(s3, "Gate Headline", 24, 108, 364, 56, "Step back before the spiral.", FONT_SANS, "Bold", 24.0, 28.0, C["oledText"], align_h="center")

    # Machined Rotary Detent Dial (center 206, 304)
    cx, cy = 206.0, 298.0
    b.ellipse(s3, "Dial Halo", cx - 128, cy - 128, 256, 256, stroke_hex="#2A2E37", stroke_w=1.5, dashes=[4.0, 6.0])
    b.ellipse(s3, "Dial Housing", cx - 108, cy - 108, 216, 216, fill_hex="#1B1D22", stroke_hex="#31353F", stroke_w=2.0)
    # 24 perimeter LED detent pips
    for idx in range(24):
        ang = -math.pi / 2.0 + (2.0 * math.pi * idx / 24.0)
        px = cx + 116.0 * math.cos(ang) - 4.0
        py = cy + 116.0 * math.sin(ang) - 4.0
        lit = idx < 11
        b.ellipse(s3, f"Dial Pip {idx}", px, py, 8, 8, fill_hex=C["orange"] if lit else "#292D36")

    b.ellipse(s3, "Dial Arc BG", cx - 90, cy - 90, 180, 180, fill_hex="#23262E", inner=0.84)
    b.ellipse(s3, "Dial Arc FG", cx - 90, cy - 90, 180, 180, fill_hex=C["orange"], inner=0.84, sweep_ratio=0.48)
    b.ellipse(s3, "Dial Core", cx - 68, cy - 68, 136, 136, fill_hex="#131519", stroke_hex="#2B2E36")
    b.text(s3, "Dial Tag", cx - 64, cy - 42, 128, 14, "HOLD TO UNLOCK", FONT_MONO, "Bold", 9.5, 14.0, C["oledMuted"], align_h="center")
    b.text(s3, "Dial Time", cx - 64, cy - 16, 128, 34, "1.9s", FONT_SANS, "Bold", 32.0, 34.0, C["oledText"], align_h="center", tnum=True)
    b.text(s3, "Dial Sub", cx - 64, cy + 24, 128, 14, "OF 4.0s DETENT", FONT_MONO, "Bold", 9.5, 14.0, C["orange"], align_h="center")

    # Mantra module
    b.rect(s3, "Gate Mantra", 20, 466, 372, 92, fill_hex="#1B1D22", stroke_hex="#2E323B", radius=10.0)
    b.text(s3, "Gate Mantra Kicker", 36, 480, 340, 14, "YOUR DECLARED PRIORITY", FONT_MONO, "Bold", 10.0, 14.0, C["orange"], align_h="center")
    b.text(s3, "Gate Mantra Body", 36, 504, 340, 42, "\"Protect your sleep and deep work tonight.\"", FONT_SANS, "Semi Bold", 15.5, 21.0, C["oledText"], align_h="center")

    # Primary Immediate Abort CTA
    abort_cta = _hardware_button(b, s3, "Abort CTA", 20, 586, 372, 66, "[X] ABORT & CLOSE YOUTUBE", C["orange"], C["orangeLip"], C["white"], FONT_MONO)

    b.text(s3, "Gate Or", 20, 672, 372, 14, "OR MAINTAIN PRESSURE FOR MANUAL OVERRIDE", FONT_MONO, "Bold", 9.5, 14.0, C["oledMuted"], align_h="center")

    # Linear synchronized press-hold actuator track
    b.rect(s3, "Hold Bar Track", 20, 700, 372, 64, fill_hex="#1B1D22", stroke_hex="#3A3F4B", stroke_w=1.5, radius=10.0)
    b.rect(s3, "Hold Bar Fill", 22, 702, 178, 60, fill_hex=C["orange"], fill_alpha=0.25, radius=8.0)
    b.rect(s3, "Hold Bar Edge", 198, 702, 3, 60, fill_hex=C["orange"])
    b.text(s3, "Hold Bar Txt", 20, 722, 372, 20, "HOLDING BYPASS · 48% (1.9s / 4.0s)", FONT_MONO, "Bold", 12.0, 20.0, C["oledText"], align_h="center", tnum=True)

    b.text(s3, "Gate Foot", 28, 784, 356, 32, "Releasing resets the mechanical detent so autopilot taps cannot unlock YouTube.", FONT_SANS, "Regular", 12.0, 17.0, C["oledMuted"], align_h="center")

    # =========================================================================
    # SCREEN 4: GUARDED APPS & 7-DAY LED TELEMETRY
    # =========================================================================
    s4 = _screen(b, 3, "04 Apps & Telemetry Matrix", C["aluminum"], "Rocker switches, hardware hold presets, and 7-day OLED LED clarity matrix.")
    _status_bar(b, s4)
    b.text(s4, "S4 Title", 24, 52, 240, 28, "Module Control", FONT_SANS, "Bold", 23.0, 28.0, C["ink"])
    b.rect(s4, "S4 Count Pill", 308, 55, 80, 22, fill_hex=C["bay"], stroke_hex=C["hairline"], radius=6.0)
    b.text(s4, "S4 Count Txt", 308, 59, 80, 14, "04 ARMED", FONT_MONO, "Bold", 10.0, 14.0, C["green"], align_h="center")

    # Preset Rail
    b.rect(s4, "Preset Bay", 20, 96, 372, 72, fill_hex=C["bay"], stroke_hex=C["hairline"], radius=10.0)
    b.text(s4, "Preset Kicker", 32, 106, 348, 14, "GLOBAL DETENT DURATION", FONT_MONO, "Bold", 9.5, 14.0, C["muted"])
    for idx, dur in enumerate(["3.0s", "4.0s", "6.0s", "10.0s"]):
        px = 32.0 + idx * 88.0
        sel = dur == "4.0s"
        b.rect(s4, f"Preset Btn {dur}", px, 126, 82, 32, fill_hex=C["oled"] if sel else C["surface"], stroke_hex=None if sel else C["hairline"], radius=6.0)
        b.text(s4, f"Preset Txt {dur}", px, 134, 82, 16, dur, FONT_MONO, "Bold", 11.5, 16.0, C["orange"] if sel else C["ink"], align_h="center", tnum=True)

    # Physical Rocker Switch List
    apps = [
        ("YouTube", "com.google.android.youtube  //  4.0s", True),
        ("Instagram", "com.instagram.android  //  4.0s", True),
        ("TikTok", "com.zhiliaoapp.musically  //  6.0s", True),
        ("Chrome", "com.android.chrome  //  OPEN", False),
    ]
    for idx, (aname, pkg, on) in enumerate(apps):
        ry = 184.0 + idx * 72.0
        b.rect(s4, f"App Row {idx}", 20, ry, 372, 62, fill_hex=C["surface"], stroke_hex=C["hairline"], radius=10.0)
        b.ellipse(s4, f"App LED {idx}", 34, ry + 26, 9, 9, fill_hex=C["green"] if on else "#B8B8B0")
        b.text(s4, f"App Name {idx}", 54, ry + 13, 210, 20, aname, FONT_SANS, "Bold", 15.0, 20.0, C["ink"])
        b.text(s4, f"App Pkg {idx}", 54, ry + 34, 240, 14, pkg, FONT_MONO, "Regular", 9.5, 14.0, C["muted"])
        # Rectangular rocker switch
        b.rect(s4, f"Rocker {idx}", 314, ry + 17, 62, 28, fill_hex=C["oled"] if on else C["bay"], stroke_hex=None if on else C["hairline"], radius=6.0)
        b.rect(s4, f"Rocker Tab {idx}", 344 if on else 317, ry + 20, 29, 22, fill_hex=C["green"] if on else C["surface"], radius=4.0)

    # 7-Day OLED Dot-Matrix LED Chart + Duel
    b.rect(s4, "LED Chart OLED", 20, 488, 372, 314, fill_hex=C["oled"], stroke_hex=C["oledBorder"], radius=14.0)
    _screws(b, s4, 20, 488, 372, 314, "#343740")
    b.text(s4, "Chart Lbl", 38, 504, 200, 14, "7-DAY CLARITY MATRIX", FONT_MONO, "Bold", 10.0, 14.0, C["oledMuted"])
    b.text(s4, "Duel Tag", 220, 504, 152, 14, "DUEL: YOU 02 vs 14", FONT_MONO, "Bold", 10.0, 14.0, C["green"], align_h="right")

    day_rows = [("MON", 5), ("TUE", 6), ("WED", 6), ("THU", 7), ("FRI", 7), ("SAT", 8), ("SUN", 7)]
    for col_i, (dlbl, lit_cnt) in enumerate(day_rows):
        cx_bar = 44.0 + col_i * 48.0
        for row_i in range(8):
            cell_y = 664.0 - row_i * 19.0
            is_lit = row_i < lit_cnt
            cell_col = (C["orange"] if row_i < 4 else C["green"]) if is_lit else "#22252C"
            b.rect(s4, f"LED {col_i}-{row_i}", cx_bar, cell_y, 32, 13, fill_hex=cell_col, radius=2.5)
        b.text(s4, f"LED Day {col_i}", cx_bar, 688, 32, 14, dlbl, FONT_MONO, "Bold", 9.5, 14.0, C["oledMuted"], align_h="center")

    b.rule(s4, "Chart Foot Rule", 38, 718, 336, C["oledBorder"])
    b.text(s4, "Chart Sum L", 38, 734, 160, 14, "WEEKLY RECLAIMED", FONT_MONO, "Bold", 9.5, 14.0, C["oledMuted"])
    b.text(s4, "Chart Sum LV", 38, 754, 160, 24, "4h 54m", FONT_MONO, "Bold", 20.0, 24.0, C["oledText"], tnum=True)
    b.text(s4, "Chart Sum R", 212, 734, 160, 14, "STREAK STATUS", FONT_MONO, "Bold", 9.5, 14.0, C["oledMuted"], align_h="right")
    b.text(s4, "Chart Sum RV", 212, 754, 160, 24, "WINNING (-86%)", FONT_MONO, "Bold", 16.0, 24.0, C["green"], align_h="right")

    _nav_bar(b, s4, 1)

    # Interactive Flows
    link_flow(b, cta1, s2)
    link_flow(b, test_cta, s3, transition="dissolve", duration_ms=240)
    link_flow(b, abort_cta, s2)
    b.nodes[str(b.page_id)]["flows"] = [
        {"name": "Hardware Detent Tour", "start": s1, "description": "VU Calibration -> Clarity Instrument -> Dark Rotary Interlock -> Module Control"}
    ]

    b.add_comment(s1, 206, 230, "Industrial Design", "VU bar analyzer + mechanical keycaps reframe scrolling load as measurable electrical strain.")
    b.add_comment(s3, 206, 298, "Hardware UX", "Machined 24-pip rotary encoder contrasts sharply in dark steel over the light aluminum OS.")

    doc = b.finalize_document(
        variables_spec=[
            ("surface/aluminum", C["aluminum"], "Matte anodized aluminum chassis"),
            ("surface/oled", C["oled"], "High-contrast dark OLED bay"),
            ("accent/safetyOrange", C["orange"], "Teenage Engineering safety orange detent"),
            ("status/armed", C["green"], "Active shield armed LED"),
        ],
        collection_name="OP-Focus Tokens",
        mode_name="Studio Aluminum",
        doc_colors=[C["aluminum"], C["bay"], C["oled"], C["ink"], C["muted"], C["orange"], C["green"], C["red"]],
        annotations_cats=[("aesthetic-system", "Aesthetic System", C["orange"])],
    )
    return doc, [s1, s2, s3, s4], b.images
