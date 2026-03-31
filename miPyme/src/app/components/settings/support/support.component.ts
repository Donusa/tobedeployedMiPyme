import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { SupportService } from '../../../services/support.service';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-support',
  templateUrl: './support.component.html',
  styleUrls: ['./support.component.css']
})
export class SupportComponent implements OnInit {

  supportForm: FormGroup;
  isLoading = false;
  successMessage = '';
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private supportService: SupportService,
    private authService: AuthService
  ) {
    this.supportForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      subject: ['', Validators.required],
      message: ['', [Validators.required, Validators.minLength(20)]]
    });
  }

  ngOnInit(): void {

    const userName = this.authService.getUserName();





    if (userName) {
      this.supportForm.patchValue({ name: userName });
    }
  }

  onSubmit() {
    if (this.supportForm.invalid) {
      this.supportForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.supportService.sendContactMessage(this.supportForm.value).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Mensaje enviado correctamente. Nos pondremos en contacto contigo pronto.';
        this.supportForm.reset();

        const userName = this.authService.getUserName();
        if (userName) this.supportForm.patchValue({ name: userName });
      },
      error: (err: any) => {
        this.isLoading = false;
        this.errorMessage = 'Error al enviar el mensaje. Por favor intenta nuevamente más tarde.';
        console.error('Support error:', err);
      }
    });
  }

  get f() { return this.supportForm.controls; }
}
