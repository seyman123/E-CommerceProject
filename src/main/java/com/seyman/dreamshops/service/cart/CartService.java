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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @Transactional(readOnly = true)
    public Cart getCart(Long id) {
        Cart cart = cartRepository.findByIdWithItems(id);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart not found!");
        }
        // Calculate and set total amount without saving (causes detached entity issue)
        BigDecimal totalAmount = cart.getItems().stream()
            .map(CartItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(totalAmount);
        return cart;
    }

    @Transactional
    @Override
    // @CacheEvict(value = "cartCache", key = "#cartId") // Temporarily disabled
    public void clearCart(Long cartId) {
        Cart cart = getCart(cartId);
        cartItemRepository.deleteAllByCartId(cartId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Override
    public BigDecimal getTotalPrice(Long id) {
        Cart cart = getCart(id);
        return cart.getTotalAmount();
    }

    @Override
    @Transactional
    public Cart initialNewCart(User user) {
        if (user == null || user.getId() == null) {
            throw new ResourceNotFoundException("User is null or has no ID");
        }
        
        return Optional.ofNullable(cartRepository.findByUserIdWithItems(user.getId()))
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    @Override
    @Transactional(readOnly = true)
    // @Cacheable(value = "cartCache", key = "#userId") // Keep disabled - complex entity with lazy loading
    public Cart getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId);
        
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
        if (cart == null) {
            return null;
        }
        
        CartDto cartDto = new CartDto();
        cartDto.setCartId(cart.getId());
        cartDto.setTotalAmount(cart.getTotalAmount());
        
        // Items are eagerly loaded via JOIN FETCH, so no lazy loading issues
        Set<CartItemDto> cartItemDtos;
        if (cart.getItems() != null && !cart.getItems().isEmpty()) {
            cartItemDtos = cart.getItems().stream()
                    .map(this::convertCartItemToDto)
                    .collect(Collectors.toSet());
        } else {
            cartItemDtos = Set.of(); // Empty set
        }
        
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
