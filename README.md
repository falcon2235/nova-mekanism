# Nova Mekanism

**A massive GregTech-inspired end-game expansion for [Mekanism](https://github.com/mekanism/Mekanism), for Minecraft 1.20.1 (Forge).**

Nova Mekanism turns Mekanism's processing into a full progression of multiblock machines — from simple parallel-processing chambers all the way to a chunk-sized black hole stabilizer that produces trans-dimensional materials and a craftable Creative Energy Cube.

> The internal mod id remains `mekanism_more_multiblock` for save compatibility; the display name is **Nova Mekanism**.

---

## Requirements

| | Version |
|---|---|
| Minecraft | 1.20.1 |
| Mod loader | **Forge 47.4.x** (not NeoForge) |
| **Mekanism** | 10.4.x — **required** |
| **Mekanism Generators** | 10.4.x — **required** (deuterium/tritium fusion) |
| JEI | 15.20.x — optional (recipe & structure viewer) |

---

## Feature overview

- **Parallel-processing multiblocks** — enriching, crushing and smelting, from 10× up to **300×** parallel, with full Mekanism speed/energy upgrade support.
- **Machine ports** — energy, item in/out, gas in/out and fluid in/out ports, each with a small GUI and auto-push/pull. Ports adopt the skin of the machine they are built into.
- **GregTech-style structures** — coil towers, barrels, assembly lines, reactor rings and containment cubes, each faithfully modelled on its GregTech counterpart.
- **Deep material chains** — titanium, nickel (Mond process), cupronickel, special steel, the platinum group (GTCEu platline), and the naquadah line (GTCEu), leading into super alloy, naquadah alloy, neutronium and trans-dimensional materials.
- **Construction Terminal** — right-click any controller to auto-build its whole structure from your inventory.
- **JEI integration** — every machine has recipe categories with gas/fluid amounts and an isometric 3-D structure preview; controllers act as recipe catalysts.
- **Alternative antimatter route** — the **Large Hadron Collider** collides hydrogen protons straight into antimatter as an end-game-adjacent alternative to a hardened SPS, and a Large Chemical Reactor polonium recipe (gated behind an antimatter-pellet upgrade) offers a second polonium path.
- **GT-style petroleum power** — an **Oil Drilling Rig** pumps crude oil, the distillation tower and chemical reactor refine it (distill → desulfurize) into diesel, and a **Large Combustion Generator** burns it at 500,000 RF/t; the end-game **Annihilation Generator** converts hydrogen + antimatter under liquid-helium cooling into 800,000,000 RF/t.
- **Void Ore Miner** — mines 10 of a random rarity-weighted RAW ore every second (22 ore types, naquadah as the jackpot) for 1,000,000 RF/t; Mekanism speed upgrades roll faster.
- **JEI bill of materials** — every structure preview lists exactly how many of each block the multiblock needs.
- **Quantum conduits** — a transmitter tier above Mekanism's ultimate: quantum universal cable / mechanical pipe / pressurized tube / logistical pipe. Anything pushed in is routed instantly; right-click a face with an empty hand to toggle extraction from tanks/chests. Crafted from 8 ultimate transmitters + a superconductor.
- **Fully configurable** — `config/mekanism_more_multiblock-common.toml` exposes every machine rate (miner, rigs, generators, LHC, conduits) plus global recipe energy/time multipliers for expert-pack tuning.
- **Hardened Mekanism recipes** — the Gas-Burning Generator is gated behind titanium and the Digital Miner behind special steel (plus the existing SPS/fission hardening).
- **Tweaks to Mekanism** — the SPS (antimatter) is made harder and the fission reactor casing recipe is hardened, via a built-in high-priority data pack.

---

## The machines

Parallel machines share a fixed **3×3×4** hollow casing box (GregTech-style block sharing: only the parallel units are exclusive). Every other machine is a controller you pipe items/gas/fluid/energy directly into.

| Machine | Size (W×H×D) | Role |
|---|---|---|
| Enriching / Crushing / Smelting | 3×3×4 | Parallel Mekanism processing (up to 300×) |
| Primitive Blast Furnace | 3×3×4 | Unpowered, coal-fired iron → steel |
| Electric Blast Furnace | 3×3×4 | Coil-tiered smelting, magnesium melting |
| Large Chemical Reactor | 5×3×5 | Two-gas chemistry |
| Distillation Tower | 3×5×3 | Gas/fluid distillation |
| Mixer | 3×3×3 | Dust alloying, acid mixing |
| Large Electrolyzer | 3×3×3 | Splits raw metals into pure metals + gas |
| Large Centrifuge | 3×3×3 | Separates sludge/solutions into fractions |
| Alloy Blast Furnace | 5×5×5 barrel | Molten super/naquadah/trans-dim alloys |
| Vacuum Freezer | 3×3×3 | Freezes molten alloys into solid ingots |
| Circuit Assembly Line | 3×3×5 | Supreme circuits, superconductors, trans-dim circuits |
| Oil Drilling Rig | 5×7×5 rig | Pumps crude oil from bedrock (10 mB/t) |
| **Large Combustion Generator** | 3×3×4 engine | Burns diesel (20 mB/t) into **500,000 RF/t** |
| **Void Ore Miner** | 7×9×7 rig | Mines 10 random rarity-weighted RAW ores per second from nothing (1M RF/t; speed upgrades roll faster) |
| **Large Hadron Collider** | 33×3×33 ring | Collides hydrogen directly into antimatter (alt to the SPS); ~100M RF/t |
| **Fusion Reactor** | 15×3×15 ring | D-T fusion → helium plasma → molten stellar matter |
| **Annihilation Generator** | 7×7×7 sphere | Hydrogen + antimatter + liquid helium → **800,000,000 RF/t** |
| **Artificial Star Generator** | 9×9×9 sphere | Stellar core + hydrogen → black hole seed |
| **Black Hole Stabilizer** | 16×16×16 cube | Black hole seed → trans-dimensional metal + Creative Energy Cube |

### Heating coils

The Electric and Alloy Blast Furnaces use tiered heating coils — **copper → cupronickel → titanium → plutonium → antimatter**. Higher tiers halve the processing time per tier above a recipe's requirement, and the coils glow while the furnace runs.

---

## Progression

1. **Ore chains** — titanium, nickel/cupronickel and special steel via the EBF, reactor, distillation tower and mixer.
2. **Platinum group** (cooperite/saltpeter ores) — nitric acid → sludge → centrifuge → electrolyzer → platinum, palladium, rhodium, ruthenium, iridium.
3. **Naquadah line** (naquadah/antimony ores) — fluoroantimonic acid dissolution → centrifuge/mixer separations → enriched naquadah, naquadria, trinium; **naquadah alloy** in the alloy blast furnace.
4. **Antimatter** — just before the end-game, build the **Large Hadron Collider** (33×3×33) to collide hydrogen straight into antimatter at ~100,000,000 RF/t, alongside the hardened SPS route.
5. **Fusion** — deuterium + tritium → helium plasma → (+ antimatter) → **molten stellar matter** → **stellar core**. Also fuses naquadria into **neutronium**.
6. **Artificial star** — stellar core + a huge charge of hydrogen at **100,000,000 RF/t for 10 minutes** → **black hole seed**.
7. **Black hole stabilizer** — black hole seed at **1,000,000,000 RF/t for 30 minutes** → **10× trans-dimensional metal**.
8. **The pinnacle** — trans-dimensional metal → alloy → circuit (each requiring hundreds of millions of RF/t) → a fully-charged, craftable **Mekanism Creative Energy Cube**.

---

## Construction Terminal

Building a 16×16×16 cube by hand is not fun. Craft a **Construction Terminal** and right-click a controller: it reads the machine's blueprint and places every casing/coil/glass block from your inventory (free in creative). It only fills empty space, never breaks other blocks, and leaves your ports alone — so it is safe to re-run.

---

## Building from source

Requires **JDK 17**. The project uses ForgeGradle 6.

```bash
./gradlew build          # -> build/libs/Nova-Mekanism-1.20.1-1.0.4.jar
./gradlew runClient      # launch a dev client
```

---

## Credits & License

Recipes and multiblock structures are inspired by **GregTech CEu** and **GregTech: New Horizons**. Nova Mekanism is not affiliated with Mekanism or the GregTech teams.

Released under the [MIT License](LICENSE). © 2026 falcon2235.
