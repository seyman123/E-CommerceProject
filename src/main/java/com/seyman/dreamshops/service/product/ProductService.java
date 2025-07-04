package com.seyman.dreamshops.service.product;

import com.seyman.dreamshops.dto.ImageDto;
import com.seyman.dreamshops.dto.ProductDto;
import com.seyman.dreamshops.exceptions.AlreadyExistsException;
import com.seyman.dreamshops.exceptions.ProductNotFoundException;
import com.seyman.dreamshops.model.Category;
import com.seyman.dreamshops.model.Image;
import com.seyman.dreamshops.model.Product;
import com.seyman.dreamshops.repository.CategoryRepository;
import com.seyman.dreamshops.repository.ImageRepository;
import com.seyman.dreamshops.repository.ProductRepository;
import com.seyman.dreamshops.repository.CartItemRepository;
import com.seyman.dreamshops.repository.OrderItemRepository;
import com.seyman.dreamshops.requests.AddProductRequest;
import com.seyman.dreamshops.requests.ProductUpdateRequest;
import com.seyman.dreamshops.service.cache.CacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService implements IProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageRepository imageRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ModelMapper modelMapper;
    private final CacheService cacheService;

    @Override
    public Product addProduct(AddProductRequest request) {
        if (this.productExists(request.getBrand(), request.getName())) {
            throw new AlreadyExistsException(request.getBrand() + " " + request.getName() + " already exists, you may update this product instead!");
        }

        Category category = null;
        
        // Önce kategori ID'si ile kontrol et
        if (request.getCategory() != null && request.getCategory().getId() != null) {
            category = categoryRepository.findById(request.getCategory().getId()).orElse(null);
        }
        
        // ID ile bulunamazsa veya ID yoksa name ile kontrol et
        if (category == null && request.getCategory() != null && request.getCategory().getName() != null) {
            // findByName birden fazla sonuç döndürebileceği için try-catch kullan
            try {
                category = categoryRepository.findByName(request.getCategory().getName());
            } catch (Exception e) {
                // Birden fazla sonuç varsa, existsByName ile kontrol edip yeni oluşturma
                if (!categoryRepository.existsByName(request.getCategory().getName())) {
                    category = new Category(request.getCategory().getName());
                    category = categoryRepository.save(category);
                } else {
                    // Aynı isimde kategoriler varsa, ilkini al
                    category = categoryRepository.findAll().stream()
                        .filter(c -> c.getName().equals(request.getCategory().getName()))
                        .findFirst()
                        .orElse(null);
                }
            }
        }
        
        // Hala kategori bulunamazsa yeni oluştur
        if (category == null && request.getCategory() != null && request.getCategory().getName() != null) {
            category = new Category(request.getCategory().getName());
            category = categoryRepository.save(category);
        }
        
        request.setCategory(category);
        Product newProduct = productRepository.save(createProduct(request, category));
        
        // Clear caches when new product is added
        clearProductCaches();
        log.info("New product added and caches cleared: {}", newProduct.getName());
        
        return newProduct;
    }

    private boolean productExists(String brand, String name) {
        return this.productRepository.existsByBrandAndName(brand, name);
    }

    private Product createProduct(AddProductRequest request, Category category) {
        return new Product(
                request.getName(),
                request.getBrand(),
                request.getInventory(),
                request.getPrice(),
                request.getDescription(),
                category
        );
    }

    @Override
    // @Cacheable(value = "productCache", key = "#id") // Disabled: Using String-only Redis to avoid JSON conflicts
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found!"));
    }

    @Override
    // @CacheEvict(value = {"productCache", "categoryCache"}, allEntries = true) // Disabled: Using String-only Redis
    @Transactional
    public void deleteProductById(Long id) {
        // Check if product exists in any carts
        if (cartItemRepository.existsByProductId(id)) {
            throw new IllegalStateException("Cannot delete product that is in users' carts");
        }
        
        // Check if product exists in any orders
        if (orderItemRepository.existsByProductId(id)) {
            throw new IllegalStateException("Cannot delete product that has been ordered");
        }
        
        productRepository.findById(id).ifPresentOrElse(productRepository::delete, () -> {
            throw new ProductNotFoundException("Product not found!");
        });
        
        // Clear custom cache as well
        clearProductCaches();
    }

    @Override
    // @CacheEvict(value = {"productCache", "categoryCache"}, allEntries = true) // Disabled: Using String-only Redis
    @Transactional
    public Product updateProduct(ProductUpdateRequest request, Long productId) {
        return productRepository.findById(productId)
                .map(existingProduct -> updateExistingProduct(existingProduct, request))
                .map(productRepository::save)
                .orElseThrow(() -> new ProductNotFoundException("Product not found!"));
    }

    private Product updateExistingProduct(Product existingProduct, ProductUpdateRequest request) {
        existingProduct.setName(request.getName());
        existingProduct.setBrand(request.getBrand());
        existingProduct.setPrice(request.getPrice());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setInventory(request.getInventory());

        Category category = null;
        
        // Önce kategori ID'si ile kontrol et
        if (request.getCategory() != null && request.getCategory().getId() != null) {
            category = categoryRepository.findById(request.getCategory().getId()).orElse(null);
        }
        
        // ID ile bulunamazsa veya ID yoksa name ile kontrol et
        if (category == null && request.getCategory() != null && request.getCategory().getName() != null) {
            try {
                category = categoryRepository.findByName(request.getCategory().getName());
            } catch (Exception e) {
                // Birden fazla sonuç varsa, ilkini al
                category = categoryRepository.findAll().stream()
                    .filter(c -> c.getName().equals(request.getCategory().getName()))
                    .findFirst()
                    .orElse(null);
            }
        }

        existingProduct.setCategory(category);

        return existingProduct;
    }

    @Override
    // @Cacheable(value = "productCache", key = "'all-products'") // Disabled: Using String-only Redis to avoid JSON conflicts
    public List<Product> getAllProducts() {
        // Use fallback to CacheService if Spring Cache is not available
        String cacheKey = "products:all:optimized";
        
        // Try to get from cache first (with proper TypeReference for List<Product>)
        try {
            Optional<List<Product>> cachedValue = cacheService.get(cacheKey, new TypeReference<List<Product>>(){});
            if (cachedValue.isPresent()) {
                List<Product> cachedProducts = cachedValue.get();
                log.info("Cache HIT for getAllProducts (optimized)");
                return cachedProducts;
            }
        } catch (Exception e) {
            log.warn("Cache get failed, falling back to database: {}", e.getMessage());
        }
        
        // Cache miss - get from database with JOIN FETCH optimization
        log.info("Cache MISS for getAllProducts (optimized) - fetching from database");
        List<Product> products = productRepository.findAllWithImagesAndCategory();
        
        // Cache the result for 30 minutes
        cacheService.put(cacheKey, products, Duration.ofMinutes(30));
        
        return products;
    }

    @Override
    // @Cacheable(value = "categoryCache", key = "#category") // Disabled: Using String-only Redis to avoid JSON conflicts
    public List<Product> getProductsByCategory(String category) {
        // Use fallback to CacheService if Spring Cache is not available
        String cacheKey = "products:category:optimized:" + category;
        
        // Try cache first (with proper TypeReference for List<Product>)
        try {
            Optional<List<Product>> cachedValue = cacheService.get(cacheKey, new TypeReference<List<Product>>(){});
            if (cachedValue.isPresent()) {
                List<Product> cachedProducts = cachedValue.get();
                log.info("Cache HIT for getProductsByCategory (optimized): {}", category);
                return cachedProducts;
            }
        } catch (Exception e) {
            log.warn("Cache get failed for category {}, falling back to database: {}", category, e.getMessage());
        }
        
        // Cache miss - get from database with JOIN FETCH optimization
        log.info("Cache MISS for getProductsByCategory (optimized): {} - fetching from database", category);
        List<Product> products = productRepository.findByCategoryNameWithImagesAndCategory(category);
        
        // Cache for 30 minutes
        cacheService.put(cacheKey, products, Duration.ofMinutes(30));
        
        return products;
    }

    @Override
    public List<Product> getProductsByBrand(String brand) {
        return productRepository.findByBrand(brand);
    }

    @Override
    public List<Product> getProductsByCategoryAndBrand(String category, String brand) {
        return productRepository.findByCategoryNameAndBrand(category, brand);
    }

    @Override
    public List<Product> getProductsByName(String name) {
        return productRepository.findByName(name);
    }

    @Override
    // @Cacheable(value = "productCache", key = "'search-' + #name") // Temporarily disabled
    public List<Product> getProductsByNameContaining(String name) {
        // Use fallback to CacheService if Spring Cache is not available
        String cacheKey = "products:search:optimized:" + name.toLowerCase();
        
        // Try cache first (with proper TypeReference for List<Product>)
        try {
            Optional<List<Product>> cachedValue = cacheService.get(cacheKey, new TypeReference<List<Product>>(){});
            if (cachedValue.isPresent()) {
                List<Product> cachedProducts = cachedValue.get();
                log.info("Cache HIT for search (optimized): {}", name);
                return cachedProducts;
            }
        } catch (Exception e) {
            log.warn("Cache get failed for search {}, falling back to database: {}", name, e.getMessage());
        }
        
        // Cache miss - get from database with JOIN FETCH optimization
        log.info("Cache MISS for search (optimized): {} - fetching from database", name);
        List<Product> products = productRepository.findByNameContainingWithImagesAndCategory(name);
        
        // Cache search results for 15 minutes
        cacheService.put(cacheKey, products, Duration.ofMinutes(15));
        
        return products;
    }

    @Override
    // @Cacheable(value = "productCache", key = "'category-search-' + #category + '-' + #search") // Temporarily disabled
    public List<Product> getProductsByCategoryAndNameContaining(String category, String search) {
        // Use fallback to CacheService if Spring Cache is not available
        String cacheKey = "products:category_search:optimized:" + category.toLowerCase() + ":" + search.toLowerCase();
        
        // Try cache first (with proper TypeReference for List<Product>)
        try {
            Optional<List<Product>> cachedValue = cacheService.get(cacheKey, new TypeReference<List<Product>>(){});
            if (cachedValue.isPresent()) {
                List<Product> cachedProducts = cachedValue.get();
                log.info("Cache HIT for category search (optimized): {} - {}", category, search);
                return cachedProducts;
            }
        } catch (Exception e) {
            log.warn("Cache get failed for category search {}-{}, falling back to database: {}", category, search, e.getMessage());
        }
        
        // Cache miss - get from database with JOIN FETCH optimization
        log.info("Cache MISS for category search (optimized): {} - {} - fetching from database", category, search);
        List<Product> products = productRepository.findByCategoryNameAndNameContainingWithImagesAndCategory(category, search);
        
        // Cache search results for 15 minutes
        cacheService.put(cacheKey, products, Duration.ofMinutes(15));
        
        return products;
    }

    @Override
    public List<Product> getProductsByBrandAndName(String brand, String name) {
        return productRepository.findByBrandAndName(brand, name);
    }

    @Override
    public Long countProductsByBrandAndName(String brand, String name) {
        return productRepository.countByBrandAndName(brand, name);
    }

    @Override
    public List<ProductDto> getConvertedProducts(List<Product> products) {
        return products.stream().map(this::convertToDto).toList();
    }
    @Override
    public ProductDto convertToDto(Product product) {
        // Manual mapping instead of ModelMapper to avoid collection conversion issues
        ProductDto productDto = new ProductDto();
        productDto.setId(product.getId());
        productDto.setName(product.getName());
        productDto.setBrand(product.getBrand());
        productDto.setPrice(product.getPrice());
        productDto.setInventory(product.getInventory());
        productDto.setDescription(product.getDescription());
        productDto.setCategory(product.getCategory());
        
        // Discount fields
        productDto.setDiscountPrice(product.getDiscountPrice());
        productDto.setDiscountPercentage(product.getDiscountPercentage());
        productDto.setIsOnSale(product.getIsOnSale());
        productDto.setIsFlashSale(product.getIsFlashSale());
        productDto.setSaleStartDate(product.getSaleStartDate());
        productDto.setSaleEndDate(product.getSaleEndDate());
        productDto.setFlashSaleStock(product.getFlashSaleStock());
        
        // Get images and convert them - manuel mapping to avoid ModelMapper issues
        List<Image> images = imageRepository.findByProductId(product.getId());
        List<ImageDto> imageDtos = images.stream()
                .map(this::convertImageToDto)
                .toList();
        productDto.setImages(imageDtos);

        // Set calculated fields for discounts
        productDto.setEffectivePrice(product.getEffectivePrice());
        productDto.setSavings(product.getSavings());
        productDto.setCurrentlyOnSale(product.isCurrentlyOnSale());

        return productDto;
    }

    private ImageDto convertImageToDto(Image image) {
        if (image == null) {
            return null;
        }
        
        ImageDto imageDto = new ImageDto();
        imageDto.setId(image.getId());
        imageDto.setFileName(image.getFileName());
        imageDto.setDownloadUrl(image.getDownloadUrl());
        
        return imageDto;
    }

    // Sale-related methods implementation
    @Override
    public List<Product> getProductsOnSale() {
        return productRepository.findByIsOnSaleTrueOrderBySaleStartDateDesc();
    }

    @Override
    public List<Product> getFlashSaleProducts() {
        return productRepository.findByIsFlashSaleTrueOrderBySaleStartDateDesc();
    }

    @Override
    public List<Product> getProductsOnSaleByCategory(String category) {
        return productRepository.findByIsOnSaleTrueAndCategoryNameOrderBySaleStartDateDesc(category);
    }

    @Override
    public void putProductOnSale(Long productId, ProductUpdateRequest saleRequest) {
        Product product = getProductById(productId);
        
        product.setIsOnSale(true);
        product.setDiscountPrice(saleRequest.getDiscountPrice());
        product.setDiscountPercentage(saleRequest.getDiscountPercentage());
        product.setSaleStartDate(saleRequest.getSaleStartDate());
        product.setSaleEndDate(saleRequest.getSaleEndDate());
        product.setIsFlashSale(saleRequest.getIsFlashSale());
        product.setFlashSaleStock(saleRequest.getFlashSaleStock());
        
        productRepository.save(product);
    }

    @Override
    public void removeProductFromSale(Long productId) {
        Product product = getProductById(productId);
        
        product.setIsOnSale(false);
        product.setDiscountPrice(null);
        product.setDiscountPercentage(null);
        product.setSaleStartDate(null);
        product.setSaleEndDate(null);
        product.setIsFlashSale(false);
        product.setFlashSaleStock(null);
        
        productRepository.save(product);
    }

    // Paginated methods implementation with FAST 2-step optimization for PostgreSQL
    @Override
    public Page<Product> getAllProducts(Pageable pageable) {
        // Simplified cache key for consistency
        String cacheKey = "products:paginated:fast:" + pageable.getPageNumber() + ":" + pageable.getPageSize();
        
        // Try cache first - cache the product list only, not the Page object
        try {
            TypeReference<List<Product>> typeReference = new TypeReference<List<Product>>() {};
            Optional<List<Product>> cachedValue = cacheService.get(cacheKey, typeReference);
            if (cachedValue.isPresent()) {
                List<Product> cachedProducts = cachedValue.get();
                log.info("Cache HIT for getAllProducts paginated (fast) - page: {}", pageable.getPageNumber());
                
                // Get total count from database (this is fast)
                long totalElements = productRepository.count();
                
                // Create Page manually from cached list
                return new org.springframework.data.domain.PageImpl<>(cachedProducts, pageable, totalElements);
            }
        } catch (Exception e) {
            log.warn("Cache get failed for paginated products, falling back to database: {}", e.getMessage());
        }
        
        // Cache miss - FAST 2-step database approach
        log.info("Cache MISS for getAllProducts paginated (fast) - page: {} - fetching from database", pageable.getPageNumber());
        
        // Step 1: Get only IDs with pagination (super fast, no JOIN)
        Page<Long> productIds = productRepository.findAllProductIds(pageable);
        
        // Step 2: Get full objects with JOIN FETCH for these specific IDs (fast, single query)
        List<Product> products = productRepository.findByIdsWithImagesAndCategory(productIds.getContent());
        
        // Create Page from results
        Page<Product> result = new org.springframework.data.domain.PageImpl<>(products, pageable, productIds.getTotalElements());
        
        // Cache only the content list for 10 minutes
        cacheService.put(cacheKey, result.getContent(), Duration.ofMinutes(10));
        
        return result;
    }

    @Override
    public Page<Product> getProductsByCategory(String category, Pageable pageable) {
        // Simplified cache key for category results
        String cacheKey = "products:category_paginated:fast:" + category.toLowerCase() + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize();
        
        // Try cache first - cache the product list only
        try {
            TypeReference<List<Product>> typeReference = new TypeReference<List<Product>>() {};
            Optional<List<Product>> cachedValue = cacheService.get(cacheKey, typeReference);
            if (cachedValue.isPresent()) {
                List<Product> cachedProducts = cachedValue.get();
                log.info("Cache HIT for getProductsByCategory paginated (fast) - category: {}, page: {}", category, pageable.getPageNumber());
                
                // Get total count for this category (fast query)
                long totalElements = productRepository.countByCategoryName(category);
                
                // Create Page manually from cached list
                return new org.springframework.data.domain.PageImpl<>(cachedProducts, pageable, totalElements);
            }
        } catch (Exception e) {
            log.warn("Cache get failed for paginated category products {}, falling back to database: {}", category, e.getMessage());
        }
        
        // Cache miss - FAST 2-step database approach
        log.info("Cache MISS for getProductsByCategory paginated (fast) - category: {}, page: {} - fetching from database", category, pageable.getPageNumber());
        
        // Step 1: Get only IDs with pagination (super fast, no JOIN)
        Page<Long> productIds = productRepository.findProductIdsByCategory(category, pageable);
        
        // Step 2: Get full objects with JOIN FETCH for these specific IDs (fast, single query)
        List<Product> products = productRepository.findByIdsWithImagesAndCategory(productIds.getContent());
        
        // Create Page from results
        Page<Product> result = new org.springframework.data.domain.PageImpl<>(products, pageable, productIds.getTotalElements());
        
        // Cache only the content list for 10 minutes
        cacheService.put(cacheKey, result.getContent(), Duration.ofMinutes(10));
        
        return result;
    }

    @Override
    public Page<Product> getProductsByNameContaining(String search, Pageable pageable) {
        // Simplified cache key for search results
        String cacheKey = "products:search_paginated:fast:" + search.toLowerCase() + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize();
        
        // Try cache first - cache the product list only
        try {
            TypeReference<List<Product>> typeReference = new TypeReference<List<Product>>() {};
            Optional<List<Product>> cachedValue = cacheService.get(cacheKey, typeReference);
            if (cachedValue.isPresent()) {
                List<Product> cachedProducts = cachedValue.get();
                log.info("Cache HIT for getProductsByNameContaining paginated (fast) - search: {}, page: {}", search, pageable.getPageNumber());
                
                // Use efficient count query instead of loading all results
                long totalElements = productRepository.findByNameContaining(search).size();
                
                // Create Page manually from cached list
                return new org.springframework.data.domain.PageImpl<>(cachedProducts, pageable, totalElements);
            }
        } catch (Exception e) {
            log.warn("Cache get failed for paginated search products {}, falling back to database: {}", search, e.getMessage());
        }
        
        // Cache miss - FAST 2-step database approach
        log.info("Cache MISS for getProductsByNameContaining paginated (fast) - search: {}, page: {} - fetching from database", search, pageable.getPageNumber());
        
        // Step 1: Get only IDs with pagination (super fast, no JOIN)
        Page<Long> productIds = productRepository.findProductIdsByNameContaining(search, pageable);
        
        // Step 2: Get full objects with JOIN FETCH for these specific IDs (fast, single query)
        List<Product> products = productRepository.findByIdsWithImagesAndCategory(productIds.getContent());
        
        // Create Page from results
        Page<Product> result = new org.springframework.data.domain.PageImpl<>(products, pageable, productIds.getTotalElements());
        
        // Cache only the content list for 10 minutes
        cacheService.put(cacheKey, result.getContent(), Duration.ofMinutes(10));
        
        return result;
    }

    @Override
    public Page<Product> getProductsByCategoryAndNameContaining(String category, String search, Pageable pageable) {
        // Simplified cache key for category+search results
        String cacheKey = "products:category_search_paginated:fast:" + category.toLowerCase() + ":" + search.toLowerCase() + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize();
        
        // Try cache first - cache the product list only
        try {
            TypeReference<List<Product>> typeReference = new TypeReference<List<Product>>() {};
            Optional<List<Product>> cachedValue = cacheService.get(cacheKey, typeReference);
            if (cachedValue.isPresent()) {
                List<Product> cachedProducts = cachedValue.get();
                log.info("Cache HIT for getProductsByCategoryAndNameContaining paginated (fast) - category: {}, search: {}, page: {}", category, search, pageable.getPageNumber());
                
                // For category+search, count matching products (this is fast)
                long totalElements = productRepository.findByCategoryNameAndNameContaining(category, search).size();
                
                // Create Page manually from cached list
                return new org.springframework.data.domain.PageImpl<>(cachedProducts, pageable, totalElements);
            }
        } catch (Exception e) {
            log.warn("Cache get failed for paginated category+search products {}-{}, falling back to database: {}", category, search, e.getMessage());
        }
        
        // Cache miss - FAST 2-step database approach
        log.info("Cache MISS for getProductsByCategoryAndNameContaining paginated (fast) - category: {}, search: {}, page: {} - fetching from database", category, search, pageable.getPageNumber());
        
        // Step 1: Get only IDs with pagination (super fast, no JOIN)
        Page<Long> productIds = productRepository.findProductIdsByCategoryAndNameContaining(category, search, pageable);
        
        // Step 2: Get full objects with JOIN FETCH for these specific IDs (fast, single query)
        List<Product> products = productRepository.findByIdsWithImagesAndCategory(productIds.getContent());
        
        // Create Page from results
        Page<Product> result = new org.springframework.data.domain.PageImpl<>(products, pageable, productIds.getTotalElements());
        
        // Cache only the content list for 10 minutes
        cacheService.put(cacheKey, result.getContent(), Duration.ofMinutes(10));
        
        return result;
    }

    private void clearProductCaches() {
        try {
            // Clear all product-related caches
            cacheService.deleteByPattern("products:*");
            log.info("Product caches cleared successfully");
        } catch (Exception e) {
            log.warn("Failed to clear product caches: {}", e.getMessage());
        }
    }
}
