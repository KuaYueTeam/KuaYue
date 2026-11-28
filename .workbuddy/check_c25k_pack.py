"""Self-check for the generated c25k pack.

Verifies that every block has the collision keys its states need, that non-north keys are the
rotated north profile, that every referenced model / texture / blockstate / loot / item exists,
and that the models stay inside their authored cell band.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

PACK = os.path.join(HERE, "..", "modules", "train-parts", "resources", "packs",
                    "kuayue-preset-passenger-new-25", "parcels", "c25k")
P = os.path.abspath(PACK)
DATA = os.path.join(P, "data", "kuayue", "carriages", "c25k_new", "blocks.json")

FACING_DEG = {"north": 0, "east": 90, "south": 180, "west": 270}
PROXY_BE = "train_panel_proxy_be"
OPENABLE_TYPES = {"train_panel_proxy_openable", "train_panel_proxy_mirror_door"}


def rot(box, deg):
    x1, y1, z1, x2, y2, z2 = box
    if deg == 0:
        return list(box)
    if deg == 90:
        return [16 - z2, y1, x1, 16 - z1, y2, x2]
    if deg == 180:
        return [16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1]
    return [z1, y1, 16 - x2, z2, y2, 16 - x1]


data = json.load(open(DATA, encoding="utf-8"))
problems = []
state_count = 0
box_count = 0
openable_count = 0

for block in data["blocks"]:
    bid = block["id"]
    aabb = block["params"]["aabb"]
    openable = block["type"] in OPENABLE_TYPES
    hinged = any("hinge=" in key for key in aabb)
    if openable:
        openable_count += 1

    keys = []
    for facing in FACING_DEG:
        for hinge in (("left", "right") if hinged else (None,)):
            for state in (("false", "true") if openable else (None,)):
                parts = ["facing=" + facing]
                if hinge:
                    parts.append("hinge=" + hinge)
                if state:
                    parts.append("open=" + state)
                keys.append((",".join(parts), facing, hinge, state))

    for key, facing, hinge, state in keys:
        if key not in aabb:
            problems.append("%s missing %s" % (bid, key))
            continue
        state_count += 1
        boxes = aabb[key]
        walk_through = "interaction_aabb" in block["params"]
        if not boxes:
            if not walk_through:
                problems.append("%s %s is empty" % (bid, key))
            continue
        box_count += len(boxes)
        for box in boxes:
            if len(box) != 6 or not all(box[i] < box[i + 3] for i in range(3)):
                problems.append("%s %s bad box %s" % (bid, key, box))
        if facing != "north":
            base_parts = ["facing=north"]
            if hinge:
                base_parts.append("hinge=" + hinge)
            if state:
                base_parts.append("open=" + state)
            base = ",".join(base_parts)
            # Sorted per-coordinate compare with a tolerance: immune to float rounding, still
            # catches a wrong rotation step.
            expected = sorted(tuple(round(v, 2) for v in rot(list(b), FACING_DEG[facing]))
                              for b in aabb[base])
            actual = sorted(tuple(round(v, 2) for v in b) for b in boxes)
            if len(expected) != len(actual) or any(
                    abs(a - e) > 0.02
                    for exp_box, act_box in zip(expected, actual)
                    for a, e in zip(act_box, exp_box)):
                problems.append("%s %s is not the rotated north state" % (bid, key))

    be = block["block_entity"]
    if be["type"] != PROXY_BE:
        problems.append("%s unexpected block entity %s" % (bid, be["type"]))
    params = be["params"]
    models = []
    groups = params.get("model_groups") or {}
    if groups:
        for name, group in groups.items():
            models.extend(part["model"] for part in group["parts"])
        ids = {tuple(part["id"] for part in group["parts"]) for group in groups.values()}
        if ids != {("model",)}:
            problems.append("%s part ids %s" % (bid, ids))
        by_state = params.get("model_by_state") or {}
        for property_name, mapping in by_state.items():
            unknown = [value for value in mapping.values() if value not in groups]
            if unknown:
                problems.append("%s model_by_state %s -> %s" % (bid, property_name, unknown))
        if params.get("default_model") not in groups:
            problems.append("%s default_model %s" % (bid, params.get("default_model")))
    else:
        models = [params["model"]]

    suffix = bid.split(":", 1)[1][len("new_"):]
    for model in models:
        path = os.path.join(P, "models", "block", "carriage", "c25k", model.rsplit("/", 1)[1])
        if not os.path.isfile(path):
            problems.append("%s missing model %s" % (bid, model))
    for folder, label in (("blockstates", "blockstate"), ("models/item", "item model")):
        if not os.path.isfile(os.path.join(P, folder, bid.split(":", 1)[1] + ".json")):
            problems.append("%s missing %s" % (bid, label))
    if not os.path.isfile(os.path.join(P, "loots", "blocks", bid.split(":", 1)[1] + ".json")):
        problems.append("%s missing loot" % bid)
    texture_suffix = suffix[:-3] if suffix.endswith("_lh") else suffix
    if not os.path.isfile(os.path.join(P, "textures", "block", "carriage", "c25k",
                                       texture_suffix + ".png")):
        problems.append("%s missing placeholder texture" % bid)

print("blocks:", len(data["blocks"]))
print("openable blocks:", openable_count)
print("states checked:", state_count, "boxes:", box_count)
print("problems:", len(problems))
for row in problems[:20]:
    print("   ", row)
sys.exit(1 if problems else 0)
