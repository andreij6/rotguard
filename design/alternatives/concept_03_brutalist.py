"""Option 3: HARD STOP — Kinetic Swiss Neo-Brutalist (International Typographic Poster)."""

from __future__ import annotations

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
    "backdrop": "#E5E5DD",
    "paper": "#F8F8F5",
    "neutral": "#E5E5DC",
    "black": "#0C0C0C",
    "white": "#FFFFFF",
    "red": "#FF2E1F",
    "yellow": "#FFE600",
    "cobalt": "#1D4ED8",
    "cobaltSoft": "#DBEAFE",
    "muted": "#52524D",
}

FONT_POSTER = "Montserrat"
FONT_SANS = "Inter"
FONT_MONO = "Liberation Mono"

OUTPUT_FIGCRAFT = ALT_DIR / "03_swiss_brutalist.figcraft"


def _screen(b: DocBuilder, slot: int, name: str, fill_hex: str, desc: str) -> int:
    return b.frame(
        b.page_id,
        name,
        screen_x(slot),
        SCREEN_TOP_Y,
        SCREEN_W,
        SCREEN_H,
        fill_hex=fill_hex,
        stroke_hex=C["black"],
        stroke_w=3.0,
        radius=28.0,
        clip=True,
        effects=drop_shadow("#0C0C0C", 0.24, 6.0, 10.0, 18.0),
        annotations=[{"label": desc, "properties": ["fill", "stroke", "typography"], "category": "aesthetic-system"}],
    )


def _status_bar(b: DocBuilder, s: int, on_red: bool = False) -> None:
    fg = C["white"] if on_red else C["black"]
    b.text(s, "Status Time", 22, 14, 60, 16, "09:41", FONT_MONO, "Bold", 12.0, 16.0, fg, tnum=True)
    b.rect(s, "Status Ticker", 268, 12, 122, 20, fill_hex=C["black"] if on_red else C["yellow"], stroke_hex=C["black"], stroke_w=1.5, radius=3.0)
    b.text(s, "Status Spec", 268, 15, 122, 14, "SHIELD: ARMED", FONT_MONO, "Bold", 9.5, 14.0, C["yellow"] if on_red else C["black"], align_h="center")
    b.rule(s, "Top Poster Rule", 0, 42, SCREEN_W, C["black"], thickness=2.5)


def _brutalist_box(
    b: DocBuilder,
    s: int,
    name: str,
    x: float,
    y: float,
    w: float,
    h: float,
    fill_hex: str,
    shadow_hex: str = C["black"],
    offset: float = 5.0,
    border_w: float = 2.5,
    radius: float = 6.0,
) -> int:
    b.rect(s, f"{name} Shadow", x + offset, y + offset, w, h, fill_hex=shadow_hex, radius=radius)
    return b.rect(s, name, x, y, w, h, fill_hex=fill_hex, stroke_hex=C["black"], stroke_w=border_w, radius=radius)


def _nav_bar(b: DocBuilder, s: int, active_idx: int) -> None:
    b.rect(s, "Nav Strip", 0, 826, SCREEN_W, 89, fill_hex=C["paper"])
    b.rule(s, "Nav Top", 0, 826, SCREEN_W, C["black"], thickness=2.5)
    items = ["HOME", "APPS", "STATS", "RULES"]
    cw = SCREEN_W / 4.0
    for idx, lbl in enumerate(items):
        cx = idx * cw
        active = idx == active_idx
        if active:
            b.rect(s, "Nav Active", cx + 6, 834, cw - 12, 52, fill_hex=C["yellow"], stroke_hex=C["black"], stroke_w=2.0, radius=4.0)
        b.text(s, f"Nav {lbl}", cx, 851, cw, 18, lbl, FONT_MONO, "Bold", 11.5, 18.0, C["black"], align_h="center")


def build_concept_03() -> tuple[dict[str, Any], list[int], dict[str, tuple[bytes, int, int]]]:
    b = DocBuilder("Option 03 — HARD STOP (Swiss Neo-Brutalist)", C["backdrop"])
    setup_gallery_chrome(
        b,
        "CONCEPT 03  //  KINETIC SWISS NEO-BRUTALIST POSTER",
        "HARD STOP: Anti-Scroll Typography",
        "High-contrast poster typography, thick 2.5px ink rules, zero blur, and a full-bleed Signal Red intervention built to break scrolling muscle memory.",
        "MONTSERRAT + INTER + LIBERATION MONO  //  SWISS POSTER",
        C["backdrop"],
        C["black"],
        C["muted"],
        C["black"],
        [(C["paper"], "#F8F8F5"), (C["black"], "#0C0C0C"), (C["red"], "#FF2E1F"), (C["yellow"], "#FFE600")],
        [
            "01  ONBOARDING · SCROLL TAX POSTER",
            "02  HOME · SWISS BENTO GRID",
            "03  YOUTUBE GATE · RED SHOCK OVERLAY",
            "04  APPS · DUEL & HISTOGRAM",
        ],
        header_font=FONT_POSTER,
        body_font=FONT_SANS,
        mono_font=FONT_MONO,
    )

    # =========================================================================
    # SCREEN 1: ONBOARDING — SCROLL TAX POSTER
    # =========================================================================
    s1 = _screen(b, 0, "01 Onboarding — Scroll Tax Poster", C["paper"], "Stark Swiss typographic warning card + stacked waking-day proportion meter.")
    _status_bar(b, s1)
    b.rect(s1, "Step Stamp", 20, 56, 98, 22, fill_hex=C["black"], radius=3.0)
    b.text(s1, "Step Stamp T", 20, 59, 98, 16, "STEP 02 / 07", FONT_MONO, "Bold", 10.0, 16.0, C["yellow"], align_h="center")
    b.text(s1, "S1 Title", 20, 86, 372, 62, "STOP TRADING\nYOUR YEARS.", FONT_POSTER, "Bold", 28.0, 30.0, C["black"])

    # Giant Signal Red Hero Block
    _brutalist_box(b, s1, "Tax Block", 20, 160, 372, 206, C["red"], offset=5.0)
    b.text(s1, "Tax Kicker", 36, 176, 340, 16, "AT 3 HOURS PER DAY ON SHORTS & REELS:", FONT_MONO, "Bold", 10.5, 16.0, C["white"])
    b.text(s1, "Tax Giant", 36, 198, 340, 76, "9.4 YRS", FONT_POSTER, "Bold", 62.0, 70.0, C["yellow"], tnum=True)
    b.text(s1, "Tax Sub", 36, 276, 340, 20, "OF YOUR WAKING LIFE ERASED.", FONT_POSTER, "Bold", 15.0, 20.0, C["white"])
    b.rect(s1, "Tax Banner", 36, 314, 340, 34, fill_hex=C["black"], radius=4.0)
    b.text(s1, "Tax Banner T", 36, 322, 340, 18, "= 45 FULL 24H DAYS GONE EVERY YEAR", FONT_MONO, "Bold", 11.0, 18.0, C["yellow"], align_h="center")

    # Stacked Waking Day Bar Chart
    b.text(s1, "Breakdown Lbl", 20, 386, 372, 16, "WHERE A 24H DAY ACTUALLY GOES (3H SCROLLING)", FONT_MONO, "Bold", 10.0, 16.0, C["black"])
    b.rect(s1, "Day Frame", 20, 408, 372, 42, fill_hex=C["white"], stroke_hex=C["black"], stroke_w=2.5, radius=4.0)
    b.rect(s1, "Seg Sleep", 22, 410, 122, 38, fill_hex=C["black"])
    b.text(s1, "Seg Sleep T", 22, 421, 122, 16, "SLEEP 8H", FONT_MONO, "Bold", 10.0, 16.0, C["white"], align_h="center")
    b.rect(s1, "Seg Work", 144, 410, 128, 38, fill_hex=C["neutral"])
    b.text(s1, "Seg Work T", 144, 421, 128, 16, "WORK 8H", FONT_MONO, "Bold", 10.0, 16.0, C["black"], align_h="center")
    b.rect(s1, "Seg Feed", 272, 410, 72, 38, fill_hex=C["red"])
    b.text(s1, "Seg Feed T", 272, 421, 72, 16, "FEED", FONT_MONO, "Bold", 10.0, 16.0, C["white"], align_h="center")
    b.rect(s1, "Seg Free", 344, 410, 46, 38, fill_hex=C["yellow"])
    b.text(s1, "Seg Free T", 344, 421, 46, 16, "YOU", FONT_MONO, "Bold", 9.5, 16.0, C["black"], align_h="center")

    # Daily input selector buttons
    b.text(s1, "Hr Kicker", 20, 468, 372, 16, "DAILY SCROLL ESTIMATE", FONT_MONO, "Bold", 10.0, 16.0, C["black"])
    for idx, hr in enumerate(["1H", "2H", "3H", "4H+"]):
        hx = 20.0 + idx * 96.0
        sel = hr == "3H"
        _brutalist_box(b, s1, f"Hr Box {idx}", hx, 490, 84, 44, C["yellow"] if sel else C["white"], offset=3.0, border_w=2.5)
        b.text(s1, f"Hr T {idx}", hx, 502, 84, 20, hr, FONT_POSTER, "Bold", 14.5, 20.0, C["black"], align_h="center")

    # Goal checkboxes
    b.text(s1, "Goals Head", 20, 554, 372, 16, "TAKE BACK CONTROL FOR:", FONT_MONO, "Bold", 10.0, 16.0, C["black"])
    goals = [("FINISH DEEP WORK DAILY", True), ("NO SCREENS AFTER MIDNIGHT", True), ("SHIP CREATIVE PROJECTS", False)]
    for idx, (gtxt, on) in enumerate(goals):
        gy = 578.0 + idx * 58.0
        _brutalist_box(b, s1, f"Goal Card {idx}", 20, gy, 372, 48, C["yellow"] if on else C["white"], offset=3.0, border_w=2.0)
        b.rect(s1, f"G Box {idx}", 34, gy + 12, 24, 24, fill_hex=C["black"] if on else C["paper"], stroke_hex=C["black"], stroke_w=2.0, radius=3.0)
        if on:
            b.text(s1, f"G Tick {idx}", 34, gy + 14, 24, 20, "X", FONT_MONO, "Bold", 14.0, 20.0, C["yellow"], align_h="center")
        b.text(s1, f"G Label {idx}", 72, gy + 14, 304, 20, gtxt, FONT_MONO, "Bold", 12.0, 20.0, C["black"])

    cta1 = _brutalist_box(b, s1, "S1 CTA", 20, 774, 372, 66, C["black"], shadow_hex=C["yellow"], offset=5.0, border_w=2.5)
    b.text(s1, "S1 CTA T", 20, 796, 372, 22, "INSTALL FRICTION WALL ->", FONT_POSTER, "Bold", 15.0, 22.0, C["yellow"], align_h="center")
    b.text(s1, "S1 Foot", 20, 856, 372, 16, "ZERO ADS  ·  ON-DEVICE ACCESSIBILITY", FONT_MONO, "Bold", 10.0, 16.0, C["muted"], align_h="center")

    # =========================================================================
    # SCREEN 2: HOME — SWISS BENTO GRID
    # =========================================================================
    s2 = _screen(b, 1, "02 Home — Swiss Bento Grid", C["paper"], "Asymmetric Swiss Bento grid using stark color blocks for focus score, blocks, and reclaimed minutes.")
    _status_bar(b, s2)
    b.text(s2, "S2 Brand", 20, 56, 220, 28, "HARD STOP", FONT_POSTER, "Bold", 24.0, 28.0, C["black"])
    b.rect(s2, "S2 Live Pill", 292, 57, 100, 26, fill_hex=C["red"], stroke_hex=C["black"], stroke_w=2.0, radius=4.0)
    b.text(s2, "S2 Live T", 292, 62, 100, 16, "LOCK LIVE", FONT_MONO, "Bold", 10.5, 16.0, C["white"], align_h="center")

    # Bento Top Left: Acid Yellow 88% Clarity
    _brutalist_box(b, s2, "Bento Clarity", 20, 102, 214, 220, C["yellow"], offset=5.0)
    b.text(s2, "BC Kicker", 34, 116, 186, 16, "CLARITY TODAY", FONT_MONO, "Bold", 10.5, 16.0, C["black"])
    b.text(s2, "BC Score", 32, 146, 190, 82, "88%", FONT_POSTER, "Bold", 68.0, 76.0, C["black"], tnum=True)
    b.rect(s2, "BC Tag", 34, 272, 156, 28, fill_hex=C["black"], radius=4.0)
    b.text(s2, "BC Tag T", 34, 278, 156, 16, "STAGE 1: SHARP", FONT_MONO, "Bold", 10.5, 16.0, C["yellow"], align_h="center")

    # Bento Top Right Top: 07 Stopped (Ink Black)
    _brutalist_box(b, s2, "Bento Blocked", 248, 102, 144, 103, C["black"], shadow_hex=C["red"], offset=5.0)
    b.text(s2, "BB Val", 262, 114, 116, 48, "07", FONT_POSTER, "Bold", 42.0, 48.0, C["yellow"], tnum=True)
    b.text(s2, "BB Lbl", 262, 168, 116, 28, "URGES\nSTOPPED", FONT_MONO, "Bold", 10.0, 13.0, C["white"])

    # Bento Top Right Bottom: 42m Reclaimed (Electric Cobalt)
    _brutalist_box(b, s2, "Bento Saved", 248, 219, 144, 103, C["cobalt"], offset=5.0)
    b.text(s2, "BS Val", 262, 232, 116, 48, "42m", FONT_POSTER, "Bold", 38.0, 44.0, C["white"], tnum=True)
    b.text(s2, "BS Lbl", 262, 284, 116, 28, "RECLAIMED\nTODAY", FONT_MONO, "Bold", 10.0, 13.0, C["cobaltSoft"])

    # Full-width White Mantra Block
    _brutalist_box(b, s2, "Mantra Box", 20, 344, 372, 134, C["white"], offset=5.0)
    b.rect(s2, "Rule1 Tag", 36, 360, 132, 22, fill_hex=C["red"], radius=3.0)
    b.text(s2, "Rule1 T", 36, 363, 132, 16, "RULE NO. 01", FONT_MONO, "Bold", 10.5, 16.0, C["white"], align_h="center")
    b.text(s2, "Mantra Txt", 36, 394, 340, 64, "BUILD THINGS THAT OUTLAST THE FEED.", FONT_POSTER, "Bold", 20.0, 26.0, C["black"])

    # Guarded Apps 4-Tile Strip
    b.text(s2, "Lock Strip L", 20, 502, 372, 16, "04 LOCKED TARGETS", FONT_MONO, "Bold", 10.5, 16.0, C["black"])
    tiles = [("YT", "4s"), ("IG", "4s"), ("TT", "6s"), ("RD", "4s")]
    for idx, (code, sec) in enumerate(tiles):
        tx = 20.0 + idx * 96.0
        _brutalist_box(b, s2, f"Tile {idx}", tx, 526, 84, 78, C["white"], offset=3.0, border_w=2.5)
        b.ellipse(s2, f"Tile Dot {idx}", tx + 62, 536, 10, 10, fill_hex=C["red"])
        b.text(s2, f"Tile Code {idx}", tx + 10, 540, 54, 26, code, FONT_POSTER, "Bold", 20.0, 26.0, C["black"])
        b.text(s2, f"Tile Sec {idx}", tx + 10, 572, 64, 16, f"{sec} HOLD", FONT_MONO, "Bold", 10.0, 16.0, C["muted"])

    # Giant Test Signal Red CTA
    test_cta = _brutalist_box(b, s2, "Test Red CTA", 20, 636, 372, 68, C["red"], offset=5.0)
    b.text(s2, "Test Red T", 20, 659, 372, 22, "TEST PRESS & HOLD WALL ->", FONT_POSTER, "Bold", 15.0, 22.0, C["white"], align_h="center")
    b.text(s2, "S2 Foot Note", 20, 722, 372, 18, "Triggers real Android overlay friction flow", FONT_SANS, "Semi Bold", 12.5, 18.0, C["muted"], align_h="center")
    _nav_bar(b, s2, 0)

    # =========================================================================
    # SCREEN 3: LIVE YOUTUBE FRICTION GATE — FULL-BLEED SIGNAL RED SHOCK POSTER
    # =========================================================================
    s3 = _screen(b, 2, "03 Live Gate — Signal Red Poster", C["red"], "Full-bleed Signal Red poster visually jolts the user out of YouTube launch autopilot.")
    _status_bar(b, s3, on_red=True)

    b.rect(s3, "Attempt Stamp", 20, 58, 372, 30, fill_hex=C["black"], radius=4.0)
    b.text(s3, "Attempt Txt", 20, 65, 372, 16, "! INTERCEPTED: YOUTUBE  //  ATTEMPT #04 !", FONT_MONO, "Bold", 11.0, 16.0, C["yellow"], align_h="center")

    b.text(s3, "Shock Giant", 20, 104, 372, 128, "STOP.\nDO YOU NEED\nYOUTUBE?", FONT_POSTER, "Bold", 36.0, 40.0, C["white"])

    # Yellow Hazard Strip
    b.rect(s3, "Hazard Pill", 20, 246, 316, 32, fill_hex=C["yellow"], stroke_hex=C["black"], stroke_w=2.5, radius=4.0)
    b.text(s3, "Hazard T", 20, 253, 316, 18, "LAST VISIT DRAINED 23 MINUTES", FONT_MONO, "Bold", 11.0, 18.0, C["black"], align_h="center")

    # Thick White Anchor Box
    _brutalist_box(b, s3, "Shock Mantra", 20, 298, 372, 134, C["white"], offset=6.0, border_w=3.0)
    b.text(s3, "Shock MK", 38, 316, 336, 16, "YOU PROMISED YOURSELF TONIGHT:", FONT_MONO, "Bold", 10.5, 16.0, C["red"])
    b.text(s3, "Shock MB", 38, 344, 336, 66, "Protect sleep & ship your deep work.", FONT_POSTER, "Bold", 22.0, 29.0, C["black"])

    # Primary Immediate Abort CTA (Solid Black + Yellow offset)
    abort_cta = _brutalist_box(b, s3, "Abort YT CTA", 20, 466, 372, 74, C["black"], shadow_hex=C["yellow"], offset=6.0, border_w=3.0)
    b.text(s3, "Abort YT T", 20, 491, 372, 24, "NO — CLOSE YOUTUBE NOW", FONT_POSTER, "Bold", 16.5, 24.0, C["white"], align_h="center")

    b.text(s3, "Or Break", 20, 566, 372, 18, "OR BREAK SEAL DELIBERATELY", FONT_MONO, "Bold", 11.0, 18.0, C["white"], align_h="center")

    # Stepped 10-Block Yellow/Black Press & Hold Meter
    _brutalist_box(b, s3, "Seal Track", 20, 598, 372, 114, C["white"], offset=6.0, border_w=3.0)
    b.text(s3, "Seal Status", 34, 612, 344, 22, "HOLD 4.0s TO ENTER · 1.9s (48%)", FONT_MONO, "Bold", 13.0, 20.0, C["black"], align_h="center", tnum=True)
    for idx in range(10):
        bx = 34.0 + idx * 34.8
        filled = idx < 5
        b.rect(s3, f"Seal Cell {idx}", bx, 646, 29, 46, fill_hex=C["yellow"] if filled else C["neutral"], stroke_hex=C["black"], stroke_w=2.0, radius=3.0)
        if filled:
            b.rect(s3, f"Seal Dot {idx}", bx + 8, 660, 13, 18, fill_hex=C["black"], radius=2.0)

    b.text(s3, "Shock Foot", 24, 738, 364, 40, "RELEASE EARLY = INSTANT RESET TO ZERO.\nMUSCLE MEMORY BLOCKED.", FONT_MONO, "Bold", 10.5, 17.0, C["white"], align_h="center")

    # =========================================================================
    # SCREEN 4: GUARDED APPS & SWISS TELEMETRY HISTOGRAM
    # =========================================================================
    s4 = _screen(b, 3, "04 Apps — Duel & Histogram", C["paper"], "Chunky lock toggles paired with a brutalist Duel scoreboard and 7-day stepped histogram.")
    _status_bar(b, s4)
    b.text(s4, "S4 Head", 20, 54, 240, 28, "LOCK MATRIX", FONT_POSTER, "Bold", 23.0, 28.0, C["black"])
    b.rect(s4, "Delay Tag", 272, 54, 120, 26, fill_hex=C["black"], radius=4.0)
    b.text(s4, "Delay Tag T", 272, 59, 120, 16, "HOLD: 4.0s", FONT_MONO, "Bold", 10.5, 16.0, C["yellow"], align_h="center")

    # App Rules List
    rows = [("YOUTUBE", "4s HOLD · 5 BLOCKED", True), ("INSTAGRAM", "4s HOLD · 4 BLOCKED", True), ("TIKTOK", "6s HOLD · 2 BLOCKED", True), ("CHROME", "UNPROTECTED", False)]
    for idx, (aname, sub, on) in enumerate(rows):
        ry = 100.0 + idx * 70.0
        _brutalist_box(b, s4, f"Row {idx}", 20, ry, 372, 56, C["white"] if on else C["neutral"], offset=3.0, border_w=2.0)
        b.rect(s4, f"Row Marker {idx}", 32, ry + 14, 10, 28, fill_hex=C["red"] if on else C["muted"], radius=2.0)
        b.text(s4, f"Row Name {idx}", 54, ry + 11, 210, 20, aname, FONT_POSTER, "Bold", 15.0, 20.0, C["black"])
        b.text(s4, f"Row Sub {idx}", 54, ry + 32, 230, 14, sub, FONT_MONO, "Bold", 9.5, 14.0, C["muted"])
        b.rect(s4, f"Row Sw {idx}", 314, ry + 13, 62, 30, fill_hex=C["yellow"] if on else C["paper"], stroke_hex=C["black"], stroke_w=2.0, radius=4.0)
        b.rect(s4, f"Row Knob {idx}", 346 if on else 318, ry + 17, 26, 22, fill_hex=C["black"], radius=2.0)

    # Focus Duel Scoreboard
    _brutalist_box(b, s4, "Duel Block", 20, 392, 372, 114, C["black"], shadow_hex=C["yellow"], offset=4.0)
    b.text(s4, "Duel Lbl", 34, 404, 344, 16, "ACCOUNTABILITY DUEL // LOWER WINS", FONT_MONO, "Bold", 10.0, 16.0, C["yellow"])
    b.text(s4, "Duel You", 34, 428, 160, 42, "YOU: 02", FONT_POSTER, "Bold", 30.0, 38.0, C["yellow"], tnum=True)
    b.text(s4, "Duel VS", 184, 440, 44, 20, "VS", FONT_MONO, "Bold", 12.0, 20.0, C["white"], align_h="center")
    b.text(s4, "Duel Avg", 224, 428, 152, 42, "AVG: 14", FONT_POSTER, "Bold", 30.0, 38.0, C["red"], align_h="right", tnum=True)
    b.text(s4, "Duel Foot", 34, 478, 344, 14, "OUTPERFORMING 86% OF HEAVY SCROLLERS", FONT_MONO, "Bold", 10.0, 14.0, C["white"])

    # 7-Day Histogram
    _brutalist_box(b, s4, "Hist Card", 20, 526, 372, 274, C["white"], offset=4.0)
    b.text(s4, "Hist Title", 36, 542, 240, 20, "7-DAY CLARITY TREND", FONT_POSTER, "Bold", 15.0, 20.0, C["black"])
    b.text(s4, "Hist Tag", 270, 544, 106, 16, "+22% WK/WK", FONT_MONO, "Bold", 10.5, 16.0, C["cobalt"], align_h="right")

    bars = [("MON", 64), ("TUE", 72), ("WED", 78), ("THU", 82), ("FRI", 85), ("SAT", 91), ("SUN", 88)]
    for idx, (dlbl, pct) in enumerate(bars):
        bx = 38.0 + idx * 49.0
        bh = round(122.0 * (pct / 100.0), 1)
        by = 740.0 - bh
        b.rect(s4, f"H Track {idx}", bx, 594, 34, 146, fill_hex=C["neutral"], stroke_hex=C["black"], stroke_w=1.5, radius=3.0)
        b.rect(s4, f"H Fill {idx}", bx, by, 34, bh, fill_hex=C["yellow"] if pct >= 85 else C["cobalt"], stroke_hex=C["black"], stroke_w=2.0, radius=3.0)
        b.text(s4, f"H Val {idx}", bx, by - 18, 34, 14, str(pct), FONT_MONO, "Bold", 9.5, 14.0, C["black"], align_h="center", tnum=True)
        b.text(s4, f"H Day {idx}", bx, 750, 34, 14, dlbl, FONT_MONO, "Bold", 9.5, 14.0, C["black"], align_h="center")

    _nav_bar(b, s4, 1)

    link_flow(b, cta1, s2)
    link_flow(b, test_cta, s3, transition="dissolve", duration_ms=200)
    link_flow(b, abort_cta, s2)
    b.nodes[str(b.page_id)]["flows"] = [
        {"name": "Hard Stop Poster Flow", "start": s1, "description": "Scroll Tax -> Bento Grid -> Red Shock Gate -> Lock Matrix"}
    ]

    b.add_comment(s1, 206, 260, "Behavioral Shock", "Turns daily short-form video time into an unavoidable red Swiss poster loss metric.")
    b.add_comment(s3, 206, 300, "Visual Interrupt", "Zero-radius hard shadows + full-bleed Signal Red intentionally break autopilot doomscrolling.")

    doc = b.finalize_document(
        variables_spec=[
            ("surface/poster", C["paper"], "Stark architectural white"),
            ("ink/black", C["black"], "Thick 2.5px ink border"),
            ("alert/signalRed", C["red"], "Full-bleed friction poster red"),
            ("accent/acidYellow", C["yellow"], "Hazard yellow focus accent"),
        ],
        collection_name="HARD STOP Tokens",
        mode_name="Swiss Poster",
        doc_colors=[C["paper"], C["black"], C["red"], C["yellow"], C["cobalt"], C["neutral"]],
        annotations_cats=[("aesthetic-system", "Aesthetic System", C["red"])],
    )
    return doc, [s1, s2, s3, s4], b.images
