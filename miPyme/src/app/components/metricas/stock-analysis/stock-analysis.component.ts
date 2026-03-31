import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { StockService } from '../../../services/stock.service';
import { OrdersService } from '../../../orders/services/orders.service';
import { Product, ProductVariant } from '../../../models/stock.models';
import { Order } from '../../../orders/models/order.model';
import { forkJoin } from 'rxjs';
import { LayoutService } from '../../../services/layout.service';

Chart.register(...registerables);

@Component({
    selector: 'app-stock-analysis',
    templateUrl: './stock-analysis.component.html',
    styleUrls: ['./stock-analysis.component.css']
})
export class StockAnalysisComponent implements OnInit {

    isLoading = false;
    errorMessage = '';

    allProducts: Product[] = [];
    allOrders: Order[] = [];
    allVariants: ProductVariant[] = [];


    totalStockValueCost = 0;
    totalStockValuePrice = 0;

    immobilizedCount = 0;
    immobilizedValue = 0;


    chartValueByCategory: any;
    chartCategoryBars: any;


    currentChartIdx = 0;
    readonly chartCount = 2;
    readonly chartTitles = [
        'Distribución por Categoría',
        'Activo vs. Inmovilizado'
    ];

    constructor(
        private stockService: StockService,
        private ordersService: OrdersService,
        private cdr: ChangeDetectorRef,
        public layoutService: LayoutService
    ) { }

    ngOnInit(): void {
        this.loadData();
    }

    loadData() {
        this.isLoading = true;
        this.errorMessage = '';
        forkJoin({
            products: this.stockService.getProducts(),
            orders: this.ordersService.getAll(),
            variants: this.stockService.getAllVariants()
        }).subscribe({
            next: ({ products, orders, variants }) => {
                this.allProducts = products;
                this.allOrders = orders;
                this.allVariants = variants;

                this.calculateMetrics();
                this.isLoading = false;
                this.cdr.detectChanges();
                this.renderCharts();
            },
            error: (err) => {
                console.error('Error loading stock analysis', err);
                this.errorMessage = 'No se pudieron cargar los datos de stock. Verifique la conexión e intente nuevamente.';
                this.isLoading = false;
            }
        });
    }

    calculateMetrics() {
        this.totalStockValueCost = 0;
        this.totalStockValuePrice = 0;
        let categoryValue = new Map<string, number>();
        let immobilizedByCategory = new Map<string, number>();



        const lastSaleMap = new Map<number, Date>();
        this.allOrders.forEach(o => {
            const date = new Date(o.date);
            o.items.forEach(i => {
                if (i.productId) {
                    const current = lastSaleMap.get(i.productId);
                    if (!current || date > current) {
                        lastSaleMap.set(i.productId, date);
                    }
                }
            });
        });

        const ninetyDaysAgo = new Date();
        ninetyDaysAgo.setDate(ninetyDaysAgo.getDate() - 90);

        this.immobilizedCount = 0;
        this.immobilizedValue = 0;


        const productIdsWithVariants = new Set<number>();
        this.allVariants.forEach(v => {
            if (v.product?.productId) productIdsWithVariants.add(v.product.productId);
        });

        const addToCategory = (catName: string, qty: number, cost: number, price: number, productId: number | undefined) => {
            this.totalStockValueCost += qty * cost;
            this.totalStockValuePrice += qty * price;
            categoryValue.set(catName, (categoryValue.get(catName) || 0) + (qty * cost));

            if (productId !== undefined) {
                const lastSale = lastSaleMap.get(productId);
                const isImmobilized = lastSale ? lastSale < ninetyDaysAgo : true;
                if (isImmobilized) {
                    this.immobilizedCount++;
                    this.immobilizedValue += qty * cost;
                    immobilizedByCategory.set(catName, (immobilizedByCategory.get(catName) || 0) + (qty * cost));
                }
            }
        };


        this.allProducts.forEach(p => {
            if (p.productId && productIdsWithVariants.has(p.productId)) return;
            const qty = p.stockQuantity || 0;
            const cost = p.cost || 0;
            const price = p.price || 0;
            if (qty > 0) {
                const catName = p.productCategory?.categoryName || 'Sin Categoría';
                addToCategory(catName, qty, cost, price, p.productId);
            }
        });


        this.allVariants.forEach(v => {
            const qty = v.stockQuantity || 0;
            const cost = v.cost ?? v.product?.cost ?? 0;
            const price = v.price ?? v.product?.price ?? 0;
            if (qty > 0) {
                const catName = v.product?.productCategory?.categoryName || 'Sin Categoría';
                addToCategory(catName, qty, cost, price, v.product?.productId);
            }
        });

        this.categoryValueData = categoryValue;
        this.immobilizedByCategoryData = immobilizedByCategory;
    }

    categoryValueData = new Map<string, number>();
    immobilizedByCategoryData = new Map<string, number>();

    renderCharts() {
        if (this.layoutService.isMobile()) {
            this.renderCurrentChart();
        } else {
            this.renderCategoryChart();
            this.renderCategoryBarsChart();
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
        if (this.currentChartIdx === 0) this.renderCategoryChart();
        else this.renderCategoryBarsChart();
    }

    renderCategoryChart() {
        if (this.chartValueByCategory) this.chartValueByCategory.destroy();

        const sortedCats = Array.from(this.categoryValueData.entries())
            .sort((a, b) => b[1] - a[1])
            .slice(0, 8);

        const ctx = document.getElementById('chartStockCategory') as HTMLCanvasElement;
        if (!ctx) return;

        const colors = ['#3b82f6','#10b981','#f59e0b','#8b5cf6','#ec4899','#06b6d4','#6366f1','#94a3b8'];
        const colorsAlpha = colors.map(c => c + '22');

        this.chartValueByCategory = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: sortedCats.map(c => c[0]),
                datasets: [{
                    data: sortedCats.map(c => c[1]),
                    backgroundColor: colors,
                    borderColor: '#ffffff',
                    borderWidth: 3,
                    hoverBorderWidth: 4,
                    hoverOffset: 8
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '68%',
                plugins: {
                    legend: {
                        position: 'right',
                        labels: {
                            font: { size: 12, weight: 600 as any },
                            color: '#475569',
                            padding: 16,
                            usePointStyle: true,
                            pointStyleWidth: 10,
                            boxHeight: 9
                        }
                    },
                    tooltip: {
                        backgroundColor: 'rgba(15,23,42,0.9)',
                        padding: 12,
                        cornerRadius: 10,
                        titleFont: { size: 13, weight: 'bold' },
                        bodyFont: { size: 12 },
                        callbacks: {
                            label: (ctx) => {
                                const total = (ctx.dataset.data as number[]).reduce((a, b) => a + b, 0);
                                const pct = total > 0 ? ((ctx.parsed / total) * 100).toFixed(1) : '0';
                                const val = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(ctx.parsed);
                                return `  ${val}  (${pct}%)`;
                            }
                        }
                    }
                }
            }
        });
    }

    renderCategoryBarsChart() {
        if (this.chartCategoryBars) this.chartCategoryBars.destroy();


        const top6 = Array.from(this.categoryValueData.entries())
            .sort((a, b) => b[1] - a[1])
            .slice(0, 6);

        const labels = top6.map(c => c[0]);
        const totalValues = top6.map(c => c[1]);
        const immobilizedValues = top6.map(c => this.immobilizedByCategoryData.get(c[0]) || 0);
        const activeValues = totalValues.map((t, i) => Math.max(0, t - immobilizedValues[i]));

        const ctx = document.getElementById('chartCategoryBars') as HTMLCanvasElement;
        if (!ctx) return;

        this.chartCategoryBars = new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [
                    {
                        label: 'Activo (<90d)',
                        data: activeValues,
                        backgroundColor: 'rgba(37,99,235,0.75)',
                        borderColor: '#2563eb',
                        borderWidth: 0,
                        borderRadius: 6,
                        borderSkipped: false
                    },
                    {
                        label: 'Inmovilizado (>90d)',
                        data: immobilizedValues,
                        backgroundColor: 'rgba(239,68,68,0.7)',
                        borderColor: '#ef4444',
                        borderWidth: 0,
                        borderRadius: 6,
                        borderSkipped: false
                    }
                ]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                        align: 'end',
                        labels: {
                            font: { size: 12, weight: 600 as any },
                            color: '#475569',
                            usePointStyle: true,
                            pointStyleWidth: 10,
                            boxHeight: 8,
                            padding: 16
                        }
                    },
                    tooltip: {
                        backgroundColor: 'rgba(15,23,42,0.9)',
                        padding: 12,
                        cornerRadius: 10,
                        titleFont: { size: 13, weight: 'bold' },
                        bodyFont: { size: 12 },
                        callbacks: {
                            label: (ctx) => {
                                const raw = ctx.parsed?.x ?? 0;
                                const val = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(raw);
                                return `  ${ctx.dataset.label}: ${val}`;
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        stacked: false,
                        grid: { color: 'rgba(100,116,139,0.08)' },
                        border: { display: false },
                        ticks: {
                            color: '#94a3b8',
                            font: { size: 11 },
                            callback: (v) => {
                                const n = Number(v);
                                if (n >= 1000000) return '$' + (n/1000000).toFixed(1) + 'M';
                                if (n >= 1000) return '$' + (n/1000).toFixed(0) + 'K';
                                return '$' + n;
                            }
                        }
                    },
                    y: {
                        stacked: false,
                        grid: { display: false },
                        border: { display: false },
                        ticks: { color: '#475569', font: { size: 12, weight: 600 as any } }
                    }
                }
            }
        });
    }

}
