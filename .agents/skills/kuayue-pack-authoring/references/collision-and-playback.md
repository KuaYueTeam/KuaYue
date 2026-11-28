# Code-defined collision and reversible playback

Read this reference when a BBModel needs collision shapes that existing factories cannot express,
or when one block state should play an authored clip forward and backward.

When KuaYue's actual KasugaLib dependency includes the data-driven shape handler, author static and
state-selected selection/collision geometry through [shapes.md](shapes.md) instead of adding new Java
shape code. Keep code-defined collision for behavior that the shape slots cannot express. The shape
schema/compiler checks definition structure; KuaYue tests must still prove state-rule selection and
anchor clipping.

## Collision from BBModel geometry

Derive the collision profile from the hierarchy instead of copying the model's overall bounds:

1. Identify elements bound to moving named groups and elements belonging to the static body.
2. Exclude editor-only witnesses, selection meshes, and hidden or non-exported geometry after checking
   their role in the source model.
3. Work in the north-facing bind pose. Apply each element origin, named-group origin, and static group
   rotation before converting Blockbench pixels.
4. Include the proxy root's translation. Horizontal model coordinates are relative to the block center,
   so a full-block center contributes eight pixels on X and Z. Do not infer the root offset from the
   model's texture or visible camera position.
5. Split the result into a small set of code-authored AABBs. Keep a doorway, ladder opening, window
   opening, or other intentional passage empty in the static body; add the separate moving-part shape
   only for the state where it occupies that space.
6. Rotate the completed north-facing shape once according to the block's `facing`. Do not pre-rotate
   individual boxes to mimic proxy yaw.

A multi-box collision profile is appropriate when the model is a frame, rail assembly, lattice, or
another shape with significant empty space. Do not turn sparse geometry into one full block merely to
make collision easy.

When the user explicitly requests one box enclosing the model's length, width, and height, compute the
transformed overall bounds including rotated element corners and return that single box for both
selection and collision. Do not approximate it with a hard-coded full block.

When a matching part exists inside the same block, define a mirrored collision profile with explicit
coordinates instead of transforming a `VoxelShape` during static initialization. Verify both endpoint
ordering and that the mirrored profile is non-empty.

If the model shows a visible connector, pier, lower fascia, or trim but ray selection passes into the
block behind it, compare that region against the AABB list. Multicell only extends an existing parent
shape; it cannot repair geometry missing from the parent shape. Add the omitted static box rather than
expanding the whole facade.

Some Blockbench sources contain zero-height Y witness cubes used only for alignment. Exclude cubes where
`from[1] == to[1]` unless the model documentation explicitly says they are collision surfaces.

Collision and selection do not have to match. When the requirement is specifically a full-block
collision box, override `getCollisionShape` with `Shapes.block()` while preserving the model-derived
selection shape unless the user asks to change selection too.

The black selection outline uses `getShape`; walking and placement use
`getCollisionShape`. Changing only collision leaves a stale plate outline. When
the requirement says both use the same shape, route both methods through one
canonical private helper so later source edits cannot desynchronize them.

Record each endpoint's source bounds and transform in the implementation comment or task notes. Stable
numbers make later source revisions reviewable.

When extending `TrainPanelBlock`, add only the subclass's extra properties in
`createBlockStateDefinition`; `facing` is already added by the parent. Re-adding `FACING` crashes
registration with `duplicate property: facing`.

## Multi-cell proxy selection

KuaYue models that extend beyond the anchor voxel also need companion cells. Declare the canonical
north-facing cell union in KuaYue block `params`, not in KasugaLibNext `proxy_model_be`:

```json
{
  "params": {
    "multi_cell": {
      "cells": [[-1, 0, 0], [0, 1, 0], [1, 0, 1]]
    }
  }
}
```

Offsets exclude `[0,0,0]` and rotate with `HORIZONTAL_FACING`. The companion follows the parent's
current `getShape()`, clips it to its own cell, and delegates use, break progress, harvest checks, and
pick-block to the anchor. It must remain invisible, collisionless, and itemless.

Before writing the anchor, preflight the anchor and all companion positions selected by the candidate
state. Reject the entire placement if any target is occupied by another anchor/companion or is not
replaceable. The manager's post-placement `ensure` step is reconciliation only; it cannot protect
against partial overlaps after the anchor has already been placed. Client placement prediction and
the server block-item path must call the same preflight, and state revalidation must allow an existing
anchor's own cells.

If different block states occupy different cells, use `cells_by_state` instead of declaring the
union. Keys use the serialized property and value names:

```json
{
  "cells_by_state": {
    "hinge": {
      "left": [[1, 0, 0], [0, 1, 0], [1, 1, 0]],
      "right": [[-1, 0, 0], [0, 1, 0], [-1, 1, 0]]
    }
  }
}
```

For exact multi-property selection, use `state_cells`; each item has a non-empty string-valued
`when` and `cells`. The most specific matching rule wins, equal specificity is ambiguous, and
`cells: []` makes that state produce no companions.

```json
{
  "state_cells": [
    {
      "when": {"facing": "north", "open": "true"},
      "cells": [[0, 0, 1]]
    }
  ]
}
```

Keep `cells` as a fallback when a rule may not match. The manager reconciles stale companions whenever
the anchor state changes.

Keep the visual model group, `model_by_state`, `cells_by_state`, and code-defined collision state on the
same semantic side. When physical left/right source files are misnamed, swap the source filenames,
internal model names, runtime copies, `model_by_state`, cells, and collision profiles together. Fixing
only one layer turns future source edits into another silent reversal.

Do not move this field into the shared Proxy API: cell ownership and companion lifecycle are KuaYue
multi-block structure concerns. The foundation may be reused by other multi-cell train parts.

## Collision and animation states

A normal `VoxelShape` returns one discrete shape for a block state. If `open=true` immediately selects
the open shape, collision changes at the state transition rather than following every intermediate
rotation. State this limitation explicitly when the user expects physical collision during motion.

Do not block animation merely because collision is discrete. First finish the correct closed/open
shapes and confirm the intended gameplay behavior. Continuous swept collision is a separate capability.

## Reversible clip timing

Treat these as different values:

- clip length: the complete Blockbench timeline, including any trailing hold;
- motion end: the last authored animation keyframe that changes pose;
- current source time: the committed sampled time used when a toggle interrupts playback.

For a boolean open/close part, use the last motion keyframe as the logical open endpoint by default.
Starting a reverse from the full clip length replays the trailing hold and looks like a delayed response.
Use the full clip length when the hold itself is an intentional part of the action.

When reversing:

1. Stop the existing playback instance.
2. Read its current committed source time.
3. Preserve that source time while switching direction at the same logical tick.
4. Cover both completed-state reversal and interruption from an intermediate frame.

The JY maintenance end face and side door are worked examples. When `door_open` declares trailing hold,
use its last motion key rather than the full clip length; treating the declared length as the open
endpoint delays reversal.

## Verification

- Check north closed, north open, and at least one rotated `facing` numerically.
- Compare walls or openings against the supplied geometry, especially negative and out-of-cell extents.
- Toggle open → close after the open clip completes; movement must begin immediately unless the source
  explicitly authors a hold.
- Toggle during both directions to confirm interruption starts from the visible midpoint.
- In the client, verify collision while approaching from multiple sides; server-side shape inspection
  alone does not prove the model and AABBs stay aligned.
