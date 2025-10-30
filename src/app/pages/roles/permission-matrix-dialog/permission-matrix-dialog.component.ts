import {Component, Inject, OnInit, signal, computed, inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MAT_DIALOG_DATA, MatDialogRef, MatDialogModule} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatTableModule} from '@angular/material/table';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatChipsModule} from '@angular/material/chips';
import {MatCardModule} from '@angular/material/card';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatTabsModule} from '@angular/material/tabs';
import {MatSelectModule} from '@angular/material/select';
import {MatFormFieldModule} from '@angular/material/form-field';
import {ReactiveFormsModule, FormControl} from '@angular/forms';

import {RoleService} from '../../../core/services/role.service';
import {
  PermissionMatrix,
  PermissionAccess,
  PERMISSION_GROUPS,
  PermissionGroup
} from '../../../core/models/role.interface';
import {
  WorkspaceRole,
  WorkspacePermission,
  ROLE_DISPLAY_NAMES,
  PERMISSION_DISPLAY_NAMES
} from '../../../core/models/member.interface';

export interface PermissionMatrixDialogData {
  workspaceId: string;
}

@Component({
  selector: 'app-permission-matrix-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatChipsModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    MatSelectModule,
    MatFormFieldModule
  ],
  templateUrl: './permission-matrix-dialog.component.html',
  styleUrl: './permission-matrix-dialog.component.scss'
})
export class PermissionMatrixDialogComponent implements OnInit {
  private roleService = inject(RoleService);

  // Signals
  loading = signal(true);
  error = signal<string | null>(null);
  matrix = signal<PermissionMatrix | null>(null);
  selectedView = signal<'matrix' | 'inheritance' | 'conflicts'>('matrix');
  selectedGroup = new FormControl('all');
  selectedRole = new FormControl('all');

  // Constants
  readonly WorkspaceRole = WorkspaceRole;
  readonly WorkspacePermission = WorkspacePermission;
  readonly PermissionAccess = PermissionAccess;
  readonly ROLE_DISPLAY_NAMES = ROLE_DISPLAY_NAMES;
  readonly PERMISSION_DISPLAY_NAMES = PERMISSION_DISPLAY_NAMES;
  readonly PERMISSION_GROUPS = PERMISSION_GROUPS;

  // Computed properties
  allRoles = computed(() => Object.values(WorkspaceRole));
  
  filteredPermissionGroups = computed(() => {
    const selectedGroupId = this.selectedGroup.value;
    if (!selectedGroupId || selectedGroupId === 'all') {
      return PERMISSION_GROUPS;
    }
    return PERMISSION_GROUPS.filter(group => group.id === selectedGroupId);
  });

  filteredRoles = computed(() => {
    const selectedRoleValue = this.selectedRole.value;
    if (!selectedRoleValue || selectedRoleValue === 'all') {
      return this.allRoles();
    }
    return this.allRoles().filter(role => role === selectedRoleValue);
  });

  matrixData = computed(() => {
    const matrixValue = this.matrix();
    if (!matrixValue) return null;

    const filteredGroups = this.filteredPermissionGroups();
    const filteredRoles = this.filteredRoles();

    return {
      roles: filteredRoles,
      permissionGroups: filteredGroups,
      matrix: matrixValue.matrix
    };
  });

  inheritanceData = computed(() => {
    const matrixValue = this.matrix();
    if (!matrixValue) return [];

    return matrixValue.inheritance || [];
  });

  conflictData = computed(() => {
    const matrixValue = this.matrix();
    if (!matrixValue) return [];

    // Calculate conflicts based on permission overlaps and dangerous combinations
    const conflicts: Array<{
      type: string;
      description: string;
      severity: 'low' | 'medium' | 'high';
      roles: WorkspaceRole[];
      permissions: WorkspacePermission[];
    }> = [];

    // Check for dangerous permission combinations
    const dangerousCombinations = [
      {
        permissions: [WorkspacePermission.WORKSPACE_DELETE, WorkspacePermission.MEMBER_DELETE],
        description: 'Workspace deletion combined with member deletion',
        severity: 'high' as const
      },
      {
        permissions: [WorkspacePermission.WORKSPACE_MANAGE, WorkspacePermission.MEMBER_DELETE],
        description: 'Workspace management combined with member deletion',
        severity: 'medium' as const
      }
    ];

    dangerousCombinations.forEach(combo => {
      this.allRoles().forEach(role => {
        const hasAllPermissions = combo.permissions.every(perm => 
          matrixValue.matrix[role]?.[perm] === PermissionAccess.GRANTED ||
          matrixValue.matrix[role]?.[perm] === PermissionAccess.INHERITED
        );

        if (hasAllPermissions) {
          conflicts.push({
            type: 'dangerous_combination',
            description: `${ROLE_DISPLAY_NAMES[role]} has ${combo.description}`,
            severity: combo.severity,
            roles: [role],
            permissions: combo.permissions
          });
        }
      });
    });

    return conflicts;
  });

  constructor(
    public dialogRef: MatDialogRef<PermissionMatrixDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PermissionMatrixDialogData
  ) {}

  async ngOnInit(): Promise<void> {
    await this.loadMatrix();
  }

  async loadMatrix(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      await this.roleService.getPermissionMatrix(this.data.workspaceId);
      const matrixValue = this.roleService.permissionMatrix();
      this.matrix.set(matrixValue);
    } catch (error: any) {
      this.error.set(error.message || 'Failed to load permission matrix');
    } finally {
      this.loading.set(false);
    }
  }

  getPermissionAccess(role: WorkspaceRole, permission: WorkspacePermission): PermissionAccess {
    const matrixValue = this.matrix();
    if (!matrixValue) return PermissionAccess.DENIED;
    
    return matrixValue.matrix[role]?.[permission] || PermissionAccess.DENIED;
  }

  getAccessIcon(access: PermissionAccess): string {
    switch (access) {
      case PermissionAccess.GRANTED: return 'check_circle';
      case PermissionAccess.INHERITED: return 'trending_up';
      case PermissionAccess.CONDITIONAL: return 'help';
      case PermissionAccess.DENIED: return 'block';
      default: return 'help';
    }
  }

  getAccessColor(access: PermissionAccess): string {
    switch (access) {
      case PermissionAccess.GRANTED: return 'success';
      case PermissionAccess.INHERITED: return 'primary';
      case PermissionAccess.CONDITIONAL: return 'warning';
      case PermissionAccess.DENIED: return 'error';
      default: return 'default';
    }
  }

  getAccessLabel(access: PermissionAccess): string {
    switch (access) {
      case PermissionAccess.GRANTED: return 'Directly Granted';
      case PermissionAccess.INHERITED: return 'Inherited from Role';
      case PermissionAccess.CONDITIONAL: return 'Conditional Access';
      case PermissionAccess.DENIED: return 'Access Denied';
      default: return 'Unknown';
    }
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

  getRoleColor(role: WorkspaceRole): string {
    switch (role) {
      case WorkspaceRole.OWNER: return '#f50057';
      case WorkspaceRole.ADMIN: return '#ff5722';
      case WorkspaceRole.MANAGER: return '#ff9800';
      case WorkspaceRole.MEMBER: return '#2196f3';
      case WorkspaceRole.GUEST: return '#9c27b0';
      case WorkspaceRole.VIEWER: return '#607d8b';
      default: return '#2196f3';
    }
  }

  getPermissionGroupIcon(permission: WorkspacePermission): string {
    const group = PERMISSION_GROUPS.find(g => g.permissions.includes(permission));
    return group?.icon || 'security';
  }

  getSeverityIcon(severity: 'low' | 'medium' | 'high'): string {
    switch (severity) {
      case 'high': return 'error';
      case 'medium': return 'warning';
      case 'low': return 'info';
      default: return 'help';
    }
  }

  getSeverityColor(severity: 'low' | 'medium' | 'high'): string {
    switch (severity) {
      case 'high': return 'error';
      case 'medium': return 'warning';
      case 'low': return 'primary';
      default: return 'default';
    }
  }

  onViewChange(view: 'matrix' | 'inheritance' | 'conflicts'): void {
    this.selectedView.set(view);
  }

  exportMatrix(): void {
    const matrixValue = this.matrix();
    if (!matrixValue) return;

    const data = JSON.stringify(matrixValue, null, 2);
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = 'permission-matrix.json';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  onClose(): void {
    this.dialogRef.close();
  }
}