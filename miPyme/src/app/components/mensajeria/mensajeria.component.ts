import { Component, ViewChild, ElementRef, AfterViewInit } from '@angular/core';

@Component({
  selector: 'app-mensajeria',
  templateUrl: './mensajeria.component.html',
  styleUrls: ['./mensajeria.component.css']
})
export class MensajeriaComponent implements AfterViewInit {
  @ViewChild('tabsContainer') tabsContainer?: ElementRef<HTMLDivElement>;

  canScrollLeft = false;
  canScrollRight = false;

  ngAfterViewInit(): void {

    setTimeout(() => this.updateArrowVisibility(), 100);
  }

  scrollTabs(direction: 'left' | 'right'): void {
    const el = this.tabsContainer?.nativeElement;
    if (!el) return;
    const scrollAmount = 140;
    el.scrollBy({ left: direction === 'left' ? -scrollAmount : scrollAmount, behavior: 'smooth' });
  }

  onTabsScroll(): void {
    this.updateArrowVisibility();
  }

  private updateArrowVisibility(): void {
    const el = this.tabsContainer?.nativeElement;
    if (!el) return;
    this.canScrollLeft = el.scrollLeft > 2;
    this.canScrollRight = el.scrollLeft + el.clientWidth < el.scrollWidth - 2;
  }
}
