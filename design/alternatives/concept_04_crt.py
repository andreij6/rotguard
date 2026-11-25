"""Option 4: NEURO.OS — Phosphor CRT Bio-Terminal (Tektronix Oscilloscope / Severance)."""

from __future__ import annotations

import math
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
    "backdrop": "#030705",
    "crt": "#060D09",
    "bay": "#0B1710",
    "bayHi": "#112218",
    "grid": "#1B3A29",
    "gridBright": "#28573D",
    "mint": "#3EFF9B",
    "mintSoft": "#103522",
    "mintDim": "#5F8E75",
    "amber": "#FFB020",
    "amberSoft": "#2D2109",
    "red": "#FF465A",
    "redSoft": "#2D1016",
    "text": "#DDFEEB",
}

FONT_MONO = "Liberation Mono"
FONT_SANS = "Inter"

OUTPUT_FIGCRAFT = ALT_DIR / "04_crt_terminal.figcraft"


def _screen(b: DocBuilder, slot: int, name: str, desc: str) -> int:
    s = b.frame(
        b.page_id,
        name,
        screen_x(slot),
        SCREEN_TOP_Y,
        SCREEN_W,
        SCREEN_H,
        fill_hex=C["crt"],
        stroke_hex=C["gridBright"],
        stroke_w=2.0,
        radius=30.0,
        clip=True,
        effects=drop_shadow(C["mint"], 0.14, 0.0, 0.0, 28.0),
        annotations=[{"label": desc, "properties": ["fill", "effects", "typography"], "category": "aesthetic-system"}],
    )
    # Subtle horizontal CRT raster scanlines
    for sy in range(44, int(SCREEN_H), 16):
        b.rect(s, "Scanline", 0, float(sy), SCREEN_W, 1.0, fill_hex=C["mint"], fill_alpha=0.04)
    return s


def _status_bar(b: DocBuilder, s: int, alert: bool = False) -> None:
    b.text(s, "CRT Time", 22, 14, 70, 16, "09:41:02", FONT_MONO, "Bold", 11.0, 16.0, C["mint"], tnum=True)
    col = C["amber"] if alert else C["mint"]
    b.text(s, "CRT Node", 220, 14, 170, 16, "[● NODE: ACTIVE]" if not alert else "[! INTERCEPTED]", FONT_MONO, "Bold", 10.5, 16.0, col, align_h="right")
    b.rule(s, "CRT Header Rule", 18, 38, 376, C["gridBright"], thickness=1.5)


def _nav_bar(b: DocBuilder, s: int, active_idx: int) -> None:
    b.rect(s, "CRT Nav BG", 0, 828, SCREEN_W, 87, fill_hex=C["bay"])
    b.rule(s, "CRT Nav Rule", 0, 828, SCREEN_W, C["gridBright"], thickness=1.5)
    items = ["[HOME]", "[APPS]", "[LOGS]", "[BIOS]"]
    cw = (SCREEN_W - 24.0) / 4.0
    for idx, lbl in enumerate(items):
        cx = 12.0 + idx * cw
        act = idx == active_idx
        if act:
            b.rect(s, "CRT Nav Act", cx + 6, 840, cw - 12, 42, fill_hex=C["mintSoft"], stroke_hex=C["mint"], radius=4.0)
        b.text(s, f"CRT Nav T {idx}", cx, 853, cw, 16, lbl, FONT_MONO, "Bold", 11.5, 16.0, C["mint"] if act else C["mintDim"], align_h="center")


def build_concept_04() -> tuple[dict[str, Any], list[int], dict[str, tuple[bytes, int, int]]]:
    b = DocBuilder("Option 04 — NEURO.OS (Phosphor CRT Bio-Terminal)", C["backdrop"])
    setup_gallery_chrome(
        b,
        "CONCEPT 04  //  RETRO PHOSPHOR OSCILLOSCOPE TERMINAL",
        "NEURO.OS: Phosphor Bio-Terminal",
        "Tektronix oscilloscope waveforms & terminal lock protocols. Gamifies mental clarity as clean alpha coherence over dopamine noise.",
        "LIBERATION MONO + INTER  //  PHOSPHOR CRT",
        C["backdrop"],
        C["mint"],
        C["mintDim"],
        C["gridBright"],
        [(C["crt"], "#060D09"), (C["mint"], "#3EFF9B"), (C["amber"], "#FFB020"), (C["red"], "#FF465A")],
        [
            "01  DIAGNOSTIC · WAVEFORM DECAY",
            "02  CONSOLE · COHERENCE MONITOR",
            "03  PROCESS TRAP · YOUTUBE.EXE",
            "04  LOCK TABLE · TELEMETRY SPECTRUM",
        ],
        header_font=FONT_MONO,
        body_font=FONT_SANS,
        mono_font=FONT_MONO,
    )

    # =========================================================================
    # SCREEN 1: ONBOARDING — EEG WAVEFORM DIAGNOSTIC
    # =========================================================================
    s1 = _screen(b, 0, "01 Diagnostic — Waveform Decay", "Dual oscilloscope channel compares steady alpha focus against jagged short-video dopamine spikes.")
    _status_bar(b, s1)
    b.text(s1, "Sys Tag", 20, 52, 220, 16, "NEURO.OS // DIAGNOSTIC_02", FONT_MONO, "Bold", 10.5, 16.0, C["mintDim"])
    b.text(s1, "Step Tag", 280, 52, 112, 16, "[STEP 2/7]", FONT_MONO, "Bold", 10.5, 16.0, C["amber"], align_h="right")
    b.text(s1, "S1 Title", 20, 74, 372, 54, "SIGNAL DEGRADATION\nDETECTED.", FONT_MONO, "Bold", 23.0, 27.0, C["mint"])

    # Oscilloscope Bay
    b.rect(s1, "Scope Frame", 20, 140, 372, 248, fill_hex=C["bay"], stroke_hex=C["gridBright"], stroke_w=1.5, radius=8.0)
    b.text(s1, "CH1 Lbl", 34, 152, 344, 14, "CH1 // BASELINE CLARITY (0 REELS/D) — STEADY", FONT_MONO, "Bold", 9.5, 14.0, C["mint"])
    # Sine wave bars for CH1
    for i in range(28):
        bx = 34.0 + i * 12.2
        amp = 22.0 + 16.0 * math.sin(i * 0.48)
        b.rect(s1, f"CH1 {i}", bx, 198.0 - amp / 2.0, 7.0, amp, fill_hex=C["mint"], radius=1.5)

    b.rect(s1, "Scope Mid Grid", 34, 232, 344, 1.0, fill_hex=C["gridBright"], dashes=[4.0, 4.0])
    b.text(s1, "CH2 Lbl", 34, 244, 344, 14, "CH2 // 150+ REELS/D — SEVERE CLIPPING (-34%)", FONT_MONO, "Bold", 9.5, 14.0, C["red"])
    spikes = [14, 52, 12, 56, 8, 48, 58, 16, 10, 54, 60, 18, 52, 12, 58, 10, 46, 58, 14, 54, 10, 56, 20, 50, 14, 58, 12, 48]
    for i, sp in enumerate(spikes):
        bx = 34.0 + i * 12.2
        b.rect(s1, f"CH2 {i}", bx, 326.0 - sp, 7.0, float(sp), fill_hex=C["red"] if sp > 35 else C["amber"], radius=1.5)

    b.rule(s1, "Scope Stat Rule", 34, 340, 344, C["grid"])
    b.text(s1, "Scope Stat L", 34, 354, 170, 18, "LOSS: 9.40 YRS", FONT_MONO, "Bold", 13.0, 18.0, C["red"], tnum=True)
    b.text(s1, "Scope Stat R", 204, 354, 174, 18, "HAPTIC: STUTTER", FONT_MONO, "Bold", 13.0, 18.0, C["amber"], align_h="right")

    # Daily Feed Input Selector
    b.text(s1, "In Lbl", 20, 408, 372, 14, "> SELECT CURRENT DAILY SCROLL VOLUME:", FONT_MONO, "Bold", 10.5, 14.0, C["mintDim"])
    for idx, val in enumerate(["0", "50", "100", "150", "200+"]):
        px = 20.0 + idx * 76.0
        sel = val == "150"
        b.rect(s1, f"Load Opt {val}", px, 432, 68, 38, fill_hex=C["redSoft"] if sel else C["bay"], stroke_hex=C["red"] if sel else C["gridBright"], stroke_w=1.5, radius=4.0)
        b.text(s1, f"Load Opt T {val}", px, 443, 68, 16, val, FONT_MONO, "Bold", 12.0, 16.0, C["red"] if sel else C["mintDim"], align_h="center", tnum=True)

    # Terminal Directives
    b.text(s1, "Dir Lbl", 20, 492, 372, 14, "> ENABLE RECOVERY DIRECTIVES:", FONT_MONO, "Bold", 10.5, 14.0, C["mintDim"])
    dirs = [
        ("[X] LOCK_DEEP_WORK_WINDOWS", True),
        ("[X] SLEEP_PROTOCOL_BEFORE_00:00", True),
        ("[ ] REAL_WORLD_CREATION", False),
    ]
    for idx, (txt, on) in enumerate(dirs):
        dy = 516.0 + idx * 54.0
        b.rect(s1, f"Dir {idx}", 20, dy, 372, 44, fill_hex=C["mintSoft"] if on else C["bay"], stroke_hex=C["mint"] if on else C["grid"], stroke_w=1.2, radius=4.0)
        b.text(s1, f"Dir T {idx}", 34, dy + 13, 344, 18, txt, FONT_MONO, "Bold", 12.0, 18.0, C["mint"] if on else C["mintDim"])

    cta1 = b.rect(s1, "S1 Engage CTA", 20, 728, 372, 62, fill_hex=C["mint"], radius=6.0, effects=drop_shadow(C["mint"], 0.35, 0.0, 4.0, 18.0))
    b.text(s1, "S1 Engage T", 20, 749, 372, 20, "[ ENGAGE_SHIELD_PROTOCOL -> ]", FONT_MONO, "Bold", 13.5, 20.0, C["crt"], align_h="center")
    b.text(s1, "S1 CRT Foot", 20, 808, 372, 16, "ZERO CLOUD TELEMETRY // LOCAL ACCESSIBILITY HOOK", FONT_MONO, "Regular", 9.5, 16.0, C["mintDim"], align_h="center")

    # =========================================================================
    # SCREEN 2: HOME — COHERENCE MONITOR CONSOLE
    # =========================================================================
    s2 = _screen(b, 1, "02 Console — Coherence Monitor", "CRT telemetry cockpit visualizing daily attention stability as live coherence.")
    _status_bar(b, s2)
    b.text(s2, "S2 Header", 20, 52, 220, 22, "NEURO.OS // CORE", FONT_MONO, "Bold", 17.0, 22.0, C["mint"])
    b.rect(s2, "S2 Armed", 272, 50, 120, 24, fill_hex=C["mintSoft"], stroke_hex=C["mint"], radius=4.0)
    b.text(s2, "S2 Armed T", 272, 54, 120, 16, "GUARD: ONLINE", FONT_MONO, "Bold", 10.0, 16.0, C["mint"], align_h="center")

    # Main Oscilloscope Card
    b.rect(s2, "Coherence Bay", 20, 90, 372, 228, fill_hex=C["bay"], stroke_hex=C["gridBright"], stroke_w=1.5, radius=8.0)
    b.text(s2, "Coh Kicker", 36, 106, 200, 14, "ALPHA COHERENCE INDEX", FONT_MONO, "Bold", 10.0, 14.0, C["mintDim"])
    b.text(s2, "Coh Stage", 240, 106, 136, 14, "[STAGE_01: PEAK]", FONT_MONO, "Bold", 10.0, 14.0, C["mint"], align_h="right")
    b.text(s2, "Coh Score", 36, 134, 190, 56, "88.0%", FONT_MONO, "Bold", 44.0, 50.0, C["mint"], tnum=True)
    b.text(s2, "Coh Delta", 36, 194, 180, 16, "+14.2% vs baseline", FONT_MONO, "Regular", 11.0, 16.0, C["mintDim"])

    # Right oscilloscope sine envelope bars inside Coherence Bay
    for i in range(14):
        bx = 228.0 + i * 10.5
        amp = 32.0 + 26.0 * math.sin(i * 0.65)
        b.rect(s2, f"Live Bar {i}", bx, 178.0 - amp / 2.0, 6.0, amp, fill_hex=C["mint"] if i % 2 == 0 else C["gridBright"], radius=2.0)

    b.rule(s2, "Bay Mid", 36, 232, 340, C["grid"])
    # 20-cell progress line
    for i in range(20):
        px = 36.0 + i * 17.2
        b.rect(s2, f"Coh Cell {i}", px, 250, 12, 16, fill_hex=C["mint"] if i < 17 else C["grid"], radius=2.0)
    b.text(s2, "Bay Status", 36, 282, 340, 16, "INTERCEPTOR: YOUTUBE, IG, TIKTOK, REDDIT", FONT_MONO, "Bold", 9.5, 16.0, C["mintDim"])

    # 3 Terminal Counters
    counters = [("[07]", "HALTED"), ("[42m]", "SAVED"), ("[04]", "LOCKED")]
    for idx, (cval, clbl) in enumerate(counters):
        cx = 20.0 + idx * 128.0
        b.rect(s2, f"Cnt {idx}", cx, 336, 116, 86, fill_hex=C["bay"], stroke_hex=C["gridBright"], radius=6.0)
        b.text(s2, f"Cnt V {idx}", cx, 350, 116, 30, cval, FONT_MONO, "Bold", 24.0, 28.0, C["mint"], align_h="center", tnum=True)
        b.text(s2, f"Cnt L {idx}", cx, 388, 116, 16, clbl, FONT_MONO, "Bold", 10.0, 16.0, C["mintDim"], align_h="center")

    # Command-Line Anchor Mantra Box
    b.rect(s2, "CLI Box", 20, 442, 372, 114, fill_hex=C["bayHi"], stroke_hex=C["mint"], stroke_w=1.2, radius=6.0)
    b.text(s2, "CLI Prompt", 36, 458, 340, 16, "> cat /user/anchor_mantra.txt", FONT_MONO, "Bold", 10.5, 16.0, C["amber"])
    b.text(s2, "CLI Mantra", 36, 484, 340, 50, "\"Build things that outlast the feed.\"", FONT_MONO, "Bold", 16.0, 22.0, C["text"])

    # Test Shield Glow CTA
    test_cta = b.rect(s2, "Test CRT CTA", 20, 588, 372, 64, fill_hex=C["mint"], radius=6.0, effects=drop_shadow(C["mint"], 0.35, 0.0, 4.0, 18.0))
    b.text(s2, "Test CRT T", 20, 610, 372, 20, "[ LAUNCH_TEST_INTERCEPT -> ]", FONT_MONO, "Bold", 13.5, 20.0, C["crt"], align_h="center")

    b.rect(s2, "CRT Log Line", 20, 672, 372, 114, fill_hex=C["bay"], stroke_hex=C["grid"], radius=6.0)
    b.text(s2, "Log 1", 34, 686, 344, 16, "09:14:22  INTERCEPTED youtube -> CLOSED (2.1s)", FONT_MONO, "Regular", 10.5, 16.0, C["mint"])
    b.text(s2, "Log 2", 34, 710, 344, 16, "08:42:10  INTERCEPTED instagram -> CLOSED (1.4s)", FONT_MONO, "Regular", 10.5, 16.0, C["mint"])
    b.text(s2, "Log 3", 34, 734, 344, 16, "07:55:03  BYPASS_GRANTED reddit (4.0s hold)", FONT_MONO, "Regular", 10.5, 16.0, C["amber"])
    b.text(s2, "Log 4", 34, 758, 344, 16, "07:54:58  _cursor_ready", FONT_MONO, "Bold", 10.5, 16.0, C["mintDim"])
    _nav_bar(b, s2, 0)

    # =========================================================================
    # SCREEN 3: LIVE YOUTUBE FRICTION GATE — RADAR RETICLE OVERRIDE
    # =========================================================================
    s3 = _screen(b, 2, "03 Process Trap — YouTube.exe", "CRT process lock with circular oscilloscope radar sweep and Amber manual override bar.")
    _status_bar(b, s3, alert=True)
    b.rect(s3, "Alert Banner", 20, 52, 372, 34, fill_hex=C["amberSoft"], stroke_hex=C["amber"], stroke_w=1.5, radius=4.0)
    b.text(s3, "Alert Banner T", 20, 61, 372, 16, "[!] TRAP_TRIGGERED: YOUTUBE.EXE // ATTEMPT_04", FONT_MONO, "Bold", 10.5, 16.0, C["amber"], align_h="center")
    b.text(s3, "Trap Title", 20, 102, 372, 30, "HALT AUTOPILOT THREAD.", FONT_MONO, "Bold", 22.0, 28.0, C["text"], align_h="center")

    # Circular CRT Oscilloscope Reticle (center 206, 272)
    cx, cy = 206.0, 268.0
    b.ellipse(s3, "Radar Outer", cx - 116, cy - 116, 232, 232, stroke_hex=C["gridBright"], stroke_w=1.5, dashes=[5.0, 5.0])
    b.ellipse(s3, "Radar Mid", cx - 96, cy - 96, 192, 192, stroke_hex=C["grid"], stroke_w=1.0)
    b.rule(s3, "Radar Cross H", cx - 122, cy, 244, C["grid"])
    b.vrule(s3, "Radar Cross V", cx, cy - 122, 244, C["grid"])
    b.ellipse(s3, "Radar Track", cx - 86, cy - 86, 172, 172, fill_hex=C["bayHi"], inner=0.84)
    b.ellipse(s3, "Radar Sweep", cx - 86, cy - 86, 172, 172, fill_hex=C["amber"], inner=0.84, sweep_ratio=0.48, effects=drop_shadow(C["amber"], 0.4, 0.0, 0.0, 16.0))
    b.ellipse(s3, "Radar Core", cx - 64, cy - 64, 128, 128, fill_hex=C["crt"], stroke_hex=C["gridBright"])
    b.text(s3, "Radar Sub Top", cx - 60, cy - 36, 120, 14, "OVERRIDE TIMER", FONT_MONO, "Bold", 9.0, 14.0, C["mintDim"], align_h="center")
    b.text(s3, "Radar Time", cx - 60, cy - 12, 120, 28, "1.9s", FONT_MONO, "Bold", 26.0, 28.0, C["amber"], align_h="center", tnum=True)
    b.text(s3, "Radar Pct", cx - 60, cy + 20, 120, 14, "48% / 4.0s", FONT_MONO, "Bold", 10.5, 14.0, C["text"], align_h="center", tnum=True)

    # Priority terminal card
    b.rect(s3, "Trap Priority", 20, 412, 372, 96, fill_hex=C["bay"], stroke_hex=C["gridBright"], radius=6.0)
    b.text(s3, "Trap PK", 36, 428, 340, 14, "> DECLARED_INTENTION:", FONT_MONO, "Bold", 10.0, 14.0, C["mint"])
    b.text(s3, "Trap PB", 36, 450, 340, 42, "\"Protect sleep & ship deep work tonight.\"", FONT_MONO, "Bold", 14.5, 20.0, C["text"])

    # Safe Terminate CTA
    term_cta = b.rect(s3, "Terminate CTA", 20, 536, 372, 66, fill_hex=C["mint"], radius=6.0, effects=drop_shadow(C["mint"], 0.35, 0.0, 4.0, 20.0))
    b.text(s3, "Terminate T", 20, 558, 372, 22, "[ TERMINATE_YOUTUBE (SAFE_EXIT) ]", FONT_MONO, "Bold", 13.5, 20.0, C["crt"], align_h="center")

    b.text(s3, "Trap Or", 20, 624, 372, 16, "// OR HOLD BUFFER TO FORCE OVERRIDE", FONT_MONO, "Bold", 10.0, 16.0, C["mintDim"], align_h="center")

    # Hold buffer track
    b.rect(s3, "Hold Buf BG", 20, 654, 372, 68, fill_hex=C["bay"], stroke_hex=C["amber"], stroke_w=1.5, radius=6.0)
    b.rect(s3, "Hold Buf Fill", 22, 656, 178, 64, fill_hex=C["amber"], fill_alpha=0.22, radius=4.0)
    b.rect(s3, "Hold Buf Plaque", 48, 670, 316, 36, fill_hex=C["crt"], stroke_hex=C["amber"], radius=4.0)
    b.text(s3, "Hold Buf T", 48, 679, 316, 18, "HOLDING 4.0s :: 1.9s [#####.....]", FONT_MONO, "Bold", 11.5, 18.0, C["amber"], align_h="center", tnum=True)

    b.text(s3, "Trap Foot", 28, 744, 356, 34, "INTERRUPTING HOLD ABORTS BUFFER IMMEDIATELY.\nHABIT REDIRECT PREVENTED.", FONT_MONO, "Regular", 10.0, 16.0, C["mintDim"], align_h="center")

    # =========================================================================
    # SCREEN 4: GUARDED PROCESS TABLE & SPECTRUM
    # =========================================================================
    s4 = _screen(b, 3, "04 Apps — Lock Table & Spectrum", "Monospace process table with phosphor lock switches and 7-day telemetry spectrum.")
    _status_bar(b, s4)
    b.text(s4, "S4 Title", 20, 52, 240, 24, "PROCESS_LOCK_TABLE", FONT_MONO, "Bold", 17.5, 24.0, C["mint"])
    b.text(s4, "S4 Cal", 272, 56, 120, 16, "DELAY: 4.0s", FONT_MONO, "Bold", 10.5, 16.0, C["amber"], align_h="right")

    procs = [
        ("youtube.exe", "4.0s", True, "5 HALTED"),
        ("instagram.apk", "4.0s", True, "4 HALTED"),
        ("tiktok.apk", "6.0s", True, "2 HALTED"),
        ("chrome.apk", "----", False, "OPEN"),
    ]
    for idx, (pname, delay, locked, stat) in enumerate(procs):
        py = 96.0 + idx * 62.0
        b.rect(s4, f"Proc Row {idx}", 20, py, 372, 52, fill_hex=C["bay"], stroke_hex=C["gridBright"] if locked else C["grid"], radius=6.0)
        b.text(s4, f"Proc Tag {idx}", 32, py + 17, 78, 16, "[LOCKED]" if locked else "[ OPEN ]", FONT_MONO, "Bold", 10.0, 16.0, C["mint"] if locked else C["mintDim"])
        b.text(s4, f"Proc Name {idx}", 112, py + 16, 146, 18, pname, FONT_MONO, "Bold", 13.0, 18.0, C["text"])
        b.text(s4, f"Proc Stat {idx}", 260, py + 17, 84, 16, stat, FONT_MONO, "Regular", 9.5, 16.0, C["amber"] if locked else C["mintDim"], align_h="right")
        b.rect(s4, f"Proc LED {idx}", 358, py + 18, 18, 16, fill_hex=C["mint"] if locked else C["grid"], radius=3.0)

    # Terminal Duel Banner
    b.rect(s4, "CRT Duel", 20, 362, 372, 92, fill_hex=C["bayHi"], stroke_hex=C["mint"], radius=6.0)
    b.text(s4, "CRT Duel K", 34, 376, 344, 14, "> NETWORK_DUEL_COMPARE // LOWER_BYPASSES_WIN", FONT_MONO, "Bold", 9.5, 14.0, C["mintDim"])
    b.text(s4, "CRT Duel V", 34, 400, 344, 24, "YOU: 02  <<  AVG_NODE: 14", FONT_MONO, "Bold", 20.0, 24.0, C["mint"], tnum=True)
    b.text(s4, "CRT Duel S", 34, 428, 344, 14, "STATUS: VICTORY (-86% DOPAMINE OVERRIDES)", FONT_MONO, "Bold", 10.0, 14.0, C["amber"])

    # 7-Day Telemetry Bar Spectrum
    b.rect(s4, "Spec Bay", 20, 474, 372, 326, fill_hex=C["bay"], stroke_hex=C["gridBright"], radius=8.0)
    b.text(s4, "Spec Title", 36, 492, 240, 16, "7-DAY COHERENCE SPECTRUM", FONT_MONO, "Bold", 11.5, 16.0, C["mint"])
    b.text(s4, "Spec Scale", 272, 492, 104, 16, "MAX: 91%", FONT_MONO, "Bold", 10.5, 16.0, C["mintDim"], align_h="right")

    bars = [("MON", 64), ("TUE", 72), ("WED", 78), ("THU", 82), ("FRI", 85), ("SAT", 91), ("SUN", 88)]
    for idx, (dlbl, pct) in enumerate(bars):
        bx = 40.0 + idx * 48.0
        bh = round(148.0 * (pct / 100.0), 1)
        by = 710.0 - bh
        b.rect(s4, f"STrack {idx}", bx, 534, 30, 176, fill_hex=C["crt"], stroke_hex=C["grid"], radius=3.0)
        b.rect(s4, f"SFill {idx}", bx + 3, by + 3, 24, bh - 4, fill_hex=C["mint"] if pct >= 80 else C["amber"], radius=2.0)
        b.text(s4, f"SVal {idx}", bx, by - 17, 30, 14, f"{pct}", FONT_MONO, "Bold", 9.5, 14.0, C["text"], align_h="center", tnum=True)
        b.text(s4, f"SDay {idx}", bx, 720, 30, 14, dlbl, FONT_MONO, "Bold", 9.5, 14.0, C["mintDim"], align_h="center")

    b.rule(s4, "Spec Foot Rule", 36, 748, 340, C["grid"])
    b.text(s4, "Spec Foot", 36, 764, 340, 16, "RECLAIMED_WEEK: 4h 54m  //  SIGNAL: CLEAN", FONT_MONO, "Bold", 10.5, 16.0, C["mint"], align_h="center")
    _nav_bar(b, s4, 1)

    link_flow(b, cta1, s2)
    link_flow(b, test_cta, s3, transition="dissolve", duration_ms=220)
    link_flow(b, term_cta, s2)
    b.nodes[str(b.page_id)]["flows"] = [
        {"name": "NEURO.OS Diagnostic Flow", "start": s1, "description": "Waveform Diagnostic -> Coherence Console -> Process Trap -> Lock Table"}
    ]

    b.add_comment(s1, 206, 220, "Terminal Identity", "Replaces cute characters with live Tektronix oscilloscope waveforms contrasting focus vs. reel clipping.")
    b.add_comment(s3, 206, 268, "CRT Reticle", "Phosphor Amber radar sweep turns the 4s press-and-hold timer into a deliberate system override.")

    doc = b.finalize_document(
        variables_spec=[
            ("crt/background", C["crt"], "Deep CRT phosphor glass black"),
            ("crt/mint", C["mint"], "Primary Tektronix phosphor green"),
            ("crt/amber", C["amber"], "Override warning amber"),
            ("crt/crimson", C["red"], "Dopamine spike clipping red"),
        ],
        collection_name="NEURO.OS Tokens",
        mode_name="Phosphor Green",
        doc_colors=[C["crt"], C["bay"], C["gridBright"], C["mint"], C["amber"], C["red"], C["text"]],
        annotations_cats=[("aesthetic-system", "Aesthetic System", C["mint"])],
    )
    return doc, [s1, s2, s3, s4], b.images
