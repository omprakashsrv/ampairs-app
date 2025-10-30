import { Routes } from '@angular/router';

export const analyticsRoutes: Routes = [
  {
    path: '',
    // TODO: Create AnalyticsDashboardComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: 'sales',
    // TODO: Create SalesAnalyticsComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: 'customer',
    // TODO: Create CustomerAnalyticsComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: 'financial',
    // TODO: Create FinancialAnalyticsComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: 'reports',
    // TODO: Create CustomReportsComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  }
];