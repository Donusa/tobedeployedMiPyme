import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../services/theme.service';
import { MercadoPagoService, PlanPricingInfo, PlanPrice } from '../../services/mercadopago.service';
import { PlanService } from '../../services/plan.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  animations: [
    trigger('expandToFull', [
      state('normal', style({
        flex: '0 0 520px',
        width: '520px',
        position: 'relative'
      })),
      state('sidebar', style({
        flex: '0 0 260px',
        width: '260px',
        position: 'relative',
        zIndex: 100
      })),
      state('expanded', style({
        flex: '0 0 100vw',
        width: '100vw',
        height: '100vh',
        position: 'absolute',
        top: 0,
        left: 0,
        zIndex: 100,
        borderRight: 'none'
      })),
      state('wide', style({
        flex: '0 0 100vw',
        width: '100vw',
        position: 'relative',
        borderRight: 'none'
      })),
      transition('normal => expanded', [
        animate('800ms cubic-bezier(0.77, 0, 0.175, 1)')
      ]),
      transition('normal => sidebar', [
        animate('350ms cubic-bezier(0.25, 0.8, 0.25, 1)')
      ]),
      transition('normal <=> wide', [
        animate('500ms cubic-bezier(0.25, 0.8, 0.25, 1)')
      ])
    ]),
    trigger('heroFadeOut', [
      state('visible', style({ opacity: 1, flex: '1', minWidth: '0' })),
      state('hidden', style({ opacity: 0, flex: '1', minWidth: '0' })),
      state('collapsed', style({ opacity: 0, flex: '0 0 0px', width: '0px', overflow: 'hidden', padding: '0' })),
      transition('visible => hidden', [
        animate('300ms ease-out')
      ]),
      transition('visible <=> collapsed', [
        animate('500ms cubic-bezier(0.25, 0.8, 0.25, 1)')
      ]),
      transition('hidden => collapsed', [
        animate('300ms ease-out')
      ])
    ]),
    trigger('contentFade', [
      transition(':enter', [
        style({ opacity: 0, transform: 'scale(0.95)' }),
        animate('500ms 300ms ease-out', style({ opacity: 1, transform: 'scale(1)' }))
      ]),
      transition(':leave', [
        animate('300ms ease-in', style({ opacity: 0 }))
      ])
    ]),
    trigger('rotateInOut', [
      transition(':enter', [
        style({ transform: 'perspective(400px) rotateX(-90deg)', transformOrigin: 'top', opacity: 0, height: 0, marginBottom: 0, overflow: 'hidden' }),
        animate('300ms ease-out', style({ transform: 'perspective(400px) rotateX(0)', opacity: 1, height: '*', marginBottom: '*' }))
      ]),
      transition(':leave', [
        style({ transform: 'perspective(400px) rotateX(0)', transformOrigin: 'top', opacity: 1, height: '*', marginBottom: '*', overflow: 'hidden' }),
        animate('300ms ease-in', style({ transform: 'perspective(400px) rotateX(-90deg)', opacity: 0, height: 0, marginBottom: 0 }))
      ])
    ]),
    trigger('fadeInOut', [
      transition(':enter', [
        style({ opacity: 0, height: 0, marginBottom: 0, overflow: 'hidden' }),
        animate('300ms ease-out', style({ opacity: 1, height: '*', marginBottom: '*' }))
      ]),
      transition(':leave', [
        style({ opacity: 1, height: '*', marginBottom: '*', overflow: 'hidden' }),
        animate('300ms ease-in', style({ opacity: 0, height: 0, marginBottom: 0 }))
      ])
    ]),
    trigger('slideFade', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(-10px)', height: 0, marginBottom: 0, overflow: 'hidden' }),
        animate('300ms ease-out', style({ opacity: 1, transform: 'translateY(0)', height: '*', marginBottom: '*' }))
      ]),
      transition(':leave', [
        style({ opacity: 1, transform: 'translateY(0)', height: '*', marginBottom: '*', overflow: 'hidden' }),
        animate('300ms ease-in', style({ opacity: 0, transform: 'translateY(-10px)', height: 0, marginBottom: 0 }))
      ])
    ])
  ]
})
export class LoginComponent implements OnInit {
  ssoCode: string = '';
  username: string = '';
  rememberMe: boolean = false;
  password: string = '';
  errorMessage: string = '';
  submitted: boolean = false;
  isLoading: boolean = false;
  isGeneratingSso: boolean = false;
  private errorTimeout: any;
  private errorClearTimeout: any;
  isErrorVisible: boolean = false;
  isForgotPasswordMode: boolean = false;
  forgotPasswordStep: number = 1;
  recoveryCode: string = '';
  newPassword: string = '';
  confirmNewPassword: string = '';
  isRegisterMode: boolean = false;
  isTwoFactorMode: boolean = false;
  twoFactorCode: string = '';
  isRegistrationSuccess: boolean = false;
  isLoginSuccess: boolean = false;
  registerStep: number = 1;


  confirmPassword: string = '';


  selectedPlan: 'base' | 'pro' | 'enterprise' | '' = '';
  billingCycle: 'monthly' | 'annual' = 'monthly';
  pricing: PlanPricingInfo | null = null;
  isLoadingPricing: boolean = false;
  pricingError: boolean = false;

  activeCarouselIndex: number = 1;

  planCards = [
    {
      id: 'base' as const,
      name: 'Base',
      description: 'Ideal para comenzar a gestionar tu negocio.',
      features: ['Dashboard principal', 'Gestión de stock', 'Registro de ventas', 'Administración de empleados']
    },
    {
      id: 'pro' as const,
      name: 'Pro',
      description: 'Conecta tus tiendas online y vende en todos los canales.',
      features: ['Todo lo del plan Base', 'Ofertas y promociones', 'Gestión de pedidos', 'Sincronización con MercadoLibre', 'Mensajería integrada'],
      badge: '28 días gratis'
    },
    {
      id: 'enterprise' as const,
      name: 'Enterprise',
      description: 'Facturación electrónica y cumplimiento fiscal completo.',
      features: ['Todo lo del plan Pro', 'Métricas avanzadas', 'Facturación electrónica ARCA', 'Emisión de facturas y tickets', 'Soporte prioritario']
    }
  ];

  passwordVisibility = {
    login: false,
    registerAdmin: false,
    registerConfirm: false,
    newPassword: false,
    confirmNewPassword: false
  };

  registerData = {
    businessName: '',
    tradeName: '',
    ssoCode: '',
    termsAccepted: false,
    adminName: '',
    adminEmail: '',
    adminPassword: ''
  };

  shakeState = {
    ssoCode: false,
    username: false,
    password: false,
    registerBusinessName: false,
    registerTradeName: false,
    registerAdminName: false,
    registerAdminEmail: false,
    registerAdminPassword: false,
    registerConfirmPassword: false
  };

  get isNextDisabled(): boolean {
    if (this.isLoading) return true;
    if (this.isRegisterMode) {
      if (this.registerStep === 1) {
        return !this.registerData.ssoCode || this.isGeneratingSso || !this.registerData.adminName;
      }
      if (this.registerStep === 2) {
        return !this.registerData.adminEmail ||
               !this.registerData.adminPassword ||
               !this.confirmPassword ||
               (this.registerData.adminPassword !== this.confirmPassword) ||
               !this.registerData.termsAccepted;
      }
      if (this.registerStep === 3) {
        return !this.selectedPlan;
      }
    }
    return false;
  }

  constructor(private authService: AuthService, private router: Router, private themeService: ThemeService, private mpService: MercadoPagoService, private planService: PlanService) {}

  ngOnInit() {
    try {
      const stored = localStorage.getItem('loginRemember');
      if (stored) {
        const data = JSON.parse(stored);
        this.ssoCode = data.ssoCode || '';
        this.username = data.username || '';
        this.rememberMe = true;
      }
    } catch {}
  }

  toggleForgotPasswordMode() {
    this.isForgotPasswordMode = !this.isForgotPasswordMode;
    this.forgotPasswordStep = 1;
    this.isRegisterMode = false;
    this.errorMessage = '';
    this.isErrorVisible = false;
    this.recoveryCode = '';
    this.newPassword = '';
    this.confirmNewPassword = '';
  }

  onRecoverPassword() {
    this.errorMessage = '';
    this.isErrorVisible = false;

    if (this.forgotPasswordStep === 1) {
      if (!this.ssoCode || !this.username) {
        this.triggerShake(['ssoCode', 'username']);
        this.showError('Por favor ingrese Código de Organización y Email');
        return;
      }

      this.isLoading = true;
      this.authService.forgotPassword(this.ssoCode, this.username).subscribe({
        next: () => {
          this.isLoading = false;
          this.forgotPasswordStep = 2;
        },
        error: (err) => {
          this.isLoading = false;
          console.error('Forgot password error', err);
          this.showError('Error al procesar la solicitud. Verifique los datos.');
        }
      });
    } else if (this.forgotPasswordStep === 2) {
      if (!this.recoveryCode || this.recoveryCode.length !== 6) {
        this.showError('Ingrese un código válido de 6 dígitos');
        return;
      }

      this.isLoading = true;
      this.authService.verifyRecoveryCode(this.ssoCode, this.username, this.recoveryCode).subscribe({
        next: () => {
          this.isLoading = false;
          this.forgotPasswordStep = 3;
        },
        error: (err) => {
          this.isLoading = false;
          console.error('Verify code error', err);
          this.showError('Código inválido o expirado');
        }
      });
    } else if (this.forgotPasswordStep === 3) {
      if (!this.newPassword || !this.confirmNewPassword) {
        this.showError('Ingrese la nueva contraseña');
        return;
      }

      if (this.newPassword !== this.confirmNewPassword) {
        this.showError('Las contraseñas no coinciden');
        return;
      }

      this.isLoading = true;
      this.authService.resetPassword(this.ssoCode, this.username, this.recoveryCode, this.newPassword).subscribe({
        next: () => {
          this.isLoading = false;
          this.isForgotPasswordMode = false;
          this.forgotPasswordStep = 1;
          this.password = '';
          this.errorMessage = '';

          setTimeout(() => {
             this.showError('Contraseña actualizada. Por favor inicie sesión.');
          }, 100);
        },
        error: (err) => {
          this.isLoading = false;
          console.error('Reset password error', err);
          this.showError('Error al restablecer la contraseña');
        }
      });
    }
  }

  cancelTwoFactor() {
    this.isTwoFactorMode = false;
    this.twoFactorCode = '';
    this.errorMessage = '';
    this.isErrorVisible = false;
  }

  onRegister() {
    this.isRegisterMode = true;
    this.isForgotPasswordMode = false;
    this.registerStep = 1;
    this.errorMessage = '';
    this.isErrorVisible = false;
  }

  onBackToLogin() {
    this.isRegisterMode = false;
    this.isForgotPasswordMode = false;
    this.errorMessage = '';
    this.isErrorVisible = false;
    this.registerData = {
      businessName: '',
      tradeName: '',
      ssoCode: '',
      termsAccepted: false,
      adminName: '',
      adminEmail: '',
      adminPassword: ''
    };
    this.selectedPlan = '';
  }

  generateSso() {
    if (!this.registerData.tradeName) {

      this.registerData.ssoCode = '';
      return;
    }

    this.isGeneratingSso = true;
    const sourceName = this.registerData.tradeName.trim();
    const words = sourceName.split(/\s+/);
    let baseSso = '';


    for (let i = 0; i < words.length && i < 4; i++) {
      baseSso += words[i].charAt(0).toUpperCase();
    }


    if (baseSso.length < 4 && words.length > 0) {
      const lastWord = words[words.length - 1].toUpperCase();

      let lastWordIndex = 1;
      while (baseSso.length < 4 && lastWordIndex < lastWord.length) {
        baseSso += lastWord.charAt(lastWordIndex);
        lastWordIndex++;
      }
    }


    this.authService.getAvailableSso(baseSso).subscribe({
      next: (response) => {
        this.registerData.ssoCode = response.ssoCode;
        this.isGeneratingSso = false;
      },
      error: (err) => {
        console.error('Error fetching SSO', err);
        this.registerData.ssoCode = '';
        this.isGeneratingSso = false;
        this.showError('Error al generar el código SSO');
      }
    });
  }

  onConfirmPasswordBlur() {
    if (this.confirmPassword && this.confirmPassword !== this.registerData.adminPassword) {
      this.showError('Las contraseñas no coinciden');
      this.shakeState.registerConfirmPassword = true;
      this.shakeState.registerAdminPassword = true;
      setTimeout(() => {
        this.shakeState.registerConfirmPassword = false;
        this.shakeState.registerAdminPassword = false;
      }, 500);
    }
  }

  onNextStep() {
    if (this.registerStep === 1) {

       this.shakeState.registerTradeName = !this.registerData.tradeName;
       this.shakeState.registerAdminName = !this.registerData.adminName;

       this.shakeState.registerBusinessName = false;

       if (this.shakeState.registerTradeName || this.shakeState.registerAdminName) {
         this.showError('Por favor complete los campos obligatorios');
         return;
       }

       if (!this.registerData.ssoCode) {
          this.showError('Por favor genere el código de organización antes de continuar.');
          return;
       }

       this.registerStep++;
    } else if (this.registerStep === 2) {

       this.shakeState.registerAdminEmail = !this.registerData.adminEmail;
       this.shakeState.registerAdminPassword = !this.registerData.adminPassword;
       this.shakeState.registerConfirmPassword = !this.confirmPassword;

       if (this.shakeState.registerAdminEmail || this.shakeState.registerAdminPassword || this.shakeState.registerConfirmPassword) {
          this.showError('Por favor complete todos los campos de administrador');
          return;
       }

       if (this.registerData.adminPassword !== this.confirmPassword) {
          this.showError('Las contraseñas no coinciden');
          this.shakeState.registerAdminPassword = true;
          this.shakeState.registerConfirmPassword = true;
          return;
       }

       if (!this.registerData.termsAccepted) {
          this.showError('Debe aceptar los términos y condiciones');
          return;
       }


       this.loadPricing();
       this.selectedPlan = 'pro';
       this.activeCarouselIndex = 1;
       this.registerStep++;
    } else if (this.registerStep === 3) {
       if (!this.selectedPlan) {
          this.showError('Por favor seleccione un plan');
          return;
       }


       this.handleRegister();
    }
  }

  handleRegister() {
    this.isLoading = true;
    const registrationPayload = {
      ...this.registerData,
      planTier: this.selectedPlan
    };

    this.authService.register(registrationPayload).subscribe({
      next: (response: any) => {

        this.isRegistrationSuccess = true;
        this.isRegisterMode = false;


        this.authService.login(response.ssoCode, this.registerData.adminEmail, this.registerData.adminPassword).subscribe({
          next: (loginResponse) => {
            this.authService.saveTokens(
              loginResponse.token, loginResponse.refreshToken,
              loginResponse.companyName, loginResponse.name,
              loginResponse.permissions, loginResponse.theme,
              loginResponse.planTier, loginResponse.planStatus
            );
            this.planService.setFromAuthResponse(loginResponse.planTier, loginResponse.planStatus);
            this.themeService.reloadForUser();

            if (this.selectedPlan === 'pro') {

              setTimeout(() => {
                this.router.navigate(['/home']);
              }, 3500);
            } else {

              const planKey = this.billingCycle === 'annual' ? `${this.selectedPlan}-annual` : this.selectedPlan;
              this.mpService.subscribe(planKey, this.registerData.adminEmail).subscribe({
                next: (mpRes) => {
                  setTimeout(() => {
                    window.location.href = mpRes.initPoint;
                  }, 3500);
                },
                error: () => {

                  this.isLoading = false;
                  this.isRegistrationSuccess = false;
                  this.isRegisterMode = true;
                  this.registerStep = 3;
                  this.showError('Tu cuenta fue creada pero no pudimos iniciar el pago. Por favor iniciá sesión y completá el pago para acceder.');
                }
              });
            }
          },
          error: (loginError) => {
             this.isLoading = false;
             this.isRegistrationSuccess = false;
             this.registerStep = 1;
             this.showError('Registro exitoso. Por favor inicie sesión manualmente.');
          }
        });
      },
      error: (error: any) => {
        this.isLoading = false;
        this.showError('Error al registrar. Por favor intente nuevamente.');
      }
    });
  }

  onPreviousStep() {
    if (this.registerStep > 1) {
      this.registerStep--;
      this.errorMessage = '';
      this.isErrorVisible = false;
    }
  }

  loadPricing() {
    if (this.pricing) return;
    this.isLoadingPricing = true;
    this.pricingError = false;
    this.mpService.getPricing().subscribe({
      next: (pricing) => {
        this.pricing = pricing;
        this.isLoadingPricing = false;
      },
      error: () => {
        this.isLoadingPricing = false;
        this.pricingError = true;
      }
    });
  }

  retryLoadPricing() {
    this.pricing = null;
    this.loadPricing();
  }

  selectPlanCard(planId: 'base' | 'pro' | 'enterprise') {
    this.selectedPlan = planId;
    this.activeCarouselIndex = this.planCards.findIndex(p => p.id === planId);
  }


  carouselNext() {
    if (this.activeCarouselIndex < this.planCards.length - 1) {
      this.activeCarouselIndex++;
      this.selectedPlan = this.planCards[this.activeCarouselIndex].id;
    }
  }

  carouselPrev() {
    if (this.activeCarouselIndex > 0) {
      this.activeCarouselIndex--;
      this.selectedPlan = this.planCards[this.activeCarouselIndex].id;
    }
  }

  getPlanPrice(planId: string): PlanPrice | null {
    if (!this.pricing) return null;
    const prices = this.billingCycle === 'annual' ? this.pricing.annual : this.pricing.monthly;
    return prices[planId] || null;
  }

  formatPlanUsd(planId: string): string {
    const price = this.getPlanPrice(planId);
    if (!price) return '...';
    return `US$ ${price.usd}`;
  }

  formatPlanArs(planId: string): string {
    const price = this.getPlanPrice(planId);
    if (!price || !price.ars) return '';
    return `$ ${price.ars.toLocaleString('es-AR')} ARS`;
  }

  private triggerShake(fields: ('ssoCode' | 'username' | 'password')[]) {
    fields.forEach(field => this.shakeState[field] = true);
    setTimeout(() => {
      fields.forEach(field => this.shakeState[field] = false);
    }, 500);
  }

  private showError(message: string) {
    this.errorMessage = message;
    this.isErrorVisible = true;

    if (this.errorTimeout) {
      clearTimeout(this.errorTimeout);
    }
    if (this.errorClearTimeout) {
      clearTimeout(this.errorClearTimeout);
    }

    this.errorTimeout = setTimeout(() => {
      this.isErrorVisible = false;
      this.submitted = true;
      this.errorClearTimeout = setTimeout(() => {
        this.errorMessage = '';
      }, 400);
    }, 5000);
  }

  onLogin() {
    if (this.errorTimeout) {
      clearTimeout(this.errorTimeout);
    }
    if (this.errorClearTimeout) {
      clearTimeout(this.errorClearTimeout);
    }
    this.errorMessage = '';
    this.isErrorVisible = false;
    this.submitted = true;

    if (!this.ssoCode || !this.username || !this.password) {
      const missing: ('ssoCode' | 'username' | 'password')[] = [];
      if (!this.ssoCode) missing.push('ssoCode');
      if (!this.username) missing.push('username');
      if (!this.password) missing.push('password');
      this.triggerShake(missing);
      this.showError('Todos los campos son obligatorios');
      return;
    }

    this.isLoading = true;

    this.authService.login(this.ssoCode, this.username, this.password).subscribe({
      next: (response) => {
        if (response.requiresTwoFactor) {
          this.isLoading = false;
          this.isTwoFactorMode = true;
          this.errorMessage = '';
          return;
        }

        console.log('Login exitoso', response);
        this.authService.saveTokens(
          response.token, response.refreshToken,
          response.companyName, response.name,
          response.permissions, response.theme,
          response.planTier, response.planStatus
        );
        this.planService.setFromAuthResponse(response.planTier, response.planStatus);
        this.themeService.reloadForUser();
        if (this.rememberMe) {
          const data = { ssoCode: this.ssoCode, username: this.username };
          localStorage.setItem('loginRemember', JSON.stringify(data));
        } else {
          localStorage.removeItem('loginRemember');
        }


        this.isLoading = false;
        this.isLoginSuccess = true;


        setTimeout(() => {
          this.router.navigate(['/home']);
        }, 500);
      },
      error: (error) => {
        console.error('Fallo en login', error);
        this.isLoading = false;
        this.triggerShake(['ssoCode', 'username', 'password']);
        this.showError('Credenciales inválidas o error del servidor');
      }
    });
  }

  onVerifyTwoFactor() {
    if (!this.twoFactorCode || this.twoFactorCode.length !== 6) {
      this.showError('Ingrese un código válido de 6 dígitos');
      return;
    }

    this.isLoading = true;
    this.authService.verifyTwoFactor(this.ssoCode, this.username, this.twoFactorCode).subscribe({
      next: (response) => {
         this.authService.saveTokens(
           response.token, response.refreshToken,
           response.companyName, response.name,
           response.permissions, response.theme,
           response.planTier, response.planStatus
         );
         this.planService.setFromAuthResponse(response.planTier, response.planStatus);
         this.themeService.reloadForUser();
         if (this.rememberMe) {
           const data = { ssoCode: this.ssoCode, username: this.username };
           localStorage.setItem('loginRemember', JSON.stringify(data));
         } else {
           localStorage.removeItem('loginRemember');
         }

         this.isLoading = false;
         this.isLoginSuccess = true;
         setTimeout(() => {
           this.router.navigate(['/home']);
         }, 500);
      },
      error: (err) => {
         this.isLoading = false;
         this.showError('Código inválido o expirado');
      }
    });
  }

  togglePasswordVisibility(field: 'login' | 'registerAdmin' | 'registerConfirm' | 'newPassword' | 'confirmNewPassword') {
    this.passwordVisibility[field] = !this.passwordVisibility[field];
  }
}
