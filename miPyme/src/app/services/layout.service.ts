import { Injectable, signal } from '@angular/core';


@Injectable({ providedIn: 'root' })
export class LayoutService {

  private readonly MOBILE_BREAKPOINT = 1024;
  private readonly _isMobile = signal<boolean>(this.checkMobile());


  readonly isMobile = this._isMobile.asReadonly();

  constructor() {
    window.addEventListener('resize', () => {
      const mobile = this.checkMobile();
      if (this._isMobile() !== mobile) {
        this._isMobile.set(mobile);
      }
    });
  }

  private checkMobile(): boolean {
    return window.innerWidth < this.MOBILE_BREAKPOINT;
  }
}
