# Directional runtime models with `proxy_model`

Use `proxy_model` for vehicle heads, cowcatchers, and runtime meshes that genuinely need entity rendering. It renders through KasugaLibNext's proxy system and supports the normal data-driven `state_properties` declaration.

A `.bbmodel` source does not automatically require `proxy_model`. When an established factory provides required gameplay semantics, preserve that factory:

- directional carports commonly use `slab_top`;
- directional floors commonly use `slab`;
- narrow walls, windows, and doors use their matching panel family.

For example, the JY290 carport uses the lower-half `slab_top` collision and the floor uses the upper-half `slab` collision. Replacing either with generic `proxy_model` would lose those family semantics. If a carport must load a native BBModel, use a dedicated `TrainPanelProxyBlock` host and preserve the half-block shape and wrench behavior in Java instead of exporting the model to OBJ or switching to generic `proxy_model`.

## Non-negotiable rules

- Pair the block with the block entity factory that matches its host family.
- A KuaYue part that must retain `TrainPanelBlock` behavior extends
  `TrainPanelProxyBlock` and uses `train_panel_proxy_be`; the general
  `proxy_model` type continues to use `proxy_model_be`.
- A non-TrainPanel custom host can extend KasugaLibNext's `ProxyModelBlock` and
  still use `proxy_model_be`. A custom block factory is not the stateful
  `proxy_model` factory, so register vanilla states such as
  `HORIZONTAL_FACING` explicitly in Java and reuse `ProxyModelBlock` placement,
  rotation, mirroring, entity creation, and ticking.
- Give heads, cowcatchers, and every exterior block with a front/back or left/right direction a real `facing` state. Do not omit it when the model appears symmetric.
- Declare `facing` as the vanilla `HORIZONTAL_FACING` property. Placement, rotation, mirroring, and proxy model yaw use that state.
- Do not use `fsm_block`, `fsm_be`, or a top-level `state_machine` to render a vehicle model. If state-machine behavior is genuinely required, stop and discuss it rather than using FSM as a rendering shortcut.
- When the artist supplies `.bbmodel`, ship the complete file unchanged. Do not export it to OBJ just to satisfy the skill.
- Confirm that the intended outliner groups and child elements are both visible and exported. A `bound ... to mc_backend` log line does not prove that the model has visible geometry.
- Do not put right-click toggles or other content-specific operations into `proxy_model` params. Implement interactions in the consuming mod's block class and factory.

## API-first implementation

Choose the narrowest existing host before writing code:

1. Use `proxy_model` plus `proxy_model_be` for a normal runtime model with data-driven state properties.
2. Extend `TrainPanelProxyBlock` and use `train_panel_proxy_be` when the block must retain `TrainPanelBlock` behavior, including `multi_cell`.
3. Extend `ProxyModelBlock` and pair it with `proxy_model_be` for other custom hosts that need Java state, interaction, or shapes without `TrainPanelBlock` behavior.
4. Represent left/right or open/closed model variants with `model_groups`, not duplicate block IDs.
5. Compose rendering through `ProxyModelProxy.CompositionBuilder`; do not build a second model loader, session ticker, transform hierarchy, or playback clock.
6. Use `ProxyControllerPort`, `ScopedPlaybackCommandApplier`, and `ModelClipPoseEvaluator` for authored clips.
7. Reuse `TrainDoorBlock.doorUse` when the interaction semantics match an existing train door.

`multi_cell` belongs on the KuaYue TrainPanel family. Reuse `TrainPanelProxyBlock`; do not introduce a parallel cross-cell Proxy base or duplicate companion lifecycle.

For visual state variants, model group names are not proof of semantic direction. Validate the rendered
left/right result, then keep `model_by_state`, `cells_by_state`, and collision orientation aligned.

## Block definition

```json
{
  "id": "example:vehicle_head",
  "type": "proxy_model",
  "state_properties": {
    "facing": {
      "type": "vanilla",
      "property": "HORIZONTAL_FACING",
      "default": "north"
    }
  },
  "block_entity": {
    "type": "proxy_model_be",
    "params": {
      "model": "example:models/block/vehicle_head.bbmodel",
      "facing_yaw": {
        "north": 0,
        "east": 90,
        "south": 180,
        "west": 270
      }
    }
  }
}
```

`params.model` is an explicit namespaced resource location containing the model path under `assets/<namespace>/`. Include the file extension. `params.model_name` is optional and is only for loaders that select an entry inside an archive. `params.yaw_offset` is an optional model-local rotation in degrees added after `facing`.

Use `params.facing_yaw` when different facings need independent mapping. It takes precedence over `facing` plus `yaw_offset`. This is necessary when only east/west are reversed: an ordinary yaw rotation cannot swap one axis pair while preserving the other.

Use `model_groups` when one block mounts multiple simultaneous parts or has alternative left/right model variants. Each part has a stable `id`, a native model resource, and optional ordered `local` transforms. Use `model_by_state` to select a complete group by block-state value:

```json
"default_model": "left",
"model_groups": {
  "left": {
    "parts": [
      {
        "id": "body",
        "model": "example:models/block/body_left.bbmodel"
      },
      {
        "id": "wheel",
        "model": "example:models/block/wheel.bbmodel",
        "local": [
          { "translate": [0, 0.8, 1.2] }
        ]
      }
    ]
  },
  "right": {
    "parts": [
      {
        "id": "body",
        "model": "example:models/block/body_right.bbmodel"
      },
      {
        "id": "wheel",
        "model": "example:models/block/wheel.bbmodel",
        "local": [
          { "translate": [0, 0.8, 1.2] }
        ]
      }
    ]
  }
},
"model_by_state": {
  "hinge": {
    "left": "left",
    "right": "right"
  }
}
```

Every variant group must keep the same ordered set of part IDs so target addresses survive switching. `model_by_state` values are group IDs when that group exists; legacy values may remain direct single-model paths. `params.model` is the single-part shorthand. Declare a vanilla `hinge` property with `DOOR_HINGE`; `proxy_model` then chooses left or right from the clicked half at placement.

Keep playback policy in the consuming mod's Java code. Implement a dedicated block class, let it
observe its state through `ProxyControllerPort`, emit `PlaybackCommand`s through the shared
Proxy/Scope runtime, and sample the chosen `.bbmodel` clip with a target evaluator. Do not add a
generic `property`/`active_when`/`animation` mapping to `proxy_model_be`: different blocks may
interrupt, blend, chain or reverse clips in different ways. A KuaYue example is
`jy_maintenance_outrigger`.

Animation position, rotation, and scale channels are local to the animated group. Their axes inherit every static parent-group rotation in the `.bbmodel`; do not rotate or rewrite keyframe values to compensate for a rotated outer group or for the block's `facing`. If a north-facing model moves sideways instead of forward, treat it as a loader transform-hierarchy bug and verify it with a nested-group regression model.

When a clip has trailing hold time, use its last animated keyframe as the motion endpoint.
Using the Blockbench `length` as that endpoint can delay a reversal while it replays the
invisible hold segment. Preserve the full clip only for authored holds, not for boolean
open/close timing. See [collision-and-playback.md](collision-and-playback.md) for the
complete collision and reversal checklist.

When the artist corrects the `.bbmodel` so its front points north (`-Z`), omit `facing_yaw` and use the canonical north/east/south/west mapping of `0/90/180/270` degrees. Do not keep compensation values after the source orientation changes.

Those values follow Minecraft blockstate semantics and match baked models such as JY290. A custom proxy that calls Kasuga `Transform.rotate` directly must negate the Y angle: Minecraft's positive Y turns toward east, while `Transform`'s positive Y turns toward west. North and south are unchanged, so missing this conversion appears specifically as east/west reversal.

The model's bind pose is assumed to face north. The proxy maps east/south/west to 90/180/270 degrees. If a source model uses another bind direction, confirm that the factory or future transform parameters support it before changing the asset path.

## Asset layout

Keep the native model under the parcel or pack asset root:

```text
assets/<namespace>/models/block/vehicle_head.bbmodel
assets/<namespace>/models/model_proxy.json
assets/<namespace>/models/item/vehicle_head.json
assets/<namespace>/textures/...
assets/<namespace>/blockstates/vehicle_head.json
```

The `.bbmodel` scanned by KasugaLibNext must be included by `models/model_proxy.json`:

```json
{
  "example": [
    "models/block/**/*.bbmodel"
  ]
}
```

Keep the pattern as narrow as practical for the parcel. The runtime block geometry comes from the native model, not from an OBJ export or a copy of the geometry in blockstate JSON. The item still needs its own model and icon. If the BBModel embeds its texture and no sibling PNG exists, extract that embedded PNG for the item/placeholder/particle references while leaving the runtime model embedded and unchanged. Add language keys, loot, tags, and train membership according to [pack-layouts.md](pack-layouts.md).

When left/right source files are discovered to be misnamed, correct the source
filenames and internal model names, copy them under the corrected runtime names,
then update `model_by_state`, multi-cell state mapping, and both collision
profiles. Swapping only the JSON model references leaves the asset names and
future edits inconsistent.

## Verification

Run the normal pack validator. For a quick source review, search the block definition and confirm:

- its block `type` is `proxy_model` for the generic factory, or the dedicated
  custom type registered by the consuming mod;
- its embedded block entity is `proxy_model_be` for generic and custom
  `ProxyModelBlock` hosts, or
  `train_panel_proxy_be` for KuaYue TrainPanel-backed custom proxy blocks;
- a TrainPanel-backed custom proxy block extends `TrainPanelProxyBlock`, not a
  separate cross-cell Proxy block base;
- `facing` uses vanilla `HORIZONTAL_FACING`, whether declared in
  `state_properties` or registered by the custom Java block class;
- `params.model` points to the shipped native model;
- every `model_by_state` value points to a shipped native model;
- state-driven clips and their control rules are implemented in Java, not in generic proxy params;
- the BBModel has at least one visible exported element and valid mesh/cube faces;
- no `fsm_block`, `fsm_be`, or `state_machine` appears in the implementation.

Static checks cannot prove the in-world origin, scale, or visible orientation. Inspect those in the client test before calling the block complete.

For missing geometry, render-layer, log, pivot, animation-axis, and source-persistence failures, use [runtime-troubleshooting.md](runtime-troubleshooting.md). Change KasugaLibNext only when evidence shows a generic loader or proxy defect rather than a pack/source defect.
