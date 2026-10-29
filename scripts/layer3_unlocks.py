"""Layer 3 unlock rows: the tier-gated features of docs/design/implants that are implemented so far.

UNLOCKS[implant] = {unlock_id: first_tier}. A tier lists every unlock introduced at or below it (the rows are cumulative),
and every id is registered in the stat file's unlock_layers with layer 3, so layers.maxEnabled < 3 switches them off.
Unlock rows that are not implemented yet are deliberately absent; the roadmap lists what is left.
"""

LAYER = 3

UNLOCKS = {
    "regenerator": {"works_when_starving": 2, "cures_poison": 3, "cures_wither_hunger": 4},
    "adrenaline_pump": {"ignores_slowness": 2, "boost_jump": 3, "boost_attack_speed": 4},
    "shock_circuit": {"slows_attacker": 2, "chain_one": 3},
    "kinetic_shield": {"absorbs_projectiles": 3},
    "emergency_shell": {"knockback_immune": 3, "cleanses": 4},
}


def unlocks_for(ident, tier):
    """Unlock ids active at the given tier (1-5) for the implant, in a stable order."""
    return sorted(u for u, first in UNLOCKS.get(ident, {}).items() if first <= tier)


def unlock_layers(ident):
    return {u: LAYER for u in sorted(UNLOCKS.get(ident, {}))}


# Tooltip text, keyed cyberimplants.unlock.<implant>.<id>
TEXT = {
    "regenerator": {"works_when_starving": "Works regardless of hunger", "cures_poison": "Cures Poison",
                    "cures_wither_hunger": "Cures Wither and Hunger"},
    "adrenaline_pump": {"ignores_slowness": "Ignores Slowness during the boost", "boost_jump": "Boost adds jump height",
                        "boost_attack_speed": "Boost adds +15% attack speed"},
    "shock_circuit": {"slows_attacker": "Attacker gets Slowness for 1 s", "chain_one": "Chains to 1 extra enemy within 4 blocks"},
    "kinetic_shield": {"absorbs_projectiles": "Projectiles are fully absorbed"},
    "emergency_shell": {"knockback_immune": "Knockback immunity while active",
                        "cleanses": "Extinguishes fire and cleanses harmful effects on trigger"},
}


def lang():
    return {"cyberimplants.unlock.%s.%s" % (impl, uid): text for impl, rows in TEXT.items() for uid, text in rows.items()}
