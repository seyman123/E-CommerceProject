package com.seyman.dreamshops.controller;

import com.seyman.dreamshops.dto.ProductDto;
import com.seyman.dreamshops.model.Product;
import com.seyman.dreamshops.requests.ProductUpdateRequest;
import com.seyman.dreamshops.response.ApiResponse;
import com.seyman.dreamshops.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/sales")
public class SaleController {
    private final IProductService productService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse> getProductsOnSale() {
        try {
            List<Product> products = productService.getProductsOnSale();
            List<ProductDto> convertedProducts = productService.getConvertedProducts(products);
            return ResponseEntity.ok(new ApiResponse("Success", convertedProducts));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error", INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping("/flash-sale")
    public ResponseEntity<ApiResponse> getFlashSaleProducts() {
        try {
            List<Product> products = productService.getFlashSaleProducts();
            List<ProductDto> convertedProducts = productService.getConvertedProducts(products);
            return ResponseEntity.ok(new ApiResponse("Success", convertedProducts));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error", INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse> getProductsOnSaleByCategory(@PathVariable String category) {
        try {
            List<Product> products = productService.getProductsOnSaleByCategory(category);
            List<ProductDto> convertedProducts = productService.getConvertedProducts(products);
            return ResponseEntity.ok(new ApiResponse("Success", convertedProducts));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error", INTERNAL_SERVER_ERROR));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/product/{productId}/sale")
    public ResponseEntity<ApiResponse> putProductOnSale(@PathVariable Long productId, @RequestBody Map<String, Object> saleData) {
        try {
            // Debug logs removed for production
            
            // Map'i ProductUpdateRequest'e dönüştür
            ProductUpdateRequest request = new ProductUpdateRequest();
            
            if (saleData.containsKey("discountPrice") && saleData.get("discountPrice") != null) {
                BigDecimal discountPrice = new BigDecimal(saleData.get("discountPrice").toString());
                request.setDiscountPrice(discountPrice);
                // Debug logs removed for production
            }
            
            if (saleData.containsKey("discountPercentage") && saleData.get("discountPercentage") != null) {
                Integer discountPercentage = Integer.valueOf(saleData.get("discountPercentage").toString());
                request.setDiscountPercentage(discountPercentage);
                // Debug logs removed for production
            }
            
            if (saleData.containsKey("saleStartDate") && saleData.get("saleStartDate") != null) {
                String startDate = saleData.get("saleStartDate").toString();
                LocalDateTime startDateTime = parseDateTime(startDate);
                request.setSaleStartDate(startDateTime);
                // Debug logs removed for production
            }
            
            if (saleData.containsKey("saleEndDate") && saleData.get("saleEndDate") != null) {
                String endDate = saleData.get("saleEndDate").toString();
                LocalDateTime endDateTime = parseDateTime(endDate);
                request.setSaleEndDate(endDateTime);
                // Debug logs removed for production
            }
            
            if (saleData.containsKey("isFlashSale")) {
                Boolean isFlashSale = Boolean.valueOf(saleData.get("isFlashSale").toString());
                request.setIsFlashSale(isFlashSale);
                // Debug logs removed for production
            }
            
            if (saleData.containsKey("flashSaleStock") && saleData.get("flashSaleStock") != null) {
                Integer flashSaleStock = Integer.valueOf(saleData.get("flashSaleStock").toString());
                request.setFlashSaleStock(flashSaleStock);
                // Debug logs removed for production
            }
            
            // İndirime ekle
            request.setIsOnSale(true);
            // Debug logs removed for production
            
            productService.putProductOnSale(productId, request);
            
            // Debug logs removed for production
            return ResponseEntity.ok(new ApiResponse("Product added to sale successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(NOT_FOUND)
                    .body(new ApiResponse("Error: " + e.getMessage(), NOT_FOUND));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/product/{productId}/sale")
    public ResponseEntity<ApiResponse> removeProductFromSale(@PathVariable Long productId) {
        try {
            productService.removeProductFromSale(productId);
            return ResponseEntity.ok(new ApiResponse("Product removed from sale successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(NOT_FOUND)
                    .body(new ApiResponse("Product not found", NOT_FOUND));
        }
    }

    /**
     * Helper method to parse datetime strings from frontend
     * Handles both ISO 8601 with timezone (2025-06-25T19:53:00.000Z) 
     * and simple LocalDateTime format (2025-06-25T19:53:00)
     */
    private LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Remove milliseconds and timezone info if present
            String cleanDateTime = dateTimeString;
            
            // If contains timezone info (Z or +XX:XX), parse as ZonedDateTime first
            if (cleanDateTime.contains("Z") || cleanDateTime.matches(".*[+-]\\d{2}:\\d{2}$")) {
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(cleanDateTime);
                return zonedDateTime.toLocalDateTime();
            }
            
            // If contains milliseconds (.000), remove them
            if (cleanDateTime.contains(".")) {
                cleanDateTime = cleanDateTime.substring(0, cleanDateTime.indexOf('.'));
            }
            
            // Parse as LocalDateTime
            return LocalDateTime.parse(cleanDateTime);
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid datetime format: " + dateTimeString);
        }
    }
} 