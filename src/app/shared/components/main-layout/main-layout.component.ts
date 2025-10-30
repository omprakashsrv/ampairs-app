import {ChangeDetectionStrategy, Component, inject, ViewChild, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterOutlet} from '@angular/router';
import {MatSidenavModule, MatDrawer} from '@angular/material/sidenav';
import {MatListModule} from '@angular/material/list';
import {MatIconModule} from '@angular/material/icon';
import {MatDividerModule} from '@angular/material/divider';
import {MatButtonModule} from '@angular/material/button';
import {AppHeaderComponent} from '../app-header/app-header.component';
import {DynamicNavigationService} from '../../../core/services/dynamic-navigation.service';
import {Router} from '@angular/router';
import {WorkspaceService} from '../../../core/services/workspace.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatDividerModule,
    MatButtonModule,
    AppHeaderComponent
  ],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MainLayoutComponent implements OnInit {
  private dynamicNavigation = inject(DynamicNavigationService);
  private router = inject(Router);
  private workspaceService = inject(WorkspaceService);

  @ViewChild('drawer') drawer!: MatDrawer;

  // Signal-based state
  readonly navigationRoutes = this.dynamicNavigation.navigationRoutes;
  readonly currentWorkspace = this.workspaceService.currentWorkspace;

  async ngOnInit() {
    // Load navigation modules when component initializes
    if (this.navigationRoutes().length === 0) {
      await this.dynamicNavigation.loadNavigationModules();
    }
  }

  toggleSidenav(): void {
    this.drawer?.toggle();
  }

  navigateToMenuItem(moduleCode: string, routePath: string): void {
    const workspace = this.currentWorkspace();
    if (workspace) {
      const fullPath = `/w/${workspace.slug}/modules/${moduleCode}${routePath}`;
      this.router.navigateByUrl(fullPath);

      // Close sidenav on mobile after navigation
      if (this.drawer?.mode === 'over') {
        this.drawer.close();
      }
    }
  }

  shouldShowSidenav(): boolean {
    const url = this.router.url;
    return url.includes('/home') || url.includes('/w/');
  }

  getSidenavMode(): 'side' | 'over' {
    return window.innerWidth > 768 ? 'side' : 'over';
  }

  getSidenavOpened(): boolean {
    return false; // Hidden by default
  }
}