package com.seyman.dreamshops.repository;

import com.seyman.dreamshops.model.Favorite;
import com.seyman.dreamshops.model.Product;
import com.seyman.dreamshops.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    // Kullanıcının favori ürünlerini getir
    List<Favorite> findByUser(User user);
    
    // Kullanıcının favori ürünlerini user ID ile getir
    List<Favorite> findByUserId(Long userId);
    
    // Belirli bir ürünün kullanıcı tarafından favorilere eklenip eklenmediğini kontrol et
    Optional<Favorite> findByUserAndProduct(User user, Product product);
    
    // Kullanıcı ID ve ürün ID ile favori kontrolü
    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);
    
    // Kullanıcının favori ürün sayısını getir
    Long countByUserId(Long userId);
    
    // Bir ürünü kaç kullanıcının favorilere eklediğini getir
    Long countByProductId(Long productId);
    
    // Kullanıcının favori ürünlerini ürün bilgileri ile birlikte getir
    @Query("SELECT f FROM Favorite f JOIN FETCH f.product p JOIN FETCH p.category WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    List<Favorite> findByUserIdWithProductDetails(@Param("userId") Long userId);
    
    // Kullanıcı ve ürün ID'si ile favoriyi sil
    void deleteByUserIdAndProductId(Long userId, Long productId);
    
    // Kullanıcının tüm favorilerini sil
    void deleteByUserId(Long userId);
    
    // Ürünün tüm favorilerini sil (ürün silindiğinde)
    void deleteByProductId(Long productId);
} 