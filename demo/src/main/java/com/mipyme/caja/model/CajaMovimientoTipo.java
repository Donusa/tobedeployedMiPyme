package com.mipyme.caja.model;

public enum CajaMovimientoTipo {

    VENTA,
    COBRO_CUENTA_CORRIENTE,
    INGRESO_MANUAL,
    REPOSICION_CAMBIO,
    AJUSTE_POSITIVO,


    GASTO_OPERATIVO,
    PAGO_PROVEEDOR,
    RETIRO_EFECTIVO,
    DEVOLUCION_CLIENTE,
    AJUSTE_NEGATIVO,


    APERTURA,
    CIERRE,
    ANULACION,
    REVERSO,
    RELEVO;

    public boolean esIngreso() {
        return switch (this) {
            case VENTA, COBRO_CUENTA_CORRIENTE, INGRESO_MANUAL, REPOSICION_CAMBIO,
                 AJUSTE_POSITIVO, APERTURA -> true;
            default -> false;
        };
    }

    public boolean esEgreso() {
        return switch (this) {
            case GASTO_OPERATIVO, PAGO_PROVEEDOR, RETIRO_EFECTIVO,
                 DEVOLUCION_CLIENTE, AJUSTE_NEGATIVO -> true;
            default -> false;
        };
    }

    public boolean esEspecial() {
        return switch (this) {
            case APERTURA, CIERRE, ANULACION, REVERSO, RELEVO -> true;
            default -> false;
        };
    }
}
