import { Routes } from '@angular/router';

export const orderRoutes: Routes = [
  {
    path: '',
    // TODO: Create OrderListComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: 'create',
    // TODO: Create OrderCreateComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: ':id',
    // TODO: Create OrderDetailComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: ':id/edit',
    // TODO: Create OrderEditComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  }
];