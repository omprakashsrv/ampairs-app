# Ampairs Design System Guidelines

## Overview

This document outlines the design system guidelines for the Ampairs Angular web application, built on **Material Design 3 (M3)** principles with a centralized token-based approach for consistency, maintainability, and accessibility.

## 🎨 Design System Architecture

### Centralized Design Tokens
All design tokens are centralized in `/src/theme/variables.scss` using SCSS variables that reference CSS custom properties for runtime theme switching.

```scss
// Example pattern
$color-primary: var(--primary-color);
$spacing-lg: var(--mat-sys-spacing-large, 1.5rem);
```

### Benefits
- **Single Source of Truth**: All design tokens in one location
- **Runtime Theming**: Support for light/dark mode switching
- **Build-Time Optimization**: SCSS variable resolution
- **Type Safety**: Better development experience with IntelliSense

---

## 🎯 Color System

### Usage Pattern
Always use SCSS variables from the theme system:

```scss
// ✅ CORRECT
@use '../../../theme/variables' as vars;

.component {
  background-color: vars.$color-surface-container;
  color: vars.$color-on-surface;
  border: 1px solid vars.$color-outline-variant;
}
```

```scss
// ❌ INCORRECT - Don't use CSS custom properties directly
.component {
  background-color: var(--surface-container-color);
  color: var(--on-surface-color);
}
```

### Color Categories

#### Primary Colors
- `$color-primary` - Main brand color
- `$color-primary-container` - Containers with primary color
- `$color-on-primary` - Text/icons on primary
- `$color-on-primary-container` - Text/icons on primary containers

#### Surface Colors
- `$color-surface` - Default surface
- `$color-surface-container` - Container surfaces
- `$color-surface-container-low` - Lower emphasis containers
- `$color-surface-container-high` - Higher emphasis containers
- `$color-on-surface` - Text/icons on surfaces
- `$color-on-surface-variant` - Secondary text on surfaces

#### Semantic Colors
- `$color-error` - Error states
- `$color-success` - Success states
- `$color-warning` - Warning states
- `$color-info` - Information states

---

## 📏 Spacing System

### Spacing Scale
```scss
$spacing-xs: var(--mat-sys-spacing-x-small, 0.25rem);    // 4px
$spacing-sm: var(--mat-sys-spacing-small, 0.5rem);       // 8px
$spacing-md: var(--mat-sys-spacing-medium, 0.75rem);     // 12px
$spacing-lg: var(--mat-sys-spacing-large, 1rem);         // 16px
$spacing-xl: var(--mat-sys-spacing-x-large, 1.5rem);     // 24px
$spacing-xxl: var(--mat-sys-spacing-xx-large, 2rem);     // 32px
```

### Usage Guidelines
- **xs**: Tight spacing within components (gaps between related items)
- **sm**: Small padding, tight component spacing
- **md**: Standard gap between UI elements
- **lg**: Standard padding, comfortable spacing
- **xl**: Generous spacing, section dividers
- **xxl**: Large padding, page-level spacing

---

## 🔤 Typography System

### Font Sizes
```scss
$font-size-xs: var(--mat-sys-typescale-label-small-size, 0.6875rem);   // 11px
$font-size-sm: var(--mat-sys-typescale-label-medium-size, 0.75rem);    // 12px
$font-size-md: var(--mat-sys-typescale-body-medium-size, 0.875rem);    // 14px
$font-size-lg: var(--mat-sys-typescale-title-medium-size, 1rem);       // 16px
$font-size-xl: var(--mat-sys-typescale-title-large-size, 1.375rem);    // 22px
$font-size-title: var(--mat-sys-typescale-headline-small-size, 1.5rem); // 24px
```

### Typography Usage
- **xs**: Small labels, captions
- **sm**: Supporting text, metadata
- **md**: Body text, standard UI text
- **lg**: Subheadings, important labels
- **xl**: Section headers
- **title**: Page titles, main headings

---

## 🏗️ Layout & Structure

### Border Radius
```scss
$border-radius-sm: var(--mat-sys-corner-extra-small, 0.25rem);  // 4px
$border-radius-md: var(--mat-sys-corner-small, 0.5rem);         // 8px
$border-radius-lg: var(--mat-sys-corner-medium, 1rem);          // 16px
$border-radius-xl: var(--mat-sys-corner-large, 1rem);           // 16px
$border-radius-round: var(--mat-sys-corner-full, 625rem);       // Full round
```

### Shadows
```scss
$shadow-1: var(--shadow-1);  // Light elevation
$shadow-2: var(--shadow-2);  // Medium elevation
```

### Transitions
```scss
$transition-fast: 0.15s;
$transition-normal: 0.3s;
$transition-slow: 0.5s;
$transition-standard: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);  // M3 easing
```

---

## 📱 Component Guidelines

### Header Components
```scss
.app-header-toolbar {
  background-color: vars.$color-surface-container;
  color: vars.$color-on-surface;
  padding: 0 vars.$spacing-xl;
  
  // Mobile responsive
  @media (max-width: vars.$breakpoint-sm) {
    padding: 0 vars.$spacing-md;
  }
}
```

### Card Components
```scss
.card {
  background-color: vars.$color-surface-container;
  border-radius: vars.$border-radius-lg;
  padding: vars.$spacing-xl;
  box-shadow: vars.$shadow-1;
  
  &:hover {
    box-shadow: vars.$shadow-2;
    transform: translateY(-2px);
  }
}
```

### Button Components
```scss
.button {
  transition: vars.$transition-standard;
  border-radius: vars.$border-radius-md;
  
  &.primary {
    background-color: vars.$color-primary;
    color: vars.$color-on-primary;
  }
  
  &.secondary {
    background-color: vars.$color-secondary-container;
    color: vars.$color-on-secondary-container;
  }
}
```

---

## 📐 Responsive Design

### Breakpoints
```scss
$breakpoint-xs: 480px;   // Extra small devices
$breakpoint-sm: 768px;   // Small devices
$breakpoint-md: 992px;   // Medium devices
$breakpoint-lg: 1200px;  // Large devices
$breakpoint-xl: 1440px;  // Extra large devices
```

### Mixins Usage
```scss
@use '../../../theme/mixins' as theme;

.component {
  padding: vars.$spacing-xxl;
  
  @include theme.tablet {
    padding: vars.$spacing-xl;
  }
  
  @include theme.mobile {
    padding: vars.$spacing-lg;
  }
}
```

---

## 🎭 Theme Implementation

### Light/Dark Mode Support
All color tokens automatically support theme switching:

```scss
// Automatically adapts to light/dark theme
.component {
  background-color: vars.$color-surface-container;  // Changes based on theme
  color: vars.$color-on-surface;                    // Proper contrast maintained
}
```

### Theme-Aware Properties
For Material Component internal properties:

```scss
.material-component {
  --mat-tab-header-active-label-text-color: #{vars.$color-primary};
  --mdc-slider-active-track-color: #{vars.$color-primary};
}
```

---

## ✅ Best Practices

### Component Structure
1. **Always import theme variables**:
   ```scss
   @use '../../../theme/variables' as vars;
   @use '../../../theme/mixins' as theme;
   ```

2. **Use semantic color tokens**:
   ```scss
   // ✅ Semantic and contextual
   background-color: vars.$color-surface-container;
   
   // ❌ Avoid direct color values
   background-color: #f5f5f5;
   ```

3. **Consistent spacing**:
   ```scss
   // ✅ Use design system spacing
   padding: vars.$spacing-lg vars.$spacing-xl;
   
   // ❌ Avoid arbitrary values
   padding: 18px 25px;
   ```

### Performance Optimization
- **Build-time resolution**: SCSS variables are resolved during build
- **Runtime theming**: CSS custom properties enable dynamic theme switching
- **Reduced bundle size**: Centralized tokens reduce duplication

### Accessibility
- **Color contrast**: All color combinations meet WCAG AA standards
- **Focus indicators**: Consistent focus styling with `$color-primary`
- **Touch targets**: Minimum 44px touch targets on mobile

---

## 🔧 Migration Guidelines

When updating existing components:

1. **Add variables import**:
   ```scss
   @use '../../../theme/variables' as vars;
   ```

2. **Replace hardcoded values**:
   ```scss
   // Before
   color: #333333;
   padding: 16px;
   
   // After
   color: vars.$color-on-surface;
   padding: vars.$spacing-lg;
   ```

3. **Update CSS custom properties**:
   ```scss
   // Before
   color: var(--primary-color);
   
   // After
   color: vars.$color-primary;
   ```

4. **Test both themes**: Verify component appearance in light and dark modes

---

## 📚 Resources

- **Variables File**: `/src/theme/variables.scss`
- **Mixins File**: `/src/theme/mixins.scss`
- **Global Styles**: `/src/styles.scss`
- **Material Design 3**: [m3.material.io](https://m3.material.io)
- **Angular Material**: [material.angular.io](https://material.angular.io)

---

## 🚀 Benefits Achieved

✅ **Consistency**: Single source of truth for all design tokens
✅ **Maintainability**: Easy to update themes and spacing globally  
✅ **Accessibility**: WCAG compliant color contrast ratios
✅ **Performance**: Optimized build-time compilation
✅ **Developer Experience**: IntelliSense support and type safety
✅ **Theme Support**: Seamless light/dark mode switching
✅ **Scalability**: Easy to extend and modify the design system

This design system ensures a cohesive, accessible, and maintainable user interface across the entire Ampairs application.