"""Re-derive proxy-model AABBs for the c25k parcel from the .bbmodel sources.

Implements the transform chain KasugaLib's KsgBbModelLoader bakes into render
space (group pivots + element pivots + Blockbench euler order), then converts to
block pixels with the proxy placement offset (+8 X/Z, see
JYMaintenanceTailShapes / ProxyModelProxy.placementRoot).
"""
import json
import math
import os
import sys

PACK = r"D:\KuaYue_6K\modules\train-parts\resources\packs\kuayue-preset-passenger-new-25"
MODELS = os.path.join(PACK, "parcels", "c25k", "models", "block", "carriage", "c25k")


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def mat_vec(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


def rot(deg, order="zyx"):
    """Blockbench euler degrees -> 3x3 rotation matrix in the loader's order."""
    deg = deg or (0.0, 0.0, 0.0)
    x, y, z = (math.radians(float(deg[i] or 0.0)) for i in range(3))
    cx, sx = math.cos(x), math.sin(x)
    cy, sy = math.cos(y), math.sin(y)
    cz, sz = math.cos(z), math.sin(z)
    rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    if order == "zyx":
        # QuaternionHelper.fromZYXAngle: qZ * qY * qX  ->  Rz * Ry * Rx
        return mat_mul(rz, mat_mul(ry, rx))
    # QuaternionHelper.fromXYZDegrees (JOML rotationXYZ): Rx * Ry * Rz
    return mat_mul(rx, mat_mul(ry, rz))


IDENT = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]


class Xf:
    """Accumulated pivot-aware transform; mirrors BlockBenchTransform."""

    __slots__ = ("r", "a", "p")

    def __init__(self, r, a, p):
        self.r, self.a, self.p = r, a, p

    @staticmethod
    def identity():
        return Xf(IDENT, [0.0, 0.0, 0.0], [0.0, 0.0, 0.0])

    def child(self, pivot_px, local_rot):
        scaled = [float(c) / 16.0 for c in pivot_px]
        delta = [scaled[i] - self.p[i] for i in range(3)]
        origin = [a + b for a, b in zip(mat_vec(self.r, delta), self.a)]
        return Xf(mat_mul(self.r, local_rot), origin, scaled)

    def apply(self, point):
        return [a + b for a, b in zip(mat_vec(self.r, point), self.a)]


def element_boxes(element, xf):
    if element.get("export") is False or element.get("visibility") is False:
        return None
    origin = element.get("origin") or [0, 0, 0]
    mesh = element.get("type") == "mesh"
    local = xf.child(origin, rot(element.get("rotation"), "xyz" if mesh else "zyx"))
    if mesh or (element.get("from") is None and element.get("to") is None):
        verts = element.get("vertices") or {}
        if not verts:
            return None
        pts = [local.apply([v[0] / 16.0, v[1] / 16.0, v[2] / 16.0]) for v in verts.values()]
        return [min(p[i] for p in pts) for i in range(3)] + [max(p[i] for p in pts) for i in range(3)]
    frm, to = element["from"], element["to"]
    lo = [(min(frm[i], to[i]) - origin[i]) / 16.0 for i in range(3)]
    hi = [(max(frm[i], to[i]) - origin[i]) / 16.0 for i in range(3)]
    corners = [local.apply([bx, by, bz]) for bx in (lo[0], hi[0])
               for by in (lo[1], hi[1]) for bz in (lo[2], hi[2])]
    return [min(p[i] for p in corners) for i in range(3)] + [max(p[i] for p in corners) for i in range(3)]


def walk(nodes, elements, xf, out):
    for node in nodes:
        if isinstance(node, str):
            element = elements.get(node)
            if element is None:
                continue
            box = element_boxes(element, xf)
            if box:
                out.append(box)
            continue
        if node.get("export", True) is False or node.get("visibility", True) is False:
            continue
        child = xf.child(node.get("origin") or [0, 0, 0], rot(node.get("rotation")))
        walk(node.get("children") or [], elements, child, out)


def model_boxes(path):
    data = json.load(open(path, encoding="utf-8"))
    elements = {e.get("uuid"): e for e in data.get("elements", [])}
    out = []
    walk(data.get("outliner") or [], elements, Xf.identity(), out)
    if not out:
        for element in elements.values():
            box = element_boxes(element, Xf.identity())
            if box:
                out.append(box)
    return [[round(v * 16.0, 4) for v in box] for box in out]


if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else MODELS
    for name in sorted(os.listdir(target)):
        if not name.endswith(".bbmodel"):
            continue
        boxes = model_boxes(os.path.join(target, name))
        if not boxes:
            print("%-34s (no geometry)" % name[:-8])
            continue
        lo = [min(b[i] for b in boxes) for i in range(3)]
        hi = [max(b[i + 3] for b in boxes) for i in range(3)]
        print("%-34s n=%-3d x %7.2f..%-7.2f y %7.2f..%-7.2f z %7.2f..%-7.2f"
              % (name[:-8], len(boxes), lo[0], hi[0], lo[1], hi[1], lo[2], hi[2]))
