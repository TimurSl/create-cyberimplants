# 4. Synapse — the Cyberdeck Scripting Language

Synapse is a small, event-driven, indentation-structured language for cyberdeck programs. Its design goals:

- **Readable by non-programmers.** It reads like the pseudocode a player would sketch. Keywords are
  case-insensitive; this document writes them in UPPER CASE.
- **Bounded.** Every instruction has a fixed cycle cost and every value has a memory cost. Programs can never stall
  the server.
- **Hardware-honest.** A program can only sense and act through installed implants and bound devices.
- **Safe by construction.** It runs in a custom sandboxed VM, not a general-purpose embedded runtime, with no
  access to Java, files or network.

---

## 1. A first look

The two illustrative examples from the brief, written as working Synapse:

```synapse
PROGRAM "Close-Quarters Chrono"
REQUIRES entity_scanner, sandevistan

ON enemy_detected:
    IF distance < 8:
        ACTIVATE sandevistan WITH duration = 3s
```

```synapse
PROGRAM "Overspeed Guard"
DEVICE press = port "Press Line A"      # bound in the deck's Devices tab
DEVICE alarm = port "Alarm Lamp"

ON rotational_speed_changed FROM press:
    IF ABS(rotational_speed) > 128:
        SEND redstone_signal TO alarm STRENGTH 15
    ELSE:
        SEND redstone_signal TO alarm STRENGTH 0
```

Inside an `ON` handler, the event's payload fields (`distance`, `rotational_speed` …) are in scope as read-only
local names. They are also available as `event.<field>`.

---

## 2. Program structure

```synapse
PROGRAM "Name"                 # required, unique within the deck library
REQUIRES biomonitor, port:"Press Line A"   # optional: checked at load time
PRIORITY 5                     # optional, 0–9 (default 5): implant arbitration and scheduling order
AUTOSTART                      # optional: load on boot

CONST LIMIT = 128              # compile-time constant (no memory cost)
VAR alarms = 0                 # program-global variable (lives while the program is loaded)
PERSIST VAR best_lap = 0       # ISA-5: survives reboot, death and logout
DEVICE press = port "Press Line A"
CHANNEL gate = link "Gate"     # Remote Sensor Uplink: a named Redstone Link channel
SENSOR yard = sensor "Yard"    # Remote Sensor Uplink: a bound Proximity Sensor

ON boot:                       # event handlers …
    LOG "online"

EVERY 2s:                      # timer handlers (ISA-2)
    …

FUNC helper(x):                # functions (ISA-3)
    RETURN x * 2
```

Top-level statements other than declarations are not allowed. All behaviour lives in handlers.

---

## 3. Lexical rules

| Element | Form |
|---------|------|
| Comments | `#` to end of line |
| Blocks | A line ending in `:` opens a block. The block is the following lines indented further (spaces or tabs, consistent per file). |
| Keywords | Case-insensitive: `ON`, `IF`, `ELIF`, `ELSE`, `WHILE`, `REPEAT`, `FOR`, `IN`, `FROM`, `TO`, `SET`, `LET`, `VAR`, … They are **contextual**: the lexer treats every word alike and the parser recognises a keyword only where the grammar expects one. So `SCAN` (statement) and `scan.entities` (the Entity Scanner object) coexist, and so do `SENSOR yard = sensor "Yard"`, `port` and `link`. |
| Identifiers | `[a-z_][a-z0-9_]*`, case-sensitive |
| Numbers | `12`, `-3`, `0.75` (64-bit float; integer operations are exact up to 2^53) |
| Durations | `500ms`, `3s`, `20t`, `2m`. They are stored as ticks; a duration may be multiplied or divided by a number. |
| Strings | `"text"`, with `\"` and `\n` escapes |
| Booleans / null | `TRUE`, `FALSE`, `NULL` |
| Lists (ISA-3) | `[1, 2, 3]` |
| Records (ISA-4) | `{x: 1, y: 2}` |
| Resource ids | `"minecraft:iron_ingot"`, `"create:honey"`, written as strings |

---

## 4. Types

| Type | Notes |
|------|-------|
| `number` | |
| `bool` | |
| `string` | |
| `duration` | Ticks with a unit. `3s == 60t` is `TRUE`. |
| `position` | `{x, y, z}` in the user's dimension. Created with `POS(x, y, z)`. |
| `entity` | An opaque handle from a sensor. Fields depend on the sensor's info level (§10.3). It becomes invalid when the entity leaves sensor coverage, and reads then return `NULL`. |
| `device` | A port, channel or sensor handle |
| `implant` | An installed implant (`sandevistan`, `aes` …) |
| `list`, `record` | ISA-3 / ISA-4 |
| `null` | Missing data, absent hardware, invalid handles |

Typing is dynamic. A runtime type error aborts the current **task** (not the program) and is logged with the line
number.

---

## 5. Statements

| Statement | Meaning | ISA |
|-----------|---------|-----|
| `LET x = expr` | Declare a handler-local variable | 1 |
| `SET target = expr` | Assign a variable or a *writable property* (`SET reflex.overdrive = TRUE`) | 1 (device properties: 2) |
| `IF c: … ELIF c: … ELSE: …` | Conditional | 1 |
| `ACTIVATE implant [AT target] [WITH k = v, …]` | Fire an Active implant. It is also an expression returning `TRUE`/`FALSE`. | 1 |
| `DEACTIVATE implant` / `TOGGLE implant` | Toggle implants | 1 |
| `LOG expr` / `ALERT expr` | Write to the deck log / show a HUD message | 1 |
| `STOP` | Unload this program | 1 |
| `WHILE c: …` | Loop | 2 |
| `REPEAT n [TIMES]: …` | Counted loop | 2 |
| `BREAK` / `CONTINUE` | Loop control | 2 |
| `WAIT duration` | Yield this task for a time | 2 |
| `CONFIGURE implant key = value` | Persistent implant setting (`CONFIGURE nano formula = "blast"`) | 2 |
| `SEND redstone_signal TO port STRENGTH n` | Set a port's redstone output (sugar for `SET port.redstone_out = n`) | 2 |
| `TRANSMIT channel n` | Transmit a signal on a Redstone Link channel (0 stops transmitting) | 2 |
| `READ device.property [INTO x]` | Explicit fresh sample of a device property. Plain property access returns the value cached at the start of the tick. | 2 |
| `SCAN` / `TAG entity` / `UNTAG entity` | Optical implant actions | 1 |
| `MANIPULATE target USE \| WRENCH \| SET_VALUE n \| PULL` | Remote Manipulator actions | 2 |
| `FOR x IN list: …` / `FOR i FROM a TO b: …` | Iteration | 3 |
| `FUNC name(params): …` / `RETURN expr` | Functions; recursion depth ≤ 8 | 3 |
| `EMIT "topic" [WITH value]` | Message every loaded program, including this one | 3 |
| `BIND handle AS "name"` | Bind a device at runtime (e.g. `analyzer.looked_at`) | 3 |
| `TRY: … ON ERROR e: …` | Catch runtime errors | 4 |
| `REQUEST item COUNT n FROM network TO "address"` | Create 6 logistics package request | 5 |
| `AWAIT ANY event1, event2 [TIMEOUT d]` | Suspend until one of the events fires; returns the event | 5 |
| `OVERCLOCK` | Trigger Processor overclock (T5) | 5 |

Expressions: `+ - * / %`, `== != < <= > >=`, `AND OR NOT`, member access `a.b`, indexing `a[i]`, calls `f(x)`.
Built-in functions: `ABS MIN MAX CLAMP ROUND FLOOR SQRT LEN STR NUM DIST(a, b) POS(x, y, z) NOW() RANDOM()`.
ISA-4 adds `SIN COS ATAN2 VEC DOT NORMALIZE`.

---

## 6. Events

### 6.1 Handler forms

```synapse
ON event_name:                         # payload fields in scope
ON event_name FROM device:             # only events from this device
ON event_name WHEN condition:          # filter evaluated before a task is queued (cheap)
ON event_name DEBOUNCE 2s:             # at most one task per 2 s          (ISA-2)
ON health_below 30:                    # threshold events take a numeric argument
ON key 1:                              # deck macro key 1
ON message "topic":                    # inter-program message           (ISA-3)
EVERY 500ms:                           # timer                           (ISA-2)
```

### 6.2 Event catalog

An event only fires if its **required hardware** is installed. A program that subscribes to an event without that
hardware still loads, but the editor warns that the handler is unreachable.

| Event | Payload | Requires |
|-------|---------|----------|
| `boot` | — | — |
| `key n` | `n` | Deck macro key |
| `implant_ready` | `implant` | — |
| `implant_activated` / `implant_ended` | `implant` | — |
| `charge_below n` | `charge` | — |
| `strain_changed` | `state` | — |
| `damaged` | `amount`, `type` (damage type id), `family` (Nanocoating family or `"other"`), `attacker` | Biomonitor |
| `health_below n` / `health_above n` | `health`, `health_pct` | Biomonitor (threshold slots) |
| `effect_applied` | `effect`, `amplifier`, `duration` | Biomonitor T2 |
| `enemy_detected` | `entity`, `distance`, `kind` | Entity Scanner |
| `entity_lost` | `entity` | Entity Scanner or Target Tracking |
| `target_tagged` / `target_lost` | `entity` | Target Tracking |
| `enemy_telegraph` | `entity`, `attack`, `eta` | Combat Predictor T2 |
| `projectile_incoming` | `projectile`, `eta`, `damage` | Ballistic Visor or AES |
| `evaded` | `projectile` | AES |
| `dodge` / `window_opened` | `direction` | Kerenzikov |
| `chrono_field_started` / `chrono_field_ended` | `position` | Sandevistan |
| `power_strike` / `blade_hit` | `target` | Power Actuators / Retractable Blade |
| `shield_broken` / `shield_full` | — | Kinetic Shield |
| `camo_broken` | `reason` | Optical Camouflage |
| `landed` | `fall_distance`, `recovered` | Impact Dampeners |
| `momentum_full` | — | Momentum Booster |
| `attached` / `detached` | `surface` | Magnetic Soles |
| `machine_looked_at` | `port` | Machine Analyzer |
| `rotational_speed_changed` | `source`, `old_speed`, `rotational_speed` | Machine Interface |
| `stress_changed` / `overstressed` | `source`, `stress_used`, `stress_capacity` | Machine Interface |
| `redstone_changed` | `source`, `level` | Machine Interface |
| `contents_changed` | `source`, `item`, `count` | Machine Interface T3 |
| `train_arrived` / `train_departed` | `source`, `train` | Machine Interface T4 |
| `link_changed` | `channel`, `level` | Remote Sensor Uplink |
| `sensor_triggered` | `sensor`, `entities` | Remote Sensor Uplink |
| `stock_changed` | `network`, `item`, `count` | Machine Interface T5 |
| `message "topic"` | `value`, `sender` | ISA-3 |

---

## 7. ISA levels

The Processor module's tier sets the highest ISA the deck can run. The compiler rejects a program that uses a
feature above the target ISA and points to the offending line.

| Feature | ISA-1 | ISA-2 | ISA-3 | ISA-4 | ISA-5 |
|---------|:-----:|:-----:|:-----:|:-----:|:-----:|
| `ON`, `IF/ELIF/ELSE`, `VAR/LET/SET` (variables), expressions | ✔ | ✔ | ✔ | ✔ | ✔ |
| Implant property reads, `ACTIVATE/DEACTIVATE/TOGGLE`, `SCAN/TAG` | ✔ | ✔ | ✔ | ✔ | ✔ |
| Device property reads (ports, channels, sensors) | ✔ | ✔ | ✔ | ✔ | ✔ |
| `LOG`, `ALERT`, `STOP` | ✔ | ✔ | ✔ | ✔ | ✔ |
| `WHILE`, `REPEAT`, `WAIT`, `EVERY`, `DEBOUNCE` | | ✔ | ✔ | ✔ | ✔ |
| `CONFIGURE`, device writes (`SEND`, `TRANSMIT`, `SET device.x`), `MANIPULATE`, `READ` | | ✔ | ✔ | ✔ | ✔ |
| String operations | | ✔ | ✔ | ✔ | ✔ |
| `FUNC/RETURN`, lists, `FOR`, `EMIT`/`ON message`, `BIND` | | | ✔ | ✔ | ✔ |
| Records, vector and trig math, `TRY/ON ERROR`, logistics reads | | | | ✔ | ✔ |
| `AWAIT ANY`, `PERSIST`, `OVERCLOCK`, `REQUEST`, `predict.*` horizon > 1 s | | | | | ✔ |

Hardware capability is gated separately. For example, a T5 Processor still can't write value boxes without a T3+
Machine Interface.

---

## 8. Execution limits

### 8.1 Cycles

| Operation | Cycles |
|-----------|--------|
| Literal, local read/write, arithmetic, comparison, jump | 1 |
| Global variable read/write | 1 |
| Implant property read | 2 |
| Device property read (cached) | 4 |
| `READ` (fresh sample) | 6 |
| Sensor list access (`scan.entities`, `tracked`) | 4 + 1 per element touched |
| Function call | 3 |
| List/record allocation | 2 + 1 per element |
| `LOG`, `ALERT`, `EMIT` | 2 / 2 / 3 |
| `ACTIVATE`, `DEACTIVATE`, `TOGGLE`, `CONFIGURE`, `SCAN`, `TAG` | 10 |
| Device write (`SEND`, `SET device.x`, `TRANSMIT`) | 8 |
| `MANIPULATE` | 12 |
| Logistics read / `REQUEST` | 20 / 40 |
| `WAIT` | 1 (and yields) |

Each thread executes up to its per-tick budget, then pauses (see [Cyberdeck §3.5](03-cyberdeck.md#35-scheduling-and-execution)).
The **watchdog** kills a task that exceeds 20 × the budget between yields. A `WHILE TRUE:` loop without `WAIT` is
therefore legal but will be killed. The editor flags it.

### 8.2 Memory

| Item | Cells (1 KB = 64 cells) |
|------|------------------------|
| Compiled code | 1 per 4 instructions |
| number / bool / null / duration | 1 |
| string | 1 + 1 per 8 characters |
| entity / device / position handle | 2 |
| list | 1 + elements |
| record | 1 + 2 per field |
| `ON` subscription / `EVERY` timer | 2 |
| Task stack frame | 2 + locals |

The compiler reports a static estimate (code + globals + subscriptions + worst-case frames). The VM enforces actual
use. Exceeding the program's reservation aborts the task with `OUT_OF_MEMORY`.

### 8.3 Server-side caps (config)

`deck.maxCyclesPerPlayerPerTick` (default 1,000), `deck.maxCyclesPerServerTick` (default 50,000; when exceeded,
decks are throttled round-robin), `deck.maxSourceLength` (32 KB), `deck.maxEventsPerSecond` (200 per deck).

---

## 9. Safety and fairness rules

1. **No input synthesis.** Scripts can never move the camera, move, jump, sneak or sprint the player, attack, use,
   place or break blocks, or rearrange the inventory. **Input-bound** implant abilities (Kerenzikov dodge, air
   jumps, Hydraulic leap, Microgrip, Ballistic Module fire) can't be scripted. Active, Toggle and configurable
   implants can.
2. **Same rules as manual use.** Scripted activations pay full Charge, obey cooldowns, ranges and line-of-sight, and
   add neural latency.
3. **Information honesty.** Scripts see exactly what installed implants and bound devices see, at their info level.
   There are no world queries (no block scanning, no ore finding, no reading unbound machines).
4. **Ownership.** Only the placer or their team can bind Neural Ports and Proximity Sensors. Writes and
   `MANIPULATE` go through the same permission and claim checks as a manual interaction by that player.
5. **No chunk loading.** Devices in unloaded chunks read as `NULL` and report `OFFLINE`. Programs run only while
   their owner is online and alive.
6. **Determinism.** `RANDOM()` is seeded per deck. There is no real-time clock. `NOW()` returns ticks since boot.

---

## 10. Standard API

### 10.1 Always available

| Name | Description |
|------|-------------|
| `self.charge`, `self.charge_max`, `self.load`, `self.capacity`, `self.strain` | Core state |
| `self.position`, `self.facing`, `self.velocity`, `self.on_ground`, `self.sprinting`, `self.dimension` | Proprioception |
| `deck.ram_free`, `deck.cycles_last_tick`, `deck.dropped_events`, `deck.tier` | Deck state |
| `last_error` | Error code of the last failed action (`NO_CHARGE`, `COOLDOWN`, `BUSY`, `RANGE`, `NO_LOS`, `DENIED`, `OFFLINE`, `MISSING`) |

### 10.2 Implants

Every installed implant is a named object with: `.installed`, `.tier`, `.ready`, `.active`, `.cooldown`
(duration), `.cost`. Specific properties and actions are listed in each implant's **Deck** line in the
[catalog](implants/README.md). If an implant is not installed, its name evaluates to `NULL`-valued properties and
actions fail with `MISSING`.

| Identifier | Implant | Identifier | Implant |
|------------|---------|------------|---------|
| `sandevistan` | Sandevistan | `scanner` / `scan` | Entity Scanner |
| `reflex` | Reflex Booster | `thermal` | Thermal Vision |
| `predict` | Combat Predictor | `analyzer` | Machine Analyzer |
| `aes` | Automatic Evasion System | `ballistics` | Ballistic Visor |
| `kerenzikov` | Kerenzikov | `camo` | Optical Camouflage |
| `skeleton` | Reinforced Skeleton | `tracked` / `tracker` | Target Tracking |
| `actuators` | Power Actuators | `bio` | Biomonitor |
| `tendons` | Kinetic Tendons | `regen` | Regenerator |
| `micro` | Micro-Actuators | `adrenaline` | Adrenaline Pump |
| `dampeners` | Impact Dampeners | `defib` | Emergency Defibrillator |
| `inertia` | Inertia Compensation | `buffer` | Energy Buffer |
| `left_arm` / `right_arm` | Cybernetic Arm | `ers` | Emergency Response System |
| `manipulator` | Remote Manipulator | `plating` | Armor Plating |
| `blade` | Retractable Blade | `nano` | Nanocoating |
| `microgrip` | Microgrip System | `shock` | Shock Circuit |
| `ballistic` | Ballistic Module | `shield` | Kinetic Shield |
| `air_jump` | Enhanced Tendons | `shell` | Emergency Shell |
| `hydraulics` | Hydraulic Legs | `soles` | Magnetic Soles |
| `silent` | Silent Actuators | `stabilizer` | Inertial Stabilizer |
| `momentum` | Momentum Booster | | |

### 10.3 Entity handles

`.type` (`"minecraft:zombie"`), `.kind` (`"hostile"`, `"neutral"`, `"passive"`, `"player"`, `"boss"`),
`.distance`, `.position`, `.alive`, `.visible` (line of sight). Fields that need a higher sensor info level, such as
`.health` (Scanner T2+), `.armor` and `.held_item` (T3+), `.target` and `.effects` (T4+), return `NULL` when the
handle's source doesn't provide them.

### 10.4 Devices

**Ports** (Neural Port or virtual port), by Machine Interface tier:

| Property | Access | MI tier |
|----------|--------|---------|
| `speed`, `abs_speed`, `stress_used`, `stress_capacity`, `overstressed`, `redstone_in`, `online` | read | 1 |
| `redstone_out` (0–15) | write | 2 |
| `processing`, `progress`, `display` (list of Display Link source lines) | read | 2 |
| `value` (value box / scroll value), `filter` (item id) | read/write | 3 |
| `count(item)`, `fluid(fluid)`, `items` | read | 3 |
| `text` (Display Board / Nixie / sign) | write | 4 |
| `train` (`present`, `name`, `destination`) | read | 4 |

**Channels** (Redstone Link): `level` (read), `TRANSMIT channel n`.
**Sensors** (Proximity Sensor): `count`, `entities` (handles at Scanner-T2-equivalent info), `triggered`.
**Logistics** (MI T5): `stock(network, item)`, `REQUEST item COUNT n FROM network TO "address"`.

---

## 11. Errors

| Code | Cause | Effect |
|------|-------|--------|
| `TYPE` | Wrong operand types | Task aborted |
| `NULL_ACCESS` | Member access on `NULL` | Task aborted (use `IF x != NULL`) |
| `WATCHDOG` | Too many cycles without yielding | Task aborted |
| `OUT_OF_MEMORY` | Reservation exceeded | Task aborted |
| `RECURSION` | Call depth > 8 | Task aborted |
| `NO_CHARGE`, `COOLDOWN`, `BUSY`, `RANGE`, `NO_LOS`, `DENIED`, `OFFLINE`, `MISSING` | Action failed | Action returns `FALSE` and sets `last_error`; the task continues |

A program that aborts **5 tasks within 10 s** is set to `CRASHED` and unloaded. The deck lamp turns red.

---

## 12. Example programs

### 12.1 Panic protocol (defensive, ISA-2)

```synapse
PROGRAM "Panic Protocol"
REQUIRES biomonitor, shield, shell, nano
PRIORITY 9

ON health_below 40:
    IF shield.value < 2 AND shield.ready:
        ACTIVATE shield                    # emergency recharge
    IF bio.health_pct < 25 AND shell.ready:
        ACTIVATE shell
    ALERT "PANIC: retreat"

ON damaged WHEN family == "blast":
    IF nano.formula != "blast":
        CONFIGURE nano formula = "blast"   # needs Nanocoating T3+ for field recalibration
```

### 12.2 Factory supervisor (engineer, ISA-3, Machine Interface T3+)

```synapse
PROGRAM "Stress Governor"
DEVICE grid  = port "Main Shaft"
DEVICE rsc   = port "Speed Controller"     # Rotation Speed Controller value box
DEVICE board = port "Status Board"         # Display Board (needs MI T4 for text)
CONST HIGH = 0.9
CONST LOW  = 0.6

EVERY 1s:
    LET load = grid.stress_used / MAX(grid.stress_capacity, 1)
    IF load > HIGH:
        SET rsc.value = MAX(rsc.value - 16, 32)
    ELIF load < LOW:
        SET rsc.value = MIN(rsc.value + 16, 256)
    SET board.text = "Stress " + STR(ROUND(load * 100)) + "% @ " + STR(rsc.value) + " RPM"

ON overstressed FROM grid:
    SET rsc.value = 32
    ALERT "Main shaft overstressed — throttled"
```

### 12.3 Recon auto-tagger (lists and functions, ISA-3)

```synapse
PROGRAM "Hunter"
REQUIRES scanner, tracker

FUNC threat(e):
    IF e.kind == "boss":
        RETURN 100
    IF e.kind == "player":
        RETURN 50
    RETURN 10 + MAX(0, 32 - e.distance)

EVERY 4s:
    SCAN

ON enemy_detected DEBOUNCE 1s:
    LET best = NULL
    LET best_score = 0
    FOR e IN scan.entities:
        IF e.kind != "passive" AND threat(e) > best_score:
            SET best = e
            SET best_score = threat(e)
    IF best != NULL:
        TAG best
```

### 12.4 Two cooperating programs (parallel execution, ISA-3, needs a Coprocessor)

```synapse
PROGRAM "Perimeter"                        # thread 1
SENSOR yard = sensor "Yard"
CHANNEL gate = link "Gate"

ON sensor_triggered FROM yard:
    TRANSMIT gate 15                       # powered link: the gate contraption closes
    EMIT "intruder" WITH yard.count
```

```synapse
PROGRAM "Responder"                        # thread 0
REQUIRES camo

ON message "intruder":
    ALERT STR(value) + " contacts in the yard"
    IF value >= 3 AND camo.ready:
        ACTIVATE camo
```

### 12.5 Logistics restock (ISA-5, Machine Interface T5)

```synapse
PROGRAM "Ammo Restock"
VAR last_request = -600

EVERY 30s:
    IF stock("Armory", "cyberimplants:flechette") < 256 AND NOW() - last_request > 600:
        REQUEST "cyberimplants:flechette" COUNT 128 FROM "Armory" TO "Player Locker"
        SET last_request = NOW()
```

---

## 13. Grammar (EBNF)

```ebnf
program      = header , { declaration } , { handler | function } ;
header       = "PROGRAM" string NL , { "REQUIRES" req { "," req } NL | "PRIORITY" number NL | "AUTOSTART" NL } ;
req          = ident | ident ":" string ;
declaration  = ( "CONST" ident "=" expr
               | [ "PERSIST" ] "VAR" ident "=" expr
               | "DEVICE"  ident "=" "port"   string
               | "CHANNEL" ident "=" "link"   string
               | "SENSOR"  ident "=" "sensor" string ) NL ;
handler      = ( "ON" event [ argument ] [ "FROM" ident ] [ "WHEN" expr ] [ "DEBOUNCE" duration ]
               | "EVERY" duration ) ":" block ;
event        = ident | "message" string | "key" number ;
function     = "FUNC" ident "(" [ ident { "," ident } ] ")" ":" block ;
block        = NL INDENT statement { statement } DEDENT ;
statement    = ( simple | compound ) ;
simple       = ( "LET" ident "=" expr | "SET" lvalue "=" expr
               | "ACTIVATE" ident [ "AT" expr ] [ "WITH" kv { "," kv } ]
               | ( "DEACTIVATE" | "TOGGLE" ) ident
               | "CONFIGURE" ident lvalue "=" expr
               | "SEND" "redstone_signal" "TO" ident "STRENGTH" expr
               | "TRANSMIT" ident expr | "READ" lvalue [ "INTO" ident ]
               | "SCAN" | ( "TAG" | "UNTAG" ) expr
               | "MANIPULATE" expr action | "EMIT" string [ "WITH" expr ]
               | "BIND" expr "AS" string | "WAIT" expr
               | "REQUEST" expr "COUNT" expr "FROM" expr "TO" expr
               | "LOG" expr | "ALERT" expr | "RETURN" [ expr ]
               | "BREAK" | "CONTINUE" | "STOP" | "OVERCLOCK" | expr ) NL ;
compound     = if | while | repeat | for | try | await ;
if           = "IF" expr ":" block { "ELIF" expr ":" block } [ "ELSE" ":" block ] ;
while        = "WHILE" expr ":" block ;
repeat       = "REPEAT" expr [ "TIMES" ] ":" block ;
for          = "FOR" ident ( "IN" expr | "FROM" expr "TO" expr ) ":" block ;
try          = "TRY" ":" block "ON" "ERROR" ident ":" block ;
await        = "AWAIT" "ANY" event { "," event } [ "TIMEOUT" expr ] [ "INTO" ident ] NL ;
kv           = ident "=" expr ;
lvalue       = ident { "." ident | "[" expr "]" } ;
expr         = or ;  (* standard precedence: OR < AND < NOT < comparison < + - < * / % < unary < postfix *)
```
