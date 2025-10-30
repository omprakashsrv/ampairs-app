import { Component, inject, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { InvitationResponse, InvitationService } from '../../../core/services/invitation.service';

@Component({
  selector: 'app-pending-invitation-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './pending-invitation-card.component.html',
  styleUrl: './pending-invitation-card.component.scss'
})
export class PendingInvitationCardComponent {
  private invitationService = inject(InvitationService);

  // Inputs
  invitation = input.required<InvitationResponse>();
  isProcessing = input<boolean>(false);

  // Outputs
  accepted = output<InvitationResponse>();
  rejected = output<InvitationResponse>();
  error = output<string>();

  async acceptInvitation() {
    const invitation = this.invitation();

    try {
      const result = await this.invitationService.acceptInvitation(invitation.id);
      this.accepted.emit(result);
    } catch (error: any) {
      this.error.emit(error.message || 'Failed to accept invitation');
    }
  }

  async rejectInvitation() {
    const invitation = this.invitation();

    try {
      const result = await this.invitationService.rejectInvitation(invitation.id);
      this.rejected.emit(result);
    } catch (error: any) {
      this.error.emit(error.message || 'Failed to reject invitation');
    }
  }

  getRoleDisplayName(role: string): string {
    return this.invitationService.getRoleDisplayName(role);
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

  isExpiringSoon(): boolean {
    const invitation = this.invitation();
    return invitation.days_until_expiry !== null &&
           invitation.days_until_expiry !== undefined &&
           invitation.days_until_expiry <= 2 &&
           invitation.days_until_expiry > 0;
  }

  isExpired(): boolean {
    return this.invitation().is_expired;
  }

  getExpiryText(): string {
    const invitation = this.invitation();

    if (invitation.is_expired) {
      return 'Expired';
    }

    if (invitation.days_until_expiry === null || invitation.days_until_expiry === undefined) {
      return '';
    }

    if (invitation.days_until_expiry === 0) {
      return 'Expires today';
    }

    if (invitation.days_until_expiry === 1) {
      return 'Expires tomorrow';
    }

    return `Expires in ${invitation.days_until_expiry} days`;
  }

  getExpiryIcon(): string {
    const invitation = this.invitation();

    if (invitation.is_expired) {
      return 'error';
    }

    if (invitation.days_until_expiry !== null && invitation.days_until_expiry !== undefined && invitation.days_until_expiry <= 2) {
      return 'warning';
    }

    return 'schedule';
  }
}