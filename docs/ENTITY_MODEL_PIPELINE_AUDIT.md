# Entity Model Pipeline Audit

## Runtime Rule

Live in-game entity visuals are currently owned by Java classes:

- `src/main/java/com/extremecraft/client/render/entity/*Renderer.java`
- `src/main/java/com/extremecraft/client/model/entity/*Model.java`

The JSON files under `assets/extremecraft/entities` and `assets/extremecraft/models/entity` are metadata/handoff files only.

## Reference Entities

### Zombie-style humanoid

- Reference entity: `tech_construct`
- Current runtime owner:
  - Entity: `com.extremecraft.entity.mob.TechConstructEntity`
  - Renderer: `com.extremecraft.client.render.entity.TechConstructRenderer`
  - Model: `com.extremecraft.client.model.entity.TechConstructModel`
- Current geometry:
  - Java-baked `ECBipedEntityModel` placeholder silhouette
- Texture path:
  - `assets/extremecraft/textures/entity/tech_construct.png`
- Metadata path:
  - `assets/extremecraft/entities/tech_construct.json`
  - `assets/extremecraft/models/entity/tech_construct.json`
- Animation path:
  - Code-driven idle/walk/attack in `ECBipedEntityModel`
- Safe replacement workflow:
  1. Keep the entity id and texture path stable.
  2. Replace the Java model/layer path deliberately.
  3. Update the metadata JSON and this audit when the runtime owner changes.

### Golem-style heavy body

- Reference entity: `runic_golem`
- Current runtime owner:
  - Entity: `com.extremecraft.entity.mob.RunicGolemEntity`
  - Renderer: `com.extremecraft.client.render.entity.RunicGolemRenderer`
  - Model: `com.extremecraft.client.model.entity.RunicGolemModel`
- Current geometry:
  - Java-baked `ECBipedEntityModel` derivative with a bulkier stone-guardian silhouette
- Texture path:
  - `assets/extremecraft/textures/entity/runic_golem.png`
- Metadata path:
  - `assets/extremecraft/entities/runic_golem.json`
  - `assets/extremecraft/models/entity/runic_golem.json`
- Animation path:
  - Code-driven idle/walk/attack in `ECBipedEntityModel`
- Safe replacement workflow:
  1. Preserve the id and texture path.
  2. Swap the Java model owner when the heavier Blockbench silhouette is ready.
  3. Keep hitbox/entity registration in `ModEntities` unchanged unless gameplay actually needs it.

### Blaze-style floating segmented caster

- Reference entity: `arcane_wraith`
- Current runtime owner:
  - Entity: `com.extremecraft.entity.mob.ArcaneWraithEntity`
  - Renderer: `com.extremecraft.client.render.entity.ArcaneWraithRenderer`
  - Model: `com.extremecraft.client.model.entity.ArcaneWraithModel`
- Current geometry:
  - Java-baked `ECBipedEntityModel` derivative with a veiled floating-caster silhouette, still owned by the Java runtime path
- Texture path:
  - `assets/extremecraft/textures/entity/arcane_wraith.png`
- Metadata path:
  - `assets/extremecraft/entities/arcane_wraith.json`
  - `assets/extremecraft/models/entity/arcane_wraith.json`
- Animation path:
  - Code-driven idle/walk/attack in `ECBipedEntityModel`
- Safe replacement workflow:
  1. Keep the entity id stable.
  2. Replace the current Java runtime model with a segmented floating path in one deliberate migration if the silhouette needs to move beyond the existing baked model.
  3. Update metadata/handoff docs at the same time so the placeholder state is not hidden.

## Wider Audit Summary

- `tech_construct`, `arcane_wraith`, `void_stalker`, `ancient_sentinel`, `energy_parasite`, `runic_golem`, `ancient_core_guardian`, `void_titan`, and `overcharged_machine_god` all currently render through Java-authored cube-style models derived from `ECBipedEntityModel`.
- No runtime animation asset folder is currently consumed for entity animation.
- Replacing art alone is safe. Replacing silhouette or animation runtime requires a deliberate Java runtime owner migration.
