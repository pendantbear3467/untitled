# Plugin Marketplace Architecture

ExtremeCraft Asset Studio now treats plugins as versioned marketplace modules with explicit metadata.

## Required metadata contract

Plugins declare metadata through either:

- `PLUGIN_METADATA` in the plugin Python module, or
- sidecar `<plugin>.plugin.json`

Fields:

- `name`
- `version`
- `dependencies`
- `compatible_platform_version`
- `entrypoint`

## Load behavior

Plugin loading now performs:

1. Metadata extraction and compatibility filtering (`compatible_platform_version` vs platform `build.gradle` version)
2. Dependency-aware load order resolution (`dependencies`)
3. Safe registration into plugin extension points

Loaded metadata is written to:

- `workspace/plugin_marketplace/index.json`

This index becomes the source of truth for marketplace inventory and future publish/install flows.

## Extension points supported

- generators
- validators
- texture styles
- datapack rules
- asset repairs
- GUI editors
- graph nodes
