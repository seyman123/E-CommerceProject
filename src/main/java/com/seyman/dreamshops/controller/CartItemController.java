package com.seyman.dreamshops.controller;

import com.seyman.dreamshops.dto.UserDto;
import com.seyman.dreamshops.exceptions.ResourceNotFoundException;
import com.seyman.dreamshops.model.Cart;
import com.seyman.dreamshops.model.User;
import com.seyman.dreamshops.response.ApiResponse;
import com.seyman.dreamshops.service.cart.ICartItemService;
import com.seyman.dreamshops.service.cart.ICartService;
import com.seyman.dreamshops.service.user.IUserService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("${api.prefix}/cartItems")
@RequiredArgsConstructor

public class CartItemController {
    private final ICartItemService cartItemService;
    private final ICartService cartService;
    private final IUserService userService;

    @PostMapping("/item/add")
    public ResponseEntity<ApiResponse> addItemToCart(@RequestParam Long productId, @RequestParam Integer quantity) {
        try {
            UserDto user = userService.getAuthenticatedUser();

            Cart cart = cartService.initialNewCart(userService.convertDtoToUser(user));

            cartItemService.addItemToCart(cart.getId(), productId, quantity);
            return ResponseEntity.ok(new ApiResponse("Add Item Success", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (JwtException e) {
            return ResponseEntity.status(UNAUTHORIZED).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @PostMapping("/cart/{cartId}/item/{productId}/add")
    public ResponseEntity<ApiResponse> addItemToCartById(@PathVariable Long cartId, @PathVariable Long productId, @RequestParam(defaultValue = "1") Integer quantity) {
        // Debug logs removed for production
        
        try {
            cartItemService.addItemToCart(cartId, productId, quantity);
            // Debug logs removed for production
            return ResponseEntity.ok(new ApiResponse("Add Item Success", null));
        } catch (ResourceNotFoundException e) {
            // Debug logs removed for production
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            // Debug logs removed for production
            return ResponseEntity.status(500).body(new ApiResponse("Internal server error: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/cart/{cartId}/item/{productId}/remove")
    public ResponseEntity<ApiResponse> removeItemFromCart(@PathVariable Long cartId, @PathVariable Long productId) {
        try {
            cartItemService.removeItemFromCart(cartId, productId);
            return ResponseEntity.ok(new ApiResponse("Remove Item Success", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            e.printStackTrace(); // Log for debugging
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Sunucu hatası oluştu. Lütfen daha sonra tekrar deneyin.", null));
        }
    }

    @PutMapping("/cart/{cartId}/item/{productId}/update")
    public ResponseEntity<ApiResponse> updateItemQuantity(@PathVariable Long cartId, @PathVariable Long productId, @RequestParam Integer quantity) {
        // Debug logs removed for production
        
        try {
            cartItemService.updateItemQuantity(cartId, productId, quantity);
            // Debug logs removed for production
            return ResponseEntity.ok(new ApiResponse("Update Item Quantity Success", null));
        } catch (ResourceNotFoundException e) {
            // Debug logs removed for production
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            // Debug logs removed for production
            return ResponseEntity.status(500).body(new ApiResponse("Internal server error", null));
        }
    }

    @PostMapping("/item")
    public ResponseEntity<ApiResponse> addItemToCart(@RequestParam Long cartId,
                                                   @RequestParam Long productId,
                                                   @RequestParam int quantity) {
        try {
            cartItemService.addItemToCart(cartId, productId, quantity);
            return ResponseEntity.ok(new ApiResponse("Add Item Success", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse("Resource not found", null));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Unexpected error occurred", null));
        }
    }
}
