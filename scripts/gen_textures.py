#!/usr/bin/env python3
"""Draws every texture of the mod in a cyberpunk pixel-art style: neon-edged hardware on dark steel.

Items: one 16x16 icon per implant. The housing shape and the neon colour come from the implant's category, the glyph in
the middle from the implant itself. Blocks: the Rig, Charger, Neural Port and Proximity Sensor, plus the animated gel fluids.
Stdlib only. Run from the repository root:  python3 scripts/gen_textures.py   (gen_layer1_data.py calls it too)
"""
import json
import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "cyberimplants")

# ---------------------------------------------------------------- palette
OUTLINE = (0x05, 0x07, 0x0D)
STEEL_DARK = (0x12, 0x17, 0x26)
STEEL = (0x1C, 0x24, 0x3A)
STEEL_LIGHT = (0x2C, 0x38, 0x56)
STEEL_HI = (0x45, 0x55, 0x7A)
WHITE = (0xF2, 0xFA, 0xFF)

NEON = {  # category -> bright neon colour
    "core": (0xE6, 0xF4, 0xFF), "neural": (0xB4, 0x5C, 0xFF), "cyberdeck": (0x39, 0xFF, 0x88),
    "skeletal": (0xFF, 0xB8, 0x30), "limbs": (0xFF, 0x5A, 0x2A), "optical": (0x19, 0xD9, 0xFF),
    "circulatory": (0xFF, 0x2A, 0x6D), "subdermal": (0x8F, 0xB4, 0xE0), "mobility": (0xF5, 0xF5, 0x4A),
}
SHAPE = {"core": "chip", "neural": "hex", "cyberdeck": "chip", "skeletal": "clip", "limbs": "plate",
         "optical": "round", "circulatory": "pill", "subdermal": "plate", "mobility": "clip"}


def dim(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


# ---------------------------------------------------------------- canvas and PNG
class Canvas:
    def __init__(self, w=16, h=16, fill=None):
        self.w, self.h = w, h
        self.px = [[fill for _ in range(w)] for _ in range(h)]

    def set(self, x, y, c, a=255):
        if 0 <= x < self.w and 0 <= y < self.h and c is not None:
            self.px[y][x] = tuple(c) + (a,)

    def get(self, x, y):
        return self.px[y][x]

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)

    def glyph(self, rows, x0, y0, colour, shade=None):
        """rows of '#' (colour), '+' (white highlight) and 'o' (shade)."""
        for gy, row in enumerate(rows):
            for gx, ch in enumerate(row):
                if ch == "#":
                    self.set(x0 + gx, y0 + gy, colour)
                elif ch == "+":
                    self.set(x0 + gx, y0 + gy, WHITE)
                elif ch == "o":
                    self.set(x0 + gx, y0 + gy, shade or dim(colour, 0.55))

    def png(self):
        raw = b"".join(b"\x00" + b"".join(bytes(p if p else (0, 0, 0, 0)) for p in row) for row in self.px)

        def chunk(tag, data):
            c = struct.pack(">I", len(data)) + tag + data
            return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

        return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", self.w, self.h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def save(path, data):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "wb") as f:
        f.write(data)


# ---------------------------------------------------------------- 7x7 glyphs
G = {
    "eye": [".#####.", "#.ooo.#", "#oo+oo#", "#.ooo.#", ".#####.", ".......", "......."],
    "bolt": ["...##+.", "..##+..", ".##+...", "#####+.", "..##+..", ".##+...", ".#+...."],
    "shield": ["#######", "#o+oo##", "#ooooo#", ".#ooo#.", ".#ooo#.", "..#o#..", "...#..."],
    "heart": [".##.##.", "#+#####", "#######", ".#####.", "..###..", "...#...", "......."],
    "gear": ["..#.#..", ".#####.", "##o.o##", ".#.+.#.", "##o.o##", ".#####.", "..#.#.."],
    "arrow": ["...#...", "..###..", ".#####.", "...#...", "...#...", "...#...", "...#..."],
    "blade": ["......#", ".....##", "....##.", "...##..", "..##...", ".##....", "#o....."],
    "spring": ["..###..", ".#...#.", "..###..", ".#...#.", "..###..", ".#...#.", "..###.."],
    "chip": [".#.#.#.", "#######", ".#ooo#.", "##o+o##", ".#ooo#.", "#######", ".#.#.#."],
    "wave": ["#.....#", "##...##", ".#...#.", ".##.##.", "..###..", "...#...", "......."],
    "cross": ["...#...", "...#...", "#######", "...#...", "...#...", "...#...", "......."],
    "target": ["..###..", ".#...#.", "#..#..#", "#.#+#.#", "#..#..#", ".#...#.", "..###.."],
    "bone": ["##...##", "###.###", ".#####.", ".#####.", "###.###", "##...##", "......."],
    "fist": [".#####.", "#######", "#######", "#######", ".#####.", ".##.##.", "......."],
    "boot": [".####..", ".####..", ".####..", ".#####.", "#######", "#######", "#######"],
    "drop": ["...#...", "..###..", ".#####.", "#######", "#######", ".#####.", "..###.."],
    "brain": [".##.##.", "#######", "#o#o#o#", "#######", "#o#o#o#", ".#####.", "..#.#.."],
    "ram": ["#######", "#.#.#.#", "#######", ".......", "#######", "#.#.#.#", "#######"],
    "disk": ["#####..", "#...###", "#.o.#.#", "#...###", "#.####.", "#.....#", "#######"],
    "plate": ["#######", "#.....#", "#.###.#", "#.#o#.#", "#.###.#", "#.....#", "#######"],
    "pulse": [".......", "#......", ".#...#.", "..#.#.#", "...#..#", ".......", "......."],
    "scan": ["#.....#", "..###..", ".#...#.", ".#.+.#.", ".#...#.", "..###..", "#.....#"],
    "flame": ["...#...", "..##...", "..###..", ".#####.", "##o+o##", "##ooo##", ".#####."],
    "ghost": [".#####.", "#######", "#.#.#.#", "#######", "#######", "#.#.#.#", "......."],
    "claw": ["#.#.#.#", "#.#.#.#", "#######", ".#####.", ".#####.", "..###..", "......."],
    "magnet": ["##...##", "##...##", "##...##", "##...##", "###.###", ".#####.", "..###.."],
    "link": ["###.###", "#.....#", "#.###.#", "...#...", "#.###.#", "#.....#", "###.###"],
    "snow": ["#..#..#", ".#.#.#.", "..###..", "#######", "..###..", ".#.#.#.", "#..#..#"],
    "cell": ["..###..", ".#####.", ".#+###.", ".#####.", ".#####.", ".#####.", ".#####."],
    "tag": ["#####..", "#...##.", "#.+..##", "#....##", "#...##.", "#####..", "......."],
    "mask": [".#####.", "#######", "#o###o#", "#o###o#", ".#####.", "..###..", "......."],
    "dash": ["..#..#.", ".##.##.", "###.###", "####+##", "###.###", ".##.##.", "..#..#."],
}

GLYPH = {
    "cybernetic_core": "chip", "sandevistan": "wave", "reflex_booster": "bolt", "combat_predictor": "target",
    "automatic_evasion_system": "dash", "kerenzikov": "pulse", "deck_chassis": "plate", "deck_processor": "chip",
    "deck_ram": "ram", "deck_storage": "disk", "deck_coprocessor": "link", "deck_machine_interface": "gear",
    "reinforced_skeleton": "bone", "power_actuators": "fist", "kinetic_tendons": "spring", "micro_actuators": "gear",
    "impact_dampeners": "arrow", "inertia_compensation": "magnet", "cybernetic_arm": "fist",
    "remote_manipulator": "claw", "retractable_blade": "blade", "microgrip": "claw", "ballistic_module": "cross",
    "flechette": "blade", "entity_scanner": "scan", "thermal_vision": "flame", "machine_analyzer": "gear",
    "ballistic_visor": "target", "optical_camouflage": "ghost", "target_tracking": "tag", "biomonitor": "pulse",
    "regenerator": "cell", "adrenaline_pump": "drop", "emergency_defibrillator": "bolt", "energy_buffer": "cell",
    "emergency_response_system": "cross", "armor_plating": "plate", "nanocoating": "snow", "shock_circuit": "bolt",
    "kinetic_shield": "shield", "emergency_shell": "shield", "enhanced_tendons": "spring", "hydraulic_legs": "arrow",
    "silent_actuators": "mask", "magnetic_soles": "magnet", "inertial_stabilizer": "cross", "momentum_booster": "dash",
    "incomplete_implant": "chip", "neuro_stabilizer": "brain",
}

CATEGORY = {
    "cybernetic_core": "core", "sandevistan": "neural", "reflex_booster": "neural", "combat_predictor": "neural",
    "automatic_evasion_system": "neural", "kerenzikov": "neural", "deck_chassis": "cyberdeck", "deck_processor": "cyberdeck",
    "deck_ram": "cyberdeck", "deck_storage": "cyberdeck", "deck_coprocessor": "cyberdeck", "deck_machine_interface": "cyberdeck",
    "reinforced_skeleton": "skeletal", "power_actuators": "skeletal", "kinetic_tendons": "skeletal", "micro_actuators": "skeletal",
    "impact_dampeners": "skeletal", "inertia_compensation": "skeletal", "cybernetic_arm": "limbs", "remote_manipulator": "limbs",
    "retractable_blade": "limbs", "microgrip": "limbs", "ballistic_module": "limbs", "flechette": "limbs",
    "entity_scanner": "optical", "thermal_vision": "optical", "machine_analyzer": "optical", "ballistic_visor": "optical",
    "optical_camouflage": "optical", "target_tracking": "optical", "biomonitor": "circulatory", "regenerator": "circulatory",
    "adrenaline_pump": "circulatory", "emergency_defibrillator": "circulatory", "energy_buffer": "circulatory",
    "emergency_response_system": "circulatory", "armor_plating": "subdermal", "nanocoating": "subdermal",
    "shock_circuit": "subdermal", "kinetic_shield": "subdermal", "emergency_shell": "subdermal",
    "enhanced_tendons": "mobility", "hydraulic_legs": "mobility", "silent_actuators": "mobility",
    "magnetic_soles": "mobility", "inertial_stabilizer": "mobility", "momentum_booster": "mobility",
    "incomplete_implant": "core", "neuro_stabilizer": "neural",
}


# ---------------------------------------------------------------- item icons
def housing(cv, shape, neon):
    """Dark steel body with a neon rim, centred in the 16x16 cell. Returns the inner bounding box."""
    if shape == "round":
        cells = {(x, y) for y in range(16) for x in range(16) if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 7.4 ** 2}
    elif shape == "pill":
        cells = {(x, y) for y in range(1, 15) for x in range(3, 13)
                 if not ((y < 4 or y > 11) and (x - 7.5) ** 2 + (min(abs(y - 3.5), abs(y - 11.5))) ** 2 > 20)}
    elif shape == "hex":
        cells = {(x, y) for y in range(1, 15) for x in range(1, 15) if abs(x - 7.5) + abs(y - 7.5) * 0.55 <= 7.6 and abs(y - 7.5) <= 6.5}
    elif shape == "clip":
        cells = {(x, y) for y in range(2, 14) for x in range(2, 14) if not ((x in (2, 13)) and (y in (2, 13)))}
    elif shape == "plate":
        cells = {(x, y) for y in range(2, 14) for x in range(1, 15)}
    else:  # chip
        cells = {(x, y) for y in range(3, 13) for x in range(3, 13)}

    # pins: chips get them on all four sides, clips only left and right
    if shape == "chip":
        for y in (4, 7, 10):
            for x in (1, 2, 13, 14):
                cv.set(x, y, dim(neon, 0.6) if x in (1, 14) else neon)
        for x in (5, 8, 11):
            cv.set(x, 1, dim(neon, 0.6))
            cv.set(x, 2, neon)
            cv.set(x, 13, neon)
            cv.set(x, 14, dim(neon, 0.6))
    elif shape == "clip":
        for y in (4, 7, 10):
            for x in (0, 1, 14, 15):
                cv.set(x, y, dim(neon, 0.6) if x in (0, 15) else neon)
    for (x, y) in cells:
        edge = any((x + dx, y + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        cv.set(x, y, OUTLINE if edge else STEEL)
    for (x, y) in cells:
        if (x, y) not in cells or cv.get(x, y)[:3] == OUTLINE:
            continue
        inner_edge = any(cv.get(x + dx, y + dy)[:3] == OUTLINE for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                         if 0 <= x + dx < 16 and 0 <= y + dy < 16 and cv.px[y + dy][x + dx] is not None)
        if inner_edge:
            top_left = (x + y) < 15
            cv.set(x, y, neon if top_left else dim(neon, 0.45))
        else:
            cv.set(x, y, STEEL_LIGHT if (x + y) % 2 == 0 and y < 7 else STEEL)
    # dark recess behind the glyph
    cv.rect(4, 4, 11, 11, STEEL_DARK)
    for (x, y) in ((4, 4), (11, 4), (4, 11), (11, 11)):
        if cv.get(x, y)[:3] != OUTLINE and (x, y) in cells:
            cv.set(x, y, STEEL)


def item_icon(ident):
    category = CATEGORY.get(ident, "core")
    neon = NEON[category]
    cv = Canvas()
    housing(cv, SHAPE[category], neon)
    cv.glyph(G[GLYPH.get(ident, "chip")], 4, 4, neon, dim(neon, 0.5))
    # tiny status LED, a corner highlight
    cv.set(12, 3, WHITE)
    return cv


def bucket_icon(neon, deep):
    cv = Canvas()
    for y in range(3, 15):  # bucket body (tapered)
        inset = (y - 3) // 6
        for x in range(3 + inset, 13 - inset):
            edge = x in (3 + inset, 12 - inset) or y in (3, 14)
            cv.set(x, y, OUTLINE if edge else STEEL)
    for x in range(4, 12):
        cv.set(x, 3, STEEL_HI)
    for y in range(6, 14):  # glowing gel
        inset = (y - 3) // 6
        for x in range(4 + inset, 12 - inset):
            cv.set(x, y, mix(deep, neon, 0.3 + 0.5 * ((x + y) % 3) / 2))
    cv.set(5, 6, WHITE)
    cv.rect(2, 1, 13, 2, OUTLINE)
    cv.rect(3, 1, 12, 1, STEEL_HI)
    return cv


# ---------------------------------------------------------------- blocks
def panel(cv, base, neon, rivets=True):
    cv.rect(0, 0, 15, 15, base)
    for i in range(16):
        for x, y in ((i, 0), (0, i)):
            cv.set(x, y, STEEL_HI)
        for x, y in ((i, 15), (15, i)):
            cv.set(x, y, OUTLINE)
    if rivets:
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
            cv.set(x, y, STEEL_HI)
            cv.set(x + (1 if x < 8 else -1), y + (1 if y < 8 else -1), OUTLINE)


def hazard(cv, y0, y1, neon):
    for y in range(y0, y1 + 1):
        for x in range(1, 15):
            cv.set(x, y, neon if ((x + y) // 2) % 2 == 0 else OUTLINE)


def rig_side():
    cv = Canvas()
    panel(cv, STEEL, NEON["circulatory"])
    cv.rect(3, 3, 12, 8, STEEL_DARK)  # display
    for x in range(4, 12):
        cv.set(x, 5 + (1 if (x // 2) % 2 else 0), NEON["optical"])
    cv.rect(3, 3, 12, 3, dim(NEON["optical"], 0.5))
    hazard(cv, 10, 11, NEON["mobility"])
    for x in (4, 6, 8):
        cv.set(x, 13, NEON["circulatory"] if x != 8 else NEON["cyberdeck"])
    return cv


def rig_top():
    cv = Canvas()
    panel(cv, STEEL_LIGHT, NEON["optical"])
    cv.rect(3, 3, 12, 12, STEEL_DARK)
    for i in range(3, 13):
        cv.set(i, 3, NEON["circulatory"])
        cv.set(i, 12, dim(NEON["circulatory"], 0.5))
        cv.set(3, i, NEON["circulatory"])
        cv.set(12, i, dim(NEON["circulatory"], 0.5))
    cv.glyph(G["cross"], 5, 4, NEON["circulatory"], dim(NEON["circulatory"], 0.5))
    return cv


def rig_bottom():
    cv = Canvas()
    panel(cv, STEEL_DARK, NEON["optical"], rivets=False)
    hazard(cv, 6, 9, dim(NEON["mobility"], 0.7))
    return cv


def charger_side():
    cv = Canvas()
    panel(cv, STEEL, NEON["optical"])
    for y in range(3, 13):  # copper coil windings
        c = (0xC8, 0x78, 0x38) if y % 2 == 0 else (0x8A, 0x50, 0x28)
        for x in range(3, 13):
            cv.set(x, y, c)
    for y in range(3, 13, 2):
        cv.set(3, y, OUTLINE)
        cv.set(12, y, OUTLINE)
    cv.rect(2, 7, 13, 8, dim(NEON["optical"], 0.9))
    cv.rect(2, 7, 13, 7, NEON["optical"])
    return cv


def charger_top():
    cv = Canvas()
    panel(cv, STEEL_LIGHT, NEON["optical"])
    cv.rect(3, 3, 12, 12, STEEL_DARK)
    cv.glyph(G["bolt"], 4, 4, NEON["optical"], dim(NEON["optical"], 0.5))
    for i in range(3, 13):
        cv.set(i, 3, dim(NEON["optical"], 0.6))
        cv.set(3, i, dim(NEON["optical"], 0.6))
    return cv


def port_tex():
    cv = Canvas()
    panel(cv, STEEL_DARK, NEON["cyberdeck"], rivets=False)
    cv.rect(2, 2, 13, 13, STEEL)
    cv.rect(4, 4, 11, 11, OUTLINE)
    for x in (5, 7, 9):
        cv.rect(x, 5, x + 1, 10, NEON["cyberdeck"])
        cv.set(x, 5, WHITE)
    return cv


def sensor_tex():
    cv = Canvas()
    panel(cv, STEEL, NEON["circulatory"])
    for y in range(3, 13):
        for x in range(3, 13):
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 20:
                cv.set(x, y, OUTLINE)
    for y in range(4, 12):
        for x in range(4, 12):
            d = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if d <= 13:
                cv.set(x, y, mix(NEON["circulatory"], (0x60, 0x08, 0x20), min(1.0, d / 13)))
    cv.set(6, 6, WHITE)
    return cv


# ---------------------------------------------------------------- fluids
def gel_still(neon, deep, frames=16):
    cv = Canvas(16, 16 * frames)
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                wave = (x * 2 + y + f) % 16
                t = 0.25 + 0.35 * (1 + __import__("math").sin(wave / 16 * 6.2832)) / 2
                if (x * 7 + y * 13 + f * 3) % 29 == 0:
                    cv.set(x, f * 16 + y, WHITE, 230)
                else:
                    cv.set(x, f * 16 + y, mix(deep, neon, t), 235)
    return cv


def gel_flow(neon, deep, frames=16):
    cv = Canvas(16, 16 * frames)
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                streak = (y * 2 - f * 2 + x // 4 * 5) % 16
                t = 0.2 + 0.5 * (streak < 6)
                cv.set(x, f * 16 + y, mix(deep, neon, t), 235)
    return cv


def mcmeta(frametime=2):
    return (json.dumps({"animation": {"frametime": frametime}}, indent=2) + "\n").encode()


# ---------------------------------------------------------------- driver
def main():
    for ident in CATEGORY:
        if ident.endswith("_bucket"):
            continue
        save("textures/item/%s.png" % ident, item_icon(ident).png())
    save("textures/item/bio_interface_gel_bucket.png", bucket_icon((0x6F, 0xE0, 0xA8), (0x0E, 0x6A, 0x4A)).png())
    save("textures/item/neural_gel_bucket.png", bucket_icon((0xB4, 0x5C, 0xFF), (0x3E, 0x12, 0x7A)).png())
    for name, cv in (("implantation_rig", rig_side()), ("implantation_rig_top", rig_top()), ("implantation_rig_bottom", rig_bottom()),
                     ("kinetic_charger", charger_side()), ("kinetic_charger_top", charger_top()),
                     ("neural_port", port_tex()), ("proximity_sensor", sensor_tex())):
        save("textures/block/%s.png" % name, cv.png())
    for gel, neon, deep in (("bio_interface_gel", (0x6F, 0xE0, 0xA8), (0x0E, 0x6A, 0x4A)),
                            ("neural_gel", (0xB4, 0x5C, 0xFF), (0x3E, 0x12, 0x7A))):
        save("textures/block/%s_still.png" % gel, gel_still(neon, deep).png())
        save("textures/block/%s_still.png.mcmeta" % gel, mcmeta())
        save("textures/block/%s_flow.png" % gel, gel_flow(neon, deep).png())
        save("textures/block/%s_flow.png.mcmeta" % gel, mcmeta())

    # models that use the new multi-face textures
    def model(path, obj):
        save(path, (json.dumps(obj, indent=2) + "\n").encode())

    model("models/block/implantation_rig.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": "cyberimplants:block/implantation_rig", "top": "cyberimplants:block/implantation_rig_top",
        "bottom": "cyberimplants:block/implantation_rig_bottom"}})
    model("models/block/kinetic_charger.json", {"parent": "minecraft:block/slab", "textures": {
        "bottom": "cyberimplants:block/implantation_rig_bottom", "top": "cyberimplants:block/kinetic_charger_top",
        "side": "cyberimplants:block/kinetic_charger"}})


if __name__ == "__main__":
    main()
