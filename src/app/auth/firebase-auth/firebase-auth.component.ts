import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FirebaseAuthService } from '../../core/services/firebase-auth.service';
import { AuthService } from '../../core/services/auth.service';
import { DeviceService } from '../../core/services/device.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-firebase-auth',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatSnackBarModule
  ],
  templateUrl: './firebase-auth.component.html',
  styleUrl: './firebase-auth.component.scss'
})
export class FirebaseAuthComponent implements OnInit, OnDestroy {
  phoneForm: FormGroup;
  otpForm: FormGroup;

  step = signal<'phone' | 'otp' | 'success'>('phone');
  countdown = signal(0);
  maskedPhone = signal('');

  private countdownInterval: any;

  constructor(
    private fb: FormBuilder,
    private firebaseAuth: FirebaseAuthService,
    private authService: AuthService,
    private deviceService: DeviceService,
    private snackBar: MatSnackBar
  ) {
    // Fixed to India only (+91)
    this.phoneForm = this.fb.group({
      countryCode: [91],
      phone: ['', [
        Validators.required,
        Validators.pattern('^[6-9][0-9]{9}$')
      ]]
    });

    this.otpForm = this.fb.group({
      otp: ['', [
        Validators.required,
        Validators.pattern('^[0-9]{6}$')
      ]]
    });
  }

  ngOnInit(): void {
    // Initialize reCAPTCHA after view is ready
    setTimeout(() => {
      this.firebaseAuth.initRecaptcha('recaptcha-container');
    }, 100);
  }

  ngOnDestroy(): void {
    this.clearCountdown();
    this.firebaseAuth.resetRecaptcha();
  }

  async onSendOTP(): Promise<void> {
    if (this.phoneForm.invalid) {
      return;
    }

    const { countryCode, phone } = this.phoneForm.value;
    const fullPhoneNumber = `+${countryCode}${phone}`;

    try {
      await this.firebaseAuth.sendOTP(fullPhoneNumber);
      this.maskedPhone.set(this.maskPhoneNumber(phone));
      this.step.set('otp');
      this.startCountdown(30);
      this.showMessage('OTP sent successfully');
    } catch (error: any) {
      this.showMessage(error.message || 'Failed to send OTP', 'error');
    }
  }

  async onVerifyOTP(): Promise<void> {
    if (this.otpForm.invalid) {
      return;
    }

    const { otp } = this.otpForm.value;

    try {
      // Step 1: Verify OTP with Firebase and get Firebase ID token
      const firebaseToken = await this.firebaseAuth.verifyOTP(otp);

      // Step 2: Exchange Firebase token for backend JWT tokens
      const { countryCode, phone } = this.phoneForm.value;
      const deviceInfo = await this.deviceService.getDeviceInfo();

      const authResponse = await this.authService.authenticateWithFirebase({
        firebase_id_token: firebaseToken,
        country_code: countryCode,
        phone: phone,
        device_id: deviceInfo.device_id,
        device_name: deviceInfo.device_name,
        device_type: deviceInfo.device_type,
        platform: deviceInfo.platform,
        browser: deviceInfo.browser,
        os: deviceInfo.os,
        user_agent: deviceInfo.user_agent
      });

      // Step 3: Redirect based on platform
      this.step.set('success');
      this.showMessage('Authentication successful!');
      this.redirectToDesktopApp(authResponse.access_token, authResponse.refresh_token);
    } catch (error: any) {
      this.showMessage(error.message || 'Invalid OTP code', 'error');
    }
  }

  async onResendOTP(): Promise<void> {
    if (this.countdown() > 0) {
      return;
    }

    // Reset and reinitialize reCAPTCHA
    this.firebaseAuth.resetRecaptcha();
    setTimeout(() => {
      this.firebaseAuth.initRecaptcha('recaptcha-container');
    }, 100);

    await this.onSendOTP();
  }

  onBackToPhone(): void {
    this.step.set('phone');
    this.otpForm.reset();
    this.clearCountdown();
  }

  private redirectToDesktopApp(accessToken: string, refreshToken: string): void {
    // Check if we're in a desktop app context or web browser
    const isDesktopApp = this.isRunningInDesktopApp();

    if (isDesktopApp) {
      // Try deep link for desktop app
      const { scheme, host } = environment.deepLink;
      const deepLink = `${scheme}://${host}?access_token=${encodeURIComponent(accessToken)}&refresh_token=${encodeURIComponent(refreshToken)}`;

      try {
        window.location.href = deepLink;
      } catch (error) {
        console.error('Failed to open deep link:', error);
        this.redirectToWorkspaces();
      }
    } else {
      // For web browsers, redirect to workspaces page
      // Tokens are already stored by authService
      this.redirectToWorkspaces();
    }
  }

  private isRunningInDesktopApp(): boolean {
    // Check if running in Electron or other desktop wrapper
    const userAgent = navigator.userAgent.toLowerCase();
    return userAgent.includes('electron') ||
           userAgent.includes('ampairs-desktop') ||
           (window as any).isDesktopApp === true;
  }

  private redirectToWorkspaces(): void {
    // Use Angular router for navigation
    setTimeout(() => {
      window.location.href = '/workspaces';
    }, 1500);
  }

  private maskPhoneNumber(phone: string): string {
    if (phone.length < 4) {
      return phone;
    }
    const lastFour = phone.slice(-4);
    const masked = '*'.repeat(phone.length - 4);
    return masked + lastFour;
  }

  private startCountdown(seconds: number): void {
    this.countdown.set(seconds);
    this.countdownInterval = setInterval(() => {
      const current = this.countdown();
      if (current > 0) {
        this.countdown.set(current - 1);
      } else {
        this.clearCountdown();
      }
    }, 1000);
  }

  private clearCountdown(): void {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
      this.countdownInterval = null;
    }
    this.countdown.set(0);
  }

  private showMessage(message: string, type: 'success' | 'error' = 'success'): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      horizontalPosition: 'center',
      verticalPosition: 'top',
      panelClass: type === 'error' ? ['error-snackbar'] : ['success-snackbar']
    });
  }

  // Getter for template access
  get firebaseAuthService() {
    return this.firebaseAuth;
  }
}
