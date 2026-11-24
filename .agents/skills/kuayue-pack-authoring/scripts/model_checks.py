#!/usr/bin/env python3
"""Portable model, texture, and Blockbench diagnostics shared by skill scripts."""

from __future__ import annotations

import struct
import zlib
from pathlib import Path
from typing import Any, Iterable


SUPPORTED_ELEMENT_ANGLES = {0.0, 22.5, -22.5, 45.0, -45.0}
TRANSPARENT_RENDER_TYPES = {
    "cutout",
    "cutout_mipped",
    "translucent",
    "minecraft:cutout",
    "minecraft:cutout_mipped",
    "minecraft:translucent",
}


def _number(value: Any) -> float | None:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        return None
    return float(value)


def model_rotation_issues(data: Any, label: str) -> list[str]:
    """Return Minecraft block-model element rotation problems."""
    if not isinstance(data, dict):
        return []
    elements = data.get("elements")
    if not isinstance(elements, list):
        return []

    issues: list[str] = []
    for index, element in enumerate(elements):
        if not isinstance(element, dict):
            continue
        rotation = element.get("rotation")
        if rotation is None:
            continue
        prefix = f"{label}: elements[{index}].rotation"
        if not isinstance(rotation, dict):
            issues.append(f"{prefix} must be an object")
            continue
        if any(axis in rotation for axis in ("x", "y", "z")):
            issues.append(
                f"{prefix} uses Blockbench's future multi-axis shape; normalize it "
                "and bake non-vanilla angles before shipping"
            )
            continue
        angle = _number(rotation.get("angle"))
        axis = rotation.get("axis")
        origin = rotation.get("origin")
        if angle is None:
            issues.append(f"{prefix}.angle must be numeric")
        elif angle not in SUPPORTED_ELEMENT_ANGLES:
            issues.append(
                f"{prefix}.angle {angle:g} is unsupported by Minecraft 1.21.1; "
                "bake 90/180-degree geometry rotations into element coordinates"
            )
        if axis not in {"x", "y", "z"}:
            issues.append(f"{prefix}.axis must be x, y, or z")
        if (
            not isinstance(origin, list)
            or len(origin) != 3
            or any(_number(value) is None for value in origin)
        ):
            issues.append(f"{prefix}.origin must contain three numbers")
    return issues


def _png_chunks(raw: bytes) -> Iterable[tuple[bytes, bytes]]:
    if not raw.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError("not a PNG file")
    offset = 8
    while offset + 12 <= len(raw):
        length = struct.unpack_from(">I", raw, offset)[0]
        kind = raw[offset + 4 : offset + 8]
        start = offset + 8
        end = start + length
        if end + 4 > len(raw):
            raise ValueError("truncated PNG chunk")
        yield kind, raw[start:end]
        offset = end + 4
        if kind == b"IEND":
            return


def _paeth(left: int, above: int, upper_left: int) -> int:
    prediction = left + above - upper_left
    left_distance = abs(prediction - left)
    above_distance = abs(prediction - above)
    upper_left_distance = abs(prediction - upper_left)
    if left_distance <= above_distance and left_distance <= upper_left_distance:
        return left
    if above_distance <= upper_left_distance:
        return above
    return upper_left


def _decode_scanlines(
    compressed: bytes,
    *,
    width: int,
    height: int,
    bytes_per_pixel: int,
    row_bytes: int,
) -> list[bytes]:
    decoded = zlib.decompress(compressed)
    stride = row_bytes + 1
    if len(decoded) < stride * height:
        raise ValueError("truncated PNG image data")

    rows: list[bytes] = []
    previous = bytearray(row_bytes)
    for row_index in range(height):
        start = row_index * stride
        filter_type = decoded[start]
        source = decoded[start + 1 : start + 1 + row_bytes]
        current = bytearray(row_bytes)
        for index, value in enumerate(source):
            left = current[index - bytes_per_pixel] if index >= bytes_per_pixel else 0
            above = previous[index]
            upper_left = (
                previous[index - bytes_per_pixel]
                if index >= bytes_per_pixel
                else 0
            )
            if filter_type == 0:
                predictor = 0
            elif filter_type == 1:
                predictor = left
            elif filter_type == 2:
                predictor = above
            elif filter_type == 3:
                predictor = (left + above) // 2
            elif filter_type == 4:
                predictor = _paeth(left, above, upper_left)
            else:
                raise ValueError(f"unsupported PNG filter {filter_type}")
            current[index] = (value + predictor) & 0xFF
        rows.append(bytes(current))
        previous = current
    return rows


def png_alpha_profile(path: Path) -> str | None:
    """Return opaque/cutout/translucent, or None when the PNG is unsupported.

    A cutout texture has only fully transparent and fully opaque pixels.
    A translucent texture has at least one partially transparent pixel.
    """
    try:
        raw = path.read_bytes()
        chunks = list(_png_chunks(raw))
    except (OSError, ValueError, zlib.error):
        return None

    header = next((payload for kind, payload in chunks if kind == b"IHDR"), None)
    if header is None or len(header) != 13:
        return None
    width, height, bit_depth, color_type, _compression, _filter, interlace = (
        struct.unpack(">IIBBBBB", header)
    )
    compressed = b"".join(payload for kind, payload in chunks if kind == b"IDAT")
    transparency = next(
        (payload for kind, payload in chunks if kind == b"tRNS"),
        None,
    )

    if color_type == 3 and transparency is not None:
        alpha_values = list(transparency)
        if any(value < 255 for value in alpha_values):
            if any(0 < value < 255 for value in alpha_values):
                return "translucent"
            return "cutout"
        return "opaque"
    if color_type in {0, 2} and transparency is not None:
        return "cutout"

    if bit_depth != 8 or interlace != 0 or color_type not in {4, 6}:
        return None
    channels = 2 if color_type == 4 else 4
    alpha_index = 1 if color_type == 4 else 3
    try:
        rows = _decode_scanlines(
            compressed,
            width=width,
            height=height,
            bytes_per_pixel=channels,
            row_bytes=width * channels,
        )
    except (ValueError, zlib.error):
        return None

    has_transparent = False
    has_partial = False
    for row in rows:
        for alpha in row[alpha_index::channels]:
            if alpha < 255:
                has_transparent = True
                if alpha > 0:
                    has_partial = True
    if has_partial:
        return "translucent"
    if has_transparent:
        return "cutout"
    return "opaque"


def render_type_issue(profile: str, render_type: Any, label: str) -> str | None:
    if profile == "opaque":
        return None
    if not isinstance(render_type, str):
        return (
            f"{label}: texture has transparent pixels but the model has no "
            "`render_type`; use `cutout_mipped` for binary alpha or "
            "`translucent` for partial alpha"
        )
    normalized = render_type.strip().lower()
    if normalized not in TRANSPARENT_RENDER_TYPES:
        return (
            f"{label}: render_type `{render_type}` cannot preserve texture "
            "transparency; use `cutout_mipped` or `translucent`"
        )
    if profile == "translucent" and normalized not in {
        "translucent",
        "minecraft:translucent",
    }:
        return (
            f"{label}: texture contains partial alpha but render_type is "
            f"`{render_type}`; use `translucent`"
        )
    return None


def _is_visible(node: dict[str, Any]) -> bool:
    return node.get("visibility", True) is not False and node.get("export", True) is not False


def _mesh_has_nonzero_face(element: dict[str, Any]) -> bool:
    vertices = element.get("vertices")
    faces = element.get("faces")
    if not isinstance(vertices, dict) or not isinstance(faces, dict):
        return False
    points: dict[str, tuple[float, float, float]] = {}
    for name, value in vertices.items():
        if (
            isinstance(value, list)
            and len(value) == 3
            and all(_number(component) is not None for component in value)
        ):
            points[name] = tuple(float(component) for component in value)  # type: ignore[arg-type]

    for face in faces.values():
        if not isinstance(face, dict):
            continue
        names = face.get("vertices")
        uvs = face.get("uv")
        if (
            not isinstance(names, list)
            or len(names) < 3
            or any(name not in points for name in names)
            or not isinstance(uvs, dict)
            or any(name not in uvs for name in names)
        ):
            continue
        first = points[names[0]]
        for second_index in range(1, len(names) - 1):
            second = points[names[second_index]]
            third = points[names[second_index + 1]]
            ab = (
                second[0] - first[0],
                second[1] - first[1],
                second[2] - first[2],
            )
            ac = (
                third[0] - first[0],
                third[1] - first[1],
                third[2] - first[2],
            )
            cross = (
                ab[1] * ac[2] - ab[2] * ac[1],
                ab[2] * ac[0] - ab[0] * ac[2],
                ab[0] * ac[1] - ab[1] * ac[0],
            )
            if sum(component * component for component in cross) > 1e-12:
                return True
    return False


def _cube_has_faces(element: dict[str, Any]) -> bool:
    faces = element.get("faces")
    bounds = (element.get("from"), element.get("to"))
    if (
        not isinstance(faces, dict)
        or not faces
        or not all(
            isinstance(value, list)
            and len(value) == 3
            and all(_number(component) is not None for component in value)
            for value in bounds
        )
    ):
        return False
    return any(
        abs(float(left) - float(right)) > 1e-9
        for left, right in zip(bounds[0], bounds[1])
    )


def bbmodel_geometry_stats(data: Any) -> dict[str, int]:
    """Count elements that Blockbench export flags make visible to Kasuga."""
    if not isinstance(data, dict):
        return {
            "visible_elements": 0,
            "valid_elements": 0,
            "valid_faces": 0,
        }
    elements = data.get("elements")
    if not isinstance(elements, list):
        elements = []
    groups = data.get("groups")
    if not isinstance(groups, list):
        groups = []
    outliner = data.get("outliner")
    if not isinstance(outliner, list):
        outliner = []

    elements_by_id: dict[str, dict[str, Any]] = {}
    for element in elements:
        if not isinstance(element, dict):
            continue
        element_id = element.get("uuid") or element.get("id")
        if isinstance(element_id, str):
            elements_by_id[element_id] = element
    groups_by_uuid = {
        group["uuid"]: group
        for group in groups
        if isinstance(group, dict) and isinstance(group.get("uuid"), str)
    }

    visible_elements: list[dict[str, Any]] = []

    def add_element(element_id: str, parent_visible: bool) -> None:
        element = elements_by_id.get(element_id)
        if element is not None and parent_visible and _is_visible(element):
            visible_elements.append(element)

    def walk(nodes: list[Any], parent_visible: bool) -> None:
        for node in nodes:
            if isinstance(node, str):
                add_element(node, parent_visible)
                continue
            if not isinstance(node, dict):
                continue
            group = groups_by_uuid.get(node.get("uuid"), node)
            group_visible = parent_visible and _is_visible(group)
            children = node.get("children")
            if isinstance(children, list):
                walk(children, group_visible)

    if outliner:
        walk(outliner, True)
    else:
        for element_id in elements_by_id:
            add_element(element_id, True)

    valid_elements = 0
    valid_faces = 0
    for element in visible_elements:
        if element.get("type") == "mesh":
            faces = element.get("faces")
            count = 0
            if isinstance(faces, dict):
                for face in faces.values():
                    if (
                        isinstance(face, dict)
                        and isinstance(face.get("vertices"), list)
                        and len(face["vertices"]) >= 3
                    ):
                        count += 1
            valid = _mesh_has_nonzero_face(element)
        else:
            count = len(element.get("faces", {})) if isinstance(element.get("faces"), dict) else 0
            valid = _cube_has_faces(element)
        if valid:
            valid_elements += 1
            valid_faces += count

    return {
        "visible_elements": len(visible_elements),
        "valid_elements": valid_elements,
        "valid_faces": valid_faces,
    }


def bbmodel_geometry_issue(data: Any, label: str) -> str | None:
    stats = bbmodel_geometry_stats(data)
    if stats["valid_elements"] > 0:
        return None
    return (
        f"{label}: no visible exported geometry; check `visibility`/`export` on "
        "the outliner groups and their child elements"
    )
