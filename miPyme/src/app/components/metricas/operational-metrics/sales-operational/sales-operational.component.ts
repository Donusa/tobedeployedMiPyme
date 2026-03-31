import { Component, OnInit } from '@angular/core';
import { SalesService } from '../../../../sales/services/sales.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
    selector: 'app-sales-operational-v2',
    templateUrl: './sales-operational.component.html',
    styleUrls: ['./sales-operational.component.css']
})
export class SalesOperationalComponent implements OnInit {


    salesChart: any;
    salesRange: 'TODAY' | '7D' | '30D' | '1Y' | 'CUSTOM' = '7D';
    salesCustomStart: string | null = null;
    salesCustomEnd: string | null = null;
    salesMetricType: 'AMOUNT' | 'ORDERS' | 'UNITS' = 'AMOUNT';
    salesChartType: 'LINE' | 'BAR' = 'LINE';
    salesCustomError: string | null = null;

    metrics = {
        operations: {
            totalSales: 0,
            totalOrders: 0
        }
    };

    constructor(private salesService: SalesService) { }

    ngOnInit(): void {
        this.loadSalesMetrics();
    }

    loadSalesMetrics() {
        this.salesCustomError = null;


        if (this.salesRange === 'CUSTOM') {
            if (!this.salesCustomStart || !this.salesCustomEnd) {

                return;
            }
            if (this.salesCustomStart > this.salesCustomEnd) {
                this.salesCustomError = 'La fecha de inicio debe ser anterior a la final.';
                return;
            }
        }

        this.salesService.getMetrics(this.salesRange, this.salesCustomStart || undefined, this.salesCustomEnd || undefined)
            .subscribe(data => {

                this.metrics.operations.totalSales = data.currentPeriod.reduce((sum: number, p: any) => sum + p.totalAmount, 0);
                this.metrics.operations.totalOrders = data.currentPeriod.reduce((sum: number, p: any) => sum + p.orderCount, 0);

                this.renderSalesChart(data);
            });
    }

    changeSalesRange(range: 'TODAY' | '7D' | '30D' | '1Y' | 'CUSTOM') {
        this.salesRange = range;
        if (range !== 'CUSTOM') {
            this.loadSalesMetrics();
        }
    }

    changeSalesMetricType(type: 'AMOUNT' | 'ORDERS' | 'UNITS') {
        this.salesMetricType = type;
        this.loadSalesMetrics();
    }

    changeSalesChartType(type: 'LINE' | 'BAR') {
        this.salesChartType = type;
        if (this.metrics.operations.totalSales > 0 || this.metrics.operations.totalOrders > 0) {
            this.loadSalesMetrics();
        }
    }

    renderSalesChart(data: any) {
        const ctx = document.getElementById('salesChart') as HTMLCanvasElement;
        if (!ctx) return;

        if (this.salesChart) {
            this.salesChart.destroy();
        }

        const labels = data.currentPeriod.map((p: any) => p.label);
        const currentData = data.currentPeriod.map((p: any) => {
            if (this.salesMetricType === 'AMOUNT') return p.totalAmount;
            if (this.salesMetricType === 'ORDERS') return p.orderCount;
            return p.unitCount;
        });
        const prevData = data.previousPeriod.map((p: any) => {
            if (this.salesMetricType === 'AMOUNT') return p.totalAmount;
            if (this.salesMetricType === 'ORDERS') return p.orderCount;
            return p.unitCount;
        });

        const datasets: any[] = [
            {
                label: `Actual (${this.salesRange})`,
                data: currentData,
                borderColor: '#4f46e5',
                backgroundColor: this.salesChartType === 'BAR' ? '#4f46e5' : 'rgba(79, 70, 229, 0.1)',
                tension: 0.4,
                fill: this.salesChartType === 'LINE',
                order: 1
            },
            {
                label: 'Período Anterior',
                data: prevData,
                borderColor: '#9ca3af',
                backgroundColor: this.salesChartType === 'BAR' ? '#e5e7eb' : undefined,
                borderDash: this.salesChartType === 'LINE' ? [5, 5] : undefined,
                tension: 0.4,
                fill: false,
                order: 2
            }
        ];

        this.salesChart = new Chart(ctx, {
            type: this.salesChartType === 'BAR' ? 'bar' : 'line',
            data: {
                labels: labels,
                datasets: datasets
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                    },
                    tooltip: {
                        mode: 'index',
                        intersect: false,
                    }
                },
                interaction: {
                    mode: 'nearest',
                    axis: 'x',
                    intersect: false
                },
                scales: {
                    y: {
                        beginAtZero: true
                    },
                    x: {
                        grid: {
                            display: false
                        }
                    }
                }
            }
        });
    }
}
