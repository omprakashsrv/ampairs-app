import { Routes } from '@angular/router';

export const customerRoutes: Routes = [
  {
    path: '',
    // TODO: Create CustomerListComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: 'create',
    // TODO: Create CustomerCreateComponent  
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: ':id',
    // TODO: Create CustomerDetailComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: ':id/edit',
    // TODO: Create CustomerEditComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  }
];