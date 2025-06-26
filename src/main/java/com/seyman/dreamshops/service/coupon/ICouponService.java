package com.seyman.dreamshops.service.coupon;

import com.seyman.dreamshops.model.Coupon;

import java.math.BigDecimal;
import java.util.List;

public interface ICouponService {
    
    List<Coupon> getAllCoupons();
    
    List<Coupon> getAllActiveCoupons();
    
    List<Coupon> getCouponsByType(Coupon.CouponType type);
    
    Coupon getCouponByCode(String code);
    
    Coupon createCoupon(Coupon coupon);
    
    Coupon updateCoupon(Long id, Coupon coupon);
    
    void deleteCoupon(Long id);
    
    BigDecimal applyCoupon(String code, BigDecimal orderAmount);
    
    void useCoupon(String code);
    
    boolean validateCoupon(String code, BigDecimal orderAmount);
} 