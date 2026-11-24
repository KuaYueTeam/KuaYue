#!/usr/bin/env python3
"""Normalize a Blockbench Java model JSON for Minecraft 1.21.1 packs."""

from __future__ import annotations

import argparse
import copy
import itertools
import json
import math
import sys
from pathlib import Path
from typing import Any

sys.path.insert(0, str(Path(__file__).parent))

from model_checks import png_alpha_profile


FACE_MAP_POSITIVE_Y = {
    "north": "west",
    "west": "south",
    "south": "east",
    "east": "north",
    "up": "up",
    "down": "down",
}
FACE_MAP_NEGATIVE_Y = {
    "north": "east",
    "east": "south",
    "south": "west",
    "west": "north",
    "up": "up",
    "down": "down",
}


def _clean_number(value: float) -> int | float:
    rounded = round(value, 6)
    if abs(rounded) < 1e-9:
        rounded = 0.0
    if rounded.is_integer():
        return int(rounded)
    return rounded


def _cleaned_xyz(values: list[float]) -> list[int | float]:
    return [_clean_number(value) for value in values]


def _swap_u(uv: list[Any]) -> list[Any]:
    return [uv[2], uv[1], uv[0], uv[3]]


def _swap_v(uv: list[Any]) -> list[Any]:
    return [uv[0], uv[3], uv[2], uv[1]]


def _rotation_angle_and_axis(rotation: dict[str, Any]) -> tuple[float, str, list[float]]:
    if "angle" in rotation:
        angle = rotation.get("angle")
        axis = rotation.get("axis")
        origin = rotation.get("origin")
    else:
        angle = None
        axis = None
        nonzero = []
        for candidate in ("x", "y", "z"):
            value = rotation.get(candidate, 0)
            if isinstance(value, bool) or not isinstance(value, (int, float)):
                raise ValueError(f"rotation.{candidate} must be numeric")
            if abs(float(value)) > 1e-9:
                nonzero.append((candidate, float(value)))
        origin = rotation.get("origin")
        if len(nonzero) > 1:
            raise ValueError(
                "multi-axis element rotations must be split or baked manually"
            )
        if nonzero:
            axis, angle = nonzero[0]
        else:
            angle = 0.0
            axis = "y"

    if isinstance(angle, bool) or not isinstance(angle, (int, float)):
        raise ValueError("rotation angle must be numeric")
    if axis not in {"x", "y", "z"}:
        raise ValueError("rotation axis must be x, y, or z")
    if (
        not isinstance(origin, list)
        or len(origin) != 3
        or any(
            isinstance(value, bool) or not isinstance(value, (int, float))
            for value in origin
        )
    ):
        raise ValueError("rotation origin must contain three numbers")
    return float(angle), str(axis), [float(value) for value in origin]


def _bake_rotation(
    element: dict[str, Any],
) -> tuple[int, str | None]:
    rotation = element.get("rotation")
    if not isinstance(rotation, dict):
        return 0, None
    angle, axis, origin = _rotation_angle_and_axis(rotation)
    turns = int(round(angle / 90.0))
    if not math.isclose(angle, turns * 90.0, abs_tol=1e-9):
        if angle not in {0.0, 22.5, -22.5, 45.0, -45.0}:
            raise ValueError(
                f"unsupported element rotation {angle:g} degrees on {axis.upper()} axis"
            )
        element["rotation"] = {
            "angle": _clean_number(angle),
            "axis": axis,
            "origin": _cleaned_xyz(origin),
        }
        return turns, None
    turns %= 4
    if turns == 0:
        element["rotation"] = {
            "angle": 0,
            "axis": axis,
            "origin": _cleaned_xyz(origin),
        }
        return 0, None
    if turns == 2:
        raise ValueError(
            "180-degree element rotations require manual baking; "
            "this helper supports ±90 and ±270 Y rotations"
        )
    if axis != "y":
        raise ValueError(
            f"{turns * 90}-degree {axis.upper()}-axis rotation requires manual baking"
        )

    start = element.get("from")
    end = element.get("to")
    if (
        not isinstance(start, list)
        or not isinstance(end, list)
        or len(start) != 3
        or len(end) != 3
    ):
        raise ValueError("element from/to must contain three numbers")
    ox, _oy, oz = origin
    points: list[tuple[float, float, float]] = []
    for x, y, z in itertools.product(
        (float(start[0]), float(end[0])),
        (float(start[1]), float(end[1])),
        (float(start[2]), float(end[2])),
    ):
        dx, dz = x - ox, z - oz
        if turns == 1:
            points.append((ox + dz, y, oz - dx))
        else:
            points.append((ox - dz, y, oz + dx))
    element["from"] = [
        _clean_number(min(point[axis] for point in points)) for axis in range(3)
    ]
    element["to"] = [
        _clean_number(max(point[axis] for point in points)) for axis in range(3)
    ]
    element["rotation"] = {
        "angle": 0,
        "axis": "y",
        "origin": [0, 0, 0],
    }

    faces = element.get("faces")
    if not isinstance(faces, dict):
        return turns if turns == 1 else -1, axis
    face_map = FACE_MAP_POSITIVE_Y if turns == 1 else FACE_MAP_NEGATIVE_Y
    remapped: dict[str, Any] = {}
    for source_name, face in faces.items():
        target_name = face_map.get(source_name, source_name)
        if isinstance(face, dict):
            face = copy.deepcopy(face)
            uv = face.get("uv")
            if isinstance(uv, list) and len(uv) == 4:
                if source_name == "up":
                    face["uv"] = _swap_u(uv) if turns == 1 else _swap_v(uv)
                elif source_name == "down":
                    face["uv"] = _swap_v(uv) if turns == 1 else _swap_u(uv)
        remapped[target_name] = face
    element["faces"] = remapped
    return turns if turns == 1 else -1, "y"


def _infer_texture_base(output: Path) -> str:
    parts = output.with_suffix("").parts
    if "models" in parts:
        index = parts.index("models")
        remainder = parts[index + 1 :]
        if remainder and remainder[0] == "block":
            return "/".join(remainder)
    return f"block/{output.stem}"


def _namespace_texture(
    value: str,
    *,
    namespace: str,
    texture_base: str,
) -> str:
    if value.startswith("#"):
        return value
    had_namespace = ":" in value
    if had_namespace:
        texture_namespace, path = value.split(":", 1)
    else:
        texture_namespace, path = namespace, value
    path = path.removeprefix("textures/")
    if path.endswith(".png"):
        path = path[:-4]
    path_parts = path.split("/")
    texture_parent = texture_base.rsplit("/", 1)[0] if "/" in texture_base else texture_base
    parent_parts = texture_parent.split("/")
    if (
        len(path_parts) == 2
        and path_parts[0] in {"block", "item"}
        and not had_namespace
        and len(parent_parts) > 1
        and parent_parts[0] == path_parts[0]
    ):
        path = "/".join([*parent_parts, path_parts[-1]])
    if "/" not in path:
        path = f"{texture_parent}/{path}"
    return f"{texture_namespace}:{path}"


def _source_texture_profile(source: Path, value: str) -> str | None:
    if value.startswith("#"):
        return None
    basename = Path(value.split(":", 1)[-1]).name
    if basename.endswith(".png"):
        candidate = source.parent / basename
    else:
        candidate = source.parent / f"{basename}.png"
    if not candidate.is_file():
        return None
    return png_alpha_profile(candidate)


def normalize_model(
    data: dict[str, Any],
    *,
    source: Path,
    output: Path,
    namespace: str,
    texture_base: str | None = None,
    render_type: str = "auto",
) -> tuple[dict[str, Any], list[str]]:
    if not isinstance(data.get("elements"), list):
        raise ValueError("input is not a Minecraft cube model with `elements`")
    normalized = copy.deepcopy(data)
    normalized["format_version"] = "1.21.6"
    normalized.setdefault("gui_light", "front")

    base = texture_base or _infer_texture_base(output)
    textures = normalized.get("textures")
    profiles: list[str] = []
    if isinstance(textures, dict):
        namespaced: dict[str, Any] = {}
        for alias, value in textures.items():
            if not isinstance(value, str):
                namespaced[alias] = value
                continue
            profile = _source_texture_profile(source, value)
            if profile not in {None, "opaque"}:
                profiles.append(profile)
            namespaced[alias] = _namespace_texture(
                value,
                namespace=namespace,
                texture_base=base,
            )
        normalized["textures"] = namespaced

    if render_type == "auto":
        if "translucent" in profiles:
            normalized["render_type"] = "translucent"
        elif profiles:
            normalized.setdefault("render_type", "cutout_mipped")
    elif render_type != "keep":
        normalized["render_type"] = render_type

    warnings: list[str] = []
    for element in normalized["elements"]:
        if not isinstance(element, dict):
            continue
        try:
            _bake_rotation(element)
        except ValueError as exc:
            raise ValueError(str(exc)) from exc
    normalized.pop("groups", None)
    if not isinstance(normalized.get("display"), dict):
        warnings.append(
            "model has no display transforms; verify the item model in-game"
        )
    return normalized, warnings


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--namespace", required=True)
    parser.add_argument(
        "--texture-base",
        help="texture path below textures/, for example block/vehicle/head",
    )
    parser.add_argument(
        "--render-type",
        choices=("auto", "cutout_mipped", "translucent", "keep"),
        default="auto",
    )
    parser.add_argument(
        "--in-place",
        action="store_true",
        help="allow source and output to be the same file",
    )
    args = parser.parse_args()

    source = args.source.resolve()
    output = args.output.resolve()
    if source == output and not args.in_place:
        parser.error("source and output are the same; pass --in-place to overwrite")
    try:
        data = json.loads(source.read_text(encoding="utf-8"))
        normalized, warnings = normalize_model(
            data,
            source=source,
            output=output,
            namespace=args.namespace,
            texture_base=args.texture_base,
            render_type=args.render_type,
        )
    except (OSError, json.JSONDecodeError, ValueError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(
        json.dumps(normalized, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(output)
    if normalized.get("render_type"):
        print(f"render_type: {normalized['render_type']}")
    for warning in warnings:
        print(f"warning: {warning}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
