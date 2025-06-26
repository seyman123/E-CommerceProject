package com.seyman.dreamshops.repository;

import com.seyman.dreamshops.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryName(String category);

    List<Product> findByBrand(String brand);

    List<Product> findByCategoryNameAndBrand(String category, String brand);

    List<Product> findByName(String name);
    
    // Search products by name containing the search term (case insensitive)
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Product> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT p FROM Product p WHERE p.category.name = :category AND LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Product> findByCategoryNameAndNameContaining(@Param("category") String category, @Param("search") String search);

    List<Product> findByBrandAndName(String brand, String name);

    Long countByBrandAndName(String brand, String name);

    boolean existsByBrandAndName(String brand, String name);
    
    Long countByCategoryId(Long categoryId);
    
    boolean existsByCategoryId(Long categoryId);
    
    // Sale-related queries
    List<Product> findByIsOnSaleTrueOrderBySaleStartDateDesc();
    
    List<Product> findByIsFlashSaleTrueOrderBySaleStartDateDesc();
    
    List<Product> findByIsOnSaleTrueAndCategoryNameOrderBySaleStartDateDesc(String categoryName);
    
    // Paginated methods
    Page<Product> findByCategoryName(String category, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> findByNameContainingIgnoreCase(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.category.name = :category AND LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> findByCategoryNameAndNameContainingIgnoreCase(@Param("category") String category, @Param("search") String search, Pageable pageable);
}
