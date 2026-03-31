package com.mipyme.stock.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "measurement_units")
public class MeasurementUnit {

    @Id
    @Column(name = "unit_code", length = 10)
    private String unitCode;

    @Column(name = "unit_name", nullable = false)
    private String unitName;

    protected MeasurementUnit() {
    }

    public MeasurementUnit(String unitCode, String unitName) {
        this.unitCode = unitCode;
        this.unitName = unitName;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }
}
