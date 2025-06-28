package com.seyman.dreamshops.controller;

import com.seyman.dreamshops.dto.CartDto;
import com.seyman.dreamshops.dto.UserDto;
import com.seyman.dreamshops.exceptions.ResourceNotFoundException;
import com.seyman.dreamshops.model.Cart;
import com.seyman.dreamshops.response.ApiResponse;
import com.seyman.dreamshops.service.cart.ICartService;
import com.seyman.dreamshops.service.user.IUserService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@RestController
@RequestMapping("${api.prefix}/carts")
@RequiredArgsConstructor
public class CartController {

    private final ICartService cartService;
    private final IUserService userService;

    @GetMapping("/{cartId}/my-cart")
    public ResponseEntity<ApiResponse> getCart(@PathVariable Long cartId) {
        try {
            Cart cart = cartService.getCart(cartId);
            CartDto cartDto = cartService.convertToDto(cart);
            return ResponseEntity.ok(new ApiResponse("Success", cartDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/user/my-cart")
    public ResponseEntity<ApiResponse> getUserCart() {
        try {
            UserDto user = userService.getAuthenticatedUser();
            
            if (user == null || user.getId() == null) {
                return ResponseEntity.status(UNAUTHORIZED).body(new ApiResponse("User authentication failed", null));
            }
            
            Cart cart = cartService.getCartByUserId(user.getId());
            
            if (cart == null) {
                cart = cartService.initialNewCart(userService.convertDtoToUser(user));
            }
            
            CartDto cartDto = cartService.convertToDto(cart);
            
            return ResponseEntity.ok(new ApiResponse("Success", cartDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (JwtException e) {
            return ResponseEntity.status(UNAUTHORIZED).body(new ApiResponse("Authentication failed: " + e.getMessage(), null));
        } catch (Exception e) {
            // Log the full error for debugging
            e.printStackTrace();
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Unexpected error occurred: " + e.getMessage(), null));
        }
    }

    @GetMapping("/user/cart")
    public ResponseEntity<ApiResponse> getUserCartAlternative() {
        try {
            UserDto user = userService.getAuthenticatedUser();
            Cart cart = cartService.getCartByUserId(user.getId());
            
            if (cart == null) {
                cart = cartService.initialNewCart(userService.convertDtoToUser(user));
            }

            CartDto cartDto = cartService.convertToDto(cart);
            return ResponseEntity.ok(new ApiResponse("Success", cartDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (JwtException e) {
            return ResponseEntity.status(UNAUTHORIZED).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/{cartId}/clear")
    public ResponseEntity<ApiResponse> clearCart(@PathVariable Long cartId) {
        try {
            cartService.clearCart(cartId);
            return ResponseEntity.ok(new ApiResponse("Clear Cart Success!", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/user/clear")
    public ResponseEntity<ApiResponse> clearUserCart() {
        try {
            UserDto user = userService.getAuthenticatedUser();
            Cart cart = cartService.getCartByUserId(user.getId());
            
            if (cart != null) {
                cartService.clearCart(cart.getId());
            }
            
            return ResponseEntity.ok(new ApiResponse("Clear Cart Success!", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (JwtException e) {
            return ResponseEntity.status(UNAUTHORIZED).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @GetMapping("/{cartId}/cart/total-price")
    public ResponseEntity<ApiResponse> getTotalAmount(@PathVariable Long cartId) {
        try {
            BigDecimal totalPrice = cartService.getTotalPrice(cartId);
            return ResponseEntity.ok(new ApiResponse("Total Price", totalPrice));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }
}
