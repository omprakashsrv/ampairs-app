import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router} from '@angular/router';
import {Observable, of} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {WorkspaceService} from '../services/workspace.service';

@Injectable({
  providedIn: 'root'
})
export class WorkspaceGuard implements CanActivate {

  constructor(
    private workspaceService: WorkspaceService,
    private router: Router
  ) {
  }

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    const slug = route.paramMap.get('slug');

    if (!slug) {
      // No slug in route, redirect to workspace selection
      this.router.navigate(['/workspaces']);
      return of(false);
    }

    // Check if current workspace matches the slug
    const currentWorkspace = this.workspaceService.getCurrentWorkspace();
    if (currentWorkspace && currentWorkspace.slug === slug) {
      return of(true);
    }

    // Try to load workspace by slug
    return this.workspaceService.getWorkspaceBySlug(slug).pipe(
      map(workspace => {
        if (workspace) {
          this.workspaceService.setCurrentWorkspace(workspace);
          return true;
        } else {
          // Workspace not found or no access, redirect to workspace selection
          this.router.navigate(['/workspaces']);
          return false;
        }
      }),
      catchError(() => {
        // Error loading workspace, redirect to workspace selection
        this.router.navigate(['/workspaces']);
        return of(false);
      })
    );
  }
}
