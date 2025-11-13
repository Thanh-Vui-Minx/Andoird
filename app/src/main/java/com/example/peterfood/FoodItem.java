package com.example.peterfood;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodItem {
    private String documentId;
    private String name;
    private String description; // Mô tả ngắn (trên menu)
    private String tag; // "thức ăn" hoặc "nước"
    private int price; // Giá gốc (có thể dùng làm giá Small)
    private String imageUrl;
    private int rating;
    private Integer salePrice; // Giá giảm (tùy chọn)

    // Thêm các trường mới
    private String detailedDescription; // Mô tả chi tiết
    private Map<String, Long> sizePrices; // Sửa thành Map<String, Long>
    private List<Map<String, Object>> comments; // Danh sách bình luận: {userId, comment, approved}
    private boolean isCombo = false;  // Mới: Đánh dấu là combo
    private ComboItem comboData;      // Mới: Dữ liệu combo nếu là combo

    public FoodItem(String documentId, String name, String description, int price, String imageUrl, int rating, Integer salePrice) {
        this.documentId = documentId;
        this.name = name;
        this.description = description;
        this.tag = ""; // Default rỗng
        this.price = price;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.salePrice = salePrice;
        this.detailedDescription = ""; // Mặc định rỗng
        this.sizePrices = new HashMap<>(); // Khởi tạo với giá mặc định
        this.sizePrices.put("Small", (long) price); // Ép sang Long
        this.sizePrices.put("Medium", (long) (price * 1.2)); // Medium tăng 20%
        this.sizePrices.put("Large", (long) (price * 1.5)); // Large tăng 50%
        this.comments = new ArrayList<>(); // Khởi tạo danh sách bình luận
    }
    public FoodItem(ComboItem combo) {
        // Use the real combo document ID so lookups match the Firestore-loaded ComboItem.id
        this.documentId = combo.getId() != null ? combo.getId() : "";
        this.name = combo.getName() != null ? combo.getName() : "COMBO";      // Tên nổi bật
        this.description = combo.getDescription();
        this.price = combo.getComboPrice();          // Giá đã giảm
        this.salePrice = combo.getOriginalPrice();   // Giá gốc (hiển thị gạch ngang)
        this.imageUrl = combo.getImageUrl();
        this.rating = 5;  // Combo luôn 5 sao
        this.tag = "combo";  // Tag đặc biệt
        this.detailedDescription = "Combo tự động - Tiết kiệm " + (combo.getOriginalPrice() - combo.getComboPrice()) + " VNĐ";
        this.sizePrices = new HashMap<>();
        this.comments = new ArrayList<>();
        this.isCombo = true;
        this.comboData = combo;
    }
    public FoodItem() {
        this.documentId = "";
        this.name = "";
        this.description = "";
        this.tag = "";
        this.price = 0;
        this.imageUrl = "";
        this.rating = 0;
        this.salePrice = null;
        this.detailedDescription = "";
        this.sizePrices = new HashMap<>();
        this.comments = new ArrayList<>();
        this.isCombo = false;
        this.comboData = null;
    }

    // Getters và Setters
    public String getDocumentId() { return documentId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
    public int getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public int getRating() { return rating; }
    public Integer getSalePrice() { return salePrice; }
    public String getDetailedDescription() { return detailedDescription != null ? detailedDescription : ""; }
    public void setDetailedDescription(String detailedDescription) { this.detailedDescription = detailedDescription; }
    public Map<String, Long> getSizePrices() { return sizePrices; } // Sửa thành Map<String, Long>
    public void setSizePrices(Map<String, Long> sizePrices) { this.sizePrices = sizePrices; }
    public List<Map<String, Object>> getComments() { return comments; }
    public void setComments(List<Map<String, Object>> comments) { this.comments = comments; }
    public boolean isCombo() { return isCombo; }

    public void setIsCombo(boolean isCombo) { this.isCombo = isCombo; }

    public ComboItem getComboData() {  return comboData; }

    public void setComboData(ComboItem comboData) { this.comboData = comboData; }
    public int getFinalPrice() { return salePrice != null && salePrice < price ? salePrice : price; } // Giữ logic giá gốc hoặc giảm giá
}