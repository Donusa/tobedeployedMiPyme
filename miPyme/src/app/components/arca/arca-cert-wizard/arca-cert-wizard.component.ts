import { Component, OnInit, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { ArcaService } from '../../../services/arca/arca.service';
import { AuthService } from '../../../services/auth.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-arca-cert-wizard',
  templateUrl: './arca-cert-wizard.component.html',
  styleUrls: ['./arca-cert-wizard.component.css']
})
export class ArcaCertWizardComponent implements OnInit {
  @ViewChild('wizardContent') wizardContent!: ElementRef;
  step: number = 1;
  isLoading: boolean = false;
  isCheckingConfig: boolean = true;
  showDashboard: boolean = false;


  isGeneratingCsr: boolean = false;
  csrGenerated: boolean = false;
  privateKey: string = '';
  csr: string = '';
  companyName: string = '';
  cuit: string = '';


  certificateContent: string = '';
  isSavingCert: boolean = false;
  isTestingConnection: boolean = false;
  isAutoTesting: boolean = false;
  showRenewalForm: boolean = false;
  selectedP12File: File | null = null;
  p12Password: string = '';
  isActivating: boolean = false;
  connectionStatus: 'success' | 'error' | null = null;

  constructor(
    private arcaService: ArcaService,
    private authService: AuthService,
    private router: Router
  ) { }

  goToSystemStatus() {
    this.router.navigate(['/configuracion/ayuda/status']);
  }

  goToFacturacion() {
    this.router.navigate(['/facturacion']);
  }

  ngOnInit(): void {


    this.companyName = 'Mi Empresa S.A.';
    this.cuit = '20123456789';





    this.checkExistingConfig();
  }

  checkExistingConfig() {
    this.isCheckingConfig = true;
    this.arcaService.getArcaConfig().subscribe({
      next: (config) => {
        this.isCheckingConfig = false;
        if (config) {
          if (config.companyName) this.companyName = config.companyName;
          if (config.cuit) this.cuit = config.cuit;

          if (config.currentStep) {
            this.step = config.currentStep;
          }
          if (config.wizardCompleted) {
            this.showDashboard = true;
            this.autoTestConnection();
          }
        }
      },
      error: (err) => {
        this.isCheckingConfig = false;
        console.log('No existing ARCA config found or error fetching it', err);
      }
    });
  }

  nextStep() {
    this.step++;
    this.saveStepState();
    this.scrollToTop();
  }

  prevStep() {
    if (this.step > 1) {
      this.step--;
      this.saveStepState();
      this.scrollToTop();
    }
  }

  scrollToTop() {
    setTimeout(() => {
      if (this.wizardContent) {
        this.wizardContent.nativeElement.scrollTo({ top: 0, behavior: 'smooth' });
      }
    }, 100);
  }

  saveStepState() {
    this.arcaService.saveWizardState(this.step, false).subscribe({
      error: (err) => console.error('Error saving step state', err)
    });
  }

  generateCsr() {
    this.isGeneratingCsr = true;
    this.arcaService.generateCsr(this.companyName, this.cuit).subscribe({
      next: (res) => {
        this.privateKey = res.privateKey;
        this.csr = res.csr;
        this.csrGenerated = true;
        this.isGeneratingCsr = false;

        this.copyToClipboard(this.csr);
        Swal.fire({
          icon: 'success',
          title: '¡Clave y CSR generados!',
          text: 'El CSR se ha copiado al portapapeles.',
          timer: 2000,
          showConfirmButton: false
        });
      },
      error: (err) => {
        console.error('Error generating CSR', err);
        this.isGeneratingCsr = false;
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: 'Error al generar el certificado. Por favor intente nuevamente.'
        });
      }
    });
  }

  copyToClipboard(text: string) {
    navigator.clipboard.writeText(text).then(() => {
      Swal.fire({
        icon: 'success',
        title: '¡Copiado!',
        text: 'Texto copiado al portapapeles',
        timer: 1500,
        showConfirmButton: false
      });
    }).catch(err => {
      console.error('Could not copy text: ', err);
    });
  }

  finishWizard() {
    this.arcaService.saveWizardState(4, true).subscribe({
      next: () => {
        Swal.fire({
          icon: 'success',
          title: '¡Configuración Finalizada!',
          text: 'Tu cuenta ha sido vinculada exitosamente con ARCA y la autorización ha sido creada.',
          confirmButtonColor: '#28a745'
        }).then(() => {
          this.showDashboard = true;
        });
      },
      error: (err) => {
        console.error('Error finalizing wizard', err);
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: 'Error al finalizar la configuración. Por favor intente nuevamente.'
        });
      }
    });
  }

  saveCertificate() {
    if (!this.certificateContent) {
      Swal.fire({
        icon: 'warning',
        title: 'Atención',
        text: 'Por favor, pega el contenido del certificado antes de continuar.'
      });
      return;
    }



    this.nextStep();
  }

  downloadP12() {
    Swal.fire({
      title: 'Generar P12',
      text: 'Ingrese una contraseña para proteger el archivo P12. ¡No la olvide!',
      input: 'password',
      inputAttributes: {
        autocapitalize: 'off'
      },
      showCancelButton: true,
      confirmButtonText: 'Generar y Descargar',
      showLoaderOnConfirm: true,
      preConfirm: (password) => {
        if (!password) {
          Swal.showValidationMessage('La contraseña es requerida');
          return false;
        }

        return this.arcaService.downloadP12(password, this.certificateContent).toPromise()
          .then(blob => {
            return blob;
          })
          .catch(error => {
            Swal.showValidationMessage(
              `Falló la solicitud: ${error}`
            );
          });
      },
      allowOutsideClick: () => !Swal.isLoading()
    }).then((result) => {
      if (result.isConfirmed && result.value) {
        const url = window.URL.createObjectURL(result.value);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'alias.p12';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        Swal.fire({
          icon: 'success',
          title: 'P12 Descargado',
          text: 'Ahora sube este archivo en el siguiente paso para activar la conexión.'
        }).then(() => {
          this.nextStep();
        });
      }
    });
  }

  autoTestConnection() {
    this.isAutoTesting = true;
    this.showRenewalForm = false;
    this.arcaService.checkConnection().subscribe({
      next: (res) => {
        this.isAutoTesting = false;
        this.goToFacturacion();
      },
      error: (err) => {
        console.error('Auto test connection failed, showing renewal form', err);
        this.isAutoTesting = false;
        this.showRenewalForm = true;
      }
    });
  }

  onFileSelected(event: any) {
    if (event.target.files && event.target.files.length > 0) {
      this.selectedP12File = event.target.files[0];
    } else {
      this.selectedP12File = null;
    }
  }

  submitRenewal() {
    if (!this.selectedP12File || !this.p12Password) {
      return;
    }
    this.isActivating = true;
    this.arcaService.activate(this.selectedP12File, this.p12Password).subscribe({
      next: () => {
        this.isActivating = false;
        Swal.fire('¡Activado!', 'La conexión con AFIP ha sido establecida exitosamente.', 'success').then(() => {
          this.goToFacturacion();
        });
      },
      error: (error: any) => {
        this.isActivating = false;
        console.error('Error de activación:', error);
        const msg = error.error || error.message || 'Error desconocido';
        Swal.fire('Error de activación', msg, 'error');
      }
    });
  }


  activateWithP12(redirectOnSuccess: boolean = false) {
    Swal.fire({
      title: 'Activar / Renovar Conexión AFIP',
      html: `
        <p>Sube el archivo <b>alias.p12</b> para renovar tu sesión de seguridad.</p>
        <input type="file" id="p12-file" class="swal2-input" accept=".p12">
        <input type="password" id="p12-password" class="swal2-input" placeholder="Contraseña del P12">
      `,
      showCancelButton: true,
      confirmButtonText: 'Activar',
      showLoaderOnConfirm: true,
      preConfirm: () => {
        const fileInput = document.getElementById('p12-file') as HTMLInputElement;
        const passwordInput = document.getElementById('p12-password') as HTMLInputElement;

        if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
          Swal.showValidationMessage('Debes seleccionar el archivo P12');
          return false;
        }
        if (!passwordInput || !passwordInput.value) {
          Swal.showValidationMessage('Debes ingresar la contraseña');
          return false;
        }

        const file = fileInput.files[0];
        const password = passwordInput.value;

        return this.arcaService.activate(file, password).toPromise()
          .catch(error => {
            console.error(error);
            Swal.showValidationMessage(`Error de activación: ${error.error || error.message}`);
          });
      }
    }).then((result) => {
      if (result.isConfirmed) {
        Swal.fire('¡Activado!', 'La conexión con AFIP ha sido establecida exitosamente.', 'success').then(() => {
          if (redirectOnSuccess || this.showDashboard) {
            this.goToFacturacion();
          } else {
            this.finishWizard();
          }
        });
      }
    });
  }

  testConnection() {
    this.isTestingConnection = true;
    this.connectionStatus = null;
    this.arcaService.checkConnection().subscribe({
      next: (res) => {
        this.isTestingConnection = false;
        this.connectionStatus = 'success';
        console.log('AFIP Response:', res);
        Swal.fire({
          icon: 'success',
          title: 'Conexión Exitosa',
          text: 'Se ha establecido conexión con los servidores de AFIP correctamente. Respuesta: ' + res.substring(0, 100) + '...'
        });
      },
      error: (err) => {
        console.error('Error connecting to AFIP', err);
        this.isTestingConnection = false;
        this.connectionStatus = 'error';


        const errorMessage = typeof err.error === 'string' ? err.error : (err.message || 'Error desconocido');
        const isExpired = errorMessage.includes('renueve la sesión') || errorMessage.includes('token válido');

        Swal.fire({
          icon: 'error',
          title: 'Error de Conexión',
          text: isExpired
            ? 'La sesión con AFIP ha expirado. Por favor utilice el botón "Renovar Sesión (P12)" para restablecerla.'
            : 'No se pudo conectar con AFIP. ' + errorMessage
        });
      }
    });
  }
}
