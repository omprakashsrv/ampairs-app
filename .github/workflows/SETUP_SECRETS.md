# GitHub Secrets Setup Guide

This guide walks you through setting up GitHub secrets required for building signed Android packages.

## 📋 Prerequisites

You need the following information from your `composeApp/build.gradle.kts`:

- **Keystore file**: `ampairs.jks` (located at project root: `/ampairs-app/ampairs.jks`)
- **Keystore password**: `SKFNNFJ234329898g723g47823gr8`
- **Key password**: `SKFNNFJ234329898g723g47823gr8`
- **Key alias**: `ampairs`

## 🔐 Step-by-Step Setup

### Step 1: Encode Keystore to Base64

#### On macOS/Linux:

```bash
# Navigate to project root
cd /Users/omprakashsrv/StudioProjects/ampairs-app

# Encode keystore and copy to clipboard (macOS)
base64 -i ampairs.jks | pbcopy

# OR encode and print to terminal (Linux)
base64 -i ampairs.jks

# OR encode and save to file
base64 -i ampairs.jks > keystore_base64.txt
```

#### On Windows:

```powershell
# Using PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("ampairs.jks")) | Set-Clipboard

# OR using certutil
certutil -encode ampairs.jks keystore_base64.txt
# Then open keystore_base64.txt and copy the content (excluding BEGIN/END lines)
```

### Step 2: Add Secrets to GitHub

1. **Navigate to GitHub Repository Settings**
   - Go to your repository: `https://github.com/yourusername/ampairs-app`
   - Click **Settings** tab
   - In left sidebar, click **Secrets and variables** → **Actions**

2. **Add Each Secret**

   Click **New repository secret** and add the following secrets one by one:

   #### Secret 1: ANDROID_KEYSTORE_BASE64
   - **Name**: `ANDROID_KEYSTORE_BASE64`
   - **Value**: Paste the base64-encoded keystore string from Step 1
   - Click **Add secret**

   #### Secret 2: KEYSTORE_PASSWORD
   - **Name**: `KEYSTORE_PASSWORD`
   - **Value**: `SKFNNFJ234329898g723g47823gr8`
   - Click **Add secret**

   #### Secret 3: KEY_PASSWORD
   - **Name**: `KEY_PASSWORD`
   - **Value**: `SKFNNFJ234329898g723g47823gr8`
   - Click **Add secret**

   #### Secret 4: KEY_ALIAS
   - **Name**: `KEY_ALIAS`
   - **Value**: `ampairs`
   - Click **Add secret**

### Step 3: Verify Secrets

After adding all secrets, you should see:

```
ANDROID_KEYSTORE_BASE64  ••••••••
KEY_ALIAS                ••••••••
KEY_PASSWORD             ••••••••
KEYSTORE_PASSWORD        ••••••••
```

## ✅ Testing the Setup

### Test with a Manual Build

1. Go to **Actions** tab
2. Click **Build Multi-Platform Installers** workflow
3. Click **Run workflow** dropdown
4. Select branch (e.g., `main`)
5. Click **Run workflow**
6. Wait for builds to complete
7. Check if Android APK/AAB builds succeed

### Expected Results

- ✅ macOS DMG builds successfully
- ✅ Windows MSI builds successfully
- ✅ Linux DEB builds successfully
- ✅ Android APK builds and is signed
- ✅ Android AAB builds and is signed

## 🔧 Troubleshooting

### Error: "Keystore file not found"

**Cause**: Base64 string is incorrect or has line breaks

**Solution**:
1. Re-encode keystore ensuring no line breaks:
   ```bash
   base64 -i ampairs.jks | tr -d '\n'
   ```
2. Update `ANDROID_KEYSTORE_BASE64` secret with new value

### Error: "Incorrect keystore password"

**Cause**: Password in secret doesn't match actual keystore password

**Solution**:
1. Verify password in `build.gradle.kts` (line 229)
2. Update `KEYSTORE_PASSWORD` secret if different

### Error: "Alias not found"

**Cause**: Key alias in secret doesn't match keystore

**Solution**:
1. Verify alias in `build.gradle.kts` (line 231)
2. Update `KEY_ALIAS` secret if different

## 🔒 Security Best Practices

### ✅ DO:
- Keep secrets in GitHub Secrets (encrypted at rest)
- Rotate keystore passwords periodically
- Use different keystores for debug and release builds
- Backup keystore securely (required for app updates)

### ❌ DON'T:
- Commit keystore files to version control
- Share secrets in plain text
- Use same passwords for multiple environments
- Store secrets in code or config files

## 📝 Updating Secrets

If you need to update a secret:

1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Find the secret to update
3. Click **Update** (or delete and re-create)
4. Enter new value
5. Click **Update secret**

## 🔄 Changing Keystore

If you need to use a different keystore:

1. Update `build.gradle.kts` with new keystore details
2. Encode new keystore to base64
3. Update `ANDROID_KEYSTORE_BASE64` secret
4. Update password and alias secrets if different

**⚠️ Warning**: Changing keystore will prevent updating existing app installations. Only do this for new apps or major version changes.

## 📚 Additional Resources

- [GitHub Encrypted Secrets Documentation](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Android App Signing Guide](https://developer.android.com/studio/publish/app-signing)
- [Managing Keystores](https://developer.android.com/studio/publish/app-signing#manage-key)

## ✨ Summary Checklist

- [ ] Keystore file (`ampairs.jks`) exists and is accessible
- [ ] Keystore encoded to base64 successfully
- [ ] `ANDROID_KEYSTORE_BASE64` secret added to GitHub
- [ ] `KEYSTORE_PASSWORD` secret added to GitHub
- [ ] `KEY_PASSWORD` secret added to GitHub
- [ ] `KEY_ALIAS` secret added to GitHub
- [ ] Test workflow run completed successfully
- [ ] Android APK/AAB builds are signed correctly

Once all checkboxes are ticked, you're ready to build releases! 🎉
