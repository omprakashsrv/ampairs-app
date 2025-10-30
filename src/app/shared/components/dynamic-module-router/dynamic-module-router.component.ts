import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { DynamicNavigationService, DynamicModuleRoute } from '../../../core/services/dynamic-navigation.service';

@Component({
  selector: 'app-dynamic-module-router',
  standalone: true,
  imports: [
    CommonModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './dynamic-module-router.component.html',
  styleUrl: './dynamic-module-router.component.scss'
})
export class DynamicModuleRouterComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dynamicNavigation = inject(DynamicNavigationService);

  // Component signals
  moduleCode = signal<string>('');
  currentPath = signal<string>('');
  moduleRoute = signal<DynamicModuleRoute | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  ngOnInit() {
    this.initializeModule();
  }

  private async initializeModule() {
    try {
      // Get module code from route parameters
      const moduleCode = this.route.snapshot.paramMap.get('moduleCode');
      if (!moduleCode) {
        this.error.set('Module code not provided in route');
        this.loading.set(false);
        return;
      }

      this.moduleCode.set(moduleCode);

      // Load navigation modules if not already loaded
      if (this.dynamicNavigation.navigationRoutes().length === 0) {
        await this.dynamicNavigation.loadNavigationModules();
      }

      // Get the module route
      const moduleRoute = this.dynamicNavigation.getModuleRoute(moduleCode);
      if (!moduleRoute) {
        this.error.set(`Module "${moduleCode}" is not installed or not available in this workspace.`);
        this.loading.set(false);
        return;
      }

      this.moduleRoute.set(moduleRoute);

      // Get current sub-path from URL
      this.updateCurrentPath();

      // Navigate to default menu item if we're at the module root
      if (this.currentPath() === '') {
        const defaultMenuItem = moduleRoute.menuItems.find(item => item.isDefault) || moduleRoute.menuItems[0];
        if (defaultMenuItem) {
          this.navigateToMenuItem(defaultMenuItem, true);
          return;
        }
      }

      this.loading.set(false);

    } catch (error: any) {
      console.error('Error initializing module:', error);
      this.error.set('Failed to load module. Please try again.');
      this.loading.set(false);
    }
  }

  private updateCurrentPath() {
    const url = this.router.url;
    const moduleCode = this.moduleCode();
    const modulesIndex = url.indexOf(`/modules/${moduleCode}`);

    if (modulesIndex !== -1) {
      const afterModule = url.substring(modulesIndex + `/modules/${moduleCode}`.length);
      this.currentPath.set(afterModule || '');
    }
  }

  navigateToMenuItem(menuItem: any, replaceUrl = false) {
    const fullPath = this.dynamicNavigation.getFullRoutePath(menuItem);

    if (replaceUrl) {
      this.router.navigateByUrl(fullPath, { replaceUrl: true });
    } else {
      this.router.navigateByUrl(fullPath);
    }
  }

  isActiveMenuItem(routePath: string): boolean {
    return this.currentPath() === routePath;
  }

  goToModules() {
    this.router.navigate(['../../'], { relativeTo: this.route });
  }
}