import { Component, OnInit } from '@angular/core';
import { StockService } from '../../../services/stock.service';
import { Product, ProductVariant } from '../../../models/stock.models';
import { forkJoin } from 'rxjs';
import { LayoutService } from '../../../services/layout.service';
import { MobileCardColumn } from '../../../shared/components/mobile-card-list/mobile-card-list.models';

interface InventoryItem {
  id: number;
  displayId: string;
  code: string;
  name: string;
  type: 'PRODUCT' | 'VARIANT' | 'PRODUCT_GROUP';
  stock: number;
  level: number;
  parentName?: string;
  minStock: number;
}

@Component({
  selector: 'app-inventory',
  templateUrl: './inventory.component.html',
  styleUrls: ['./inventory.component.css']
})
export class InventoryComponent implements OnInit {
  items: InventoryItem[] = [];
  filteredItems: InventoryItem[] = [];
  searchTerm: string = '';
  isLoading: boolean = false;


  readonly mobileColumns: MobileCardColumn[] = [
    { field: 'name',  label: 'Producto/Variante', isPrimary: true },
    { field: 'code',  label: 'Código' },
    { field: 'stock', label: 'Stock actual',
      isBadge: true,
      format: (v: any) => String(v ?? 0),
      badgeClass: (item: any) => {
        if (item.minStock > 0 && item.stock <= item.minStock)         return 'badge-danger';
        if (item.minStock > 0 && item.stock <= item.minStock * 1.5)  return 'badge-warning';
        return 'badge-success';
      }
    },
    {
      field: 'minStock', label: 'Stock mínimo',
      format: (v: any) => v > 0 ? String(v) : 'Sin límite',
      expandOnly: true
    }
  ];


  constructor(private stockService: StockService, public layoutService: LayoutService) { }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    forkJoin({
      products: this.stockService.getProducts(),
      variants: this.stockService.getAllVariants()
    }).subscribe({
      next: (data) => {
        this.processData(data.products, data.variants);
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading inventory', err);
        this.isLoading = false;
      }
    });
  }

  processData(products: Product[], variants: ProductVariant[]): void {
    const variantMap = new Map<number, ProductVariant[]>();
    variants.forEach(v => {
      const pid = v.product?.productId;
      if (pid) {
        if (!variantMap.has(pid)) {
          variantMap.set(pid, []);
        }
        variantMap.get(pid)?.push(v);
      }
    });

    const inventoryList: InventoryItem[] = [];


    products.sort((a, b) => a.productName.localeCompare(b.productName));

    products.forEach(p => {
      const pVariants = variantMap.get(p.productId!) || [];

      if (pVariants.length > 0) {


        const totalStock = pVariants.reduce((sum, v) => sum + (v.stockQuantity || 0), 0);

        inventoryList.push({
          id: p.productId!,
          displayId: `p-${p.productId}`,
          code: p.internalCode || 'N/A',
          name: p.productName,
          type: 'PRODUCT_GROUP',
          stock: totalStock,
          level: 0,
          minStock: 0
        });


        pVariants.forEach(v => {

          let variantName = v.variantSku || 'Variant';
          if (v.attributes && v.attributes.length > 0) {
            const attrs = v.attributes.map(a => `${a.attributeKey}: ${a.attributeValue}`).join(', ');
            variantName += ` (${attrs})`;
          }

          inventoryList.push({
            id: v.productVariantId!,
            displayId: `v-${v.productVariantId}`,
            code: v.variantSku || v.variantGtin || 'N/A',
            name: variantName,
            type: 'VARIANT',
            stock: v.stockQuantity || 0,
            level: 1,
            parentName: p.productName,
            minStock: v.minStock || 0
          });
        });

      } else {

        inventoryList.push({
          id: p.productId!,
          displayId: `p-${p.productId}`,
          code: p.internalCode || 'N/A',
          name: p.productName,
          type: 'PRODUCT',
          stock: p.stockQuantity || 0,
          level: 0,
          minStock: p.minStock || 0
        });
      }
    });

    this.items = inventoryList;
    this.filterItems();
  }

  filterItems(): void {
    if (!this.searchTerm) {
      this.filteredItems = this.items;
      return;
    }

    const term = this.searchTerm.toLowerCase();
    this.filteredItems = this.items.filter(item => {
      const matchName = item.name.toLowerCase().includes(term);
      const matchCode = item.code.toLowerCase().includes(term);
      const matchParent = item.parentName ? item.parentName.toLowerCase().includes(term) : false;
      return matchName || matchCode || matchParent;
    });
  }

  getStockStatus(item: InventoryItem): { label: string, cssClass: string } {

    if (item.type === 'PRODUCT_GROUP') return { label: '', cssClass: '' };

    if (item.minStock > 0 && item.stock <= item.minStock) {
      return { label: 'Crítico', cssClass: 'status-danger' };
    } else if (item.minStock > 0 && item.stock > item.minStock && item.stock <= item.minStock * 1.5) {
      return { label: 'Bajo', cssClass: 'status-warning' };
    } else {
      return { label: 'Normal', cssClass: 'status-ok' };
    }
  }

  trackByFn(index: number, item: InventoryItem): string {
    return item.displayId;
  }
}
