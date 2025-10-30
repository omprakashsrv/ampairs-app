# Ampairs Web Application

A modern Angular web application for business management built with Material Design 3, providing comprehensive authentication, user management, and business operations functionality.

## 🏗️ Architecture

Ampairs Web is part of a **three-tier system architecture**:

### System Integration
```
┌─────────────────┐    ┌─────────────────────────────────┐    ┌─────────────────┐
│   Angular Web   │    │        Spring Boot              │    │ Kotlin MP App   │
│   Application   │◄──►│       Backend API               │◄──►│ (Android/iOS/   │
│                 │    │                                 │    │    Desktop)     │
└─────────────────┘    │  - REST API Endpoints           │    └─────────────────┘
                       │  - JWT Authentication            │
                       │  - Multi-tenant Support          │
                       │  - Business Logic                │
                       │  - Database Management           │
                       │  - AWS S3, SNS Integration       │
                       └─────────────────────────────────┘
                                      │
                                      ▼
                               ┌─────────────┐
                               │   MySQL     │
                               │  Database   │
                               └─────────────┘
```

### Frontend Architecture
- **Framework**: Angular 18+ with standalone components
- **Design System**: Material Design 3 (M3)
- **State Management**: RxJS Observables
- **Authentication**: JWT token-based with refresh tokens
- **Routing**: Lazy-loaded modules with child routes
- **Build System**: Angular CLI with esbuild

## 📱 Features

### Authentication & Security
- **Phone/OTP Login**: SMS-based authentication flow
- **JWT Tokens**: Secure token-based authentication with refresh tokens  
- **Multi-Device Support**: Multiple concurrent sessions across devices
- **Device Management**: Track and manage active login sessions
- **Session Security**: Device-specific logout and session termination

### User Management
- **Profile Management**: Complete and update user profiles
- **Phone Verification**: SMS-based phone number verification
- **Multi-Device Sessions**: View and manage devices logged into account

### Application Layout
- **Responsive Design**: Mobile-first Material Design 3 interface
- **Navigation**: Header toolbar with logo, theme controls, and profile menu
- **Lazy Loading**: Route-based code splitting for optimal performance
- **Theme Support**: Light/dark theme switching with persistent preferences

### Business Features (Planned)
- Customer Management
- Product Catalog
- Inventory Management  
- Order Processing
- Invoice Generation
- Tally Integration

## Getting Started

### Prerequisites
- Node.js (v18 or higher)
- npm or yarn
- Angular CLI

### Installation

1. Install dependencies:
```bash
npm install
```

2. Start the development server:
```bash
npm start
```

3. Open your browser and navigate to `http://localhost:4200`

### Build

To build the project for production:
```bash
npm run build
```

## 🗂️ Project Structure

```
src/
├── app/
│   ├── auth/                          # Authentication modules
│   │   ├── login/                     # Phone login component
│   │   ├── verify-otp/                # OTP verification component
│   │   └── complete-profile/          # Profile completion component
│   ├── core/                          # Core services and guards
│   │   ├── guards/                    # Route guards (AuthGuard)
│   │   ├── interceptors/              # HTTP interceptors
│   │   ├── models/                    # TypeScript interfaces
│   │   └── services/                  # Core services (AuthService, etc.)
│   ├── home/                          # Main application layout
│   ├── pages/                         # Feature pages
│   │   ├── dashboard/                 # Dashboard component
│   │   └── devices/                   # Device management page
│   ├── shared/                        # Shared components and utilities
│   │   ├── components/                # Reusable components
│   │   └── styles/                    # Global styles and M3 theme
│   └── app.routes.ts                  # Application routing configuration
├── environments/                      # Environment configurations
└── assets/                           # Static assets
```

## 🎨 Design System

### Material Design 3 Compliance
- **Components**: Uses Angular Material 18+ with M3 design tokens
- **Color System**: Semantic color tokens for light/dark themes
- **Typography**: M3 typography scale with proper font weights
- **Elevation**: M3 shadow system for depth and hierarchy
- **State Management**: Proper focus, hover, and active states

### Styling Guidelines
- **Global Customization Only**: M3 components styled at global theme level
- **No Component-Level Overrides**: Maintains design system consistency
- **Semantic Colors**: Uses CSS custom properties for theme-aware colors
- **Responsive Design**: Mobile-first approach with breakpoint mixins

### Theme Architecture
```scss
// Material 3 Color Tokens (from src/theme/variables.scss)
$color-primary: var(--primary-color)
$color-primary-container: var(--primary-container-color)
$color-surface: var(--surface-color)
$color-surface-container: var(--surface-container-color)
$color-on-surface: var(--on-surface-color)
$color-outline-variant: var(--outline-variant-color)

// Material 3 Typography Tokens
$font-body-large: var(--mat-sys-body-large)
$font-body-medium: var(--mat-sys-body-medium)
$font-headline-large: var(--mat-sys-headline-large)
$font-headline-medium: var(--mat-sys-headline-medium)
$font-label-large: var(--mat-sys-label-large)

// Material 3 Spacing Tokens
$spacing-xs: var(--mat-sys-spacing-small, 0.125rem)    // ~2px
$spacing-sm: var(--mat-sys-spacing-medium, 0.25rem)   // ~4px
$spacing-md: var(--mat-sys-spacing-large, 0.5rem)     // ~8px
$spacing-lg: var(--mat-sys-spacing-x-large, 0.75rem)  // ~12px
$spacing-xl: var(--mat-sys-spacing-xx-large, 1rem)    // ~16px
```

## Authentication Flow

1. **Login**: User enters mobile number (+91 only)
2. **OTP Generation**: System sends 6-digit OTP via SMS
3. **Verification**: User enters OTP for verification
4. **Token Storage**: Access and refresh tokens stored in secure cookies
5. **Auto-login**: User stays logged in across sessions

## 🔗 API Integration

### Backend Communication
- **Base URL**: Configured via environment variables
- **Authentication**: JWT tokens in Authorization headers
- **Error Handling**: Standardized error response format
- **Request/Response**: JSON with snake_case naming convention

### API Response Format
```typescript
// Success Response
{
  success: true,
  data: T,
  timestamp: string
}

// Error Response  
{
  success: false,
  error: {
    code: string,
    message: string,
    details?: string,
    module: string
  },
  timestamp: string
}
```

### Key Endpoints
- `POST /auth/v1/init` - Initiate phone login
- `POST /auth/v1/verify` - Verify OTP and authenticate
- `POST /auth/v1/refresh_token` - Refresh access token
- `GET /auth/v1/devices` - List active device sessions
- `POST /auth/v1/logout` - Logout from current device

## 🛣️ Routing Structure

### Route Configuration
```typescript
// Public routes
/login                    # Phone login
/verify-otp              # OTP verification
/complete-profile        # Standalone profile completion

// Protected routes (requires authentication)
/home                    # Main application layout
  ├── /dashboard         # Default dashboard (lazy-loaded)
  ├── /profile          # Profile management (lazy-loaded)
  └── /devices          # Device sessions (lazy-loaded)
```

### Route Guards
- **AuthGuard**: Protects routes requiring authentication
- **Profile Guards**: Redirects incomplete profiles to completion flow

## 🔧 Development Guidelines

### Code Style
- **TypeScript**: Strict mode enabled
- **ESLint**: Enforced code quality rules
- **Prettier**: Automated code formatting
- **Angular Style Guide**: Official Angular conventions

### Component Development
- **Standalone Components**: Use standalone components over NgModules
- **Reactive Forms**: Prefer reactive forms over template-driven
- **OnPush Strategy**: Use OnPush change detection where possible
- **Lazy Loading**: Implement route-based lazy loading

### Material Design 3 Rules
- **No Component-Level Styling**: Only global theme customizations
- **Semantic Colors**: Use CSS custom properties for colors
- **Proper Component Usage**: Follow M3 component specifications
- **Accessibility**: Ensure WCAG compliance

## 🔐 Security Features

### Security Measures
- **JWT Authentication**: Secure token-based authentication
- **HTTPS Only**: All communications encrypted
- **CSRF Protection**: Cross-site request forgery prevention
- **Content Security Policy**: XSS attack mitigation
- **Secure Headers**: Security-focused HTTP headers

### Data Protection
- **Input Validation**: Client and server-side validation
- **Sanitization**: HTML/XSS sanitization
- **Sensitive Data**: No sensitive data in localStorage
- **Token Management**: Secure token storage and rotation

## Development

### Code Scaffolding

Run `ng generate component component-name` to generate a new component.

### Running Tests

Run `ng test` to execute unit tests via [Karma](https://karma-runner.github.io).

### Linting

Run `ng lint` to lint the project using ESLint.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the MIT License.