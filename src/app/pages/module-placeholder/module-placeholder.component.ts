import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { WorkspaceModuleService } from '../../core/services/workspace-module.service';
import { WorkspaceService } from '../../core/services/workspace.service';

@Component({
  selector: 'app-module-placeholder',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <div class="module-placeholder-container">
      <div class="placeholder-content">
        <mat-card class="placeholder-card">
          <mat-card-header>
            <div class="module-icon" mat-card-avatar>
              <mat-icon>{{ moduleIcon() }}</mat-icon>
            </div>
            <mat-card-title>{{ moduleName() }}</mat-card-title>
            <mat-card-subtitle>Module Coming Soon</mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            <div class="placeholder-message">
              <mat-icon class="construction-icon">construction</mat-icon>
              <h2>Module Under Development</h2>
              <p>
                The <strong>{{ moduleName() }}</strong> module is currently being developed.
                Please check back later for full functionality.
              </p>

              @if (moduleCode()) {
                <div class="module-info">
                  <p><strong>Module Code:</strong> {{ moduleCode() }}</p>
                  <p><strong>Status:</strong> In Development</p>
                </div>
              }
            </div>
          </mat-card-content>

          <mat-card-actions>
            <button mat-raised-button color="primary" (click)="goBack()">
              <mat-icon>arrow_back</mat-icon>
              Back to Modules
            </button>
          </mat-card-actions>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .module-placeholder-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: var(--background-color);
      padding: 2rem;
    }

    .placeholder-content {
      width: 100%;
      max-width: 600px;
    }

    .placeholder-card {
      text-align: center;
      background-color: var(--surface-container-color);
    }

    .module-icon {
      background-color: var(--primary-container-color);
      color: var(--on-primary-container-color);
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto;

      mat-icon {
        font-size: 2rem;
        width: 32px;
        height: 32px;
      }
    }

    .placeholder-message {
      padding: 2rem 0;

      .construction-icon {
        font-size: 4rem;
        color: var(--warning-color);
        margin-bottom: 1rem;
      }

      h2 {
        color: var(--on-surface-color);
        margin: 1rem 0;
        font-size: 1.5rem;
      }

      p {
        color: var(--on-surface-variant-color);
        line-height: 1.6;
        margin: 1rem 0;
      }
    }

    .module-info {
      background-color: var(--surface-container-low-color);
      border-radius: 8px;
      padding: 1rem;
      margin-top: 2rem;
      text-align: left;

      p {
        margin: 0.5rem 0;
        font-size: 0.9rem;
      }
    }

    mat-card-actions {
      justify-content: center;
      padding: 1.5rem;

      button {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }
    }
  `]
})
export class ModulePlaceholderComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private workspaceService = inject(WorkspaceService);
  private workspaceModuleService = inject(WorkspaceModuleService);

  moduleCode = signal<string>('');
  moduleName = signal<string>('Module');
  moduleIcon = signal<string>('extension');

  ngOnInit(): void {
    const moduleCode = this.route.snapshot.paramMap.get('moduleCode');
    if (moduleCode) {
      this.moduleCode.set(moduleCode);

      // Try to get module info from installed modules
      const installedModule = this.workspaceModuleService.getModuleByCode(moduleCode);
      if (installedModule) {
        this.moduleName.set(installedModule.name);
        this.moduleIcon.set(installedModule.icon || this.getCategoryIcon(installedModule.category));
      } else {
        // Generate a display name from the module code
        this.moduleName.set(this.generateDisplayName(moduleCode));
        this.moduleIcon.set(this.getCategoryIcon(''));
      }
    }
  }

  goBack(): void {
    const currentWorkspace = this.workspaceService.currentWorkspace();
    if (currentWorkspace) {
      window.history.back();
    }
  }

  private generateDisplayName(moduleCode: string): string {
    return moduleCode
      .split('-')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  private getCategoryIcon(category: string): string {
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
}
