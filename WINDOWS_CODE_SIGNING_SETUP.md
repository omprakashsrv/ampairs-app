# Windows Code Signing Setup Guide

Complete guide to obtaining and configuring Windows code signing certificates for MSI installers.

## Table of Contents
- [Why Code Signing?](#why-code-signing)
- [Obtaining a Certificate](#obtaining-a-certificate)
- [Converting to Base64](#converting-to-base64)
- [Configuring GitHub Secrets](#configuring-github-secrets)
- [Testing the Setup](#testing-the-setup)
- [Troubleshooting](#troubleshooting)

---

## Why Code Signing?

Without code signing, Windows shows this warning during installation:

```
⚠️ Unknown Publisher
Windows protected your PC
```

With code signing, users see:

```
✅ Verified Publisher: Your Company Name
Digital signature is valid
```

**Benefits:**
- ✅ No "Unknown Publisher" warnings
- ✅ Users trust your application
- ✅ Passes Windows SmartScreen
- ✅ Enterprise deployment ready
- ✅ Professional appearance

---

## Obtaining a Certificate

### Step 1: Purchase from a Trusted Certificate Authority

**Recommended CAs (Certificate Authorities):**

| Provider | Price | Type | Link |
|----------|-------|------|------|
| **Sectigo** | $70-90/year | Standard | https://sectigo.com/ssl-certificates-tls/code-signing |
| **DigiCert** | $200-500/year | Standard/EV | https://www.digicert.com/signing/code-signing-certificates |
| **GlobalSign** | $200-400/year | Standard | https://www.globalsign.com/en/code-signing-certificate |
| **SSL.com** | $200-300/year | Standard | https://www.ssl.com/certificates/code-signing/ |

**Certificate Types:**

1. **Standard Code Signing** ($70-200/year)
   - ✅ Sufficient for most use cases
   - ✅ Removes "Unknown Publisher" warning
   - ✅ Validates company identity
   - ⚠️ Takes 1-3 days to build reputation with SmartScreen

2. **EV Code Signing** ($400-500/year)
   - ✅ Immediate SmartScreen reputation
   - ✅ Higher trust level
   - ✅ Requires hardware USB token
   - ❌ More expensive
   - ❌ Harder to use in CI/CD (requires physical token)

**Recommendation:** Start with **Standard Code Signing** for CI/CD automation.

### Step 2: Complete Identity Verification

The CA will require:

**For Companies:**
- Business registration documents
- Tax ID / EIN
- Phone verification
- Email verification
- Physical address verification

**For Individuals:**
- Government-issued ID
- Phone verification
- Email verification
- Address verification

**Time:** 1-5 business days depending on CA

### Step 3: Generate Certificate Signing Request (CSR)

**On Windows (using certreq):**
```powershell
# Create request.inf file
@"
[NewRequest]
Subject = "CN=Your Company Name, O=Your Organization, L=City, S=State, C=US"
KeyLength = 2048
KeySpec = 1
Exportable = TRUE
MachineKeySet = FALSE
ProviderName = "Microsoft Enhanced RSA and AES Cryptographic Provider"
RequestType = PKCS10
KeyUsage = 0xa0

[Extensions]
2.5.29.37 = "{text}1.3.6.1.5.5.7.3.3"  ; Code Signing
"@ | Out-File request.inf

# Generate CSR
certreq -new request.inf request.csr

# Submit request.csr to the CA
```

**Alternative (using OpenSSL):**
```bash
# Generate private key
openssl genrsa -out private.key 2048

# Generate CSR
openssl req -new -key private.key -out request.csr \
  -subj "/C=US/ST=State/L=City/O=Your Organization/CN=Your Company Name"

# Submit request.csr to the CA
```

### Step 4: Receive and Install Certificate

The CA will send you:
- Certificate file (`.cer` or `.crt`)
- Intermediate certificates (if applicable)

**Install on Windows:**
```powershell
# Import certificate
certmgr.msc
# File → Import → Select your certificate → Personal store
```

### Step 5: Export as PFX/P12

**Export from Windows Certificate Manager:**
```powershell
# Open certmgr.msc
# Navigate to: Personal → Certificates
# Right-click your certificate → All Tasks → Export
# Choose: "Yes, export the private key"
# Format: Personal Information Exchange (.PFX)
# Set a strong password
# Save as: ampairs-codesign.pfx
```

**Export using OpenSSL:**
```bash
# Combine certificate and private key
openssl pkcs12 -export \
  -out ampairs-codesign.pfx \
  -inkey private.key \
  -in certificate.crt \
  -certfile intermediate.crt \
  -password pass:YourStrongPassword
```

**⚠️ Important:**
- Use a **strong password** (12+ characters, mixed case, numbers, symbols)
- Store the `.pfx` file securely
- **Never commit the `.pfx` file to git**
- Keep a backup in a secure location

---

## Converting to Base64

GitHub Actions requires the certificate in base64 format for storage as a secret.

### On Linux/macOS:

```bash
# Convert PFX to base64 (single line)
base64 -i ampairs-codesign.pfx | tr -d '\n' > cert-base64.txt

# Display result
cat cert-base64.txt
```

### On Windows (PowerShell):

```powershell
# Convert PFX to base64
[Convert]::ToBase64String([IO.File]::ReadAllBytes("ampairs-codesign.pfx")) | Out-File cert-base64.txt

# Display result
Get-Content cert-base64.txt
```

### On Windows (Command Prompt):

```cmd
certutil -encode ampairs-codesign.pfx cert-base64.txt
```

**Result:** A long string like:
```
MIIKpAIBAzCCCmAGCSqGSIb3DQEHAaCCClEEggpNMIIKSTCCBgAGCSqGSIb3DQEH...
```

**⚠️ Important:**
- The base64 string should be **one continuous line** (no line breaks)
- If using `certutil`, remove the header/footer lines (`-----BEGIN CERTIFICATE-----`)
- Keep this string secure - it contains your private key!

---

## Configuring GitHub Secrets

### Step 1: Navigate to Repository Secrets

1. Go to your repository: https://github.com/omprakashsrv/ampairs-app
2. Click **Settings** tab
3. Navigate to **Secrets and variables** → **Actions**
4. Click **New repository secret**

### Step 2: Add WINDOWS_SIGN_CERT_BASE64

**Name:**
```
WINDOWS_SIGN_CERT_BASE64
```

**Value:**
```
MIIKpAIBAzCCCmAGCSqGSIb3DQEHAaCCClEEggpNMIIKSTCCBgAGCSqGSIb3DQEH...
(paste your entire base64 string here - it will be very long, ~5000 characters)
```

**⚠️ Important:**
- Paste the **entire base64 string** (no line breaks)
- Don't include header/footer if present
- Click **Add secret**

### Step 3: Add WINDOWS_SIGN_PASSWORD

**Name:**
```
WINDOWS_SIGN_PASSWORD
```

**Value:**
```
YourStrongPassword
(the password you set when exporting the PFX)
```

**⚠️ Important:**
- This is the password for the `.pfx` file
- Must match exactly (case-sensitive)
- Click **Add secret**

### Step 4: Verify Secrets

After adding both secrets, you should see:

```
WINDOWS_SIGN_CERT_BASE64    Updated X minutes ago
WINDOWS_SIGN_PASSWORD       Updated X minutes ago
```

**Security Notes:**
- Secrets are encrypted at rest
- Only visible during workflow runs
- Can be updated but not viewed after creation
- Rotate annually when renewing certificate

---

## Testing the Setup

### Option 1: Trigger a Release

```bash
# Create test release tag
git tag -a v1.0.0.14-test -m "Test code signing setup"
git push origin v1.0.0.14-test
```

**Watch workflow logs for:**
```
✅ Code signing configured
...
✅ MSI signed successfully: Ampairs-1.0.0.14-test.msi
...
✅ MSI is properly signed
Signer: CN=Your Company Name
```

### Option 2: Manual Local Test

**On Windows with signtool:**

```powershell
# Decode certificate locally
$certBytes = [Convert]::FromBase64String("YOUR_BASE64_STRING")
[IO.File]::WriteAllBytes("test-cert.pfx", $certBytes)

# Sign a test file
signtool.exe sign `
  /f test-cert.pfx `
  /p "YOUR_PASSWORD" `
  /fd SHA256 `
  /tr http://timestamp.digicert.com `
  /td SHA256 `
  /d "Test Application" `
  test-file.exe

# Verify signature
signtool.exe verify /pa test-file.exe

# Clean up
Remove-Item test-cert.pfx
```

**Expected output:**
```
Successfully signed: test-file.exe

Number of files successfully Signed: 1
Number of warnings: 0
Number of errors: 0
```

### Option 3: Verify MSI After Build

**Download MSI from GitHub Release and verify:**

**On Windows:**
```powershell
# Right-click MSI → Properties → Digital Signatures
# Should show: Your Company Name (verified)
```

**Or use PowerShell:**
```powershell
Get-AuthenticodeSignature -FilePath "Ampairs-1.0.0.14.msi"

# Expected output:
# Status: Valid
# SignerCertificate: CN=Your Company Name
```

---

## Troubleshooting

### Issue: "Certificate file not found"

**Symptoms:**
```
⚠️ Code signing secrets not configured - MSI will be unsigned
```

**Causes:**
1. Secrets not set in GitHub
2. Secret names misspelled
3. Running on forked repository (secrets not available)

**Fix:**
1. Verify secrets exist: GitHub → Settings → Secrets → Actions
2. Check exact spelling: `WINDOWS_SIGN_CERT_BASE64` and `WINDOWS_SIGN_PASSWORD`
3. Ensure running on main repository (not fork)

### Issue: "Invalid password"

**Symptoms:**
```
❌ MSI signing failed with exit code: 1
Error: The specified network password is not correct
```

**Causes:**
1. Wrong password in secret
2. Password changed after export
3. Special characters not escaped

**Fix:**
1. Re-export PFX with new password
2. Update `WINDOWS_SIGN_PASSWORD` secret
3. Test password locally first

### Issue: "signtool.exe not found"

**Symptoms:**
```
❌ Failed to sign MSI: signtool.exe is not recognized
```

**Causes:**
- Windows SDK not installed on runner

**Fix:**
- GitHub's `windows-latest` runners include signtool.exe by default
- If running locally, install Windows SDK: https://developer.microsoft.com/windows/downloads/windows-sdk/

### Issue: "Timestamp server unavailable"

**Symptoms:**
```
Warning: Timestamp Server Warning: The timestamp signature or certificate could not be verified or is malformed
```

**Impact:**
- Signature is valid but won't have timestamp
- Signature expires when certificate expires

**Fix:**
- Use reliable timestamp server: `http://timestamp.digicert.com`
- Alternative: `http://timestamp.sectigo.com`
- Build will retry automatically

### Issue: "Certificate expired"

**Symptoms:**
```
❌ The specified certificate is not valid
```

**Fix:**
1. Renew certificate with CA (usually 30 days before expiration)
2. Export new PFX
3. Convert to base64
4. Update `WINDOWS_SIGN_CERT_BASE64` secret
5. Keep old certificate for verifying existing releases

### Issue: "Base64 decode failed"

**Symptoms:**
```
Exception calling "FromBase64String"
```

**Causes:**
1. Line breaks in base64 string
2. Header/footer included
3. Whitespace in string

**Fix:**
```bash
# Ensure single line, no header/footer
base64 -i cert.pfx | tr -d '\n' > cert-base64.txt
```

---

## Certificate Lifecycle

### Annual Renewal (Recommended Schedule)

**90 days before expiration:**
- [ ] Contact CA to start renewal process
- [ ] Complete identity verification (faster for existing customers)

**30 days before expiration:**
- [ ] Receive new certificate
- [ ] Export as PFX with new password
- [ ] Convert to base64
- [ ] Update GitHub secrets
- [ ] Test with a pre-release

**After renewal:**
- [ ] Keep old certificate for historical verification
- [ ] Update documentation with new expiration date
- [ ] Set calendar reminder for next renewal

### Security Best Practices

1. **Store certificates securely:**
   - Use password manager for passwords
   - Encrypted backup of PFX files
   - Limit access to authorized personnel

2. **Rotate passwords:**
   - Change PFX password annually
   - Use strong, unique passwords
   - Never reuse passwords

3. **Monitor certificate:**
   - Set expiration reminders (90, 60, 30 days)
   - Verify signatures regularly
   - Check revocation status

4. **Audit usage:**
   - Review GitHub Actions logs
   - Track signed releases
   - Monitor for unauthorized use

---

## Cost Analysis

### Initial Setup

| Item | Cost | Frequency |
|------|------|-----------|
| Certificate (Standard) | $70-200 | Annual |
| Certificate (EV) | $400-500 | Annual |
| Setup time | 2-4 hours | One-time |

### Ongoing Costs

| Item | Cost | Frequency |
|------|------|-----------|
| Annual renewal | Same as initial | Yearly |
| Time to renew | 30-60 min | Yearly |

**Total Annual Cost:**
- Standard: $70-200/year
- EV: $400-500/year

**ROI Benefits:**
- ✅ Professional appearance
- ✅ No security warnings
- ✅ Higher user trust
- ✅ Better conversion rates
- ✅ Enterprise deployment ready

---

## Alternative: Building Without Code Signing

If you're not ready to purchase a certificate, you can still build and distribute:

**What happens:**
- MSI builds successfully
- No signature verification
- Windows shows "Unknown Publisher" warning
- Users must click "More info" → "Run anyway"

**When to skip signing:**
- ✅ Internal testing
- ✅ Development builds
- ✅ Pre-alpha releases
- ✅ Budget constraints

**Workflow behavior:**
```
⚠️ Code signing secrets not configured - MSI will be unsigned
... (build continues normally)
⏭️ Signature verification skipped (no certificate configured)
```

The MSI is still fully functional, just not signed.

---

## Support Resources

**Certificate Authorities:**
- Sectigo Support: https://sectigo.com/support
- DigiCert Support: https://www.digicert.com/support
- GlobalSign Support: https://support.globalsign.com

**Microsoft Documentation:**
- SignTool: https://docs.microsoft.com/windows/win32/seccrypto/signtool
- Code Signing: https://docs.microsoft.com/windows-hardware/drivers/dashboard/code-signing-cert-manage

**GitHub Actions:**
- Encrypted Secrets: https://docs.github.com/actions/security-guides/encrypted-secrets

**Community:**
- Stack Overflow: https://stackoverflow.com/questions/tagged/code-signing
- Reddit: r/sysadmin, r/devops

---

## Quick Reference

### Certificate Purchase Checklist
- [ ] Choose CA (Sectigo recommended for cost)
- [ ] Select certificate type (Standard for CI/CD)
- [ ] Complete identity verification
- [ ] Generate CSR
- [ ] Receive certificate from CA
- [ ] Export as PFX with strong password
- [ ] Store securely

### GitHub Secrets Setup Checklist
- [ ] Convert PFX to base64 (single line)
- [ ] Add `WINDOWS_SIGN_CERT_BASE64` secret
- [ ] Add `WINDOWS_SIGN_PASSWORD` secret
- [ ] Verify secrets are saved
- [ ] Test with release tag
- [ ] Verify MSI is signed

### Annual Renewal Checklist
- [ ] Renew certificate 30 days before expiration
- [ ] Export new PFX
- [ ] Update GitHub secrets
- [ ] Test with pre-release
- [ ] Update documentation
- [ ] Set next year's reminder

---

**Generated with Claude Code**
