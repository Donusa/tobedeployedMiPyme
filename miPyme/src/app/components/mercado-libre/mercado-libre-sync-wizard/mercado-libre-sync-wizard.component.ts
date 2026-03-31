import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { MercadoLibreService, MercadoLibreMessage } from '../../../services/mercadolibre.service';
import { MeliRealtimeService } from '../../../services/meli-realtime.service';
import { StockService } from '../../../services/stock.service';

export interface MeliOrderSummary {
  orderId: number;
  packId?: number;
  buyerId?: number;
  dateCreated?: string;
  buyerNickname?: string;
  totalAmount?: number;
  currencyId?: string;
  orderStatus?: string;
  shippingId?: number;
  shippingStatus?: string;
  shippingSubstatus?: string;
  claimType?: string;
  claimStatus?: string;
}

@Component({
  selector: 'app-mercado-libre-sync-wizard',
  templateUrl: './mercado-libre-sync-wizard.component.html',
  styleUrls: ['./mercado-libre-sync-wizard.component.css']
})
export class MercadoLibreSyncWizardComponent implements OnInit {
  isLoading = false;
  loadingMessage = '';
  errorMessage = '';
  successMessage = '';
  isConnected = false;
  step: number = 1;

  get iconClass(): string {
    if (this.step === 2) return 'bx-box';
    if (this.isConnected && this.step === 1) return 'bx-check';
    return 'bx-shopping-bag';
  }

  products: any[] = [];
  allMeliProducts: any[] = [];
  problematicMeliProducts: any[] = [];
  localProducts: any[] = [];
  linkedLocalProducts: any[] = [];
  activeTab: 'products' | 'sales' | 'linked' | 'problems' = 'products';
  selectedProducts: Set<string> = new Set();
  selectedLinkedProducts: Set<number> = new Set();
  orders: MeliOrderSummary[] = [];


  showResolveModal = false;
  itemToResolve: any = null;
  resolveLoading = false;
  resolveError = '';
  resolveSuccess = '';

  private apiUrl = `${environment.apiUrl}/api/mercadolibre`;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private meliService: MercadoLibreService,
    private realtime: MeliRealtimeService,
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

  checkConnectionStatus() {
    this.isLoading = true;
    this.loadingMessage = 'Verificando conexión...';
    this.http.get<{ connected: boolean }>(`${this.apiUrl}/status`).subscribe({
      next: (res) => {
        this.isConnected = res.connected;
        if (this.isConnected) {
          this.step = 2;
          this.fetchProducts();
          this.loadLinkedProducts();
          this.fetchSales();
        } else {
          this.isLoading = false;
        }
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Status check failed', err);
        this.isConnected = false;
      }
    });
  }

  async connect() {
    this.isLoading = true;
    this.loadingMessage = 'Iniciando conexión...';

    try {
      const config = await this.http.get<{ appId: string, redirectUri: string }>(`${this.apiUrl}/config`).toPromise();

      if (!config || !config.appId) {
        throw new Error('Configuración de MercadoLibre no encontrada.');
      }

      const codeVerifier = this.generateRandomString(128);
      const codeChallenge = await this.generateCodeChallenge(codeVerifier);

      sessionStorage.setItem('meli_code_verifier', codeVerifier);

      const authUrl = `https://auth.mercadolibre.com.ar/authorization?response_type=code&client_id=${config.appId}&redirect_uri=${encodeURIComponent(config.redirectUri)}&code_challenge=${codeChallenge}&code_challenge_method=S256`;

      window.location.href = authUrl;
    } catch (err) {
      this.isLoading = false;
      this.errorMessage = 'Error al iniciar conexión: ' + (err as any).message;
    }
  }

  handleAuthCode(code: string) {
    this.isLoading = true;
    this.loadingMessage = 'Conectando con MercadoLibre...';

    const codeVerifier = sessionStorage.getItem('meli_code_verifier');
    if (!codeVerifier) {
      this.errorMessage = 'Error de seguridad: Code Verifier no encontrado. Por favor intente nuevamente.';
      this.isLoading = false;
      return;
    }

    this.http.get<{ redirectUri: string }>(`${this.apiUrl}/config`).subscribe(config => {
      this.http.post(`${this.apiUrl}/token`, {
        code,
        code_verifier: codeVerifier,
        redirect_uri: config.redirectUri
      }).subscribe({
        next: () => {
          this.isConnected = true;
          this.step = 2;
          this.successMessage = '¡Conexión exitosa!';
          sessionStorage.removeItem('meli_code_verifier');
          this.router.navigate([], {
            queryParams: { code: null, state: null },
            queryParamsHandling: 'merge',
            replaceUrl: true
          });
          this.fetchProducts();
          this.loadLinkedProducts();
          this.fetchSales();
        },
        error: (err) => {
          this.isLoading = false;
          this.errorMessage = 'Error al conectar: ' + (err.error?.message || err.message);
        }
      });
    });
  }

  fetchProducts() {
    this.isLoading = true;
    this.loadingMessage = 'Cargando productos de MercadoLibre...';
    this.http.get<any[]>(`${this.apiUrl}/products`).subscribe({
      next: (products) => {
        this.allMeliProducts = (products || []).map(p => {
          p._diagnosis = this.getProblemDiagnosis(p);
          return p;
        });
        this.problematicMeliProducts = this.allMeliProducts.filter(p => {
          const isProblematicStatus = ['paused', 'closed', 'inactive', 'under_review', 'deleted'].includes(p.status);
          const hasSubStatus = p.sub_status && p.sub_status.length > 0;
          return isProblematicStatus || hasSubStatus;
        });
        this.filterUnsyncedProducts();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error fetching products', err);
        this.isLoading = false;
        this.errorMessage = 'Error al cargar productos de MercadoLibre.';
      }
    });
  }

  loadLocalProducts() {
    this.stockService.getProducts().subscribe({
      next: (products) => {
        this.localProducts = products || [];
        this.filterUnsyncedProducts();
      },
      error: () => { }
    });
  }

  filterUnsyncedProducts() {
    if (!this.allMeliProducts) {
      this.products = [];
      return;
    }
    const linkedIds = new Set(
      (this.localProducts || [])
        .filter(p => (p as any).mercadoLibreId)
        .map(p => String((p as any).mercadoLibreId))
    );
    this.products = (this.allMeliProducts || []).filter(p => !linkedIds.has(String(p.id)));
  }

  getMlProduct(id: string): any {
    return (this.allMeliProducts || []).find(p => String(p.id) === String(id));
  }

  getProblematicMeliProducts(): any[] {
    return this.problematicMeliProducts;
  }

  toggleTab(tab: 'products' | 'sales' | 'linked' | 'problems') {
    console.log('[ML Wizard] toggleTab', tab);
    this.activeTab = tab;
    if (tab === 'sales' && this.orders.length === 0) {
      this.fetchSales();
    } else if (tab === 'linked' && this.linkedLocalProducts.length === 0) {
      this.loadLinkedProducts();
    }
  }

  getProblemDiagnosis(product: any): { message: string, action: string, type: 'error' | 'warning' | 'info', instructions?: string[] } | null {
    const status = product.status;
    const subStatuses = product.sub_status || [];

    if (status === 'under_review') {
      if (subStatuses.includes('warning')) {
        return { message: 'Tiene una corrección pendiente. Riesgo de ser ocultada.', action: 'Revisar y editar desde MercadoLibre.', type: 'warning', instructions: ['Ingresá a tu cuenta de MercadoLibre.', 'Buscá esta publicación en "Publicaciones".', 'Revisá las advertencias indicadas y corregí las imágenes o la descripción.', 'Guardá los cambios y esperá la revalidación.'] };
      }
      if (subStatuses.includes('waiting_for_patch')) {
        return { message: 'Oculta por errores reportados.', action: 'Corregir el problema desde MercadoLibre.', type: 'error', instructions: ['Ingresá a tu cuenta de MercadoLibre.', 'Buscá esta publicación en "Publicaciones" > "Inactivas".', 'Leé el motivo del error y editá los campos indicados.', 'Guardá y esperá que ML la reactive.'] };
      }
      if (subStatuses.includes('held')) {
        return { message: 'Esperando moderación manual.', action: 'Esperar resolución o contactar a soporte de ML.', type: 'info', instructions: ['Esta publicación está siendo revisada por el equipo de MercadoLibre.', 'No se requiere acción inmediata.', 'Si demora más de 48hs, contactá a soporte de ML.'] };
      }
      if (subStatuses.includes('pending_documentation')) {
        return { message: 'Oculta temporalmente.', action: 'Presentar documentación requerida.', type: 'warning', instructions: ['Ingresá a tu cuenta de MercadoLibre.', 'Revisá las notificaciones pendientes.', 'Subí la documentación solicitada (ej: certificados, facturas de compra).', 'Esperá la aprobación.'] };
      }
      if (subStatuses.includes('forbidden')) {
        return { message: 'Dada de baja por moderación.', action: 'Esta publicación debe ser eliminada.', type: 'error', instructions: ['Ingresá a tu cuenta de MercadoLibre.', 'Buscá esta publicación.', 'Eliminala de forma manual.', 'En la app MiPyme, desvinculá el producto local editándolo o dejá que se actualice la sincronización.'] };
      }
      return { message: 'En revisión por MercadoLibre.', action: 'Verificar estado en el panel de MercadoLibre.', type: 'info', instructions: ['Ingresá a tu cuenta de MercadoLibre.', 'Revisá el estado de esta publicación en "Publicaciones".', 'Seguí las instrucciones que MercadoLibre indique.'] };
    }

    if (status === 'paused') {
      if (subStatuses.includes('out_of_stock')) {
        return { message: 'Pausada por falta de stock.', action: 'Agregar stock desde MercadoLibre.', type: 'warning', instructions: ['Ingresá a tu cuenta de MercadoLibre.', 'Buscá esta publicación en "Publicaciones" > "Pausadas".', 'Editá la publicación y agregá stock disponible.', 'Al guardar, se reactivará automáticamente.'] };
      }
      if (subStatuses.includes('picture_download_pending')) {
        return { message: 'Esperando descarga de imágenes.', action: 'Revisar las fotos desde MercadoLibre.', type: 'warning', instructions: ['Ingresá a tu cuenta de MercadoLibre.', 'Editá esta publicación y revisá las imágenes.', 'Asegurate de que las URLs sean válidas y accesibles.', 'Resubí las fotos si es necesario.'] };
      }
      return { message: 'La publicación está pausada.', action: 'Reactivar desde MercadoLibre.', type: 'info', instructions: ['Ingresá a tu cuenta de MercadoLibre.', 'Buscá esta publicación en "Publicaciones" > "Pausadas".', 'Hacé clic en "Activar" para reactivarla.'] };
    }

    if (status === 'inactive') {
      return { message: 'Inactiva (generalmente por no corregir errores a tiempo).', action: 'No es posible editar la publicación. Solo se puede eliminar y desvincular.', type: 'error', instructions: ['Ingresá a tu cuenta de MercadoLibre.', 'Buscá esta publicación inactiva.', 'Eliminala o republicala como un ítem nuevo.', 'En MiPyme, actualizá el ID de MercadoLibre en el producto local si hiciste una publicación nueva.'] };
    }

    if (status === 'closed') {
      if (subStatuses.includes('suspended') || subStatuses.includes('freezed')) {
        return { message: 'Cerrada o suspendida por políticas de ML.', action: 'Contactar a soporte de MercadoLibre.', type: 'error', instructions: ['Ingresá a tu cuenta de MercadoLibre.', 'Buscá las notificaciones de suspensión.', 'Contactá a soporte de ML para resolver el problema.', 'Una vez devuelta, la app se sincronizará automáticamente.'] };
      }
      if (subStatuses.includes('deleted')) {
        return { message: 'Publicación eliminada.', action: 'Fue dada de baja u ocultada definitivamente.', type: 'error', instructions: ['Esta publicación fue eliminada de MercadoLibre.', 'No se requieren más acciones en ML.', 'En MiPyme, podés desvincular el producto local editándolo y borrando el ID de MercadoLibre.'] };
      }
      return { message: 'Publicación cerrada permanentemente.', action: 'No puede reactivarse. Debe republicarse como un ítem nuevo.', type: 'error', instructions: ['Ingresá a tu cuenta de MercadoLibre.', 'Republicá el ítem como una publicación nueva.', 'En MiPyme, actualizá el ID de MercadoLibre en el producto local para enlazarlo con la nueva publicación.'] };
    }

    if (status === 'payment_required') {
      return { message: 'Restringida por deuda o política de crédito.', action: 'Regularizar estado de cuenta en MercadoLibre.', type: 'error', instructions: ['Ingresá a tu cuenta de MercadoLibre.', 'Revisá tu estado de cuenta y pagos pendientes.', 'Regularizá la deuda para que se reactiven tus publicaciones.'] };
    }

    if (subStatuses.length > 0) {
      return { message: 'Presenta advertencias registradas.', action: 'Revisar los sub-estados en MercadoLibre.', type: 'warning', instructions: ['Ingresá a tu cuenta de MercadoLibre.', 'Revisá esta publicación y los mensajes de advertencia.', 'Corregí los problemas indicados.'] };
    }

    return null;
  }

  openResolveModal(product: any) {
    this.itemToResolve = product;
    this.resolveError = '';
    this.resolveSuccess = '';
    this.showResolveModal = true;
  }

  closeResolveModal() {
    this.showResolveModal = false;
    this.itemToResolve = null;
    this.resolveError = '';
    this.resolveSuccess = '';
  }

  loadLinkedProducts() {
    this.meliService.getLinkedLocalProducts().subscribe({
      next: (products) => {
        this.linkedLocalProducts = products;
      },
      error: (err) => {
        console.error('Error loading linked products', err);
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

  showUnlinkConfirmModal = false;
  showUnlinkSuccessModal = false;
  showSyncSuccessModal = false;

  unlinkSelected() {
    if (this.selectedLinkedProducts.size === 0) return;
    this.showUnlinkConfirmModal = true;
  }

  closeUnlinkConfirmModal() {
    this.showUnlinkConfirmModal = false;
  }

  confirmUnlink() {
    this.showUnlinkConfirmModal = false;
    this.isLoading = true;
    this.loadingMessage = 'Desvinculando productos...';

    this.meliService.unlinkProducts(Array.from(this.selectedLinkedProducts)).subscribe({
      next: () => {
        this.isLoading = false;
        this.selectedLinkedProducts.clear();
        this.loadLinkedProducts();

        this.loadLocalProducts();
        this.fetchProducts();
        this.showUnlinkSuccessModal = true;
      },
      error: (err) => {
        console.error('Error unlinking products', err);
        this.isLoading = false;
        this.errorMessage = 'Error al desvincular productos.';
      }
    });
  }

  closeUnlinkSuccessModal() {
    this.showUnlinkSuccessModal = false;
  }

  closeSyncSuccessModal() {
    this.showSyncSuccessModal = false;
  }

  toggleProductSelection(id: string) {
    if (this.selectedProducts.has(id)) {
      this.selectedProducts.delete(id);
    } else {
      this.selectedProducts.add(id);
    }
  }

  isProductSelected(id: string): boolean {
    return this.selectedProducts.has(id);
  }

  mappingCandidates: { mlProduct: any, selectedLocalId: number | null }[] = [];
  conflicts: any[] = [];
  showMappingModal = false;
  showConflictModal = false;

  syncSelected() {
    if (this.selectedProducts.size === 0) return;

    this.mappingCandidates = [];
    const selected = this.products.filter(p => this.selectedProducts.has(p.id));

    selected.forEach(p => {
      this.mappingCandidates.push({
        mlProduct: p,
        selectedLocalId: null
      });
    });

    this.showMappingModal = true;
  }

  cancelMapping() {
    this.showMappingModal = false;
    this.mappingCandidates = [];
  }

  confirmMapping() {
    this.isLoading = true;
    this.loadingMessage = 'Analizando diferencias...';
    this.showMappingModal = false;

    const requests = this.mappingCandidates.map(c => ({
      mlItemId: c.mlProduct.id,
      localProductId: c.selectedLocalId,
      createNew: !c.selectedLocalId
    }));

    this.meliService.previewBulkSync(requests).subscribe({
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

  closeConflictModal() {
    this.showConflictModal = false;
    this.conflicts = [];
  }

  resolveConflict(conflictIndex: number, diffIndex: number, resolution: string) {
    this.conflicts[conflictIndex].differences[diffIndex].resolution = resolution;
  }

  onManualMatch(previewIndex: number, localProductId: any) {
    const mlItemId = this.conflicts[previewIndex].mercadoLibreItem.id;
    this.isLoading = true;

    let localId: number | null = null;
    if (localProductId && localProductId !== 'null' && localProductId !== 'undefined') {
      localId = Number(localProductId);
    }

    const request = {
      mlItemId: mlItemId,
      localProductId: localId,
      createNew: !localId
    };

    this.meliService.previewBulkSync([request]).subscribe({
      next: (previews) => {
        if (previews && previews.length > 0) {
          this.conflicts[previewIndex] = previews[0];
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error("Error matching product manually", err);
        this.isLoading = false;
        this.errorMessage = "Error al vincular producto manualmente.";
      }
    });
  }

  confirmSync() {
    this.showConflictModal = false;
    this.isLoading = true;
    this.loadingMessage = 'Sincronizando productos...';

    this.meliService.executeSync(this.conflicts).subscribe({
      next: () => {
        this.isLoading = false;
        this.showSyncSuccessModal = true;
        this.conflicts = [];
        this.selectedProducts.clear();
        this.fetchProducts();
        this.loadLocalProducts();
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Sync failed', err);
        this.errorMessage = 'Error al sincronizar productos.';
      }
    });
  }

  private generateRandomString(length: number): string {
    const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
    let result = '';
    const values = new Uint32Array(length);
    crypto.getRandomValues(values);
    for (let i = 0; i < length; i++) {
      result += charset[values[i] % charset.length];
    }
    return result;
  }

  private async generateCodeChallenge(codeVerifier: string): Promise<string> {
    const encoder = new TextEncoder();
    const data = encoder.encode(codeVerifier);
    const digest = await crypto.subtle.digest('SHA-256', data);
    return btoa(String.fromCharCode(...new Uint8Array(digest)))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }

  fetchSales() {
    this.isLoading = true;
    this.loadingMessage = 'Cargando ventas...';
    this.http.get<MeliOrderSummary[]>(`${this.apiUrl}/orders`).subscribe({
      next: (orders) => {
        this.orders = orders || [];
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error fetching orders', err);
        this.isLoading = false;
        this.errorMessage = 'Error al cargar ventas.';
      }
    });
  }

  chatOpen = false;
  chatConversationId: number | null = null;
  chatSellerId: number | null = null;
  chatMessages: MercadoLibreMessage[] = [];
  chatNewMessage = '';
  chatLoading = false;
  isChatMinimized = false;
  statusChip(kind: 'order' | 'shipping' | 'claim', value?: string): string {
    const v = (value || '').toLowerCase();
    if (kind === 'order') {
      if (v.includes('paid')) return 'status-chip status-paid';
      if (v.includes('cancel')) return 'status-chip status-cancelled';
      if (v.includes('process')) return 'status-chip status-processing';
      if (v.includes('confirmed')) return 'status-chip status-confirmed';
      return 'status-chip';
    }
    if (kind === 'shipping') {
      if (v === 'delivered') return 'status-chip status-delivered';
      if (v === 'shipped') return 'status-chip status-shipped';
      if (v === 'ready_to_ship') return 'status-chip status-ready';
      if (v === 'not_delivered') return 'status-chip status-not-delivered';
      if (v === 'cancelled') return 'status-chip status-cancelled';
      return 'status-chip';
    }
    if (kind === 'claim') {
      if (v === 'opened') return 'status-chip status-claim-opened';
      if (v === 'closed') return 'status-chip status-claim-closed';
      return 'status-chip';
    }
    return 'status-chip';
  }

  getOrderStatusBorderClass(order: MeliOrderSummary): string {
    const status = (order.orderStatus || '').toLowerCase();
    if (status.includes('paid') || status.includes('confirmed')) return 'border-success';
    if (status.includes('cancel')) return 'border-danger';
    if (status.includes('process')) return 'border-warning';
    return 'border-neutral';
  }

  openChatForOrder(order: MeliOrderSummary) {
    if (!order.packId) return;
    console.log('[ML Chat] openChatForOrder click', { orderId: order.orderId, packId: order.packId });
    this.chatConversationId = order.packId;
    console.log('[ML Chat] opening chat bubble for packId', order.packId);
    this.chatOpen = true;
    this.isChatMinimized = false;
    this.saveChatState();
    this.chatLoading = true;
    this.meliService.ensureConversation(order.packId).subscribe({
      next: () => {
        console.log('[ML Chat] conversation ensured for packId', order.packId);
        this.meliService.getConversations().subscribe(cs => {
          const found = (cs || []).find(c => c.id === order.packId);
          this.chatSellerId = found ? found.sellerId : null;
        });
        this.meliService.getMessages(order.packId!, { limit: 10 }).subscribe({
          next: (msgs) => {
            this.chatMessages = msgs || [];
            console.log('[ML Chat] messages loaded', this.chatMessages.length);
            this.chatLoading = false;
            this.saveChatState();
          },
          error: () => {
            console.log('[ML Chat] error loading messages for packId', order.packId);
            this.chatLoading = false;
          }
        });
      },
      error: () => {
        console.log('[ML Chat] ensureConversation error for packId', order.packId);
        this.chatLoading = false;
      }
    });
    this.realtime.connect().then(() => {
      console.log('[ML Chat] realtime connected, subscribing to packId', order.packId);
      this.realtime.subscribeToConversation(order.packId!, (payload: any) => {
        if (Array.isArray(payload)) {
          const existing = new Set(this.chatMessages.map(m => m.id));
          const latest = this.chatMessages.length > 0 ? new Date(this.chatMessages[this.chatMessages.length - 1].dateCreated).getTime() : null;
          if (this.chatMessages.length === 0) {
            const lastTen = payload.slice(-10);
            this.chatMessages = lastTen.map((p: any) => ({
              id: p.id,
              fromUserId: p.fromUserId,
              toUserId: p.toUserId,
              text: p.text,
              status: p.status,
              dateCreated: p.dateCreated,
              dateRead: '',
              readByMe: false
            }));
          } else {
            for (const p of payload) {
              const ts = p.dateCreated ? new Date(p.dateCreated).getTime() : 0;
              if (!existing.has(p.id) && latest != null && ts > latest) {
                this.chatMessages.push({
                  id: p.id,
                  fromUserId: p.fromUserId,
                  toUserId: p.toUserId,
                  text: p.text,
                  status: p.status,
                  dateCreated: p.dateCreated,
                  dateRead: '',
                  readByMe: false
                });
              }
            }
          }
        } else if (payload && payload.type === 'sent' && this.chatConversationId) {
          this.meliService.getMessages(this.chatConversationId, { limit: 10 }).subscribe((data: MercadoLibreMessage[]) => {
            this.chatMessages = data;
          });
        }
      });
    }).catch(() => { });
  }

  onChatButton(order: MeliOrderSummary) {
    console.log('[ML Chat] chat button clicked', { orderId: order.orderId, packId: order.packId, activeTab: this.activeTab, step: this.step });
    if (order.packId) {
      this.openChatForOrder(order);
      return;
    }
    if (order.buyerId) {
      console.log('[ML Chat] intentando resolver conversación por buyerId', order.buyerId);
      this.meliService.getConversations().subscribe({
        next: (convs) => {
          const matches = (convs || []).filter(c => c.buyerId === order.buyerId);
          if (matches.length > 0) {
            const conv = matches[0];
            console.log('[ML Chat] conversación encontrada por buyerId', conv.id);
            this.chatConversationId = conv.id;
            this.chatOpen = true;
            this.chatSellerId = conv.sellerId;
            this.chatLoading = true;
            this.isChatMinimized = false;
            this.saveChatState();
            this.meliService.ensureConversation(conv.id).subscribe({
              next: () => {
                this.meliService.getMessages(conv.id, { limit: 10 }).subscribe({
                  next: (msgs) => {
                    this.chatMessages = msgs || [];
                    this.chatLoading = false;
                    this.saveChatState();
                  },
                  error: () => {
                    this.chatLoading = false;
                  }
                });
              },
              error: () => {
                this.chatLoading = false;
              }
            });
            this.realtime.connect().then(() => {
              this.realtime.subscribeToConversation(conv.id, (payload: any) => {
                if (Array.isArray(payload)) {
                  const existing = new Set(this.chatMessages.map(m => m.id));
                  const latest = this.chatMessages.length > 0 ? new Date(this.chatMessages[this.chatMessages.length - 1].dateCreated).getTime() : null;
                  if (this.chatMessages.length === 0) {
                    const lastTen = payload.slice(-10);
                    this.chatMessages = lastTen.map((p: any) => ({
                      id: p.id,
                      fromUserId: p.fromUserId,
                      toUserId: p.toUserId,
                      text: p.text,
                      status: p.status,
                      dateCreated: p.dateCreated,
                      dateRead: '',
                      readByMe: false
                    }));
                  } else {
                    for (const p of payload) {
                      const ts = p.dateCreated ? new Date(p.dateCreated).getTime() : 0;
                      if (!existing.has(p.id) && latest != null && ts > latest) {
                        this.chatMessages.push({
                          id: p.id,
                          fromUserId: p.fromUserId,
                          toUserId: p.toUserId,
                          text: p.text,
                          status: p.status,
                          dateCreated: p.dateCreated,
                          dateRead: '',
                          readByMe: false
                        });
                      }
                    }
                  }
                  this.saveChatState();
                } else if (payload && payload.type === 'sent' && this.chatConversationId) {
                  this.meliService.getMessages(this.chatConversationId, { limit: 10 }).subscribe((data: MercadoLibreMessage[]) => {
                    this.chatMessages = data;
                    this.saveChatState();
                  });
                }
              });
            }).catch(() => { });
          } else {
            console.log('[ML Chat] no se encontró conversación por buyerId');
            this.errorMessage = 'No se encontró conversación para este comprador.';
          }
        },
        error: () => {
          console.log('[ML Chat] error obteniendo conversaciones');
          this.errorMessage = 'No se pudo obtener conversaciones.';
        }
      });
      return;
    }
    console.log('[ML Chat] chat no disponible: faltan packId y buyerId');
    this.errorMessage = 'Chat no disponible: faltan datos de conversación.';
  }

  toggleMinimize() {
    this.isChatMinimized = !this.isChatMinimized;
    this.saveChatState();
    if (!this.isChatMinimized) {
      try {
        window.dispatchEvent(new Event('mlChatStateChanged'));
      } catch { }
    }
  }

  isMyMessage(m: MercadoLibreMessage): boolean {
    return this.chatSellerId != null && m.fromUserId === this.chatSellerId;
  }

  saveChatState() {
    const state = {
      open: this.chatOpen,
      minimized: this.isChatMinimized,
      conversationId: this.chatConversationId,
      sellerId: this.chatSellerId
    };
    try {
      localStorage.setItem('mlChatState', JSON.stringify(state));
      try {
        window.dispatchEvent(new Event('mlChatStateChanged'));
      } catch { }
    } catch { }
  }

  tryRestoreChat() {
    let raw = null;
    try {
      raw = localStorage.getItem('mlChatState');
    } catch { }
    if (!raw) return;
    let state: any = null;
    try {
      state = JSON.parse(raw);
    } catch { }
    if (!state || !state.open || !state.conversationId) return;
    this.chatConversationId = state.conversationId;
    this.chatSellerId = state.sellerId || null;
    this.chatOpen = true;
    this.isChatMinimized = !!state.minimized;
    this.chatLoading = true;
    this.meliService.ensureConversation(this.chatConversationId!).subscribe({
      next: () => {
        this.meliService.getMessages(this.chatConversationId!, { limit: 10 }).subscribe({
          next: (msgs) => {
            this.chatMessages = msgs || [];
            this.chatLoading = false;
          },
          error: () => {
            this.chatLoading = false;
          }
        });
      },
      error: () => {
        this.chatLoading = false;
      }
    });
    this.realtime.connect().then(() => {
      this.realtime.subscribeToConversation(this.chatConversationId!, (payload: any) => {
        if (Array.isArray(payload)) {
          const existing = new Set(this.chatMessages.map(m => m.id));
          const latest = this.chatMessages.length > 0 ? new Date(this.chatMessages[this.chatMessages.length - 1].dateCreated).getTime() : null;
          if (this.chatMessages.length === 0) {
            const lastTen = payload.slice(-10);
            this.chatMessages = lastTen.map((p: any) => ({
              id: p.id,
              fromUserId: p.fromUserId,
              toUserId: p.toUserId,
              text: p.text,
              status: p.status,
              dateCreated: p.dateCreated,
              dateRead: '',
              readByMe: false
            }));
          } else {
            for (const p of payload) {
              const ts = p.dateCreated ? new Date(p.dateCreated).getTime() : 0;
              if (!existing.has(p.id) && latest != null && ts > latest) {
                this.chatMessages.push({
                  id: p.id,
                  fromUserId: p.fromUserId,
                  toUserId: p.toUserId,
                  text: p.text,
                  status: p.status,
                  dateCreated: p.dateCreated,
                  dateRead: '',
                  readByMe: false
                });
              }
            }
          }
        } else if (payload && payload.type === 'sent' && this.chatConversationId) {
          this.meliService.getMessages(this.chatConversationId, { limit: 10 }).subscribe((data: MercadoLibreMessage[]) => {
            this.chatMessages = data;
          });
        }
      });
    }).catch(() => { });
  }

  closeChat() {
    this.chatOpen = false;
    this.chatConversationId = null;
    this.chatMessages = [];
    this.chatNewMessage = '';
    this.realtime.disconnect();
  }

  sendChatMessage() {
    if (!this.chatConversationId || !this.chatNewMessage.trim()) return;
    this.meliService.sendMessage(this.chatConversationId, this.chatNewMessage).subscribe(() => {
      this.chatNewMessage = '';
    });
  }
}
