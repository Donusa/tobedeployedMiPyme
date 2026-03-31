import { NgModule, APP_INITIALIZER } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { firstValueFrom, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './components/login/login.component';
import { MainLayoutComponent } from './components/main-layout/main-layout.component';
import { PlaceholderComponent } from './components/placeholder/placeholder.component';
import { ProfileComponent } from './components/profile/profile.component';
import { EmailComponent } from './components/profile/email/email.component';
import { PasswordComponent } from './components/profile/password/password.component';
import { AccessHistoryComponent } from './components/profile/security/access-history.component';
import { ActiveSessionsComponent } from './components/profile/security/active-sessions.component';
import { CompanyDataComponent } from './components/settings/company-data/company-data.component';
import { PreferencesComponent } from './components/profile/preferences/preferences.component';
import { MetricasComponent } from './components/metricas/metricas.component';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { SharedModule } from './shared/shared.module';
import { AuthService } from './services/auth.service';
import { PlanService } from './services/plan.service';
import { OperationalMetricsComponent } from './components/metricas/operational-metrics/operational-metrics.component';
import { TiendaNubeSyncWizardComponent } from './components/tienda-nube/tienda-nube-sync-wizard/tienda-nube-sync-wizard.component';
import { MercadoLibreSyncWizardComponent } from './components/mercado-libre/mercado-libre-sync-wizard/mercado-libre-sync-wizard.component';
import { MensajeriaComponent } from './components/mensajeria/mensajeria.component';
import { IntegrationConfigComponent } from './components/settings/integration-config/integration-config.component';
import { TwoFactorComponent } from './components/profile/security/two-factor/two-factor.component';

export function initializeApp(authService: AuthService, planService: PlanService) {
  return (): Promise<void> => {
    const token = authService.getToken();
    if (!token) return Promise.resolve();
    if (authService.isTokenExpired(token)) {
      authService.logout();
      return Promise.resolve();
    }




    return firstValueFrom(
      authService.refreshToken().pipe(catchError(() => of(null)))
    ).then(response => {
      if (response?.token) {
        authService.saveTokens(response.token, response.refreshToken);
        planService.setFromAuthResponse(response.planTier, response.planStatus);
        return Promise.resolve();
      }


      return planService.syncFromBackend();
    });
  };
}

import { CommonModule } from '@angular/common';
import { SystemStatusComponent } from './components/settings/system-status/system-status.component';
import { SupportComponent } from './components/settings/support/support.component';
import { FaqComponent } from './components/settings/faq/faq.component';
import { ChangelogComponent } from './components/settings/changelog/changelog.component';

import { ArcaCertWizardComponent } from './components/arca/arca-cert-wizard/arca-cert-wizard.component';
import { ArcaConfigComponent } from './components/arca/arca-config/arca-config.component';
import { FacturacionComponent } from './components/facturacion/facturacion.component';
import { FacturaDetalleComponent } from './components/facturacion/factura-detalle/factura-detalle.component';
import { HomeModule } from './components/home-dashboard/home.module';
import { BusinessMetricsComponent } from './components/metricas/business-metrics/business-metrics.component';
import { StockAnalysisComponent } from './components/metricas/stock-analysis/stock-analysis.component';
import { SalesOperationalComponent } from './components/metricas/operational-metrics/sales-operational/sales-operational.component';
import { BillingPlanComponent } from './components/settings/billing/billing-plan/billing-plan.component';
import { BillingPaymentComponent } from './components/settings/billing/billing-payment/billing-payment.component';
import { BillingInvoicesComponent } from './components/settings/billing/billing-invoices/billing-invoices.component';
import { BillingDataComponent } from './components/settings/billing/billing-data/billing-data.component';
import { PlanRequiredComponent } from './components/plan-required/plan-required.component';
import { MensajeriaWhatsappComponent } from './components/mensajeria/whatsapp/mensajeria-whatsapp.component';
import { MensajeriaMercadolibreComponent } from './components/mensajeria/mercadolibre/mensajeria-mercadolibre.component';
import { MensajeriaInstagramComponent } from './components/mensajeria/instagram/mensajeria-instagram.component';
import { PrivacyDataComponent } from './components/settings/privacy-data/privacy-data.component';
import { DeleteAccountComponent } from './components/settings/delete-account/delete-account.component';
import { AuditComponent } from './components/settings/audit/audit.component';
import { BottomNavComponent } from './components/main-layout/bottom-nav/bottom-nav.component';
import { CheckoutResultComponent } from './components/settings/billing/checkout-result/checkout-result.component';
import { QRCodeModule } from 'angularx-qrcode';
import { LandingComponent } from './components/landing/landing.component';
import { MetricasAvanzadasComponent } from './components/metricas/advanced-metrics/metricas-avanzadas.component';
import { AuditAdvancedComponent } from './components/settings/audit/audit-advanced/audit-advanced.component';

@NgModule({
  declarations: [
    MetricasComponent,
    AppComponent,
    LoginComponent,
    MainLayoutComponent,
    PlaceholderComponent,
    ProfileComponent,
    EmailComponent,
    PasswordComponent,
    AccessHistoryComponent,
    ActiveSessionsComponent,
    CompanyDataComponent,
    PreferencesComponent,
    OperationalMetricsComponent,
    TiendaNubeSyncWizardComponent,
    MercadoLibreSyncWizardComponent,
    ArcaCertWizardComponent,
    ArcaConfigComponent,
    MensajeriaComponent,
    IntegrationConfigComponent,
    TwoFactorComponent,
    SystemStatusComponent,
    SupportComponent,
    FaqComponent,
    ChangelogComponent,
    FacturacionComponent,
    FacturaDetalleComponent,
    BusinessMetricsComponent,
    StockAnalysisComponent,
    SalesOperationalComponent,
    BillingPlanComponent,
    BillingPaymentComponent,
    BillingInvoicesComponent,
    BillingDataComponent,
    PlanRequiredComponent,
    CheckoutResultComponent,
    MensajeriaWhatsappComponent,
    MensajeriaMercadolibreComponent,
    MensajeriaInstagramComponent,
    PrivacyDataComponent,
    DeleteAccountComponent,
    AuditComponent,
    BottomNavComponent,
    LandingComponent,
    MetricasAvanzadasComponent,
    AuditAdvancedComponent,
  ],
  imports: [
    CommonModule,
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    SharedModule,
    HomeModule,
    QRCodeModule
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    {
      provide: APP_INITIALIZER,
      useFactory: initializeApp,
      deps: [AuthService, PlanService],
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})

export class AppModule { }
