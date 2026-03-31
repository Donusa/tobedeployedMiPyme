import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize, timeout } from 'rxjs/operators';
import { OrdersService } from '../../../orders/services/orders.service';
import { StockService } from '../../../services/stock.service';
import { Product } from '../../../models/stock.models';
import { Order } from '../../../orders/models/order.model';
import { LayoutService } from '../../../services/layout.service';

Chart.register(...registerables);

@Component({
  selector: 'app-business-metrics',
  templateUrl: './business-metrics.component.html',
  styleUrls: ['./business-metrics.component.css']
})
export class BusinessMetricsComponent implements OnInit {

  allOrders: Order[] = [];
  allProducts: Product[] = [];


  filteredOrders: Order[] = [];


  totalSales = 0;
  totalOrders = 0;
  averageTicket = 0;
  grossMargin = 0;
  grossMarginPercent = 0;


  chartSalesByEmployee: any;
  chartTicketDistribution: any;
  chartTopProducts: any;
  chartCategoryDist: any;
  chartEvolution: any;

  startDate: string = '';
  endDate: string = '';

  isLoading = true;
  errorMessage: string | null = null;


  currentChartIdx = 0;
  readonly chartCount = 3;
  readonly chartTitles = [
    'Ventas por Empleado / Canal',
    'Evolución Diaria',
    'Top 10 Productos'
  ];

  constructor(
    private ordersService: OrdersService,
    private stockService: StockService,
    private cdr: ChangeDetectorRef,
    public layoutService: LayoutService
  ) { }

  ngOnInit(): void {

    const end = new Date();
    const start = new Date();
    start.setDate(start.getDate() - 30);

    this.startDate = start.toISOString().split('T')[0];
    this.endDate = end.toISOString().split('T')[0];

    this.loadData();
  }

  loadData() {
    this.isLoading = true;
    this.errorMessage = null;
    this.cdr.detectChanges();

    const orders$ = this.ordersService.getAll().pipe(
      timeout(15000),
      catchError(err => {
        console.warn('Orders load failed, using empty array:', err);
        return of([] as Order[]);
      })
    );

    const products$ = this.stockService.getProducts().pipe(
      timeout(15000),
      catchError(err => {
        console.warn('Products load failed, using empty array:', err);
        return of([] as Product[]);
      })
    );

    forkJoin({ orders: orders$, products: products$ }).pipe(
      finalize(() => {


      })
    ).subscribe({
      next: ({ orders, products }) => {
        this.allOrders = orders;
        this.allProducts = products;
        this.applyFilters();
      },
      error: (err) => {
        console.error('Unexpected error in business metrics forkJoin:', err);
        this.errorMessage = 'No se pudieron cargar los datos. Verificá la conexión con el servidor.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  applyFilters() {
    const start = new Date(this.startDate);
    const end = new Date(this.endDate);

    end.setHours(23, 59, 59, 999);

    this.filteredOrders = this.allOrders.filter(o => {
      const d = new Date(o.date);
      return d >= start && d <= end && o.status !== 'CANCELLED';
    });

    this.calculateKPIs();


    this.isLoading = false;
    this.cdr.detectChanges();
    this.renderCharts();
  }

  calculateKPIs() {
    this.totalSales = 0;
    this.totalOrders = this.filteredOrders.length;
    let totalCost = 0;


    const productCostMap = new Map<number, number>();
    this.allProducts.forEach(p => {
      if (p.productId) productCostMap.set(p.productId, p.cost || 0);
    });

    this.filteredOrders.forEach(o => {
      this.totalSales += o.total;


      o.items.forEach(i => {
        if (i.productId) {



          const cost = productCostMap.get(i.productId) || 0;
          totalCost += cost * i.quantity;
        }
      });
    });

    this.averageTicket = this.totalOrders > 0 ? this.totalSales / this.totalOrders : 0;
    this.grossMargin = this.totalSales - totalCost;
    this.grossMarginPercent = this.totalSales > 0 ? (this.grossMargin / this.totalSales) : 0;
  }

  renderCharts() {
    if (this.layoutService.isMobile()) {

      this.renderCurrentChart();
    } else {
      this.renderSalesByEmployee();
      this.renderTopProducts();
      this.renderEvolution();
    }
  }


  prevChart() {
    this.currentChartIdx = (this.currentChartIdx - 1 + this.chartCount) % this.chartCount;
    this.cdr.detectChanges();
    setTimeout(() => this.renderCurrentChart(), 50);
  }

  nextChart() {
    this.currentChartIdx = (this.currentChartIdx + 1) % this.chartCount;
    this.cdr.detectChanges();
    setTimeout(() => this.renderCurrentChart(), 50);
  }

  goToChart(idx: number) {
    this.currentChartIdx = idx;
    this.cdr.detectChanges();
    setTimeout(() => this.renderCurrentChart(), 50);
  }

  renderCurrentChart() {
    if (this.currentChartIdx === 0) this.renderSalesByEmployee();
    else if (this.currentChartIdx === 1) this.renderEvolution();
    else this.renderTopProducts();
  }

  destroyChart(chart: any) {
    if (chart) chart.destroy();
  }

  renderSalesByEmployee() {
    this.destroyChart(this.chartSalesByEmployee);

    const salesByEmp = new Map<string, number>();
    const ordersByEmp = new Map<string, number>();

    this.filteredOrders.forEach(o => {
      const emp = o.sellerName || 'Desconocido';
      salesByEmp.set(emp, (salesByEmp.get(emp) || 0) + o.total);
      ordersByEmp.set(emp, (ordersByEmp.get(emp) || 0) + 1);
    });


    const sorted = Array.from(salesByEmp.entries()).sort((a, b) => b[1] - a[1]);
    const labels = sorted.map(e => e[0] || 'N/A');
    const dataSales = sorted.map(e => e[1]);
    const dataOrders = sorted.map(e => ordersByEmp.get(e[0]!) || 0);

    const ctx = document.getElementById('chartSalesByEmployee') as HTMLCanvasElement;
    if (!ctx) return;

    this.chartSalesByEmployee = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Ventas ($)',
            data: dataSales,
            backgroundColor: '#3b82f6',
            yAxisID: 'y',
            order: 1
          },
          {
            label: 'Órdenes (#)',
            data: dataOrders,
            borderColor: '#f59e0b',
            backgroundColor: 'transparent',
            type: 'line',
            yAxisID: 'y1',
            order: 0
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom' }
        },
        scales: {
          y: {
            type: 'linear',
            display: true,
            position: 'left',
            title: { display: true, text: 'Ventas ($)' }
          },
          y1: {
            type: 'linear',
            display: true,
            position: 'right',
            grid: {
              drawOnChartArea: false,
            },
            title: { display: true, text: 'Órdenes' }
          },
        }
      }
    });
  }

  renderTopProducts() {
    this.destroyChart(this.chartTopProducts);

    const productSales = new Map<string, number>();

    this.filteredOrders.forEach(o => {
      o.items.forEach(i => {
        const name = i.productName || 'Desconocido';
        productSales.set(name, (productSales.get(name) || 0) + i.subtotal);
      });
    });

    const sorted = Array.from(productSales.entries()).sort((a, b) => b[1] - a[1]).slice(0, 10);

    const ctx = document.getElementById('chartTopProducts') as HTMLCanvasElement;
    if (!ctx) return;

    this.chartTopProducts = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: sorted.map(i => i[0].substring(0, 20) + (i[0].length > 20 ? '...' : '')),
        datasets: [{
          label: 'Ventas ($)',
          data: sorted.map(i => i[1]),
          backgroundColor: '#10b981'
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false }
        }
      }
    } as any);
  }

  renderEvolution() {
    this.destroyChart(this.chartEvolution);


    const salesByDate = new Map<string, number>();

    this.filteredOrders.forEach(o => {
      const date = o.date.split('T')[0];
      salesByDate.set(date, (salesByDate.get(date) || 0) + o.total);
    });


    const dates = Array.from(salesByDate.keys()).sort();
    const values = dates.map(d => salesByDate.get(d) || 0);

    const ctx = document.getElementById('chartEvolution') as HTMLCanvasElement;
    if (!ctx) return;

    this.chartEvolution = new Chart(ctx, {
      type: 'line',
      data: {
        labels: dates,
        datasets: [{
          label: 'Ventas Diarias',
          data: values,
          borderColor: '#8b5cf6',
          backgroundColor: 'rgba(139, 92, 246, 0.1)',
          fill: true,
          tension: 0.4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { display: false }
        }
      }
    });
  }
}
