import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { WorkspaceModuleService } from '../services/workspace-module.service';
import { WorkspaceService } from '../services/workspace.service';

/**
 * Module Guard
 * 
 * Ensures that users can only access modules that are:
 * 1. Installed in the current workspace
 * 2. Active and enabled
 * 
 * If the module is not available, redirects to the modules page
 */
export const ModuleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const workspaceModuleService = inject(WorkspaceModuleService);
  const workspaceService = inject(WorkspaceService);
  const router = inject(Router);

  // Get the module code from the route
  const moduleCode = getModuleCodeFromRoute(route);
  
  if (!moduleCode) {
    // If we can't determine the module code, allow access
    return true;
  }

  // Check if the module is installed and active
  if (workspaceModuleService.isModuleInstalled(moduleCode)) {
    return true;
  }

  // Module is not installed or not active, redirect to modules page
  const currentWorkspace = workspaceService.currentWorkspace();
  if (currentWorkspace) {
    router.navigate(['/w', currentWorkspace.slug, 'modules']);
    return false;
  }

  // Fallback redirect
  router.navigate(['/workspaces']);
  return false;
};

/**
 * Extract module code from route path
 */
function getModuleCodeFromRoute(route: ActivatedRouteSnapshot): string | null {
  const path = route.routeConfig?.path || '';

  // Check if it's a parameterized module route (modules/:moduleCode)
  if (path === 'modules/:moduleCode' || path === 'modules/:moduleCode/**') {
    return route.paramMap.get('moduleCode');
  }

  // Legacy static route mapping (for backward compatibility)
  const routeToModuleMap: Record<string, string> = {
    'customers': 'customer-management',
    'orders': 'order-management',
    'invoices': 'invoice-management',
    'products': 'product-catalog',
    'inventory': 'inventory-management',
    'finance': 'financial-management',
    'analytics': 'analytics-reporting',
    'communication': 'communication',
    'projects': 'project-management',
    'hr': 'hr-management',
    'marketing': 'marketing',
    'integrations': 'integrations',
    'admin': 'administration'
  };

  // Check if the path matches any known module route
  if (routeToModuleMap[path]) {
    return routeToModuleMap[path];
  }

  // For child routes, check the parent route
  if (route.parent) {
    return getModuleCodeFromRoute(route.parent);
  }

  return null;
}