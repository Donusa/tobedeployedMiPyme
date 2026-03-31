import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ArcaService, ArcaConfig } from '../../../services/arca/arca.service';
import Swal from 'sweetalert2';
import { Router } from '@angular/router';

@Component({
  selector: 'app-arca-config',
  templateUrl: './arca-config.component.html',
  styleUrls: ['./arca-config.component.css']
})
export class ArcaConfigComponent implements OnInit {
  configForm: FormGroup;
  isLoading = false;
  isSaving = false;
  currentConfig: ArcaConfig | null = null;

  constructor(
    private fb: FormBuilder,
    private arcaService: ArcaService,
    private router: Router
  ) {
    this.configForm = this.fb.group({
      companyName: ['', Validators.required],
      cuit: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
      certificateExpiration: [{value: '', disabled: true}]
    });
  }

  ngOnInit(): void {
    this.loadConfig();
  }

  loadConfig() {
    this.isLoading = true;
    this.arcaService.getArcaConfig().subscribe({
      next: (config) => {
        this.currentConfig = config;
        if (config) {
          this.configForm.patchValue({
            companyName: config.companyName || '',
            cuit: config.cuit || '',


          });
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading ARCA config', err);
        this.isLoading = false;
      }
    });
  }

  onSubmit() {
    if (this.configForm.invalid) {
      return;
    }

    this.isSaving = true;
    const formData = this.configForm.getRawValue();

    this.arcaService.updateConfig(formData).subscribe({
      next: (updatedConfig) => {
        this.isSaving = false;
        this.currentConfig = updatedConfig;
        Swal.fire({
          icon: 'success',
          title: 'Guardado',
          text: 'La configuración ha sido actualizada exitosamente.',
          timer: 1500,
          showConfirmButton: false
        });
      },
      error: (err) => {
        this.isSaving = false;
        console.error('Error updating config', err);
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: 'No se pudieron guardar los cambios. Intente nuevamente.'
        });
      }
    });
  }

  onUnlink() {
    Swal.fire({
      title: '¿Estás seguro?',
      text: "Se eliminará toda la configuración de ARCA y los certificados asociados. Esta acción no se puede deshacer.",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Sí, desvincular',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.isLoading = true;
        this.arcaService.deleteConfig().subscribe({
          next: () => {
            this.isLoading = false;
            this.currentConfig = null;
            this.configForm.reset();
            Swal.fire(
              'Desvinculado',
              'La configuración de ARCA ha sido eliminada.',
              'success'
            );
            this.loadConfig();
          },
          error: (err) => {
            this.isLoading = false;
            console.error('Error deleting config', err);
            Swal.fire(
              'Error',
              'No se pudo eliminar la configuración.',
              'error'
            );
          }
        });
      }
    });
  }

  goBack() {
    this.router.navigate(['/arca']);
  }
}
