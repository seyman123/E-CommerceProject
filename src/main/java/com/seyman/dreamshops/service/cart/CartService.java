package com.seyman.dreamshops.service.cart;


import com.seyman.dreamshops.dto.CartDto;
import com.seyman.dreamshops.dto.CartItemDto;
import com.seyman.dreamshops.exceptions.ResourceNotFoundException;
import com.seyman.dreamshops.model.Cart;
import com.seyman.dreamshops.model.CartItem;
import com.seyman.dreamshops.model.User;
import com.seyman.dreamshops.repository.CartItemRepository;
import com.seyman.dreamshops.repository.CartRepository;
import com.seyman.dreamshops.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService implements ICartService{

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final IProductService productService;
    private final AtomicLong cartIdGenerator = new AtomicLong(0);

    @Override
    public Cart getCart(Long id) {
        Cart cart = cartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found!"));
        cart.setTotalAmount(cart.getTotalAmount());
        return cartRepository.save(cart);
    }

    @Transactional
    @Override
    public void clearCart(Long id) {
        Cart cart = this.getCart(id);
        cartItemRepository.deleteAllByCartId(id);
        cart.getItems().clear();
        cartRepository.deleteById(id);
    }

    @Override
    public BigDecimal getTotalPrice(Long id) {
        Cart cart = getCart(id);
        return cart.getTotalAmount();
    }

    @Override
    public Cart initialNewCart(User user) {
        return Optional.ofNullable(this.getCartByUserId(user.getId()))
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    @Override
    public Cart getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserId(userId);
        
        if (cart != null) {
            // Update prices for all cart items in case of discount changes
            boolean pricesUpdated = false;
            for (CartItem item : cart.getItems()) {
                BigDecimal currentEffectivePrice = item.getProduct().getEffectivePrice();
                if (!item.getUnitPrice().equals(currentEffectivePrice)) {
                    item.setUnitPrice(currentEffectivePrice);
                    item.setTotalPrice();
                    pricesUpdated = true;
                }
            }
            
            // Recalculate cart total if any prices were updated
            if (pricesUpdated) {
                BigDecimal totalAmount = cart.getItems().stream()
                    .map(CartItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                cart.setTotalAmount(totalAmount);
                cart = cartRepository.save(cart);
            }
        }
        
        return cart;
    }

    @Override
    public CartDto convertToDto(Cart cart) {
        CartDto cartDto = new CartDto();
        cartDto.setCartId(cart.getId());
        cartDto.setTotalAmount(cart.getTotalAmount());
        
        Set<CartItemDto> cartItemDtos = cart.getItems().stream()
                .map(this::convertCartItemToDto)
                .collect(Collectors.toSet());
        
        cartDto.setItems(cartItemDtos);
        return cartDto;
    }

    private CartItemDto convertCartItemToDto(CartItem cartItem) {
        CartItemDto cartItemDto = new CartItemDto();
        cartItemDto.setItemId(cartItem.getId());
        cartItemDto.setQuantity(cartItem.getQuantity());
        cartItemDto.setUnitPrice(cartItem.getUnitPrice());
        cartItemDto.setTotalPrice(cartItem.getTotalPrice());
        cartItemDto.setProduct(productService.convertToDto(cartItem.getProduct()));
        return cartItemDto;
    }
}
