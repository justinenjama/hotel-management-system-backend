package com.justine.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud_name}") String cloudName,
            @Value("${cloudinary.api_key}") String apiKey,
            @Value("${cloudinary.api_secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    /**
     * Upload MultipartFile to Cloudinary and automatically generate
     * multiple sizes using eager transformations.
     *
     * @param multipartFile The image to upload
     * @param folder Target Cloudinary folder
     * @return Map<String, String> with keys "large", "medium", "thumbnail"
     */
    public Map<String, String> uploadFileWithEagerSizes(MultipartFile multipartFile, String folder) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        try {
            // Correct eager transformations
            List<Transformation> eagerTransforms = List.of(
                    new Transformation().width(1200).height(1200).crop("limit").quality("auto:good").fetchFormat("auto"),
                    new Transformation().width(600).height(600).crop("limit").quality("auto:good").fetchFormat("auto"),
                    new Transformation().width(300).height(300).crop("limit").quality("auto:good").fetchFormat("auto")
            );

            Map uploadResult = cloudinary.uploader().upload(
                    multipartFile.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto",
                            "eager", eagerTransforms,
                            "eager_async", false
                    )
            );

            Map<String, String> urls = new HashMap<>();
            urls.put("large", ((Map)((List)uploadResult.get("eager")).get(0)).get("secure_url").toString());
            urls.put("medium", ((Map)((List)uploadResult.get("eager")).get(1)).get("secure_url").toString());
            urls.put("thumbnail", ((Map)((List)uploadResult.get("eager")).get(2)).get("secure_url").toString());

            return urls;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read MultipartFile for Cloudinary upload: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String publicId) {
        try {
            if (publicId == null || publicId.isBlank()) return;
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from Cloudinary: " + e.getMessage(), e);
        }
    }
    public String extractPublicIdFromUrl(String url) {
        try {
            if (url == null || !url.contains("/upload/")) return null;
            String afterUpload = url.substring(url.indexOf("/upload/") + 8);
            String[] parts = afterUpload.split("/");
            if (parts.length > 1 && parts[0].matches("^v\\d+$")) {
                afterUpload = afterUpload.substring(parts[0].length() + 1);
            }
            return afterUpload.replaceFirst("\\.[^.]+$", "");
        } catch (Exception e) {
            return null;
        }
    }
}
