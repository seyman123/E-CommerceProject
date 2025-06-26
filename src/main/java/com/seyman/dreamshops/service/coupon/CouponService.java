package com.seyman.dreamshops.service.coupon;

import com.seyman.dreamshops.exceptions.ResourceNotFoundException;
import com.seyman.dreamshops.model.Coupon;
import com.seyman.dreamshops.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService implements ICouponService {
    
    private final CouponRepository couponRepository;

    @Override
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    @Override
    public List<Coupon> getAllActiveCoupons() {
        return couponRepository.findValidCoupons(LocalDateTime.now());
    }

    @Override
    public List<Coupon> getCouponsByType(Coupon.CouponType type) {
        return couponRepository.findByType(type);
    }

    @Override
    public Coupon getCouponByCode(String code) {
        return couponRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + code));
    }

    @Override
    public Coupon createCoupon(Coupon coupon) {
        if (couponRepository.existsByCode(coupon.getCode())) {
            throw new IllegalArgumentException("Coupon with code " + coupon.getCode() + " already exists");
        }
        return couponRepository.save(coupon);
    }

    @Override
    public Coupon updateCoupon(Long id, Coupon coupon) {
        Coupon existingCoupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));
        
        existingCoupon.setDescription(coupon.getDescription());
        existingCoupon.setDiscountValue(coupon.getDiscountValue());
        existingCoupon.setMinOrderAmount(coupon.getMinOrderAmount());
        existingCoupon.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        existingCoupon.setUsageLimit(coupon.getUsageLimit());
        existingCoupon.setEndDate(coupon.getEndDate());
        existingCoupon.setIsActive(coupon.getIsActive());
        
        return couponRepository.save(existingCoupon);
    }

    @Override
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));
        couponRepository.delete(coupon);
    }

    @Override
    public BigDecimal applyCoupon(String code, BigDecimal orderAmount) {
        Coupon coupon = getCouponByCode(code);
        return coupon.calculateDiscount(orderAmount);
    }

    @Override
    public void useCoupon(String code) {
        Coupon coupon = getCouponByCode(code);
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
    }

    @Override
    public boolean validateCoupon(String code, BigDecimal orderAmount) {
        try {
            Coupon coupon = getCouponByCode(code);
            return coupon.isValid() && orderAmount.compareTo(coupon.getMinOrderAmount()) >= 0;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }
} 