# Create: Cybernetics — Design Overview

## 1. Vision

Create: Cybernetics turns the player's body into one more machine in the factory.

1. **Manufacture.** Every implant comes off a Create production line: pressing, cutting, mixing, deploying,
   sequenced assembly and a new kinetic machine, the Precision Etcher. Higher tiers need faster shafts, more fluids
   and longer assembly loops. In practice that means a bigger factory.
2. **Install.** Implants go into anatomical slots on a kinetic **Implantation Rig**. The player's **Neural
   Capacity** limits how much hardware they can carry, so they have to specialise.
3. **Program.** A **cyberdeck** runs scripts written in **Synapse**, a small event-driven language. Scripts react
   to what the player's implants sense, trigger other implants, and read and drive Create machinery through Neural
   Ports, Redstone Link channels and the Create 6 logistics network.

The defining feature is how the physical implants and the programmable automation work together. A deck has no
senses or effectors of its own. It is only as capable as the implants and machines it is wired to.

## 2. Design pillars

| Pillar | What it means in practice |
|--------|---------------------------|
| **Function over numbers** | Each implant changes *how* the player plays: a new input, a new information channel, a new reaction or a new risk. Pure stat boosts are limited to a few passive implants, and each of those also has a qualitative feature. |
| **One identity, five tiers** | An implant is one item with a `tier` component (I–V). Upgrading an implant improves its function or unlocks new capabilities. There is never a "Better Sandevistan" item. |
| **Built, not found** | Implants are never loot. Every tier is produced through Create processing. Tier IV–V need automated assembly lines, high-RPM kinetic networks and several fluid supplies. |
| **The deck amplifies, it does not replace** | Scripts may only do what installed implants and connected machines can already do. They pay the same costs, plus neural latency. Scripts never synthesise player inputs (aim, movement, attacks). |
| **Every strength has a price** | Each implant has at least one real constraint: Neural Load, Charge, cooldown, duration, slot competition, a hard counter, or an opportunity cost. |
| **Readable at a glance** | Every active effect has a clear visual and audio signature. This matters both for fairness in PvP and for Ponder-style teaching. |

## 3. Glossary

| Term | Definition |
|------|------------|
| **Implant** | An installable augmentation item. It has a type (e.g. `sandevistan`) and a tier (1–5). |
| **Slot / Region** | An anatomical location that holds implants (Cortex, Spine, Ocular, Skeleton, Arms, Circulatory, Integument, Legs). |
| **Cybernetic Core** | The mandatory spinal bus implant. Its tier sets Neural Capacity and battery size. |
| **Neural Load / Capacity** | Each implant costs Load. The total Load must fit inside the Core's Capacity (plus a small overload margin). |
| **Neural Strain** | The penalty state when Load exceeds Capacity: glitches, misfires and higher Charge costs. |
| **Charge (C)** | The internal energy that active implants consume. It is stored in the Core battery and, optionally, the Energy Buffer. |
| **Cooldown** | Per-implant recovery time. Cooldowns persist through logout and death. |
| **Cyberdeck / Deck** | A Spine implant (chassis) with module sockets. It runs Synapse programs. |
| **Module** | A deck component: Processor, RAM, Storage, Coprocessor, Machine Interface, Remote Sensor Uplink. |
| **Synapse** | The deck's scripting language. |
| **ISA level** | The set of language features a Processor tier supports (ISA-1 … ISA-5). |
| **Cycles** | The VM's unit of computation. Each Processor provides a per-tick cycle budget. |
| **Neural Port** | A block attached to a Create machine that exposes the machine's data and controls to decks. |
| **Relay Mast** | A kinetic-powered block that extends deck and port range. Its range scales with RPM. |
| **Neural latency** | The delay, in ticks, between a script issuing an activation and the implant firing. It is set by the deck chassis tier. Manual activation has no latency. |

## 4. Conventions in this specification

- Time is given in seconds (`s`) or ticks (`t`, 20 t = 1 s). Distances are in blocks. Health is in half-hearts (HP).
- Tier values are listed as `T1/T2/T3/T4/T5`.
- All numbers are **initial balance targets**. In the implementation they live in data-driven stat files and server
  config (see [Technical architecture](08-technical-architecture.md)), so they can be tuned without code changes.
- "Create" means Create 6.0.x for Minecraft 1.21.1. Create item names (Precision Mechanism, Electron Tube, Sturdy
  Sheet …) refer to that mod's existing items.

## 5. Document map

1. [Core systems](01-core-systems.md)
2. [Implant catalog](implants/README.md)
   - [A. Neural Systems](implants/a-neural.md)
   - [B. Cyberdecks (summary; full spec in 03)](implants/b-cyberdecks.md)
   - [C. Skeletal & Muscular](implants/c-skeletal-muscular.md)
   - [D. Cybernetic Limbs](implants/d-limbs.md)
   - [E. Optical](implants/e-optical.md)
   - [F. Circulatory & Vital](implants/f-circulatory.md)
   - [G. Subdermal Armor](implants/g-subdermal.md)
   - [H. Mobility](implants/h-mobility.md)
3. [Cyberdeck hardware](03-cyberdeck.md)
4. [Synapse scripting language](04-synapse-language.md)
5. [Create integration](05-create-integration.md)
6. [Progression & crafting](06-progression-and-crafting.md)
7. [Balance & builds](07-balance-and-builds.md)
8. [Technical architecture](08-technical-architecture.md)
9. [Roadmap](09-roadmap.md)
