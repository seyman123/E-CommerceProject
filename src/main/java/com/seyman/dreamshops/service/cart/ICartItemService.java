package com.seyman.dreamshops.service.cart;

import com.seyman.dreamshops.model.CartItem;

public interface ICartItemService {
    CartItem getCartItem(Long cartId, Long productId);
    void addItemToCart(Long cartId, Long productId, int quantity);
    void removeItemFromCart(Long cartId, Long productId);
    void updateItemQuantity(Long cartId, Long productId, int quantity);
}
