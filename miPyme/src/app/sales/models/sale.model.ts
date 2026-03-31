export interface SaleItem {
    saleItemId: number;
    productId?: number;
    productVariantId?: number;
    productName?: string;
    variantSku?: string;
    internalCode?: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
}

export interface Sale {
    saleId: number;
    saleDate: string;
    totalAmount: number;
    facturado: boolean;
    createdBy?: string;
    medioPago?: string;
    items: SaleItem[];
}

export interface CreateSaleItemRequest {
    productId?: number;
    productVariantId?: number;
    quantity: number;
}

export interface CreateSaleRequest {
    items: CreateSaleItemRequest[];
    cajaId?: number;
    medioPago?: string;
    emitInvoice?: boolean;
    docType?: 'FISCAL' | 'NON_FISCAL';
    invoiceFormat?: 'TICKET' | 'INVOICE';
}

export interface SalesMetricPoint {
    label: string;
    totalAmount: number;
    orderCount: number;
    unitCount: number;
}

export interface SalesMetricsResponse {
    currentPeriod: SalesMetricPoint[];
    previousPeriod: SalesMetricPoint[];
    periodType: string;
}
