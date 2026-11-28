"""End-face collision profiles copied from KuaYue's EndFaceShapes.java.

The reference blocks are `kuayue:22_end_face` (kuayue-preset-passenger-22) and the old
`end_face_25k_*` / `end_face_sliding_25k_*` of kuayue-preset-passenger-25: both are
`custom_rendered_end_face` hosts whose shapes come from `EndFaceShapes.getEndFaceShape`,
moved up one block by `EndFaceBlock.getShape`.

Door-family mapping used by that code:
* rotating door (`rotate_single_sided`, 橡胶端面): closed -> KT, open -> BG_OPEN
* sliding door (`slide_2`, 折棚端面): closed -> BG, open -> KT_OPEN
* `no_door` is coerced to sliding + open -> KT_OPEN

Boxes below are the north-facing bind pose in Blockbench pixels; `Y_SHIFT` reproduces the
`.move(0, 1, 0)` that aligns the profile with the rendered face.
"""
Y_SHIFT = 16.0

KT_NORTH = [
    [-16, -16, -1, 32, 21, 1],
    [-1, 17.75, -7.975, 17, 19, -1],
    [15.2, -14, -7.975, 17.2, 18, -1],
    [0, -16, -7.7, 2, -11, -1.25],
    [-1.2, -14, -7.975, 0.8, 18, -1],
    [-1, -16, -7.75, 17, -14, -6.15],
    [-1, -16, -6.375, 17, -14, 1],
    [1, 18, -1, 15, 21, 1],
    [14, -16, -7.7, 16, -11, -1.25],
]

BG_NORTH = [
    [-16, -16, -1, 32, 21, 1],
    [-3, 18, -7.9, 19, 21, -5.15],
    [-1, 17.75, -6.15, 17, 19, -1],
    [15.5, -14, -7.9, 18.5, 18, -5.25],
    [-0.2, -14, -6.15, 0.8, 18, -1],
    [-2.5, -14, -7.9, 0.5, 18, -5.25],
    [15.2, -14, -6.15, 16.2, 18, -1],
    [-1, -16, -7.75, 17, -14, -6.15],
    [-1, -16, -6.15, 17, -14, 1],
    [1, 18, -1, 15, 21, 1],
]

KT_NORTH_OPEN = [
    [15, -16, -1, 32, 21, 1],
    [-16, -16, -1, 1, 21, 1],
    [1, 18, -1, 15, 21, 1],
    [-1, 17.75, -7.975, 17, 19, -1],
    [15.2, -14, -7.975, 17.2, 18, -1],
    [0, -16, -7.7, 2, -11, -1.25],
    [-1.2, -14, -7.975, 0.8, 18, -1],
    [-1, -16, -7.75, 17, -14, -6.15],
    [-1, -16, -6.375, 17, -14, 1],
    [14, -16, -7.7, 16, -11, -1.25],
    [-6, -14, 1, 1, 1, 2],
    [-4.75, 15, 1, -0.25, 18, 2],
    [-0.25, 1, 1, 1, 18, 2],
    [-6, 1, 1, -4.75, 18, 2],
    [15, -14, 1, 22, 1, 2],
    [16.25, 15, 1, 20.75, 18, 2],
    [15, 1, 1, 16.25, 18, 2],
    [20.75, 1, 1, 22, 18, 2],
]

BG_NORTH_OPEN = [
    [0, -14, 0, 2, 18, 14],
    [-1, -16, -7.75, 17, -14, -6.15],
    [15.5, -14, -7.9, 18.5, 18, -5.25],
    [-2.5, -14, -7.9, 0.5, 18, -5.25],
    [15.2, -14, -6.15, 16.2, 18, -1],
    [-0.2, -14, -6.15, 0.8, 18, -1],
    [-1, -16, -6.15, 17, -14, 1],
    [-3, 18, -7.9, 19, 21, -5.15],
    [-1, 17.75, -6.15, 17, 19, -1],
    [-16, -16, -1, 1, 21, 1],
    [1, 18, -1, 15, 21, 1],
    [15, -16, -1, 32, 21, 1],
]

# suffix -> (closed profile, open profile)
#
# Empty on purpose: the reference profiles below are the pre-rework `EndFaceShapes` boxes, and
# they no longer line up with the artist's model (the new end faces are taller and wider), so the
# collision now comes from the model itself — the same source every other part uses. The profiles
# stay in this module as documentation of the old behaviour.
PROFILES = {}


def shifted(boxes):
    return [[round(b[0], 3), round(b[1] + Y_SHIFT, 3), round(b[2], 3),
             round(b[3], 3), round(b[4] + Y_SHIFT, 3), round(b[5], 3)] for b in boxes]


def profile_for(suffix):
    """Return (closed_boxes, open_boxes) in north bind pose, already Y aligned, or None."""
    entry = PROFILES.get(suffix)
    if entry is None:
        return None
    closed, open_ = entry
    return shifted(closed), shifted(open_)
