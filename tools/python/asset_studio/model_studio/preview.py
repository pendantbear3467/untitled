from __future__ import annotations

from asset_studio.model_studio.models import ModelDocument


def build_preview_payload(document: ModelDocument) -> dict:
    bones = []
    for bone in sorted(document.bones.values(), key=lambda item: item.id):
        bones.append(
            {
                "id": bone.id,
                "pivot": [bone.pivot.x, bone.pivot.y, bone.pivot.z],
                "rotation": [bone.rotation.x, bone.rotation.y, bone.rotation.z],
                "parent": bone.parent,
                "cubes": list(bone.cubes),
                "children": list(bone.children),
            }
        )
    cubes = []
    for cube in sorted(document.cubes.values(), key=lambda item: item.id):
        cubes.append(
            {
                "id": cube.id,
                "from": [cube.from_pos.x, cube.from_pos.y, cube.from_pos.z],
                "to": [cube.to_pos.x, cube.to_pos.y, cube.to_pos.z],
                "pivot": [cube.pivot.x, cube.pivot.y, cube.pivot.z],
                "rotation": [cube.rotation.x, cube.rotation.y, cube.rotation.z],
                "texture": cube.texture,
                "mirror": cube.mirror,
            }
        )
    return {
        "document": document.name,
        "modelType": document.model_type,
        "textureSize": [document.texture_width, document.texture_height],
        "bones": bones,
        "cubes": cubes,
    }
