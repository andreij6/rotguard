"""Shared tokens and plain-language UI primitives for RotGuard's 16-screen FigCraft architecture."""

from __future__ import annotations

import math
from typing import Any

from fc_core import (
    GALLERY_PAD_X,
    GALLERY_W,
    SCREEN_GAP_X,
    SCREEN_H,
    SCREEN_W,
    DocBuilder,
    drop_shadow,
    make_radii,
    solid,
)

FONT_SERIF = "Merriweather"
FONT_SANS = "Nunito Sans"
FONT_MONO = "Liberation Mono"

ROW_W = GALLERY_W
ROW_H = 1008.0
ROW_SCREEN_Y = 44.0
ROW_CAPTION_Y = 972.0

THEMES: dict[str, dict[str, str]] = {
    "light": {
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
        "terracottaSoft": "#F4E5DE",
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
    },
    "dark": {
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
        "terracottaSoft": "#35211A",
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
    },
}

# Clean 20-variation prompt pairs: opening message -> second-thought message before timer finishes.
# Rules & rotation happen silently without surfacing timing internals in the UI.
TWENTY_BLOCKER_VARIATIONS: list[tuple[int, str, str, str]] = [
    (1, "Pause", "Take one slow breath before opening.", "Still want to open it? Release to close right now."),
    (2, "Goals", "Will tomorrow morning thank you for this tap?", "Keep holding only if this actually helps your goal."),
    (3, "Time", "The average quick check turns into 19 minutes.", "Letting go right now gives those 19 minutes back."),
    (4, "Inspiration", "Pause before opening YouTube.", "Still want YouTube? Lift your thumb to close it."),
    (5, "Habit", "Notice how fast your thumb opened this app.", "You already broke the reflex — you can stop here."),
    (6, "Sleep", "Protect tonight's sleep before your head hits the pillow.", "One lift now means earlier, better rest tonight."),
    (7, "Focus", "Spend curiosity on creating, not endless scrolling.", "Close before the feed pulls away your best focus."),
    (8, "Pause", "Drop your shoulders. Unclench your jaw. Exhale.", "Still continuing? Make sure it's an intentional choice."),
    (9, "Goals", "What could you finish in the next 15 uninterrupted minutes?", "Closing takes zero effort. Opening costs attention."),
    (10, "Time", "At 3 hours a day, short feeds take 45 full days a year.", "Skip this scroll session and keep your evening yours."),
    (11, "Inspiration", "Look at your reminder photo above.", "Choose what's in that photo over another random feed."),
    (12, "Habit", "Did you open this app with a specific purpose?", "If you're only bored, closing now feels much better."),
    (13, "Pause", "Four seconds of pause beats twenty minutes lost.", "Release anytime if you changed your mind."),
    (14, "Sleep", "Fast video feeds keep your brain wired late at night.", "Step away now and give your eyes a break."),
    (15, "Focus", "Give your mind a quiet moment instead of noise.", "Lift your finger to protect that uninterrupted space."),
    (16, "Time", "Every blocked urge makes focus easier tomorrow.", "Walk away now to keep your blocked streak climbing."),
    (17, "Goals", "Your top goal today is worth more than one video.", "Confirm you truly want this break before entering."),
    (18, "Habit", "Boredom passes in seconds; scrolling lasts for hours.", "Ride out the final moments — or close it free."),
    (19, "Inspiration", "The life you're working toward happens off screen.", "Release gently and go back to what matters today."),
    (20, "Pause", "Check in before the algorithm picks your next mood.", "Last moment before open: release to exit cleanly."),
]


def slot_x(slot: int) -> float:
    return GALLERY_PAD_X + slot * (SCREEN_W + SCREEN_GAP_X)


def create_screen(
    b: DocBuilder,
    parent_id: int,
    slot: int,
    y: float,
    name: str,
    fill_hex: str,
    t: dict[str, str],
    desc: str,
) -> int:
    return b.frame(
        parent_id,
        name,
        slot_x(slot),
        y,
        SCREEN_W,
        SCREEN_H,
        fill_hex=fill_hex,
        stroke_hex=t["screenStroke"] if fill_hex == t["canvas"] else t["canopyStroke"],
        stroke_w=1.5,
        radius=36.0,
        clip=True,
        effects=drop_shadow(t["shadowHex"], float(t["shadowAlpha"]), 0.0, 14.0, 30.0),
        annotations=[{"label": desc, "properties": ["fill", "cornerRadius", "stroke"], "category": "rotguard-design"}],
    )


def draw_status_bar(b: DocBuilder, s: int, t: dict[str, str], on_canopy: bool = False) -> None:
    ink = t["cream"] if on_canopy else t["ink"]
    muted = t["canopyMuted"] if on_canopy else t["inkMuted"]
    dot = t["sunGold"] if on_canopy else t["mossAccent"]
    b.text(s, "Status Time", 26, 16, 60, 18, "9:41", FONT_SANS, "Bold", 14.0, 18.0, ink, tnum=True)
    b.ellipse(s, "Status Dot", 272, 21, 8, 8, fill_hex=dot)
    b.text(s, "Status Spec", 284, 17, 102, 16, "Blocker Active", FONT_SANS, "Semi Bold", 11.5, 16.0, muted, align_h="right")


def draw_ruled_bottom_nav(b: DocBuilder, s: int, t: dict[str, str], active_idx: int) -> None:
    """Grounded double-ruled bottom navigation with plain tab labels."""
    b.rect(s, "Nav Canvas BG", 0, 828, SCREEN_W, 87, fill_hex=t["canvas"])
    b.rule(s, "Nav Double Top", 24, 828, 364, t["ruleStrong"], thickness=1.0)
    b.rule(s, "Nav Double Bot", 24, 831, 364, t["rule"], thickness=1.0)
    items = ["Home", "Apps", "Insights", "Settings"]
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
            13.5,
            20.0,
            t["navActiveInk"] if act else t["inkMuted"],
            align_h="center",
        )
        if act:
            b.ellipse(s, "Nav Active Dot", cx + cw / 2.0 - 2.5, 876, 5.0, 5.0, fill_hex=t["navActiveInk"])


def draw_square_cta(
    b: DocBuilder,
    s: int,
    name: str,
    x: float,
    y: float,
    w: float,
    h: float,
    label: str,
    fill_hex: str,
    ink_hex: str,
    stroke_hex: str | None = None,
    shadow_hex: str | None = None,
) -> int:
    btn = b.rect(
        s,
        name,
        x,
        y,
        w,
        h,
        fill_hex=fill_hex,
        stroke_hex=stroke_hex,
        stroke_w=1.2 if stroke_hex else 0.0,
        radius=6.0,
        effects=drop_shadow(shadow_hex or "#09120E", 0.25, 0.0, 6.0, 16.0),
    )
    b.text(s, f"{name} Txt", x, y + (h - 20.0) / 2.0, w, 20.0, label, FONT_SANS, "Bold", 15.0, 20.0, ink_hex, align_h="center")
    return btn


def draw_square_hold_reservoir(
    b: DocBuilder,
    s: int,
    x: float,
    y: float,
    w: float,
    h: float,
    pct: float,
    label: str,
    well_hex: str,
    stroke_hex: str,
    plaque_hex: str,
    text_hex: str,
) -> None:
    """Crisp 6px rectangular hold track with advancing fill and centered 4px label plaque."""
    b.rect(s, "Hold Reservoir Well", x, y, w, h, fill_hex=well_hex, stroke_hex=stroke_hex, stroke_w=1.5, radius=6.0)
    if pct > 0.0:
        fill_w = max(8.0, (w - 3.0) * pct)
        b.rect(s, "Hold Reservoir Fill", x + 1.5, y + 1.5, fill_w, h - 3.0, fill_hex=stroke_hex, fill_alpha=0.30, radius=4.0)
        b.rect(s, "Hold Meniscus Line", x + 1.5 + fill_w - 2.0, y + 1.5, 2.0, h - 3.0, fill_hex=stroke_hex)
    pw = 290.0
    ph = 36.0
    px = x + (w - pw) / 2.0
    py = y + (h - ph) / 2.0
    b.rect(s, "Hold Label Plaque", px, py, pw, ph, fill_hex=plaque_hex, stroke_hex=stroke_hex, stroke_w=1.0, radius=4.0)
    b.text(s, "Hold Label Txt", px, py + 9.0, pw, 18.0, label, FONT_MONO, "Bold", 11.0, 18.0, text_hex, align_h="center", tnum=True)


def draw_rotated_rect(
    b: DocBuilder,
    parent_id: int,
    name: str,
    center_x: float,
    center_y: float,
    w: float,
    h: float,
    angle_rad: float,
    fill_hex: str,
    radius: float = 2.0,
) -> int:
    """Place a rectangle rotated cleanly around its center in FigCraft's affine convention."""
    ca = math.cos(angle_rad)
    sa = math.sin(angle_rad)
    a, b_m, c, d = ca, sa, -sa, ca
    tx = center_x - (a * (w * 0.5) + c * (h * 0.5))
    ty = center_y - (b_m * (w * 0.5) + d * (h * 0.5))
    nid = b.alloc_id()
    node: dict[str, Any] = {
        "id": nid,
        "name": name,
        "type": "rectangle",
        "transform": [round(a, 6), round(b_m, 6), round(c, 6), round(d, 6), round(tx, 4), round(ty, 4)],
        "width": float(w),
        "height": float(h),
        "fills": solid(fill_hex),
        "corners": {"radii": make_radii(radius), "smoothing": 0.4},
        "blend": "normal",
    }
    return b.add_node(node, parent_id)


def draw_inline_celebration_art(b: DocBuilder, s: int, cx: float, cy: float, t: dict[str, str], is_dark: bool) -> None:
    """Open-canvas sunlit aurora glow, rising botanical embers & glowing check orb (no modal box)."""
    aurora_moss = "#2D6D4C" if is_dark else "#2F6F4E"
    orb_ring = t["sunGold"]
    orb_fill = "#24573D" if not is_dark else "#1D4632"

    # Layered translucent aurora halos seamlessly lighting the open canopy
    b.ellipse(s, "Aurora Wide", cx - 150, cy - 150, 300, 300, fill_hex=aurora_moss, fill_alpha=0.14)
    b.ellipse(s, "Aurora Warm", cx - 114, cy - 114, 228, 228, fill_hex=orb_ring, fill_alpha=0.10)
    b.ellipse(s, "Aurora Mid", cx - 84, cy - 84, 168, 168, fill_hex=aurora_moss, fill_alpha=0.22)
    b.ellipse(s, "Bloom Orbit Hairline", cx - 68, cy - 68, 136, 136, stroke_hex=orb_ring, stroke_alpha=0.55, stroke_w=1.2, dashes=[4.0, 6.0])

    # Organic rising golden embers & floating sage sparks drifting upward across upper canopy
    embers = [
        (-86, -54, 8, orb_ring, 0.90),
        (-112, -16, 5, "#74C493", 0.70),
        (-74, -108, 6, "#74C493", 0.85),
        (-36, -124, 9, orb_ring, 0.92),
        (12, -138, 5, "#FFFFFF", 0.55),
        (52, -118, 7, orb_ring, 0.88),
        (92, -88, 10, "#74C493", 0.85),
        (118, -34, 6, orb_ring, 0.78),
        (82, -16, 5, "#FFFFFF", 0.50),
        (-124, 34, 6, orb_ring, 0.55),
        (126, 26, 5, "#74C493", 0.60),
        (-94, 68, 7, "#74C493", 0.72),
        (94, 66, 8, orb_ring, 0.75),
        (-48, 96, 5, "#FFFFFF", 0.45),
        (46, 98, 6, "#74C493", 0.65),
        (0, -102, 7, orb_ring, 0.95),
    ]
    for idx, (dx, dy, sz, col, al) in enumerate(embers):
        b.ellipse(s, f"Ember {idx}", cx + dx - sz * 0.5, cy + dy - sz * 0.5, sz, sz, fill_hex=col, fill_alpha=al)

    # Central glowing checkmark badge (pure vector/geometric symmetry)
    b.ellipse(
        s,
        "Check Orb",
        cx - 46,
        cy - 46,
        92,
        92,
        fill_hex=orb_fill,
        stroke_hex=orb_ring,
        stroke_w=2.0,
        effects=drop_shadow(orb_ring, 0.38, 0.0, 0.0, 22.0),
    )
    # Vertex at (cx - 4, cy + 12) so left & right arms meet cleanly
    vx, vy = cx - 4.0, cy + 11.0
    draw_rotated_rect(b, s, "Check Arm L", vx - 5.2, vy - 5.2, 20.0, 5.2, math.pi / 4.0, "#F6F5EE", radius=2.5)
    draw_rotated_rect(b, s, "Check Arm R", vx + 11.0, vy - 11.0, 36.0, 5.2, -math.pi / 4.0, "#F6F5EE", radius=2.5)
