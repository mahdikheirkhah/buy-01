package com.backend.media_service.controller;

import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.media_service.model.Media;
import com.backend.media_service.service.FileStorageService;
import com.backend.media_service.service.MediaService; // Import MediaService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private MediaService mediaService; // ✅ Inject the new MediaService

    @PostMapping("/upload")
    public ResponseEntity<MediaUploadResponseDTO> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("productId") String productId) {

        Media savedMedia = mediaService.uploadFile(file, productId);
        String fileUrl = "/api/media/files/" + savedMedia.getImagePath();

        MediaUploadResponseDTO response = new MediaUploadResponseDTO(savedMedia.getId(), fileUrl);
        return ResponseEntity.ok(response);
    }

    // This endpoint for serving files is still correct
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource file = fileStorageService.load(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    // Update the batch endpoint to use the service and return real data
    @GetMapping("/batch")
    public ResponseEntity<List<MediaUploadResponseDTO>> getMediaByIds(@RequestParam String productID) {
        List<MediaUploadResponseDTO> mediaList = mediaService.findMediaByProductID(productID);
        return ResponseEntity.ok(mediaList);
    }
    @KafkaListener(topics = "product-deleted-topic", groupId = "media-service-group")
    public void handleProductDeleted(String productId) {
        System.out.println("Received product deletion event for ID: " + productId);
        mediaService.DeleteMediaByProductID(productId);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMediaById(@RequestParam String ID) {
        mediaService.DeleteMediaByID(ID);
    }
}