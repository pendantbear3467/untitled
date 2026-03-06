# Tool Wizard CLI Example

Equivalent to the GUI Tool Wizard fields:

```bash
python assetstudio.py generate tool mythril_pickaxe \
  --material mythril \
  --durability 1800 \
  --attack-damage 7 \
  --mining-speed 9 \
  --tier 4 \
  --texture-style metallic
```

Generated outputs include:
- `workspace/assets/textures/item/mythril_pickaxe.png`
- `workspace/assets/models/item/mythril_pickaxe.json`
- `workspace/data/recipes/mythril_pickaxe.json`
- `workspace/data/tags/tools/pickaxes.json`
- `workspace/assets/lang/en_us.json`
