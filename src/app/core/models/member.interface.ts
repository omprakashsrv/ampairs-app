export interface WorkspaceMember {
  id: string;
  workspace_id: string;
  user_id: string;
  email: string;
  name: string;
  role: WorkspaceRole;
  permissions: WorkspacePermission[];
  status: MemberStatus;
  is_active: boolean;
  department?: string;
  phone?: string;
  avatar_url?: string;
  joined_at: string;
  last_activity_at?: string;
  invited_by?: string;
  invitation_accepted_at?: string;
  team_ids?: string[];
  primary_team_id?: string;
  created_at: string;
  updated_at: string;
}

export interface WorkspaceMemberListItem {
  id: string;
  user_id: string;
  user: {
    id: number;
    uid: string;
    country_code: number;
    phone: string;
    email?: string;
    display_name: string;
    full_name: string;
    first_name: string;
    last_name: string;
    profile_picture_url?: string;
    is_active: boolean;
    is_enabled: boolean;
  };
  role: WorkspaceRole;
  permissions: WorkspacePermission[];
  is_active: boolean;
  joined_at: string;
  last_activity_at?: string;
  primary_team?: any;
  teams: any[];
  job_title?: string;
}

export interface WorkspaceInvitation {
  id: string;
  workspace_id: string;
  email: string;
  phone?: string;
  role: WorkspaceRole;
  status: InvitationStatus;
  invited_by: string;
  invited_by_name?: string;
  custom_message?: string;
  team_ids?: string[];
  expires_at: string;
  created_at: string;
  updated_at: string;
  accepted_at?: string;
  cancelled_at?: string;
  token?: string;
}

export interface CreateInvitationRequest {
  email?: string;
  phone?: string;
  country_code?: number;
  invited_role: WorkspaceRole;
  name?: string;
  message?: string;
  expires_in_days?: number;
  send_notification?: boolean;
  permissions?: string[];
}

export interface BulkInvitationRequest {
  invitations: CreateInvitationRequest[];
  default_message?: string;
}

export interface UpdateMemberRequest {
  role?: WorkspaceRole;
  permissions?: WorkspacePermission[];
  department?: string;
  team_ids?: string[];
  primary_team_id?: string;
  is_active?: boolean;
  reason?: string;
  notify_member?: boolean;
}

export interface MemberSearchFilters {
  role?: WorkspaceRole;
  status?: MemberStatus;
  department?: string;
  search_query?: string;
  team_id?: string;
  is_active?: boolean;
}

export interface InvitationSearchFilters {
  status?: InvitationStatus;
  role?: WorkspaceRole;
  email?: string;
  search_query?: string;
}

export interface MemberStatistics {
  total_members: number;
  active_members: number;
  inactive_members: number;
  pending_invitations: number;
  role_distribution: { [key in WorkspaceRole]?: number };
  department_distribution: { [department: string]: number };
  recent_joins: number;
  online_members: number;
}

export interface InvitationStatistics {
  total_invitations: number;
  pending_invitations: number;
  accepted_invitations: number;
  expired_invitations: number;
  cancelled_invitations: number;
  acceptance_rate: number;
  average_response_time: number;
}

export enum WorkspaceRole {
  OWNER = 'OWNER',
  ADMIN = 'ADMIN',
  MANAGER = 'MANAGER',
  MEMBER = 'MEMBER',
  GUEST = 'GUEST',
  VIEWER = 'VIEWER'
}

export enum WorkspacePermission {
  WORKSPACE_MANAGE = 'WORKSPACE_MANAGE',
  WORKSPACE_DELETE = 'WORKSPACE_DELETE',
  MEMBER_VIEW = 'MEMBER_VIEW',
  MEMBER_INVITE = 'MEMBER_INVITE',
  MEMBER_MANAGE = 'MEMBER_MANAGE',
  MEMBER_DELETE = 'MEMBER_DELETE',
  VIEW_TEAMS = 'VIEW_TEAMS',
  MANAGE_TEAMS = 'MANAGE_TEAMS',
  MANAGE_TEAM_MEMBERS = 'MANAGE_TEAM_MEMBERS',
  MODULE_MANAGE = 'MODULE_MANAGE',
  SETTINGS_MANAGE = 'SETTINGS_MANAGE',
  DATA_EXPORT = 'DATA_EXPORT',
  ANALYTICS_VIEW = 'ANALYTICS_VIEW'
}

export enum MemberStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  SUSPENDED = 'SUSPENDED',
  PENDING = 'PENDING'
}

export enum InvitationStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  EXPIRED = 'EXPIRED',
  CANCELLED = 'CANCELLED',
  REJECTED = 'REJECTED'
}

export const ROLE_HIERARCHY_LEVELS: { [key in WorkspaceRole]: number } = {
  [WorkspaceRole.OWNER]: 100,
  [WorkspaceRole.ADMIN]: 80,
  [WorkspaceRole.MANAGER]: 60,
  [WorkspaceRole.MEMBER]: 40,
  [WorkspaceRole.GUEST]: 20,
  [WorkspaceRole.VIEWER]: 10
};

export const ROLE_DISPLAY_NAMES: { [key in WorkspaceRole]: string } = {
  [WorkspaceRole.OWNER]: 'Owner',
  [WorkspaceRole.ADMIN]: 'Administrator',
  [WorkspaceRole.MANAGER]: 'Manager',
  [WorkspaceRole.MEMBER]: 'Member',
  [WorkspaceRole.GUEST]: 'Guest',
  [WorkspaceRole.VIEWER]: 'Viewer'
};

export const PERMISSION_DISPLAY_NAMES: { [key in WorkspacePermission]: string } = {
  [WorkspacePermission.WORKSPACE_MANAGE]: 'Manage Workspace',
  [WorkspacePermission.WORKSPACE_DELETE]: 'Delete Workspace',
  [WorkspacePermission.MEMBER_VIEW]: 'View Members',
  [WorkspacePermission.MEMBER_INVITE]: 'Invite Members',
  [WorkspacePermission.MEMBER_MANAGE]: 'Manage Members',
  [WorkspacePermission.MEMBER_DELETE]: 'Remove Members',
  [WorkspacePermission.VIEW_TEAMS]: 'View Teams',
  [WorkspacePermission.MANAGE_TEAMS]: 'Manage Teams',
  [WorkspacePermission.MANAGE_TEAM_MEMBERS]: 'Manage Team Members',
  [WorkspacePermission.MODULE_MANAGE]: 'Manage Modules',
  [WorkspacePermission.SETTINGS_MANAGE]: 'Manage Settings',
  [WorkspacePermission.DATA_EXPORT]: 'Export Data',
  [WorkspacePermission.ANALYTICS_VIEW]: 'View Analytics'
};

export const STATUS_COLORS: { [key in MemberStatus]: string } = {
  [MemberStatus.ACTIVE]: 'success',
  [MemberStatus.INACTIVE]: 'warn',
  [MemberStatus.SUSPENDED]: 'error',
  [MemberStatus.PENDING]: 'accent'
};

export const INVITATION_STATUS_COLORS: { [key in InvitationStatus]: string } = {
  [InvitationStatus.PENDING]: 'accent',
  [InvitationStatus.ACCEPTED]: 'success',
  [InvitationStatus.EXPIRED]: 'warn',
  [InvitationStatus.CANCELLED]: 'error',
  [InvitationStatus.REJECTED]: 'error'
};

// Backend API Response Interfaces
export interface WorkspaceRoleResponse {
  name: string;
  display_name: string;  // snake_case from backend
  level: number;
  description: string;
  manageable_roles: string[];  // snake_case from backend
}

export interface WorkspacePermissionResponse {
  name: string;
  permissionName: string;
  description: string;
}