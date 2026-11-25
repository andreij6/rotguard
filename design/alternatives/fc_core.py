"""Shared FigCraft 0.10.0 document builder, gallery layout, and --control batch renderer."""

from __future__ import annotations

import hashlib
import json
import math
import socket
import struct
import subprocess
import time
import zipfile
from pathlib import Path
from typing import Any

ROOT_DIR = Path(__file__).resolve().parents[2]
ALT_DIR = ROOT_DIR / "design" / "alternatives"
ASSETS_DIR = ALT_DIR / "assets"
PREVIEWS_DIR = ALT_DIR / "previews"
FIGCRAFT_BIN = Path("/Applications/FigCraft.app/Contents/MacOS/FigCraft")

SCREEN_W = 412.0
SCREEN_H = 915.0

GALLERY_W = 1932.0
GALLERY_H = 1172.0
GALLERY_PAD_X = 64.0
SCREEN_GAP_X = 52.0
SCREEN_TOP_Y = 172.0

BASE_TS = 1764000000


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


def linear_gradient(
    stops: list[tuple[float, str, float]],
    from_pt: list[float] | None = None,
    to_pt: list[float] | None = None,
) -> list[dict[str, Any]]:
    return [
        {
            "type": "gradient",
            "kind": "linear",
            "stops": [{"position": pos, "color": hex_to_rgba(col, a)} for pos, col, a in stops],
            "from": from_pt or [0.5, 0.0],
            "to": to_pt or [0.5, 1.0],
            "width": [-0.5, 0.0],
            "opacity": 1.0,
            "visible": True,
            "blend": "normal",
        }
    ]


def radial_glow(hex_str: str, fade_to_hex: str, center_alpha: float = 0.35) -> list[dict[str, Any]]:
    return [
        {
            "type": "gradient",
            "kind": "radial",
            "stops": [
                {"position": 0.0, "color": hex_to_rgba(hex_str, center_alpha)},
                {"position": 1.0, "color": hex_to_rgba(fade_to_hex, 0.0)},
            ],
            "from": [0.5, 0.5],
            "to": [0.5, 1.0],
            "width": [0.5, 0.0],
            "opacity": 1.0,
            "visible": True,
            "blend": "normal",
        }
    ]


def image_paint(image_hash: str, scale_mode: str = "fill", opacity: float = 1.0) -> list[dict[str, Any]]:
    return [
        {
            "type": "image",
            "image": image_hash,
            "scale_mode": scale_mode,
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
            "opacity": float(opacity),
            "visible": True,
            "blend": "normal",
        }
    ]


def drop_shadow(
    hex_str: str = "#000000",
    alpha: float = 0.25,
    dx: float = 0.0,
    dy: float = 12.0,
    radius: float = 28.0,
    spread: float = 0.0,
) -> list[dict[str, Any]]:
    return [
        {
            "type": "dropShadow",
            "color": hex_to_rgba(hex_str, alpha),
            "offset": [float(dx), float(dy)],
            "radius": float(radius),
            "spread": float(spread),
            "blend": "normal",
            "show_behind": False,
            "visible": True,
        }
    ]


def inner_shadow(
    hex_str: str = "#FFFFFF",
    alpha: float = 0.5,
    dx: float = 0.0,
    dy: float = 1.0,
    radius: float = 0.0,
) -> dict[str, Any]:
    return {
        "type": "innerShadow",
        "color": hex_to_rgba(hex_str, alpha),
        "offset": [float(dx), float(dy)],
        "radius": float(radius),
        "spread": 0.0,
        "blend": "normal",
        "visible": True,
    }


def make_radii(radius: float | tuple[float, float, float, float]) -> list[float]:
    if isinstance(radius, (tuple, list)):
        return [float(r) for r in radius]
    r = float(radius)
    return [r, r, r, r]


class DocBuilder:
    """Ergonomic builder for valid FigCraft 0.10.0 JSON documents."""

    def __init__(self, name: str, backdrop_hex: str) -> None:
        self.doc_name = name
        self.nodes: dict[str, dict[str, Any]] = {}
        self.next_id = 1
        self.images: dict[str, tuple[bytes, int, int]] = {}
        self.image_index: list[dict[str, Any]] = []
        self.comments: list[dict[str, Any]] = []
        self.comment_id = 1
        self.page_id = self._create_page(backdrop_hex)

    def alloc_id(self) -> int:
        nid = self.next_id
        self.next_id += 1
        return nid

    def _create_page(self, backdrop_hex: str) -> int:
        nid = self.alloc_id()
        self.nodes[str(nid)] = {
            "id": nid,
            "name": "Gallery Overview",
            "type": "page",
            "background": hex_to_rgba(backdrop_hex),
            "flows": [],
            "guides": [],
            "children": [],
            "width": 0.0,
            "height": 0.0,
        }
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

    def group(self, parent_id: int, name: str, x: float = 0.0, y: float = 0.0, w: float = 100.0, h: float = 100.0) -> int:
        nid = self.alloc_id()
        return self.add_node(
            {
                "id": nid,
                "name": name,
                "type": "group",
                "children": [],
                "transform": [1.0, 0.0, 0.0, 1.0, float(x), float(y)],
                "width": float(w),
                "height": float(h),
                "blend": "normal",
            },
            parent_id,
        )

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
        stroke_align: str = "inside",
        radius: float | tuple[float, float, float, float] = 0.0,
        smoothing: float = 0.6,
        clip: bool = False,
        effects: list[dict[str, Any]] | None = None,
        annotations: list[dict[str, Any]] | None = None,
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
                "align": stroke_align,
                "sides": None,
                "cap_start": "round",
                "cap_end": "round",
                "join": "round",
                "miter_limit": 4.0,
                "dashes": [],
            }
        radii = make_radii(radius)
        if any(r > 0 for r in radii):
            node["corners"] = {"radii": radii, "smoothing": float(smoothing)}
        if effects:
            node["effects"] = effects
        if annotations:
            node["annotations"] = annotations
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
        stroke_alpha: float = 1.0,
        stroke_w: float = 1.0,
        dashes: list[float] | None = None,
        radius: float | tuple[float, float, float, float] = 0.0,
        smoothing: float = 0.45,
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
            node["strokes"] = solid(stroke_hex, stroke_alpha)
            node["stroke"] = {
                "weight": float(stroke_w),
                "align": "inside",
                "sides": None,
                "cap_start": "none" if dashes else "round",
                "cap_end": "none" if dashes else "round",
                "join": "miter" if radius == 0 else "round",
                "miter_limit": 4.0,
                "dashes": list(dashes) if dashes else [],
            }
        radii = make_radii(radius)
        if any(r > 0 for r in radii):
            node["corners"] = {"radii": radii, "smoothing": float(smoothing)}
        if effects:
            node["effects"] = effects
        return self.add_node(node, parent_id)

    def rule(
        self,
        parent_id: int,
        name: str,
        x: float,
        y: float,
        w: float,
        color_hex: str,
        thickness: float = 1.0,
        alpha: float = 1.0,
    ) -> int:
        return self.rect(parent_id, name, x, y, w, thickness, fill_hex=color_hex, fill_alpha=alpha)

    def vrule(
        self,
        parent_id: int,
        name: str,
        x: float,
        y: float,
        h: float,
        color_hex: str,
        thickness: float = 1.0,
        alpha: float = 1.0,
    ) -> int:
        return self.rect(parent_id, name, x, y, thickness, h, fill_hex=color_hex, fill_alpha=alpha)

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
        stroke_alpha: float = 1.0,
        stroke_w: float = 1.5,
        dashes: list[float] | None = None,
        inner: float = 0.0,
        sweep_ratio: float = 1.0,
        start_rad: float = -math.pi / 2.0,
        effects: list[dict[str, Any]] | None = None,
    ) -> int:
        nid = self.alloc_id()
        node: dict[str, Any] = {
            "id": nid,
            "name": name,
            "type": "ellipse",
            "transform": [1.0, 0.0, 0.0, 1.0, float(x), float(y)],
            "width": float(w),
            "height": float(h),
            "start": float(start_rad),
            "sweep": 2.0 * math.pi * float(sweep_ratio),
            "inner": float(inner),
            "blend": "normal",
        }
        if fill_hex is not None:
            node["fills"] = solid(fill_hex, fill_alpha)
        if stroke_hex is not None:
            node["strokes"] = solid(stroke_hex, stroke_alpha)
            node["stroke"] = {
                "weight": float(stroke_w),
                "align": "inside",
                "sides": None,
                "cap_start": "none" if dashes else "round",
                "cap_end": "none" if dashes else "round",
                "join": "round",
                "miter_limit": 4.0,
                "dashes": list(dashes) if dashes else [],
            }
        if effects:
            node["effects"] = effects
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
        font_family: str,
        font_style: str,
        font_size: float,
        line_height_px: float | None,
        color_hex: str,
        color_alpha: float = 1.0,
        align_h: str = "left",
        letter_spacing_pct: float | None = None,
        tnum: bool = False,
        case: str = "original",
    ) -> int:
        nid = self.alloc_id()
        lh = line_height_px if line_height_px is not None else round(font_size * 1.32, 1)
        ls = letter_spacing_pct if letter_spacing_pct is not None else (-1.2 if font_size >= 24 else 0.0)
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
                "letter_spacing": {"unit": "percent", "value": float(ls)},
                "decoration": "none",
                "case": case,
                "fills": None,
                "link": None,
            },
            "align_h": align_h,
            "auto_resize": "height",
            "transform": [1.0, 0.0, 0.0, 1.0, float(x), float(y)],
            "width": float(w),
            "height": float(h),
            "fills": solid(color_hex, color_alpha),
        }
        if tnum:
            node["features"] = {"tnum": True}
        return self.add_node(node, parent_id)

    def clipped_image(
        self,
        parent_id: int,
        name: str,
        image_path: Path,
        x: float,
        y: float,
        w: float,
        h: float,
        bg_hex: str | None = None,
        stroke_hex: str | None = None,
        stroke_w: float = 1.0,
        radius: float | tuple[float, float, float, float] = 0.0,
        img_inset: float = 0.0,
    ) -> int:
        holder = self.frame(
            parent_id,
            name,
            x,
            y,
            w,
            h,
            fill_hex=bg_hex,
            stroke_hex=stroke_hex,
            stroke_w=stroke_w,
            radius=radius,
            smoothing=0.7,
            clip=True,
        )
        img_hash = self.register_png(image_path)
        self.rect(
            holder,
            f"{name} Artwork",
            img_inset,
            img_inset,
            w - 2 * img_inset,
            h - 2 * img_inset,
            fills_override=image_paint(img_hash, scale_mode="fill"),
        )
        return holder

    def add_comment(
        self,
        anchor_id: int,
        x: float,
        y: float,
        author: str,
        text: str,
    ) -> None:
        cid = self.comment_id
        self.comment_id += 1
        self.comments.append(
            {
                "id": cid,
                "page": self.page_id,
                "x": float(x),
                "y": float(y),
                "anchor": anchor_id,
                "author": author,
                "text": text,
                "created": BASE_TS + cid * 60,
                "edited": 0,
                "reactions": [],
                "resolved": False,
                "replies": [],
                "read_by": [author],
            }
        )

    def finalize_document(
        self,
        variables_spec: list[tuple[str, str, str]],
        collection_name: str,
        mode_name: str,
        doc_colors: list[str],
        annotations_cats: list[tuple[str, str, str]],
    ) -> dict[str, Any]:
        return {
            "format": "figcraft",
            "version": 1,
            "imageIndex": self.image_index,
            "name": self.doc_name,
            "pages": [self.page_id],
            "nodes": self.nodes,
            "next_id": self.next_id,
            "styles": [],
            "variables": {
                "collections": [
                    {
                        "id": "VariableCollectionId:1",
                        "name": collection_name,
                        "modes": [{"id": "VariableCollectionId:1/mode1", "name": mode_name}],
                    }
                ],
                "variables": [
                    {
                        "id": f"VariableID:{i + 1}",
                        "name": vname,
                        "resolvedType": "color",
                        "collection": "VariableCollectionId:1",
                        "values": {
                            "VariableCollectionId:1/mode1": {
                                "type": "color",
                                "value": hex_to_rgba(vhex),
                            }
                        },
                        "description": vdesc,
                    }
                    for i, (vname, vhex, vdesc) in enumerate(variables_spec)
                ],
            },
            "current_page": self.page_id,
            "document_colors": [hex_to_rgba(c) for c in doc_colors],
            "annotation_categories": [
                {"id": cid, "name": cname, "color": hex_to_rgba(chex)}
                for cid, cname, chex in annotations_cats
            ],
            "comments": self.comments,
        }


def setup_gallery_chrome(
    b: DocBuilder,
    concept_tag: str,
    title: str,
    subtitle: str,
    font_label: str,
    backdrop_hex: str,
    title_hex: str,
    muted_hex: str,
    rule_hex: str,
    swatches: list[tuple[str, str]],
    screen_captions: list[str],
    header_font: str = "Inter",
    body_font: str = "Inter",
    mono_font: str = "Liberation Mono",
) -> None:
    """Place the outer studio backdrop, concept header, swatches, and bottom captions."""
    b.rect(b.page_id, "Studio Gallery Backdrop", 0.0, 0.0, GALLERY_W, GALLERY_H, fill_hex=backdrop_hex)

    hdr = b.group(b.page_id, "Gallery Header", 0.0, 0.0, GALLERY_W, 150.0)
    b.text(
        hdr,
        "Concept Kicker",
        GALLERY_PAD_X,
        40.0,
        600.0,
        18.0,
        concept_tag,
        mono_font,
        "Bold",
        11.5,
        16.0,
        muted_hex,
        letter_spacing_pct=6.0,
    )
    b.text(
        hdr,
        "Gallery Title",
        GALLERY_PAD_X,
        64.0,
        1020.0,
        44.0,
        title,
        header_font,
        "Bold",
        34.0,
        40.0,
        title_hex,
    )
    b.text(
        hdr,
        "Gallery Subtitle",
        GALLERY_PAD_X,
        112.0,
        1160.0,
        24.0,
        subtitle,
        body_font,
        "Regular",
        15.0,
        22.0,
        muted_hex,
    )

    # Top-right typography & palette strip
    b.text(
        hdr,
        "Font Pairing Label",
        GALLERY_W - GALLERY_PAD_X - 420.0,
        42.0,
        420.0,
        18.0,
        font_label,
        mono_font,
        "Bold",
        11.0,
        16.0,
        muted_hex,
        align_h="right",
    )
    sw_start_x = GALLERY_W - GALLERY_PAD_X - len(swatches) * 64.0 + 14.0
    for idx, (shex, sname) in enumerate(swatches):
        sx = sw_start_x + idx * 64.0
        b.rect(hdr, f"Swatch {idx}", sx, 68.0, 50.0, 32.0, fill_hex=shex, stroke_hex=rule_hex, stroke_w=1.0, radius=6.0)
        b.text(hdr, f"Swatch Hex {idx}", sx, 105.0, 50.0, 14.0, sname, mono_font, "Regular", 9.0, 12.0, muted_hex, align_h="center")

    b.rule(hdr, "Header Divider", GALLERY_PAD_X, 148.0, GALLERY_W - 2 * GALLERY_PAD_X, rule_hex, thickness=1.0)

    ftr_y = SCREEN_TOP_Y + SCREEN_H + 18.0
    ftr = b.group(b.page_id, "Gallery Footer Captions", 0.0, ftr_y, GALLERY_W, 50.0)
    for idx, cap in enumerate(screen_captions):
        cx = GALLERY_PAD_X + idx * (SCREEN_W + SCREEN_GAP_X)
        b.text(
            ftr,
            f"Screen Caption {idx + 1}",
            cx,
            8.0,
            SCREEN_W,
            24.0,
            cap,
            mono_font,
            "Bold",
            12.0,
            18.0,
            muted_hex,
            align_h="center",
        )


def screen_x(slot_idx: int) -> float:
    return GALLERY_PAD_X + slot_idx * (SCREEN_W + SCREEN_GAP_X)


def link_flow(b: DocBuilder, src_id: int, dst_id: int, transition: str = "smartAnimate", duration_ms: int = 300) -> None:
    b.nodes[str(src_id)]["interactions"] = [
        {
            "trigger": {"type": "onClick"},
            "actions": [{"type": "navigate", "dest": dst_id}],
            "transition": {"type": transition},
            "duration_ms": duration_ms,
            "easing": "gentle",
        }
    ]


def write_figcraft_zip(doc: dict[str, Any], images: dict[str, tuple[bytes, int, int]], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    raw = json.dumps(doc, indent=2, ensure_ascii=False).encode("utf-8")
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("document.json", raw)
        for digest, (img_bytes, _, _) in images.items():
            zf.writestr(f"images/{digest}", img_bytes)


def figcraft_roundtrip_batch(jobs: list[tuple[Path, Path]], port: int = 47981) -> None:
    """Validate fonts, embed authentic thumbnail.png, and render gallery PNGs in one FigCraft session."""
    PREVIEWS_DIR.mkdir(parents=True, exist_ok=True)
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
    time.sleep(2.5)

    def rpc(method: str, params: dict[str, Any], req_id: int) -> dict[str, Any]:
        sock = socket.create_connection(("127.0.0.1", port), timeout=45)
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
        req = 1
        for fig_path, preview_png in jobs:
            r_open = rpc("engine.execute", {"command": "file.open", "params": {"path": str(fig_path)}}, req)
            req += 1
            assert r_open.get("ok"), f"file.open failed for {fig_path.name}: {r_open}"

            r_fonts = rpc("engine.execute", {"command": "text.missingFonts", "params": {}}, req)
            req += 1
            assert r_fonts.get("ok"), f"text.missingFonts failed for {fig_path.name}: {r_fonts}"
            missing = r_fonts["result"].get("families", [])
            assert not missing, f"Missing fonts in {fig_path.name}: {missing}"

            r_save = rpc("engine.execute", {"command": "file.saveAs", "params": {"path": str(fig_path)}}, req)
            req += 1
            assert r_save.get("ok"), f"file.saveAs failed for {fig_path.name}: {r_save}"

            r_render = rpc("ui.render", {"path": str(preview_png), "scale": 1.25}, req)
            req += 1
            assert r_render.get("ok"), f"ui.render failed for {fig_path.name}: {r_render}"
            print(f"[FigCraft OK] {fig_path.name} -> {preview_png.name}")
    finally:
        proc.terminate()
        try:
            proc.wait(timeout=4)
        except subprocess.TimeoutExpired:
            proc.kill()
