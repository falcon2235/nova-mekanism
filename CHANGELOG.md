# Changelog

All notable changes to Nova Mekanism are documented here. This project targets
Minecraft 1.20.1 (Forge) and follows loose semantic versioning.

## [1.0.4] - 2026-07-12

### Added
- **Quantum conduits** — a transmitter tier above Mekanism's ultimate: quantum
  universal cable, mechanical pipe, pressurized tube and logistical pipe.
  Anything pushed into a conduit network is routed instantly to its consumers;
  right-click a face with an empty hand to toggle extraction mode (pulls from
  tanks/chests/machine outputs at up to ~2.1B RF/t, 1,024,000 mB/t or a stack
  of items per face per tick). Crafted from 8 ultimate transmitters + a
  superconductor.
- **Full config** (`config/mekanism_more_multiblock-common.toml`) — every
  machine rate is tunable: void miner (energy/interval/yield), oil rig,
  combustion generator, annihilation generator, hadron collider and quantum
  conduit rates, plus global recipe energy/time multipliers for expert packs.
  Config reloads rebuild the recipe set live.

### Changed (semi-expert balance pass)
- **Void Ore Miner** now mines RAW ores (raw iron/tin/titanium, gems for
  non-metals) instead of ore blocks, and rolls once per second instead of every
  tick. Mekanism speed upgrades now shorten the roll interval.
- **Large Hadron Collider** antimatter output halved: 500 mB per operation
  (5 mB/t), keeping the annihilation loop strong without trivializing the SPS.
- Supreme control circuit energy cost raised to 4,000 RF/t and the
  superconductor to 10,000 RF/t.
- **Hardened Mekanism recipes**: the Gas-Burning Generator now requires
  titanium + elite circuits, and the Digital Miner requires special steel +
  elite circuits (functional parts unchanged).

## [1.0.3] - 2026-07-10

### Added
- **Void Ore Miner** — a 7×9×7 drill rig that mines ore from nothing: every tick
  it consumes 1,000,000 RF and produces 10 of one randomly chosen ore, weighted
  by rarity (22 ore types across vanilla, Mekanism and Nova Mekanism; naquadah
  is the jackpot). JEI shows each ore with its exact roll chance.
- **GT-style petroleum chain** — an Oil Drilling Rig (5×7×5) pumps crude oil
  (10 mB/t at 4,000 RF/t); the distillation tower refines it into sulfuric fuel
  and the large chemical reactor desulfurizes it (with hydrogen, sulfur
  byproduct) into diesel.
- **Large Combustion Generator** — a 3×3×4 engine multiblock (GT Large
  Combustion Engine style, gearbox rings) that burns diesel at 20 mB/t into
  **500,000 RF/t**. Energy flows out through its energy ports; cables can also
  pull from them.
- **Annihilation Generator** — a 7×7×7 containment sphere that annihilates
  hydrogen (50 mB/t) against antimatter (1 mB/t) under liquid-helium cooling
  (10 mB/t), producing **800,000,000 RF/t**. Gated behind the stellar core;
  extremely expensive (neutronium + superconductor casings).
- **Liquid helium** — helium plasma condensed in the vacuum freezer; the
  annihilation generator's coolant.
- **JEI bill of materials** — the structure preview now lists exactly how many
  of each block every multiblock needs (counted from the construction
  blueprint; heating-coil slots cycle through all valid tiers).

## [1.0.2] - 2026-07-09

### Added
- **Large Hadron Collider** — a huge 33×3×33 flat octagonal accelerator ring
  (built like the fusion reactor, ~6× its footprint) that collides hydrogen
  protons directly into antimatter at ~100,000,000 RF/t. An end-game-adjacent
  alternative to the SPS antimatter route, with a high build cost
  (accelerator casing, collider magnets, fusion glass windows).
- **Alternative polonium recipe** in the Large Chemical Reactor, gated behind a
  new **Polonium Synthesis Upgrade** whose recipe consumes antimatter pellets.

### Changed
- Internal Java package renamed from `com.chihaya.moremultiblock` to
  `com.falcon2235.moremultiblock`. (Save-facing mod id is unchanged:
  `mekanism_more_multiblock`.)

### Fixed
- Large Hadron Collider ring no longer leaves gaps at its four corners — the
  straight edges and diagonal corners now form one continuous octagon loop.

## [1.0.1] - 2026

### Added
- Active-machine visual effects: a collision-free star renders inside the
  Artificial Star Generator, and a collision-free black hole inside the
  Black Hole Stabilizer, while each is running.

## [1.0.0] - 2026

- Initial public release: parallel-processing multiblocks (up to 300×),
  full ore-processing chains (titanium, nickel, special steel, platinum group,
  naquadah), chemical/alloy/energy multiblocks (electric & alloy blast furnaces,
  vacuum freezer, distillation tower, large chemical reactor, mixer,
  electrolyzer, centrifuge, circuit assembly line, fusion reactor, artificial
  star generator, chunk-sized black hole stabilizer), trans-dimensional
  materials, a craftable Creative Energy Cube, and a construction terminal that
  auto-builds any structure.

[1.0.4]: https://github.com/falcon2235/nova-mekanism/releases/tag/v1.0.4
[1.0.3]: https://github.com/falcon2235/nova-mekanism/releases/tag/v1.0.3
[1.0.2]: https://github.com/falcon2235/nova-mekanism/releases/tag/v1.0.2
[1.0.1]: https://github.com/falcon2235/nova-mekanism/releases/tag/v1.0.1
[1.0.0]: https://github.com/falcon2235/nova-mekanism/releases/tag/v1.0.0
