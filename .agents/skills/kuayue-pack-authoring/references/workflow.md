# Contributor workflow

Use this sequence for one pack task. Keep one active target and one reviewable output.

## 1. Intake

Collect only missing information that changes the result:

- paths to supplied models, textures, icons, and design notes;
- target pack and parcel, if already known;
- display names in Chinese and English;
- preferred English terminology when the Chinese role has several valid translations;
- whether the content is a carriage, locomotive, or shared runtime asset;
- intended factory or behavior when the model name alone is ambiguous;
- whether the user wants a local commit and/or a PR.

If the user has not named a target, inspect the repository first and offer the three modes with actual pack/parcel choices.

Create an ignored working directory:

```text
doc/local/[local]model-intake/
├── intake.md
├── model-candidates.md
└── test-notes.md
```

Do not commit files from `doc/local/`.

## 2. Block-list gate

Run `analyze_models.py` and review its output before showing it. Remove obvious non-block assets such as GUI images or unrelated metadata, but retain low-confidence candidates with a warning instead of silently dropping them.

Present a compact table:

| Proposed block | Source/evidence | Candidate factory (verify after block confirmation) | Confidence | Notes |
|---|---|---|---|---|

Explain that filenames, OBJ groups, BBModel groups, and glTF nodes are suggestions. Ask the user to confirm, rename, merge, split, or drop candidates. Do not create pack files until the user answers.

Review analyzer warnings before presenting candidates. A BBModel with no visible exported geometry, a future multi-axis rotation, or an unsupported 90/270-degree element rotation is a source problem to resolve before block-list confirmation.

When terminology is ambiguous, propose the actual alternatives and lock the chosen namespace/ID before creating files. A later rename can affect parcel data, blockstates, models, textures, languages, loot, recipes, tags, train membership, and source filenames.

If a candidate needs behavior unsupported by the current data-driven factories,
say whether it needs limited integration code or a new gameplay feature. A
dedicated block class/factory or collision profile is still pack integration;
do not reject the candidate merely because Java is required.

## 3. Target gate

Use `inspect_workspace.py` to list real packs and parcels. Offer:

1. Create a new internal preset pack.
2. Create a new external standalone pack.
3. Add a parcel to an existing pack.
4. Modify an existing parcel.

For internal presets, the expected source is:

```text
modules/train-parts/resources/packs/<pack-id>/
```

For a new internal pack, confirm the pack ID, display namespace, expected vehicles, dependency on `kuayue-preset-running-gear`, icon, and creative tab.

For an external pack, confirm its destination outside generated/build directories and use the standard `assets/<namespace>/` and `data/<namespace>/` layout.

After confirmation, write a scope statement listing the exact pack and parcel paths plus the confirmed block IDs. Do not expand it silently.

If the user asks to test through a pack under `run/`, treat it as a disposable client-preview copy. Record the intended durable source before implementation and copy accepted changes back before the commit gate. The validator warns for runtime/generated paths; `--require-durable-source` turns that warning into an error.

If the user expects a PR and the worktree is safe, create the feature branch from `upstream/dev/new_train` before editing. If unrelated changes could conflict, stop before branch switching and use an isolated worktree or ask the user how to preserve their work.

## 4. Implementation

Read the current authoring and schema documents, then inspect the closest working parcel before writing:

- `doc/packs/v1/AUTHORING.md`
- `doc/packs/v1/SCHEMAS.md`
- `doc/schemas/data-driven/v1/README.md`
- at least one manifest, one index, and one resource of the same factory family

Normalize Blockbench Java model JSON when necessary:

```powershell
py -3 .agents/skills/kuayue-pack-authoring/scripts/normalize_model_json.py SOURCE.json OUTPUT.json --namespace NAMESPACE
```

The helper displays warnings when it cannot determine item transforms. It does not replace visual inspection.

Implement all required layers together:

- pack and parcel metadata;
- data-driven index and registration content;
- blockstate and model resources;
- item models and item definitions/tags when required;
- block/item language keys;
- loot tables, recipes, mining tags, train membership, creative metadata, and icons when the existing convention requires them;
- vehicle-specific bogey definitions in the same vehicle parcel;
- dependencies only when a referenced definition is owned by another pack.

For heads, cowcatchers, and other blocks whose geometry comes from a runtime model:

- read [proxy-model.md](proxy-model.md) before choosing a factory;
- use generic `proxy_model` plus `proxy_model_be`, or a dedicated custom host
  paired with the matching proxy block entity when Java state or collision is required;
- give each directional exterior block a real `facing` state;
- keep `.bbmodel` assets intact and point `params.model` at them;
- do not use `fsm_block`, `fsm_be`, or a top-level `state_machine` as a rendering shortcut.

Before accepting a BBModel, confirm that at least one intended element survives the `visibility`/`export` flags from its complete outliner hierarchy. Binding or loading the file does not prove that it contains visible geometry.

Keep rendering configuration and business behavior separate. `proxy_model` and
`proxy_model_be` may select a model or transform the root, but animation state
input, clip selection and playback composition belong to a dedicated Java Proxy
controller. When a block needs a custom operation, implement a dedicated Java
block class and register a dedicated factory type in the consuming mod. A
vehicle-specific collision shape or door behavior belongs in that same
integration layer.

Read [implementation-scope.md](implementation-scope.md) before writing code.
Models, facing, collision, block-local door behavior, and simple artist-authored
animation controls are in scope. New train-operation rules, broader simulation,
new persistent systems, or generic scripting are not.

For a code-defined collision profile or reversible state animation, read
[collision-and-playback.md](collision-and-playback.md). Derive static and moving
AABBs separately, preserve intentional openings, and distinguish clip length
from the last animated keyframe before changing playback timing.

When model geometry spans neighboring cells, declare the north-facing cell union
through the KuaYue `params.multi_cell.cells` entry. Do not put this footprint into
KasugaLibNext's Proxy data; companion ownership is KuaYue multi-block structure
behavior. The option belongs to `TrainPanelBlock`, so every TrainPanel family
factory may use it. A TrainPanel part that also needs custom Proxy rendering
extends `TrainPanelProxyBlock`; do not introduce a separate cross-cell Proxy
base. When left/right or open/closed states occupy different cells, use
`cells_by_state` rather than the union of all states.

Before placing a multi-cell anchor, preflight every selected companion cell from
the candidate state. Reject the whole placement when any target overlaps another
component or cannot be replaced. Never place the anchor first and rely on cleanup
to repair a partial structure.

For a proxy block that mounts several models together, use `model_groups`; keep every selectable group's stable ordered part IDs identical so the same animation targets and instance identities survive a group switch.

For directional carports and floors, inspect the closest vehicle before choosing the factory. An asset being `.bbmodel` does not imply generic `proxy_model`. JY290 uses `slab_top` for carports and `slab` for floors. A native-BBModel carport that must preserve the same half-block and wrench semantics uses a dedicated `TrainPanelProxyBlock` host instead.

For an internal preset, edit the source layout and let `generateKuaYuePresetPacks` produce the standard assets. Never hand-edit `build/generated/kuayue-preset-packs/`.

Follow the existing namespace and ownership rules. Avoid duplicate block/item claims, implicit vanilla namespace IDs, and duplicate definitions.

When reusing a block or bogey from another vehicle, keep the original explicit registry IDs and add a direct dependency on the owning pack. Copying the asset tree or re-declaring the same logical object under the new vehicle namespace creates two sources of truth and is not a shared-resource implementation.

## 5. Static verification

Use the Python and Gradle commands detected by `inspect_workspace.py`. Windows PowerShell examples:

```powershell
py -3 .agents/skills/kuayue-pack-authoring/scripts/validate_pack.py PACK_ROOT --require-schema
.\gradlew.bat :modules:train-parts:test --console=plain
```

macOS/Linux use `python3` and `./gradlew`. If `jsonschema` or `referencing` is unavailable, follow the virtual-environment commands in [platform-commands.md](platform-commands.md).

For an internal preset also run:

```powershell
.\gradlew.bat :modules:train-parts:generateKuaYuePresetPacks --console=plain
```

Use `git diff --check`. Inspect the generated pack when relevant. The pack validator resolves available workspace dependencies and checks the merged train definition, but reports incomplete dependency verification as a warning when a dependency pack is unavailable; never present that warning as a complete train pass. If JSON Schema validation is unavailable, the validator reports that explicitly; it is not a pass.

The validator also checks:

- supported Minecraft element rotations;
- transparent PNGs against `render_type`;
- visible exported geometry in local `proxy_model` BBModels;
- complete four-direction blockstates for known directional factories;
- runtime/generated pack locations.

Before a commit, rerun with `--require-durable-source`. A runtime-preview pack is not a complete result.

Fix only failures attributable to this change. Distinguish pre-existing or unrelated failures.

## 6. Client-test gate

After static checks pass, summarize:

- registered block/item IDs;
- expected location in the creative tab;
- what visual states the user should inspect;
- test commands already completed.

Ask one direct question: whether to start `:modules:train-parts:runClient`.

If the answer is yes:

```powershell
.\gradlew.bat :modules:train-parts:runClient --console=plain
```

Run it in a persistent PTY. Watch for model, texture, language, and registration errors. Keep the process available while the user tests. Do not claim visual success without the user's observation.

For a visual failure, use [runtime-troubleshooting.md](runtime-troubleshooting.md). Search the exact block ID and model path in `latest.log` and `debug.log`; distinguish registration, backend binding, model parsing, resource lookup, and actual geometry visibility. Do not let unrelated recipe or optional-content errors drive a model diagnosis.

## 7. Commit and PR gate

Show:

- exact files to stage;
- unrelated dirty files that will remain untouched;
- proposed commit message;
- verification results.

Ask for commit confirmation. After committing, show the commit hash and ask separately before pushing and opening the PR. The push/PR confirmation must include the exact fork remote and target branch.

Use `references/git-pr.md` for the beginner-safe Git sequence.

## Completion report

Report the confirmed block list, pack/parcel, resource layers, validation evidence, client-test status, commit, and PR URL. State any unverified visual or platform behavior explicitly.
