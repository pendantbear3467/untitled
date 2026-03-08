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
    min_x = min((cube.from_pos.x for cube in document.cubes.values()), default=0.0)
    min_y = min((cube.from_pos.y for cube in document.cubes.values()), default=0.0)
    min_z = min((cube.from_pos.z for cube in document.cubes.values()), default=0.0)
    max_x = max((cube.to_pos.x for cube in document.cubes.values()), default=16.0)
    max_y = max((cube.to_pos.y for cube in document.cubes.values()), default=16.0)
    max_z = max((cube.to_pos.z for cube in document.cubes.values()), default=16.0)
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
                "metadata": dict(cube.metadata),
            }
        )
    return {
        "document": document.name,
        "namespace": document.namespace,
        "modelType": document.model_type,
        "textureSize": [document.texture_width, document.texture_height],
        "bounds": {
            "min": [min_x, min_y, min_z],
            "max": [max_x, max_y, max_z],
        },
        "bones": bones,
        "cubes": cubes,
    }
