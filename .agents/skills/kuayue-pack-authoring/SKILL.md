---
name: kuayue-pack-authoring
description: Guide a KuaYue contributor on Windows, macOS, or Linux from supplied model files through block-list confirmation, pack/parcel selection, resource and limited vehicle-integration code, client testing, and a beginner-safe Git commit and pull request. Use when creating or modifying a KuaYue train preset pack, adding a vehicle parcel, or turning artist-provided models into working pack content.
---

# KuaYue pack authoring

Help a contributor finish one reviewable train-pack change without relying on Git knowledge. Work from supplied assets and the repository's current conventions; never invent a vehicle, block list, destination, branch, or commit scope.

Read [references/workflow.md](references/workflow.md) first. Then load only the reference for the active phase:

- Shell, Python, Gradle, temp paths, and install commands: [references/platform-commands.md](references/platform-commands.md)
- Pack or parcel layout: [references/pack-layouts.md](references/pack-layouts.md)
- Model interpretation: [references/model-analysis.md](references/model-analysis.md)
- Directional runtime models and `proxy_model`: [references/proxy-model.md](references/proxy-model.md)
- Code-defined collision and reversible state animation: [references/collision-and-playback.md](references/collision-and-playback.md)
- Missing, transparent, displaced, or wrongly oriented runtime models: [references/runtime-troubleshooting.md](references/runtime-troubleshooting.md)
- Vehicle integration code versus new gameplay: [references/implementation-scope.md](references/implementation-scope.md)
- Git, commit, and PR: [references/git-pr.md](references/git-pr.md)

## Model-rendering hard rule

Vehicle heads, cowcatchers, and runtime meshes that need entity rendering use the KasugaLibNext proxy path. Use the generic `proxy_model` / `proxy_model_be` pair when no block-specific host is needed; a custom host still renders through `proxy_model_be` or the matching TrainPanel proxy BE. Declare the real horizontal direction with a `facing` property. Do not omit `facing` because the model looks symmetric.

When one proxy block mounts several models, use `model_groups` and keep every selectable group's ordered stable part IDs identical. `model_by_state` selects a group; it does not select or convert an individual model file.

When a proxy-rendered block needs a content-specific Java interaction, register a dedicated consuming-mod block type. KuaYue parts that retain `TrainPanelBlock` behavior extend `TrainPanelProxyBlock` and pair with `train_panel_proxy_be`. Other custom hosts extend KasugaLibNext's `ProxyModelBlock` and pair with `proxy_model_be`. The custom type replaces only the block-level behavior; rendering still uses the proxy block entity.

Before adding a proxy host, model loader, session ticker, transform stack, or animation player, inspect the existing `proxy_model` / `proxy_model_be`, `TrainPanelProxyBlock` / `train_panel_proxy_be`, `ProxyModelProxy.CompositionBuilder`, `ProxyControllerPort`, `ScopedPlaybackCommandApplier`, and `ModelClipPoseEvaluator`. Use those seams first; add code only for the missing content-specific behavior.

For `multi_cell`, placement must preflight the anchor and every companion cell selected by the candidate state before writing the anchor. If any target is occupied by another component or cannot be replaced, reject the whole placement; do not rely on post-placement cleanup to repair a partial structure.

The block selection outline uses `getShape`; movement and placement use `getCollisionShape`. When a request says both should match, route both through one canonical helper.

Never use `fsm_block`, `fsm_be`, or a top-level `state_machine` as a shortcut for rendering a vehicle model. If a confirmed block genuinely requires state-machine behavior, stop and explain the conflict instead of silently selecting FSM.

Read [references/proxy-model.md](references/proxy-model.md) before writing any head, cowcatcher, or other proxy-rendered model block.
Read [references/collision-and-playback.md](references/collision-and-playback.md) before deriving custom AABBs or making a state toggle reverse an authored clip.

## Business-behavior hard rule

Use generic data-driven fields for stable presentation decisions such as which model a state selects or how the root is rotated. Keep animation state input, clip selection, interruption, composition and reversal in the consuming mod's Java Proxy controller/playback code; do not add a generic `property`/`active_when` animation mapping. Do not add generic params that execute content-specific gameplay: right-click toggles, opening procedures, deployment rules, resource checks, permissions, sounds with business meaning, or other state transitions.

When a block needs those behaviors, implement a dedicated block class and factory in the consuming mod. Its Proxy composition observes the state and controls playback in Java. Keep `multi_cell` on KuaYue's `TrainPanelBlock`; do not create a second cross-cell Proxy block base or put the footprint in Proxy-model data.

If a proposed JSON field would make a library type understand one vehicle's workflow, stop and move that behavior into Java instead.

## Implementation scope

The skill includes the limited Java wiring needed to make confirmed vehicle content work. The contributor does not need to write that code; the agent owns it when it is required by an in-scope behavior.

In scope:

- loading and rendering supplied models with correct facing, pivots, grouping, textures, and blockstates;
- dedicated consuming-mod block classes and factories for vehicle behavior not exposed by an existing factory;
- KuaYue multi-cell anchors, invisible companion cells, and delegated selection/interaction for models extending beyond their anchor voxel;
- collision shapes, door/key state changes, hinge/orientation behavior, and state-driven model selection that reproduce established train-block behavior;
- simple animation control for artist-authored clips, including forward/reverse playback, interruption, and playback-time continuity;
- minimal general fixes in KasugaLibNext or KuaYue when a rendering or animation capability is broken for multiple vehicle parts and cannot be fixed correctly in pack content.

Out of scope:

- new train operation, physics, occupancy, signalling, mission, economy, progression, or automation systems;
- broad gameplay workflows, persistent cross-block simulation, new networking/save formats, permissions, or player progression;
- an arbitrary state-machine/scripting layer, or generic JSON params that encode one vehicle's business workflow;
- speculative API redesigns or refactors that are unrelated to making the confirmed vehicle content function.

Read [references/implementation-scope.md](references/implementation-scope.md) when a requested behavior crosses from model/state adaptation into gameplay.

## Required confirmations

Do not cross these gates without an explicit user confirmation:

1. **Block list**: analyze supplied models, show every proposed block with evidence and confidence, and ask the user to confirm or edit the list before creating pack content.
2. **Change target**: show detected packs and parcels, then ask the user to choose exactly one mode: new pack, add parcel to an existing pack, or modify an existing parcel. For a new pack, also confirm whether it is an internal preset under `modules/train-parts/resources/packs/` or an external standalone pack. Runtime copies under `run/` are temporary previews; identify the durable source separately.
3. **Client test**: after implementation and static checks, ask whether the user wants to launch the game. Launch `:modules:train-parts:runClient` only after they say yes.
4. **Commit**: show the exact changed-file list, proposed commit message, tests already run, and unrelated dirty files. Ask before staging or committing.
5. **Remote write**: before pushing or opening a PR, show source branch, fork/remote, target repository `KuaYueTeam/KuaYue`, target branch `dev/new_train`, PR title, and PR body summary. Ask before any push or `gh pr create`.

If the user gives all required choices in the initial request, summarize them once and treat that summary as confirmation for those choices. Do not ask the same question again.

## Workflow

1. Locate the repository and inspect the current packs, parcels, branch, remotes, Git state, and GitHub CLI:

   ```powershell
   py -3 .agents/skills/kuayue-pack-authoring/scripts/inspect_workspace.py
   ```

   On macOS/Linux use `python3` instead of `py -3`. Use the exact commands reported by the script.

2. Analyze every supplied model path:

   ```powershell
   py -3 .agents/skills/kuayue-pack-authoring/scripts/analyze_models.py MODEL_PATH --format markdown
   ```

   Store durable process output under an ignored path such as the literal `doc/local/[local]model-intake/`; the `[local]` prefix is intentional and the whole directory must not be committed. You may rename the suffix after the target pack is confirmed. Treat parser output as a proposal, not truth. Present the candidate table and stop at the block-list gate.

   Before locking IDs, confirm the Chinese role and English identifier with the user when more than one reasonable translation exists. IDs and source filenames should be renamed before implementation starts.

3. After the user confirms the block list, present the three change modes and the detected pack/parcel inventory. Stop at the target gate.

   If the user explicitly wants a temporary runtime pack for visual iteration, mark that copy as disposable and record the future durable source path. Never present the runtime copy as the final deliverable.

4. Before editing for a likely PR, verify the worktree can safely move to a feature branch based on `upstream/dev/new_train`. Preserve unrelated changes; if branch setup would risk them, stop and offer isolated worktree guidance.

5. Implement from source files and current conventions:

   - Read `doc/packs/v1/AUTHORING.md`, `doc/packs/v1/SCHEMAS.md`, and `doc/schemas/data-driven/v1/README.md` before editing.
   - Edit internal preset sources under `modules/train-parts/resources/packs/`, never their generated output.
   - Use the layout rules in [references/pack-layouts.md](references/pack-layouts.md); do not mix internal legacy layout with external standard layout.
   - Match factories, blockstates, item models, languages, loot tables, tags, recipes, dependencies, train membership, and creative metadata using nearby working examples.
   - For heads, cowcatchers, and runtime meshes that need entity rendering, follow [references/proxy-model.md](references/proxy-model.md). Reject an implementation that renders these through `fsm_block` / `fsm_be`.
   - Keep content-specific interaction and state transitions in a dedicated Java block class/factory. Do not use rendering params as a substitute for business logic.
   - Do not choose the generic `proxy_model` type merely because the source is `.bbmodel`. Structural parts that need half-block collision or wrench behavior must preserve their family behavior. JY290-style carports use `slab_top`, while a carport explicitly required to load a native BBModel uses a dedicated `TrainPanelProxyBlock` host with the same family semantics.
   - For code-defined collision, separate static and moving geometry, preserve actual openings, derive north-facing AABBs before applying `facing`, and state whether collision follows the final state or the animation angle. For reversible clips with trailing holds, derive playable motion time from the last keyframe rather than the declared clip length.
   - Normalize newer Blockbench model JSON with `scripts/normalize_model_json.py` when it uses future rotation shapes, unnamespaced textures, or 90/270-degree element rotations.
   - When another vehicle already owns a reusable block or bogey, reference that existing ID and add a direct dependency on its pack. Do not copy its definitions or assets into the new parcel.
   - Keep one vehicle per parcel. Put a vehicle-specific bogey in that parcel; use `shared` or a truly generic `bogey-*` parcel only for shared ownership.
   - If a confirmed block needs a dedicated Java block class, factory, collision shape, Proxy controller, or small library compatibility fix to reproduce established vehicle behavior, implement it as part of the in-scope integration work. Read [references/implementation-scope.md](references/implementation-scope.md) before crossing into gameplay logic. Read [references/runtime-troubleshooting.md](references/runtime-troubleshooting.md) before attributing a visual failure to the loader or proxy.
   - Do not touch `doc/local/`, `manual_test/`, build outputs, logs, screenshots, or unrelated dirty files.

6. Validate before offering the game test:

   ```powershell
   py -3 .agents/skills/kuayue-pack-authoring/scripts/validate_pack.py PACK_ROOT --require-schema
   py -3 .agents/skills/kuayue-pack-authoring/scripts/validate_pack.py PACK_ROOT --require-schema --require-durable-source
   .\gradlew.bat :modules:train-parts:test --console=plain
   ```

   On macOS/Linux replace `py -3` with `python3` and `.\gradlew.bat` with `./gradlew`. Use the second validator command before a commit; it must fail while the pack remains under `run/`, generated build output, or compiled output. If the schema packages are absent, use the temporary virtual-environment instructions in [references/platform-commands.md](references/platform-commands.md). Do not add a project dependency or environment directory for this check.

   For an internal preset, also run:

   ```powershell
   .\gradlew.bat :modules:train-parts:generateKuaYuePresetPacks --console=plain
   ```

   Fix failures attributable to the change. Report unrelated pre-existing failures separately.

7. Ask whether to enter the game. If yes, run:

   ```powershell
   .\gradlew.bat :modules:train-parts:runClient --console=plain
   ```

   Use a persistent PTY session, report when the client is ready or the build fails, and let the user inspect visuals. Do not call a visual change validated from server-side tests alone.

   When a visual failure occurs, read [references/runtime-troubleshooting.md](references/runtime-troubleshooting.md), filter logs by the exact block/model ID, and distinguish successful binding from visible geometry.

8. Handle Git and PR work in plain language using [references/git-pr.md](references/git-pr.md). Check `gh`, preserve unrelated changes, stage only reviewed files, commit, push to the contributor's fork, and open a PR against `KuaYueTeam/KuaYue:dev/new_train`.

If `gh` is absent, stop before remote work and give the user the platform-appropriate installation command plus `gh auth login`; continue all local work first. If `gh` exists but is not authenticated, offer `gh auth login`.

## Reporting

At each gate, state the concrete result in plain language. At completion, report:

- chosen pack/parcel and confirmed blocks;
- files created or changed;
- validation and client-test results;
- branch, commit, push remote, PR URL, or the exact unresolved blocker.
