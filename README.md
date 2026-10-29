# Create: Cybernetics

An add-on for [Create](https://github.com/Creators-of-Create/Create) on **Minecraft 1.21.1 / NeoForge** that adds
cybernetic implants, programmable cyberdecks and modular character augmentation.

Players **manufacture** implants on Create production lines, **install** them on a kinetic surgical rig, and
**program** their behaviour with a cyberdeck. The cyberdeck can also read and drive Create machinery. Implant tiers
make the existing abilities stronger; they never add duplicate items.

> Status: **Layer 2 (scaling)**. All 46 implant types exist at Tiers I-V with their real numeric rows, with the
> upgrade chain, timed Rig operations (RPM, gel, aborts), Strain, Coprocessors and parallel programs, Machine Interface
> Tiers I-II, the Proximity Sensor and the ISA-2 Synapse language. The Synapse language is compiled and unit-tested; the
> NeoForge-dependent code is compiled by CI but has not been run in a game yet (see the roadmap). See
> [Implementation layers](docs/design/09-roadmap.md).

## Design specification

| # | Document | Contents |
|---|----------|----------|
| 0 | [Overview & principles](docs/design/README.md) | Pillars, glossary, conventions |
| 1 | [Core systems](docs/design/01-core-systems.md) | Body slots, Neural Load, Charge, installation, activation, HUD |
| 2 | [Implant catalog](docs/design/implants/README.md) | All eight implant categories, tier tables, limitations |
| 3 | [Cyberdeck hardware](docs/design/03-cyberdeck.md) | Chassis, modules, resources, OS, devices |
| 4 | [Synapse scripting language](docs/design/04-synapse-language.md) | Syntax, events, APIs, execution limits, examples |
| 5 | [Create integration](docs/design/05-create-integration.md) | New machines, fluids, machine data access, Redstone Links, logistics |
| 6 | [Progression & crafting](docs/design/06-progression-and-crafting.md) | Five technology tiers, materials, production chains, upgrade rules |
| 7 | [Balance & builds](docs/design/07-balance-and-builds.md) | Specialisations, example loadouts, counterplay |
| 8 | [Technical architecture](docs/design/08-technical-architecture.md) | NeoForge/Create implementation plan |
| 9 | [Implementation layers](docs/design/09-roadmap.md) | Five layers of increasing complexity, each covering all content |

## Target platform

- Minecraft **1.21.1**, NeoForge **21.1.x**, Java **21**
- Create **6.0.x** for 1.21.1 (with its bundled Ponder and Flywheel)
- Mod id: `cyberimplants`

## Building

Requires JDK 21.

```sh
./gradlew build            # full mod build (needs NeoForge, Create and its libraries from Maven)
./gradlew runClient        # dev client
./gradlew runGameTestServer
./scripts/test-synapse.sh  # Synapse language tests only; needs just a JDK and Maven Central
python3 scripts/gen_layer1_data.py  # regenerates stats (from scripts/layer2_tiers.py), recipes, models, textures and lang keys
```

The Maven repositories used are `maven.neoforged.net`, `maven.createmod.net` and `maven.tterrag.com`.
The Create (6.0.8-169), Ponder and Flywheel pins in `gradle.properties` were checked against `maven.createmod.net`.
Registrate is not a dependency: its artifact could not be found in either Maven repo, and this mod does not use it, so a dev run may need it added if Create asks for it.

## Repository layout

```
src/main/java/dev/cyberimplants
  registry/        deferred registers (+ the implant_type registry)
  body/            BodyState attachment, slots, sync events
  data/            implant stats codecs and reload listener
  network/         payloads
  config/          server and client config
  compat/create/   the only package allowed to touch Create
  implant/         catalog of types, items, actions and one behaviour class per implant, by category
  combat/          the damage mitigation pipeline
  deck/            binds Synapse programs to the player (host, handles, storage, events)
  client/          HUD, key bindings, client-side movement, optics overlays and screens
  gametest/        Layer 1 and Layer 2 GameTests
  synapse/         scripting language (no Minecraft imports): lexer, parser, compiler, VM
src/main/templates/META-INF/neoforge.mods.toml
src/test/java      JUnit tests
docs/design/       design specification
```
