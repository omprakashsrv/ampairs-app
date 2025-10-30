import { Routes } from '@angular/router';

export const invoiceRoutes: Routes = [
  {
    path: '',
    // TODO: Create InvoiceListComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: 'create',
    // TODO: Create InvoiceCreateComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: ':id',
    // TODO: Create InvoiceDetailComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  },
  {
    path: ':id/edit',
    // TODO: Create InvoiceEditComponent
    loadComponent: () => import('../../pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
  }
];