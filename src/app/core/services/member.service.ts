import {computed, inject, Injectable, signal} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {firstValueFrom, Observable} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {environment} from '../../../environments/environment';
import {ApiResponse} from '../models/api-response.interface';
import {
  BulkInvitationRequest,
  CreateInvitationRequest,
  InvitationSearchFilters,
  InvitationStatistics,
  InvitationStatus,
  MemberSearchFilters,
  MemberStatistics,
  UpdateMemberRequest,
  WorkspaceInvitation,
  WorkspaceMember,
  WorkspaceMemberListItem,
  WorkspacePermissionResponse,
  WorkspaceRole,
  WorkspaceRoleResponse
} from '../models/member.interface';
import {PaginatedResponse} from './workspace.service';

@Injectable({
  providedIn: 'root'
})
export class MemberService {
  private readonly http = inject(HttpClient);

  // Signal-based state management
  private _members = signal<WorkspaceMemberListItem[]>([]);
  private _invitations = signal<WorkspaceInvitation[]>([]);
  private _loading = signal(false);
  private _error = signal<string | null>(null);
  private _memberStatistics = signal<MemberStatistics | null>(null);
  private _invitationStatistics = signal<InvitationStatistics | null>(null);

  // Public readonly signals
  readonly members = this._members.asReadonly();
  readonly invitations = this._invitations.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly memberStatistics = this._memberStatistics.asReadonly();
  readonly invitationStatistics = this._invitationStatistics.asReadonly();

  // Computed signals
  readonly memberCount = computed(() => this._members().length);
  readonly activeMembers = computed(() =>
    this._members().filter(member => member.is_active)
  );
  readonly pendingInvitations = computed(() =>
    this._invitations().filter(invitation => invitation.status === InvitationStatus.PENDING)
  );
  readonly pendingInvitationCount = computed(() => this.pendingInvitations().length);

  private getWorkspaceApiUrl(workspaceId: string): string {
    return `${environment.apiBaseUrl}/workspace/v1/${workspaceId}`;
  }

  /**
   * Get all members in a workspace
   */
  async getMembers(
    workspaceId: string,
    page = 0,
    size = 20,
    sortBy = 'joinedAt',
    sortDir = 'desc',
    filters?: MemberSearchFilters
  ): Promise<WorkspaceMemberListItem[]> {
    this._loading.set(true);
    this._error.set(null);

    try {
      let params = new HttpParams()
        .set('page', page.toString())
        .set('size', size.toString())
        .set('sortBy', sortBy)
        .set('sortDir', sortDir);

      if (filters) {
        if (filters.role) params = params.set('role', filters.role);
        if (filters.status) params = params.set('status', filters.status);
        if (filters.department) params = params.set('department', filters.department);
        if (filters.search_query) params = params.set('search_query', filters.search_query);
        if (filters.team_id) params = params.set('team_id', filters.team_id);
        if (filters.is_active !== undefined) params = params.set('is_active', filters.is_active.toString());
      }

      const response = await firstValueFrom(
        this.http.get<PaginatedResponse<WorkspaceMemberListItem>>(
          `${environment.apiBaseUrl}/workspace/v1/member`,
          {params}
        ).pipe(catchError(this.handleError))
      );

      const members = response.content;
      this._members.set(members);
      return members;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to load members');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Search members with filters
   */
  async searchMembers(
    workspaceId: string,
    filters: MemberSearchFilters,
    page = 0,
    size = 20
  ): Promise<WorkspaceMemberListItem[]> {
    this._loading.set(true);
    this._error.set(null);

    try {
      let params = new HttpParams()
        .set('page', page.toString())
        .set('size', size.toString());

      if (filters.search_query) params = params.set('search_query', filters.search_query);
      if (filters.role) params = params.set('role', filters.role);
      if (filters.status) params = params.set('status', filters.status);
      if (filters.department) params = params.set('department', filters.department);

      const response = await firstValueFrom(
        this.http.get<ApiResponse<PaginatedResponse<WorkspaceMemberListItem>>>(
          `${environment.apiBaseUrl}/workspace/v1/member/search`,
          {params}
        ).pipe(catchError(this.handleError))
      );

      const members = response.data.content;
      this._members.set(members);
      return members;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to search members');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Get member by ID
   */
  getMemberById(workspaceId: string, memberId: string): Observable<WorkspaceMember> {
    return this.http.get<WorkspaceMember>(`${environment.apiBaseUrl}/workspace/v1/member/${memberId}`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Update member
   */
  async updateMember(
    workspaceId: string,
    memberId: string,
    updateData: UpdateMemberRequest
  ): Promise<WorkspaceMember> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const response = await firstValueFrom(
        this.http.put<WorkspaceMember>(
          `${environment.apiBaseUrl}/workspace/v1/member/${memberId}`,
          updateData
        ).pipe(catchError(this.handleError))
      );

      // Refresh members list
      await this.getMembers(workspaceId);

      return response;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to update member');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Remove member from workspace
   */
  async removeMember(workspaceId: string, memberId: string): Promise<void> {
    this._loading.set(true);
    this._error.set(null);

    try {
      await firstValueFrom(
        this.http.delete(`${environment.apiBaseUrl}/workspace/v1/member/${memberId}`)
          .pipe(catchError(this.handleError))
      );

      // Refresh members list
      await this.getMembers(workspaceId);
    } catch (error: any) {
      this._error.set(error.message || 'Failed to remove member');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Get member statistics
   */
  async getMemberStatistics(workspaceId: string): Promise<MemberStatistics> {
    try {
      const response = await firstValueFrom(
        this.http.get<MemberStatistics>(`${environment.apiBaseUrl}/workspace/v1/member/statistics`)
          .pipe(catchError(this.handleError))
      );

      this._memberStatistics.set(response);
      return response;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to load member statistics');
      throw error;
    }
  }

  // ========== INVITATION MANAGEMENT ==========

  /**
   * Get all invitations in a workspace
   */
  async getInvitations(
    workspaceId: string,
    page = 0,
    size = 20,
    filters?: InvitationSearchFilters
  ): Promise<WorkspaceInvitation[]> {
    this._loading.set(true);
    this._error.set(null);

    try {
      let params = new HttpParams()
        .set('page', page.toString())
        .set('size', size.toString());

      if (filters) {
        if (filters.status) params = params.set('status', filters.status);
        if (filters.role) params = params.set('role', filters.role);
        if (filters.email) params = params.set('email', filters.email);
        if (filters.search_query) params = params.set('search_query', filters.search_query);
      }

      const response = await firstValueFrom(
        this.http.get<PaginatedResponse<WorkspaceInvitation>>(
          `${environment.apiBaseUrl}/workspace/v1/invitation/search`,
          {params}
        ).pipe(catchError(this.handleError))
      );

      const invitations = response.content;
      this._invitations.set(invitations);
      return invitations;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to load invitations');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Create invitation
   */
  async createInvitation(
    workspaceId: string,
    invitationData: CreateInvitationRequest
  ): Promise<WorkspaceInvitation> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const response = await firstValueFrom(
        this.http.post<WorkspaceInvitation>(
          `${environment.apiBaseUrl}/workspace/v1/invitation`,
          invitationData
        ).pipe(catchError(this.handleError))
      );

      // Refresh invitations list
      await this.getInvitations(workspaceId);

      return response;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to create invitation');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Create bulk invitations
   */
  async createBulkInvitations(
    workspaceId: string,
    bulkData: BulkInvitationRequest
  ): Promise<WorkspaceInvitation[]> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const response = await firstValueFrom(
        this.http.post<WorkspaceInvitation[]>(
          `${environment.apiBaseUrl}/workspace/v1/invitation/bulk`,
          bulkData
        ).pipe(catchError(this.handleError))
      );

      // Refresh invitations list
      await this.getInvitations(workspaceId);

      return response;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to create bulk invitations');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Resend invitation
   */
  async resendInvitation(workspaceId: string, invitationId: string): Promise<void> {
    this._loading.set(true);
    this._error.set(null);

    try {
      await firstValueFrom(
        this.http.post(`${environment.apiBaseUrl}/workspace/v1/invitation/${invitationId}/resend`, {})
          .pipe(catchError(this.handleError))
      );

      // Refresh invitations list
      await this.getInvitations(workspaceId);
    } catch (error: any) {
      this._error.set(error.message || 'Failed to resend invitation');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Cancel invitation
   */
  async cancelInvitation(workspaceId: string, invitationId: string): Promise<void> {
    this._loading.set(true);
    this._error.set(null);

    try {
      await firstValueFrom(
        this.http.delete(`${environment.apiBaseUrl}/workspace/v1/invitation/${invitationId}`)
          .pipe(catchError(this.handleError))
      );

      // Refresh invitations list
      await this.getInvitations(workspaceId);
    } catch (error: any) {
      this._error.set(error.message || 'Failed to cancel invitation');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Accept invitation (public endpoint)
   */
  async acceptInvitation(token: string): Promise<void> {
    this._loading.set(true);
    this._error.set(null);

    try {
      await firstValueFrom(
        this.http.post(`${environment.apiBaseUrl}/workspace/v1/invitation/${token}/accept`, {})
          .pipe(catchError(this.handleError))
      );
    } catch (error: any) {
      this._error.set(error.message || 'Failed to accept invitation');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Get invitation statistics
   */
  async getInvitationStatistics(workspaceId: string): Promise<InvitationStatistics> {
    try {
      const response = await firstValueFrom(
        this.http.get<InvitationStatistics>(`${environment.apiBaseUrl}/workspace/v1/invitation/statistics`)
          .pipe(catchError(this.handleError))
      );

      this._invitationStatistics.set(response);
      return response;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to load invitation statistics');
      throw error;
    }
  }

  // ========== UTILITY METHODS ==========

  /**
   * Get available roles for the workspace
   */
  getAvailableRoles(workspaceId: string): Observable<WorkspaceRole[]> {
    return this.http.get<WorkspaceRole[]>(`${environment.apiBaseUrl}/workspace/v1/member/roles`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Get available departments
   */
  getDepartments(workspaceId: string): Observable<string[]> {
    return this.http.get<string[]>(`${environment.apiBaseUrl}/workspace/v1/member/departments`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Export members data
   */
  exportMembers(
    workspaceId: string,
    format: 'CSV' | 'EXCEL' = 'CSV',
    filters?: MemberSearchFilters
  ): Observable<Blob> {
    let params = new HttpParams().set('format', format);

    if (filters) {
      if (filters.role) params = params.set('role', filters.role);
      if (filters.status) params = params.set('status', filters.status);
      if (filters.department) params = params.set('department', filters.department);
    }

    return this.http.get(`${environment.apiBaseUrl}/workspace/v1/member/export`, {
      params,
      responseType: 'blob'
    }).pipe(catchError(this.handleError));
  }

  /**
   * Bulk operations on members
   */
  async bulkUpdateMembers(
    workspaceId: string,
    memberIds: string[],
    updateData: Partial<UpdateMemberRequest>
  ): Promise<void> {
    this._loading.set(true);
    this._error.set(null);

    try {
      await firstValueFrom(
        this.http.put(`${environment.apiBaseUrl}/workspace/v1/member/bulk`, {
          member_ids: memberIds,
          ...updateData
        }).pipe(catchError(this.handleError))
      );

      // Refresh members list
      await this.getMembers(workspaceId);
    } catch (error: any) {
      this._error.set(error.message || 'Failed to update members');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Bulk remove members
   */
  async bulkRemoveMembers(workspaceId: string, memberIds: string[]): Promise<void> {
    this._loading.set(true);
    this._error.set(null);

    try {
      await firstValueFrom(
        this.http.request('delete', `${environment.apiBaseUrl}/workspace/v1/member/bulk`, {
          body: {member_ids: memberIds}
        }).pipe(catchError(this.handleError))
      );

      // Refresh members list
      await this.getMembers(workspaceId);
    } catch (error: any) {
      this._error.set(error.message || 'Failed to remove members');
      throw error;
    } finally {
      this._loading.set(false);
    }
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
    this._members.set([]);
    this._invitations.set([]);
    this._memberStatistics.set(null);
    this._invitationStatistics.set(null);
    this._error.set(null);
    this._loading.set(false);
  }

  /**
   * Get available workspace roles from backend
   */
  async getWorkspaceRoles(workspaceId: string): Promise<WorkspaceRoleResponse[]> {
    try {
      return await firstValueFrom(
        this.http.get<WorkspaceRoleResponse[]>(
          `${environment.apiBaseUrl}/workspace/v1/member/roles`
        ).pipe(
          map(apiResponse => apiResponse),
          catchError(this.handleError)
        )
      );
    } catch (error: any) {
      console.error('Failed to fetch workspace roles:', error);
      throw error;
    }
  }

  /**
   * Get available workspace permissions from backend
   */
  async getWorkspacePermissions(workspaceId: string): Promise<WorkspacePermissionResponse[]> {
    try {
      return await firstValueFrom(
        this.http.get<WorkspacePermissionResponse[]>(
          `${environment.apiBaseUrl}/workspace/v1/member/permissions`
        ).pipe(
          map(apiResponse => apiResponse),
          catchError(this.handleError)
        )
      );
    } catch (error: any) {
      console.error('Failed to fetch workspace permissions:', error);
      throw error;
    }
  }

  /**
   * Handle HTTP errors
   */
  private handleError(error: any): Observable<never> {
    console.error('Member Service Error:', error);
    let errorMessage = 'An unexpected error occurred';

    // Extract error message from API response structure
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
