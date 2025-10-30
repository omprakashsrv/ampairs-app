import {computed, inject, Injectable, signal} from '@angular/core';
import {toObservable} from '@angular/core/rxjs-interop';
import {HttpClient} from '@angular/common/http';
import {firstValueFrom, Observable} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {environment} from '../../../environments/environment';

export interface Workspace {
  id: string;
  name: string;
  slug: string;
  description?: string;
  workspace_type: string;
  avatar_url?: string;
  is_active: boolean;
  subscription_plan: string;
  max_members: number;
  storage_limit_gb: number;
  storage_used_gb: number;
  timezone: string;
  language: string;
  created_by: string;
  created_at: string;
  updated_at: string;
  last_activity_at?: string;
  trial_expires_at?: string;
  member_count?: number;
  is_trial?: boolean;
  storage_percentage?: number;
}

export interface WorkspaceListItem {
  id: string;
  name: string;
  slug: string;
  description?: string;
  workspace_type: string;
  avatar_url?: string;
  subscription_plan: string;
  member_count: number;
  last_activity_at?: string;
  created_at: string;
}

export interface CreateWorkspaceRequest {
  name: string;
  description?: string;
  workspace_type: string;
  avatar_url?: string;
  timezone?: string;
  language?: string;
  slug?: string;
  // Business Details
  address_line1?: string;
  address_line2?: string;
  city?: string;
  state?: string;
  postal_code?: string;
  country?: string;
  phone?: string;
  email?: string;
  website?: string;
  registration_number?: string;
  business_hours_start?: string;
  business_hours_end?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: any;
  timestamp: string;
  path?: string;
  trace_id?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  pageable: {
    page_number: number;
    page_size: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean;
  total_elements: number;
  total_pages: number;
  first: boolean;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  number_of_elements: number;
  empty: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class WorkspaceService {
  private readonly WORKSPACE_API_URL = `${environment.apiBaseUrl}/workspace/v1`;
  private http = inject(HttpClient);

  // Signal-based state management
  private _currentWorkspace = signal<Workspace | null>(null);
  // Public readonly signals
  readonly currentWorkspace = this._currentWorkspace.asReadonly();
  // Backward compatibility Observable properties (deprecated - use signals instead)
  /** @deprecated Use currentWorkspace signal instead */
  readonly currentWorkspace$ = toObservable(this._currentWorkspace);
  // Computed signals
  readonly hasSelectedWorkspace = computed(() => this._currentWorkspace() !== null);
  readonly currentWorkspaceName = computed(() => this._currentWorkspace()?.name || '');
  readonly currentWorkspaceSlug = computed(() => this._currentWorkspace()?.slug || '');
  readonly storageUsage = computed(() => {
    const workspace = this._currentWorkspace();
    if (!workspace || !workspace.storage_limit_gb) return 0;
    return Math.round((workspace.storage_used_gb / workspace.storage_limit_gb) * 100);
  });
  readonly isTrialWorkspace = computed(() => {
    const workspace = this._currentWorkspace();
    return workspace?.is_trial === true;
  });
  private _workspaces = signal<WorkspaceListItem[]>([]);
  readonly workspaces = this._workspaces.asReadonly();
  readonly workspaceCount = computed(() => this._workspaces().length);
  private _loading = signal(false);
  readonly loading = this._loading.asReadonly();
  private _error = signal<string | null>(null);
  readonly error = this._error.asReadonly();

  constructor() {
    this.loadSelectedWorkspace();
  }

  /**
   * Get user's workspaces with pagination
   */
  async getUserWorkspaces(page = 0, size = 20, sortBy = 'createdAt', sortDir = 'desc'): Promise<WorkspaceListItem[]> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const params = {
        page: page.toString(),
        size: size.toString(),
        sortBy,
        sortDir
      };

      const response = await firstValueFrom(
        this.http.get<any>(`${this.WORKSPACE_API_URL}`, {params})
          .pipe(catchError(this.handleError))
      );

      console.log('Full API Response:', response);
      console.log('Response properties:', Object.keys(response || {}));

      let workspaces: WorkspaceListItem[] = [];

      // Check if response has the ApiResponse wrapper structure
      if (response && response.data && response.data.content) {
        console.log('Found ApiResponse wrapper with data.content');
        workspaces = response.data.content;
      }
      // Check if response is directly the paginated structure
      else if (response && response.content) {
        console.log('Found direct paginated response with content');
        workspaces = response.content;
      }
      // Check if response is directly an array
      else if (Array.isArray(response)) {
        console.log('Response is directly an array');
        workspaces = response;
      } else {
        console.error('Unexpected response structure. Available properties:', Object.keys(response || {}));
        workspaces = [];
      }

      this._workspaces.set(workspaces);
      return workspaces;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to load workspaces');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Create a new workspace
   */
  async createWorkspace(workspaceData: CreateWorkspaceRequest): Promise<Workspace> {
    this._loading.set(true);
    this._error.set(null);

    try {
      const response = await firstValueFrom(
        this.http.post<ApiResponse<Workspace>>(`${this.WORKSPACE_API_URL}`, workspaceData)
          .pipe(catchError(this.handleError))
      );

      const workspace = response.data;

      // Refresh workspace list after creation
      await this.getUserWorkspaces();

      return workspace;
    } catch (error: any) {
      this._error.set(error.message || 'Failed to create workspace');
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Get workspace by ID
   */
  getWorkspaceById(workspaceId: string): Observable<Workspace> {
    return this.http.get<Workspace>(`${this.WORKSPACE_API_URL}/${workspaceId}`)
      .pipe(
        map(response => {
          console.log('getWorkspaceById response:', response);
          // ApiResponseInterceptor already unwrapped the data, so response is directly the workspace
          if (!response || !response.id) {
            throw new Error('Invalid workspace data received');
          }
          return response;
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Get workspace by slug
   */
  getWorkspaceBySlug(slug: string): Observable<Workspace> {
    return this.http.get<ApiResponse<Workspace>>(`${this.WORKSPACE_API_URL}/slug/${slug}`)
      .pipe(
        map(response => response.data),
        catchError(this.handleError)
      );
  }

  /**
   * Update workspace
   */
  updateWorkspace(workspaceId: string, workspaceData: Partial<CreateWorkspaceRequest>): Observable<Workspace> {
    return this.http.put<ApiResponse<Workspace>>(`${this.WORKSPACE_API_URL}/${workspaceId}`, workspaceData)
      .pipe(
        map(response => {
          const workspace = response.data;
          // Update current workspace if it's the one being updated
          if (this._currentWorkspace()?.id === workspaceId) {
            this._currentWorkspace.set(workspace);
          }
          return workspace;
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Search workspaces
   */
  searchWorkspaces(query: string, workspaceType?: string, subscriptionPlan?: string): Observable<WorkspaceListItem[]> {
    const params: any = {query};
    if (workspaceType) params.workspaceType = workspaceType;
    if (subscriptionPlan) params.subscriptionPlan = subscriptionPlan;

    return this.http.get<ApiResponse<PaginatedResponse<WorkspaceListItem>>>(`${this.WORKSPACE_API_URL}/search`, {params})
      .pipe(
        map(response => response.data.content),
        catchError(this.handleError)
      );
  }

  /**
   * Check if workspace slug is available
   */
  checkSlugAvailability(slug: string): Observable<boolean> {
    return this.http.get<ApiResponse<{ available: boolean }>>(`${this.WORKSPACE_API_URL}/check-slug/${slug}`)
      .pipe(
        map(response => response.data.available),
        catchError(this.handleError)
      );
  }

  /**
   * Set current workspace and store in localStorage
   */
  setCurrentWorkspace(workspace: Workspace): void {
    if (!workspace || !workspace.id) {
      console.error('Invalid workspace provided to setCurrentWorkspace:', workspace);
      return;
    }

    this._currentWorkspace.set(workspace);
    localStorage.setItem('selected_workspace', JSON.stringify(workspace));
    // Set workspace header for API requests
    localStorage.setItem('workspace_id', workspace.id);
  }

  /**
   * Get current workspace (legacy method - use signal instead)
   */
  getCurrentWorkspace(): Workspace | null {
    return this._currentWorkspace();
  }

  /**
   * Clear current workspace
   */
  clearCurrentWorkspace(): void {
    this._currentWorkspace.set(null);
    localStorage.removeItem('selected_workspace');
    localStorage.removeItem('workspace_id');
  }

  /**
   * Check if user has selected a workspace (legacy method - use computed signal instead)
   * @deprecated Use hasSelectedWorkspace computed signal instead
   */
  hasSelectedWorkspaceLegacy(): boolean {
    return this._currentWorkspace() !== null;
  }

  /**
   * Load selected workspace from localStorage
   */
  private loadSelectedWorkspace(): void {
    const savedWorkspace = localStorage.getItem('selected_workspace');
    if (savedWorkspace) {
      try {
        const workspace = JSON.parse(savedWorkspace);
        this._currentWorkspace.set(workspace);
      } catch (error) {
        console.error('Failed to parse saved workspace:', error);
        localStorage.removeItem('selected_workspace');
        localStorage.removeItem('workspace_id');
      }
    }
  }

  /**
   * Handle HTTP errors
   */
  private handleError(error: any): Observable<never> {
    console.error('Workspace Service Error:', error);
    let errorMessage = 'An unexpected error occurred';

    // Extract error message from API response structure
    if (error.error && error.error.error && error.error.error.message) {
      errorMessage = error.error.error.message;
    } else if (error.error && error.error.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    }

    throw new Error(errorMessage);
  }
}
