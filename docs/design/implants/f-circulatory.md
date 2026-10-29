# F. Circulatory and Vital Systems

**Region:** Circulatory (3 slots). These implants keep the user alive and manage the energy economy. The Biomonitor
is the deck's only window into the user's own body.

---

## Biomonitor

**Circulatory · Passive · Load 1/1/2/2/2 · Charge —**

*Identity:* **knows the body's state** and acts on thresholds. It provides an exact vitals HUD and threshold
alarms, and it is the data source for every health-related deck event.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Threshold alarms | 1 | 2 | 4 | 6 | 8 |
| Data | health, hunger, saturation | + active effects, air, freeze, last-5-hits damage log | + damage-type breakdown, damage-per-second estimate | + **teammates' vitals** (Biomonitor users on the same team within 64 blocks) | + **time-to-death** estimate at current DPS |
| Redstone Link broadcast channels | — | 1 | 2 | 3 | 4 |

**Redstone Link broadcast:** each channel is a Create Redstone Link frequency (two items, set in the UI) that
transmits a chosen vital as a signal strength 0–15. For example, `health fraction × 15` lets a base light a warning
lamp or open the medbay door when someone is hurt. It uses Create's normal link range.

**Limits:** none. It is cheap, but it takes a Circulatory slot. Without it, decks can't read health.

**Deck:** `bio.health`, `bio.max_health`, `bio.health_pct`, `bio.hunger`, `bio.effects`, `bio.dps` (T3+),
`bio.time_to_death` (T5). Events `damaged(amount, type, attacker)`, `health_below(pct)`, `health_above(pct)`,
`effect_applied(effect, amplifier)` (T2+).

---

## Regenerator

**Circulatory · Passive (+ Burst at T5) · Load 3/3/4/5/6 · Charge 2/1.75/1.5/1.25/1 per HP healed**

*Identity:* **out-of-combat regeneration** powered by Charge instead of food.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Heal rate | 0.25 HP/s | 0.35 | 0.5 | 0.65 | 0.85 |
| Combat lockout after damage | 8 s | 6 s | 5 s | 4 s | 3 s |
| Unlocks | — | Works regardless of hunger level | Cures Poison | Cures Wither and Hunger effects | **Burst:** 3 s at ×3 rate with no lockout, 60 s cooldown (manual or deck) |

**Limits:** it stops whenever the user is hit, so it doesn't help mid-fight. It drains Charge that other implants
need. It stacks with vanilla natural regeneration.

**Deck:** `regen.healing`, `ACTIVATE regen` (Burst, T5).

---

## Adrenaline Pump

**Circulatory · Triggered · Load 2/2/3/3/4 · Charge — (costs 1 hunger point per trigger)**

*Identity:* **reactive speed**. A sizable hit floods the system and the user surges away (or back in).

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Trigger: single hit ≥ | 3 HP | 3 HP | 2 HP | 2 HP | any hit |
| Speed boost | +15% / 3 s | +20% / 3.5 s | +25% / 4 s | +30% / 5 s | +35% / 6 s |
| Cooldown | 30 s | 25 s | 20 s | 15 s | 12 s |
| Unlocks | — | Ignores Slowness during the boost | + Jump +0.5 | + Attack speed +15% | Boost doubled below 30% HP |
| Crash (Slowness I after the boost) | 4 s | 3 s | 2 s | 1 s | none |

**Limits:** it only fires when the user is hurt and can't be triggered manually. The crash punishes fights that
outlast the boost.

**Deck:** `adrenaline.ready`. Event `adrenaline_triggered`. Scripts can read it but not fire it.

---

## Emergency Defibrillator

**Circulatory · Triggered (Emergency) · Load 4/4/5/5/6 · Charge 80/100/120/140/160 (can use Energy Buffer)**

*Identity:* **cheat death once** per long cooldown. When fatal damage lands, the defibrillator restarts the heart.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Health restored | 2 HP | 4 | 6 | 8 | 10 |
| After revival | Resistance I 2 s | Resistance II 2 s | Resistance II 3 s, extinguish | Resistance III 3 s, clears negative effects | Resistance IV 4 s, knockback nova (r = 4) |
| Cooldown | 30 min | 25 min | 20 min | 15 min | 10 min |

**Rules and limits:**
- Doesn't trigger on damage tagged `bypasses_invulnerability` (void, `/kill`).
- **Fails if the main battery plus Energy Buffer holds less than the required Charge.** Running empty on implants
  can cost the user their life.
- A held Totem of Undying takes priority, so the defibrillator isn't consumed (config).
- The cooldown counts down only while the user is online and persists through death.

**Deck:** `defib.ready`, `defib.cooldown`. Event `defib_fired`. Scripts can't trigger it but can plan around it
(e.g. retreat when it is on cooldown).

---

## Energy Buffer

**Circulatory · Passive · Load 2/2/3/3/4 · Charge: storage**

*Identity:* a **reserve energy pool** that only Emergency activations may draw on.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Capacity | 50 C | 100 C | 175 C | 250 C | 400 C |
| Fill rate (from main battery overflow and metabolic surplus) | 1 C/s | 1.5 | 2 | 3 | 4 |
| Unlocks | — | **Manual dump:** move the whole buffer into the main battery (60 s cooldown) | Deck access | Charges directly on a Kinetic Charger at ×2 rate | **Surge:** once per minute, one *non*-Emergency activation may draw from the buffer |

**Limits:** Charge in the buffer can't be used for normal activations (except via dump or T5 Surge). It only fills
when the main battery is full.

**Deck:** `buffer.charge`, `buffer.capacity`, `DUMP buffer` (T2+).

---

## Emergency Response System (ERS)

**Circulatory · Triggered (Emergency) · Load 3/3/4/4/5 · Charge: as activated implants**

*Identity:* **hardwired, zero-latency defensive automation**. ERS computes a **Danger score** every tick. When the
score crosses a threshold, it fires the user's chosen defensive implants in priority order. It needs no deck and no
programming, and at T3+ it acts *before* the triggering hit resolves.

| Stat | T1 | T2 | T3 | T4 | T5 |
|------|----|----|----|----|----|
| Response slots (implants fired per trigger) | 1 | 2 | 2 | 3 | 4 |
| Danger inputs | health % | + on fire, drowning, freezing | + hostiles within 6 blocks, incoming DPS | + incoming lethal fall, imminent explosions (with Ballistic Visor) | + user-set input weights |
| Pre-damage reaction | no (next tick) | no | **yes** | yes | yes |
| Cooldown between responses | 30 s | 25 s | 20 s | 15 s | 10 s |

**Compatible implants (Emergency-capable):** Kinetic Shield (emergency recharge), Emergency Shell (out-of-threshold
trigger), Optical Camouflage, Regenerator Burst, Hydraulic Legs (escape leap upward), and Sandevistan (T4+ ERS
only).

**Limits:** fixed logic. It can't tell a creeper from a zombie, only "danger". Each response spends the fired
implants' normal costs and cooldowns. It occupies a Circulatory slot that could hold a Regenerator or Defibrillator.

**Deck:** `ers.enabled` (write), `ers.danger` (read), `CONFIGURE ers threshold = <n>`. Event `ers_fired(implants)`.
A deck can **tune** ERS but not replace its zero-latency path.
