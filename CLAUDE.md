 Angular Application Architecture & Patterns

  Application Structure

  The Angular application follows a modern standalone component architecture with clear separation of concerns:

  src/app/
  ├── core/                    # Services, guards, interceptors, models
  │   ├── guards/             # AuthGuard, WorkspaceGuard, WorkspaceMemberGuard
  │   ├── interceptors/       # ApiResponse, Auth, Workspace, Loading
  │   ├── models/            # TypeScript interfaces (snake_case)
  │   └── services/          # Core business services
  ├── shared/                 # Reusable components and design system
  │   ├── components/        # Shared UI components
  │   └── styles/           # M3 theming system
  ├── auth/                  # Authentication flow (Login → OTP → Profile)
  ├── pages/                # Feature pages (Dashboard, Workspace management)
  ├── home/                 # Main application shell with navigation
  └── app.component.ts      # Root component (minimal shell)

  Key Patterns:
  - No NgModules: Pure standalone components with lazy loading
  - Feature-based organization: Clear boundaries between business domains
  - Multi-tenant routing: /w/:slug for workspace-scoped navigation
  - Service-based state management: BehaviorSubject reactive patterns

  Material Design 3 System

  CRITICAL: Enforce Material Design 3 exclusively

  - Components: ONLY @angular/material components allowed
  - Theming: Complete M3 color system with dynamic theme switching
  - CSS Custom Properties: All styling uses M3 design tokens
  - Typography: Full M3 typography scale (display → label)
  - Responsive: Mobile-first with proper breakpoint handling

  Theme Architecture:
  shared/styles/
  ├── _theme-m3.scss           # M3 theme definitions
  ├── _theme-m3-palettes.scss  # Color palettes
  ├── _variables.scss          # Design tokens
  └── _mixins.scss            # Utility mixins

  Centralized Design Token System (src/theme/variables.scss):
  
  CRITICAL: Always use SCSS variables instead of direct CSS custom properties
  
  ```scss
  // Import pattern for all components
  @use '../../../theme/variables' as vars;
  @use '../../../theme/mixins' as theme;
  
  // Material 3 Color System - Complete token set
  $color-primary: var(--primary-color);
  $color-primary-container: var(--primary-container-color);
  $color-on-primary: var(--on-primary-color);
  $color-on-primary-container: var(--on-primary-container-color);
  
  $color-secondary: var(--secondary-color);
  $color-secondary-container: var(--secondary-container-color);
  $color-on-secondary-container: var(--on-secondary-container-color);
  
  $color-surface: var(--surface-color);
  $color-surface-container: var(--surface-container-color);
  $color-surface-container-low: var(--surface-container-low-color);
  $color-surface-container-high: var(--surface-container-high-color);
  $color-surface-variant: var(--surface-variant-color);
  $color-on-surface: var(--on-surface-color);
  $color-on-surface-variant: var(--on-surface-variant-color);
  
  $color-background: var(--background-color);
  $color-outline: var(--outline-color);
  $color-outline-variant: var(--outline-variant-color);
  
  // Semantic Colors
  $color-error: var(--error-color);
  $color-success: var(--success-color);
  $color-warning: var(--warning-color);
  $color-info: var(--info-color);
  
  // Menu System Colors
  $color-menu-item-label: var(--on-surface-color);
  $color-menu-item-supporting: var(--on-surface-variant-color);
  $color-menu-divider: var(--outline-variant-color);

  // Typography - Material Design 3 System (Use proper M3 tokens)
  $font-body-large: var(--mat-sys-body-large);
  $font-body-medium: var(--mat-sys-body-medium);
  $font-body-small: var(--mat-sys-body-small);
  $font-display-large: var(--mat-sys-display-large);
  $font-display-medium: var(--mat-sys-display-medium);
  $font-display-small: var(--mat-sys-display-small);
  $font-headline-large: var(--mat-sys-headline-large);
  $font-headline-medium: var(--mat-sys-headline-medium);
  $font-headline-small: var(--mat-sys-headline-small);
  $font-label-large: var(--mat-sys-label-large);
  $font-label-medium: var(--mat-sys-label-medium);
  $font-label-small: var(--mat-sys-label-small);
  $font-title-large: var(--mat-sys-title-large);
  $font-title-medium: var(--mat-sys-title-medium);
  $font-title-small: var(--mat-sys-title-small);

  // Spacing Scale - M3 System  
  $spacing-xs: var(--mat-sys-spacing-x-small, 0.25rem);      // 4px
  $spacing-sm: var(--mat-sys-spacing-small, 0.5rem);         // 8px
  $spacing-md: var(--mat-sys-spacing-medium, 0.75rem);       // 12px
  $spacing-lg: var(--mat-sys-spacing-large, 1rem);           // 16px
  $spacing-xl: var(--mat-sys-spacing-x-large, 1.5rem);       // 24px
  $spacing-xxl: var(--mat-sys-spacing-xx-large, 2rem);       // 32px

  // Layout Tokens
  $border-radius-sm: var(--mat-sys-corner-extra-small, 0.25rem);  // 4px
  $border-radius-md: var(--mat-sys-corner-small, 0.5rem);         // 8px
  $border-radius-lg: var(--mat-sys-corner-medium, 1rem);          // 16px
  $border-radius-xl: var(--mat-sys-corner-large, 1rem);           // 16px
  $border-radius-round: var(--mat-sys-corner-full, 625rem);       // Full round
  
  // Animation & Transitions
  $transition-fast: 0.15s;
  $transition-normal: 0.3s;
  $transition-slow: 0.5s;
  $transition-standard: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);   // M3 easing
  
  // Elevation System
  $shadow-1: var(--shadow-1);    // Light elevation
  $shadow-2: var(--shadow-2);    // Medium elevation
  ```
  
  Usage Pattern - Always use SCSS variables with proper M3 typography:
  ```scss
  // ✅ CORRECT - Use centralized SCSS variables with M3 typography tokens
  .component {
    background-color: vars.$color-surface-container;
    color: vars.$color-on-surface;
    font: vars.$font-body-medium;  // Complete M3 typography token
    padding: vars.$spacing-lg vars.$spacing-xl;
    border-radius: vars.$border-radius-lg;
    box-shadow: vars.$shadow-1;
    transition: vars.$transition-standard;
    
    h1 { font: vars.$font-headline-large; }
    h2 { font: vars.$font-headline-medium; }
    h3 { font: vars.$font-headline-small; }
    p { font: vars.$font-body-medium; }
    small { font: vars.$font-label-medium; }
    .caption { font: vars.$font-label-small; }
  }
  
  // ❌ INCORRECT - Never use explicit font sizes or CSS custom properties directly  
  .component {
    background-color: var(--surface-container-color);
    color: var(--on-surface-color);
    font-size: 14px; // Don't use explicit sizes
    font-size: vars.$font-size-md; // Don't use deprecated size variables
  }
  ```

  ThemeService Features:
  - Runtime theme switching (light/dark/auto)
  - Density control (-5 to 0)
  - Persistent user preferences
  - Export/import theme configurations

  Authentication & Security

  Multi-step Authentication Flow:
  1. Phone Login → 2. OTP Verification → 3. Profile Completion

  Security Patterns:
  - JWT Token Management: Automatic refresh with device tracking
  - Multi-device Sessions: Device-specific session isolation
  - reCAPTCHA Integration: Conditional security (dev bypass available)
  - Workspace Context: Tenant-aware API calls via interceptors

  Key Services:
  - AuthService: Complete auth lifecycle with session management
  - AuthInterceptor: Token injection and refresh handling
  - WorkspaceInterceptor: Tenant context headers

  HTTP Interceptor Chain

  Processing Order:
  1. ApiResponseInterceptor: Unwraps ApiResponse<T> structure
  2. AuthInterceptor: JWT token management and refresh
  3. WorkspaceInterceptor: Workspace context headers
  4. LoadingInterceptor: Global loading state management

  CRITICAL: API Response Structure Handling

  The ApiResponseInterceptor automatically unwraps the backend's ApiResponse<T> wrapper structure:

  ```typescript
  // Backend returns: ApiResponse<T> = { success: boolean, data: T, error?: ErrorInfo }
  // Interceptor unwraps to: T (just the data field)
  
  // ✅ CORRECT - Service methods expect unwrapped data
  async getWorkspaceRoles(workspaceId: string): Promise<WorkspaceRoleResponse[]> {
    return await firstValueFrom(
      this.http.get<WorkspaceRoleResponse[]>(`/api/roles`) // Returns T directly
    );
  }

  // ❌ INCORRECT - Don't expect ApiResponse wrapper
  async getWorkspaceRoles(workspaceId: string): Promise<ApiResponse<WorkspaceRoleResponse[]>> {
    // This will fail - interceptor already unwrapped the response
  }
  ```

  Key Rules for New API Integration:
  - Service method return types should be the unwrapped data type (T), not ApiResponse<T>
  - HTTP client calls receive the actual data directly, not wrapped responses  
  - Error handling is automatically converted to standard HTTP errors by the interceptor
  - No manual response unwrapping needed in service methods

  Component Patterns

  Form Handling:
  - Reactive Forms Only: FormBuilder and FormGroup patterns
  - Smart Validation: Custom validators with user-friendly messages
  - Auto-submit Logic: (e.g., OTP auto-submit on completion)

  State Management:
  - Service-based: No external state library needed
  - BehaviorSubject: Reactive state sharing across components
  - Subscription Management: takeUntil pattern for cleanup
  - Local Storage: Persistent user preferences and workspace selection

  Dialog Patterns:
  - Material Dialog: Consistent dialog implementation
  - Result Handling: Proper dialog result processing
  - Theme-aware: Dialogs inherit M3 theming

  Workspace Multi-tenancy

  Routing Pattern:
  /login → /workspaces → /w/:slug/dashboard

  Key Features:
  - Workspace Context: Automatic tenant isolation
  - Slug-based Routing: SEO-friendly workspace URLs
  - Role-based Guards: WorkspaceMemberGuard for access control
  - Context Persistence: Workspace selection survives browser refresh

  Error Handling & UX

  Centralized Error Processing:
  - Interceptor-based: Consistent error handling across API calls
  - User-friendly Messages: Proper error extraction and display
  - Loading States: Global loading service integration
  - Notifications: Theme-aware snackbar with semantic colors

  Development Guidelines

  TypeScript Conventions:
  - Snake Case Interfaces: Match backend API structure (following Jackson configuration)
  - Strong Typing: Comprehensive interface definitions
  - Observable Patterns: Proper RxJS lifecycle management

  Component Lifecycle:
  - OnInit/OnDestroy: Standard lifecycle implementation
  - Memory Management: Proper subscription cleanup patterns
  - Component Communication: Services for cross-component state

  Testing Patterns:
  - Component testing with Angular Testing Library approach
  - Service testing with proper mock patterns
  - E2E testing for critical user flows

  Critical Rules

  1. Material Design 3 Only: Never use Bootstrap, Tailwind, or custom UI frameworks. ALWAYS use centralized SCSS variables from src/theme/variables.scss instead of direct CSS custom properties. Make all components theme-aware.
  2. Design Token Usage: MANDATORY use of SCSS variables (vars.$color-primary) instead of CSS custom properties (var(--primary-color)) for consistency and build optimization.
  3. Component Import Pattern: Always include '@use "../../../theme/variables" as vars;' and '@use "../../../theme/mixins" as theme;' in every component SCSS file.
  4. Snake Case APIs: Interface properties match backend naming (following Jackson snake_case configuration).
  5. Standalone Components: No NgModules in new code - use standalone component architecture.
  6. Service State Management: Use Angular signals for reactive state (Angular 20 best practice), avoid BehaviorSubject, use @if @for @switch directives for template logic.
  7. Interceptor Chain: Respect established HTTP processing order (ApiResponse → Auth → Workspace → Loading).
  8. Workspace Context: All business APIs must be workspace-aware with proper tenant isolation.
  9. Theme Integration: All components must support complete M3 theming with proper color contrast and accessibility.
  10. Security First: Follow established auth patterns with JWT refresh, device tracking, and session management.
  11. Responsive Design: Use established breakpoint mixins (theme.mobile, theme.tablet) for consistent responsive behavior.
  12. Performance: Leverage build-time SCSS compilation while maintaining runtime theme switching capabilities.

  This architecture supports a scalable, secure, multi-tenant business management platform with excellent UX and maintainable code patterns.

## Recent Updates & Features (2025-01-15)

### Workspace Invitation System
- **Components**: `InvitationService`, `MemberService`, `PendingInvitationCardComponent`, `AcceptInvitationComponent`
- **Flow**: Email Link → Login (if needed) → Accept/Reject
- **Features**: Role-based invitations, expiry warnings, token validation
- **APIs**:
  - User: `/user/v1/invitation/*` (JWT only)
  - Admin: `/workspace/v1/invitation/*` (X-Workspace-ID header)
- **Integration**: Pending invitations on workspace selection page

### Enhanced Theme System
- **Modes**: `system` (default), `light`, `dark`
- **Toggle**: Cycles through all three options
- **Detection**: OS preference via `matchMedia('(prefers-color-scheme: dark)')`
- **Storage**: `app_theme_mode` localStorage key
- **Icons**: `brightness_auto`, `light_mode`, `dark_mode`
- **Service**: `ThemeMode` type, `setThemeMode()`, signal-based

### API Endpoint Migration
- **Pattern**: Singular resources with X-Workspace-ID header
- **Members**: `/workspace/v1/member/*` (was `/members`)
- **Invitations**: `/workspace/v1/invitation/*` (was `/invitations`)
- **Benefits**: Cleaner URLs, better REST compliance

### Material Design 3 Fixes
- **Content Projection**: Use `<ng-container>` for multiple nodes in `@else` blocks
- **Icon Alignment**: `vertical-align: middle` for text baseline alignment
- **Module Store**: Simplified cards, "Installed" tab, fixed scrolling

### Development Patterns
- **State**: Prefer Angular signals over BehaviorSubject
- **Components**: Standalone architecture, input/output signals
- **Styling**: SCSS variables, M3 tokens, responsive breakpoints
- **APIs**: Snake case interfaces, unwrapped responses

### Updated Best Practices
13. **Invitation Flows**: Complete auth integration with workspace context
14. **Theme Support**: Three-mode system with visual indicators
15. **API Consistency**: Singular naming, header-based context
16. **Content Projection**: `<ng-container>` for Material compliance
17. **Icon Alignment**: `vertical-align: middle` standard
18. **Signals First**: Modern reactive patterns over observables
