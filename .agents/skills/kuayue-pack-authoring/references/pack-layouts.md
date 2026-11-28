# KuaYue pack layouts

Choose one layout before writing files. The two layouts are not interchangeable without the internal generator.

## Internal preset source

Built-in preset sources live at:

```text
modules/train-parts/resources/packs/<pack-id>/
├── pack.json
├── pack.mcmeta
├── icon.png
├── assets/kuayue/lang/{zh_cn,en_us}.json
└── parcels/<vehicle>/
    ├── parcel.json
    ├── blockstates/
    ├── models/
    ├── textures/
    ├── sounds/
    ├── recipes/
    ├── loots/
    ├── tags/
    └── data/<namespace>/
        ├── kasuga_lib/data_driven/<entry>.json
        ├── <kind>/<vehicle>/shapes.json
        └── trains/<vehicle>.json
```

`modules/train-parts/preset-packs.gradle` converts legacy `blockstates`, `models`, `sounds`, `textures`, `recipes`, and `loots` into standard Minecraft resource paths and merges language/tag files. These legacy source folders currently map to the `kuayue` namespace. For a different namespace, place a ready-to-project `assets/<namespace>/...` tree under the pack or parcel.

The generated output is:

```text
modules/train-parts/build/generated/kuayue-preset-packs/<pack-id>/
```

It is written into the JAR under `kuayue-packs/<pack-id>/`. Never edit generated output.

Internal requirements enforced by the generator:

- directory name equals `pack.json.id`;
- `pack.json`, `pack.mcmeta`, and `icon.png` exist;
- every declared parcel exists and every parcel directory is declared;
- parcel IDs are globally unique across the generated preset catalog;
- each declared data-driven index exists.

Every non-running-gear preset in the current repository declares:

```json
{
  "dependencies": {
    "mods": {
      "minecraft": ">=1.21.1",
      "neoforge": ">=21.1.0",
      "kuayue": ">=2.0.0",
      "kasuga_lib": ">=1.0.0",
      "create": ">=6.0.0"
    },
    "packs": {
      "kuayue-preset-running-gear": ">=2.0.0"
    }
  }
}
```

Keep that dependency when the preset references shared styles, bogeys, models, languages, or runtime data. `required: true` is reserved for `kuayue-preset-running-gear`.

## External standalone pack

External packs use standard Minecraft resource roots:

```text
<pack-id>/
├── pack.json
├── icon.png                       # optional
├── pack.mcmeta                    # optional
└── parcels/<vehicle>/
    ├── parcel.json
    ├── assets/<namespace>/
    │   ├── blockstates/
    │   ├── lang/
    │   ├── models/
    │   └── textures/
    └── data/<namespace>/
        ├── kasuga_lib/data_driven/<entry>.json
        ├── <kind>/<vehicle>/shapes.json
        ├── loot_table/
        ├── recipe/
        └── tags/
```

Resources may also live at the pack root under `assets/` and `data/`. Parcel roots rank above the pack root inside the same pack. Only `assets/**`, `data/**`, `pack.mcmeta`, and `pack.png` are projected as Minecraft resources.

Start from `examples/packs/sample-train` and add missing pieces from `examples/packs/integration-train`. Do not copy the old `examples/kuayue/config*.json` files because they still contain the removed `kuayue-core` ID.

## Parcel metadata

`parcel.json` must declare:

- a namespaced ID owned by the pack;
- `name_key`;
- `indexes` entries using paths relative to the pack root, such as `data/kuayue/kasuga_lib/data_driven/c22.json`.

`indexes` may be empty for a resource-only parcel. An index file is not a registration entry unless the parcel lists it.

When adding vehicle metadata:

- carriages use `kuayue:kuayue_carriage`;
- freight wagons use `kuayue:kuayue_freight`;
- locomotives use `kuayue:kuayue_loco`;
- add `trains/<vehicle>.json` to `on_register`;
- list the vehicle blocks, items, tags, and bogey definitions in its `members`;
- add translations for `name_key`, `description_key`, and the parcel name.

Do not add `creative_filter` to a new built-in parcel. It remains only as a fallback for old external packs.

## Registration content

An index contains paths relative to `data/<namespace>/`:

```json
{
  "on_register": [
    "carriages/my_train/shapes.json",
    "carriages/my_train/groups.json",
    "carriages/my_train/blocks.json",
    "trains/my_train.json"
  ],
  "on_reload": []
}
```

`shapes.json` is a registration resource, not an asset. It contains the
top-level `shapes` definition array and must be listed by the owning parcel
index before its `blocks.json`. Keep the path relative to `data/<namespace>/`
inside the index. If an existing shape ID is owned by another pack, reference
it and add a direct dependency instead of copying it into this parcel. See
[shapes.md](shapes.md) for the DSL, state bindings, and validation contract.

The train file describes one complete vehicle identity:

```json
{
  "trains": [{
    "id": "mytrain:c22",
    "category": "carriage",
    "name_key": "parcel.mytrain.c22.name",
    "description_key": "parcel.mytrain.c22.description",
    "members": {
      "blocks": ["mytrain:c22_panel"],
      "items": [],
      "item_tags": ["mytrain:c22_parts"],
      "bogey_sizes": ["mytrain:c22"],
      "bogey_motions": [],
      "bogey_models": ["mytrain:bogey/c22"],
      "bogey_blocks": ["mytrain:c22_bogey"],
      "bogey_styles": ["kuayue:loco"],
      "bogey_style_variants": ["mytrain:c22_loco"]
    },
    "creative": {
      "tab": "kuayue:kuayue_carriage",
      "icon_item": "mytrain:c22_panel"
    }
  }]
}
```

Same-ID trains merge from lower to higher pack priority. Objects merge recursively, scalars replace, and arrays replace as a whole (`[]` clears). The override pack must directly depend on the lower-priority contributor. The effective definition must provide all three text fields and all nine member arrays.

Registering a block does not create its blockstate, model, texture, item model, language entry, loot table, recipe, or tags automatically. Those resource layers still follow the factory and Minecraft conventions.

## Cross-vehicle shared resources

When a block or bogey is already valid for another vehicle, reuse its existing registry IDs instead of copying the source files or creating near-duplicate IDs. Add the owning pack to the consuming pack's `dependencies.packs`.

Use the category that matches the resource:

- cowcatchers, heads, panels, and ordinary placed blocks go in `members.blocks`;
- bogey sizes, motions, models, blocks, styles, and style variants go in their matching `bogey_*` arrays;
- do not duplicate a bogey block in `members.blocks` merely to make it part of the train; its `members.bogey_blocks` entry owns that relationship.

Before finishing, verify both pack dependency closure and train-member closure. A shared namespace that merely happens to exist in the development workspace is not enough for an installed pack.

Factory-specific resource requirements are not centralized completely. Inspect the corresponding Java factory and a working parcel before choosing state properties. In particular:

- do not assume a factory uses vanilla slab or waterlogged states;
- check whether the factory automatically registers a BlockItem;
- check the actual `FACING`, `OPEN`, hinge, type, and custom model-state properties;
- heads, cowcatchers, and runtime meshes that need entity rendering use `proxy_model`; see [proxy-model.md](proxy-model.md) for the required registration and asset layout.

## Directional carports and floors

For JY290-style structural halves, preserve the family semantics instead of treating every source model as a proxy entity:

- JY290-style carports use `slab_top` and the lower-half carport collision;
- floors use `slab` and the upper-half floor collision;
- floor-with-ladder variants also use `slab`, but their model may have a different bind direction; copy the working source blockstate rather than assuming the plain floor mapping;
- both register a real horizontal `facing` property through their panel-family factory;
- cover east, north, south, and west blockstate variants with the same y rotation convention as the source vehicle;
- provide block model, item model, texture or OBJ/MTL assets, loot, recipe, mining tag, panel tag, language keys, and train membership.

If the user explicitly requires a carport to load a native BBModel, use a dedicated
`TrainPanelProxyBlock`-backed factory that preserves the half-block shape and
wrench behavior. Do not export an OBJ merely to keep the old family type, and do
not switch to generic `proxy_model` with full-block collision.

For a confirmed climbable floor or structural leg, implement `isLadder` in the
dedicated block class and add the block IDs to `minecraft:climbable` for tag
consumers. These Java and block-tag changes require a client restart; a model or
resource reload is not enough to prove the behavior. Check climbing from the
intended approach side, especially when collision remains a full block.

JY290's floor-with-ladder uses a south-facing bind pose: south `0`, north `180`, east `270`, west `90`. Plain floor blocks in the same vehicle may use another mapping.

Newer Blockbench model JSON may store rotations as `{x,y,z,origin}` or use a future `format_version`. Before shipping it as a Minecraft block model, normalize to the repository convention: set the supported `format_version`, convert the nonzero axis to `{angle,axis,origin}`, and namespace every texture reference.

Use the bundled helper for supported Y-axis cases:

```powershell
py -3 .agents/skills/kuayue-pack-authoring/scripts/normalize_model_json.py SOURCE.json PACK/.../models/block/path/model.json --namespace NAMESPACE
```

The output path determines the default texture directory below `models/block/`. Review its warnings and confirm that it guessed the right texture base and item transform.

Minecraft 1.21.1 block elements accept only `0`, `±22.5`, and `±45` degree element rotations. A `±90` element rotation fails the entire shared block model with `Invalid rotation ...`; because the item model parents the same block model, both the placed block and item render purple/black. Bake 90-degree rotations into element coordinates and remap the affected cardinal faces instead of writing them as element rotation.

When baking a positive Y rotation, remap outward face normals as `north→west`, `west→south`, `south→east`, and `east→north`; use the reverse mapping for a negative Y rotation. Do not confuse this with the coordinate transform: rotating point `(x, z)` by `+90°` maps a source face's outward normal to the face named above. Rename the cardinal face objects accordingly. Side-face UV coordinates remain in their matching corners, while `up` and `down` UV corners rotate with the geometry, so rebuild those `uv` arrays by matching each destination corner to its rotated source corner. Renaming face keys without applying the vertex-based UV step can make the top or bottom of a ladder appear mirrored or rotated.

An RGBA PNG can still be fully opaque. Inspect actual pixels before declaring transparency:

- binary transparent alpha uses `cutout_mipped`;
- partially transparent alpha uses `translucent`;
- opaque alpha needs no special render type.

Without a compatible `render_type`, transparent pixels can render as solid color or disappear.

For NeoForge OBJ assets, every `map_Kd` value in the MTL must be a namespaced texture ID such as `example:block/vehicle/carport`. A bare filename resolves under `minecraft:` and produces a missing-texture model.

Create 6 sequenced-assembly recipes use `id` for processing outputs, `transitional_item` instead of `transitionalItem`, and `id` in the transitional item. Legacy `item`/`transitionalItem` output fields fail recipe deserialization.

When copying a working vehicle recipe, keep shared blueprint nodes and shared ingredients explicit. Do not invent a vehicle-specific blueprint unless the user asks for one.

## Bogeys

Keep a vehicle-specific bogey in that vehicle's parcel:

```text
parcels/<vehicle>/data/<namespace>/<vehicle>/bogeys/<vehicle>.json
parcels/<vehicle>/models/block/bogey/<vehicle>/
parcels/<vehicle>/textures/block/bogey/<vehicle>/
```

Use `bogey_style_variants` to attach variants to a style owned by `kuayue-preset-running-gear`. Keep only genuinely shared styles, motions, models, or bogeys in running-gear.

## Verification

From the repository root, use the platform-specific command from [platform-commands.md](platform-commands.md). Windows PowerShell:

```powershell
py -3 .agents/skills/kuayue-pack-authoring/scripts/validate_pack.py PACK_ROOT --require-schema
.\gradlew.bat :modules:train-parts:test --console=plain
```

For internal presets:

```powershell
.\gradlew.bat :modules:train-parts:generateKuaYuePresetPacks --console=plain
```

`validate_pack.py` checks each train patch and, when its dependency packs are present in the workspace, merges the visible patches before checking the complete train definition. It reports missing required text, the nine member arrays, `creative.tab`, unresolved owned IDs, duplicate membership entries, and overlays that lack a direct pack dependency. It also checks supported element rotations, transparent texture render types, exported BBModel geometry, four-direction blockstates, and temporary pack locations. A missing dependency pack downgrades completeness to a warning because the script cannot inspect it.

Packs under `run/`, generated build output, or compiled output are preview copies. Use `--require-durable-source` before a commit; it must fail until the reviewed change exists in the durable source chosen at the target gate.

Confirm the generated path, declaration closure, and absence of duplicate claims. Client model, texture, state-selection, and visual placement require `runClient`.
