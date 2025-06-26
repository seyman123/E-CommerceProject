package com.seyman.dreamshops.controller;

import com.seyman.dreamshops.model.Coupon;
import com.seyman.dreamshops.response.ApiResponse;
import com.seyman.dreamshops.service.coupon.ICouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/coupons")
public class CouponController {
    
    private final ICouponService couponService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse> getAllCouponsForAdmin() {
        try {
            List<Coupon> coupons = couponService.getAllCoupons();
            return ResponseEntity.ok(new ApiResponse("Success", coupons));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error", INTERNAL_SERVER_ERROR));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse> getAllCoupons() {
        try {
            List<Coupon> coupons = couponService.getAllCoupons();
            return ResponseEntity.ok(new ApiResponse("Success", coupons));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error", INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse> getActiveCoupons() {
        try {
            List<Coupon> coupons = couponService.getAllActiveCoupons();
            return ResponseEntity.ok(new ApiResponse("Success", coupons));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error", INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse> getCouponsByType(@PathVariable String type) {
        try {
            Coupon.CouponType couponType = Coupon.CouponType.valueOf(type.toUpperCase());
            List<Coupon> coupons = couponService.getCouponsByType(couponType);
            return ResponseEntity.ok(new ApiResponse("Success", coupons));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(BAD_REQUEST)
                    .body(new ApiResponse("Invalid coupon type", BAD_REQUEST));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error", INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse> getCouponByCode(@PathVariable String code) {
        try {
            Coupon coupon = couponService.getCouponByCode(code);
            return ResponseEntity.ok(new ApiResponse("Success", coupon));
        } catch (Exception e) {
            return ResponseEntity.status(NOT_FOUND)
                    .body(new ApiResponse("Coupon not found", NOT_FOUND));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse> validateCoupon(@RequestParam String code, 
                                                     @RequestParam BigDecimal orderAmount) {
        try {
            // First check if coupon exists
            Coupon coupon;
            try {
                coupon = couponService.getCouponByCode(code);
            } catch (Exception e) {
                return ResponseEntity.ok(new ApiResponse("Kupon kodu bulunamadı", 
                    new CouponValidationResponse(false, BigDecimal.ZERO)));
            }
            
            // Check if coupon is active
            if (!coupon.getIsActive()) {
                return ResponseEntity.ok(new ApiResponse("Kupon kodu deaktif durumda", 
                    new CouponValidationResponse(false, BigDecimal.ZERO)));
            }
            
            // Check dates
            LocalDateTime now = LocalDateTime.now();
            if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
                return ResponseEntity.ok(new ApiResponse("Kupon henüz başlamamış", 
                    new CouponValidationResponse(false, BigDecimal.ZERO)));
            }
            if (coupon.getEndDate() != null && now.isAfter(coupon.getEndDate())) {
                return ResponseEntity.ok(new ApiResponse("Kupon süresi dolmuş", 
                    new CouponValidationResponse(false, BigDecimal.ZERO)));
            }
            
            // Check usage limit
            if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
                return ResponseEntity.ok(new ApiResponse("Kupon kullanım limiti dolmuş", 
                    new CouponValidationResponse(false, BigDecimal.ZERO)));
            }
            
            // Check minimum order amount
            if (coupon.getMinOrderAmount() != null && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
                return ResponseEntity.ok(new ApiResponse("Minimum sipariş tutarı karşılanmadı. Minimum: " + coupon.getMinOrderAmount() + " TL", 
                    new CouponValidationResponse(false, BigDecimal.ZERO)));
            }
            
            // If all checks pass, calculate discount
            BigDecimal discount = coupon.calculateDiscount(orderAmount);
            return ResponseEntity.ok(new ApiResponse("Kupon geçerli", 
                new CouponValidationResponse(true, discount)));
                
        } catch (Exception e) {
            return ResponseEntity.status(BAD_REQUEST)
                    .body(new ApiResponse("Kupon doğrulanırken hata oluştu: " + e.getMessage(), 
                        new CouponValidationResponse(false, BigDecimal.ZERO)));
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse> applyCoupon(@RequestParam String code, 
                                                  @RequestParam BigDecimal orderAmount) {
        try {
            BigDecimal discount = couponService.applyCoupon(code, orderAmount);
            return ResponseEntity.ok(new ApiResponse("Coupon applied successfully", discount));
        } catch (Exception e) {
            return ResponseEntity.status(BAD_REQUEST)
                    .body(new ApiResponse("Error applying coupon", BAD_REQUEST));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse> createCoupon(@RequestBody Coupon coupon) {
        try {
            Coupon createdCoupon = couponService.createCoupon(coupon);
            return ResponseEntity.status(CREATED)
                    .body(new ApiResponse("Coupon created successfully", createdCoupon));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(CONFLICT)
                    .body(new ApiResponse(e.getMessage(), CONFLICT));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Error creating coupon", INTERNAL_SERVER_ERROR));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateCoupon(@PathVariable Long id, @RequestBody Coupon coupon) {
        try {
            Coupon updatedCoupon = couponService.updateCoupon(id, coupon);
            return ResponseEntity.ok(new ApiResponse("Coupon updated successfully", updatedCoupon));
        } catch (Exception e) {
            return ResponseEntity.status(NOT_FOUND)
                    .body(new ApiResponse("Coupon not found", NOT_FOUND));
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCoupon(@PathVariable Long id) {
        try {
            couponService.deleteCoupon(id);
            return ResponseEntity.ok(new ApiResponse("Coupon deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(NOT_FOUND)
                    .body(new ApiResponse("Coupon not found", NOT_FOUND));
        }
    }

    // Inner class for coupon validation response
    public static class CouponValidationResponse {
        private boolean valid;
        private BigDecimal discountAmount;

        public CouponValidationResponse(boolean valid, BigDecimal discountAmount) {
            this.valid = valid;
            this.discountAmount = discountAmount;
        }

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    }
} 