import { Component, OnInit } from '@angular/core';
import { OfferService, Offer, OfferActionResult } from '../../../services/offer.service';
import { CompanyService } from '../../../services/company.service';

@Component({
  selector: 'app-offers-list',
  templateUrl: './offers-list.component.html',
  styleUrls: ['./offers-list.component.css']
})
export class OffersListComponent implements OnInit {
  offers: Offer[] = [];
  showModal = false;
  selectedOffer: Offer | null = null;

  showDeleteModal = false;
  offerToDelete: Offer | null = null;


  showActionModal = false;
  actionTitle = '';
  actionMessage = '';
  actionCallback: (() => void) | null = null;


  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  showToast = false;

  isLoading = true;
  processingOfferId: number | null = null;

  constructor(
    private offerService: OfferService,
    private companyService: CompanyService
  ) { }

  ngOnInit(): void {
    this.companyService.updateSchema().subscribe({
      next: () => this.loadOffers(),
      error: (err) => {
        console.error('Failed to update schema', err);
        this.loadOffers();
      }
    });
  }

  loadOffers() {
    this.isLoading = true;
    this.offerService.getAll().subscribe({
      next: (data: Offer[]) => {
        this.offers = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading offers', err);
        this.showResultToast('Error al cargar ofertas: ' + (err.error?.message || err.message || 'Error desconocido'), 'error');
        this.isLoading = false;
      }
    });
  }

  openCreateModal() {
    this.selectedOffer = null;
    this.showModal = true;
  }

  openEditModal(offer: Offer) {
    this.selectedOffer = { ...offer };
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.selectedOffer = null;
  }

  onSave(offer: Offer) {
    if (offer.offerId) {
      this.offerService.update(offer.offerId, offer).subscribe({
        next: () => {
          this.loadOffers();
          this.closeModal();
        },
        error: (err) => {
          console.error('Error updating offer', err);
          this.showResultToast('Error al actualizar: ' + (err.error?.message || err.message), 'error');
        }
      });
    } else {
      this.offerService.create(offer).subscribe({
        next: () => {
          this.loadOffers();
          this.closeModal();
        },
        error: (err) => {
          console.error('Error creating offer', err);
          this.showResultToast('Error al crear: ' + (err.error?.message || err.message), 'error');
        }
      });
    }
  }

  deleteOffer(offer: Offer) {
    this.offerToDelete = offer;
    this.showDeleteModal = true;
  }

  confirmDelete() {
    if (this.offerToDelete && this.offerToDelete.offerId) {
      this.offerService.delete(this.offerToDelete.offerId).subscribe({
        next: () => {
          this.loadOffers();
          this.closeDeleteModal();
        },
        error: (err) => {
          console.error('Error deleting offer', err);
          this.showResultToast('Error al eliminar la oferta', 'error');
          this.closeDeleteModal();
        }
      });
    }
  }

  closeDeleteModal() {
    this.showDeleteModal = false;
    this.offerToDelete = null;
  }

  toggleTiendaNube(offer: Offer) {
    const action = offer.publishedTiendaNube ? 'despublicar' : 'publicar';
    const preposition = offer.publishedTiendaNube ? 'de' : 'en';

    this.actionTitle = `Confirmar ${action}`;
    this.actionMessage = `¿Estás seguro que deseas ${action} la oferta "${offer.name}" ${preposition} TiendaNube?`;

    this.actionCallback = () => {
      this.processingOfferId = offer.offerId!;
      const obs = offer.publishedTiendaNube
        ? this.offerService.unpublishTiendaNube(offer.offerId!)
        : this.offerService.publishTiendaNube(offer.offerId!);

      obs.subscribe({
        next: (result: OfferActionResult) => {
          this.processingOfferId = null;
          this.showResultToast(result.message || (result.success ? 'Operación exitosa' : 'Error'), result.success ? 'success' : 'error');
          this.loadOffers();
          this.closeActionModal();
        },
        error: (err) => {
          this.processingOfferId = null;
          const errResult = err.error as OfferActionResult;
          this.showResultToast(errResult?.message || errResult?.reason || err.message || 'Error inesperado', 'error');
          this.loadOffers();
          this.closeActionModal();
        }
      });
    };

    this.showActionModal = true;
  }

  toggleMercadoLibre(offer: Offer) {
    const action = offer.publishedMercadoLibre ? 'despublicar' : 'publicar';
    const preposition = offer.publishedMercadoLibre ? 'de' : 'en';

    this.actionTitle = `Confirmar ${action}`;
    this.actionMessage = `¿Estás seguro que deseas ${action} la oferta "${offer.name}" ${preposition} MercadoLibre?`;

    this.actionCallback = () => {
      this.processingOfferId = offer.offerId!;
      const obs = offer.publishedMercadoLibre
        ? this.offerService.unpublishMercadoLibre(offer.offerId!)
        : this.offerService.publishMercadoLibre(offer.offerId!);

      obs.subscribe({
        next: (result: OfferActionResult) => {
          this.processingOfferId = null;
          this.showResultToast(result.message || (result.success ? 'Operación exitosa' : 'Error'), result.success ? 'success' : 'error');
          this.loadOffers();
          this.closeActionModal();
        },
        error: (err) => {
          this.processingOfferId = null;
          const errResult = err.error as OfferActionResult;
          this.showResultToast(errResult?.message || errResult?.reason || err.message || 'Error inesperado', 'error');
          this.loadOffers();
          this.closeActionModal();
        }
      });
    };

    this.showActionModal = true;
  }

  syncTiendaNube(offer: Offer) {
    this.processingOfferId = offer.offerId!;
    this.offerService.syncTiendaNube(offer.offerId!).subscribe({
      next: (result: OfferActionResult) => {
        this.processingOfferId = null;
        this.showResultToast(result.message || 'Sincronizado', result.success ? 'success' : 'error');
        this.loadOffers();
      },
      error: (err) => {
        this.processingOfferId = null;
        const errResult = err.error as OfferActionResult;
        this.showResultToast(errResult?.message || 'Error al sincronizar TN', 'error');
        this.loadOffers();
      }
    });
  }

  syncMercadoLibre(offer: Offer) {
    this.processingOfferId = offer.offerId!;
    this.offerService.syncMercadoLibre(offer.offerId!).subscribe({
      next: (result: OfferActionResult) => {
        this.processingOfferId = null;
        this.showResultToast(result.message || 'Sincronizado', result.success ? 'success' : 'error');
        this.loadOffers();
      },
      error: (err) => {
        this.processingOfferId = null;
        const errResult = err.error as OfferActionResult;
        this.showResultToast(errResult?.message || 'Error al sincronizar ML', 'error');
        this.loadOffers();
      }
    });
  }

  confirmAction() {
    if (this.actionCallback) {
      this.actionCallback();
    }
  }

  closeActionModal() {
    this.showActionModal = false;
    this.actionCallback = null;
  }

  getStatusLabel(status?: string): string {
    switch (status) {
      case 'DRAFT': return 'Borrador';
      case 'ACTIVE': return 'Activa';
      case 'OUT_OF_SYNC': return 'Desincronizada';
      case 'ERROR': return 'Error';
      case 'REMOVED': return 'Removida';
      case 'ENDED': return 'Finalizada';
      default: return 'Borrador';
    }
  }

  getStatusClass(status?: string): string {
    switch (status) {
      case 'ACTIVE': return 'status-active';
      case 'ERROR': return 'status-error';
      case 'OUT_OF_SYNC': return 'status-warning';
      case 'REMOVED': return 'status-removed';
      case 'ENDED': return 'status-ended';
      default: return 'status-draft';
    }
  }

  showResultToast(message: string, type: 'success' | 'error') {
    this.toastMessage = message;
    this.toastType = type;
    this.showToast = true;
    setTimeout(() => {
      this.showToast = false;
    }, 5000);
  }
}
