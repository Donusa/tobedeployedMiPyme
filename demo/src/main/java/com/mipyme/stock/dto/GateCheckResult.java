package com.mipyme.stock.dto;

public class GateCheckResult {
    private boolean ok;
    private String reason;

    public GateCheckResult() {
    }

    public static GateCheckResult pass() {
        GateCheckResult r = new GateCheckResult();
        r.ok = true;
        return r;
    }

    public static GateCheckResult block(String reason) {
        GateCheckResult r = new GateCheckResult();
        r.ok = false;
        r.reason = reason;
        return r;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
