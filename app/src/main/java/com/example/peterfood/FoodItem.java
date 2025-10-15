package com.example.peterfood;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodItem {
    private String documentId;
    private String name;
    private String description; // Mô tả ngắn (trên menu)
    private int price; // Giá gốc (có thể dùng làm giá Small)
    private String imageUrl;
    private int rating;
    private Integer salePrice; // Giá giảm (tùy chọn)

    // Thêm các trường mới
    private String detailedDescription; // Mô tả chi tiết
    private Map<String, Integer> sizePrices; // Map chứa size và giá: "Small", "Medium", "Large"
    private List<Map<String, Object>> comments; // Danh sách bình luận: {userId, comment, approved}

    public FoodItem(String documentId, String name, String description, int price, String imageUrl, int rating, Integer salePrice) {
        this.documentId = documentId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.salePrice = salePrice;
        this.detailedDescription = ""; // Mặc định rỗng
        this.sizePrices = new HashMap<>(); // Khởi tạo với giá mặc định
        this.sizePrices.put("Small", price); // Giá Small mặc định là price
        this.sizePrices.put("Medium", (int) (price * 1.2)); // Medium tăng 20%
        this.sizePrices.put("Large", (int) (price * 1.5)); // Large tăng 50%
        this.comments = new ArrayList<>(); // Khởi tạo danh sách bình luận
    }

    // Getters và Setters
    public String getDocumentId() { return documentId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public int getRating() { return rating; }
    public Integer getSalePrice() { return salePrice; }
    public String getDetailedDescription() { return detailedDescription != null ? detailedDescription : ""; }
    public void setDetailedDescription(String detailedDescription) { this.detailedDescription = detailedDescription; }
    public Map<String, Integer> getSizePrices() { return sizePrices; }
    public void setSizePrices(Map<String, Integer> sizePrices) { this.sizePrices = sizePrices; }
    public List<Map<String, Object>> getComments() { return comments; }
    public void setComments(List<Map<String, Object>> comments) { this.comments = comments; }
    public int getFinalPrice() { return salePrice != null && salePrice < price ? salePrice : price; } // Giữ logic giá gốc hoặc giảm giá
}