package com.example.peterfood;

import java.util.List;
import java.util.Map;

public class ComboItem {
    private String id;
    private String name;
    private String description;
    private int comboPrice;
    private int originalPrice;
    private String imageUrl;
    private List<String> foodIds;
    private Map<String, Integer> quantities;  // Map<docId, số lượng>

    // Constructor mặc định (cho Firestore)
    public ComboItem() {}

    public ComboItem(String id, String name, String description, int comboPrice, int originalPrice,
                     String imageUrl, List<String> foodIds, Map<String, Integer> quantities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.comboPrice = comboPrice;
        this.originalPrice = originalPrice;
        this.imageUrl = imageUrl;
        this.foodIds = foodIds;
        this.quantities = quantities;
    }

    // Getter và Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getComboPrice() { return comboPrice; }
    public void setComboPrice(int comboPrice) { this.comboPrice = comboPrice; }

    public int getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(int originalPrice) { this.originalPrice = originalPrice; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getFoodIds() { return foodIds; }
    public void setFoodIds(List<String> foodIds) { this.foodIds = foodIds; }

    public Map<String, Integer> getQuantities() { return quantities; }
    public void setQuantities(Map<String, Integer> quantities) { this.quantities = quantities; }
}