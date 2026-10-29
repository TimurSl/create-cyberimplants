# G. Subdermal Armor

**Region:** Integument (2 slots). These are defensive layers under the skin. Each implant handles a different
*shape* of incoming damage, so with two slots the user has to decide what they are defending against.

### Damage pipeline order

Mitigation layers apply in a fixed order, which keeps stacking predictable:

1. **Nanocoating** — typed % reduction (incoming-damage stage)
2. **Reinforced Skeleton** — flat per-hit / machinery % (incoming-damage stage)
3. Vanilla **armor and toughness**, including Armor Plating attributes and enchantment protection
4. **Emergency Shell** — % reduction while active
5. **Kinetic Shield** — absorbs from its pool (post-armor, like absorption hearts)
6. Health loss. On lethal damage, the **Emergency Defibrillator** fires.

---

## Armor Plating

**Integument · Passive · Load 3/3/4/4/5 · Charge —**

*Identity:* **armor points under the skin**. They stack with worn armor up to the vanilla cap, and they protect worn
armor from wear.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Armor | +2 | +3 | +4 | +5 | +6 |
| Armor toughness | — | +1 | +2 | +2 | +3 |
| Worn-armor durability loss reduction | — | — | 20% | 35% | 50% |
| Weight (movement speed) | — | — | −2% | −3% | −3% |

**Limits:** vanilla armor scales poorly against single huge hits and has a cap of 30. Weight at T3+ conflicts with
speed builds.

---

## Nanocoating

**Integument · Passive · Load 3/3/4/5/6 · Charge — (recalibration 20 C)**

*Identity:* **typed damage resistance**. A nanite layer is tuned with a *formula* that targets one damage family.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Balanced formula (all listed families) | 4% | 6% | 8% | 10% | 12% |
| Specialised formula (one family) | 15% | 20% | 30% | 40% | 50% |
| Formulas available | Balanced, Thermal, Kinetic | + Blast | + Chemical | + **Dual** (two families at 75% strength each) | + **Adaptive** (shifts to the last family taken, 10 s lag) |
| Field recalibration | at the Rig only | at the Rig only | from the implant wheel or a deck (30 s, during which there is no protection) | 20 s | 10 s |

**Damage families** (data-driven damage-type tags):
- **Thermal:** fire, lava, hot floor, Create fan blasting damage
- **Kinetic:** arrows, tridents, other projectiles
- **Blast:** explosions
- **Chemical:** magic, indirect magic, wither, dragon breath

**Limits:** a specialised formula is useless against the wrong family. Recalibrating leaves the user unprotected
for the duration. The formula is per-player configuration, not a separate item.

**Deck:** `nano.formula`, `CONFIGURE nano formula = "thermal" | "kinetic" | "blast" | "chemical" | "balanced"`.
Event `nano_recalibrated`.

---

## Shock Circuit

**Integument · Triggered (+ Discharge at T4+) · Load 3/3/4/4/5 · Charge 3 per proc**

*Identity:* **punishes melee attackers** with an electric discharge through the skin.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Retaliation damage (50% armor-piercing) | 2 | 3 | 4 | 5 | 6 |
| Internal cooldown | 1.5 s | 1.25 s | 1.0 s | 0.75 s | 0.5 s |
| Unlocks | — | Attacker gets Slowness I for 1 s | Chains to 1 extra enemy within 4 blocks | **Discharge:** manual nova, r = 4, 6 damage, 40 C, 20 s cooldown | Chains to 3. **Grounded:** immune to lightning, and a lightning strike fully recharges the user |

**Quirk (all tiers):** a creeper hit by a Shock Circuit becomes **charged**, as if struck by lightning. This is a
real risk.

**Limits:** melee only. Projectiles don't trigger it. It drains Charge on every proc, and in a large melee brawl it
can empty the battery quickly. This is intentional.

**Deck:** `shock.enabled` (write), `ACTIVATE shock` (Discharge, T4+). Event `shock_proc(attacker)`.

---

## Kinetic Shield

**Integument · Passive · Load 4/4/5/6/7 · Charge 3 per HP recharged**

*Identity:* a **regenerating absorb pool** projected just above the skin.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Capacity | 4 HP | 6 | 8 | 10 | 14 |
| Share of each hit absorbed | 50% | 60% | 70% | 80% | 90% |
| Recharge delay after damage | 8 s | 7 s | 6 s | 5 s | 4 s |
| Recharge rate | 0.5 HP/s | 0.75 | 1.0 | 1.25 | 1.75 |
| Unlocks | — | — | **Projectiles are fully absorbed** (100%) | **Break pulse:** knockback burst (r = 3) when the shield breaks | **Overcharge:** Energy Buffer can push the pool to 150% |

**Limits:** doesn't absorb `#cyberimplants:shield_bypass` damage (fall, drowning, starvation, suffocation, void).
Recharging costs Charge. An ERS or deck can trigger an **emergency recharge** (fills 50% instantly, 45 s cooldown,
Emergency-flagged).

**Deck:** `shield.value`, `shield.max`, `ACTIVATE shield` (emergency recharge). Events `shield_broken` and
`shield_full`.

---

## Emergency Shell

**Integument · Triggered (Emergency) · Load 3/3/4/4/5 · Charge 30 (can use Energy Buffer)**

*Identity:* a **last-ditch hardening**. At critical health, the subdermal layer locks rigid for a few seconds.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Auto-trigger at health ≤ | 20% | 25% | 25% | 30% | 30% |
| Damage reduction | 40% | 50% | 60% | 70% | 80% |
| Duration | 2 s | 2.5 s | 3 s | 4 s | 5 s |
| Movement penalty while active | −40% | −35% | −30% | −20% | −10% |
| Cooldown | 120 s | 100 s | 90 s | 75 s | 60 s |
| Unlocks | — | — | Knockback immunity while active | Extinguishes fire, cleanses negative effects on trigger | **Manual/deck activation** at any health (half duration) |

**Limits:** the user is slowed and the cooldown is long. If a burst of damage takes them from high health to dead
in one hit, the Shell never triggers. That case belongs to the Defibrillator.

**Deck:** `shell.ready`, `shell.active`, `ACTIVATE shell` (T5, or via ERS).
