import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {MatDialog, MatDialogModule} from '@angular/material/dialog';
import {MatChipsModule} from '@angular/material/chips';
import {MatTooltipModule} from '@angular/material/tooltip';
import {AuthService, DeviceSession} from '../../../core/services/auth.service';
import {DeviceService} from '../../../core/services/device.service';
import {DeviceDebugDialogComponent} from '../device-debug-dialog/device-debug-dialog.component';

@Component({
  selector: 'app-device-management',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatChipsModule,
    MatTooltipModule
  ],
  templateUrl: './device-management.component.html',
  styleUrl: './device-management.component.scss'
})
export class DeviceManagementComponent implements OnInit {
  deviceSessions: DeviceSession[] = [];
  isLoading = false;
  isLoggingOut = false;

  constructor(
    private authService: AuthService,
    private deviceService: DeviceService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {
  }

  ngOnInit(): void {
    this.loadDeviceSessions();
  }

  loadDeviceSessions(): void {
    this.isLoading = true;
    this.authService.getDeviceSessions().subscribe({
      next: (sessions) => {
        this.deviceSessions = sessions;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load device sessions:', error);
        this.showError('Failed to load device sessions');
        this.isLoading = false;
      }
    });
  }

  refreshDevices(): void {
    this.loadDeviceSessions();
  }

  logoutDevice(deviceId: string): void {
    const device = this.deviceSessions.find(d => d.device_id === deviceId);
    if (!device) return;

    if (confirm(`Are you sure you want to logout from "${device.device_name}"?`)) {
      this.isLoggingOut = true;
      this.authService.logoutDevice(deviceId).subscribe({
        next: () => {
          this.showSuccess(`Successfully logged out from ${device.device_name}`);
          this.loadDeviceSessions(); // Refresh the list
          this.isLoggingOut = false;
        },
        error: (error) => {
          console.error('Failed to logout device:', error);
          this.showError('Failed to logout from device');
          this.isLoggingOut = false;
        }
      });
    }
  }

  logoutAllDevices(): void {
    if (confirm('Are you sure you want to logout from all devices? This will end all your sessions and you will need to login again.')) {
      this.isLoggingOut = true;
      this.authService.logoutAllDevices().subscribe({
        next: () => {
          this.showSuccess('Successfully logged out from all devices');
          // User will be redirected to login page by the service
        },
        error: (error) => {
          console.error('Failed to logout all devices:', error);
          this.showError('Failed to logout from all devices');
          this.isLoggingOut = false;
        }
      });
    }
  }

  getDeviceIcon(deviceType: string): string {
    switch (deviceType.toLowerCase()) {
      case 'mobile':
        return 'smartphone';
      case 'tablet':
        return 'tablet';
      case 'desktop':
        return 'computer';
      default:
        return 'device_unknown';
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMinutes / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMinutes < 1) {
      return 'Just now';
    } else if (diffMinutes < 60) {
      return `${diffMinutes} minute${diffMinutes > 1 ? 's' : ''} ago`;
    } else if (diffHours < 24) {
      return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    } else if (diffDays < 7) {
      return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    } else {
      return date.toLocaleDateString();
    }
  }

  showDebugInfo(): void {
    const debugInfo = {
      currentDeviceInfo: this.deviceService.getDeviceInfo(),
      platformDebugInfo: this.deviceService.getPlatformDebugInfo()
    };

    this.dialog.open(DeviceDebugDialogComponent, {
      width: '600px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      data: debugInfo
    });
  }

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
