import {Component, OnInit, inject, signal} from '@angular/core';
import {Router} from '@angular/router';
import {CommonModule, NgOptimizedImage} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatDividerModule} from '@angular/material/divider';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatListModule} from '@angular/material/list';
import {MatChipsModule} from '@angular/material/chips';
import {MatRippleModule} from '@angular/material/core';
import {MatMenuModule} from '@angular/material/menu';
import {MatDialog} from '@angular/material/dialog';
import {WorkspaceListItem, WorkspaceService} from '../../../core/services/workspace.service';
import {WorkspaceEditDialogComponent} from '../workspace-edit-dialog/workspace-edit-dialog.component';
import {InvitationService, InvitationResponse} from '../../../core/services/invitation.service';
import {PendingInvitationCardComponent} from '../../../shared/components/pending-invitation-card/pending-invitation-card.component';

@Component({
  selector: 'app-workspace-select',
  standalone: true,
  imports: [
    CommonModule,
    NgOptimizedImage,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatDividerModule,
    MatToolbarModule,
    MatListModule,
    MatChipsModule,
    MatRippleModule,
    MatMenuModule,
    PendingInvitationCardComponent
  ],
  templateUrl: './workspace-select.component.html',
  styleUrl: './workspace-select.component.scss'
})
export class WorkspaceSelectComponent implements OnInit {
  private workspaceService = inject(WorkspaceService);
  private invitationService = inject(InvitationService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  // Signals for reactive state management
  workspaces = signal<WorkspaceListItem[]>([]);
  pendingInvitations = signal<InvitationResponse[]>([]);
  isLoading = signal(false);
  isLoadingInvitations = signal(false);
  isSelecting = signal(false);
  selectedWorkspaceId = signal('');

  ngOnInit(): void {
    this.loadWorkspaces();
    this.loadPendingInvitations();
  }

  async loadWorkspaces(): Promise<void> {
    this.isLoading.set(true);

    try {
      const workspaces = await this.workspaceService.getUserWorkspaces();
      this.workspaces.set(workspaces);
      this.isLoading.set(false);

      // If no workspaces found, show create workspace option
      if (workspaces.length === 0) {
        this.showNoWorkspacesMessage();
      }
    } catch (error: any) {
      this.isLoading.set(false);
      console.error('Failed to load workspaces:', error);
      this.showError('Failed to load workspaces. Please try again.');
    }
  }

  async loadPendingInvitations(): Promise<void> {
    this.isLoadingInvitations.set(true);

    try {
      const invitations = await this.invitationService.getPendingInvitations();
      this.pendingInvitations.set(invitations);
    } catch (error: any) {
      console.error('Failed to load pending invitations:', error);
      // Don't show error for invitations - it's optional information
    } finally {
      this.isLoadingInvitations.set(false);
    }
  }

  selectWorkspace(workspace: WorkspaceListItem): void {
    if (this.isSelecting()) return;

    this.isSelecting.set(true);
    this.selectedWorkspaceId.set(workspace.id);
    localStorage.setItem('workspace_id', workspace.id);
    // Get full workspace details
    this.workspaceService.getWorkspaceById(workspace.id).subscribe({
      next: (fullWorkspace) => {
        // Set as current workspace
        this.workspaceService.setCurrentWorkspace(fullWorkspace);

        this.snackBar.open(`Switched to ${workspace.name}`, 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });

        // Navigate to workspace modules page using slug
        this.router.navigate(['/w', fullWorkspace.slug, 'modules']);
      },
      error: (error) => {
        this.isSelecting.set(false);
        this.selectedWorkspaceId.set('');
        console.error('Failed to select workspace:', error);
        this.showError('Failed to select workspace. Please try again.');
      }
    });
  }

  createNewWorkspace(): void {
    this.router.navigate(['/workspace/create']);
  }


  formatLastActivity(lastActivity?: string): string {
    if (!lastActivity) return 'No recent activity';

    const now = new Date();
    const activityDate = new Date(lastActivity);
    const diffMs = now.getTime() - activityDate.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMinutes = Math.floor(diffMs / (1000 * 60));

    if (diffDays > 0) {
      return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    } else if (diffHours > 0) {
      return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    } else if (diffMinutes > 0) {
      return `${diffMinutes} minute${diffMinutes > 1 ? 's' : ''} ago`;
    } else {
      return 'Just now';
    }
  }

  getWorkspaceTypeIcon(workspaceType: string): string {
    switch (workspaceType.toLowerCase()) {
      case 'business':
        return 'business';
      case 'personal':
        return 'person';
      case 'team':
        return 'group';
      case 'enterprise':
        return 'corporate_fare';
      default:
        return 'workspace_premium';
    }
  }

  getSubscriptionColor(plan: string): string {
    switch (plan.toLowerCase()) {
      case 'free':
        return 'accent';
      case 'basic':
        return 'primary';
      case 'premium':
        return 'warn';
      case 'enterprise':
        return '';
      default:
        return 'accent';
    }
  }

  private showNoWorkspacesMessage(): void {
    this.snackBar.open('No workspaces found. Create your first workspace to get started!', 'Create Workspace', {
      duration: 8000,
      panelClass: ['info-snackbar']
    }).onAction().subscribe(() => {
      this.createNewWorkspace();
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }

  editWorkspace(workspace: WorkspaceListItem): void {
    const dialogRef = this.dialog.open(WorkspaceEditDialogComponent, {
      width: '600px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      data: { workspace },
      disableClose: false,
      autoFocus: true
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Workspace was updated, refresh the list
        this.loadWorkspaces();

        // Update current workspace if it was the one edited
        const currentWorkspace = this.workspaceService.getCurrentWorkspace();
        if (currentWorkspace && currentWorkspace.id === workspace.id) {
          // Reload the full workspace details to update the current workspace
          this.workspaceService.getWorkspaceById(workspace.id).subscribe({
            next: (updatedWorkspace) => {
              this.workspaceService.setCurrentWorkspace(updatedWorkspace);
            }
          });
        }
      }
    });
  }

  viewWorkspaceSettings(workspace: WorkspaceListItem): void {
    // Navigate to workspace settings - we can implement this route later
    this.snackBar.open('Workspace settings coming soon!', 'Close', {
      duration: 3000,
      panelClass: ['info-snackbar']
    });
  }

  onInvitationAccepted(invitation: InvitationResponse): void {
    this.snackBar.open(`Successfully joined ${invitation.workspace_name}!`, 'Close', {
      duration: 5000,
      panelClass: ['success-snackbar']
    });

    // Remove the accepted invitation from the list
    const currentInvitations = this.pendingInvitations();
    const updatedInvitations = currentInvitations.filter(inv => inv.id !== invitation.id);
    this.pendingInvitations.set(updatedInvitations);

    // Refresh workspace list to include the newly joined workspace
    this.loadWorkspaces();
  }

  onInvitationRejected(invitation: InvitationResponse): void {
    this.snackBar.open(`Invitation from ${invitation.workspace_name} declined.`, 'Close', {
      duration: 3000,
      panelClass: ['info-snackbar']
    });

    // Remove the rejected invitation from the list
    const currentInvitations = this.pendingInvitations();
    const updatedInvitations = currentInvitations.filter(inv => inv.id !== invitation.id);
    this.pendingInvitations.set(updatedInvitations);
  }

  onInvitationError(errorMessage: string): void {
    this.showError(errorMessage);
  }
}
