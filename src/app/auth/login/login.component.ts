import {Component, OnDestroy, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {CommonModule} from '@angular/common';
import {AuthService} from '../../core/services/auth.service';
import {ReCaptchaV3Service} from 'ng-recaptcha-2';
import {environment} from '../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit, OnDestroy {
  loginForm: FormGroup;
  isLoading = false;
  private recaptchaScript: HTMLScriptElement | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private recaptchaV3Service: ReCaptchaV3Service
  ) {
    this.loginForm = this.fb.group({
      mobileNumber: ['', [
        Validators.required,
        Validators.pattern(/^[6-9]\d{9}$/), // Indian mobile number pattern
        Validators.minLength(10),
        Validators.maxLength(10)
      ]]
    });
  }

  ngOnInit(): void {
    // Check for return URL from invitation flow
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    if (returnUrl) {
      // Store the return URL in session storage so it persists through the OTP flow
      sessionStorage.setItem('auth_return_url', returnUrl);
    }

    // Check if user is already authenticated
    this.authService.isAuthenticated$.subscribe(isAuthenticated => {
      if (isAuthenticated === true) {
        // If there's a return URL, go there instead of default home
        const storedReturnUrl = sessionStorage.getItem('auth_return_url');
        if (storedReturnUrl) {
          sessionStorage.removeItem('auth_return_url');
          this.router.navigateByUrl(storedReturnUrl);
        } else {
          this.router.navigate(['/home']);
        }
      }
    });
  }

  ngOnDestroy(): void {
    // Clean up reCAPTCHA when component is destroyed
    this.cleanupRecaptcha();
  }

  onSubmit(): void {
    if (this.loginForm.valid && !this.isLoading) {
      this.isLoading = true;
      this.loginForm.get('mobileNumber')?.disable();
      const mobileNumber = this.loginForm.get('mobileNumber')?.value;

      // Check if reCAPTCHA is enabled for this environment
      if (!environment.recaptcha.enabled) {
        console.log('reCAPTCHA disabled for development, using dummy token');
        const dummyToken = 'dev-dummy-token-' + Date.now();
        this.handleAuthRequest(mobileNumber, dummyToken);
        return;
      }

      // Load reCAPTCHA dynamically and execute
      this.loadRecaptchaAndExecute(mobileNumber);
    }
  }

  private loadRecaptchaAndExecute(mobileNumber: string): void {
    console.log('Loading reCAPTCHA dynamically...');

    // Create and load the reCAPTCHA script
    this.recaptchaScript = document.createElement('script');
    this.recaptchaScript.src = `https://www.google.com/recaptcha/api.js?render=${environment.recaptcha.siteKey}`;
    this.recaptchaScript.async = true;
    this.recaptchaScript.defer = true;

    this.recaptchaScript.onload = () => {
      console.log('reCAPTCHA script loaded successfully');

      // Wait a bit for reCAPTCHA to initialize, then execute
      setTimeout(() => {
        this.executeRecaptcha(mobileNumber);
      }, 1000);
    };

    this.recaptchaScript.onerror = () => {
      console.error('Failed to load reCAPTCHA script');
      this.isLoading = false;
      this.loginForm.get('mobileNumber')?.enable();
      this.showError('Security verification failed. Please try again.');
    };

    document.head.appendChild(this.recaptchaScript);
  }

  private executeRecaptcha(mobileNumber: string): void {
    console.log('Attempting to execute reCAPTCHA...');
    this.recaptchaV3Service.execute('login').subscribe({
      next: (recaptchaToken: string) => {
        console.log('Received reCAPTCHA token:', recaptchaToken);

        if (!recaptchaToken) {
          console.error('reCAPTCHA token is null or empty');
          this.isLoading = false;
          this.loginForm.get('mobileNumber')?.enable();
          this.showError('Security verification failed. Please try again.');
          return;
        }

        this.handleAuthRequest(mobileNumber, recaptchaToken);

        // Clean up reCAPTCHA after successful token generation
        this.cleanupRecaptcha();
      },
      error: (recaptchaError) => {
        console.error('reCAPTCHA error:', recaptchaError);
        this.isLoading = false;
        this.loginForm.get('mobileNumber')?.enable();
        this.showError('Security verification failed. Please try again.');
        this.cleanupRecaptcha();
      }
    });
  }

  private cleanupRecaptcha(): void {
    console.log('Cleaning up reCAPTCHA...');

    // Remove the script
    if (this.recaptchaScript && this.recaptchaScript.parentNode) {
      this.recaptchaScript.parentNode.removeChild(this.recaptchaScript);
      this.recaptchaScript = null;
    }

    // Remove all reCAPTCHA related elements
    const recaptchaElements = document.querySelectorAll('.grecaptcha-badge, iframe[src*="recaptcha"]');
    recaptchaElements.forEach(element => {
      if (element.parentNode) {
        element.parentNode.removeChild(element);
      }
    });

    // Clean up global reCAPTCHA object
    if ((window as any).grecaptcha) {
      delete (window as any).grecaptcha;
    }
  }

  private async handleAuthRequest(mobileNumber: string, recaptchaToken: string): Promise<void> {
    try {
      const response = await this.authService.initAuth(mobileNumber, recaptchaToken);
      this.isLoading = false;
      this.loginForm.get('mobileNumber')?.enable();
      if (response && response.session_id) {
        // Store session ID for OTP verification
        sessionStorage.setItem('auth_session_id', response.session_id);
        sessionStorage.setItem('mobile_number', mobileNumber);

        this.snackBar.open('OTP sent successfully!', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });

        // Navigate to OTP verification page
        this.router.navigate(['/verify-otp']);
      } else {
        this.showError('Failed to send OTP');
      }
    } catch (error: any) {
      this.isLoading = false;
      this.loginForm.get('mobileNumber')?.enable();
      // Extract error message from interceptor-formatted error
      const errorMessage = error.error?.message || error.message || 'Failed to send OTP. Please try again.';
      this.showError(errorMessage);
    }
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }
}
