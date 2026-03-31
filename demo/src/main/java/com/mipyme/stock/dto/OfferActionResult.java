package com.mipyme.stock.dto;

import com.mipyme.stock.model.Channel;
import com.mipyme.stock.model.OfferStatus;

public class OfferActionResult {
    private boolean success;
    private Long offerId;
    private Channel channel;
    private OfferStatus status;
    private String message;
    private String reason;

    public OfferActionResult() {
    }

    public static OfferActionResult ok(Long offerId, Channel channel, OfferStatus status, String message) {
        OfferActionResult r = new OfferActionResult();
        r.success = true;
        r.offerId = offerId;
        r.channel = channel;
        r.status = status;
        r.message = message;
        return r;
    }

    public static OfferActionResult fail(Long offerId, Channel channel, String reason) {
        OfferActionResult r = new OfferActionResult();
        r.success = false;
        r.offerId = offerId;
        r.channel = channel;
        r.reason = reason;
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public OfferStatus getStatus() {
        return status;
    }

    public void setStatus(OfferStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
