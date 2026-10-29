# 2. Implant Catalog

This catalog defines all 47 implant types: the Cybernetic Core, 40 implants across eight categories, and the 6 deck
modules. Each entry uses the same format:

> **Slot · Activation · Load T1–T5 · Charge** — header line
> *Identity* — the one thing this implant does that nothing else does
> **Tier table** — how the function grows (improves, expands, or becomes more efficient)
> **Limits** — the costs and counters that keep it from being universally best
> **Deck** — properties, actions and events it exposes to [Synapse](../04-synapse-language.md)

| Category | Region (slots) | Implants | File |
|----------|----------------|----------|------|
| A. Neural Systems | Cortex (2) | Sandevistan, Reflex Booster, Combat Predictor, Automatic Evasion System, Kerenzikov | [a-neural.md](a-neural.md) |
| B. Cyberdecks | Spine (1) + module sockets | Deck Chassis; Processor, RAM, Storage, Coprocessor, Machine Interface, Remote Sensor Uplink modules | [b-cyberdecks.md](b-cyberdecks.md) |
| C. Skeletal & Muscular | Skeleton (3) | Reinforced Skeleton, Power Actuators, Kinetic Tendons, Micro-Actuators, Impact Dampeners, Inertia Compensation | [c-skeletal-muscular.md](c-skeletal-muscular.md) |
| D. Cybernetic Limbs | Left/Right Arm (frame + 1–3 modules) | Cybernetic Arm; Remote Manipulator, Retractable Blade, Microgrip System, Ballistic Module | [d-limbs.md](d-limbs.md) |
| E. Optical | Ocular (2) | Entity Scanner, Thermal Vision, Machine Analyzer, Ballistic Visor, Optical Camouflage, Target Tracking | [e-optical.md](e-optical.md) |
| F. Circulatory & Vital | Circulatory (3) | Biomonitor, Regenerator, Adrenaline Pump, Emergency Defibrillator, Energy Buffer, Emergency Response System | [f-circulatory.md](f-circulatory.md) |
| G. Subdermal Armor | Integument (2) | Armor Plating, Nanocoating, Shock Circuit, Kinetic Shield, Emergency Shell | [g-subdermal.md](g-subdermal.md) |
| H. Mobility | Legs (2) | Enhanced Tendons, Hydraulic Legs, Silent Actuators, Magnetic Soles, Inertial Stabilizer, Momentum Booster | [h-mobility.md](h-mobility.md) |

## Telling similar implants apart

Several requested implants touch the same area (jumping, speed, momentum, surface adhesion, perception). Each one
is given a single, non-overlapping job:

| Area | Implant | Its unique job | What it explicitly does **not** do |
|------|---------|----------------|-----------------------------------|
| Jumping | Kinetic Tendons (C) | Passive: higher normal jumps, auto-step | No air jumps, no charged leap |
| | Enhanced Tendons (H) | Mid-air jumps (multi-jump, air dash) | No ground-jump boost |
| | Hydraulic Legs (H) | Charged directional leap and ground slam | No passive bonus, no air jumps |
| Speed | Reflex Booster (A) | *Action* speed: attack, bow draw, eating, shield raise | Only a small walk-speed bonus |
| | Micro-Actuators (C) | *Locomotion*: sprint speed, acceleration, climbing | No combat speed |
| | Kerenzikov (A) | Burst reaction window opened by a dodge | No passive bonus |
| | Adrenaline Pump (F) | Reactive speed after taking damage | No manual activation |
| | Momentum Booster (H) | Stores movement/mechanical energy, releases it as a dash | No passive bonus |
| Momentum | Inertia Compensation (C) | *Grounded* direction changes, keeps sprint, knockback resistance | No airborne control |
| | Inertial Stabilizer (H) | *Airborne/high-speed* control: air strafing, elytra, ice, wall impacts | No ground turning help |
| Falling | Impact Dampeners (C) | Fall damage reduction and energy recovery | — |
| Surfaces | Microgrip System (D) | Climb almost any surface with your hands (stamina-limited) | Hands must be free |
| | Magnetic Soles (H) | Walk on *metal* surfaces, anchor to moving contraptions | Metal only |
| Seeing entities | Entity Scanner (E) | *Identifies*: type, health, gear, threat (pulse) | No continuous view |
| | Thermal Vision (E) | *Sees* heat through walls continuously | No identity, undead are invisible |
| | Target Tracking (E) | *Persists* chosen targets and feeds them to the deck | No discovery |
| Prediction | Combat Predictor (A) | Predicts *entity* movement and attack telegraphs | No projectile paths |
| | Ballistic Visor (E) | Predicts *projectile* paths (incoming and your own) | No entity movement |
| Evasion | AES (A) | Automatic sidestep of *projectiles* under configured conditions | No melee, not manual |
| | Kerenzikov (A) | *Manual* dodge with a reaction window | Not automatic |
| Defence automation | Emergency Response System (F) | Hardwired, zero-latency, fixed-rule defensive triggers | Not programmable |
| | Cyberdeck (B) | Fully programmable, any implant | Has neural latency and needs sensors |
| Damage reduction | Armor Plating (G) | Armor points (vanilla formula; good vs many small hits) | — |
| | Nanocoating (G) | % reduction specialised by *damage type* | — |
| | Reinforced Skeleton (C) | Flat per-hit reduction vs *blunt* hits; resists *Create machinery* | No % vs magic/fire |
| | Kinetic Shield (G) | Rechargeable absorb pool | Doesn't stop fall/drowning |
| | Emergency Shell (G) | Short, huge reduction at critical HP | Slows you, long cooldown |
