package com.secondhand.authservice.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Value("${cloudinary.folder:secondhand-avatars}")
    private String folderName;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        long timestamp = System.currentTimeMillis() / 1000L;

        // Build signature: sign "folder=...&timestamp=..." with api_secret
        String signingString = "folder=" + folderName + "&timestamp=" + timestamp + apiSecret;
        String signature = sha1Hex(signingString);

        // Build multipart body
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename() != null ? file.getOriginalFilename() : "avatar";
            }
        };
        body.add("file", fileResource);
        body.add("api_key", apiKey);
        body.add("timestamp", String.valueOf(timestamp));
        body.add("folder", folderName);
        body.add("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            String imageUrl = json.get("secure_url").asText();
            log.info("Avatar uploaded successfully: {}", imageUrl);
            return imageUrl;
        } catch (Exception e) {
            log.error("Error uploading avatar to Cloudinary: {}", e.getMessage(), e);
            throw new IOException("Failed to upload avatar to cloud storage: " + e.getMessage(), e);
        }
    }

    private String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute SHA-1 signature", e);
        }
    }
}

