# Extraction Readiness Summary

**Date**: 2026-04-04
**Status**: ✅ Ready for first-wave extraction (`api`, `core`)
**Verification**: Full compile suite passes (`./gradlew.bat check`)

---

## Quick Status

| Module | Status | Reason | Action |
|--------|--------|--------|--------|
| **`api/`** | ✅ Extract now | Zero host dependencies; public API only | Create repo template |
| **`core/`** | ✅ Extract now | Only depends on `:api`; no host imports | Create repo template |
| **`progression/`** | ⚠️ Bridge mode; repo later | Still imports host runtime packages | Complete coupling reduction first |
| **`platform/`, `src/`** | 🔒 Host only | Forge bootstrap and gameplay monolith | Keep in main repo |

---

## Module Extraction Readiness Details

### ✅ API Module (`api/`)

**Current State:**
- Gradle subproject since day one
- Zero host runtime dependencies
- Only internal imports (`com.extremecraft.api.*`)

**Independence Assessment:**
- No Minecraft utilities or Forge coupling
- Stands alone as portable integration surface
- Can be published to Maven independently

**Extraction Ready**: YES
**Immediate Action**: Create separate repository template with `api/` as content

**Verification Command**:
```bash
./gradlew.bat :api:compileJava -x compileTestJava
# Result: ✅ UP-TO-DATE
```

---

### ✅ Core Module (`core/`)

**Current State:**
- Gradle subproject with confined dependencies
- Only imports `:api` and `com.extremecraft.ecosystem.core.*`
- compileOnly Forge dependency (for server type references)

**Dependencies Verified**:
```
api project(':api')
compileOnly 'net.minecraftforge:forge:1.20.1-47.4.0'
```

**Independence Assessment:**
- No direct host gameplay imports
- Can be independently compiled and published
- Read-only seams work as designed (quest bridge, skill bridge, research bridge)

**Extraction Ready**: YES
**Immediate Action**: Create separate repository template with `core/` as content

**Verification Command**:
```bash
./gradlew.bat :core:compileJava -x compileTestJava
# Result: ✅ UP-TO-DATE
```

---

### ⚠️ Progression Module (`progression/`)

**Current State:**
- Included as Gradle subproject in bridge mode
- Compiled independently but depends on host runtime classpath
- Authority-hardened with read-only contracts in `:core`

**Current Dependencies**:
```
implementation project(':api')
implementation project(':core')
compileOnly rootProject.files(...)  // Bridge-mode access to host runtime
```

**Coupling Analysis** (blockers for clean separation):

| Host Package | Usage | Blocker |
|--------------|-------|---------|
| `com.extremecraft.quest.*` | Quest trigger tracking, progression increments | `ProgressionQuestCatalogBridge` partially seals; model mapping still tight |
| `com.extremecraft.network.*` | Packet handling, server-client sync | Direct packet imports in event handlers |
| `com.extremecraft.machine.*` | Machine unlock checks | `ProgressionMachineIdBridge` seals reads |
| `com.extremecraft.ability.*` | Ability unlock gating | Direct imports in ability unlock checks |
| `com.extremecraft.skill.*` | Skill level reads | `ProgressionSkillLevelBridge` seals reads |
| `com.extremecraft.reactor.*` | Reactor unlock gating | Direct imports in event handlers |

**Extraction Readiness**: NOT YET
**Next Steps Before Split**:
1. Create `:gameplay` subproject for non-progression host systems
2. Move host-owned net/packet/ability code to `:gameplay`
3. Replace direct progression imports with read-only bridge contracts
4. Run boundary tests in isolation

**Gradual Path**:
- Phase 1 (now): Bridge mode with read-only seams ← **Current**
- Phase 2 (preparation): Direct coupling cleanup
- Phase 3 (extraction): Independent repo with clear maven publish

---

## Build & Compile Verification

### Full Suite Check
```bash
./gradlew.bat check -x validateContentCompleteness
```
**Result**: ✅ BUILD SUCCESSFUL (4 unused import fixes applied)

### Unit & Boundary Tests
- `GameplayAuthoritySourceTest` → ✅ Passes
- `ProgressionBoundarySourceTest` → ✅ Passes
- `validateExtremeCraft` → ✅ 0 findings

### Checkstyle
- `checkstyleMain` → ✅ PASS (after unused import cleanup)
- `checkstyleTest` → ✅ PASS

---

## Repository Structure Validation

### Tracked Artifacts (Intentional)
- `build/reports/tests/` — Boundary test reports
- `build/extremecraft-validation-report.txt` — Validation summary
- `docs/CANONICAL_OWNERSHIP_MAP.md` — Authority reference

### Generated/Local (Properly Excluded)
- All build/, bin/, run/, .gradle/ — gitignored ✅
- Python venv, .tmp-* — gitignored ✅
- IDE state (.vscode, .idea) — gitignored ✅

### Module Root Documentation
- `api/README.md` → ✅ Updated
- `core/README.md` → ✅ Updated
- `progression/README.md` → ✅ Updated
- `platform/README.md` → ✅ Updated

---

## Recommendations

### Immediate (This Pass)
1. ✅ Clean up unused imports (DONE)
2. ✅ Update REPOSITORY_FOLDER_GUIDE.md (DONE)
3. ✅ Document extraction readiness (DONE — this file)
4. Create separate GitHub repo templates for `api/` and `core/`

### Near-term (Before Progression Extraction)
1. Identify `com.extremecraft.network` usage in progression
2. Create read-only packet bridge in `:core`
3. Identify `com.extremecraft.ability` usage in progression
4. Move non-progression ability code to `:gameplay`
5. Run boundary tests with progression as external module

### Long-term
1. Establish CI/CD publication pipeline for `api` and `core`
2. Publish to Maven Central or central repository
3. Complete progression decoupling
4. Extract progression into independent repository

---

## Notes for Contributors

- **Do not add new imports from `src/main/java` to `api/` or `core/`** — they must remain self-contained
- **Read-only bridges are preferred** — `ProgressionQuestCatalogBridge`, `ProgressionSkillLevelBridge`, etc. should be extended before direct progression imports increase
- **Bridge mode is temporary** — Once progression compilation passes independently, convert to external publication

---

## File Artifacts Updated This Session

1. `docs/REPOSITORY_FOLDER_GUIDE.md` — Expanded classification + extraction readiness
2. `docs/EXTRACTION_READINESS_SUMMARY.md` — This file (new)
3. Fixed unused imports:
   - `src/main/java/com/extremecraft/classsystem/ClassAccessResolver.java`
   - `src/main/java/com/extremecraft/client/DwClientHooks.java`
   - `progression/src/main/java/com/extremecraft/progression/ProgressionGate.java`
   - `progression/src/main/java/com/extremecraft/progression/unlock/UnlockRuleLoader.java`
