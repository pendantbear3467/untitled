# Repository Templates Ready for GitHub Extraction

**Location**: `~/ExtremeCraft-Repo-Templates/`

Two standalone repository templates have been generated and are ready to become independent GitHub repositories.

---

## Repository 1: `extremecraft-api`

**Template Path**: `~/ExtremeCraft-Repo-Templates/extremecraft-api/`

**Status**: ✅ Ready for GitHub extraction now

**Contents**:
- `src/main/java/com/extremecraft/api/` — All API interfaces and contracts
- `build.gradle` — Maven publication configuration
- `settings.gradle` — Project root settings
- `.gitignore` — Standard exclusions
- `LICENSE` — MIT license
- `REPOSITORY_README.md` — Repository-specific README

**To Create GitHub Repo**:

1. Create new GitHub repository: `extremecraft-api`
2. Initialize locally from template:
   ```bash
   cd ~/ExtremeCraft-Repo-Templates/extremecraft-api
   git init
   git add .
   git commit -m "Initial commit: Extract API module from main ExtremeCraft repo"
   git branch -M main
   git remote add origin https://github.com/YOUR-ORG/extremecraft-api.git
   git push -u origin main
   ```
3. (Optional) Set up GitHub Actions for Maven publication to Maven Central

**Publishing**:
```bash
cd ~/ExtremeCraft-Repo-Templates/extremecraft-api
./gradlew publish  # publishes to ~/.m2/repository locally
```

Or configure `build.gradle` with Maven Central credentials for automated CI/CD publication.

---

## Repository 2: `extremecraft-core`

**Template Path**: `~/ExtremeCraft-Repo-Templates/extremecraft-core/`

**Status**: ✅ Ready for GitHub extraction now

**Contents**:
- `src/main/java/com/extremecraft/ecosystem/core/` — All core contracts, registries, and bridges
- `build.gradle` — Maven publication configuration (depends on `extremecraft-api:1.2.0`)
- `settings.gradle` — Project root settings
- `.gitignore` — Standard exclusions
- `LICENSE` — MIT license
- `REPOSITORY_README.md` — Repository-specific README

**To Create GitHub Repo**:

1. Create new GitHub repository: `extremecraft-core`
2. Initialize locally from template:
   ```bash
   cd ~/ExtremeCraft-Repo-Templates/extremecraft-core
   git init
   git add .
   git commit -m "Initial commit: Extract core module from main ExtremeCraft repo"
   git branch -M main
   git remote add origin https://github.com/YOUR-ORG/extremecraft-core.git
   git push -u origin main
   ```
3. (Optional) Set up GitHub Actions for Maven publication to Maven Central

**Publishing**:
```bash
cd ~/ExtremeCraft-Repo-Templates/extremecraft-core
./gradlew publish  # publishes to ~/.m2/repository locally
```

Dependencies:
- ✅ `extremecraft-api:1.2.0` (from Maven Central or local)
- ✅ `forge:1.20.1-47.4.0` (compileOnly, for type refs)

---

## Next Steps

### Immediate
1. ✅ Review template contents
2. ✅ Verify build succeeds in each template
3. Create GitHub organizations/repositories
4. Push templates to GitHub

### Short-term (After Repo Creation)
1. Set up GitHub Actions workflows
2. Configure Maven Central / Sonatype credentials
3. Automate artifact publication on release/tag
4. Update main ExtremeCraft repo to import from Maven Central instead of local Gradle includes

### Long-term
1. Iterate on published APIs based on addon feedback
2. Publish new versions to Maven Central
3. Continue progression decoupling
4. Extract progression module once independent

---

## Verification: Test Build in Template

```bash
cd ~/ExtremeCraft-Repo-Templates/extremecraft-api
./gradlew clean build
# Expected: BUILD SUCCESSFUL

cd ~/ExtremeCraft-Repo-Templates/extremecraft-core
./gradlew clean build
# Expected: BUILD SUCCESSFUL
```

---

## Files Included in Each Template

### extremecraft-api/
```
.
├── .gitignore
├── LICENSE
├── README.md (from source)
├── REPOSITORY_README.md (new)
├── build.gradle (standalone, no subprojects)
├── settings.gradle
└── src/
    └── main/java/com/extremecraft/api/
```

### extremecraft-core/
```
.
├── .gitignore
├── LICENSE
├── README.md (from source)
├── REPOSITORY_README.md (new)
├── build.gradle (standalone, depends on extremecraft-api)
├── settings.gradle
└── src/
    └── main/java/com/extremecraft/ecosystem/core/
```

---

## Customization Before GitHub Push

Edit templates as needed:

- **Update `build.gradle`** with Maven Central credentials (when ready to publish)
- **Update license** if different from MIT
- **Customize `REPOSITORY_README.md`** with real Discord/support links
- **Add CHANGELOG.md** with version history
- **Add CONTRIBUTING.md** if different from main repo

---

**Status**: ✅ Templates complete and ready for GitHub extraction
