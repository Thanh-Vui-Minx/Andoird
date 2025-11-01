package com.example.peterfood;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Order {
    private final String id;
    private final Date createdAt;
    private final long total;
    private final String paymentMethod;
    private final String paymentStatus;
    private final String address;
    private final String bankRef;
    private final List<Map<String, Object>> items;

    public Order(String id, Date createdAt, long total, String paymentMethod, String paymentStatus, String address, String bankRef, List<Map<String, Object>> items) {
        this.id = id;
        this.createdAt = createdAt;
        this.total = total;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.address = address;
        this.bankRef = bankRef;
        this.items = items;
    }

    public String getId() { return id; }
    public Date getCreatedAt() { return createdAt; }
    public long getTotal() { return total; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getAddress() { return address; }
    public String getBankRef() { return bankRef; }
    public List<Map<String, Object>> getItems() { return items; }
}
