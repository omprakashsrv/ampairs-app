# WebSocket Event System Implementation Tasks

This document outlines the remaining implementation tasks for the Angular frontend WebSocket event system integration with the backend event infrastructure.

## Backend Status: ✅ Complete

The backend implementation is fully functional with:
- WorkspaceEvent entity with @TenantId multi-tenancy support
- WebSocket configuration with JWT authentication at `/ws` endpoint
- Device status tracking (online/away/offline) with heartbeat mechanism
- Event publishing integrated in CustomerService, ProductService, OrderService, InvoiceService
- WebSocket security with workspace-aware message routing

## Frontend Tasks: 🔄 Pending Implementation

### 1. EventSyncService - WebSocket Connection Management

**File**: `src/app/core/services/event-sync.service.ts`

**Purpose**: Manage WebSocket connection with JWT authentication and workspace context

**Implementation Details**:
```typescript
import { Injectable, inject, signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth.service';
import { WorkspaceService } from './workspace.service';

@Injectable({ providedIn: 'root' })
export class EventSyncService {
  private authService = inject(AuthService);
  private workspaceService = inject(WorkspaceService);

  private client: Client | null = null;
  private heartbeatInterval: any = null;

  // Signals for reactive state
  connectionStatus = signal<'connected' | 'connecting' | 'disconnected'>('disconnected');
  lastEventSequence = signal<number>(0);

  connect(): void {
    const token = this.authService.getAccessToken();
    const workspaceId = this.workspaceService.currentWorkspace()?.uid;

    if (!token || !workspaceId) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: {
        'Authorization': `Bearer ${token}`,
        'X-Workspace-ID': workspaceId
      },
      onConnect: () => {
        this.connectionStatus.set('connected');
        this.subscribeToWorkspaceEvents(workspaceId);
        this.startHeartbeat();
      },
      onDisconnect: () => {
        this.connectionStatus.set('disconnected');
        this.stopHeartbeat();
      }
    });

    this.client.activate();
  }

  private subscribeToWorkspaceEvents(workspaceId: string): void {
    this.client?.subscribe(`/topic/workspace.events.${workspaceId}`, (message: IMessage) => {
      const event = JSON.parse(message.body);
      this.handleWorkspaceEvent(event);
    });
  }

  private handleWorkspaceEvent(event: any): void {
    // Update sequence number
    this.lastEventSequence.set(event.sequenceNumber);

    // Dispatch to appropriate handler based on event type
    switch (event.eventType) {
      case 'CUSTOMER_CREATED':
      case 'CUSTOMER_UPDATED':
        this.handleCustomerEvent(event);
        break;
      case 'PRODUCT_UPDATED':
      case 'PRODUCT_STOCK_CHANGED':
        this.handleProductEvent(event);
        break;
      // ... other event types
    }
  }

  private startHeartbeat(): void {
    this.heartbeatInterval = setInterval(() => {
      this.client?.publish({
        destination: '/app/heartbeat',
        body: JSON.stringify({ timestamp: Date.now() })
      });
    }, 30000); // Every 30 seconds
  }

  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  disconnect(): void {
    this.stopHeartbeat();
    this.client?.deactivate();
  }
}
```

**Key Features**:
- Uses Angular 20 signals for reactive state management
- Injects existing AuthService and WorkspaceService
- SockJS fallback for browser compatibility
- JWT token in connection headers
- Workspace-aware topic subscription: `/topic/workspace.events.{workspaceId}`
- 30-second heartbeat interval
- Automatic event dispatching based on type

**Integration Points**:
- Call `connect()` after successful workspace selection
- Call `disconnect()` on logout or workspace change
- Subscribe to `connectionStatus` signal for UI indicators

---

### 2. Heartbeat Mechanism

**File**: `src/app/core/services/event-sync.service.ts` (integrated above)

**Purpose**: Send periodic keep-alive messages to maintain online status

**Implementation Details**:
- Interval: 30 seconds (matches backend `DeviceStatusService` detection)
- Endpoint: `/app/heartbeat`
- Payload: `{ timestamp: number }`
- Automatic start on connection, stop on disconnect

**Backend Correlation**:
- Backend marks sessions as "away" after 30s without heartbeat
- Backend marks sessions as "offline" after 2 minutes
- Heartbeat updates `WebSocketSession.lastHeartbeat` timestamp

---

### 3. DeviceStatusService - Active User Tracking

**File**: `src/app/core/services/device-status.service.ts`

**Purpose**: Track active users/devices in the current workspace

**Implementation Details**:
```typescript
import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EventSyncService } from './event-sync.service';

export interface DeviceStatus {
  userId: string;
  deviceId: string;
  status: 'ONLINE' | 'AWAY' | 'OFFLINE';
  lastHeartbeat: string;
}

@Injectable({ providedIn: 'root' })
export class DeviceStatusService {
  private http = inject(HttpClient);
  private eventSync = inject(EventSyncService);

  activeDevices = signal<DeviceStatus[]>([]);

  async loadActiveDevices(): Promise<void> {
    const devices = await this.http.get<DeviceStatus[]>('/api/v1/devices/active').toPromise();
    this.activeDevices.set(devices);
  }

  // Subscribe to USER_STATUS_CHANGED events from EventSyncService
  handleStatusChangeEvent(event: any): void {
    const payload = JSON.parse(event.payload);
    const devices = this.activeDevices();

    const index = devices.findIndex(d => d.deviceId === payload.deviceId);
    if (index >= 0) {
      devices[index] = { ...devices[index], status: payload.newStatus };
      this.activeDevices.set([...devices]);
    } else if (payload.newStatus === 'ONLINE') {
      this.activeDevices.set([...devices, payload]);
    }
  }
}
```

**API Endpoint**:
- `GET /api/v1/devices/active` - Returns active devices in workspace

**Features**:
- Signal-based reactive state for active devices list
- Real-time updates via WebSocket events
- Integration with EventSyncService for status changes

**UI Integration**:
- Display active user count in header/sidebar
- Show online/away/offline indicators next to user avatars
- Filter device list by status

---

### 4. Status Indicator UI Component

**File**: `src/app/shared/components/device-status-indicator/device-status-indicator.component.ts`

**Purpose**: Visual indicator for device online/away/offline status

**Implementation Details**:
```typescript
import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-device-status-indicator',
  standalone: true,
  imports: [MatIconModule, MatTooltipModule],
  template: `
    <span class="status-indicator"
          [class.online]="status() === 'ONLINE'"
          [class.away]="status() === 'AWAY'"
          [class.offline]="status() === 'OFFLINE'"
          [matTooltip]="tooltipText()">
      <mat-icon [fontIcon]="iconName()"></mat-icon>
    </span>
  `,
  styles: [`
    @use '../../../theme/variables' as vars;

    .status-indicator {
      display: inline-flex;
      align-items: center;

      &.online mat-icon {
        color: vars.$color-success;
      }

      &.away mat-icon {
        color: vars.$color-warning;
      }

      &.offline mat-icon {
        color: vars.$color-on-surface-variant;
      }
    }
  `]
})
export class DeviceStatusIndicatorComponent {
  status = input.required<'ONLINE' | 'AWAY' | 'OFFLINE'>();

  iconName = computed(() => {
    switch (this.status()) {
      case 'ONLINE': return 'circle';
      case 'AWAY': return 'schedule';
      case 'OFFLINE': return 'cancel';
    }
  });

  tooltipText = computed(() => {
    switch (this.status()) {
      case 'ONLINE': return 'Online';
      case 'AWAY': return 'Away';
      case 'OFFLINE': return 'Offline';
    }
  });
}
```

**Features**:
- Material Design 3 compliant (uses M3 color tokens)
- Signal-based input for reactive updates
- Tooltip for accessibility
- Icon indicators: circle (online), schedule (away), cancel (offline)
- Semantic color coding: success (green), warning (yellow), variant (gray)

**Usage**:
```html
<app-device-status-indicator [status]="device.status" />
```

---

### 5. Event Handling Interceptor - State Synchronization

**File**: `src/app/core/services/event-handler.service.ts`

**Purpose**: Route incoming WebSocket events to appropriate state management services

**Implementation Details**:
```typescript
import { Injectable, inject } from '@angular/core';
import { CustomerService } from './customer.service';
import { ProductService } from './product.service';
import { OrderService } from './order.service';
import { InvoiceService } from './invoice.service';

@Injectable({ providedIn: 'root' })
export class EventHandlerService {
  private customerService = inject(CustomerService);
  private productService = inject(ProductService);
  private orderService = inject(OrderService);
  private invoiceService = inject(InvoiceService);

  handleEvent(event: any): void {
    const eventType = event.eventType;
    const entityId = event.entityId;
    const payload = JSON.parse(event.payload);

    switch (eventType) {
      case 'CUSTOMER_CREATED':
      case 'CUSTOMER_UPDATED':
        this.customerService.handleRemoteUpdate(entityId, payload);
        break;

      case 'CUSTOMER_DELETED':
        this.customerService.handleRemoteDelete(entityId);
        break;

      case 'PRODUCT_UPDATED':
        this.productService.handleRemoteUpdate(entityId, payload);
        break;

      case 'PRODUCT_STOCK_CHANGED':
        this.productService.handleStockChange(entityId, payload.newStock);
        break;

      case 'ORDER_STATUS_CHANGED':
        this.orderService.handleStatusChange(entityId, payload.newStatus);
        break;

      case 'INVOICE_UPDATED':
        this.invoiceService.handleRemoteUpdate(entityId, payload);
        break;

      // ... other event types
    }
  }
}
```

**Service Integration Pattern**:
Each domain service (CustomerService, ProductService, etc.) should implement:

```typescript
// Example: CustomerService
export class CustomerService {
  private customers = signal<Customer[]>([]);

  handleRemoteUpdate(entityId: string, changes: any): void {
    const current = this.customers();
    const index = current.findIndex(c => c.uid === entityId);

    if (index >= 0) {
      // Update existing customer
      current[index] = { ...current[index], ...changes };
      this.customers.set([...current]);
    } else {
      // Fetch new customer from API
      this.fetchCustomer(entityId);
    }
  }

  handleRemoteDelete(entityId: string): void {
    const current = this.customers();
    this.customers.set(current.filter(c => c.uid !== entityId));
  }
}
```

**Key Patterns**:
- Use signals for reactive state updates
- Immutable updates (spread operators)
- Fetch from API if entity not in local state
- Optimistic UI updates with conflict resolution

---

### 6. End-to-End Testing

**Test Scenarios**:

#### Scenario 1: Multi-Device Customer Update
**Goal**: Verify real-time synchronization across devices

**Steps**:
1. Open workspace in browser tab 1 (Device A)
2. Open same workspace in browser tab 2 (Device B)
3. Both devices should show "Connected" status indicator
4. In Device A: Update customer name "John Doe" → "Jane Doe"
5. Verify Device B receives CUSTOMER_UPDATED event
6. Verify Device B UI updates customer name automatically
7. Check console logs for event payload and sequence number

**Expected Results**:
- Event received within 500ms on Device B
- UI updates without page refresh
- No duplicate events or race conditions

#### Scenario 2: Device Status Tracking
**Goal**: Verify online/away/offline status detection

**Steps**:
1. Connect Device A to workspace
2. Verify Device A shows as "Online" in active devices list
3. Wait 30+ seconds without interaction
4. Verify Device A status changes to "Away"
5. Close tab (disconnect WebSocket)
6. Wait 2 minutes
7. Verify Device A status changes to "Offline"

**Expected Results**:
- Status transitions: Online → Away (30s) → Offline (2min)
- Other connected devices see status changes immediately
- Stale sessions cleaned up by backend scheduled job

#### Scenario 3: Offline Sync (Catch-up)
**Goal**: Verify offline devices receive missed events on reconnection

**Steps**:
1. Device A connected and online
2. Disconnect Device B (close tab)
3. In Device A: Create 5 new customers
4. Reconnect Device B
5. Device B should request events since last sequence number
6. Verify all 5 customer create events are processed
7. Verify Device B UI shows all new customers

**Expected Results**:
- Device B fetches events where `sequenceNumber > lastAcknowledged`
- All missed events applied in order
- No data loss or inconsistencies

---

## Implementation Checklist

### Phase 1: Core WebSocket Infrastructure
- [ ] Install dependencies: `@stomp/stompjs`, `sockjs-client`
- [ ] Create EventSyncService with JWT authentication
- [ ] Implement WebSocket connection management
- [ ] Add heartbeat mechanism (30-second interval)
- [ ] Test connection with backend `/ws` endpoint

### Phase 2: Device Status Tracking
- [ ] Create DeviceStatusService
- [ ] Implement `GET /api/v1/devices/active` API call
- [ ] Subscribe to USER_STATUS_CHANGED events
- [ ] Create DeviceStatusIndicatorComponent
- [ ] Add status indicators to user avatars in UI

### Phase 3: Event Handling
- [ ] Create EventHandlerService
- [ ] Add event routing to domain services
- [ ] Implement `handleRemoteUpdate()` in CustomerService
- [ ] Implement `handleRemoteUpdate()` in ProductService
- [ ] Implement `handleRemoteUpdate()` in OrderService
- [ ] Implement `handleRemoteUpdate()` in InvoiceService
- [ ] Add conflict resolution strategies

### Phase 4: UI Integration
- [ ] Add connection status indicator to app header
- [ ] Display active user count in workspace dashboard
- [ ] Show event notification toasts for critical updates
- [ ] Add "Syncing..." loader during event processing
- [ ] Handle reconnection UI (retry button, error messages)

### Phase 5: Testing & Optimization
- [ ] Test multi-device customer update scenario
- [ ] Test device status tracking (online/away/offline)
- [ ] Test offline sync catch-up mechanism
- [ ] Test workspace switching (disconnect/reconnect)
- [ ] Add error handling for WebSocket failures
- [ ] Implement exponential backoff for reconnection
- [ ] Add telemetry for event latency monitoring

---

## Backend API Endpoints Reference

### WebSocket
- **Endpoint**: `ws://localhost:8080/ws` (SockJS)
- **Authentication**: JWT token in `Authorization` header
- **Workspace Context**: `X-Workspace-ID` header
- **Topics**: `/topic/workspace.events.{workspaceId}`
- **Application Prefix**: `/app/heartbeat`

### REST APIs
- **GET /api/v1/events?since={sequenceNumber}** - Fetch missed events for offline sync
- **POST /api/v1/events/acknowledge** - Mark events as consumed
- **GET /api/v1/devices/active** - List active devices in workspace

---

## Configuration Notes

### Environment Variables
```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  wsUrl: 'http://localhost:8080/ws',
  apiUrl: 'http://localhost:8080/api/v1'
};
```

### CORS Configuration
Backend already configured in `WebSocketConfig.kt`:
```kotlin
.setAllowedOriginPatterns("*")
```

### Security Considerations
- JWT token validation on WebSocket handshake
- Workspace isolation via topic subscriptions
- Device ID tracked via `X-Device-ID` header (from AuthInterceptor)
- No cross-workspace message leakage

---

## Material Design 3 Compliance

All UI components must follow established patterns:

```scss
@use '../../../theme/variables' as vars;
@use '../../../theme/mixins' as theme;

.status-indicator {
  background-color: vars.$color-surface-container;
  color: vars.$color-on-surface;
  font: vars.$font-label-medium;
  padding: vars.$spacing-sm vars.$spacing-md;
  border-radius: vars.$border-radius-md;

  &.online {
    color: vars.$color-success;
  }

  &.away {
    color: vars.$color-warning;
  }
}
```

**Key Rules**:
- Use SCSS variables from `src/theme/variables.scss`
- Follow M3 typography tokens (`vars.$font-body-medium`, etc.)
- Use M3 color system (`vars.$color-primary`, `vars.$color-success`)
- Support light/dark theme switching
- Use Material icons only

---

## Dependencies

Already installed in `package.json`:
```json
{
  "@stomp/stompjs": "^7.2.0",
  "sockjs-client": "^1.6.1"
}
```

TypeScript types:
```bash
npm install --save-dev @types/sockjs-client
```

---

## Summary

**Backend Status**: ✅ Fully implemented and tested
- Event persistence with sequence-based ordering
- WebSocket configuration with JWT auth
- Multi-tenant workspace isolation
- Device status tracking with heartbeat
- Event publishing in all domain services

**Frontend Status**: 🔄 Ready for implementation
- Architecture designed for Angular 20 signals
- Integration points with existing services defined
- Material Design 3 compliant component patterns
- Comprehensive testing scenarios documented

**Next Steps**:
1. Start with Phase 1 (Core WebSocket Infrastructure)
2. Test connection with backend incrementally
3. Add event handling for one domain (Customer) as proof-of-concept
4. Expand to other domains (Product, Order, Invoice)
5. Complete UI integration and testing

**Estimated Effort**: 3-4 days for full frontend implementation
