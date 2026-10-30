# 6. Progression and Crafting

## 6.1 Tier overview

| Tier | Name | Materials | Manufacturing | Unlocks |
|------|------|-----------|---------------|---------|
| I | **Civilian** | Andesite alloy, iron/copper sheets, copper wire, synthetic fiber, Bio-Interface Gel | Press, saw, unheated mixer, deployer, spout; hand-fed | Basic function of every implant; ISA-1 decks (one program) |
| II | **Industrial** | Brass, Precision Mechanisms, Tempered Steel, synthetic muscle | Heated mixing, bulk blasting, first sequenced assembly loops, mechanical crafting | Stronger baselines, first "unlock" features; ISA-2 (loops, timers, device writes) |
| III | **Advanced** | Electron Tubes, Silicon Wafers, Logic Chips, Myomer Bundles, Conductive Ink | Superheated mixing, crushing, **Precision Etcher ≥ 64 RPM**, multi-fluid assembly | Signature advanced functions (Sandevistan freeze, Ballistic Visor, ERS pre-damage); ISA-3 (functions, lists, messages) |
| IV | **Military** | Sturdy Sheets, Composite Plate, Military Processors, Cryo-Coolant, Nanite Suspension | **Etcher ≥ 128 RPM**, 3–4-loop sequenced assembly, several parallel fluid supplies | Specialised combat and defence functions; ISA-4; display and train control |
| V | **Experimental** | Echo Lattice, Experimental Core, category exotics (Nether Star, Breeze Rods, Totem, Netherite …) | **Etcher at 256 RPM**, rare-resource lines, Neural Gel | Unique experimental features; ISA-5; logistics requests |

## 6.2 Upgrade rules

1. **One item per implant type.** Tier is a data component (`cyberimplants:tier`, 1–5). Item names display as
   *"Sandevistan [Mk. III — Advanced]"*.
2. **Tier I is crafted. Tiers II–V are upgrades.** Each upgrade is a **sequenced assembly** whose base input is the
   *same implant at the previous tier* (matched by component). The previous implant is consumed and its identity is
   carried forward. The player can't skip tiers or craft a high tier from scratch.
3. **Upgrades follow a shared template per tier.** Each tier's template uses that tier's *shared components*, and
   each category adds one *category part* (§6.4). The template keeps the recipe count manageable (one template per
   tier and category) while every category still needs its own sub-line.
4. **Settings live on the player, not the item.** Upgrading doesn't lose Nanocoating formulas, AES conditions and
   similar settings (see [Core systems §1.5](01-core-systems.md#15-installation-the-implantation-rig)).
5. **Hosts cap children.** Limb modules ≤ arm frame tier; deck modules ≤ chassis tier. The Rig needs the tier's
   minimum RPM to install.
6. **Recipes are data.** All recipes are datapack JSON, so modpacks can swap rare inputs or lengthen chains.

## 6.3 Shared components by tier

### Tier I — Civilian

| Component | Recipe |
|-----------|--------|
| Copper Wire | **Cutting** (Mechanical Saw): Copper Sheet → 2 Copper Wire |
| Synthetic Fiber | **Mixing** (unheated): 2 String + 2 Dried Kelp + Slime Ball → 4 Synthetic Fiber |
| Mechanical Servo | **Deploying**: Cogwheel onto Iron Sheet → Mechanical Servo |
| Civilian Casing | **Pressing**: Andesite Alloy + Synthetic Fiber (in basin) → Civilian Casing |

### Tier II — Industrial

| Component | Recipe |
|-----------|--------|
| Crude Steel Blend | **Mixing** (heated): Iron Ingot + 2 Coal/Charcoal → Crude Steel Blend |
| Tempered Steel Ingot | **Bulk Blasting** (Encased Fan through lava): Crude Steel Blend → Tempered Steel Ingot |
| Tempered Steel Sheet | **Pressing**: Tempered Steel Ingot → Tempered Steel Sheet |
| Precision Servo | **Sequenced assembly** on Mechanical Servo: deploy Precision Mechanism → deploy Copper Wire → press (1 loop) |
| Synthetic Muscle | **Mixing** (heated): 3 Synthetic Fiber + 100 mB Bio-Interface Gel + Redstone → 2 Synthetic Muscle |

### Tier III — Advanced

| Component | Recipe |
|-----------|--------|
| Silica Dust | **Crushing** / Millstone: Nether Quartz → 2 Silica Dust (+ 25% extra) |
| Silicon Boule | **Mixing** (superheated): 4 Silica Dust + Coal → Silicon Boule |
| Silicon Wafer | **Cutting**: Silicon Boule → 6 Silicon Wafer |
| Logic Chip | **Sequenced assembly** on Silicon Wafer: spout 50 mB Conductive Ink → **etch (≥ 64 RPM)** → deploy Electron Tube → **etch (≥ 64 RPM)** (2 loops) |
| Myomer Bundle | **Mixing** (heated): 3 Synthetic Muscle + 100 mB Bio-Interface Gel + Slime Ball → Myomer Bundle; then **Pressing** |

### Tier IV — Military

| Component | Recipe |
|-----------|--------|
| Composite Plate | **Sequenced assembly** on Sturdy Sheet (Create): deploy Tempered Steel Sheet → deploy Netherite Scrap → press (1 loop) |
| Military Processor | **Sequenced assembly** on Logic Chip: deploy Golden Sheet → **etch (≥ 128 RPM)** → spout 100 mB Cryo-Coolant → deploy Logic Chip → **etch (≥ 128 RPM)** (3 loops) |
| Nanite Suspension (fluid) | See [Create integration §5.3](05-create-integration.md#53-fluids) |

### Tier V — Experimental

| Component | Recipe |
|-----------|--------|
| Echo Lattice | **Mixing** (superheated): Echo Shard + 2 Polished Rose Quartz + 100 mB Nanite Suspension → 2 Echo Lattice |
| Experimental Core | **Sequenced assembly** on Military Processor: deploy Echo Lattice → **etch at 256 RPM** → spout 100 mB Nanite Suspension → **etch at 256 RPM** → press (1 loop: every Tier V upgrade already takes four cores) |
| Quantum Substrate | **Mixing** (superheated): Amethyst Shard + Ender Pearl + Echo Shard (the deck's Tier V exotic) |

## 6.4 Category parts and exotics

Each category's upgrade template adds one part per tier. Most parts reuse vanilla or Create items, so new item
count stays small. Tier V adds a category **exotic**, a rare resource that gates experimental builds.

| Category | T1 (crafting) | T2 part | T3 part | T4 part | T5 exotic |
|----------|---------------|---------|---------|---------|-----------|
| Cybernetic Core | Civilian Casing + Copper Wire ×4 + Redstone Block | Precision Servo + Electron Tube | Logic Chip ×2 + Myomer Bundle | Military Processor + Composite Plate | Echo Lattice + Netherite Ingot |
| A. Neural | Civilian Casing + Copper Wire + Redstone + Clock | Precision Servo | Logic Chip ×2 | Military Processor + Neural Gel | Echo Lattice (×2); **Sandevistan:** + Nether Star |
| B. Deck chassis & modules | Civilian Casing + Comparator/Repeater + Copper Wire | Electron Tube ×2 | Logic Chip ×2 | Military Processor | Quantum Substrate (superheated mix: Amethyst Shard + Ender Pearl + Echo Shard) |
| C. Skeletal | Civilian Casing + Iron Sheet + Bone Block | Tempered Steel Sheet ×2 | Myomer Bundle | Composite Plate | Netherite Ingot |
| D. Limbs | Civilian Casing + Mechanical Servo ×2 | Precision Servo + Tempered Steel Sheet | Myomer Bundle + Logic Chip | Composite Plate + Military Processor | Breeze Rod ×2 + Netherite Ingot |
| E. Optical | Civilian Casing + Glass Pane + Spyglass | Polished Rose Quartz | Logic Chip + Phase Lens¹ | Military Processor | Phase Lens + Echo Lattice |
| F. Circulatory | Civilian Casing + Synthetic Fiber + Glistering Melon Slice | Synthetic Muscle + Bio-Gel (spout) | Myomer Bundle + Logic Chip | Neural Gel (spout) + Military Processor | Ghast Tear ×2; **Defibrillator:** + Totem of Undying |
| G. Subdermal | Civilian Casing + Iron Sheet ×2 + Armadillo Scute | Tempered Steel Sheet ×2 | Logic Chip + Inked Steel Sheet² | Composite Plate + Nanite Suspension (spout) | Shulker Shell ×2 + Netherite Ingot |
| H. Mobility | Civilian Casing + Synthetic Fiber + Piston | Synthetic Muscle ×2 | Myomer Bundle ×2 | Composite Plate + Military Processor | Breeze Rod ×3 |

¹ **Phase Lens:** cut an Amethyst Shard into a Ground Lens (saw), then **bulk haunt** it (Encased Fan through soul
fire). This is the mod's use of Create's haunting.
² **Inked Steel Sheet:** a Tempered Steel Sheet spouted with 100 mB Conductive Ink. Nanite Suspension only appears
at T4.

### Template: Tier I crafting (basic implants)

Tier I implants are **crafting-table** recipes from the Civilian parts, so the first implant comes right after the andesite
age (§6.5): `a b a / w X w / a C a`, where `a` is the category's frame part (×4), `b` its category part, `w` Copper Wire,
`X` the implant's signature item and `C` a Civilian Casing. The parts already need a saw, a mixer, a deployer and a press,
so Tier I is still a small machine setup. (Earlier drafts used Mechanical Crafters, which need brass and so came too late.)

| Category | Frame part `a` | Category part `b` |
|----------|----------------|-------------------|
| Core | Copper Sheet | Mechanical Servo |
| Neural, Cyberdeck | Copper Wire | Redstone |
| Skeletal | Iron Sheet | Bone Block |
| Limbs | Iron Sheet | Mechanical Servo |
| Optical | Glass Pane | Copper Sheet |
| Circulatory | Synthetic Fiber | Copper Sheet |
| Subdermal | Iron Sheet | Synthetic Fiber |
| Mobility | Synthetic Fiber | Piston |

### Item tags for rare inputs

Rare or biome-bound inputs are item tags so a modpack can swap them without new recipes: `cyberimplants:exotics/<category>`
(plus `exotics/sandevistan` and `exotics/emergency_defibrillator`), `composite_reinforcement` (Netherite Scrap),
`echo_sources` (Echo Shard), `cryo_ice` (packed ice, blue ice, snow block) and `fiber_fillers` (dried kelp, paper).

### Etching

Every **etch** step of the chains below runs on the **Precision Etcher** (§5), a `cyberimplants:etching` step of the
sequenced assembly with the tier's `min_rpm`: 64 RPM for Tier III, 128 for Tier IV and 256 for Tier V. Below that
speed the Etcher lets the item pass, so the assembly stalls until the line spins fast enough.

### Template: upgrade sequences

| Upgrade | Steps (one loop) | Loops |
|---------|------------------|-------|
| I → II | deploy **category T2 part** → deploy Tempered Steel Sheet → spout 100 mB Bio-Interface Gel → press | 1 |
| II → III | deploy **category T3 part** → spout 50 mB Conductive Ink → **etch ≥ 64 RPM** → deploy Logic Chip | 2 |
| III → IV | deploy **category T4 part** → spout 100 mB Cryo-Coolant → **etch ≥ 128 RPM** → deploy Composite Plate → press | 3 |
| IV → V | deploy **category exotic** → deploy Experimental Core → spout 100 mB Nanite Suspension → **etch at 256 RPM** | 4 |

Because the loop count grows and the steps need two or three different fluids on the same belt, T4–T5 production
is impractical by hand. Players are pushed to build dedicated, automated assembly lines, as Create's own Precision
Mechanism encourages at a smaller scale.

## 6.5 Progression pacing (targets)

| In-game milestone | Expected game stage |
|-----------|---------------------|
| First implant (Core T1 + one T1 implant) | After the andesite age: a press, saw, mixer and deployer on a waterwheel |
| A full T1 body | Early brass age |
| First T2 upgrades | Blaze Burner and fan processing online |
| First T3 (Sandevistan freeze, deck ISA-3) | Electron tubes in bulk, superheated mixing, a 64+ RPM etch line |
| First T4 | Nether infrastructure (netherite scrap), Sturdy Sheet production, a 128 RPM etch line, coolant supply |
| First T5 | Ancient City (echo shards), Trial Chambers (breeze rods), a Wither kill (Sandevistan T5), a 256 RPM line |
