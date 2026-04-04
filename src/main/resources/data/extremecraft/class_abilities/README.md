# class_abilities

Status: `LIVE_RUNTIME`

`progression/classsystem/data/ClassAbilityLoader` loads this folder and
`progression/classsystem/ability/ClassAbilityService` triggers the live class
ability path before compiling into the shared `AbilityExecutor` effect runtime.

Adding JSON here changes the live definition, but new ability ids still need a
runtime effect path in `ClassAbilityService` if they are not already supported.
