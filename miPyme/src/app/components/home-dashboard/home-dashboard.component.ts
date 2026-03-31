import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { OrdersService } from '../../orders/services/orders.service';
import { StockService } from '../../services/stock.service';
import { MercadoLibreService } from '../../services/mercadolibre.service';
import { CajaService } from '../../caja/services/caja.service';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Order } from '../../orders/models/order.model';
import { Product } from '../../models/stock.models';
import { PlanService } from '../../services/plan.service';

interface NorthStarMetric {
  title: string;
  value: string | number;
  subValue?: string;
  trend?: number;
  icon: string;
  type: 'success' | 'warning' | 'danger' | 'info';
  route?: string;
  requiredPlan?: 'pro' | 'enterprise';
}

interface ActionAlert {
  title: string;
  description: string;
  impactValue: number;
  impactFormatted: string;
  type: 'stock' | 'payment' | 'integration' | 'order';
  severity: 'high' | 'medium' | 'low';
  route: string;
}

@Component({
  selector: 'app-home-dashboard',
  templateUrl: './home-dashboard.component.html',
  styleUrls: ['./home-dashboard.component.css']
})
export class HomeDashboardComponent implements OnInit, OnDestroy {
  isLoading = true;
  northStarMetrics: NorthStarMetric[] = [];
  todayActions: ActionAlert[] = [];


  trendData: any = null;

  private dataSub?: Subscription;

  constructor(
    private ordersService: OrdersService,
    private stockService: StockService,
    private mlService: MercadoLibreService,
    private cajaService: CajaService,
    private router: Router,
    public planService: PlanService
  ) { }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  ngOnDestroy(): void {
    this.dataSub?.unsubscribe();
  }

  loadDashboardData() {
    this.isLoading = true;

    this.dataSub = forkJoin({
      orders: this.ordersService.getAll().pipe(catchError(() => of([]))),
      products: this.stockService.getProducts().pipe(catchError(() => of([]))),
      conversations: this.mlService.getConversations().pipe(catchError(() => of([]))),
      caja: this.cajaService.obtenerCajaActual(1).pipe(catchError(() => of(null))),
    }).subscribe(({ orders, products, conversations, caja }) => {
      this.processDashboardData(orders, products, conversations, caja);
      this.isLoading = false;
    });
  }

  private processDashboardData(orders: Order[], products: Product[], conversations: any[], caja: any) {
    const now = new Date();
    const today = now.toISOString().split('T')[0];
    const yesterday = new Date(now.getTime() - 86400000).toISOString().split('T')[0];


    const salesToday = orders.filter(o => o.date && o.date.startsWith(today) && o.status !== 'CANCELLED');
    const salesYesterday = orders.filter(o => o.date && o.date.startsWith(yesterday) && o.status !== 'CANCELLED');

    const revenueToday = salesToday.reduce((acc, o) => acc + o.total, 0);
    const revenueYesterday = salesYesterday.reduce((acc, o) => acc + o.total, 0);
    const revenueTrend = revenueYesterday > 0 ? ((revenueToday - revenueYesterday) / revenueYesterday) * 100 : 0;


    const productCostMap = new Map<number, number>();
    products.forEach(p => { if (p.productId) productCostMap.set(p.productId, p.cost || 0); });

    const calculateMargin = (orderList: Order[]) => {
      let totalRev = 0;
      let totalCost = 0;
      orderList.forEach(o => {
        totalRev += o.total;
        o.items.forEach(i => {
          if (i.productId) totalCost += (productCostMap.get(i.productId) || 0) * i.quantity;
        });
      });
      return totalRev - totalCost;
    };

    const marginToday = calculateMargin(salesToday);
    const marginPercent = revenueToday > 0 ? (marginToday / revenueToday) * 100 : 0;


    const firstOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0];
    const salesMonth = orders.filter(o => o.date && o.date >= firstOfMonth && o.status !== 'CANCELLED');
    const revenueMonth = salesMonth.reduce((acc, o) => acc + o.total, 0);


    const productSoldQty = new Map<number, { name: string; qty: number }>();
    salesMonth.forEach(o => {
      o.items.forEach(i => {
        if (i.productId) {
          const cur = productSoldQty.get(i.productId);
          if (cur) { cur.qty += i.quantity; }
          else { productSoldQty.set(i.productId, { name: i.productName || String(i.productId), qty: i.quantity }); }
        }
      });
    });
    const topProduct = [...productSoldQty.values()].sort((a, b) => b.qty - a.qty)[0];
    const topProductLabel = topProduct ? `${topProduct.name} (${topProduct.qty})` : '—';


    const pendingOrders = orders.filter(o => ['RECEIVED', 'PACKED'].includes(o.status));
    const unreadMsgs = conversations.reduce((acc, c) => acc + (c.unreadCount || 0), 0);


    this.northStarMetrics = [
      {
        title: 'Ingresos Hoy',
        value: this.formatCurrency(revenueToday),
        trend: revenueTrend,
        icon: 'bx-dollar-circle',
        type: 'success',
        route: '/ventas'
      },
      {
        title: 'Margen Bruto',
        value: this.formatCurrency(marginToday),
        subValue: `${marginPercent.toFixed(1)}%`,
        icon: 'bx-trending-up',
        type: 'info',
        route: '/metricas/alertas',
        requiredPlan: 'pro'
      },
      {
        title: 'Pedidos Pendientes',
        value: pendingOrders.length,
        subValue: 'Por procesar',
        icon: 'bx-package',
        type: 'warning',
        route: '/pedidos',
        requiredPlan: 'pro'
      },
      {
        title: 'Mensajes ML',
        value: unreadMsgs,
        subValue: 'Sin leer',
        icon: 'bx-message-rounded-dots',
        type: unreadMsgs > 0 ? 'danger' : 'info',
        route: '/mensajeria',
        requiredPlan: 'pro'
      },
      {
        title: 'Ventas del Mes',
        value: this.formatCurrency(revenueMonth),
        subValue: `${salesMonth.length} ventas`,
        icon: 'bx-calendar-event',
        type: 'success',
        route: '/ventas'
      },
      {
        title: 'Stock Crítico',
        value: products.filter(p => (p.stockQuantity || 0) <= (p.minStock || 5)).length,
        icon: 'bx-error',
        type: 'danger',
        route: '/stock'
      },
      {
        title: 'Top Producto (mes)',
        value: topProductLabel,
        icon: 'bx-bar-chart-alt-2',
        type: 'info',
        route: '/ventas'
      },
      {
        title: 'Estado de Caja',
        value: caja ? (caja.estado === 'ABIERTA' ? 'Abierta' : 'Cerrada') : 'Sin datos',
        subValue: caja && caja.totalVendido != null ? this.formatCurrency(caja.totalVendido) : undefined,
        icon: 'bx-wallet-alt',
        type: caja?.estado === 'ABIERTA' ? 'success' : 'warning',
        route: '/caja'
      },
      {
        title: 'Cuentas x Cobrar',
        value: this.formatCurrency(orders.filter(o => o.status === 'SHIPPED' && o.source !== 'local').reduce((acc, o) => acc + o.total, 0)),
        subValue: 'Estimado platforms',
        icon: 'bx-wallet',
        type: 'info',
        route: '/pedidos',
        requiredPlan: 'pro'
      }
    ];


    this.todayActions = [];


    const lowStock = products.filter(p => (p.stockQuantity || 0) <= (p.minStock || 5))
      .sort((a, b) => ((b.price || 0) * (b.minStock || 5)) - ((a.price || 0) * (a.minStock || 5)))
      .slice(0, 3);

    lowStock.forEach(p => {
      this.todayActions.push({
        title: `Quiebre de Stock: ${p.productName}`,
        description: `Quedan ${p.stockQuantity} unidades. Reponer para evitar pérdida de ventas.`,
        impactValue: (p.price || 0) * 10,
        impactFormatted: this.formatCurrency((p.price || 0) * 10),
        type: 'stock',
        severity: 'high',
        route: `/stock`
      });
    });


    const threeDaysAgo = new Date(now.getTime() - (3 * 24 * 60 * 60 * 1000));
    const delayedOrders = orders.filter(o => ['RECEIVED', 'PACKED'].includes(o.status) && new Date(o.date) < threeDaysAgo);

    if (delayedOrders.length > 0) {
      const totalDelayed = delayedOrders.reduce((acc, o) => acc + o.total, 0);
      this.todayActions.push({
        title: `${delayedOrders.length} Pedidos Atrasados`,
        description: `Pedidos con más de 72hs sin despachar.`,
        impactValue: totalDelayed,
        impactFormatted: this.formatCurrency(totalDelayed),
        type: 'order',
        severity: 'high',
        route: '/pedidos'
      });
    }

    this.todayActions.sort((a, b) => b.impactValue - a.impactValue);


    this.prepareTrendData(orders, products);
  }

  private prepareTrendData(orders: Order[], products: Product[]) {

    const labels: string[] = [];
    const revenue: number[] = [];
    const margin: number[] = [];

    const productCostMap = new Map<number, number>();
    products.forEach(p => { if (p.productId) productCostMap.set(p.productId, p.cost || 0); });

    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const dateStr = d.toISOString().split('T')[0];
      labels.push(d.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' }));

      const dayOrders = orders.filter(o => o.date && o.date.startsWith(dateStr) && o.status !== 'CANCELLED');
      const dayRev = dayOrders.reduce((acc, o) => acc + o.total, 0);

      let dayCost = 0;
      dayOrders.forEach(o => {
        o.items.forEach(item => {
          if (item.productId) dayCost += (productCostMap.get(item.productId) || 0) * item.quantity;
        });
      });

      revenue.push(dayRev);
      margin.push(dayRev - dayCost);
    }

    this.trendData = { labels, revenue, margin };
  }

  private formatCurrency(val: number): string {
    return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 }).format(val);
  }

  navigateTo(route: string) {
    this.router.navigate([route]);
  }
}
