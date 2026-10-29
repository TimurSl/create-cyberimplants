# D. Cybernetic Limbs

**Region:** Left Arm and Right Arm (1 each). A **Cybernetic Arm** is a *frame*. Its tier sets its own passive
stats and how many **limb modules** it can hold. Modules are separate implants installed into a frame's sockets.
Module tier ≤ frame tier.

The **right arm** acts with the main hand and the **left arm** with the off hand (swapped for left-handed players).
A module's hand-specific behaviour, such as a blade deploying or a grip needing a free hand, follows its arm.

---

## Cybernetic Arm (frame)

**Arm · Passive · Load 3/4/4/5/6 (per arm) · Charge —**

*Identity:* mechanical strength and dexterity. It speeds up tool use, turns the empty hand into a tool, and hosts
modules.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Module sockets | 1 | 1 | 2 | 2 | 3 |
| Block break speed (this hand) | +10% | +20% | +30% | +40% | +60% |
| Interaction reach | +0 | +0.5 | +0.5 | +1.0 | +1.5 |
| Empty-hand harvest level | — | stone | iron | diamond | netherite |
| Unlocks | — | — | — | **Built-in wrench:** an empty hand can rotate and dismantle Create blocks, like the Wrench | **Haptic precision:** toggleable silk-touch for the empty hand |

**Bilateral bonus:** two frames of the same tier give +10% block break speed to both hands.

**Limits:** a frame is pure Load until modules are added. Frames are the gateway to the limb modules below.

---

## Remote Manipulator

**Arm module · Active · Load 3/3/4/4/5 · Charge 1 per action (or 20 backtank air) · Pneumatic**

*Identity:* **interact with blocks and things at a distance**. A tethered micro-arm extends from the wrist to the
looked-at target. Line of sight is required.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Range | 6 | 10 | 14 | 20 | 28 |
| Action cooldown | 0.75 s | 0.6 s | 0.5 s | 0.4 s | 0.3 s |
| Actions | *use* simple blocks (levers, buttons, doors, trapdoors) | + pull item entities and XP orbs to you | + Wrench actions and adjusting Create **value boxes** (scroll values) | + extract or insert one stack from a container | + **Grab & drag** non-boss entities ≤ 1 block wide |

**Limits:** every action fires the normal interaction events, so claim and protection mods apply as if the player
clicked the block in person. It cannot break or place blocks. The tether is visible and can be followed back to the
user.

**Deck:** `MANIPULATE <port|position> USE | WRENCH | SET_VALUE <n> | PULL`. LOS and range are checked at execution.
Event `manipulated(target)`.

---

## Retractable Blade

**Arm module · Toggle (deploy) · Load 3/4/4/5/6 · Charge: Lunge 5**

*Identity:* an **integrated melee weapon** that deploys from the forearm when that hand is empty.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Damage | 6 | 7 | 8 | 9 | 11 |
| Attack speed | 1.8 | 1.8 | 2.0 | 2.0 | 2.2 |
| Unlocks | — | Sweep attack | **Lunge:** sprint + attack leaps 4 blocks to the target | Armor piercing 25% | **Thermal edge:** sets targets on fire, armor piercing 40% |

**Dual blades** (both arms): hits alternate between hands, +15% attack speed.

**Limits:** it only works with an empty hand, so the user can't hold a tool or shield on that side. It can't be
enchanted. It counts as unarmed for Power Actuators, which gives a strong synergy.

**Deck:** `blade.deployed` (read/write), event `blade_hit(target)`.

---

## Microgrip System

**Arm module · Input-bound · Load 2/2/3/3/4 · Charge —**

*Identity:* **cling to and climb walls** with micro-hooked fingertips. Hold *Sneak* while pressed against a wall
to cling, then move in any direction along the surface.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Grip stamina | 3 s | 5 s | 8 s | 12 s | 20 s |
| Climb speed | 2.0 b/s | 2.5 b/s | 3.0 b/s | 3.5 b/s | 4.5 b/s |
| Unlocks | — | **Wall-jump:** kick off the wall while clinging | Grip on glass, ice and other smooth blocks | **Ledge vault:** auto-mantle onto ledges | **Ceiling hang:** cling to undersides (stamina drains ×1.5) |

Stamina refills at 2× speed while standing on the ground. Two Microgrip modules give +50% stamina.

**Limits:** the hand with the module must be **empty**. `#cyberimplants:ungrippable` surfaces (slime, honey,
powder snow and, below T3, glass and ice) can't be gripped. Taking damage while clinging drains 1 s of stamina.

---

## Ballistic Module

**Arm module · Active (aim and fire) · Load 3/4/4/5/6 · Charge 2 per shot (or 40 backtank air) · Pneumatic**

*Identity:* a **forearm-mounted launcher**. While the module's hand is empty, holding *Use* aims and releasing
fires.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Damage (Flechette) | 3 | 4 | 5 | 6 | 8 |
| Fire rate | 1.0/s | 1.25/s | 1.5/s | 2.0/s | 2.5/s |
| Internal magazine | 4 | 6 | 8 | 12 | 16 |
| Muzzle velocity | 2.0 | 2.5 | 3.0 | 3.5 | 4.0 b/t |
| Ammunition | Flechettes | + Incendiary Flechettes | + Concussive Flechettes (knockback burst, no block damage) | + **any Create Potato Cannon ammo**, using the same projectile type registry | + **Smart Flechettes**: light homing onto Target Tracking tags |

Reloading takes 2 s and draws ammo from inventory.

**Limits:** the hand must be empty. Uses Charge or air on every shot. There is no auto-fire from scripts (see
[safety rules](../04-synapse-language.md#9-safety-and-fairness-rules)). Damage falls off past 32 blocks.

**Deck:** `ballistic.ammo`, `ballistic.magazine`, `CONFIGURE ballistic ammo = "<type>"`. Scripts may choose ammo
and reload, but never aim or fire.
