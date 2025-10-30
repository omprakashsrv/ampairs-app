import {Component, Inject, OnInit, signal, computed} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule} from '@angular/forms';
import {MatDialogRef, MAT_DIALOG_DATA, MatDialogModule} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatDividerModule} from '@angular/material/divider';
import {MatTabsModule} from '@angular/material/tabs';
import {MatChipsModule} from '@angular/material/chips';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatStepperModule} from '@angular/material/stepper';

import {MemberService} from '../../../core/services/member.service';
import {
  WorkspaceRole,
  CreateInvitationRequest,
  BulkInvitationRequest,
  ROLE_DISPLAY_NAMES
} from '../../../core/models/member.interface';

export interface InviteDialogData {
  workspaceId: string;
  workspaceName: string;
}

@Component({
  selector: 'app-member-invite-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatInputModule,
    MatSelectModule,
    MatFormFieldModule,
    MatIconModule,
    MatDividerModule,
    MatTabsModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatStepperModule
  ],
  templateUrl: './member-invite-dialog.component.html',
  styleUrl: './member-invite-dialog.component.scss'
})
export class MemberInviteDialogComponent implements OnInit {
  loading = signal(false);
  currentTab = signal(0);
  singleFormValid = signal(false);
  
  // Forms
  singleInviteForm: FormGroup;
  bulkInviteForm: FormGroup;
  
  // Constants for template
  readonly WorkspaceRole = WorkspaceRole;
  readonly ROLE_DISPLAY_NAMES = ROLE_DISPLAY_NAMES;
  readonly availableRoles = [
    WorkspaceRole.ADMIN,
    WorkspaceRole.MANAGER, 
    WorkspaceRole.MEMBER,
    WorkspaceRole.GUEST,
    WorkspaceRole.VIEWER
  ];

  // Computed properties
  canSubmitSingle = computed(() => {
    return this.singleFormValid() && !this.loading();
  });

  canSubmitBulk = computed(() => {
    return this.bulkInviteForm?.valid && this.bulkEmails().length > 0 && !this.loading();
  });

  bulkEmails = computed(() => {
    const control = this.bulkInviteForm?.get('emails');
    return control?.value?.filter((email: string) => email.trim()) || [];
  });

  constructor(
    private dialogRef: MatDialogRef<MemberInviteDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: InviteDialogData,
    private fb: FormBuilder,
    private memberService: MemberService,
    private snackBar: MatSnackBar
  ) {
    this.singleInviteForm = this.createSingleInviteForm();
    this.bulkInviteForm = this.createBulkInviteForm();
    
    // Track form validity changes
    this.singleInviteForm.statusChanges.subscribe(() => {
      this.singleFormValid.set(this.singleInviteForm.valid);
    });
    
    // Set initial validity
    this.singleFormValid.set(this.singleInviteForm.valid);
  }

  ngOnInit(): void {
    // Focus on email field after dialog opens
    setTimeout(() => {
      const phoneInput = document.querySelector('input[formControlName="phone"]') as HTMLInputElement;
      phoneInput?.focus();
    }, 100);
  }

  private createSingleInviteForm(): FormGroup {
    return this.fb.group({
      phone: ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
      role: [WorkspaceRole.MEMBER, Validators.required],
      custom_message: [''],
      expires_in_days: [7, [Validators.required, Validators.min(1), Validators.max(30)]]
    });
  }

  private createBulkInviteForm(): FormGroup {
    return this.fb.group({
      emails: this.fb.array([this.fb.control('', [Validators.email])]),
      role: [WorkspaceRole.MEMBER, Validators.required],
      default_message: [''],
      expires_in_days: [7, [Validators.required, Validators.min(1), Validators.max(30)]]
    });
  }

  get bulkEmailControls() {
    return (this.bulkInviteForm.get('emails') as FormArray).controls;
  }

  getCurrentRole(): WorkspaceRole {
    return this.singleInviteForm.get('role')?.value as WorkspaceRole || WorkspaceRole.MEMBER;
  }

  getBulkRole(): WorkspaceRole {
    return this.bulkInviteForm.get('role')?.value as WorkspaceRole || WorkspaceRole.MEMBER;
  }

  // ========== SINGLE INVITE METHODS ==========

  async sendSingleInvite(): Promise<void> {
    if (!this.singleInviteForm.valid) {
      this.markFormGroupTouched(this.singleInviteForm);
      return;
    }

    this.loading.set(true);
    
    try {
      const formValue = this.singleInviteForm.value;
      const invitationData: CreateInvitationRequest = {
        phone: formValue.phone.trim(),
        country_code: 91,
        invited_role: formValue.role,
        message: formValue.custom_message?.trim() || undefined,
        expires_in_days: formValue.expires_in_days,
        send_notification: true
      };

      await this.memberService.createInvitation(this.data.workspaceId, invitationData);
      
      this.showSuccess(`Invitation sent to +91${formValue.phone}`);
      this.dialogRef.close(true);
    } catch (error: any) {
      this.showError(error.message || 'Failed to send invitation');
    } finally {
      this.loading.set(false);
    }
  }

  // ========== BULK INVITE METHODS ==========

  addEmailField(): void {
    const emailsArray = this.bulkInviteForm.get('emails') as FormArray;
    emailsArray.push(this.fb.control('', [Validators.email]));
  }

  removeEmailField(index: number): void {
    const emailsArray = this.bulkInviteForm.get('emails') as FormArray;
    if (emailsArray.length > 1) {
      emailsArray.removeAt(index);
    }
  }

  parseEmailList(event: Event): void {
    const target = event.target as HTMLTextAreaElement;
    const text = target.value;
    
    if (!text.trim()) return;

    // Parse emails from various formats (comma, semicolon, newline separated)
    const emails = text
      .split(/[,;\n\r]+/)
      .map(email => email.trim())
      .filter(email => email && this.isValidEmail(email));

    if (emails.length > 0) {
      // Clear existing fields
      const emailsArray = this.bulkInviteForm.get('emails') as FormArray;
      emailsArray.clear();

      // Add parsed emails
      emails.forEach(email => {
        emailsArray.push(this.fb.control(email, [Validators.email]));
      });

      // Clear textarea
      target.value = '';
      
      this.showInfo(`Added ${emails.length} email address(es)`);
    }
  }

  async sendBulkInvites(): Promise<void> {
    if (!this.bulkInviteForm.valid) {
      this.markFormGroupTouched(this.bulkInviteForm);
      return;
    }

    const validEmails = this.bulkEmails();
    if (validEmails.length === 0) {
      this.showError('Please add at least one valid email address');
      return;
    }

    this.loading.set(true);

    try {
      const formValue = this.bulkInviteForm.value;
      const bulkData: BulkInvitationRequest = {
        invitations: validEmails.map((email: string) => ({
          email: email.trim(),
          invited_role: formValue.role,
          message: formValue.default_message?.trim() || undefined,
          expires_in_days: formValue.expires_in_days,
          send_notification: true
        })),
        default_message: formValue.default_message?.trim() || undefined
      };

      await this.memberService.createBulkInvitations(this.data.workspaceId, bulkData);
      
      this.showSuccess(`${validEmails.length} invitation(s) sent successfully`);
      this.dialogRef.close(true);
    } catch (error: any) {
      this.showError(error.message || 'Failed to send invitations');
    } finally {
      this.loading.set(false);
    }
  }

  // ========== HELPER METHODS ==========

  onTabChange(tabIndex: number): void {
    this.currentTab.set(tabIndex);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();

      if (control instanceof FormArray) {
        control.controls.forEach(c => {
          c.markAsTouched();
        });
      }
    });
  }

  getRoleIcon(role: WorkspaceRole): string {
    switch (role) {
      case WorkspaceRole.OWNER: return 'crown';
      case WorkspaceRole.ADMIN: return 'admin_panel_settings';
      case WorkspaceRole.MANAGER: return 'supervisor_account';
      case WorkspaceRole.MEMBER: return 'person';
      case WorkspaceRole.GUEST: return 'person_outline';
      case WorkspaceRole.VIEWER: return 'visibility';
      default: return 'person';
    }
  }

  getRoleDescription(role: WorkspaceRole): string {
    switch (role) {
      case WorkspaceRole.ADMIN:
        return 'Full administrative access to manage workspace, members, and settings';
      case WorkspaceRole.MANAGER:
        return 'Can manage projects, teams, and invite new members';
      case WorkspaceRole.MEMBER:
        return 'Standard access to workspace features and collaboration';
      case WorkspaceRole.GUEST:
        return 'Limited access for external collaborators';
      case WorkspaceRole.VIEWER:
        return 'Read-only access to workspace content';
      default:
        return '';
    }
  }

  getRoleDisplayName(role: WorkspaceRole): string {
    return ROLE_DISPLAY_NAMES[role] || role;
  }

  // ========== NOTIFICATION METHODS ==========

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      panelClass: ['success-snackbar']
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }

  private showInfo(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      panelClass: ['info-snackbar']
    });
  }
}