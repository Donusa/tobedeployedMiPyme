import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'app-invoice-preview-modal',
    templateUrl: './invoice-preview-modal.component.html',
    styleUrls: ['./invoice-preview-modal.component.css']
})
export class InvoicePreviewModalComponent {
    @Input() saleData: any;
    @Input() docType: 'FISCAL' | 'NON_FISCAL' = 'FISCAL';
    @Output() confirm = new EventEmitter<'TICKET' | 'INVOICE'>();
    @Output() cancel = new EventEmitter<void>();

    selectedFormat: 'TICKET' | 'INVOICE' = 'TICKET';

    today = new Date();

    selectFormat(format: 'TICKET' | 'INVOICE') {
        this.selectedFormat = format;
    }

    onConfirm() {
        this.confirm.emit(this.selectedFormat);
    }

    onCancel() {
        this.cancel.emit();
    }
}
