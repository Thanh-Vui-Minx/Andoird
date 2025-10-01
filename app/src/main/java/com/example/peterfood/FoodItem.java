package com.example.peterfood;

public class FoodItem {
    private String documentId;
    private String name;
    private String description;
    private int price;
    private String imageUrl;
    private int rating;

    public FoodItem(String documentId, String name, String description, int price, String imageUrl, int rating) {
        this.documentId = documentId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.rating = rating;
    }

    public String getDocumentId() { return documentId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public int getRating() { return rating; }
}