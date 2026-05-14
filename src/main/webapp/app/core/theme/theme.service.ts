import { DOCUMENT } from '@angular/common';
import { Inject, Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type ThemeEffective = 'light' | 'dark';

/** Stored preference: explicit light/dark, or follow OS. */
export type ThemePreference = ThemeEffective | 'system';

const STORAGE_KEY = 'jho-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService implements OnDestroy {
  private readonly preference$ = new BehaviorSubject<ThemePreference>('system');
  private readonly darkModeSubject = new BehaviorSubject<boolean>(false);
  /** Emits when resolved dark/light changes (for templates). */
  readonly darkMode$: Observable<boolean> = this.darkModeSubject.asObservable();

  private mediaQuery?: MediaQueryList;
  private mediaListener?: (e: MediaQueryListEvent) => void;

  constructor(@Inject(DOCUMENT) private document: Document) {}

  /** Call once at app bootstrap (APP_INITIALIZER). */
  init(): void {
    const win = this.document.defaultView;
    if (!win) {
      return;
    }
    const stored = win.localStorage.getItem(STORAGE_KEY) as ThemePreference | null;
    const pref: ThemePreference = stored === 'light' || stored === 'dark' || stored === 'system' ? stored : 'system';
    this.preference$.next(pref);
    this.applyEffective(this.resolveEffective(pref));

    if (pref === 'system' && win.matchMedia) {
      this.mediaQuery = win.matchMedia('(prefers-color-scheme: dark)');
      this.mediaListener = () => this.applyEffective(this.resolveEffective('system'));
      this.mediaQuery.addEventListener('change', this.mediaListener);
    }
  }

  ngOnDestroy(): void {
    if (this.mediaQuery && this.mediaListener) {
      this.mediaQuery.removeEventListener('change', this.mediaListener);
    }
  }

  getPreference(): ThemePreference {
    return this.preference$.value;
  }

  preferenceChanges(): Observable<ThemePreference> {
    return this.preference$.asObservable();
  }

  /** True when the resolved theme is dark. */
  isDark(): boolean {
    return this.resolveEffective(this.preference$.value) === 'dark';
  }

  /** Toggle between light and dark; always persists an explicit choice. */
  toggleLightDark(): void {
    const next: ThemeEffective = this.isDark() ? 'light' : 'dark';
    this.setPreference(next);
  }

  setPreference(pref: ThemePreference): void {
    const win = this.document.defaultView;
    if (win) {
      win.localStorage.setItem(STORAGE_KEY, pref);
    }
    this.detachMediaListener();
    this.preference$.next(pref);
    if (pref === 'system' && win?.matchMedia) {
      this.mediaQuery = win.matchMedia('(prefers-color-scheme: dark)');
      this.mediaListener = () => this.applyEffective(this.resolveEffective('system'));
      this.mediaQuery.addEventListener('change', this.mediaListener);
    }
    this.applyEffective(this.resolveEffective(pref));
  }

  private detachMediaListener(): void {
    if (this.mediaQuery && this.mediaListener) {
      this.mediaQuery.removeEventListener('change', this.mediaListener);
    }
    this.mediaQuery = undefined;
    this.mediaListener = undefined;
  }

  private resolveEffective(pref: ThemePreference): ThemeEffective {
    if (pref === 'light' || pref === 'dark') {
      return pref;
    }
    const win = this.document.defaultView;
    if (win?.matchMedia?.('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }
    return 'light';
  }

  private applyEffective(theme: ThemeEffective): void {
    this.document.documentElement.setAttribute('data-theme', theme);
    this.darkModeSubject.next(theme === 'dark');
  }
}
