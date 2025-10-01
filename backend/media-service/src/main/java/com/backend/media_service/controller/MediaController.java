package com.backend.media_service.controller;

import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.media_service.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<MediaUploadResponseDTO> uploadFile(@RequestPart("file") MultipartFile file) {
        // Now we actually save the file and get its unique generated name
        String filename = fileStorageService.save(file);

        // This is the URL path the frontend will use to fetch the image later
        String fileUrl = "/api/media/files/" + filename;

        MediaUploadResponseDTO response = new MediaUploadResponseDTO(filename, fileUrl);
        return ResponseEntity.ok(response);
    }

    // This new endpoint serves the saved files
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource file = fileStorageService.load(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }
}