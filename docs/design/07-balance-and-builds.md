# 7. Balance and Specialisation

## 7.1 Balancing levers

No implant should be universally best. Every implant is limited by several of these levers at once:

| Lever | Where it bites |
|-------|----------------|
| **Slot competition** | 2 Cortex, 2 Ocular, 2 Integument and 2 Legs slots for 5–6 candidates each |
| **Neural Load** | A full T5 body costs about 150 Load against a T5 Core's 100 Capacity |
| **Charge** | Active implants share one battery. Overspending can even make the Defibrillator fail. |
| **Cooldowns** | Long ones on the strongest effects (Sandevistan, Camo, Shell, Defib) |
| **Duration limits** | Short windows: Sandevistan, Kerenzikov, Adrenaline |
| **Conditions** | ERS danger inputs, AES conditions, Shell HP threshold, Regenerator combat lockout |
| **Hard counters** | Thermal vs Camo, ranged vs Shock Circuit, wrong family vs Nanocoating |
| **Physical constraints** | Free hands (Blade, Microgrip, Ballistic Module), metal surfaces (Magnetic Soles), stationary fields (Sandevistan) |
| **Anti-synergies** | Micro-Actuators servo whine vs stealth; Armor Plating weight vs speed; Dense frame vs swimming |
| **Processing limits** | Deck cycles, RAM, threads, ISA level, neural latency |
| **Production cost** | Each tier needs a bigger factory, and T5 needs rare resources |

## 7.2 Specialisations and example loadouts

Each loadout below is a **Tier III body** (Core T3: Capacity 60, overload margin to 69) built from T3 implants.
The totals show that all five archetypes are viable *and* that each one leaves something out.

### Combat Specialist — "Close the gap, end the fight"

| Region | Implants (Load) |
|--------|-----------------|
| Cortex | Combat Predictor (6), Kerenzikov (5) |
| Skeleton | Power Actuators (5), Reinforced Skeleton (4), Inertia Compensation (4) |
| Arms | 2× Cybernetic Arm (4 + 4), 2× Retractable Blade (4 + 4) |
| Ocular | Target Tracking (4), Ballistic Visor (3) |
| Circulatory | Adrenaline Pump (3), Regenerator (4) |
| Integument | Shock Circuit (4) |
| **Total** | **58 / 60** — no deck, no mobility, one subdermal slot empty |

*Play pattern:* tag a target, read its telegraph, dodge to open a Kerenzikov window, then land Power Strikes with
dual blades during the counter window. *Weakness:* kiting ranged enemies, and Camo users (no Thermal).

### Speed Specialist — "Be where they aren't"

| Region | Implants (Load) |
|--------|-----------------|
| Cortex | Sandevistan (11), Reflex Booster (5) |
| Skeleton | Micro-Actuators (4), Kinetic Tendons (3), Inertia Compensation (4) |
| Right arm | Cybernetic Arm (4) + Microgrip (3) |
| Ocular | Ballistic Visor (3) |
| Circulatory | Adrenaline Pump (3), Energy Buffer (3) |
| Integument | Kinetic Shield (5) |
| Legs | Enhanced Tendons (4), Momentum Booster (4) |
| **Total** | **56 / 60** |

*Play pattern:* build Momentum on rails and sprints, dash in, cast a Chronosphere on the group, and act inside it.
*Weakness:* the Sandevistan cooldown. Between casts the build is fragile (one subdermal layer, no Regenerator).

### Reconnaissance Specialist — "See everything, be seen by nothing"

| Region | Implants (Load) |
|--------|-----------------|
| Cortex | Automatic Evasion System (6) |
| Spine | Deck T3: chassis (6) + Processor, RAM, Storage (3) |
| Ocular | Optical Camouflage (7), Entity Scanner (4) |
| Skeleton | Impact Dampeners (3) |
| Right arm | Cybernetic Arm (4) + Microgrip (3) + Remote Manipulator (4) |
| Circulatory | Biomonitor (2) |
| Legs | Silent Actuators (3), Magnetic Soles (3) |
| **Total** | **48 / 60** — spare Load for an early T4 upgrade |

*Play pattern:* the deck runs the auto-tagging and auto-camo scripts (see [Synapse §12.3](04-synapse-language.md#123-recon-auto-tagger-lists-and-functions-isa-3)).
The player climbs, clings and works levers from range without being seen. *Weakness:* very little damage, and
Thermal Vision users counter Camo completely.

### Defensive Specialist — "Outlast"

| Region | Implants (Load) |
|--------|-----------------|
| Cortex | Automatic Evasion System (6) |
| Skeleton | Reinforced Skeleton (4), Inertia Compensation (4), Impact Dampeners (3) |
| Right arm | Cybernetic Arm (4) |
| Ocular | Ballistic Visor (3) |
| Circulatory | Regenerator (4), Emergency Response System (4), Emergency Defibrillator (5) |
| Integument | Armor Plating (4), Kinetic Shield (5) |
| **Total** | **46 / 60** |

*Play pattern:* ERS fires shield recharges before lethal hits land, AES handles arrows, Regenerator heals between
waves, and the Defibrillator is the backstop. *Weakness:* low damage and mobility. A Charge-hungry build that has
to manage the battery carefully.

### Cyberdeck Engineer — "The factory is my weapon"

| Region | Implants (Load) |
|--------|-----------------|
| Spine | Deck T3: chassis (6) + Processor, RAM, Storage, Machine Interface, Coprocessor (5) |
| Ocular | Machine Analyzer (3), Entity Scanner (4) |
| Cortex | Reflex Booster (5) |
| Skeleton | Micro-Actuators (4) |
| Right arm | Cybernetic Arm (4) + Remote Manipulator (4) |
| Circulatory | Biomonitor (2), Energy Buffer (3) |
| Integument | Kinetic Shield (5) |
| Legs | Magnetic Soles (3) |
| **Total** | **48 / 60** |

*Play pattern:* bind machines by looking at them, govern stress automatically, run base defences through Redstone
Links and Proximity Sensors, and request supplies through the logistics network at T5. *Weakness:* weak personal
combat. Its power depends on infrastructure and on being in range of it.

### Hybrid builds

The slot/Load model encourages hybrids. Typical pairs are Speed + Defence (Sandevistan with ERS T4 "panic chrono"),
Recon + Engineer (sensor networks), and Combat + Speed (Kerenzikov chains inside a Chronosphere).

## 7.3 Counterplay matrix

| Effect | Counters |
|--------|----------|
| Sandevistan field | Stay outside the radius or shoot in from range. Boss resistance. 30% PvP time-scale floor. Defenders' ERS/AES still trigger. |
| Optical Camouflage | Thermal Vision (any tier), Entity Scanner T5, the Warden, area damage, rain shimmer |
| Shock Circuit | Ranged attacks; Charge drain in long fights |
| Kinetic Shield | Fall/drowning/suffocation damage; sustained DPS during the recharge delay |
| Nanocoating | Mixed damage families; hitting during recalibration |
| AES | Melee; volleys larger than its charge stack; walls behind the target |
| Combat Predictor | Erratic movement; no aim assist in PvP by default |
| Entity Scanner | "You were scanned" warnings; Silent Actuators T5 (auto-scan); Camouflage |
| Target Tracking | Breaking range; "Tracked" warning for scanner users |
| Hydraulic Legs | Interrupting the audible, rooted charge-up |
| Emergency Defibrillator | Draining the target's Charge; burst within the post-revive window |
| Regenerator | Constant pressure (combat lockout) |
| Deck automation | Sensor denial (Camo, Silent); neural latency; Charge drain; cutting a Relay Mast's rotation |
| Magnetic Soles | Fighting away from metal |

## 7.4 PvP configuration

| Option | Default | Effect |
|--------|---------|--------|
| `pvp.sandevistanTimeScaleFloor` | 0.30 | Minimum time scale for players inside a field |
| `pvp.combatPredictorAimAssist` | false | Aim assist against players |
| `pvp.scanWarnings` | true | Warn players who are scanned or tracked |
| `pvp.controlEffectMultiplier` | 1.0 | Scales the duration of stuns, slows and freezes on players |
| `pvp.deckScriptsAffectPlayers` | true | Whether script-issued activations may target players (e.g. `ACTIVATE sandevistan AT player`) |

## 7.5 Tuning workflow

All stats live in data-driven files (`data/<namespace>/cyberimplants/implant_stats/<implant>.json`), and global
multipliers live in the server config. A modpack can rebalance or reskin progression without code changes.
Initial tuning should use scripted GameTests that check each specialisation's time-to-kill and time-to-death
against vanilla netherite gear (see [Technical architecture §8.11](08-technical-architecture.md#811-testing)).
