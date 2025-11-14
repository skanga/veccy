# CI/CD Pipeline Setup Guide

Complete setup instructions for Veccy's GitHub Actions workflows.

---

## ✅ What's Included

Your CI/CD pipeline now includes:

### 1. **Continuous Integration** (`ci.yml`)
- ✅ Multi-OS testing (Ubuntu, Windows, macOS)
- ✅ Multi-Java testing (21, 23)
- ✅ Code quality checks (CheckStyle, SpotBugs)
- ✅ Integration tests
- ✅ Example validation
- ✅ Security scanning

### 2. **Release Automation** (`release.yml`)
- ✅ Automated JAR builds
- ✅ GitHub Releases
- ✅ Docker image publishing
- ✅ Maven Central publishing
- ✅ Checksum generation

### 3. **Security** (`codeql.yml`)
- ✅ CodeQL analysis
- ✅ Weekly security scans
- ✅ Vulnerability alerts

### 4. **Documentation** (`docs.yml`)
- ✅ JavaDoc generation
- ✅ Link validation
- ✅ Markdown linting
- ✅ GitHub Pages deployment

### 5. **Performance** (`performance.yml`)
- ✅ JMH benchmarks
- ✅ Stress testing
- ✅ Performance tracking
- ✅ Regression alerts

### 6. **Dependencies** (`dependency-update.yml`)
- ✅ Automated dependency updates
- ✅ Dependabot integration
- ✅ Auto-merge for safe updates

---

## 🚀 Quick Start

### Step 1: Enable GitHub Actions

1. Go to your GitHub repository
2. Click **Settings** → **Actions** → **General**
3. Under "Actions permissions", select **Allow all actions and reusable workflows**
4. Click **Save**

### Step 2: Enable GitHub Pages (for JavaDoc)

1. Go to **Settings** → **Pages**
2. Under "Source", select **Deploy from a branch**
3. Select branch: `gh-pages`, folder: `/ (root)`
4. Click **Save**

### Step 3: Push the Workflows

```bash
git add .github/
git commit -m "ci: add GitHub Actions workflows"
git push origin main
```

The CI pipeline will run automatically! 🎉

---

## 🔧 Configuration (Optional)

### For Full Release Pipeline

If you want automated releases to Docker Hub and Maven Central, configure these secrets:

#### Docker Hub Secrets

1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Add:
   - Name: `DOCKER_USERNAME`, Value: your Docker Hub username
   - Name: `DOCKER_PASSWORD`, Value: your Docker Hub access token

#### Maven Central Secrets (Advanced)

1. **Generate GPG Key:**
   ```bash
   gpg --full-generate-key
   # Follow prompts, use your email

   # List keys
   gpg --list-secret-keys --keyid-format=long

   # Export (replace KEY_ID)
   gpg --armor --export-secret-keys KEY_ID > private-key.asc
   ```

2. **Add to GitHub:**
   - `GPG_PRIVATE_KEY`: Contents of `private-key.asc`
   - `GPG_PASSPHRASE`: Your GPG passphrase
   - `OSSRH_USERNAME`: Sonatype username
   - `OSSRH_PASSWORD`: Sonatype password

**Note:** Only needed if publishing to Maven Central. Skip for now if not publishing.

---

## 📊 Monitoring

### View Workflow Status

- **All Workflows:** https://github.com/skanga/veccy/actions
- **Latest Run:** Click on any workflow in the Actions tab
- **Commit Status:** Look for ✅/❌ next to commits

### Add Status Badges

Add to your `README.md`:

```markdown
[![CI](https://github.com/skanga/veccy/actions/workflows/ci.yml/badge.svg)](https://github.com/skanga/veccy/actions/workflows/ci.yml)
[![CodeQL](https://github.com/skanga/veccy/actions/workflows/codeql.yml/badge.svg)](https://github.com/skanga/veccy/actions/workflows/codeql.yml)
```

---

## 🎯 Usage Examples

### Creating a Release

```bash
# Tag your code
git tag -a v0.2.0 -m "Release version 0.2.0"
git push origin v0.2.0

# GitHub Actions will automatically:
# 1. Build JARs
# 2. Create GitHub Release
# 3. Publish to Docker Hub (if configured)
# 4. Publish to Maven Central (if configured)
```

### Running Workflows Manually

1. Go to **Actions** tab
2. Select a workflow (e.g., "Performance Benchmarks")
3. Click **Run workflow**
4. Select branch and click **Run workflow**

### Viewing Test Results

1. Go to **Actions** → Select a workflow run
2. Scroll to **Artifacts** section
3. Download `test-results-*` to view locally

---

## ⚙️ Workflow Triggers

| Workflow | Automatic Triggers | Manual |
|----------|-------------------|--------|
| CI | Push to main/develop, PRs | ✅ |
| Release | Tags matching `v*` | ✅ |
| CodeQL | Push, PRs, Weekly (Sunday) | ❌ |
| Docs | Push to main (docs changes) | ✅ |
| Performance | Push to main, PRs, Weekly (Monday) | ✅ |
| Dependencies | Weekly (Monday) | ✅ |

---

## 🐛 Known Issues & Workarounds

### Windows ONNX Tests May Fail

**Expected behavior** - Native library issues on Windows.

**Solution:** Tests are marked with `continue-on-error: true` so they won't fail the build.

### First JavaDoc Deploy May Fail

**Cause:** `gh-pages` branch doesn't exist yet.

**Solution:** After first run:
1. Go to **Settings** → **Pages**
2. Verify `gh-pages` branch was created
3. Select it as the source
4. Next run will succeed

### Dependabot PRs Pile Up

**Cause:** Many dependencies to update.

**Solution:**
- Review and merge important ones first
- Close or auto-merge minor updates
- Adjust `open-pull-requests-limit` in `dependabot.yml`

---

## 🔒 Security Best Practices

### Secrets Management

- ✅ **Never commit secrets** to the repository
- ✅ **Use GitHub Secrets** for sensitive data
- ✅ **Rotate credentials** regularly
- ✅ **Limit secret access** to necessary workflows only

### Dependency Security

- ✅ **Dependabot enabled** - Auto-updates dependencies
- ✅ **CodeQL scanning** - Detects vulnerabilities
- ✅ **Dependency check** - OWASP vulnerability scanning

### Code Security

- ✅ **Required reviews** - Set up branch protection
- ✅ **Status checks** - Require CI to pass before merge
- ✅ **Signed commits** - Recommended for releases

---

## 📈 Performance Monitoring

### Benchmark Results

- Stored for 90 days
- Automatic regression detection
- Alert on 50%+ degradation

### Viewing Benchmarks

1. Go to **Actions** → **Performance Benchmarks**
2. Download `benchmark-results` artifact
3. View `jmh-result.json` or `.txt`

---

## 🎨 Customization

### Change Test Matrix

Edit `.github/workflows/ci.yml`:

```yaml
strategy:
  matrix:
    os: [ubuntu-latest, windows-latest, macos-latest]
    java: [21, 23]  # Add or remove versions
```

### Change Schedule

Edit cron expressions:

```yaml
schedule:
  - cron: '0 0 * * 0'  # Sunday midnight
  # Format: minute hour day month weekday
  # * = any, 0 = first, 1-6 = Mon-Sat, 0/7 = Sun
```

### Disable Workflows

Rename file extension:
```bash
mv .github/workflows/performance.yml .github/workflows/performance.yml.disabled
```

---

## 📚 Next Steps

### Recommended

1. **Add badges** to README.md
2. **Set up branch protection** (require CI to pass)
3. **Enable GitHub Pages** for JavaDoc
4. **Review Dependabot PRs** weekly

### Optional

1. Configure Docker Hub (for container distribution)
2. Set up Maven Central (for library distribution)
3. Add Codecov integration
4. Set up Slack/Discord notifications

---

## 🆘 Troubleshooting

### Workflow Not Running

**Check:**
1. GitHub Actions enabled in repo settings
2. Workflow file in `.github/workflows/`
3. YAML syntax valid (use YAML validator)
4. Branch/tag matches trigger conditions

### Build Failing

**Check:**
1. View logs in Actions tab
2. Look for red ❌ steps
3. Check error messages
4. Run locally: `mvn clean test`

### Can't Create Release

**Check:**
1. Tag format: `v1.2.3` (must start with 'v')
2. Secrets configured (if publishing)
3. GPG key valid and not expired

### Docker Push Failing

**Check:**
1. Docker Hub credentials valid
2. Repository exists on Docker Hub
3. Credentials have write access

---

## 📖 Documentation

- **Workflow Guide:** `.github/workflows/README.md`
- **Dependabot Config:** `.github/dependabot.yml`
- **PR Template:** `.github/pull_request_template.md`

---

## ✨ Summary

You now have a **complete CI/CD pipeline** including:

✅ **Automated testing** on every push/PR
✅ **Multi-platform support** (Linux, Windows, macOS)
✅ **Security scanning** with CodeQL
✅ **Automated releases** with version tagging
✅ **Documentation generation** and deployment
✅ **Performance monitoring** and benchmarks
✅ **Dependency management** with Dependabot

**Everything runs automatically** - just push your code! 🚀

---

## 🤝 Contributing

See `CONTRIBUTING.md` for contribution guidelines.

## 📄 License

CI/CD workflows are part of Veccy and follow the same license.
