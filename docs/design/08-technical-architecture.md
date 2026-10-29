# 8. Technical Architecture

This document maps the design onto Minecraft 1.21.1, NeoForge and Create 6. Class and event names refer to
NeoForge 21.1.x and Create 6.0.x for 1.21.1. Create's internal API is not stable, so every Create reference sits
behind a compatibility layer (§8.9). Verify those references against the pinned Create version.

---

## 8.1 Platform

| Item | Choice |
|------|--------|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.x (ModDevGradle) |
| Java | 21 |
| Hard dependency | Create 6.0.x (mc1.21.1), bringing Ponder, Flywheel and Registrate |
| Optional | JEI or EMI (recipe categories), Jade (block info), FTB Teams (team resolution; falls back to scoreboard teams) |
| Mod id / root package | `cyberimplants` / `dev.cyberimplants` |

## 8.2 Source layout

```
dev.cyberimplants
├── CyberImplants.java              # @Mod entry, registration wiring
├── registry/                       # items, blocks, block entities, fluids, data components, menus, recipe types, attachment types
├── body/                           # BodyState attachment, slot rules, Load/Strain, Charge, cooldowns
├── implant/                        # ImplantType registry, ImplantBehavior base, one package per category
│   ├── neural/  deck/  skeletal/  limbs/  optical/  circulatory/  subdermal/  mobility/
├── combat/                         # damage pipeline, temporal fields, projectile tracking
├── movement/                       # client-side movement abilities + server validation
├── synapse/                        # language: lexer, parser, compiler, verifier, VM, scheduler (no Minecraft imports)
├── deck/                           # binds synapse to the game: API bindings, event router, device bus
├── compat/create/                  # the ONLY package that imports com.simibubi.create.*
├── content/                        # blocks & BEs: rig, charger, etcher, port, relay, sensor, terminal
├── network/                        # payloads + handlers
├── client/                         # HUD layers, overlays, renderers, screens (editor), keybinds
└── data/                           # datagen: recipes, tags, models, lang, implant stats
```

## 8.3 Data model

- **`ImplantType` custom registry** (`cyberimplants:implant_type`). Each entry declares: category, region, activation
  type, flags (`emergency`, `pneumatic`, `input_bound`), an `ImplantBehavior` factory, and its Synapse API descriptor
  (properties, actions, events, cycle costs, minimum tier per member).
- **Items.** One `ImplantItem` per implant type, plus deck modules. Tier is the data component
  `cyberimplants:tier` (int 1–5), registered with `DeferredRegister.DataComponents` and synced to clients for
  tooltips.
- **Stats are data.** `data/<ns>/cyberimplants/implant_stats/<type>.json` holds per-tier numbers (load, cost,
  cooldown, radius …). They are loaded by a reload listener, validated against a codec per implant type, and synced to
  clients on login and reload.
- **Tags.** Blocks: `magnetic`, `ungrippable`. Entity types: `augment_resistant`, `evadable` (projectiles). Damage
  types: `blunt`, `machinery`, `shield_bypass`, plus one tag per Nanocoating family (`nano/thermal`, …).
- **Recipes.** Tier I via Create mechanical crafting followed by filling. Upgrades via Create sequenced assembly whose
  base ingredient is NeoForge's component-matching ingredient (`neoforge:components`, matching the previous tier). A
  new `cyberimplants:etching` recipe type extends Create's processing recipe base and implements its
  assembly-step interface.

## 8.4 Player state and synchronisation

- **`BodyState`** data attachment on players (`AttachmentType` with codec serialisation and `copyOnDeath`):
  installed implants (slot → type + tier), per-implant configuration, Charge and buffer, cooldown end-ticks (in
  "online ticks", so they don't advance while logged out), Strain state, Momentum, deck storage (program sources +
  compiled cache), `PERSIST` variables, bound devices.
- **Transient runtime** (not serialised): VM tasks, temporal fields, tags, scan results, AES stacks in flight.
- **Sync.** NeoForge 21.1 attachments are not auto-synced, so we use explicit payloads (`RegisterPayloadHandlersEvent`):
  - S2C: `BodySnapshot` (on login, respawn, dimension change, Rig operation), `BodyDelta` (Charge, cooldowns,
    toggles; batched every 4 t), `ScanResults`, `TemporalField` start/end, `DeckLog`, `HudAlert`.
  - C2S: `ActivateImplant`, `ToggleImplant`, `MovementAbility` (§8.6), `DeckMacroKey`, `UploadProgram`
    (size-capped), `ConfigureImplant`, `BindDevice`.
- All C2S payloads are server-validated and rate-limited.

## 8.5 Implant runtime and hooks

Each tick, `PlayerTickEvent.Post` (server) ticks the installed behaviours, regenerates Charge, advances
cooldowns, then runs the deck scheduler.

| Concern | Hook |
|---------|------|
| Passive stats | `AttributeModifier`s with stable `ResourceLocation` ids per (implant, attribute). Uses 1.21 attributes such as `attack_damage`, `attack_speed`, `movement_speed`, `jump_strength`, `step_height`, `safe_fall_distance`, `fall_damage_multiplier`, `block_break_speed`, `block_interaction_range`, `entity_interaction_range`, `knockback_resistance`, `explosion_knockback_resistance`, `armor`, `armor_toughness`, `max_health`, `gravity`, `sneaking_speed` |
| Typed % / flat reduction (Nanocoating, Reinforced Skeleton) | `LivingIncomingDamageEvent` (damage container reductions) |
| ERS pre-damage reaction (T3+) | `LivingIncomingDamageEvent`, highest priority, before mitigation |
| Emergency Shell | `LivingDamageEvent.Pre` threshold check + reduction while active |
| Kinetic Shield absorption | `LivingDamageEvent.Pre` (post-armor) |
| Emergency Defibrillator | `LivingDeathEvent` (cancel, set health, apply effects) |
| Shock Circuit, Adrenaline, Biomonitor log | `LivingDamageEvent.Post` |
| AES / Ballistic Visor | Per-tick projectile scan in a radius around implant users, using swept-AABB intersection prediction (server) |
| Reflex item-use speed | `LivingEntityUseItemEvent.Start/Tick` (shorten duration) |
| Arm mining and harvest | `PlayerEvent.BreakSpeed`, `PlayerEvent.HarvestCheck` |
| Camouflage and Silent Actuators detection | `LivingEvent.LivingVisibilityEvent`, `LivingChangeTargetEvent` |
| Silent Actuators vibrations | Game-event hook (`VanillaGameEvent`) filtering player-sourced vibrations |
| Impact Dampeners | `LivingFallEvent` |
| Remote Manipulator | Server ray-cast, then `ServerPlayerGameMode.useItemOn` with an extended reach context. The normal interaction events fire, so claim mods see a real player interaction. |

**Mixins** are used only where no event exists. Planned list: shield raise delay; sprint-cancel conditions
(Inertia Compensation); airborne acceleration and ice friction (Inertial Stabilizer); client entity glow
(Thermal); elytra turning. Each mixin is small, injection-only and documented.

## 8.6 Movement abilities

Player movement is **client-authoritative** in vanilla. All mobility abilities (air jump, dodge, charged leap,
wall cling, wall walk, momentum dash, air brake) follow the same pattern:

1. The **client** detects the input, checks a locally mirrored state (charges, cooldowns, Charge), applies the
   velocity change immediately for responsiveness, and sends `MovementAbility(type, params)`.
2. The **server** validates it: the implant is installed, it is ready, Charge is sufficient, and the parameters are
   within tier bounds. Then it spends the cost and widens its movement tolerance for a few ticks so vanilla's "moved
   too quickly" check doesn't rubber-band the player.
3. On rejection, the server sends a correction (velocity reset and state resync).

Server-originated impulses (AES sidestep, Kinetic Debt release, knockback pulses) set velocity on the server and
mark the player for a motion update packet.

## 8.7 Temporal fields (Sandevistan)

- A `TemporalFieldManager` per `ServerLevel` holds active fields (centre, radius, scale, owner, expiry, allies). It
  is mirrored to clients by payload.
- **Non-player entities** inside a field: in `EntityTickEvent.Pre` (cancellable), a per-entity accumulator adds
  `scale` each tick and only lets a tick through when it reaches 1. This runs on both client and server, so
  client-side animation stops too. The field also decrements `invulnerableTime` and `hurtTime` at real speed, which
  keeps the hit window.
- **Projectiles** are entities, so they freeze in place the same way and keep their velocity.
- **Players** can't have ticks skipped. They get movement-speed, attack-speed and item-use modifiers equivalent to
  the PvP time-scale floor.
- **Kinetic Debt (T4):** knockback applied to frozen entities is accumulated in a map and applied on release.
- Fields end on owner logout, death or dimension change.

## 8.8 Synapse VM

- **Pure Java with no Minecraft imports**, so it can be unit-tested in isolation.
- **Pipeline:** Lexer (indentation tokens) → Pratt parser → AST → semantic checker (names, ISA level, hardware
  profile) → bytecode compiler (compact `int[]` with a constant pool) → verifier.
- **VM:** a stack machine with frames, per-instruction cycle accounting, memory accounting on allocation, and a
  watchdog. A task is a resumable frame stack. `WAIT` and an exhausted budget both yield.
- **Bindings:** generated from `ImplantType` API descriptors and device descriptors. Each binding has a cycle cost,
  a minimum ISA level and a minimum hardware tier. The game side (`deck/`) implements the binding interface.
- **Scheduler:** one per player. Threads are mapped to programs, with FIFO task queues and coalescing (see
  [Cyberdeck §3.5](03-cyberdeck.md#35-scheduling-and-execution)). Global per-tick server caps are enforced
  round-robin across players.
- **Persistence:** only source, compiled cache, bindings and `PERSIST` variables are saved. Tasks restart on reboot.
- **Security:** no reflection, no host calls outside the binding table, bounded source size, bounded recursion and
  allocation.

## 8.9 Create compatibility layer (`compat/create`)

| Bridge | Responsibility |
|--------|----------------|
| `KineticBridge` | Speed, direction, network stress/capacity and overstress for any kinetic block entity |
| `BehaviourBridge` | Read and write value boxes (scroll value behaviour) and filters (filtering behaviour) through Create's block-entity behaviour lookup |
| `DisplaySourceBridge` | Enumerate Display Link sources for a target and render their output as text lines for `display` |
| `DisplayTargetBridge` | Write text to display targets (boards, nixie tubes, signs) |
| `LinkBridge` | Virtual Redstone Link nodes for decks and Biomonitor broadcast. Implements Create's link-node interface and registers with its link network handler. |
| `BacktankBridge` | Read and consume backtank air for pneumatic implants |
| `PotatoBridge` | Resolve Potato Cannon projectile types for Ballistic Module ammo and Visor arcs |
| `ContraptionBridge` | Detect standing on moving contraptions and trains and read their velocity; anchoring for Magnetic Soles |
| `TrainBridge` | Station state for MI T4 |
| `LogisticsBridge` | Stock summaries and package requests on the Create 6 logistics network (MI T5; implemented last because the API is the newest and most volatile) |
| `GoggleBridge` | Reuse goggle tooltips for the Machine Analyzer; provide goggle info for our own kinetic blocks |

Our kinetic blocks extend Create's kinetic block and block-entity bases, declare stress impact through Create's
stress-value registration (so Create's config and goggles see them), and use Create's visual/instancing system for
moving parts (Rig arm, Etcher head, Mast rotor). Ponder scenes are registered through the Ponder plugin API.

## 8.10 Client

- **Keybinds** (`RegisterKeyMappingsEvent`): Implant Wheel, Quick Slots 1–4, Deck UI, Deck Macro 1–4, Dodge, Scan,
  Tag.
- **HUD** (`RegisterGuiLayersEvent`): Charge bar, cooldown strip, deck lamp, Biomonitor vitals, alerts.
- **World overlays** (`RenderLevelStageEvent`): scanner markers, tracking markers, lead indicators, trajectories,
  temporal field sphere, kinetic area overlay, Proximity Sensor boxes.
- **Screens:** Rig body diagram, Implant Wheel, deck UI (six tabs), Synapse editor (syntax highlighting, inline
  errors, footprint panel).
- **Accessibility:** every overlay has an opacity and colour-blind palette option. Static and screen-shake effects
  can be disabled.

## 8.11 Testing

| Layer | Approach |
|-------|----------|
| Synapse | JUnit: lexer and parser golden files, compiler/ISA gating, cycle and memory accounting, watchdog, scheduler fairness, fuzzing the parser with random input |
| Stats and recipes | Datagen validation: every implant has stats for T1–T5, every tier has an upgrade recipe, every referenced item exists |
| Gameplay | NeoForge GameTests: damage pipeline ordering, Defibrillator conditions, Sandevistan freeze and hit window, Rig abort conditions, port reads against real Create blocks (speedometer, rotation speed controller) |
| Balance | GameTest scenarios that measure time-to-kill and time-to-death for each archetype vs. standard mobs and netherite PvP dummies |

## 8.12 Configuration

| File | Contents |
|------|----------|
| Server | Charge on/off and multipliers, cooldown multiplier, keep-on-death, PvP options (§7.4), deck VM caps, Sandevistan block-entity freeze, Relay Mast range formula, Rig RPM requirements |
| Client | HUD layout, overlay opacity, palette, effect intensity |
| Data (datapack) | Implant stats, tags, recipes |

## 8.13 Performance budgets

| System | Budget / approach |
|--------|-------------------|
| Implant ticking | O(installed implants) per player; passive attribute implants do no per-tick work |
| Projectile prediction (AES/Visor) | Only for users with those implants; radius-limited entity query every 2 t |
| Entity Scanner | Queries only on pulse or auto-scan interval |
| Thermal / Analyzer overlays | Client-side only, using already-synced entity and block-entity data |
| Deck VM | ≤ 1,000 cycles per player per tick, ≤ 50,000 per server tick (config) |
| Ports and relays | Lazy reads (cached once per tick on first access); relay networks rebuilt on change, not polled |

## 8.14 Key risks

| Risk | Mitigation |
|------|------------|
| Create API changes between patch versions | Pin an exact Create version; isolate all access in `compat/create`; GameTests against real Create blocks |
| Movement desync and anti-cheat kicks | Client-predicted abilities with server validation and temporary movement-tolerance widening (§8.6) |
| Temporal field visual desync | Fields mirrored to clients; client-side tick skipping; interpolation clamped for frozen entities |
| Scope (47 implant types × 5 tiers) | Shared behaviour base classes and data-driven stats; layered implementation plan ([§9](09-roadmap.md)) |
| Script abuse (lag, griefing) | Sandboxed VM, hard cycle caps, ownership checks, no chunk loading, rate-limited payloads |
