from __future__ import annotations

from compiler.asset_builder import AssetBuilder as CoreAssetBuilder


class AssetBuilder(CoreAssetBuilder):
    """Compatibility wrapper around top-level asset builder."""
