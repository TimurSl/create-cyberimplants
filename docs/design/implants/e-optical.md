# E. Optical Implants

**Region:** Ocular (2 slots). Optics give the player information. Most of the deck's senses come from here, and
having only two eye slots is the main limit on what a build can know.

---

## Entity Scanner

**Ocular · Active (pulse) + Passive auto-scan at T3+ · Load 3/3/4/4/5 · Charge 5/5/6/6/8 per pulse**

*Identity:* **identifies** what is nearby. A pulse reveals entities in a radius for a few seconds as HUD markers
(through walls), each with an info card.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Pulse radius | 12 | 16 | 24 | 32 | 48 |
| Reveal duration | 3 s | 4 s | 5 s | 6 s | 8 s |
| Pulse cooldown | 10 s | 8 s | 6 s | 5 s | 4 s |
| Max markers | 8 | 12 | 16 | 24 | 32 |
| Info card | type, threat class (Hostile/Neutral/Passive/Player/Boss) | + health | + armor value, held item | + active effects, **current target** (who it is attacking) | + other players' installed implant **categories**; reveals Optical Camouflage ≤ T4 |
| Passive auto-scan | — | — | every 4 s, ½ radius | every 3 s | every 2 s |

**Limits:** scanning is **detectable**. Players hit by a pulse who have an Entity Scanner T3+ or a Biomonitor T4+
get a "You were scanned" warning with a direction. Markers show the position at pulse time and are not updated
afterwards (live tracking is Target Tracking's job).

**Deck:** `scan.entities` (list), `scan.nearest(kind)`, `SCAN` (fires a pulse).
Events `enemy_detected(entity, distance, kind)` (once per entity per reveal) and `entity_lost(entity)`.

---

## Thermal Vision

**Ocular · Toggle · Load 2/3/3/4/4 · Charge 1 C/s**

*Identity:* **see heat**. Warm-blooded entities glow through walls continuously, colour-coded by temperature. It
gives no names or numbers, only presence and shape.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Range | 16 | 24 | 32 | 40 | 48 |
| Wall penetration | 2 blocks | 4 | 8 | 12 | unlimited within range |
| Unlocks | — | Sees entities with the Invisibility effect | Heat-source blocks (lava, fire, lit furnaces, Create **Blaze Burners**) | Negates the *Darkness* and *Blindness* visual effects while on | **Heat trails:** footprints of players and mobs, visible for 10 s |

**Temperature palette:** the default player and mob palette is orange. Blazes, magma cubes, striders and burning
entities show white-hot. Entities in powder snow show blue. **Undead are cold and invisible to Thermal**; at T5
they show as faint grey.

**Limits:** while on, the screen gets a strong colour wash, which makes block detail harder to read. No
identification. Silent Actuators T5 users leave no heat trails.

**Deck:** `thermal.active` (read/write), `thermal.signatures` (a list of `{position, temperature}`, without entity
handles, so scripts get anonymous presence data only).

---

## Machine Analyzer

**Ocular · Passive (+ overlays) · Load 2/2/3/3/4 · Charge: overlays 0.5 C/s**

*Identity:* **engineer's vision**. It includes everything Create's Engineer's Goggles show, without the helmet
slot, plus data the goggles don't show. It is also the deck's way to **bind machines just by looking at them**.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Readouts | Goggle info, exact RPM and direction, redstone in/out strength, Redstone Link frequency items | + processing progress (press, mixer, deployer, saw, millstone, crushing wheels), basin, depot and belt contents, fluid amounts | + **area kinetic overlay** (r = 8): speed and direction of every kinetic block | + **overstress tracing** (r = 16): highlights the consumers responsible | + 60 s **history graphs** (RPM, stress) on looked-at blocks, r = 32 |
| Virtual ports | 0 | 1 | 2 | 4 | 8 |

A **virtual port** binds a looked-at block to the deck as if a Neural Port were attached to it. It stays valid
while the block is within Machine Analyzer range (8/12/16/24/32) of the user.

**Limits:** only shows data for blocks the user looks at, or within the overlay radius. It has no combat value.

**Deck:** `analyzer.looked_at` (a port handle or `NULL`), `BIND analyzer.looked_at AS "<name>"`.
Event `machine_looked_at(port)`.

---

## Ballistic Visor

**Ocular · Passive · Load 2/3/3/4/4 · Charge —**

*Identity:* predicts **projectile paths**. It shows incoming projectiles that will pass near the user (arc, impact
point and time to impact), and the trajectory of the user's own aimed ranged weapon.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Incoming tracking radius | 8 | 12 | 16 | 24 | 32 |
| Max tracked projectiles | 3 | 5 | 8 | 12 | 20 |
| Own trajectory | bow, crossbow | + tridents, throwables | + Create **Potato Cannon** (per-ammo gravity and drag) and Ballistic Module | + **blast radius preview** for TNT, fireballs and creepers about to explode | + **lethal-hit warning**: marks projectiles whose damage would kill the user |

**Limits:** information only. Without an AES or Kerenzikov, the user still has to dodge manually.

**Deck:** `ballistics.threats` (list of `{projectile, eta, damage}`), shared event `projectile_incoming`.

---

## Optical Camouflage

**Ocular · Active · Load 6/6/7/8/9 · Charge 40/45/50/55/60 + 2 C/s**

*Identity:* **temporary concealment**. The user turns into a faint shimmer. Mobs lose track of them and detection
ranges collapse.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Duration | 4 s | 6 s | 8 s | 10 s | 14 s |
| Cooldown | 60 s | 50 s | 40 s | 35 s | 25 s |
| Mob detection range multiplier (still / moving) | 0.5 / 0.8 | 0.4 / 0.7 | 0.25 / 0.6 | 0.15 / 0.5 | 0.05 / 0.35 |
| Aggro drop delay (existing targeters forget the user) | 3 s | 2.5 s | 2 s | 1.5 s | instant |
| Unlocks | — | — | Hides armor and held items | **Ambush:** the first attack out of camo deals +50% damage | **Re-cloak:** camo survives one attack and re-engages after 1 s |

**Limits:**
- Attacking, using an item on an entity, or taking damage **breaks** camo (except T5 re-cloak).
- Sprinting counts as moving, and the shimmer is visible in rain or water.
- **Hard counters:** Thermal Vision (any tier, outlines the user's heat), Entity Scanner T5 (reveals camo ≤ T4), and
  the Warden, which senses vibrations (Silent Actuators help).
- One of the highest Load costs among optics.

**Deck:** `camo.active`, `camo.ready`, `ACTIVATE camo`, `DEACTIVATE camo`. Event `camo_broken(reason)`.

---

## Target Tracking

**Ocular · Active (tag) · Load 3/3/4/4/5 · Charge 3 per tag**

*Identity:* **persistently tracks chosen targets** and feeds them to the deck. Tagged entities keep an on-screen
marker with distance through walls, and stay addressable by scripts as `tracked[n]`.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Max tags | 1 | 2 | 3 | 5 | 8 |
| Tag range | 24 | 32 | 48 | 64 | 96 |
| Persistence out of line of sight | 10 s | 20 s | 40 s | 90 s | until out of range |
| Unlocks | — | Last-known-position marker when a tag is lost | **Auto-tag attackers** (anyone who damages the user) | **Share tags** with teammates through a deck Remote Sensor Uplink | **Tracer tagging:** Ballistic Module hits apply a tag; Combat Predictor Precog ghosts show for tagged targets |

**Limits:** tags are lost on dimension change or when the target leaves range. A player who is tagged with an
Entity Scanner T4+ installed sees a "Tracked" warning.

**Deck:** `tracked` (list), `TAG <entity>`, `UNTAG <entity>`. Events `target_tagged(entity)` and `target_lost(entity)`.
