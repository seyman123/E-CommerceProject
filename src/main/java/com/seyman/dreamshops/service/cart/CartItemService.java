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
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void addItemToCart(Long cartId, Long productId, int quantity) {
        if (cartId == null) {
            throw new ResourceNotFoundException("Cart ID cannot be null");
        }
        if (productId == null) {
            throw new ResourceNotFoundException("Product ID cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        
        try {
            // Get fresh cart from database to avoid detached entity issues
            Cart cart = cartRepository.findByIdWithItems(cartId);
            if (cart == null) {
                throw new ResourceNotFoundException("Cart not found with ID: " + cartId);
            }
            
            Product product = productService.getProductById(productId);
            if (product == null) {
                throw new ResourceNotFoundException("Product not found with ID: " + productId);
            }
            
            CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product);
            
            // Eğer item zaten sepette varsa quantity arttır
            if (cartItem != null) {
                cartItem.setQuantity(cartItem.getQuantity() + quantity);
                cartItem.setTotalPrice();
                cartItemRepository.save(cartItem);
            } else {
                // Yeni item oluştur
                cartItem = new CartItem();
                cartItem.setCart(cart);
                cartItem.setProduct(product);
                cartItem.setQuantity(quantity);
                cartItem.setUnitPrice(product.getEffectivePrice()); // Use effective price with discounts
                cartItem.setTotalPrice();
                cartItemRepository.save(cartItem);
            }
            
            // Update cart total amount
            BigDecimal totalAmount = cart.getItems().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            cart.setTotalAmount(totalAmount);
            cartRepository.save(cart);
            
        } catch (ResourceNotFoundException e) {
            throw e; // Re-throw ResourceNotFoundException as is
        } catch (Exception e) {
            e.printStackTrace(); // Log for debugging
            throw new RuntimeException("Sepete ürün eklenirken hata oluştu: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void removeItemFromCart(Long cartId, Long productId) {
        System.out.println("=== CART ITEM SERVICE - REMOVE ITEM ===");
        System.out.println("Cart ID: " + cartId);
        System.out.println("Product ID: " + productId);
        
        try {
            // Get fresh cart from database to avoid detached entity issues
            Cart cart = cartRepository.findByIdWithItems(cartId);
            System.out.println("Cart found: " + (cart != null));
            if (cart == null) {
                System.out.println("Cart is null for ID: " + cartId);
                throw new ResourceNotFoundException("Cart not found with ID: " + cartId);
            }
            
            System.out.println("Cart has " + cart.getItems().size() + " items");
            
            Product product = productService.getProductById(productId);
            System.out.println("Product found: " + (product != null));
            if (product == null) {
                System.out.println("Product is null for ID: " + productId);
                throw new ResourceNotFoundException("Product not found with ID: " + productId);
            }
            
            CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product);
            System.out.println("CartItem found: " + (cartItem != null));
            
            if (cartItem != null) {
                System.out.println("Removing cart item with ID: " + cartItem.getId());
                
                // Remove the item from cart's collection first
                boolean removed = cart.getItems().remove(cartItem);
                System.out.println("Item removed from collection: " + removed);
                
                // Delete the cart item from database
                cartItemRepository.delete(cartItem);
                System.out.println("Item deleted from database");
                
                // Recalculate cart total amount from remaining items
                BigDecimal totalAmount = cart.getItems().stream()
                    .map(CartItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                cart.setTotalAmount(totalAmount);
                cartRepository.save(cart);
                System.out.println("Cart total updated to: " + totalAmount);
                System.out.println("Remove item operation completed successfully");
            } else {
                System.out.println("CartItem not found for cart: " + cartId + " and product: " + productId);
                throw new ResourceNotFoundException("Item not found in cart");
            }
        } catch (ResourceNotFoundException e) {
            System.out.println("ResourceNotFoundException in removeItemFromCart: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.out.println("Exception in removeItemFromCart: " + e.getMessage());
            e.printStackTrace(); // Log for debugging
            throw new RuntimeException("Ürün sepetten çıkarılırken hata oluştu: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void updateItemQuantity(Long cartId, Long productId, int quantity) {
        try {
            // Get fresh cart from database to avoid detached entity issues
            Cart cart = cartRepository.findByIdWithItems(cartId);
            if (cart == null) {
                throw new ResourceNotFoundException("Cart not found with ID: " + cartId);
            }
            
            Optional<CartItem> optionalItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
            
            if (optionalItem.isPresent()) {
                CartItem item = optionalItem.get();
                item.setQuantity(quantity);
                item.setTotalPrice();
                cartItemRepository.save(item);
                
                // Update cart total amount
                BigDecimal totalAmount = cart.getItems().stream()
                    .map(CartItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                cart.setTotalAmount(totalAmount);
                cartRepository.save(cart);
            } else {
                throw new ResourceNotFoundException("Ürün sepette bulunamadı!");
            }
            
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Miktar güncellenirken hata oluştu: " + e.getMessage());
        }
    }
}
