import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../../services/auth.service';

@Component({
  selector: 'app-two-factor',
  templateUrl: './two-factor.component.html',
  styleUrls: ['./two-factor.component.css']
})
export class TwoFactorComponent implements OnInit {
  isEnabled = false;
  isLoading = false;
  successMessage = '';
  errorMessage = '';

  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    this.loadStatus();
  }

  loadStatus() {
    this.isLoading = true;
    this.authService.getTwoFactorStatus().subscribe({
      next: (status) => {
        this.isEnabled = status;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Error al cargar el estado de 2FA';
        this.isLoading = false;
      }
    });
  }

  toggleTwoFactor() {
    this.isLoading = true;
    this.successMessage = '';
    this.errorMessage = '';
    const newState = !this.isEnabled;

    this.authService.toggleTwoFactor(newState).subscribe({
      next: () => {
        this.isEnabled = newState;
        this.successMessage = newState ? 'Autenticación de dos factores activada' : 'Autenticación de dos factores desactivada';
        this.isLoading = false;


        setTimeout(() => {
            this.successMessage = '';
        }, 3000);
      },
      error: () => {
        this.errorMessage = 'Error al actualizar la configuración';
        this.isLoading = false;
      }
    });
  }
}
