#!/usr/bin/env python3
"""
Deterministic FigCraft 0.10.0 generator for RotGuard's 8-screen Android design system.

Creates `design/rotguard-app.figcraft`
with:
  - Warm Obsidian & Tactile Amber design tokens (inspired by brainpalapp.ai)
  - Embedded Nano Banana 3D mascot illustrations (`images/<sha256>` + `imageIndex`)
  - 8 Android screens (412 x 915 px) in a clean 2x4 gallery grid
  - 16 native anchored FigCraft comments + Dev-Mode annotations
  - Interactive prototype flows across onboarding, friction shield, and dashboard
  - FigCraft --control roundtrip (`thumbnail.png` + 2x PNG renders)
"""

from __future__ import annotations

import argparse
import hashlib
import json
import socket
import struct
import subprocess
import time
import zipfile
from pathlib import Path
from typing import Any

ROOT_DIR = Path(__file__).resolve().parents[1]
DESIGN_DIR = ROOT_DIR / "design"
ASSETS_DIR = DESIGN_DIR / "assets"
PREVIEWS_DIR = DESIGN_DIR / "previews"
OUTPUT_FIGCRAFT = DESIGN_DIR / "rotguard-app.figcraft"
FIGCRAFT_BIN = Path("/Applications/FigCraft.app/Contents/MacOS/FigCraft")

SCREEN_W = 412.0
SCREEN_H = 915.0
BASE_TS = 1764000000

FONT_DISPLAY = "Poppins"
FONT_SANS = "Inter"
FONT_MONO = "Liberation Mono"

COLORS = {
    "bg.canvas": "#0A0907",
    "bg.surface": "#14130F",
    "bg.surfaceElevated": "#1D1B16",
    "bg.surfaceDeep": "#0F0E0B",
    "border.hairline": "#2E2921",
    "border.warm": "#4A3A24",
    "accent.cream": "#FFE9B4",
    "accent.creamDeep": "#FFD99A",
    "accent.apricot": "#FFB367",
    "accent.lip": "#B8732C",
    "accent.amberGlow": "#713A00",
    "accent.amberSoft": "#291B0C",
    "ink.onCream": "#2B1600",
    "status.thriving": "#79D056",
    "status.thrivingSoft": "#192914",
    "status.warning": "#FFB367",
    "status.rot": "#FC4A52",
    "status.rotSoft": "#321315",
    "text.primary": "#FCFFFA",
    "text.secondary": "#B8B4A9",
    "text.muted": "#7D786D",
    "page.bg": "#060504",
    "white": "#FFFFFF",
}


def hex_to_rgba(hex_str: str, alpha: float = 1.0) -> dict[str, float]:
    h = hex_str.lstrip("#")
    return {
        "r": round(int(h[0:2], 16) / 255.0, 6),
        "g": round(int(h[2:4], 16) / 255.0, 6),
        "b": round(int(h[4:6], 16) / 255.0, 6),
        "a": round(alpha, 6),
    }


def solid(hex_str: str, alpha: float = 1.0) -> list[dict[str, Any]]:
    return [
        {
            "type": "solid",
            "color": hex_to_rgba(hex_str, alpha),
            "opacity": 1.0,
            "visible": True,
            "blend": "normal",
        }
    ]


def radial_glow(hex_str: str, center_alpha: float = 0.45) -> list[dict[str, Any]]:
    return [
        {
            "type": "gradient",
            "kind": "radial",
            "stops": [
                {"position": 0.0, "color": hex_to_rgba(hex_str, center_alpha)},
                {"position": 1.0, "color": hex_to_rgba(COLORS["bg.canvas"], 0.0)},
            ],
            "from": [0.5, 0.3],
            "to": [0.5, 1.0],
            "width": [0.5, 0.0],
            "opacity": 1.0,
            "visible": True,
            "blend": "normal",
        }
    ]


def image_paint(image_hash: str) -> list[dict[str, Any]]:
    return [
        {
            "type": "image",
            "image": image_hash,
            "scale_mode": "fill",
            "scale": 1.0,
            "rotation": 0,
            "crop": None,
            "filters": {
                "exposure": 0.0,
                "contrast": 0.0,
                "saturation": 0.0,
                "temperature": 0.0,
                "tint": 0.0,
                "highlights": 0.0,
                "shadows": 0.0,
            },
            "opacity": 1.0,
            "visible": True,
            "blend": "normal",
        }
    ]


def drop_shadow(hex_str: str = "#000000", alpha: float = 0.35, dy: float = 10.0, radius: float = 24.0) -> list[dict[str, Any]]:
    return [
        {
            "type": "dropShadow",
            "color": hex_to_rgba(hex_str, alpha),
            "offset": [0.0, dy],
            "radius": radius,
            "spread": 0.0,
            "blend": "normal",
            "show_behind": False,
            "visible": True,
        }
    ]


class DocBuilder:
    def __init__(self) -> None:
        self.nodes: dict[str, dict[str, Any]] = {}
        self.next_id = 1
        self.images: dict[str, tuple[bytes, int, int]] = {}
        self.image_index: list[dict[str, Any]] = []
        self.comments: list[dict[str, Any]] = []
        self.comment_id = 1

    def alloc_id(self) -> int:
        nid = self.next_id
        self.next_id += 1
        return nid

    def register_png(self, path: Path) -> str:
        raw = path.read_bytes()
        digest = hashlib.sha256(raw).hexdigest()
        if digest not in self.images:
            w, h = struct.unpack(">II", raw[16:24])
            self.images[digest] = (raw, int(w), int(h))
            self.image_index.append({"hash": digest, "width": int(w), "height": int(h)})
        return digest

    def add_node(self, node: dict[str, Any], parent_id: int | None = None) -> int:
        nid = node["id"]
        self.nodes[str(nid)] = node
        if parent_id is not None:
            node["parent"] = parent_id
            self.nodes[str(parent_id)].setdefault("children", []).append(nid)
        return nid

    def frame(
        self,
        parent_id: int,
        name: str,
        x: float,
        y: float,
        w: float,
        h: float,
        fill_hex: str | None = None,
        fill_alpha: float = 1.0,
        fills_override: list[dict[str, Any]] | None = None,
        stroke_hex: str | None = None,
        stroke_w: float = 1.0,
        radius: float = 0.0,
        clip: bool = False,
        effects: list[dict[str, Any]] | None = None,
        annotations: list[dict[str, Any]] | None = None,
        interactions: list[dict[str, Any]] | None = None,
    ) -> int:
        nid = self.alloc_id()
        node: dict[str, Any] = {
            "id": nid,
            "name": name,
            "type": "frame",
            "children": [],
            "transform": [1.0, 0.0, 0.0, 1.0, float(x), float(y)],
            "width": float(w),
            "height": float(h),
            "blend": "normal",
        }
        if clip:
            node["clip"] = True
        if fills_override is not None:
            node["fills"] = fills_override
        elif fill_hex is not None:
            node["fills"] = solid(fill_hex, fill_alpha)
        if stroke_hex is not None:
            node["strokes"] = solid(stroke_hex)
            node["stroke"] = {
                "weight": float(stroke_w),
                "align": "inside",
                "sides": None,
                "cap_start": "round",
                "cap_end": "round",
                "join": "round",
                "miter_limit": 4.0,
                "dashes": [],
            }
        if radius > 0:
            node["corners"] = {"radii": [radius, radius, radius, radius], "smoothing": 0.6}
        if effects:
            node["effects"] = effects
        if annotations:
            node["annotations"] = annotations
        if interactions:
            node["interactions"] = interactions
        return self.add_node(node, parent_id)

    def rect(
        self,
        parent_id: int,
        name: str,
        x: float,
        y: float,
        w: float,
        h: float,
        fill_hex: str | None = None,
        fill_alpha: float = 1.0,
        fills_override: list[dict[str, Any]] | None = None,
        stroke_hex: str | None = None,
        stroke_w: float = 1.0,
        radius: float = 0.0,
        effects: list[dict[str, Any]] | None = None,
    ) -> int:
        nid = self.alloc_id()
        node: dict[str, Any] = {
            "id": nid,
            "name": name,
            "type": "rectangle",
            "transform": [1.0, 0.0, 0.0, 1.0, float(x), float(y)],
            "width": float(w),
            "height": float(h),
            "blend": "normal",
        }
        if fills_override is not None:
            node["fills"] = fills_override
        elif fill_hex is not None:
            node["fills"] = solid(fill_hex, fill_alpha)
        if stroke_hex is not None:
            node["strokes"] = solid(stroke_hex)
            node["stroke"] = {
                "weight": float(stroke_w),
                "align": "inside",
                "sides": None,
                "cap_start": "round",
                "cap_end": "round",
                "join": "round",
                "miter_limit": 4.0,
                "dashes": [],
            }
        if radius > 0:
            node["corners"] = {"radii": [radius, radius, radius, radius], "smoothing": 0.4}
        if effects:
            node["effects"] = effects
        return self.add_node(node, parent_id)

    def ellipse(
        self,
        parent_id: int,
        name: str,
        x: float,
        y: float,
        w: float,
        h: float,
        fill_hex: str | None = None,
        fill_alpha: float = 1.0,
        stroke_hex: str | None = None,
        stroke_w: float = 2.0,
        inner: float = 0.0,
        sweep_ratio: float = 1.0,
    ) -> int:
        nid = self.alloc_id()
        node: dict[str, Any] = {
            "id": nid,
            "name": name,
            "type": "ellipse",
            "transform": [1.0, 0.0, 0.0, 1.0, float(x), float(y)],
            "width": float(w),
            "height": float(h),
            "start": -1.5707963267948966,
            "sweep": 6.283185307179586 * sweep_ratio,
            "inner": float(inner),
            "blend": "normal",
        }
        if fill_hex is not None:
            node["fills"] = solid(fill_hex, fill_alpha)
        if stroke_hex is not None:
            node["strokes"] = solid(stroke_hex)
            node["stroke"] = {
                "weight": float(stroke_w),
                "align": "inside",
                "sides": None,
                "cap_start": "round",
                "cap_end": "round",
                "join": "round",
                "miter_limit": 4.0,
                "dashes": [],
            }
        return self.add_node(node, parent_id)

    def text(
        self,
        parent_id: int,
        name: str,
        x: float,
        y: float,
        w: float,
        h: float,
        characters: str,
        font_family: str = FONT_SANS,
        font_style: str = "Regular",
        font_size: float = 14.0,
        line_height_px: float | None = None,
        color_hex: str = COLORS["text.primary"],
        align_h: str = "left",
        tnum: bool = False,
    ) -> int:
        nid = self.alloc_id()
        lh = line_height_px if line_height_px is not None else round(font_size * 1.35, 1)
        node: dict[str, Any] = {
            "id": nid,
            "name": name,
            "type": "text",
            "characters": characters,
            "style": {
                "font_family": font_family,
                "font_style": font_style,
                "font_size": float(font_size),
                "line_height": {"unit": "pixels", "value": float(lh)},
                "letter_spacing": {"unit": "percent", "value": -1.0 if font_size >= 22 else 0.0},
                "decoration": "none",
                "case": "original",
                "fills": None,
                "link": None,
            },
            "align_h": align_h,
            "auto_resize": "height",
            "transform": [1.0, 0.0, 0.0, 1.0, float(x), float(y)],
            "width": float(w),
            "height": float(h),
            "fills": solid(color_hex),
        }
        if tnum:
            node["features"] = {"tnum": True}
        return self.add_node(node, parent_id)

    def mascot(self, parent_id: int, name: str, asset_name: str, x: float, y: float, size: float, glow_hex: str) -> int:
        img_hash = self.register_png(ASSETS_DIR / asset_name)
        self.ellipse(
            parent_id,
            f"{name} Glow",
            x - 16,
            y - 16,
            size + 32,
            size + 32,
            fill_hex=glow_hex,
            fill_alpha=0.14,
        )
        return self.rect(
            parent_id,
            name,
            x,
            y,
            size,
            size,
            fills_override=image_paint(img_hash),
            radius=28.0,
        )

    def add_comment(
        self,
        page_id: int,
        anchor_id: int,
        local_x: float,
        local_y: float,
        author: str,
        text: str,
        reply_author: str | None = None,
        reply_text: str | None = None,
    ) -> None:
        replies = []
        if reply_author and reply_text:
            replies.append(
                {
                    "author": reply_author,
                    "text": reply_text,
                    "created": BASE_TS + self.comment_id * 45 + 15,
                    "edited": 0,
                    "reactions": [],
                }
            )
        self.comments.append(
            {
                "id": self.comment_id,
                "page": page_id,
                "x": float(local_x),
                "y": float(local_y),
                "anchor": anchor_id,
                "author": author,
                "text": text,
                "created": BASE_TS + self.comment_id * 45,
                "edited": 0,
                "reactions": [{"emoji": "✨", "users": [author]}],
                "resolved": False,
                "replies": replies,
                "read_by": [author],
            }
        )
        self.comment_id += 1


def draw_status_bar(b: DocBuilder, screen_id: int, step_pill: str | None = None) -> None:
    b.text(screen_id, "Status Time", 24, 16, 60, 18, "9:41", FONT_SANS, "Semi Bold", 13.0, 18.0, COLORS["text.primary"], tnum=True)
    b.rect(screen_id, "Signal Pill", 342, 20, 46, 10, fill_hex=COLORS["text.muted"], fill_alpha=0.45, radius=5.0)
    if step_pill:
        b.rect(screen_id, "Top Badge BG", 24, 52, 160, 26, fill_hex=COLORS["accent.amberSoft"], stroke_hex=COLORS["border.warm"], radius=13.0)
        b.text(screen_id, "Top Badge", 36, 57, 136, 16, step_pill, FONT_MONO, "Bold", 11.0, 16.0, COLORS["accent.cream"])


def draw_tactile_cta(
    b: DocBuilder,
    parent_id: int,
    name: str,
    x: float,
    y: float,
    w: float,
    label: str,
    bg_hex: str = COLORS["accent.cream"],
    lip_hex: str = COLORS["accent.lip"],
    ink_hex: str = COLORS["ink.onCream"],
) -> int:
    b.rect(parent_id, f"{name} Lip", x, y + 5, w, 54, fill_hex=lip_hex, radius=27.0)
    btn = b.rect(parent_id, name, x, y, w, 54, fill_hex=bg_hex, radius=27.0, effects=drop_shadow(COLORS["accent.apricot"], 0.16, 10.0, 24.0))
    b.text(parent_id, f"{name} Label", x + 16, y + 16, w - 32, 22, label, FONT_DISPLAY, "Bold", 16.0, 22.0, ink_hex, align_h="center")
    return btn


def draw_bottom_nav(b: DocBuilder, screen_id: int, active_idx: int) -> None:
    labels = ["Home", "Apps", "Insights", "Settings"]
    b.rect(screen_id, "Bottom Nav BG", 16, 832, 380, 66, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.hairline"], radius=24.0)
    slot_w = 364.0 / 4.0
    for idx, label in enumerate(labels):
        tx = 24.0 + idx * slot_w
        if idx == active_idx:
            b.rect(screen_id, f"Nav Active {label}", tx + 4, 840, slot_w - 8, 50, fill_hex=COLORS["accent.amberSoft"], stroke_hex=COLORS["border.warm"], radius=18.0)
            color = COLORS["accent.cream"]
            weight = "Bold"
        else:
            color = COLORS["text.muted"]
            weight = "Medium"
        b.text(screen_id, f"Nav {label}", tx, 856, slot_w, 18, label, FONT_SANS, weight, 12.0, 18.0, color, align_h="center")


def create_screen(b: DocBuilder, page_id: int, col: int, row: int, name: str, note: str, category: str) -> int:
    x = col * 480.0
    y = row * 1020.0
    sid = b.frame(
        page_id,
        name,
        x,
        y,
        SCREEN_W,
        SCREEN_H,
        fill_hex=COLORS["bg.canvas"],
        stroke_hex=COLORS["border.hairline"],
        stroke_w=1.5,
        radius=40.0,
        clip=True,
        effects=drop_shadow("#000000", 0.55, 24.0, 48.0),
        annotations=[{"label": note, "properties": ["fill", "cornerRadius", "fontSize"], "category": category}],
    )
    b.rect(sid, "Ambient Warm Glow", 0, 0, SCREEN_W, 300, fills_override=radial_glow(COLORS["accent.amberGlow"], 0.45))
    return sid


def build_rotguard_document() -> tuple[dict[str, Any], list[int], dict[str, tuple[bytes, int, int]]]:
    b = DocBuilder()
    page_id = b.alloc_id()
    b.add_node(
        {
            "id": page_id,
            "name": "RotGuard Android Suite",
            "type": "page",
            "background": hex_to_rgba(COLORS["page.bg"]),
            "flows": [],
            "guides": [],
            "children": [],
            "width": 0.0,
            "height": 0.0,
        }
    )

    # 1. Onboarding — Feel The Rot
    s1 = create_screen(
        b, page_id, 0, 0, "01 Onboarding — Feel The Rot",
        "Interactive 5-stage brain decay simulator pairs Nano Banana clay brain evolution with haptic shudders.",
        "haptic-choreography",
    )
    draw_status_bar(b, s1, "STEP 2 OF 7")
    b.rect(s1, "Progress Track", 24, 90, 364, 4, fill_hex=COLORS["border.hairline"], radius=2.0)
    b.rect(s1, "Progress Fill", 24, 90, 104, 4, fill_hex=COLORS["accent.cream"], radius=2.0)
    b.text(s1, "Title", 24, 110, 364, 72, "See what endless scrolling does.", FONT_DISPLAY, "Bold", 28.0, 34.0, COLORS["text.primary"])
    b.text(s1, "Subtitle", 24, 190, 364, 40, "Tap any stage to feel the brainrot haptic pulse shift in real time.", FONT_SANS, "Regular", 14.0, 20.0, COLORS["text.secondary"])
    b.mascot(s1, "Brain Stage 3", "brain_stage_3_cracking.png", 96, 250, 220, COLORS["status.rot"])
    b.rect(s1, "Stage Card", 24, 496, 364, 134, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.hairline"], radius=24.0)
    b.rect(s1, "Stage Alert Pill", 44, 514, 132, 26, fill_hex=COLORS["status.rotSoft"], radius=13.0)
    b.text(s1, "Stage Pill Text", 54, 519, 112, 16, "STAGE 4 · CRACKING", FONT_MONO, "Bold", 10.5, 16.0, COLORS["status.rot"])
    b.text(s1, "Stage Metric", 190, 514, 180, 22, "150+ Reels / Day", FONT_DISPLAY, "Bold", 16.0, 22.0, COLORS["accent.apricot"], align_h="right", tnum=True)
    b.text(s1, "Stage Insight", 44, 552, 324, 62, "Dopamine baselines drop after 45 minutes of rapid variable rewards, leaving normal work feeling flat.", FONT_SANS, "Regular", 13.5, 19.0, COLORS["text.secondary"])
    # 5-stage scrubber pills
    stage_labels = ["0", "50", "100", "150", "200+"]
    for idx, lbl in enumerate(stage_labels):
        px = 24.0 + idx * 74.0
        selected = idx == 3
        b.rect(
            s1, f"Scrubber {lbl}", px, 650, 66, 48,
            fill_hex=COLORS["status.rotSoft"] if selected else COLORS["bg.surface"],
            stroke_hex=COLORS["status.rot"] if selected else COLORS["border.hairline"],
            radius=16.0,
        )
        b.text(s1, f"Scrubber Text {lbl}", px, 664, 66, 20, lbl, FONT_DISPLAY, "Bold", 14.0, 20.0, COLORS["text.primary"] if selected else COLORS["text.muted"], align_h="center", tnum=True)
    b.text(s1, "Haptic Note", 24, 718, 364, 20, "HAPTIC: TRIPLE STUTTER PULSE · 240ms", FONT_MONO, "Bold", 11.0, 18.0, COLORS["accent.apricot"], align_h="center")
    cta1 = draw_tactile_cta(b, s1, "Next CTA", 24, 810, 364, "Continue")

    # 2. Onboarding — Life Cost & Goals
    s2 = create_screen(
        b, page_id, 1, 0, "02 Onboarding — Life Cost & Goals",
        "Quantifies daily scroll hours into waking years reclaimed + lets users choose core focus goals.",
        "behavioral-friction",
    )
    draw_status_bar(b, s2, "STEP 4 OF 7")
    b.rect(s2, "Progress Track", 24, 90, 364, 4, fill_hex=COLORS["border.hairline"], radius=2.0)
    b.rect(s2, "Progress Fill", 24, 90, 208, 4, fill_hex=COLORS["accent.cream"], radius=2.0)
    b.text(s2, "Title", 24, 110, 364, 72, "Reclaim your waking years.", FONT_DISPLAY, "Bold", 28.0, 34.0, COLORS["text.primary"])
    b.rect(s2, "Impact Card", 24, 196, 364, 166, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.warm"], radius=26.0)
    b.text(s2, "Impact Kicker", 44, 214, 324, 16, "AT 3 HOURS / DAY OF SHORT-FORM VIDEO", FONT_MONO, "Bold", 11.0, 16.0, COLORS["accent.apricot"])
    b.text(s2, "Impact Hero Number", 44, 238, 324, 54, "9.4 Years", FONT_DISPLAY, "Bold", 42.0, 50.0, COLORS["status.rot"], tnum=True)
    b.text(s2, "Impact Body", 44, 300, 324, 42, "That is 45 full days every year spent inside feeds you don't remember tomorrow.", FONT_SANS, "Regular", 13.5, 19.0, COLORS["text.secondary"])
    hours_opts = ["1h", "2h", "3h", "4h+"]
    for idx, hr in enumerate(hours_opts):
        hx = 24.0 + idx * 93.0
        sel = idx == 2
        b.rect(s2, f"Hour {hr}", hx, 382, 84, 46, fill_hex=COLORS["accent.amberSoft"] if sel else COLORS["bg.surface"], stroke_hex=COLORS["accent.cream"] if sel else COLORS["border.hairline"], radius=16.0)
        b.text(s2, f"Hour Label {hr}", hx, 395, 84, 20, hr, FONT_DISPLAY, "Bold", 15.0, 20.0, COLORS["accent.cream"] if sel else COLORS["text.secondary"], align_h="center")
    b.text(s2, "Goals Header", 24, 454, 364, 24, "What are you protecting your focus for?", FONT_DISPLAY, "Semi Bold", 17.0, 24.0, COLORS["text.primary"])
    goals = [
        ("Deep Work & Study", True),
        ("Better Sleep Before Midnight", True),
        ("Create & Ship Projects", False),
        ("Present With Real Friends", False),
    ]
    for idx, (g_text, active) in enumerate(goals):
        gy = 492.0 + idx * 62.0
        b.rect(s2, f"Goal {idx}", 24, gy, 364, 52, fill_hex=COLORS["accent.amberSoft"] if active else COLORS["bg.surface"], stroke_hex=COLORS["accent.apricot"] if active else COLORS["border.hairline"], radius=18.0)
        b.text(s2, f"Goal Text {idx}", 42, gy + 16, 280, 20, g_text, FONT_SANS, "Semi Bold", 15.0, 20.0, COLORS["accent.cream"] if active else COLORS["text.primary"])
        b.ellipse(s2, f"Goal Check {idx}", 346, gy + 14, 24, 24, fill_hex=COLORS["status.thriving"] if active else COLORS["border.hairline"])
    cta2 = draw_tactile_cta(b, s2, "Lock Goals CTA", 24, 810, 364, "Set My Focus Plan")

    # 3. Onboarding — Permissions & Pledge
    s3 = create_screen(
        b, page_id, 2, 0, "03 Onboarding — Permissions & Pledge",
        "Prominent Play-compliant disclosure for Accessibility + Overlay and tactile Hold-to-Pledge button.",
        "android-permission",
    )
    draw_status_bar(b, s3, "STEP 7 OF 7")
    b.rect(s3, "Progress Track", 24, 90, 364, 4, fill_hex=COLORS["border.hairline"], radius=2.0)
    b.rect(s3, "Progress Fill", 24, 90, 364, 4, fill_hex=COLORS["status.thriving"], radius=2.0)
    b.text(s3, "Title", 24, 110, 364, 70, "Arm the Shield & seal your pledge.", FONT_DISPLAY, "Bold", 26.0, 32.0, COLORS["text.primary"])
    perms = [
        ("Accessibility Detection", "Detects when guarded apps like YouTube open. Never reads messages or screen text.", "GRANTED", COLORS["status.thriving"], COLORS["status.thrivingSoft"]),
        ("Display Over Other Apps", "Shows the calm pause shield on top of distracting apps before doomscrolling starts.", "GRANTED", COLORS["status.thriving"], COLORS["status.thrivingSoft"]),
    ]
    for idx, (ptitle, pdesc, badge, bcol, bsoft) in enumerate(perms):
        py = 196.0 + idx * 124.0
        b.rect(s3, f"Perm Card {idx}", 24, py, 364, 108, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.hairline"], radius=22.0)
        b.text(s3, f"Perm Title {idx}", 42, py + 16, 220, 22, ptitle, FONT_DISPLAY, "Bold", 15.5, 22.0, COLORS["text.primary"])
        b.rect(s3, f"Perm Badge {idx}", 286, py + 15, 86, 24, fill_hex=bsoft, radius=12.0)
        b.text(s3, f"Perm Badge Txt {idx}", 286, py + 19, 86, 16, badge, FONT_MONO, "Bold", 10.5, 16.0, bcol, align_h="center")
        b.text(s3, f"Perm Desc {idx}", 42, py + 48, 324, 44, pdesc, FONT_SANS, "Regular", 13.0, 18.0, COLORS["text.secondary"])
    b.mascot(s3, "Celebrate Preview", "brain_celebrate_focus.png", 136, 456, 140, COLORS["status.thriving"])
    b.text(s3, "Pledge Prompt", 24, 616, 364, 42, "Press & hold for 3 seconds to experience the exact friction gate protecting your brain.", FONT_SANS, "Medium", 13.5, 19.0, COLORS["text.secondary"], align_h="center")
    # Hold-to-pledge ring control
    b.rect(s3, "Hold Track", 24, 682, 364, 68, fill_hex=COLORS["bg.surfaceElevated"], stroke_hex=COLORS["accent.apricot"], stroke_w=2.0, radius=34.0)
    b.rect(s3, "Hold Progress Fill", 24, 682, 240, 68, fill_hex=COLORS["accent.amberGlow"], fill_alpha=0.65, radius=34.0)
    b.text(s3, "Hold Label", 24, 704, 364, 24, "HOLD 3s TO SEAL PLEDGE · 66%", FONT_DISPLAY, "Bold", 14.5, 22.0, COLORS["accent.cream"], align_h="center", tnum=True)
    b.text(s3, "Haptic Pledge Hint", 24, 764, 364, 18, "HEARTBEAT HAPTICS SYNCED TO RING FILL", FONT_MONO, "Bold", 10.5, 16.0, COLORS["text.muted"], align_h="center")

    # 4. Home — Brain Health Dashboard
    s4 = create_screen(
        b, page_id, 3, 0, "04 Home — Brain Health Dashboard",
        "Live Brain Health score (88%), thriving Nano Banana brain hero, shield status, KPIs, and instant Test Shield CTA.",
        "unique-feature",
    )
    draw_status_bar(b, s4)
    b.text(s4, "Brand Header", 24, 52, 180, 28, "RotGuard", FONT_DISPLAY, "Bold", 24.0, 28.0, COLORS["accent.cream"])
    b.rect(s4, "Shield Status Pill", 240, 50, 148, 32, fill_hex=COLORS["status.thrivingSoft"], stroke_hex=COLORS["status.thriving"], radius=16.0)
    b.text(s4, "Shield Status Text", 240, 58, 148, 16, "● SHIELD ARMED", FONT_MONO, "Bold", 11.0, 16.0, COLORS["status.thriving"], align_h="center")
    b.rect(s4, "Hero Card", 24, 98, 364, 326, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.hairline"], radius=28.0)
    b.mascot(s4, "Nori Thriving", "brain_stage_0_thriving.png", 106, 114, 200, COLORS["status.thriving"])
    b.text(s4, "Brain Health Score", 24, 324, 364, 42, "88% Brain Health", FONT_DISPLAY, "Bold", 28.0, 34.0, COLORS["text.primary"], align_h="center", tnum=True)
    b.text(s4, "Brain Subtitle", 44, 370, 324, 36, "Stage 1 · Thriving · 42m reclaimed today", FONT_SANS, "Medium", 13.5, 19.0, COLORS["status.thriving"], align_h="center")
    # KPI row
    kpis = [("7", "URGES RESISTED", COLORS["status.thriving"]), ("42m", "TIME RECLAIMED", COLORS["accent.cream"]), ("4", "GUARDED APPS", COLORS["accent.apricot"])]
    for idx, (val, lbl, col) in enumerate(kpis):
        kx = 24.0 + idx * 124.0
        b.rect(s4, f"KPI {idx}", kx, 442, 116, 88, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.hairline"], radius=20.0)
        b.text(s4, f"KPI Val {idx}", kx, 458, 116, 32, val, FONT_DISPLAY, "Bold", 24.0, 30.0, col, align_h="center", tnum=True)
        b.text(s4, f"KPI Lbl {idx}", kx + 8, 496, 100, 16, lbl, FONT_MONO, "Bold", 9.5, 14.0, COLORS["text.muted"], align_h="center")
    # Personal Mantra Card
    b.rect(s4, "Mantra Card", 24, 548, 364, 88, fill_hex=COLORS["accent.amberSoft"], stroke_hex=COLORS["border.warm"], radius=22.0)
    b.text(s4, "Mantra Kicker", 42, 564, 328, 16, "YOUR ANCHOR MANTRA", FONT_MONO, "Bold", 10.5, 16.0, COLORS["accent.apricot"])
    b.text(s4, "Mantra Quote", 42, 586, 328, 36, "\"Build things that outlast the feed.\"", FONT_DISPLAY, "Semi Bold", 16.0, 22.0, COLORS["accent.cream"])
    # Guarded Apps strip + Test Shield
    b.text(s4, "Active Targets Header", 24, 654, 364, 20, "Guarded right now: YouTube · Instagram · TikTok · Reddit", FONT_SANS, "Medium", 13.0, 18.0, COLORS["text.secondary"])
    test_cta = draw_tactile_cta(b, s4, "Test Shield CTA", 24, 704, 364, "Test Press & Hold Shield")
    draw_bottom_nav(b, s4, 0)

    # 5. Friction Gate — YouTube Shield
    s5 = create_screen(
        b, page_id, 0, 1, "05 Friction Gate — YouTube Shield",
        "Full-screen TYPE_APPLICATION_OVERLAY drawn over YouTube. Instant exit to Home OR deliberate 4s press-and-hold.",
        "behavioral-friction",
    )
    draw_status_bar(b, s5)
    b.rect(s5, "Intercept Pill", 76, 56, 260, 30, fill_hex=COLORS["status.rotSoft"], stroke_hex=COLORS["status.rot"], radius=15.0)
    b.text(s5, "Intercept Pill Text", 76, 63, 260, 16, "INTERCEPTED · YOUTUBE · ATTEMPT #4", FONT_MONO, "Bold", 10.5, 16.0, COLORS["status.rot"], align_h="center")
    b.mascot(s5, "Nori Shield Hero", "brain_hero_shield.png", 96, 114, 220, COLORS["accent.apricot"])
    b.text(s5, "Shield Headline", 28, 356, 356, 68, "Pause before the spiral.", FONT_DISPLAY, "Bold", 30.0, 36.0, COLORS["text.primary"], align_h="center")
    b.rect(s5, "Shield Mantra Box", 24, 444, 364, 96, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.warm"], radius=22.0)
    b.text(s5, "Shield Mantra Kicker", 42, 460, 328, 16, "REMEMBER WHY YOU STARTED", FONT_MONO, "Bold", 10.5, 16.0, COLORS["accent.apricot"], align_h="center")
    b.text(s5, "Shield Mantra Text", 42, 484, 328, 40, "\"Protect your sleep and deep work tonight.\"", FONT_DISPLAY, "Semi Bold", 16.0, 22.0, COLORS["accent.cream"], align_h="center")
    exit_cta = draw_tactile_cta(b, s5, "Protect Brain CTA", 24, 586, 364, "Protect My Brain (Close YouTube)")
    b.text(s5, "Or Divider", 24, 666, 364, 18, "OR BYPASS WITH DELIBERATE EFFORT", FONT_MONO, "Bold", 10.5, 16.0, COLORS["text.muted"], align_h="center")
    # Signature Press & Hold Friction Control
    b.rect(s5, "Hold Bypass Track", 24, 700, 364, 72, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.warm"], stroke_w=2.0, radius=36.0)
    b.rect(s5, "Hold Bypass Fill", 24, 700, 175, 72, fill_hex=COLORS["accent.amberGlow"], fill_alpha=0.72, radius=36.0)
    b.text(s5, "Hold Bypass Text", 24, 724, 364, 24, "PRESS & HOLD 4s TO CONTINUE (1.9s)", FONT_DISPLAY, "Bold", 13.5, 22.0, COLORS["accent.cream"], align_h="center", tnum=True)
    b.text(s5, "Hold Hint", 36, 788, 340, 36, "Releasing early resets the timer so autopilot muscle memory cannot slip through.", FONT_SANS, "Regular", 12.5, 18.0, COLORS["text.muted"], align_h="center")

    # 6. Guarded Apps — Friction Rules
    s6 = create_screen(
        b, page_id, 1, 1, "06 Guarded Apps — Friction Rules",
        "Real device package picker with search, friction duration presets (3s/4s/6s/10s), and instant toggles.",
        "unique-feature",
    )
    draw_status_bar(b, s6)
    b.text(s6, "Apps Title", 24, 54, 260, 32, "Guarded Apps", FONT_DISPLAY, "Bold", 26.0, 32.0, COLORS["text.primary"])
    b.rect(s6, "Search Bar", 24, 102, 364, 48, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.hairline"], radius=16.0)
    b.text(s6, "Search Placeholder", 44, 116, 320, 20, "Search installed apps (YouTube, Chrome...)", FONT_SANS, "Regular", 14.0, 20.0, COLORS["text.muted"])
    b.text(s6, "Hold Preset Label", 24, 168, 364, 16, "DEFAULT FRICTION HOLD DURATION", FONT_MONO, "Bold", 10.5, 16.0, COLORS["accent.apricot"])
    for idx, dur in enumerate(["3s", "4s", "6s", "10s"]):
        dx = 24.0 + idx * 93.0
        sel = dur == "4s"
        b.rect(s6, f"Preset {dur}", dx, 192, 84, 38, fill_hex=COLORS["accent.cream"] if sel else COLORS["bg.surface"], stroke_hex=COLORS["border.hairline"], radius=19.0)
        b.text(s6, f"Preset Txt {dur}", dx, 201, 84, 20, dur, FONT_DISPLAY, "Bold", 14.0, 20.0, COLORS["ink.onCream"] if sel else COLORS["text.secondary"], align_h="center")
    apps = [
        ("YouTube", "com.google.android.youtube · 4s hold", True, "#FC4A52"),
        ("Instagram", "com.instagram.android · 4s hold", True, "#FFB367"),
        ("TikTok", "com.zhiliaoapp.musically · 6s hold", True, "#FFE9B4"),
        ("Reddit", "com.reddit.frontpage · 4s hold", True, "#FF7A45"),
        ("Chrome", "com.android.chrome · Unprotected", False, "#7D786D"),
    ]
    for idx, (aname, pkg, guarded, badge_col) in enumerate(apps):
        ay = 250.0 + idx * 92.0
        b.rect(s6, f"App Row {aname}", 24, ay, 364, 78, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.warm"] if guarded else COLORS["border.hairline"], radius=22.0)
        b.rect(s6, f"App Badge {aname}", 40, ay + 17, 44, 44, fill_hex=badge_col, fill_alpha=0.22, stroke_hex=badge_col, radius=14.0)
        b.text(s6, f"App Initial {aname}", 40, ay + 27, 44, 24, aname[:1], FONT_DISPLAY, "Bold", 18.0, 24.0, badge_col, align_h="center")
        b.text(s6, f"App Name {aname}", 98, ay + 17, 200, 22, aname, FONT_DISPLAY, "Semi Bold", 16.0, 22.0, COLORS["text.primary"])
        b.text(s6, f"App Pkg {aname}", 98, ay + 42, 204, 18, pkg, FONT_SANS, "Regular", 12.0, 18.0, COLORS["text.muted"])
        b.rect(s6, f"Toggle {aname}", 320, ay + 25, 50, 28, fill_hex=COLORS["status.thriving"] if guarded else COLORS["border.hairline"], radius=14.0)
        b.ellipse(s6, f"Knob {aname}", 344 if guarded else 323, ay + 28, 22, 22, fill_hex=COLORS["bg.canvas"] if guarded else COLORS["text.muted"])
    draw_bottom_nav(b, s6, 1)

    # 7. Insights — Trends & Focus Duel
    s7 = create_screen(
        b, page_id, 2, 1, "07 Insights — Trends & Focus Duel",
        "7-day Brain Health bar trend plus BrainPal-inspired daily Focus Duel accountability card.",
        "unique-feature",
    )
    draw_status_bar(b, s7)
    b.text(s7, "Insights Title", 24, 54, 260, 32, "Insights", FONT_DISPLAY, "Bold", 26.0, 32.0, COLORS["text.primary"])
    # Duel card
    b.rect(s7, "Duel Card", 24, 102, 364, 220, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.warm"], radius=26.0)
    b.mascot(s7, "Duel Celebrate", "brain_celebrate_focus.png", 268, 120, 100, COLORS["status.thriving"])
    b.text(s7, "Duel Kicker", 42, 120, 210, 16, "TODAY'S FOCUS DUEL", FONT_MONO, "Bold", 11.0, 16.0, COLORS["accent.apricot"])
    b.text(s7, "Duel Verdict", 42, 144, 210, 32, "You win today", FONT_DISPLAY, "Bold", 24.0, 30.0, COLORS["status.thriving"])
    b.text(s7, "Duel Sub", 42, 180, 214, 36, "Lower bypassed urges wins. Keep YouTube blocked after 10pm.", FONT_SANS, "Regular", 12.5, 18.0, COLORS["text.secondary"])
    b.rect(s7, "You Box", 42, 232, 142, 68, fill_hex=COLORS["status.thrivingSoft"], stroke_hex=COLORS["status.thriving"], radius=18.0)
    b.text(s7, "You Score", 42, 240, 142, 30, "2", FONT_DISPLAY, "Bold", 24.0, 28.0, COLORS["text.primary"], align_h="center", tnum=True)
    b.text(s7, "You Label", 42, 272, 142, 16, "YOUR BYPASSES", FONT_MONO, "Bold", 10.0, 16.0, COLORS["status.thriving"], align_h="center")
    b.text(s7, "VS", 192, 254, 28, 20, "VS", FONT_MONO, "Bold", 12.0, 20.0, COLORS["text.muted"], align_h="center")
    b.rect(s7, "Rival Box", 228, 232, 142, 68, fill_hex=COLORS["bg.surfaceElevated"], stroke_hex=COLORS["border.hairline"], radius=18.0)
    b.text(s7, "Rival Score", 228, 240, 142, 30, "14", FONT_DISPLAY, "Bold", 24.0, 28.0, COLORS["status.rot"], align_h="center", tnum=True)
    b.text(s7, "Rival Label", 228, 272, 142, 16, "AVG SCROLLER", FONT_MONO, "Bold", 10.0, 16.0, COLORS["text.muted"], align_h="center")
    # 7-Day chart
    b.rect(s7, "Chart Card", 24, 340, 364, 240, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.hairline"], radius=24.0)
    b.text(s7, "Chart Title", 42, 358, 328, 22, "7-Day Brain Health Trend", FONT_DISPLAY, "Semi Bold", 16.0, 22.0, COLORS["text.primary"])
    days = [("M", 64), ("T", 72), ("W", 78), ("T", 82), ("F", 85), ("S", 91), ("S", 88)]
    for idx, (d_lbl, pct) in enumerate(days):
        bx = 48.0 + idx * 46.0
        bh = round(120.0 * (pct / 100.0), 1)
        by = 530.0 - bh
        b.rect(s7, f"Bar BG {idx}", bx, 410, 26, 120, fill_hex=COLORS["bg.surfaceElevated"], radius=8.0)
        b.rect(s7, f"Bar Fill {idx}", bx, by, 26, bh, fill_hex=COLORS["status.thriving"] if pct >= 80 else COLORS["accent.apricot"], radius=8.0)
        b.text(s7, f"Day {idx}", bx, 542, 26, 16, d_lbl, FONT_MONO, "Bold", 11.0, 16.0, COLORS["text.muted"], align_h="center")
    # App breakdown
    b.rect(s7, "Breakdown Card", 24, 598, 364, 206, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.hairline"], radius=24.0)
    b.text(s7, "Breakdown Title", 42, 616, 328, 22, "Most Intercepted Today", FONT_DISPLAY, "Semi Bold", 16.0, 22.0, COLORS["text.primary"])
    rows = [("YouTube", "5 resisted · 1 bypassed", "25m saved"), ("Instagram", "4 resisted · 1 bypassed", "20m saved"), ("Reddit", "3 resisted · 0 bypassed", "15m saved")]
    for idx, (aname, sub, saved) in enumerate(rows):
        ry = 652.0 + idx * 48.0
        b.text(s7, f"Bk App {idx}", 42, ry, 160, 20, aname, FONT_SANS, "Semi Bold", 14.5, 20.0, COLORS["text.primary"])
        b.text(s7, f"Bk Sub {idx}", 42, ry + 20, 200, 16, sub, FONT_SANS, "Regular", 12.0, 16.0, COLORS["text.muted"])
        b.text(s7, f"Bk Saved {idx}", 256, ry + 6, 114, 20, saved, FONT_MONO, "Bold", 12.0, 18.0, COLORS["status.thriving"], align_h="right", tnum=True)
    draw_bottom_nav(b, s7, 2)

    # 8. Settings — Shield & Haptics
    s8 = create_screen(
        b, page_id, 3, 1, "08 Settings — Shield & Haptics",
        "Real-time Android permission diagnostics, haptic tuning, grace session control, and onboarding replay.",
        "android-permission",
    )
    draw_status_bar(b, s8)
    b.text(s8, "Settings Title", 24, 54, 260, 32, "Shield & Haptics", FONT_DISPLAY, "Bold", 26.0, 32.0, COLORS["text.primary"])
    b.rect(s8, "Perms Card", 24, 102, 364, 212, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.hairline"], radius=24.0)
    b.text(s8, "Perms Kicker", 42, 120, 328, 16, "SYSTEM PERMISSION DIAGNOSTICS", FONT_MONO, "Bold", 10.5, 16.0, COLORS["accent.apricot"])
    diag = [
        ("Accessibility Service", "ACTIVE", COLORS["status.thriving"], COLORS["status.thrivingSoft"]),
        ("Draw Over Other Apps", "ACTIVE", COLORS["status.thriving"], COLORS["status.thrivingSoft"]),
        ("Usage Access (Optional)", "GRANTED", COLORS["accent.cream"], COLORS["accent.amberSoft"]),
    ]
    for idx, (dname, dstat, dcol, dsoft) in enumerate(diag):
        dy = 152.0 + idx * 50.0
        b.text(s8, f"Diag Name {idx}", 42, dy + 6, 220, 20, dname, FONT_SANS, "Medium", 14.5, 20.0, COLORS["text.primary"])
        b.rect(s8, f"Diag Pill {idx}", 278, dy + 2, 92, 26, fill_hex=dsoft, radius=13.0)
        b.text(s8, f"Diag Txt {idx}", 278, dy + 7, 92, 16, dstat, FONT_MONO, "Bold", 10.5, 16.0, dcol, align_h="center")
    # Calibration card
    b.rect(s8, "Calibration Card", 24, 332, 364, 286, fill_hex=COLORS["bg.surface"], stroke_hex=COLORS["border.hairline"], radius=24.0)
    b.text(s8, "Cal Kicker", 42, 350, 328, 16, "FRICTION CALIBRATION", FONT_MONO, "Bold", 10.5, 16.0, COLORS["accent.apricot"])
    settings_rows = [
        ("Global Hold Duration", "4 seconds"),
        ("Session Grace Window", "Per-Open (Strict)"),
        ("Escalating Haptics", "Enabled"),
        ("Focus Anchor Mantra", "Build things that outlast the feed."),
    ]
    for idx, (sname, sval) in enumerate(settings_rows):
        sy = 382.0 + idx * 56.0
        b.text(s8, f"Set Name {idx}", 42, sy, 328, 20, sname, FONT_SANS, "Semi Bold", 14.5, 20.0, COLORS["text.primary"])
        b.text(s8, f"Set Val {idx}", 42, sy + 22, 328, 18, sval, FONT_SANS, "Regular", 13.0, 18.0, COLORS["accent.cream"])
    draw_tactile_cta(b, s8, "Replay Onboarding CTA", 24, 650, 364, "Replay Onboarding Tutorial", bg_hex=COLORS["bg.surfaceElevated"], lip_hex=COLORS["border.hairline"], ink_hex=COLORS["accent.cream"])
    draw_bottom_nav(b, s8, 3)

    # Interactive Prototype Flows & Node Transitions
    b.nodes[str(cta1)]["interactions"] = [{"trigger": {"type": "onClick"}, "actions": [{"type": "navigate", "dest": s2}], "transition": {"type": "smartAnimate"}, "duration_ms": 320, "easing": "gentle"}]
    b.nodes[str(cta2)]["interactions"] = [{"trigger": {"type": "onClick"}, "actions": [{"type": "navigate", "dest": s3}], "transition": {"type": "smartAnimate"}, "duration_ms": 320, "easing": "gentle"}]
    b.nodes[str(test_cta)]["interactions"] = [{"trigger": {"type": "onClick"}, "actions": [{"type": "navigate", "dest": s5}], "transition": {"type": "dissolve"}, "duration_ms": 260, "easing": "quick"}]
    b.nodes[str(exit_cta)]["interactions"] = [{"trigger": {"type": "onClick"}, "actions": [{"type": "navigate", "dest": s4}], "transition": {"type": "smartAnimate"}, "duration_ms": 300, "easing": "gentle"}]

    b.nodes[str(page_id)]["flows"] = [
        {"name": "Onboarding to Pledge", "start": s1, "description": "Interactive 5-stage rot simulator -> Life Cost & Goals -> Permissions & Hold-to-Pledge"},
        {"name": "Live YouTube Friction Gate", "start": s4, "description": "Dashboard -> Test Press & Hold Shield -> Protect My Brain or 4s Hold Bypass"},
    ]

    # Native Anchor-Local Comments (2 per screen = 16 total)
    b.add_comment(page_id, s1, 206, 360, "Haptic Design", "Each decay stage fires a progressively sharper VibrationEffect.Composition sequence.", "Android Eng", "Stage 0=tick, Stage 3=triple stutter, Stage 4=heavy thud.")
    b.add_comment(page_id, s1, 206, 674, "Product Design", "Scrubber mirrors BrainPal's 0..200+ reel scale while emphasizing cognitive drain.")
    b.add_comment(page_id, s2, 206, 260, "Behavioral Design", "Translating daily hours into waking years creates immediate loss-aversion clarity.")
    b.add_comment(page_id, s2, 206, 540, "Product Design", "Selected goal pills populate the user's anchor mantra on the live shield overlay.")
    b.add_comment(page_id, s3, 206, 250, "Android Compliance", "Prominent disclosure explains Accessibility & Overlay purpose before launching Settings.")
    b.add_comment(page_id, s3, 206, 716, "Haptic Design", "3-second hold-to-pledge teaches the exact muscle memory used inside the friction shield.")
    b.add_comment(page_id, s4, 206, 220, "Product Design", "Nori mascot state reacts dynamically to today's Brain Health % computed from Room events.")
    b.add_comment(page_id, s4, 206, 730, "UX Polish", "In-app Test Shield lets users demo the overlay immediately without leaving RotGuard.")
    b.add_comment(page_id, s5, 206, 613, "Behavioral Friction", "Primary 3D cream pill exits immediately to Home via GLOBAL_ACTION_HOME.")
    b.add_comment(page_id, s5, 206, 736, "Haptic Design", "Press-and-hold ticker lives in ViewModel/Controller; releasing early resets to 0%.")
    b.add_comment(page_id, s6, 206, 210, "Product Design", "Users calibrate hold friction (3s/4s/6s/10s) to match their impulse strength.")
    b.add_comment(page_id, s6, 206, 290, "Android Eng", "Pre-seeds YouTube/social packages discovered via PackageManager launcher query.")
    b.add_comment(page_id, s7, 206, 200, "Competitive Parity", "Focus Duel echoes BrainPal's social accountability: lower bypassed urges wins.")
    b.add_comment(page_id, s7, 206, 460, "Data Viz", "7-day trend bars built with standard Compose Row/Box primitives per constitution.")
    b.add_comment(page_id, s8, 206, 200, "Android Eng", "Live Settings inspector refreshes status in Activity onResume after System Settings.")
    b.add_comment(page_id, s8, 206, 677, "Onboarding", "Users can replay the animated tutorial & haptic simulator anytime.")

    doc = {
        "format": "figcraft",
        "version": 1,
        "imageIndex": b.image_index,
        "name": "RotGuard — Brainrot & Friction Shield",
        "pages": [page_id],
        "nodes": b.nodes,
        "next_id": b.next_id,
        "styles": [],
        "variables": {
            "collections": [
                {
                    "id": "VariableCollectionId:1",
                    "name": "Warm Obsidian",
                    "modes": [{"id": "VariableCollectionId:1/mode1", "name": "Obsidian"}],
                }
            ],
            "variables": [
                {"id": "VariableID:1", "name": "bg/canvas", "resolvedType": "color", "collection": "VariableCollectionId:1", "values": {"VariableCollectionId:1/mode1": {"type": "color", "value": hex_to_rgba(COLORS["bg.canvas"])}}, "description": "Primary obsidian canvas"},
                {"id": "VariableID:2", "name": "bg/surface", "resolvedType": "color", "collection": "VariableCollectionId:1", "values": {"VariableCollectionId:1/mode1": {"type": "color", "value": hex_to_rgba(COLORS["bg.surface"])}}, "description": "Card surface"},
                {"id": "VariableID:3", "name": "accent/cream", "resolvedType": "color", "collection": "VariableCollectionId:1", "values": {"VariableCollectionId:1/mode1": {"type": "color", "value": hex_to_rgba(COLORS["accent.cream"])}}, "description": "Tactile cream primary CTA"},
                {"id": "VariableID:4", "name": "accent/apricot", "resolvedType": "color", "collection": "VariableCollectionId:1", "values": {"VariableCollectionId:1/mode1": {"type": "color", "value": hex_to_rgba(COLORS["accent.apricot"])}}, "description": "Warm apricot highlight"},
                {"id": "VariableID:5", "name": "status/thriving", "resolvedType": "color", "collection": "VariableCollectionId:1", "values": {"VariableCollectionId:1/mode1": {"type": "color", "value": hex_to_rgba(COLORS["status.thriving"])}}, "description": "Healthy green"},
                {"id": "VariableID:6", "name": "status/rot", "resolvedType": "color", "collection": "VariableCollectionId:1", "values": {"VariableCollectionId:1/mode1": {"type": "color", "value": hex_to_rgba(COLORS["status.rot"])}}, "description": "Brainrot alert red"},
            ],
        },
        "current_page": page_id,
        "document_colors": [
            hex_to_rgba(COLORS["bg.canvas"]),
            hex_to_rgba(COLORS["bg.surface"]),
            hex_to_rgba(COLORS["bg.surfaceElevated"]),
            hex_to_rgba(COLORS["border.hairline"]),
            hex_to_rgba(COLORS["accent.cream"]),
            hex_to_rgba(COLORS["accent.apricot"]),
            hex_to_rgba(COLORS["accent.lip"]),
            hex_to_rgba(COLORS["status.thriving"]),
            hex_to_rgba(COLORS["status.rot"]),
            hex_to_rgba(COLORS["text.primary"]),
            hex_to_rgba(COLORS["text.secondary"]),
        ],
        "annotation_categories": [
            {"id": "unique-feature", "name": "Unique Feature", "color": hex_to_rgba(COLORS["accent.cream"])},
            {"id": "haptic-choreography", "name": "Haptic Choreography", "color": hex_to_rgba(COLORS["accent.apricot"])},
            {"id": "behavioral-friction", "name": "Behavioral Friction", "color": hex_to_rgba(COLORS["status.rot"])},
            {"id": "android-permission", "name": "Android Permission", "color": hex_to_rgba(COLORS["status.thriving"])},
        ],
        "comments": b.comments,
    }
    return doc, [s1, s2, s3, s4, s5, s6, s7, s8], b.images


def write_figcraft_zip(doc: dict[str, Any], images: dict[str, tuple[bytes, int, int]], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    raw = json.dumps(doc, indent=2, ensure_ascii=False).encode("utf-8")
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("document.json", raw)
        for digest, (img_bytes, _, _) in images.items():
            zf.writestr(f"images/{digest}", img_bytes)


def figcraft_roundtrip_and_render(figcraft_path: Path, screen_ids: list[int], previews_dir: Path, port: int = 47976) -> None:
    previews_dir.mkdir(parents=True, exist_ok=True)
    proc = subprocess.Popen(
        [
            str(FIGCRAFT_BIN),
            "--control",
            str(port),
            "--automation-read-root",
            str(ROOT_DIR),
            "--automation-write-root",
            str(ROOT_DIR),
        ],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    time.sleep(2.4)

    def rpc(method: str, params: dict[str, Any], req_id: int) -> dict[str, Any]:
        sock = socket.create_connection(("127.0.0.1", port), timeout=30)
        sock.sendall((json.dumps({"id": req_id, "method": method, "params": params}) + "\n").encode("utf-8"))
        buf = b""
        while b"\n" not in buf:
            chunk = sock.recv(65536)
            if not chunk:
                break
            buf += chunk
        sock.close()
        return json.loads(buf.decode("utf-8"))

    try:
        r_open = rpc("engine.execute", {"command": "file.open", "params": {"path": str(figcraft_path)}}, 1)
        assert r_open.get("ok"), f"file.open failed: {r_open}"
        r_fonts = rpc("engine.execute", {"command": "text.missingFonts", "params": {}}, 2)
        assert r_fonts.get("ok"), f"text.missingFonts failed: {r_fonts}"
        missing = r_fonts["result"].get("families", [])
        assert len(missing) == 0, f"Missing fonts detected: {missing}"

        r_comments = rpc("engine.execute", {"command": "comment.list", "params": {}}, 3)
        assert r_comments.get("ok"), f"comment.list failed: {r_comments}"

        r_save = rpc("engine.execute", {"command": "file.saveAs", "params": {"path": str(figcraft_path)}}, 4)
        assert r_save.get("ok"), f"file.saveAs failed: {r_save}"

        slugs = [
            "01_onboarding_rot",
            "02_onboarding_goals",
            "03_onboarding_pledge",
            "04_home_dashboard",
            "05_friction_shield",
            "06_guarded_apps",
            "07_insights_duel",
            "08_settings_haptics",
        ]
        for idx, (sid, slug) in enumerate(zip(screen_ids, slugs), start=10):
            out_png = previews_dir / f"{slug}.png"
            r_ren = rpc("ui.render", {"ids": [sid], "path": str(out_png), "scale": 2}, idx)
            assert r_ren.get("ok"), f"ui.render failed for {slug}: {r_ren}"
            print(f"[FigCraft] rendered {out_png.name}")

        overview_png = previews_dir / "00_gallery_overview.png"
        r_ov = rpc("ui.render", {"path": str(overview_png), "scale": 1}, 30)
        assert r_ov.get("ok"), f"overview ui.render failed: {r_ov}"
        print("[FigCraft] rendered 00_gallery_overview.png")
    finally:
        proc.terminate()
        try:
            proc.wait(timeout=4)
        except subprocess.TimeoutExpired:
            proc.kill()


def main() -> None:
    parser = argparse.ArgumentParser(description="Build and validate rotguard-app.figcraft")
    parser.add_argument("--roundtrip", action="store_true")
    args = parser.parse_args()

    doc, screen_ids, images = build_rotguard_document()
    write_figcraft_zip(doc, images, OUTPUT_FIGCRAFT)
    print(f"Wrote {OUTPUT_FIGCRAFT} ({OUTPUT_FIGCRAFT.stat().st_size} bytes), {len(doc['nodes'])} nodes, {len(doc['comments'])} comments")

    if args.roundtrip:
        figcraft_roundtrip_and_render(OUTPUT_FIGCRAFT, screen_ids, PREVIEWS_DIR)
        with zipfile.ZipFile(OUTPUT_FIGCRAFT) as zf:
            names = zf.namelist()
            print("Final archive entries:", len(names))
            assert "document.json" in names
            assert "thumbnail.png" in names
            assert any(n.startswith("images/") for n in names)
            assert zf.testzip() is None


if __name__ == "__main__":
    main()
