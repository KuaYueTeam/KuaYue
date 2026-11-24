# Runtime model troubleshooting

Load this reference when static validation passes but a placed block or item is absent, transparent, purple/black, displaced, mirrored, or animated on the wrong axis.

Use `modules/train-parts/run/logs/latest.log` and `debug.log` as evidence, not as a second guessing loop. Filter by the exact block ID and exact model path before reading surrounding errors.

## Classify the failure

| Evidence | Meaning | Next check |
|---|---|---|
| registered block/item/block entity | Registration succeeded | Confirm the expected blockstate and resource file is in the active pack |
| `bound '<model>' ... to mc_backend` | The model resource reached a rendering backend | Inspect visible/exported geometry, render type, camera-relative origin, and scale |
| missing texture or model `FileNotFoundException` naming the model | Resource closure or path is wrong | Fix namespace, extension, texture alias, item parent, or `model_proxy.json` scan pattern |
| `Invalid rotation`, model parse exception, or malformed JSON | The whole shared model failed | Normalize element rotations and compare the placed model with its item parent |
| unrelated recipe/tag/item errors | Usually does not explain a missing model | Keep them out of the render diagnosis unless the same model path is named |

A binding line proves loading, not visibility. A model can bind successfully with zero exported elements or geometry outside the camera area.

## Minecraft model JSON

Run the supplied analyzer on the source JSON before copying it:

```powershell
py -3 .agents/skills/kuayue-pack-authoring/scripts/analyze_models.py MODEL.json --format markdown
```

For newer Blockbench output, normalize it into the target pack model path:

```powershell
py -3 .agents/skills/kuayue-pack-authoring/scripts/normalize_model_json.py SOURCE.json PACK/.../models/block/path/model.json --namespace NAMESPACE
```

The normalizer:

- sets the supported format version;
- namespaces texture references;
- converts zero-angle future rotations to the vanilla shape;
- bakes ±90/±270 Y rotations into coordinates;
- remaps cardinal face names and the `up`/`down` UV corners;
- detects sibling PNG alpha and selects `cutout_mipped` or `translucent` when it can do so safely;
- rejects multi-axis or axis combinations that need manual review.

Minecraft 1.21.1 rejects 90/180-degree element rotations. Bake them; do not expect the block and item to show separate errors, because both commonly share the same model.

## Texture alpha

Check the PNG's actual pixels, not only its RGBA color type. Opaque RGBA images must not gain a transparent render type merely because the file has an alpha channel.

- Binary transparent pixels: use `cutout_mipped`.
- Partially transparent pixels: use `translucent`.
- No transparent pixels: no transparency declaration is required.

The pack validator reports transparent textures whose model has no compatible `render_type`.

## Blockbench BBModel geometry

KasugaLibNext honors Blockbench `visibility` and `export` fields on both outliner groups and elements. A parent group with `export: false` hides its complete subtree.

Run:

```powershell
py -3 .agents/skills/kuayue-pack-authoring/scripts/analyze_models.py MODEL.bbmodel --format markdown
```

The analyzer reports a BBModel with no visible exported geometry. Before changing a model or loader, verify:

1. the intended top-level group is visible and exported;
2. the intended child elements are visible and exported;
3. exported mesh faces reference existing vertex and UV names;
4. transformed bounds are near the vehicle part, not several blocks away;
5. an item/placeholder model does not hide the native model failure.

Do not enable every hidden cube blindly. Hidden source cubes can sit beside generated `cube_selection` meshes and may duplicate the intended surface.

## Facing and origin

Blockstate rotation, proxy `facing`, and animation-local axes are separate layers:

- A directional factory such as `train_panel`, `slab`, or `slab_top` normally gets its orientation from blockstate variants.
- `proxy_model` uses the data-driven `facing` state and optional `facing_yaw` mapping.
- Animation channels remain local to their Blockbench group and inherit static parent rotations.

Do not call Kasuga `RotHelper` or rewrite animation keys to compensate for proxy-level facing. Verify the bind pose first, then compare the four blockstate directions with a known working vehicle.

## Displacement and pivots

If components spread across the world:

- compare the failing model with a minimal pivot/group fixture through the same loader;
- inspect nested group origins and rotations rather than moving every mesh;
- confirm animation hierarchy parsing did not flatten a transformed parent;
- test the same structure without animation to separate static transform from keyframe problems.

If the same model renders correctly through one path and incorrectly through another, inspect the loader or proxy implementation. Do not compensate by changing artist-supplied pivots unless the source is actually wrong.

## Collision and delayed reversal

If collision appears offset from the visible model, check proxy root centering,
named-group transforms, Blockbench Y orientation, and final `facing` rotation in
that order. If a doorway is blocked, separate static-body boxes from moving-part
boxes and confirm the opening is absent from the static shape.

If the collision feels correct but the yellow/black outline still looks like a
plate, inspect `getShape`: the selection outline does not derive from
`getCollisionShape`. When both are meant to match, make both call one canonical
shape helper rather than maintaining two box lists.

If reversing `open=true` to `open=false` appears to wait before moving, compare
the clip's declared length with its last animated keyframe. A trailing hold
replayed in reverse is not network latency. Use its last motion keyframe as the
toggle endpoint unless the hold is intentional. Read
[collision-and-playback.md](collision-and-playback.md) before changing the
controller.

If newly implemented ladder behavior does not work, first restart the client:
new block classes and `minecraft:climbable` tags are not installed by a model or
resource reload alone. Confirm the block ID is in the tag and that the player can
actually occupy or touch the climbable block. A true `isLadder` value does not
guarantee an approach path when a full collision box prevents the entity from
entering that block position; test the requested physical behavior in game rather
than treating the boolean return value as proof.

## Source persistence

Packs under `run/`, `build/`, or `src/compiled/` are runtime or generated copies. Use them for fast client inspection, then copy accepted changes to:

- environment-specific working data under ignored `doc/local/[local].../`; and
- the durable internal preset or external pack source chosen at the target gate.

Run the validator with `--require-durable-source` before a commit. It intentionally fails while the validated pack still lives in a runtime/generated directory.

## Escalation

When the evidence points to a generic loader, proxy factory, animation hierarchy,
or model transform bug, first reproduce it through more than one vehicle block
or a minimal fixture. A minimal general fix belongs to vehicle integration when
the defect prevents normal model loading or simple animation. Do not hide a
generic defect with a content-specific workaround. Broaden the task into an API
redesign or new gameplay system only after that boundary is established.
