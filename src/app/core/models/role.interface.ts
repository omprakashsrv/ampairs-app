import {WorkspaceRole, WorkspacePermission} from './member.interface';

export interface CustomRole {
  id: string;
  workspace_id: string;
  name: string;
  description: string;
  role_type: RoleType;
  base_role: WorkspaceRole;
  permissions: WorkspacePermission[];
  custom_permissions: CustomPermission[];
  is_system_role: boolean;
  is_active: boolean;
  member_count: number;
  created_by: string;
  created_at: string;
  updated_at: string;
  color?: string;
  icon?: string;
}

export interface CustomPermission {
  id: string;
  name: string;
  description: string;
  resource: string;
  action: string;
  scope: PermissionScope;
  is_dangerous: boolean;
}

export interface RoleTemplate {
  id: string;
  name: string;
  description: string;
  business_type: string[];
  base_role: WorkspaceRole;
  permissions: WorkspacePermission[];
  custom_permissions: string[];
  is_popular: boolean;
  use_count: number;
  tags: string[];
}

export interface PermissionGroup {
  id: string;
  name: string;
  description: string;
  permissions: WorkspacePermission[];
  icon: string;
  order: number;
}

export interface RoleAssignment {
  member_id: string;
  member_name: string;
  member_email: string;
  current_role: WorkspaceRole;
  proposed_role: WorkspaceRole;
  custom_role_id?: string;
  reason: string;
  effective_permissions: WorkspacePermission[];
  conflicts: PermissionConflict[];
  approved_by?: string;
  assigned_at?: string;
}

export interface PermissionConflict {
  permission: WorkspacePermission;
  conflict_type: ConflictType;
  current_source: string;
  proposed_source: string;
  description: string;
  severity: ConflictSeverity;
  resolution?: ConflictResolution;
}

export interface PermissionMatrix {
  roles: WorkspaceRole[];
  permissions: PermissionGroup[];
  matrix: { [role: string]: { [permission: string]: PermissionAccess } };
  inheritance: RoleInheritance[];
}

export interface RoleInheritance {
  parent_role: WorkspaceRole;
  child_role: WorkspaceRole;
  inherited_permissions: WorkspacePermission[];
  overridden_permissions: WorkspacePermission[];
}

export interface RoleStatistics {
  total_roles: number;
  system_roles: number;
  custom_roles: number;
  active_roles: number;
  role_usage: { [role: string]: number };
  permission_usage: { [permission: string]: number };
  most_used_permissions: WorkspacePermission[];
  least_used_permissions: WorkspacePermission[];
}

export interface CreateCustomRoleRequest {
  name: string;
  description: string;
  base_role: WorkspaceRole;
  permissions: WorkspacePermission[];
  custom_permissions?: string[];
  color?: string;
  icon?: string;
}

export interface UpdateCustomRoleRequest {
  name?: string;
  description?: string;
  permissions?: WorkspacePermission[];
  custom_permissions?: string[];
  is_active?: boolean;
  color?: string;
  icon?: string;
}

export interface BulkRoleAssignmentRequest {
  assignments: {
    member_id: string;
    role: WorkspaceRole;
    custom_role_id?: string;
  }[];
  reason: string;
  notify_members: boolean;
}

export interface PermissionCheckRequest {
  member_id: string;
  permission: WorkspacePermission;
  resource_id?: string;
  context?: { [key: string]: any };
}

export interface PermissionCheckResponse {
  has_permission: boolean;
  granted_by: string[];
  denied_by: string[];
  effective_scope: PermissionScope;
  conditions: PermissionCondition[];
}

export interface PermissionCondition {
  type: ConditionType;
  field: string;
  operator: string;
  value: any;
  description: string;
}

export enum RoleType {
  SYSTEM = 'SYSTEM',
  CUSTOM = 'CUSTOM',
  TEMPLATE = 'TEMPLATE'
}

export enum PermissionScope {
  GLOBAL = 'GLOBAL',
  WORKSPACE = 'WORKSPACE',
  TEAM = 'TEAM',
  PERSONAL = 'PERSONAL',
  RESOURCE = 'RESOURCE'
}

export enum ConflictType {
  PERMISSION_OVERLAP = 'PERMISSION_OVERLAP',
  ROLE_HIERARCHY = 'ROLE_HIERARCHY',
  CUSTOM_OVERRIDE = 'CUSTOM_OVERRIDE',
  TEAM_CONFLICT = 'TEAM_CONFLICT'
}

export enum ConflictSeverity {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL'
}

export enum ConflictResolution {
  USE_HIGHER_ROLE = 'USE_HIGHER_ROLE',
  USE_CUSTOM_ROLE = 'USE_CUSTOM_ROLE',
  MERGE_PERMISSIONS = 'MERGE_PERMISSIONS',
  MANUAL_REVIEW = 'MANUAL_REVIEW'
}

export enum PermissionAccess {
  DENIED = 'DENIED',
  GRANTED = 'GRANTED',
  INHERITED = 'INHERITED',
  CONDITIONAL = 'CONDITIONAL'
}

export enum ConditionType {
  TIME_BASED = 'TIME_BASED',
  LOCATION_BASED = 'LOCATION_BASED',
  RESOURCE_BASED = 'RESOURCE_BASED',
  APPROVAL_REQUIRED = 'APPROVAL_REQUIRED'
}

// Permission Groups for UI Organization
export const PERMISSION_GROUPS: PermissionGroup[] = [
  {
    id: 'workspace_management',
    name: 'Workspace Management',
    description: 'Core workspace administration and settings',
    permissions: [WorkspacePermission.WORKSPACE_MANAGE, WorkspacePermission.WORKSPACE_DELETE],
    icon: 'settings',
    order: 1
  },
  {
    id: 'member_management',
    name: 'Member Management',
    description: 'User and member administration',
    permissions: [
      WorkspacePermission.MEMBER_VIEW,
      WorkspacePermission.MEMBER_INVITE,
      WorkspacePermission.MEMBER_MANAGE,
      WorkspacePermission.MEMBER_DELETE
    ],
    icon: 'people',
    order: 2
  },
  {
    id: 'team_management',
    name: 'Team Management',
    description: 'Team creation and member assignment',
    permissions: [
      WorkspacePermission.VIEW_TEAMS,
      WorkspacePermission.MANAGE_TEAMS,
      WorkspacePermission.MANAGE_TEAM_MEMBERS
    ],
    icon: 'groups',
    order: 3
  },
  {
    id: 'module_management',
    name: 'Module Management',
    description: 'Module installation and configuration',
    permissions: [WorkspacePermission.MODULE_MANAGE],
    icon: 'extension',
    order: 4
  },
  {
    id: 'data_analytics',
    name: 'Data & Analytics',
    description: 'Data access and analytics',
    permissions: [
      WorkspacePermission.DATA_EXPORT,
      WorkspacePermission.ANALYTICS_VIEW
    ],
    icon: 'analytics',
    order: 5
  },
  {
    id: 'settings',
    name: 'Settings',
    description: 'Workspace settings and configuration',
    permissions: [WorkspacePermission.SETTINGS_MANAGE],
    icon: 'tune',
    order: 6
  }
];

// Role Templates for Common Business Scenarios
export const ROLE_TEMPLATES: RoleTemplate[] = [
  {
    id: 'dept_manager',
    name: 'Department Manager',
    description: 'Manages a department with team oversight and member management',
    business_type: ['BUSINESS', 'ENTERPRISE'],
    base_role: WorkspaceRole.MANAGER,
    permissions: [
      WorkspacePermission.MEMBER_VIEW,
      WorkspacePermission.MEMBER_INVITE,
      WorkspacePermission.MEMBER_MANAGE,
      WorkspacePermission.VIEW_TEAMS,
      WorkspacePermission.MANAGE_TEAMS,
      WorkspacePermission.MANAGE_TEAM_MEMBERS,
      WorkspacePermission.ANALYTICS_VIEW,
      WorkspacePermission.DATA_EXPORT
    ],
    custom_permissions: [],
    is_popular: true,
    use_count: 245,
    tags: ['management', 'leadership', 'teams']
  },
  {
    id: 'hr_specialist',
    name: 'HR Specialist',
    description: 'Human resources specialist with member and team management focus',
    business_type: ['BUSINESS', 'ENTERPRISE'],
    base_role: WorkspaceRole.MANAGER,
    permissions: [
      WorkspacePermission.MEMBER_VIEW,
      WorkspacePermission.MEMBER_INVITE,
      WorkspacePermission.MEMBER_MANAGE,
      WorkspacePermission.MEMBER_DELETE,
      WorkspacePermission.VIEW_TEAMS,
      WorkspacePermission.MANAGE_TEAMS,
      WorkspacePermission.MANAGE_TEAM_MEMBERS,
      WorkspacePermission.DATA_EXPORT
    ],
    custom_permissions: [],
    is_popular: true,
    use_count: 189,
    tags: ['hr', 'people', 'recruitment']
  },
  {
    id: 'project_lead',
    name: 'Project Lead',
    description: 'Project leadership with team coordination and limited member management',
    business_type: ['BUSINESS', 'ENTERPRISE', 'STARTUP'],
    base_role: WorkspaceRole.MEMBER,
    permissions: [
      WorkspacePermission.MEMBER_VIEW,
      WorkspacePermission.VIEW_TEAMS,
      WorkspacePermission.MANAGE_TEAM_MEMBERS,
      WorkspacePermission.ANALYTICS_VIEW
    ],
    custom_permissions: [],
    is_popular: true,
    use_count: 312,
    tags: ['project', 'leadership', 'coordination']
  },
  {
    id: 'read_only_analyst',
    name: 'Read-Only Analyst',
    description: 'Data analyst with view-only access to workspace information',
    business_type: ['BUSINESS', 'ENTERPRISE'],
    base_role: WorkspaceRole.VIEWER,
    permissions: [
      WorkspacePermission.MEMBER_VIEW,
      WorkspacePermission.VIEW_TEAMS,
      WorkspacePermission.ANALYTICS_VIEW
    ],
    custom_permissions: [],
    is_popular: false,
    use_count: 67,
    tags: ['analytics', 'readonly', 'data']
  },
  {
    id: 'contractor',
    name: 'External Contractor',
    description: 'Limited access for external contractors and freelancers',
    business_type: ['BUSINESS', 'ENTERPRISE', 'STARTUP'],
    base_role: WorkspaceRole.GUEST,
    permissions: [
      WorkspacePermission.MEMBER_VIEW,
      WorkspacePermission.VIEW_TEAMS
    ],
    custom_permissions: [],
    is_popular: true,
    use_count: 156,
    tags: ['contractor', 'external', 'limited']
  }
];

// Default Role Colors
export const ROLE_COLORS: { [key in WorkspaceRole]: string } = {
  [WorkspaceRole.OWNER]: '#f50057',
  [WorkspaceRole.ADMIN]: '#ff5722',
  [WorkspaceRole.MANAGER]: '#ff9800',
  [WorkspaceRole.MEMBER]: '#2196f3',
  [WorkspaceRole.GUEST]: '#9c27b0',
  [WorkspaceRole.VIEWER]: '#607d8b'
};

// Permission Descriptions for tooltips and help text
export const PERMISSION_DESCRIPTIONS: { [key in WorkspacePermission]: string } = {
  [WorkspacePermission.WORKSPACE_MANAGE]: 'Modify workspace settings, name, description, and configuration',
  [WorkspacePermission.WORKSPACE_DELETE]: 'Archive or permanently delete the workspace',
  [WorkspacePermission.MEMBER_VIEW]: 'View member directory and basic member information',
  [WorkspacePermission.MEMBER_INVITE]: 'Send invitations to new workspace members',
  [WorkspacePermission.MEMBER_MANAGE]: 'Update member roles, permissions, and profile information',
  [WorkspacePermission.MEMBER_DELETE]: 'Remove members from the workspace',
  [WorkspacePermission.VIEW_TEAMS]: 'View team information and member assignments',
  [WorkspacePermission.MANAGE_TEAMS]: 'Create, update, and delete teams',
  [WorkspacePermission.MANAGE_TEAM_MEMBERS]: 'Add and remove members from teams',
  [WorkspacePermission.MODULE_MANAGE]: 'Install, configure, and manage workspace modules',
  [WorkspacePermission.SETTINGS_MANAGE]: 'Modify workspace settings and preferences',
  [WorkspacePermission.DATA_EXPORT]: 'Export workspace data and generate reports',
  [WorkspacePermission.ANALYTICS_VIEW]: 'View workspace analytics and usage statistics'
};

// Helper functions
export function getRoleHierarchyLevel(role: WorkspaceRole): number {
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

export function canRoleAssignRole(assignerRole: WorkspaceRole, targetRole: WorkspaceRole): boolean {
  return getRoleHierarchyLevel(assignerRole) > getRoleHierarchyLevel(targetRole);
}

export function getInheritedPermissions(role: WorkspaceRole): WorkspacePermission[] {
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

export function hasPermissionConflict(
  currentPermissions: WorkspacePermission[],
  newPermissions: WorkspacePermission[]
): PermissionConflict[] {
  const conflicts: PermissionConflict[] = [];
  
  // Check for dangerous permission combinations
  const dangerousCombinations = [
    [WorkspacePermission.WORKSPACE_DELETE, WorkspacePermission.MEMBER_DELETE],
    [WorkspacePermission.WORKSPACE_MANAGE, WorkspacePermission.MEMBER_DELETE]
  ];
  
  dangerousCombinations.forEach(combination => {
    if (combination.every(perm => newPermissions.includes(perm))) {
      if (combination[0]) {
        conflicts.push({
          permission: combination[0],
          conflict_type: ConflictType.PERMISSION_OVERLAP,
          current_source: 'Current Role',
          proposed_source: 'New Role',
          description: `Dangerous combination: ${combination.join(' + ')}`,
          severity: ConflictSeverity.HIGH,
          resolution: ConflictResolution.MANUAL_REVIEW
        });
      }
    }
  });
  
  return conflicts;
}