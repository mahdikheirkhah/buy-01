package com.backend.media_service.controller;

import com.backend.common.dto.MediaUploadResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    // You would inject a service here to handle the actual file saving logic
    // @Autowired
    // private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<MediaUploadResponseDTO> uploadFile(@RequestPart("file") MultipartFile file) {
        // 1. VALIDATION LOGIC
        // Check file size (e.g., file.getSize() > 2 * 1024 * 1024)
        // Check file type (e.g., file.getContentType() is "image/jpeg" or "image/png")
        // If validation fails, throw a custom exception.

        // 2. SAVING LOGIC (inside a service class)
        // Save the file to a directory, a cloud storage service (like AWS S3), or a database.
        // String fileId = fileStorageService.save(file);

        // For this example, let's pretend we saved it and got an ID and URL.
        String fileId = "some-unique-id-" + file.getOriginalFilename();
        String fileUrl = "/media/" + fileId; // The URL to access the file later

        // 3. RETURN RESPONSE
        MediaUploadResponseDTO response = new MediaUploadResponseDTO(fileId, fileUrl);
        return ResponseEntity.ok(response);
    }
}