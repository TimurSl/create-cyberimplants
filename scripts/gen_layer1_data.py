#!/usr/bin/env python3
"""Generates the Layer 1 data files: implant stats, placeholder recipes, language keys, models and textures.

Layer 1 keeps every stat file at five tiers (design rule 3) but fills tiers II-V with the Tier I numbers, changing
only Load. Layer 2 replaces the tier tables with the real numeric rows from docs/design/implants.

Run from the repository root:  python3 scripts/gen_layer1_data.py
"""
import json
import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
MOD = "cyberimplants"

# id: (category, [load T1..T5], charge_cost, cooldown_ticks, {values})
IMPLANTS = {
    "cybernetic_core": ("core", [0, 0, 0, 0, 0], 0, 0, {}),
    "sandevistan": ("neural", [9, 10, 11, 12, 14], 60, 3000, {"radius": 3, "duration_ticks": 40, "time_scale": 0.25}),
    "reflex_booster": ("neural", [4, 4, 5, 5, 6], 0, 0, {"attack_speed": 0.05, "walk_speed": 0.02, "item_use_speed": 0.10}),
    "combat_predictor": ("neural", [5, 5, 6, 6, 7], 0, 0, {"horizon_ticks": 10}),
    "automatic_evasion_system": ("neural", [5, 5, 6, 6, 7], 10, 0, {"charges": 1, "recharge_ticks": 240, "detection_ticks": 4, "distance": 1.5}),
    "kerenzikov": ("neural", [4, 4, 5, 5, 6], 8, 60, {"distance": 2.0, "window_ticks": 8}),
    "deck_chassis": ("cyberdeck", [4, 5, 6, 7, 8], 0, 0, {"module_sockets": 3, "macro_keys": 2, "log_lines": 20}),
    "deck_processor": ("cyberdeck", [1, 1, 1, 1, 1], 0, 0, {"cycles_per_tick": 20, "isa": 1}),
    "deck_ram": ("cyberdeck", [1, 1, 1, 1, 1], 0, 0, {"kb": 2}),
    "deck_storage": ("cyberdeck", [1, 1, 1, 1, 1], 0, 0, {"kb": 4}),
    "reinforced_skeleton": ("skeletal", [3, 3, 4, 4, 5], 0, 0, {"blunt_flat": 0.5, "machinery": 0.20}),
    "power_actuators": ("skeletal", [4, 4, 5, 6, 7], 2, 0, {"attack_damage": 1.0, "unarmed_multiplier": 2.0, "knockback": 0.2}),
    "kinetic_tendons": ("skeletal", [2, 3, 3, 4, 4], 0, 0, {"jump_strength": 0.042}),
    "micro_actuators": ("skeletal", [3, 3, 4, 4, 5], 0, 0, {"sprint_speed": 0.05}),
    "impact_dampeners": ("skeletal", [2, 2, 3, 3, 4], 0, 0, {"safe_fall": 2.0, "fall_reduction": 0.15}),
    "inertia_compensation": ("skeletal", [3, 3, 4, 4, 5], 0, 0, {"knockback_resistance": 0.10}),
    "cybernetic_arm": ("limbs", [3, 4, 4, 5, 6], 0, 0, {"break_speed": 0.10, "sockets": 1}),
    "remote_manipulator": ("limbs", [3, 3, 4, 4, 5], 1, 15, {"range": 6}),
    "retractable_blade": ("limbs", [3, 4, 4, 5, 6], 0, 0, {"damage": 6.0, "attack_speed": 1.8}),
    "microgrip": ("limbs", [2, 2, 3, 3, 4], 0, 0, {"stamina_ticks": 60, "climb_speed": 2.0}),
    "ballistic_module": ("limbs", [3, 4, 4, 5, 6], 2, 20, {"damage": 3.0, "velocity": 2.0, "magazine": 4, "reload_ticks": 40}),
    "entity_scanner": ("optical", [3, 3, 4, 4, 5], 5, 200, {"radius": 12, "reveal_ticks": 60, "max_markers": 8}),
    "thermal_vision": ("optical", [2, 3, 3, 4, 4], 0, 0, {"range": 16, "upkeep_per_second": 1.0}),
    "machine_analyzer": ("optical", [2, 2, 3, 3, 4], 0, 0, {}),
    "ballistic_visor": ("optical", [2, 3, 3, 4, 4], 0, 0, {"tracking_radius": 8, "max_projectiles": 3}),
    "optical_camouflage": ("optical", [6, 6, 7, 8, 9], 40, 1200, {"duration_ticks": 80, "upkeep_per_second": 2.0, "detect_still": 0.5, "detect_moving": 0.8, "aggro_drop_ticks": 60}),
    "target_tracking": ("optical", [3, 3, 4, 4, 5], 3, 0, {"range": 24, "max_tags": 1}),
    "biomonitor": ("circulatory", [1, 1, 2, 2, 2], 0, 0, {}),
    "regenerator": ("circulatory", [3, 3, 4, 5, 6], 0, 0, {"heal_per_second": 0.25, "lockout_ticks": 160, "charge_per_hp": 2.0}),
    "adrenaline_pump": ("circulatory", [2, 2, 3, 3, 4], 0, 600, {"trigger_damage": 3.0, "boost": 0.15, "boost_ticks": 60, "crash_ticks": 80}),
    "emergency_defibrillator": ("circulatory", [4, 4, 5, 5, 6], 80, 36000, {"health_restored": 2.0, "resistance_ticks": 40}),
    "energy_buffer": ("circulatory", [2, 2, 3, 3, 4], 0, 0, {"capacity": 50, "fill_rate": 1.0}),
    "emergency_response_system": ("circulatory", [3, 3, 4, 4, 5], 0, 600, {"response_slots": 1}),
    "armor_plating": ("subdermal", [3, 3, 4, 4, 5], 0, 0, {"armor": 2.0, "toughness": 0.0}),
    "nanocoating": ("subdermal", [3, 3, 4, 5, 6], 0, 0, {"balanced": 0.04}),
    "shock_circuit": ("subdermal", [3, 3, 4, 4, 5], 3, 0, {"damage": 2.0, "internal_cooldown_ticks": 30}),
    "kinetic_shield": ("subdermal", [4, 4, 5, 6, 7], 0, 0, {"capacity": 4.0, "absorb_share": 0.5, "recharge_delay_ticks": 160, "recharge_rate": 0.5, "charge_per_hp": 3.0, "emergency_cooldown_ticks": 900}),
    "emergency_shell": ("subdermal", [3, 3, 4, 4, 5], 30, 2400, {"trigger_health": 0.2, "reduction": 0.4, "duration_ticks": 40, "movement_penalty": 0.4}),
    "enhanced_tendons": ("mobility", [3, 3, 4, 4, 5], 4, 0, {"air_jumps": 1, "strength": 0.7}),
    "hydraulic_legs": ("mobility", [4, 4, 5, 5, 6], 15, 60, {"max_vertical": 4.0, "max_horizontal": 6.0, "full_charge_ticks": 30}),
    "silent_actuators": ("mobility", [2, 2, 3, 3, 4], 0, 0, {}),
    "magnetic_soles": ("mobility", [2, 2, 3, 3, 4], 0, 0, {"upkeep_per_second": 0.5, "wall_walk_speed": 0.5}),
    "inertial_stabilizer": ("mobility", [2, 3, 3, 4, 4], 0, 0, {"air_control": 0.25, "wall_impact_reduction": 0.20}),
    "momentum_booster": ("mobility", [3, 3, 4, 4, 5], 0, 0, {"dash_blocks": 6.0, "buff_ticks": 40, "sprint_gain_per_second": 8.0, "decay_per_second": 5.0}),
}

# The Core is the only type whose numbers differ per tier already (docs/design/01-core-systems.md, section 1.2).
CORE_TIERS = {
    "capacity": [30, 45, 60, 80, 100],
    "overload_margin": [0.10, 0.10, 0.15, 0.15, 0.20],
    "battery": [100, 200, 350, 500, 750],
    "recharge_per_second": [0.25, 0.4, 0.6, 0.8, 1.0],
}

NAMES = {
    "cybernetic_core": "Cybernetic Core", "sandevistan": "Sandevistan", "reflex_booster": "Reflex Booster",
    "combat_predictor": "Combat Predictor", "automatic_evasion_system": "Automatic Evasion System",
    "kerenzikov": "Kerenzikov", "deck_chassis": "Cyberdeck Chassis", "deck_processor": "Deck Processor",
    "deck_ram": "Deck RAM", "deck_storage": "Deck Storage", "reinforced_skeleton": "Reinforced Skeleton",
    "power_actuators": "Power Actuators", "kinetic_tendons": "Kinetic Tendons", "micro_actuators": "Micro-Actuators",
    "impact_dampeners": "Impact Dampeners", "inertia_compensation": "Inertia Compensation",
    "cybernetic_arm": "Cybernetic Arm", "remote_manipulator": "Remote Manipulator",
    "retractable_blade": "Retractable Blade", "microgrip": "Microgrip System", "ballistic_module": "Ballistic Module",
    "entity_scanner": "Entity Scanner", "thermal_vision": "Thermal Vision", "machine_analyzer": "Machine Analyzer",
    "ballistic_visor": "Ballistic Visor", "optical_camouflage": "Optical Camouflage",
    "target_tracking": "Target Tracking", "biomonitor": "Biomonitor", "regenerator": "Regenerator",
    "adrenaline_pump": "Adrenaline Pump", "emergency_defibrillator": "Emergency Defibrillator",
    "energy_buffer": "Energy Buffer", "emergency_response_system": "Emergency Response System",
    "armor_plating": "Armor Plating", "nanocoating": "Nanocoating", "shock_circuit": "Shock Circuit",
    "kinetic_shield": "Kinetic Shield", "emergency_shell": "Emergency Shell", "enhanced_tendons": "Enhanced Tendons",
    "hydraulic_legs": "Hydraulic Legs", "silent_actuators": "Silent Actuators", "magnetic_soles": "Magnetic Soles",
    "inertial_stabilizer": "Inertial Stabilizer", "momentum_booster": "Momentum Booster",
}

DESCRIPTIONS = {
    "cybernetic_core": "The spinal bus that hosts all other hardware. Sets Neural Capacity and the battery.",
    "sandevistan": "Casts a stationary temporal field that slows everything inside it.",
    "reflex_booster": "Faster attacks, bow draw, eating and drinking.",
    "combat_predictor": "Shows a lead marker while you aim a ranged weapon.",
    "automatic_evasion_system": "Sidesteps incoming arrows on its own.",
    "kerenzikov": "A dodge key that opens a short reaction window.",
    "deck_chassis": "The spine-mounted body of a cyberdeck. Needs a Processor, RAM and Storage.",
    "deck_processor": "Runs Synapse programs. Tier I speaks ISA-1.",
    "deck_ram": "Memory for the running program.",
    "deck_storage": "Stores your programs.",
    "reinforced_skeleton": "Flat damage reduction against blunt hits; resists machinery.",
    "power_actuators": "Stronger melee; full-charge hits become Power Strikes.",
    "kinetic_tendons": "Higher ground jumps.",
    "micro_actuators": "Faster sprinting.",
    "impact_dampeners": "Survive longer falls.",
    "inertia_compensation": "Resist being shoved around.",
    "cybernetic_arm": "A mechanical arm: faster mining and one module socket.",
    "remote_manipulator": "Use levers, buttons and doors from a distance.",
    "retractable_blade": "A forearm blade for the empty hand.",
    "microgrip": "Hold Sneak against a wall to cling and climb.",
    "ballistic_module": "A forearm launcher firing flechettes.",
    "entity_scanner": "A pulse that outlines nearby creatures through walls.",
    "thermal_vision": "See warm-blooded creatures through walls.",
    "machine_analyzer": "Works as Engineer's Goggles without a helmet slot.",
    "ballistic_visor": "Draws the arc of incoming arrows.",
    "optical_camouflage": "Temporary concealment; mobs lose track of you.",
    "target_tracking": "Tags one creature and keeps a marker on it.",
    "biomonitor": "Exact vitals and the source of health events for decks.",
    "regenerator": "Heals you out of combat using Charge instead of food.",
    "adrenaline_pump": "A big hit triggers a burst of speed, then a crash.",
    "emergency_defibrillator": "Restarts your heart once per long cooldown.",
    "energy_buffer": "A reserve pool for emergency activations only.",
    "emergency_response_system": "Fires your defensive implants when health runs low.",
    "armor_plating": "Armor points under the skin.",
    "nanocoating": "A small cut against every damage family.",
    "shock_circuit": "Melee attackers take an electric discharge.",
    "kinetic_shield": "A regenerating absorb pool.",
    "emergency_shell": "Hardens at critical health for a few seconds.",
    "enhanced_tendons": "Jump again in mid-air.",
    "hydraulic_legs": "Hold Sneak and Jump to charge a directional super-leap.",
    "silent_actuators": "Your footsteps stop making noise.",
    "magnetic_soles": "Walk up metal walls while toggled on.",
    "inertial_stabilizer": "Better steering in the air; survive wall impacts.",
    "momentum_booster": "Sprinting fills a gauge; release it as a dash.",
}

CATEGORY_COLOR = {
    "core": (0xE0, 0xE0, 0xE8), "neural": (0x9B, 0x6B, 0xFF), "cyberdeck": (0x3A, 0xC8, 0x9A),
    "skeletal": (0xC8, 0xB4, 0x8A), "limbs": (0xE0, 0x82, 0x3C), "optical": (0x3A, 0xB4, 0xE8),
    "circulatory": (0xE8, 0x48, 0x58), "subdermal": (0x7A, 0x8C, 0xA0), "mobility": (0xE8, 0xD0, 0x3C),
}

# Placeholder recipe ingredients. Pattern is ABA / CXC / ADA with a signature centrepiece X per implant.
CATEGORY_INGREDIENTS = {
    "core": ("create:iron_sheet", "create:andesite_casing", "create:copper_sheet", "create:precision_mechanism"),
    "neural": ("create:copper_sheet", "create:andesite_casing", "minecraft:redstone", "create:electron_tube"),
    "cyberdeck": ("create:copper_sheet", "create:andesite_casing", "minecraft:redstone", "create:precision_mechanism"),
    "skeletal": ("create:iron_sheet", "create:andesite_casing", "minecraft:bone_block", "create:cogwheel"),
    "limbs": ("create:iron_sheet", "create:andesite_casing", "create:cogwheel", "create:shaft"),
    "optical": ("create:copper_sheet", "create:andesite_casing", "minecraft:glass_pane", "create:electron_tube"),
    "circulatory": ("create:copper_sheet", "create:andesite_casing", "minecraft:string", "minecraft:redstone"),
    "subdermal": ("create:iron_sheet", "create:andesite_casing", "create:iron_sheet", "minecraft:string"),
    "mobility": ("create:iron_sheet", "create:andesite_casing", "minecraft:string", "minecraft:piston"),
}
SIGNATURE = {
    "cybernetic_core": "minecraft:redstone_block", "sandevistan": "minecraft:clock", "reflex_booster": "minecraft:feather",
    "combat_predictor": "minecraft:compass", "automatic_evasion_system": "minecraft:shield",
    "kerenzikov": "minecraft:rabbit_foot", "deck_chassis": "minecraft:repeater", "deck_processor": "minecraft:comparator",
    "deck_ram": "minecraft:redstone_lamp", "deck_storage": "minecraft:chest", "reinforced_skeleton": "minecraft:iron_bars",
    "power_actuators": "minecraft:iron_block", "kinetic_tendons": "minecraft:slime_ball", "micro_actuators": "minecraft:leather",
    "impact_dampeners": "minecraft:slime_block", "inertia_compensation": "minecraft:honey_bottle",
    "cybernetic_arm": "minecraft:lever", "remote_manipulator": "minecraft:tripwire_hook", "retractable_blade": "minecraft:iron_sword",
    "microgrip": "minecraft:chain", "ballistic_module": "minecraft:crossbow", "entity_scanner": "minecraft:spyglass",
    "thermal_vision": "minecraft:blaze_powder", "machine_analyzer": "create:goggles", "ballistic_visor": "minecraft:arrow",
    "optical_camouflage": "minecraft:fermented_spider_eye", "target_tracking": "minecraft:ender_eye",
    "biomonitor": "minecraft:glistering_melon_slice", "regenerator": "minecraft:golden_apple",
    "adrenaline_pump": "minecraft:sweet_berries", "emergency_defibrillator": "minecraft:gold_block",
    "energy_buffer": "minecraft:amethyst_shard", "emergency_response_system": "minecraft:observer",
    "armor_plating": "minecraft:iron_chestplate", "nanocoating": "minecraft:lapis_block", "shock_circuit": "minecraft:lightning_rod",
    "kinetic_shield": "minecraft:diamond", "emergency_shell": "minecraft:armadillo_scute", "enhanced_tendons": "minecraft:rabbit_hide",
    "hydraulic_legs": "minecraft:sticky_piston", "silent_actuators": "minecraft:white_wool", "magnetic_soles": "minecraft:iron_boots",
    "inertial_stabilizer": "minecraft:phantom_membrane", "momentum_booster": "minecraft:blaze_rod",
}

FONT = {
    "A": [".#.", "#.#", "###", "#.#", "#.#"], "B": ["##.", "#.#", "##.", "#.#", "##."], "C": [".##", "#..", "#..", "#..", ".##"],
    "D": ["##.", "#.#", "#.#", "#.#", "##."], "E": ["###", "#..", "##.", "#..", "###"], "F": ["###", "#..", "##.", "#..", "#.."],
    "G": [".##", "#..", "#.#", "#.#", ".##"], "H": ["#.#", "#.#", "###", "#.#", "#.#"], "I": ["###", ".#.", ".#.", ".#.", "###"],
    "J": ["..#", "..#", "..#", "#.#", ".#."], "K": ["#.#", "#.#", "##.", "#.#", "#.#"], "L": ["#..", "#..", "#..", "#..", "###"],
    "M": ["#.#", "###", "###", "#.#", "#.#"], "N": ["##.", "#.#", "#.#", "#.#", "#.#"], "O": [".#.", "#.#", "#.#", "#.#", ".#."],
    "P": ["##.", "#.#", "##.", "#..", "#.."], "Q": [".#.", "#.#", "#.#", "##.", ".##"], "R": ["##.", "#.#", "##.", "#.#", "#.#"],
    "S": [".##", "#..", ".#.", "..#", "##."], "T": ["###", ".#.", ".#.", ".#.", ".#."], "U": ["#.#", "#.#", "#.#", "#.#", "###"],
    "V": ["#.#", "#.#", "#.#", "#.#", ".#."], "W": ["#.#", "#.#", "###", "###", "#.#"], "X": ["#.#", "#.#", ".#.", "#.#", "#.#"],
    "Y": ["#.#", "#.#", ".#.", ".#.", ".#."], "Z": ["###", "..#", ".#.", "#..", "###"],
}


def write(path, text):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        f.write(text)


def write_json(path, obj):
    write(path, json.dumps(obj, indent=2) + "\n")


def png(width, height, pixels):
    """pixels: list of rows of (r, g, b, a). Minimal PNG writer."""
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in pixels)

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def item_texture(name, category):
    base = CATEGORY_COLOR[category]
    dark = tuple(int(c * 0.45) for c in base)
    light = tuple(min(255, int(c * 1.15) + 20) for c in base)
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for y in range(1, 15):
        for x in range(1, 15):
            edge = x in (1, 14) or y in (1, 14)
            px[y][x] = dark + (255,) if edge else base + (255,)
    for x in range(2, 14):
        px[2][x] = light + (255,)
    words = [w for w in name.replace("-", " ").split() if w]
    letters = (words[0][0] + (words[1][0] if len(words) > 1 else words[0][1])).upper()
    for li, ch in enumerate(letters):
        glyph = FONT.get(ch, FONT["X"])
        for gy, row in enumerate(glyph):
            for gx, cell in enumerate(row):
                if cell == "#":
                    px[5 + gy][4 + li * 4 + gx] = (16, 20, 28, 255)
    return png(16, 16, px)


def block_texture(kind):
    colors = {"rig": ((0x70, 0x88, 0xA0), (0xC8, 0xE0, 0xF0)), "charger": ((0x4A, 0x5C, 0x70), (0x35, 0xD0, 0xFF)),
              "port": ((0x3A, 0x48, 0x58), (0x3A, 0xC8, 0x9A))}[kind]
    base, accent = colors
    px = [[base + (255,) for _ in range(16)] for _ in range(16)]
    for i in range(16):
        for edge in (0, 15):
            px[edge][i] = tuple(int(c * 0.6) for c in base) + (255,)
            px[i][edge] = tuple(int(c * 0.6) for c in base) + (255,)
    for y in range(6, 10):
        for x in range(6, 10):
            px[y][x] = accent + (255,)
    return png(16, 16, px)


def nbt_string(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def empty_structure_nbt(size=(9, 8, 9)):
    """A structure template with no blocks, for GameTests. Uncompressed NBT then gzip."""
    out = b"\x0a" + nbt_string("")  # root compound
    out += b"\x03" + nbt_string("DataVersion") + struct.pack(">i", 3955)
    out += b"\x09" + nbt_string("size") + b"\x03" + struct.pack(">i", 3) + b"".join(struct.pack(">i", n) for n in size)
    out += b"\x09" + nbt_string("blocks") + b"\x0a" + struct.pack(">i", 0)
    out += b"\x09" + nbt_string("entities") + b"\x0a" + struct.pack(">i", 0)
    out += b"\x09" + nbt_string("palette") + b"\x0a" + struct.pack(">i", 0)
    out += b"\x00"
    import gzip
    return gzip.compress(out, 9, mtime=0)


def main():
    # ---- stats
    for ident, (category, loads, charge, cooldown, values) in IMPLANTS.items():
        tiers = []
        for t in range(5):
            v = dict(values)
            if ident == "cybernetic_core":
                v = {k: arr[t] for k, arr in CORE_TIERS.items()}
            tiers.append({"load": loads[t], "charge_cost": charge, "cooldown_ticks": cooldown, "values": v, "unlocks": []})
        write_json("data/%s/cyberimplants/implant_stats/%s.json" % (MOD, ident), {"tiers": tiers, "unlock_layers": {}})

    # ---- item models and textures
    for ident, (category, *_rest) in IMPLANTS.items():
        write_json("assets/%s/models/item/%s.json" % (MOD, ident),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:item/%s" % (MOD, ident)}})
        path = os.path.join(ROOT, "assets", MOD, "textures", "item", ident + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(item_texture(NAMES[ident], category))

    # flechette
    write_json("assets/%s/models/item/flechette.json" % MOD,
               {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:item/flechette" % MOD}})
    with open(os.path.join(ROOT, "assets", MOD, "textures", "item", "flechette.png"), "wb") as f:
        f.write(item_texture("Flechette Ammo", "limbs"))

    # ---- blocks
    for name, kind in (("implantation_rig", "rig"), ("kinetic_charger", "charger"), ("neural_port", "port")):
        tex = os.path.join(ROOT, "assets", MOD, "textures", "block", name + ".png")
        os.makedirs(os.path.dirname(tex), exist_ok=True)
        with open(tex, "wb") as f:
            f.write(block_texture(kind))
    write_json("assets/%s/models/block/implantation_rig.json" % MOD,
               {"parent": "minecraft:block/cube_all", "textures": {"all": "%s:block/implantation_rig" % MOD}})
    write_json("assets/%s/models/block/kinetic_charger.json" % MOD,
               {"parent": "minecraft:block/slab", "textures": {k: "%s:block/kinetic_charger" % MOD for k in ("bottom", "top", "side")}})
    # Neural Port: a small plug on the north side of its cell; the blockstate rotates it onto the face it is attached to.
    write_json("assets/%s/models/block/neural_port.json" % MOD, {
        "textures": {"particle": "%s:block/neural_port" % MOD, "all": "%s:block/neural_port" % MOD},
        "elements": [{"from": [5, 5, 0], "to": [11, 11, 5],
                      "faces": {d: {"uv": [0, 0, 16, 16], "texture": "#all"} for d in ("north", "south", "east", "west", "up", "down")}}]})
    write_json("assets/%s/models/item/neural_port.json" % MOD, {"parent": "%s:block/neural_port" % MOD})
    port_model = "%s:block/neural_port" % MOD
    write_json("assets/%s/blockstates/neural_port.json" % MOD, {"variants": {
        "facing=north": {"model": port_model},
        "facing=east": {"model": port_model, "y": 90},
        "facing=south": {"model": port_model, "y": 180},
        "facing=west": {"model": port_model, "y": 270},
        "facing=down": {"model": port_model, "x": 90},
        "facing=up": {"model": port_model, "x": 270}}})
    write_json("assets/%s/models/item/implantation_rig.json" % MOD, {"parent": "%s:block/implantation_rig" % MOD})
    write_json("assets/%s/models/item/kinetic_charger.json" % MOD, {"parent": "%s:block/kinetic_charger" % MOD})
    write_json("assets/%s/blockstates/implantation_rig.json" % MOD, {"variants": {
        "facing=north": {"model": "%s:block/implantation_rig" % MOD},
        "facing=east": {"model": "%s:block/implantation_rig" % MOD, "y": 90},
        "facing=south": {"model": "%s:block/implantation_rig" % MOD, "y": 180},
        "facing=west": {"model": "%s:block/implantation_rig" % MOD, "y": 270}}})
    write_json("assets/%s/blockstates/kinetic_charger.json" % MOD, {"variants": {
        "axis=y": {"model": "%s:block/kinetic_charger" % MOD},
        "axis=x": {"model": "%s:block/kinetic_charger" % MOD},
        "axis=z": {"model": "%s:block/kinetic_charger" % MOD}}})
    for name in ("implantation_rig", "kinetic_charger", "neural_port"):
        write_json("data/%s/loot_table/blocks/%s.json" % (MOD, name), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                       "entries": [{"type": "minecraft:item", "name": "%s:%s" % (MOD, name)}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write_json("data/minecraft/tags/block/mineable/pickaxe.json",
               {"replace": False, "values": ["%s:implantation_rig" % MOD, "%s:kinetic_charger" % MOD, "%s:neural_port" % MOD]})

    # ---- recipes
    for ident, (category, *_rest) in IMPLANTS.items():
        a, b, c, d = CATEGORY_INGREDIENTS[category]
        write_json("data/%s/recipe/%s.json" % (MOD, ident), {
            "type": "create:mechanical_crafting",
            "accept_mirrored": False,
            "category": "misc",
            "key": {"A": {"item": a}, "B": {"item": b}, "C": {"item": c}, "D": {"item": d}, "X": {"item": SIGNATURE[ident]}},
            "pattern": ["ABA", "CXC", "ADA"],
            "result": {"id": "%s:%s" % (MOD, ident), "count": 1},
            "show_notification": False})
    write_json("data/%s/recipe/implantation_rig.json" % MOD, {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"A": {"item": "create:andesite_casing"}, "I": {"item": "create:iron_sheet"}, "M": {"item": "create:precision_mechanism"},
                "S": {"item": "create:shaft"}, "C": {"item": "create:cogwheel"}},
        "pattern": ["IMI", "SAC", "III"], "result": {"id": "%s:implantation_rig" % MOD, "count": 1}})
    write_json("data/%s/recipe/kinetic_charger.json" % MOD, {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"A": {"item": "create:andesite_casing"}, "R": {"item": "minecraft:redstone_block"}, "C": {"item": "create:copper_sheet"},
                "S": {"item": "create:shaft"}},
        "pattern": ["CRC", "SAS", "CCC"], "result": {"id": "%s:kinetic_charger" % MOD, "count": 1}})
    write_json("data/%s/recipe/neural_port.json" % MOD, {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"T": {"item": "create:electron_tube"}, "C": {"item": "create:copper_sheet"}, "A": {"item": "create:andesite_casing"},
                "R": {"item": "minecraft:redstone"}},
        "pattern": [" T ", "CAC", " R "], "result": {"id": "%s:neural_port" % MOD, "count": 1}})
    write_json("data/%s/recipe/flechette.json" % MOD, {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "minecraft:iron_nugget"}, {"item": "minecraft:iron_nugget"}, {"item": "minecraft:flint"}],
        "result": {"id": "%s:flechette" % MOD, "count": 4}})

    # ---- game test template
    path = os.path.join(ROOT, "data", MOD, "structure", "empty.nbt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(empty_structure_nbt())

    # ---- language
    lang_path = os.path.join(ROOT, "assets", MOD, "lang", "en_us.json")
    with open(lang_path, encoding="utf-8") as f:
        lang = json.load(f)
    lang.update(LANG_STATIC)
    for ident, name in NAMES.items():
        lang["item.%s.%s" % (MOD, ident)] = name
        lang["item.%s.%s.desc" % (MOD, ident)] = DESCRIPTIONS[ident]
    with open(lang_path, "w", encoding="utf-8") as f:
        json.dump(lang, f, indent=2, ensure_ascii=False)
        f.write("\n")


LANG_STATIC = {
    "itemGroup.cyberimplants": "Create: Cybernetics",
    "item.cyberimplants.flechette": "Flechette",
    "block.cyberimplants.implantation_rig": "Implantation Rig",
    "block.cyberimplants.kinetic_charger": "Kinetic Charger",
    "block.cyberimplants.neural_port": "Neural Port",
    "cyberimplants.port.bound": "Bound port '%s' to your deck.",
    "cyberimplants.port.unbound": "Unbound port '%s'.",
    "cyberimplants.port.full": "Your deck can bind %s ports.",
    "cyberimplants.port.name_taken": "A port named '%s' is already bound. Rename this one in an anvil.",
    "cyberimplants.port.not_owner": "This port belongs to someone else.",
    "cyberimplants.region.deck": "Deck module",
    "cyberimplants.region.left_socket": "Left arm socket",
    "cyberimplants.region.right_socket": "Right arm socket",
    "cyberimplants.category.core": "Core",
    "cyberimplants.activation.passive": "Passive",
    "cyberimplants.activation.toggle": "Toggle",
    "cyberimplants.activation.active": "Active",
    "cyberimplants.activation.input_bound": "Input-bound",
    "cyberimplants.activation.triggered": "Triggered",
    "cyberimplants.tooltip.tier": "Mk. %s (%s)",
    "cyberimplants.tooltip.slot": "Category: %s",
    "cyberimplants.tooltip.activation": "Use: %s",
    "cyberimplants.tooltip.load": "Neural Load: %s",
    "cyberimplants.use.ok": "OK",
    "cyberimplants.use.no_charge": "Not enough Charge",
    "cyberimplants.use.cooldown": "Not ready",
    "cyberimplants.use.busy": "Already running",
    "cyberimplants.use.range": "Out of range",
    "cyberimplants.use.no_los": "No target in sight",
    "cyberimplants.use.denied": "Not possible right now",
    "cyberimplants.use.missing": "Not installed",
    "cyberimplants.rig.title": "Implantation Rig",
    "cyberimplants.rig.no_power": "The Rig needs rotation to operate.",
    "cyberimplants.rig.empty": "empty",
    "cyberimplants.rig.inventory": "Implants in your inventory",
    "cyberimplants.rig.load": "Neural Load %s / %s (rejected above %s)",
    "cyberimplants.rig.unknown_implant": "That is not an implant.",
    "cyberimplants.rig.wrong_slot": "It does not fit there.",
    "cyberimplants.rig.occupied": "That slot is taken.",
    "cyberimplants.rig.no_core": "Install a Cybernetic Core first.",
    "cyberimplants.rig.duplicate": "That region already holds one.",
    "cyberimplants.rig.no_arm_frame": "Install a Cybernetic Arm in that arm first.",
    "cyberimplants.rig.no_chassis": "Install a Cyberdeck Chassis first.",
    "cyberimplants.rig.host_tier": "Its tier is higher than its host's.",
    "cyberimplants.rig.overload": "Neural Load too high; the Rig refuses the operation.",
    "cyberimplants.rig.empty_slot": "Nothing is installed there.",
    "cyberimplants.rig.has_dependents": "Remove what is attached to it first.",
    "cyberimplants.wheel.title": "Implants",
    "cyberimplants.wheel.on": "ON",
    "cyberimplants.wheel.ready": "ready",
    "cyberimplants.hud.scan": "SCAN",
    "cyberimplants.hud.magazine": "Magazine: %s",
    "cyberimplants.hydraulics.pressure": "Pressure %s%%",
    "cyberimplants.microgrip.exhausted": "Grip exhausted",
    "cyberimplants.ballistic.no_ammo": "No flechettes to load.",
    "cyberimplants.ballistic.reloading": "Reloading...",
    "cyberimplants.deck.title": "Cyberdeck",
    "cyberimplants.deck.placeholder": "Write a Synapse program",
    "cyberimplants.deck.save": "Save",
    "cyberimplants.deck.load": "Load",
    "cyberimplants.deck.unload": "Unload",
    "cyberimplants.deck.delete": "Delete",
    "cyberimplants.deck.new": "+ New program",
    "cyberimplants.deck.waiting": "Waiting for the deck...",
    "cyberimplants.deck.not_booted": "No bootable deck: needs a chassis, Processor, RAM and Storage.",
    "cyberimplants.deck.hardware": "ISA-%s | RAM %s cells | Storage %s / %s bytes",
    "cyberimplants.deck.running": "Running: %s (%s cells)",
    "key.categories.cyberimplants": "Create: Cybernetics",
    "key.cyberimplants.wheel": "Implant Wheel (hold)",
    "key.cyberimplants.dodge": "Dodge (Kerenzikov)",
    "key.cyberimplants.fire_ballistic": "Fire Ballistic Module",
    "key.cyberimplants.deck": "Open Cyberdeck",
    "key.cyberimplants.macro_1": "Deck Macro Key 1",
    "key.cyberimplants.macro_2": "Deck Macro Key 2",
    "key.cyberimplants.macro_3": "Deck Macro Key 3",
    "key.cyberimplants.macro_4": "Deck Macro Key 4",
}

if __name__ == "__main__":
    main()
