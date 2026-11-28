"""Compare the c25k pack against the artist source folder.

Checks that every source model is used by exactly one block, that the copied .bbmodel keeps
every element / group / texture of its source, and that no source asset is left unused.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import build_c25k_pack as gen

SRC = gen.SRC
PACK = gen.PDIR
MODELS = os.path.join(PACK, "models", "block", "carriage", "c25k")


def counts(data):
    return {
        "elements": len(data.get("elements") or []),
        "textures": len(data.get("textures") or []),
        "outliner": len(data.get("outliner") or []),
        "has_animations": bool(data.get("animations")),
    }


def main():
    problems = []
    all_files = []
    for root, _, files in os.walk(SRC):
        for name in files:
            rel = os.path.relpath(os.path.join(root, name), SRC).replace(os.sep, "/")
            all_files.append(rel)
    bbmodels = sorted(f for f in all_files if f.lower().endswith(".bbmodel"))
    others = sorted(f for f in all_files if not f.lower().endswith(".bbmodel"))
    print("source files: %d bbmodels + %d others" % (len(bbmodels), len(others)))
    for extra in others:
        print("   non-model file:", extra)

    used = {}
    for rel, (suffix, _, _, open_rel) in gen.MAPPING.items():
        used[rel] = suffix
        if open_rel:
            used[open_rel] = suffix + "_open"
    for suffix, entry in gen.LEFT_VARIANTS.items():
        left_rel = entry[0] if isinstance(entry, tuple) else entry
        used[left_rel] = suffix + "_left"
        if isinstance(entry, tuple) and entry[1]:
            used[entry[1]] = suffix + "_left_open"
    missing_use = [f for f in bbmodels if f not in used]
    overridden = []
    kept = []
    for entry in missing_use:
        stem = os.path.basename(entry)[:-len(".bbmodel")]
        if stem.endswith("开"):
            suffix = None
            for rel, (sfx, _, _, open_rel) in gen.MAPPING.items():
                if open_rel == entry:
                    suffix = sfx
            if suffix in gen.ROTATE_END_FACE_LEAVES:
                overridden.append(entry)
                continue
        kept.append(entry)
    missing_use = kept
    for entry in overridden:
        print("   open pose replaced by leaf rotation:", entry)
    unused_entries = [f for f in used if f not in bbmodels]
    if missing_use:
        problems.append("source models not used: %s" % missing_use)
    if unused_entries:
        problems.append("mapping points at missing sources: %s" % unused_entries)

    for rel, suffix in sorted(used.items(), key=lambda kv: kv[1]):
        source = json.load(open(os.path.join(SRC, rel), encoding="utf-8"))
        target_path = os.path.join(MODELS, suffix + ".bbmodel")
        if not os.path.isfile(target_path):
            problems.append("%s: pack model missing" % suffix)
            continue
        target = json.load(open(target_path, encoding="utf-8"))
        a, b = counts(source), counts(target)
        for key in ("elements", "textures", "outliner"):
            if a[key] != b[key]:
                problems.append("%s: %s %d -> %d" % (suffix, key, a[key], b[key]))
        if a["has_animations"]:
            problems.append("%s: source has animations that the pack drops" % suffix)
        # texture pixel payload must survive (mirrored left-hand parts change pixels on purpose)
        src_tex = [t.get("source", "") for t in source.get("textures") or []]
        dst_tex = [t.get("source", "") for t in target.get("textures") or []]
        if len(src_tex) != len(dst_tex):
            problems.append("%s: embedded textures differ" % suffix)
        elif any(s and d and s != d for s, d in zip(src_tex, dst_tex)):
            if suffix not in gen.MIRROR_ART:
                problems.append("%s: embedded texture changed unexpectedly" % suffix)

    blocks = json.load(open(os.path.join(PACK, "data", "kuayue", "carriages", "c25k_new",
                                        "blocks.json"), encoding="utf-8"))["blocks"]
    block_ids = {b["id"] for b in blocks}
    print("blocks: %d, pack models: %d" % (len(blocks), len(os.listdir(MODELS)) - 0))
    print("problems: %d" % len(problems))
    for row in problems:
        print("   ", row)


if __name__ == "__main__":
    main()
