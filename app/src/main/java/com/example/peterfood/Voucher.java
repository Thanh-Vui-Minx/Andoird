package com.example.peterfood;

import java.util.List;
import java.util.Map;

public class Voucher {
    private String id;
    private String code;
    private boolean percent; // true => percent discount, false => fixed amount
    private int value; // percent or fixed amount
    private long expiryMillis; // 0 if none
    private boolean active;
    // transient flag to indicate this entry came from a user request (not an official voucher)
    private boolean pendingRequest = false;

    public Voucher() {}

    public Voucher(String id, String code, boolean percent, int value, long expiryMillis, boolean active) {
        this.id = id;
        this.code = code;
        this.percent = percent;
        this.value = value;
        this.expiryMillis = expiryMillis;
        this.active = active;
    }

    public boolean isPendingRequest() { return pendingRequest; }
    public void setPendingRequest(boolean pendingRequest) { this.pendingRequest = pendingRequest; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public boolean isPercent() { return percent; }
    public void setPercent(boolean percent) { this.percent = percent; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public long getExpiryMillis() { return expiryMillis; }
    public void setExpiryMillis(long expiryMillis) { this.expiryMillis = expiryMillis; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
