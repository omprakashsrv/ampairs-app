import {Component, inject, Inject, OnInit, signal} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {MAT_DIALOG_DATA, MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {CommonModule} from '@angular/common';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSnackBar} from '@angular/material/snack-bar';
import {WorkspaceListItem, WorkspaceService} from '../../../core/services/workspace.service';

@Component({
  selector: 'app-workspace-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './workspace-edit-dialog.component.html',
  styleUrl: './workspace-edit-dialog.component.scss'
})
export class WorkspaceEditDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  private workspaceService = inject(WorkspaceService);
  private snackBar = inject(MatSnackBar);
  private dialogRef = inject(MatDialogRef<WorkspaceEditDialogComponent>);

  workspaceForm: FormGroup;
  isLoading = signal(false);

  workspaceTypes = [
    {value: 'BUSINESS', label: 'Business', description: 'For companies and organizations', icon: 'business'},
    {value: 'PERSONAL', label: 'Personal', description: 'For individual use', icon: 'person'},
    {value: 'TEAM', label: 'Team', description: 'For small teams and groups', icon: 'group'},
    {value: 'ENTERPRISE', label: 'Enterprise', description: 'For large organizations', icon: 'corporate_fare'}
  ];

  timezones = [
    {value: 'UTC', label: 'UTC (Coordinated Universal Time)'},
    {value: 'America/New_York', label: 'Eastern Time (ET)'},
    {value: 'America/Chicago', label: 'Central Time (CT)'},
    {value: 'America/Denver', label: 'Mountain Time (MT)'},
    {value: 'America/Los_Angeles', label: 'Pacific Time (PT)'},
    {value: 'Europe/London', label: 'London (GMT)'},
    {value: 'Europe/Paris', label: 'Paris (CET)'},
    {value: 'Asia/Tokyo', label: 'Tokyo (JST)'},
    {value: 'Asia/Shanghai', label: 'Shanghai (CST)'},
    {value: 'Asia/Kolkata', label: 'India (IST)'},
    {value: 'Australia/Sydney', label: 'Sydney (AEST)'}
  ];

  languages = [
    {value: 'en', label: 'English'},
    {value: 'es', label: 'Spanish'},
    {value: 'fr', label: 'French'},
    {value: 'de', label: 'German'},
    {value: 'it', label: 'Italian'},
    {value: 'pt', label: 'Portuguese'},
    {value: 'ru', label: 'Russian'},
    {value: 'ja', label: 'Japanese'},
    {value: 'ko', label: 'Korean'},
    {value: 'zh', label: 'Chinese'},
    {value: 'hi', label: 'Hindi'},
    {value: 'ar', label: 'Arabic'}
  ];

  constructor(@Inject(MAT_DIALOG_DATA) public data: { workspace: WorkspaceListItem }) {
    this.workspaceForm = this.fb.group({
      name: [data.workspace.name, [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: [data.workspace.description || '', [Validators.maxLength(500)]],
      workspace_type: [data.workspace.workspace_type, [Validators.required]],
      // Business Details
      address_line1: ['', [Validators.maxLength(255)]],
      address_line2: ['', [Validators.maxLength(255)]],
      city: ['', [Validators.maxLength(100)]],
      state: ['', [Validators.maxLength(100)]],
      postal_code: ['', [Validators.maxLength(20)]],
      country: ['', [Validators.maxLength(100)]],
      phone: ['', [Validators.pattern(/^[+]?[0-9\s\-()]+$/), Validators.maxLength(20)]],
      email: ['', [Validators.email, Validators.maxLength(255)]],
      website: ['', [Validators.maxLength(255)]],
      registration_number: ['', [Validators.maxLength(100)]],
      business_hours_start: ['', [Validators.pattern(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/)]],
      business_hours_end: ['', [Validators.pattern(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/)]]
    });
  }

  ngOnInit(): void {
    // Load full workspace details if needed
    this.loadWorkspaceDetails();
  }

  private loadWorkspaceDetails(): void {
    this.workspaceService.getWorkspaceById(this.data.workspace.id).subscribe({
      next: (fullWorkspace) => {
        // Update form with additional details that might not be in WorkspaceListItem
        this.workspaceForm.patchValue({
          // Business Details (need to access these from fullWorkspace)
          address_line1: (fullWorkspace as any).address_line1 || '',
          address_line2: (fullWorkspace as any).address_line2 || '',
          city: (fullWorkspace as any).city || '',
          state: (fullWorkspace as any).state || '',
          postal_code: (fullWorkspace as any).postal_code || '',
          country: (fullWorkspace as any).country || '',
          phone: (fullWorkspace as any).phone || '',
          email: (fullWorkspace as any).email || '',
          website: (fullWorkspace as any).website || '',
          registration_number: (fullWorkspace as any).registration_number || '',
          business_hours_start: (fullWorkspace as any).business_hours_start || '',
          business_hours_end: (fullWorkspace as any).business_hours_end || ''
        });
      },
      error: (error) => {
        console.error('Failed to load full workspace details:', error);
        // Continue with basic details if full details fail to load
      }
    });
  }

  async onSave(): Promise<void> {
    if (this.workspaceForm.valid && !this.isLoading()) {
      this.isLoading.set(true);

      const updateData = {
        name: this.workspaceForm.get('name')?.value.trim(),
        description: this.workspaceForm.get('description')?.value?.trim() || undefined,
        workspace_type: this.workspaceForm.get('workspace_type')?.value,
        // Business Details
        address_line1: this.workspaceForm.get('address_line1')?.value?.trim() || undefined,
        address_line2: this.workspaceForm.get('address_line2')?.value?.trim() || undefined,
        city: this.workspaceForm.get('city')?.value?.trim() || undefined,
        state: this.workspaceForm.get('state')?.value?.trim() || undefined,
        postal_code: this.workspaceForm.get('postal_code')?.value?.trim() || undefined,
        country: this.workspaceForm.get('country')?.value?.trim() || undefined,
        phone: this.workspaceForm.get('phone')?.value?.trim() || undefined,
        email: this.workspaceForm.get('email')?.value?.trim() || undefined,
        website: this.workspaceForm.get('website')?.value?.trim() || undefined,
        registration_number: this.workspaceForm.get('registration_number')?.value?.trim() || undefined,
        business_hours_start: this.workspaceForm.get('business_hours_start')?.value?.trim() || undefined,
        business_hours_end: this.workspaceForm.get('business_hours_end')?.value?.trim() || undefined
      };

      try {
        const updatedWorkspace = await this.workspaceService.updateWorkspace(this.data.workspace.id, updateData).toPromise();
        
        this.snackBar.open('Workspace updated successfully!', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });

        this.dialogRef.close(updatedWorkspace);
      } catch (error: any) {
        this.snackBar.open(error.message || 'Failed to update workspace. Please try again.', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      } finally {
        this.isLoading.set(false);
      }
    } else {
      this.markFormGroupTouched();
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  get nameField() {
    return this.workspaceForm.get('name');
  }

  get descriptionField() {
    return this.workspaceForm.get('description');
  }

  get workspaceTypeField() {
    return this.workspaceForm.get('workspace_type');
  }

  // Business Detail Field Getters
  get phoneField() {
    return this.workspaceForm.get('phone');
  }

  get emailField() {
    return this.workspaceForm.get('email');
  }

  get websiteField() {
    return this.workspaceForm.get('website');
  }

  get registrationNumberField() {
    return this.workspaceForm.get('registration_number');
  }

  get addressLine1Field() {
    return this.workspaceForm.get('address_line1');
  }

  get addressLine2Field() {
    return this.workspaceForm.get('address_line2');
  }

  get cityField() {
    return this.workspaceForm.get('city');
  }

  get stateField() {
    return this.workspaceForm.get('state');
  }

  get postalCodeField() {
    return this.workspaceForm.get('postal_code');
  }

  get countryField() {
    return this.workspaceForm.get('country');
  }

  get businessHoursStartField() {
    return this.workspaceForm.get('business_hours_start');
  }

  get businessHoursEndField() {
    return this.workspaceForm.get('business_hours_end');
  }

  private markFormGroupTouched(): void {
    Object.keys(this.workspaceForm.controls).forEach(key => {
      const control = this.workspaceForm.get(key);
      control?.markAsTouched();
    });
  }
}