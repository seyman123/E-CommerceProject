package com.seyman.dreamshops.repository;

import com.seyman.dreamshops.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    @Query("SELECT DISTINCT o FROM com.seyman.dreamshops.model.Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product p " +
           "LEFT JOIN FETCH p.images " +
           "LEFT JOIN FETCH p.category " +
           "WHERE o.user.id = :userId " +
           "ORDER BY o.orderDate DESC")
    List<Order> findByUser_Id(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT o FROM com.seyman.dreamshops.model.Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product p " +
           "LEFT JOIN FETCH p.images " +
           "LEFT JOIN FETCH p.category " +
           "WHERE o.orderId = :orderId")
    Order findByIdWithDetails(@Param("orderId") Long orderId);
    
    @Query("SELECT DISTINCT o FROM com.seyman.dreamshops.model.Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product p " +
           "LEFT JOIN FETCH p.images " +
           "LEFT JOIN FETCH p.category " +
           "ORDER BY o.orderDate DESC")
    List<Order> findAllWithDetails();
}
