# C. Skeletal and Muscular Augmentations

**Region:** Skeleton (3 slots). These are structural upgrades. Most are passive and Charge-free, which is why they
cost Load and slot space. Each one still has a qualitative feature that unlocks at higher tiers.

---

## Reinforced Skeleton

**Skeleton · Passive · Load 3/3/4/4/5 · Charge —**

*Identity:* a skeleton that resists **blunt force and crushing**. It gives a *flat* reduction per hit, which beats
swarms of weak attackers, and strong resistance to Create machinery and crushing hazards.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Flat reduction per hit vs `#cyberimplants:blunt` (mob/player melee, falling blocks, anvils, `fly_into_wall`, mace) | 0.5 | 1.0 | 1.5 | 2.0 | 2.5 |
| Reduction vs `#cyberimplants:machinery` (Create crushing wheels, saws, drills, rollers, fan damage), cramming | 20% | 30% | 40% | 55% | 70% |
| Max health | — | — | +2 | +4 | +6 |
| Unlocks | — | — | — | Immune to cramming | Immune to bone-shattering crits: incoming critical hits deal non-crit damage |

**Limits:** **Dense frame**. At T3+ the user sinks in water (−15% swim speed) and gains +5% gravity. A flat
reduction is weak against single huge hits.

**Deck:** read-only `skeleton.tier`.

---

## Power Actuators

**Skeleton · Passive (+ Power Strike) · Load 4/4/5/6/7 · Charge 2 per Power Strike**

*Identity:* stronger melee. Every **fully charged** hit becomes a **Power Strike** with an extra mechanical effect.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Attack damage | +1 | +1.5 | +2 | +3 | +4 |
| Power Strike effect | +20% knockback | + disables shields (like an axe) | + 25% bonus damage | + **Stagger:** Slowness II 1 s | + **Shockwave:** 2-block cone sweep for 50% damage |
| Unarmed bonus | ×2 attack damage bonus when the main hand is empty or holds a Retractable Blade | ← | ← | ← | ← |

**Limits:** a Power Strike only happens on a full attack cooldown, so spam-clicking gets none. If Charge is empty,
hits still land but without the Power Strike.

**Deck:** event `power_strike(target)`.

---

## Kinetic Tendons

**Skeleton · Passive · Load 2/3/3/4/4 · Charge —**

*Identity:* passive, **always-on** jump improvements for normal ground jumps. (Air jumps are Enhanced Tendons;
charged leaps are Hydraulic Legs.)

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Jump height bonus | +0.25 | +0.5 | +0.75 | +1.0 | +1.25 |
| Step height | vanilla | vanilla | 1.0 (auto-step) | 1.0 | 1.25 |
| Unlocks | — | — | Auto-step (disabled while sneaking) | Sprint-jump distance +10% | **Elastic Recoil:** jumping within 3 t of landing gives +30% jump height |

**Limits:** none beyond Load and a Skeleton slot. It is cheap and simple. Auto-step can be toggled off in the wheel.

---

## Micro-Actuators

**Skeleton · Passive · Load 3/3/4/4/5 · Charge —**

*Identity:* **locomotion**: sprint speed, how fast top speed is reached, and climbing speed.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Sprint speed | +5% | +8% | +11% | +14% | +18% |
| Time to top speed | −10% | −20% | −30% | −40% | −50% |
| Ladder / scaffolding / vine climb speed | — | — | +25% | +50% | +75% |
| Sprint exhaustion | +10% | +10% | +10% | +0% | +0% |

**Limits:** **Servo whine**. At T1–T3 the user's sprinting footsteps carry 1.5× further as vibrations and sounds.
This conflicts with stealth builds; Silent Actuators T4+ cancel it.

---

## Impact Dampeners

**Skeleton · Passive · Load 2/2/3/3/4 · Charge: generates**

*Identity:* survive falls and **recover energy from them**.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Safe fall distance bonus | +2 | +4 | +6 | +8 | +12 |
| Fall damage reduction beyond safe distance | 15% | 25% | 35% | 50% | 65% |
| Energy recovery | — | 1 C / block absorbed | 2 C | 3 C | 4 C |
| Unlocks | — | — | — | No crop trampling, no landing slowdown | **Recoil Storage:** absorbed fall energy feeds Momentum Booster at ×2 |

**Limits:** no effect on non-fall damage. Recovery is capped at 50 C per landing to prevent "fall farming" in water
columns.

**Deck:** event `landed(fall_distance, recovered)`.

---

## Inertia Compensation

**Skeleton · Passive · Load 3/3/4/4/5 · Charge —**

*Identity:* **grounded** momentum management. The user keeps speed through sharp turns, stays sprinting, and resists
being shoved. (Airborne control is the Inertial Stabilizer.)

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Speed kept through a ≥ 90° turn | 30% | 45% | 60% | 75% | 90% |
| Knockback resistance | 10% | 15% | 20% | 30% | 40% |
| Unlocks | — | Strafing does not cancel sprint | Minor collisions (< 0.5 block overlap) don't cancel sprint | Sprint while using items (at 50% sprint speed) | Explosion knockback resistance 50% |

**Limits:** ground only. It does nothing while airborne, swimming or flying.
