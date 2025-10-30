import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal, Output, EventEmitter} from '@angular/core';
import {ActivatedRoute, NavigationEnd, Router} from '@angular/router';
import {CommonModule, Location} from '@angular/common';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';
import {MatDividerModule} from '@angular/material/divider';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatDialog} from '@angular/material/dialog';
import {filter} from 'rxjs/operators';
import {AuthService} from '../../../core/services/auth.service';
import {WorkspaceService} from '../../../core/services/workspace.service';
import {ThemeService} from '../../../core/services/theme.service';
import {DeviceManagementComponent} from '../device-management/device-management.component';
import {ThemeSettingsComponent} from '../theme-settings/theme-settings.component';
import {MatTooltip} from "@angular/material/tooltip";
import {DynamicNavigationService} from '../../../core/services/dynamic-navigation.service';

export interface HeaderContext {
  showWorkspaceInfo: boolean;
  showBackButton: boolean;
  showSidenavToggle: boolean;
  backAction?: () => void;
  title?: string;
  titleIcon?: string;
  showStepIndicator?: boolean;
  stepInfo?: {
    current: number;
    total: number;
  };
}

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDividerModule,
    MatProgressBarModule,
    MatTooltip,
  ],
  templateUrl: './app-header.component.html',
  styleUrl: './app-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppHeaderComponent implements OnInit {
  private authService = inject(AuthService);
  private workspaceService = inject(WorkspaceService);
  private themeService = inject(ThemeService);
  private router = inject(Router);
  private location = inject(Location);
  private dialog = inject(MatDialog);
  private route = inject(ActivatedRoute);
  private dynamicNavigation = inject(DynamicNavigationService);

  // Output for sidenav toggle
  @Output() toggleSidenav = new EventEmitter<void>();

  // Signal-based state
  readonly currentUser = this.authService.currentUser;
  readonly currentWorkspace = this.workspaceService.currentWorkspace;
  readonly isAuthenticated = this.authService.isAuthenticated;
  readonly themeMode = this.themeService.themeMode;
  readonly isDarkMode = this.themeService.isDarkMode;
  readonly navigationRoutes = this.dynamicNavigation.navigationRoutes;

  // Reactive header context using signals
  private _currentUrl = signal('');
  readonly headerContext = computed(() => this.getHeaderContext(this._currentUrl()));

  ngOnInit() {
    // Update URL signal when route changes
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this._currentUrl.set(this.router.url);
    });

    // Set initial URL
    this._currentUrl.set(this.router.url);
  }

  private getHeaderContext(url: string = this.router.url): HeaderContext {

    // Home/Dashboard routes - show full workspace info and sidenav toggle
    if (url.includes('/home') || url.includes('/w/')) {
      return {
        showWorkspaceInfo: true,
        showBackButton: false,
        showSidenavToggle: true
      };
    }

    // Workspace selection - minimal header
    if (url.includes('/workspaces')) {
      return {
        showWorkspaceInfo: false,
        showBackButton: false,
        showSidenavToggle: false,
        title: 'Select Workspace',
        titleIcon: 'business'
      };
    }

    // Workspace creation - back button with stepper
    if (url.includes('/workspace/create')) {
      return {
        showWorkspaceInfo: false,
        showBackButton: true,
        showSidenavToggle: false,
        backAction: () => this.router.navigate(['/workspaces']),
        title: 'Create Workspace',
        titleIcon: 'add_business',
        showStepIndicator: true
      };
    }

    // Profile editing
    if (url.includes('/complete-profile')) {
      return {
        showWorkspaceInfo: false,
        showBackButton: true,
        showSidenavToggle: false,
        backAction: () => this.location.back(),
        title: 'Complete Profile',
        titleIcon: 'person'
      };
    }

    // Default context
    return {
      showWorkspaceInfo: true,
      showBackButton: false,
      showSidenavToggle: false
    };
  }

  // Workspace management actions
  manageMembers(): void {
    const workspace = this.currentWorkspace();
    if (workspace) {
      this.router.navigate(['/w', workspace.slug, 'members']);
    }
  }

  manageRoles(): void {
    const workspace = this.currentWorkspace();
    if (workspace) {
      this.router.navigate(['/w', workspace.slug, 'roles']);
    }
  }

  manageModules(): void {
    const workspace = this.currentWorkspace();
    if (workspace) {
      this.router.navigate(['/w', workspace.slug, 'modules']);
    }
  }

  switchWorkspace(): void {
    this.router.navigate(['/workspaces']);
  }

  // User profile actions
  editProfile(): void {
    this.router.navigate(['/complete-profile'], {
      queryParams: {mode: 'edit'}
    });
  }

  viewSettings(): void {
    const workspace = this.currentWorkspace();
    if (workspace) {
      this.router.navigate(['/w', workspace.slug, 'settings']);
    }
  }

  viewDevices(): void {
    this.dialog.open(DeviceManagementComponent, {
      width: '600px',
      maxHeight: '80vh'
    });
  }

  // Theme management
  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  getThemeModeIcon(): string {
    const mode = this.themeMode();
    switch (mode) {
      case 'system': return 'brightness_auto';
      case 'light': return 'light_mode';
      case 'dark': return 'dark_mode';
      default: return 'brightness_auto';
    }
  }

  getThemeModeLabel(): string {
    const mode = this.themeMode();
    switch (mode) {
      case 'system': return 'System';
      case 'light': return 'Light';
      case 'dark': return 'Dark';
      default: return 'System';
    }
  }

  openThemeSettings(): void {
    this.dialog.open(ThemeSettingsComponent, {
      width: '500px',
      maxHeight: '80vh'
    });
  }

  // Navigation actions
  goBack(): void {
    const context = this.headerContext();
    if (context.backAction) {
      context.backAction();
    } else {
      this.location.back();
    }
  }

  // Authentication
  async logout(): Promise<void> {
    try {
      await this.authService.logout();
      this.router.navigate(['/login']);
    } catch (error) {
      console.error('Logout failed:', error);
    }
  }

  // Utility methods
  formatUserName(user: any): string {
    return user?.full_name || user?.first_name || 'User';
  }

  shouldShowUserMenu(): boolean {
    return this.isAuthenticated() === true;
  }

  shouldShowWorkspaceMenu(): boolean {
    return this.headerContext().showWorkspaceInfo && this.currentWorkspace() !== null;
  }

  shouldShowThemeControls(): boolean {
    // Show theme controls only on authenticated pages
    return this.isAuthenticated() === true;
  }

  shouldShowSidenavToggle(): boolean {
    return this.headerContext().showSidenavToggle && this.isAuthenticated() === true;
  }

  onToggleSidenav(): void {
    this.toggleSidenav.emit();
  }

  // Dynamic step indicator (for workspace creation)
  updateStepIndicator(current: number, total: number): void {
    // Note: This method is now primarily for external API compatibility
    // The step indicator is managed through the route-based context
    const context = this.headerContext();
    if (context.showStepIndicator) {
      // Step info is now managed through URL-based context detection
      console.log(`Step ${current} of ${total}`);
    }
  }

}
