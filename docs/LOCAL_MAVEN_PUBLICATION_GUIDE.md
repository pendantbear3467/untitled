# Local Maven Publication Guide

**Status**: ✅ `api/` and `core/` modules are now publishable to local Maven repository

---

## Quick Start

### Publish to Local Maven Repository

```bash
./gradlew.bat :api:publish :core:publish
```

Or use the convenience script:

```bash
./publish-local-modules.bat
```

**Result**: Artifacts published to `~/.m2/repository/com/extremecraft/`
- `extremecraft-api-1.2.0.jar`
- `extremecraft-core-1.2.0.jar`

---

## Using Published Modules in the Host

Once published locally, update `build.gradle` to import as external dependencies:

```gradle
dependencies {
    // Instead of:
    // implementation project(':api')
    // implementation project(':core')

    // Use Maven coordinates:
    implementation 'com.extremecraft:extremecraft-api:1.2.0'
    implementation 'com.extremecraft:extremecraft-core:1.2.0'
}
```

Then remove from `settings.gradle`:

```gradle
// Remove these lines:
// include 'api', 'core'
```

Keep only:

```gradle
include 'progression'
```

---

## Benefits of This Approach

1. **Decouples modules** from host build structure
2. **Simulates external dependencies** before full repo extraction
3. **Allows independent versioning** (future: publish to Maven Central)
4. **Enables addon projects** to use same coordinates

---

## Publishing Coordinates

| Artifact | GroupId | ArtifactId | Version |
|----------|---------|-----------|---------|
| API | `com.extremecraft` | `extremecraft-api` | `1.2.0` |
| Core | `com.extremecraft` | `extremecraft-core` | `1.2.0` |

---

## Next Steps

### Stage 1 (Current)
- ✅ Modules are publishable locally
- ⏳ You can optionally update host `build.gradle` to import via Maven

### Stage 2 (After GitHub Repo Creation)
- Create `extremecraft-api` repository on GitHub
- Create `extremecraft-core` repository on GitHub
- Configure GitHub Actions to publish to Maven Central
- Update host to pull from Maven Central instead of local

### Stage 3 (Progression Decoupling)
- Reduce progression host coupling
- Enable `progression/build.gradle` maven-publish block
- Publish progression to Maven
- Extract progression to independent repository

---

## Manual Verification

Check published artifacts:

```bash
ls -la ~/.m2/repository/com/extremecraft/extremecraft-*/1.2.0/
```

Expected output:
```
extremecraft-api-1.2.0.jar
extremecraft-api-1.2.0.pom
extremecraft-api-1.2.0.module
extremecraft-core-1.2.0.jar
extremecraft-core-1.2.0.pom
extremecraft-core-1.2.0.module
```

---

## Troubleshooting

### Artifacts not appearing in ~/.m2/repository

Run rebuild with clean:

```bash
./gradlew.bat clean :api:publish :core:publish
```

### Gradle complains about duplicate modules

If you're importing both as `project(':api')` and `'com.extremecraft:extremecraft-api:1.2.0'` simultaneously, remove the project includes from `settings.gradle` and `build.gradle`.

---

## Future: Publishing to Maven Central

Once repositories are created on GitHub, use [Sonatype deployment](https://central.sonatype.org/publish/publish-guide/) or GitHub Actions + Maven Central plugin to automate publication.

Example workflow step:

```yaml
- name: Publish to Maven Central
  run: ./gradlew :api:publish :core:publish -PremoteRepository=mavenCentral
```
