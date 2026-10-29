# B. Cyberdecks

**Region:** Spine (1 slot). This page summarises the deck as an implant category. The full hardware specification
is in [Cyberdeck hardware](../03-cyberdeck.md), and the programming model is in
[Synapse scripting language](../04-synapse-language.md).

A cyberdeck is a **chassis** installed in the Spine, plus **modules** in the chassis's sockets. Module tier ≤
chassis tier. Each module costs +1 Load.

| Requested function | Provided by | Summary |
|--------------------|-------------|---------|
| Script Execution | Chassis (built in) + **Processor** module | Runs Synapse programs. The Processor tier sets the cycle budget and the ISA level (which language features are available). |
| Implant Automation | Chassis (built in) | Programs can read, configure and activate any installed implant. They pay normal costs plus neural latency. |
| Machine Interface | **Machine Interface** module | Reads and writes Create machines through Neural Ports, Machine Analyzer virtual ports and Relay Masts. |
| Remote Sensor Access | **Remote Sensor Uplink** module | Joins Create Redstone Link frequencies (read and transmit), reads Proximity Sensors, and shares Target Tracking tags. |
| RAM Expansion | **RAM** module (stackable) | More memory for loaded programs and their variables. |
| Processing Upgrade | **Processor** module (exactly one) | More cycles per tick and a higher ISA level. |
| Parallel Execution | **Coprocessor** module | Extra execution threads, each with its own cycle budget. |
| Storage Expansion | **Storage** module (stackable) | More stored programs and larger program size. |

### Chassis at a glance

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Load (chassis only) | 4 | 5 | 6 | 7 | 8 |
| Module sockets | 3 | 4 | 5 | 6 | 8 |
| Neural latency (script → implant) | 4 t | 3 t | 2 t | 1 t | 0 t |
| Base threads | 1 | 1 | 1 | 1 | 1 |
| Macro keys usable | 2 | 4 | 4 | 4 | 4 |

A minimal deck is Chassis + Processor + RAM + Storage. That fills all three sockets of a T1 chassis, so a player
needs a T2+ chassis before a Machine Interface fits.

### Design intent

- A deck on its own **senses nothing**. Health data needs a Biomonitor, enemy data needs an Entity Scanner, and
  machine data needs a Machine Interface with Ports. The deck's value grows with the rest of the build.
- **Latency vs. ERS:** the [Emergency Response System](f-circulatory.md#emergency-response-system-ers) reacts in the
  same tick, *before* damage resolves, but follows fixed rules. A deck can follow any rule but reacts 4–0 ticks
  later. Defensive players often run both.
- The **Cyberdeck Engineer** specialisation spends its Load on deck modules instead of combat implants, and gets
  factory control and complex automation in return.
