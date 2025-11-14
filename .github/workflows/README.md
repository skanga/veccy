# GitHub Actions Workflows

This directory contains CI/CD pipelines and automation workflows for Veccy.

---

## Workflows Overview

### 🔄 CI Pipeline (`ci.yml`)

**Triggers:** Push to `main`/`develop`, Pull Requests
**Purpose:** Continuous Integration - build, test, and quality checks

**Jobs:**
- **Build & Test** - Multi-OS (Ubuntu, Windows, macOS) and multi-Java (21, 23)
- **Code Quality** - CheckStyle, SpotBugs analysis
- **Integration Tests** - Full integration test suite with coverage
- **Examples** - Test example applications
- **Dependency Check** - Security vulnerability scanning

**Artifacts:**
- Test results (30 days)
- JAR files (30 days)
- Dependency check reports (30 days)

**Note:** Windows ONNX tests may fail due to native library issues - this is expected and won't fail the build.

---

### 🚀 Release Pipeline (`release.yml`)

**Triggers:** Tags matching `v*` pattern, Manual dispatch
**Purpose:** Automated release creation and artifact publishing

**Jobs:**
- **Build Release** - Create fat JAR with checksums
- **Docker Build** - Build and push Docker images
- **Maven Publish** - Publish to Maven Central

**Artifacts Created:**
- `veccy-{version}.jar` - Regular JAR
- `veccy-{version}-fat.jar` - Fat JAR with dependencies
- SHA-256 checksums
- GitHub Release with notes

**Required Secrets:**
- `DOCKER_USERNAME` - Docker Hub username
- `DOCKER_PASSWORD` - Docker Hub password/token
- `GPG_PRIVATE_KEY` - GPG key for signing
- `GPG_PASSPHRASE` - GPG passphrase
- `OSSRH_USERNAME` - Maven Central username
- `OSSRH_PASSWORD` - Maven Central password

**Creating a Release:**
```bash
# Tag and push
git tag -a v0.2.0 -m "Release version 0.2.0"
git push origin v0.2.0

# Or use GitHub UI to create release
```

---

### 🔒 Security Analysis (`codeql.yml`)

**Triggers:** Push to `main`/`develop`, PRs, Weekly schedule
**Purpose:** Security vulnerability detection with CodeQL

**Features:**
- Static code analysis
- Security vulnerability detection
- Weekly scheduled scans
- Results in Security tab

**Queries:** `security-and-quality`

---

### 📚 Documentation (`docs.yml`)

**Triggers:** Push to `main`, Changes to docs/source files
**Purpose:** Generate and deploy documentation

**Jobs:**
- **JavaDoc** - Generate API documentation
- **Link Check** - Validate markdown links
- **Markdown Lint** - Validate markdown formatting

**Deployment:**
- JavaDoc → GitHub Pages (`/javadoc`)

---

### ⚡ Performance Benchmarks (`performance.yml`)

**Triggers:** Push to `main`, PRs, Weekly schedule, Manual
**Purpose:** Performance regression testing

**Jobs:**
- **JMH Benchmarks** - Microbenchmarks for critical paths
- **Stress Tests** - High-load testing

**Features:**
- Automated performance tracking
- Alert on 50%+ regression
- 90-day result retention

---

### 📦 Dependency Updates (`dependency-update.yml`)

**Triggers:** Weekly schedule, Manual
**Purpose:** Automated dependency management

**Jobs:**
- **Update Dependencies** - Check and update to latest versions
- **Dependabot Auto-merge** - Auto-merge patch/minor updates

**Creates:** Pull requests for dependency updates

---

## Badges

Add these to your README.md:

```markdown
[![CI](https://github.com/skanga/veccy/actions/workflows/ci.yml/badge.svg)](https://github.com/skanga/veccy/actions/workflows/ci.yml)
[![Release](https://github.com/skanga/veccy/actions/workflows/release.yml/badge.svg)](https://github.com/skanga/veccy/actions/workflows/release.yml)
[![CodeQL](https://github.com/skanga/veccy/actions/workflows/codeql.yml/badge.svg)](https://github.com/skanga/veccy/actions/workflows/codeql.yml)
[![Performance](https://github.com/skanga/veccy/actions/workflows/performance.yml/badge.svg)](https://github.com/skanga/veccy/actions/workflows/performance.yml)
```

---

## Setting Up Secrets

### Required for Release Pipeline

1. **Docker Hub:**
   ```
   DOCKER_USERNAME=your-username
   DOCKER_PASSWORD=your-token
   ```

2. **GPG Signing:**
   ```bash
   # Generate key
   gpg --full-generate-key

   # Export private key
   gpg --armor --export-secret-keys YOUR_KEY_ID

   # Add to GitHub secrets:
   GPG_PRIVATE_KEY=<exported key>
   GPG_PASSPHRASE=<your passphrase>
   ```

3. **Maven Central:**
   ```
   OSSRH_USERNAME=your-username
   OSSRH_PASSWORD=your-password
   ```

### Optional

- `CODECOV_TOKEN` - For Codecov integration

---

## Workflow Files

| File | Purpose | Frequency |
|------|---------|-----------|
| `ci.yml` | Build & test | On push/PR |
| `release.yml` | Create releases | On tag |
| `codeql.yml` | Security scan | Weekly |
| `docs.yml` | Documentation | On docs change |
| `performance.yml` | Benchmarks | Weekly |
| `dependency-update.yml` | Dep updates | Weekly |

---

## Configuration Files

| File | Purpose |
|------|---------|
| `dependabot.yml` | Dependabot configuration |
| `markdown-link-check-config.json` | Link validation config |
| `markdown-lint-config.yml` | Markdown linting rules |

---

## Workflow Customization

### Disable Windows Tests

If ONNX issues persist, disable Windows in CI:

```yaml
# In ci.yml, change:
strategy:
  matrix:
    os: [ubuntu-latest, macos-latest]  # Remove windows-latest
```

### Change Release Schedule

```yaml
# In performance.yml, change cron:
schedule:
  - cron: '0 2 * * 1'  # Monday 2 AM
  # Format: minute hour day month weekday
```

### Add New Environments

```yaml
# In ci.yml, add Java versions:
strategy:
  matrix:
    java: [21, 23, 25]  # Add 25
```

---

## Manual Workflow Triggers

### Run Release Manually

1. Go to Actions → Release
2. Click "Run workflow"
3. Enter version (e.g., `v0.2.0`)
4. Run

### Run Benchmarks

1. Go to Actions → Performance Benchmarks
2. Click "Run workflow"
3. Select branch
4. Run

---

## Monitoring

### Check Status

- **Dashboard:** https://github.com/skanga/veccy/actions
- **CI Status:** Look for ✅ or ❌ on commits/PRs
- **Security:** Check Security tab for CodeQL alerts

### Notifications

GitHub will notify on:
- Failed workflows
- Security vulnerabilities
- Dependency updates

---

## Troubleshooting

### Windows Tests Failing

**Expected** - ONNX native library issues. Set `continue-on-error: true` for Windows matrix.

### Release Not Creating

Check:
1. Tag format: Must be `v*` (e.g., `v0.1.0`)
2. Secrets configured
3. GPG key valid

### Docker Push Failing

Check:
1. `DOCKER_USERNAME` and `DOCKER_PASSWORD` set
2. Docker Hub repo exists
3. Credentials valid

### Maven Central Publish Failing

Check:
1. GPG key configured
2. `OSSRH_*` credentials valid
3. `pom.xml` has release profile

---

## Best Practices

1. **Always test locally** before pushing
2. **Review Dependabot PRs** before merging
3. **Monitor performance benchmarks** for regressions
4. **Keep secrets up to date**
5. **Use semantic versioning** for releases

---

## Support

- **Issues:** https://github.com/skanga/veccy/issues
- **Discussions:** https://github.com/skanga/veccy/discussions
- **Security:** See SECURITY.md for reporting vulnerabilities

---

## License

Workflows are part of Veccy and follow the same license terms.
