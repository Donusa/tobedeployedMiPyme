import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Sale } from '../../models/sale.model';

@Component({
  selector: 'app-sale-detail-modal',
  templateUrl: './sale-detail-modal.component.html',
  styleUrls: ['./sale-detail-modal.component.css']
})
export class SaleDetailModalComponent {
  @Input() sale!: Sale;
  @Output() close = new EventEmitter<void>();
}
