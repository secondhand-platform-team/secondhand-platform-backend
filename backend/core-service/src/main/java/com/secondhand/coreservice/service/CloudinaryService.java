package com.secondhand.coreservice.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Value("${cloudinary.folder:secondhand-items}")
    private String folderName;

    /**
     * Upload image to Cloudinary
     */
    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret));

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folderName,
                            "resource_type", "auto",
                            "use_filename", true,
                            "unique_filename", true));

            String imageUrl = (String) uploadResult.get("secure_url");
            log.info("Image uploaded successfully: {}", imageUrl);
            return imageUrl;

        } catch (IOException e) {
            log.error("Error uploading image to Cloudinary", e);
            throw new IOException("Failed to upload image to cloud storage", e);
        }
    }

    /**
     * Delete image from Cloudinary by URL or public ID
     */
    public void deleteImage(String publicId) throws IOException {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret));

        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("invalidate", true));
            log.info("Image deleted successfully: {}", publicId);
        } catch (IOException e) {
            log.error("Error deleting image from Cloudinary", e);
            throw new IOException("Failed to delete image from cloud storage", e);
        }
    }

    /**
     * Upload multiple images
     */
    public java.util.List<String> uploadMultipleImages(MultipartFile[] files) throws IOException {
        java.util.List<String> uploadedUrls = new java.util.ArrayList<>();

        for (MultipartFile file : files) {
            String url = uploadImage(file);
            uploadedUrls.add(url);
        }

        return uploadedUrls;
    }
}
