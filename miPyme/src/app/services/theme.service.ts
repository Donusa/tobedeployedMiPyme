import { Injectable, Renderer2, RendererFactory2 } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private renderer: Renderer2;
  private currentTheme: Theme = 'light';
  private darkThemeClass = 'dark-theme';


  public theme$ = new BehaviorSubject<Theme>('light');

  constructor(rendererFactory: RendererFactory2) {
    this.renderer = rendererFactory.createRenderer(null, null);
    this.initTheme();
  }


  private getStorageKey(): string {
    const username = localStorage.getItem('userName') || 'default';
    return `app_theme_${username}`;
  }

  private initTheme() {
    const username = localStorage.getItem('userName');


    if (!username) {
      this.currentTheme = 'light';
      this.theme$.next(this.currentTheme);
      this.applyTheme();
      return;
    }

    const key = `app_theme_${username}`;
    let storedTheme = localStorage.getItem(key) as Theme | string | null;

    if (storedTheme) {

      if (storedTheme === 'system') {
        storedTheme = 'light';
        localStorage.setItem(key, 'light');
      }
      this.currentTheme = storedTheme as Theme;
    } else {

      const legacyTheme = localStorage.getItem('app_theme') as string | null;
      if (legacyTheme && legacyTheme !== 'system') {
        this.currentTheme = legacyTheme as Theme;
        localStorage.setItem(key, legacyTheme);
        localStorage.removeItem('app_theme');
      } else if (legacyTheme === 'system') {
        this.currentTheme = 'light';
        localStorage.setItem(key, 'light');
        localStorage.removeItem('app_theme');
      } else {

        const prefsRaw = localStorage.getItem(`app_preferences_${username}`);
        if (prefsRaw) {
          try {
            const parsed = JSON.parse(prefsRaw);
            if (parsed.theme && parsed.theme !== 'system') {
              this.currentTheme = parsed.theme as Theme;
              localStorage.setItem(key, parsed.theme);
            } else {
              this.currentTheme = 'light';
            }
          } catch (_) {
            this.currentTheme = 'light';
          }
        }
      }
    }

    this.theme$.next(this.currentTheme);
    this.applyTheme();
  }


  reloadForUser(): void {
    const key = this.getStorageKey();
    let storedTheme = localStorage.getItem(key) as string | null;


    if (storedTheme === 'system') {
      storedTheme = 'light';
      localStorage.setItem(key, 'light');
    }

    if (storedTheme) {
      this.currentTheme = storedTheme as Theme;
    } else {

      const username = localStorage.getItem('userName') || 'default';
      const prefsRaw = localStorage.getItem(`app_preferences_${username}`);
      if (prefsRaw) {
        try {
          const parsed = JSON.parse(prefsRaw);
          if (parsed.theme && parsed.theme !== 'system') {
            this.currentTheme = parsed.theme as Theme;
            localStorage.setItem(key, parsed.theme);
          } else {
            this.currentTheme = 'light';
          }
        } catch (_) {
          this.currentTheme = 'light';
        }
      } else {

        this.currentTheme = 'light';
      }
    }

    this.theme$.next(this.currentTheme);
    this.applyTheme();
  }


  toggleTheme(): void {
    this.setTheme(this.currentTheme === 'dark' ? 'light' : 'dark');
  }

  setTheme(theme: Theme) {
    this.currentTheme = theme;
    localStorage.setItem(this.getStorageKey(), theme);
    this.theme$.next(theme);
    this.applyTheme();
  }

  getTheme(): Theme {
    return this.currentTheme;
  }

  private applyTheme() {
    if (this.currentTheme === 'dark') {
      this.renderer.addClass(document.body, this.darkThemeClass);
    } else {
      this.renderer.removeClass(document.body, this.darkThemeClass);
    }
  }
}

