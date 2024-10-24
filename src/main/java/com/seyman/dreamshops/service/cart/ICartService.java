package com.seyman.dreamshops.service.cart;

import com.seyman.dreamshops.model.Cart;
import com.seyman.dreamshops.model.User;

import java.math.BigDecimal;

public interface ICartService {
    Cart getCart(Long id);
    void clearCart(Long id);
    BigDecimal getTotalPrice(Long id);

    Cart initialNewCart(User user);

    Cart getCartByUserId(Long userId);
}
