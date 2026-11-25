#!/usr/bin/env python3
"""Build, validate, and render all 5 alternative RotGuard visual directions via FigCraft."""

from __future__ import annotations

import argparse
import sys
import zipfile
from pathlib import Path
from typing import Callable

from concept_01_hardware import OUTPUT_FIGCRAFT as OUT_01, build_concept_01
from concept_02_paper import OUTPUT_FIGCRAFT as OUT_02, build_concept_02
from concept_03_brutalist import OUTPUT_FIGCRAFT as OUT_03, build_concept_03
from concept_04_crt import OUTPUT_FIGCRAFT as OUT_04, build_concept_04
from concept_05_botanical import OUTPUT_FIGCRAFT as OUT_05, build_concept_05
from fc_core import PREVIEWS_DIR, figcraft_roundtrip_batch, write_figcraft_zip

CONCEPTS: dict[int, tuple[str, Path, Callable]] = {
    1: ("01_hardware_detent", OUT_01, build_concept_01),
    2: ("02_daylight_paper", OUT_02, build_concept_02),
    3: ("03_swiss_brutalist", OUT_03, build_concept_03),
    4: ("04_crt_terminal", OUT_04, build_concept_04),
    5: ("05_botanical_sanctuary", OUT_05, build_concept_05),
}


def main() -> None:
    parser = argparse.ArgumentParser(description="Build 5 distinct FigCraft gallery options for RotGuard")
    parser.add_argument("--only", type=str, default="", help="Comma-separated concept numbers (e.g. 1,3,5)")
    parser.add_argument("--roundtrip", action="store_true", help="Validate & render gallery PNGs via FigCraft --control")
    parser.add_argument("--port", type=int, default=47981, help="TCP control port for FigCraft")
    args = parser.parse_args()

    selected = (
        [int(x.strip()) for x in args.only.split(",") if x.strip()]
        if args.only
        else sorted(CONCEPTS.keys())
    )

    jobs: list[tuple[Path, Path]] = []
    for cid in selected:
        if cid not in CONCEPTS:
            print(f"Unknown concept ID: {cid}", file=sys.stderr)
            sys.exit(2)
        slug, fig_path, builder_fn = CONCEPTS[cid]
        doc, _screen_ids, images = builder_fn()
        write_figcraft_zip(doc, images, fig_path)
        preview_png = PREVIEWS_DIR / f"{slug}_gallery.png"
        jobs.append((fig_path, preview_png))
        print(f"Built {fig_path.name} ({fig_path.stat().st_size} bytes, {len(doc['nodes'])} nodes)")

    if args.roundtrip:
        figcraft_roundtrip_batch(jobs, port=args.port)
        for fig_path, preview_png in jobs:
            with zipfile.ZipFile(fig_path) as zf:
                names = zf.namelist()
                assert "document.json" in names, f"Missing document.json in {fig_path.name}"
                assert "thumbnail.png" in names, f"Missing thumbnail.png in {fig_path.name}"
                assert zf.testzip() is None, f"Corrupt ZIP: {fig_path.name}"
            assert preview_png.exists() and preview_png.stat().st_size > 10_000, f"Bad preview: {preview_png}"
            print(f"Verified {fig_path.name} + {preview_png.name} ({preview_png.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
