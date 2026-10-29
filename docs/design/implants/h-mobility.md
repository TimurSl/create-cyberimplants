# H. Mobility Systems

**Region:** Legs (2 slots). Movement, traversal and stealth. Each implant here gives the player a **new way to
move**, not just faster movement.

> **Implementation note.** Player movement in Minecraft is client-authoritative. Every mobility ability runs on the
> client and is validated by the server (charges, cooldowns, Charge cost). See
> [Technical architecture §8.6](../08-technical-architecture.md#86-movement-abilities).

---

## Enhanced Tendons

**Legs · Input-bound · Load 3/3/4/4/5 · Charge 4 per air jump**

*Identity:* **mid-air jumps**.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Air jumps per airtime | 1 | 1 | 2 | 2 | 3 |
| Air jump strength (vs. ground jump) | 70% | 80% | 90% | 100% | 100% |
| Unlocks | — | An air jump halves accumulated fall distance | **Directional** air jump (follows movement input) | **Air dash:** sprint + jump in the air dashes horizontally 4 blocks instead | **Gust:** each air jump pushes nearby entities away, like a small wind charge |

**Limits:** costs Charge on every jump. Air jumps reset only on landing, climbing, or Microgrip/Magnetic Soles
attachment.

---

## Hydraulic Legs

**Legs · Input-bound (charged) · Load 4/4/5/5/6 · Charge 15 per leap (or 100 backtank air) · Pneumatic**

*Identity:* a **charged, directional super-leap** and a **ground slam**. Hold *Sneak + Jump* to build pressure,
then release to launch along the look direction.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Max vertical leap | 4 | 6 | 8 | 10 | 14 |
| Max horizontal leap | 6 | 9 | 12 | 16 | 22 |
| Full-charge time | 1.5 s | 1.25 s | 1.0 s | 0.8 s | 0.6 s |
| Leap cooldown | 3 s | 3 s | 2.5 s | 2.5 s | 2 s |
| Unlocks | — | No fall damage from the landing of the user's own leap | **Ground slam:** *Sneak* mid-air dives down, 3 damage AoE r = 3 on landing | Slam damage scales with fall height (+1 per 4 blocks, max +6), and launches hit mobs upward | **Rebound:** jumping within 0.5 s of a slam chains into a free full-charge leap |

**Limits:** charging roots the user (movement −80%) and is audible. The pneumatic option ties long-range mobility
to Create air compressors.

**Deck:** `hydraulics.ready`. Events `leap(power)` and `slam(damage)`. ERS may fire an *escape leap* (straight up,
full power).

---

## Silent Actuators

**Legs · Passive · Load 2/2/3/3/4 · Charge —**

*Identity:* **acoustic stealth**. Footsteps and actions stop producing sound and vibrations. This is a separate
layer from Optical Camouflage, which handles visual stealth.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Vibrations suppressed | footsteps | + landing, swimming | + eating, drinking, block interaction | + combat hits | all player-caused vibrations except explosions |
| Sneak speed bonus | — | +10% | +15% | +25% | +35% |
| Mob detection reduction while sneaking | 10% | 15% | 20% | 30% | 40% |
| Unlocks | — | — | Sculk sensors never trigger from the user | **Warden can't sense the user while sneaking** | **Ghost step:** no Thermal heat trails, and Entity Scanner auto-scan ignores the user while still. Cancels Micro-Actuators *servo whine* |

**Limits:** does nothing against line-of-sight detection. Most effective while sneaking, and much weaker while
sprinting.

---

## Magnetic Soles

**Legs · Toggle · Load 2/2/3/3/4 · Charge 0.5 C/s while attached**

*Identity:* **stick to metal**. While toggled on, the user can walk up metal walls and stay anchored on moving metal.

**Magnetic surfaces** (`#cyberimplants:magnetic`): blocks of iron, copper (all oxidation states), gold and
netherite; iron bars, doors and trapdoors; anvils, cauldrons, chains; Create **brass, copper, andesite and train
casings**, **metal girders**, **item vaults**, **fluid tanks**, **metal ladders**, sheet-metal blocks. The tag is
extensible by datapack.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Wall-walk speed (vs. normal walk) | 50% | 65% | 80% | 90% | 100% |
| Unlocks | — | **Anchor:** no sliding on moving Create contraptions and trains, no fan push, and no knockback while on metal | **Ceiling walk** (upside-down, 50% speed) | **Snap:** jump toward a metal surface within 6 blocks and attach | **Polarity:** repel off metal for a magnetic leap; also pulls metal item entities within 6 blocks |

**Limits:** metal surfaces only. While toggled on, jumps from metal surfaces are 50% weaker, and the Charge drain
continues while attached.

**Deck:** `soles.active` (read/write), `soles.attached`. Events `attached(surface)` and `detached`.

---

## Inertial Stabilizer

**Legs · Passive (+ air brake) · Load 2/3/3/4/4 · Charge 5 per air brake, 10 per redirect**

*Identity:* **control at speed and in the air**: air strafing, elytra handling, traction on ice, and surviving wall
impacts. (Grounded turning is Inertia Compensation.)

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Air control (horizontal acceleration while airborne) | +25% | +50% | +75% | +100% | +150% |
| `fly_into_wall` damage reduction | 20% | 40% | 60% | 80% | 100% |
| Unlocks | — | **Traction:** normal friction on ice and slime (toggle) | **Elytra handling:** tighter turns, stall protection | **Air brake:** *Sneak* in the air kills horizontal momentum | **Redirect:** once per airtime, redirect the current velocity to the look direction |

**Limits:** no effect on the ground. Its value depends on how much time the user spends airborne (Hydraulic Legs,
elytra, Enhanced Tendons, Momentum Booster).

---

## Momentum Booster

**Legs · Active (release) · Load 3/3/4/4/5 · Charge — (fuelled by stored Momentum)**

*Identity:* **converts movement and mechanical energy into a burst**. It fills a Momentum gauge (0–100%) from
motion, and on release spends it as a dash plus a speed buff.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Dash at 100% Momentum | 6 blocks | 8 | 10 | 12 | 16 |
| Speed buff after release (at 100%) | +20% / 2 s | +25% / 3 s | +30% / 3 s | +40% / 4 s | +50% / 5 s |
| Momentum sources | sprinting | + falling | + **riding Create machinery**: standing on moving belts, contraptions and trains (gain scales with their speed) | + elytra flight speed; knockback received converts to Momentum | + Impact Dampeners Recoil Storage ×2 |
| Idle decay | 5%/s | 4%/s | 3%/s | 2%/s | 1%/s |

**Limits:** needs time to build up. A release always spends the whole gauge. Momentum decays while standing still.
Riding a fast Create train charges it, which rewards factory and rail infrastructure.

**Deck:** `momentum.value` (0–100), `ACTIVATE momentum` (release in the current facing direction). Event
`momentum_full`.
