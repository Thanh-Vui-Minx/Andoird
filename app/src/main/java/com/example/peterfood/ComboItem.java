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
    private boolean isLimitedTime;  // Combo giới hạn thời gian
    private Long startDate;  // Timestamp bắt đầu (milliseconds)
    private Long endDate;    // Timestamp kết thúc (milliseconds)
    private boolean active;  // Trạng thái hoạt động
    private Long createdAt;  // Thời gian tạo

    // Constructor mặc định (cho Firestore)
    public ComboItem() {
        this.active = true;
        this.isLimitedTime = false;
    }

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
        this.active = true;
        this.isLimitedTime = false;
        this.createdAt = System.currentTimeMillis();
    }

    // Constructor đầy đủ với limited time
    public ComboItem(String id, String name, String description, int comboPrice, int originalPrice,
                     String imageUrl, List<String> foodIds, Map<String, Integer> quantities,
                     boolean isLimitedTime, Long startDate, Long endDate) {
        this(id, name, description, comboPrice, originalPrice, imageUrl, foodIds, quantities);
        this.isLimitedTime = isLimitedTime;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Kiểm tra combo còn hợp lệ
    public boolean isValid() {
        if (!active) return false;
        if (!isLimitedTime) return true;
        
        long now = System.currentTimeMillis();
        if (startDate != null && now < startDate) return false;
        if (endDate != null && now > endDate) return false;
        return true;
    }

    // Tính thời gian còn lại (ms)
    public long getRemainingTime() {
        if (!isLimitedTime || endDate == null) return -1;
        return Math.max(0, endDate - System.currentTimeMillis());
    }

    // Format thời gian còn lại
    public String getFormattedRemainingTime() {
        long remaining = getRemainingTime();
        if (remaining <= 0) return "Đã hết hạn";
        
        long days = remaining / (24 * 60 * 60 * 1000);
        long hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);
        
        if (days > 0) return days + " ngày " + hours + " giờ";
        if (hours > 0) return hours + " giờ " + minutes + " phút";
        return minutes + " phút";
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

    public boolean isLimitedTime() { return isLimitedTime; }
    public void setLimitedTime(boolean limitedTime) { isLimitedTime = limitedTime; }

    public Long getStartDate() { return startDate; }
    public void setStartDate(Long startDate) { this.startDate = startDate; }

    public Long getEndDate() { return endDate; }
    public void setEndDate(Long endDate) { this.endDate = endDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public int getSavings() { return originalPrice - comboPrice; }
}