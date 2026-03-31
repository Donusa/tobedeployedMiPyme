import { Product, ProductVariant } from '../../models/stock.models';

export type OrderStatus = 'RECEIVED' | 'PACKED' | 'SHIPPED' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED';
export type OrderSource = 'local' | 'mercadolibre' | 'tiendanube';

export interface OrderItem {
    id: number;
    productId?: number;
    productVariantId?: number;
    productName: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
    product?: Product;
    variant?: ProductVariant;
}

export interface AuditLogEntry {
    timestamp: string;
    action: string;
    user: string;
    details?: string;
    platformResponse?: any;
}

export interface TrackingInfo {
    code: string;
    url?: string;
    carrier?: string;
}

export interface Order {
    id: number;
    date: string;
    customer: {
        name: string;
        email?: string;
        phone?: string;
        address?: string;
        docNumber?: string;
    };
    items: OrderItem[];
    total: number;
    status: OrderStatus;
    source: OrderSource;


    externalId?: string;
    platformShipmentId?: string;
    platformStatus?: string;
    lastSyncAt?: string;

    notes?: string;
    paymentMethod?: string;
    paymentStatus?: string;
    shippingMethod?: string;
    trackingInfo?: TrackingInfo;
    sellerName?: string;

    auditLog?: AuditLogEntry[];
}
