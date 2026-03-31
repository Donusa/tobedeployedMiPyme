import { Component } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-password',
  templateUrl: './password.component.html',
  styleUrls: ['../profile.component.css']
})
export class PasswordComponent {
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';

  showCurrent = false;
  showNew = false;
  showConfirm = false;

  isLoading = false;
  successMessage = '';
  errorMessage = '';

  constructor(private authService: AuthService) { }

  toggleVisibility(field: 'current' | 'new' | 'confirm'): void {
    if (field === 'current') this.showCurrent = !this.showCurrent;
    if (field === 'new') this.showNew = !this.showNew;
    if (field === 'confirm') this.showConfirm = !this.showConfirm;
  }

  onInputChange(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }

  onSubmit(): void {
    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Las contraseñas no coinciden';
      return;
    }

    this.isLoading = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.authService.changePassword({
      currentPassword: this.currentPassword,
      newPassword: this.newPassword
    }).subscribe({
      next: () => {
        this.successMessage = 'Contraseña actualizada correctamente';
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.isLoading = false;
      },
      error: (err) => {
        if (err.status === 400) {
            this.errorMessage = 'La contraseña actual es incorrecta';
        } else {
            this.errorMessage = 'Error al actualizar la contraseña';
        }
        this.isLoading = false;
      }
    });
  }
}
