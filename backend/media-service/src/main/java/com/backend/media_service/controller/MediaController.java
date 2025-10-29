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

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
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
            @RequestPart("productId") String productId) { // ✅ Make sure it accepts productId

        Media savedMedia = mediaService.uploadFile(file, productId); // Service saves file & metadata
        String fileUrl = "/api/media/files/" + savedMedia.getImagePath();

        MediaUploadResponseDTO response = new MediaUploadResponseDTO(savedMedia.getId(), fileUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload/avatar")
    public ResponseEntity<String> uploadFileForAvatar(
            @RequestParam("file") MultipartFile file
    ){
        String fileName = mediaService.uploadFileAvatar(file);
        String fileUrl = "/api/media/files/" + fileName;
        System.out.println(fileUrl);
        return ResponseEntity.ok(fileUrl);
    }

    @GetMapping("/files/{filename}") // Make sure this path matches your DB
    public ResponseEntity<Resource> getFile(@PathVariable("filename") String filename) {
        Resource file = fileStorageService.load(filename);

        // Try to determine the file's content type
        String contentType = "application/octet-stream"; // Default
        try {
            // Get the path from the resource and probe its content type
            contentType = Files.probeContentType(file.getFile().toPath());
        } catch (IOException e) {
            // Log this error, but we can proceed with the default
            System.err.println("Could not determine file type for: " + filename);
        }

        return ResponseEntity.ok()
                // ❌ REMOVE THIS LINE
                // .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")

                // ✅ ADD THIS LINE
                .header(HttpHeaders.CONTENT_TYPE, contentType)
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
        return ResponseEntity.ok("Delete media successfully");
    }
    @PutMapping("/{mediaId}")
    public ResponseEntity<Media> updateMedia(
            @PathVariable String mediaId,
            @RequestPart("file") MultipartFile file) {
        Media updatedMedia = mediaService.updateMedia(mediaId, file);
        return ResponseEntity.ok(updatedMedia);
    }

}