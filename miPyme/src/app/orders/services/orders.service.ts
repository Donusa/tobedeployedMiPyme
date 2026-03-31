import { Injectable } from '@angular/core';
import { Observable, forkJoin, of, map, catchError, switchMap } from 'rxjs';
import { Order, OrderStatus } from '../models/order.model';
import { Product } from '../../models/stock.models';
import { MercadoLibreService } from '../../services/mercadolibre.service';
import { TiendaNubeService } from '../../services/tienda-nube.service';
import { SalesService } from '../../sales/services/sales.service';

@Injectable({
    providedIn: 'root'
})
export class OrdersService {

    constructor(
        private mlService: MercadoLibreService,
        private tnService: TiendaNubeService,
        private salesService: SalesService
    ) { }

    getAll(): Observable<Order[]> {
        return forkJoin({
            ml: this.mlService.getOrders().pipe(catchError(() => of([]))),
            tn: this.tnService.getOrders().pipe(catchError(() => of([]))),
            local: this.salesService.getAll().pipe(catchError(() => of([])))
        }).pipe(
            map(({ ml, tn, local }) => {
                const orders: Order[] = [];


                ml.forEach((o: any) => {

                    const rawItems = o.items || [];


                    const mappedItems = rawItems.map((i: any) => {
                        return {
                            id: 0,
                            productName: i.title || 'Producto desconocido',
                            quantity: i.quantity || 1,
                            unitPrice: i.unitPrice || 0,
                            subtotal: (i.quantity || 1) * (i.unitPrice || 0),
                            productId: i.id
                        };
                    });


                    const calculatedTotal = mappedItems.reduce((acc: number, item: any) => acc + item.subtotal, 0);
                    const orderTotal = (o.totalAmount || o.total_amount || 0) > 0 ? (o.totalAmount || o.total_amount) : calculatedTotal;

                    orders.push({
                        id: o.orderId || o.id,
                        date: o.dateCreated || o.date_created,
                        customer: {
                            name: o.buyerNickname || (o.buyer?.first_name || '') + ' ' + (o.buyer?.last_name || ''),
                            email: o.buyerEmail || o.buyer?.email,
                            docNumber: o.buyer?.billing_info?.doc_number
                        },
                        items: mappedItems,
                        total: orderTotal,
                        status: this.mapMlStatus(o.orderStatus || o.status, o.shippingStatus),
                        source: 'mercadolibre',
                        externalId: (o.orderId || o.id || '').toString(),
                        paymentMethod: 'MercadoPago',
                        shippingMethod: 'MercadoEnvíos',
                        platformShipmentId: (o.shippingId || o.shipping?.id || '').toString(),
                        sellerName: 'MercadoLibre'
                    } as any);
                });


                tn.forEach((o: any) => {

                    const mappedItems = (o.products || []).map((p: any) => ({
                        id: 0,
                        productName: p.name || 'Producto desconocido',
                        quantity: p.quantity || 1,
                        unitPrice: p.price || 0,
                        subtotal: (p.price || 0) * (p.quantity || 1),
                        productId: p.product_id || p.productId
                    }));


                    const calculatedTotal = mappedItems.reduce((acc: number, item: any) => acc + item.subtotal, 0);
                    const orderTotal = (o.total || 0) > 0 ? o.total : calculatedTotal;


                    let customerName = o.customer?.name;
                    if (!customerName && (o.customer?.first_name || o.customer?.last_name)) {
                        customerName = ((o.customer?.first_name || '') + ' ' + (o.customer?.last_name || '')).trim();
                    }
                    if (!customerName) customerName = 'Cliente TiendaNube';


                    let address = o.shipping_address?.address || '';
                    if (o.shipping_address?.number) address += ' ' + o.shipping_address.number;
                    if (o.shipping_address?.city) address += ', ' + o.shipping_address.city;

                    orders.push({
                        id: o.id,
                        date: o.created_at,
                        customer: {
                            name: customerName,
                            email: o.contact_email || o.customer?.email,
                            address: address,
                            phone: o.shipping_address?.phone || o.customer?.phone
                        },
                        items: mappedItems,
                        total: orderTotal,
                        status: this.mapTnStatus(o.status, o.shipping_status),
                        source: 'tiendanube',
                        externalId: (o.number || o.id || '').toString(),
                        paymentMethod: o.payment_details?.method || 'N/A',
                        paymentStatus: o.payment_status,
                        shippingMethod: o.shipping_option || 'N/A',
                        sellerName: 'TiendaNube'
                    } as any);
                });


                local.forEach((s: any) => {
                    orders.push({
                        id: s.saleId,
                        date: s.saleDate,
                        customer: { name: 'Cliente Local' },
                        items: s.items.map((i: any) => ({
                            id: i.saleItemId,
                            productName: i.product?.productName || 'Producto',
                            quantity: i.quantity,
                            unitPrice: i.unitPrice,
                            subtotal: i.subtotal,
                            productId: i.product?.productId
                        })),
                        total: s.totalAmount,
                        status: 'DELIVERED',
                        source: 'local',
                        paymentMethod: 'Mostrador',
                        paymentStatus: 'paid',
                        sellerName: s.createdBy || 'Sistema'
                    } as any);
                });

                return orders;
            })
        );
    }


    markPaid(order: Order): Observable<boolean> {
        console.log('OrdersService: markPaid called for order', order.id, 'Source:', order.source);
        if (order.source === 'tiendanube') {
            return this.tnService.markOrderAsPaid(order.id).pipe(
                map(() => true),
                catchError((err) => {
                    console.error('OrdersService: Error marking as paid', err);
                    return of(false);
                })
            );
        }
        return of(false);
    }

    private mapMlStatus(status: string, shippingStatus?: string): OrderStatus {
        if (shippingStatus) {
            const s = shippingStatus.toLowerCase();
            if (s === 'shipped' || s === 'dispatched') return 'SHIPPED';
            if (s === 'delivered') return 'DELIVERED';
            if (s === 'packed') return 'PACKED';

            if (s === 'ready_to_ship') return 'PACKED';
        }

        switch (status) {
            case 'paid': return 'RECEIVED';
            case 'confirmed': return 'RECEIVED';
            case 'payment_required': return 'RECEIVED';
            case 'payment_in_process': return 'RECEIVED';
            case 'partially_paid': return 'RECEIVED';

            case 'shipped': return 'SHIPPED';
            case 'delivered': return 'DELIVERED';
            case 'cancelled': return 'CANCELLED';
            default: return 'RECEIVED';
        }
    }

    private mapTnStatus(status: string, shippingStatus?: string): OrderStatus {
        if (shippingStatus) {
            const s = shippingStatus.toLowerCase();
            if (s === 'shipped' || s === 'dispatched') return 'SHIPPED';
            if (s === 'delivered') return 'DELIVERED';
            if (s === 'packed') return 'PACKED';
        }

        switch (status?.toLowerCase()) {
            case 'open': return 'RECEIVED';
            case 'closed': return 'COMPLETED';
            case 'packed': return 'PACKED';
            case 'shipped': return 'SHIPPED';
            case 'delivered': return 'DELIVERED';
            case 'cancelled': return 'CANCELLED';
            default: return 'RECEIVED';
        }
    }

    getById(id: number): Observable<Order | undefined> {


        return this.getAll().pipe(
            map(orders => orders.find(o => o.id == id))
        );
    }

    create(order: Order): Observable<Order> {

        const saleRequest = {
            items: order.items.map(i => ({
                productId: i.productId,
                quantity: i.quantity,





            }))
        };


        return of(order);


    }

    updateStatus(id: number, status: OrderStatus, tracking?: { code: string, url: string }): Observable<boolean> {
        return this.getById(id).pipe(
            switchMap(order => {
                if (!order) return of(false);

                let action$: Observable<any> = of(true);

                if (order.source === 'tiendanube' && order.externalId) {


                    action$ = this.tnService.getFulfillmentOrders(order.id).pipe(
                        switchMap(fulfillments => {
                            if (!fulfillments || fulfillments.length === 0) {

                                console.warn('No fulfillment found for TN order', order.id);
                                return of(false);
                            }

                            const ffId = fulfillments[0].id;

                            let tnStatus = '';
                            let body: any = {};

                            if (status === 'PACKED') tnStatus = 'PACKED';
                            if (status === 'SHIPPED') {
                                tnStatus = 'DISPATCHED';
                                if (tracking) {
                                    body.tracking_number = tracking.code;
                                    body.tracking_url = tracking.url;
                                    body.notify_customer = true;
                                }
                            }
                            if (status === 'DELIVERED') tnStatus = 'DELIVERED';

                            if (!tnStatus) return of(true);

                            body.status = tnStatus;
                            return this.tnService.updateFulfillmentOrder(order.id, ffId, body).pipe(
                                map(() => true),
                                catchError(err => {
                                    console.error('TN Fulfillment Update Error', err);
                                    return of(false);
                                })
                            );
                        }),
                        catchError(err => {
                            console.error('TN Get Fulfillments Error', err);
                            return of(false);
                        })
                    );



                    if (status === 'DELIVERED') {
                        action$ = action$.pipe(
                            switchMap((success) => {
                                if (!success) return of(false);
                                return this.tnService.closeOrder(order.id).pipe(
                                    map(() => true),
                                    catchError(() => of(true))
                                );
                            })
                        );
                    }

                } else if (order.source === 'mercadolibre') {



                    let platformUpdate$: Observable<any> = of(true);

                    if (order.platformShipmentId) {
                        const shipmentId = Number(order.platformShipmentId);
                        if (status === 'SHIPPED' && tracking) {

                            platformUpdate$ = this.mlService.notifySeller(shipmentId, tracking.code, tracking.url).pipe(
                                catchError(() => {

                                    return this.mlService.updateShipment(shipmentId, 'shipped', { number: tracking.code, url: tracking.url });
                                }),
                                catchError(() => of(true))
                            );
                        } else if (status === 'DELIVERED') {
                            platformUpdate$ = this.mlService.updateShipment(shipmentId, 'delivered').pipe(
                                catchError(() => of(true))
                            );
                        }
                    }

                    action$ = platformUpdate$;
                }

                return action$.pipe(
                    map(() => true),
                    catchError(err => {
                        console.error('Error updating platform status', err);
                        return of(false);
                    })
                );
            })
        );
    }


    markPacked(order: Order): Observable<boolean> {
        return this.updateStatus(order.id, 'PACKED');
    }

    markShipped(order: Order, tracking?: { code: string, url: string }): Observable<boolean> {
        return this.updateStatus(order.id, 'SHIPPED', tracking);
    }

    markDelivered(order: Order): Observable<boolean> {
        return this.updateStatus(order.id, 'DELIVERED');
    }

    revertStatus(order: Order): Observable<boolean> {
        let prevStatus: OrderStatus = 'RECEIVED';
        if (order.status === 'DELIVERED') prevStatus = 'SHIPPED';
        else if (order.status === 'SHIPPED') prevStatus = 'PACKED';
        else if (order.status === 'PACKED') prevStatus = 'RECEIVED';
        else return of(false);

        if (order.source === 'mercadolibre') {
            return this.mlService.updateLocalStatus(order.externalId!, prevStatus).pipe(
                map(() => true),
                catchError(() => of(false))
            );
        } else if (order.source === 'tiendanube') {

            return this.tnService.updateLocalStatus(order.id, prevStatus).pipe(
                map(() => true),
                catchError(() => of(false))
            );
        }
        return of(false);
    }
}
