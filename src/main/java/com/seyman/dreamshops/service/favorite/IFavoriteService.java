package com.seyman.dreamshops.service.favorite;

import com.seyman.dreamshops.model.Favorite;
import com.seyman.dreamshops.model.Product;

import java.util.List;

public interface IFavoriteService {
    
    // Ürünü favorilere ekle
    Favorite addToFavorites(Long userId, Long productId);
    
    // Ürünü favorilerden çıkar
    void removeFromFavorites(Long userId, Long productId);
    
    // Kullanıcının favori ürünlerini getir
    List<Product> getUserFavorites(Long userId);
    
    // Ürünün favori olup olmadığını kontrol et
    boolean isProductFavorite(Long userId, Long productId);
    
    // Kullanıcının favori ürün sayısını getir
    Long getUserFavoriteCount(Long userId);
    
    // Ürünü kaç kullanıcının favorilere eklediğini getir
    Long getProductFavoriteCount(Long productId);
    
    // Kullanıcının tüm favorilerini temizle
    void clearUserFavorites(Long userId);
} 