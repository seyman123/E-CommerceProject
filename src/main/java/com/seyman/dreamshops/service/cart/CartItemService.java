package com.seyman.dreamshops.service.cart;

import com.seyman.dreamshops.exceptions.ResourceNotFoundException;
import com.seyman.dreamshops.model.Cart;
import com.seyman.dreamshops.model.CartItem;
import com.seyman.dreamshops.model.Product;
import com.seyman.dreamshops.repository.CartItemRepository;
import com.seyman.dreamshops.repository.CartRepository;
import com.seyman.dreamshops.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartItemService implements ICartItemService {

    private final CartItemRepository cartItemRepository;
    private final ICartService cartService;
    private final CartRepository cartRepository;
    private final IProductService productService;

    @Override
    public CartItem getCartItem(Long cartId, Long productId) {
        Cart cart = cartService.getCart(cartId);
        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found!"));
        return cartItem;
    }

    @Override
    public void addItemToCart(Long cartId, Long productId, int quantity) {
        // Debug logs removed for production
        
        //1 -> Sepeti al
        try {
            Cart cart = cartService.getCart(cartId);
            
            Product product = productService.getProductById(productId);
            
            CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product);
            
            // Eğer item zaten sepette varsa quantity arttır
            if (cartItem != null) {
                cartItem.setQuantity(cartItem.getQuantity() + quantity);
            } else {
                // Yeni item oluştur
                cartItem = new CartItem();
                cartItem.setCart(cart);
                cartItem.setProduct(product);
                cartItem.setQuantity(quantity);
                cartItem.setUnitPrice(product.getPrice());
            }
            
            cartItem.setTotalPrice();
            cartItemRepository.save(cartItem);
            
            // Debug logs removed for production
        } catch (Exception e) {
            // Debug logs removed for production
            throw new RuntimeException("Sepete ürün eklenirken hata oluştu: " + e.getMessage());
        }
    }

    @Override
    public void removeItemFromCart(Long cartId, Long productId) {
        Cart cart = cartService.getCart(cartId);
        Product product = productService.getProductById(productId);
        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product);
        
        if (cartItem != null) {
            cartItemRepository.delete(cartItem);
        }
    }

    @Override
    public void updateItemQuantity(Long cartId, Long productId, int quantity) {
        // Debug logs removed for production
        
        try {
            Cart cart = cartService.getCart(cartId);
            // Debug logs removed for production
            
            Optional<CartItem> optionalItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
            
            if (optionalItem.isPresent()) {
                CartItem item = optionalItem.get();
                // Debug logs removed for production
                item.setQuantity(quantity);
                item.setTotalPrice();
                // Debug logs removed for production
            } else {
                // Debug logs removed for production
                throw new ResourceNotFoundException("Ürün sepette bulunamadı!");
            }
            
            // Sepeti kaydet
            cartRepository.save(cart);
            // Debug logs removed for production
        } catch (Exception e) {
            throw new RuntimeException("Miktar güncellenirken hata oluştu: " + e.getMessage());
        }
    }
}
