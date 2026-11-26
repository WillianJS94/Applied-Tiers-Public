# Applied Tiers

Applied Tiers is an addon for Applied Energistics 2 that adds compact, higher-tier autocrafting devices while retaining AE2's native crafting CPU, ME network and energy mechanics.

## Features

| Tier | Patterns per provider | Parallel assembler operations | Equivalent coprocessors | Coprocessor idle power |
|---|---:|---:|---:|---:|
| Advanced | 18 | 2 | 2 | 2 AE/t |
| Elite | 36 | 3 | 3 | 3 AE/t |
| Ultimate | 72 | 4 | 4 | 4 AE/t |

- **Pattern Providers:** block and cable-mounted variants with compact interfaces. Convert between the two forms in a crafting grid; remove stored patterns and items first. Memory Cards transfer settings between forms of the same tier.
- **Molecular Assemblers:** independent crafting units, five shared Acceleration Cards, a shared permanent pattern and automatic, exclusive and diversified dispatch modes. Energy consumption scales with active units.
- **Crafting Co-Processing Units:** native AE2 CPU integration, including mixed-tier structures. At least one Crafting Storage block is required; each CPU still runs one active job at a time.

## Compatibility

| Dependency | Tested version |
|---|---|
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.129 |
| Applied Energistics 2 | 26.1.14-beta |
| GuideME | 26.1.17-beta |
| Java | 25 |

Install the compatible dependencies and place the addon JAR in the mods folder. The first public release is **Beta 1**. The approved JAR reports its internal version as `0.9.0-alpha.1`. Back up existing worlds before testing alpha builds. Worlds from 0.5.0-alpha.1 and earlier use a different namespace and are not compatible.

## Recipes

All recipes use existing AE2 ingredients; no new materials are required.

- Providers: `FCF / CPC / FCF` — fluix crystals (F), tier processor (C), previous provider (P).
- Assemblers: `CAC / FPF / AMA` — tier processor (C), acceleration cards (A), fluix crystals (F), previous assembler (P), original assembler (M).
- Coprocessors: `FCF / CPC / FMF` — fluix crystals (F), tier processor (C), previous coprocessor (P), original coprocessor (M).

Previous means the AE2 original for Advanced, Advanced for Elite, and Elite for Ultimate. Processors are Logic, Calculation and Engineering respectively. Crafted assemblers start without installed cards.

## Development and testing

Run `./gradlew build` (Linux/macOS) or `gradlew.bat build` (Windows) with JDK 25. The build runs headless Minecraft GameTests and checks packaged textures, layouts, models, recipes and license notices. Functional tests are not a substitute for measured performance benchmarks. Report issues with the mod version, dependency versions and relevant logs.

See the [Spectral palette](docs/paleta-espectral.png) for the approved visual identity.

## License and credits

Applied Tiers is licensed under **LGPL-3.0-or-later**. See [LICENSE](LICENSE) and [NOTICE](NOTICE.md). Developed by Willian Jesus da Silva. This is an independent addon, not an official AE2 release. Derived assets and adapted code retain the credits of AlgorithmX2 and Team Applied Energistics. Corresponding source is available in the repository history.
