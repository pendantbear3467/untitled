# ExtremeCraft API Guide

Package: `com.extremecraft.api`

## Registration surface

- `ExtremeCraftAPI.registerMachine(...)`
- `ExtremeCraftAPI.registerMaterial(...)`
- `ExtremeCraftAPI.registerSkillTree(...)`
- `ExtremeCraftAPI.registerQuest(...)`
- `ExtremeCraftAPI.registerModule(...)`
- `ExtremeCraftAPI.registerAbility(...)`
- `ExtremeCraftAPI.registerTechTree(...)`

## Module extension contract

Implement `com.extremecraft.api.module.ExtremeCraftModule` and provide a `META-INF/services` entry.

Required metadata methods:

- `moduleId()`
- `moduleName()`
- `apiVersion()`
- `protocolVersion()`
- `register(ExtremeCraftApiProvider api)`

Loaded modules are visible through `/ec modules`.

## Compatibility

Current constants:

- `EXTREMECRAFT_API_VERSION = 1`
- `EXTREMECRAFT_PROTOCOL_VERSION = 1`

Incompatible modules are skipped during startup.
