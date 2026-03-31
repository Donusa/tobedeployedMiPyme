import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-email',
  templateUrl: './email.component.html',
  styleUrls: ['../profile.component.css']
})
export class EmailComponent implements OnInit {
  currentEmail: string = '';
  newEmail: string = '';
  currentPassword: string = '';
  showPassword = false;

  isLoading = false;
  successMessage = '';
  errorMessage = '';

  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.isLoading = true;
    this.authService.getProfile().subscribe({
      next: (data) => {
        this.currentEmail = data.email;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Error al cargar el perfil';
        this.isLoading = false;
      }
    });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    this.isLoading = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.authService.changeEmail({
      currentEmail: this.currentEmail,
      newEmail: this.newEmail,
      password: this.currentPassword
    }).subscribe({
      next: (response: any) => {

        if (response && response.token) {
          this.authService.saveTokens(
            response.token,
            response.refreshToken,
            response.companyName,
            response.name,
            response.permissions,
            response.theme
          );
        }

        this.successMessage = 'Email actualizado correctamente';
        this.currentEmail = this.newEmail;
        this.newEmail = '';
        this.currentPassword = '';
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Email update error:', err);
        if (err.status === 400) {

            if (err.error && typeof err.error === 'object' && err.error.message) {
                this.errorMessage = err.error.message;
            } else if (typeof err.error === 'string') {

                 try {
                     const parsed = JSON.parse(err.error);
                     this.errorMessage = parsed.message || 'Contraseña incorrecta o email actual no coincide';
                 } catch (e) {
                     this.errorMessage = err.error || 'Contraseña incorrecta o email actual no coincide';
                 }
            } else {
                this.errorMessage = 'Contraseña incorrecta o email actual no coincide';
            }
        } else {
            this.errorMessage = 'Error al actualizar el email';
        }
        this.isLoading = false;
      }
    });
  }
}
