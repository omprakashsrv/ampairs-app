# feature:workspace

Multi-tenant workspace management. Handles workspace creation, selection, members, roles & permissions, invitations, installed modules, and workspace context switching.

## Responsibilities

- List, create, and edit workspaces
- Workspace selection and context activation (sets workspace slug for database isolation)
- Member management (invite, list, view details, assign roles)
- Invitation workflows (create, accept, list pending)
- Module availability: discover which backend modules are installed per workspace
- Roles and permissions enforcement

## Architecture

```
WorkspaceListScreen → WorkspaceListViewModel
    ↓ (workspace selected)
WorkspaceContextManager.setWorkspace(slug)
DatabaseScopeManager.clearDatabasesForWorkspace(prev)
    ↓
All workspace-scoped databases re-created on next access
```

## Key Classes

| Class | Purpose |
|---|---|
| `WorkspaceApi`, `WorkspaceMemberApi` | REST endpoints |
| `WorkspaceInvitationApi`, `WorkspaceModuleApi` | Invitation + module endpoints |
| `OfflineFirstWorkspaceRepository` | Store5 workspace data |
| `OfflineFirstWorkspaceInvitationRepository` | Store5 invitation data |
| `OfflineFirstRolesPermissionsRepository` | Roles and permissions |
| `WorkspaceContextManager` | Holds active workspace slug as `StateFlow` |
| `WorkspaceListViewModel` | Workspace picker |
| `WorkspaceMembersViewModel` | Member list and invite |
| `MemberDetailsViewModel` | Member detail and role assignment |
| `WorkspaceModulesViewModel` | Installed module list with navigation |
| `DynamicModuleNavigationService` | Maps backend module codes to local routes |
| `ModuleRegistry` | Registry of `IModuleNavigationProvider` implementations |

## Koin Module

```kotlin
workspaceModule()  // in com.ampairs.workspace (function returning module)
```

## Navigation Routes

```kotlin
WorkspaceRoute.Root
WorkspaceRoute.Create
WorkspaceRoute.Edit(workspaceId)
WorkspaceRoute.Detail(workspaceId)
WorkspaceRoute.Members(workspaceId)
WorkspaceRoute.MemberDetail(workspaceId, memberId)
WorkspaceRoute.Invitations(workspaceId)
WorkspaceRoute.CreateInvitation(workspaceId)
WorkspaceRoute.AcceptInvitation(token)
WorkspaceRoute.Modules(workspaceId)
WorkspaceRoute.ModuleStore(workspaceId)
```

## Module Code Mappings

| Backend Code | Local Route |
|---|---|
| `customer-management` | `Route.Customer` |
| `product-management` | `Route.Product` |
| `order-management` | `Route.Order` |
| `invoice-management` | `Route.Invoice` |

## Platform-Specific

Platform database factory in `androidMain`, `iosMain`, `desktopMain`.

## Database

`WorkspaceRoomDatabase` — `single` scope (exists before workspace selection, stores the workspace list itself).
