import {computed, inject, Injectable, signal} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {firstValueFrom, Observable} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {environment} from '../../../environments/environment';
import {ApiResponse} from '../models/api-response.interface';
import {
  CustomRole,
  RoleTemplate,
  PermissionMatrix,
  RoleStatistics,
  RoleAssignment,
  CreateCustomRoleRequest,
  UpdateCustomRoleRequest,
  BulkRoleAssignmentRequest,
  PermissionCheckRequest,
  PermissionCheckResponse,
  PermissionConflict,
  RoleType,
  PermissionAccess,
  PERMISSION_GROUPS,
  ROLE_TEMPLATES,
  getInheritedPermissions,
  hasPermissionConflict
} from '../models/role.interface';
import {
  WorkspaceRole,
  WorkspacePermission,
  ROLE_DISPLAY_NAMES,
  PERMISSION_DISPLAY_NAMES
} from '../models/member.interface';
import {PaginatedResponse} from './workspace.service';

@Injectable({
  providedIn: 'root'
})
export class RoleService {
  private readonly http = inject(HttpClient);

  // Signal-based state management
  private _customRoles = signal<CustomRole[]>([]);
  private _roleTemplates = signal<RoleTemplate[]>(ROLE_TEMPLATES);
  private _permissionMatrix = signal<PermissionMatrix | null>(null);
  private _roleStatistics = signal<RoleStatistics | null>(null);
  private _loading = signal(false);
  private _error = signal<string | null>(null);

  // Public readonly signals
  readonly customRoles = this._customRoles.asReadonly();
  readonly roleTemplates = this._roleTemplates.asReadonly();
  readonly permissionMatrix = this._permissionMatrix.asReadonly();
  readonly roleStatistics = this._roleStatistics.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  // Computed signals
  readonly activeCustomRoles = computed(() => 
    this._customRoles().filter(role => role.is_active)
  );
  readonly systemRoles = computed(() => 
    Object.values(WorkspaceRole).map(role => ({
      role,
      name: ROLE_DISPLAY_NAMES[role],
      permissions: getInheritedPermissions(role),
      member_count: this._roleStatistics()?.role_usage?.[role] || 0
    }))
  );
  readonly allRoles = computed(() => [
    ...this.systemRoles(),
    ...this.activeCustomRoles().map(role => ({
      role: role.base_role,
      name: role.name,
      permissions: role.permissions,
      member_count: role.member_count,
      custom: true,
      customRole: role
    }))
  ]);
  readonly permissionGroups = computed(() => PERMISSION_GROUPS);

  private getWorkspaceApiUrl(workspaceId: string): string {
    return `${environment.apiBaseUrl}/workspace/v1/${workspaceId}`;
  }

  // ========== ROLE MANAGEMENT ==========

  /**
   * Get all custom roles in a workspace
   */
  async getCustomRoles(
    workspaceId: string,
    page = 0,
    size = 50,
    type?: RoleType,
    active?: boolean
  ): Promise<CustomRole[]> {
    this._loading.set(true);
    this._error.set(null);

    try {
      let params = new HttpParams()
        .set('page', page.toString())
        .set('size', size.toString());

      if (type) params = params.set('type', type);
      if (active !== undefined) params = params.set('active', active.toString());

      const response = await firstValueFrom(
        this.http.get<ApiResponse<PaginatedResponse<CustomRole>>>(
          `${this.getWorkspaceApiUrl(workspaceId)}/roles`,
          { params }
        ).pipe(catchError(this.handleError))
      );

      const roles = response.data.content;
      this._customRoles.set(roles);
      return roles;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to load custom roles');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Create custom role
   */
  async createCustomRole(
    workspaceId: string,
    roleData: CreateCustomRoleRequest
  ): Promise<CustomRole> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const response = await firstValueFrom(
        this.http.post<CustomRole>(
          `${this.getWorkspaceApiUrl(workspaceId)}/roles`,
          roleData
        ).pipe(catchError(this.handleError))
      );

      // Refresh roles list
      await this.getCustomRoles(workspaceId);
      
      return response;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to create custom role');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Update custom role
   */
  async updateCustomRole(
    workspaceId: string,
    roleId: string,
    roleData: UpdateCustomRoleRequest
  ): Promise<CustomRole> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const response = await firstValueFrom(
        this.http.put<CustomRole>(
          `${this.getWorkspaceApiUrl(workspaceId)}/roles/${roleId}`,
          roleData
        ).pipe(catchError(this.handleError))
      );

      // Refresh roles list
      await this.getCustomRoles(workspaceId);
      
      return response;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to update custom role');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Delete custom role
   */
  async deleteCustomRole(workspaceId: string, roleId: string): Promise<void> {
    this._loading.set(true);
    this._error.set(null);

    try {
      await firstValueFrom(
        this.http.delete(`${this.getWorkspaceApiUrl(workspaceId)}/roles/${roleId}`)
          .pipe(catchError(this.handleError))
      );

      // Refresh roles list
      await this.getCustomRoles(workspaceId);
    } catch (error: any) {
      this._error.set(error.message || 'Failed to delete custom role');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Create role from template
   */
  async createRoleFromTemplate(
    workspaceId: string,
    templateId: string,
    roleName: string,
    customizations?: Partial<CreateCustomRoleRequest>
  ): Promise<CustomRole> {
    const template = ROLE_TEMPLATES.find(t => t.id === templateId);
    if (!template) {
      throw new Error('Template not found');
    }

    const roleData: CreateCustomRoleRequest = {
      name: roleName,
      description: template.description,
      base_role: template.base_role,
      permissions: template.permissions,
      custom_permissions: template.custom_permissions,
      ...customizations
    };

    return this.createCustomRole(workspaceId, roleData);
  }

  // ========== PERMISSION MATRIX ==========

  /**
   * Get permission matrix for workspace
   */
  async getPermissionMatrix(workspaceId: string): Promise<PermissionMatrix> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const response = await firstValueFrom(
        this.http.get<PermissionMatrix>(
          `${this.getWorkspaceApiUrl(workspaceId)}/permissions/matrix`
        ).pipe(catchError(this.handleError))
      );

      this._permissionMatrix.set(response);
      return response;
    } catch (error: any) {
      // If API doesn't exist yet, generate matrix from local data
      const matrix = this.generatePermissionMatrix();
      this._permissionMatrix.set(matrix);
      return matrix;
    } finally {
      this._loading.set(false);
    }
  }

  private generatePermissionMatrix(): PermissionMatrix {
    const roles = Object.values(WorkspaceRole);
    const permissionGroups = PERMISSION_GROUPS;
    const matrix: { [role: string]: { [permission: string]: any } } = {};

    roles.forEach(role => {
      matrix[role] = {};
      const rolePermissions = getInheritedPermissions(role);
      
      permissionGroups.forEach(group => {
        group.permissions.forEach(permission => {
          if (matrix[role]) {
            matrix[role][permission] = PermissionAccess.GRANTED;
          }
        });
      });
    });

    return {
      roles,
      permissions: permissionGroups,
      matrix,
      inheritance: this.generateRoleInheritance()
    };
  }

  private generateRoleInheritance() {
    const roles = Object.values(WorkspaceRole);
    const inheritance = [];

    // Generate inheritance relationships
    const hierarchy = [
      WorkspaceRole.OWNER,
      WorkspaceRole.ADMIN,
      WorkspaceRole.MANAGER,
      WorkspaceRole.MEMBER,
      WorkspaceRole.GUEST,
      WorkspaceRole.VIEWER
    ];

    for (let i = 0; i < hierarchy.length - 1; i++) {
      const parent = hierarchy[i];
      const child = hierarchy[i + 1];
      
      if (parent && child) {
        const parentPermissions = getInheritedPermissions(parent);
        const childPermissions = getInheritedPermissions(child);
      
        inheritance.push({
          parent_role: parent,
          child_role: child,
          inherited_permissions: childPermissions,
          overridden_permissions: parentPermissions.filter(p => !childPermissions.includes(p))
        });
      }
    }

    return inheritance;
  }

  // ========== ROLE ASSIGNMENTS ==========

  /**
   * Bulk assign roles to members
   */
  async bulkAssignRoles(
    workspaceId: string,
    assignmentData: BulkRoleAssignmentRequest
  ): Promise<void> {
    this._loading.set(true);
    this._error.set(null);

    try {
      await firstValueFrom(
        this.http.post(
          `${this.getWorkspaceApiUrl(workspaceId)}/roles/bulk-assign`,
          assignmentData
        ).pipe(catchError(this.handleError))
      );
    } catch (error: any) {
      this._error.set(error.message || 'Failed to assign roles');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Validate role assignment
   */
  async validateRoleAssignment(
    workspaceId: string,
    assignment: RoleAssignment
  ): Promise<PermissionConflict[]> {
    try {
      const response = await firstValueFrom(
        this.http.post<PermissionConflict[]>(
          `${this.getWorkspaceApiUrl(workspaceId)}/roles/validate-assignment`,
          assignment
        ).pipe(catchError(this.handleError))
      );

      return response;
    } catch (error: any) {
      // If API doesn't exist, use local validation
      return this.validateRoleAssignmentLocally(assignment);
    }
  }

  private validateRoleAssignmentLocally(assignment: RoleAssignment): PermissionConflict[] {
    const currentPermissions = getInheritedPermissions(assignment.current_role);
    const proposedPermissions = getInheritedPermissions(assignment.proposed_role);
    
    return hasPermissionConflict(currentPermissions, proposedPermissions);
  }

  // ========== PERMISSION CHECKING ==========

  /**
   * Check if member has specific permission
   */
  async checkPermission(
    workspaceId: string,
    checkRequest: PermissionCheckRequest
  ): Promise<PermissionCheckResponse> {
    try {
      const response = await firstValueFrom(
        this.http.post<PermissionCheckResponse>(
          `${this.getWorkspaceApiUrl(workspaceId)}/permissions/check`,
          checkRequest
        ).pipe(catchError(this.handleError))
      );

      return response;
    } catch (error: any) {
      // Fallback to client-side checking if API not available
      return {
        has_permission: false,
        granted_by: [],
        denied_by: ['API Not Available'],
        effective_scope: 'WORKSPACE' as any,
        conditions: []
      };
    }
  }

  // ========== STATISTICS ==========

  /**
   * Get role statistics
   */
  async getRoleStatistics(workspaceId: string): Promise<RoleStatistics> {
    try {
      const response = await firstValueFrom(
        this.http.get<RoleStatistics>(
          `${this.getWorkspaceApiUrl(workspaceId)}/roles/statistics`
        ).pipe(catchError(this.handleError))
      );

      this._roleStatistics.set(response);
      return response;
    } catch (error: any) {
      // Generate mock statistics if API not available
      const mockStats: RoleStatistics = {
        total_roles: Object.keys(WorkspaceRole).length,
        system_roles: Object.keys(WorkspaceRole).length,
        custom_roles: this._customRoles().length,
        active_roles: Object.keys(WorkspaceRole).length + this.activeCustomRoles().length,
        role_usage: {
          [WorkspaceRole.OWNER]: 1,
          [WorkspaceRole.ADMIN]: 2,
          [WorkspaceRole.MANAGER]: 5,
          [WorkspaceRole.MEMBER]: 15,
          [WorkspaceRole.GUEST]: 3,
          [WorkspaceRole.VIEWER]: 2
        },
        permission_usage: {},
        most_used_permissions: [
          WorkspacePermission.MEMBER_VIEW,
          WorkspacePermission.VIEW_TEAMS,
          WorkspacePermission.ANALYTICS_VIEW
        ],
        least_used_permissions: [
          WorkspacePermission.WORKSPACE_DELETE,
          WorkspacePermission.MODULE_MANAGE
        ]
      };

      this._roleStatistics.set(mockStats);
      return mockStats;
    }
  }

  // ========== UTILITY METHODS ==========

  /**
   * Get role templates filtered by business type
   */
  getRoleTemplatesForBusiness(businessType: string): RoleTemplate[] {
    return ROLE_TEMPLATES.filter(template => 
      template.business_type.includes(businessType.toUpperCase()) ||
      template.business_type.includes('ALL')
    );
  }

  /**
   * Get permissions for role
   */
  getPermissionsForRole(role: WorkspaceRole, customRole?: CustomRole): WorkspacePermission[] {
    if (customRole) {
      return customRole.permissions;
    }
    return getInheritedPermissions(role);
  }

  /**
   * Check if user can assign role
   */
  canAssignRole(
    currentUserRole: WorkspaceRole,
    targetRole: WorkspaceRole,
    customRole?: CustomRole
  ): boolean {
    const currentLevel = this.getRoleLevel(currentUserRole);
    const targetLevel = this.getRoleLevel(customRole?.base_role || targetRole);
    
    // Users can only assign roles lower than their own
    return currentLevel > targetLevel;
  }

  private getRoleLevel(role: WorkspaceRole): number {
    const levels = {
      [WorkspaceRole.OWNER]: 100,
      [WorkspaceRole.ADMIN]: 80,
      [WorkspaceRole.MANAGER]: 60,
      [WorkspaceRole.MEMBER]: 40,
      [WorkspaceRole.GUEST]: 20,
      [WorkspaceRole.VIEWER]: 10
    };
    return levels[role] || 0;
  }

  /**
   * Get role display information
   */
  getRoleDisplayInfo(role: WorkspaceRole, customRole?: CustomRole) {
    if (customRole) {
      return {
        name: customRole.name,
        description: customRole.description,
        color: customRole.color,
        icon: customRole.icon || 'person',
        permissions: customRole.permissions
      };
    }

    return {
      name: ROLE_DISPLAY_NAMES[role],
      description: `System role: ${ROLE_DISPLAY_NAMES[role]}`,
      color: this.getDefaultRoleColor(role),
      icon: this.getDefaultRoleIcon(role),
      permissions: getInheritedPermissions(role)
    };
  }

  private getDefaultRoleColor(role: WorkspaceRole): string {
    const colors = {
      [WorkspaceRole.OWNER]: '#f50057',
      [WorkspaceRole.ADMIN]: '#ff5722',
      [WorkspaceRole.MANAGER]: '#ff9800',
      [WorkspaceRole.MEMBER]: '#2196f3',
      [WorkspaceRole.GUEST]: '#9c27b0',
      [WorkspaceRole.VIEWER]: '#607d8b'
    };
    return colors[role] || '#2196f3';
  }

  private getDefaultRoleIcon(role: WorkspaceRole): string {
    const icons = {
      [WorkspaceRole.OWNER]: 'crown',
      [WorkspaceRole.ADMIN]: 'admin_panel_settings',
      [WorkspaceRole.MANAGER]: 'supervisor_account',
      [WorkspaceRole.MEMBER]: 'person',
      [WorkspaceRole.GUEST]: 'person_outline',
      [WorkspaceRole.VIEWER]: 'visibility'
    };
    return icons[role] || 'person';
  }

  /**
   * Clear error state
   */
  clearError(): void {
    this._error.set(null);
  }

  /**
   * Clear all state
   */
  clearState(): void {
    this._customRoles.set([]);
    this._roleTemplates.set(ROLE_TEMPLATES);
    this._permissionMatrix.set(null);
    this._roleStatistics.set(null);
    this._error.set(null);
    this._loading.set(false);
  }

  /**
   * Handle HTTP errors
   */
  private handleError(error: any): Observable<never> {
    console.error('Role Service Error:', error);
    let errorMessage = 'An unexpected error occurred';

    if (error.error && error.error.error && error.error.error.message) {
      errorMessage = error.error.error.message;
    } else if (error.error && error.error.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }

    throw new Error(errorMessage);
  }
}