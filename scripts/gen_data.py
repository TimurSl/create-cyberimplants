#!/usr/bin/env python3
"""Generates the mod's data files: implant stats, recipes, item tags, language keys, models and textures.

Sources: implants.py (catalogue), layer2_tiers.py (the numeric tier rows), unlocks.py (tier-gated features),
crafting.py (components, fluids, basic Tier I recipes and the grade upgrades), gen_textures.py (icons) and
ui_lang.py (interface strings, run last so they win).

Run from the repository root:  python3 scripts/gen_data.py
"""
import gzip
import json
import os
import shutil
import struct

import crafting
import gen_textures
import ui_lang
import unlocks
from implants import CORE_TIERS, DESCRIPTIONS, IMPLANTS, NAMES, SIGNATURE
from layer2_tiers import TIERS, per_tier

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
MOD = "cyberimplants"
LANG_DIR = os.path.join(ROOT, "assets", MOD, "lang")


def write(path, text):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        f.write(text)


def write_json(path, obj):
    write(path, json.dumps(obj, indent=2, ensure_ascii=False) + "\n")


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
    return gzip.compress(out, 9, mtime=0)


def generated_model(name):
    write_json("assets/%s/models/item/%s.json" % (MOD, name),
               {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:item/%s" % (MOD, name)}})


# ---------------------------------------------------------------- stats
def write_stats():
    for ident, (category, loads, charge, cooldown, values) in IMPLANTS.items():
        if ident in TIERS:
            loads, charge, cooldown, values = TIERS[ident]
        tiers = []
        for t in range(5):
            v = {k: per_tier(x, t) for k, x in values.items()}
            if ident == "cybernetic_core":
                v = {k: arr[t] for k, arr in CORE_TIERS.items()}
            tiers.append({"load": loads[t], "charge_cost": per_tier(charge, t), "cooldown_ticks": per_tier(cooldown, t),
                          "values": v, "unlocks": unlocks.unlocks_for(ident, t + 1)})
        write_json("data/%s/cyberimplants/implant_stats/%s.json" % (MOD, ident),
                   {"tiers": tiers, "unlock_layers": unlocks.unlock_layers(ident)})


# ---------------------------------------------------------------- recipes and tags
def write_recipes():
    recipe_dir = os.path.join(ROOT, "data", MOD, "recipe")
    shutil.rmtree(recipe_dir, ignore_errors=True)  # stale placeholder recipes must not survive a rename
    for ident, (category, *_rest) in IMPLANTS.items():
        write_json("data/%s/recipe/%s.json" % (MOD, ident), crafting.tier1_recipe(ident, category, SIGNATURE[ident]))
        for tier in range(2, 6):
            write_json("data/%s/recipe/upgrade/%s_t%d.json" % (MOD, ident, tier), crafting.upgrade_recipe(ident, category, tier))
    for path, recipe in {**crafting.component_recipes(), **crafting.machine_recipes()}.items():
        write_json("data/%s/recipe/%s.json" % (MOD, path), recipe)
    for name, values in crafting.TAGS.items():
        write_json("data/%s/tags/item/%s.json" % (MOD, name), {"replace": False, "values": values})


# ---------------------------------------------------------------- assets
def write_assets():
    for ident in IMPLANTS:
        generated_model(ident)
    for extra in ("incomplete_implant", "neuro_stabilizer", "field_manual", "flechette"):
        generated_model(extra)
    for name in list(crafting.COMPONENTS) + list(crafting.INCOMPLETE):
        generated_model(name)

    # process fluids: a single block model keeps the particle sprite; the fluid renders through the client extension
    for f in crafting.FLUIDS:
        generated_model(f + "_bucket")
        write_json("assets/%s/blockstates/%s.json" % (MOD, f), {"variants": {"": {"model": "%s:block/%s" % (MOD, f)}}})
        write_json("assets/%s/models/block/%s.json" % (MOD, f), {"textures": {"particle": "%s:block/%s_still" % (MOD, f)}})

    # Neural Port: a small plug on the north side of its cell; the blockstate rotates it onto the face it is attached to.
    write_json("assets/%s/models/block/neural_port.json" % MOD, {
        "textures": {"particle": "%s:block/neural_port" % MOD, "all": "%s:block/neural_port" % MOD},
        "elements": [{"from": [5, 5, 0], "to": [11, 11, 5],
                      "faces": {d: {"uv": [0, 0, 16, 16], "texture": "#all"} for d in ("north", "south", "east", "west", "up", "down")}}]})
    port_model = "%s:block/neural_port" % MOD
    write_json("assets/%s/blockstates/neural_port.json" % MOD, {"variants": {
        "facing=north": {"model": port_model},
        "facing=east": {"model": port_model, "y": 90},
        "facing=south": {"model": port_model, "y": 180},
        "facing=west": {"model": port_model, "y": 270},
        "facing=down": {"model": port_model, "x": 90},
        "facing=up": {"model": port_model, "x": 270}}})
    write_json("assets/%s/models/block/proximity_sensor.json" % MOD,
               {"parent": "minecraft:block/cube_all", "textures": {"all": "%s:block/proximity_sensor" % MOD}})
    write_json("assets/%s/blockstates/proximity_sensor.json" % MOD, {"variants": {"": {"model": "%s:block/proximity_sensor" % MOD}}})
    rig = "%s:block/implantation_rig" % MOD
    write_json("assets/%s/blockstates/implantation_rig.json" % MOD, {"variants": {
        "facing=north": {"model": rig}, "facing=east": {"model": rig, "y": 90},
        "facing=south": {"model": rig, "y": 180}, "facing=west": {"model": rig, "y": 270}}})
    charger = "%s:block/kinetic_charger" % MOD
    write_json("assets/%s/blockstates/kinetic_charger.json" % MOD, {"variants": {
        "axis=y": {"model": charger}, "axis=x": {"model": charger}, "axis=z": {"model": charger}}})
    etcher = "%s:block/precision_etcher" % MOD
    write_json("assets/%s/blockstates/precision_etcher.json" % MOD, {"variants": {
        "facing=north": {"model": etcher}, "facing=east": {"model": etcher, "y": 90},
        "facing=south": {"model": etcher, "y": 180}, "facing=west": {"model": etcher, "y": 270}}})
    blocks = ("implantation_rig", "kinetic_charger", "precision_etcher", "neural_port", "proximity_sensor")
    for name in blocks:
        write_json("assets/%s/models/item/%s.json" % (MOD, name), {"parent": "%s:block/%s" % (MOD, name)})
        write_json("data/%s/loot_table/blocks/%s.json" % (MOD, name), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                       "entries": [{"type": "minecraft:item", "name": "%s:%s" % (MOD, name)}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write_json("data/minecraft/tags/block/mineable/pickaxe.json",
               {"replace": False, "values": ["%s:%s" % (MOD, b) for b in blocks]})

    # game test template
    path = os.path.join(ROOT, "data", MOD, "structure", "empty.nbt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(empty_structure_nbt())


# ---------------------------------------------------------------- language
def generated_lang(index):
    """Keys this generator owns in both languages (index 0 English, 1 Russian)."""
    out = dict(unlocks.lang(index))
    out["cyberimplants.tooltip.unlocks"] = ("Unlocked at this tier:", "Открыто на этом тире:")[index]
    for ident, row in crafting.COMPONENTS.items():
        out["item.%s.%s" % (MOD, ident)] = row[1 + index]
    for ident, row in crafting.INCOMPLETE.items():
        out["item.%s.%s" % (MOD, ident)] = row[1 + index]
    for f, row in crafting.FLUIDS.items():
        name = row[index]
        out["fluid_type.%s.%s" % (MOD, f)] = name
        out["block.%s.%s" % (MOD, f)] = name
        out["item.%s.%s_bucket" % (MOD, f)] = ("%s Bucket" % name, row[2])[index]
    return out


def write_lang():
    for lang, index in (("en_us", 0), ("ru_ru", 1)):
        path = os.path.join(LANG_DIR, lang + ".json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        if index == 0:
            data.update(LANG_STATIC)
            for ident, name in NAMES.items():
                data["item.%s.%s" % (MOD, ident)] = name
                data["item.%s.%s.desc" % (MOD, ident)] = DESCRIPTIONS[ident]
        data.update(generated_lang(index))
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")


def main():
    gen_textures.main()
    write_stats()
    write_recipes()
    write_assets()
    write_lang()
    ui_lang.main()


LANG_STATIC = {
    "itemGroup.cyberimplants": "Create: Cybernetics",
    "item.cyberimplants.flechette": "Flechette",
    "item.cyberimplants.field_manual": "Field Manual",
    "item.cyberimplants.incomplete_implant": "Incomplete Implant",
    "item.cyberimplants.neuro_stabilizer": "Neuro-Stabilizer",
    "cyberimplants.rig.locked": "A redstone signal has locked the chair.",
    "cyberimplants.use.offline": "Offline (neural glitch)",
    "cyberimplants.rig.low_rpm": "The Rig needs at least %s RPM for that.",
    "cyberimplants.rig.no_gel": "The Rig needs %s mB of %s.",
    "cyberimplants.rig.busy": "The Rig is already operating.",
    "cyberimplants.rig.progress": "Operating... %s%%",
    "cyberimplants.rig.done": "Operation complete.",
    "cyberimplants.rig.abort.rpm": "Operation aborted: the Rig slowed down.",
    "cyberimplants.rig.abort.gel": "Operation aborted: out of gel.",
    "cyberimplants.rig.abort.moved": "Operation aborted: the patient moved.",
    "cyberimplants.rig.abort.invalid": "Operation aborted: the plan changed.",
    "cyberimplants.rig.no_socket": "That host has no free socket.",
    "cyberimplants.rig.too_many": "The chassis cannot host another one.",
    "cyberimplants.strain.glitch": "Neural glitch: %s went offline.",
    "cyberimplants.strain.static": "Neural static...",
    "cyberimplants.load.stable": "Stable",
    "cyberimplants.load.strained": "Strained",
    "cyberimplants.sensor.filter": "Proximity filter: %s",
    "cyberimplants.sensor.size": "Detection box: %s blocks",
    "block.cyberimplants.proximity_sensor": "Proximity Sensor",
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
    "cyberimplants.deck.tab.programs": "Programs",
    "cyberimplants.deck.tab.processes": "Processes",
    "cyberimplants.deck.tab.log": "Log",
    "cyberimplants.deck.thread": "Thread %s",
    "cyberimplants.deck.threads": "Threads: %s | neural latency: %s ticks",
    "cyberimplants.deck.idle": "No program is running.",
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
