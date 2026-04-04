# Progression Decoupling Plan

**Objective**: Reduce direct coupling between progression module and host runtime packages to enable independent repository extraction.

**Current Status**: Bridge mode active; progression compiles via bridge-config classpath dependency.

**Target Status**: Progression can compile and publish as independent module (`com.extremecraft:extremecraft-progression:1.2.0`)

---

## Host Package Coupling Audit

### 1. **Ability System** (7 imports)
| Class | Usage | Current Approach | Decoupling Strategy |
|-------|-------|------------------|-------------------|
| `AbilityContext` | Ability unlock/casting context | Direct import | → Read-only bridge in `:core` (ProgressionAbilityContextBridge) |
| `AbilityDefinition` | Ability data types | Direct import | → Already defined in API; no bridge needed |
| `AbilityEffect` | Effect execution | Direct import | → Read-only bridge (ProgressionAbilityEffectBridge) |
| `AbilityExecutor` | Execute abilities | Direct import | → Read-only bridge (ProgressionAbilityExecutorBridge) |
| `AbilityTargetResolver` | Target resolution | Direct import | → Read-only bridge (ProgressionAbilityTargetResolverBridge) |

**Blocker**: Progression ability unlock logic directly imports host ability system. Must create read-only service bridge.

**Effort**: Medium | **Risk**: Low | **Priority**: 🟠 High

---

### 2. **Network & Packets** (6 imports)
| Class | Usage | Current Approach | Decoupling Strategy |
|-------|-------|------------------|-------------------|
| `ModNetwork` | Packet channel access | Direct import | → Wrap in read-only sync bridge (ProgressionNetworkSyncBridge) |
| `PlayerStatsPacket` | Sync player stats | Direct import | → Read-only bridge packet sync |
| `SyncClassAbilityStateS2CPacket` | Sync class ability | Direct import | → Read-only bridge |
| `SyncPlayerLevelS2CPacket` | Sync player level | Direct import | → Read-only bridge |
| `SyncPlayerProgressCapabilityPacket` | Sync progression | Direct import | → Already internal to progression; keep internal |
| `SyncProgressPacket` | Sync progress | Direct import | → Keep internal to progression |
| `ServerPacketLimiter` | Rate limiting | Direct import | → Read-only bridge for limiter access (ProgressionPacketLimiterBridge) |
| `RuntimeSyncService` | Sync coordination | Direct import | → Read-only bridge (ProgressionRuntimeSyncBridge) |

**Blocker**: Progression server-client sync logic imports host packet channels. Must create abstraction layer.

**Effort**: High | **Risk**: Medium | **Priority**: 🔴 Critical (blocks independent compilation)

**Plan**:
- Move progression S2C packet definitions to `:progression` entirely
- Host-owned packets (stats, etc.) can stay in host; progression reads via bridge
- Create `ProgressionPacketManager` interface in `:core` for progression packet registration

---

### 3. **Class System** (4 imports)
| Class | Usage | Current Approach | Decoupling Strategy |
|-------|-------|------------------|-------------------|
| `ClassAbilityBindings` | Class ability lookup | Direct import | → Already has read-only bridge (ProgressionQuestType done); extend bridge |
| `ClassAccessResolver` | Class access checking | Direct import | → Already resolves via canonical progression; minimize imports |
| `ClassPassives` | Passive ability application | Direct import | → Read-only bridge (ProgressionClassPassivesBridge) |
| `PlayerClass` | Class data types | Direct import | → Keep; needed for unlock type checking |

**Blocker**: Progression class unlock checks import host class definitions. Must create read-only bridge.

**Effort**: Low-Medium | **Risk**: Low | **Priority**: 🟠 High

**Plan**:
- Create `ProgressionClassLookupBridge` in `:core`
- Host registers `ClassRegistry` as provider
- Progression reads classes via bridge, not direct imports

---

### 4. **Magic System** (1 import)
| Class | Usage | Current Approach | Decoupling Strategy |
|-------|-------|------------------|-------------------|
| `ManaService` | Mana cost/drain on progression events | Direct import | → Read-only bridge (ProgressionManaBridge) |

**Blocker**: Progression spell/ability logic drains mana. Must create read-only service abstraction.

**Effort**: Low | **Risk**: Low | **Priority**: 🟡 Medium

---

### 5. **Machine System** (1 import)
| Class | Usage | Current Approach | Decoupling Strategy |
|-------|-------|------------------|-------------------|
| `MachineCatalog` | Machine unlock gating | Direct import | → Already has read-only bridge (ProgressionMachineIdBridge done); verify complete |

**Status**: ✅ Bridge already in place via `ProgressionMachineIdBridge` in `:core`

**Effort**: Done | **Risk**: None | **Priority**: ✅ Complete

---

### 6. **Materials System** (1 import)
| Class | Usage | Current Approach | Decoupling Strategy |
|-------|-------|------------------|-------------------|
| `ModMaterials` | Material lookup in progression | Direct import | → Read-only bridge (ProgressionMaterialBridge) |

**Blocker**: Progression references host material definitions.

**Effort**: Low | **Risk**: Low | **Priority**: 🟡 Medium

---

### 7. **Skills System** (3 imports)
| Class | Usage | Current Approach | Decoupling Strategy |
|-------|-------|------------------|-------------------|
| `PlayerSkillsCapability` | Skill level capability | Direct import | → Already has read-only bridge (ProgressionSkillLevelBridge done); verify complete |
| `SkillRegistry` | Skill lookup | Direct import | → Read-only bridge existing; keep |
| `SkillsApi` | Skills API access | Direct import | → Already routes to bridge; verify |

**Status**: ✅ Bridge already in place via `ProgressionSkillLevelBridge` in `:core`

**Effort**: Verify | **Risk**: None | **Priority**: ✅ Review

---

### 8. **Client GUI** (2 imports)
| Class | Usage | Current Approach | Decoupling Strategy |
|-------|-------|------------------|-------------------|
| `BaseExtremeScreen` | UI screen base | Direct import | → Client-only; move to client package or create interface |
| `ECGuiPrimitives` | GUI rendering | Direct import | → Client-only; move to client package or create interface |
| `ECGuiTheme` | GUI theme reference | Direct import | → Client-only; move to client package or create interface |

**Blocker**: Progression references host client-side GUI classes. Client code shouldn't be in progression.

**Effort**: Low | **Risk**: Low | **Priority**: 🟡 Medium

**Plan**: Move client progression GUI code to `:progression/src/main/java/com/extremecraft/progression/client/` and remove direct host imports. Or wrap in client-safe bridge.

---

## Phased Decoupling Roadmap

### **Phase 1: Critical Network Decoupling** (Priority: 🔴 Blocker)

**Goal**: Enable progression to publish independently by removing packet channel coupling.

**Tasks**:
1. [ ] Create `ProgressionPacketManager` interface in `:core`
2. [ ] Move progression S2C packet classes to `:progression/src/main/java` entirely
3. [ ] Host registers `ModNetwork.registerProgressionPackets()` via bridge callback
4. [ ] Update progression sync events to use `ProgressionPacketManager` instead of direct `ModNetwork`
5. [ ] Test: Progression compiles without host network imports
6. [ ] Test: Host runtime still syncs progression state correctly

**Effort**: 2-3 days | **Risk**: Medium (packet sync is critical)

**Verification**: Run boundary test with progression as external Gradle module

---

### **Phase 2: Ability & Magic Decoupling** (Priority: 🟠 High)

**Goal**: Remove progression ability unlock and mana coupling.

**Tasks**:
1. [ ] Create `ProgressionAbilityBridge` interface in `:core` (context, effects, execution)
2. [ ] Create `ProgressionManaBridge` interface in `:core` (mana cost/drain)
3. [ ] Host registers `AbilityRegistry` and `ManaService` as providers
4. [ ] Progression event handlers use bridges instead of direct imports
5. [ ] Test: Ability unlock gating still works
6. [ ] Test: Mana costs still applied to progression events

**Effort**: 1-2 days | **Risk**: Low

**Verification**: Integration tests; ability unlock gates and mana drains work end-to-end

---

### **Phase 3: Class & Material Decoupling** (Priority: 🟡 Medium)

**Goal**: Remove class and material system coupling.

**Tasks**:
1. [ ] Create `ProgressionClassLookupBridge` in `:core` (expand existing class bridge)
2. [ ] Create `ProgressionMaterialBridge` in `:core` (material lookup)
3. [ ] Host registers `ClassRegistry` and `ModMaterials` as providers
4. [ ] Progression uses bridges for class/material data access
5. [ ] Test: Class gating works; class passives apply correctly
6. [ ] Test: Material unlocks work as expected

**Effort**: 1 day | **Risk**: Low

**Verification**: Progression unlocks/gating feature tests

---

### **Phase 4: Client GUI Separation** (Priority: 🟡 Medium)

**Goal**: Remove client-side GUI coupling to keep progression as server-safe library.

**Tasks**:
1. [ ] Move progression client GUI code to `:progression/src/main/java/com/extremecraft/progression/client/`
2. [ ] Create client GUI bridge interface if needed
3. [ ] Host registers only server-side progression services in non-client contexts
4. [ ] Test: Client GUI doesn't load progression server code

**Effort**: 1 day | **Risk**: Low

**Verification**: Classpath inspection; client code not on server

---

### **Phase 5: Verification & Independent Compilation** (Priority: 🔴 Blocker)

**Goal**: Confirm progression can compile and publish as standalone artifact.

**Tasks**:
1. [ ] Enable `publishing` block in `progression/build.gradle`
2. [ ] Run `./gradlew :progression:publish`
3. [ ] Update host `build.gradle` to import `com.extremecraft:extremecraft-progression:1.2.0`
4. [ ] Remove `include 'progression'` from `settings.gradle`
5. [ ] Remove `:progression/src/main/java` from root `sourceSets`
6. [ ] Verify clean build: `./gradlew clean build`
7. [ ] Run all boundary tests with progression as external module
8. [ ] Verify gameplay: Run client/server dev sessions

**Effort**: 1 day | **Risk**: Medium (integration point)

**Verification**: Full end-to-end functionality test; all tests pass

---

## New Modules to Create (Preparation for Phase 1)

### `:gameplay` Module (Deferred, Optional)

Once progression is independent, consider extracting non-progression host systems:

```
:gameplay
├── src/main/java/com/extremecraft/quest/**
├── src/main/java/com/extremecraft/machine/**
├── src/main/java/com/extremecraft/ability/**
└── src/main/java/com/extremecraft/network/**
```

**Why**: Keep host-only systems separated from progression, clearer authority boundaries.

**Status**: Not needed for progression extraction; optional future refactor.

---

## Bridge Checklist (Pre-Flight)

Before Phase 1, verify all required bridges exist in `:core`:

- ✅ `ProgressionRuntimeFlags` — Debug bypass flag
- ✅ `ProgressionQuestType` — Quest type enum
- ✅ `ProgressionQuestDescriptor` — Quest definition projection
- ✅ `ProgressionQuestCatalogBridge` — Quest lookup
- ✅ `ProgressionSkillLevelBridge` — Skill level lookup
- ✅ `ProgressionMachineIdBridge` — Machine unlock checking
- ✅ `ProgressionResearchBridge` — Research unlock checking
- ⏳ `ProgressionPacketManager` — Packet sync (Phase 1)
- ⏳ `ProgressionAbilityBridge` — Ability context/execution (Phase 2)
- ⏳ `ProgressionManaBridge` — Mana service (Phase 2)
- ⏳ `ProgressionClassLookupBridge` — Class access (Phase 3)
- ⏳ `ProgressionMaterialBridge` — Material lookup (Phase 3)

---

## Timeline Estimate

| Phase | Duration | Blocker | Start After |
|-------|----------|---------|-------------|
| Phase 1 (Network) | 2-3 days | 🔴 Yes | Immediate |
| Phase 2 (Ability/Mana) | 1-2 days | No | Phase 1 |
| Phase 3 (Class/Material) | 1 day | No | Phase 2 |
| Phase 4 (GUI Separation) | 1 day | No | Phase 3 |
| Phase 5 (Verification) | 1 day | 🔴 Yes | Phase 4 |

**Total**: ~7-8 days of work (if no blockers)

---

## Risk Mitigation

1. **Test Before Each Phase**: Run boundary tests after each phase to catch integration issues early
2. **Commit Frequently**: Create commits after each decoupling task for easy rollback
3. **Parallel Testing**: Client/server dev sessions after each phase
4. **Keep Host Working**: Always ensure host builds and runs cleanly
5. **Document Bridges**: Each bridge in `:core` should have clear ownership comments

---

## Success Criteria

- ✅ Progression module compiles independently
- ✅ Progression publishes to Maven local (`./gradlew :progression:publish`)
- ✅ Host can import progression from Maven instead of local Gradle
- ✅ No host-owned package imports remain in progression
- ✅ Boundary tests pass with progression as external module
- ✅ Gameplay functionality fully intact (no regressions)
- ✅ Ready for GitHub repository extraction

---

## Next Step

Once this plan is approved, begin **Phase 1: Critical Network Decoupling** by:

1. Creating `ProgressionPacketManager` interface in `:core`
2. Auditing all packet registration in `progression/` source
3. Moving progression S2C packets to `:progression` package
4. Integrating with host via bridge callback

This first phase is the most critical blocker for independent compilation.
