package com.mipyme.sales.dto;

import java.math.BigDecimal;
import java.util.List;

public class SalesMetricsResponse {
    private List<SalesMetricPoint> currentPeriod;
    private List<SalesMetricPoint> previousPeriod;
    private String periodType;

    public SalesMetricsResponse() {}

    public SalesMetricsResponse(List<SalesMetricPoint> currentPeriod, List<SalesMetricPoint> previousPeriod, String periodType) {
        this.currentPeriod = currentPeriod;
        this.previousPeriod = previousPeriod;
        this.periodType = periodType;
    }

    public List<SalesMetricPoint> getCurrentPeriod() {
        return currentPeriod;
    }

    public void setCurrentPeriod(List<SalesMetricPoint> currentPeriod) {
        this.currentPeriod = currentPeriod;
    }

    public List<SalesMetricPoint> getPreviousPeriod() {
        return previousPeriod;
    }

    public void setPreviousPeriod(List<SalesMetricPoint> previousPeriod) {
        this.previousPeriod = previousPeriod;
    }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public static class SalesMetricPoint {
        private String label;
        private BigDecimal totalAmount;
        private int orderCount;
        private int unitCount;

        public SalesMetricPoint() {}

        public SalesMetricPoint(String label, BigDecimal totalAmount, int orderCount, int unitCount) {
            this.label = label;
            this.totalAmount = totalAmount;
            this.orderCount = orderCount;
            this.unitCount = unitCount;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public int getOrderCount() {
            return orderCount;
        }

        public void setOrderCount(int orderCount) {
            this.orderCount = orderCount;
        }

        public int getUnitCount() {
            return unitCount;
        }

        public void setUnitCount(int unitCount) {
            this.unitCount = unitCount;
        }
    }
}
