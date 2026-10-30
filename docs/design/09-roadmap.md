# 9. Implementation Layers

The add-on is built in **layers of complexity**, not in content milestones. Every layer touches **all** content:
all eight implant categories, the cyberdeck, the Create machinery and the crafting chains. Each successive layer
makes all of it deeper.

> "Layer" is not "Tier". A *Tier* (I–V) is an in-game technology level that players progress through. A *Layer*
> is a stage of implementation depth. Layer 2 is where all five tiers first exist. Layer 5 is where Tier V content
> is finished.

## 9.1 Rules for layering

1. **Complete breadth at every layer.** From Layer 1 onward, every implant in the [catalog](implants/README.md) exists
   and works. No layer ships "half the categories".
2. **Playable at every layer.** Each layer ends in a coherent build with its own exit criteria (§9.4). A layer
   never leaves content half-wired.
3. **Layers add depth, not rewrites.** The data model is designed for the final layer from the start. Stat files
   hold all five tiers and every unlock flag in the Layer 1 schema, and the Synapse API descriptors list every
   member. Early layers leave deeper fields switched off or filled with simple values.
4. **Each layer has a config cap.** The server option `layers.maxEnabled` (1–5) disables everything above that
   layer: stats fall back to the previous layer's behaviour, higher-layer recipes are hidden, and higher-layer
   language features don't compile. This lets modpacks ship a simpler version and gives us a safe way to bisect
   regressions.
5. **Recipes are replaced, not stacked.** A layer's recipe for an item replaces the earlier layer's placeholder
   recipe of the same item.

## 9.2 Layer overview

| Layer | Name | Depth added | Content breadth |
|-------|------|-------------|-----------------|
| **0** | Foundation | Infrastructure only, no player-facing content | — |
| **1** | **Function** | Each implant does its single core job | All implants, Tier I |
| **2** | **Scaling** | The five tiers exist and stats scale; the upgrade chain exists; the deck becomes loops and device I/O | All implants, Tiers I–V (numbers only) |
| **3** | **Capability** | Tier-gated unlock features, configuration, custom components and fluids, full machine data link | All implants, Tiers I–IV features |
| **4** | **Interplay** | Costs, counters, synergies, automation between systems, PvP rules | All implants, interactions |
| **5** | **Mastery** | Tier V experimental content, top-end language and logistics, balance, teaching and polish | All content, final |

## 9.3 Layer contents

### Layer 0 — Foundation

Infrastructure only. No implant is playable yet.

- NeoForge project (ModDevGradle), Create dependency pinned, CI build
- Registries: `ImplantType`, items, blocks, block entities, fluids, data components, recipe types, attachment types
- `BodyState` attachment with sync payloads
- Data pipeline for stats (`implant_stats/*.json`, all five tiers in the schema), tags and the config skeleton
- Synapse project skeleton: lexer/parser/VM packages with JUnit harness (no game bindings)
- `compat/create` package with a stub for each bridge

*Status:* implemented in the repository (see the README). The Synapse lexer and its test suite are included; the
parser and VM are Layer 1. NeoForge-dependent code has not yet been compiled against NeoForge or Create.

### Layer 1 — Function

*Goal: every implant does the one thing that defines it, in its simplest form.*

| Area | Layer 1 scope |
|------|---------------|
| **Body** | Slots and regions; Cybernetic Core with a single tier; Load is a hard cap (the Rig refuses overflow); Charge as a single pool with metabolic recharge |
| **Installation** | Implantation Rig installs and removes. No abort conditions, no gel, no RPM check. Rotation just needs to be nonzero. |
| **A. Neural** | Sandevistan (stationary field with a fixed slow, no unlocks) · Reflex Booster (attack and use speed) · Combat Predictor (lead indicator only) · AES (dodges arrows, fixed rule) · Kerenzikov (dodge + window) |
| **B. Cyberdeck** | Chassis, Processor, RAM, Storage. **ISA-1**: `ON`, `IF`, variables, `ACTIVATE`, `LOG`. Implant-side events only. One program at a time. |
| **C. Skeletal** | All six, as attribute or damage-pipeline effects at base values |
| **D. Limbs** | Cybernetic Arm frame with one module socket · Retractable Blade · Remote Manipulator (use blocks only) · Microgrip (climb) · Ballistic Module (standard flechette) |
| **E. Optical** | Entity Scanner (pulse, type + threat) · Thermal Vision · Machine Analyzer (goggle info) · Ballistic Visor (incoming arcs) · Optical Camouflage · Target Tracking (one tag) |
| **F. Circulatory** | Biomonitor (vitals HUD, health events) · Regenerator · Adrenaline Pump · Defibrillator · Energy Buffer · ERS (health-threshold trigger only) |
| **G. Subdermal** | Armor Plating · Nanocoating (balanced formula only) · Shock Circuit · Kinetic Shield · Emergency Shell |
| **H. Mobility** | Enhanced Tendons (single air jump) · Hydraulic Legs (charged leap) · Silent Actuators (footsteps) · Magnetic Soles (wall-walk) · Inertial Stabilizer (air control) · Momentum Booster (sprint-fuelled dash) |
| **Create** | Rig and Kinetic Charger. Neural Port is read-only for rotation speed. |
| **Crafting** | One placeholder recipe per implant from vanilla and basic Create items (Mechanical Crafters). No tiers. |
| **UI** | Implant Wheel, Charge bar, Rig body screen, basic text editor for decks |

*Status:* implemented in the repository. The Synapse parser, compiler and VM (ISA-1) are unit-tested
(`./scripts/test-synapse.sh`). The NeoForge-dependent code compiles against Minecraft 1.21.1, NeoForge 21.1.172 and Create 6.0.8 (verified with
`./gradlew build` on CI) and the Tier-free unit tests pass, but it has **never been run**: no GameTest, client or
server session has executed it yet, so expect gameplay bugs. The Layer 1 GameTests exist but CI does not run them.
Deliberate Layer 1 simplifications:

- The Rig is a single block opened by right-click (no chair, no two-block form); operations are instant.
- Movement abilities are applied by the client and validated by the server for Charge, cooldown and air-jump
  counts only; there is no position validation yet (Layer 4).
- Sandevistan skips ticks on the server only; clients still extrapolate frozen entities.
- Optical overlays use vanilla glow outlines and particles; there is no custom world rendering yet.
- The Ballistic Module fires a vanilla arrow entity named for its damage; it has its own key (`G`) instead of
  the hold-use interaction.
- Every implant has stat files for five tiers, but tiers II-V repeat the Tier I numbers (only Load differs).
- Deck: Chassis, Processor, RAM and Storage are installed into three module sockets through the Rig; the editor
  is a single screen without tabs. Coprocessor, Machine Interface and Remote Sensor Uplink arrive in Layers 2-3.
- Neural Port (rotation read-only): the chassis has the Machine Interface's Tier I limits built in (2 bound ports,
  16 blocks). Right-click your own port to bind or unbind it; name it by renaming the item in an anvil before
  placing. Scripts declare it with `DEVICE press = port "Name"` and read `press.speed`, `press.abs_speed`,
  `press.online`; `ON rotational_speed_changed FROM press` fires when the speed changes.

*Exit criteria:* a survival player can build a Core and at least one implant per category and use each one's core
ability. Example program 1 in [Synapse §1](04-synapse-language.md#1-a-first-look) runs. Damage-pipeline and
Sandevistan-field GameTests pass.

### Layer 2 — Scaling

*Goal: the five-tier progression exists for everything. Tiers change numbers, and the upgrade path is complete.*

| Area | Layer 2 scope |
|------|---------------|
| **Body** | Core Tiers I–V (Capacity, battery, recharge). Overload margin and **Strain** with glitches. Cooldowns persist through death and logout. |
| **Installation** | Gel consumption, RPM requirements by tier, abort conditions, a 5–20 s operation time |
| **All implants** | **Every table's numeric rows** for Tiers I–V (radius, duration, cooldown, damage, range, charges, capacity …). Unlock rows stay off. |
| **B. Cyberdeck** | Tiers I–V for chassis and modules; **Coprocessor** and parallel programs; **ISA-2**: loops, `WAIT`, `EVERY`, `DEBOUNCE`, `CONFIGURE`, device writes; neural latency; cycle and RAM enforcement; watchdog |
| **D. Limbs** | Arm frames with 1–3 sockets; bilateral bonus |
| **Create** | Neural Port with Machine Interface Tiers I–II (rotation, stress, redstone out). Kinetic Charger rates scale with RPM. Proximity Sensor. |
| **Crafting** | A complete **I → V upgrade chain** for every implant using **standard Create machines only** (press, saw, mixer, deployer, spout, sequenced assembly) and placeholder items. Production cost scales by loop count. |
| **UI** | Deck tabs (Programs, Processes, Log), cooldown strip, Load meter |

*Status:* implemented in the repository, **not yet run in a game** (CI compiled it with `./gradlew build`, and the unit tests including the stat audit pass;
the GameTests exist but CI does not run them). What is in:

- **Data:** `scripts/layer2_tiers.py` holds the numeric rows of every implant table for Tiers I-V; the stat files are
  generated from it. Unlock rows stay empty. Where a table row mixes a number with an unlock (Sandevistan's 0% time
  scale at T3 is the "Full freeze" unlock) the Layer 2 value is the last non-unlock number and the unlocked one sits
  under a separate key (`freeze_time_scale`). `StatAuditTest` checks five tiers per implant, equal key sets, no Tier I
  copies, and an upgrade recipe per tier.
- **Body:** Core tiers (already in Layer 1), Strain (+20% Charge cost, a glitch every 60-120 s that knocks one
  Active/Toggle implant offline for 3-5 s or flickers the HUD), the Neuro-Stabilizer item (10 minutes without
  glitches), cooldowns that count in online ticks and survive death and logout.
- **Rig:** timed operations (5/8/12/16/20 s), minimum RPM 16/32/64/128/256, 250 mB of Bio-Interface Gel (T1-T3) or
  Neural Gel (T4-T5) from a 1,000 mB tank (pipes, buckets), aborts when the Rig slows, the gel runs out or the
  patient moves (4 HP of damage, the implant stays where it was), comparator output = progress, a redstone signal
  locks the chair.
- **Hosts:** the chassis tier sets 3-8 deck sockets, the arm frame tier 1-3 limb sockets; RAM and Storage stack;
  Coprocessors add threads (capped per chassis tier). Bilateral frames add +10% break speed.
- **Deck:** ISA-2 (`WHILE`, `REPEAT`, `BREAK`, `CONTINUE`, `WAIT`, `EVERY`, `DEBOUNCE`, `CONFIGURE`, `SEND`, `READ`,
  `MANIPULATE`, string functions), sleeping tasks, RAM enforcement per task, a watchdog that kills a task that
  runs 20x its budget without a `WAIT`, the Processor's minimum `EVERY` interval, neural latency for scripted
  activations, parallel programs per thread, tabs for Programs, Processes and Log.
- **Create:** Machine Interface Tiers I-II (more bound ports and range, stress and redstone reads, redstone out on a
  Neural Port), the Kinetic Charger's `|RPM| / 8` rate, the Proximity Sensor (redstone output).
- **Crafting:** a sequenced-assembly upgrade chain for every implant from Tier I to V, built from placeholder parts and
  standard Create machines (1/2/3/4 loops), plus the two gel recipes. Layer 3 replaces the placeholder parts.

Deliberate Layer 2 simplifications:

- Neural latency delays the whole activation, so the script sees an optimistic `TRUE` and a later refusal only lands in
  `last_error` (cost and cooldown are not reserved up front).
- The Rig's operation is not persisted: unloading the chunk mid-operation cancels it without damage. The patient fade
  and the two-block chair are still missing.
- `CHANNEL`, `SENSOR` and `TRANSMIT` need the Remote Sensor Uplink and stay rejected. `MANIPULATE` supports `USE` and `PULL`;
  `WRENCH` and `SET_VALUE` arrive with Tier III.
- Processes tab shows each thread's state and cycles; pause and restart buttons are not there (unload kills).
- The Neuro-Stabilizer's real recipe (mix, then spout) arrives with Layer 3; Layer 2 spouts a bottle with Neural Gel.
- Machine Interface Tier II's write access is the redstone output of a Neural Port; the Relay Mast is Layer 3.

*Exit criteria:* every implant can be built and upgraded to Tier V. A stat audit test confirms every implant has
data for all five tiers. "Overspeed Guard" ([Synapse §1](04-synapse-language.md#1-a-first-look)) runs against a
real Create machine. Strain, Rig aborts and the watchdog behave as specified.

### Layer 3 — Capability

*Goal: tiers change what implants can do. Tier-gated functions, configuration, and the real manufacturing
chain arrive.*

| Area | Layer 3 scope |
|------|---------------|
| **Unlock features** | Every **"Unlocks"** row for Tiers II–IV. Examples: Sandevistan full freeze and Kinetic Debt, Reflex Booster Overdrive, Entity Scanner info cards and auto-scan, Thermal heat sources, Microgrip wall-jump and ledge vault, Regenerator Burst, Nanocoating field formulas, Hydraulic Legs ground slam, Magnetic Soles ceiling walk |
| **Configuration** | AES condition slots, Nanocoating formulas, Biomonitor alarms, ERS inputs, Remote Manipulator actions, Ballistic ammo types, per-implant config screen |
| **B. Cyberdeck** | **ISA-3**: functions, lists, `FOR`, `EMIT`/`ON message`, `BIND`; **ISA-4**: records, math, `TRY` · full event catalog · Devices and Implants tabs · Data Shards and Programming Terminal · Remote Sensor Uplink |
| **E. Optical** | Machine Analyzer overlays and virtual ports; Target Tracking auto-tag |
| **Create** | Machine Interface Tiers III–IV (value boxes, filters, inventories, fluids, Display Link reads, display targets, trains) · **Relay Mast** · **Redstone Link channels** · Biomonitor broadcast · pneumatic (backtank) implants · Magnetic Soles anchoring |
| **Crafting** | **Replace placeholders with the real chain**: Precision Etcher, the five fluids, Silica/Silicon/Logic Chip/Myomer/Composite Plate/Military Processor. Tier III needs a ≥ 64 RPM etch line and Tier IV a ≥ 128 RPM one ([Progression §6.3](06-progression-and-crafting.md#63-shared-components-by-tier)). |
| **UI** | Full Synapse editor (highlighting, lints, footprint), implant config screens, JEI/EMI categories |

*Status:* **started**, not finished. Like Layers 1 and 2 this has only been compiled by CI, never run in a game. In so far:

- **Unlock framework:** `scripts/unlocks.py` lists the unlock rows that exist in code; the stat files carry them
  cumulatively per tier (Tier I never has any) and register each one under `unlock_layers` as layer 3, so
  `layers.maxEnabled < 3` switches them off. Code asks `Stats.unlocked(player | id + tier, "unlock_id")`. Item tooltips list
  the unlocks of the stack's tier. `StatAuditTest` checks that the rows are cumulative and registered.
- **Unlocks implemented (11 of the roughly 60 rows):** Regenerator (works while starving, cures Poison, cures Wither and
  Hunger), Adrenaline Pump (ignores Slowness, boost jump, boost attack speed), Shock Circuit (slows the attacker, chains to
  one more enemy), Kinetic Shield (projectiles fully absorbed), Emergency Shell (knockback immunity, cleanse and extinguish).
  `Layer3Tests` covers the tier gating, the Shield and the Shell.
- **Look and feel (also for the later layers):** every item, block and gel fluid has a hand-drawn neon-on-steel texture
  (`scripts/gen_textures.py`, called by `gen_data.py`), and the Rig, Deck and Implant Wheel screens and the HUD share the
  cyberpunk style of `client/CyberUi.java` (glass panels, neon edges, scanlines, `CyberButton`, `meter`).

Still to do in Layer 3: the remaining unlock rows (Sandevistan, Reflex Booster, Entity Scanner, Thermal Vision, Microgrip, the
mobility and limb unlocks ...), configuration screens, ISA-3/4, Machine Interface Tiers III-IV, Relay Mast, Redstone Link
channels, the Precision Etcher and the real manufacturing chain, the full editor and JEI/EMI categories.

*Exit criteria:* a survival player reaches a full Tier IV body using the real Create production chain. The
"Stress Governor", "Hunter" and "Panic Protocol" programs run. All Tier II–IV unlock rows are covered by GameTests.

### Layer 4 — Interplay

*Goal: systems interact with each other and with other players. Strengths get their costs, and counters work.*

| Area | Layer 4 scope |
|------|---------------|
| **Limitations** | Every **Limits** line enforced: free hands, metal-only, Temporal Backlash, Charge drain, servo whine vs stealth, Dense frame, recalibration windows, Shock Circuit charging creepers |
| **Counterplay** | Scan and track warnings; Thermal vs Camo; boss resistance tag; hard counters in the [matrix](07-balance-and-builds.md#73-counterplay-matrix) |
| **Cross-implant synergies** | AES + Kerenzikov windows; Visor detection bonus; Power Actuators + Blade; Impact Dampeners → Momentum Booster; Combat Predictor + Target Tracking; Silent Actuators ghost step vs Entity Scanner |
| **Emergency system** | Emergency-flagged activations, Energy Buffer rules, **ERS** full danger model with pre-damage reaction, Shell and Shield emergency paths, Defibrillator priority vs Totem |
| **Deck arbitration** | Program `PRIORITY`, implant arbitration (`BUSY`), event coalescing, per-server cycle caps, ownership and claim checks on device writes |
| **Create** | Momentum Booster charging from belts, contraptions and trains · Relay Mast RPM → range with network rebuild · Magnetic Soles on moving contraptions · Machine Analyzer stress tracing |
| **PvP and server** | [PvP options](07-balance-and-builds.md#74-pvp-configuration), team resolution (scoreboard, FTB Teams optional), movement-validation hardening |
| **Balance pass 1** | Archetype loadouts built in GameTests; first tuning of cooldowns, Load and Charge costs |

*Status:* **started**, not finished. Like the earlier layers this is only compiled by CI, never run in a game. All of it is off under
`layers.maxEnabled < 4`. In so far:

- **Limits:** *Dense frame* (Reinforced Skeleton Tier III+ gets +5% gravity, so the user also sinks in water; the separate
  -15% swim speed is not done) and *Servo whine* (sprinting Micro-Actuators Tier I-III footsteps are 1.5x louder, cancelled
  by Silent Actuators Tier IV+). Shock Circuit charging creepers has existed since Layer 1. Both rules live in `implant/Limits.java`.
- **Synergies** (`implant/Synergies.java`): an AES evasion opens a Kerenzikov window for free; Momentum Booster Tier II+ gains
  Momentum from falls (5 per block beyond 3), x1.5 with Impact Dampeners installed. Power Actuators + Retractable Blade already
  works because the blade counts as an empty hand. The Ballistic Visor adds 1 tick to the AES detection window, and the Combat
  Predictor's lead indicator follows the Target Tracking tag instead of the crosshair. Silent Actuators' ghost step vs the Entity
  Scanner waits for the scanner's auto-scan (Layer 3).
- `Layer4Tests` covers the limits and the AES, Momentum and Visor synergies.

Still to do in Layer 4: the other Limits (free hands, metal-only, Temporal Backlash, Charge drain, recalibration windows,
the Magnetic Soles jump penalty), counterplay and scan warnings, the remaining synergies, the emergency system and ERS danger
model, deck arbitration, the Create interplay, PvP options and team resolution, and the balance pass.

*Exit criteria:* the five example loadouts in [Balance §7.2](07-balance-and-builds.md#72-specialisations-and-example-loadouts)
can be assembled and each has a measured weakness against its counter. Multiplayer tests (two clients) cover
scanning, tracking, Sandevistan fields and ERS reactions without desyncs.

### Layer 5 — Mastery

*Goal: Tier V, the top end of the language, and everything that makes the mod teachable and finished.*

| Area | Layer 5 scope |
|------|---------------|
| **Tier V** | Every **Tier V unlock**: Sandevistan Temporal Anchor, Camo Re-cloak, Entity Scanner implant reading, Thermal heat trails, Regenerator Burst tuning, Shock Circuit Grounded, Momentum Booster Recoil Storage ×2 and others |
| **Experimental crafting** | Echo Lattice, Experimental Core, Quantum Substrate, Phase Lens (haunting), per-category exotics, 256 RPM etch lines, Neural Gel |
| **Language** | **ISA-5**: `AWAIT ANY`, `PERSIST`, `OVERCLOCK`, `REQUEST`, long-horizon `predict.*` · Processor Overclock |
| **Logistics** | Machine Interface Tier V: Create 6 stock reads and package requests (`LogisticsBridge`, the most volatile API, so it is built last) |
| **Teaching** | Ponder scenes for every new block; an in-game guide; example programs on Data Shards; tooltips that show tier unlocks |
| **Polish** | Accessibility options, localisation, performance profiling against the [budgets](08-technical-architecture.md#813-performance-budgets), crash and fuzz hardening, **final balance pass** |

*Status:* **implemented** in the repository: every Layer 5 area has its code, but like the earlier layers it is only
compiled and unit-tested by CI and has **never been run in a game**, so the exit criteria below are not verified yet. All
of it is off under `layers.maxEnabled < 5`, except the recipes (data packs replace them, rule 5). What is in:

- **Data generator:** `scripts/gen_data.py` drives `implants.py` (the catalogue), `layer2_tiers.py` (numbers),
  `unlocks.py` (every unlock row with its first tier *and its layer*: Tier II-IV rows are Layer 3, Tier V rows Layer 5),
  `crafting.py` (components, fluids, tags, recipes) and the Ponder schematics. `StatAuditTest` checks that a row's layer
  matches its first tier.
- **Every Tier V unlock row (26 rows, 25 implants):** Sandevistan *Temporal Anchor*, Combat Predictor *Precog* (a ghost
  where each tracked or looked-at target will be after the horizon), AES *Parry*, Reinforced Skeleton crit immunity,
  Kinetic Tendons *Elastic Recoil*, Impact Dampeners *Recoil Storage*, Inertia Compensation -50% explosion knockback,
  Cybernetic Arm *Haptic precision* (a wheel toggle: the empty hand mines with silk touch), Retractable Blade *Thermal
  edge*, Microgrip *Ceiling hang*, Entity Scanner *implant reading* (other players' category letters; breaks Optical
  Camouflage up to Tier IV), Thermal Vision *heat trails* (10 s footprints), Camo *Re-cloak*, Target Tracking *Tracer
  tagging*, Regenerator *Burst*, Adrenaline Pump *Last Stand*, Energy Buffer *Surge*, Shock Circuit chain to 3 and
  *Grounded*, Kinetic Shield *Overcharge*, Emergency Shell manual activation, Enhanced Tendons *Gust*, Hydraulic Legs
  *Rebound*, Silent Actuators *Ghost step*, Magnetic Soles *Polarity*, Inertial Stabilizer *Redirect* (tap Sprint mid-air).
  Some rows needed their lower-tier prerequisites, which arrive here as well: Hydraulic Legs ground slam (III) and slam
  scaling (IV), Entity Scanner auto-scan (III), and the arm frame's empty-hand harvest level (Layer 2 numbers).
  `Layer5Tests` covers the gating, the actions, Recoil Storage, slam damage, harvest levels, implant reading and Redirect.
- **Unlock-gated actions:** an unlock may give a Passive or Triggered implant an action (`ImplantActions.ACTION_UNLOCKS`):
  Regenerator Burst, the manual Shell and Haptic precision appear on the Implant Wheel and answer `ACTIVATE` only at Tier V.
- **Experimental crafting and the real chain:** 22 shared components and 5 assembly transitional items, Conductive Ink,
  Cryo-Coolant and Nanite Suspension next to the two gels, the real Neural Gel recipe, basic Tier I implants on a
  crafting table, Tier II-V upgrades from the per-tier template plus category parts, and Tier V exotics as item tags.
- **Precision Etcher:** the `cyberimplants:etching` recipe type (a Create processing recipe with `min_rpm`, usable as a
  sequenced assembly step) and the Etcher block, mounted like a Mechanical Press above a belt or depot (8 SU/RPM, idles
  below a recipe's speed, goggles say why, faster shafts etch faster). Logic Chips and Tier III grades etch at 64 RPM,
  Military Processors and Tier IV at 128, Experimental Cores and Tier V at 256; `StatAuditTest` checks the gates.
- **ISA-5:** `AWAIT ANY` (with `TIMEOUT` and `INTO`), `PERSIST VAR` (kept in the deck attachment through reboot, death
  and logout), `OVERCLOCK` (Tier V Processor: x2 cycles for 10 s at x3 Charge, 60 s cooldown), `REQUEST`, the `STOCK`
  logistics read (ISA-4) and method calls, used by `predict.position(entity, s)` (over 1 s needs ISA-5) and
  `predict.windup(entity)`. `Isa5Test` covers them.
- **Machine Interface Tier V:** `LogisticsBridge` against Create 6: a bound Neural Port on a Stock Link (or Stock
  Ticker) names a stock network; `STOCK` reads its summary and `REQUEST` broadcasts a package order to an address.
- **Teaching:** Ponder scenes for every block (Rig, Charger, Etcher, Neural Port, Proximity Sensor), Field Manual
  sections for the Etcher, logistics and ISA-5 (its code blocks compile at ISA-5 in `LangAuditTest`), tooltips that list
  the unlocks of a stack's tier.
- **Polish:** the colour-blind palette is wired (scan readout, heat trails, Precog), `accessibility.particleDensity`
  thins the optics' particles; the section 8.13 deck budgets are enforced (`maxCyclesPerServerTick` is shared fairly
  between deck threads, `maxEventsPerSecond` drops excess events) and `/cyberimplants perf` reports cycles, VM time,
  throttling and dropped events over the last second.

Deliberate Layer 5 simplifications and what is still open:

- ISA-5 builds on ISA-3/4, which Layer 3 has not finished: a Tier V deck has ISA-5 statements but still no functions,
  lists, records or `TRY`, so the full example set of [Synapse §12](04-synapse-language.md#12-example-programs) does not
  compile yet. `AWAIT ANY` waits on event names (and thresholds), not on `message` topics.
- The Etcher has no fluid tank (no recipe needs one) and no animated head; JEI draws its assembly steps as pressing.
  The `stock_changed` event is not raised; scripts poll `STOCK`.
- `predict.windup` knows creeper fuses and skeleton draws only (the full telegraph list is the Tier II unlock, Layer 3).
- **Not done because it needs a running game:** the final balance pass, time-to-kill and time-to-death measurements of
  the five archetypes, and profiling 20 active decks on a test server (`/cyberimplants perf` exists for that run).

*Exit criteria:* a complete Tier V progression is possible. All five archetypes sit inside target
time-to-kill and time-to-death bands. The full example set in [Synapse §12](04-synapse-language.md#12-example-programs)
runs. Profiling stays within budget with 20 active decks on a test server.

## 9.4 Content × layer matrix

How deep each feature family is at the end of each layer (· = not present, ◐ = partial, ● = complete).

| Feature family | L1 | L2 | L3 | L4 | L5 |
|----------------|:--:|:--:|:--:|:--:|:--:|
| Implant core functions (all 40 implants) | ● | ● | ● | ● | ● |
| Tier I–V numeric scaling | · | ● | ● | ● | ● |
| Tier II–IV unlock features | · | · | ● | ● | ● |
| Tier V unlock features | · | · | · | · | ● |
| Implant configuration (conditions, formulas) | · | · | ● | ● | ● |
| Limits, counters, synergies | ◐ | ◐ | ◐ | ● | ● |
| Body: Load, Strain, Charge | ◐ | ● | ● | ● | ● |
| Rig operation rules | ◐ | ● | ● | ● | ● |
| Cyberdeck hardware tiers and modules | ◐ | ● | ● | ● | ● |
| Synapse language | ISA-1 | ISA-2 | ISA-3/4 | ISA-3/4 | ISA-5 |
| Machine Interface | read-only rotation | Tiers I–II | Tiers III–IV | Tiers III–IV | Tier V |
| Relay Masts, Redstone Link channels | · | · | ● | ● | ● |
| Upgrade chain I → V | · | placeholder | real (I–IV) | real (I–IV) | real (I–V) |
| Etcher, fluids, custom components | · | · | ● | ● | ● |
| Exotics (Tier V resources) | · | · | · | · | ● |
| PvP rules and team handling | · | · | · | ● | ● |
| Ponder, guide, accessibility | · | · | ◐ | ◐ | ● |

## 9.5 Out of scope for 1.0 (candidates for later)

- A visual node editor as an alternative front-end that compiles to the same bytecode.
- Implant maintenance and degradation (would be an optional config).
- NPC "ripperdoc" villagers and trading of Data Shards.
- Integrations with other Create add-ons and energy mods, such as FE-powered charging.
