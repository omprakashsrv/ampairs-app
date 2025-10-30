import {Component, OnInit, computed, signal, inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule, FormBuilder, FormGroup} from '@angular/forms';
import {Router, ActivatedRoute} from '@angular/router';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatTableModule} from '@angular/material/table';
import {MatPaginatorModule} from '@angular/material/paginator';
import {MatSortModule} from '@angular/material/sort';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatChipsModule} from '@angular/material/chips';
import {MatBadgeModule} from '@angular/material/badge';
import {MatMenuModule} from '@angular/material/menu';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatDialogModule, MatDialog} from '@angular/material/dialog';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatDividerModule} from '@angular/material/divider';
import {MatTabsModule} from '@angular/material/tabs';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatExpansionModule} from '@angular/material/expansion';
import {SelectionModel} from '@angular/cdk/collections';

import {WorkspaceService} from '../../core/services/workspace.service';
import {RoleService} from '../../core/services/role.service';
import {MemberService} from '../../core/services/member.service';
import {RoleDialogComponent} from './role-dialog/role-dialog.component';
import {PermissionMatrixDialogComponent} from './permission-matrix-dialog/permission-matrix-dialog.component';
import {
  CustomRole,
  RoleTemplate,
  PermissionMatrix,
  RoleStatistics,
  RoleType,
  PERMISSION_GROUPS,
  ROLE_TEMPLATES,
  PermissionGroup
} from '../../core/models/role.interface';
import {
  WorkspaceRole,
  WorkspacePermission,
  ROLE_DISPLAY_NAMES,
  PERMISSION_DISPLAY_NAMES
} from '../../core/models/member.interface';

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatInputModule,
    MatSelectModule,
    MatFormFieldModule,
    MatChipsModule,
    MatBadgeModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatDialogModule,
    MatToolbarModule,
    MatDividerModule,
    MatTabsModule,
    MatCheckboxModule,
    MatSlideToggleModule,
    MatExpansionModule
  ],
  templateUrl: './roles.component.html',
  styleUrl: './roles.component.scss'
})
export class RolesComponent implements OnInit {
  private workspaceService = inject(WorkspaceService);
  private roleService = inject(RoleService);
  private memberService = inject(MemberService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  private fb = inject(FormBuilder);

  // Component state
  workspaceId = signal<string>('');
  currentWorkspace = this.workspaceService.currentWorkspace;
  selectedTab = signal(0);
  
  // Role management
  customRoles = this.roleService.customRoles;
  systemRoles = this.roleService.systemRoles;
  allRoles = this.roleService.allRoles;
  roleTemplates = this.roleService.roleTemplates;
  permissionMatrix = this.roleService.permissionMatrix;
  roleStatistics = this.roleService.roleStatistics;
  roleLoading = this.roleService.loading;
  roleError = this.roleService.error;
  
  // Permission management
  permissionGroups = this.roleService.permissionGroups;

  // Table configuration
  systemRoleColumns = ['role', 'permissions', 'members', 'actions'];
  customRoleColumns = ['select', 'role', 'baseRole', 'permissions', 'members', 'status', 'actions'];
  templateColumns = ['template', 'businessType', 'permissions', 'popularity', 'actions'];
  
  // Selection
  customRoleSelection = new SelectionModel<CustomRole>(true, []);
  templateSelection = new SelectionModel<RoleTemplate>(false, []);

  // Search and filters
  searchForm: FormGroup;
  selectedRoleType: RoleType | null = null;
  showInactiveRoles = false;

  // Constants for template
  readonly WorkspaceRole = WorkspaceRole;
  readonly WorkspacePermission = WorkspacePermission;
  readonly RoleType = RoleType;
  readonly ROLE_DISPLAY_NAMES = ROLE_DISPLAY_NAMES;
  readonly PERMISSION_DISPLAY_NAMES = PERMISSION_DISPLAY_NAMES;
  readonly PERMISSION_GROUPS = PERMISSION_GROUPS;

  // Computed properties
  hasSelectedCustomRoles = computed(() => this.customRoleSelection.selected.length > 0);
  activeCustomRolesCount = computed(() => this.customRoles().filter(role => role.is_active).length);
  inactiveCustomRolesCount = computed(() => this.customRoles().filter(role => !role.is_active).length);

  constructor() {
    this.searchForm = this.fb.group({
      searchQuery: [''],
      roleType: [''],
      baseRole: [''],
      showInactive: [false]
    });

    // Watch for search changes
    this.searchForm.valueChanges.subscribe(() => {
      this.applyFilters();
    });
  }

  async ngOnInit(): Promise<void> {
    // Get workspace ID from route
    const workspaceSlug = this.route.parent?.snapshot.paramMap.get('slug');
    if (!workspaceSlug) {
      this.showError('Workspace not found');
      this.router.navigate(['/workspaces']);
      return;
    }

    const workspace = this.currentWorkspace();
    if (!workspace || workspace.slug !== workspaceSlug) {
      this.showError('Workspace not accessible');
      this.router.navigate(['/workspaces']);
      return;
    }

    this.workspaceId.set(workspace.id);
    await this.loadData();
  }

  async loadData(): Promise<void> {
    const wId = this.workspaceId();
    if (!wId) return;

    try {
      // Load in parallel
      await Promise.all([
        this.loadCustomRoles(),
        this.loadRoleStatistics(),
        this.loadPermissionMatrix()
      ]);
    } catch (error) {
      console.error('Failed to load role data:', error);
      this.showError('Failed to load role information');
    }
  }

  async loadCustomRoles(): Promise<void> {
    const wId = this.workspaceId();
    if (!wId) return;

    try {
      await this.roleService.getCustomRoles(
        wId,
        0,
        100,
        this.selectedRoleType || undefined,
        this.showInactiveRoles ? undefined : true
      );
    } catch (error: any) {
      this.showError(error.message || 'Failed to load custom roles');
    }
  }

  async loadRoleStatistics(): Promise<void> {
    const wId = this.workspaceId();
    if (!wId) return;

    try {
      await this.roleService.getRoleStatistics(wId);
    } catch (error: any) {
      console.error('Failed to load role statistics:', error);
      // Don't show error for statistics as it's not critical
    }
  }

  async loadPermissionMatrix(): Promise<void> {
    const wId = this.workspaceId();
    if (!wId) return;

    try {
      await this.roleService.getPermissionMatrix(wId);
    } catch (error: any) {
      console.error('Failed to load permission matrix:', error);
      // Don't show error for matrix as it's not critical
    }
  }

  applyFilters(): void {
    const formValue = this.searchForm.value;
    
    this.selectedRoleType = formValue.roleType || null;
    this.showInactiveRoles = formValue.showInactive || false;

    // Reload custom roles with filters
    this.loadCustomRoles();
  }

  clearFilters(): void {
    this.searchForm.reset();
    this.selectedRoleType = null;
    this.showInactiveRoles = false;
    this.loadCustomRoles();
  }

  onTabChange(tabIndex: number): void {
    this.selectedTab.set(tabIndex);
    this.customRoleSelection.clear();
    this.templateSelection.clear();
  }

  // ========== ROLE ACTIONS ==========

  createCustomRole(): void {
    const workspace = this.currentWorkspace();
    if (!workspace) return;

    const dialogRef = this.dialog.open(RoleDialogComponent, {
      width: '800px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      data: {
        workspaceId: workspace.id,
        mode: 'create'
      },
      disableClose: false,
      autoFocus: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Role was created, refresh data
        this.loadData();
      }
    });
  }

  editCustomRole(role: CustomRole): void {
    const workspace = this.currentWorkspace();
    if (!workspace) return;

    const dialogRef = this.dialog.open(RoleDialogComponent, {
      width: '800px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      data: {
        workspaceId: workspace.id,
        mode: 'edit',
        role: role
      },
      disableClose: false,
      autoFocus: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Role was updated, refresh data
        this.loadData();
      }
    });
  }

  async toggleCustomRoleStatus(role: CustomRole): Promise<void> {
    const wId = this.workspaceId();
    if (!wId) return;

    try {
      await this.roleService.updateCustomRole(wId, role.id, {
        is_active: !role.is_active
      });
      
      this.showSuccess(`Role ${role.name} ${role.is_active ? 'deactivated' : 'activated'}`);
    } catch (error: any) {
      this.showError(error.message || 'Failed to update role status');
    }
  }

  async deleteCustomRole(role: CustomRole): Promise<void> {
    if (role.member_count > 0) {
      this.showError(`Cannot delete role "${role.name}" - it is assigned to ${role.member_count} member(s)`);
      return;
    }

    const confirmed = confirm(`Are you sure you want to delete the custom role "${role.name}"?`);
    if (!confirmed) return;

    const wId = this.workspaceId();
    if (!wId) return;

    try {
      await this.roleService.deleteCustomRole(wId, role.id);
      this.showSuccess(`Role "${role.name}" deleted successfully`);
    } catch (error: any) {
      this.showError(error.message || 'Failed to delete custom role');
    }
  }

  duplicateCustomRole(role: CustomRole): void {
    const workspace = this.currentWorkspace();
    if (!workspace) return;

    // Create a template-like object from the existing role
    const template: RoleTemplate = {
      id: 'duplicate_' + role.id,
      name: role.name + ' (Copy)',
      description: role.description,
      business_type: ['BUSINESS'],
      base_role: role.base_role,
      permissions: role.permissions,
      custom_permissions: [],
      is_popular: false,
      use_count: 0,
      tags: []
    };

    const dialogRef = this.dialog.open(RoleDialogComponent, {
      width: '800px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      data: {
        workspaceId: workspace.id,
        mode: 'template',
        template: template
      },
      disableClose: false,
      autoFocus: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Role was created, refresh data
        this.loadData();
      }
    });
  }

  // ========== TEMPLATE ACTIONS ==========

  async createRoleFromTemplate(template: RoleTemplate): Promise<void> {
    const workspace = this.currentWorkspace();
    if (!workspace) return;

    const dialogRef = this.dialog.open(RoleDialogComponent, {
      width: '800px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      data: {
        workspaceId: workspace.id,
        mode: 'template',
        template: template
      },
      disableClose: false,
      autoFocus: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Role was created, refresh data
        this.loadData();
      }
    });
  }

  viewTemplateDetails(template: RoleTemplate): void {
    // TODO: Open template details dialog
    this.showInfo('Template details dialog will be implemented');
  }

  // ========== BULK ACTIONS ==========

  async bulkActivateRoles(): Promise<void> {
    const selected = this.customRoleSelection.selected.filter(role => !role.is_active);
    if (selected.length === 0) {
      this.showInfo('No inactive roles selected');
      return;
    }

    const confirmed = confirm(`Activate ${selected.length} custom role(s)?`);
    if (!confirmed) return;

    const wId = this.workspaceId();
    if (!wId) return;

    try {
      for (const role of selected) {
        await this.roleService.updateCustomRole(wId, role.id, { is_active: true });
      }
      
      this.showSuccess(`${selected.length} role(s) activated`);
      this.customRoleSelection.clear();
    } catch (error: any) {
      this.showError(error.message || 'Failed to activate roles');
    }
  }

  async bulkDeactivateRoles(): Promise<void> {
    const selected = this.customRoleSelection.selected.filter(role => role.is_active);
    if (selected.length === 0) {
      this.showInfo('No active roles selected');
      return;
    }

    const confirmed = confirm(`Deactivate ${selected.length} custom role(s)?`);
    if (!confirmed) return;

    const wId = this.workspaceId();
    if (!wId) return;

    try {
      for (const role of selected) {
        await this.roleService.updateCustomRole(wId, role.id, { is_active: false });
      }
      
      this.showSuccess(`${selected.length} role(s) deactivated`);
      this.customRoleSelection.clear();
    } catch (error: any) {
      this.showError(error.message || 'Failed to deactivate roles');
    }
  }

  async bulkDeleteRoles(): Promise<void> {
    const selected = this.customRoleSelection.selected;
    const rolesWithMembers = selected.filter(role => role.member_count > 0);
    
    if (rolesWithMembers.length > 0) {
      this.showError(`Cannot delete ${rolesWithMembers.length} role(s) that have assigned members`);
      return;
    }

    if (selected.length === 0) {
      this.showInfo('No roles selected');
      return;
    }

    const confirmed = confirm(`Permanently delete ${selected.length} custom role(s)?`);
    if (!confirmed) return;

    const wId = this.workspaceId();
    if (!wId) return;

    try {
      for (const role of selected) {
        await this.roleService.deleteCustomRole(wId, role.id);
      }
      
      this.showSuccess(`${selected.length} role(s) deleted`);
      this.customRoleSelection.clear();
    } catch (error: any) {
      this.showError(error.message || 'Failed to delete roles');
    }
  }

  // ========== PERMISSION MATRIX ACTIONS ==========

  openPermissionMatrix(): void {
    const workspace = this.currentWorkspace();
    if (!workspace) return;

    const dialogRef = this.dialog.open(PermissionMatrixDialogComponent, {
      width: '95vw',
      maxWidth: '1400px',
      height: '90vh',
      maxHeight: '90vh',
      data: {
        workspaceId: workspace.id
      },
      disableClose: false,
      autoFocus: false
    });

    dialogRef.afterClosed().subscribe(() => {
      // Dialog closed, no action needed
    });
  }

  // ========== SELECTION METHODS ==========

  isAllCustomRolesSelected(): boolean {
    const numSelected = this.customRoleSelection.selected.length;
    const numRows = this.customRoles().length;
    return numSelected === numRows;
  }

  masterToggleCustomRoles(): void {
    if (this.isAllCustomRolesSelected()) {
      this.customRoleSelection.clear();
    } else {
      this.customRoles().forEach(row => this.customRoleSelection.select(row));
    }
  }

  // ========== UTILITY METHODS ==========

  getRoleIcon(role: WorkspaceRole, customRole?: CustomRole): string {
    if (customRole?.icon) return customRole.icon;
    
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

  getRoleColor(role: WorkspaceRole, customRole?: CustomRole): string {
    if (customRole?.color) return customRole.color;
    
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

  formatPermissionList(permissions: WorkspacePermission[]): string {
    return permissions.map(p => PERMISSION_DISPLAY_NAMES[p]).join(', ');
  }

  formatBusinessTypes(businessTypes: string[]): string {
    return businessTypes.map(type => type.charAt(0) + type.slice(1).toLowerCase()).join(', ');
  }

  getPopularityText(useCount: number): string {
    if (useCount > 200) return 'Very Popular';
    if (useCount > 100) return 'Popular';
    if (useCount > 50) return 'Moderate';
    return 'Niche';
  }

  getPopularityColor(useCount: number): string {
    if (useCount > 200) return 'success';
    if (useCount > 100) return 'primary';
    if (useCount > 50) return 'accent';
    return 'warn';
  }

  hasGroupPermissions(group: PermissionGroup, role: WorkspaceRole): boolean {
    const matrix = this.permissionMatrix();
    if (!matrix) return false;
    return group.permissions.some(p => matrix.matrix[role]?.[p] === 'GRANTED');
  }

  hasCustomRoleGroupPermissions(group: PermissionGroup, role: CustomRole): boolean {
    return group.permissions.some(p => role.permissions.includes(p));
  }

  hasTemplateGroupPermissions(group: PermissionGroup, template: RoleTemplate): boolean {
    return group.permissions.some(p => template.permissions.includes(p));
  }

  getRoleUsageCount(role: WorkspaceRole): number {
    const stats = this.roleStatistics();
    if (!stats || !stats.role_usage) return 0;
    return stats.role_usage[role] || 0;
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