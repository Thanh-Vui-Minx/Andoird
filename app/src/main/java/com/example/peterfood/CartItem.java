package com.example.peterfood;

import java.io.Serializable;

public class CartItem implements Serializable {
    private String name;
    private int price;
    private int quantity;
    private String imageUrl;
    public CartItem() {}

    public CartItem(String name, int price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }

    // ✅ THÊM CONSTRUCTOR MỚI VỚI IMAGEURL
    public CartItem(String name, int price, int quantity, String imageUrl) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }


    public int getTotalPrice() {
        return price * quantity;
    }
}