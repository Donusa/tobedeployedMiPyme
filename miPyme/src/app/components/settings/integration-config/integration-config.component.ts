import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TiendaNubeService } from '../../../services/tienda-nube.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-integration-config',
  templateUrl: './integration-config.component.html',
  styleUrls: ['./integration-config.component.css']
})
export class IntegrationConfigComponent implements OnInit {
  type: 'mercadolibre' | 'tiendanube' | 'whatsapp' = 'mercadolibre';
  isLoading = true;
  isConnected = false;
  userData: any = null;
  errorMessage = '';
  successMessage = '';

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private tiendaNubeService: TiendaNubeService,
    private router: Router
  ) { }

  ngOnInit() {
    this.route.data.subscribe(data => {
      this.type = data['type'];
      this.checkStatus();
    });
  }

  private getStatusUrl(): string {
    switch (this.type) {
      case 'mercadolibre': return `${environment.apiUrl}/api/mercadolibre/status`;
      case 'tiendanube': return `${environment.apiUrl}/api/tiendanube/status`;
      case 'whatsapp': return `${environment.apiUrl}/api/whatsapp/status`;
    }
  }

  checkStatus() {
    this.isLoading = true;
    this.http.get<any>(this.getStatusUrl()).subscribe({
      next: (res) => {
        this.isConnected = res.connected;
        if (this.isConnected) {
          this.userData = res;
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Error al verificar estado';
        this.isLoading = false;
      }
    });
  }

  private getUnlinkUrl(): string {
    switch (this.type) {
      case 'mercadolibre': return `${environment.apiUrl}/api/mercadolibre/unlink`;
      case 'tiendanube': return `${environment.apiUrl}/api/tiendanube/unlink`;
      case 'whatsapp': return `${environment.apiUrl}/api/whatsapp/disconnect`;
    }
  }

  unlink() {
    if (!confirm('¿Estás seguro de que deseas desvincular esta cuenta?')) {
      return;
    }

    this.isLoading = true;
    this.http.post(this.getUnlinkUrl(), {}).subscribe({
      next: () => {
        this.successMessage = 'Cuenta desvinculada correctamente';
        this.isConnected = false;
        this.userData = null;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Error al desvincular';
        this.isLoading = false;
      }
    });
  }

  goToWizard() {
    switch (this.type) {
      case 'mercadolibre': this.router.navigate(['/mercadolibre']); break;
      case 'tiendanube': this.router.navigate(['/tiendanube']); break;
      case 'whatsapp': this.router.navigate(['/whatsapp']); break;
    }
  }

  getTitle(): string {
    switch (this.type) {
      case 'mercadolibre': return 'Mercado Libre';
      case 'tiendanube': return 'Tienda Nube';
      case 'whatsapp': return 'WhatsApp';
    }
  }

  getIcon(): string {
    switch (this.type) {
      case 'mercadolibre': return 'bx-shopping-bag';
      case 'tiendanube': return 'bx-cloud';
      case 'whatsapp': return 'bxl-whatsapp';
    }
  }
}
