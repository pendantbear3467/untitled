from __future__ import annotations


def cube_block_model(block_id: str) -> dict:
    return {
        "parent": "block/cube_all",
        "textures": {"all": f"extremecraft:block/{block_id}"},
    }


def item_model_template(item_id: str) -> dict:
    return {
        "parent": "item/generated",
        "textures": {"layer0": f"extremecraft:item/{item_id}"},
    }


def block_model_from_blockbench(model_name: str, bbmodel_data: dict) -> dict:
    display = bbmodel_data.get("display", {})
    return {
        "parent": "block/block",
        "textures": {"particle": f"extremecraft:block/{model_name}"},
        "display": display,
    }
