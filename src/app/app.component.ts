import {Component, inject, OnInit} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {ThemeService} from './core/services/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
        <router-outlet></router-outlet>`,
  styles: []
})
export class AppComponent implements OnInit {
  title = 'ampairs-web';

  // Inject ThemeService to ensure it initializes when the app starts
  private themeService = inject(ThemeService);

  ngOnInit(): void {
    // The ThemeService constructor and initializeTheme() will be called automatically
    // when the service is injected, ensuring themes are applied before any component renders
  }
}
