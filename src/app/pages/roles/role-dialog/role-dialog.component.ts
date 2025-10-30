import {Component, Inject, OnInit, signal, computed, inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, FormArray, FormControl, Validators, ReactiveFormsModule} from '@angular/forms';
import {MAT_DIALOG_DATA, MatDialogRef, MatDialogModule} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatChipsModule} from '@angular/material/chips';
import {MatExpansionModule} from '@angular/material/expansion';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatDividerModule} from '@angular/material/divider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatStepperModule} from '@angular/material/stepper';

import {RoleService} from '../../../core/services/role.service';
import {
  CustomRole,
  RoleTemplate,
  CreateCustomRoleRequest,
  UpdateCustomRoleRequest,
  PERMISSION_GROUPS,
  PERMISSION_DESCRIPTIONS,
  PermissionGroup
} from '../../../core/models/role.interface';
import {
  WorkspaceRole,
  WorkspacePermission,
  ROLE_DISPLAY_NAMES,
  PERMISSION_DISPLAY_NAMES
} from '../../../core/models/member.interface';

export interface RoleDialogData {
  workspaceId: string;
  mode: 'create' | 'edit' | 'template';
  role?: CustomRole;
  template?: RoleTemplate;
}

@Component({
  selector: 'app-role-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatFormFieldModule,
    MatCheckboxModule,
    MatChipsModule,
    MatExpansionModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatTooltipModule,
    MatStepperModule
  ],
  templateUrl: './role-dialog.component.html',
  styleUrl: './role-dialog.component.scss'
})
export class RoleDialogComponent implements OnInit {
  private roleService = inject(RoleService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  // Signals
  loading = signal(false);
  currentStep = signal(0);
  selectedBaseRole = signal<WorkspaceRole | null>(null);
  selectedPermissions = signal<Set<WorkspacePermission>>(new Set());
  permissionConflicts = signal<string[]>([]);

  // Form
  roleForm: FormGroup;
  permissionGroups: PermissionGroup[] = PERMISSION_GROUPS;

  // Constants
  readonly WorkspaceRole = WorkspaceRole;
  readonly WorkspacePermission = WorkspacePermission;
  readonly ROLE_DISPLAY_NAMES = ROLE_DISPLAY_NAMES;
  readonly PERMISSION_DISPLAY_NAMES = PERMISSION_DISPLAY_NAMES;
  readonly PERMISSION_DESCRIPTIONS = PERMISSION_DESCRIPTIONS;

  // Computed properties
  isEditMode = computed(() => this.data.mode === 'edit');
  isTemplateMode = computed(() => this.data.mode === 'template');
  dialogTitle = computed(() => {
    switch (this.data.mode) {
      case 'edit': return `Edit Role: ${this.data.role?.name}`;
      case 'template': return `Create Role from Template: ${this.data.template?.name}`;
      default: return 'Create Custom Role';
    }
  });

  baseRoleOptions = computed(() => [
    { value: WorkspaceRole.ADMIN, label: ROLE_DISPLAY_NAMES[WorkspaceRole.ADMIN], disabled: false },
    { value: WorkspaceRole.MANAGER, label: ROLE_DISPLAY_NAMES[WorkspaceRole.MANAGER], disabled: false },
    { value: WorkspaceRole.MEMBER, label: ROLE_DISPLAY_NAMES[WorkspaceRole.MEMBER], disabled: false },
    { value: WorkspaceRole.GUEST, label: ROLE_DISPLAY_NAMES[WorkspaceRole.GUEST], disabled: false },
    { value: WorkspaceRole.VIEWER, label: ROLE_DISPLAY_NAMES[WorkspaceRole.VIEWER], disabled: false }
  ]);

  selectedPermissionCount = computed(() => this.selectedPermissions().size);
  
  inheritedPermissions = computed(() => {
    const baseRole = this.selectedBaseRole();
    if (!baseRole) return [];
    return this.getInheritedPermissions(baseRole);
  });

  constructor(
    public dialogRef: MatDialogRef<RoleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RoleDialogData
  ) {
    this.roleForm = this.createForm();
    this.setupFormWatchers();
  }

  ngOnInit(): void {
    if (this.isEditMode() && this.data.role) {
      this.populateForm(this.data.role);
    } else if (this.isTemplateMode() && this.data.template) {
      this.populateFromTemplate(this.data.template);
    }
  }

  private createForm(): FormGroup {
    return this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(200)]],
      baseRole: ['', Validators.required],
      permissions: this.fb.array([]),
      color: ['#2196f3'],
      icon: ['person']
    });
  }

  private setupFormWatchers(): void {
    // Watch base role changes
    this.roleForm.get('baseRole')?.valueChanges.subscribe(baseRole => {
      if (baseRole) {
        this.selectedBaseRole.set(baseRole);
        this.updatePermissionConflicts();
      }
    });

    // Watch permission changes
    this.permissionsFormArray.valueChanges.subscribe(() => {
      this.updateSelectedPermissions();
      this.updatePermissionConflicts();
    });
  }

  private populateForm(role: CustomRole): void {
    this.roleForm.patchValue({
      name: role.name,
      description: role.description,
      baseRole: role.base_role,
      color: role.color || '#2196f3',
      icon: role.icon || 'person'
    });

    this.selectedBaseRole.set(role.base_role);
    this.populatePermissions(role.permissions);
  }

  private populateFromTemplate(template: RoleTemplate): void {
    this.roleForm.patchValue({
      name: template.name,
      description: template.description,
      baseRole: template.base_role,
      color: '#2196f3',
      icon: 'person'
    });

    this.selectedBaseRole.set(template.base_role);
    this.populatePermissions(template.permissions);
  }

  private populatePermissions(permissions: WorkspacePermission[]): void {
    const permissionsArray = this.permissionsFormArray;
    permissionsArray.clear();

    // Group permissions by permission groups
    PERMISSION_GROUPS.forEach(group => {
      group.permissions.forEach(permission => {
        const isSelected = permissions.includes(permission);
        permissionsArray.push(new FormControl(isSelected));
      });
    });

    this.updateSelectedPermissions();
  }

  private updateSelectedPermissions(): void {
    const selected = new Set<WorkspacePermission>();
    let index = 0;

    PERMISSION_GROUPS.forEach(group => {
      group.permissions.forEach(permission => {
        if (this.permissionsFormArray.at(index)?.value) {
          selected.add(permission);
        }
        index++;
      });
    });

    this.selectedPermissions.set(selected);
  }

  private updatePermissionConflicts(): void {
    const conflicts: string[] = [];
    const selected = this.selectedPermissions();
    const baseRole = this.selectedBaseRole();

    if (!baseRole) {
      this.permissionConflicts.set([]);
      return;
    }

    const inherited = this.getInheritedPermissions(baseRole);
    
    // Check for redundant permissions (already inherited)
    selected.forEach(permission => {
      if (inherited.includes(permission)) {
        conflicts.push(`Permission "${PERMISSION_DISPLAY_NAMES[permission]}" is already inherited from ${ROLE_DISPLAY_NAMES[baseRole]}`);
      }
    });

    // Check for dangerous combinations
    const dangerous = [
      {
        perms: [WorkspacePermission.WORKSPACE_DELETE, WorkspacePermission.MEMBER_DELETE],
        message: 'Combining workspace deletion with member deletion can be dangerous'
      },
      {
        perms: [WorkspacePermission.WORKSPACE_MANAGE, WorkspacePermission.MEMBER_DELETE],
        message: 'Combining workspace management with member deletion requires careful consideration'
      }
    ];

    dangerous.forEach(check => {
      if (check.perms.every(perm => selected.has(perm))) {
        conflicts.push(check.message);
      }
    });

    this.permissionConflicts.set(conflicts);
  }

  get permissionsFormArray(): FormArray {
    return this.roleForm.get('permissions') as FormArray;
  }

  getPermissionFormControl(groupIndex: number, permissionIndex: number): FormControl {
    let flatIndex = 0;
    
    // Calculate flat index based on group and permission index
    for (let i = 0; i < groupIndex; i++) {
      const group = PERMISSION_GROUPS[i];
      if (group) {
        flatIndex += group.permissions.length;
      }
    }
    flatIndex += permissionIndex;
    
    return this.permissionsFormArray.at(flatIndex) as FormControl;
  }

  getInheritedPermissions(role: WorkspaceRole): WorkspacePermission[] {
    const rolePermissions: { [key in WorkspaceRole]: WorkspacePermission[] } = {
      [WorkspaceRole.OWNER]: Object.values(WorkspacePermission),
      [WorkspaceRole.ADMIN]: [
        WorkspacePermission.WORKSPACE_MANAGE,
        WorkspacePermission.MEMBER_VIEW,
        WorkspacePermission.MEMBER_INVITE,
        WorkspacePermission.MEMBER_MANAGE,
        WorkspacePermission.MEMBER_DELETE,
        WorkspacePermission.VIEW_TEAMS,
        WorkspacePermission.MANAGE_TEAMS,
        WorkspacePermission.MANAGE_TEAM_MEMBERS,
        WorkspacePermission.MODULE_MANAGE,
        WorkspacePermission.SETTINGS_MANAGE,
        WorkspacePermission.DATA_EXPORT,
        WorkspacePermission.ANALYTICS_VIEW
      ],
      [WorkspaceRole.MANAGER]: [
        WorkspacePermission.MEMBER_VIEW,
        WorkspacePermission.MEMBER_INVITE,
        WorkspacePermission.MEMBER_MANAGE,
        WorkspacePermission.VIEW_TEAMS,
        WorkspacePermission.MANAGE_TEAMS,
        WorkspacePermission.MANAGE_TEAM_MEMBERS,
        WorkspacePermission.ANALYTICS_VIEW
      ],
      [WorkspaceRole.MEMBER]: [
        WorkspacePermission.MEMBER_VIEW,
        WorkspacePermission.VIEW_TEAMS
      ],
      [WorkspaceRole.GUEST]: [
        WorkspacePermission.MEMBER_VIEW,
        WorkspacePermission.VIEW_TEAMS
      ],
      [WorkspaceRole.VIEWER]: [
        WorkspacePermission.MEMBER_VIEW,
        WorkspacePermission.VIEW_TEAMS
      ]
    };
    
    return rolePermissions[role] || [];
  }

  isPermissionInherited(permission: WorkspacePermission): boolean {
    const baseRole = this.selectedBaseRole();
    if (!baseRole) return false;
    return this.inheritedPermissions().includes(permission);
  }

  isPermissionSelected(permission: WorkspacePermission): boolean {
    return this.selectedPermissions().has(permission);
  }

  toggleAllPermissionsInGroup(groupIndex: number, checked: boolean): void {
    const group = PERMISSION_GROUPS[groupIndex];
    if (!group) return;
    
    let flatIndex = 0;
    
    // Calculate starting flat index for this group
    for (let i = 0; i < groupIndex; i++) {
      const currentGroup = PERMISSION_GROUPS[i];
      if (currentGroup) {
        flatIndex += currentGroup.permissions.length;
      }
    }
    
    // Toggle all permissions in the group
    group.permissions.forEach((_, permissionIndex) => {
      this.permissionsFormArray.at(flatIndex + permissionIndex)?.setValue(checked);
    });
  }

  isAllPermissionsInGroupSelected(groupIndex: number): boolean {
    const group = PERMISSION_GROUPS[groupIndex];
    if (!group) return false;
    
    let flatIndex = 0;
    
    // Calculate starting flat index for this group
    for (let i = 0; i < groupIndex; i++) {
      const currentGroup = PERMISSION_GROUPS[i];
      if (currentGroup) {
        flatIndex += currentGroup.permissions.length;
      }
    }
    
    return group.permissions.every((_, permissionIndex) => 
      this.permissionsFormArray.at(flatIndex + permissionIndex)?.value
    );
  }

  isSomePermissionsInGroupSelected(groupIndex: number): boolean {
    const group = PERMISSION_GROUPS[groupIndex];
    if (!group) return false;
    
    let flatIndex = 0;
    
    // Calculate starting flat index for this group
    for (let i = 0; i < groupIndex; i++) {
      const currentGroup = PERMISSION_GROUPS[i];
      if (currentGroup) {
        flatIndex += currentGroup.permissions.length;
      }
    }
    
    const selectedCount = group.permissions.filter((_, permissionIndex) => 
      this.permissionsFormArray.at(flatIndex + permissionIndex)?.value
    ).length;
    
    return selectedCount > 0 && selectedCount < group.permissions.length;
  }

  getPermissionGroupSummary(groupIndex: number): string {
    const group = PERMISSION_GROUPS[groupIndex];
    if (!group) return 'Invalid group';
    
    let selectedCount = 0;
    let inheritedCount = 0;
    let flatIndex = 0;
    
    // Calculate starting flat index for this group
    for (let i = 0; i < groupIndex; i++) {
      const currentGroup = PERMISSION_GROUPS[i];
      if (currentGroup) {
        flatIndex += currentGroup.permissions.length;
      }
    }
    
    group.permissions.forEach((permission, permissionIndex) => {
      if (this.permissionsFormArray.at(flatIndex + permissionIndex)?.value) {
        selectedCount++;
      }
      if (this.isPermissionInherited(permission)) {
        inheritedCount++;
      }
    });
    
    const totalCount = group.permissions.length;
    const parts: string[] = [];
    
    if (selectedCount > 0) {
      parts.push(`${selectedCount} selected`);
    }
    if (inheritedCount > 0) {
      parts.push(`${inheritedCount} inherited`);
    }
    if (parts.length === 0) {
      parts.push('None selected');
    }
    
    return `${parts.join(', ')} of ${totalCount}`;
  }

  async onSubmit(): Promise<void> {
    if (this.roleForm.invalid) {
      this.markFormGroupTouched(this.roleForm);
      return;
    }

    this.loading.set(true);

    try {
      const formValue = this.roleForm.value;
      const selectedPermissions = Array.from(this.selectedPermissions());

      if (this.isEditMode() && this.data.role) {
        const updateRequest: UpdateCustomRoleRequest = {
          name: formValue.name,
          description: formValue.description,
          permissions: selectedPermissions,
          color: formValue.color,
          icon: formValue.icon
        };

        await this.roleService.updateCustomRole(this.data.workspaceId, this.data.role.id, updateRequest);
        this.showSuccess(`Role "${formValue.name}" updated successfully`);
      } else {
        const createRequest: CreateCustomRoleRequest = {
          name: formValue.name,
          description: formValue.description,
          base_role: formValue.baseRole,
          permissions: selectedPermissions,
          color: formValue.color,
          icon: formValue.icon
        };

        await this.roleService.createCustomRole(this.data.workspaceId, createRequest);
        this.showSuccess(`Role "${formValue.name}" created successfully`);
      }

      this.dialogRef.close(true);
    } catch (error: any) {
      this.showError(error.message || 'Failed to save role');
    } finally {
      this.loading.set(false);
    }
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(field => {
      const control = formGroup.get(field);
      control?.markAsTouched({ onlySelf: true });

      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
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

  // Utility methods for template
  getFieldError(fieldName: string): string {
    const field = this.roleForm.get(fieldName);
    if (field?.errors && field.touched) {
      if (field.errors['required']) return `${fieldName} is required`;
      if (field.errors['minlength']) return `${fieldName} is too short`;
      if (field.errors['maxlength']) return `${fieldName} is too long`;
    }
    return '';
  }

  hasFieldError(fieldName: string): boolean {
    const field = this.roleForm.get(fieldName);
    return !!(field?.errors && field.touched);
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

  getPermissionGroupIcon(permission: WorkspacePermission): string {
    const group = PERMISSION_GROUPS.find(g => g.permissions.includes(permission));
    return group?.icon || 'security';
  }

  // For template access to Array constructor
  Array = Array;
}