import {Component, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatSelectModule} from '@angular/material/select';
import {MatSliderModule} from '@angular/material/slider';
import {MatCardModule} from '@angular/material/card';
import {MatDividerModule} from '@angular/material/divider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatTabsModule} from '@angular/material/tabs';
import {Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {M3DensityConfig, M3ThemeConfig, M3TypographyConfig, ThemeService} from '../../../core/services/theme.service';

@Component({
  selector: 'app-theme-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatSliderModule,
    MatCardModule,
    MatDividerModule,
    MatTooltipModule,
    MatTabsModule
  ],
  templateUrl: './theme-settings.component.html',
  styleUrl: './theme-settings.component.scss'
})
export class ThemeSettingsComponent implements OnInit, OnDestroy {
  // Current configurations
  currentTheme!: M3ThemeConfig;
  currentDensity!: M3DensityConfig;
  currentTypography!: M3TypographyConfig;
  // Available options
  availableThemes: readonly M3ThemeConfig[] = [];
  availableDensities: readonly M3DensityConfig[] = [];
  availableTypographies: readonly M3TypographyConfig[] = [];
  // Selected values
  selectedTheme: string = '';
  selectedDensityLevel: number = 0;
  selectedTypographyIndex: number = 0;
  private destroy$ = new Subject<void>();

  constructor(
    private themeService: ThemeService,
    private dialogRef: MatDialogRef<ThemeSettingsComponent>
  ) {
  }

  ngOnInit(): void {
    // Load available options
    this.availableThemes = this.themeService.getAvailableThemes();
    this.availableDensities = this.themeService.getAvailableDensities();
    this.availableTypographies = this.themeService.getAvailableTypographies();

    // Subscribe to current configurations
    this.themeService.currentTheme$
      .pipe(takeUntil(this.destroy$))
      .subscribe(theme => {
        this.currentTheme = theme;
        this.selectedTheme = theme.name;
      });

    this.themeService.currentDensity$
      .pipe(takeUntil(this.destroy$))
      .subscribe(density => {
        this.currentDensity = density;
        this.selectedDensityLevel = density.level;
      });

    this.themeService.currentTypography$
      .pipe(takeUntil(this.destroy$))
      .subscribe(typography => {
        this.currentTypography = typography;
        this.selectedTypographyIndex = this.availableTypographies.indexOf(typography);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onThemeChange(): void {
    this.themeService.setTheme(this.selectedTheme);
  }

  onDensityChange(): void {
    this.themeService.setDensity(this.selectedDensityLevel);
  }

  onTypographyChange(): void {
    const typography = this.availableTypographies[this.selectedTypographyIndex];
    if (typography) {
      this.themeService.setTypography(typography.name);
    }
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  resetToDefaults(): void {
    this.themeService.resetToDefaults();
  }

  exportConfiguration(): void {
    const config = this.themeService.exportConfiguration();

    // Create and download configuration file
    const blob = new Blob([config], {type: 'application/json'});
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'ampairs-theme-config.json';
    link.click();
    window.URL.revokeObjectURL(url);
  }

  importConfiguration(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const reader = new FileReader();

      reader.onload = (e) => {
        try {
          const config = e.target?.result as string;
          this.themeService.importConfiguration(config);
        } catch (error) {
          console.error('Failed to import configuration:', error);
        }
      };

      reader.readAsText(file);
    }
  }

  onClose(): void {
    this.dialogRef.close();
  }

  onApply(): void {
    this.dialogRef.close({
      applied: true,
      theme: this.selectedTheme,
      density: this.selectedDensityLevel,
      typography: this.selectedTypographyIndex
    });
  }

  getDensityDescription(level: number): string {
    const density = this.availableDensities.find(d => d.level === level);
    return density ? density.name : 'Custom';
  }

  getTypographyName(index: number): string {
    const typography = this.availableTypographies[index];
    if (!typography) return 'Unknown';

    return typography.fontFamily.split(',')[0]?.replace(/['"]/g, '') || 'Unknown';
  }

}
