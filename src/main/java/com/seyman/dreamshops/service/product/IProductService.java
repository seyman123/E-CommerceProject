package com.seyman.dreamshops.service.product;

import com.seyman.dreamshops.dto.ProductDto;
import com.seyman.dreamshops.model.Product;
import com.seyman.dreamshops.requests.AddProductRequest;
import com.seyman.dreamshops.requests.ProductUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IProductService {

    Product addProduct(AddProductRequest request);
    Product getProductById(Long id);
    void deleteProductById(Long id);
    Product updateProduct(ProductUpdateRequest request, Long productId);

    List<Product> getAllProducts();
    List<Product> getProductsByCategory(String category);
    List<Product> getProductsByBrand(String brand);
    List<Product> getProductsByCategoryAndBrand(String category, String brand);
    List<Product> getProductsByName(String name);
    List<Product> getProductsByNameContaining(String name);
    List<Product> getProductsByCategoryAndNameContaining(String category, String search);
    List<Product> getProductsByBrandAndName(String brand, String name);

    Long countProductsByBrandAndName(String brand, String name);

    List<ProductDto> getConvertedProducts(List<Product> products);

    ProductDto convertToDto(Product product);
    
    // Sale-related methods
    List<Product> getProductsOnSale();
    List<Product> getFlashSaleProducts();
    List<Product> getProductsOnSaleByCategory(String category);
    void putProductOnSale(Long productId, ProductUpdateRequest saleRequest);
    void removeProductFromSale(Long productId);
    
    // Paginated methods
    Page<Product> getAllProducts(Pageable pageable);
    Page<Product> getProductsByCategory(String category, Pageable pageable);
    Page<Product> getProductsByNameContaining(String search, Pageable pageable);
    Page<Product> getProductsByCategoryAndNameContaining(String category, String search, Pageable pageable);
}
