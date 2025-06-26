package com.seyman.dreamshops.requests;

import com.seyman.dreamshops.model.Category;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductUpdateRequest {
    private Long id;
    private String name;
    private String brand;
    private BigDecimal price;
    private int inventory;
    private String description;
    private Category category;
    
    // Discount fields
    private BigDecimal discountPrice;
    private Integer discountPercentage;
    private Boolean isOnSale;
    private Boolean isFlashSale;
    private LocalDateTime saleStartDate;
    private LocalDateTime saleEndDate;
    private Integer flashSaleStock;
}
