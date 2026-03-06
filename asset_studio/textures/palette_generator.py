from __future__ import annotations

import hashlib


def material_hex_color(material: str, style_hint: str = "#8a8a8a") -> str:
    digest = hashlib.sha256(material.encode("utf-8")).hexdigest()
    r = int(digest[0:2], 16)
    g = int(digest[2:4], 16)
    b = int(digest[4:6], 16)

    hint = style_hint.lstrip("#")
    if len(hint) != 6:
        return f"#{r:02x}{g:02x}{b:02x}"

    hr = int(hint[0:2], 16)
    hg = int(hint[2:4], 16)
    hb = int(hint[4:6], 16)

    mix_r = (r + hr) // 2
    mix_g = (g + hg) // 2
    mix_b = (b + hb) // 2
    return f"#{mix_r:02x}{mix_g:02x}{mix_b:02x}"
