# class_abilities

Status: `LIVE_RUNTIME`

`progression/classsystem/data/ClassAbilityLoader` loads this folder and
`progression/classsystem/ability/ClassAbilityService` executes the live effects.

Adding JSON here changes the live definition, but new ability ids still need a
runtime effect path in `ClassAbilityService` if they are not already supported.
