import { Routes } from '@angular/router';

export const productRoutes: Routes = [
  {
    path: '',
    // TODO: Create ProductListComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: 'create',
    // TODO: Create ProductCreateComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: ':id',
    // TODO: Create ProductDetailComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: ':id/edit',
    // TODO: Create ProductEditComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  }
];