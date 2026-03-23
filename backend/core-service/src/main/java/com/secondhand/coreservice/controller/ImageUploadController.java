package com.secondhand.coreservice.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.secondhand.coreservice.service.CloudinaryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class ImageUploadController {

    private final CloudinaryService cloudinaryService;

 
    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ImageUploadResponse(false, "File is empty", null));
            }

            String imageUrl = cloudinaryService.uploadImage(file);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ImageUploadResponse(true, "Image uploaded successfully", imageUrl));

        } catch (IOException e) {
            log.error("Error uploading image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ImageUploadResponse(false, "Failed to upload image: " + e.getMessage(), null));
        }
    }

   
    @PostMapping("/upload-multiple")
    public ResponseEntity<MultipleImageUploadResponse> uploadMultipleImages(
            @RequestParam("files") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest()
                        .body(new MultipleImageUploadResponse(false, "No files provided", null));
            }

            List<String> imageUrls = cloudinaryService.uploadMultipleImages(files);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new MultipleImageUploadResponse(true, "Images uploaded successfully", imageUrls));

        } catch (IOException e) {
            log.error("Error uploading images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MultipleImageUploadResponse(false, "Failed to upload images: " + e.getMessage(), null));
        }
    }

    public static class ImageUploadResponse {
        public boolean success;
        public String message;
        public String imageUrl;

        public ImageUploadResponse(boolean success, String message, String imageUrl) {
            this.success = success;
            this.message = message;
            this.imageUrl = imageUrl;
        }
    }

    public static class MultipleImageUploadResponse {
        public boolean success;
        public String message;
        public List<String> imageUrls;

        public MultipleImageUploadResponse(boolean success, String message, List<String> imageUrls) {
            this.success = success;
            this.message = message;
            this.imageUrls = imageUrls;
        }
    }
}
