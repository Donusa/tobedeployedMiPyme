import { Component, HostBinding, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { trigger, transition, style, animate, query, stagger } from '@angular/animations';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { PlanService, PlanType, PlanStatus } from '../../services/plan.service';
import { ThemeService, Theme } from '../../services/theme.service';
import { Notification, NotificationType } from '../../models/notification.model';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { MercadoLibreService, MercadoLibreMessage } from '../../services/mercadolibre.service';
import { MeliRealtimeService } from '../../services/meli-realtime.service';

@Component({
  selector: 'app-main-layout',
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.css'],
  animations: [
    trigger('sidebarContentAnim', [
      transition(':enter', [
        query('.logo, .brand-name, .nav-item, .sidebar-footer', [
          style({ opacity: 0, transform: 'translateX(-15px)' }),
          stagger(50, [
            animate('500ms 200ms cubic-bezier(0.35, 0, 0.25, 1)', style({ opacity: 1, transform: 'translateX(0)' }))
          ])
        ], { optional: true })
      ])
    ]),
    trigger('mainContentAnim', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(15px)' }),
        animate('600ms 400ms cubic-bezier(0.35, 0, 0.25, 1)', style({ opacity: 1, transform: 'translateY(0)' }))
      ])
    ]),
    trigger('slideInOut', [
      transition(':enter', [
        style({ transform: 'translateY(100%)', opacity: 0 }),
        animate('300ms cubic-bezier(0.2, 0, 0, 1)', style({ transform: 'translateY(0)', opacity: 1 }))
      ]),
      transition(':leave', [
        animate('200ms ease-in', style({ transform: 'translateY(100%)', opacity: 0 }))
      ])
    ])
  ]
})
export class MainLayoutComponent implements OnInit, OnDestroy {

  isSidebarOpen = false;
  isSidebarCollapsed = false;
  settingsFromMobile = false;
  isUserMenuOpen = false;
  isNotificationOpen = false;
  isSettingsView = false;
  currentTitle = 'Vista Principal';
  companyName = 'MiPyme';
  userName = 'Usuario';
  notifications: Notification[] = [];
  unreadCount = 0;
  private notifSub?: Subscription;
  private unreadSub?: Subscription;
  private planStatusSub?: Subscription;


  currentPlanStatus: PlanStatus = 'active';
  currentPlan: PlanType = 'base';
  subscriptionBannerDismissed = false;

  chatOpen = false;
  isChatMinimized = false;
  chatConversationId: number | null = null;
  chatSellerId: number | null = null;
  chatMessages: MercadoLibreMessage[] = [];
  chatLoading = false;
  chatNewMessage = '';
  @ViewChild('mlChatBody') mlChatBody!: ElementRef<HTMLDivElement>;
  loadingOlder = false;
  oldestLoadedDate: string | null = null;
  allowLoadOlder = false;
  noMoreOlder = false;

  private allMenuItems = [
    { label: 'Home', icon: 'bx bx-home', route: '/home', permission: 'ventas' },
    {
      label: 'Métricas',
      icon: 'bx bx-bar-chart-alt-2',
      route: '/metricas',
      permission: 'metricas',
      requiredPlan: 'enterprise' as PlanType,
      expanded: false,
      children: [
        { label: 'Alertas', icon: 'bx bx-bell', route: '/metricas/alertas', permission: 'metricas' },
        { label: 'Operativas', icon: 'bx bx-task', route: '/metricas/operativas', permission: 'metricas' }
      ]
    },
    { label: 'Stock', icon: 'bx bx-box', route: '/stock', permission: 'stock' },
    { label: 'Ofertas', icon: 'bx bx-purchase-tag', route: '/ofertas', permission: 'stock', requiredPlan: 'pro' as PlanType },
    { label: 'Ventas', icon: 'bx bx-cart', route: '/ventas', permission: 'ventas' },
    { label: 'Pedidos', icon: 'bx bx-package', route: '/pedidos', permission: 'ventas', requiredPlan: 'pro' as PlanType },
    { label: 'Empleados', icon: 'bx bx-user-pin', route: '/empleados', permission: 'empleados' },
    { label: 'Caja', icon: 'bx bx-calculator', route: '/caja', permission: 'caja' },
    { label: 'Mensajería', icon: 'bx bx-message-rounded-dots', route: '/mensajeria', permission: 'mensajeria', requiredPlan: 'pro' as PlanType },
    { label: 'MercadoLibre', icon: 'bx bx-shopping-bag', route: '/mercadolibre', permission: 'mercadolibre', requiredPlan: 'pro' as PlanType },
    { label: 'TiendaNube', icon: 'bx bx-cloud', route: '/tiendanube', permission: 'tiendanube', requiredPlan: 'pro' as PlanType },
    { label: 'ARCA', icon: 'bx bx-file', route: '/arca', permission: 'arca', requiredPlan: 'enterprise' as PlanType }
  ];

  get menuItems() {
    return this.allMenuItems.filter(item => {

      if (item.permission === 'always') return true;

      try {
        return this.authService.hasPermission(item.permission);
      } catch (error) {
        console.warn(`Error checking permission for ${item.label}:`, error);
        return false;
      }
    });
  }

  get debugInfo() {
    try {
      return {
        role: this.authService.getRole(),
        permissions: this.authService.getPermissions(),
        rawToken: this.authService.getToken() ? 'Present' : 'Missing'
      };
    } catch (e) {
      return { role: 'Error', permissions: [], rawToken: 'Error' };
    }
  }

  settingsMenu = [
    {
      category: 'Cuenta',
      items: [
        { label: 'Perfil', icon: 'bx bx-user', action: 'profile', route: '/perfil' },
        { label: 'Email', icon: 'bx bx-envelope', action: 'email', route: '/perfil/email' },
        { label: 'Contraseña', icon: 'bx bx-lock-alt', action: 'password', route: '/perfil/password' },
        { label: 'Preferencias', icon: 'bx bx-slider-alt', action: 'preferences', route: '/perfil/preferencias' }
      ]
    },
    {
      category: 'Seguridad',
      items: [
        { label: '2FA/MFA', icon: 'bx bx-shield-quarter', action: '2fa' },
        { label: 'Sesiones activas', icon: 'bx bx-devices', action: 'sessions', route: '/perfil/sessions' },
        { label: 'Historial de accesos', icon: 'bx bx-history', action: 'access-history', route: '/perfil/access-history' }
      ]
    },
    {
      category: 'Empresa / Tenant',
      items: [
        { label: 'Datos de la empresa', icon: 'bx bx-building', action: 'company-data', route: '/perfil/company-data' }
      ]
    },
    {
      category: 'Facturación / Plan',
      items: [
        { label: 'Mi plan y suscripción', icon: 'bx bx-credit-card-front', action: 'plan', route: '/configuracion/facturacion/plan' },
        { label: 'Método de pago', icon: 'bx bx-credit-card', action: 'payment', route: '/configuracion/facturacion/payment' },
        { label: 'Facturas', icon: 'bx bx-receipt', action: 'invoices', route: '/configuracion/facturacion/invoices' },
        { label: 'Datos de facturación', icon: 'bx bx-file', action: 'billing-data', route: '/configuracion/facturacion/billing-data' }
      ]
    },
    {
      category: 'Integraciones',
      items: [
        { label: 'Mercado Libre', icon: 'bx bx-shopping-bag', action: 'meli', route: '/configuracion/integraciones/mercadolibre' },
        { label: 'Tienda Nube', icon: 'bx bx-cloud', action: 'tiendanube', route: '/configuracion/integraciones/tiendanube' },
        { label: 'ARCA/AFIP', icon: 'bx bx-file', action: 'afip', route: '/configuracion/integraciones/arca' },
        { label: 'WhatsApp', icon: 'bx bxl-whatsapp', action: 'whatsapp', route: '/configuracion/integraciones/whatsapp' }
      ]
    },
    {
      category: 'Datos y privacidad',
      items: [
        { label: 'Exportar datos', icon: 'bx bx-download', action: 'export-data', route: '/configuracion/datos-privacidad' },
        { label: 'Borrar cuenta', icon: 'bx bx-trash', action: 'delete-account', route: '/configuracion/borrar-cuenta' },
        { label: 'Auditoría', icon: 'bx bx-list-check', action: 'audit', route: '/configuracion/auditoria' }
      ]
    },

    {
      category: 'Ayuda',
      items: [
        { label: 'Centro de ayuda', icon: 'bx bx-help-circle', action: 'help-center', route: '/configuracion/ayuda/centro-ayuda' },
        { label: 'Soporte', icon: 'bx bx-support', action: 'support', route: '/configuracion/ayuda/soporte' },
        { label: 'Estado del sistema', icon: 'bx bx-pulse', action: 'status', route: '/configuracion/ayuda/status' },
        { label: 'Novedades', icon: 'bx bx-news', action: 'changelog', route: '/configuracion/ayuda/novedades' }
      ]
    }
  ];

  constructor(
    private router: Router,
    private authService: AuthService,
    private notificationService: NotificationService,
    private meliService: MercadoLibreService,
    private realtime: MeliRealtimeService,
    public planService: PlanService,
    public themeService: ThemeService
  ) { }

  isPlanLocked(item: any): boolean {
    if (!item.requiredPlan) return false;
    return !this.planService.hasAccess(item.requiredPlan);
  }

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }


  private touchStartX = 0;

  onBackdropTouchStart(event: TouchEvent): void {
    this.touchStartX = event.touches[0]?.clientX ?? 0;
  }

  onBackdropTouchEnd(event: TouchEvent): void {
    const endX = event.changedTouches[0]?.clientX ?? 0;

    if (this.touchStartX - endX > 50) {
      this.isSidebarOpen = false;
    }
  }

  toggleSidebarCollapse() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  toggleSubmenu(item: any) {
    item.expanded = !item.expanded;
  }

  toggleUserMenu() {
    this.isUserMenuOpen = !this.isUserMenuOpen;
  }

  toggleNotificationMenu() {
    this.isNotificationOpen = !this.isNotificationOpen;
  }

  onNotificationClick(n: Notification) {
    if (!n.read) {
      this.notificationService.markAsRead(n.id).subscribe();
    }
    this.router.navigateByUrl(n.url);
    this.isNotificationOpen = false;
  }

  markAllRead() {
    this.notificationService.markAllAsRead().subscribe();
  }

  getNotificationIcon(type: NotificationType): string {
    switch (type) {
      case 'MESSAGE': return 'bx bx-message-rounded-dots';
      case 'STOCK_ALERT': return 'bx bx-package';
      case 'UNINVOICED_SALE': return 'bx bx-receipt';
      case 'ML_WEBHOOK': return 'bx bx-shopping-bag';
      case 'TN_WEBHOOK': return 'bx bx-cloud';
      default: return 'bx bx-bell';
    }
  }

  getNotificationIconColor(type: NotificationType): string {
    switch (type) {
      case 'MESSAGE': return '#6366f1';
      case 'STOCK_ALERT': return '#ef4444';
      case 'UNINVOICED_SALE': return '#f59e0b';
      case 'ML_WEBHOOK': return '#ffe600';
      case 'TN_WEBHOOK': return '#00b4d8';
      default: return '#6b7280';
    }
  }

  getTimeAgo(dateStr: string): string {
    const now = new Date();
    const date = new Date(dateStr);
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 1) return 'Ahora';
    if (diffMins < 60) return diffMins + ' min';
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return diffHours + 'h';
    const diffDays = Math.floor(diffHours / 24);
    return diffDays + 'd';
  }

  goToProfile() {
    this.isUserMenuOpen = false;
    this.router.navigate(['/perfil']);
  }

  goToSubscription() {
    this.isUserMenuOpen = false;
    this.router.navigate(['/configuracion/facturacion/plan']);
  }

  goToSettings() {
    this.settingsFromMobile = false;
    this.isSettingsView = true;

  }

  openSettingsMobile() {
    this.settingsFromMobile = true;
    this.isSettingsView = true;
    this.isSidebarOpen = true;
  }

  openMainMenuMobile() {
    if (this.isSettingsView) {

      this.isSettingsView = false;
      this.settingsFromMobile = false;
    } else {
      this.isSidebarOpen = !this.isSidebarOpen;
    }
  }

  backToMainMenu() {
    this.isSettingsView = false;
    this.settingsFromMobile = false;
  }

  onSettingClick(action: string) {
    if (action === 'profile') {
      this.router.navigate(['/perfil']);
    } else if (action === 'email') {
      this.router.navigate(['/perfil/email']);
    } else if (action === 'password') {
      this.router.navigate(['/perfil/password']);
    } else if (action === 'access-history') {
      this.router.navigate(['/perfil/access-history']);
    } else if (action === 'sessions') {
      this.router.navigate(['/perfil/sessions']);
    } else if (action === '2fa') {
      this.router.navigate(['/perfil/2fa']);
    } else if (action === 'company-data') {
      this.router.navigate(['/perfil/company-data']);
    } else if (action === 'preferences') {
      this.router.navigate(['/perfil/preferencias']);
    }
    console.log('Setting clicked:', action);

  }

  onMenuClick(item: any) {
    console.log('Menu item clicked:', item);
  }

  ngOnInit(): void {

    this.planService.syncFromBackend(true);

    const storedName = this.authService.getCompanyName();
    if (storedName) {
      this.companyName = storedName;
    }
    const storedUser = this.authService.getUserName();
    if (storedUser) {
      this.userName = storedUser;
    }

    this.updateTitle();
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.updateTitle();
      this.tryRestoreChat();
      this.isSidebarOpen = false;
      this.isSettingsView = false;
      this.settingsFromMobile = false;
    });


    this.notifSub = this.notificationService.notifications$.subscribe(data => {
      this.notifications = data;
    });
    this.unreadSub = this.notificationService.unreadCount$.subscribe(count => {
      this.unreadCount = count;
    });
    this.notificationService.startPolling();


    this.planStatusSub = this.planService.planStatus$.subscribe(status => {
      this.currentPlanStatus = status;
      this.subscriptionBannerDismissed = false;
    });
    this.planService.currentPlan$.subscribe(plan => {
      this.currentPlan = plan;
    });

    this.tryRestoreChat();
    try {
      window.addEventListener('mlChatStateChanged', () => this.tryRestoreChat());
    } catch { }
  }

  ngOnDestroy(): void {
    this.notifSub?.unsubscribe();
    this.unreadSub?.unsubscribe();
    this.planStatusSub?.unsubscribe();
  }

  updateTitle() {
    const currentRoute = this.router.url;

    if (currentRoute.startsWith('/perfil') || currentRoute.startsWith('/configuracion')) {
      this.currentTitle = 'Configuración';
      return;
    }


    let menuItem = this.menuItems.find(item => currentRoute.startsWith(item.route));


    if (menuItem && menuItem.children) {
      const childItem = menuItem.children.find((child: any) => currentRoute.startsWith(child.route));
      if (childItem) {




      }
    }


    let activeLabel = 'Vista Principal';

    for (const item of this.menuItems) {
      if (item.children) {
        const child = item.children.find((c: any) => currentRoute.startsWith(c.route));
        if (child) {
          activeLabel = child.label;
          item.expanded = true;
          break;
        }
      }
      if (currentRoute.startsWith(item.route)) {
        activeLabel = item.label;



      }
    }

    if (activeLabel !== 'Vista Principal') {
      this.currentTitle = activeLabel;
    } else {
      this.currentTitle = 'Vista Principal';
    }
  }

  logout() {
    this.authService.logout();

    this.router.navigate(['/login']);
  }


  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  get isDarkTheme(): boolean {
    return this.themeService.getTheme() === 'dark';
  }


  get showSubscriptionBanner(): boolean {
    if (this.subscriptionBannerDismissed) return false;
    return ['trial', 'pending_payment', 'past_due', 'suspended', 'blocked'].includes(this.currentPlanStatus);
  }

  get subscriptionBannerClass(): string {
    switch (this.currentPlanStatus) {
      case 'trial': return 'banner-info';
      case 'past_due': return 'banner-warning';
      case 'pending_payment': return 'banner-warning';
      case 'suspended': return 'banner-danger';
      case 'blocked': return 'banner-danger';
      default: return 'banner-info';
    }
  }

  get subscriptionBannerMessage(): string {
    switch (this.currentPlanStatus) {
      case 'trial': return `Estás en el período de prueba gratuita de 28 días del plan Pro.`;
      case 'pending_payment': return `Tu cuenta está pendiente de pago. Completá el pago para acceder a todas las funciones.`;
      case 'past_due': return `Tu suscripción tiene un pago vencido. Regularizá el pago para continuar operando.`;
      case 'suspended': return `Tu suscripción está suspendida. Reactivala para continuar operando.`;
      case 'blocked': return `Tu cuenta fue bloqueada. Contactá a soporte para resolverlo.`;
      default: return '';
    }
  }

  get subscriptionBannerAction(): { label: string; route: string } | null {
    switch (this.currentPlanStatus) {
      case 'trial': return { label: 'Ver plan', route: '/configuracion/facturacion/plan' };
      case 'pending_payment': return { label: 'Completar pago', route: '/configuracion/facturacion/plan' };
      case 'past_due': return { label: 'Regularizar', route: '/configuracion/facturacion/plan' };
      case 'suspended': return { label: 'Reactivar', route: '/configuracion/facturacion/plan' };
      default: return null;
    }
  }

  dismissSubscriptionBanner(): void {
    this.subscriptionBannerDismissed = true;
  }

  toggleChatHeader() {
    this.isChatMinimized = !this.isChatMinimized;
    this.saveChatState();
    if (!this.isChatMinimized) {
      this.allowLoadOlder = false;
      this.noMoreOlder = false;
      setTimeout(() => {
        this.scrollChatToBottom();
        setTimeout(() => this.allowLoadOlder = true, 180);
      }, 0);
    }
  }

  closeChat() {
    this.chatOpen = false;
    this.chatConversationId = null;
    this.chatMessages = [];
    this.chatNewMessage = '';
    this.realtime.disconnect();
    this.saveChatState();
  }

  sendChatMessage() {
    if (!this.chatConversationId || !this.chatNewMessage.trim()) return;
    this.meliService.sendMessage(this.chatConversationId, this.chatNewMessage).subscribe(() => {
      this.chatNewMessage = '';
      setTimeout(() => this.scrollChatToBottom(), 0);
    });
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
    this.allowLoadOlder = false;
    this.noMoreOlder = false;
    this.meliService.ensureConversation(this.chatConversationId!).subscribe({
      next: () => {
        this.meliService.getMessages(this.chatConversationId!, { limit: 10 }).subscribe({
          next: (msgs) => {
            this.chatMessages = msgs || [];
            this.chatLoading = false;
            if (this.chatMessages.length > 0) {
              this.oldestLoadedDate = this.chatMessages[0].dateCreated;
            }
            setTimeout(() => {
              this.scrollChatToBottom();
              setTimeout(() => this.allowLoadOlder = true, 180);
            }, 0);
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
          setTimeout(() => this.scrollChatToBottom(), 0);
        } else if (payload && payload.type === 'sent' && this.chatConversationId) {
          this.allowLoadOlder = false;
          this.noMoreOlder = false;
          this.meliService.getMessages(this.chatConversationId, { limit: 10 }).subscribe((data: MercadoLibreMessage[]) => {
            this.chatMessages = data;
            if (this.chatMessages.length > 0) {
              this.oldestLoadedDate = this.chatMessages[0].dateCreated;
            }
            setTimeout(() => {
              this.scrollChatToBottom();
              setTimeout(() => this.allowLoadOlder = true, 180);
            }, 0);
          });
        }
      });
    }).catch(() => { });
  }

  private scrollChatToBottom() {
    try {
      const el = this.mlChatBody?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    } catch { }
  }

  onChatScroll() {
    const el = this.mlChatBody?.nativeElement;
    if (!el || this.loadingOlder || this.isChatMinimized || !this.allowLoadOlder || this.noMoreOlder) return;
    if (el.scrollTop <= 12) {
      if (!this.chatConversationId) return;
      this.loadingOlder = true;
      const prevHeight = el.scrollHeight;
      const before = this.oldestLoadedDate || new Date().toISOString();
      this.meliService.getMessages(this.chatConversationId, { limit: 10, before }).subscribe({
        next: (older) => {
          if (older && older.length > 0) {
            this.chatMessages = [...older, ...this.chatMessages];
            this.oldestLoadedDate = this.chatMessages[0].dateCreated;
            setTimeout(() => {
              const newHeight = el.scrollHeight;
              el.scrollTop = newHeight - prevHeight;
              this.loadingOlder = false;
            }, 0);
          } else {
            this.loadingOlder = false;
            this.noMoreOlder = true;
          }
        },
        error: () => {
          this.loadingOlder = false;
        }
      });
    }
  }
}
