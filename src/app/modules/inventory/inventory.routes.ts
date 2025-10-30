import { Routes } from '@angular/router';

export const inventoryRoutes: Routes = [
  {
    path: '',
    // TODO: Create InventoryDashboardComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: 'items',
    // TODO: Create InventoryItemsComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: 'adjustments',
    // TODO: Create InventoryAdjustmentsComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: 'reports',
    // TODO: Create InventoryReportsComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  }
];