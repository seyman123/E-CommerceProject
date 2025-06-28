package com.seyman.dreamshops.data;

import com.seyman.dreamshops.exceptions.ResourceNotFoundException;
import com.seyman.dreamshops.model.*;
import com.seyman.dreamshops.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Transactional
@Component 
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationListener<ApplicationReadyEvent> {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ImageRepository imageRepository;
    private final CouponRepository couponRepository;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Prevent multiple initializations in cloud environments
        if (System.getProperty("data.initialized") != null) {
            log.info("Data already initialized, skipping...");
            return;
        }
        
        try {
            Set<String> defaultRoles = Set.of("ROLE_ADMIN", "ROLE_USER");
            createDefaultRoleIfNotExists(defaultRoles);
            createDefaultUserIfNotExists();
            createDefaultAdminIfNotExists();
            createDefaultCategoriesIfNotExists();
            createDefaultProductsIfNotExists();
            createSampleSaleData();
            createSampleCoupons();
            
            // Mark as initialized
            System.setProperty("data.initialized", "true");
            log.info("Data initialization completed successfully");
        } catch (Exception e) {
            log.error("Data initialization failed: {}", e.getMessage(), e);
        }
    }

    private void createDefaultUserIfNotExists() {
        Role userRole =  roleRepository.findByName("ROLE_USER").get();
        for (int i = 1; i <= 5; i++) {
            String defaultEmail = "user" + i + "@gmail.com";

            if (userRepository.existsByEmail(defaultEmail)) {
                continue;
            }
            User user = new User();
            user.setFirstName("The User");
            user.setLastName("User" + i);
            user.setEmail(defaultEmail);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRoles(Set.of(userRole));
            userRepository.save(user);
        }
    }

    private void createDefaultAdminIfNotExists() {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").get();
        for (int i = 1; i <= 2; i++) {
            String defaultEmail = "admin" + i + "@gmail.com";

            if (userRepository.existsByEmail(defaultEmail)) {
                continue;
            }
            User user = new User();
            user.setFirstName("The Admin");
            user.setLastName("Admin" + i);
            user.setEmail(defaultEmail);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRoles(Set.of(adminRole));
            userRepository.save(user);
        }
    }

    private void createDefaultRoleIfNotExists(Set<String> roles) {
        roles.stream()
                .filter(role -> roleRepository.findByName(role).isEmpty())
                .map(Role::new).forEach(roleRepository::save);
    }

    private void createDefaultCategoriesIfNotExists() {
        String[] categoryNames = {
            "iPhone", 
            "Mac", 
            "iPad", 
            "Apple Watch", 
            "AirPods & Audio", 
            "Apple TV & Display", 
            "Accessories", 
            "Other Brands"
        };
        
        for (String categoryName : categoryNames) {
            if (!categoryRepository.existsByName(categoryName)) {
                Category category = new Category();
                category.setName(categoryName);
                categoryRepository.save(category);
            }
        }
    }

    private void createDefaultProductsIfNotExists() {
        if (productRepository.count() > 0) {
            return; // Products already exist
        }

        // Get categories
        Category iphone = categoryRepository.findByName("iPhone");
        Category mac = categoryRepository.findByName("Mac");
        Category ipad = categoryRepository.findByName("iPad");
        Category watch = categoryRepository.findByName("Apple Watch");
        Category audio = categoryRepository.findByName("AirPods & Audio");
        Category tv = categoryRepository.findByName("Apple TV & Display");
        Category accessories = categoryRepository.findByName("Accessories");
        Category others = categoryRepository.findByName("Other Brands");

        // iPhone Products
        createProductWithImage("iPhone 16 Pro Max", "Apple", new BigDecimal("54999.00"), 15, 
                "Apple iPhone 16 Pro Max - 256GB Titanyum Siyah. A18 Pro çip, 48MP kamera sistemi", iphone, "iphone-16-pro.jpg");
        
        createProductWithImage("iPhone 16 Pro", "Apple", new BigDecimal("49999.00"), 20, 
                "Apple iPhone 16 Pro - 128GB Titanyum Doğal. En gelişmiş iPhone kamera sistemi", iphone, "iphone-16-pro.jpg");
        
        createProductWithImage("iPhone 15", "Apple", new BigDecimal("39999.00"), 25, 
                "Apple iPhone 15 - 128GB Mavi. A16 Bionic çip, Dynamic Island", iphone, "iphone-15.jpg");
        
        createProductWithImage("iPhone SE 3. Nesil", "Apple", new BigDecimal("19999.00"), 30, 
                "Apple iPhone SE 3. nesil - 64GB Gece Yarısı. A15 Bionic çip, Touch ID", iphone, "iphone-se.jpg");

        // Mac Products  
        createProductWithImage("MacBook Pro 14'' M3 Pro", "Apple", new BigDecimal("89999.00"), 8, 
                "Apple MacBook Pro 14'' M3 Pro çip, 18GB RAM, 512GB SSD. Liquid Retina XDR ekran", mac, "macbook-pro-14.jpg");
        
        createProductWithImage("MacBook Air 13'' M3", "Apple", new BigDecimal("44999.00"), 12, 
                "Apple MacBook Air 13'' M3 çip, 8GB RAM, 256GB SSD. Gece Yarısı rengi", mac, "macbook-air-13.jpg");
        
        createProductWithImage("iMac 24'' M3", "Apple", new BigDecimal("59999.00"), 6, 
                "Apple iMac 24'' M3 çip, 8GB RAM, 256GB SSD. 4.5K Retina ekran, Mavi", mac, "imac-24.jpg");
        
        createProductWithImage("Mac Studio M2 Ultra", "Apple", new BigDecimal("179999.00"), 3, 
                "Apple Mac Studio M2 Ultra çip, 64GB RAM, 1TB SSD. Profesyonel performans", mac, "mac-studio.jpg");

        // iPad Products
        createProductWithImage("iPad Pro 12.9'' M4", "Apple", new BigDecimal("54999.00"), 10, 
                "Apple iPad Pro 12.9'' M4 çip, 256GB, Wi-Fi. Liquid Retina XDR ekran", ipad, "ipad-pro-12.jpg");
        
        createProductWithImage("iPad Air 11'' M2", "Apple", new BigDecimal("29999.00"), 15, 
                "Apple iPad Air 11'' M2 çip, 128GB, Wi-Fi. Liquid Retina ekran, Uzay Grisi", ipad, "ipad-air-11.jpg");
        
        createProductWithImage("iPad 10. Nesil", "Apple", new BigDecimal("19999.00"), 20, 
                "Apple iPad 10. nesil, A14 Bionic çip, 64GB, Wi-Fi. 10.9'' Liquid Retina ekran", ipad, "ipad-10.jpg");

        // Apple Watch Products
        createProductWithImage("Apple Watch Ultra 2", "Apple", new BigDecimal("34999.00"), 12, 
                "Apple Watch Ultra 2 GPS + Cellular 49mm Titanyum Kasa. En dayanıklı Apple Watch", watch, "watch-ultra-2.jpg");
        
        createProductWithImage("Apple Watch Series 10", "Apple", new BigDecimal("17999.00"), 18, 
                "Apple Watch Series 10 GPS 42mm Alüminyum Kasa. Gelişmiş sağlık özellikleri", watch, "watch-series-10.jpg");
        
        createProductWithImage("Apple Watch SE 2. Nesil", "Apple", new BigDecimal("11999.00"), 25, 
                "Apple Watch SE 2. nesil GPS 40mm Alüminyum Kasa. Temel Apple Watch deneyimi", watch, "watch-se.jpg");

        // AirPods & Audio Products
        createProductWithImage("AirPods Pro 2. Nesil", "Apple", new BigDecimal("12999.00"), 30, 
                "Apple AirPods Pro 2. nesil USB-C. Aktif Gürültü Engelleme, Spatial Audio", audio, "airpods-pro-2.jpg");
        
        createProductWithImage("AirPods Max", "Apple", new BigDecimal("24999.00"), 8, 
                "Apple AirPods Max Uzay Grisi. Premium ses kalitesi, Aktif Gürültü Engelleme", audio, "airpods-max.jpg");
        
        createProductWithImage("AirPods 3. Nesil", "Apple", new BigDecimal("7999.00"), 25, 
                "Apple AirPods 3. nesil Lightning. Spatial Audio, Ter ve su direnci", audio, "airpods-3.jpg");

        // Apple TV & Display Products  
        createProductWithImage("Apple TV 4K", "Apple", new BigDecimal("8999.00"), 15, 
                "Apple TV 4K 64GB. A15 Bionic çip, 4K HDR, Dolby Vision ve Atmos desteği", tv, "apple-tv-4k.jpg");
        
        createProductWithImage("Studio Display", "Apple", new BigDecimal("74999.00"), 4, 
                "Apple Studio Display 27'' 5K Retina ekran. 12MP Ultra Wide kamera, altı hoparlör", tv, "studio-display.jpg");

        // Accessories
        createProductWithImage("MagSafe Charger", "Apple", new BigDecimal("1999.00"), 50, 
                "Apple MagSafe Charger. iPhone 12 ve sonrası için manyetik kablosuz şarj", accessories, "magsafe-charger.jpg");
        
        createProductWithImage("Magic Keyboard", "Apple", new BigDecimal("5999.00"), 20, 
                "Apple Magic Keyboard Türkçe Q klavye. Touch ID ve sayısal tuş takımı", accessories, "magic-keyboard.jpg");
        
        createProductWithImage("Magic Mouse", "Apple", new BigDecimal("3999.00"), 25, 
                "Apple Magic Mouse Beyaz. Multi-Touch yüzey, şarj edilebilir pil", accessories, "magic-mouse.jpg");

        // Other Premium Brands (Minority)
        createProductWithImage("Samsung Galaxy S24 Ultra", "Samsung", new BigDecimal("44999.00"), 8, 
                "Samsung Galaxy S24 Ultra 256GB Titanyum Siyah. S Pen dahil, 200MP kamera", others, "galaxy-s24-ultra.jpg");
        
        createProductWithImage("Sony WH-1000XM5", "Sony", new BigDecimal("14999.00"), 15, 
                "Sony WH-1000XM5 Kablosuz Gürültü Engelleyici Kulaklık. 30 saat pil ömrü", others, "sony-wh1000xm5.jpg");
        
        createProductWithImage("Microsoft Surface Pro 9", "Microsoft", new BigDecimal("49999.00"), 6, 
                "Microsoft Surface Pro 9 Intel i5, 8GB RAM, 256GB SSD. 2-in-1 laptop/tablet", others, "surface-pro-9.jpg");

        System.out.println("Apple-focused tech products created successfully.");
    }

    private void createSampleSaleData() {
        // First, initialize discount fields for all existing products that don't have them set
        initializeDiscountFieldsForExistingProducts();
        
        // Put some products on sale
        Product iphone = productRepository.findByBrandAndName("Apple", "iPhone 15 Pro").stream().findFirst().orElse(null);
        if (iphone != null && !Boolean.TRUE.equals(iphone.getIsOnSale())) {
            iphone.setIsOnSale(true);
            iphone.setIsFlashSale(true);
            iphone.setDiscountPrice(new BigDecimal("1099.99"));
            iphone.setDiscountPercentage(15);
            iphone.setSaleStartDate(LocalDateTime.now().minusHours(1));
            iphone.setSaleEndDate(LocalDateTime.now().plusDays(2));
            iphone.setFlashSaleStock(12);
            productRepository.save(iphone);
        }

        Product macbook = productRepository.findByBrandAndName("Apple", "MacBook Air M3").stream().findFirst().orElse(null);
        if (macbook != null && !Boolean.TRUE.equals(macbook.getIsOnSale())) {
            macbook.setIsOnSale(true);
            macbook.setIsFlashSale(true);
            macbook.setDiscountPrice(new BigDecimal("999.99"));
            macbook.setDiscountPercentage(17);
            macbook.setSaleStartDate(LocalDateTime.now().minusHours(2));
            macbook.setSaleEndDate(LocalDateTime.now().plusDays(1));
            macbook.setFlashSaleStock(8);
            productRepository.save(macbook);
        }

        Product samsung = productRepository.findByBrandAndName("Samsung", "Samsung Galaxy S24").stream().findFirst().orElse(null);
        if (samsung != null && !Boolean.TRUE.equals(samsung.getIsOnSale())) {
            samsung.setIsOnSale(true);
            samsung.setDiscountPrice(new BigDecimal("749.99"));
            samsung.setDiscountPercentage(17);
            samsung.setSaleStartDate(LocalDateTime.now().minusHours(3));
            samsung.setSaleEndDate(LocalDateTime.now().plusDays(7));
            productRepository.save(samsung);
        }

        Product headset = productRepository.findByBrandAndName("SteelSeries", "Gaming Headset").stream().findFirst().orElse(null);
        if (headset != null && !Boolean.TRUE.equals(headset.getIsOnSale())) {
            headset.setIsOnSale(true);
            headset.setDiscountPrice(new BigDecimal("149.99"));
            headset.setDiscountPercentage(25);
            headset.setSaleStartDate(LocalDateTime.now().minusHours(1));
            headset.setSaleEndDate(LocalDateTime.now().plusDays(5));
            productRepository.save(headset);
        }
    }

    private void initializeDiscountFieldsForExistingProducts() {
        List<Product> allProducts = productRepository.findAll();
        boolean needsUpdate = false;
        
        for (Product product : allProducts) {
            if (product.getIsOnSale() == null) {
                product.setIsOnSale(false);
                needsUpdate = true;
            }
            if (product.getIsFlashSale() == null) {
                product.setIsFlashSale(false);
                needsUpdate = true;
            }
        }
        
        if (needsUpdate) {
            productRepository.saveAll(allProducts);
        }
    }

    private void createSampleCoupons() {
        if (couponRepository.count() > 0) {
            return; // Coupons already exist
        }

        // Welcome coupon
        Coupon welcomeCoupon = new Coupon(
            "WELCOME20",
            "Yeni üyelere özel %20 indirim",
            Coupon.CouponType.WELCOME,
            Coupon.DiscountType.PERCENTAGE,
            new BigDecimal("20"),
            new BigDecimal("100"),
            LocalDateTime.now().plusMonths(1)
        );
        welcomeCoupon.setUsageLimit(100);
        couponRepository.save(welcomeCoupon);

        // Flash sale coupon
        Coupon flashCoupon = new Coupon(
            "FLASH50",
            "Flash sale ürünlerinde 50₺ indirim",
            Coupon.CouponType.FLASH_SALE,
            Coupon.DiscountType.FIXED_AMOUNT,
            new BigDecimal("50"),
            new BigDecimal("500"),
            LocalDateTime.now().plusDays(1)
        );
        flashCoupon.setUsageLimit(50);
        couponRepository.save(flashCoupon);

        // Student coupon
        Coupon studentCoupon = new Coupon(
            "STUDENT15",
            "Öğrencilere özel %15 indirim",
            Coupon.CouponType.STUDENT,
            Coupon.DiscountType.PERCENTAGE,
            new BigDecimal("15"),
            new BigDecimal("200"),
            LocalDateTime.now().plusMonths(2)
        );
        studentCoupon.setUsageLimit(200);
        couponRepository.save(studentCoupon);

        // Mega discount coupon
        Coupon megaCoupon = new Coupon(
            "MEGA100",
            "3000₺ üzeri alışverişlerde 100₺ indirim",
            Coupon.CouponType.MEGA_DISCOUNT,
            Coupon.DiscountType.FIXED_AMOUNT,
            new BigDecimal("100"),
            new BigDecimal("3000"),
            LocalDateTime.now().plusWeeks(2)
        );
        megaCoupon.setMaxDiscountAmount(new BigDecimal("100"));
        megaCoupon.setUsageLimit(30);
        couponRepository.save(megaCoupon);

        System.out.println("Sample coupons created successfully.");
    }

    private void createProductWithImage(String name, String brand, BigDecimal price, int inventory, 
                                      String description, Category category, String imageName) {
        Product product = new Product();
        product.setName(name);
        product.setBrand(brand);
        product.setPrice(price);
        product.setInventory(inventory);
        product.setDescription(description);
        product.setCategory(category);
        
        // Initialize discount fields
        product.setIsOnSale(false);
        product.setIsFlashSale(false);
        
        Product savedProduct = productRepository.save(product);
        
        // Create placeholder image
        createDefaultImage(savedProduct, imageName);
    }

    private void createDefaultImage(Product product, String imageName) {
        try {
            // Local image mapping from /resimler folder
            String localImageFile = getLocalImageFile(product.getName());
            
            // Create a placeholder with reference to local file
            String placeholderImageData = "local-image-reference-" + localImageFile;
            byte[] imageBytes = placeholderImageData.getBytes();
            
            Image image = new Image();
            image.setFileName(localImageFile);
            image.setFileType("image/jpeg");
            image.setImage(new SerialBlob(imageBytes));
            image.setProduct(product);
            
            // First save to get the ID
            Image savedImage = imageRepository.save(image);
            
            // Use local image service URL
            String downloadUrl = "/api/v1/images/image/" + savedImage.getId();
            savedImage.setDownloadUrl(downloadUrl);
            
            // Save again with the correct URL
            imageRepository.save(savedImage);
            
        } catch (SQLException e) {
            System.err.println("Error creating image for product: " + product.getName());
        }
    }
    
    private String getLocalImageFile(String productName) {
        // Map products to actual files in /resimler folder
        switch (productName.toLowerCase()) {
            case "iphone 16 pro max":
            case "iphone 16 pro":
                return "iPhone-16-Pro-9.jpg";
            case "iphone 15":
                return "Apple-iPhone-16-Pro-hero-geo-240909_inline.jpg.large.jpg";
            case "iphone se 3. nesil":
                return "1-289_large.webp";
            case "macbook pro 14'' m3 pro":
                return "macbook-pro-14-m2-max.webp";
            case "macbook air 13'' m3":
                return "MXCL3TQ.jpg";
            case "imac 24'' m3":
                return "og__eui2mpgzwyaa_overview.png";
            case "mac studio m2 ultra":
                return "maxresdefault.jpg";
            case "ipad pro 12.9'' m4":
                return "ipad-pro-finish-select-202405-11inch-spaceblack-glossy-wifi_AV1_GEO_EMEA_FMT_WHH.jpg";
            case "ipad air 11'' m2":
                return "mnxf3tua-apple-11-inc-ipad-pro-wi-fi-256gb-uzay-grisi-mnxf3tua-638024012164116553.webp";
            case "ipad 10. nesil":
                return "a1.webp";
            case "apple watch ultra 2":
                return "MXM23ref_FV99_VW_34FR+watch-case-46-aluminum-jetblack-nc-s10_VW_34FR+watch-face-46-aluminum-jetblack-s10_VW_34FR.jpg";
            case "apple watch series 10":
                return "s10-case-unselect-gallery-1-202503_GEO_TR_FMT_WHH.jpg";
            case "apple watch se 2. nesil":
                return "111853_apple-watch-se-2nd-gen.png";
            case "airpods pro 2. nesil":
                return "95b6cedc-f494-46e4-a41c-488f4e2a3f0f.jpg";
            case "airpods max":
                return "713auwYVkHL._AC_UF1000,1000_QL80_.jpg";
            case "airpods 3. nesil":
                return "images (1).jpg";
            case "apple tv 4k":
                return "Apple-TV-4K-1.jpg";
            case "studio display":
                return "studio-display-og-202203.jpg";
            case "magsafe charger":
                return "images (2).jpg";
            case "magic keyboard":
                return "images (3).jpg";
            case "magic mouse":
                return "images (4).jpg";
            case "samsung galaxy s24 ultra":
                return "89905763.webp";
            case "sony wh-1000xm5":
                return "4caf29e5-dd71-4427-be2c-938523ca8b6c.__CR0,0,362,453_PT0_SX362_V1___.jpg";
            case "microsoft surface pro 9":
                return "110000607770892.jpg";
            default:
                return "yeni-proje-2023-07-19t111255320-8e88f6.jpg"; // Default Apple logo
        }
    }
}
