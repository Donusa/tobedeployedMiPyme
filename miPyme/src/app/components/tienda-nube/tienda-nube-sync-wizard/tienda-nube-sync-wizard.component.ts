import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TiendaNubeService } from '../../../services/tienda-nube.service';
import { StockService } from '../../../services/stock.service';
import { catchError, timeout } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-tienda-nube-sync-wizard',
  templateUrl: './tienda-nube-sync-wizard.component.html',
  styleUrls: ['./tienda-nube-sync-wizard.component.css']
})
export class TiendaNubeSyncWizardComponent implements OnInit {
  isLoading = false;
  loadingMessage = '';
  errorMessage = '';
  successMessage = '';
  isConnected = false;
  step: number = 1;

  get iconClass(): string {
    if (this.step === 2) return 'bx-box';
    if (this.isConnected && this.step === 1) return 'bx-check';
    return 'bx-store-alt';
  }

  products: any[] = [];
  allTnProducts: any[] = [];
  problematicTnProducts: any[] = [];
  localProducts: any[] = [];
  linkedLocalProducts: any[] = [];
  orders: any[] = [];
  tokenData: any = null;
  activeTab: 'products' | 'sales' | 'linked' | 'problems' = 'products';
  selectedProducts: Set<number> = new Set();
  selectedLinkedProducts: Set<number> = new Set();

  conflicts: any[] = [];
  showConflictModal = false;
  showMappingModal = false;
  showUnlinkSuccessModal = false;
  showUnlinkConfirmModal = false;
  mappingCandidates: { tnProduct: any, selectedLocalId: number | null }[] = [];
  variantMappings: { [tnProductId: number]: { [tnVariantId: number]: number } } = {};

  private readonly APP_ID = '26042';
  private readonly REDIRECT_URI = 'https://px47l7q6-4200.brs.devtunnels.ms/tiendanube';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private tiendaNubeService: TiendaNubeService,
    private stockService: StockService
  ) { }

  ngOnInit(): void {

    this.route.queryParams.subscribe(params => {
      const code = params['code'];
      if (code) {
        this.handleAuthCode(code);
      } else {

        this.checkConnectionStatus();
      }
    });
    this.loadLocalProducts();
  }

  loadLocalProducts() {
    this.stockService.getProducts().subscribe({
      next: (products) => {
        this.localProducts = products;
        this.filterSyncedProducts();
      },
      error: (err) => {
        console.error('Error loading local products', err);
      }
    });
  }

  checkConnectionStatus() {
    this.isLoading = true;
    this.loadingMessage = 'Verificando conexión...';

    this.tiendaNubeService.getConnectionStatus().pipe(
      timeout(5000),
      catchError(err => {
        console.error('Connection check timed out or failed', err);
        return of({ connected: false });
      })
    ).subscribe({
      next: (status) => {
        if (status.connected) {
          this.isConnected = true;
          this.tokenData = status;
          this.step = 2;
          this.loadProducts();
          this.loadLinkedProducts();
          this.loadSales();
        } else {
          this.isLoading = false;
        }
      },
      error: (err) => {
        console.error('Error checking connection status', err);
        this.isLoading = false;
      }
    });
  }

  startSync() {
    this.isLoading = true;
    this.loadingMessage = 'Redirigiendo a TiendaNube...';
    const authUrl = `https://www.tiendanube.com/apps/${this.APP_ID}/authorize?response_type=code&scope=write_products read_products read_orders write_orders read_customers write_customers read_orders_risk write_orders_risk read_draft_orders write_draft_orders read_coupons write_coupons read_fulfillment_orders write_fulfillment_orders&redirect_uri=${this.REDIRECT_URI}`;
    window.location.href = authUrl;
  }

  connect() {
    this.startSync();
  }

  handleAuthCode(code: string) {
    this.isLoading = true;
    this.loadingMessage = 'Conectando con TiendaNube...';
    this.errorMessage = '';


    this.router.navigate([], {
      queryParams: {
        code: null
      },
      queryParamsHandling: 'merge'
    });

    this.tiendaNubeService.exchangeToken(code, this.REDIRECT_URI).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.isConnected = true;
        this.tokenData = response;
        this.step = 2;
        this.loadProducts();
        this.loadLinkedProducts();
        this.loadSales();
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Error exchanging token:', error);
        this.errorMessage = 'Error al conectar con TiendaNube. Por favor intente nuevamente.';
      }
    });
  }

  loadProducts() {
    if (!this.tokenData) return;

    this.isLoading = true;
    this.loadingMessage = 'Aguarde un momento, estamos cargando sus productos...';

    this.tiendaNubeService.getProducts(this.tokenData.user_id, this.tokenData.access_token)
      .subscribe({
        next: (products) => {
          this.allTnProducts = products;
          this.problematicTnProducts = this.allTnProducts.filter(p => p.published === false);
          this.filterSyncedProducts();
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Error loading products', err);
          this.isLoading = false;
          this.errorMessage = 'Error al cargar productos.';
        }
      });
  }

  filterSyncedProducts() {
    if (!this.allTnProducts) return;


    const linkedTnIds = new Set(
      this.localProducts
        .filter(lp => lp.tiendaNubeId)
        .map(lp => Number(lp.tiendaNubeId))
    );


    this.products = this.allTnProducts.filter(p => !linkedTnIds.has(Number(p.id)));
  }

  getTnProduct(id: string | number): any {
    return this.allTnProducts.find(p => String(p.id) === String(id));
  }

  getProblematicTnProducts(): any[] {
    return this.problematicTnProducts;
  }

  toggleTab(tab: 'products' | 'sales' | 'linked' | 'problems') {
    this.activeTab = tab;
    if (tab === 'linked' && this.linkedLocalProducts.length === 0) {
      this.loadLinkedProducts();
    } else if (tab === 'sales' && this.orders.length === 0) {
      this.loadSales();
    }
  }

  loadSales() {
    this.isLoading = true;
    this.loadingMessage = 'Cargando ventas de TiendaNube...';
    this.tiendaNubeService.getOrders().subscribe({
      next: (orders) => {
        this.orders = orders;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading sales', err);
        this.isLoading = false;
        this.errorMessage = 'Error al cargar ventas.';
      }
    });
  }

  statusChip(kind: 'order' | 'payment' | 'shipping', value?: string): string {
    const v = (value || '').toLowerCase();
    if (kind === 'order') {
      if (v === 'open') return 'status-chip status-paid';
      if (v === 'closed') return 'status-chip status-confirmed';
      if (v === 'cancelled') return 'status-chip status-cancelled';
      if (v.includes('paid')) return 'status-chip status-paid';
    }
    if (kind === 'payment') {
      if (v === 'paid') return 'status-chip status-paid';
      if (v === 'pending') return 'status-chip status-processing';
      if (v === 'voided' || v === 'refunded' || v === 'abandoned') return 'status-chip status-cancelled';
      if (v === 'authorized') return 'status-chip status-ready';
    }
    if (kind === 'shipping') {
      if (v === 'shipped' || v === 'delivered') return 'status-chip status-shipped';
      if (v === 'packed') return 'status-chip status-processing';
      if (v === 'unpacked' || v === 'unshipped') return 'status-chip status-not-delivered';
    }
    return 'status-chip';
  }

  getOrderStatusBorderClass(order: any): string {
    const status = (order.status || '').toLowerCase();
    if (status === 'open' || status === 'closed' || status.includes('paid')) return 'border-success';
    if (status === 'cancelled') return 'border-danger';
    return 'border-neutral';
  }

  loadLinkedProducts() {
    this.isLoading = true;
    this.loadingMessage = 'Cargando productos vinculados...';
    this.tiendaNubeService.getLinkedLocalProducts().subscribe({
      next: (products) => {
        this.linkedLocalProducts = products;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading linked products', err);
        this.isLoading = false;
        this.errorMessage = 'Error al cargar productos vinculados.';
      }
    });
  }

  toggleLinkedProductSelection(productId: number) {
    if (this.selectedLinkedProducts.has(productId)) {
      this.selectedLinkedProducts.delete(productId);
    } else {
      this.selectedLinkedProducts.add(productId);
    }
  }

  isLinkedProductSelected(productId: number): boolean {
    return this.selectedLinkedProducts.has(productId);
  }

  closeUnlinkSuccessModal() {
    this.showUnlinkSuccessModal = false;
  }

  closeUnlinkConfirmModal() {
    this.showUnlinkConfirmModal = false;
  }

  unlinkSelected() {
    if (this.selectedLinkedProducts.size === 0) return;
    this.showUnlinkConfirmModal = true;
  }

  confirmUnlink() {
    this.showUnlinkConfirmModal = false;
    this.isLoading = true;
    this.loadingMessage = 'Desvinculando productos...';

    this.tiendaNubeService.unlinkProducts(Array.from(this.selectedLinkedProducts)).subscribe({
      next: () => {
        this.isLoading = false;
        this.selectedLinkedProducts.clear();
        this.loadLinkedProducts();

        this.loadLocalProducts();
        if (this.allTnProducts.length > 0) {




        }
        this.showUnlinkSuccessModal = true;
      },
      error: (err) => {
        console.error('Error unlinking products', err);
        this.isLoading = false;
        this.errorMessage = 'Error al desvincular productos.';
      }
    });
  }

  toggleProductSelection(productId: number) {
    if (this.selectedProducts.has(productId)) {
      this.selectedProducts.delete(productId);
    } else {
      this.selectedProducts.add(productId);
    }
  }

  isProductSelected(productId: number): boolean {
    return this.selectedProducts.has(productId);
  }

  syncSelected() {
    if (this.selectedProducts.size === 0) return;

    this.mappingCandidates = [];
    const productIds = Array.from(this.selectedProducts);

    productIds.forEach(id => {
      const tnP = this.products.find(p => p.id === id);
      if (tnP) {

        const tnName = tnP.name?.es || tnP.name?.pt || '';
        const match = this.localProducts.find(local =>
          local.productName && local.productName.toLowerCase().trim() === tnName.toLowerCase().trim()
        );

        this.mappingCandidates.push({
          tnProduct: tnP,
          selectedLocalId: match ? match.productId : null
        });
      }
    });

    this.showMappingModal = true;
  }

  confirmMapping() {
    this.isLoading = true;
    this.loadingMessage = 'Analizando diferencias...';
    this.showMappingModal = false;
    this.variantMappings = {};

    const requests = this.mappingCandidates.map(c => ({
      tnProductId: c.tnProduct.id,
      localProductId: c.selectedLocalId,
      createNew: !c.selectedLocalId
    }));

    this.tiendaNubeService.previewBulkSync(requests).subscribe({
      next: (previews) => {
        this.conflicts = previews;
        this.isLoading = false;
        this.showConflictModal = true;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Error al analizar productos.';
        console.error(err);
      }
    });
  }

  cancelMapping() {
    this.showMappingModal = false;
    this.mappingCandidates = [];
  }

  resolveConflict(previewIndex: number, diffIndex: number, resolution: 'LOCAL' | 'REMOTE') {
    const diff = this.conflicts[previewIndex].differences[diffIndex];
    if (resolution === 'LOCAL' || resolution === 'REMOTE') {
      diff.resolution = resolution;




    }
  }

  onVariantMatch(conflictIndex: number, diffIndex: number, localVariantId: any) {
    const conflict = this.conflicts[conflictIndex];
    const diff = conflict.differences[diffIndex];
    const tnProductId = conflict.tiendaNubeProduct.id;

    const tnVariantId = Number(diff.identifier);

    if (!this.variantMappings[tnProductId]) {
      this.variantMappings[tnProductId] = {};
    }

    if (localVariantId && localVariantId !== 'null') {
      this.variantMappings[tnProductId][tnVariantId] = Number(localVariantId);
    } else {

      this.variantMappings[tnProductId][tnVariantId] = -1;
    }


    this.refreshProductPreview(conflictIndex);
  }

  refreshProductPreview(index: number) {
    const conflict = this.conflicts[index];
    const tnProductId = conflict.tiendaNubeProduct.id;
    const localProductId = conflict.localProduct ? conflict.localProduct.productId : null;

    if (!localProductId) return;

    this.isLoading = true;

    const request = {
      tnProductId: tnProductId,
      localProductId: localProductId,
      variantMapping: this.variantMappings[tnProductId] || null
    };

    this.tiendaNubeService.previewBulkSync([request]).subscribe({
      next: (previews) => {
        if (previews && previews.length > 0) {
          this.conflicts[index] = previews[0];
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error("Error refreshing preview", err);
        this.isLoading = false;
      }
    });
  }

  onManualMatch(previewIndex: number, localProductId: any) {
    const tnProductId = this.conflicts[previewIndex].tiendaNubeProduct.id;
    this.isLoading = true;


    if (this.variantMappings[tnProductId]) {
      delete this.variantMappings[tnProductId];
    }


    let localId: number | null = null;
    if (localProductId && localProductId !== 'null' && localProductId !== 'undefined') {
      localId = Number(localProductId);
    }

    if (localId) {
      this.tiendaNubeService.previewManualMatch(tnProductId, localId).subscribe({
        next: (preview) => {
          this.conflicts[previewIndex] = preview;
          this.isLoading = false;
        },
        error: (err) => {
          console.error("Error matching product manually", err);
          this.isLoading = false;
          this.errorMessage = "Error al vincular producto manualmente.";
        }
      });
    } else {

      const request = {
        tnProductId: tnProductId,
        createNew: true,
        localProductId: null,
        variantMapping: null
      };
      this.tiendaNubeService.previewBulkSync([request]).subscribe({
        next: (previews) => {
          if (previews && previews.length > 0) {
            this.conflicts[previewIndex] = previews[0];
          }
          this.isLoading = false;
        },
        error: (err) => {
          console.error("Error resetting match", err);
          this.isLoading = false;
          this.errorMessage = "Error al restablecer vinculación.";
        }
      });
    }
  }

  confirmSync() {
    this.isLoading = true;
    this.loadingMessage = 'Sincronizando...';
    this.showConflictModal = false;

    this.tiendaNubeService.executeSync(this.conflicts).subscribe({
      next: () => {
        this.successMessage = 'Sincronización completada exitosamente.';
        this.selectedProducts.clear();
        this.conflicts = [];


        this.stockService.getProducts().subscribe({
          next: (products) => {
            this.localProducts = products;
            this.loadProducts();
          },
          error: (err) => {
            console.error('Error refreshing local products', err);
            this.isLoading = false;
          }
        });

        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Error al sincronizar.';
        console.error(err);
      }
    });
  }

  cancelSync() {
    this.showConflictModal = false;
    this.conflicts = [];
  }
}
