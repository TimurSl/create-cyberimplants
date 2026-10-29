# 5. Create Integration

Create: Cybernetics adds a deliberately small set of blocks. Each new block does something Create can't do already,
and each one runs on Create's own resources: rotational force, fluids, Redstone Links, Display Link sources, value
boxes and the backtank. Everything else uses existing Create machines.

---

## 5.1 New blocks

| Block | Kinetic? | Stress impact | Purpose |
|-------|----------|---------------|---------|
| [Implantation Rig](#implantation-rig) | Yes | 8 SU/RPM | Install, remove and swap implants |
| [Kinetic Charger](#kinetic-charger) | Yes | 4 SU/RPM | Recharge the body battery and Charge Cells |
| [Precision Etcher](#precision-etcher) | Yes | 8 SU/RPM | New processing step for chips; speed-gated recipes |
| [Neural Port](#neural-port) | No | — | Exposes a machine's data and controls to decks |
| [Relay Mast](#relay-mast) | Yes | 2 SU/RPM | Extends deck/port/sensor range; range scales with RPM |
| [Proximity Sensor](#proximity-sensor) | No | — | Entity detection volume for decks and redstone |
| [Programming Terminal](#programming-terminal) | No | — | Full editor, shard copying, team library |

All kinetic blocks provide Engineer's Goggles information (stress, speed requirement, current operation) and have
Ponder scenes.

### Implantation Rig

- **Form:** a two-block-tall surgical chair with an overhead articulated arm (one placeable item). Shaft input at
  the back of the upper block. Fluid input (pipe) at the base, with an internal 1,000 mB tank.
- **Operation:** see [Core systems §1.5](01-core-systems.md#15-installation-the-implantation-rig). It needs
  **16/32/64/128/256 RPM** for implant tiers 1–5. Each operation consumes 250 mB Bio-Interface Gel (T1–T3) or
  Neural Gel (T4–T5).
- **Visuals:** the arm uses Create's deployer-style animation, and its speed follows shaft RPM. The patient's screen
  fades during the operation.
- **Redstone:** comparator output = operation progress. A redstone signal locks the chair (e.g. for a clinic door
  interlock).

### Kinetic Charger

- **Form:** a floor pad with a cradle slot on top that works like a depot. Shaft input from below or the sides.
- **Charging:** a player standing on the pad, or a Charge Cell in the cradle, receives **|RPM| / 8 C/s** (32 C/s at
  256 RPM). The Charger doesn't run below 16 RPM. An Energy Buffer T4+ charges at ×2 rate here.
- **Automation:** Charge Cells can be fed and extracted by funnels, belts or a Mechanical Arm. A comparator reads the
  cell's fill level.

### Precision Etcher

- **Form:** mounted above a depot, belt or basin, like the Mechanical Press. It has an internal 1,000 mB fluid tank
  fed by pipe.
- **Recipe type `cyberimplants:etching`:** item input, optional fluid input, output(s), processing time, and
  **`min_rpm`**. Below `min_rpm` the Etcher idles and the goggles show *"Speed too low for this recipe"*. Processing
  time scales with speed, as for the press.
- **Sequenced assembly step:** etching recipes can be used as steps in Create sequenced assembly, as Create's own
  pressing, deploying, spouting and cutting steps are.
- **Speed tiers used by this mod:** Logic Chip ≥ **64 RPM**, Military Processor ≥ **128 RPM**, Experimental Core ≥
  **256 RPM**. Higher technology tiers therefore need stronger kinetic infrastructure.

### Neural Port

- **Form:** a small data plug placed against any face of a target block (one port per target face).
- **Binding:** the owner, or someone on their team, opens the port by right-click or through the deck's Devices tab,
  names it ("Press Line A"), and binds it to their deck. Ports remember an allow-list of players and teams.
- **Data access:** exposes the target's readable and writable properties at the binding deck's Machine Interface
  tier (see [Synapse §10.4](04-synapse-language.md#104-devices)).
- **Redstone:** the port emits its `redstone_out` level from all faces except the attached one. This lets a deck
  drive any redstone-controlled Create part (clutch, gearshift, sequenced gearshift, contraption controls) through a
  port.
- **Range:** reachable from a deck within its Machine Interface direct range, or anywhere on a Relay Mast network
  that the deck is also within range of.

### Relay Mast

- **Form:** three blocks tall, with a shaft input at the base.
- **Range:** **16 + |RPM| / 2** blocks (16 at a stall, 144 at 256 RPM). With no rotation, the mast is offline.
- **Network:** masts within each other's range link into a **relay network**. Any Neural Port, Proximity Sensor or
  deck within range of any mast in the network can reach every other member. Networks don't load chunks: a member
  in an unloaded chunk is `OFFLINE`.
- **Design intent:** remote factory control has a running stress cost. A long-range data backbone means real
  kinetic infrastructure.

### Proximity Sensor

- **Form:** a block with a configurable detection box (up to 16 × 16 × 16, shown with a wrench outline) and a filter
  slot. The filter takes Create filter items or attribute filters, for example "hostile", "player" or a specific
  mob.
- **Outputs:** redstone (signal = count of matching entities, capped at 15), and data for decks bound through a
  Remote Sensor Uplink (`count`, `entities`, `sensor_triggered`).

### Programming Terminal

- A desk block. It opens the full-screen Synapse editor with syntax highlighting, error lints and hardware
  profiles. It also duplicates Data Shards and hosts a library shared by the placer's team. No power needed.

## 5.2 New items (non-implant)

| Item | Purpose |
|------|---------|
| Charge Cell | Portable 100 C battery. Recharged on a Kinetic Charger; using it transfers the charge to the body over 1 s. |
| Data Shard | Program source storage (8 KB) for sharing and trading |
| Neuro-Stabilizer | Suppresses Neural Strain for 10 minutes |
| Flechettes (standard, incendiary, concussive, smart) | Ballistic Module ammunition |
| Intermediate components | See [Progression & crafting](06-progression-and-crafting.md) |

## 5.3 Fluids

| Fluid | Recipe (Create mixing unless stated) | Used for |
|-------|--------------------------------------|----------|
| **Bio-Interface Gel** | unheated: 250 mB water + slime ball + bone meal + sugar → 250 mB | T1–T3 surgery, T1–T2 implant finishing (spout) |
| **Conductive Ink** | heated: 250 mB water + 2 redstone + copper nugget → 250 mB | Etching chips (spout and Etcher) |
| **Cryo-Coolant** | unheated: 250 mB water + 2 packed ice + lapis lazuli → 250 mB | T4 processors |
| **Nanite Suspension** | superheated: 250 mB Bio-Interface Gel + 2 Silica Dust + redstone + 3 iron nuggets → 250 mB | Nanocoating, Neural Gel, T5 parts |
| **Neural Gel** | heated: 250 mB Bio-Interface Gel + 50 mB Nanite Suspension → 250 mB | T4–T5 surgery |

All fluids can be bottled with a spout (as Create's own fluids can) and moved through pipes, tanks and item drains.

## 5.4 Data integration with Create machinery

The deck and the Machine Analyzer read Create machinery through Create's own abstractions, so most blocks
(including other add-ons' blocks built on those abstractions) work without special-case code.

| Data / control | Create mechanism reused | Exposed as |
|----------------|-------------------------|------------|
| Rotation speed and direction | Kinetic block entities | `speed`, `abs_speed`, `rotational_speed_changed` |
| Stress used / capacity, overstress | Kinetic networks | `stress_used`, `stress_capacity`, `overstressed` |
| Machine-specific readouts | **Display Link sources**: anything a Display Link can read (stockpile counts, train schedules, fluid levels, item names, time-of-day, etc.) | `display` lines |
| Adjustable numeric settings | **Value boxes** (scroll value behaviour): Rotation Speed Controller, Threshold Switch thresholds, funnel/chute extraction amounts, etc. | `value` (read/write) |
| Filters | **Filtering behaviour** on funnels, chutes, deployers, smart observers | `filter` (read/write) |
| Inventories and tanks | NeoForge item/fluid handler capabilities (vaults, tanks, depots, basins, belts) | `count()`, `fluid()`, `items` |
| Display output | **Display targets**: Display Boards, Nixie Tubes, signs | `text` (write) |
| Wireless control | **Redstone Link network**: the deck registers as a virtual link node for each bound channel | `CHANNEL`, `TRANSMIT`, `link_changed`; Biomonitor broadcast |
| Pressurised air | **Backtank** air storage (same as Extendo Grip and Potato Cannon) | Pneumatic implants |
| Projectiles | **Potato Cannon projectile types** (data-driven registry) | Ballistic Module T4 ammo; Ballistic Visor arcs |
| Moving structures | Contraptions and trains | Magnetic Soles anchoring, Momentum Booster charging, train reads (MI T4) |
| Logistics (Create 6) | Stock network summary and package requests | `stock()`, `REQUEST`, `stock_changed` (MI T5) |

## 5.5 Production scaling with Create

| Tier | Create capability required (new at this tier) | Typical factory |
|------|-----------------------------------------------|-----------------|
| I Civilian | Mechanical Press, Mechanical Saw (cutting), unheated Mixer, Deployer, Spout | A hand-fed line on one shaft |
| II Industrial | Heated mixing (Blaze Burner), **bulk blasting** (Encased Fan + lava), first **sequenced assembly** loops, Mechanical Crafters | A belt line with a brass-age kinetic network |
| III Advanced | Superheated mixing (Blaze Cake), Crushing Wheels, **Precision Etcher ≥ 64 RPM**, multi-fluid spouting | Automated chip line feeding a sequenced assembly loop |
| IV Military | **Etcher ≥ 128 RPM**, Sturdy Sheets, coolant fluid supply, 3–4 loop assemblies, Rig at 128 RPM | Several automated sub-lines with buffered intermediates |
| V Experimental | **Etcher at 256 RPM** (Create's maximum), rare-resource input lines, Neural Gel, Rig at 256 RPM | A full production complex; high-capacity stress sources required |

## 5.6 Ponder scenes

Implantation Rig (surgery and abort conditions) · Kinetic Charger (speed → rate) · Precision Etcher (speed-gated
recipes, sequenced step) · Neural Port (binding, redstone out) · Relay Mast (RPM → range, networks) · Proximity
Sensor (box, filter) · "Your first program": a deck reading a Speedometer through a port.
