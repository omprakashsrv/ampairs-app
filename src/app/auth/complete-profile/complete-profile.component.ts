import {Component, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatButtonModule} from '@angular/material/button';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatIconModule} from '@angular/material/icon';
import {ActivatedRoute, Router} from '@angular/router';
import {Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {AuthService, User} from '../../core/services/auth.service';

@Component({
  selector: 'app-complete-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule
  ],
  templateUrl: './complete-profile.component.html',
  styleUrl: './complete-profile.component.scss'
})
export class CompleteProfileComponent implements OnInit, OnDestroy {
  profileForm: FormGroup;
  isSubmitting = false;
  currentUser: User | null = null;
  isEditMode = false;
  isInLayout = false;
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {
    this.profileForm = this.createForm();
  }

  ngOnInit(): void {
    // Check if component is loaded within home layout
    this.isInLayout = this.router.url.includes('/home/profile');

    // Check if this is edit mode from query params
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.isEditMode = params['edit'] === 'true';
      });

    // Subscribe to current user changes using signal
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe((user: User | null) => {
        this.currentUser = user;
        if (user) {
          this.updateFormWithUserData(user);

          // If user already has complete profile and NOT in edit mode, redirect to workspace selection
          if (!this.isEditMode && !this.authService.isProfileIncomplete(user)) {
            this.router.navigate(['/workspaces']).catch(error =>
              console.error('Navigation error:', error)
            );
          }
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  goBack(): void {
    this.router.navigate(['/home']);
  }

  async onSubmit(): Promise<void> {
    if (this.profileForm.invalid || this.isSubmitting) {
      this.markFormGroupTouched();
      return;
    }

    this.isSubmitting = true;
    const {firstName, lastName} = this.profileForm.value;

    // Trim whitespace from inputs
    const trimmedFirstName = firstName.trim();
    const trimmedLastName = lastName.trim();

    try {
      await this.authService.updateUserName(trimmedFirstName, trimmedLastName || undefined);
      this.isSubmitting = false;

      if (this.isEditMode) {
        this.showSuccessMessage('Profile updated successfully!');
        // Navigate back to home page after a short delay
        setTimeout(() => {
          this.router.navigate(['/home']).catch(error =>
            console.error('Navigation error:', error)
          );
        }, 1000);
      } else {
        this.showSuccessMessage('Profile completed successfully!');
        // Navigate to workspace selection after a short delay
        setTimeout(() => {
          this.router.navigate(['/workspaces']).catch(error =>
            console.error('Navigation error:', error)
          );
        }, 1000);
      }
    } catch (error: any) {
      this.isSubmitting = false;
      this.handleError(error);
    }
  }

  private createForm(): FormGroup {
    return this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(100), this.noWhitespaceValidator]],
      lastName: ['', [Validators.maxLength(100)]] // Optional field, no required validator
    });
  }

  private updateFormWithUserData(user: User): void {
    this.profileForm.patchValue({
      firstName: user.first_name || '',
      lastName: user.last_name || ''
    });
  }

  private noWhitespaceValidator(control: any) {
    if (control.value && control.value.trim().length === 0) {
      return {whitespace: true};
    }
    return null;
  }

  private markFormGroupTouched(): void {
    Object.keys(this.profileForm.controls).forEach(key => {
      const control = this.profileForm.get(key);
      if (control) {
        control.markAsTouched();
      }
    });
  }

  private showSuccessMessage(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      panelClass: ['success-snackbar'],
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }

  private handleError(error: any): void {
    console.error('Profile update error:', error);

    let errorMessage = 'Failed to update profile. Please try again.';

    // Extract error message from different error formats
    if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    } else if (typeof error === 'string') {
      errorMessage = error;
    }

    this.snackBar.open(errorMessage, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar'],
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }
}
