package com.seyman.dreamshops.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String brand;
    private BigDecimal price;
    private int inventory;
    private String description;

    // Discount fields
    private BigDecimal discountPrice;
    private Integer discountPercentage;
    private Boolean isOnSale = false;
    private Boolean isFlashSale = false;
    private LocalDateTime saleStartDate;
    private LocalDateTime saleEndDate;
    private Integer flashSaleStock;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @JsonManagedReference
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images;

    public Product(String name, String brand, int inventory, BigDecimal price, String description, Category category) {
        this.name = name;
        this.brand = brand;
        this.inventory = inventory;
        this.price = price;
        this.description = description;
        this.category = category;
    }

    // Helper methods for discount calculations
    public BigDecimal getEffectivePrice() {
        if (!Boolean.TRUE.equals(isOnSale)) {
            return price;
        }
        
        // If discountPrice is set, use it directly
        if (discountPrice != null) {
            return discountPrice;
        }
        
        // If discountPercentage is set, calculate from percentage
        if (discountPercentage != null && discountPercentage > 0) {
            BigDecimal discountAmount = price.multiply(BigDecimal.valueOf(discountPercentage))
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            return price.subtract(discountAmount);
        }
        
        return price;
    }

    public BigDecimal getSavings() {
        if (!Boolean.TRUE.equals(isOnSale)) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal effectivePrice = getEffectivePrice();
        return price.subtract(effectivePrice);
    }

    public boolean isCurrentlyOnSale() {
        // Debug logs removed for production
        
        if (!Boolean.TRUE.equals(isOnSale)) {
            // Debug logs removed for production
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        // Debug logs removed for production
        
        if (saleStartDate != null && now.isBefore(saleStartDate)) {
            // Debug logs removed for production
            return false;
        }
        
        if (saleEndDate != null && now.isAfter(saleEndDate)) {
            // Debug logs removed for production
            return false;
        }
        
        // Debug logs removed for production
        return true;
    }
}
