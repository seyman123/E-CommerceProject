package com.seyman.dreamshops.service.image;

import com.seyman.dreamshops.dto.ImageDto;
import com.seyman.dreamshops.model.Image;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IImageService {
    Image getImageById(Long id);
    void deleteImageById(Long id);
    List<ImageDto> saveImages(List<MultipartFile> files, Long productId);
    void updateImage(MultipartFile file, Long imageId);
    int migrateImageUrls();
}
