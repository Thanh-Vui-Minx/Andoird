package com.example.peterfood;

import java.io.Serializable;

public class CartItem implements Serializable {
    private String name;
    private int price;
    private int quantity;
    private String imageUrl;
    private String note; // Ghi chú

    public CartItem() {}

    public CartItem(String name, int price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = "";
        this.note = "";
    }

    public CartItem(String name, int price, int quantity, String imageUrl) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl != null ? imageUrl : "";
        this.note = "";
    }

    // THÊM CONSTRUCTOR MỚI – 5 THAM SỐ
    public CartItem(String name, int price, int quantity, String imageUrl, String note) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl != null ? imageUrl : "";
        this.note = note != null ? note : "";
    }

    // Getter & Setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl != null ? imageUrl : ""; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note != null ? note : ""; }

    public int getTotalPrice() {
        return price * quantity;
    }
}