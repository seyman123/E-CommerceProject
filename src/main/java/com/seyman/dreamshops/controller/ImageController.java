package com.seyman.dreamshops.controller;


import com.seyman.dreamshops.dto.ImageDto;
import com.seyman.dreamshops.exceptions.ResourceNotFoundException;
import com.seyman.dreamshops.model.Image;
import com.seyman.dreamshops.response.ApiResponse;
import com.seyman.dreamshops.service.image.IImageService;
import com.seyman.dreamshops.service.image.EnhancedImageService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/images")
public class ImageController {
    private final IImageService imageService;
    private final EnhancedImageService enhancedImageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse> saveImages(@RequestParam List<MultipartFile> files, @RequestParam Long productId) {
        try {
            List<ImageDto> imageDtos = enhancedImageService.saveOptimizedImages(files, productId);
            return ResponseEntity.ok(new ApiResponse("Optimized upload success! 🚀", imageDtos));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Upload failed!", e.getMessage()));
        }
    }

    @PostMapping("/upload-optimized")
    public ResponseEntity<ApiResponse> saveOptimizedImages(@RequestParam List<MultipartFile> files, @RequestParam Long productId) {
        try {
            List<ImageDto> imageDtos = enhancedImageService.saveOptimizedImages(files, productId);
            return ResponseEntity.ok(new ApiResponse("Optimized upload success!", imageDtos));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Optimized upload failed!", e.getMessage()));
        }
    }

    @GetMapping("/image/{imageId}")
    @Transactional(readOnly = true, timeout = 30)
    public ResponseEntity<byte[]> getImage(@PathVariable Long imageId) {
        try {
            Image image = imageService.getImageById(imageId);
            byte[] imageData = image.getImage().getBytes(1, (int) image.getImage().length());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf(image.getFileType()))
                    .contentLength(imageData.length)
                    .body(imageData);
                    
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/image/download/{imageId}")
    public ResponseEntity<byte[]> getImageLegacy(@PathVariable Long imageId) {
        try {
            Image image = imageService.getImageById(imageId);
            byte[] imageData = image.getImage().getBytes(1, (int) image.getImage().length());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(image.getFileType()));
            headers.setContentLength(imageData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageData);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).build();
        } catch (SQLException e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/image/{imageId}/update")
    public ResponseEntity<ApiResponse> updateImage(@PathVariable Long imageId, @RequestBody MultipartFile file) {
        try {
            Image image = imageService.getImageById(imageId);
            if (image != null) {
                imageService.updateImage(file, imageId);
                return ResponseEntity.ok(new ApiResponse("Update success!", null));
            }
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }

        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Update failed!", INTERNAL_SERVER_ERROR));
    }

    @DeleteMapping("/image/{imageId}/delete")
    public ResponseEntity<ApiResponse> deleteImage(@PathVariable Long imageId) {
        try {
            Image image = imageService.getImageById(imageId);
            if (image != null) {
                imageService.deleteImageById(imageId);
                return ResponseEntity.ok(new ApiResponse("Delete success!", null));
            }
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }

        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Delete failed!", INTERNAL_SERVER_ERROR));
    }

    // Temporary public endpoint for URL migration - remove after migration is complete
    @PostMapping("/migrate-urls")
    public ResponseEntity<ApiResponse> migrateImageUrls() {
        try {
            int updatedCount = imageService.migrateImageUrls();
            return ResponseEntity.ok(new ApiResponse("URL migration completed!", "Updated " + updatedCount + " image URLs"));
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Migration failed!", e.getMessage()));
        }
    }

    // Test endpoint to serve a simple image
    @GetMapping("/test-image")
    public ResponseEntity<byte[]> getTestImage() {
        try {
            // Create a simple 1x1 pixel PNG image
            byte[] imageData = new byte[]{
                (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x02, 0x00, 0x00, 0x00, (byte)0x90, (byte)0x77, 0x53, (byte)0xDE,
                0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54,
                0x08, (byte)0xD7, 0x63, (byte)0xF8, (byte)0x0F, 0x00, 0x00, 0x01,
                0x00, 0x01, (byte)0x9A, 0x6C, (byte)0xCE, (byte)0x8E, 0x00, 0x00, 0x00, 0x00,
                0x49, 0x45, 0x4E, 0x44, (byte)0xAE, 0x42, 0x60, (byte)0x82
            };
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(imageData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageData);
        } catch (Exception e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).build();
        }
    }

}
