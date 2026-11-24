# Model-to-block analysis

Use `scripts/analyze_models.py` as a discovery aid. A filename or internal model group is never enough to register a block without user confirmation.

## Supported evidence

The analyzer understands:

- `.obj`: filename, object `o` names, group `g` names;
- `.bbmodel`: project name and outliner group names;
- `.gltf` and `.glb`: scene node names;
- Minecraft-style blockstate `variants` and `multipart` model references;
- Minecraft model JSON files containing `elements`, `textures`, or `parent`.

Other file types are listed as ignored evidence. If the artist supplied a format with no parser support, ask for exported blockstate/model files or inspect the file manually before proposing blocks.

The analyzer also performs source-level rendering checks:

- a Blockbench outliner hierarchy that leaves no visible/exported geometry is an issue;
- future `{x,y,z,origin}` element rotations must be normalized;
- Minecraft element angles outside 0, ±22.5, and ±45 degrees must be baked.

Treat these as issues to resolve, not automatic reasons to change the artist's design. Use `normalize_model_json.py` for supported Y rotations and manual review for multi-axis transforms.

## Candidate quality

- `high`: a blockstate file or one OBJ/model file that clearly represents one block.
- `medium`: a named OBJ object/group, BBModel outliner group, or glTF node that is likely a separate block.
- `low`: a generic group such as `Cube`, `Object`, `Root`, or a project-level group that may contain the whole asset.

Merge duplicate evidence from wrapper JSON and OBJ files. Preserve source paths in the final confirmation table.

## Factory hints

These are search hints, not automatic decisions:

| Name evidence | Inspect first |
|---|---|
| floor, slab | `slab` |
| roof, carport, top cap | `slab_top`; a native-BBModel carport with family collision/wrench semantics needs a dedicated TrainPanel proxy host |
| panel, side, wall | `train_panel` |
| hinge panel | `train_hinge_panel` |
| ladder | `ladder` |
| handrail, railing | `train_panel` |
| open window | `train_openable_window` |
| fixed/sealed window | `train_unopenable_window` |
| small window | `train_small_window` |
| door | `train_door`, `custom_rendered_door`, or a dedicated Proxy block when geometry, AABBs, or playback require Java integration |
| end face | `custom_rendered_end_face` or related end-face factories |
| head, cowcatcher | `proxy_model` + `proxy_model_be`; see [proxy-model.md](proxy-model.md) |
| outrigger, deployable equipment | dedicated TrainPanelProxy block type paired with `train_panel_proxy_be`; do not add a generic toggle param |
| console, control desk | dedicated `ProxyModelBlock` type with `proxy_model_be` when the model needs Java-defined collision or fixed AABBs |
| air vent | `air_vent` |
| skirt | `skirt` |
| table | `full_shape_directional` or a dedicated integration factory if thin collision is required |

Verify the selected factory against `doc/schemas/data-driven/v1/README.md`, Java factory code, and a working example from the same family.

Heads and cowcatchers always use `proxy_model` and always declare a real `facing` state. Do not propose `fsm_block` or `fsm_be` as a rendering choice. Apply the same `facing` requirement to every other exterior model that has a front/back or left/right orientation.

Before locking an ID, check that its English term describes the actual role. Compare alternatives with the user when a literal translation may be less clear than an operational term.

## Confirmation format

Before implementation, present:

| Proposed block ID | Evidence | Candidate factory (verify later) | Confidence | Ambiguity/action |
|---|---|---|---|---|

Then ask for one of:

- confirm all candidates;
- remove named candidates;
- rename candidates;
- merge multiple model groups into one block;
- split a candidate into additional blocks.

Only after that answer should the agent create block definitions or resource files.

## Stop conditions

Ask rather than guess when:

- the model uses generic group names;
- one file contains several spatially independent objects;
- the same visual part could reasonably be a panel, slab, door, or decorative block;
- the intended collision semantics are ambiguous and cannot be inferred from
  the model or a working vehicle precedent; a missing factory alone is an
  integration task, not a reason to stop;
- the requested behavior includes a content-specific interaction or state transition that would require a new generic JSON parameter;
- a referenced texture is missing or unreadable; embedded BBModel textures can
  render natively and can be extracted for item/particle references when needed;
- Blockbench `visibility`/`export` flags leave the intended geometry hidden;
- the model depends on rotations, animations, or block entities not supported by the chosen factory;
- a proposed block ID collides with an existing registry claim.
