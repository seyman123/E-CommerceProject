package com.seyman.dreamshops.repository;

import com.seyman.dreamshops.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Cart findByUserId(Long userId);
    
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items ci LEFT JOIN FETCH ci.product p LEFT JOIN FETCH p.images WHERE c.user.id = :userId")
    Cart findByUserIdWithItems(@Param("userId") Long userId);
    
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items ci LEFT JOIN FETCH ci.product p LEFT JOIN FETCH p.images WHERE c.id = :cartId")
    Cart findByIdWithItems(@Param("cartId") Long cartId);
}
