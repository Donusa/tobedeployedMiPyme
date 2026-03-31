import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { MainLayoutComponent } from './components/main-layout/main-layout.component';
import { PlaceholderComponent } from './components/placeholder/placeholder.component';
import { OperationalMetricsComponent } from './components/metricas/operational-metrics/operational-metrics.component';
import { MetricasComponent } from './components/metricas/metricas.component';
import { ProfileComponent } from './components/profile/profile.component';
import { EmailComponent } from './components/profile/email/email.component';
import { PasswordComponent } from './components/profile/password/password.component';
import { AccessHistoryComponent } from './components/profile/security/access-history.component';
import { ActiveSessionsComponent } from './components/profile/security/active-sessions.component';
import { TwoFactorComponent } from './components/profile/security/two-factor/two-factor.component';
import { CompanyDataComponent } from './components/settings/company-data/company-data.component';
import { PreferencesComponent } from './components/profile/preferences/preferences.component';
import { TiendaNubeSyncWizardComponent } from './components/tienda-nube/tienda-nube-sync-wizard/tienda-nube-sync-wizard.component';
import { MercadoLibreSyncWizardComponent } from './components/mercado-libre/mercado-libre-sync-wizard/mercado-libre-sync-wizard.component';
import { MensajeriaComponent } from './components/mensajeria/mensajeria.component';
import { MensajeriaWhatsappComponent } from './components/mensajeria/whatsapp/mensajeria-whatsapp.component';
import { MensajeriaMercadolibreComponent } from './components/mensajeria/mercadolibre/mensajeria-mercadolibre.component';
import { MensajeriaInstagramComponent } from './components/mensajeria/instagram/mensajeria-instagram.component';
import { IntegrationConfigComponent } from './components/settings/integration-config/integration-config.component';
import { authGuard } from './guards/auth.guard';
import { permissionGuard } from './guards/permission.guard';
import { planGuard } from './guards/plan.guard';
import { LandingComponent } from './components/landing/landing.component';

import { SystemStatusComponent } from './components/settings/system-status/system-status.component';
import { SupportComponent } from './components/settings/support/support.component';
import { FaqComponent } from './components/settings/faq/faq.component';
import { ChangelogComponent } from './components/settings/changelog/changelog.component';
import { ArcaCertWizardComponent } from './components/arca/arca-cert-wizard/arca-cert-wizard.component';
import { ArcaConfigComponent } from './components/arca/arca-config/arca-config.component';
import { FacturacionComponent } from './components/facturacion/facturacion.component';
import { FacturaDetalleComponent } from './components/facturacion/factura-detalle/factura-detalle.component';
import { HomeDashboardComponent } from './components/home-dashboard/home-dashboard.component';
import { BillingPlanComponent } from './components/settings/billing/billing-plan/billing-plan.component';
import { BillingPaymentComponent } from './components/settings/billing/billing-payment/billing-payment.component';
import { BillingInvoicesComponent } from './components/settings/billing/billing-invoices/billing-invoices.component';
import { BillingDataComponent } from './components/settings/billing/billing-data/billing-data.component';
import { CheckoutResultComponent } from './components/settings/billing/checkout-result/checkout-result.component';
import { PlanRequiredComponent } from './components/plan-required/plan-required.component';
import { PrivacyDataComponent } from './components/settings/privacy-data/privacy-data.component';
import { DeleteAccountComponent } from './components/settings/delete-account/delete-account.component';
import { AuditComponent } from './components/settings/audit/audit.component';
import { MetricasAvanzadasComponent } from './components/metricas/advanced-metrics/metricas-avanzadas.component';
import { AuditAdvancedComponent } from './components/settings/audit/audit-advanced/audit-advanced.component';

const routes: Routes = [
  { path: '', pathMatch: 'full', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'home', pathMatch: 'full' },
      { path: 'home', component: HomeDashboardComponent, canActivate: [permissionGuard], data: { permission: 'ventas' } },
      {
        path: 'metricas',
        canActivate: [permissionGuard, planGuard],
        data: { permission: 'metricas', requiredPlan: 'pro' },
        children: [
          { path: '', redirectTo: 'alertas', pathMatch: 'full' },
          { path: 'alertas', component: MetricasComponent },
          { path: 'operativas', component: OperationalMetricsComponent },
          {
            path: 'avanzadas',
            component: MetricasAvanzadasComponent,
            canActivate: [planGuard],
            data: { requiredPlan: 'enterprise' }
          }
        ]
      },
      { path: 'perfil', component: ProfileComponent },
      { path: 'perfil/email', component: EmailComponent },
      { path: 'perfil/password', component: PasswordComponent },
      { path: 'perfil/access-history', component: AccessHistoryComponent },
      { path: 'perfil/sessions', component: ActiveSessionsComponent },
      { path: 'perfil/2fa', component: TwoFactorComponent },
      {
        path: 'perfil/company-data',
        component: CompanyDataComponent
      },
      {
        path: 'configuracion/integraciones/mercadolibre',
        component: IntegrationConfigComponent,
        data: { type: 'mercadolibre', permission: 'mercadolibre', requiredPlan: 'pro' },
        canActivate: [permissionGuard, planGuard]
      },
      {
        path: 'configuracion/integraciones/tiendanube',
        component: IntegrationConfigComponent,
        data: { type: 'tiendanube', permission: 'tiendanube', requiredPlan: 'pro' },
        canActivate: [permissionGuard, planGuard]
      },
      {
        path: 'configuracion/integraciones/arca',
        component: ArcaConfigComponent,
        data: { permission: 'arca', requiredPlan: 'pro' },
        canActivate: [permissionGuard, planGuard]
      },
      {
        path: 'configuracion/integraciones/whatsapp',
        component: IntegrationConfigComponent,
        data: { type: 'whatsapp', permission: 'mensajeria', requiredPlan: 'pro' },
        canActivate: [permissionGuard, planGuard]
      },
      { path: 'perfil/preferencias', component: PreferencesComponent },
      { path: 'configuracion/ayuda/status', component: SystemStatusComponent },
      { path: 'configuracion/ayuda/soporte', component: SupportComponent },
      { path: 'configuracion/ayuda/centro-ayuda', component: FaqComponent },
      { path: 'configuracion/ayuda/novedades', component: ChangelogComponent },
      {
        path: 'stock',
        loadChildren: () => import('./stock/stock.module').then(m => m.StockModule),
        canActivate: [permissionGuard],
        data: { permission: 'stock' }
      },
      {
        path: 'ofertas',
        loadChildren: () => import('./offers/offers.module').then(m => m.OffersModule),
        canActivate: [permissionGuard, planGuard],
        data: { permission: 'stock', requiredPlan: 'pro' }
      },
      {
        path: 'ventas',
        loadChildren: () => import('./sales/sales.module')
          .then(m => {
            console.log('SalesModule loaded successfully');
            return m.SalesModule;
          })
          .catch(err => {
            console.error('Error loading SalesModule:', err);
            throw err;
          }),
        canActivate: [permissionGuard],
        data: { permission: 'ventas' }
      },
      {
        path: 'pedidos',
        loadChildren: () => import('./orders/orders.module').then(m => m.OrdersModule),
        canActivate: [permissionGuard, planGuard],
        data: { permission: 'ventas', requiredPlan: 'pro' }
      },
      {
        path: 'empleados',
        loadChildren: () => import('./employees/employees.module').then(m => m.EmployeesModule),
        canActivate: [permissionGuard],
        data: { permission: 'empleados' }
      },
      {
        path: 'caja',
        loadChildren: () => import('./caja/caja.module').then(m => m.CajaModule),
        canActivate: [permissionGuard],
        data: { permission: 'caja' }
      },
      {
        path: 'mensajeria',
        component: MensajeriaComponent,
        canActivate: [permissionGuard, planGuard],
        data: { permission: 'mensajeria', requiredPlan: 'pro' },
        children: [
          { path: '', redirectTo: 'whatsapp', pathMatch: 'full' },
          { path: 'whatsapp', component: MensajeriaWhatsappComponent },
          { path: 'mercadolibre', component: MensajeriaMercadolibreComponent },
          { path: 'instagram', component: MensajeriaInstagramComponent }
        ]
      },
      {
        path: 'mercadolibre',
        component: MercadoLibreSyncWizardComponent,
        canActivate: [permissionGuard, planGuard],
        data: { permission: 'mercadolibre', requiredPlan: 'pro' }
      },
      {
        path: 'tiendanube',
        component: TiendaNubeSyncWizardComponent,
        canActivate: [permissionGuard, planGuard],
        data: { permission: 'tiendanube', requiredPlan: 'pro' }
      },
      {
        path: 'arca',
        component: ArcaCertWizardComponent,
        canActivate: [permissionGuard, planGuard],
        data: { permission: 'arca', requiredPlan: 'pro' }
      },
      {
        path: 'facturacion',
        component: FacturacionComponent,
        canActivate: [permissionGuard, planGuard],
        data: { permission: 'ventas', requiredPlan: 'pro' }
      },
      {
        path: 'facturacion/nueva',
        component: FacturaDetalleComponent,
        canActivate: [permissionGuard, planGuard],
        data: { permission: 'ventas', requiredPlan: 'pro' }
      },
      { path: 'configuracion/facturacion/plan', component: BillingPlanComponent },
      { path: 'configuracion/facturacion/payment', component: BillingPaymentComponent },
      { path: 'configuracion/facturacion/invoices', component: BillingInvoicesComponent },
      { path: 'configuracion/facturacion/billing-data', component: BillingDataComponent },
      { path: 'configuracion/facturacion/checkout-result', component: CheckoutResultComponent },
      { path: 'configuracion/datos-privacidad', component: PrivacyDataComponent },
      { path: 'configuracion/borrar-cuenta', component: DeleteAccountComponent },
      { path: 'configuracion/auditoria', component: AuditComponent },
      {
        path: 'configuracion/auditoria/avanzada',
        component: AuditAdvancedComponent,
        canActivate: [planGuard],
        data: { requiredPlan: 'enterprise' }
      },
      { path: 'plan-required', component: PlanRequiredComponent }
    ]
  },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
