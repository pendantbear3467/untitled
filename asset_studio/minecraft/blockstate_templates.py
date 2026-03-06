from __future__ import annotations


def single_variant_blockstate(block_id: str) -> dict:
    return {"variants": {"": {"model": f"extremecraft:block/{block_id}"}}}
