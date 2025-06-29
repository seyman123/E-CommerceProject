package com.seyman.dreamshops.service.image;

import com.seyman.dreamshops.dto.ImageDto;
import com.seyman.dreamshops.exceptions.ResourceNotFoundException;
import com.seyman.dreamshops.model.Image;
import com.seyman.dreamshops.model.Product;
import com.seyman.dreamshops.repository.ImageRepository;
import com.seyman.dreamshops.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageService implements IImageService {

    private final ImageRepository imageRepository;
    private final IProductService productService;

    @Override
    @Transactional(readOnly = true)
    public Image getImageById(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No image found with id: " + id));
    }

    @Override
    public void deleteImageById(Long id) {
        imageRepository.findById(id).ifPresentOrElse(imageRepository::delete, () -> {
            throw new ResourceNotFoundException("No image found with id: " + id);
        });
    }

    @Override
    public List<ImageDto> saveImages(List<MultipartFile> files, Long productId) {
        Product product = productService.getProductById(productId);

        List<ImageDto> savedImageDto = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                Image image = new Image();
                image.setFileName(file.getOriginalFilename());
                image.setFileType(file.getContentType());
                image.setImage(new SerialBlob(file.getBytes()));
                image.setProduct(product);

                // First save without URL to get the ID
                Image savedImage = imageRepository.save(image);

                // Now set the correct URL with the actual ID (relative path without /api/v1 prefix)
                String buildDownloadUrl = "/images/image/";
                String downloadUrl = buildDownloadUrl + savedImage.getId();
                savedImage.setDownloadUrl(downloadUrl);
                
                // Save again with the correct URL
                savedImage = imageRepository.save(savedImage);

                ImageDto imageDto = new ImageDto();
                imageDto.setId(savedImage.getId());
                imageDto.setFileName(savedImage.getFileName());
                imageDto.setDownloadUrl(savedImage.getDownloadUrl());
                savedImageDto.add(imageDto);
            } catch (IOException | SQLException e) {
                throw new RuntimeException(e.getMessage());
            }

        }

        return savedImageDto;
    }

    @Override
    public void updateImage(MultipartFile file, Long imageId) {
        Image image = getImageById(imageId);

        try {
            image.setFileName(file.getOriginalFilename());
            image.setFileName(file.getOriginalFilename());
            image.setImage(new SerialBlob(file.getBytes()));
            imageRepository.save(image);
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public int migrateImageUrls() {
        List<Image> allImages = imageRepository.findAll();
        int updatedCount = 0;
        
        for (Image image : allImages) {
            String currentUrl = image.getDownloadUrl();
            if (currentUrl != null && (currentUrl.contains("/download/") || currentUrl.startsWith("/api/v1/"))) {
                // Update old format URLs to new format (relative path without /api/v1 prefix)
                String newUrl = "/images/image/" + image.getId();
                image.setDownloadUrl(newUrl);
                imageRepository.save(image);
                updatedCount++;
            }
        }
        
        return updatedCount;
    }
}
