package com.mipyme.mercadopago.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "mp_chargeback")
public class MpChargeback extends MpBaseDispute {
}
