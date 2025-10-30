import {inject, Injectable, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {firstValueFrom, Observable} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {environment} from '../../../environments/environment';

export interface InvitationResponse {
  id: string;
  workspace_id: string;
  workspace_name?: string;
  email?: string;
  phone?: string;
  country_code?: number;
  role: 'ADMIN' | 'MANAGER' | 'MEMBER' | 'VIEWER' | 'OWNER' | 'GUEST';
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED' | 'CANCELLED';
  token: string;
  message?: string;
  invited_by?: string;
  inviter_name?: string;
  expires_at: string;
  accepted_at?: string;
  rejected_at?: string;
  cancelled_at?: string;
  cancelled_by?: string;
  cancellation_reason?: string;
  send_count: number;
  last_sent_at?: string;
  created_at: string;
  updated_at: string;
  is_expired: boolean;
  days_until_expiry?: number;
}

export interface PublicInvitationDetails {
  workspace_name: string;
  workspace_description?: string;
  workspace_avatar_url?: string;
  inviter_name: string;
  role: 'ADMIN' | 'MANAGER' | 'MEMBER' | 'VIEWER';
  expires_at: string;
  is_expired: boolean;
  is_valid: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class InvitationService {
  private readonly INVITATION_API_URL = `${environment.apiBaseUrl}/user/v1/invitation`;
  private http = inject(HttpClient);

  // Signal-based state management
  private _loading = signal(false);
  private _error = signal<string | null>(null);
  private _currentInvitation = signal<InvitationResponse | null>(null);

  // Public readonly signals
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly currentInvitation = this._currentInvitation.asReadonly();

  /**
   * Get pending invitations for the current user
   * Uses JWT token authentication - no workspace context required
   */
  async getPendingInvitations(): Promise<InvitationResponse[]> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const invitations = await firstValueFrom(
        this.http.get<InvitationResponse[]>(`${this.INVITATION_API_URL}/pending`)
          .pipe(catchError(this.handleError.bind(this)))
      );

      return invitations;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to load pending invitations');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Get invitation details by token (public endpoint - no auth required)
   * This should be called when user clicks invitation link before login
   */
  async getInvitationDetails(token: string): Promise<PublicInvitationDetails> {
    this._loading.set(true);
    this._error.set(null);

    try {
      // Note: This endpoint doesn't exist in backend yet - would need to be added
      // For now, we'll handle this in the accept flow
      const details = await firstValueFrom(
        this.http.get<PublicInvitationDetails>(`${this.INVITATION_API_URL}/${token}/details`)
          .pipe(catchError(this.handleError.bind(this)))
      );

      return details;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to load invitation details');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Accept invitation using invitation ID (requires JWT authentication)
   */
  async acceptInvitation(invitationId: string): Promise<InvitationResponse> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const invitation = await firstValueFrom(
        this.http.post<InvitationResponse>(`${this.INVITATION_API_URL}/${invitationId}/accept`, {})
          .pipe(catchError(this.handleError.bind(this)))
      );

      this._currentInvitation.set(invitation);
      return invitation;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to accept invitation');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Reject invitation using invitation ID (requires JWT authentication)
   */
  async rejectInvitation(invitationId: string, reason?: string): Promise<InvitationResponse> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const body = reason ? {reason} : {};
      const invitation = await firstValueFrom(
        this.http.post<InvitationResponse>(`${this.INVITATION_API_URL}/${invitationId}/reject`, body)
          .pipe(catchError(this.handleError.bind(this)))
      );

      this._currentInvitation.set(invitation);
      return invitation;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to reject invitation');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Clear current invitation state
   */
  clearInvitation(): void {
    this._currentInvitation.set(null);
    this._error.set(null);
  }

  /**
   * Check if invitation token is valid format
   */
  isValidInvitationToken(token: string): boolean {
    // Basic validation - invitation tokens typically start with 'inv_'
    return !!token && token.length > 10 && token.startsWith('inv_');
  }

  /**
   * Get role display name
   */
  getRoleDisplayName(role: string): string {
    const roleMap: Record<string, string> = {
      'ADMIN': 'Administrator',
      'MANAGER': 'Manager',
      'MEMBER': 'Member',
      'VIEWER': 'Viewer'
    };
    return roleMap[role] || role;
  }

  /**
   * Get role description
   */
  getRoleDescription(role: string): string {
    const descriptionMap: Record<string, string> = {
      'ADMIN': 'Full access to manage workspace settings, members, and content',
      'MANAGER': 'Can manage teams, projects, and workspace content',
      'MEMBER': 'Can access and contribute to workspace content',
      'VIEWER': 'Can view workspace content with limited editing permissions'
    };
    return descriptionMap[role] || 'Basic workspace access';
  }

  /**
   * Handle HTTP errors
   */
  private handleError(error: any): Observable<never> {
    console.error('Invitation Service Error:', error);
    let errorMessage = 'An unexpected error occurred';

    if (error.error && error.error.error && error.error.error.message) {
      errorMessage = error.error.error.message;
    } else if (error.error && error.error.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }

    // Handle specific invitation errors
    if (error.status === 404) {
      errorMessage = 'Invitation not found or has expired';
    } else if (error.status === 409) {
      errorMessage = 'You are already a member of this workspace';
    } else if (error.status === 410) {
      errorMessage = 'This invitation has expired';
    } else if (error.status === 401) {
      errorMessage = 'You must be logged in to accept this invitation';
    }

    throw new Error(errorMessage);
  }
}
