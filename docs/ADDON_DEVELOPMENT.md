# Addon Development with ExtremeCraft SDK

## 1. Initialize an addon

```bash
assetstudio sdk init-addon mythril_expansion
```

This creates:

- `workspace/addons/mythril_expansion/addon.json`
- `workspace/addons/mythril_expansion/definitions/*.json`

## 2. Author definitions

Supported types:

- `material`
- `machine`
- `weapon`
- `tool`
- `armor`
- `item`
- `block`
- `recipe`
- `skill_tree`
- `quest`
- `worldgen`

Definitions can be JSON or Python (`get_definitions()` or `DEFINITIONS`).

## 3. Declare dependency graph + compatibility

`addon.json` supports graph dependencies with version constraints:

```json
{
  "name": "mythril_expansion",
  "namespace": "mythril",
  "version": "1.0.0",
  "compatible_platform_version": ">=1.2.0",
  "dependency_graph": {
    "materials": ["mythril"],
    "machines": ["baseline_crusher"],
    "addons": [{ "id": "extremecraft-core", "version": ">=1.0.0" }],
    "apis": [
      { "id": "extremecraft-api", "version": ">=1" },
      { "id": "extremecraft-protocol", "version": ">=1" }
    ]
  }
}
```

## 4. Validate + generate

```bash
assetstudio sdk validate mythril_expansion
assetstudio sdk generate mythril_expansion
```

## 5. Compile module artifact (registry code + datapack + docs)

```bash
assetstudio compile expansion mythril_expansion
```

Compile now generates:

- Forge registry classes: `GeneratedItems`, `GeneratedBlocks`, `GeneratedMachines`, `GeneratedRecipes`, `GeneratedWorldgen`, `GeneratedRegistries`
- Full datapack trees (`recipes`, `loot_tables`, `tags`, `advancements`, `worldgen`)
- Documentation (`build/modules/<addon>/docs/*.md`)
- `module_manifest.json` with dependency graph/load order and conflicts

Output artifact:

- `workspace/build/modules/artifacts/extremecraft-mythril_expansion.jar`

## 6. Addon package operations

```bash
assetstudio addon list
assetstudio addon build-all
assetstudio addon install path/to/addon_or_zip
assetstudio addon remove mythril_expansion
```

## 7. Optional release workflow

```bash
assetstudio release build --name mythril-expansion-v1
assetstudio release publish --name mythril-expansion-v1
```

`release publish` defaults to dry-run unless `--live` is provided and required tokens are configured.
