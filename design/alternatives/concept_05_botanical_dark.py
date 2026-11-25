"""Option 5 (Dark Mode): Verdant Nocturnal Canopy — Dark Forest & Moonlit Botanical Sanctuary."""

from __future__ import annotations

from typing import Any

from fc_core import (
    ALT_DIR,
    ASSETS_DIR,
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
    "backdrop": "#09120E",
    "forest": "#11211A",
    "pebble": "#182D24",
    "pebbleHi": "#1E382D",
    "canopyDeep": "#0B1712",
    "canopyCard": "#14271F",
    "canopyCore": "#08120E",
    "warmCedar": "#202821",
    "warmBorder": "#3B4737",
    "rule": "#274337",
    "sagePill": "#223D31",
    "cream": "#EEF3EB",
    "eucalyptus": "#94AFA2",
    "mossBright": "#63B584",
    "mossDeep": "#2D6D4C",
    "terracotta": "#E28360",
    "sunGold": "#E8B861",
    "white": "#FAFCF8",
}

FONT_SERIF = "Merriweather"
FONT_SANS = "Nunito Sans"
FONT_MONO = "Liberation Mono"

OUTPUT_FIGCRAFT = ALT_DIR / "05_botanical_sanctuary_dark.figcraft"


def _screen(b: DocBuilder, slot: int, name: str, fill_hex: str, desc: str) -> int:
    return b.frame(
        b.page_id,
        name,
        screen_x(slot),
        SCREEN_TOP_Y,
        SCREEN_W,
        SCREEN_H,
        fill_hex=fill_hex,
        stroke_hex=C["rule"] if fill_hex == C["forest"] else "#20392E",
        stroke_w=1.5,
        radius=38.0,
        clip=True,
        effects=drop_shadow("#030805", 0.45, 0.0, 18.0, 34.0),
        annotations=[{"label": desc, "properties": ["fill", "cornerRadius", "typography"], "category": "aesthetic-system"}],
    )


def _status_bar(b: DocBuilder, s: int) -> None:
    b.text(s, "Status Time", 28, 16, 60, 18, "9:41", FONT_SANS, "Bold", 14.0, 18.0, C["cream"], tnum=True)
    b.ellipse(s, "Quiet Leaf Dot", 272, 21, 8, 8, fill_hex=C["mossBright"])
    b.text(s, "Quiet Mode", 284, 17, 102, 16, "Sanctuary On", FONT_SANS, "Semi Bold", 11.5, 16.0, C["eucalyptus"], align_h="right")


def _floating_nav(b: DocBuilder, s: int, active_idx: int) -> None:
    b.rect(
        s,
        "Floating Nav",
        30,
        822,
        352,
        68,
        fill_hex=C["pebbleHi"],
        stroke_hex=C["rule"],
        stroke_w=1.2,
        radius=34.0,
        effects=drop_shadow("#040A07", 0.40, 0.0, 10.0, 22.0),
    )
    items = ["Home", "Garden", "Rhythm", "Calm"]
    cw = 328.0 / 4.0
    for idx, label in enumerate(items):
        cx = 42.0 + idx * cw
        act = idx == active_idx
        if act:
            b.rect(s, "Nav Pill Act", cx + 4, 833, cw - 8, 46, fill_hex=C["sagePill"], stroke_hex="#375E4D", radius=23.0)
        b.text(
            s,
            f"Nav {label}",
            cx,
            847,
            cw,
            18,
            label,
            FONT_SANS,
            "Bold" if act else "Semi Bold",
            13.0,
            18.0,
            C["cream"] if act else C["eucalyptus"],
            align_h="center",
        )


def build_concept_05_dark() -> tuple[dict[str, Any], list[int], dict[str, tuple[bytes, int, int]]]:
    b = DocBuilder("Option 05 Dark — Verdant Nocturnal Canopy", C["backdrop"])
    setup_gallery_chrome(
        b,
        "CONCEPT 05 · DARK MODE  //  NOCTURNAL BOTANICAL SANCTUARY",
        "Verdant: Nocturnal Canopy Edition",
        "Deep forest evening palette where moonlit maidenhair ferns and moss terrariums glow softly inside dark eucalyptus pebbles with zero harsh white glare.",
        "MERRIWEATHER + NUNITO SANS  //  NOCTURNAL MOSS & CANOPY",
        C["backdrop"],
        C["cream"],
        C["eucalyptus"],
        "#213B30",
        [(C["forest"], "#11211A"), (C["pebble"], "#182D24"), (C["mossBright"], "#63B584"), (C["sunGold"], "#E8B861")],
        [
            "01  ONBOARDING · MOONLIT SPROUT",
            "02  SANCTUARY · NIGHT TERRARIUM",
            "03  MIDNIGHT GATE · BREATHE & HOLD",
            "04  GARDEN APPS · EVENING RHYTHM",
        ],
        header_font=FONT_SERIF,
        body_font=FONT_SANS,
        mono_font=FONT_MONO,
    )

    # =========================================================================
    # SCREEN 1: ONBOARDING — MOONLIT RECOVERY
    # =========================================================================
    s1 = _screen(b, 0, "01 Onboarding — Moonlit Sprout", C["forest"], "Arched nocturnal fern medallion set into deep moss linen for calm evening onboarding.")
    _status_bar(b, s1)
    b.rect(s1, "Step Soft Pill", 118, 52, 176, 26, fill_hex=C["sagePill"], stroke_hex=C["rule"], radius=13.0)
    b.text(s1, "Step Soft T", 118, 57, 176, 16, "Step 2 · Gentle Check-in", FONT_SANS, "Bold", 11.5, 16.0, C["mossBright"], align_h="center")
    b.text(s1, "S1 Head", 28, 90, 356, 36, "Tend your quiet hours.", FONT_SERIF, "Bold", 25.0, 32.0, C["cream"], align_h="center")

    b.clipped_image(
        s1,
        "Sprout Arch Dark",
        ASSETS_DIR / "verdant_sprout_dark.png",
        96,
        138,
        220,
        214,
        bg_hex=C["pebble"],
        stroke_hex=C["rule"],
        stroke_w=1.5,
        radius=(110.0, 110.0, 26.0, 26.0),
    )

    b.rect(s1, "Time Cost Card", 24, 368, 364, 96, fill_hex=C["pebble"], stroke_hex=C["rule"], radius=24.0)
    b.text(s1, "Cost Val", 42, 382, 328, 30, "9.4 waking years", FONT_SERIF, "Bold", 23.0, 28.0, C["terracotta"], align_h="center", tnum=True)
    b.text(s1, "Cost Desc", 42, 416, 328, 36, "At 3 hours daily, endless feeds quietly absorb 45 full days out of every year.", FONT_SANS, "Regular", 12.5, 17.0, C["eucalyptus"], align_h="center")

    b.text(s1, "Hour Lbl", 28, 480, 356, 16, "Daily feed time today", FONT_SANS, "Bold", 12.5, 16.0, C["eucalyptus"], align_h="center")
    for idx, hr in enumerate(["1h", "2h", "3h", "4h+"]):
        px = 36.0 + idx * 88.0
        sel = hr == "3h"
        b.rect(s1, f"Hr Pebble {idx}", px, 504, 76, 38, fill_hex=C["mossDeep"] if sel else C["pebble"], stroke_hex=C["mossBright"] if sel else C["rule"], stroke_w=1.2 if sel else 1.0, radius=19.0)
        b.text(s1, f"Hr T {idx}", px, 514, 76, 18, hr, FONT_SANS, "Bold", 13.5, 18.0, C["white"] if sel else C["eucalyptus"], align_h="center")

    b.text(s1, "Goals Q", 28, 560, 356, 16, "What helps your mind flourish?", FONT_SANS, "Bold", 12.5, 16.0, C["eucalyptus"])
    goals = [("Deep focus & reading", True), ("Restful sleep before midnight", True), ("Unrushed evenings offline", False)]
    for idx, (gtxt, on) in enumerate(goals):
        gy = 586.0 + idx * 58.0
        b.rect(s1, f"Goal Peb {idx}", 24, gy, 364, 48, fill_hex=C["pebble"], stroke_hex=C["mossBright"] if on else C["rule"], stroke_w=1.5 if on else 1.0, radius=24.0)
        b.text(s1, f"Goal Txt {idx}", 44, gy + 14, 280, 20, gtxt, FONT_SANS, "Bold" if on else "Regular", 14.0, 20.0, C["cream"] if on else C["eucalyptus"])
        b.ellipse(s1, f"Goal Dot {idx}", 348, gy + 13, 22, 22, fill_hex=C["mossBright"] if on else C["sagePill"])

    cta1 = b.rect(s1, "S1 Moss CTA", 24, 782, 364, 60, fill_hex=C["mossDeep"], stroke_hex=C["mossBright"], stroke_w=1.2, radius=30.0, effects=drop_shadow("#050D09", 0.35, 0.0, 8.0, 20.0))
    b.text(s1, "S1 Moss T", 24, 801, 364, 22, "Grow My Sanctuary", FONT_SANS, "Bold", 15.5, 22.0, C["white"], align_h="center")
    b.text(s1, "S1 Soft Note", 24, 854, 364, 16, "Calm haptics · Zero guilt-tripping", FONT_SANS, "Semi Bold", 11.5, 16.0, C["eucalyptus"], align_h="center")

    # =========================================================================
    # SCREEN 2: HOME — NOCTURNAL SANCTUARY DASHBOARD
    # =========================================================================
    s2 = _screen(b, 1, "02 Sanctuary — Night Terrarium", C["forest"], "Moonlit moss terrarium card paired with warm cedar reflection pebbles.")
    _status_bar(b, s2)
    b.text(s2, "S2 Sub", 26, 52, 200, 16, "TUESDAY EVENING", FONT_MONO, "Bold", 10.0, 16.0, C["eucalyptus"])
    b.text(s2, "S2 Title", 26, 70, 210, 32, "Sanctuary", FONT_SERIF, "Bold", 25.0, 30.0, C["cream"])
    b.rect(s2, "Shield Leaf Pill", 258, 66, 130, 30, fill_hex=C["sagePill"], stroke_hex=C["rule"], radius=15.0)
    b.ellipse(s2, "Shield Leaf Dot", 270, 77, 8, 8, fill_hex=C["mossBright"])
    b.text(s2, "Shield Leaf T", 282, 73, 98, 16, "Shield Ready", FONT_SANS, "Bold", 11.5, 16.0, C["mossBright"], align_h="center")

    b.rect(
        s2,
        "Hero Terrarium Card",
        22,
        112,
        368,
        310,
        fill_hex=C["pebble"],
        stroke_hex=C["rule"],
        stroke_w=1.2,
        radius=32.0,
        effects=drop_shadow("#040A07", 0.35, 0.0, 10.0, 24.0),
    )
    b.clipped_image(
        s2,
        "Terrarium Medallion Dark",
        ASSETS_DIR / "verdant_terrarium_dark.png",
        96,
        122,
        220,
        204,
        bg_hex=C["pebble"],
        stroke_hex=C["rule"],
        stroke_w=1.0,
        radius=28.0,
    )
    b.text(s2, "Hero Health", 38, 336, 336, 34, "88% Flourishing", FONT_SERIF, "Bold", 24.0, 30.0, C["cream"], align_h="center")
    b.rect(s2, "Hero Stage Pill", 104, 376, 204, 28, fill_hex=C["sagePill"], stroke_hex=C["rule"], radius=14.0)
    b.text(s2, "Hero Stage T", 104, 382, 204, 16, "Lush · 42m reclaimed today", FONT_SANS, "Bold", 11.5, 16.0, C["mossBright"], align_h="center")

    b.rect(s2, "Stat Pebble L", 22, 438, 176, 94, fill_hex=C["pebble"], stroke_hex=C["rule"], radius=24.0)
    b.text(s2, "Stat L Val", 40, 454, 140, 34, "7", FONT_SERIF, "Bold", 28.0, 32.0, C["cream"], tnum=True)
    b.text(s2, "Stat L Lbl", 40, 492, 140, 18, "Mindful pauses", FONT_SANS, "Semi Bold", 12.5, 18.0, C["eucalyptus"])

    b.rect(s2, "Stat Pebble R", 214, 438, 176, 94, fill_hex=C["pebble"], stroke_hex=C["rule"], radius=24.0)
    b.text(s2, "Stat R Val", 232, 454, 140, 34, "42m", FONT_SERIF, "Bold", 28.0, 32.0, C["mossBright"], tnum=True)
    b.text(s2, "Stat R Lbl", 232, 492, 140, 18, "Quiet returned", FONT_SANS, "Semi Bold", 12.5, 18.0, C["eucalyptus"])

    b.rect(s2, "Mantra Warm Card", 22, 548, 368, 108, fill_hex=C["warmCedar"], stroke_hex=C["warmBorder"], radius=26.0)
    b.text(s2, "Mantra Tag", 40, 566, 332, 14, "YOUR ANCHOR SEED", FONT_MONO, "Bold", 10.0, 14.0, C["terracotta"])
    b.text(s2, "Mantra Body", 40, 588, 332, 48, "“Build things that outlast the feed.”", FONT_SERIF, "Italic", 17.5, 24.0, C["cream"])

    test_cta = b.rect(s2, "Test Canopy CTA", 22, 682, 368, 60, fill_hex=C["cream"], radius=30.0, effects=drop_shadow("#050D09", 0.35, 0.0, 8.0, 20.0))
    b.text(s2, "Test Canopy T", 22, 701, 368, 22, "Preview Canopy Pause", FONT_SANS, "Bold", 15.0, 22.0, C["forest"], align_h="center")
    b.text(s2, "Guarded Note", 22, 756, 368, 18, "Gently guarding YouTube · Instagram · TikTok · Reddit", FONT_SANS, "Semi Bold", 11.5, 18.0, C["eucalyptus"], align_h="center")

    _floating_nav(b, s2, 0)

    # =========================================================================
    # SCREEN 3: LIVE YOUTUBE FRICTION GATE — MIDNIGHT CANOPY BREATHING SHIELD
    # =========================================================================
    s3 = _screen(b, 2, "03 Midnight Gate — Breathe & Hold", C["canopyDeep"], "Deepest midnight forest overlay with moonlit-gold breathing halo and slow hold ritual.")
    _status_bar(b, s3)

    b.rect(s3, "Canopy Pill", 84, 54, 244, 28, fill_hex=C["canopyCard"], stroke_hex=C["rule"], radius=14.0)
    b.text(s3, "Canopy Pill T", 84, 60, 244, 16, "PAUSE · YOUTUBE · VISIT #4", FONT_MONO, "Bold", 10.0, 16.0, C["sunGold"], align_h="center")

    b.text(s3, "Canopy Head", 28, 98, 356, 68, "Take one slow breath\nbefore entering.", FONT_SERIF, "Bold", 24.0, 31.0, C["cream"], align_h="center")

    cx, cy = 206.0, 290.0
    b.ellipse(s3, "Halo Outer", cx - 120, cy - 120, 240, 240, fill_hex="#183428", fill_alpha=0.45)
    b.ellipse(s3, "Halo Mid", cx - 98, cy - 98, 196, 196, fill_hex="#214636", fill_alpha=0.55)
    b.ellipse(s3, "Hold Track Ring", cx - 80, cy - 80, 160, 160, fill_hex="#234234", inner=0.84)
    b.ellipse(s3, "Hold Gold Arc", cx - 80, cy - 80, 160, 160, fill_hex=C["sunGold"], inner=0.84, sweep_ratio=0.48, effects=drop_shadow(C["sunGold"], 0.40, 0.0, 0.0, 20.0))
    b.ellipse(s3, "Hold Core", cx - 60, cy - 60, 120, 120, fill_hex=C["canopyCore"])
    b.text(s3, "Core Prompt", cx - 56, cy - 36, 112, 16, "BREATHE & HOLD", FONT_MONO, "Bold", 9.0, 14.0, C["eucalyptus"], align_h="center")
    b.text(s3, "Core Timer", cx - 56, cy - 14, 112, 32, "1.9s", FONT_SERIF, "Bold", 28.0, 32.0, C["cream"], align_h="center", tnum=True)
    b.text(s3, "Core Goal", cx - 56, cy + 22, 112, 16, "of 4 seconds", FONT_SANS, "Semi Bold", 11.0, 16.0, C["sunGold"], align_h="center")

    b.rect(s3, "Intention Card", 24, 434, 364, 96, fill_hex=C["canopyCard"], stroke_hex=C["rule"], radius=26.0)
    b.text(s3, "Intent Kicker", 42, 452, 328, 14, "GENTLY REMEMBERING", FONT_MONO, "Bold", 10.0, 14.0, C["sunGold"], align_h="center")
    b.text(s3, "Intent Quote", 42, 474, 328, 44, "“Protect your sleep and deep work tonight.”", FONT_SERIF, "Italic", 15.5, 22.0, C["cream"], align_h="center")

    close_cta = b.rect(s3, "Return Quiet CTA", 24, 558, 364, 62, fill_hex=C["cream"], radius=31.0, effects=drop_shadow("#040A07", 0.4, 0.0, 8.0, 20.0))
    b.text(s3, "Return Quiet T", 24, 578, 364, 22, "Return to Quiet (Close YouTube)", FONT_SANS, "Bold", 15.0, 22.0, C["canopyDeep"], align_h="center")

    b.text(s3, "Or Gentle", 24, 642, 364, 16, "OR CONTINUE SLOWLY WITH INTENT", FONT_MONO, "Bold", 10.0, 16.0, C["eucalyptus"], align_h="center")

    b.rect(s3, "Hold Well", 24, 672, 364, 64, fill_hex=C["canopyCard"], stroke_hex=C["sunGold"], stroke_w=1.5, radius=32.0)
    b.rect(s3, "Hold Fill", 26, 674, 174, 60, fill_hex=C["sunGold"], fill_alpha=0.28, radius=30.0)
    b.rect(s3, "Hold Label Pill", 62, 686, 288, 36, fill_hex=C["canopyCore"], stroke_hex=C["sunGold"], stroke_w=1.0, radius=18.0)
    b.text(s3, "Hold Bar T", 62, 695, 288, 18, "Press & hold 4s · 1.9s", FONT_SANS, "Bold", 13.0, 18.0, C["cream"], align_h="center", tnum=True)

    b.text(s3, "Canopy Foot", 36, 756, 340, 38, "Lifting your finger resets the breath gently so mindless habit never slips past.", FONT_SANS, "Regular", 12.0, 17.0, C["eucalyptus"], align_h="center")

    # =========================================================================
    # SCREEN 4: GARDEN APPS & WEEKLY RHYTHM
    # =========================================================================
    s4 = _screen(b, 3, "04 Garden Apps & Rhythm Dark", C["forest"], "Nocturnal forest app pebbles paired with glowing moss and sunlit gold rhythm bars.")
    _status_bar(b, s4)
    b.text(s4, "S4 Title", 26, 54, 240, 32, "Guarded Garden", FONT_SERIF, "Bold", 24.0, 30.0, C["cream"])
    b.rect(s4, "S4 Pill", 296, 56, 92, 26, fill_hex=C["sagePill"], stroke_hex=C["rule"], radius=13.0)
    b.text(s4, "S4 Pill T", 296, 61, 92, 16, "4s Breath", FONT_SANS, "Bold", 11.5, 16.0, C["mossBright"], align_h="center")

    apps = [
        ("YouTube", "4s gentle pause · 5 paused", True, C["terracotta"]),
        ("Instagram", "4s gentle pause · 4 paused", True, C["sunGold"]),
        ("TikTok", "6s gentle pause · 2 paused", True, C["mossBright"]),
        ("Chrome", "Unprotected browser", False, C["eucalyptus"]),
    ]
    for idx, (aname, sub, on, dot_col) in enumerate(apps):
        ay = 104.0 + idx * 66.0
        b.rect(s4, f"GApp {idx}", 22, ay, 368, 56, fill_hex=C["pebble"], stroke_hex=C["rule"], radius=22.0)
        b.ellipse(s4, f"GApp Dot {idx}", 38, ay + 21, 14, 14, fill_hex=dot_col, fill_alpha=0.9 if on else 0.3)
        b.text(s4, f"GApp N {idx}", 64, ay + 10, 210, 20, aname, FONT_SANS, "Bold", 15.0, 20.0, C["cream"])
        b.text(s4, f"GApp S {idx}", 64, ay + 31, 220, 16, sub, FONT_SANS, "Regular", 11.5, 16.0, C["eucalyptus"])
        b.rect(s4, f"GApp Sw {idx}", 324, ay + 15, 48, 26, fill_hex=C["mossBright"] if on else C["sagePill"], radius=13.0)
        b.ellipse(s4, f"GApp K {idx}", 349 if on else 327, ay + 18, 20, 20, fill_hex=C["forest"] if on else C["eucalyptus"])

    b.rect(s4, "Duel Peb", 22, 380, 368, 96, fill_hex=C["warmCedar"], stroke_hex=C["warmBorder"], radius=24.0)
    b.text(s4, "Duel K", 40, 396, 220, 14, "TODAY'S CALM COMPARISON", FONT_MONO, "Bold", 10.0, 14.0, C["terracotta"])
    b.text(s4, "Duel Main", 40, 416, 332, 24, "2 bypasses · Avg scroller: 14", FONT_SERIF, "Bold", 17.5, 22.0, C["cream"])
    b.text(s4, "Duel Sub", 40, 444, 332, 18, "86% fewer impulsive overrides today.", FONT_SANS, "Semi Bold", 12.5, 18.0, C["mossBright"])

    b.rect(s4, "Rhythm Card", 22, 492, 368, 308, fill_hex=C["pebble"], stroke_hex=C["rule"], radius=28.0)
    b.text(s4, "Rhy Title", 40, 512, 230, 24, "Weekly Rhythm", FONT_SERIF, "Bold", 18.0, 24.0, C["cream"])
    b.text(s4, "Rhy Tot", 272, 515, 100, 18, "4h 54m saved", FONT_SANS, "Bold", 12.0, 18.0, C["mossBright"], align_h="right")

    bars = [("Mon", 64), ("Tue", 72), ("Wed", 78), ("Thu", 82), ("Fri", 85), ("Sat", 91), ("Sun", 88)]
    for idx, (dlbl, pct) in enumerate(bars):
        bx = 42.0 + idx * 48.0
        bh = round(162.0 * (pct / 100.0), 1)
        by = 720.0 - bh
        b.rect(s4, f"BarBG {idx}", bx, 558, 28, 162, fill_hex=C["forest"], radius=14.0)
        b.rect(s4, f"BarFG {idx}", bx, by, 28, bh, fill_hex=C["mossBright"] if pct >= 80 else C["sunGold"], radius=14.0)
        b.text(s4, f"BarPct {idx}", bx, by - 17, 28, 14, f"{pct}", FONT_SANS, "Bold", 10.5, 14.0, C["eucalyptus"], align_h="center", tnum=True)
        b.text(s4, f"BarDay {idx}", bx, 730, 28, 16, dlbl, FONT_SANS, "Semi Bold", 11.0, 16.0, C["eucalyptus"], align_h="center")

    b.text(s4, "Rhy Note", 40, 760, 332, 18, "Steadily thriving · keep evening hours quiet", FONT_SANS, "Semi Bold", 12.0, 18.0, C["mossBright"], align_h="center")

    _floating_nav(b, s4, 1)

    link_flow(b, cta1, s2)
    link_flow(b, test_cta, s3, transition="dissolve", duration_ms=320)
    link_flow(b, close_cta, s2)
    b.nodes[str(b.page_id)]["flows"] = [
        {"name": "Verdant Nocturnal Flow", "start": s1, "description": "Moonlit Sprout -> Night Terrarium -> Midnight Breath Gate -> Rhythm"}
    ]

    b.add_comment(s1, 206, 240, "Nocturnal Aura", "Botanical gouache isolated with de-fringed neighbor edges into a #2E5443 moonlit canopy aura.")
    b.add_comment(s3, 206, 290, "Midnight Canopy", "Steps down to #0B1712 for maximum eye rest during late-night YouTube urge intercepts.")

    doc = b.finalize_document(
        variables_spec=[
            ("surface/forest", C["forest"], "Deep nocturnal moss linen"),
            ("surface/pebble", C["pebble"], "Elevated forest canopy pebble"),
            ("canopy/midnight", C["canopyDeep"], "Deepest YouTube gate canopy"),
            ("accent/mossBright", C["mossBright"], "Luminous living moss"),
            ("accent/sunGold", C["sunGold"], "Moonlit golden breathing ring"),
        ],
        collection_name="Verdant Nocturnal Tokens",
        mode_name="Nocturnal Canopy",
        doc_colors=[C["forest"], C["pebble"], C["canopyDeep"], C["mossBright"], C["terracotta"], C["sunGold"], C["cream"]],
        annotations_cats=[("aesthetic-system", "Aesthetic System", C["mossBright"])],
    )
    return doc, [s1, s2, s3, s4], b.images
