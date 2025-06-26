package com.seyman.dreamshops.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    private CouponType type;
    
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;
    
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    
    private Integer usageLimit;
    private Integer usedCount = 0;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    private Boolean isActive = true;

    public enum CouponType {
        WELCOME, FLASH_SALE, STUDENT, MEGA_DISCOUNT, CATEGORY_SPECIFIC, GENERAL
    }

    public enum DiscountType {
        PERCENTAGE, FIXED_AMOUNT
    }

    public Coupon(String code, String description, CouponType type, DiscountType discountType, 
                  BigDecimal discountValue, BigDecimal minOrderAmount, LocalDateTime endDate) {
        this.code = code;
        this.description = description;
        this.type = type;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.endDate = endDate;
        this.startDate = LocalDateTime.now();
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return isActive && 
               (startDate == null || now.isAfter(startDate)) &&
               (endDate == null || now.isBefore(endDate)) &&
               (usageLimit == null || usedCount < usageLimit);
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isValid() || orderAmount.compareTo(minOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (discountType == DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(discountValue).divide(BigDecimal.valueOf(100));
        } else {
            discount = discountValue;
        }

        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }

        return discount;
    }
} 