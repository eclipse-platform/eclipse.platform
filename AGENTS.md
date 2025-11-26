# Eclipse Platform Repository - AI Agent Instructions

This file provides guidance for AI coding assistants (GitHub Copilot, Claude Code, etc.) working with this repository.

## Repository Overview

This repository contains the core Eclipse Platform components that form the basis for the Eclipse IDE. It is a **large-scale Java/OSGi project** (~120MB, 5,600+ Java files across 38 Maven modules) using **Maven/Tycho** for building Eclipse RCP bundles and features.

**Key Technologies:**
- Language: Java (JDK 21)
- Build: Maven 3.9.11 with Eclipse Tycho (OSGi/RCP build tooling)
- Architecture: OSGi bundles organized as Eclipse plugins
- Testing: JUnit with Tycho Surefire plugin

**Main Modules:**
- `runtime/` - Core runtime, jobs, expressions, content types (org.eclipse.core.runtime, org.eclipse.core.jobs)
- `resources/` - Workspace, filesystem, project management (org.eclipse.core.resources, org.eclipse.core.filesystem)
- `debug/` - Debug framework and UI, external tools, launch configurations
- `team/` - Version control framework (CVS examples)
- `ua/` - User assistance: help system, cheatsheets, tips
- `ant/` - Ant integration and UI
- `terminal/` - Terminal view
- `platform/` - SDK packaging

## Critical Build Information

The `-Pbuild-individual-bundles` profile (configured in `.mvn/maven.config`) enables the bundle to fetch the parent POM from https://repo.eclipse.org/content/repositories/eclipse/.

**Note:** If network access to Eclipse repositories is blocked, individual bundle builds will fail. In such environments, code exploration and analysis can still be performed, but build verification is not possible.

### Build Profiles Used in CI

The Jenkinsfile shows the complete build command:
```bash
mvn clean verify --batch-mode --fail-at-end \
  -Pbree-libs -Papi-check -Pjavadoc \
  -Dmaven.test.failure.ignore=true \
  -Dcompare-version-with-baselines.skip=false \
  -Dmaven.compiler.failOnWarning=false
```

Key profiles:
- `-Pbree-libs` - Bundle Runtime Execution Environment libraries
- `-Papi-check` - API baseline comparison (detects breaking changes)
- `-Pjavadoc` - Generate Javadoc

## Testing

**Test Organization:**
- Tests are in `<module>/tests/` subdirectories (e.g., `runtime/tests/`, `resources/tests/`)
- Test bundles follow naming: `org.eclipse.<area>.tests.<component>`
- Tests use JUnit 4/5 with Tycho Surefire

**Running Tests:**
```bash
# Run tests for a specific bundle
cd <test-bundle-directory>
mvn clean verify -Pbuild-individual-bundles

# Tests are automatically run during 'mvn verify'
# Test results: target/surefire-reports/TEST-*.xml
```

**Important Test Notes:**
- Some tests require graphical display (use Xvnc in CI - see Jenkinsfile)
- Tests in `debug/org.eclipse.debug.tests/src/org/eclipse/debug/tests/LocalSuite.java` require user terminal and should NOT run on build machines
- Test failures are allowed in CI (`-Dmaven.test.failure.ignore=true`)

## Validation & CI Checks

### GitHub Actions Workflows

All workflows delegate to the aggregator repository:

1. **PR Checks** (`.github/workflows/pr-checks.yml`):
   - Freeze period verification
   - No merge commits check
   - Version increment verification (uses PDE API Tools)

2. **Continuous Integration** (`.github/workflows/ci.yml`):
   - Delegates to `mavenBuild.yml` in aggregator
   - Runs full build with all profiles

3. **CodeQL** (`.github/workflows/codeql.yml`):
   - Security scanning for Java code

### Local Validation Steps

Before committing, verify your changes:

```bash
# 1. Build the affected bundle
cd <bundle-directory>
mvn clean verify -Pbuild-individual-bundles

# 2. Check for API issues (PDE API Tools)
# API baseline checks run automatically with -Papi-check
# Results in: target/apianalysis/*.xml

# 3. Check for compiler warnings
# Results in: target/compilelogs/*.xml
```

### API Tools & Version Management

**Critical:** Eclipse uses semantic versioning with API tooling enforcement:
- Major version: Breaking API changes
- Minor version: Binary compatible API additions, significant changes
- Service version: Bug fixes (increments: +1 for maintenance, +100 for next release)
- Qualifier: Build timestamp

**Version Change Rules:**
1. API breaking change → Increment major version, reset minor/service to 0
2. API addition (binary compatible) → Increment minor version, reset service to 0
3. Bug fix in maintenance → Increment service by 1
4. Bug fix in next release → Increment service by 100

**PDE API Tools automatically detects API changes and enforces version increments.**

See `docs/VersionNumbering.md` and `docs/Evolving-Java-based-APIs.md` for complete details.

## Project Structure

### Root Files
- `pom.xml` - Main reactor POM (defines modules)
- `Jenkinsfile` - Jenkins CI pipeline configuration
- `.mvn/maven.config` - Default Maven options (includes `-Pbuild-individual-bundles`)
- `.gitignore` - Excludes `target/`, `bin/`, `*.class`, etc.

### Key Configuration Files

**Per Bundle:**
- `pom.xml` - Maven coordinates and build config
- `META-INF/MANIFEST.MF` - OSGi bundle manifest (Bundle-SymbolicName, Bundle-Version, dependencies)
- `build.properties` - Tycho/PDE build configuration (source folders, bin.includes)
- `.project` - Eclipse project descriptor
- `.classpath` - Eclipse classpath (typically generated)

**Coding Standards:**
- `docs/Coding_Conventions.md` - Java coding style (follows Oracle conventions with modifications)
- `docs/Naming_Conventions.md` - Package/class naming rules
- Indent with tabs (4 spaces wide)
- Encoding: UTF-8 (see `.settings/org.eclipse.core.resources.prefs`)

## Common Pitfalls & Solutions

### 1. Parent POM Resolution Failure
**Error:** `Non-resolvable parent POM for org.eclipse.platform:eclipse.platform`

**Solution:** Always use `-Pbuild-individual-bundles` profile when building individual bundles. This profile is pre-configured in `.mvn/maven.config` but may be needed explicitly in some contexts.

### 2. Missing Dependencies During Build
**Error:** Cannot resolve bundle dependencies

**Solution:** 
- Individual bundles fetch dependencies from Eclipse repositories
- Ensure https://repo.eclipse.org is accessible
- Clean local Maven cache if corrupted: `rm -rf ~/.m2/repository/org/eclipse`

### 3. Test Failures Requiring Display
**Error:** Tests fail with "No display available"

**Solution:** 
- Tests requiring GUI run automatically on CI (Xvnc configured in Jenkinsfile)
- For local testing, use Xvfb: `xvfb-run mvn verify`
- Or skip tests: `mvn verify -DskipTests`

### 4. API Tools Errors
**Error:** "API baseline errors found"

**Solution:**
- Review changes in `target/apianalysis/*.xml`
- If API changed, update bundle version in `META-INF/MANIFEST.MF`
- Follow version increment rules (see docs/VersionNumbering.md)
- For intentional API breaks, update baseline comparison

### 5. Build Timeouts
Maven operations can take considerable time:
- Clean build of single bundle: 1-3 minutes
- Full platform build (aggregator): 30-60 minutes
- Test execution: Variable, some test suites take 10+ minutes

**Set adequate timeouts when building (default 120s may not be enough):**
```bash
mvn verify -Pbuild-individual-bundles  # May need 180-300 seconds
```

## Making Changes

### Typical Change Workflow

1. **Locate the Bundle:**
   - Runtime/core services → `runtime/bundles/`
   - Resource/workspace → `resources/bundles/`
   - Debug/launch → `debug/`
   - Help/documentation → `ua/`

2. **Make Code Changes:**
   - Edit Java sources in bundle's `src/` directory
   - Follow coding conventions (see `docs/Coding_Conventions.md`)
   - Add/update Javadoc for public APIs

3. **Update MANIFEST.MF if needed:**
   - Changed API? Update `Bundle-Version` following semantic versioning
   - New dependencies? Add to `Require-Bundle` or `Import-Package`

4. **Build and Test:**
   ```bash
   cd <bundle-directory>
   mvn clean verify -Pbuild-individual-bundles
   ```

5. **Verify No API Issues:**
   - Check `target/apianalysis/*.xml` for API baseline errors
   - Address any version increment requirements

6. **Commit:**
   - Write clear commit message
   - Reference issue number if applicable

## File Locations Reference

**Documentation:** All in `docs/`
- `docs/Coding_Conventions.md` - Code style
- `docs/API_Central.md` - API guidelines hub
- `docs/VersionNumbering.md` - Version management
- `docs/FAQ/` - 200+ FAQ markdown files

**Build Configuration:**
- `.mvn/maven.config` - Maven CLI defaults
- `Jenkinsfile` - CI build definition (60 min timeout)
- `.github/workflows/*.yml` - GitHub Actions (all delegate to aggregator)

**Key Bundle Directories:**
- `runtime/bundles/org.eclipse.core.runtime` - Core Platform Runtime
- `runtime/bundles/org.eclipse.core.jobs` - Jobs and scheduling
- `resources/bundles/org.eclipse.core.resources` - Workspace API
- `resources/bundles/org.eclipse.core.filesystem` - Filesystem abstraction

## Working Efficiently

**Trust these instructions first.** This repository has a complex build setup that cannot be fully explored from the repository alone. The information above captures the essential knowledge needed to:
- Understand build requirements and limitations
- Make targeted changes without breaking CI
- Navigate the codebase effectively
- Avoid common build pitfalls

Only search beyond these instructions if:
- Specific API behavior needs clarification (check `docs/FAQ/`)
- Detailed versioning rules are needed (check `docs/VersionNumbering.md`)
- You need examples of existing code patterns (search Java sources)
- CI is failing with an error not covered here (check Jenkinsfile and workflow YAMLs)

**When in doubt:** Build at the bundle level with `-Pbuild-individual-bundles` profile and verify tests pass locally before pushing changes.

## AI Agent-Specific Notes

### For GitHub Copilot
- This file is automatically read by GitHub Copilot when providing code suggestions
- Copilot uses this context to understand the project structure and conventions
- Copilot excels at inline code completion and small-scale refactoring

### For Claude Code
- Claude Code has access to this file via the `CLAUDE.md` file in the repository root
- Claude Code is better suited for multi-file refactoring and architectural changes
- Use Claude Code for tasks requiring deep codebase understanding across multiple modules
- Claude Code can execute builds and tests directly via Maven commands

### For Other AI Agents
- Read this file to understand the repository structure and build requirements
- Follow the coding conventions in `docs/Coding_Conventions.md`
- Always test changes with `mvn clean verify -Pbuild-individual-bundles` before committing
- Check API baseline with `-Papi-check` when modifying public APIs
