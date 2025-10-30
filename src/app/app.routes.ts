import {Routes} from '@angular/router';
import {AuthGuard} from './core/guards/auth.guard';
import {WorkspaceGuard} from './core/guards/workspace.guard';
import {ModuleGuard} from './core/guards/module.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  // Authentication routes (without main layout)
  // {
  //   path: 'login',
  //   loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent)
  // },
  {
    path: 'verify-otp',
    loadComponent: () => import('./auth/verify-otp/verify-otp.component').then(m => m.VerifyOtpComponent)
  },
  {
    path: 'accept-invitation/:token',
    loadComponent: () => import('./pages/accept-invitation/accept-invitation.component').then(m => m.AcceptInvitationComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./auth/firebase-auth/firebase-auth.component').then(m => m.FirebaseAuthComponent)
  },
  // Authenticated routes (with main layout)
  {
    path: '',
    loadComponent: () => import('./shared/components/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [AuthGuard],
    children: [
      {
        path: 'complete-profile',
        loadComponent: () => import('./auth/complete-profile/complete-profile.component').then(m => m.CompleteProfileComponent)
      },
      {
        path: 'workspaces',
        loadComponent: () => import('./pages/workspace/workspace-select/workspace-select.component').then(m => m.WorkspaceSelectComponent)
      },
      {
        path: 'workspace/create',
        loadComponent: () => import('./pages/workspace/workspace-create/workspace-create.component').then(m => m.WorkspaceCreateComponent)
      },
      {
        path: 'home',
        loadComponent: () => import('./home/home.component').then(m => m.HomeComponent),
        canActivate: [WorkspaceGuard]
      },
      {
        path: 'w/:slug',
        loadComponent: () => import('./home/home.component').then(m => m.HomeComponent),
        canActivate: [WorkspaceGuard],
        children: [
          {
            path: '',
            redirectTo: 'modules',
            pathMatch: 'full'
          },
          {
            path: 'modules',
            loadComponent: () => import('./pages/workspace/workspace-modules/workspace-modules.component').then(m => m.WorkspaceModulesComponent)
          },
          {
            path: 'modules/:moduleCode',
            loadComponent: () => import('./shared/components/dynamic-module-router/dynamic-module-router.component').then(m => m.DynamicModuleRouterComponent),
            canActivate: [ModuleGuard]
          },
          {
            path: 'modules/:moduleCode/**',
            loadComponent: () => import('./shared/components/dynamic-module-router/dynamic-module-router.component').then(m => m.DynamicModuleRouterComponent),
            canActivate: [ModuleGuard]
          },
          {
            path: 'dashboard',
            loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent)
          },
          {
            path: 'profile',
            loadComponent: () => import('./auth/complete-profile/complete-profile.component').then(m => m.CompleteProfileComponent)
          },
          {
            path: 'devices',
            loadComponent: () => import('./pages/devices/devices.component').then(m => m.DevicesComponent)
          },
          {
            path: 'members',
            loadComponent: () => import('./pages/members/members.component').then(m => m.MembersComponent)
          },
          {
            path: 'roles',
            loadComponent: () => import('./pages/roles/roles.component').then(m => m.RolesComponent)
          },
          // Module Routes (protected by ModuleGuard)
          {
            path: 'customers',
            canActivate: [ModuleGuard],
            loadChildren: () => import('./modules/customer/customer.routes').then(m => m.customerRoutes)
          },
          {
            path: 'orders',
            canActivate: [ModuleGuard],
            loadChildren: () => import('./modules/order/order.routes').then(m => m.orderRoutes)
          },
          {
            path: 'invoices',
            canActivate: [ModuleGuard],
            loadChildren: () => import('./modules/invoice/invoice.routes').then(m => m.invoiceRoutes)
          },
          {
            path: 'products',
            canActivate: [ModuleGuard],
            loadChildren: () => import('./modules/product/product.routes').then(m => m.productRoutes)
          },
          {
            path: 'inventory',
            canActivate: [ModuleGuard],
            loadChildren: () => import('./modules/inventory/inventory.routes').then(m => m.inventoryRoutes)
          },
          {
            path: 'finance',
            canActivate: [ModuleGuard],
            loadChildren: () => import('./modules/finance/finance.routes').then(m => m.financeRoutes)
          },
          {
            path: 'analytics',
            canActivate: [ModuleGuard],
            loadChildren: () => import('./modules/analytics/analytics.routes').then(m => m.analyticsRoutes)
          },
          // Fallback for generic module routing
          {
            path: 'modules/:moduleCode',
            loadComponent: () => import('./pages/module-placeholder/module-placeholder.component').then(m => m.ModulePlaceholderComponent)
          }
        ]
      }
    ]
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];
