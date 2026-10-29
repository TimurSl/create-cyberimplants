# 1. Core Systems

This document defines the rules every implant shares: where implants go, what limits how many a player can carry,
how they are powered, how they are installed, and how the player activates them.

---

## 1.1 Body regions and slots

| Region | Slots | Accepts | Notes |
|--------|-------|---------|-------|
| **Core** | 1 (mandatory) | Cybernetic Core | Must be installed before anything else. Defines Capacity and battery. |
| **Cortex** | 2 | A. Neural Systems | |
| **Spine** | 1 | B. Cyberdeck chassis | The deck has its own internal module sockets. |
| **Ocular** | 2 | E. Optical implants | |
| **Skeleton** | 3 | C. Skeletal & Muscular | |
| **Left Arm / Right Arm** | 1 each | D. Cybernetic Arm frame | Each frame has 1–3 sockets for limb modules. |
| **Circulatory** | 3 | F. Circulatory & Vital | |
| **Integument** | 2 | G. Subdermal Armor | |
| **Legs** | 2 | H. Mobility Systems | |

Rules:

- **No duplicates within a region.** For example, two Regenerators cannot be installed. Arm modules are the
  exception: the same module may sit in *both* arms, and some modules gain a bilateral bonus from that (see
  [D. Limbs](implants/d-limbs.md)).
- **Host tier caps child tier.** A limb module's tier cannot exceed its arm frame's tier, and a deck module's tier
  cannot exceed its chassis tier.
- The slot counts are deliberately lower than the number of implants in each category. A player chooses 2 of 5
  Neural implants and 2 of 6 Optical implants. Most specialisation comes from this choice.

## 1.2 Cybernetic Core

Every cyborg starts with a Cybernetic Core, the spinal bus that hosts all other hardware.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Neural Capacity | 30 | 45 | 60 | 80 | 100 |
| Overload margin | +10% | +10% | +15% | +15% | +20% |
| Battery (C) | 100 | 200 | 350 | 500 | 750 |
| Metabolic recharge | 0.25 C/s | 0.4 C/s | 0.6 C/s | 0.8 C/s | 1.0 C/s |

Upgrading the Core is the main way to fit more hardware. A T5 Core still cannot hold a full set of T5 implants.
The full slot set at T5 costs roughly 150 Load against 100 Capacity.

## 1.3 Neural Load, Capacity and Strain

Each implant lists its Load per tier, for example `load 9/10/11/12/14`. Deck modules cost **+1 Load each**,
regardless of tier.

| Total Load | State | Effect |
|-----------|-------|--------|
| ≤ Capacity | **Stable** | None. |
| ≤ Capacity × (1 + margin) | **Strained** | Every 60–120 s a *glitch* fires. Glitches knock one random active implant offline for 3–5 s, or briefly add HUD static. All Charge costs are +20%. |
| > Capacity × (1 + margin) | **Rejected** | The Implantation Rig refuses the operation. |

**Neuro-Stabilizer** is a consumable bottle (mixed and spout-filled; see [crafting](06-progression-and-crafting.md)).
It suppresses Strain glitches for 10 minutes and gives *Nausea* for its first 5 s. Strain is therefore manageable
for a big fight but expensive to sustain.

## 1.4 Charge (energy)

Charge is the shared energy that active and toggled implants consume. Passive stat implants are free unless their
entry says otherwise.

### Sources

| Source | Rate | Notes |
|--------|------|-------|
| **Metabolic conversion** (Core) | 0.25–1.0 C/s | Converts food: 1 C costs 0.1 exhaustion (≈ 40 C per hunger point). It stops when hunger ≤ 6. |
| **Kinetic Charger** (block) | \|RPM\| / 8 C/s | The player stands on the pad. At 256 RPM this is 32 C/s. See [Create integration](05-create-integration.md). |
| **Charge Cell** (item) | 100 C per cell | A rechargeable battery. Filled on the Kinetic Charger, discharged by using it (1 s). |
| **Impact Dampeners T2+** | 1–4 C per block of absorbed fall | Recovers energy from landings. |
| **Shock Circuit T5** | Full refill | When the player is struck by lightning. |

### Sinks

- Activation costs (one-off), upkeep costs (per second while toggled on), per-proc costs (e.g. Shock Circuit) and
  deck computation (1 C per 2,000 cycles).
- If Charge is too low for an activation, the activation fails with an audible fizzle. No Charge is spent and no
  cooldown starts.

### Pneumatic alternative (Create backtank)

Implants tagged **pneumatic** may draw pressurised air from a worn Create backtank instead of Charge. The tagged
implants are Hydraulic Legs, Ballistic Module and Remote Manipulator. The player toggles this per implant in the
implant wheel. Air is used at the rate listed in each entry. This uses the same air that the Extendo Grip and Potato
Cannon consume, so a pneumatic build depends on air compressors in the base.

### Energy Buffer

The [Energy Buffer](implants/f-circulatory.md#energy-buffer) is a second, emergency-only pool. It only powers
activations flagged **Emergency**: Emergency Defibrillator, Emergency Shell, any activation issued by the Emergency
Response System, and Kinetic Shield emergency recharge.

### Configuration

The server config `charge.enabled = false` turns off the energy system for packs that don't want it. All Charge
costs become 0, and every active cooldown is multiplied by `charge.disabledCooldownMultiplier` (default 1.5).

## 1.5 Installation: the Implantation Rig

Implants are installed, removed and swapped on an **Implantation Rig**. This is a two-block-tall kinetic machine:
a surgical chair with an articulated arm head. Full block spec is in
[Create integration](05-create-integration.md#implantation-rig).

**Procedure**

1. The player sits in the Rig and opens the body-diagram UI. Alternatively, another player can operate it on them.
2. They drag an implant into a valid slot, or take one out.
3. The Rig checks: Core installed, slot rules, host-tier caps, and Load ≤ Capacity × (1 + margin).
4. The operation runs for **5/8/12/16/20 s** (by implant tier). It consumes **250 mB Bio-Interface Gel** for T1–T3
   or **250 mB Neural Gel** for T4–T5, and needs a minimum shaft speed of **16/32/64/128/256 RPM**.
5. If rotation drops below the minimum, the gel runs out, or the patient stands up mid-operation, the operation
   **aborts**. The patient takes 4 HP damage and the implant is returned to them.

*As implemented:* the Rig is a one-block surgical stool (gearbox and shaft at the back). When an operation starts it
straps the patient onto the cushion (an invisible, locked seat entity): movement input is dropped on the client and the
server refuses the dismount, so "standing up" can only happen through outside means such as an ender pearl, which
aborts. Dying or disconnecting always frees the patient, and the seat is never saved. The screen is a 3D skeleton
hologram with a node per slot, an animated preview and stats comparison of the implant in focus, and an operation
monitor; the block entity syncs the running operation, gel and speed to clients, and its renderer draws a scanner ring
and laser emitters around the patient.

Removal returns the implant item with its tier intact. Per-implant configuration (Nanocoating formula, AES
conditions, etc.) is stored on the player, not on the item, so swapping hardware does not erase settings.

**Design intent.** Surgery is a deliberate, base-bound act. Players cannot swap loadouts in the field, and higher
tiers need a stronger kinetic network just to install.

## 1.6 Implant activation types

| Type | Behaviour | Example |
|------|-----------|---------|
| **Passive** | Always on. No input. Usually no Charge cost. | Reinforced Skeleton |
| **Toggle** | On/off. Drains Charge per second while on. | Thermal Vision |
| **Active** | Fired on demand. Has activation cost and cooldown. | Sandevistan |
| **Input-bound** | Grants a new movement or combat input. | Enhanced Tendons (air jump) |
| **Triggered** | Fires automatically when its built-in condition is met. | Emergency Defibrillator |

### Controls

- **Implant Wheel** (hold `R` by default): a radial menu of every Active and Toggle implant installed. Releasing on
  an entry fires or toggles it.
- **Quick Slots 1–4** (unbound by default): direct keybinds for up to four chosen Active/Toggle implants.
- **Deck Macro Keys 1–4** (unbound by default): send `key(n)` events to deck programs (see [Synapse](04-synapse-language.md)).
- **Movement inputs** reuse vanilla keys. Air jump is *Jump* while airborne. Dodge is a double-tap of a direction
  key, or the *Dodge* keybind. Wall cling is *Sneak* while pressed against a wall. Charged leap is *hold Jump* while
  sneaking.

## 1.7 HUD

- **Charge bar** under the hotbar, with the Energy Buffer shown as a separate segment.
- **Load meter** (Stable / Strained). It is only visible in the Implant Wheel and the Rig UI.
- **Cooldown strip**: icons for every Active implant, with a radial cooldown sweep.
- **Deck status lamp**: green when programs are running, amber when a watchdog throttled a program, red when a
  program crashed. Hovering in the deck UI shows details.
- Overlays from Optical implants: markers, outlines, trajectories, machine readouts. Each has a client-config
  opacity setting.

## 1.8 Death, logout and dimensions

- Installed implants **persist through death** (default `implants.keepOnDeath = true`). The hardcore-flavoured
  option `implants.dropOnDeath = random_one` drops one random non-Core implant.
- On respawn, Charge is set to 25%. Cooldowns are kept, so dying does not reset the Defibrillator.
- Deck programs **reboot** on login and respawn: they restart from `ON boot` and only keep `PERSIST` variables.
- Temporal fields, tags and other transient effects end on dimension change.

## 1.9 Rules shared by all implants

1. **Manual and scripted use cost the same.** A script pays the same Charge, cooldown and range limits as a manual
   activation, plus neural latency.
2. **Cooldowns start when an effect ends**, not when it starts. This applies to Sandevistan, Optical Camouflage and
   Emergency Shell.
3. **PvP floor.** Effects that remove control (freeze, stun, silence) have a minimum effect on players, set by
   config. For example, Sandevistan's PvP time scale floor defaults to 30%.
4. **Boss resistance.** Entities in `#cyberimplants:augment_resistant` (Wither, Ender Dragon, Warden, Elder
   Guardian by default) take half-strength control effects.
5. **Every effect is visible.** Each active effect has a distinctive particle, sound and, where relevant, a
   world-space shape that other players can see.
