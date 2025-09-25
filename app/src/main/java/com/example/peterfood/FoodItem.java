package com.example.peterfood;

public class FoodItem {
    private String name;
    private String description;
    private int price;
    private int rating;

    public FoodItem(String name, String description, int price, int rating) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.rating = rating;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPrice() { return price; }
    public int getRating() { return rating; }
}