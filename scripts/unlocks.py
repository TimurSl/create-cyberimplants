"""Unlock rows: the tier-gated features of docs/design/implants that are implemented.

UNLOCKS[implant] = {unlock_id: (first_tier, layer, english, russian)}. A tier lists every unlock introduced at or below
it (the rows are cumulative). Every id is registered in the stat file's unlock_layers with its layer, so
layers.maxEnabled below that layer switches it off: the Tier II-IV rows belong to Layer 3 (Capability), the Tier V
rows to Layer 5 (Mastery). Rows that are not implemented yet are deliberately absent; the roadmap lists what is left.
"""

CAPABILITY = 3
MASTERY = 5

UNLOCKS = {
    # ---- A. Neural
    "sandevistan": {
        "temporal_anchor": (5, MASTERY, "Temporal Anchor: allies are not slowed; +25% attack speed inside your own field",
                            "Временной якорь: союзники не замедляются; +25% к скорости атаки внутри своего поля"),
    },
    "automatic_evasion_system": {
        "parry": (5, MASTERY, "Parry: with a melee weapon (or a deployed blade) in hand, arrows are reflected instead of dodged",
                  "Парирование: с оружием ближнего боя (или выдвинутым лезвием) в руке стрелы отражаются вместо уклонения"),
    },
    # ---- C. Skeletal
    "reinforced_skeleton": {
        "crit_immune": (5, MASTERY, "Incoming critical hits deal normal damage", "Входящие критические удары наносят обычный урон"),
    },
    "impact_dampeners": {
        "recoil_storage": (5, MASTERY, "Recoil Storage: absorbed fall energy feeds the Momentum Booster at x2",
                           "Накопитель отдачи: энергия падения питает Ускоритель импульса с множителем x2"),
    },
    "inertia_compensation": {
        "blast_resistant": (5, MASTERY, "Explosion knockback -50%", "Отбрасывание от взрывов -50%"),
    },
    # ---- D. Limbs
    "retractable_blade": {
        "thermal_edge": (5, MASTERY, "Thermal edge: sets targets on fire and ignores 40% of their armour",
                         "Термокромка: поджигает цель и игнорирует 40% её брони"),
    },
    # ---- E. Optical
    "optical_camouflage": {
        "recloak": (5, MASTERY, "Re-cloak: the camo survives one attack and re-engages after 1 s",
                    "Повторная маскировка: камуфляж переживает одну атаку и возвращается через 1 с"),
    },
    "target_tracking": {
        "tracer_tagging": (5, MASTERY, "Tracer tagging: Ballistic Module hits tag the target",
                           "Трассирующие метки: попадания Баллистического модуля помечают цель"),
    },
    # ---- F. Circulatory
    "regenerator": {
        "works_when_starving": (2, CAPABILITY, "Works regardless of hunger", "Работает независимо от голода"),
        "cures_poison": (3, CAPABILITY, "Cures Poison", "Снимает отравление"),
        "cures_wither_hunger": (4, CAPABILITY, "Cures Wither and Hunger", "Снимает иссушение и голод"),
        "burst": (5, MASTERY, "Burst (Implant Wheel or deck): 3 s at x3 healing with no lockout, 60 s cooldown",
                  "Всплеск (колесо имплантов или дек): 3 с лечения x3 без блокировки, перезарядка 60 с"),
    },
    "adrenaline_pump": {
        "ignores_slowness": (2, CAPABILITY, "Ignores Slowness during the boost", "Игнорирует замедление во время рывка"),
        "boost_jump": (3, CAPABILITY, "Boost adds jump height", "Рывок увеличивает высоту прыжка"),
        "boost_attack_speed": (4, CAPABILITY, "Boost adds +15% attack speed", "Рывок даёт +15% к скорости атаки"),
        "last_stand": (5, MASTERY, "Boost doubled below 30% health", "Рывок удваивается при здоровье ниже 30%"),
    },
    "energy_buffer": {
        "surge": (5, MASTERY, "Surge: once per minute a normal activation may draw on the buffer",
                  "Выброс: раз в минуту обычная активация может взять заряд из буфера"),
    },
    # ---- G. Subdermal
    "shock_circuit": {
        "slows_attacker": (2, CAPABILITY, "Attacker gets Slowness for 1 s", "Атакующий получает замедление на 1 с"),
        "chain_one": (3, CAPABILITY, "Chains to 1 extra enemy within 4 blocks", "Перескакивает ещё на 1 врага в радиусе 4 блоков"),
        "chain_three": (5, MASTERY, "Chains to 3 extra enemies", "Перескакивает ещё на 3 врагов"),
        "grounded": (5, MASTERY, "Grounded: immune to lightning, and a strike fully recharges the battery",
                     "Заземление: иммунитет к молнии, удар молнии полностью заряжает батарею"),
    },
    "kinetic_shield": {
        "absorbs_projectiles": (3, CAPABILITY, "Projectiles are fully absorbed", "Снаряды поглощаются полностью"),
        "overcharge": (5, MASTERY, "Overcharge: the Energy Buffer pushes the pool to 150%",
                       "Перезаряд: Энергобуфер поднимает запас щита до 150%"),
    },
    "emergency_shell": {
        "knockback_immune": (3, CAPABILITY, "Knockback immunity while active", "Неуязвимость к отбрасыванию, пока активна"),
        "cleanses": (4, CAPABILITY, "Extinguishes fire and cleanses harmful effects on trigger",
                     "При срабатывании тушит огонь и снимает вредные эффекты"),
        "manual_activation": (5, MASTERY, "Manual or deck activation at any health (half duration)",
                              "Ручная активация или через дек при любом здоровье (половина длительности)"),
    },
    # ---- H. Mobility
    "enhanced_tendons": {
        "gust": (5, MASTERY, "Gust: each air jump pushes nearby creatures away", "Порыв: каждый прыжок в воздухе отталкивает существ рядом"),
    },
    "magnetic_soles": {
        "polarity": (5, MASTERY, "Polarity: while on, pulls metal items within 6 blocks",
                     "Полярность: пока включены, притягивают металлические предметы в радиусе 6 блоков"),
    },
}

# Unlocks that give a Passive or Triggered implant something to activate: they put it on the Implant Wheel and make it
# usable from a deck once the installed tier has the row (dev.cyberimplants.implant.ImplantActions).
ACTION_UNLOCKS = {"regenerator": "burst", "emergency_shell": "manual_activation"}


def unlocks_for(ident, tier):
    """Unlock ids active at the given tier (1-5) for the implant, in a stable order."""
    return sorted(u for u, row in UNLOCKS.get(ident, {}).items() if row[0] <= tier)


def unlock_layers(ident):
    return {u: row[1] for u, row in sorted(UNLOCKS.get(ident, {}).items())}


def lang(index):
    """Tooltip text keyed cyberimplants.unlock.<implant>.<id>; index 0 is English, 1 Russian."""
    return {"cyberimplants.unlock.%s.%s" % (impl, uid): row[2 + index]
            for impl, rows in UNLOCKS.items() for uid, row in rows.items()}
