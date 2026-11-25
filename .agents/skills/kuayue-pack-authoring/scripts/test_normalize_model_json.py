#!/usr/bin/env python3
"""Regression tests for Blockbench model JSON normalization."""

from __future__ import annotations

import struct
import sys
import tempfile
import unittest
import zlib
from binascii import crc32
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from normalize_model_json import normalize_model


def write_rgba_png(path: Path, rgba: tuple[int, int, int, int]) -> None:
    def chunk(kind: bytes, payload: bytes) -> bytes:
        return (
            struct.pack(">I", len(payload))
            + kind
            + payload
            + struct.pack(">I", crc32(kind + payload) & 0xFFFFFFFF)
        )

    header = struct.pack(">IIBBBBB", 1, 1, 8, 6, 0, 0, 0)
    image = zlib.compress(b"\x00" + bytes(rgba))
    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", header)
        + chunk(b"IDAT", image)
        + chunk(b"IEND", b"")
    )


def source_model() -> dict:
    faces = {
        "north": {"uv": [10, 3, 10.5, 4], "texture": "#24"},
        "east": {"uv": [8, 1, 16, 2], "texture": "#24"},
        "south": {"uv": [10, 2, 10.5, 3], "texture": "#24"},
        "west": {"uv": [8, 0, 16, 1], "texture": "#24"},
        "up": {"uv": [8, 2, 8.5, 10], "texture": "#24"},
        "down": {"uv": [8.5, 10, 9, 2], "texture": "#24"},
    }
    return {
        "format_version": "26.3",
        "texture_size": [32, 32],
        "textures": {
            "24": "demo_texture",
            "particle": "demo_texture",
        },
        "elements": [
            {
                "from": [15, -2, -16],
                "to": [16, 0, 0],
                "rotation": {"x": 0, "y": -90, "z": 0, "origin": [0, 0, 0]},
                "faces": faces,
            }
        ],
        "groups": [{"name": "group"}],
    }


class NormalizeModelTests(unittest.TestCase):
    def test_bakes_negative_y_rotation_and_namespaces_textures(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            source = root / "demo_texture_source.json"
            output = root / "assets/demo/models/block/vehicle/handrail.json"
            write_rgba_png(root / "demo_texture.png", (255, 255, 255, 0))
            source.write_text("{}", encoding="utf-8")

            normalized, warnings = normalize_model(
                source_model(),
                source=source,
                output=output,
                namespace="demo",
            )
            element = normalized["elements"][0]
            self.assertEqual([0, -2, 15], element["from"])
            self.assertEqual([16, 0, 16], element["to"])
            self.assertIn("east", element["faces"])
            self.assertIn("south", element["faces"])
            self.assertIn("west", element["faces"])
            self.assertIn("north", element["faces"])
            self.assertEqual([8, 10, 8.5, 2], element["faces"]["up"]["uv"])
            self.assertEqual([9, 10, 8.5, 2], element["faces"]["down"]["uv"])
            self.assertEqual(
                "demo:block/vehicle/demo_texture",
                normalized["textures"]["24"],
            )
            self.assertEqual("cutout_mipped", normalized["render_type"])
            self.assertEqual("1.21.6", normalized["format_version"])
            self.assertNotIn("groups", normalized)
            self.assertTrue(warnings)

    def test_partial_alpha_selects_translucent(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            source = root / "model.json"
            output = root / "models/block/demo/model.json"
            write_rgba_png(root / "demo_texture.png", (255, 255, 255, 128))
            source.write_text("{}", encoding="utf-8")
            normalized, _warnings = normalize_model(
                source_model(),
                source=source,
                output=output,
                namespace="demo",
            )
            self.assertEqual("translucent", normalized["render_type"])

    def test_adds_model_folder_to_short_block_texture_paths(self) -> None:
        data = source_model()
        data["textures"] = {
            "24": "block/demo_texture",
            "particle": "block/demo_texture",
        }
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            source = root / "model.json"
            source.write_text("{}", encoding="utf-8")
            normalized, _warnings = normalize_model(
                data,
                source=source,
                output=root / "models/block/vehicle/model.json",
                namespace="demo",
            )
            self.assertEqual(
                "demo:block/vehicle/demo_texture",
                normalized["textures"]["24"],
            )

    def test_rejects_multiple_nonzero_axes(self) -> None:
        data = source_model()
        data["elements"][0]["rotation"] = {
            "x": 45,
            "y": 90,
            "z": 0,
            "origin": [0, 0, 0],
        }
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            source = root / "model.json"
            source.write_text("{}", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "multi-axis"):
                normalize_model(
                    data,
                    source=source,
                    output=root / "models/block/demo/model.json",
                    namespace="demo",
                )

    def test_converts_supported_fractional_angle_shape(self) -> None:
        data = source_model()
        data["elements"][0]["rotation"] = {
            "x": 45,
            "y": 0,
            "z": 0,
            "origin": [8, 8, 8],
        }
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            source = root / "model.json"
            source.write_text("{}", encoding="utf-8")
            normalized, _warnings = normalize_model(
                data,
                source=source,
                output=root / "models/block/demo/model.json",
                namespace="demo",
                render_type="keep",
            )
            self.assertEqual(
                {
                    "angle": 45,
                    "axis": "x",
                    "origin": [8, 8, 8],
                },
                normalized["elements"][0]["rotation"],
            )


if __name__ == "__main__":
    unittest.main()
