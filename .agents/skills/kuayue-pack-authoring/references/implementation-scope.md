# Implementation scope

Use this reference when deciding whether a requested vehicle feature belongs to
content integration or requires a separate gameplay feature.

## Layer boundaries

Treat the work as three layers:

| Layer | Decision |
|---|---|
| Resources and data | Always in scope for confirmed pack content. |
| Vehicle integration code | In scope when it connects an existing engine behavior to a confirmed block, model, state, collision shape, interaction, or artist-authored animation. |
| New gameplay logic | Out of scope when it defines new player-facing rules or a broader simulation/system. |

A missing factory is not by itself a blocker. A shaped door, special collision
box, or content-specific interaction may require a dedicated block class and
factory; that is still integration when the behavior is local to the block and
reproduces an established train-part concept.

## In-scope examples

- A BBModel loads through `proxy_model`, keeps its internal transforms, and has
  four correct `facing` states.
- A directional console uses a custom `ProxyModelBlock` host because its
  selection and collision must share one model-enclosing AABB.
- A door uses closed/open or left/right hinge states, swaps its model, updates
  its collision shape, and responds to the normal use action.
- A confirmed floor or leg is climbable through block-local `isLadder`
  behavior and the standard climbable tag.
- A proxy-rendered end face derives closed/open AABBs from the supplied
  Blockbench geometry and drives its `door_open` clip with continuous reversal.
- A deployable part plays one supplied clip forward to extend, backward to
  retract, and reverses from the current time when interrupted.
- A renderer needs a small general KasugaLibNext fix because several vehicle
  parts lose nested group transforms or playback state through the same path.

These cases use short-lived block state and existing playback mechanisms. Put a
dedicated Minecraft interaction in Java rather than adding a vehicle-specific
JSON param to a general proxy type.

## Out-of-scope examples

- Designing a new controlling, braking, coupling, signalling, economy,
  progression, or mission workflow.
- Simulating many blocks over time, inventing a new save/network protocol, or
  adding permissions and cross-player rules.
- Adding a generic scripting/state-machine language because one vehicle needs
  several conditional interactions.
- Rewriting the proxy system without evidence that the existing API cannot
  express the required presentation or animation.

## Decision test

Ask these questions before writing code:

1. Does the feature make the confirmed vehicle block render, collide, open,
   orient, or animate like an established block family?
2. Can it be expressed with block state, an existing interaction, and a finite
   model clip?
3. Is the code scoped to the consuming mod, apart from a minimal fix to a
   demonstrably general library defect?

If the answers are yes, keep the work in scope. If the request instead defines
new rules, persistent systems, or a broad player workflow, stop and ask to
separate that feature from pack authoring. Do not ask merely because Java code
is required.
