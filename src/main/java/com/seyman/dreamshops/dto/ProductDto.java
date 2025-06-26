package com.seyman.dreamshops.dto;

import com.seyman.dreamshops.model.Category;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDto {
    private Long id;
    private String name;
    private String brand;
    private BigDecimal price;
    private int inventory;
    private String description;
    private Category category;
    private List<ImageDto> images;
    
    // Discount fields
    private BigDecimal discountPrice;
    private Integer discountPercentage;
    private Boolean isOnSale;
    private Boolean isFlashSale;
    private LocalDateTime saleStartDate;
    private LocalDateTime saleEndDate;
    private Integer flashSaleStock;
    
    // Calculated fields
    private BigDecimal effectivePrice;
    private BigDecimal savings;
    private Boolean currentlyOnSale;
}
