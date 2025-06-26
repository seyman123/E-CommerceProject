package com.seyman.dreamshops.service.favorite;

import com.seyman.dreamshops.exceptions.ResourceNotFoundException;
import com.seyman.dreamshops.model.Favorite;
import com.seyman.dreamshops.model.Product;
import com.seyman.dreamshops.model.User;
import com.seyman.dreamshops.repository.FavoriteRepository;
import com.seyman.dreamshops.repository.ProductRepository;
import com.seyman.dreamshops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService implements IFavoriteService {
    
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    
    @Override
    @Transactional
    public Favorite addToFavorites(Long userId, Long productId) {
        // Kullanıcı ve ürünün var olup olmadığını kontrol et
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        // Zaten favorilerde olup olmadığını kontrol et
        if (favoriteRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
            throw new IllegalStateException("Product is already in favorites");
        }
        
        // Yeni favori oluştur
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setProduct(product);
        
        return favoriteRepository.save(favorite);
    }
    
    @Override
    @Transactional
    public void removeFromFavorites(Long userId, Long productId) {
        // Favoriyi bul ve sil
        Favorite favorite = favoriteRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite not found"));
        
        favoriteRepository.delete(favorite);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Product> getUserFavorites(Long userId) {
        // Kullanıcının var olup olmadığını kontrol et
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        
        // Kullanıcının favori ürünlerini getir
        List<Favorite> favorites = favoriteRepository.findByUserIdWithProductDetails(userId);
        
        return favorites.stream()
                .map(Favorite::getProduct)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isProductFavorite(Long userId, Long productId) {
        return favoriteRepository.findByUserIdAndProductId(userId, productId).isPresent();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long getUserFavoriteCount(Long userId) {
        return favoriteRepository.countByUserId(userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Long getProductFavoriteCount(Long productId) {
        return favoriteRepository.countByProductId(productId);
    }
    
    @Override
    @Transactional
    public void clearUserFavorites(Long userId) {
        // Kullanıcının var olup olmadığını kontrol et
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        
        favoriteRepository.deleteByUserId(userId);
    }
} 