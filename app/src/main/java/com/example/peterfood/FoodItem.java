package com.example.peterfood;

public class FoodItem {
    private String documentId;
    private String name;
    private String description;
    private int price;
    private String imageUrl;
    private int rating;
    private Integer salePrice; // Kiểu Integer để hỗ trợ null

    public FoodItem() {
    }

    public FoodItem(String documentId, String name, String description, int price, String imageUrl, int rating, Integer salePrice) {
        this.documentId = documentId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.salePrice = salePrice;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public Integer getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(Integer salePrice) {
        this.salePrice = salePrice;
    }

    public int getFinalPrice() {
        return (salePrice != null && salePrice < price) ? salePrice : price;
    }
}