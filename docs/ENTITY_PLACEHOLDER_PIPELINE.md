# Entity Placeholder Pipeline

This file documents the current placeholder entity replacement path using the repo's actual live renderer/model ownership.

## Current live path

- Renderer registration: `src/main/java/com/extremecraft/client/render/entity/ModEntityRenderers.java`
- Model classes: `src/main/java/com/extremecraft/client/model/entity/`
- Textures: `src/main/resources/assets/extremecraft/textures/entity/`
- Asset metadata only: `src/main/resources/assets/extremecraft/entities/`

All current ExtremeCraft placeholder mobs use the classic Forge `MobRenderer` path with cube-based `ECBipedEntityModel` derivatives. GeckoLib is not the active render path yet.

## Replacement map

| Entity id | Runtime renderer | Runtime model | Texture path | Asset metadata | Notes |
| --- | --- | --- | --- | --- | --- |
| `tech_construct` | `TechConstructRenderer` | `TechConstructModel` | `textures/entity/tech_construct.png` | `assets/extremecraft/entities/tech_construct.json` | Biped placeholder |
| `arcane_wraith` | `ArcaneWraithRenderer` | `ArcaneWraithModel` | `textures/entity/arcane_wraith.png` | `assets/extremecraft/entities/arcane_wraith.json` | Best current blaze-type replacement slot |
| `void_stalker` | `VoidStalkerRenderer` | `VoidStalkerModel` | `textures/entity/void_stalker.png` | `assets/extremecraft/entities/void_stalker.json` | Best current zombie-type replacement target |
| `ancient_sentinel` | `AncientSentinelRenderer` | `AncientSentinelModel` | `textures/entity/ancient_sentinel.png` | `assets/extremecraft/entities/ancient_sentinel.json` | Biped sentinel placeholder |
| `energy_parasite` | `EnergyParasiteRenderer` | `EnergyParasiteModel` | `textures/entity/energy_parasite.png` | `assets/extremecraft/entities/energy_parasite.json` | Small hostile placeholder |
| `runic_golem` | `RunicGolemRenderer` | `RunicGolemModel` | `textures/entity/runic_golem.png` | `assets/extremecraft/entities/runic_golem.json` | Best current golem-type replacement target |
| `ancient_core_guardian` | `AncientCoreGuardianRenderer` | `AncientCoreGuardianModel` | `textures/entity/ancient_core_guardian.png` | `assets/extremecraft/entities/ancient_core_guardian.json` | Boss-scale biped placeholder |
| `void_titan` | `VoidTitanRenderer` | `VoidTitanModel` | `textures/entity/void_titan.png` | `assets/extremecraft/entities/void_titan.json` | Boss-scale biped placeholder |
| `overcharged_machine_god` | `OverchargedMachineGodRenderer` | `OverchargedMachineGodModel` | `textures/entity/overcharged_machine_god.png` | `assets/extremecraft/entities/overcharged_machine_god.json` | Final boss placeholder |

## Recommended Blockbench examples

### Zombie-type example

- Use `void_stalker` as the clean replacement target.
- Update `VoidStalkerModel` and `VoidStalkerRenderer` together.
- Keep the texture id at `extremecraft:textures/entity/void_stalker.png` unless you intentionally rename the runtime asset.

### Golem-type example

- Use `runic_golem` as the clean replacement target.
- Increase model bulk in `RunicGolemModel` or replace it with a Blockbench-generated model class.
- Keep renderer shadow size and hitbox expectations aligned with the entity class.

### Blaze-type example

- Use `arcane_wraith` as the closest current replacement slot.
- The current live model is still Java-authored on the biped base, so a true blaze-style replacement still requires changing the model class and likely the renderer silhouette, not just swapping the PNG.
- If GeckoLib is introduced later, wire it through `ModEntityRenderers` instead of relying on `assets/extremecraft/entities/*.json`.

## Replacement workflow

1. Export or hand-author the replacement model.
2. Add or replace the Java model class under `src/main/java/com/extremecraft/client/model/entity/`.
3. Update the per-entity renderer to bake the correct layer and point to the correct texture path.
4. Register the new layer definition in `ModEntityRenderers.registerLayerDefinitions`.
5. Add or replace the PNG under `src/main/resources/assets/extremecraft/textures/entity/`.
6. Optionally update `assets/extremecraft/entities/*.json` lore or notes for art handoff, but do not treat that JSON as renderer ownership.

## Important constraints

- Do not import or rip third-party mod assets.
- The current runtime does not automatically consume Blockbench JSON exports by folder convention alone.
- If a folder looks descriptive but has no loader, document it as metadata rather than assuming it drives gameplay or rendering.
