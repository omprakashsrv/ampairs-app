import {computed, DOCUMENT, Inject, Injectable, signal} from '@angular/core';
import {toObservable} from '@angular/core/rxjs-interop';

/**
 * Material Design 3 Theme Configuration
 * Contains all M3 semantic color roles and properties
 */
export interface M3ThemeConfig {
  readonly name: string;
  readonly displayName: string;
  readonly isDark: boolean;
  readonly colors: {
    // Primary colors
    readonly primary: string;
    readonly onPrimary: string;
    readonly primaryContainer: string;
    readonly onPrimaryContainer: string;

    // Secondary colors
    readonly secondary: string;
    readonly onSecondary: string;
    readonly secondaryContainer: string;
    readonly onSecondaryContainer: string;

    // Tertiary colors
    readonly tertiary: string;
    readonly onTertiary: string;
    readonly tertiaryContainer: string;
    readonly onTertiaryContainer: string;

    // Error colors
    readonly error: string;
    readonly onError: string;
    readonly errorContainer: string;
    readonly onErrorContainer: string;

    // Surface colors
    readonly surface: string;
    readonly onSurface: string;
    readonly surfaceVariant: string;
    readonly onSurfaceVariant: string;
    readonly surfaceContainer: string;
    readonly surfaceContainerLow: string;
    readonly surfaceContainerHigh: string;
    readonly surfaceContainerHighest: string;

    // Background colors
    readonly background: string;
    readonly onBackground: string;

    // Outline colors
    readonly outline: string;
    readonly outlineVariant: string;

    // Inverse colors
    readonly inverseSurface: string;
    readonly inverseOnSurface: string;
    readonly inversePrimary: string;
  };
}

/**
 * Theme mode configuration
 */
export type ThemeMode = 'system' | 'light' | 'dark';

/**
 * Material Design 3 Density Configuration
 */
export interface M3DensityConfig {
  readonly level: number; // -5 to 0 (most compact to most spacious)
  readonly name: string;
  readonly description: string;
}

/**
 * Material Design 3 Typography Configuration
 */
export interface M3TypographyConfig {
  readonly name: string;
  readonly fontFamily: string;
  readonly scale: {
    readonly displayLarge: { size: string; lineHeight: string; weight: number; };
    readonly displayMedium: { size: string; lineHeight: string; weight: number; };
    readonly displaySmall: { size: string; lineHeight: string; weight: number; };
    readonly headlineLarge: { size: string; lineHeight: string; weight: number; };
    readonly headlineMedium: { size: string; lineHeight: string; weight: number; };
    readonly headlineSmall: { size: string; lineHeight: string; weight: number; };
    readonly titleLarge: { size: string; lineHeight: string; weight: number; };
    readonly titleMedium: { size: string; lineHeight: string; weight: number; };
    readonly titleSmall: { size: string; lineHeight: string; weight: number; };
    readonly bodyLarge: { size: string; lineHeight: string; weight: number; };
    readonly bodyMedium: { size: string; lineHeight: string; weight: number; };
    readonly bodySmall: { size: string; lineHeight: string; weight: number; };
    readonly labelLarge: { size: string; lineHeight: string; weight: number; };
    readonly labelMedium: { size: string; lineHeight: string; weight: number; };
    readonly labelSmall: { size: string; lineHeight: string; weight: number; };
  };
}

/**
 * Material Design 3 Theme Service
 *
 * Provides comprehensive theming support following Material Design 3 specifications:
 * - Semantic color roles and design tokens
 * - Dynamic density adjustment
 * - Typography scale management
 * - Theme persistence and import/export
 * - System theme detection support
 */
@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  // Storage keys for persistence
  private static readonly STORAGE_KEYS = {
    themeMode: 'app_theme_mode',
    density: 'app_density',
    typography: 'app_typography'
  } as const;

  // M3 Color palettes generated from brand colors (#667eea, #764ba2, #4caf50)
  private readonly themes: ReadonlyArray<M3ThemeConfig> = [
    {
      name: 'light',
      displayName: 'Light',
      isDark: false,
      colors: {
        primary: '#576fda',
        onPrimary: '#ffffff',
        primaryContainer: '#dde1ff',
        onPrimaryContainer: '#001356',

        secondary: '#8f63bc',
        onSecondary: '#ffffff',
        secondaryContainer: '#f0dbff',
        onSecondaryContainer: '#2c0051',

        tertiary: '#41a447',
        onTertiary: '#ffffff',
        tertiaryContainer: '#c8ffc0',
        onTertiaryContainer: '#002204',

        error: '#de3730',
        onError: '#ffffff',
        errorContainer: '#ffdad6',
        onErrorContainer: '#410002',

        surface: '#fefbff',
        onSurface: '#1a1b22',
        surfaceVariant: '#e3e1eb',
        onSurfaceVariant: '#46464e',
        surfaceContainer: '#f1f0f9',
        surfaceContainerLow: '#fbf8ff',
        surfaceContainerHigh: '#eeedf6',
        surfaceContainerHighest: '#e3e1eb',

        background: '#fefbff',
        onBackground: '#1a1b22',

        outline: '#76767f',
        outlineVariant: '#c7c5cf',

        inverseSurface: '#2f3036',
        inverseOnSurface: '#f1f0f7',
        inversePrimary: '#b9c3ff'
      }
    },
    {
      name: 'dark',
      displayName: 'Dark',
      isDark: true,
      colors: {
        primary: '#b9c3ff',
        onPrimary: '#001356',
        primaryContainer: '#3c55bf',
        onPrimaryContainer: '#dde1ff',

        secondary: '#dcb8ff',
        onSecondary: '#2c0051',
        secondaryContainer: '#754aa1',
        onSecondaryContainer: '#f0dbff',

        tertiary: '#94f990',
        onTertiary: '#002204',
        tertiaryContainer: '#006e1c',
        onTertiaryContainer: '#c8ffc0',

        error: '#ffb4ab',
        onError: '#690005',
        errorContainer: '#ba1a1a',
        onErrorContainer: '#ffdad6',

        surface: '#121319',
        onSurface: '#e3e1eb',
        surfaceVariant: '#46464e',
        onSurfaceVariant: '#c7c5cf',
        surfaceContainer: '#1e1f26',
        surfaceContainerLow: '#1a1b22',
        surfaceContainerHigh: '#292931',
        surfaceContainerHighest: '#34343c',

        background: '#0d0e14',
        onBackground: '#e3e1eb',

        outline: '#909099',
        outlineVariant: '#46464e',

        inverseSurface: '#e3e1eb',
        inverseOnSurface: '#2f3036',
        inversePrimary: '#576fda'
      }
    }
  ];

  // M3 Density levels
  private readonly densityLevels: ReadonlyArray<M3DensityConfig> = [
    {level: -5, name: 'Maximum', description: 'Most compact - Maximum information density'},
    {level: -4, name: 'High', description: 'High density - More content, less spacing'},
    {level: -3, name: 'Medium-High', description: 'Moderately high density'},
    {level: -2, name: 'Medium', description: 'Balanced density - Recommended default'},
    {level: -1, name: 'Medium-Low', description: 'More spacious than default'},
    {level: 0, name: 'Default', description: 'Standard Material Design spacing'}
  ];

  // M3 Typography configurations
  private readonly typographyConfigs: ReadonlyArray<M3TypographyConfig> = [
    {
      name: 'Roboto',
      fontFamily: 'Roboto, "Helvetica Neue", sans-serif',
      scale: {
        displayLarge: {size: '3.5rem', lineHeight: '4rem', weight: 400},
        displayMedium: {size: '2.8rem', lineHeight: '3.2rem', weight: 400},
        displaySmall: {size: '2.25rem', lineHeight: '2.8rem', weight: 400},
        headlineLarge: {size: '2rem', lineHeight: '2.5rem', weight: 400},
        headlineMedium: {size: '1.75rem', lineHeight: '2.25rem', weight: 400},
        headlineSmall: {size: '1.5rem', lineHeight: '2rem', weight: 400},
        titleLarge: {size: '1.375rem', lineHeight: '1.75rem', weight: 400},
        titleMedium: {size: '1rem', lineHeight: '1.5rem', weight: 500},
        titleSmall: {size: '0.875rem', lineHeight: '1.25rem', weight: 500},
        bodyLarge: {size: '1rem', lineHeight: '1.5rem', weight: 400},
        bodyMedium: {size: '0.875rem', lineHeight: '1.25rem', weight: 400},
        bodySmall: {size: '0.75rem', lineHeight: '1rem', weight: 400},
        labelLarge: {size: '0.875rem', lineHeight: '1.25rem', weight: 500},
        labelMedium: {size: '0.75rem', lineHeight: '1rem', weight: 500},
        labelSmall: {size: '0.6875rem', lineHeight: '1rem', weight: 500}
      }
    },
    {
      name: 'Inter',
      fontFamily: 'Inter, "Helvetica Neue", sans-serif',
      scale: {
        displayLarge: {size: '3.5rem', lineHeight: '4rem', weight: 300},
        displayMedium: {size: '2.8rem', lineHeight: '3.2rem', weight: 300},
        displaySmall: {size: '2.25rem', lineHeight: '2.8rem', weight: 400},
        headlineLarge: {size: '2rem', lineHeight: '2.5rem', weight: 400},
        headlineMedium: {size: '1.75rem', lineHeight: '2.25rem', weight: 400},
        headlineSmall: {size: '1.5rem', lineHeight: '2rem', weight: 400},
        titleLarge: {size: '1.375rem', lineHeight: '1.75rem', weight: 400},
        titleMedium: {size: '1rem', lineHeight: '1.5rem', weight: 500},
        titleSmall: {size: '0.875rem', lineHeight: '1.25rem', weight: 500},
        bodyLarge: {size: '1rem', lineHeight: '1.5rem', weight: 400},
        bodyMedium: {size: '0.875rem', lineHeight: '1.25rem', weight: 400},
        bodySmall: {size: '0.75rem', lineHeight: '1rem', weight: 400},
        labelLarge: {size: '0.875rem', lineHeight: '1.25rem', weight: 500},
        labelMedium: {size: '0.75rem', lineHeight: '1rem', weight: 500},
        labelSmall: {size: '0.6875rem', lineHeight: '1rem', weight: 500}
      }
    }
  ];

  // Signal-based state management
  private readonly _themeMode = signal<ThemeMode>('system');
  private readonly _currentTheme = signal<M3ThemeConfig>(this.themes[0]!);
  private readonly _currentDensity = signal<M3DensityConfig>(this.densityLevels[3]!); // Medium density (-2)
  private readonly _currentTypography = signal<M3TypographyConfig>(this.typographyConfigs[0]!);

  // Public readonly signals
  public readonly themeMode = this._themeMode.asReadonly();
  public readonly currentTheme = this._currentTheme.asReadonly();
  public readonly currentDensity = this._currentDensity.asReadonly();
  public readonly currentTypography = this._currentTypography.asReadonly();

  // Backward compatibility observables
  public readonly themeMode$ = toObservable(this._themeMode);
  public readonly currentTheme$ = toObservable(this._currentTheme);
  public readonly currentDensity$ = toObservable(this._currentDensity);
  public readonly currentTypography$ = toObservable(this._currentTypography);

  // Computed signals
  public readonly isDarkMode = computed(() => this._currentTheme().isDark);
  public readonly currentThemeName = computed(() => this._currentTheme().name);
  public readonly currentDensityLevel = computed(() => this._currentDensity().level);
  public readonly currentTypographyName = computed(() => this._currentTypography().name);

  constructor(@Inject(DOCUMENT) private document: Document) {
    this.initializeTheme();
  }

  // Public API
  public getAvailableThemes(): ReadonlyArray<M3ThemeConfig> {
    return this.themes;
  }

  public getAvailableDensities(): ReadonlyArray<M3DensityConfig> {
    return this.densityLevels;
  }

  public getAvailableTypographies(): ReadonlyArray<M3TypographyConfig> {
    return this.typographyConfigs;
  }

  public getCurrentTheme(): M3ThemeConfig {
    return this._currentTheme();
  }

  public getCurrentDensity(): M3DensityConfig {
    return this._currentDensity();
  }

  public getCurrentTypography(): M3TypographyConfig {
    return this._currentTypography();
  }

  public getCurrentThemeMode(): ThemeMode {
    return this._themeMode();
  }

  public setTheme(themeName: string): void {
    const theme = this.themes.find(t => t.name === themeName);
    if (theme) {
      this.applyTheme(theme);
      // Note: Individual theme setting doesn't persist mode, only direct theme changes
    }
  }

  public setThemeMode(mode: ThemeMode): void {
    this._themeMode.set(mode);
    localStorage.setItem(ThemeService.STORAGE_KEYS.themeMode, mode);
    this.applyThemeMode(mode);
  }

  public setDensity(level: number): void {
    const density = this.densityLevels.find(d => d.level === level);
    if (density) {
      this.applyDensity(density);
      localStorage.setItem(ThemeService.STORAGE_KEYS.density, level.toString());
    }
  }

  public setTypography(typographyName: string): void {
    const typography = this.typographyConfigs.find(t => t.name === typographyName);
    if (typography) {
      this.applyTypography(typography);
      const index = this.typographyConfigs.indexOf(typography);
      localStorage.setItem(ThemeService.STORAGE_KEYS.typography, index.toString());
    }
  }

  public toggleTheme(): void {
    const currentMode = this._themeMode();
    const nextMode: ThemeMode = currentMode === 'system' ? 'light' :
                                currentMode === 'light' ? 'dark' : 'system';
    this.setThemeMode(nextMode);
  }

  public resetToDefaults(): void {
    this.setThemeMode('system');
    this.setDensity(-2); // Medium density
    this.setTypography('Roboto');
  }

  public exportConfiguration(): string {
    return JSON.stringify({
      themeMode: this.getCurrentThemeMode(),
      density: this.getCurrentDensity().level,
      typography: this.getCurrentTypography().name,
      version: '1.0',
      timestamp: new Date().toISOString()
    }, null, 2);
  }

  public importConfiguration(configJson: string): boolean {
    try {
      const config = JSON.parse(configJson);

      // Support both old and new format
      if (config.themeMode) this.setThemeMode(config.themeMode);
      else if (config.theme) this.setTheme(config.theme); // Backward compatibility
      if (config.density !== undefined) this.setDensity(config.density);
      if (config.typography) this.setTypography(config.typography);

      return true;
    } catch (error) {
      console.error('Invalid theme configuration:', error);
      return false;
    }
  }

  // Private methods
  private applyThemeMode(mode: ThemeMode): void {
    let themeName: string;

    if (mode === 'system') {
      // Detect system preference
      if (typeof window !== 'undefined' && window.matchMedia) {
        const isDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches;
        themeName = isDarkMode ? 'dark' : 'light';
      } else {
        themeName = 'light'; // Fallback
      }
    } else {
      themeName = mode;
    }

    const theme = this.themes.find(t => t.name === themeName);
    if (theme) {
      this.applyTheme(theme);
    }
  }

  private initializeTheme(): void {
    // Load saved preferences
    const savedThemeMode = localStorage.getItem(ThemeService.STORAGE_KEYS.themeMode) as ThemeMode;
    const savedDensity = localStorage.getItem(ThemeService.STORAGE_KEYS.density);
    const savedTypography = localStorage.getItem(ThemeService.STORAGE_KEYS.typography);

    // Set theme mode (default to 'system')
    const themeMode = savedThemeMode || 'system';
    this._themeMode.set(themeMode);

    // Apply theme based on mode
    this.applyThemeMode(themeMode);

    // Listen for system theme changes
    if (typeof window !== 'undefined' && window.matchMedia) {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      mediaQuery.addEventListener('change', () => {
        if (this._themeMode() === 'system') {
          this.applyThemeMode('system');
        }
      });
    }

    // Apply saved or default density
    const density = savedDensity
      ? this.densityLevels.find(d => d.level === parseInt(savedDensity)) ?? this.densityLevels[3]!
      : this.densityLevels[3]!;
    this.applyDensity(density);

    // Apply saved or default typography
    const typography = savedTypography
      ? this.typographyConfigs[parseInt(savedTypography)] ?? this.typographyConfigs[0]!
      : this.typographyConfigs[0]!;
    this.applyTypography(typography);
  }

  private applyTheme(theme: M3ThemeConfig): void {
    // Update body class
    this.updateBodyClass('theme', `${theme.name}-theme`);

    // Apply M3 color tokens - we need to set ALL theme properties to ensure proper switching
    const root = this.document.documentElement;

    // Always set both light and dark theme properties from their respective theme configs
    this.setThemeProperties(root, this.themes[0]!, 'light'); // Light theme (index 0)
    this.setThemeProperties(root, this.themes[1]!, 'dark');  // Dark theme (index 1)

    // Set direct color tokens for current theme that styles.scss expects
    const {colors} = theme;
    root.style.setProperty('--primary-color', colors.primary);
    root.style.setProperty('--on-primary-color', colors.onPrimary);
    root.style.setProperty('--primary-container-color', colors.primaryContainer);
    root.style.setProperty('--on-primary-container-color', colors.onPrimaryContainer);

    root.style.setProperty('--secondary-color', colors.secondary);
    root.style.setProperty('--on-secondary-color', colors.onSecondary);
    root.style.setProperty('--secondary-container-color', colors.secondaryContainer);
    root.style.setProperty('--on-secondary-container-color', colors.onSecondaryContainer);

    root.style.setProperty('--tertiary-color', colors.tertiary);
    root.style.setProperty('--on-tertiary-color', colors.onTertiary);
    root.style.setProperty('--tertiary-container-color', colors.tertiaryContainer);
    root.style.setProperty('--on-tertiary-container-color', colors.onTertiaryContainer);

    root.style.setProperty('--error-color', colors.error);
    root.style.setProperty('--on-error-color', colors.onError);
    root.style.setProperty('--error-container-color', colors.errorContainer);
    root.style.setProperty('--on-error-container-color', colors.onErrorContainer);

    root.style.setProperty('--surface-color', colors.surface);
    root.style.setProperty('--on-surface-color', colors.onSurface);
    root.style.setProperty('--surface-variant-color', colors.surfaceVariant);
    root.style.setProperty('--on-surface-variant-color', colors.onSurfaceVariant);
    root.style.setProperty('--surface-container-color', colors.surfaceContainer);
    root.style.setProperty('--surface-container-low-color', colors.surfaceContainerLow);
    root.style.setProperty('--surface-container-high-color', colors.surfaceContainerHigh);
    root.style.setProperty('--surface-container-highest-color', colors.surfaceContainerHighest);

    root.style.setProperty('--background-color', colors.background);
    root.style.setProperty('--on-background-color', colors.onBackground);

    root.style.setProperty('--outline-color', colors.outline);
    root.style.setProperty('--outline-variant-color', colors.outlineVariant);

    root.style.setProperty('--inverse-surface-color', colors.inverseSurface);
    root.style.setProperty('--inverse-on-surface-color', colors.inverseOnSurface);
    root.style.setProperty('--inverse-primary-color', colors.inversePrimary);

    // Legacy compatibility (for gradual migration)
    root.style.setProperty('--accent-color', colors.secondary);
    root.style.setProperty('--warn-color', colors.error);
    root.style.setProperty('--success-color', colors.tertiary);
    root.style.setProperty('--text-color', colors.onBackground);
    root.style.setProperty('--text-secondary-color', colors.onSurfaceVariant);

    this._currentTheme.set(theme);
  }

  private setThemeProperties(root: HTMLElement, themeConfig: M3ThemeConfig, themePrefix: string): void {
    const {colors} = themeConfig;

    root.style.setProperty(`--app-${themePrefix}-primary`, colors.primary);
    root.style.setProperty(`--app-${themePrefix}-primary-container`, colors.primaryContainer);
    root.style.setProperty(`--app-${themePrefix}-on-primary`, colors.onPrimary);
    root.style.setProperty(`--app-${themePrefix}-on-primary-container`, colors.onPrimaryContainer);

    root.style.setProperty(`--app-${themePrefix}-secondary`, colors.secondary);
    root.style.setProperty(`--app-${themePrefix}-secondary-container`, colors.secondaryContainer);
    root.style.setProperty(`--app-${themePrefix}-on-secondary`, colors.onSecondary);
    root.style.setProperty(`--app-${themePrefix}-on-secondary-container`, colors.onSecondaryContainer);

    root.style.setProperty(`--app-${themePrefix}-tertiary`, colors.tertiary);
    root.style.setProperty(`--app-${themePrefix}-tertiary-container`, colors.tertiaryContainer);
    root.style.setProperty(`--app-${themePrefix}-on-tertiary`, colors.onTertiary);
    root.style.setProperty(`--app-${themePrefix}-on-tertiary-container`, colors.onTertiaryContainer);

    root.style.setProperty(`--app-${themePrefix}-error`, colors.error);
    root.style.setProperty(`--app-${themePrefix}-error-container`, colors.errorContainer);
    root.style.setProperty(`--app-${themePrefix}-on-error`, colors.onError);
    root.style.setProperty(`--app-${themePrefix}-on-error-container`, colors.onErrorContainer);

    root.style.setProperty(`--app-${themePrefix}-surface`, colors.surface);
    root.style.setProperty(`--app-${themePrefix}-surface-container`, colors.surfaceContainer);
    root.style.setProperty(`--app-${themePrefix}-surface-container-low`, colors.surfaceContainerLow);
    root.style.setProperty(`--app-${themePrefix}-surface-container-high`, colors.surfaceContainerHigh);
    root.style.setProperty(`--app-${themePrefix}-surface-container-highest`, colors.surfaceContainerHighest);
    root.style.setProperty(`--app-${themePrefix}-on-surface`, colors.onSurface);
    root.style.setProperty(`--app-${themePrefix}-on-surface-variant`, colors.onSurfaceVariant);

    root.style.setProperty(`--app-${themePrefix}-background`, colors.background);
    root.style.setProperty(`--app-${themePrefix}-on-background`, colors.onBackground);

    root.style.setProperty(`--app-${themePrefix}-outline`, colors.outline);
    root.style.setProperty(`--app-${themePrefix}-outline-variant`, colors.outlineVariant);
  }

  private applyDensity(density: M3DensityConfig): void {
    // Update body class for density
    this.updateBodyClass('mat-density', `mat-density-${density.level}`);

    // Calculate responsive spacing based on 8px grid
    const baseSpacing = 8;
    const densityMultiplier = Math.max(0.5, 1 + (density.level * 0.2)); // Minimum 50% scaling
    const spacing = Math.max(4, baseSpacing * densityMultiplier); // Minimum 4px

    // Apply spacing custom properties
    const root = this.document.documentElement;
    root.style.setProperty('--mat-density-level', density.level.toString());
    root.style.setProperty('--spacing-base', `${spacing}px`);
    root.style.setProperty('--spacing-xs', `${spacing * 0.5}px`);
    root.style.setProperty('--spacing-sm', `${spacing}px`);
    root.style.setProperty('--spacing-md', `${spacing * 2}px`);
    root.style.setProperty('--spacing-lg', `${spacing * 3}px`);
    root.style.setProperty('--spacing-xl', `${spacing * 4}px`);

    this._currentDensity.set(density);
  }

  private applyTypography(typography: M3TypographyConfig): void {
    const root = this.document.documentElement;
    const {scale} = typography;

    // Apply font family
    root.style.setProperty('--font-family', typography.fontFamily);

    // Apply M3 typography scale
    Object.entries(scale).forEach(([level, config]) => {
      const kebabLevel = level.replace(/[A-Z]/g, letter => `-${letter.toLowerCase()}`);
      root.style.setProperty(`--font-${kebabLevel}-size`, config.size);
      root.style.setProperty(`--font-${kebabLevel}-line-height`, config.lineHeight);
      root.style.setProperty(`--font-${kebabLevel}-weight`, config.weight.toString());
    });

    // Legacy compatibility
    root.style.setProperty('--font-size-small', scale.bodySmall.size);
    root.style.setProperty('--font-size-medium', scale.bodyMedium.size);
    root.style.setProperty('--font-size-large', scale.bodyLarge.size);

    this._currentTypography.set(typography);
  }

  private updateBodyClass(prefix: string, newClass: string): void {
    // Remove both light-theme and dark-theme classes specifically
    this.document.body.classList.remove('light-theme', 'dark-theme');

    // Also remove any classes that start with the prefix (for other use cases)
    this.document.body.className = this.document.body.className
      .split(' ')
      .filter(className => !className.startsWith(prefix))
      .join(' ');

    this.document.body.classList.add(newClass);
  }
}
