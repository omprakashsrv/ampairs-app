import {Component, Inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {MAT_DIALOG_DATA, MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatIconModule} from '@angular/material/icon';
import {MatChipsModule} from '@angular/material/chips';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatCheckboxModule} from '@angular/material/checkbox';

import {MemberService} from '../../../core/services/member.service';
import {
  PERMISSION_DISPLAY_NAMES,
  WorkspaceMemberListItem,
  WorkspacePermission,
  WorkspacePermissionResponse,
  WorkspaceRole,
  WorkspaceRoleResponse
} from '../../../core/models/member.interface';
import {WorkspaceListItem} from '../../../core/services/workspace.service';

export interface MemberEditDialogData {
  member: WorkspaceMemberListItem;
  currentWorkspace: WorkspaceListItem;
}

@Component({
  selector: 'app-member-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatChipsModule,
    MatSnackBarModule,
    MatCheckboxModule
  ],
  templateUrl: './member-edit-dialog.component.html',
  styleUrl: './member-edit-dialog.component.scss'
})
export class MemberEditDialogComponent implements OnInit {
  editForm: FormGroup;
  isLoading = signal(false);
  isLoadingRoles = signal(true);
  isLoadingPermissions = signal(true);
  
  // Dynamic data from backend
  availableRolesData = signal<WorkspaceRoleResponse[]>([]);
  availablePermissionsData = signal<WorkspacePermissionResponse[]>([]);
  
  // Enums for template
  WorkspaceRole = WorkspaceRole;
  WorkspacePermission = WorkspacePermission;

  constructor(
    private fb: FormBuilder,
    private memberService: MemberService,
    private snackBar: MatSnackBar,
    private dialogRef: MatDialogRef<MemberEditDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: MemberEditDialogData
  ) {
    this.editForm = this.fb.group({
      role: [data.member.role, Validators.required],
      is_active: [data.member.is_active, Validators.required],
      permissions: [data.member.permissions || []]
    });
  }

  async ngOnInit(): Promise<void> {
    // Load roles and permissions from backend
    await this.loadRolesAndPermissions();
    
    // Ensure the form has the correct initial values
    this.editForm.patchValue({
      role: this.data.member.role,
      is_active: this.data.member.is_active,
      permissions: this.data.member.permissions || []
    });
  }

  private async loadRolesAndPermissions(): Promise<void> {
    try {
      // Load both roles and permissions in parallel
      const [roles, permissions] = await Promise.all([
        this.memberService.getWorkspaceRoles(this.data.currentWorkspace.id),
        this.memberService.getWorkspacePermissions(this.data.currentWorkspace.id)
      ]);

      this.availableRolesData.set(roles);
      this.availablePermissionsData.set(permissions);
    } catch (error: any) {
      console.error('Failed to load roles and permissions:', error);
      // Show error but don't prevent dialog from opening
      // You could show a snackbar error message here if desired
    } finally {
      this.isLoadingRoles.set(false);
      this.isLoadingPermissions.set(false);
    }
  }

  get availableRoles(): WorkspaceRoleResponse[] {
    return this.availableRolesData();
  }

  get availableStatuses(): { value: boolean; label: string }[] {
    return [
      { value: true, label: 'Active' },
      { value: false, label: 'Inactive' }
    ];
  }

  get availablePermissions(): WorkspacePermissionResponse[] {
    return this.availablePermissionsData();
  }

  isPermissionSelected(permission: WorkspacePermissionResponse): boolean {
    const currentPermissions = this.editForm.get('permissions')?.value || [];
    return currentPermissions.includes(permission.name);
  }

  togglePermission(permission: WorkspacePermissionResponse): void {
    const currentPermissions = this.editForm.get('permissions')?.value || [];
    let updatedPermissions: string[];

    if (currentPermissions.includes(permission.name)) {
      updatedPermissions = currentPermissions.filter((p: string) => p !== permission.name);
    } else {
      updatedPermissions = [...currentPermissions, permission.name];
    }

    this.editForm.patchValue({ permissions: updatedPermissions });
  }

  async onSubmit(): Promise<void> {
    if (this.editForm.invalid) {
      return;
    }

    this.isLoading.set(true);

    try {
      const formValues = this.editForm.value;
      
      const updateRequest = {
        role: formValues.role,
        is_active: formValues.is_active,
        permissions: formValues.permissions
      };

      await this.memberService.updateMember(
        this.data.currentWorkspace.id,
        this.data.member.id,
        updateRequest
      );

      this.showSuccess('Member updated successfully');
      this.dialogRef.close(true);
    } catch (error: any) {
      this.showError(error.message || 'Failed to update member');
    } finally {
      this.isLoading.set(false);
    }
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  getRoleDisplayName(roleName: string): string {
    const role = this.availableRolesData().find(r => r.name === roleName);
    return role ? role.display_name : roleName;
  }

  getPermissionDisplayName(permission: WorkspacePermission): string {
    return PERMISSION_DISPLAY_NAMES[permission] || permission;
  }

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
}