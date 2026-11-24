#!/usr/bin/env python3
"""Focused regression tests for train validation in validate_pack.py."""

from __future__ import annotations

import json
import struct
import sys
import tempfile
import unittest
import zlib
from binascii import crc32
from pathlib import Path
from typing import Any

sys.path.insert(0, str(Path(__file__).parent))

from validate_pack import Report, validate_multi_cell_params, validate_pack


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def write_rgba_png(path: Path, rgba: tuple[int, int, int, int]) -> None:
    def chunk(kind: bytes, payload: bytes) -> bytes:
        return (
            struct.pack(">I", len(payload))
            + kind
            + payload
            + struct.pack(">I", crc32(kind + payload) & 0xFFFFFFFF)
        )

    path.parent.mkdir(parents=True, exist_ok=True)
    header = struct.pack(">IIBBBBB", 1, 1, 8, 6, 0, 0, 0)
    image = zlib.compress(b"\x00" + bytes(rgba))
    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", header)
        + chunk(b"IDAT", image)
        + chunk(b"IEND", b"")
    )


def valid_bbmodel() -> dict[str, Any]:
    return {
        "meta": {"format_version": "5.0"},
        "resolution": {"width": 16, "height": 16},
        "textures": [],
        "elements": [
            {
                "uuid": "cube-1",
                "type": "cube",
                "export": True,
                "visibility": True,
                "from": [0, 0, 0],
                "to": [16, 16, 16],
                "origin": [0, 0, 0],
                "faces": {
                    "north": {"texture": -1},
                },
            }
        ],
        "groups": [],
        "outliner": ["cube-1"],
        "animations": [],
    }


def complete_train(train_id: str = "demo:car") -> dict[str, Any]:
    return {
        "id": train_id,
        "category": "carriage",
        "name_key": "parcel.demo.car.name",
        "description_key": "parcel.demo.car.description",
        "members": {
            "blocks": [],
            "items": [],
            "item_tags": [],
            "bogey_sizes": [],
            "bogey_motions": [],
            "bogey_models": [],
            "bogey_blocks": [],
            "bogey_styles": [],
            "bogey_style_variants": [],
        },
        "creative": {"tab": "kuayue:kuayue_carriage"},
    }


def write_pack(
    root: Path,
    pack_id: str,
    train: dict[str, Any],
    dependency_ids: tuple[str, ...] = (),
) -> None:
    write_json(
        root / "pack.json",
        {
            "format_version": 1,
            "id": pack_id,
            "version": "1.0.0",
            "namespaces": ["demo"],
            "dependencies": {
                "packs": {
                    dependency_id: "*" for dependency_id in dependency_ids
                }
            },
            "parcels": ["parcels/car/parcel.json"],
        },
    )
    parcel_dir = root / "parcels/car"
    write_json(
        parcel_dir / "parcel.json",
        {
            "format_version": 1,
            "id": "demo:car",
            "name_key": "parcel.demo.car.name",
            "indexes": ["data/demo/kasuga_lib/data_driven/car.json"],
        },
    )
    write_json(
        parcel_dir / "data/demo/kasuga_lib/data_driven/car.json",
        {"on_register": ["trains/car.json"], "on_reload": []},
    )
    write_json(parcel_dir / "data/demo/trains/car.json", {"trains": [train]})


def write_block_pack(root: Path, block: dict[str, Any]) -> Path:
    pack_root = root / "packs/demo-pack"
    train = complete_train()
    train["members"]["blocks"] = [block["id"]]
    write_pack(pack_root, "demo-pack", train)
    parcel_dir = pack_root / "parcels/car"
    write_json(
        parcel_dir / "data/demo/kasuga_lib/data_driven/car.json",
        {
            "on_register": ["locomotives/car/blocks.json", "trains/car.json"],
            "on_reload": [],
        },
    )
    write_json(
        parcel_dir / "data/demo/locomotives/car/blocks.json",
        {"blocks": [block]},
    )
    write_json(
        parcel_dir / "assets/demo/blockstates/demo_head.json",
        {"variants": {"": {"model": "demo:block/empty"}}},
    )
    write_json(parcel_dir / "assets/demo/models/block/empty.json", {})
    write_json(
        parcel_dir / "assets/demo/models/item/demo_head.json",
        {"parent": "minecraft:item/generated"},
    )
    write_json(
        parcel_dir / "data/demo/loot_table/blocks/demo_head.json",
        {"type": "minecraft:block"},
    )
    write_json(
        parcel_dir / "assets/demo/lang/zh_cn.json",
        {"block.demo.demo_head": "测试车头"},
    )
    write_json(
        parcel_dir / "assets/demo/lang/en_us.json",
        {"block.demo.demo_head": "Test head"},
    )
    return pack_root


def proxy_head() -> dict[str, Any]:
    return {
        "id": "demo:demo_head",
        "type": "proxy_model",
        "state_properties": {
            "facing": {
                "type": "vanilla",
                "property": "HORIZONTAL_FACING",
                "default": "north",
            }
        },
        "block_entity": {
            "type": "proxy_model_be",
            "params": {
                "model": "demo:models/block/demo_head.bbmodel",
            },
        },
    }


class TrainValidationTests(unittest.TestCase):
    def validate(self, repo: Path, pack_root: Path):
        return validate_pack(repo, pack_root, require_schema=False)

    def test_accepts_complete_train(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            pack_root = repo / "packs/demo-pack"
            write_pack(pack_root, "demo-pack", complete_train())
            report = self.validate(repo, pack_root)
            self.assertEqual([], report.errors)

    def test_accepts_state_selected_multi_cell_footprints(self) -> None:
        report = Report()
        validate_multi_cell_params(
            "demo:door",
            {
                "multi_cell": {
                    "cells_by_state": {
                        "hinge": {
                            "left": [[1, 0, 0], [0, 1, 0], [1, 1, 0]],
                            "right": [[-1, 0, 0], [0, 1, 0], [-1, 1, 0]],
                        }
                    }
                }
            },
            report,
        )
        self.assertEqual([], report.errors)

    def test_rejects_incomplete_train(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            pack_root = repo / "packs/demo-pack"
            write_pack(pack_root, "demo-pack", {"id": "demo:car"})
            report = self.validate(repo, pack_root)
            self.assertTrue(
                any("is incomplete after merging" in error for error in report.errors)
            )

    def test_requires_creative_tab(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            pack_root = repo / "packs/demo-pack"
            train = complete_train()
            train["creative"] = {}
            write_pack(pack_root, "demo-pack", train)
            report = self.validate(repo, pack_root)
            self.assertTrue(
                any("`/creative/tab`" in error for error in report.errors)
            )

    def test_validates_creative_filter_order(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            pack_root = repo / "packs/demo-pack"
            train = complete_train()
            train["creative"]["filter_order"] = 100
            write_pack(pack_root, "demo-pack", train)
            report = self.validate(repo, pack_root)
            self.assertEqual([], report.errors)

            train["creative"]["filter_order"] = "first"
            write_pack(pack_root, "demo-pack", train)
            report = self.validate(repo, pack_root)
            self.assertTrue(
                any("/creative/filter_order" in error for error in report.errors)
            )

    def test_rejects_unresolved_owned_member(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            pack_root = repo / "packs/demo-pack"
            train = complete_train()
            train["members"]["blocks"] = ["demo:missing_block"]
            write_pack(pack_root, "demo-pack", train)
            report = self.validate(repo, pack_root)
            self.assertTrue(
                any("`demo:missing_block` is not declared" in error for error in report.errors)
            )

    def test_accepts_direct_dependency_overlay(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            base_root = repo / "packs/base-pack"
            target_root = repo / "packs/target-pack"
            write_pack(base_root, "base-pack", complete_train())
            write_pack(
                target_root,
                "target-pack",
                {"id": "demo:car", "members": {"items": []}},
                dependency_ids=("base-pack",),
            )
            report = self.validate(repo, target_root)
            self.assertEqual([], report.errors)

    def test_rejects_transitive_overlay_without_direct_dependency(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            base_root = repo / "packs/base-pack"
            middle_root = repo / "packs/middle-pack"
            target_root = repo / "packs/target-pack"
            write_pack(base_root, "base-pack", complete_train())
            write_pack(
                middle_root,
                "middle-pack",
                complete_train("demo:other"),
                dependency_ids=("base-pack",),
            )
            write_pack(
                target_root,
                "target-pack",
                {"id": "demo:car", "members": {"items": []}},
                dependency_ids=("middle-pack",),
            )
            report = self.validate(repo, target_root)
            self.assertTrue(
                any(
                    "does not directly depend on it" in error
                    for error in report.errors
                )
            )

    def test_rejects_fsm_rendering(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            block = proxy_head()
            block["type"] = "fsm_block"
            block["state_machine"] = "demo:head"
            block["block_entity"] = {
                "type": "fsm_be",
                "params": {"state_machine": "demo:head"},
            }
            block.pop("state_properties")
            pack_root = write_block_pack(repo, block)
            report = self.validate(repo, pack_root)
            self.assertTrue(
                any("FSM rendering is forbidden" in error for error in report.errors)
            )

    def test_rejects_proxy_head_without_facing(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            block = proxy_head()
            block.pop("state_properties")
            pack_root = write_block_pack(repo, block)
            report = self.validate(repo, pack_root)
            self.assertTrue(
                any(
                    "require `state_properties.facing`" in error
                    for error in report.errors
                )
            )

    def test_rejects_proxy_model_block_params(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            block = proxy_head()
            block["params"] = {"toggle_property": "extended"}
            pack_root = write_block_pack(repo, block)
            parcel_dir = pack_root / "parcels/car"
            write_json(
                parcel_dir / "assets/demo/models/block/demo_head.bbmodel",
                valid_bbmodel(),
            )
            write_json(
                parcel_dir / "assets/demo/models/model_proxy.json",
                {"demo": ["models/block/**/*.bbmodel"]},
            )
            report = self.validate(repo, pack_root)
            self.assertTrue(
                any("block-level params are not supported" in error for error in report.errors)
            )

    def test_rejects_generic_property_to_clip_animation(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            block = proxy_head()
            block["block_entity"]["params"]["animation"] = {
                "name": "deploy",
                "property": "extended",
                "active_when": True,
            }
            pack_root = write_block_pack(repo, block)
            report = self.validate(repo, pack_root)
            self.assertTrue(
                any("generic `proxy_model_be.params.animation`" in error
                    for error in report.errors)
            )

    def test_accepts_proxy_head_with_native_bbmodel(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            pack_root = write_block_pack(repo, proxy_head())
            parcel_dir = pack_root / "parcels/car"
            write_json(
                parcel_dir / "assets/demo/blockstates/demo_head.json",
                {
                    "variants": {
                        f"facing={direction}": {"model": "demo:block/empty"}
                        for direction in ("east", "north", "south", "west")
                    }
                },
            )
            write_json(
                parcel_dir / "assets/demo/models/block/demo_head.bbmodel",
                valid_bbmodel(),
            )
            write_json(
                parcel_dir / "assets/demo/models/model_proxy.json",
                {"demo": ["models/block/**/*.bbmodel"]},
            )
            report = self.validate(repo, pack_root)
            self.assertEqual([], report.errors)

    def test_accepts_proxy_model_group_selected_by_block_state(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            block = proxy_head()
            block["state_properties"]["hinge"] = {
                "type": "vanilla",
                "property": "DOOR_HINGE",
                "default": "left",
            }
            block["block_entity"]["params"] = {
                "default_model": "left",
                "model_groups": {
                    "left": {
                        "parts": [
                            {
                                "id": "body",
                                "model": "demo:models/block/left.bbmodel",
                            }
                        ]
                    },
                    "right": {
                        "parts": [
                            {
                                "id": "body",
                                "model": "demo:models/block/right.bbmodel",
                            }
                        ]
                    },
                },
                "model_by_state": {
                    "hinge": {"left": "left", "right": "right"}
                },
            }
            pack_root = write_block_pack(repo, block)
            parcel_dir = pack_root / "parcels/car"
            for name in ("left", "right"):
                write_json(
                    parcel_dir / f"assets/demo/models/block/{name}.bbmodel",
                    valid_bbmodel(),
                )
            write_json(
                parcel_dir / "assets/demo/models/model_proxy.json",
                {"demo": ["models/block/**/*.bbmodel"]},
            )
            write_json(
                parcel_dir / "assets/demo/blockstates/demo_head.json",
                {
                    "variants": {
                        f"facing={direction}": {"model": "demo:block/empty"}
                        for direction in ("east", "north", "south", "west")
                    }
                },
            )

            report = self.validate(repo, pack_root)

            self.assertEqual([], report.errors)

    def test_rejects_proxy_bbmodel_without_exported_geometry(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            pack_root = write_block_pack(repo, proxy_head())
            parcel_dir = pack_root / "parcels/car"
            bbmodel = valid_bbmodel()
            bbmodel["groups"] = [
                {"uuid": "group-1", "name": "tail", "export": False, "visibility": True}
            ]
            bbmodel["outliner"] = [
                {"uuid": "group-1", "children": ["cube-1"]}
            ]
            write_json(
                parcel_dir / "assets/demo/models/block/demo_head.bbmodel",
                bbmodel,
            )
            write_json(
                parcel_dir / "assets/demo/models/model_proxy.json",
                {"demo": ["models/block/**/*.bbmodel"]},
            )
            report = self.validate(repo, pack_root)
            self.assertTrue(
                any("no visible exported geometry" in error for error in report.errors)
            )

    def test_rejects_unsupported_element_rotation(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            block = {
                "id": "demo:demo_head",
                "type": "train_panel",
            }
            pack_root = write_block_pack(repo, block)
            parcel_dir = pack_root / "parcels/car"
            write_json(
                parcel_dir / "assets/demo/models/block/empty.json",
                {
                    "elements": [
                        {
                            "from": [0, 0, 0],
                            "to": [16, 16, 16],
                            "rotation": {"angle": 90, "axis": "y", "origin": [0, 0, 0]},
                            "faces": {},
                        }
                    ]
                },
            )
            report = self.validate(repo, pack_root)
            self.assertTrue(
                any("unsupported by Minecraft 1.21.1" in error for error in report.errors)
            )

    def test_requires_facing_variants_for_directional_factory(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            block = {
                "id": "demo:demo_head",
                "type": "train_panel",
            }
            pack_root = write_block_pack(repo, block)
            report = self.validate(repo, pack_root)
            self.assertTrue(
                any("missing facing blockstate variant" in error for error in report.errors)
            )

    def test_warns_when_transparent_texture_has_no_render_type(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            block = {
                "id": "demo:demo_head",
                "type": "train_panel",
            }
            pack_root = write_block_pack(repo, block)
            parcel_dir = pack_root / "parcels/car"
            write_json(
                parcel_dir / "assets/demo/blockstates/demo_head.json",
                {
                    "variants": {
                        f"facing={direction}": {"model": "demo:block/transparent"}
                        for direction in ("east", "north", "south", "west")
                    }
                },
            )
            write_json(
                parcel_dir / "assets/demo/models/block/transparent.json",
                {
                    "textures": {
                        "particle": "demo:block/transparent",
                        "texture": "demo:block/transparent",
                    },
                    "elements": [
                        {
                            "from": [0, 0, 0],
                            "to": [16, 16, 16],
                            "faces": {},
                        }
                    ],
                },
            )
            write_rgba_png(
                parcel_dir / "assets/demo/textures/block/transparent.png",
                (255, 255, 255, 0),
            )
            report = self.validate(repo, pack_root)
            self.assertTrue(
                any("texture has transparent pixels" in warning for warning in report.warnings)
            )

    def test_require_durable_source_rejects_runtime_pack(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo = Path(temp_dir)
            pack_root = (
                repo
                / "modules/train-parts/run/kuayue/packs/demo-pack"
            )
            write_pack(pack_root, "demo-pack", complete_train())
            report = validate_pack(
                repo,
                pack_root,
                require_schema=False,
                require_durable_source=True,
            )
            self.assertTrue(
                any("durable source" in error for error in report.errors)
            )


if __name__ == "__main__":
    unittest.main()
