import { Injectable, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { WorkspaceModuleService, InstalledModule } from './workspace-module.service';
import { WorkspaceService } from './workspace.service';

export interface DynamicModuleRoute {
  moduleCode: string;
  displayName: string;
  basePath: string;
  icon: string;
  primaryColor: string;
  navigationIndex: number;
  menuItems: ModuleMenuItem[];
}

export interface ModuleMenuItem {
  id: string;
  label: string;
  routePath: string;
  icon: string;
  order: number;
  isDefault: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class DynamicNavigationService {
  private router = inject(Router);
  private workspaceModuleService = inject(WorkspaceModuleService);
  private workspaceService = inject(WorkspaceService);

  // Signal-based state management
  private _installedModules = signal<InstalledModule[]>([]);
  private _loading = signal(false);
  private _error = signal<string | null>(null);

  // Public readonly signals
  readonly installedModules = this._installedModules.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  // Computed navigation routes
  readonly navigationRoutes = computed(() =>
    this._installedModules()
      .filter(module => module.status === 'ACTIVE' && module.enabled)
      .map(module => this.mapToNavigationRoute(module))
      .sort((a, b) => a.navigationIndex - b.navigationIndex)
  );

  /**
   * Load and process installed modules for navigation
   */
  async loadNavigationModules(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const modules = await this.workspaceModuleService.getInstalledModules();
      this._installedModules.set(modules);
    } catch (error: any) {
      this._error.set(error.message || 'Failed to load navigation modules');
      console.error('Failed to load navigation modules:', error);
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Get full route path for a module menu item
   */
  getFullRoutePath(menuItem: ModuleMenuItem): string {
    const currentWorkspace = this.workspaceService.currentWorkspace();
    if (!currentWorkspace) {
      return menuItem.routePath;
    }

    return `/w/${currentWorkspace.slug}/modules${menuItem.routePath}`;
  }

  /**
   * Get module route by module code
   */
  getModuleRoute(moduleCode: string): DynamicModuleRoute | null {
    return this.navigationRoutes().find(route => route.moduleCode === moduleCode) || null;
  }

  /**
   * Navigate to a specific module's default route
   */
  navigateToModule(moduleCode: string): void {
    const moduleRoute = this.getModuleRoute(moduleCode);
    if (!moduleRoute) {
      console.warn(`Module ${moduleCode} not found in navigation routes`);
      return;
    }

    const defaultMenuItem = moduleRoute.menuItems.find(item => item.isDefault) || moduleRoute.menuItems[0];
    if (defaultMenuItem) {
      const fullPath = this.getFullRoutePath(defaultMenuItem);
      this.router.navigate([fullPath]);
    }
  }

  /**
   * Navigate to a specific module menu item
   */
  navigateToMenuItem(moduleCode: string, menuItemId: string): void {
    const moduleRoute = this.getModuleRoute(moduleCode);
    if (!moduleRoute) {
      console.warn(`Module ${moduleCode} not found in navigation routes`);
      return;
    }

    const menuItem = moduleRoute.menuItems.find(item => item.id === menuItemId);
    if (menuItem) {
      const fullPath = this.getFullRoutePath(menuItem);
      this.router.navigate([fullPath]);
    }
  }

  /**
   * Check if a module is installed and active
   */
  isModuleAvailable(moduleCode: string): boolean {
    return this.navigationRoutes().some(route => route.moduleCode === moduleCode);
  }

  /**
   * Get navigation structure grouped by categories (future enhancement)
   */
  getNavigationGroups(): Record<string, DynamicModuleRoute[]> {
    const routes = this.navigationRoutes();
    const groups: Record<string, DynamicModuleRoute[]> = {};

    routes.forEach(route => {
      // For now, group all in 'MODULES' - can be enhanced later with category info
      const groupName = 'MODULES';
      if (!groups[groupName]) {
        groups[groupName] = [];
      }
      groups[groupName].push(route);
    });

    return groups;
  }

  /**
   * Map installed module to navigation route
   */
  private mapToNavigationRoute(module: InstalledModule): DynamicModuleRoute {
    return {
      moduleCode: module.module_code,
      displayName: module.route_info.display_name || module.name,
      basePath: module.route_info.base_path || `/${module.module_code}`,
      icon: module.route_info.icon_name || module.icon,
      primaryColor: module.primary_color,
      navigationIndex: module.navigation_index || 999,
      menuItems: module.route_info.menu_items.map(item => ({
        id: item.id,
        label: item.label,
        routePath: item.route_path,
        icon: item.icon,
        order: item.order,
        isDefault: item.is_default
      }))
    };
  }

  /**
   * Refresh navigation modules (after module install/uninstall)
   */
  async refreshNavigationModules(): Promise<void> {
    await this.loadNavigationModules();
  }

  /**
   * Clear navigation state (on workspace change)
   */
  clearNavigation(): void {
    this._installedModules.set([]);
    this._error.set(null);
  }
}