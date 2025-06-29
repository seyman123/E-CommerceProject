package com.seyman.dreamshops.repository;

import com.seyman.dreamshops.model.Cart;
import com.seyman.dreamshops.model.CartItem;
import com.seyman.dreamshops.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    void deleteAllByCartId(Long id);
    void deleteAllByProductId(Long productId);
    boolean existsByProductId(Long productId);
    CartItem findByCartAndProduct(Cart cart, Product product);
}
