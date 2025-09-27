package com.example.peterfood;

public class FoodItem {
    private String name;
    private String description;
    private int price;
    private String imageUrl;
    private int rating;

    public FoodItem(String name, String description, int price, String imageUrl, int rating) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.rating = rating;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public int getRating() { return rating; }
}