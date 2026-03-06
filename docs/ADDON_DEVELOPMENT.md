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

- `material.json`
- `machine.json`
- `weapon.json`
- `tool.json`
- `armor.json`
- `skill_tree.json`
- `quest.json`
- `worldgen.json`

Definitions can be JSON or Python (`get_definitions()` or `DEFINITIONS`).

## 3. Validate + generate

```bash
assetstudio sdk validate mythril_expansion
assetstudio sdk generate mythril_expansion
```

## 4. Compile module artifact

```bash
assetstudio compile expansion mythril_expansion
```

Output artifact:

- `workspace/build/modules/artifacts/extremecraft-mythril_expansion.jar`

## 5. Optional release workflow

```bash
assetstudio release build --name mythril-expansion-v1
assetstudio release publish --name mythril-expansion-v1
```

`release publish` defaults to dry-run unless `--live` is provided and required tokens are configured.
