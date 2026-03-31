package com.mipyme.company;


public class PlanLimitsResponse {

    private String planTier;
    private Limits limits;
    private Usage usage;

    public PlanLimitsResponse() {}

    public PlanLimitsResponse(String planTier, Limits limits, Usage usage) {
        this.planTier = planTier;
        this.limits = limits;
        this.usage = usage;
    }

    public String getPlanTier() { return planTier; }
    public void setPlanTier(String planTier) { this.planTier = planTier; }

    public Limits getLimits() { return limits; }
    public void setLimits(Limits limits) { this.limits = limits; }

    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }



    public static class Limits {
        private int maxUsers;
        private int maxSucursales;
        private int maxDepositos;
        private int maxCajas;

        public Limits() {}

        public Limits(int maxUsers, int maxSucursales, int maxDepositos, int maxCajas) {
            this.maxUsers = maxUsers;
            this.maxSucursales = maxSucursales;
            this.maxDepositos = maxDepositos;
            this.maxCajas = maxCajas;
        }

        public int getMaxUsers() { return maxUsers; }
        public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }

        public int getMaxSucursales() { return maxSucursales; }
        public void setMaxSucursales(int maxSucursales) { this.maxSucursales = maxSucursales; }

        public int getMaxDepositos() { return maxDepositos; }
        public void setMaxDepositos(int maxDepositos) { this.maxDepositos = maxDepositos; }

        public int getMaxCajas() { return maxCajas; }
        public void setMaxCajas(int maxCajas) { this.maxCajas = maxCajas; }
    }

    public static class Usage {
        private long activeUsers;
        private long activeSucursales;
        private long activeDepositos;
        private long activeCajas;

        public Usage() {}

        public Usage(long activeUsers, long activeSucursales, long activeDepositos, long activeCajas) {
            this.activeUsers = activeUsers;
            this.activeSucursales = activeSucursales;
            this.activeDepositos = activeDepositos;
            this.activeCajas = activeCajas;
        }

        public long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

        public long getActiveSucursales() { return activeSucursales; }
        public void setActiveSucursales(long activeSucursales) { this.activeSucursales = activeSucursales; }

        public long getActiveDepositos() { return activeDepositos; }
        public void setActiveDepositos(long activeDepositos) { this.activeDepositos = activeDepositos; }

        public long getActiveCajas() { return activeCajas; }
        public void setActiveCajas(long activeCajas) { this.activeCajas = activeCajas; }
    }
}
