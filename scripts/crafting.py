"""The real production chain (docs/design/06-progression-and-crafting.md): shared components, the process fluids, the
basic Tier I recipes and the Tier II-V grade upgrades, plus the item tags modpacks use to swap rare inputs.

Tier I ("basic") implants are shaped crafting-table recipes from andesite-age parts (press, saw, unheated mixer,
deployer), so the first implant comes right after the andesite age (section 6.5). Every grade above is a sequenced
assembly on the implant of the previous grade, built from a shared template per tier plus a category part. The
Precision Etcher is not built yet: its "etch" steps run on a Mechanical Saw (create:cutting) for now.

Everything here is data; gen_data.py writes it.
"""
MOD = "cyberimplants"


def m(path):
    return "%s:%s" % (MOD, path)


def ing(x):
    """'#tag' or 'ns:item' -> a recipe ingredient."""
    return {"tag": x[1:]} if x.startswith("#") else {"item": x}


def fluid(f, amount):
    return {"type": "neoforge:single", "amount": amount, "fluid": f}


def res(x, count=1, chance=None):
    r = {"id": x}
    if count != 1:
        r["count"] = count
    if chance is not None:
        r["chance"] = chance
    return r


# ---------------------------------------------------------------- items
# id: (tier, english, russian, glyph, shape). Glyphs and shapes are drawn by gen_textures.py.
COMPONENTS = {
    "copper_wire": (1, "Copper Wire", "Медная проволока", "wire", "round"),
    "synthetic_fiber": (1, "Synthetic Fiber", "Синтетическое волокно", "fiber", "round"),
    "mechanical_servo": (1, "Mechanical Servo", "Механический сервопривод", "gear", "clip"),
    "civilian_casing": (1, "Civilian Casing", "Гражданский корпус", "plate", "plate"),
    "crude_steel_blend": (2, "Crude Steel Blend", "Сырая стальная смесь", "dust", "round"),
    "tempered_steel_ingot": (2, "Tempered Steel Ingot", "Слиток закалённой стали", "ingot", "round"),
    "tempered_steel_sheet": (2, "Tempered Steel Sheet", "Лист закалённой стали", "sheet", "plate"),
    "precision_servo": (2, "Precision Servo", "Прецизионный сервопривод", "gear", "chip"),
    "synthetic_muscle": (2, "Synthetic Muscle", "Синтетическая мышца", "fiber", "pill"),
    "silica_dust": (3, "Silica Dust", "Кремнезёмная пыль", "dust", "round"),
    "silicon_boule": (3, "Silicon Boule", "Кремниевая буля", "cell", "pill"),
    "silicon_wafer": (3, "Silicon Wafer", "Кремниевая пластина", "wafer", "round"),
    "logic_chip": (3, "Logic Chip", "Логический чип", "chip", "chip"),
    "myomer_bundle": (3, "Myomer Bundle", "Миомерный пучок", "fiber", "hex"),
    "inked_steel_sheet": (3, "Inked Steel Sheet", "Лист стали с токопроводящей краской", "sheet", "plate"),
    "ground_lens": (3, "Ground Lens", "Шлифованная линза", "lens", "round"),
    "phase_lens": (3, "Phase Lens", "Фазовая линза", "eye", "round"),
    "composite_plate": (4, "Composite Plate", "Композитная плита", "plate", "plate"),
    "military_processor": (4, "Military Processor", "Военный процессор", "chip", "chip"),
    "echo_lattice": (5, "Echo Lattice", "Эхо-решётка", "wave", "hex"),
    "experimental_core": (5, "Experimental Core", "Экспериментальное ядро", "brain", "chip"),
    "quantum_substrate": (5, "Quantum Substrate", "Квантовая подложка", "link", "hex"),
}

# Transitional items of the component assemblies (sequenced assembly keeps its progress on them).
INCOMPLETE = {
    "incomplete_precision_servo": ("precision_servo", "Incomplete Precision Servo", "Недоделанный прецизионный сервопривод"),
    "incomplete_logic_chip": ("logic_chip", "Incomplete Logic Chip", "Недоделанный логический чип"),
    "incomplete_composite_plate": ("composite_plate", "Incomplete Composite Plate", "Недоделанная композитная плита"),
    "incomplete_military_processor": ("military_processor", "Incomplete Military Processor", "Недоделанный военный процессор"),
    "incomplete_experimental_core": ("experimental_core", "Incomplete Experimental Core", "Недоделанное экспериментальное ядро"),
}

# ---------------------------------------------------------------- fluids (docs/design/05-create-integration.md, 5.3)
# id: (english, russian, russian bucket, neon, deep). The two gels already existed; the order matches dev.cyberimplants.registry.ModFluids.
FLUIDS = {
    "bio_interface_gel": ("Bio-Interface Gel", "Биоинтерфейсный гель", "Ведро биоинтерфейсного геля", (0x6F, 0xE0, 0xA8), (0x0E, 0x6A, 0x4A)),
    "neural_gel": ("Neural Gel", "Нейрогель", "Ведро нейрогеля", (0xB4, 0x5C, 0xFF), (0x3E, 0x12, 0x7A)),
    "conductive_ink": ("Conductive Ink", "Токопроводящая краска", "Ведро токопроводящей краски", (0xFF, 0x6A, 0x3C), (0x6A, 0x12, 0x0A)),
    "cryo_coolant": ("Cryo-Coolant", "Криохладагент", "Ведро криохладагента", (0x9C, 0xF0, 0xFF), (0x14, 0x5A, 0x8A)),
    "nanite_suspension": ("Nanite Suspension", "Нанитовая суспензия", "Ведро нанитовой суспензии", (0xD8, 0xDC, 0xE8), (0x3A, 0x40, 0x56)),
}

BIO, NEURAL, INK, COOLANT, NANITES = (m(f) for f in ("bio_interface_gel", "neural_gel", "conductive_ink", "cryo_coolant",
                                                       "nanite_suspension"))

# ---------------------------------------------------------------- item tags
# Rare or biome-bound inputs go through tags so a modpack can swap them (a skyblock pack without Trial Chambers, say).
EXOTIC_TAG = "#%s:exotics/%%s" % MOD
TAGS = {
    "fiber_fillers": ["minecraft:dried_kelp", "minecraft:paper"],
    "cryo_ice": ["minecraft:packed_ice", "minecraft:blue_ice", "minecraft:snow_block"],
    "echo_sources": ["minecraft:echo_shard"],
    "composite_reinforcement": ["minecraft:netherite_scrap"],
    "magnetic_items": [{"id": "#c:ingots/iron", "required": False}, {"id": "#c:nuggets/iron", "required": False},
                       {"id": "#c:raw_materials/iron", "required": False}, {"id": "#c:plates/iron", "required": False},
                       {"id": "#c:storage_blocks/iron", "required": False}, "minecraft:iron_ore", "minecraft:deepslate_iron_ore"],
    # Tier V category exotics (section 6.4), one per category, plus the two implant-specific ones.
    "exotics/core": ["minecraft:netherite_ingot"],
    "exotics/neural": [m("echo_lattice")],
    "exotics/cyberdeck": [m("quantum_substrate")],
    "exotics/skeletal": ["minecraft:netherite_ingot"],
    "exotics/limbs": ["minecraft:breeze_rod"],
    "exotics/optical": [m("phase_lens")],
    "exotics/circulatory": ["minecraft:ghast_tear"],
    "exotics/subdermal": ["minecraft:shulker_shell"],
    "exotics/mobility": ["minecraft:breeze_rod"],
    "exotics/sandevistan": ["minecraft:nether_star"],
    "exotics/emergency_defibrillator": ["minecraft:totem_of_undying"],
}
SPECIAL_EXOTICS = ("sandevistan", "emergency_defibrillator")


# ---------------------------------------------------------------- component recipes
def seq(base, transitional, loops, steps, result):
    """A Create sequenced assembly. steps: ("deploy", ingredient) | ("fill", fluid, mB) | ("press",) | ("cut",)."""
    out = []
    for step in steps:
        t = {"id": transitional}
        if step[0] == "deploy":
            out.append({"type": "create:deploying", "ingredients": [{"item": transitional}, ing(step[1])], "results": [t]})
        elif step[0] == "fill":
            out.append({"type": "create:filling", "ingredients": [{"item": transitional}, fluid(step[1], step[2])], "results": [t]})
        elif step[0] == "press":
            out.append({"type": "create:pressing", "ingredients": [{"item": transitional}], "results": [t]})
        elif step[0] == "cut":  # "etch" until the Precision Etcher exists
            out.append({"type": "create:cutting", "ingredients": [{"item": transitional}], "results": [t]})
        else:
            raise ValueError(step)
    return {"type": "create:sequenced_assembly", "ingredient": base, "loops": loops, "results": [result],
            "sequence": out, "transitional_item": {"id": transitional}}


def mixing(ingredients, results, heat=None):
    r = {"type": "create:mixing"}
    if heat:
        r["heat_requirement"] = heat
    r.update(ingredients=ingredients, results=results)
    return r


def component_recipes():
    """recipe path -> json (under data/cyberimplants/recipe/)."""
    R = {}
    # Tier I - Civilian: andesite-age machines only
    R["component/copper_wire"] = {"type": "create:cutting", "ingredients": [ing("#c:plates/copper")], "processing_time": 50,
                                  "results": [res(m("copper_wire"), 2)]}
    R["component/synthetic_fiber"] = mixing([ing("#c:strings"), ing("#c:strings"), ing("#%s:fiber_fillers" % MOD),
                                             ing("#%s:fiber_fillers" % MOD), ing("#c:slime_balls")], [res(m("synthetic_fiber"), 4)])
    R["component/mechanical_servo"] = {"type": "create:deploying", "ingredients": [ing("#c:plates/iron"), ing("create:cogwheel")],
                                       "results": [res(m("mechanical_servo"))]}
    R["component/civilian_casing"] = {"type": "create:compacting", "ingredients": [ing("create:andesite_alloy"), ing(m("synthetic_fiber"))],
                                      "results": [res(m("civilian_casing"))]}
    # Tier II - Industrial: heat, bulk blasting, the first assembly loop
    R["component/crude_steel_blend"] = mixing([ing("#c:ingots/iron"), ing("#minecraft:coals"), ing("#minecraft:coals")],
                                              [res(m("crude_steel_blend"))], "heated")
    R["component/tempered_steel_ingot"] = {"type": "minecraft:blasting", "category": "misc", "cookingtime": 100, "experience": 0.3,
                                           "ingredient": ing(m("crude_steel_blend")), "result": {"id": m("tempered_steel_ingot")}}
    R["component/tempered_steel_sheet"] = {"type": "create:pressing", "ingredients": [ing(m("tempered_steel_ingot"))],
                                           "results": [res(m("tempered_steel_sheet"))]}
    R["component/precision_servo"] = seq(ing(m("mechanical_servo")), m("incomplete_precision_servo"), 1,
                                         [("deploy", "create:precision_mechanism"), ("deploy", m("copper_wire")), ("press",)],
                                         res(m("precision_servo")))
    R["component/synthetic_muscle"] = mixing([ing(m("synthetic_fiber"))] * 3 + [ing("#c:dusts/redstone"), fluid(BIO, 100)],
                                             [res(m("synthetic_muscle"), 2)], "heated")
    # Tier III - Advanced: superheated mixing, silicon, etched chips
    for kind in ("milling", "crushing"):
        R["component/silica_dust_%s" % kind] = {"type": "create:" + kind, "ingredients": [ing("#c:gems/quartz")],
                                                "processing_time": 150,
                                                "results": [res(m("silica_dust"), 2), res(m("silica_dust"), 1, 0.25)]}
    R["component/silicon_boule"] = mixing([ing(m("silica_dust"))] * 4 + [ing("#minecraft:coals")], [res(m("silicon_boule"))], "superheated")
    R["component/silicon_wafer"] = {"type": "create:cutting", "ingredients": [ing(m("silicon_boule"))], "processing_time": 100,
                                    "results": [res(m("silicon_wafer"), 6)]}
    R["component/logic_chip"] = seq(ing(m("silicon_wafer")), m("incomplete_logic_chip"), 2,
                                    [("fill", INK, 50), ("cut",), ("deploy", "create:electron_tube"), ("cut",)], res(m("logic_chip")))
    R["component/myomer_bundle"] = mixing([ing(m("synthetic_muscle"))] * 3 + [ing("#c:slime_balls"), fluid(BIO, 100)],
                                          [res(m("myomer_bundle"))], "heated")
    R["component/inked_steel_sheet"] = {"type": "create:filling", "ingredients": [ing(m("tempered_steel_sheet")), fluid(INK, 100)],
                                        "results": [res(m("inked_steel_sheet"))]}
    R["component/ground_lens"] = {"type": "create:cutting", "ingredients": [ing("#c:gems/amethyst")], "processing_time": 100,
                                  "results": [res(m("ground_lens"))]}
    R["component/phase_lens"] = {"type": "create:haunting", "ingredients": [ing(m("ground_lens"))], "results": [res(m("phase_lens"))]}
    # Tier IV - Military
    R["component/composite_plate"] = seq(ing("create:sturdy_sheet"), m("incomplete_composite_plate"), 1,
                                         [("deploy", m("tempered_steel_sheet")), ("deploy", "#%s:composite_reinforcement" % MOD), ("press",)],
                                         res(m("composite_plate")))
    R["component/military_processor"] = seq(ing(m("logic_chip")), m("incomplete_military_processor"), 3,
                                            [("deploy", "#c:plates/gold"), ("cut",), ("fill", COOLANT, 100), ("deploy", m("logic_chip")), ("cut",)],
                                            res(m("military_processor")))
    # Tier V - Experimental
    R["component/echo_lattice"] = mixing([ing("#%s:echo_sources" % MOD), ing("create:polished_rose_quartz"),
                                          ing("create:polished_rose_quartz"), fluid(NANITES, 100)], [res(m("echo_lattice"), 2)], "superheated")
    R["component/experimental_core"] = seq(ing(m("military_processor")), m("incomplete_experimental_core"), 1,
                                           [("deploy", m("echo_lattice")), ("cut",), ("fill", NANITES, 100), ("cut",), ("press",)],
                                           res(m("experimental_core")))
    R["component/quantum_substrate"] = mixing([ing("#c:gems/amethyst"), ing("#c:ender_pearls"), ing("#%s:echo_sources" % MOD)],
                                              [res(m("quantum_substrate"))], "superheated")
    # Process fluids
    R["fluid/bio_interface_gel"] = mixing([fluid("minecraft:water", 250), ing("#c:slime_balls"), ing("minecraft:bone_meal"),
                                           ing("minecraft:sugar")], [{"amount": 250, "id": BIO}])
    R["fluid/conductive_ink"] = mixing([fluid("minecraft:water", 250), ing("#c:dusts/redstone"), ing("#c:dusts/redstone"),
                                        ing("#c:nuggets/copper")], [{"amount": 250, "id": INK}], "heated")
    R["fluid/cryo_coolant"] = mixing([fluid("minecraft:water", 250), ing("#%s:cryo_ice" % MOD), ing("#%s:cryo_ice" % MOD),
                                      ing("#c:gems/lapis")], [{"amount": 250, "id": COOLANT}])
    R["fluid/nanite_suspension"] = mixing([fluid(BIO, 250), ing(m("silica_dust")), ing(m("silica_dust")), ing("#c:dusts/redstone"),
                                           ing("#c:nuggets/iron"), ing("#c:nuggets/iron"), ing("#c:nuggets/iron")],
                                          [{"amount": 250, "id": NANITES}], "superheated")
    R["fluid/neural_gel"] = mixing([fluid(BIO, 250), fluid(NANITES, 50)], [{"amount": 250, "id": NEURAL}], "heated")
    return R


# ---------------------------------------------------------------- basic (Tier I) implants
# Pattern  a b a / w X w / a C a : four of the category's frame part a, one category part b, two Copper Wire, the
# implant's signature item X and a Civilian Casing.
TIER1_PARTS = {
    "core": ("#c:plates/copper", m("mechanical_servo")),
    "neural": (m("copper_wire"), "#c:dusts/redstone"),
    "cyberdeck": (m("copper_wire"), "#c:dusts/redstone"),
    "skeletal": ("#c:plates/iron", "minecraft:bone_block"),
    "limbs": ("#c:plates/iron", m("mechanical_servo")),
    "optical": ("#c:glass_panes", "#c:plates/copper"),
    "circulatory": (m("synthetic_fiber"), "#c:plates/copper"),
    "subdermal": ("#c:plates/iron", m("synthetic_fiber")),
    "mobility": (m("synthetic_fiber"), "minecraft:piston"),
}


def tier1_recipe(ident, category, signature):
    a, b = TIER1_PARTS[category]
    return {"type": "minecraft:crafting_shaped", "category": "misc",
            "key": {"a": ing(a), "b": ing(b), "w": ing(m("copper_wire")), "X": ing(signature), "C": ing(m("civilian_casing"))},
            "pattern": ["aba", "wXw", "aCa"],
            "result": {"id": m(ident), "count": 1, "components": {m("tier"): 1}}}


# ---------------------------------------------------------------- grades (Tier II-V upgrades)
D = lambda x: ("deploy", x)  # noqa: E731
F = lambda f, n: ("fill", f, n)  # noqa: E731

# The category part(s) of each upgrade (section 6.4), placed before the tier's shared template.
CATEGORY_PARTS = {
    2: {"core": [D(m("precision_servo")), D("create:electron_tube")], "neural": [D(m("precision_servo"))],
        "cyberdeck": [D("create:electron_tube"), D("create:electron_tube")], "skeletal": [D(m("tempered_steel_sheet"))],
        "limbs": [D(m("precision_servo"))], "optical": [D("create:polished_rose_quartz")], "circulatory": [D(m("synthetic_muscle"))],
        "subdermal": [D(m("tempered_steel_sheet"))], "mobility": [D(m("synthetic_muscle")), D(m("synthetic_muscle"))]},
    3: {"core": [D(m("logic_chip")), D(m("myomer_bundle"))], "neural": [D(m("logic_chip"))], "cyberdeck": [D(m("logic_chip"))],
        "skeletal": [D(m("myomer_bundle"))], "limbs": [D(m("myomer_bundle"))], "optical": [D(m("phase_lens"))],
        "circulatory": [D(m("myomer_bundle"))], "subdermal": [D(m("inked_steel_sheet"))],
        "mobility": [D(m("myomer_bundle")), D(m("myomer_bundle"))]},
    4: {"core": [D(m("military_processor"))], "neural": [D(m("military_processor")), F(NEURAL, 100)],
        "cyberdeck": [D(m("military_processor"))], "skeletal": [D("create:sturdy_sheet")], "limbs": [D(m("military_processor"))],
        "optical": [D(m("military_processor"))], "circulatory": [F(NEURAL, 100), D(m("military_processor"))],
        "subdermal": [F(NANITES, 100)], "mobility": [D(m("military_processor"))]},
}

# The shared template of each tier, after the category part.
TEMPLATE = {
    2: [D(m("tempered_steel_sheet")), F(BIO, 100), ("press",)],
    3: [F(INK, 50), ("cut",), D(m("logic_chip"))],
    4: [F(COOLANT, 100), ("cut",), D(m("composite_plate")), ("press",)],
    5: [D(m("experimental_core")), F(NANITES, 100), ("cut",)],
}
LOOPS = {2: 1, 3: 2, 4: 3, 5: 4}
TRANSITIONAL = m("incomplete_implant")


def upgrade_steps(ident, category, tier):
    if tier == 5:
        parts = [D(EXOTIC_TAG % category)]
        if ident in SPECIAL_EXOTICS:
            parts.append(D(EXOTIC_TAG % ident))
    else:
        parts = CATEGORY_PARTS[tier][category]
    return parts + TEMPLATE[tier]


def upgrade_recipe(ident, category, tier):
    base = {"type": "neoforge:components", "items": m(ident), "components": {m("tier"): tier - 1}}
    return seq(base, TRANSITIONAL, LOOPS[tier], upgrade_steps(ident, category, tier),
               {"id": m(ident), "count": 1, "components": {m("tier"): tier}})


# ---------------------------------------------------------------- machines
def machine_recipes():
    return {
        "implantation_rig": {"type": "minecraft:crafting_shaped", "category": "misc",
                             "key": {"A": ing("create:andesite_casing"), "I": ing("#c:plates/iron"), "M": ing(m("mechanical_servo")),
                                     "S": ing("create:shaft"), "C": ing("create:cogwheel"), "K": ing(m("civilian_casing"))},
                             "pattern": ["IMI", "SKC", "IAI"], "result": {"id": m("implantation_rig"), "count": 1}},
        "kinetic_charger": {"type": "minecraft:crafting_shaped", "category": "misc",
                            "key": {"A": ing("create:andesite_casing"), "R": ing("minecraft:redstone_block"), "C": ing("#c:plates/copper"),
                                    "S": ing("create:shaft"), "W": ing(m("copper_wire"))},
                            "pattern": ["CRC", "SAS", "WCW"], "result": {"id": m("kinetic_charger"), "count": 1}},
        "neural_port": {"type": "minecraft:crafting_shaped", "category": "misc",
                        "key": {"T": ing("create:electron_tube"), "C": ing("#c:plates/copper"), "A": ing("create:andesite_casing"),
                                "W": ing(m("copper_wire"))},
                        "pattern": [" T ", "CAC", " W "], "result": {"id": m("neural_port"), "count": 1}},
        "proximity_sensor": {"type": "minecraft:crafting_shaped", "category": "misc",
                             "key": {"E": ing("create:electron_tube"), "A": ing("create:andesite_casing"), "R": ing("#c:dusts/redstone"),
                                     "S": ing("minecraft:spyglass")},
                             "pattern": [" E ", "ASA", " R "], "result": {"id": m("proximity_sensor"), "count": 1}},
        "flechette": {"type": "minecraft:crafting_shapeless", "category": "misc",
                      "ingredients": [ing("#c:nuggets/iron"), ing("#c:nuggets/iron"), ing("minecraft:flint")],
                      "result": {"id": m("flechette"), "count": 4}},
        "field_manual": {"type": "minecraft:crafting_shapeless", "category": "misc",
                         "ingredients": [ing("minecraft:book"), ing("#c:dusts/redstone")], "result": {"id": m("field_manual")}},
        # Neuro-Stabilizer: a bottle spouted with Neural Gel.
        "neuro_stabilizer": {"type": "create:filling", "ingredients": [ing("minecraft:glass_bottle"), fluid(NEURAL, 250)],
                             "results": [res(m("neuro_stabilizer"))]},
    }
