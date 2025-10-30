import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface ModuleMenuItemResponse {
  id: string;
  label: string;
  route_path: string;
  icon: string;
  order: number;
  is_default: boolean;
}

export interface ModuleRouteInfoResponse {
  base_path: string;
  display_name: string;
  icon_name: string;
  menu_items: ModuleMenuItemResponse[];
}

export interface InstalledModule {
  id: string;
  module_code: string;
  name: string;
  category: string;
  version: string;
  status: 'ACTIVE' | 'INSTALLED' | 'INACTIVE';
  enabled: boolean;
  installed_at: string;
  icon: string;
  primary_color: string;
  health_score?: number;
  needs_attention?: boolean;
  description?: string;
  route_info: ModuleRouteInfoResponse;
  navigation_index: number;
}

export interface AvailableModule {
  module_code: string;
  name: string;
  description: string | null;
  category: string;
  version: string;
  rating: number;
  install_count: number;
  complexity: string;
  icon: string;
  primary_color: string;
  featured: boolean;
  required_tier: string;
  size_mb: number;
}

export interface ModuleCategory {
  code: string;
  display_name: string;
  description: string;
  icon: string;
}

export interface ModuleDetail {
  module_id: string;
  workspace_id: string;
  message: string;
  module_info: {
    name: string;
    category: string;
    description: string;
    version: string;
    status: string;
    enabled: boolean;
    installed_at: string;
    last_updated: string;
  };
  configuration: Record<string, any>;
  analytics: {
    daily_active_users: number;
    monthly_access: number;
    average_session_duration: string;
  };
  permissions: {
    can_configure: boolean;
    can_uninstall: boolean;
    can_view_analytics: boolean;
  };
}

export interface ModuleInstallationResponse {
  module_id: string;
  module_code: string;
  name: string;
  status: string;
  installed_at: string;
  message: string;
}

export interface ModuleUninstallationResponse {
  module_id: string;
  module_code: string;
  name: string;
  uninstalled_at: string;
  message: string;
}

export interface ModuleInstallationStatus {
  is_installed: boolean;
  workspace_module_id?: string;
  status?: 'ACTIVE' | 'INSTALLED' | 'INACTIVE';
  enabled?: boolean;
  installed_at?: string;
  health_score?: number;
  needs_attention?: boolean;
}

export interface ModuleActionOption {
  action_type: 'INSTALL' | 'UNINSTALL' | 'ENABLE' | 'DISABLE' | 'CONFIGURE' | 'UPDATE';
  label: string;
  description: string;
  enabled: boolean;
  requires_confirmation: boolean;
  confirmation_message?: string;
}

export interface ModuleActionPermissions {
  can_install: boolean;
  can_uninstall: boolean;
  can_configure: boolean;
  can_enable: boolean;
  can_disable: boolean;
}

export interface ModuleWithActions {
  module_code: string;
  name: string;
  description?: string;
  category: string;
  version: string;
  icon: string;
  primary_color: string;
  featured: boolean;
  rating: number;
  install_count: number;
  complexity: string;
  size_mb: number;
  required_tier: string;
  installation_status: ModuleInstallationStatus;
  available_actions: ModuleActionOption[];
  permissions: ModuleActionPermissions;
}

export interface ModuleCatalogStatistics {
  total_installed: number;
  total_available: number;
  enabled_modules: number;
  disabled_modules: number;
  modules_needing_attention: number;
}

export interface ModuleCatalogResponse {
  installed_modules: ModuleWithActions[];
  available_modules: ModuleWithActions[];
  categories: ModuleCategory[];
  statistics: ModuleCatalogStatistics;
}

@Injectable({
  providedIn: 'root'
})
export class WorkspaceModuleService {
  private readonly MODULE_API_URL = `${environment.apiBaseUrl}/workspace/v1/modules`;
  private http = inject(HttpClient);

  // Signal-based state management
  private _installedModules = signal<InstalledModule[]>([]);
  private _availableModules = signal<AvailableModule[]>([]);
  private _categories = signal<ModuleCategory[]>([]);
  private _loading = signal(false);
  private _error = signal<string | null>(null);

  // Public readonly signals
  readonly installedModules = this._installedModules.asReadonly();
  readonly availableModules = this._availableModules.asReadonly();
  readonly categories = this._categories.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  /**
   * Get all installed modules for the current workspace
   */
  async getInstalledModules(): Promise<InstalledModule[]> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const modules = await firstValueFrom(
        this.http.get<InstalledModule[]>(this.MODULE_API_URL)
          .pipe(catchError(this.handleError.bind(this)))
      );

      this._installedModules.set(modules);
      return modules;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to load installed modules');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Get detailed information about a specific module
   */
  getModuleDetail(moduleId: string): Observable<ModuleDetail> {
    return this.http.get<ModuleDetail>(`${this.MODULE_API_URL}/${moduleId}`)
      .pipe(catchError(this.handleError.bind(this)));
  }

  /**
   * Get available modules from the master catalog
   */
  async getAvailableModules(category?: string, featured = false): Promise<AvailableModule[]> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const params: any = {};
      if (category) params.category = category;
      if (featured) params.featured = featured;

      const modules = await firstValueFrom(
        this.http.get<AvailableModule[]>(`${this.MODULE_API_URL}/available`, { params })
          .pipe(catchError(this.handleError.bind(this)))
      );

      this._availableModules.set(modules);
      return modules;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to load available modules');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Get unified module catalog with both installed and available modules
   * This includes installation status and available actions for each module
   */
  async getModuleCatalog(category?: string, includeDisabled = false): Promise<ModuleCatalogResponse> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const params: any = {};
      if (category) params.category = category;
      if (includeDisabled) params.include_disabled = includeDisabled;

      const catalog = await firstValueFrom(
        this.http.get<ModuleCatalogResponse>(`${this.MODULE_API_URL}/catalog`, { params })
          .pipe(catchError(this.handleError.bind(this)))
      );

      // Update the categories from the catalog response
      this._categories.set(catalog.categories);

      return catalog;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to load module catalog');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Install a module in the current workspace
   */
  async installModule(moduleCode: string): Promise<ModuleInstallationResponse> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const result = await firstValueFrom(
        this.http.post<ModuleInstallationResponse>(`${this.MODULE_API_URL}/install/${moduleCode}`, {})
          .pipe(catchError(this.handleError.bind(this)))
      );

      // Refresh installed modules after installation
      await this.getInstalledModules();
      
      return result;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to install module');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Uninstall a module from the current workspace
   */
  async uninstallModule(moduleId: string): Promise<ModuleUninstallationResponse> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const result = await firstValueFrom(
        this.http.delete<ModuleUninstallationResponse>(`${this.MODULE_API_URL}/${moduleId}`)
          .pipe(catchError(this.handleError.bind(this)))
      );

      // Refresh installed modules after uninstallation
      await this.getInstalledModules();
      
      return result;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to uninstall module');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Navigate to a specific module by module code
   */
  getModuleRoute(moduleCode: string): string {
    // Map module codes to their respective routes
    const moduleRouteMap: Record<string, string> = {
      'customer-management': '/customers',
      'order-management': '/orders',
      'invoice-management': '/invoices',
      'product-management': '/products',
      'inventory-management': '/inventory',
      'sales-management': '/sales',
      'financial-management': '/finance',
      'analytics-reporting': '/analytics',
      'communication': '/communication',
      'project-management': '/projects',
      'hr-management': '/hr',
      'marketing': '/marketing',
      'integrations': '/integrations',
      'administration': '/admin'
    };

    return moduleRouteMap[moduleCode] || `/modules/${moduleCode}`;
  }

  /**
   * Check if a specific module is installed and active
   */
  isModuleInstalled(moduleCode: string): boolean {
    const modules = this._installedModules();
    return modules.some(m => m.module_code === moduleCode && m.status === 'ACTIVE' && m.enabled);
  }

  /**
   * Get module by code
   */
  getModuleByCode(moduleCode: string): InstalledModule | null {
    const modules = this._installedModules();
    return modules.find(m => m.module_code === moduleCode) || null;
  }

  /**
   * Handle HTTP errors
   */
  private handleError(error: any): Observable<never> {
    console.error('WorkspaceModule Service Error:', error);
    let errorMessage = 'An unexpected error occurred';

    if (error.error && error.error.error && error.error.error.message) {
      errorMessage = error.error.error.message;
    } else if (error.error && error.error.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }

    throw new Error(errorMessage);
  }
}