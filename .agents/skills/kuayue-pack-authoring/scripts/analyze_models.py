#!/usr/bin/env python3
"""Propose possible KuaYue blocks from artist-supplied model files."""

from __future__ import annotations

import argparse
import json
import re
import struct
import sys
from pathlib import Path
from typing import Any, Iterable

from model_checks import bbmodel_geometry_issue, model_rotation_issues


GENERIC_NAMES = {
    "cube",
    "cubes",
    "group",
    "groups",
    "mesh",
    "meshes",
    "object",
    "objects",
    "root",
    "scene",
    "default",
    "untitled",
}


def snake(value: str) -> str:
    value = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", value)
    value = re.sub(r"[^A-Za-z0-9]+", "_", value).strip("_").lower()
    return value or "unnamed"


def add_candidate(
    candidates: dict[str, dict[str, Any]],
    *,
    name: str,
    source: Path,
    kind: str,
    evidence: str,
    confidence: str,
    warnings: Iterable[str] = (),
) -> None:
    proposed = f"<namespace>:{snake(name)}"
    candidate = candidates.setdefault(
        proposed,
        {
            "proposed_block_id": proposed,
            "sources": [],
            "kind": kind,
            "evidence": [],
            "confidence": confidence,
            "suggested_factory": suggest_factory(name),
            "warnings": [],
        },
    )
    rank = {"low": 0, "medium": 1, "high": 2}
    if rank[confidence] > rank[candidate["confidence"]]:
        candidate["confidence"] = confidence
    source_text = str(source)
    if source_text not in candidate["sources"]:
        candidate["sources"].append(source_text)
    if evidence not in candidate["evidence"]:
        candidate["evidence"].append(evidence)
    for warning in warnings:
        if warning not in candidate["warnings"]:
            candidate["warnings"].append(warning)


def suggest_factory(name: str) -> str:
    tokens = set(snake(name).split("_"))
    lowered = snake(name)
    if "window" in tokens:
        if "small" in tokens:
            return "train_small_window"
        if tokens & {"sealed", "fixed", "closed", "unopenable"}:
            return "train_unopenable_window"
        return "train_openable_window (verify fixed/open behavior)"
    if tokens & {"door", "doors"}:
        return "train_door or custom_rendered_door"
    if "end" in tokens and "face" in tokens:
        return "custom_rendered_end_face"
    if tokens & {"floor", "slab"}:
        return "slab"
    if "carport" in tokens:
        return (
            "slab_top; use a dedicated TrainPanel proxy host when a native "
            "BBModel must preserve family collision/wrench semantics"
        )
    if tokens & {"roof", "cap"} and "top" in tokens:
        return "slab_top"
    if "ladder" in tokens:
        return "ladder"
    if tokens & {"handrail", "railing"}:
        return "train_panel"
    if "skirt" in tokens:
        return "skirt"
    if "vent" in tokens:
        return "air_vent"
    if "pantograph" in tokens:
        return "single_arm_pantograph"
    if "outrigger" in tokens:
        return "dedicated Java proxy-model block type with proxy_model_be"
    if "console" in tokens or ({"control", "desk"} & tokens):
        return "dedicated Java ProxyModelBlock type with proxy_model_be"
    if "cowcatcher" in tokens:
        return "hxd3d_cowcatcher or vehicle-specific factory"
    if "head" in tokens:
        return "head"
    if "chimney" in tokens:
        return "chimney or df5_chimney"
    if "panel" in tokens or "wall" in tokens or "side" in tokens:
        return "train_panel"
    if "table" in lowered:
        return "full_shape_directional (new factory if thin collision is required)"
    return "unresolved; inspect factory table and matching parcel"


def parse_obj(
    path: Path,
    display_path: Path,
    candidates: dict[str, dict[str, Any]],
    issues: list[dict[str, str]],
    known_block_names: set[str],
) -> None:
    add_candidate(
        candidates,
        name=path.stem,
        source=display_path,
        kind="obj",
        evidence="OBJ filename",
        confidence="medium",
    )
    objects: list[str] = []
    groups: list[str] = []
    materials: set[str] = set()
    try:
        with path.open("r", encoding="utf-8", errors="replace") as handle:
            for line in handle:
                fields = line.strip().split(maxsplit=1)
                if len(fields) != 2:
                    continue
                tag, value = fields
                if tag == "o":
                    objects.append(value.strip())
                elif tag == "g":
                    groups.append(value.strip())
                elif tag == "usemtl":
                    materials.add(value.strip())
    except OSError as exc:
        issues.append({"source": str(display_path), "warning": str(exc)})
        return

    seen: set[str] = set()
    if snake(path.stem) not in known_block_names:
        for values, evidence_label, confidence in (
            (objects, "OBJ object", "medium"),
            (groups, "OBJ group", "low"),
        ):
            for value in values:
                normalized = snake(value)
                if normalized in seen or normalized in GENERIC_NAMES:
                    continue
                seen.add(normalized)
                add_candidate(
                    candidates,
                    name=f"{path.stem}_{value}",
                    source=display_path,
                    kind="obj",
                    evidence=f"{evidence_label}: {value}",
                    confidence=confidence,
                    warnings=["Nested model name may be a part rather than a block."],
                )
    if materials:
        add_candidate(
            candidates,
            name=path.stem,
            source=display_path,
            kind="obj",
            evidence=f"materials: {', '.join(sorted(materials)[:8])}",
            confidence="medium",
        )


def walk_outliner(nodes: Any) -> list[str]:
    names: list[str] = []
    if isinstance(nodes, list):
        for node in nodes:
            names.extend(walk_outliner(node))
    elif isinstance(nodes, dict):
        name = nodes.get("name")
        if isinstance(name, str) and name.strip():
            names.append(name.strip())
        for key in ("children", "outliner"):
            if key in nodes:
                names.extend(walk_outliner(nodes[key]))
    return names


def scan_json(
    data: Any,
    path: Path,
    display_path: Path,
    candidates: dict[str, dict[str, Any]],
    include_model_json_as_candidates: bool,
    referenced_model_paths: set[str],
    issues: list[dict[str, str]],
) -> None:
    if not isinstance(data, dict):
        return
    if "variants" in data or "multipart" in data:
        add_candidate(
            candidates,
            name=path.stem,
            source=display_path,
            kind="blockstate",
            evidence="Minecraft blockstate JSON",
            confidence="high",
        )
        return

    if "elements" in data and "outliner" in data and (
        "meta" in data or "resolution" in data
    ):
        geometry_issue = bbmodel_geometry_issue(data, str(display_path))
        if geometry_issue is not None:
            issues.append({"source": str(display_path), "warning": geometry_issue})
        meta = data.get("meta")
        project_name = None
        if isinstance(meta, dict) and isinstance(meta.get("name"), str):
            project_name = meta["name"]
        for name in [project_name, path.stem]:
            if not name:
                continue
            add_candidate(
                candidates,
                name=name,
                source=display_path,
                kind="bbmodel",
                evidence="BBModel project filename/metadata",
                confidence="medium",
                warnings=["Confirm whether this project is one block or multiple blocks."],
            )
        for name in walk_outliner(data.get("outliner")):
            normalized = snake(name)
            if normalized in GENERIC_NAMES:
                continue
            add_candidate(
                candidates,
                name=f"{path.stem}_{name}",
                source=display_path,
                kind="bbmodel",
                evidence=f"BBModel outliner group: {name}",
                confidence="low",
                warnings=["BBModel group may be a model part rather than a block."],
            )
        return

    if any(key in data for key in ("textures", "elements", "parent", "display")):
        for issue in model_rotation_issues(data, str(display_path)):
            issues.append({"source": str(display_path), "warning": issue})
        if "item" in path.parts and "models" in path.parts:
            return
        model_path = model_path_from_file(path)
        if (
            model_path in referenced_model_paths
            and not include_model_json_as_candidates
        ):
            return
        confidence = "high" if "block" in path.parts else "medium"
        add_candidate(
            candidates,
            name=path.stem,
            source=display_path,
            kind="minecraft-model",
            evidence="Minecraft block-model JSON",
            confidence=confidence,
            warnings=(
                ["Model is not referenced by a supplied blockstate."]
                if model_path not in referenced_model_paths
                else []
            )
            + (
                []
                if "block" in path.parts
                else ["Model path does not prove that this is a block; confirm the visual role."]
            ),
        )


def model_path_from_file(path: Path) -> str | None:
    parts = list(path.with_suffix("").parts)
    if "models" not in parts:
        return None
    index = parts.index("models")
    return "/".join(parts[index + 1 :])


def blockstate_model_references(data: dict[str, Any]) -> list[str]:
    refs: list[str] = []

    def add(value: Any) -> None:
        if isinstance(value, dict):
            model = value.get("model")
            if isinstance(model, str):
                refs.append(model)
        elif isinstance(value, list):
            for child in value:
                add(child)

    variants = data.get("variants")
    if isinstance(variants, dict):
        for value in variants.values():
            add(value)
    multipart = data.get("multipart")
    if isinstance(multipart, list):
        for part in multipart:
            if isinstance(part, dict):
                add(part.get("apply"))
    return refs


def normalize_model_reference(reference: str) -> str:
    return reference.split(":", 1)[-1]


def model_field_references(value: Any, parent_key: str = "") -> list[str]:
    refs: list[str] = []
    if isinstance(value, dict):
        for key, child in value.items():
            refs.extend(model_field_references(child, str(key)))
    elif isinstance(value, list):
        for child in value:
            refs.extend(model_field_references(child, parent_key))
    elif isinstance(value, str) and "model" in parent_key.lower():
        refs.append(value)
    return refs


def expand_model_reference(reference: str) -> tuple[str, ...]:
    path = normalize_model_reference(reference)
    if path.startswith("block/"):
        return (path,)
    return (path, f"block/{path}")


def read_glb_json(path: Path) -> dict[str, Any]:
    raw = path.read_bytes()
    if len(raw) < 20:
        raise ValueError("GLB is shorter than its header")
    magic, version, length = struct.unpack_from("<4sII", raw, 0)
    if magic != b"glTF" or version != 2:
        raise ValueError("not a glTF 2.0 binary")
    if length > len(raw):
        raise ValueError("GLB length exceeds file size")
    chunk_length, chunk_type = struct.unpack_from("<II", raw, 12)
    if chunk_type != 0x4E4F534A:
        raise ValueError("first GLB chunk is not JSON")
    payload = raw[20 : 20 + chunk_length].rstrip(b" \t\r\n\0")
    return json.loads(payload.decode("utf-8"))


def scan_gltf(
    data: dict[str, Any],
    path: Path,
    display_path: Path,
    candidates: dict[str, dict[str, Any]],
) -> None:
    add_candidate(
        candidates,
        name=path.stem,
        source=display_path,
        kind=data.get("_kind", "gltf"),
        evidence="glTF file name",
        confidence="medium",
        warnings=["Confirm whether the glTF scene is one block or multiple blocks."],
    )
    for node in data.get("nodes", []):
        if not isinstance(node, dict) or not isinstance(node.get("name"), str):
            continue
        name = node["name"].strip()
        if not name or snake(name) in GENERIC_NAMES:
            continue
        add_candidate(
            candidates,
            name=f"{path.stem}_{name}",
            source=display_path,
            kind=data.get("_kind", "gltf"),
            evidence=f"glTF node: {name}",
            confidence="low",
            warnings=["glTF node may be a model part rather than a block."],
        )


def collect_files(inputs: list[Path]) -> tuple[list[Path], list[dict[str, str]]]:
    files: list[Path] = []
    issues: list[dict[str, str]] = []
    for supplied in inputs:
        if supplied.is_dir():
            files.extend(path for path in supplied.rglob("*") if path.is_file())
        elif supplied.is_file():
            files.append(supplied)
        else:
            issues.append({"source": str(supplied), "warning": "path does not exist"})
    return sorted(set(files)), issues


def analyze(
    inputs: list[Path],
    repo: Path,
    include_model_json_as_candidates: bool,
) -> dict[str, Any]:
    files, issues = collect_files(inputs)
    candidates: dict[str, dict[str, Any]] = {}
    ignored: list[str] = []
    known_block_names: set[str] = set()
    direct_model_references: set[str] = set()
    model_data: dict[str, dict[str, Any]] = {}
    for path in files:
        if path.suffix.lower() != ".json":
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, UnicodeError, json.JSONDecodeError):
            continue
        if isinstance(data, dict) and ("variants" in data or "multipart" in data):
            known_block_names.add(snake(path.stem))
            for reference in blockstate_model_references(data):
                direct_model_references.update(expand_model_reference(reference))
        elif isinstance(data, dict) and any(
            key in data for key in ("textures", "elements", "parent", "display")
        ):
            model_path = model_path_from_file(path)
            if model_path is not None:
                model_data[model_path] = data
        if isinstance(data, dict):
            for reference in model_field_references(data):
                direct_model_references.update(expand_model_reference(reference))

    referenced_model_paths = set(direct_model_references)
    pending = list(direct_model_references)
    while pending:
        model_path = pending.pop()
        data = model_data.get(model_path)
        if not isinstance(data, dict):
            continue
        parent = data.get("parent")
        if not isinstance(parent, str):
            continue
        for parent_path in expand_model_reference(parent):
            if parent_path not in referenced_model_paths:
                referenced_model_paths.add(parent_path)
                pending.append(parent_path)
    has_blockstates = bool(known_block_names)
    for path in files:
        try:
            display = path.resolve().relative_to(repo)
        except ValueError:
            display = path.resolve()
        suffix = path.suffix.lower()
        try:
            if suffix == ".obj":
                parse_obj(
                    path,
                    display,
                    candidates,
                    issues,
                    known_block_names,
                )
            elif suffix in {".json", ".bbmodel"}:
                data = json.loads(path.read_text(encoding="utf-8"))
                scan_json(
                    data,
                    path,
                    display,
                    candidates,
                    include_model_json_as_candidates,
                    referenced_model_paths,
                    issues,
                )
            elif suffix == ".gltf":
                data = json.loads(path.read_text(encoding="utf-8"))
                if isinstance(data, dict):
                    scan_gltf(data, path, display, candidates)
            elif suffix == ".glb":
                data = read_glb_json(path)
                data["_kind"] = "glb"
                scan_gltf(data, path, display, candidates)
            elif suffix in {".mtl", ".png", ".jpg", ".jpeg", ".webp"}:
                continue
            else:
                ignored.append(str(display))
        except (OSError, UnicodeError, json.JSONDecodeError, ValueError, struct.error) as exc:
            issues.append({"source": str(display), "warning": str(exc)})

    ordered = sorted(
        candidates.values(),
        key=lambda item: (
            {"high": 0, "medium": 1, "low": 2}[item["confidence"]],
            item["proposed_block_id"],
            item["sources"],
        ),
    )
    return {
        "requires_user_confirmation": True,
        "input_files_scanned": len(files),
        "blockstate_files_found": has_blockstates,
        "candidates": ordered,
        "ignored_files": ignored,
        "issues": issues,
    }


def print_markdown(data: dict[str, Any]) -> None:
    print("# Model block candidates")
    print()
    print("These are proposals. Confirm, rename, merge, split, or remove rows before implementation.")
    print()
    print("| Proposed block | Source/evidence | Candidate factory (verify later) | Confidence | Notes |")
    print("|---|---|---|---|---|")
    for item in data["candidates"]:
        evidence = "`" + "`, `".join(item["sources"]) + "`: " + "; ".join(
            item["evidence"]
        )
        notes = " ".join(item["warnings"])
        print(
            f"| `{item['proposed_block_id']}` | {evidence} | "
            f"{item['suggested_factory']} | {item['confidence']} | {notes} |"
        )
    if not data["candidates"]:
        print("| _(none)_ | No supported block candidates found |  |  |  |")
    if data["issues"]:
        print()
        print("## Warnings")
        for issue in data["issues"]:
            print(f"- `{issue['source']}`: {issue['warning']}")
    if data["ignored_files"]:
        print()
        print("## Ignored files")
        for path in data["ignored_files"][:100]:
            print(f"- `{path}`")
        if len(data["ignored_files"]) > 100:
            print(f"- ... {len(data['ignored_files']) - 100} more")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("paths", type=Path, nargs="+")
    parser.add_argument("--repo", type=Path, default=Path.cwd())
    parser.add_argument("--format", choices=("markdown", "json"), default="markdown")
    parser.add_argument("--output", type=Path)
    parser.add_argument(
        "--include-model-json-as-candidates",
        action="store_true",
        help="When blockstates are present, still treat standalone model JSON files as candidates.",
    )
    args = parser.parse_args()

    data = analyze(
        args.paths,
        args.repo.resolve(),
        args.include_model_json_as_candidates,
    )
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        if args.format == "json":
            text = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
        else:
            from contextlib import redirect_stdout
            import io

            buffer = io.StringIO()
            with redirect_stdout(buffer):
                print_markdown(data)
            text = buffer.getvalue()
        args.output.write_text(text, encoding="utf-8")
        print(args.output)
    elif args.format == "json":
        print(json.dumps(data, ensure_ascii=False, indent=2))
    else:
        print_markdown(data)
    return 0


if __name__ == "__main__":
    sys.exit(main())
