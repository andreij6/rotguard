#!/usr/bin/env python3
"""Build, validate, and render Dark Mode galleries for Option 2 (Quiet Folio) and Option 5 (Verdant)."""

from __future__ import annotations

import argparse
import zipfile
from pathlib import Path
from typing import Callable

from concept_02_paper_dark import OUTPUT_FIGCRAFT as OUT_02_DARK, build_concept_02_dark
from concept_05_botanical_dark import OUTPUT_FIGCRAFT as OUT_05_DARK, build_concept_05_dark
from fc_core import PREVIEWS_DIR, figcraft_roundtrip_batch, write_figcraft_zip

DARK_CONCEPTS: list[tuple[str, Path, Callable]] = [
    ("02_daylight_paper_dark", OUT_02_DARK, build_concept_02_dark),
    ("05_botanical_sanctuary_dark", OUT_05_DARK, build_concept_05_dark),
]


def main() -> None:
    parser = argparse.ArgumentParser(description="Build & render Dark Mode Option 2 and Option 5 FigCraft galleries")
    parser.add_argument("--roundtrip", action="store_true", help="Validate & render PNGs via FigCraft --control")
    parser.add_argument("--port", type=int, default=47984)
    args = parser.parse_args()

    jobs: list[tuple[Path, Path]] = []
    for slug, fig_path, builder_fn in DARK_CONCEPTS:
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
                assert "document.json" in names
                assert "thumbnail.png" in names
                assert zf.testzip() is None
            assert preview_png.exists() and preview_png.stat().st_size > 10_000
            print(f"Verified {fig_path.name} + {preview_png.name} ({preview_png.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
