import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';
import { ThemeService, Theme } from '../../../services/theme.service';

@Component({
  selector: 'app-preferences',
  templateUrl: './preferences.component.html',
  styleUrls: ['./preferences.component.css']
})
export class PreferencesComponent implements OnInit {
  isLoading = false;
  successMessage = '';


  preferences = {
    theme: 'light',
    language: 'es',
    notifications: {
      email: true,
      push: false
    },
    stockMetrics: {
      targetMargin: 30
    }
  };

  constructor(
    private authService: AuthService,
    private themeService: ThemeService
  ) { }

  ngOnInit(): void {
    this.loadPreferences();
  }

  loadPreferences() {
    this.isLoading = true;
    const username = this.authService.getUserName() || 'default';


    const storedPrefs = localStorage.getItem(`app_preferences_${username}`);
    if (storedPrefs) {
      try {
        const parsed = JSON.parse(storedPrefs);

        this.preferences = {
            ...this.preferences,
            ...parsed,
            notifications: { ...this.preferences.notifications, ...parsed.notifications },
            stockMetrics: { ...this.preferences.stockMetrics, ...parsed.stockMetrics }
        };
      } catch (e) {
        console.error('Error parsing app preferences', e);
      }
    }


    this.preferences.theme = this.themeService.getTheme();


    const stockPrefs = localStorage.getItem(`stock_metrics_prefs_${username}`);
    if (stockPrefs) {
        try {
            const parsedStock = JSON.parse(stockPrefs);
            if (parsedStock.targetMargin) {
                this.preferences.stockMetrics.targetMargin = parsedStock.targetMargin;
            }
        } catch(e) {}
    }

    this.isLoading = false;
  }

  savePreferences() {
    this.isLoading = true;
    this.successMessage = '';
    const username = this.authService.getUserName() || 'default';


    this.themeService.setTheme(this.preferences.theme as Theme);


    this.authService.updatePreferences({ theme: this.preferences.theme }).subscribe();


    localStorage.setItem(`app_preferences_${username}`, JSON.stringify(this.preferences));


    const stockPrefs = {
        targetMargin: this.preferences.stockMetrics.targetMargin
    };
    localStorage.setItem(`stock_metrics_prefs_${username}`, JSON.stringify(stockPrefs));

    setTimeout(() => {
        this.isLoading = false;
        this.successMessage = 'Preferencias guardadas correctamente';


        setTimeout(() => this.successMessage = '', 3000);
    }, 500);
  }
}
