package com.seyman.dreamshops.service.image;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class ImageOptimizationService {

    // Image sizes for responsive design
    public static final int SIZE_MOBILE = 400;
    public static final int SIZE_TABLET = 800;
    public static final int SIZE_DESKTOP = 1200;
    
    // Quality settings
    public static final float QUALITY_HIGH = 0.9f;
    public static final float QUALITY_MEDIUM = 0.7f;
    public static final float QUALITY_LOW = 0.5f;

    /**
     * Güvenli resim optimizasyonu - hata durumunda orijinal dosyayı döner
     */
    public byte[] optimizeImage(MultipartFile originalFile, int targetWidth) {
        try {
            if (!isImageFile(originalFile)) {
                return originalFile.getBytes();
            }

            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalFile.getBytes()));
            if (originalImage == null) {
                return originalFile.getBytes();
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            
            if (originalWidth <= targetWidth) {
                return originalFile.getBytes();
            }

            int targetHeight = (int) ((double) originalHeight * targetWidth / originalWidth);
            BufferedImage resizedImage = resizeImage(originalImage, targetWidth, targetHeight);
            byte[] optimizedBytes = compressToJPEG(resizedImage);
            
            log.info("Image optimized: {} -> {}x{} ({}KB -> {}KB)", 
                originalFile.getOriginalFilename(), 
                targetWidth, targetHeight, 
                originalFile.getSize() / 1024, 
                optimizedBytes.length / 1024);

            return optimizedBytes;

        } catch (Exception e) {
            log.error("Image optimization failed, returning original: {}", e.getMessage());
            try {
                return originalFile.getBytes();
            } catch (IOException ioException) {
                throw new RuntimeException("Complete image processing failure", ioException);
            }
        }
    }

    /**
     * Responsive image variants oluşturur (mobile, tablet, desktop)
     */
    public ResponsiveImageSet createResponsiveSet(MultipartFile originalFile) {
        try {
            // Original dosyayı sakla
            byte[] originalBytes = originalFile.getBytes();
            
            // Farklı boyutlarda optimize et
            byte[] mobileOptimized = optimizeImage(originalFile, SIZE_MOBILE);
            byte[] tabletOptimized = optimizeImage(originalFile, SIZE_TABLET);
            byte[] desktopOptimized = optimizeImage(originalFile, SIZE_DESKTOP);

            return ResponsiveImageSet.builder()
                .original(originalBytes)
                .mobile(mobileOptimized)
                .tablet(tabletOptimized)
                .desktop(desktopOptimized)
                .originalFileName(originalFile.getOriginalFilename())
                .build();

        } catch (Exception e) {
            log.error("Failed to create responsive image set for {}: {}", 
                originalFile.getOriginalFilename(), e.getMessage());
            
            // Fallback: Sadece orijinal dosyayı döner
            try {
                byte[] originalBytes = originalFile.getBytes();
                return ResponsiveImageSet.builder()
                    .original(originalBytes)
                    .mobile(originalBytes)
                    .tablet(originalBytes)
                    .desktop(originalBytes)
                    .originalFileName(originalFile.getOriginalFilename())
                    .build();
            } catch (IOException ioException) {
                throw new RuntimeException("Complete image processing failure", ioException);
            }
        }
    }

    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    private BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        
        return resized;
    }

    private byte[] compressToJPEG(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "JPEG", outputStream);
        return outputStream.toByteArray();
    }

    // Responsive image set için data class
    public static class ResponsiveImageSet {
        private final byte[] original;
        private final byte[] mobile;
        private final byte[] tablet;
        private final byte[] desktop;
        private final String originalFileName;

        private ResponsiveImageSet(Builder builder) {
            this.original = builder.original;
            this.mobile = builder.mobile;
            this.tablet = builder.tablet;
            this.desktop = builder.desktop;
            this.originalFileName = builder.originalFileName;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public byte[] getOriginal() { return original; }
        public byte[] getMobile() { return mobile; }
        public byte[] getTablet() { return tablet; }
        public byte[] getDesktop() { return desktop; }
        public String getOriginalFileName() { return originalFileName; }

        public static class Builder {
            private byte[] original;
            private byte[] mobile;
            private byte[] tablet;
            private byte[] desktop;
            private String originalFileName;

            public Builder original(byte[] original) { this.original = original; return this; }
            public Builder mobile(byte[] mobile) { this.mobile = mobile; return this; }
            public Builder tablet(byte[] tablet) { this.tablet = tablet; return this; }
            public Builder desktop(byte[] desktop) { this.desktop = desktop; return this; }
            public Builder originalFileName(String originalFileName) { this.originalFileName = originalFileName; return this; }

            public ResponsiveImageSet build() {
                return new ResponsiveImageSet(this);
            }
        }
    }
} 