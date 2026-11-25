"""Option 2 (Dark Mode): Quiet Folio Nocturne — Candlelit Paper, Ivory Ink & Moonlit Indigo."""

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
    "backdrop": "#0C0B09",
    "paper": "#151412",
    "parchment": "#1D1B18",
    "parchmentDeep": "#26231F",
    "plate": "#1D1B18",
    "ink": "#EDE7DA",
    "inkSoft": "#CFC8B9",
    "graphite": "#928B7D",
    "rule": "#2E2B25",
    "ruleDark": "#4A453C",
    "indigo": "#7FA6D4",
    "indigoFill": "#28466B",
    "indigoTint": "#1E2D40",
    "vermilion": "#E06B55",
    "sageInk": "#74B891",
    "obsidian": "#12110F",
}

FONT_SERIF_DISPLAY = "Playfair Display"
FONT_SERIF_BODY = "Source Serif 4"
FONT_MONO = "Liberation Mono"

OUTPUT_FIGCRAFT = ALT_DIR / "02_daylight_paper_dark.figcraft"


def _screen(b: DocBuilder, slot: int, name: str, desc: str) -> int:
    return b.frame(
        b.page_id,
        name,
        screen_x(slot),
        SCREEN_TOP_Y,
        SCREEN_W,
        SCREEN_H,
        fill_hex=C["paper"],
        stroke_hex="#2F2C26",
        stroke_w=1.5,
        radius=34.0,
        clip=True,
        effects=drop_shadow("#000000", 0.45, 0.0, 18.0, 34.0),
        annotations=[{"label": desc, "properties": ["fill", "fontFamily", "stroke"], "category": "aesthetic-system"}],
    )


def _status_bar(b: DocBuilder, s: int) -> None:
    b.text(s, "Status Time", 26, 15, 64, 16, "9:41", FONT_MONO, "Bold", 11.5, 16.0, C["ink"], tnum=True)
    b.text(s, "Status Spec", 224, 16, 162, 14, "NOCTURNE  ·  QUIET MODE", FONT_MONO, "Regular", 9.5, 14.0, C["graphite"], align_h="right")
    b.rule(s, "Top Rule A", 24, 40, 364, C["ruleDark"], thickness=1.0)
    b.rule(s, "Top Rule B", 24, 43, 364, C["rule"], thickness=1.0)


def _nav_bar(b: DocBuilder, s: int, active_idx: int) -> None:
    b.rect(s, "Nav Paper", 0, 828, SCREEN_W, 87, fill_hex=C["paper"])
    b.rule(s, "Nav Double A", 24, 828, 364, C["ruleDark"], thickness=1.0)
    b.rule(s, "Nav Double B", 24, 831, 364, C["rule"], thickness=1.0)
    items = ["Today", "Editions", "Ledger", "Notes"]
    col_w = 364.0 / 4.0
    for idx, label in enumerate(items):
        cx = 24.0 + idx * col_w
        active = idx == active_idx
        b.text(
            s,
            f"Nav {label}",
            cx,
            850,
            col_w,
            20,
            label,
            FONT_SERIF_BODY,
            "Bold" if active else "Regular",
            14.0,
            20.0,
            C["indigo"] if active else C["graphite"],
            align_h="center",
        )
        if active:
            b.ellipse(s, "Nav Active Dot", cx + col_w / 2 - 2.5, 876, 5, 5, fill_hex=C["indigo"])


def build_concept_02_dark() -> tuple[dict[str, Any], list[int], dict[str, tuple[bytes, int, int]]]:
    b = DocBuilder("Option 02 Dark — Quiet Folio Nocturne", C["backdrop"])
    setup_gallery_chrome(
        b,
        "CONCEPT 02 · DARK MODE  //  CANDLELIT ARCHIVAL FOLIO",
        "Quiet Folio: Nocturne Edition",
        "Dark-mode adaptation of Quiet Folio. Replaces stark screen glare with warm charcoal cotton paper, ivory scratchboard etchings, and moonlit slate indigo.",
        "PLAYFAIR DISPLAY + SOURCE SERIF 4 + MONO  //  NOCTURNE PAPER",
        C["backdrop"],
        C["ink"],
        C["graphite"],
        "#302C26",
        [(C["paper"], "#151412"), (C["ink"], "#EDE7DA"), (C["indigo"], "#7FA6D4"), (C["vermilion"], "#E06B55")],
        [
            "I · ONBOARDING · NOCTURNE ESSAY",
            "II · FOLIO · CLARITY BROADSHEET",
            "III · PAUSE SHEET · STILL-WATER GATE",
            "IV · EDITIONS · RULED LEDGER",
        ],
        header_font=FONT_SERIF_DISPLAY,
        body_font=FONT_SERIF_BODY,
        mono_font=FONT_MONO,
    )

    # =========================================================================
    # SCREEN 1: ONBOARDING — ESSAY & LIFE COST
    # =========================================================================
    s1 = _screen(b, 0, "I. Onboarding — Nocturne Essay", "Dark charcoal scratchboard bookplate paired with calm literary reflection on lost time.")
    _status_bar(b, s1)
    b.text(s1, "Chapter Lbl", 24, 56, 220, 14, "CHAPTER II  ·  SILENT DRIFT", FONT_MONO, "Bold", 10.0, 14.0, C["vermilion"])
    b.text(s1, "Folio Num", 280, 56, 108, 14, "PAGE 02 / 07", FONT_MONO, "Regular", 10.0, 14.0, C["graphite"], align_h="right")
    b.text(s1, "S1 Title", 24, 80, 364, 68, "Nine point four\nwaking years.", FONT_SERIF_DISPLAY, "Bold", 28.0, 33.0, C["ink"])

    b.rect(s1, "S1 Bookplate Outer", 24, 158, 364, 242, fill_hex=C["plate"], stroke_hex=C["ruleDark"], stroke_w=1.0, radius=3.0)
    b.clipped_image(
        s1,
        "Folio Hourglass Dark Plate",
        ASSETS_DIR / "folio_hourglass_dark.png",
        98,
        166,
        216,
        200,
        bg_hex=C["plate"],
        stroke_hex=C["rule"],
        stroke_w=0.9,
        radius=2.0,
    )
    b.text(s1, "Fig Caption", 36, 374, 340, 18, "Fig. 1 — At 3h daily, feeds claim 45 full days every year.", FONT_SERIF_BODY, "Italic", 12.0, 17.0, C["graphite"], align_h="center")

    b.text(s1, "Daily Q", 24, 418, 364, 16, "DAILY SHORT-FORM ATTENTION GIVEN", FONT_MONO, "Bold", 10.0, 14.0, C["graphite"])
    b.rule(s1, "Sel Top Rule", 24, 442, 364, C["ruleDark"])
    hours = ["1 hr", "2 hrs", "3 hrs", "4+ hrs"]
    col_w = 364.0 / 4.0
    for idx, h_lbl in enumerate(hours):
        hx = 24.0 + idx * col_w
        sel = h_lbl == "3 hrs"
        if sel:
            b.rect(s1, f"Hr Fill {idx}", hx + 1, 443, col_w - 2, 42, fill_hex=C["indigoTint"])
            b.rect(s1, f"Hr Bar {idx}", hx + 1, 483, col_w - 2, 2.5, fill_hex=C["indigo"])
        b.text(
            s1,
            f"Hr Txt {idx}",
            hx,
            454,
            col_w,
            20,
            h_lbl,
            FONT_SERIF_BODY,
            "Bold" if sel else "Regular",
            14.5,
            20.0,
            C["indigo"] if sel else C["inkSoft"],
            align_h="center",
        )
        if idx < 3:
            b.vrule(s1, f"Hr Div {idx}", hx + col_w, 443, 42, C["rule"])
    b.rule(s1, "Sel Bot Rule", 24, 485, 364, C["ruleDark"])

    b.text(s1, "Goals Head", 24, 506, 364, 16, "WHAT YOU ARE RECLAIMING THIS SPACE FOR", FONT_MONO, "Bold", 10.0, 14.0, C["graphite"])
    goals = [
        ("Deep reading & quiet thinking", True),
        ("Unhurried sleep before midnight", True),
        ("Slow creative work without rush", False),
    ]
    for idx, (gtxt, checked) in enumerate(goals):
        gy = 532.0 + idx * 54.0
        b.rule(s1, f"G Top {idx}", 24, gy, 364, C["rule"])
        b.rect(s1, f"Chk {idx}", 26, gy + 17, 18, 18, fill_hex=C["paper"], stroke_hex=C["indigo"] if checked else C["ruleDark"], stroke_w=1.4, radius=2.0)
        if checked:
            b.rect(s1, f"Chk Mark {idx}", 30, gy + 21, 10, 10, fill_hex=C["indigo"], radius=1.0)
        b.text(s1, f"Goal Txt {idx}", 58, gy + 15, 326, 22, gtxt, FONT_SERIF_BODY, "Bold" if checked else "Regular", 15.0, 22.0, C["ink"] if checked else C["graphite"])
    b.rule(s1, "G End Rule", 24, 694, 364, C["rule"])

    cta1 = b.rect(s1, "S1 Ink CTA", 24, 738, 364, 58, fill_hex=C["indigoFill"], stroke_hex=C["indigo"], stroke_w=1.2, radius=6.0)
    b.text(s1, "S1 CTA Txt", 24, 756, 364, 22, "Turn the page & guard focus", FONT_SERIF_BODY, "Bold", 15.5, 22.0, C["ink"], align_h="center")
    b.text(s1, "S1 Colophon", 24, 814, 364, 16, "Quiet tactile feedback · No dark patterns", FONT_SERIF_BODY, "Italic", 12.5, 16.0, C["graphite"], align_h="center")

    # =========================================================================
    # SCREEN 2: HOME — CLARITY BROADSHEET
    # =========================================================================
    s2 = _screen(b, 1, "II. Folio — Nocturne Broadsheet", "Candlelit broadsheet with luminous ivory score and warm charcoal margin plates.")
    _status_bar(b, s2)
    b.text(s2, "Masthead", 24, 56, 364, 16, "THE QUIET FOLIO", FONT_MONO, "Bold", 11.0, 16.0, C["ink"], align_h="center", letter_spacing_pct=10.0)
    b.text(s2, "Date Sub", 24, 76, 364, 20, "Tuesday Evening  ·  Index No. 14", FONT_SERIF_BODY, "Italic", 14.0, 20.0, C["graphite"], align_h="center")
    b.rule(s2, "Mast Rule A", 24, 106, 364, C["ink"], thickness=1.5)
    b.rule(s2, "Mast Rule B", 24, 110, 364, C["ruleDark"], thickness=1.0)

    b.text(s2, "Clarity Label", 24, 130, 364, 14, "TODAY'S CLARITY INDEX", FONT_MONO, "Bold", 10.0, 14.0, C["indigo"], align_h="center")
    b.text(s2, "Big 88", 24, 146, 364, 96, "88", FONT_SERIF_DISPLAY, "Bold", 82.0, 90.0, C["ink"], align_h="center", tnum=True)
    b.text(s2, "Index Deck", 32, 246, 348, 44, "An unhurried evening. Seven urges let pass\nand 42 quiet minutes returned.", FONT_SERIF_BODY, "Italic", 14.5, 21.0, C["graphite"], align_h="center")

    b.rule(s2, "Stat Top", 24, 314, 364, C["ruleDark"])
    cols = [("07", "PAUSES"), ("42m", "RECLAIMED"), ("04", "SHELVED")]
    c3_w = 364.0 / 3.0
    for idx, (val, lbl) in enumerate(cols):
        cx = 24.0 + idx * c3_w
        b.text(s2, f"Col Val {idx}", cx, 328, c3_w, 34, val, FONT_SERIF_DISPLAY, "Bold", 28.0, 34.0, C["ink"], align_h="center", tnum=True)
        b.text(s2, f"Col Lbl {idx}", cx, 366, c3_w, 14, lbl, FONT_MONO, "Bold", 9.5, 14.0, C["graphite"], align_h="center")
        if idx < 2:
            b.vrule(s2, f"Col VRule {idx}", cx + c3_w, 326, 56, C["rule"])
    b.rule(s2, "Stat Bot", 24, 396, 364, C["ruleDark"])

    b.rect(s2, "Epigraph Plate", 24, 422, 364, 140, fill_hex=C["parchment"])
    b.rule(s2, "Epi Rule Top", 24, 422, 364, C["indigo"], thickness=2.0)
    b.text(s2, "Epi Kicker", 44, 442, 324, 14, "PERSONAL MARGIN NOTE", FONT_MONO, "Bold", 10.0, 14.0, C["indigo"], align_h="center")
    b.text(s2, "Epi Quote", 40, 468, 332, 64, "“Build things that outlast the feed.”", FONT_SERIF_DISPLAY, "Italic", 21.0, 29.0, C["ink"], align_h="center")
    b.rule(s2, "Epi Rule Bot", 24, 562, 364, C["ruleDark"], thickness=1.0)

    b.text(s2, "Ed Kicker", 24, 588, 364, 14, "VOLUNTARY FRICTION ACTIVE", FONT_MONO, "Bold", 10.0, 14.0, C["graphite"])
    b.text(s2, "Ed Summary", 24, 608, 364, 42, "YouTube, Instagram, TikTok, and Reddit pause silently with a 4-second breath before opening.", FONT_SERIF_BODY, "Regular", 14.0, 20.0, C["inkSoft"])

    preview_cta = b.rect(s2, "Preview Sheet CTA", 24, 680, 364, 58, fill_hex=C["ink"], radius=6.0)
    b.text(s2, "Preview Sheet Txt", 24, 698, 364, 22, "Open Pause Sheet Preview", FONT_SERIF_BODY, "Bold", 15.0, 22.0, C["obsidian"], align_h="center")
    b.text(s2, "Status Foot", 24, 752, 364, 18, "Background guard steady · zero battery wakeups", FONT_SERIF_BODY, "Italic", 12.0, 18.0, C["graphite"], align_h="center")
    _nav_bar(b, s2, 0)

    # =========================================================================
    # SCREEN 3: LIVE FRICTION GATE — NOCTURNE PAUSE SHEET
    # =========================================================================
    s3 = _screen(b, 2, "III. Pause Sheet — Still-Water Dark", "Candlelit YouTube pause overlay with ivory-on-obsidian ripple etching and slate-indigo ink reservoir.")
    _status_bar(b, s3)
    b.text(s3, "Pause Meta", 24, 58, 364, 16, "PAUSE SHEET  ·  YOUTUBE  ·  ENTRY NO. 4", FONT_MONO, "Bold", 10.5, 16.0, C["vermilion"], align_h="center")
    b.text(s3, "Pause Headline", 28, 86, 356, 72, "Before you enter\nYouTube.", FONT_SERIF_DISPLAY, "Bold", 28.0, 34.0, C["ink"], align_h="center")

    b.rect(s3, "Still Plate Box", 72, 174, 268, 212, fill_hex=C["plate"], stroke_hex=C["ruleDark"], stroke_w=1.0, radius=4.0)
    b.clipped_image(
        s3,
        "Still Water Dark Plate",
        ASSETS_DIR / "folio_stillwater_dark.png",
        98,
        180,
        216,
        200,
        bg_hex=C["plate"],
        stroke_hex=C["rule"],
        stroke_w=0.9,
        radius=2.0,
    )

    b.text(s3, "Pause Quote", 36, 404, 340, 56, "“Remember what matters tonight:\nprotect your sleep and quiet deep work.”", FONT_SERIF_BODY, "Italic", 15.0, 22.0, C["inkSoft"], align_h="center")
    b.rule(s3, "Pause Mid Rule", 126, 476, 160, C["ruleDark"])

    close_cta = b.rect(s3, "Close YT CTA", 24, 504, 364, 60, fill_hex=C["ink"], radius=6.0)
    b.text(s3, "Close YT Txt", 24, 523, 364, 22, "Return to quiet (Close YouTube)", FONT_SERIF_BODY, "Bold", 15.0, 22.0, C["obsidian"], align_h="center")

    b.text(s3, "Pause Or", 24, 584, 364, 16, "— OR ENTER WITH DELIBERATE INTENT —", FONT_MONO, "Bold", 10.0, 16.0, C["graphite"], align_h="center")

    b.rect(s3, "Ink Hold Well", 24, 616, 364, 72, fill_hex=C["parchment"], stroke_hex=C["indigo"], stroke_w=1.5, radius=6.0)
    b.rect(s3, "Ink Hold Fill", 25.5, 617.5, 174, 69, fill_hex=C["indigoFill"], radius=4.0)
    b.rect(s3, "Ink Edge Line", 198, 617.5, 2.0, 69, fill_hex=C["indigo"])
    b.rect(s3, "Ink Hold Plaque", 66, 634, 280, 36, fill_hex=C["paper"], stroke_hex=C["indigo"], stroke_w=1.0, radius=4.0)
    b.text(s3, "Ink Hold Txt", 66, 643, 280, 18, "Press & hold 4s  ·  1.9s filled", FONT_MONO, "Bold", 11.5, 18.0, C["ink"], align_h="center", tnum=True)

    b.text(s3, "Pause Foot", 36, 712, 340, 42, "Lifting your finger returns the ink cleanly to zero so habit cannot slip through.", FONT_SERIF_BODY, "Italic", 12.5, 18.0, C["graphite"], align_h="center")

    # =========================================================================
    # SCREEN 4: GUARDED EDITIONS & NOCTURNE DOT-AND-STEM LEDGER
    # =========================================================================
    s4 = _screen(b, 3, "IV. Editions — Nocturne Ledger", "Nocturne archival toggle switches with a crisp moonlit dot-and-stem weekly ledger.")
    _status_bar(b, s4)
    b.text(s4, "S4 Title", 24, 54, 260, 32, "Archival Rules", FONT_SERIF_DISPLAY, "Bold", 26.0, 32.0, C["ink"])
    b.text(s4, "S4 Sub", 24, 90, 364, 18, "Select applications granted a deliberate pause.", FONT_SERIF_BODY, "Italic", 13.5, 18.0, C["graphite"])

    b.rule(s4, "Delay Top", 24, 120, 364, C["ruleDark"])
    durs = ["3 sec", "4 sec", "6 sec", "10 sec"]
    dw = 364.0 / 4.0
    for idx, dlbl in enumerate(durs):
        dx = 24.0 + idx * dw
        sel = dlbl == "4 sec"
        if sel:
            b.rect(s4, f"Dur BG {idx}", dx + 1, 121, dw - 2, 36, fill_hex=C["indigoTint"])
            b.rect(s4, f"Dur UL {idx}", dx + 1, 155, dw - 2, 2.5, fill_hex=C["indigo"])
        b.text(s4, f"Dur T {idx}", dx, 129, dw, 20, dlbl, FONT_MONO, "Bold" if sel else "Regular", 11.5, 18.0, C["indigo"] if sel else C["graphite"], align_h="center")
        if idx < 3:
            b.vrule(s4, f"Dur V {idx}", dx + dw, 121, 36, C["rule"])
    b.rule(s4, "Delay Bot", 24, 157, 364, C["ruleDark"])

    apps = [
        ("YouTube", "Pause 4s  ·  5 paused today", True),
        ("Instagram", "Pause 4s  ·  4 paused today", True),
        ("TikTok", "Pause 6s  ·  0 opened today", True),
        ("Chrome", "Unprotected browser", False),
    ]
    for idx, (aname, sub, guarded) in enumerate(apps):
        ay = 178.0 + idx * 64.0
        b.text(s4, f"Ed Name {idx}", 24, ay + 8, 220, 22, aname, FONT_SERIF_DISPLAY, "Bold", 17.0, 22.0, C["ink"])
        b.text(s4, f"Ed Sub {idx}", 24, ay + 32, 240, 18, sub, FONT_SERIF_BODY, "Italic", 12.5, 18.0, C["graphite"])
        b.rect(s4, f"Ed Sw {idx}", 338, ay + 16, 48, 24, fill_hex=C["indigo"] if guarded else C["parchmentDeep"], stroke_hex=C["indigo"] if guarded else C["ruleDark"], radius=12.0)
        b.ellipse(s4, f"Ed Knob {idx}", 364 if guarded else 341, ay + 19, 18, 18, fill_hex=C["paper"] if guarded else C["graphite"])
        b.rule(s4, f"Ed Rule {idx}", 24, ay + 60, 364, C["rule"])

    b.rect(s4, "Ledger Card", 24, 456, 364, 344, fill_hex=C["parchment"], stroke_hex=C["ruleDark"], radius=4.0)
    b.text(s4, "Ledger Kicker", 42, 474, 210, 14, "SEVEN-DAY CLARITY LEDGER", FONT_MONO, "Bold", 10.0, 14.0, C["graphite"])
    b.text(s4, "Ledger Duel", 220, 474, 150, 14, "YOU 02  ·  AVG 14", FONT_MONO, "Bold", 10.0, 14.0, C["indigo"], align_h="right")
    b.text(s4, "Ledger Headline", 42, 494, 328, 24, "Steadier each evening.", FONT_SERIF_DISPLAY, "Bold", 18.0, 24.0, C["ink"])

    for pct_y, pct_lbl in [(540, "100"), (576, "75"), (612, "50"), (648, "25")]:
        b.rect(s4, f"Grid {pct_lbl}", 68, pct_y, 296, 1.0, fill_hex=C["rule"], dashes=[3.0, 3.0])
        b.text(s4, f"Grid L {pct_lbl}", 38, pct_y - 6, 24, 12, pct_lbl, FONT_MONO, "Regular", 8.5, 12.0, C["graphite"], align_h="right", tnum=True)

    b.rule(s4, "X Axis", 68, 684, 296, C["ruleDark"])
    pts = [("M", 64), ("T", 72), ("W", 78), ("T", 82), ("F", 85), ("S", 91), ("S", 88)]
    for idx, (dlbl, val) in enumerate(pts):
        sx_pos = 88.0 + idx * 42.0
        stem_h = round(144.0 * (val / 100.0), 1)
        top_y = 684.0 - stem_h
        b.vrule(s4, f"Stem {idx}", sx_pos, top_y, stem_h, C["indigo"], thickness=2.0)
        dot_col = C["sageInk"] if val >= 85 else C["indigo"]
        b.ellipse(s4, f"Dot {idx}", sx_pos - 5, top_y - 6, 12, 12, fill_hex=dot_col, stroke_hex=C["parchment"], stroke_w=2.0)
        b.text(s4, f"XDay {idx}", sx_pos - 10, 692, 22, 14, dlbl, FONT_MONO, "Bold", 10.0, 14.0, C["graphite"], align_h="center")

    b.rule(s4, "Ledger Foot Rule", 42, 722, 328, C["rule"])
    b.text(s4, "Ledger Sum L", 42, 738, 180, 18, "Weekly quiet hours saved", FONT_SERIF_BODY, "Italic", 13.0, 18.0, C["graphite"])
    b.text(s4, "Ledger Sum LV", 42, 758, 180, 26, "4 hrs 54 mins", FONT_SERIF_DISPLAY, "Bold", 19.0, 26.0, C["ink"], tnum=True)
    b.text(s4, "Ledger Sum R", 240, 742, 130, 14, "STREAK HELD", FONT_MONO, "Bold", 9.5, 14.0, C["sageInk"], align_h="right")
    b.text(s4, "Ledger Sum RV", 240, 760, 130, 24, "7 Days", FONT_SERIF_DISPLAY, "Bold", 19.0, 24.0, C["sageInk"], align_h="right")

    _nav_bar(b, s4, 1)

    link_flow(b, cta1, s2)
    link_flow(b, preview_cta, s3, transition="dissolve", duration_ms=280)
    link_flow(b, close_cta, s2)
    b.nodes[str(b.page_id)]["flows"] = [
        {"name": "Quiet Folio Nocturne Flow", "start": s1, "description": "Nocturne Essay -> Broadsheet -> Still-Water Pause -> Ledger"}
    ]

    b.add_comment(s1, 206, 260, "Nocturne Engraving", "Candlelit charcoal paper paired with inverted ivory-on-obsidian woodcut plate prevents blue-light shock.")
    b.add_comment(s3, 206, 650, "Night Legibility", "Deep slate-indigo reservoir and ivory pause plaque retain print-like contrast after dark.")

    doc = b.finalize_document(
        variables_spec=[
            ("surface/nocturnePaper", C["paper"], "Warm obsidian cotton paper"),
            ("ink/ivory", C["ink"], "Antique ivory ink"),
            ("accent/moonIndigo", C["indigo"], "Moonlit slate indigo"),
            ("accent/vermilion", C["vermilion"], "Warm vermilion marginalia"),
        ],
        collection_name="Quiet Folio Nocturne Tokens",
        mode_name="Nocturne Rag Paper",
        doc_colors=[C["paper"], C["parchment"], C["ink"], C["graphite"], C["indigo"], C["vermilion"], C["sageInk"]],
        annotations_cats=[("aesthetic-system", "Aesthetic System", C["indigo"])],
    )
    return doc, [s1, s2, s3, s4], b.images
