import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
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
import {MatDialog, MatDialogModule} from '@angular/material/dialog';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatDividerModule} from '@angular/material/divider';
import {MatTabsModule} from '@angular/material/tabs';

import {WorkspaceService} from '../../core/services/workspace.service';
import {MemberService} from '../../core/services/member.service';
import {
  INVITATION_STATUS_COLORS,
  InvitationSearchFilters,
  InvitationStatus,
  MemberSearchFilters,
  ROLE_DISPLAY_NAMES,
  WorkspaceInvitation,
  WorkspaceMemberListItem,
  WorkspaceRole
} from '../../core/models/member.interface';
import {MemberInviteDialogComponent} from './member-invite-dialog/member-invite-dialog.component';
import {MemberEditDialogComponent} from './member-edit-dialog/member-edit-dialog.component';

@Component({
  selector: 'app-members',
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
    MatTabsModule
  ],
  templateUrl: './members.component.html',
  styleUrl: './members.component.scss'
})
export class MembersComponent implements OnInit {
  private workspaceService = inject(WorkspaceService);
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

  // Member management
  members = this.memberService.members;
  memberLoading = this.memberService.loading;
  memberError = this.memberService.error;
  memberStatistics = this.memberService.memberStatistics;

  // Invitation management
  invitations = this.memberService.invitations;
  pendingInvitations = this.memberService.pendingInvitations;
  invitationStatistics = this.memberService.invitationStatistics;

  // Table configuration
  memberColumns = ['member', 'role', 'status', 'lastActivity', 'actions'];
  invitationColumns = ['email', 'role', 'status', 'invitedBy', 'expiresAt', 'actions'];

  // Filters
  memberFilters: MemberSearchFilters = {};
  invitationFilters: InvitationSearchFilters = {};
  searchForm: FormGroup;

  // Pagination
  pageSize = 20;
  currentPage = 0;

  // Constants for template
  readonly WorkspaceRole = WorkspaceRole;
  readonly InvitationStatus = InvitationStatus;
  readonly ROLE_DISPLAY_NAMES = ROLE_DISPLAY_NAMES;
  readonly INVITATION_STATUS_COLORS = INVITATION_STATUS_COLORS;

  // Computed properties

  constructor() {
    this.searchForm = this.fb.group({
      searchQuery: [''],
      roleFilter: [''],
      statusFilter: [''],
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
        this.loadMembers(),
        this.loadInvitations(),
        this.loadStatistics()
      ]);
    } catch (error) {
      console.error('Failed to load member data:', error);
      this.showError('Failed to load member information');
    }
  }

  async loadMembers(): Promise<void> {
    const wId = this.workspaceId();
    if (!wId) return;

    try {
      await this.memberService.getMembers(
        wId,
        this.currentPage,
        this.pageSize,
        'joinedAt',
        'desc',
        this.memberFilters
      );
    } catch (error: any) {
      this.showError(error.message || 'Failed to load members');
    }
  }

  async loadInvitations(): Promise<void> {
    const wId = this.workspaceId();
    if (!wId) return;

    try {
      await this.memberService.getInvitations(
        wId,
        this.currentPage,
        this.pageSize,
        this.invitationFilters
      );
    } catch (error: any) {
      this.showError(error.message || 'Failed to load invitations');
    }
  }

  async loadStatistics(): Promise<void> {
    const wId = this.workspaceId();
    if (!wId) return;

    try {
      await Promise.all([
        this.memberService.getMemberStatistics(wId),
        this.memberService.getInvitationStatistics(wId)
      ]);
    } catch (error: any) {
      console.error('Failed to load statistics:', error);
      // Don't show error for statistics as it's not critical
    }
  }

  applyFilters(): void {
    const formValue = this.searchForm.value;

    this.memberFilters = {
      search_query: formValue.searchQuery || undefined,
      role: formValue.roleFilter || undefined,
      status: formValue.statusFilter || undefined,
    };

    this.invitationFilters = {
      search_query: formValue.searchQuery || undefined,
      role: formValue.roleFilter || undefined,
      status: formValue.statusFilter || undefined
    };

    // Reset pagination and reload
    this.currentPage = 0;
    this.loadData();
  }

  clearFilters(): void {
    this.searchForm.reset();
    this.memberFilters = {};
    this.invitationFilters = {};
    this.currentPage = 0;
    this.loadData();
  }

  onTabChange(tabIndex: number): void {
    this.selectedTab.set(tabIndex);
  }

  // ========== MEMBER ACTIONS ==========

  openInviteDialog(): void {
    const workspace = this.currentWorkspace();
    if (!workspace) return;

    const dialogRef = this.dialog.open(MemberInviteDialogComponent, {
      width: '540px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      data: {
        workspaceId: workspace.id,
        workspaceName: workspace.name
      },
      disableClose: false,
      autoFocus: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Invitation was sent, refresh data
        this.loadData();
      }
    });
  }

  async removeMember(member: WorkspaceMemberListItem): Promise<void> {
    const confirmed = confirm(`Are you sure you want to remove ${member.user.display_name} from this workspace?`);
    if (!confirmed) return;

    const wId = this.workspaceId();
    if (!wId) return;

    try {
      await this.memberService.removeMember(wId, member.id);
      this.showSuccess(`${member.user.display_name} has been removed from the workspace`);
      await this.loadMembers();
    } catch (error: any) {
      this.showError(error.message || 'Failed to remove member');
    }
  }

  editMember(member: WorkspaceMemberListItem): void {
    const dialogRef = this.dialog.open(MemberEditDialogComponent, {
      width: '500px',
      data: {
        member: member,
        currentWorkspace: this.currentWorkspace()
      }
    });

    dialogRef.afterClosed().subscribe(async result => {
      if (result) {
        await this.loadMembers();
      }
    });
  }


  // ========== INVITATION ACTIONS ==========

  async resendInvitation(invitation: WorkspaceInvitation): Promise<void> {
    const wId = this.workspaceId();
    if (!wId) return;

    try {
      await this.memberService.resendInvitation(wId, invitation.id);
      this.showSuccess(`Invitation resent to ${invitation.email}`);
      await this.loadInvitations();
    } catch (error: any) {
      this.showError(error.message || 'Failed to resend invitation');
    }
  }

  async cancelInvitation(invitation: WorkspaceInvitation): Promise<void> {
    const confirmed = confirm(`Are you sure you want to cancel the invitation to ${invitation.email}?`);
    if (!confirmed) return;

    const wId = this.workspaceId();
    if (!wId) return;

    try {
      await this.memberService.cancelInvitation(wId, invitation.id);
      this.showSuccess(`Invitation to ${invitation.email} has been cancelled`);
      await this.loadInvitations();
    } catch (error: any) {
      this.showError(error.message || 'Failed to cancel invitation');
    }
  }


  // ========== UTILITY METHODS ==========

  formatDate(dateString?: string): string {
    if (!dateString) return 'Never';
    return new Date(dateString).toLocaleDateString();
  }

  formatRelativeTime(dateString?: string): string {
    if (!dateString) return 'Never';

    const now = new Date();
    const date = new Date(dateString);
    const diffMs = now.getTime() - date.getTime();
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffHours / 24);

    if (diffDays > 7) {
      return date.toLocaleDateString();
    } else if (diffDays > 0) {
      return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    } else if (diffHours > 0) {
      return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    } else {
      return 'Just now';
    }
  }

  getRoleIcon(role: WorkspaceRole): string {
    switch (role) {
      case WorkspaceRole.OWNER:
        return 'crown';
      case WorkspaceRole.ADMIN:
        return 'admin_panel_settings';
      case WorkspaceRole.MANAGER:
        return 'supervisor_account';
      case WorkspaceRole.MEMBER:
        return 'person';
      case WorkspaceRole.GUEST:
        return 'person_outline';
      case WorkspaceRole.VIEWER:
        return 'visibility';
      default:
        return 'person';
    }
  }

  getRoleDisplayName(role: WorkspaceRole): string {
    return ROLE_DISPLAY_NAMES[role] || role;
  }

  getInvitationStatusColor(status: InvitationStatus): string {
    return INVITATION_STATUS_COLORS[status] || '';
  }

  getInvitationStatusIcon(status: InvitationStatus): string {
    switch (status) {
      case InvitationStatus.PENDING:
        return 'schedule';
      case InvitationStatus.ACCEPTED:
        return 'check_circle';
      case InvitationStatus.EXPIRED:
        return 'schedule';
      case InvitationStatus.CANCELLED:
        return 'cancel';
      case InvitationStatus.REJECTED:
        return 'close';
      default:
        return 'help';
    }
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

}
