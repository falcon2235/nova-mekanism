# Changelog

All notable changes to Nova Mekanism are documented here. This project targets
Minecraft 1.20.1 (Forge) and follows loose semantic versioning.

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

[1.0.2]: https://github.com/falcon2235/nova-mekanism/releases/tag/v1.0.2
[1.0.1]: https://github.com/falcon2235/nova-mekanism/releases/tag/v1.0.1
[1.0.0]: https://github.com/falcon2235/nova-mekanism/releases/tag/v1.0.0
