package com.seyman.dreamshops.repository;

import com.seyman.dreamshops.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    
    Optional<Coupon> findByCode(String code);
    
    List<Coupon> findByIsActiveTrueOrderByEndDateAsc();
    
    List<Coupon> findByType(Coupon.CouponType type);
    
    @Query("SELECT c FROM Coupon c WHERE c.isActive = true AND " +
           "(c.startDate IS NULL OR c.startDate <= :now) AND " +
           "(c.endDate IS NULL OR c.endDate >= :now) AND " +
           "(c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    List<Coupon> findValidCoupons(LocalDateTime now);
    
    boolean existsByCode(String code);
} 