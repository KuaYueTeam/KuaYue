#!/usr/bin/env python3
"""Structural, schema, claim, and resource-closure validation for a KuaYue pack."""

from __future__ import annotations

import argparse
import copy
import json
import re
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any, Iterable

from model_checks import (
    bbmodel_geometry_issue,
    model_rotation_issues,
    png_alpha_profile,
    render_type_issue,
)


KNOWN_UPSTREAM_MODEL_GAPS = {
    "kuayue:block/df11g/door/frame_door_cabin_df11g_left",
    "kuayue:block/df11g/door/frame_door_cabin_df11g_right",
}
MODEL_FILE_SUFFIXES = (
    ".geo.json",
    ".bbmodel",
    ".gltf",
    ".glb",
    ".obj",
    ".pmx",
    ".fmt",
    ".json",
)
DIRECTIONAL_MODEL_TOKENS = {"endface", "head", "cowcatcher", "outrigger", "pilot"}
DIRECTIONAL_FACTORY_TYPES = {
    "air_vent",
    "custom_rendered_door",
    "custom_rendered_end_face",
    "double_rotate_door",
    "freight_end_face",
    "full_shape_directional",
    "head",
    "height_slab",
    "hinge_slab",
    "hxd3d_cowcatcher",
    "jy290_ac",
    "jy_maintenance_endface",
    "jy_maintenance_outrigger",
    "jy_maintenance_head",
    "ladder",
    "level_panel",
    "level_window",
    "meter_carriage_end_face",
    "proxy_model",
    "single_arm_pantograph",
    "skirt",
    "slab",
    "slab_top",
    "train_door",
    "train_hinge_panel",
    "train_openable_window",
    "train_panel",
    "train_small_window",
    "train_unopenable_small_window",
    "train_unopenable_window",
}
FACING_DIRECTIONS = ("east", "north", "south", "west")
PROXY_MODEL_BLOCK_ENTITIES = {"proxy_model_be", "train_panel_proxy_be"}

TRAIN_ROOT_FIELDS = {
    "id",
    "category",
    "name_key",
    "description_key",
    "members",
    "creative",
}
TRAIN_MEMBER_FIELDS = (
    "blocks",
    "items",
    "item_tags",
    "bogey_sizes",
    "bogey_motions",
    "bogey_models",
    "bogey_blocks",
    "bogey_styles",
    "bogey_style_variants",
)
TRAIN_MEMBER_DEFINITION_TYPES = {
    "blocks": "blocks",
    "items": "items",
    "bogey_sizes": "bogey_sizes",
    "bogey_motions": "bogey_motions",
    "bogey_models": "bogey_models",
    "bogey_blocks": "bogey_blocks",
    "bogey_styles": "bogey_styles",
    "bogey_style_variants": "bogey_style_variants",
}
TRAIN_CREATIVE_FIELDS = {"tab", "icon_item", "filter_order"}
SHAPE_SLOTS = (
    "shape",
    "collision",
    "visual",
    "interaction",
    "support",
    "occlusion",
)
SHAPE_STATE_RULE_FIELDS = {"when", *SHAPE_SLOTS}
SHAPE_ROOT_FIELDS = {*SHAPE_SLOTS, "states"}
DEFINITION_TYPES = (
    "shapes",
    "blocks",
    "items",
    "bogey_sizes",
    "bogey_motions",
    "bogey_models",
    "bogey_blocks",
    "bogey_styles",
    "bogey_style_variants",
    "trains",
)
EXPLICIT_LOCATION_PATTERN = re.compile(
    r"^[a-z0-9_.-]+:[a-z0-9_.-]+(?:/[a-z0-9_.-]+)*$"
)


class Report:
    def __init__(self) -> None:
        self.errors: list[str] = []
        self.warnings: list[str] = []
        self.info: list[str] = []

    def error(self, message: str) -> None:
        self.errors.append(message)

    def warn(self, message: str) -> None:
        self.warnings.append(message)

    def note(self, message: str) -> None:
        self.info.append(message)


def load_json(path: Path, report: Report) -> Any | None:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except OSError as exc:
        report.error(f"{path}: cannot read: {exc}")
    except json.JSONDecodeError as exc:
        report.error(f"{path}: invalid JSON at {exc.lineno}:{exc.colno}: {exc.msg}")
    return None


def location(value: str) -> tuple[str, str] | None:
    if ":" not in value:
        return None
    namespace, path = value.split(":", 1)
    if not namespace or not path:
        return None
    return namespace, path


def flatten_strings(value: Any, parent_key: str = "") -> Iterable[tuple[str, str]]:
    if isinstance(value, dict):
        for key, child in value.items():
            yield from flatten_strings(child, str(key))
    elif isinstance(value, list):
        for child in value:
            yield from flatten_strings(child, parent_key)
    elif isinstance(value, str):
        yield parent_key, value


def is_explicit_location(value: Any) -> bool:
    return (
        isinstance(value, str)
        and EXPLICIT_LOCATION_PATTERN.fullmatch(value) is not None
    )


def temporary_pack_reason(pack_root: Path) -> str | None:
    parts = pack_root.parts
    if "run" in parts:
        return "runtime directory"
    for index, part in enumerate(parts[:-1]):
        if part == "build" and parts[index + 1] == "generated":
            return "generated build output"
    for index, part in enumerate(parts[:-1]):
        if part == "src" and parts[index + 1] == "compiled":
            return "compiled source output"
    return None


def merge_overlay(target: dict[str, Any], source: dict[str, Any]) -> None:
    for key, incoming in source.items():
        existing = target.get(key)
        if isinstance(existing, dict) and isinstance(incoming, dict):
            merge_overlay(existing, incoming)
        else:
            target[key] = copy.deepcopy(incoming)


def validate_train_resource_list(
    value: Any,
    *,
    train_label: str,
    pointer: str,
    report: Report,
) -> None:
    if not isinstance(value, list):
        report.error(f"{train_label} {pointer}: expected an array of namespaced IDs")
        return
    seen: set[str] = set()
    for index, resource_id in enumerate(value):
        entry_pointer = f"{pointer}/{index}"
        if not is_explicit_location(resource_id):
            report.error(
                f"{train_label} {entry_pointer}: expected an explicit "
                f"`namespace:path` ID, got {resource_id!r}"
            )
            continue
        if resource_id in seen:
            report.error(
                f"{train_label} {entry_pointer}: duplicate train membership ID "
                f"`{resource_id}`"
            )
        seen.add(resource_id)


def validate_train_patch(
    source: Path,
    train: Any,
    report: Report,
) -> str | None:
    if not isinstance(train, dict):
        report.error(f"{source}: each item in `trains` must be an object")
        return None

    train_id = train.get("id")
    label_id = train_id if isinstance(train_id, str) and train_id else "(missing id)"
    train_label = f"{source}: train `{label_id}`"

    unknown_root = sorted(set(train) - TRAIN_ROOT_FIELDS)
    if unknown_root:
        report.error(
            f"{train_label}: unsupported field(s) {', '.join(unknown_root)}; "
            "remove them or use a supported top-level train field"
        )
    if not is_explicit_location(train_id):
        report.error(
            f"{train_label} /id: expected an explicit `namespace:path` ID"
        )

    for field in ("category", "name_key", "description_key"):
        value = train.get(field)
        if field in train and (
            not isinstance(value, str) or not value.strip()
        ):
            report.error(
                f"{train_label} /{field}: expected a non-blank string"
            )

    members = train.get("members")
    if members is not None:
        if not isinstance(members, dict):
            report.error(f"{train_label} /members: expected an object")
        else:
            unknown_members = sorted(set(members) - set(TRAIN_MEMBER_FIELDS))
            if unknown_members:
                report.error(
                    f"{train_label} /members: unsupported field(s) "
                    f"{', '.join(unknown_members)}"
                )
            for field, value in members.items():
                if field in TRAIN_MEMBER_FIELDS:
                    validate_train_resource_list(
                        value,
                        train_label=train_label,
                        pointer=f"/members/{field}",
                        report=report,
                    )

    creative = train.get("creative")
    if creative is not None:
        if not isinstance(creative, dict):
            report.error(f"{train_label} /creative: expected an object")
        else:
            unknown_creative = sorted(set(creative) - TRAIN_CREATIVE_FIELDS)
            if unknown_creative:
                report.error(
                    f"{train_label} /creative: unsupported field(s) "
                    f"{', '.join(unknown_creative)}"
                )
            for field in ("tab", "icon_item"):
                if field in creative and not is_explicit_location(creative[field]):
                    report.error(
                        f"{train_label} /creative/{field}: expected an explicit "
                        "`namespace:path` ID"
                    )
            filter_order = creative.get("filter_order")
            if "filter_order" in creative and (
                not isinstance(filter_order, int) or isinstance(filter_order, bool)
            ):
                report.error(
                    f"{train_label} /creative/filter_order: expected an integer"
                )

    return train_id if isinstance(train_id, str) else None


def discover_pack_roots(repo: Path, pack_root: Path) -> dict[str, Path]:
    bases = (
        pack_root.parent,
        repo / "modules/train-parts/resources/packs",
        repo / "examples/packs",
    )
    candidates: set[Path] = set()
    for base in bases:
        if not base.is_dir():
            continue
        if (base / "pack.json").is_file():
            candidates.add(base.resolve())
        for manifest in base.glob("*/pack.json"):
            candidates.add(manifest.parent.resolve())

    roots: dict[str, Path] = {}
    for candidate in sorted(candidates):
        try:
            manifest = json.loads(
                (candidate / "pack.json").read_text(encoding="utf-8")
            )
        except (OSError, json.JSONDecodeError):
            continue
        pack_id = manifest.get("id")
        if isinstance(pack_id, str) and pack_id and pack_id not in roots:
            roots[pack_id] = candidate
    return roots


def collect_pack_declarations(
    pack_root: Path,
    report: Report,
) -> tuple[
    dict[str, Any] | None,
    dict[str, set[str]],
    list[tuple[str, Path, dict[str, Any]]],
    set[str],
]:
    manifest_path = pack_root / "pack.json"
    manifest = load_json(manifest_path, report)
    if not isinstance(manifest, dict):
        return None, {}, [], set()

    owner = manifest.get("id")
    if not isinstance(owner, str):
        owner = pack_root.name
    namespaces = {
        namespace
        for namespace in manifest.get("namespaces", [])
        if isinstance(namespace, str)
    }
    definitions = {definition_type: set() for definition_type in DEFINITION_TYPES}
    train_patches: list[tuple[str, Path, dict[str, Any]]] = []

    for parcel_rel in manifest.get("parcels", []):
        if not isinstance(parcel_rel, str):
            continue
        parcel_path = pack_root / parcel_rel
        parcel = load_json(parcel_path, report)
        if not isinstance(parcel, dict):
            continue
        for index_rel in parcel.get("indexes", []):
            if not isinstance(index_rel, str):
                continue
            index_path = parcel_path.parent / index_rel
            index = load_json(index_path, report)
            if not isinstance(index, dict):
                continue
            parts = Path(index_rel).parts
            if len(parts) < 4 or parts[0] != "data":
                continue
            data_root = parcel_path.parent / "data" / parts[1]
            for content_rel in index.get("on_register", []):
                if not isinstance(content_rel, str):
                    continue
                content_path = data_root / content_rel
                content = load_json(content_path, report)
                if not isinstance(content, dict):
                    continue
                for definition_type in DEFINITION_TYPES:
                    values = content.get(definition_type, [])
                    if not isinstance(values, list):
                        continue
                    for definition in values:
                        if not isinstance(definition, dict):
                            continue
                        definition_id = definition.get("id")
                        if isinstance(definition_id, str):
                            definitions[definition_type].add(definition_id)
                for train in content.get("trains", []):
                    validate_train_patch(content_path, train, report)
                    if isinstance(train, dict) and isinstance(train.get("id"), str):
                        train_patches.append((owner, content_path, train))

    return manifest, definitions, train_patches, namespaces


def collect_dependency_context(
    repo: Path,
    pack_root: Path,
    manifest: dict[str, Any],
    report: Report,
) -> dict[str, Any]:
    dependencies = manifest.get("dependencies")
    dependency_map = dependencies.get("packs") if isinstance(dependencies, dict) else {}
    if not isinstance(dependency_map, dict):
        dependency_map = {}
    direct_dependencies = {
        dependency_id
        for dependency_id in dependency_map
        if isinstance(dependency_id, str)
    }

    roots_by_id = discover_pack_roots(repo, pack_root)
    definitions = {definition_type: set() for definition_type in DEFINITION_TYPES}
    train_patches: list[tuple[str, Path, dict[str, Any]]] = []
    namespaces: set[str] = set()
    visited: set[str] = set()
    missing: set[str] = set()
    pending = sorted(direct_dependencies)

    while pending:
        dependency_id = pending.pop()
        if dependency_id in visited:
            continue
        visited.add(dependency_id)
        dependency_root = roots_by_id.get(dependency_id)
        if dependency_root is None:
            missing.add(dependency_id)
            continue
        dependency_manifest, dependency_definitions, dependency_trains, dependency_namespaces = (
            collect_pack_declarations(dependency_root, report)
        )
        if dependency_manifest is None:
            continue
        for definition_type, ids in dependency_definitions.items():
            definitions[definition_type].update(ids)
        train_patches.extend(dependency_trains)
        namespaces.update(dependency_namespaces)
        nested_dependencies = dependency_manifest.get("dependencies")
        nested_packs = (
            nested_dependencies.get("packs")
            if isinstance(nested_dependencies, dict)
            else {}
        )
        if isinstance(nested_packs, dict):
            pending.extend(
                dependency
                for dependency in nested_packs
                if isinstance(dependency, str) and dependency not in visited
            )

    for dependency_id in sorted(missing):
        report.warn(
            f"dependency pack `{dependency_id}` was not found in the workspace; "
            "train completeness and member references may depend on it"
        )

    return {
        "definitions": definitions,
        "namespaces": namespaces,
        "train_patches": train_patches,
        "direct_dependencies": direct_dependencies,
        "all_resolved": not missing,
        "missing": missing,
    }


def validate_train_references(
    train: dict[str, Any],
    *,
    source: Path,
    definitions: dict[str, set[str]],
    owned_namespaces: set[str],
    all_dependencies_resolved: bool,
    report: Report,
) -> None:
    train_id = train.get("id", "(missing id)")
    members = train.get("members")
    if not isinstance(members, dict):
        return
    for member_field, definition_type in TRAIN_MEMBER_DEFINITION_TYPES.items():
        values = members.get(member_field)
        if not isinstance(values, list):
            continue
        for index, resource_id in enumerate(values):
            if not is_explicit_location(resource_id):
                continue
            if resource_id in definitions[definition_type]:
                continue
            namespace = resource_id.split(":", 1)[0]
            pointer = f"/members/{member_field}/{index}"
            if namespace in owned_namespaces:
                report.error(
                    f"{source}: train `{train_id}` {pointer}: `{resource_id}` "
                    f"is not declared as `{definition_type}` by this pack or an "
                    "available dependency; check the ID or add its definition"
                )
            elif all_dependencies_resolved:
                report.warn(
                    f"{source}: train `{train_id}` {pointer}: cannot verify "
                    f"`{resource_id}` from namespace `{namespace}`; confirm it is "
                    "provided by a runtime source"
                )
            else:
                report.warn(
                    f"{source}: train `{train_id}` {pointer}: cannot verify "
                    f"`{resource_id}` until all dependency packs are available"
                )


def validate_shape_selection(
    block_id: str,
    path: str,
    selection: Any,
    *,
    definitions: dict[str, set[str]],
    owned_namespaces: set[str],
    all_dependencies_resolved: bool,
    report: Report,
) -> None:
    if isinstance(selection, str):
        if selection not in {"native", "empty"}:
            report.error(
                f"block `{block_id}`: `{path}` string must be `native` or `empty`"
            )
        return
    if not isinstance(selection, dict):
        report.error(
            f"block `{block_id}`: `{path}` must be native, empty, ref, or from"
        )
        return

    if "ref" in selection:
        unknown = sorted(set(selection) - {"ref", "transform"})
        if unknown:
            report.error(
                f"block `{block_id}`: `{path}` ref selection has unsupported "
                f"field(s) {', '.join(unknown)}"
            )
        shape_id = selection.get("ref")
        if not is_explicit_location(shape_id):
            report.error(
                f"block `{block_id}`: `{path}.ref` must be an explicit "
                "`namespace:path` shape ID"
            )
        elif shape_id not in definitions["shapes"]:
            namespace = shape_id.split(":", 1)[0]
            if namespace in owned_namespaces:
                report.error(
                    f"block `{block_id}`: `{path}.ref` targets `{shape_id}`, "
                    "which is not declared by this pack or an available direct "
                    "dependency"
                )
            elif all_dependencies_resolved:
                report.warn(
                    f"block `{block_id}`: `{path}.ref` cannot verify runtime "
                    f"shape `{shape_id}`"
                )
            else:
                report.warn(
                    f"block `{block_id}`: `{path}.ref` cannot verify `{shape_id}` "
                    "until all dependency packs are available"
                )
        return

    if "from" in selection:
        unknown = sorted(set(selection) - {"from"})
        if unknown:
            report.error(
                f"block `{block_id}`: `{path}` from selection has unsupported "
                f"field(s) {', '.join(unknown)}"
            )
        target = selection.get("from")
        if target not in SHAPE_SLOTS:
            report.error(
                f"block `{block_id}`: `{path}.from` must name a shape slot"
            )
        return

    if "nodes" in selection or "output" in selection:
        report.error(
            f"block `{block_id}`: `{path}` inline shape graphs are not accepted "
            "in KuaYue vehicle bindings"
        )
        return
    report.error(f"block `{block_id}`: `{path}` is not a supported shape selection")


def validate_shape_bindings(
    block_id: str,
    params: dict[str, Any],
    *,
    definitions: dict[str, set[str]],
    owned_namespaces: set[str],
    all_dependencies_resolved: bool,
    report: Report,
) -> None:
    if "shapes" in params:
        report.error(
            f"block `{block_id}`: `params.shapes` is unsupported; "
            "rename it to `params.shape_bindings`"
        )
    if "shape_bindings" not in params:
        return
    bindings = params.get("shape_bindings")
    path = "params.shape_bindings"
    if not isinstance(bindings, dict):
        report.error(f"block `{block_id}`: `{path}` must be an object")
        return

    unknown = sorted(set(bindings) - SHAPE_ROOT_FIELDS)
    if unknown:
        report.error(
            f"block `{block_id}`: `{path}` has unsupported field(s) "
            + ", ".join(unknown)
        )
    for slot in SHAPE_SLOTS:
        if slot in bindings:
            validate_shape_selection(
                block_id,
                f"{path}.{slot}",
                bindings[slot],
                definitions=definitions,
                owned_namespaces=owned_namespaces,
                all_dependencies_resolved=all_dependencies_resolved,
                report=report,
            )

    states = bindings.get("states")
    if states is None:
        return
    if not isinstance(states, list):
        report.error(f"block `{block_id}`: `{path}.states` must be an array")
        return
    for index, rule in enumerate(states):
        rule_path = f"{path}.states[{index}]"
        if not isinstance(rule, dict):
            report.error(f"block `{block_id}`: `{rule_path}` must be an object")
            continue
        unknown = sorted(set(rule) - SHAPE_STATE_RULE_FIELDS)
        if unknown:
            report.error(
                f"block `{block_id}`: `{rule_path}` has unsupported field(s) "
                + ", ".join(unknown)
            )
        when = rule.get("when")
        if not isinstance(when, dict) or not when:
            report.error(
                f"block `{block_id}`: `{rule_path}.when` must be a non-empty object"
            )
        elif any(
            not isinstance(name, str)
            or not name
            or not isinstance(value, str)
            or not value
            for name, value in when.items()
        ):
            report.error(
                f"block `{block_id}`: `{rule_path}.when` values must be "
                "non-empty serialized state strings"
            )
        for slot in SHAPE_SLOTS:
            if slot in rule:
                validate_shape_selection(
                    block_id,
                    f"{rule_path}.{slot}",
                    rule[slot],
                    definitions=definitions,
                    owned_namespaces=owned_namespaces,
                    all_dependencies_resolved=all_dependencies_resolved,
                    report=report,
                )


def validate_effective_train(
    train_id: str,
    patches: list[tuple[str, Path, dict[str, Any]]],
    *,
    definitions: dict[str, set[str]],
    owned_namespaces: set[str],
    direct_dependencies: set[str],
    target_owner: str,
    all_dependencies_resolved: bool,
    report: Report,
) -> None:
    target_patches = [
        patch for patch in patches if patch[0] == target_owner
    ]
    lower_patches = [
        patch for patch in patches if patch[0] != target_owner
    ]
    for owner, source, _train in lower_patches:
        if owner not in direct_dependencies:
            report.error(
                f"{source}: train `{train_id}` overlays lower-priority contributor "
                f"`{owner}`, but `{target_owner}` does not directly depend on it; "
                "add it to pack.json.dependencies.packs"
            )

    merged: dict[str, Any] = {}
    for _owner, _source, train in lower_patches + target_patches:
        merge_overlay(merged, train)

    display_source = target_patches[0][1] if target_patches else patches[0][1]
    incomplete: list[str] = []
    for field in ("category", "name_key", "description_key"):
        value = merged.get(field)
        if not isinstance(value, str) or not value.strip():
            incomplete.append(f"`/{field}`")

    members = merged.get("members")
    if not isinstance(members, dict):
        incomplete.append("`/members` object")
    else:
        for field in TRAIN_MEMBER_FIELDS:
            if not isinstance(members.get(field), list):
                incomplete.append(f"`/members/{field}` array")

    creative = merged.get("creative")
    if creative is not None:
        if not isinstance(creative, dict):
            incomplete.append("`/creative` object")
        elif not is_explicit_location(creative.get("tab")):
            incomplete.append("`/creative/tab`")

    if incomplete:
        message = (
            f"{display_source}: train `{train_id}` is incomplete after merging "
            f"available sources; missing {', '.join(incomplete)}"
        )
        if all_dependencies_resolved:
            report.error(message)
        else:
            report.warn(
                message + ". This may be supplied by an unresolved dependency pack."
            )

    for _owner, source, train in patches:
        validate_train_references(
            train,
            source=source,
            definitions=definitions,
            owned_namespaces=owned_namespaces,
            all_dependencies_resolved=all_dependencies_resolved,
            report=report,
        )


def schema_validate(
    repo: Path,
    documents: list[tuple[Path, str, Any]],
    report: Report,
    require_schema: bool,
) -> None:
    try:
        from jsonschema import Draft202012Validator
        from referencing import Registry, Resource
    except ImportError:
        message = (
            "JSON Schema validation skipped because `jsonschema` and/or `referencing` "
            "is not installed; create a temporary venv and install both packages"
        )
        if require_schema:
            report.error(message)
        else:
            report.warn(message)
        return

    schema_paths = list((repo / "doc/schemas").rglob("*.schema.json"))
    schemas = {
        path.name: json.loads(path.read_text(encoding="utf-8"))
        for path in schema_paths
    }
    registry = Registry().with_resources(
        (schema["$id"], Resource.from_contents(schema))
        for schema in schemas.values()
    )
    for path, schema_name, document in documents:
        schema = schemas.get(schema_name)
        if schema is None:
            report.error(f"{path}: schema not found: {schema_name}")
            continue
        validator = Draft202012Validator(schema, registry=registry)
        errors = sorted(
            validator.iter_errors(document),
            key=lambda error: list(error.absolute_path),
        )
        for error in errors:
            pointer = "/" + "/".join(map(str, error.absolute_path))
            report.error(f"{path}: schema {pointer or '/'}: {error.message}")


def add_resource_tree(
    resources: dict[str, Path],
    root: Path,
    namespace: str,
    logical_prefix: str,
    physical_root: Path,
) -> None:
    if not physical_root.is_dir():
        return
    for path in physical_root.rglob("*"):
        if not path.is_file():
            continue
        relative = path.relative_to(physical_root).as_posix()
        logical = f"assets/{namespace}/{logical_prefix}/{relative}"
        resources[logical] = path


def collect_resources(
    pack_root: Path,
    parcel_roots: list[tuple[Path, str]],
    internal: bool,
) -> tuple[dict[str, Path], dict[str, dict[str, str]]]:
    resources: dict[str, Path] = {}
    languages: dict[str, dict[str, str]] = defaultdict(dict)
    roots = [(pack_root, "<root>")] + parcel_roots
    for root, _parcel_id in roots:
        assets = root / "assets"
        if assets.is_dir():
            for namespace_dir in assets.iterdir():
                if not namespace_dir.is_dir():
                    continue
                namespace = namespace_dir.name
                add_resource_tree(
                    resources, root, namespace, "blockstates",
                    namespace_dir / "blockstates",
                )
                add_resource_tree(
                    resources, root, namespace, "models",
                    namespace_dir / "models",
                )
                add_resource_tree(
                    resources, root, namespace, "textures",
                    namespace_dir / "textures",
                )
        if not internal:
            continue
        for category in ("blockstates", "models", "textures", "sounds"):
            add_resource_tree(
                resources, root, "kuayue", category, root / category
            )

    lang_roots: list[tuple[str, Path]] = []
    for root, _parcel_id in roots:
        lang_roots.append(("kuayue", root / "lang"))
        assets = root / "assets"
        if assets.is_dir():
            for namespace_dir in assets.iterdir():
                if namespace_dir.is_dir():
                    lang_roots.append((namespace_dir.name, namespace_dir / "lang"))
    for namespace, lang_root in lang_roots:
        if not lang_root.is_dir():
            continue
        for path in lang_root.glob("*.json"):
            data = load_json(path, Report())
            if not isinstance(data, dict):
                continue
            for key, value in data.items():
                if isinstance(value, str):
                    languages[path.stem][key] = value
    for locale, values in languages.items():
        for key, value in values.items():
            resources[f"lang/{locale}/{key}"] = Path(f"<language:{locale}:{key}>")
    return resources, languages


def resolve_resource(
    resources: dict[str, Path],
    namespace: str,
    kind: str,
    path: str,
) -> Path | None:
    if kind == "textures":
        suffix = "" if path.endswith(".png") else ".png"
    elif kind == "models" and path.endswith(MODEL_FILE_SUFFIXES):
        suffix = ""
    else:
        suffix = ".json"
    return resources.get(f"assets/{namespace}/{kind}/{path}{suffix}")


def effective_render_type(
    data: dict[str, Any],
    resources: dict[str, Path],
    namespaces: set[str],
) -> Any:
    current = data
    seen: set[str] = set()
    while isinstance(current, dict):
        render_type = current.get("render_type")
        if render_type is not None:
            return render_type
        parent = current.get("parent")
        if not isinstance(parent, str):
            return None
        parsed = location(parent)
        if parsed is None or parsed[0] not in namespaces:
            return None
        key = f"{parsed[0]}:{parsed[1]}"
        if key in seen:
            return None
        seen.add(key)
        parent_path = resolve_resource(
            resources,
            parsed[0],
            "models",
            parsed[1],
        )
        if parent_path is None:
            return None
        parent_data = load_json(parent_path, Report())
        if not isinstance(parent_data, dict):
            return None
        current = parent_data
    return None


def validate_model_transparency(
    model_file: Path,
    data: dict[str, Any],
    resources: dict[str, Path],
    namespaces: set[str],
    report: Report,
) -> None:
    elements = data.get("elements")
    if not isinstance(elements, list) or not elements:
        return
    textures = data.get("textures")
    if not isinstance(textures, dict):
        return
    profiles: list[str] = []
    for value in textures.values():
        if not isinstance(value, str) or value.startswith("#"):
            continue
        parsed = location(value)
        if parsed is None or parsed[0] not in namespaces:
            continue
        texture_file = resolve_resource(
            resources,
            parsed[0],
            "textures",
            parsed[1],
        )
        if texture_file is None or texture_file.suffix.lower() != ".png":
            continue
        profile = png_alpha_profile(texture_file)
        if profile in {"cutout", "translucent"}:
            profiles.append(profile)
    if not profiles:
        return
    profile = "translucent" if "translucent" in profiles else "cutout"
    issue = render_type_issue(
        profile,
        effective_render_type(data, resources, namespaces),
        str(model_file),
    )
    if issue is not None:
        report.warn(issue)


def validate_model_tree(
    model_id: str,
    resources: dict[str, Path],
    namespaces: set[str],
    checked: set[str],
    report: Report,
    chain: tuple[str, ...] = (),
) -> None:
    parsed = location(model_id)
    if parsed is None:
        report.error(f"model reference is not namespaced: {model_id}")
        return
    namespace, model_path = parsed
    if namespace not in namespaces:
        return
    key = f"{namespace}:{model_path}"
    if key in chain:
        report.error(f"cyclic model parent chain: {' -> '.join(chain + (key,))}")
        return
    if key in checked:
        return
    checked.add(key)
    model_file = resolve_resource(resources, namespace, "models", model_path)
    if model_file is None:
        if key in KNOWN_UPSTREAM_MODEL_GAPS:
            report.warn(f"known upstream model gap `{key}`")
        else:
            report.error(f"missing model `{key}`")
        return
    data = load_json(model_file, report)
    if not isinstance(data, dict):
        return
    for issue in model_rotation_issues(data, str(model_file)):
        report.error(issue)
    validate_model_transparency(
        model_file,
        data,
        resources,
        namespaces,
        report,
    )
    parent = data.get("parent")
    if isinstance(parent, str):
        validate_model_tree(
            parent, resources, namespaces, checked, report, chain + (key,)
        )
    textures = data.get("textures")
    if isinstance(textures, dict):
        for alias, value in textures.items():
            if not isinstance(value, str) or value.startswith("#"):
                continue
            parsed_texture = location(value)
            if parsed_texture is None:
                report.error(f"{model_file}: texture `{alias}` is not namespaced: {value}")
                continue
            texture_namespace, texture_path = parsed_texture
            if texture_namespace not in namespaces:
                continue
            if resolve_resource(
                resources, texture_namespace, "textures", texture_path
            ) is None:
                report.error(
                    f"{model_file}: missing texture `{texture_namespace}:{texture_path}`"
                )


def validate_blockstate(
    block_id: str,
    resources: dict[str, Path],
    namespaces: set[str],
    checked_models: set[str],
    report: Report,
) -> dict[str, Any] | None:
    parsed = location(block_id)
    if parsed is None:
        report.error(f"block ID is not namespaced: {block_id}")
        return None
    namespace, block_path = parsed
    blockstate = resolve_resource(resources, namespace, "blockstates", block_path)
    if blockstate is None:
        report.error(f"missing blockstate for block `{block_id}`")
        return None
    data = load_json(blockstate, report)
    if not isinstance(data, dict):
        return None
    model_refs: list[str] = []
    variants = data.get("variants")
    if isinstance(variants, dict):
        for variant in variants.values():
            if isinstance(variant, dict) and isinstance(variant.get("model"), str):
                model_refs.append(variant["model"])
            elif isinstance(variant, list):
                for entry in variant:
                    if isinstance(entry, dict) and isinstance(entry.get("model"), str):
                        model_refs.append(entry["model"])
    multipart = data.get("multipart")
    if isinstance(multipart, list):
        for part in multipart:
            if not isinstance(part, dict):
                continue
            for key in ("apply", "when"):
                value = part.get(key)
                if isinstance(value, dict) and isinstance(value.get("model"), str):
                    model_refs.append(value["model"])
                elif isinstance(value, list):
                    for entry in value:
                        if isinstance(entry, dict) and isinstance(entry.get("model"), str):
                            model_refs.append(entry["model"])
    if not model_refs:
        report.warn(f"{blockstate}: no model references found")
    for model_ref in sorted(set(model_refs)):
        validate_model_tree(
            model_ref, resources, namespaces, checked_models, report
        )
    return data


def blockstate_facing_variants(data: dict[str, Any]) -> set[str]:
    variants = data.get("variants")
    if not isinstance(variants, dict):
        return set()
    result: set[str] = set()
    for key in variants:
        if not isinstance(key, str):
            continue
        properties = {
            part.strip()
            for part in key.split(",")
            if part.strip()
        }
        for direction in FACING_DIRECTIONS:
            if f"facing={direction}" in properties:
                result.add(direction)
    return result


def validate_directional_blockstate(
    block_id: str,
    block: dict[str, Any],
    blockstate: dict[str, Any] | None,
    report: Report,
) -> None:
    if block.get("type") not in DIRECTIONAL_FACTORY_TYPES:
        return
    if blockstate is None:
        return
    present = blockstate_facing_variants(blockstate)
    missing = [direction for direction in FACING_DIRECTIONS if direction not in present]
    if missing:
        report.error(
            f"block `{block_id}`: directional factory "
            f"`{block.get('type')}` is missing facing blockstate variant(s): "
            f"{', '.join(missing)}"
        )


def resolve_model_reference(
    resources: dict[str, Path],
    namespace: str,
    model_path: str,
) -> Path | None:
    candidates: list[str] = [model_path]
    if model_path.startswith("models/"):
        candidates.append(model_path.removeprefix("models/"))
    if not model_path.startswith(("models/", "block/")):
        candidates.append(f"block/{model_path}")
    for candidate in dict.fromkeys(candidates):
        resolved = resolve_resource(resources, namespace, "models", candidate)
        if resolved is not None:
            return resolved
    return None


def is_directional_model_block(block_id: str) -> bool:
    parsed = location(block_id)
    block_path = parsed[1] if parsed is not None else block_id
    tokens = {
        token
        for token in re.split(r"[/_.:-]+", block_path)
        if token
    }
    return bool(tokens & DIRECTIONAL_MODEL_TOKENS)


def validate_native_model_asset(
    block_id: str,
    field: str,
    model_value: Any,
    resources: dict[str, Path],
    namespaces: set[str],
    report: Report,
) -> None:
    if not is_explicit_location(model_value):
        return
    namespace, model_path = location(model_value)  # type: ignore[misc]
    if namespace not in namespaces:
        return
    model_file = resolve_model_reference(resources, namespace, model_path)
    if model_file is None:
        report.error(f"block `{block_id}`: missing native model `{model_value}`")
        return
    if model_file.suffix.lower() != ".bbmodel":
        report.warn(
            f"block `{block_id}`: `{field}` does not point to a `.bbmodel`: "
            f"{model_value}"
        )
        return
    data = load_json(model_file, report)
    if not isinstance(data, dict):
        return
    issue = bbmodel_geometry_issue(data, str(model_file))
    if issue is not None:
        report.error(f"block `{block_id}`: {issue}")


def validate_proxy_native_models(
    block_id: str,
    params: dict[str, Any],
    resources: dict[str, Path],
    namespaces: set[str],
    report: Report,
) -> None:
    if "model" in params:
        validate_native_model_asset(
            block_id,
            "params.model",
            params.get("model"),
            resources,
            namespaces,
            report,
        )
    groups = params.get("model_groups")
    group_ids: set[str] = set()
    group_part_layouts: dict[str, tuple[str, ...]] = {}
    if groups is not None:
        if not isinstance(groups, dict) or not groups:
            report.error(f"block `{block_id}`: `params.model_groups` must be a non-empty object")
            groups = {}
        else:
            for group_id, group in groups.items():
                group_ids.add(group_id)
                if not isinstance(group, dict):
                    report.error(
                        f"block `{block_id}`: `model_groups.{group_id}` must be an object"
                    )
                    continue
                parts = group.get("parts")
                if not isinstance(parts, list) or not parts:
                    report.error(
                        f"block `{block_id}`: `model_groups.{group_id}.parts` "
                        "must be a non-empty array"
                    )
                    continue
                part_ids: list[str] = []
                part_id_set: set[str] = set()
                for index, part in enumerate(parts):
                    path = f"model_groups.{group_id}.parts[{index}]"
                    if not isinstance(part, dict):
                        report.error(f"block `{block_id}`: `{path}` must be an object")
                        continue
                    part_id = part.get("id")
                    if not isinstance(part_id, str) or not part_id:
                        report.error(f"block `{block_id}`: `{path}.id` must be non-empty")
                    elif part_id in part_id_set:
                        report.error(
                            f"block `{block_id}`: duplicate part id `{part_id}` "
                            f"in model group `{group_id}`"
                        )
                    else:
                        part_id_set.add(part_id)
                        part_ids.append(part_id)
                    validate_native_model_asset(
                        block_id,
                        f"params.{path}.model",
                        part.get("model"),
                        resources,
                        namespaces,
                        report,
                    )
                group_part_layouts[group_id] = tuple(part_ids)
    layouts = set(group_part_layouts.values())
    if len(layouts) > 1:
        report.error(
            f"block `{block_id}`: all `params.model_groups` must use the same "
            "ordered stable part ids"
        )
    default_model = params.get("default_model")
    if default_model is not None and default_model not in group_ids:
        report.error(
            f"block `{block_id}`: `params.default_model` references "
            f"unknown model group `{default_model}`"
        )
    model_by_state = params.get("model_by_state")
    if isinstance(model_by_state, dict):
        for property_name, states in model_by_state.items():
            if not isinstance(states, dict):
                report.error(
                    f"block `{block_id}`: `model_by_state.{property_name}` "
                    "must map state values to model groups or native model paths"
                )
                continue
            for state_value, model_value in states.items():
                if isinstance(model_value, str) and model_value in group_ids:
                    continue
                validate_native_model_asset(
                    block_id,
                    f"params.model_by_state.{property_name}.{state_value}",
                    model_value,
                    resources,
                    namespaces,
                    report,
                )


def validate_rendering_policy(
    block_id: str,
    block: dict[str, Any],
    resources: dict[str, Path],
    namespaces: set[str],
    report: Report,
) -> None:
    block_type = block.get("type")
    block_entity = block.get("block_entity")
    entity_type = (
        block_entity.get("type")
        if isinstance(block_entity, dict)
        else None
    )

    if (
        block_type == "fsm_block"
        or entity_type == "fsm_be"
        or "state_machine" in block
    ):
        report.error(
            f"block `{block_id}`: FSM rendering is forbidden in vehicle packs; "
            "use `proxy_model` with an embedded `proxy_model_be`"
        )

    if block_type != "proxy_model" and entity_type not in PROXY_MODEL_BLOCK_ENTITIES:
        return

    if block_type == "proxy_model" and entity_type != "proxy_model_be":
        report.error(
            f"block `{block_id}`: `proxy_model` requires an embedded "
            "`proxy_model_be` block entity"
        )
    block_params = block.get("params")
    if isinstance(block_params, dict) and block_params:
        if block_type == "proxy_model":
            report.error(
                f"block `{block_id}`: proxy model block-level params are not "
                "supported; implement content-specific interactions in a dedicated "
                "Java block class and factory"
            )
        validate_multi_cell_params(block_id, block_params, report)
    params = (
        block_entity.get("params")
        if isinstance(block_entity, dict)
        else None
    )
    model = params.get("model") if isinstance(params, dict) else None
    model_groups = params.get("model_groups") if isinstance(params, dict) else None
    if not is_explicit_location(model) and not isinstance(model_groups, dict):
        report.error(
            f"block `{block_id}`: proxy model block entity requires `params.model` "
            "or `params.model_groups`"
        )
    elif isinstance(params, dict):
        if "animation" in params:
            report.error(
                f"block `{block_id}`: generic `proxy_model_be.params.animation` is "
                "not supported; implement state-driven playback in dedicated Java"
            )
        validate_proxy_native_models(
            block_id,
            params,
            resources,
            namespaces,
            report,
        )

    if not is_directional_model_block(block_id):
        return
    if block_type != "proxy_model":
        return
    state_properties = block.get("state_properties")
    facing = (
        state_properties.get("facing")
        if isinstance(state_properties, dict)
        else None
    )
    if not isinstance(facing, dict):
        report.error(
            f"block `{block_id}`: head, cowcatcher, and pilot blocks require "
            "`state_properties.facing`"
        )
        return
    if facing.get("type") != "vanilla" or facing.get("property") != "HORIZONTAL_FACING":
        report.error(
            f"block `{block_id}`: `state_properties.facing` must use vanilla "
            "`HORIZONTAL_FACING`"
        )


def validate_multi_cell_params(
    block_id: str,
    params: dict[str, Any],
    report: Report,
) -> None:
    if "multi_cell" not in params:
        return
    multi_cell = params.get("multi_cell")
    if not isinstance(multi_cell, dict):
        report.error(f"block `{block_id}`: `params.multi_cell` must be an object")
        return

    if "cells" in multi_cell:
        validate_multi_cell_list(
            block_id,
            "params.multi_cell.cells",
            multi_cell.get("cells"),
            report,
        )

    if "cells_by_state" in multi_cell:
        state_rules = multi_cell.get("cells_by_state")
        if not isinstance(state_rules, dict) or not state_rules:
            report.error(
                f"block `{block_id}`: `params.multi_cell.cells_by_state` "
                "must be a non-empty object"
            )
        else:
            for property_name, values in state_rules.items():
                path = f"params.multi_cell.cells_by_state.{property_name}"
                if not isinstance(values, dict) or not values:
                    report.error(
                        f"block `{block_id}`: `{path}` must be a non-empty object"
                    )
                    continue
                for state_name, cells in values.items():
                    validate_multi_cell_list(
                        block_id,
                        f"{path}.{state_name}",
                        cells,
                        report,
                    )

    if "state_cells" in multi_cell:
        state_rules = multi_cell.get("state_cells")
        if not isinstance(state_rules, list) or not state_rules:
            report.error(
                f"block `{block_id}`: `params.multi_cell.state_cells` "
                "must be a non-empty array"
            )
        else:
            for index, rule in enumerate(state_rules):
                path = f"params.multi_cell.state_cells[{index}]"
                if not isinstance(rule, dict):
                    report.error(f"block `{block_id}`: `{path}` must be an object")
                    continue
                unknown = sorted(set(rule) - {"when", "cells"})
                if unknown:
                    report.error(
                        f"block `{block_id}`: `{path}` has unsupported field(s) "
                        + ", ".join(unknown)
                    )
                when = rule.get("when")
                if not isinstance(when, dict) or not when:
                    report.error(
                        f"block `{block_id}`: `{path}.when` must be a "
                        "non-empty object"
                    )
                elif any(
                    not isinstance(name, str)
                    or not name
                    or not isinstance(value, str)
                    or not value
                    for name, value in when.items()
                ):
                    report.error(
                        f"block `{block_id}`: `{path}.when` values must be "
                        "non-empty serialized state strings"
                    )
                if "cells" not in rule:
                    report.error(
                        f"block `{block_id}`: `{path}.cells` is required"
                    )
                else:
                    validate_multi_cell_list(
                        block_id,
                        f"{path}.cells",
                        rule.get("cells"),
                        report,
                        allow_empty=True,
                    )

    if not any(
        field in multi_cell for field in ("cells", "cells_by_state", "state_cells")
    ):
        report.error(
            f"block `{block_id}`: `params.multi_cell` requires `cells`, "
            "`cells_by_state`, or `state_cells`"
        )


def validate_multi_cell_list(
    block_id: str,
    path: str,
    cells: Any,
    report: Report,
    *,
    allow_empty: bool = False,
) -> None:
    minimum = 0 if allow_empty else 1
    if not isinstance(cells, list) or not minimum <= len(cells) <= 64:
        report.error(
            f"block `{block_id}`: `{path}` must contain "
            f"between {minimum} and 64 offsets"
        )
        return
    seen: set[tuple[int, int, int]] = set()
    for index, cell in enumerate(cells):
        if (
            not isinstance(cell, list)
            or len(cell) != 3
            or any(not isinstance(value, int) or isinstance(value, bool) for value in cell)
            or any(abs(value) > 16 for value in cell)
        ):
            report.error(
                f"block `{block_id}`: `{path}[{index}]` "
                "must be an integer [x,y,z] offset within -16..16"
            )
            continue
        offset = (cell[0], cell[1], cell[2])
        if offset == (0, 0, 0):
            report.error(
                f"block `{block_id}`: `{path}` must not "
                "contain the anchor [0,0,0]"
            )
        elif offset in seen:
            report.error(
                f"block `{block_id}`: duplicate `{path}` "
                f"offset {index}"
            )
        seen.add(offset)


def validate_pack(
    repo: Path,
    pack_root: Path,
    require_schema: bool,
    require_durable_source: bool = False,
) -> Report:
    report = Report()
    pack_root = pack_root.resolve()
    if not pack_root.is_dir():
        report.error(f"pack root does not exist: {pack_root}")
        return report
    temporary_reason = temporary_pack_reason(pack_root)
    if temporary_reason is not None:
        message = (
            f"{pack_root}: pack lives in a {temporary_reason}; use it for client "
            "inspection only and copy accepted changes back to a durable source "
            "outside `run/`, `build/`, and `src/compiled/`"
        )
        if require_durable_source:
            report.error(message)
        else:
            report.warn(message)

    manifest_path = pack_root / "pack.json"
    manifest = load_json(manifest_path, report)
    if not isinstance(manifest, dict):
        return report

    internal = "modules/train-parts/resources/packs" in pack_root.as_posix()
    if internal:
        for required in ("pack.mcmeta", "icon.png"):
            if not (pack_root / required).is_file():
                report.error(f"internal preset is missing `{required}`")

    namespaces = set(manifest.get("namespaces", []))
    parcel_entries = manifest.get("parcels", [])
    if not isinstance(parcel_entries, list) or not parcel_entries:
        report.error("`pack.json.parcels` must be a non-empty array")
        parcel_entries = []

    declared_paths = list(dict.fromkeys(str(entry) for entry in parcel_entries))
    actual_paths = sorted(
        path.relative_to(pack_root).as_posix()
        for path in (pack_root / "parcels").glob("*/parcel.json")
    )
    if set(declared_paths) != set(actual_paths):
        report.error(
            "pack parcel declarations do not match directories: "
            f"declared={sorted(declared_paths)}, actual={actual_paths}"
        )

    schema_documents: list[tuple[Path, str, Any]] = [
        (manifest_path, "pack.schema.json", manifest)
    ]
    parcel_roots: list[tuple[Path, str]] = []
    indexes: list[tuple[Path, dict[str, Any]]] = []
    content_files: list[Path] = []
    block_claims: dict[str, list[str]] = defaultdict(list)
    item_claims: dict[str, list[str]] = defaultdict(list)
    group_claims: dict[str, list[str]] = defaultdict(list)
    blocks: dict[str, dict[str, Any]] = {}
    items: set[str] = set()
    trains: list[tuple[Path, dict[str, Any]]] = []
    train_claims: dict[str, list[str]] = defaultdict(list)
    local_definitions = {
        definition_type: set() for definition_type in DEFINITION_TYPES
    }

    for parcel_rel in declared_paths:
        parcel_path = pack_root / parcel_rel
        parcel = load_json(parcel_path, report)
        if not isinstance(parcel, dict):
            continue
        parcel_id = parcel.get("id")
        if not isinstance(parcel_id, str):
            report.error(f"{parcel_path}: missing string `id`")
            continue
        parcel_roots.append((parcel_path.parent, parcel_id))
        schema_documents.append((parcel_path, "parcel.schema.json", parcel))
        if ":" in parcel_id and parcel_id.split(":", 1)[0] not in namespaces:
            report.error(f"{parcel_path}: parcel namespace is not declared by pack")

        for index_rel in parcel.get("indexes", []):
            index_path = parcel_path.parent / index_rel
            index = load_json(index_path, report)
            if not isinstance(index, dict):
                continue
            schema_documents.append((index_path, "index.schema.json", index))
            indexes.append((index_path, index))
            parts = Path(index_rel).parts
            if len(parts) < 4 or parts[0] != "data":
                report.error(f"{index_path}: cannot derive data namespace from index path")
                continue
            namespace = parts[1]
            data_root = parcel_path.parent / "data" / namespace
            for lifecycle, schema_name in (
                ("on_register", "content.schema.json"),
                ("on_reload", "animation-clips.schema.json"),
            ):
                for content_rel in index.get(lifecycle, []):
                    content_path = data_root / content_rel
                    content = load_json(content_path, report)
                    if content is None:
                        continue
                    document_schema_name = (
                        "shapes.schema.json"
                        if lifecycle == "on_register"
                        and isinstance(content, dict)
                        and set(content) == {"shapes"}
                        else schema_name
                    )
                    schema_documents.append(
                        (content_path, document_schema_name, content)
                    )
                    content_files.append(content_path)
                    if lifecycle != "on_register" or not isinstance(content, dict):
                        continue
                    for definition_type in DEFINITION_TYPES:
                        values = content.get(definition_type, [])
                        if not isinstance(values, list):
                            continue
                        for definition in values:
                            if not isinstance(definition, dict):
                                continue
                            definition_id = definition.get("id")
                            if isinstance(definition_id, str):
                                local_definitions[definition_type].add(definition_id)
                    for group in content.get("registry_groups", []):
                        if isinstance(group, dict) and isinstance(group.get("id"), str):
                            group_claims[group["id"]].append(str(content_path))
                    for block in content.get("blocks", []):
                        if not isinstance(block, dict) or not isinstance(block.get("id"), str):
                            continue
                        block_id = block["id"]
                        blocks[block_id] = block
                        block_claims[block_id].append(str(content_path))
                    for item in content.get("items", []):
                        if not isinstance(item, dict) or not isinstance(item.get("id"), str):
                            continue
                        item_id = item["id"]
                        items.add(item_id)
                        item_claims[item_id].append(str(content_path))
                    for train in content.get("trains", []):
                        if not isinstance(train, dict) or not isinstance(train.get("id"), str):
                            continue
                        trains.append((content_path, train))
                        train_claims[train["id"]].append(str(content_path))

    for label, claims in (
        ("registry group", group_claims),
        ("block", block_claims),
        ("item", item_claims),
    ):
        for claim_id, owners in claims.items():
            if len(owners) > 1:
                report.error(
                    f"duplicate {label} claim `{claim_id}` in "
                    + ", ".join(sorted(set(owners)))
                )
    for train_id, owners in train_claims.items():
        if len(owners) > 1:
            report.error(
                f"train `{train_id}` is declared more than once in the same pack: "
                + ", ".join(sorted(set(owners)))
            )

    local_trains_by_id: dict[str, list[tuple[Path, dict[str, Any]]]] = defaultdict(list)
    for source, train in trains:
        validate_train_patch(source, train, report)
        train_id = train.get("id")
        if isinstance(train_id, str):
            local_trains_by_id[train_id].append((source, train))

    target_owner = manifest.get("id")
    if not isinstance(target_owner, str):
        target_owner = pack_root.name
    dependency_context = collect_dependency_context(
        repo, pack_root, manifest, report
    )
    combined_definitions = {
        definition_type: set(local_definitions[definition_type])
        | set(dependency_context["definitions"][definition_type])
        for definition_type in DEFINITION_TYPES
    }
    owned_namespaces = namespaces | dependency_context["namespaces"]
    dependency_trains_by_id: dict[
        str, list[tuple[str, Path, dict[str, Any]]]
    ] = defaultdict(list)
    for owner, source, train in dependency_context["train_patches"]:
        dependency_trains_by_id[train["id"]].append((owner, source, train))
    for train_id, local_patches in local_trains_by_id.items():
        if len(local_patches) != 1:
            continue
        target_patches = [
            (target_owner, source, train) for source, train in local_patches
        ]
        validate_effective_train(
            train_id,
            dependency_trains_by_id.get(train_id, []) + target_patches,
            definitions=combined_definitions,
            owned_namespaces=owned_namespaces,
            direct_dependencies=dependency_context["direct_dependencies"],
            target_owner=target_owner,
            all_dependencies_resolved=dependency_context["all_resolved"],
            report=report,
        )

    resources, languages = collect_resources(pack_root, parcel_roots, internal)
    checked_models: set[str] = set()
    for block_id, block in sorted(blocks.items()):
        block_params = block.get("params")
        if isinstance(block_params, dict):
            validate_shape_bindings(
                block_id,
                block_params,
                definitions=combined_definitions,
                owned_namespaces=owned_namespaces,
                all_dependencies_resolved=dependency_context["all_resolved"],
                report=report,
            )
        validate_rendering_policy(block_id, block, resources, namespaces, report)
        blockstate = validate_blockstate(
            block_id,
            resources,
            namespaces,
            checked_models,
            report,
        )
        validate_directional_blockstate(
            block_id,
            block,
            blockstate,
            report,
        )
        parsed = location(block_id)
        if parsed is None:
            report.error(f"block ID is not namespaced: {block_id}")
            continue
        namespace, block_path = parsed

        item_model = resolve_resource(resources, namespace, "models", f"item/{block_path}")
        if item_model is None:
            report.warn(f"no item model found for block `{block_id}`")

        loot_candidates = [
            parcel_root / "data" / namespace / "loot_table" / "blocks" / f"{block_path}.json"
            for parcel_root, _ in parcel_roots
        ] + [
            parcel_root / "loots" / "blocks" / f"{block_path}.json"
            for parcel_root, _ in parcel_roots
        ]
        if not any(path.is_file() for path in loot_candidates):
            report.warn(f"no loot table found for block `{block_id}`")

        for locale in ("zh_cn", "en_us"):
            key = f"block.{namespace}.{block_path.replace('/', '.')}"
            if key not in languages.get(locale, {}):
                message = f"language `{locale}` is missing `{key}`"
                if internal:
                    report.error(message)
                else:
                    report.warn(message)

        for key, value in flatten_strings(block):
            if "model" not in key.lower() or not isinstance(value, str):
                continue
            if (
                "block_entity.params.model_groups" in key
                or "block_entity.params.model_by_state" in key
                or key.endswith("default_model")
            ):
                continue
            parsed_model = location(value)
            if parsed_model is None:
                report.warn(
                    f"{block_id}: model-like field `{key}` is not namespaced: {value}; "
                    f"checking it in `{namespace}`"
                )
                model_namespace, model_path = namespace, value
            else:
                model_namespace, model_path = parsed_model
            if model_namespace not in namespaces:
                continue
            resolved = resolve_model_reference(
                resources, model_namespace, model_path
            )
            if resolved is None:
                report.error(f"{block_id}: missing model `{value}` from field `{key}`")

    for pack_key in (
        manifest.get("name_key"),
        manifest.get("description_key"),
    ):
        if isinstance(pack_key, str) and pack_key:
            for locale in ("zh_cn", "en_us"):
                if pack_key not in languages.get(locale, {}):
                    message = f"language `{locale}` is missing `{pack_key}`"
                    if internal:
                        report.error(message)
                    else:
                        report.warn(message)
    for _parcel_root, parcel_id in parcel_roots:
        for parcel_rel in declared_paths:
            parcel_path = pack_root / parcel_rel
            parcel = load_json(parcel_path, Report())
            if not isinstance(parcel, dict) or parcel.get("id") != parcel_id:
                continue
            for field in ("name_key", "description_key"):
                key = parcel.get(field)
                if isinstance(key, str) and key:
                    for locale in ("zh_cn", "en_us"):
                        if key not in languages.get(locale, {}):
                            report.warn(
                                f"language `{locale}` is missing parcel key `{key}`"
                            )
    for _source, train in trains:
        for field in ("name_key", "description_key"):
            key = train.get(field)
            if not isinstance(key, str) or not key:
                continue
            for locale in ("zh_cn", "en_us"):
                if key not in languages.get(locale, {}):
                    message = (
                        f"language `{locale}` is missing train "
                        f"`{train.get('id')}` key `{key}`"
                    )
                    if internal:
                        report.error(message)
                    else:
                        report.warn(message)

    schema_validate(repo, schema_documents, report, require_schema)
    report.note(f"layout: {'internal preset' if internal else 'standard/external'}")
    report.note(f"parcels: {len(parcel_roots)}")
    report.note(f"indexes: {len(indexes)}")
    report.note(f"content files: {len(content_files)}")
    report.note(f"blocks: {len(blocks)}")
    report.note(f"trains: {len(trains)}")
    report.note(f"explicit items: {len(items)}")
    report.note(f"resource files indexed: {sum(1 for key in resources if not key.startswith('lang/'))}")
    return report


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("pack_root", type=Path)
    parser.add_argument("--repo", type=Path, default=Path.cwd())
    parser.add_argument("--require-schema", action="store_true")
    parser.add_argument("--require-durable-source", action="store_true")
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()

    report = validate_pack(
        args.repo.resolve(),
        args.pack_root,
        args.require_schema,
        args.require_durable_source,
    )
    for message in report.info:
        print(f"INFO: {message}")
    for message in report.warnings:
        print(f"WARN: {message}")
    for message in report.errors:
        print(f"ERROR: {message}")
    print(
        f"RESULT: {len(report.errors)} error(s), "
        f"{len(report.warnings)} warning(s)"
    )
    return 1 if report.errors or (args.strict and report.warnings) else 0


if __name__ == "__main__":
    sys.exit(main())
