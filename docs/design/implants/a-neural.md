# A. Neural Systems

**Region:** Cortex (2 slots). Neural implants change how the player perceives and reacts to time and threats.
With only two Cortex slots, the choice here defines a build's combat rhythm.

---

## Sandevistan

**Cortex · Active · Load 9/10/11/12/14 · Charge 60/90/120/150/200**

*Identity:* creates a **stationary temporal field**, a sphere in which every other entity is slowed or frozen while
the user acts at full speed. It behaves like Faceless Void's *Chronosphere*: it is a *place*, not a personal speed
buff. The field stays where it was cast, and the user can step in and out of it.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Field radius | 3 | 4 | 5 | 6 | 7 |
| Duration | 2.0 s | 2.5 s | 3.0 s | 3.5 s | 4.5 s |
| Time scale inside field | 25% | 15% | 0% (freeze) | 0% | 0% |
| Cast point | on self | on self | up to 8 blocks (look target) | 12 blocks | 16 blocks |
| Cooldown | 150 s | 120 s | 100 s | 80 s | 60 s |
| Unlocks | — | Projectiles inside are halted in mid-air and keep their velocity for release | Full freeze | **Kinetic Debt:** knockback dealt to frozen entities is stored and applied all at once on release | **Temporal Anchor:** teammates (scoreboard team or deck allow-list) are unaffected; user gains +25% attack speed inside |

**Behaviour details**

- Affected entities run only a *time-scale fraction* of their ticks (25% means one tick in four). AI, movement,
  attack timers, projectile flight, fuse timers (creepers, TNT) and potion durations all slow down together.
- **Damage is applied normally**, and the hurt cooldown of frozen entities keeps counting down at real speed. The
  user can land repeated hits at their normal attack cadence. This hit window is the reason to use a Sandevistan.
- Entities entering the field after it is cast are affected immediately. Entities leaving it resume normal time.
- The field does **not** affect blocks, block entities or Create contraptions. This is a performance and grief
  safeguard (server config `sandevistan.affectBlockEntities`, default `false`).

**Limits**

- A heavy Load and Charge cost, and the longest cooldown in the Neural category.
- **Temporal Backlash:** when the field ends, the user gets Slowness I for 3 s (T1–T4) or 1.5 s (T5).
- The field is stationary, so enemies outside it are unaffected and ranged enemies can still shoot in.
- `#cyberimplants:augment_resistant` entities are slowed to at most 50% instead of frozen. Players have a 30% time
  scale floor by default.

**Deck:** `sandevistan.ready`, `.active`, `.cooldown`, `.radius_max`, `.duration_max`.
`ACTIVATE sandevistan [AT <entity|position>] [WITH duration = <d>, radius = <r>]`. Values are clamped to the tier
maximum. Using less than the maximum refunds cooldown: `cooldown × (0.5 + 0.5 × used_fraction)`.
Events: `chrono_field_started(position)`, `chrono_field_ended`.

**Synergies:** Combat Predictor, which shows which frozen enemy will act first on release. Kinetic Debt combined
with Power Actuators. Counter: Emergency Response System and AES, because they fire on players' behalf even while
frozen.

---

## Reflex Booster

**Cortex · Passive (+ Toggle at T3+) · Load 4/4/5/5/6 · Charge: Overdrive only**

*Identity:* speeds up **actions**, not running: attacks, bow draw, crossbow charge, eating and drinking, and how
fast a shield comes up.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Attack speed | +5% | +10% | +15% | +20% | +25% |
| Item use speed (bows, crossbows, food, potions) | +10% | +15% | +25% | +35% | +50% |
| Shield raise delay (vanilla 5 t) | 5 t | 4 t | 3 t | 2 t | 0 t |
| Walk speed | +2% | +3% | +4% | +5% | +6% |
| Overdrive (toggle) | — | — | ×2 all bonuses, 3 C/s | ×2, 2 C/s | ×2, 1.5 C/s |

**Limits:** while Overdrive is on, hunger exhaustion is +50%. Bonuses don't apply to Create's Potato Cannon reload
(that weapon is air-limited by design).

**Deck:** `reflex.overdrive` (read/write), event `overdrive_changed`.

---

## Combat Predictor

**Cortex · Passive (aim assist toggle) · Load 5/5/6/6/7 · Charge: aim assist 0.5 C/s**

*Identity:* predicts **where enemies will be and when they will strike**.

- **Lead indicator:** while the player holds a ranged weapon (bow, crossbow, trident, Create Potato Cannon,
  Ballistic Module), a marker shows where to aim to hit the looked-at or tracked target. It accounts for the
  target's velocity and the projectile's speed and gravity.
- **Telegraph warnings:** windups are highlighted with an "incoming" glyph and countdown. Covered windups: creeper
  fuse, skeleton draw, evoker fang cast, ravager roar, warden sonic charge, blaze volley, breeze wind-up.
- **Counter window:** a melee hit that lands *during* a telegraphed windup deals bonus damage.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Prediction horizon | 0.5 s | 0.75 s | 1.0 s | 1.5 s | 2.0 s |
| Simultaneous lead indicators | 1 | 1 | 2 | 3 | 4 |
| Counter-window bonus | — | +10% | +20% | +30% | +40% |
| Aim assist (camera nudge toward lead point while drawing) | — | — | low | medium | high |
| Unlocks | — | Telegraph warnings | Counter window shown as a timed ring | Predictions use the mob's **actual navigation path** | **Precog:** shows a ghost of where each tracked target will be after the horizon |

**Limits:** prediction is only accurate for straight-line or path-following movement. Players dodging erratically
"break" the indicator, which is a natural PvP counter. Aim assist is disabled against players unless server config
`combatPredictor.aimAssistPvp = true` (default `false`).

**Deck:** `predict.position(entity, seconds)` → position, `predict.windup(entity)` → `{attack, eta}` or `NULL`.
Event `enemy_telegraph(entity, attack, eta)`.

---

## Automatic Evasion System (AES)

**Cortex · Triggered · Load 5/5/6/6/7 · Charge 10 per evasion**

*Identity:* **automatically sidesteps incoming projectiles** when user-defined conditions are met. The player
writes the rules; the implant reacts.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Evasion charges (stack) | 1 | 1 | 2 | 2 | 3 |
| Recharge per charge | 12 s | 10 s | 8 s | 6 s | 4 s |
| Detection window before impact | 4 t | 5 t | 6 t | 7 t | 8 t |
| Sidestep distance | 1.5 | 1.75 | 2.0 | 2.5 | 3.0 |
| Projectiles recognised | arrows | + tridents, throwables | + fireballs, shulker bullets | + wind charges, Create potato projectiles | all `#cyberimplants:evadable` |
| Condition slots | 1 | 2 | 3 | 4 | 6 |
| Unlocks | — | **Ledge safety:** never sidesteps off an edge > 3 blocks | — | — | **Parry:** if a melee weapon is held, reflect the projectile instead of dodging |

**Conditions** are configured in the implant wheel or from a deck with `CONFIGURE aes ...`. Each slot holds one
predicate, and all predicates must pass:
`health_below <pct>`, `health_above <pct>`, `not_attacking`, `sprinting`, `sneaking_blocks` (never evade while
sneaking), `min_damage <hp>`, `source <entity-type tag>`, `airborne_only`, `grounded_only`.

**Limits:** never evades melee. Needs free space in the dodge direction. A dodge into a wall is wasted and the
charge is still consumed. AES evasions are visible to the attacker as a flicker trail.

**Deck:** `aes.charges`, `aes.enabled` (write), `CONFIGURE aes condition[<n>] = <predicate>`.
Events: `projectile_incoming(projectile, eta, damage)` (shared with Ballistic Visor) and `evaded(projectile)`.

**Synergy:** with Kerenzikov installed, each AES evasion opens a Kerenzikov window. With a Ballistic Visor, the
detection window is +1 t.

---

## Kerenzikov

**Cortex · Input-bound · Load 4/4/5/5/6 · Charge 8 per dodge**

*Identity:* gives the player a **dodge** (a short dash) and turns every dodge into a brief **reaction window** in
which they act at peak speed. It is personal, short and manual. Sandevistan is area-wide; Kerenzikov affects only
the user.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Dodge distance | 2.0 | 2.5 | 3.0 | 3.5 | 4.0 |
| Dodge cooldown | 3.0 s | 2.5 s | 2.0 s | 1.5 s | 1.0 s |
| Reaction window | 0.4 s | 0.5 s | 0.6 s | 0.8 s | 1.0 s |
| Window effects | attack cooldown fully reset | + bows/crossbows fire at full draw | + window also opens on **sharp turns** (≥ 90° velocity change while sprinting) | + 6 t of **i-frames** during the dash | + **Chain:** a kill inside the window opens a new window at no cost |

**Limits:** the window is short and has to be used deliberately. A dodge can't be cancelled, so dodging off a cliff
is the player's fault. Doesn't work while riding, swimming or using an elytra.

**Deck:** `kerenzikov.window_active`. Events `dodge(direction)` and `window_opened`. Scripts **cannot** trigger a
dodge, because that would synthesise player movement (see [Synapse safety rules](../04-synapse-language.md#9-safety-and-fairness-rules)).
