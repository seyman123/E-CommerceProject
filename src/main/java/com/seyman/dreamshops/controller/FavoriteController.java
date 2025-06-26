package com.seyman.dreamshops.controller;

import com.seyman.dreamshops.dto.ProductDto;
import com.seyman.dreamshops.exceptions.ResourceNotFoundException;
import com.seyman.dreamshops.model.Favorite;
import com.seyman.dreamshops.model.Product;
import com.seyman.dreamshops.response.ApiResponse;
import com.seyman.dreamshops.service.favorite.IFavoriteService;
import com.seyman.dreamshops.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/favorites")
public class FavoriteController {
    
    private final IFavoriteService favoriteService;
    private final IProductService productService;
    
    @PostMapping("/add")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> addToFavorites(@RequestParam Long userId, @RequestParam Long productId) {
        try {
            Favorite favorite = favoriteService.addToFavorites(userId, productId);
            return ResponseEntity.ok(new ApiResponse("Product added to favorites successfully", favorite.getId()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(CONFLICT).body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Error adding product to favorites", null));
        }
    }
    
    @DeleteMapping("/remove")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> removeFromFavorites(@RequestParam Long userId, @RequestParam Long productId) {
        try {
            favoriteService.removeFromFavorites(userId, productId);
            return ResponseEntity.ok(new ApiResponse("Product removed from favorites successfully", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Error removing product from favorites", null));
        }
    }
    
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getUserFavorites(@PathVariable Long userId) {
        try {
            List<Product> favorites = favoriteService.getUserFavorites(userId);
            List<ProductDto> favoriteDtos = productService.getConvertedProducts(favorites);
            return ResponseEntity.ok(new ApiResponse("User favorites retrieved successfully", favoriteDtos));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Error retrieving user favorites", null));
        }
    }
    
    @GetMapping("/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> isProductFavorite(@RequestParam Long userId, @RequestParam Long productId) {
        try {
            boolean isFavorite = favoriteService.isProductFavorite(userId, productId);
            return ResponseEntity.ok(new ApiResponse("Favorite status checked successfully", isFavorite));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Error checking favorite status", null));
        }
    }
    
    @GetMapping("/count/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getUserFavoriteCount(@PathVariable Long userId) {
        try {
            Long count = favoriteService.getUserFavoriteCount(userId);
            return ResponseEntity.ok(new ApiResponse("User favorite count retrieved successfully", count));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Error retrieving user favorite count", null));
        }
    }
    
    @GetMapping("/count/product/{productId}")
    public ResponseEntity<ApiResponse> getProductFavoriteCount(@PathVariable Long productId) {
        try {
            Long count = favoriteService.getProductFavoriteCount(productId);
            return ResponseEntity.ok(new ApiResponse("Product favorite count retrieved successfully", count));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Error retrieving product favorite count", null));
        }
    }
    
    @DeleteMapping("/clear/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> clearUserFavorites(@PathVariable Long userId) {
        try {
            favoriteService.clearUserFavorites(userId);
            return ResponseEntity.ok(new ApiResponse("User favorites cleared successfully", null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Error clearing user favorites", null));
        }
    }
} 