# GitHub Repository Creation & Maven Central Publication Workflow

**Status**: ✅ Ready to execute

This document outlines the complete end-to-end process for extracting `extremecraft-api` and `extremecraft-core` to independent GitHub repositories with automated Maven Central publication.

---

## Phase 1: GitHub Organization & Repository Setup

### 1.1 Create GitHub Organization (One-time)

**Option A: Create New Organization**
1. Go to https://github.com/organizations/new
2. Enter organization name: `ExtremeCraftMods` (or similar)
3. Set to free plan initially
4. Complete setup

**Option B: Use Existing Organization**
- Skip to Step 1.2 if organization already exists

### 1.2 Create Repositories

**Repository 1: extremecraft-api**

1. Navigate to organization dashboard
2. Click "New repository"
3. Fill in:
   - **Repository name**: `extremecraft-api`
   - **Description**: "Core API interfaces and contracts for ExtremeCraft ecosystem"
   - **Visibility**: Public
   - **Initialize this repository with**: None (we'll push existing templates)
4. Create repository
5. Note the HTTPS URL: `https://github.com/ExtremeCraftMods/extremecraft-api.git`

**Repository 2: extremecraft-core**

Repeat for:
   - **Repository name**: `extremecraft-core`
   - **Description**: "Core contracts, registries, and bridges for ExtremeCraft ecosystem"
   - **Visibility**: Public
   - **HTTPS URL**: `https://github.com/ExtremeCraftMods/extremecraft-core.git`

### 1.3 Configure Branch Protection

For **both repositories**:

1. Go to Settings → Branches
2. Add rule for `main`:
   - ✅ Require a pull request before merging
   - ✅ Require status checks to pass before merging (required: `build`)
   - ✅ Require branches to be up to date before merging
3. Save

---

## Phase 2: Push Repository Templates to GitHub

### 2.1 Initialize extremecraft-api

```bash
cd ~/ExtremeCraft-Repo-Templates/extremecraft-api
git init
git add .
git commit -m "Initial commit: Extract API module from main ExtremeCraft repo"
git branch -M main
git remote add origin https://github.com/ExtremeCraftMods/extremecraft-api.git
git push -u origin main
```

### 2.2 Initialize extremecraft-core

```bash
cd ~/ExtremeCraft-Repo-Templates/extremecraft-core
git init
git add .
git commit -m "Initial commit: Extract core module from main ExtremeCraft repo"
git branch -M main
git remote add origin https://github.com/ExtremeCraftMods/extremecraft-core.git
git push -u origin main
```

### 2.3 Verify Repositories

- ✅ Both repos visible on GitHub with template content
- ✅ `main` branch is default
- ✅ `.gitignore` and `build.gradle` are present
- ✅ README files show

---

## Phase 3: Set Up Maven Central Publication

### 3.1 Prepare Sonatype OSSRH Account

**One-time setup** (do once per organization):

1. Go to https://central.sonatype.com/publish
2. Create Sonatype account (requires free registration)
3. Create a new project:
   - **Project Name**: `ExtremeCraft`
   - **Group ID**: `com.extremecraft` (ensure you own this domain or have proof)
   - **Project URL**: `https://github.com/ExtremeCraftMods/extremecraft-api`
   - **SCM URL**: `https://github.com/ExtremeCraftMods/extremecraft-api.git`
4. Verify domain ownership (Sonatype will guide)
5. Once approved, note your **username** and **password** (keep secure)

### 3.2 Generate GPG Signing Key

**One-time setup** (local machine):

```bash
gpg --gen-key
# Follow prompts:
# - Real name: ExtremeCraft Bot (or your name)
# - Email: your-email@example.com
# - Passphrase: (strong, unique)
```

Export keys for GitHub Actions:

```bash
# List keys
gpg --list-keys

# Export public key
gpg --armor --export <KEY_ID> > ~/gpg-public.asc

# Export secret key (KEEP SECURE)
gpg --armor --export-secret-keys <KEY_ID> > ~/gpg-secret.asc
```

### 3.3 Configure GitHub Secrets

For **each repository** (extremecraft-api and extremecraft-core):

1. Go to Settings → Secrets and variables → Actions
2. Add new secrets:
   - **Name**: `SONATYPE_USERNAME` | **Value**: `<your-sonatype-username>`
   - **Name**: `SONATYPE_PASSWORD` | **Value**: `<your-sonatype-password>`
   - **Name**: `GPG_SECRET_KEYS` | **Value**: (contents of `~/gpg-secret.asc`)
   - **Name**: `GPG_OWNERTRUST` | **Value**: (export: `gpg --export-ownertrust`)
   - **Name**: `GPG_PASSPHRASE` | **Value**: `<your-gpg-passphrase>`

---

## Phase 4: Create CI/CD Workflows

### 4.1 Build & Test Workflow

Create `.github/workflows/build.yml` in **both repositories**:

```yaml
name: Build

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew clean build

      - name: Publish to local Maven
        run: ./gradlew publish
```

### 4.2 Release & Publish to Maven Central Workflow

Create `.github/workflows/publish.yml` in **both repositories**:

```yaml
name: Publish to Maven Central

on:
  release:
    types: [ created ]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Import GPG key
        run: |
          echo "${{ secrets.GPG_SECRET_KEYS }}" | gpg --import --no-tty --batch --yes
          echo "${{ secrets.GPG_OWNERTRUST }}" | gpg --import-ownertrust

      - name: Publish to Maven Central
        run: ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
        env:
          ORG_GRADLE_PROJECT_sonatypeUsername: ${{ secrets.SONATYPE_USERNAME }}
          ORG_GRADLE_PROJECT_sonatypePassword: ${{ secrets.SONATYPE_PASSWORD }}
          ORG_GRADLE_PROJECT_signingKeyId: ${{ secrets.GPG_KEY_ID }}
          ORG_GRADLE_PROJECT_signingKey: ${{ secrets.GPG_SECRET_KEYS }}
          ORG_GRADLE_PROJECT_signingPassword: ${{ secrets.GPG_PASSPHRASE }}
```

### 4.3 Update build.gradle for Maven Central Signing

Add to **both build.gradle** files (replace existing publishing block):

```gradle
plugins {
    // ... other plugins
    id 'signing'
    id 'io.github.gradle-nexus.publish-plugin' version '2.0.0'
}

signing {
    useInMemoryPgpKeys(
        providers.environmentVariable("ORG_GRADLE_PROJECT_signingKeyId").getOrNull(),
        providers.environmentVariable("ORG_GRADLE_PROJECT_signingKey").getOrNull(),
        providers.environmentVariable("ORG_GRADLE_PROJECT_signingPassword").getOrNull()
    )
    sign publishing.publications
}

publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
            groupId = 'com.extremecraft'
            artifactId = 'extremecraft-api'  // or 'extremecraft-core'
            version = rootProject.version

            pom {
                name = 'extremecraft-api'
                description = 'Core API interfaces and contracts for ExtremeCraft ecosystem'
                url = 'https://github.com/ExtremeCraftMods/extremecraft-api'

                licenses {
                    license {
                        name = 'MIT License'
                        url = 'https://opensource.org/licenses/MIT'
                    }
                }

                developers {
                    developer {
                        name = 'ExtremeCraft Team'
                        email = 'your-email@example.com'
                    }
                }

                scm {
                    connection = 'scm:git:https://github.com/ExtremeCraftMods/extremecraft-api.git'
                    developerConnection = 'scm:git:https://github.com/ExtremeCraftMods/extremecraft-api.git'
                    url = 'https://github.com/ExtremeCraftMods/extremecraft-api'
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(providers.environmentVariable("ORG_GRADLE_PROJECT_sonatypeUsername"))
            password.set(providers.environmentVariable("ORG_GRADLE_PROJECT_sonatypePassword"))
        }
    }
}
```

---

## Phase 5: Create Release & Publish

### 5.1 Create GitHub Release (Triggers Maven Central Publication)

For **extremecraft-api**:

1. Go to Releases → Create a new release
2. Fill in:
   - **Tag version**: `v1.2.0`
   - **Release title**: `API v1.2.0`
   - **Description**: Release notes
3. Click "Publish release"
4. Monitor Actions tab → Publish workflow should run automatically

### 5.2 Monitor Publishing

1. Go to Actions tab
2. Watch `Build` workflow complete successfully
3. Watch `Publish to Maven Central` workflow run
4. Check https://s01.oss.sonatype.org/#view-repositories;stagingRepositories for staging repo
5. Once workflow completes, artifact should be available in Maven Central within ~10 minutes

### 5.3 Verify Maven Central Availability

```bash
# Check Maven Central
mvn dependency:resolve -Dartifact=com.extremecraft:extremecraft-api:1.2.0

# Or wait and use in new project
# build.gradle:
# implementation 'com.extremecraft:extremecraft-api:1.2.0'
```

---

## Phase 6: Update Host Project

### 6.1 Transition Host from Local to Maven Central

Once `api/` and `core/` are published:

**Before:**
```gradle
dependencies {
    implementation project(':api')
    implementation project(':core')
}
```

**After:**
```gradle
dependencies {
    implementation 'com.extremecraft:extremecraft-api:1.2.0'
    implementation 'com.extremecraft:extremecraft-core:1.2.0'
}
```

### 6.2 Update settings.gradle

Remove local includes:

```gradle
// Remove or comment out:
// include 'api', 'core'

// Keep progression for now (bridge mode):
include 'progression'
```

### 6.3 Update rootProject.version (if applicable)

In root `build.gradle`:

```gradle
// Version centrally defined
version = '1.20.1'
```

### 6.4 Verify Host Build

```bash
./gradlew clean build -x test
```

---

## Phase 7: Progression Extraction Preparation

### 7.1 When Ready to Extract Progression

Once `api/` and `core/` are independent and stable:

1. **Begin Phase 1** of the Progression Decoupling Roadmap (Network Decoupling)
   - Create `ProgressionPacketManager` interface in `:core`
   - Move S2C packets to `:progression` internally
   - Test with host via bridge

2. **Complete all 5 phases** of decoupling

3. **Create extremecraft-progression repository** (similar to api/core process)

4. **Release progression** to Maven Central

---

## Complete Checklist

### Pre-Release (One-time Setup)

- [ ] Create GitHub organization (or use existing)
- [ ] Set up Sonatype OSSRH account & verify namespace `com.extremecraft`
- [ ] Generate GPG signing key
- [ ] Create GitHub secrets for both repos (Sonatype credentials, GPG keys)
- [ ] Add build.gradle publishing configurations to templates
- [ ] Create `.github/workflows/build.yml` in both repos
- [ ] Create `.github/workflows/publish.yml` in both repos

### Per-Release (Repeatable)

- [ ] Update version in `build.gradle` (e.g., `1.2.1`)
- [ ] Update CHANGELOG.md with new features/fixes
- [ ] Commit changes
- [ ] Create GitHub Release with tag matching version
- [ ] Verify `Build` workflow passes
- [ ] Verify `Publish` workflow completes
- [ ] Confirm artifact appears in Maven Central (10 min delay typical)
- [ ] Test consuming in new project or host update

### Host Migration (After First Release)

- [ ] Update `build.gradle` to use Maven coordinates
- [ ] Remove `:api` and `:core` from `settings.gradle`
- [ ] Run `./gradlew clean build` and verify success
- [ ] Commit host changes
- [ ] Push to origin

---

## Troubleshooting

### Build Fails on Release

- Check GitHub Actions logs in Repo → Actions → Publish workflow
- Common issues:
  - GPG key not imported correctly → Re-export secrets
  - Sonatype credentials wrong → Update GitHub secrets
  - Version already exists → Increment version in build.gradle

### Maven Central Shows Artifact Not Found

- Typical delay is 10-15 minutes after publish workflow completes
- Increase delay to 30+ minutes if searching immediately
- Verify in staging repo first: https://s01.oss.sonatype.org/#view-repositories;stagingRepositories

### Signing Fails

- Ensure GPG_PASSPHRASE is set correctly
- Try locally: `./gradlew publishToSonatype -Psigning.gnupg.passphrase=YOUR_PASS`
- Fetch public key: `gpg --keyserver keys.openpgp.org --recv YOUR_KEY_ID`

---

## Success Criteria

- ✅ GitHub organization created and visible
- ✅ Both repositories (`extremecraft-api`, `extremecraft-core`) created and pushed
- ✅ Branch protection rules enforced on `main`
- ✅ CI/CD workflows run successfully on push
- ✅ Release workflow publishes to Maven Central
- ✅ Artifacts discoverable on https://central.sonatype.com/artifact/com/extremecraft
- ✅ Host project successfully imports from Maven coordinates
- ✅ All gameplay functionality intact (no regressions)

---

## Next Steps

1. **Execute Phase 1 Setup**: GitHub org + repos
2. **Execute Phase 3-4**: Sonatype + CI/CD setup
3. **Execute Phase 5**: Create first release (v1.2.0)
4. **Verify Phase 6**: Host migration to Maven Central
5. **Begin Progression Decoupling**: If proceeding with independent progression repo

