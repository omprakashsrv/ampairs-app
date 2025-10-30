import { Component, OnInit, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';
import { DynamicNavigationService, DynamicModuleRoute } from '../../../core/services/dynamic-navigation.service';

@Component({
  selector: 'app-dynamic-navigation-menu',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatExpansionModule,
    MatListModule,
    MatDividerModule,
    MatBadgeModule
  ],
  templateUrl: './dynamic-navigation-menu.component.html',
  styleUrl: './dynamic-navigation-menu.component.scss'
})
export class DynamicNavigationMenuComponent implements OnInit {
  private dynamicNavigation = inject(DynamicNavigationService);

  // Component signals
  loading = this.dynamicNavigation.loading;
  error = this.dynamicNavigation.error;
  navigationRoutes = this.dynamicNavigation.navigationRoutes;

  // Track expanded panels
  private expandedPanels = signal<Set<string>>(new Set());

  constructor() {
    // Auto-load modules when component initializes
    effect(() => {
      if (this.navigationRoutes().length === 0 && !this.loading() && !this.error()) {
        this.loadModules();
      }
    });
  }

  ngOnInit() {
    this.loadModules();
  }

  async loadModules() {
    await this.dynamicNavigation.loadNavigationModules();
  }

  getFullRoutePath(moduleCode: string, routePath: string): string {
    return `/w/${this.getCurrentWorkspaceSlug()}/modules/${moduleCode}${routePath}`;
  }

  navigateToModule(moduleCode: string) {
    this.dynamicNavigation.navigateToModule(moduleCode);
  }

  isExpanded(moduleCode: string): boolean {
    return this.expandedPanels().has(moduleCode);
  }

  togglePanel(moduleCode: string) {
    const expanded = new Set(this.expandedPanels());
    if (expanded.has(moduleCode)) {
      expanded.delete(moduleCode);
    } else {
      expanded.add(moduleCode);
    }
    this.expandedPanels.set(expanded);
  }

  getTotalMenuItems(): number {
    return this.navigationRoutes().reduce((total, route) => total + route.menuItems.length, 0);
  }

  private getCurrentWorkspaceSlug(): string {
    // Extract workspace slug from current URL
    const url = window.location.pathname;
    const match = url.match(/\/w\/([^\/]+)/);
    return match && match[1] ? match[1] : 'default';
  }
}