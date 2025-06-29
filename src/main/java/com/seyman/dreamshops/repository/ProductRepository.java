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
    
    // Optimized queries with JOIN FETCH to avoid N+1 problem
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.images " +
           "LEFT JOIN FETCH p.category " +
           "ORDER BY p.name ASC")
    List<Product> findAllWithImagesAndCategory();
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.images " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.category.name = :category " +
           "ORDER BY p.name ASC")
    List<Product> findByCategoryNameWithImagesAndCategory(@Param("category") String category);
    
    // Search products by name containing the search term (case insensitive) with JOIN FETCH
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.images " +
           "LEFT JOIN FETCH p.category " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "ORDER BY p.name ASC")
    List<Product> findByNameContainingWithImagesAndCategory(@Param("name") String name);
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.images " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.category.name = :category AND LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY p.name ASC")
    List<Product> findByCategoryNameAndNameContainingWithImagesAndCategory(@Param("category") String category, @Param("search") String search);
    
    // Sale-related queries
    List<Product> findByIsOnSaleTrueOrderBySaleStartDateDesc();
    
    List<Product> findByIsFlashSaleTrueOrderBySaleStartDateDesc();
    
    List<Product> findByIsOnSaleTrueAndCategoryNameOrderBySaleStartDateDesc(String categoryName);
    
    // Paginated methods with JOIN FETCH
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.images " +
           "LEFT JOIN FETCH p.category " +
           "ORDER BY p.name ASC")
    Page<Product> findAllWithImagesAndCategory(Pageable pageable);
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.images " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.category.name = :category " +
           "ORDER BY p.name ASC")
    Page<Product> findByCategoryNameWithImagesAndCategory(@Param("category") String category, Pageable pageable);
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.images " +
           "LEFT JOIN FETCH p.category " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY p.name ASC")
    Page<Product> findByNameContainingWithImagesAndCategory(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.images " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.category.name = :category AND LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY p.name ASC")
    Page<Product> findByCategoryNameAndNameContainingWithImagesAndCategory(@Param("category") String category, @Param("search") String search, Pageable pageable);
    
    // Original paginated methods for backward compatibility
    Page<Product> findByCategoryName(String category, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> findByNameContainingIgnoreCase(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.category.name = :category AND LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> findByCategoryNameAndNameContainingIgnoreCase(@Param("category") String category, @Param("search") String search, Pageable pageable);
}
