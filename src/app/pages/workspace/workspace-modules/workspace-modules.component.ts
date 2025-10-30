import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatTabsModule } from '@angular/material/tabs';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDialog } from '@angular/material/dialog';
import { MatDivider } from "@angular/material/divider";
import { WorkspaceService } from '../../../core/services/workspace.service';
import { WorkspaceModuleService, InstalledModule } from '../../../core/services/workspace-module.service';
import { ModuleStoreDialogComponent } from '../module-store-dialog/module-store-dialog.component';
import { DynamicNavigationMenuComponent } from '../../../shared/components/dynamic-navigation-menu/dynamic-navigation-menu.component';

@Component({
  selector: 'app-workspace-modules',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatChipsModule,
    MatMenuModule,
    MatTabsModule,
    MatBadgeModule,
    DynamicNavigationMenuComponent
  ],
  templateUrl: './workspace-modules.component.html',
  styleUrl: './workspace-modules.component.scss'
})
export class WorkspaceModulesComponent implements OnInit {
  private workspaceService = inject(WorkspaceService);
  private workspaceModuleService = inject(WorkspaceModuleService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  // Signals for reactive state management
  installedModules = signal<InstalledModule[]>([]);
  isLoading = signal(false);
  selectedTab = signal(0);
  isInstalling = signal<string | null>(null);

  // Computed signals
  get currentWorkspace() {
    return this.workspaceService.currentWorkspace();
  }

  get activeModules() {
    return this.installedModules().filter(m => m.status === 'ACTIVE' && m.enabled);
  }

  get inactiveModules() {
    return this.installedModules().filter(m => m.status !== 'ACTIVE' || !m.enabled);
  }

  ngOnInit(): void {
    this.loadModules();
  }

  async loadModules(): Promise<void> {
    this.isLoading.set(true);

    try {
      // Load installed modules
      const installed = await this.workspaceModuleService.getInstalledModules();
      this.installedModules.set(installed);

      // Available modules are loaded on demand in the store dialog
    } catch (error: any) {
      console.error('Failed to load modules:', error);
      this.showError('Failed to load modules. Please try again.');
    } finally {
      this.isLoading.set(false);
    }
  }

  openModule(module: InstalledModule): void {
    if (!module.enabled || module.status !== 'ACTIVE') {
      this.showError('Module is not active or enabled');
      return;
    }

    const route = this.workspaceModuleService.getModuleRoute(module.module_code);
    const currentWorkspace = this.currentWorkspace;

    if (currentWorkspace) {
      this.router.navigate(['/w', currentWorkspace.slug, route]);
    }
  }

  async uninstallModule(module: InstalledModule): Promise<void> {
    if (this.isInstalling()) return;

    this.isInstalling.set(module.module_code);

    try {
      const result = await this.workspaceModuleService.uninstallModule(module.id);

      this.snackBar.open(`Module "${result.name}" uninstalled successfully!`, 'Close', {
        duration: 4000,
        panelClass: ['success-snackbar']
      });

      // Refresh modules list
      await this.loadModules();
    } catch (error: any) {
      this.showError(error.message || 'Failed to uninstall module. Please try again.');
    } finally {
      this.isInstalling.set(null);
    }
  }

  getModuleStatusColor(module: InstalledModule): string {
    switch (module.status) {
      case 'ACTIVE':
        return module.enabled ? 'primary' : 'accent';
      case 'INSTALLED':
        return 'warn';
      default:
        return '';
    }
  }

  getModuleStatusText(module: InstalledModule): string {
    if (module.status === 'ACTIVE' && module.enabled) {
      return 'Active';
    } else if (module.status === 'ACTIVE' && !module.enabled) {
      return 'Disabled';
    } else if (module.status === 'INSTALLED') {
      return 'Installed';
    } else {
      return 'Inactive';
    }
  }

  getCategoryIcon(category: string): string {
    const categoryIcons: Record<string, string> = {
      'CUSTOMER_MANAGEMENT': 'people',
      'SALES_MANAGEMENT': 'trending_up',
      'FINANCIAL_MANAGEMENT': 'account_balance',
      'INVENTORY_MANAGEMENT': 'inventory',
      'ORDER_MANAGEMENT': 'shopping_cart',
      'ANALYTICS_REPORTING': 'analytics',
      'COMMUNICATION': 'chat',
      'PROJECT_MANAGEMENT': 'assignment',
      'HR_MANAGEMENT': 'badge',
      'MARKETING': 'campaign',
      'INTEGRATIONS': 'integration_instructions',
      'ADMINISTRATION': 'admin_panel_settings'
    };

    return categoryIcons[category] || 'extension';
  }

  getCategoryDisplayName(category: string): string {
    return category.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
  }

  isModuleInstalled(moduleCode: string): boolean {
    return this.workspaceModuleService.isModuleInstalled(moduleCode);
  }

  goToModuleStore(): void {
    const dialogRef = this.dialog.open(ModuleStoreDialogComponent, {
      width: '800px',
      maxWidth: '90vw',
      maxHeight: '80vh',
      panelClass: 'module-store-dialog'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // If a module was installed, refresh the modules list
        this.loadModules();
        this.snackBar.open('Module installed successfully! You can now use it.', 'Close', {
          duration: 4000,
          panelClass: ['success-snackbar']
        });
      }
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar']
    });
  }
}
