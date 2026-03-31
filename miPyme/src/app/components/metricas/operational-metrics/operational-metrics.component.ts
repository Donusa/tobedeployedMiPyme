import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-operational-metrics',
  templateUrl: './operational-metrics.component.html',
  styleUrls: ['./operational-metrics.component.css']
})
export class OperationalMetricsComponent implements OnInit {

  currentTab: 'SALES_OPS' | 'BUSINESS' | 'STOCK_ANALYSIS' = 'SALES_OPS';

  tabs: Array<{ id: 'SALES_OPS' | 'BUSINESS' | 'STOCK_ANALYSIS'; label: string }> = [
    { id: 'SALES_OPS', label: 'Operaciones de Ventas' },
    { id: 'BUSINESS', label: 'Negocio y Reportes' },
    { id: 'STOCK_ANALYSIS', label: 'Análisis de Stock' }
  ];

  constructor() { }

  ngOnInit(): void {
  }

  getCurrentTabLabel(): string {
    const tab = this.tabs.find(t => t.id === this.currentTab);
    return tab?.label || 'Operaciones';
  }

  goToPreviousTab(): void {
    const currentIndex = this.tabs.findIndex(t => t.id === this.currentTab);
    const previousIndex = currentIndex === 0 ? this.tabs.length - 1 : currentIndex - 1;
    this.currentTab = this.tabs[previousIndex].id;
  }

  goToNextTab(): void {
    const currentIndex = this.tabs.findIndex(t => t.id === this.currentTab);
    const nextIndex = (currentIndex + 1) % this.tabs.length;
    this.currentTab = this.tabs[nextIndex].id;
  }

  selectTab(tabId: 'SALES_OPS' | 'BUSINESS' | 'STOCK_ANALYSIS'): void {
    this.currentTab = tabId;
  }
}
