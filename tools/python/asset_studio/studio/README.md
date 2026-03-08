# Studio Namespace Compatibility

`asset_studio.gui` is the canonical desktop UI/runtime namespace.

`asset_studio.studio` remains only as a compatibility surface for older imports.
The wrapper modules in this folder intentionally re-export the canonical
implementation instead of owning separate logic.
