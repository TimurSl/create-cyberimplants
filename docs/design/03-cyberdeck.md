# 3. Cyberdeck Hardware

The cyberdeck is the central interface of the cybernetics system. It is a programmable computer in the user's
spine. It runs [Synapse](04-synapse-language.md) programs that watch the user's senses, drive their implants, and
talk to Create machinery.

---

## 3.1 Architecture

```
            ┌──────────────────────── Deck Chassis (Spine) ───────────────────────┐
 Implants ──┤ Implant Bus ──► Event Router ──► Scheduler ──► Thread 0 (Processor) │
 (senses,   │                     ▲               │     └──► Thread 1..n (Coproc.)│
  effectors)│                     │               ▼                               │
            │  Device Bus ◄── Machine Interface / Remote Sensor Uplink            │
            │     │             RAM (loaded programs + variables)                 │
            │     │             Storage (program library)                         │
            └─────┼───────────────────────────────────────────────────────────────┘
                  ▼
   Neural Ports · Virtual Ports · Relay Masts · Redstone Link channels · Proximity Sensors · Logistics network
```

- **Implant Bus.** Every installed implant registers its properties, actions and events here. A program can only
  use the APIs of hardware that is physically installed.
- **Device Bus.** External devices bound to the deck, by name.
- **Event Router.** Turns implant and device signals into program events (`enemy_detected`, `damaged`,
  `rotational_speed_changed` …).
- **Scheduler.** Runs event handlers on threads within the per-tick cycle budget. See §3.5.

## 3.2 Chassis

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Load | 4 | 5 | 6 | 7 | 8 |
| Module sockets | 3 | 4 | 5 | 6 | 8 |
| Neural latency (script activation → implant fires) | 4 t | 3 t | 2 t | 1 t | 0 t |
| Macro keys | 2 | 4 | 4 | 4 | 4 |
| Program log buffer | 20 lines | 50 | 100 | 200 | 500 |

A chassis needs **exactly one Processor**, **at least one RAM** and **at least one Storage** module to boot. Every
module costs +1 Load. Module tier ≤ chassis tier.

## 3.3 Modules

### Processor (Processing Upgrade)

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Cycles per tick (thread 0) | 20 | 40 | 80 | 160 | 320 |
| ISA level | ISA-1 | ISA-2 | ISA-3 | ISA-4 | ISA-5 |
| Minimum `EVERY` interval | 1 s | 10 t | 5 t | 2 t | 1 t |
| Special | — | — | — | — | **Overclock:** ×2 cycles for 10 s, Charge cost ×3, 60 s cooldown |

The **ISA level** decides which language features compile (full table in
[Synapse §7](04-synapse-language.md#7-isa-levels)):

| ISA | Adds |
|-----|------|
| 1 | `ON` handlers, `IF/ELSE`, `VAR/LET/SET`, expressions, implant and device **reads**, `ACTIVATE/DEACTIVATE`, `SCAN/TAG`, `LOG`, `ALERT` |
| 2 | `WHILE`, `REPEAT`, `WAIT`, `EVERY`, `DEBOUNCE`, `CONFIGURE`, device **writes** (`SEND`, `TRANSMIT`, `SET port.x`), `MANIPULATE`, strings |
| 3 | `FUNC/RETURN`, lists, `FOR`, `EMIT` / `ON message`, runtime `BIND` |
| 4 | Records, vector and trig math, `TRY/ON ERROR`, logistics reads |
| 5 | `AWAIT ANY`, `PERSIST`, `OVERCLOCK`, logistics `REQUEST`, `predict.*` horizons > 1 s |

### RAM (RAM Expansion), stackable

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Memory per module | 2 KB | 4 KB | 8 KB | 16 KB | 32 KB |

1 KB = 64 memory *cells*. A loaded program occupies `code size + variable cells + subscriptions`
(see [Synapse §8.2](04-synapse-language.md#82-memory)). A program can't start unless its footprint fits in free RAM.

### Storage (Storage Expansion), stackable

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Program storage per module | 4 KB | 8 KB | 16 KB | 32 KB | 64 KB |

Storage holds compiled programs (1 KB ≈ 256 instructions) and their source. The library can hold any number of
programs that fit.

### Coprocessor (Parallel Execution)

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Extra threads | +1 | +1 | +2 | +2 | +3 |
| Cycles per tick, per extra thread | 10 | 20 | 40 | 80 | 160 |
| Max coprocessors per chassis | 1 (T1–T2 chassis) | | 2 (T3–T4 chassis) | | 3 (T5 chassis) |

Each **thread runs one program**. A deck with no Coprocessor runs one program at a time. Programs on different
threads run concurrently. They share no variables and communicate only through `EMIT` messages (ISA-3).

### Machine Interface

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Direct range (without Relay Masts) | 16 | 24 | 32 | 48 | 64 |
| Bound ports (Neural + virtual) | 2 | 4 | 8 | 16 | 32 |
| Read access | kinetic (speed, stress, overstressed), redstone in | + processing state, Display Link source data | + inventory and fluid contents | + train station state | + logistics stock levels |
| Write access | — | redstone out (port face output) | + **value boxes** (scroll values: Rotation Speed Controller, Threshold Switch …), **filters** | + **display targets** (Display Boards, Nixie Tubes, signs) | + **logistics requests** (send packages through the Create 6 stock network) |

### Remote Sensor Uplink

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Redstone Link channels (read and transmit) | 2 | 4 | 6 | 8 | 12 |
| Bound Proximity Sensors | 1 | 2 | 4 | 6 | 8 |
| Unlocks | — | — | Joins **Relay Mast** networks for sensors | **Tag sharing:** sends Target Tracking tags to teammates' decks | **Sensor fusion:** Proximity Sensor detections appear in `scan.entities` and raise `enemy_detected` |

A **Redstone Link channel** is a named pair of Create frequency items. The deck acts as a virtual Redstone Link
node for that frequency: it can read the network's signal and transmit its own, within Create's configured link
range. A deck can therefore remote-control anything already driven by Redstone Links (clutches, gearshifts,
contraption controls, doors) without any extra blocks.

## 3.4 Power

- A deck uses no Charge while idle.
- Computation costs **1 C per 2,000 cycles executed**. A fully loaded T5 deck (320 cycles/tick on thread 0) uses
  about 3.2 C/s.
- Implant activations made by scripts pay the implant's normal cost.

## 3.5 Scheduling and execution

1. Each tick, the Event Router queues events for subscribed handlers. Each queued handler run is a **task**.
2. Each thread executes the tasks of its program in FIFO order, up to the thread's cycle budget. A task that runs
   out of budget **pauses mid-handler and resumes next tick**. `WAIT` always yields.
3. **Watchdog:** a task may not use more than **20 × its thread's per-tick budget** between yields. If it does, it
   is killed with `WATCHDOG`. The program keeps running, and the error is logged.
4. **Queue cap:** each program holds at most 16 pending tasks. Further events of the same name are *coalesced*
   (only the latest payload is kept), and overflow increments `deck.dropped_events`.
5. **Arbitration:** if two programs activate the same implant in the same tick, the higher `PRIORITY` wins. The
   loser's `ACTIVATE` returns `FALSE` with `last_error = "BUSY"`.
6. **Neural latency:** a successful script activation fires after the chassis's latency. The implant's
   cost and cooldown are reserved immediately.

## 3.6 User interface

The deck UI opens with the *Deck* keybind (default `K`), anywhere, or at a Programming Terminal.

| Tab | Purpose |
|-----|---------|
| **Programs** | The library. Edit, compile (errors with line numbers), view footprint (code KB, RAM KB, required hardware, ISA level), load and unload, set autostart. |
| **Processes** | Running programs per thread: state, cycles per tick (live graph), RAM, queued tasks, last error. Buttons: pause, restart, kill. |
| **Devices** | Bound Neural Ports, virtual ports, Redstone Link channels and Proximity Sensors. Rename, test-read, unbind. Relay Mast network status. |
| **Implants** | Every installed implant's deck API: live property values, configurable parameters (AES conditions, Nanocoating formula, ERS threshold …). |
| **Keys** | Assign deck macro keys 1–4 to `key(n)` events with labels. |
| **Log** | `LOG` output and runtime errors, with timestamps and program names. |

## 3.7 Programs outside the body

- **Data Shard** (item): portable storage for up to 8 KB of program source, used for sharing and trading. Shards are
  written and read at a **Programming Terminal** or through the deck UI when the shard is in the inventory. To run,
  a program must be copied into deck Storage.
- **Programming Terminal** (block): a full-screen editor with syntax highlighting, linting against a *hardware
  profile* (your body or a chosen deck tier), shard duplication (consumes a blank shard), and a team-shared program
  library.
- A Create **Clipboard** with text can be imported into the editor as source.
