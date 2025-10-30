import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';

import { InvitationService, InvitationResponse } from '../../core/services/invitation.service';
import { AuthService } from '../../core/services/auth.service';

type InvitationState = 'loading' | 'login_required' | 'ready_to_accept' | 'accepting' | 'accepted' | 'rejected' | 'error';

@Component({
  selector: 'app-accept-invitation',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatChipsModule
  ],
  templateUrl: './accept-invitation.component.html',
  styleUrl: './accept-invitation.component.scss'
})
export class AcceptInvitationComponent implements OnInit {
  private route = inject(ActivatedRoute);
  router = inject(Router); // Make public for template access
  private invitationService = inject(InvitationService);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);

  // Component state
  invitationToken = signal<string | null>(null);
  state = signal<InvitationState>('loading');
  invitation = signal<InvitationResponse | null>(null);
  errorMessage = signal<string | null>(null);

  // Auth state
  isAuthenticated = this.authService.isAuthenticated;

  async ngOnInit() {
    // Get invitation token from route
    const token = this.route.snapshot.paramMap.get('token');

    if (!token || !this.invitationService.isValidInvitationToken(token)) {
      this.state.set('error');
      this.errorMessage.set('Invalid invitation link. Please check the link in your email.');
      return;
    }

    this.invitationToken.set(token);

    // Check authentication status
    if (!this.isAuthenticated()) {
      this.state.set('login_required');
      // Store the invitation token for after login
      sessionStorage.setItem('pending_invitation_token', token);
      return;
    }

    // User is authenticated, try to get invitation details
    await this.loadInvitationDetails();
  }

  async loadInvitationDetails() {
    const token = this.invitationToken();
    if (!token) return;

    this.state.set('loading');

    try {
      // Since there's no public endpoint, we'll try to accept directly
      // In a real implementation, you'd want a public endpoint to get details first
      this.state.set('ready_to_accept');
    } catch (error: any) {
      this.state.set('error');
      this.errorMessage.set(error.message || 'Failed to load invitation details');
    }
  }

  async acceptInvitation() {
    const token = this.invitationToken();
    if (!token) return;

    this.state.set('accepting');

    try {
      const invitation = await this.invitationService.acceptInvitation(token);
      this.invitation.set(invitation);
      this.state.set('accepted');

      this.snackBar.open(
        `Welcome to ${invitation.workspace_name || 'the workspace'}!`,
        'Close',
        { duration: 5000 }
      );

      // Clear stored token
      sessionStorage.removeItem('pending_invitation_token');

      // Redirect to workspace after a delay
      setTimeout(() => {
        this.router.navigate(['/workspaces']);
      }, 2000);

    } catch (error: any) {
      this.state.set('error');
      this.errorMessage.set(error.message || 'Failed to accept invitation');
      this.snackBar.open(error.message || 'Failed to accept invitation', 'Close', { duration: 5000 });
    }
  }

  async rejectInvitation() {
    const token = this.invitationToken();
    if (!token) return;

    this.state.set('accepting'); // Reuse accepting state for loading

    try {
      const invitation = await this.invitationService.rejectInvitation(token);
      this.invitation.set(invitation);
      this.state.set('rejected');

      this.snackBar.open('Invitation rejected', 'Close', { duration: 3000 });

      // Clear stored token
      sessionStorage.removeItem('pending_invitation_token');

      // Redirect to home after a delay
      setTimeout(() => {
        this.router.navigate(['/']);
      }, 2000);

    } catch (error: any) {
      this.state.set('error');
      this.errorMessage.set(error.message || 'Failed to reject invitation');
      this.snackBar.open(error.message || 'Failed to reject invitation', 'Close', { duration: 5000 });
    }
  }

  navigateToLogin() {
    // Navigate to login with return URL
    const token = this.invitationToken();
    const returnUrl = `/accept-invitation/${token}`;
    this.router.navigate(['/auth/login'], {
      queryParams: { returnUrl }
    });
  }

  getRoleDisplayName(role: string): string {
    return this.invitationService.getRoleDisplayName(role);
  }

  getRoleDescription(role: string): string {
    return this.invitationService.getRoleDescription(role);
  }

  getRoleColor(role: string): string {
    const colorMap: Record<string, string> = {
      'ADMIN': 'warn',
      'MANAGER': 'primary',
      'MEMBER': 'accent',
      'VIEWER': ''
    };
    return colorMap[role] || '';
  }
}