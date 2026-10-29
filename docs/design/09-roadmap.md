# 9. Roadmap

Development proceeds in **vertical slices**. Each milestone produces a playable build that exercises the full
loop (manufacture → install → use → program) for a subset of content, before scope widens.

| Milestone | Scope | Exit criteria |
|-----------|-------|---------------|
| **M0 — Scaffold** | ModDevGradle project, Create dependency pinned, registries, datagen, CI build | Mod loads alongside Create 6 in dev; CI green |
| **M1 — Body** | `BodyState` attachment and sync, Cybernetic Core, Charge, Load/Strain, Implantation Rig, Kinetic Charger, Implant Wheel and HUD | Install and remove a Core on a powered Rig; Charge HUD works; state survives death and relog |
| **M2 — First implants (T1)** | One implant per category at T1, chosen to cover each hook type: Reinforced Skeleton (passive attribute), Kinetic Shield (damage pipeline), Enhanced Tendons (client movement), Entity Scanner (overlay and events), Biomonitor (vitals), Cybernetic Arm + Retractable Blade (host/module), Sandevistan T1 (temporal field) | Each works in survival with T1 recipes; GameTests for the damage pipeline and fields |
| **M3 — Cyberdeck + Synapse ISA-1/2** | VM, compiler, scheduler, editor UI, chassis + Processor/RAM/Storage modules, implant bindings | The §1 and §12.1 example programs run; watchdog and memory limits enforced; JUnit suite |
| **M4 — Create data link** | Neural Port, Machine Interface module, Machine Analyzer, Redstone Link channels, Remote Sensor Uplink, Proximity Sensor, Relay Mast | "Overspeed Guard" and "Stress Governor" programs run against real Create machines |
| **M5 — Manufacturing T1–T3** | Components, fluids, Precision Etcher, upgrade sequences to T3, JEI/EMI categories, Ponder scenes | A survival player can reach a full T3 body using only Create production |
| **M6 — Full catalog** | Remaining implants at T1–T3; ISA-3; Coprocessor; all T3 unlocks | Every implant in the catalog is functional to T3 |
| **M7 — Military & Experimental** | T4–T5 components and exotics, T4–T5 unlocks, ISA-4/5, display/train/logistics bridges | Full five-tier progression; logistics `REQUEST` works |
| **M8 — Balance & polish** | Balance GameTests, PvP options, accessibility, localisation, documentation (in-game guide and Ponder) | Archetype TTK/TTD inside target bands; no open crash bugs |

### Out of scope for 1.0 (candidates for later)

- A visual node editor as an alternative front-end that compiles to the same bytecode.
- Implant maintenance and degradation (would be an optional config).
- NPC "ripperdoc" villagers and trading of Data Shards.
- Integrations with other Create add-ons and energy mods, such as FE-powered charging.
