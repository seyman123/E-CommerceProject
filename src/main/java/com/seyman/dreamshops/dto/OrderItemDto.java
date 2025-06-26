package com.seyman.dreamshops.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDto {
    private Long productId;
    private String productName;
    private String productBrand;
    private String productImageUrl;
    private String productCategory;
    private int quantity;
    private BigDecimal price;
}
