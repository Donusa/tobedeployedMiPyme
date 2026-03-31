import { Component, OnInit, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { StockService } from '../../../services/stock.service';
import { AuthService } from '../../../services/auth.service';
import { forkJoin } from 'rxjs';
import { Product, ProductVariant, ProductBrand, ProductCategory } from '../../../models/stock.models';
import { Router } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { LayoutService } from '../../../services/layout.service';

Chart.register(...registerables);

export type MetricSection = 'PRODUCTS' | 'INVENTARIO' | 'MINIMUM' | 'GROWTH' | 'CONFLICTS';
export type IssueType =
  | 'INACTIVE' | 'NO_CATEGORY' | 'NO_BRAND' | 'PRICE_ZERO'
  | 'NO_GTIN' | 'NO_WEIGHT'
  | 'BELOW_MIN' | 'NO_MIN_CONFIG'
  | 'NO_COST' | 'NEGATIVE_MARGIN' | 'HIGH_DEVIATION' | 'LOW_MARGIN'
  | 'DUPLICATE_CODE' | 'DUPLICATE_SKU' | 'DUPLICATE_GTIN';

export interface GrowthItem {
  id: string;
  code: string;
  name: string;
  stock: number;
  costoEfectivo: number;
  precioEfectivo: number;
  valorInventario: number;
  sourceCost: 'VARIANTE' | 'PRODUCTO';
  sourcePrice: 'VARIANTE' | 'PRODUCTO';


  precioSugerido: number;
  margenPct: number;
  cumple: boolean;
  gap: number;
  impactoGap: number;


  product: Product;
  variant?: ProductVariant;
}

interface DashboardFilters {

  depositoId?: number;
  ubicacionId?: number;

  issueType?: IssueType;
}

@Component({
  selector: 'app-stock-metrics',
  templateUrl: './stock-metrics.component.html',
  styleUrls: ['./stock-metrics.component.css']
})
export class StockMetricsComponent implements OnInit {


  selectedSection: MetricSection = 'PRODUCTS';
  selectedIssue: IssueType | null = null;
  showFullTableModal = false;


  mobilePage: 'sections' | 'detail' = 'sections';


  allProducts: Product[] = [];
  allVariants: (ProductVariant & { product: Product; variantName: string })[] = [];


  filteredProducts: Product[] = [];
  filteredInventory: (ProductVariant & { product: Product; variantName: string })[] = [];
  filteredMinimum: any[] = [];
  filteredGrowth: GrowthItem[] = [];


  growthItems: GrowthItem[] = [];
  targetMargin = 0.20;


  duplicateProducts: Product[] = [];
  duplicateSkus: ProductVariant[] = [];
  duplicateGtins: ProductVariant[] = [];
  filteredConflicts: any[] = [];


  metrics = {
    products: {
      active: 0, inactive: 0,
      issues: { inactive: 0, noCategory: 0, noBrand: 0, priceZero: 0 }
    },
    inventory: {
      variants: 0, noGtin: 0,
      issues: { noGtin: 0, noWeight: 0 }
    },
    stockMin: {
      below: 0, ok: 0,
      issues: { belowMin: 0, noMinConfig: 0 }
    },
    growth: {
      totalInventoryValue: 0,
      itemsNoCost: 0,
      itemsNegativeMargin: 0,
      inventoryValueNoCost: 0,
      issues: { noCost: 0, negativeMargin: 0, highDeviation: 0, lowMargin: 0 }
    },
    conflicts: {
      total: 0,
      issues: { duplicateCode: 0, duplicateSku: 0, duplicateGtin: 0 }
    }
  };


  issueMetrics: { label: string, count: number, percent: number, type: IssueType, description: string }[] = [];


  isDragging = false;
  startX = 0;
  scrollLeft = 0;

  isLoading = false;

  constructor(
    private stockService: StockService,
    private authService: AuthService,
    private router: Router,
    public layoutService: LayoutService
  ) { }


  startDrag(e: MouseEvent | TouchEvent, container: HTMLElement) {
    this.isDragging = true;
    container.classList.add('active');
    const pageX = (e instanceof MouseEvent) ? e.pageX : e.touches[0].pageX;
    this.startX = pageX - container.offsetLeft;
    this.scrollLeft = container.scrollLeft;
  }

  stopDrag() {
    this.isDragging = false;
  }

  moveDrag(e: MouseEvent | TouchEvent, container: HTMLElement) {
    if (!this.isDragging) return;

    let pageX: number;
    if (e instanceof MouseEvent) {
      e.preventDefault();
      pageX = e.pageX;
    } else {
      pageX = e.touches[0].pageX;
    }

    const x = pageX - container.offsetLeft;
    const walk = (x - this.startX) * 2;
    container.scrollLeft = this.scrollLeft - walk;
  }

  ngOnInit(): void {
    this.loadUserPreferences();

    this.loadData();
  }

  loadData(preserveState: boolean = false) {
    this.isLoading = true;

    const currentSection = this.selectedSection;
    const currentIssue = this.selectedIssue;

    forkJoin({
      products: this.stockService.getProducts(),
      brands: this.stockService.getBrands(),
      categories: this.stockService.getCategories(),
      conflicts: this.stockService.getMetricConflicts()
    }).subscribe({
      next: ({ products, conflicts }) => {
        this.allProducts = products;


        this.duplicateProducts = conflicts.duplicateProducts || [];
        this.duplicateSkus = conflicts.duplicateSkus || [];
        this.duplicateGtins = conflicts.duplicateGtins || [];

        this.metrics.conflicts.issues.duplicateCode = this.duplicateProducts.length;
        this.metrics.conflicts.issues.duplicateSku = this.duplicateSkus.length;
        this.metrics.conflicts.issues.duplicateGtin = this.duplicateGtins.length;
        this.metrics.conflicts.total = this.duplicateProducts.length + this.duplicateSkus.length + this.duplicateGtins.length;


        let activeCount = 0;
        let noCat = 0;
        let noBrand = 0;
        let priceZero = 0;

        products.forEach(p => {
          if (p.isActive) activeCount++;
          if (!p.productCategory || !p.productCategory.categoryId) noCat++;
          if (!p.productBrand || !p.productBrand.brandId) noBrand++;
          if (!p.price || p.price === 0) priceZero++;
        });

        this.metrics.products.active = activeCount;
        this.metrics.products.inactive = products.length - activeCount;
        this.metrics.products.issues.inactive = this.metrics.products.inactive;
        this.metrics.products.issues.noCategory = noCat;
        this.metrics.products.issues.noBrand = noBrand;
        this.metrics.products.issues.priceZero = priceZero;


        const validProducts = products.filter(p => p.productId !== undefined && p.productId !== null);

        if (validProducts.length > 0) {
          const variantRequests = validProducts.map(p => this.stockService.getVariants(p.productId!));
          forkJoin(variantRequests).subscribe({
            next: (variantsList) => {
              const enrichedVariants: (ProductVariant & { product: Product; variantName: string })[] = [];

              let totalVariants = 0;
              let noGtin = 0;
              let noWeight = 0;

              let prodBelow = 0;
              let prodOk = 0;
              let varBelow = 0;
              let varOk = 0;
              let noMinConfig = 0;

              validProducts.forEach((prod, index) => {
                const variants = variantsList[index] || [];


                const pQty = prod.stockQuantity || 0;
                const pMin = prod.minStock || 0;
                if (pMin > 0) {
                  if (pQty < pMin) prodBelow++; else prodOk++;
                } else {
                  noMinConfig++;
                }

                variants.forEach(v => {
                  totalVariants++;
                  const vName = v.attributes?.map(a => `${a.attributeKey}: ${a.attributeValue}`).join(', ') || '';
                  enrichedVariants.push({ ...v, product: prod, variantName: vName });


                  if (!v.variantGtin || v.variantGtin.toString().trim() === '') noGtin++;
                  if (!v.netWeightGrams || v.netWeightGrams === 0) noWeight++;


                  const vQty = v.stockQuantity || 0;
                  const vMin = v.minStock || 0;
                  if (vMin > 0) {
                    if (vQty < vMin) varBelow++; else varOk++;
                  } else {
                    noMinConfig++;
                  }
                });
              });

              this.allVariants = enrichedVariants;

              this.metrics.inventory.variants = totalVariants;
              this.metrics.inventory.noGtin = noGtin;
              this.metrics.inventory.issues.noGtin = noGtin;
              this.metrics.inventory.issues.noWeight = noWeight;

              const totalBelow = prodBelow + varBelow;
              const totalOk = prodOk + varOk;

              this.metrics.stockMin.below = totalBelow;
              this.metrics.stockMin.ok = totalOk;
              this.metrics.stockMin.issues.belowMin = totalBelow;
              this.metrics.stockMin.issues.noMinConfig = noMinConfig;


              this.processGrowthData();


              this.updateIssueMetrics();


              if (preserveState) {
                this.selectSection(currentSection);
                if (currentIssue) {
                  this.selectIssue(currentIssue);
                }
              } else {
                this.selectSection('PRODUCTS');
              }
              this.isLoading = false;
            },
            error: (err) => {
              console.error('Error loading variants', err);
              this.isLoading = false;
            }
          });
        } else {
          if (preserveState) {
            this.selectSection(currentSection);
          } else {
            this.selectSection('PRODUCTS');
          }
          this.isLoading = false;
        }
      },
      error: (err) => {
        console.error('Error loading initial data', err);
        this.isLoading = false;
      }
    });
  }

  getIssueMetrics(): { label: string, count: number, percent: number, type: IssueType, description: string }[] {
    return this.issueMetrics;
  }



  updateIssueMetrics() {
    const issues: { label: string, count: number, percent: number, type: IssueType, description: string }[] = [];
    let total = 0;

    switch (this.selectedSection) {
      case 'PRODUCTS':
        total = this.allProducts.length || 1;
        issues.push({
          label: 'Inactivos',
          count: this.metrics.products.issues.inactive,
          percent: (this.metrics.products.issues.inactive / total) * 100,
          type: 'INACTIVE',
          description: 'Productos marcados como inactivos. No se muestran en ventas.'
        });
        issues.push({
          label: 'Sin Cat',
          count: this.metrics.products.issues.noCategory,
          percent: (this.metrics.products.issues.noCategory / total) * 100,
          type: 'NO_CATEGORY',
          description: 'Productos que no pertenecen a ninguna categoría.'
        });
        issues.push({
          label: 'Sin Marca',
          count: this.metrics.products.issues.noBrand,
          percent: (this.metrics.products.issues.noBrand / total) * 100,
          type: 'NO_BRAND',
          description: 'Productos sin marca asignada.'
        });
        issues.push({
          label: 'Precio 0',
          count: this.metrics.products.issues.priceZero,
          percent: (this.metrics.products.issues.priceZero / total) * 100,
          type: 'PRICE_ZERO',
          description: 'Productos con precio de venta igual a 0.'
        });
        break;

      case 'CONFLICTS':
        total = this.metrics.conflicts.total || 1;
        issues.push({
          label: 'Duplicado Código',
          count: this.metrics.conflicts.issues.duplicateCode,
          percent: (this.metrics.conflicts.issues.duplicateCode / total) * 100,
          type: 'DUPLICATE_CODE',
          description: 'Productos con el mismo Código Interno.'
        });
        issues.push({
          label: 'Duplicado SKU',
          count: this.metrics.conflicts.issues.duplicateSku,
          percent: (this.metrics.conflicts.issues.duplicateSku / total) * 100,
          type: 'DUPLICATE_SKU',
          description: 'Variantes con el mismo SKU.'
        });
        issues.push({
          label: 'Duplicado GTIN',
          count: this.metrics.conflicts.issues.duplicateGtin,
          percent: (this.metrics.conflicts.issues.duplicateGtin / total) * 100,
          type: 'DUPLICATE_GTIN',
          description: 'Variantes con el mismo GTIN/EAN.'
        });
        break;

      case 'INVENTARIO':
        total = this.metrics.inventory.variants || 1;
        issues.push({
          label: 'Sin GTIN',
          count: this.metrics.inventory.issues.noGtin,
          percent: (this.metrics.inventory.issues.noGtin / total) * 100,
          type: 'NO_GTIN',
          description: 'Variantes sin código de barras (GTIN/EAN).'
        });
        issues.push({
          label: 'Sin Peso',
          count: this.metrics.inventory.issues.noWeight,
          percent: (this.metrics.inventory.issues.noWeight / total) * 100,
          type: 'NO_WEIGHT',
          description: 'Variantes sin peso especificado (requerido para logística).'
        });
        break;
      case 'MINIMUM':
        total = (this.metrics.stockMin.below + this.metrics.stockMin.ok) || 1;
        issues.push({
          label: 'Bajo Mín',
          count: this.metrics.stockMin.issues.belowMin,
          percent: (this.metrics.stockMin.issues.belowMin / total) * 100,
          type: 'BELOW_MIN',
          description: 'Stock actual inferior al mínimo configurado.'
        });
        issues.push({
          label: 'Sin Config',
          count: this.metrics.stockMin.issues.noMinConfig,
          percent: (this.metrics.stockMin.issues.noMinConfig / total) * 100,
          type: 'NO_MIN_CONFIG',
          description: 'Productos sin configuración de stock mínimo.'
        });
        break;
      case 'GROWTH':
        total = this.growthItems.length || 1;
        issues.push({
          label: 'Sin Costo',
          count: this.metrics.growth.issues.noCost,
          percent: (this.metrics.growth.issues.noCost / total) * 100,
          type: 'NO_COST',
          description: 'Items con costo 0 o no asignado.'
        });
        issues.push({
          label: 'Margen Neg',
          count: this.metrics.growth.issues.negativeMargin,
          percent: (this.metrics.growth.issues.negativeMargin / total) * 100,
          type: 'NEGATIVE_MARGIN',
          description: 'Precio de venta menor al costo (Pérdida).'
        });
        issues.push({
          label: 'Desvío Costo',
          count: this.metrics.growth.issues.highDeviation,
          percent: (this.metrics.growth.issues.highDeviation / total) * 100,
          type: 'HIGH_DEVIATION',
          description: 'Costo de variante difiere >30% del producto base.'
        });
        issues.push({
          label: 'Margen Bajo',
          count: this.metrics.growth.issues.lowMargin,
          percent: (this.metrics.growth.issues.lowMargin / total) * 100,
          type: 'LOW_MARGIN',
          description: 'Margen inferior al objetivo establecido.'
        });
        break;
    }
    this.issueMetrics = issues;
  }



  selectSection(section: MetricSection) {
    this.selectedSection = section;
    this.updateIssueMetrics();


    switch (section) {
      case 'PRODUCTS':    this.selectIssue('INACTIVE');         break;
      case 'CONFLICTS':   this.selectIssue('DUPLICATE_CODE');   break;
      case 'INVENTARIO':  this.selectIssue('NO_GTIN');          break;
      case 'MINIMUM':     this.selectIssue('BELOW_MIN');        break;
      case 'GROWTH':      this.selectIssue('NEGATIVE_MARGIN');  break;
    }
  }


  mobileSelectSection(section: MetricSection) {
    this.selectSection(section);
    this.mobilePage = 'detail';
  }

  mobileBack() {
    this.mobilePage = 'sections';
  }

  selectIssue(issue: IssueType) {
    this.selectedIssue = issue;
    this.updateDetailView();
  }

  toggleFullTable() {
    this.showFullTableModal = !this.showFullTableModal;
  }



  updateDetailView() {
    this.applyFilters();
  }

  applyFilters() {
    if (!this.selectedIssue) return;

    switch (this.selectedSection) {
      case 'PRODUCTS':
        this.filteredProducts = this.allProducts.filter(p => {
          if (this.selectedIssue === 'INACTIVE') return !p.isActive;
          if (this.selectedIssue === 'NO_CATEGORY') return !p.productCategory || !p.productCategory.categoryId;
          if (this.selectedIssue === 'NO_BRAND') return !p.productBrand || !p.productBrand.brandId;
          if (this.selectedIssue === 'PRICE_ZERO') return !p.price || p.price === 0;
          return true;
        });
        break;

      case 'CONFLICTS':
        if (this.selectedIssue === 'DUPLICATE_CODE') {
          this.filteredConflicts = this.duplicateProducts;
        } else if (this.selectedIssue === 'DUPLICATE_SKU') {
          this.filteredConflicts = this.duplicateSkus;
        } else if (this.selectedIssue === 'DUPLICATE_GTIN') {
          this.filteredConflicts = this.duplicateGtins;
        }
        break;

      case 'INVENTARIO':
        this.filteredInventory = this.allVariants.filter(v => {
          if (this.selectedIssue === 'NO_GTIN') return !v.variantGtin || v.variantGtin.trim() === '';
          if (this.selectedIssue === 'NO_WEIGHT') return !v.netWeightGrams || v.netWeightGrams === 0;
          return true;
        });
        break;

      case 'MINIMUM':
        const items: any[] = [];

        const addItem = (type: string, name: string, variant: string, qty: number, min: number, reason: string, ref: any) => {
          items.push({ type, name, variant, current: qty, min, diff: min - qty, reason, ref });
        };

        if (this.selectedIssue === 'BELOW_MIN') {
          this.allProducts.forEach(p => {
            if (p.minStock && p.minStock > 0 && (p.stockQuantity || 0) < p.minStock) {
              addItem('Prod', p.productName, '-', p.stockQuantity || 0, p.minStock, 'Bajo Mínimo', p);
            }
          });
          this.allVariants.forEach(v => {
            if (v.minStock && v.minStock > 0 && (v.stockQuantity || 0) < v.minStock) {
              addItem('Var', v.product.productName, v.variantSku || '', v.stockQuantity || 0, v.minStock, 'Bajo Mínimo', v);
            }
          });
        } else if (this.selectedIssue === 'NO_MIN_CONFIG') {
          this.allProducts.forEach(p => {
            if (!p.minStock || p.minStock === 0) {
              addItem('Prod', p.productName, '-', p.stockQuantity || 0, 0, 'Sin Configuración', p);
            }
          });
          this.allVariants.forEach(v => {
            if (!v.minStock || v.minStock === 0) {
              addItem('Var', v.product.productName, v.variantSku || '', v.stockQuantity || 0, 0, 'Sin Configuración', v);
            }
          });
        }
        this.filteredMinimum = items;
        break;

      case 'GROWTH':
        this.filteredGrowth = this.growthItems.filter(item => {
          if (this.selectedIssue === 'NO_COST') return item.costoEfectivo <= 0;
          if (this.selectedIssue === 'NEGATIVE_MARGIN') return item.precioEfectivo > 0 && item.costoEfectivo > 0 && item.precioEfectivo < item.costoEfectivo;
          if (this.selectedIssue === 'HIGH_DEVIATION') {
            return item.variant && item.sourceCost === 'VARIANTE' && item.product.cost && item.product.cost > 0 &&
              (Math.abs(item.costoEfectivo - item.product.cost) / item.product.cost >= 0.30);
          }
          if (this.selectedIssue === 'LOW_MARGIN') return !item.cumple && item.costoEfectivo > 0 && item.precioEfectivo > 0;
          return true;
        });


        if (this.selectedIssue === 'LOW_MARGIN') {
          this.filteredGrowth.sort((a, b) => b.impactoGap - a.impactoGap);
        } else if (this.selectedIssue === 'NO_COST') {
          this.filteredGrowth.sort((a, b) => b.stock - a.stock);
        } else if (this.selectedIssue === 'NEGATIVE_MARGIN') {
          this.filteredGrowth.sort((a, b) => ((b.costoEfectivo - b.precioEfectivo) * b.stock) - ((a.costoEfectivo - a.precioEfectivo) * a.stock));
        }
        break;
    }
  }

  getSectionIndex(section: MetricSection): number {
    const sections: MetricSection[] = ['PRODUCTS', 'INVENTARIO', 'MINIMUM', 'GROWTH', 'CONFLICTS'];
    return sections.indexOf(section);
  }

  getActionableCount(): number {
    switch (this.selectedSection) {
      case 'PRODUCTS': return this.filteredProducts.length;
      case 'INVENTARIO': return this.filteredInventory.length;
      case 'MINIMUM': return this.filteredMinimum.length;
      case 'GROWTH': return this.filteredGrowth.length;
      case 'CONFLICTS': return this.filteredConflicts.length;
      default: return 0;
    }
  }

  getSectionTitle(section: MetricSection): string {
    switch (section) {
      case 'PRODUCTS': return 'Productos';
      case 'INVENTARIO': return 'Inventario';
      case 'MINIMUM': return 'Reposición';
      case 'GROWTH': return 'Rentabilidad';
      case 'CONFLICTS': return 'Conflictos';
      default: return '';
    }
  }

  getIssueTitle(issue: IssueType | null = this.selectedIssue): string {
    if (!issue) return '';
    switch (issue) {
      case 'INACTIVE': return 'Inactivos';
      case 'NO_CATEGORY': return 'Sin Categoría';
      case 'NO_BRAND': return 'Sin Marca';
      case 'PRICE_ZERO': return 'Precio Base 0';
      case 'NO_GTIN': return 'Sin GTIN';
      case 'NO_WEIGHT': return 'Sin Peso';
      case 'BELOW_MIN': return 'Bajo Mínimo';
      case 'NO_MIN_CONFIG': return 'Sin Mínimo';
      case 'NO_COST': return 'Sin Costo';
      case 'NEGATIVE_MARGIN': return 'Margen Negativo';
      case 'HIGH_DEVIATION': return 'Desvío Costo';
      case 'LOW_MARGIN': return 'Margen Bajo';
      case 'DUPLICATE_CODE': return 'Duplicado Código';
      case 'DUPLICATE_SKU': return 'Duplicado SKU';
      case 'DUPLICATE_GTIN': return 'Duplicado GTIN';
      default: return '';
    }
  }

  editProduct(product: Product) {
    if (product && product.productId) {
      this.router.navigate(['/stock/products', product.productId]);
    }
  }

  editVariant(variant: ProductVariant) {
    if (variant && variant.product) {
      this.router.navigate(['/stock/products', variant.product.productId]);
    }
  }

  openGrowthItem(item: GrowthItem) {
    if (item.variant) {
      this.editVariant(item.variant);
    } else {
      this.editProduct(item.product);
    }
  }

  getKpiGradient(section: MetricSection): string {

    const cOk = '#86efac';
    const cWarning = '#fbbf24';
    const cCritical = '#f87171';
    const cNeutral = '#e5e7eb';

    let value = 0, total = 0;
    let mainColor = cNeutral;


    const sweep = 270;

    switch (section) {
      case 'PRODUCTS':


        value = this.metrics.products.active;
        total = (this.metrics.products.active + this.metrics.products.inactive) || 1;
        mainColor = cOk;
        break;

      case 'INVENTARIO':






        value = this.metrics.inventory.variants;
        total = this.metrics.inventory.variants || 1;



        value = 1; total = 1;
        mainColor = cOk;
        break;

      case 'MINIMUM':


        value = this.metrics.stockMin.below;
        total = (this.metrics.stockMin.ok + this.metrics.stockMin.below) || 1;
        mainColor = cCritical;
        break;

      case 'GROWTH':


        value = this.metrics.growth.itemsNegativeMargin;
        total = this.growthItems.length || 1;
        mainColor = cCritical;
        break;

      case 'CONFLICTS':
        value = this.metrics.conflicts.total;
        total = this.metrics.conflicts.total || 1;
        mainColor = cCritical;
        break;
    }

    const deg = (value / total) * sweep;


    return `conic-gradient(from 225deg, ${mainColor} 0deg ${deg}deg, ${cNeutral} ${deg}deg ${sweep}deg, transparent ${sweep}deg)`;
  }

  getKpiTooltip(section: MetricSection): string {
    switch (section) {
      case 'PRODUCTS':
        return `Activos: ${this.metrics.products.active}, Inactivos: ${this.metrics.products.inactive}`;
      case 'CONFLICTS':
        return `Códigos: ${this.metrics.conflicts.issues.duplicateCode}, SKUs: ${this.metrics.conflicts.issues.duplicateSku}, GTINs: ${this.metrics.conflicts.issues.duplicateGtin}`;
      case 'INVENTARIO':
        return `Con GTIN: ${this.metrics.inventory.variants - this.metrics.inventory.noGtin}, Sin GTIN: ${this.metrics.inventory.noGtin}`;
      case 'MINIMUM':
        return `OK: ${this.metrics.stockMin.ok}, Bajo: ${this.metrics.stockMin.below}`;
      case 'GROWTH':
        return `Rentables: ${this.growthItems.length - this.metrics.growth.itemsNegativeMargin}, Margen Neg: ${this.metrics.growth.itemsNegativeMargin}`;
      default:
        return '';
    }
  }



  processGrowthData() {
    const items: GrowthItem[] = [];
    const productsWithVariants = new Set<number>();


    this.allVariants.forEach(v => {
      if (v.product && v.product.productId) {
        productsWithVariants.add(v.product.productId);
        items.push(this.createGrowthItem(v.product, v));
      }
    });


    this.allProducts.forEach(p => {
      if (p.productId && !productsWithVariants.has(p.productId)) {
        items.push(this.createGrowthItem(p));
      }
    });

    this.growthItems = items;
    this.calculateEconomicMetrics();
  }

  createGrowthItem(product: Product, variant?: ProductVariant & { variantName?: string }): GrowthItem {
    const isVariant = !!variant;



    let costo = 0;
    let sourceCost: 'VARIANTE' | 'PRODUCTO' = 'PRODUCTO';

    if (isVariant && variant?.cost && variant.cost > 0) {
      costo = variant.cost;
      sourceCost = 'VARIANTE';
    } else if (product.cost && product.cost > 0) {
      costo = product.cost;
      sourceCost = 'PRODUCTO';
    }


    let precio = 0;
    let sourcePrice: 'VARIANTE' | 'PRODUCTO' = 'PRODUCTO';

    if (isVariant && variant?.price && variant.price > 0) {
      precio = variant.price;
      sourcePrice = 'VARIANTE';
    } else if (product.price && product.price > 0) {
      precio = product.price;
      sourcePrice = 'PRODUCTO';
    }

    const stock = isVariant ? (variant?.stockQuantity || 0) : (product.stockQuantity || 0);
    const id = isVariant ? `V-${variant?.productVariantId}` : `P-${product.productId}`;
    const code = isVariant ? (variant?.variantSku || product.internalCode || '') : (product.internalCode || '');
    const name = isVariant ? `${product.productName} - ${variant?.variantName}` : product.productName;

    return {
      id,
      code,
      name,
      stock,
      costoEfectivo: costo,
      precioEfectivo: precio,
      valorInventario: stock * costo,
      sourceCost,
      sourcePrice,
      precioSugerido: 0,
      margenPct: 0,
      cumple: false,
      gap: 0,
      impactoGap: 0,
      product,
      variant
    };
  }

  calculateEconomicMetrics() {

    let totalValue = 0;
    let noCostCount = 0;
    let negativeMarginCount = 0;
    let valueNoCost = 0;
    let lowMarginCount = 0;
    let highDeviationCount = 0;

    this.growthItems.forEach(item => {

      item.precioSugerido = item.costoEfectivo * (1 + this.targetMargin);

      if (item.costoEfectivo > 0) {
        item.margenPct = (item.precioEfectivo - item.costoEfectivo) / item.costoEfectivo;
      } else {
        item.margenPct = 0;
      }

      item.cumple = item.precioEfectivo >= item.precioSugerido;
      item.gap = Math.max(0, item.precioSugerido - item.precioEfectivo);
      item.impactoGap = item.gap * item.stock;


      totalValue += item.valorInventario;

      if (item.costoEfectivo <= 0) {
        noCostCount++;
        valueNoCost += item.stock;
      }

      if (item.precioEfectivo > 0 && item.costoEfectivo > 0 && item.precioEfectivo < item.costoEfectivo) {
        negativeMarginCount++;
      }

      if (!item.cumple && item.costoEfectivo > 0 && item.precioEfectivo > 0) {
        lowMarginCount++;
      }


      if (item.variant && item.sourceCost === 'VARIANTE' && item.product.cost && item.product.cost > 0) {
        const dev = Math.abs(item.costoEfectivo - item.product.cost) / item.product.cost;
        if (dev >= 0.30) highDeviationCount++;
      }
    });


    this.metrics.growth.totalInventoryValue = totalValue;
    this.metrics.growth.itemsNoCost = noCostCount;
    this.metrics.growth.itemsNegativeMargin = negativeMarginCount;
    this.metrics.growth.inventoryValueNoCost = valueNoCost;

    this.metrics.growth.issues.noCost = noCostCount;
    this.metrics.growth.issues.negativeMargin = negativeMarginCount;
    this.metrics.growth.issues.lowMargin = lowMarginCount;
    this.metrics.growth.issues.highDeviation = highDeviationCount;

    this.updateIssueMetrics();
  }

  onTargetMarginChange() {
    this.saveUserPreferences();
    this.calculateEconomicMetrics();

    if (this.selectedSection === 'GROWTH') {
      this.applyFilters();
    }
  }


  private getPrefsKey(): string {
    const username = this.authService.getUserName() || 'default';
    return `stock_metrics_prefs_${username}`;
  }

  private loadUserPreferences() {
    const stored = localStorage.getItem(this.getPrefsKey());
    if (stored) {
      try {
        const prefs = JSON.parse(stored);
        if (prefs.targetMargin) {
          this.targetMargin = prefs.targetMargin;
        }
      } catch (e) {
        console.error('Error loading preferences', e);
      }
    }
  }

  private saveUserPreferences() {
    const prefs = {
      targetMargin: this.targetMargin
    };
    localStorage.setItem(this.getPrefsKey(), JSON.stringify(prefs));
  }



}