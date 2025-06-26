package com.seyman.dreamshops.service.image;

import com.seyman.dreamshops.dto.ImageDto;
import com.seyman.dreamshops.model.Image;
import com.seyman.dreamshops.model.Product;
import com.seyman.dreamshops.repository.ImageRepository;
import com.seyman.dreamshops.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedImageService {

    private final ImageRepository imageRepository;
    private final IProductService productService;
    private final ImageOptimizationService optimizationService;

    /**
     * ENHANCED IMAGE UPLOAD - Mevcut sisteme ek optimizasyon
     * Eğer optimization başarısız olursa, orijinal dosya upload edilir
     */
    public List<ImageDto> saveOptimizedImages(List<MultipartFile> files, Long productId) {
        Product product = productService.getProductById(productId);
        List<ImageDto> savedImageDto = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                // 1. Önce resmi optimize etmeyi dene
                byte[] imageData = tryOptimizeImage(file);
                
                // 2. Image entity oluştur
                Image image = new Image();
                image.setFileName(generateOptimizedFileName(file.getOriginalFilename()));
                image.setFileType("image/jpeg"); // Optimized images are JPEG
                image.setImage(new SerialBlob(imageData));
                image.setProduct(product);

                // 3. Save to database
                Image savedImage = imageRepository.save(image);

                // 4. URL generate et
                String downloadUrl = "/images/image/" + savedImage.getId();
                savedImage.setDownloadUrl(downloadUrl);
                savedImage = imageRepository.save(savedImage);

                // 5. DTO oluştur
                ImageDto imageDto = new ImageDto();
                imageDto.setId(savedImage.getId());
                imageDto.setFileName(savedImage.getFileName());
                imageDto.setDownloadUrl(savedImage.getDownloadUrl());
                savedImageDto.add(imageDto);

                log.info("Successfully saved optimized image: {}", savedImage.getFileName());

            } catch (Exception e) {
                log.error("Failed to save optimized image {}, falling back to original upload: {}", 
                    file.getOriginalFilename(), e.getMessage());
                
                // FALLBACK: Orijinal ImageService kullan
                try {
                    ImageDto fallbackDto = saveOriginalImage(file, product);
                    savedImageDto.add(fallbackDto);
                } catch (Exception fallbackError) {
                    log.error("Even fallback failed for {}: {}", file.getOriginalFilename(), fallbackError.getMessage());
                    throw new RuntimeException("Complete image upload failure", fallbackError);
                }
            }
        }

        return savedImageDto;
    }

    /**
     * Güvenli optimization - başarısız olursa orijinal dosyayı döner
     */
    private byte[] tryOptimizeImage(MultipartFile file) throws IOException {
        try {
            // Desktop versiyonu için optimize et (1200px max)
            byte[] optimizedData = optimizationService.optimizeImage(file, ImageOptimizationService.SIZE_DESKTOP);
            
            long originalSize = file.getSize();
            long optimizedSize = optimizedData.length;
            
            if (optimizedSize < originalSize) {
                double compressionRatio = (1.0 - (double) optimizedSize / originalSize) * 100;
                log.info("Image compression successful: {:.1f}% reduction ({} KB -> {} KB)", 
                    compressionRatio, originalSize / 1024, optimizedSize / 1024);
                return optimizedData;
            } else {
                log.info("Optimization didn't reduce size, using original");
                return file.getBytes();
            }
            
        } catch (Exception e) {
            log.warn("Optimization failed, using original file: {}", e.getMessage());
            return file.getBytes();
        }
    }

    /**
     * Orijinal dosya adından optimize edilmiş dosya adı oluştur
     */
    private String generateOptimizedFileName(String originalFileName) {
        if (originalFileName == null) {
            return "optimized_image.jpg";
        }
        
        String nameWithoutExtension = originalFileName;
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            nameWithoutExtension = originalFileName.substring(0, lastDotIndex);
        }
        
        return nameWithoutExtension + "_opt.jpg";
    }

    /**
     * Fallback method - orijinal ImageService mantığı
     */
    private ImageDto saveOriginalImage(MultipartFile file, Product product) throws IOException, SQLException {
        Image image = new Image();
        image.setFileName(file.getOriginalFilename());
        image.setFileType(file.getContentType());
        image.setImage(new SerialBlob(file.getBytes()));
        image.setProduct(product);

        Image savedImage = imageRepository.save(image);
        String downloadUrl = "/images/image/" + savedImage.getId();
        savedImage.setDownloadUrl(downloadUrl);
        savedImage = imageRepository.save(savedImage);

        ImageDto imageDto = new ImageDto();
        imageDto.setId(savedImage.getId());
        imageDto.setFileName(savedImage.getFileName());
        imageDto.setDownloadUrl(savedImage.getDownloadUrl());

        return imageDto;
    }

    /**
     * Mevcut resmi yeniden optimize et (opsiyonel)
     */
    public void optimizeExistingImage(Long imageId) {
        try {
            Image image = imageRepository.findById(imageId).orElse(null);
            if (image == null) {
                log.warn("Image not found for optimization: {}", imageId);
                return;
            }

            // Mevcut resim verisini al
            byte[] currentData = image.getImage().getBytes(1, (int) image.getImage().length());
            
            // Mock MultipartFile oluştur
            MockMultipartFile mockFile = new MockMultipartFile(image.getFileName(), currentData);
            
            // Optimize et
            byte[] optimizedData = optimizationService.optimizeImage(mockFile, ImageOptimizationService.SIZE_DESKTOP);
            
            if (optimizedData.length < currentData.length) {
                // Optimize edilmişi kaydet
                image.setImage(new SerialBlob(optimizedData));
                image.setFileName(generateOptimizedFileName(image.getFileName()));
                imageRepository.save(image);
                
                log.info("Existing image optimized: {} ({} KB -> {} KB)", 
                    imageId, currentData.length / 1024, optimizedData.length / 1024);
            } else {
                log.info("Existing image {} already optimal", imageId);
            }
            
        } catch (Exception e) {
            log.error("Failed to optimize existing image {}: {}", imageId, e.getMessage());
        }
    }

    // Mock MultipartFile for internal optimization
    private static class MockMultipartFile implements MultipartFile {
        private final String fileName;
        private final byte[] content;

        public MockMultipartFile(String fileName, byte[] content) {
            this.fileName = fileName;
            this.content = content;
        }

        @Override public String getName() { return fileName; }
        @Override public String getOriginalFilename() { return fileName; }
        @Override public String getContentType() { return "image/jpeg"; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public java.io.InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File dest) throws IOException, IllegalStateException {}
    }
} 