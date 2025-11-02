# Desktop Browser Authentication Implementation Guide

## Overview

This document describes how to implement browser-side authentication support for the Ampairs Desktop application. The desktop app supports two authentication methods:

1. **Deep Link Method** (Preferred): Browser automatically redirects to `ampairs://auth` deep link
2. **Manual Token Paste** (Fallback): User copies JSON tokens and pastes them into the desktop app

## Browser Implementation Requirements

### 1. Detecting Desktop Client

When the desktop app opens the authentication URL, it includes a query parameter `client=desktop`:

```
http://localhost:4200/login?client=desktop
https://app.ampairs.com/login?client=desktop
```

**Angular Route Detection:**
```typescript
import { ActivatedRoute } from '@angular/router';

export class LoginComponent implements OnInit {
  isDesktopClient = false;

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.isDesktopClient = params['client'] === 'desktop';
    });
  }
}
```

### 2. Authentication Flow

#### Standard Flow (Same as Web/Mobile)
1. User enters phone number
2. User receives and enters OTP
3. Backend returns JWT tokens (access_token, refresh_token)

#### Desktop-Specific Flow (After Token Receipt)

When `isDesktopClient === true`, show the desktop authentication completion UI instead of redirecting to the main app.

### 3. Desktop Authentication Completion UI

After successful authentication, display TWO methods for the user to complete desktop login:

#### Method 1: Deep Link (Primary - Automatic)

Automatically attempt to redirect to the deep link URL:

```typescript
completeDesktopAuth(accessToken: string, refreshToken: string) {
  if (this.isDesktopClient) {
    const deepLinkUrl = `ampairs://auth?access_token=${encodeURIComponent(accessToken)}&refresh_token=${encodeURIComponent(refreshToken)}`;

    // Attempt automatic deep link
    window.location.href = deepLinkUrl;

    // Show success message
    this.showDeepLinkAttemptMessage();
  }
}
```

**UI Template (Angular Material 3):**
```html
<mat-card class="desktop-auth-card" *ngIf="isDesktopClient && authSuccess">
  <mat-card-header>
    <mat-card-title>Desktop Authentication Successful</mat-card-title>
  </mat-card-header>

  <mat-card-content>
    <div class="success-icon">
      <mat-icon color="primary">check_circle</mat-icon>
    </div>

    <p class="status-message">
      Redirecting to desktop app...
    </p>

    <p class="sub-message">
      If your desktop app doesn't open automatically, use the "Copy Tokens" option below.
    </p>
  </mat-card-content>
</mat-card>
```

#### Method 2: Manual Token Copy (Fallback)

Provide a button to copy tokens in JSON format:

```typescript
export class LoginComponent {
  tokensJson = '';

  generateTokensJson(accessToken: string, refreshToken: string): string {
    return JSON.stringify({
      access_token: accessToken,
      refresh_token: refreshToken
    }, null, 2); // Pretty print with 2-space indentation
  }

  copyTokensToClipboard() {
    navigator.clipboard.writeText(this.tokensJson).then(() => {
      this.showCopySuccessMessage();
    }).catch(err => {
      console.error('Failed to copy tokens:', err);
      this.showCopyErrorMessage();
    });
  }
}
```

**UI Template:**
```html
<mat-card class="manual-copy-card" *ngIf="isDesktopClient && authSuccess">
  <mat-card-header>
    <mat-card-title>Alternative: Copy Tokens</mat-card-title>
    <mat-card-subtitle>If automatic redirect doesn't work</mat-card-subtitle>
  </mat-card-header>

  <mat-card-content>
    <p class="instructions">
      Copy these tokens and paste them into the desktop app:
    </p>

    <!-- Read-only text area showing the JSON -->
    <mat-form-field appearance="outline" class="tokens-field">
      <mat-label>Authentication Tokens (JSON)</mat-label>
      <textarea
        matInput
        [value]="tokensJson"
        readonly
        rows="6"
        class="monospace-text">
      </textarea>
    </mat-form-field>

    <div class="action-buttons">
      <button
        mat-raised-button
        color="primary"
        (click)="copyTokensToClipboard()">
        <mat-icon>content_copy</mat-icon>
        Copy Tokens
      </button>

      <button
        mat-button
        (click)="showManualInstructions()">
        <mat-icon>help_outline</mat-icon>
        How to use?
      </button>
    </div>

    <!-- Success message -->
    <mat-chip-set *ngIf="tokensCopied">
      <mat-chip color="accent">
        <mat-icon matChipAvatar>check</mat-icon>
        Copied! Now paste in desktop app
      </mat-chip>
    </mat-chip-set>
  </mat-card-content>
</mat-card>
```

### 4. Complete Implementation Example

**login.component.ts:**
```typescript
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

export class LoginComponent implements OnInit {
  // Desktop client detection
  isDesktopClient = false;

  // Authentication state
  authSuccess = false;
  accessToken = '';
  refreshToken = '';

  // UI state
  tokensJson = '';
  tokensCopied = false;
  showInstructions = false;

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    // Detect desktop client
    this.route.queryParams.subscribe(params => {
      this.isDesktopClient = params['client'] === 'desktop';
    });
  }

  /**
   * Called after successful OTP verification
   */
  onAuthSuccess(response: AuthResponse) {
    this.authSuccess = true;
    this.accessToken = response.access_token;
    this.refreshToken = response.refresh_token;

    if (this.isDesktopClient) {
      this.handleDesktopAuth();
    } else {
      // Normal web flow - redirect to dashboard
      this.router.navigate(['/dashboard']);
    }
  }

  /**
   * Handle desktop authentication completion
   */
  private handleDesktopAuth() {
    // Generate JSON for manual copy
    this.tokensJson = JSON.stringify({
      access_token: this.accessToken,
      refresh_token: this.refreshToken
    }, null, 2);

    // Attempt automatic deep link
    this.attemptDeepLink();

    // Show desktop completion UI
    // (UI will show both automatic redirect status and manual copy option)
  }

  /**
   * Attempt to open desktop app via deep link
   */
  private attemptDeepLink() {
    const deepLinkUrl = `ampairs://auth?access_token=${encodeURIComponent(this.accessToken)}&refresh_token=${encodeURIComponent(this.refreshToken)}`;

    try {
      window.location.href = deepLinkUrl;
      console.log('Desktop deep link triggered:', deepLinkUrl);
    } catch (error) {
      console.error('Failed to trigger deep link:', error);
      // Fallback UI is already visible
    }
  }

  /**
   * Copy tokens to clipboard
   */
  copyTokensToClipboard() {
    navigator.clipboard.writeText(this.tokensJson)
      .then(() => {
        this.tokensCopied = true;
        this.snackBar.open('Tokens copied! Paste them in the desktop app.', 'OK', {
          duration: 5000
        });

        // Reset copied state after 5 seconds
        setTimeout(() => this.tokensCopied = false, 5000);
      })
      .catch(err => {
        console.error('Failed to copy tokens:', err);
        this.snackBar.open('Failed to copy. Please select and copy manually.', 'OK', {
          duration: 3000
        });
      });
  }

  /**
   * Show manual instructions dialog
   */
  showManualInstructions() {
    const dialogRef = this.dialog.open(DesktopAuthInstructionsDialog, {
      width: '500px'
    });
  }
}
```

**login.component.html:**
```html
<!-- Normal login form (same as before) -->
<mat-card class="login-card" *ngIf="!authSuccess">
  <!-- Phone number input -->
  <!-- OTP input -->
  <!-- Etc. -->
</mat-card>

<!-- Desktop authentication completion (shown after auth success) -->
<div class="desktop-auth-container" *ngIf="isDesktopClient && authSuccess">
  <!-- Primary: Automatic redirect status -->
  <mat-card class="desktop-auth-card">
    <mat-card-header>
      <mat-icon mat-card-avatar color="primary">desktop_windows</mat-icon>
      <mat-card-title>Desktop Authentication Successful</mat-card-title>
      <mat-card-subtitle>Connecting to desktop app...</mat-card-subtitle>
    </mat-card-header>

    <mat-card-content>
      <div class="status-section">
        <mat-spinner diameter="40"></mat-spinner>
        <p class="status-text">
          Attempting to open desktop app automatically...
        </p>
        <p class="help-text">
          If the app doesn't open in 5 seconds, use the manual option below.
        </p>
      </div>
    </mat-card-content>
  </mat-card>

  <!-- Fallback: Manual token copy -->
  <mat-card class="manual-copy-card">
    <mat-card-header>
      <mat-card-title>
        <mat-icon>content_paste</mat-icon>
        Alternative: Copy Tokens Manually
      </mat-card-title>
      <mat-card-subtitle>
        If your browser doesn't support automatic redirect
      </mat-card-subtitle>
    </mat-card-header>

    <mat-card-content>
      <ol class="instructions-list">
        <li>Click the "Copy Tokens" button below</li>
        <li>Go back to the desktop app</li>
        <li>Click "Browser doesn't support deep links? Paste tokens here"</li>
        <li>Paste the tokens in the text field</li>
        <li>Click "Sign In"</li>
      </ol>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Authentication Tokens (JSON)</mat-label>
        <textarea
          matInput
          [value]="tokensJson"
          readonly
          rows="8"
          class="monospace">
        </textarea>
        <mat-hint>Copy this entire JSON object</mat-hint>
      </mat-form-field>

      <div class="button-row">
        <button
          mat-raised-button
          color="primary"
          (click)="copyTokensToClipboard()"
          class="copy-button">
          <mat-icon>content_copy</mat-icon>
          {{ tokensCopied ? 'Copied!' : 'Copy Tokens' }}
        </button>

        <button
          mat-stroked-button
          (click)="showManualInstructions()">
          <mat-icon>help_outline</mat-icon>
          Show Instructions
        </button>
      </div>

      <mat-chip-set *ngIf="tokensCopied" class="success-chips">
        <mat-chip color="accent">
          <mat-icon matChipAvatar>check_circle</mat-icon>
          Tokens copied successfully!
        </mat-chip>
      </mat-chip-set>
    </mat-card-content>
  </mat-card>
</div>
```

**login.component.scss:**
```scss
.desktop-auth-container {
  max-width: 600px;
  margin: 24px auto;
  padding: 16px;

  .desktop-auth-card {
    margin-bottom: 24px;

    .status-section {
      text-align: center;
      padding: 24px;

      mat-spinner {
        margin: 0 auto 16px;
      }

      .status-text {
        font-size: 16px;
        margin: 16px 0 8px;
      }

      .help-text {
        font-size: 14px;
        color: rgba(0, 0, 0, 0.6);
      }
    }
  }

  .manual-copy-card {
    .instructions-list {
      padding-left: 20px;
      margin-bottom: 16px;

      li {
        margin-bottom: 8px;
        line-height: 1.5;
      }
    }

    .monospace {
      font-family: 'Courier New', monospace;
      font-size: 12px;
    }

    .button-row {
      display: flex;
      gap: 8px;
      margin-top: 16px;

      .copy-button {
        flex: 1;
      }
    }

    .success-chips {
      margin-top: 16px;
    }
  }
}
```

### 5. Instructions Dialog Component

**desktop-auth-instructions-dialog.component.ts:**
```typescript
import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-desktop-auth-instructions',
  template: `
    <h2 mat-dialog-title>How to Complete Desktop Authentication</h2>

    <mat-dialog-content>
      <mat-stepper orientation="vertical" [linear]="false">
        <mat-step>
          <ng-template matStepLabel>Copy the tokens</ng-template>
          <p>Click the "Copy Tokens" button to copy the authentication JSON.</p>
        </mat-step>

        <mat-step>
          <ng-template matStepLabel>Return to desktop app</ng-template>
          <p>Switch back to the Ampairs desktop application window.</p>
        </mat-step>

        <mat-step>
          <ng-template matStepLabel>Open paste option</ng-template>
          <p>Click the button that says "Browser doesn't support deep links? Paste tokens here"</p>
        </mat-step>

        <mat-step>
          <ng-template matStepLabel>Paste and sign in</ng-template>
          <p>Paste the tokens in the text field and click "Sign In"</p>
        </mat-step>
      </mat-stepper>

      <mat-divider style="margin: 16px 0;"></mat-divider>

      <div class="note">
        <mat-icon color="primary">info</mat-icon>
        <p>
          <strong>Note:</strong> The automatic redirect should work in most browsers.
          This manual method is only needed if your browser doesn't support
          custom URL schemes (ampairs://).
        </p>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Got it</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .note {
      display: flex;
      gap: 12px;
      align-items: flex-start;
      padding: 12px;
      background-color: rgba(33, 150, 243, 0.1);
      border-radius: 4px;

      mat-icon {
        flex-shrink: 0;
      }

      p {
        margin: 0;
      }
    }
  `]
})
export class DesktopAuthInstructionsDialog {
  constructor(public dialogRef: MatDialogRef<DesktopAuthInstructionsDialog>) {}
}
```

## Security Considerations

### 1. Token Exposure
- Tokens are displayed in the browser only temporarily
- Use read-only text fields to prevent accidental modification
- Clear tokens when user navigates away (optional)

### 2. HTTPS Requirement
- Production URLs must use HTTPS to prevent token interception
- Deep link URLs encode tokens in query params (ensure proper URL encoding)

### 3. Token Validation
- Desktop app validates token format before use
- Minimum token length check (10 characters)
- JSON structure validation

## Testing Checklist

### Desktop Client Detection
- [ ] URL parameter `client=desktop` is correctly detected
- [ ] Desktop-specific UI is shown only when parameter is present
- [ ] Normal web flow works when parameter is absent

### Deep Link Method
- [ ] Deep link URL is correctly formatted: `ampairs://auth?access_token=xxx&refresh_token=yyy`
- [ ] Tokens are properly URL-encoded
- [ ] Browser attempts to open desktop app automatically
- [ ] Desktop app receives and processes tokens correctly

### Manual Copy Method
- [ ] JSON is correctly formatted with proper structure
- [ ] Copy button successfully copies to clipboard
- [ ] Visual feedback is shown after copy
- [ ] Instructions are clear and accessible
- [ ] Pasted tokens work in desktop app

### Error Handling
- [ ] Failed clipboard copy shows appropriate error message
- [ ] Invalid tokens show validation errors in desktop app
- [ ] User can retry authentication if needed

## Browser Compatibility

### Deep Link Support
- ✅ **Chrome/Edge**: Full support
- ✅ **Firefox**: Full support (may show confirmation dialog)
- ✅ **Safari**: Full support
- ⚠️ **Older browsers**: May not support - manual copy fallback required

### Clipboard API Support
- ✅ **Modern browsers**: `navigator.clipboard.writeText()` supported
- ⚠️ **Older browsers**: Use fallback method with `document.execCommand('copy')`

## Additional Resources

- Desktop app documentation: `/docs/DESKTOP_AUTH.md`
- Deep link handler: `/composeApp/src/desktopMain/kotlin/com/ampairs/auth/deeplink/DeepLinkHandler.desktop.kt`
- Desktop auth screen: `/composeApp/src/desktopMain/kotlin/com/ampairs/auth/ui/DesktopBrowserAuthScreen.kt`
