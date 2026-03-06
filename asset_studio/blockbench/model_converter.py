from __future__ import annotations


def bbmodel_to_minecraft_json(bbmodel: dict, model_name: str) -> dict:
    textures = bbmodel.get("textures", [])
    texture_ref = "extremecraft:block/" + model_name
    if textures:
        texture_path = str(textures[0].get("path", "textures/block/" + model_name + ".png"))
        texture_ref = "extremecraft:block/" + texture_path.split("/")[-1].removesuffix(".png")

    elements = []
    for element in bbmodel.get("elements", []):
        elements.append(
            {
                "from": element.get("from", [0, 0, 0]),
                "to": element.get("to", [16, 16, 16]),
                "faces": element.get(
                    "faces",
                    {
                        "north": {"texture": "#all"},
                        "south": {"texture": "#all"},
                        "east": {"texture": "#all"},
                        "west": {"texture": "#all"},
                        "up": {"texture": "#all"},
                        "down": {"texture": "#all"},
                    },
                ),
            }
        )

    return {
        "textures": {"all": texture_ref},
        "elements": elements,
        "display": bbmodel.get("display", {}),
    }


def minecraft_json_to_bbmodel(model_json: dict, model_name: str) -> dict:
    elements = []
    for element in model_json.get("elements", []):
        elements.append(
            {
                "name": f"cube_{len(elements)}",
                "from": element.get("from", [0, 0, 0]),
                "to": element.get("to", [16, 16, 16]),
                "faces": element.get("faces", {}),
            }
        )

    texture_ref = model_json.get("textures", {}).get("all", f"extremecraft:block/{model_name}")
    texture_name = texture_ref.split("/")[-1]
    return {
        "meta": {"format_version": "4.9", "model_format": "java_block"},
        "name": model_name,
        "textures": [{"name": "texture", "path": f"textures/block/{texture_name}.png", "id": "0"}],
        "elements": elements,
    }
