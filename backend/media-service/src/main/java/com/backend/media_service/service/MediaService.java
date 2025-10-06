package com.backend.media_service.service;

import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.common.exception.CustomException;
import com.backend.media_service.model.Media;
import com.backend.media_service.repository.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MediaService {
    private final MediaRepository mediaRepository;
    private final FileStorageService fileStorageService;
    @Autowired
    public MediaService(MediaRepository mediaRepository, FileStorageService fileStorageService) {
        this.mediaRepository = mediaRepository;
        this.fileStorageService = fileStorageService;
    }
    public Media uploadFile(MultipartFile file, String productId) {
        String filename = fileStorageService.save(file);
        Media media = new Media();
        media.setImagePath(filename);
        media.setProductID(productId);
        return mediaRepository.save(media);
    }
    public String uploadFileAvatar(MultipartFile file) {
        return fileStorageService.save(file);
    }

    public List<MediaUploadResponseDTO> findMediaByProductID(String productID) {
        List<Media> mediaList = mediaRepository.findByProductID(productID);
        List<MediaUploadResponseDTO> mediaUploadResponseDTOList = new ArrayList<>();
        if(mediaList.isEmpty()){
            return Collections.emptyList();
        }
        for (Media media : mediaList) {
            mediaUploadResponseDTOList.add(MediaUploadResponseDTO
                    .builder()
                    .fileId(media.getId())
                    .fileUrl(media.getImagePath())
                    .build());
        }
        return mediaUploadResponseDTOList;
    }
    public void DeleteMediaByProductID(String productID) {
        List<Media> mediaToDelete = mediaRepository.findByProductID(productID);
        for (Media media : mediaToDelete) {
            fileStorageService.delete(media.getImagePath());
        }
        mediaRepository.deleteAll(mediaToDelete);
    }
    public void DeleteMediaByID(String ID) {
        mediaRepository.deleteById(ID);
    }
    public Media updateMedia(String mediaId, MultipartFile newFile) {
        Media existingMedia = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found with id: " + mediaId));
        String oldImagePath = existingMedia.getImagePath();
        String newImagePath = fileStorageService.save(newFile);
        existingMedia.setImagePath(newImagePath);
        Media updatedMedia = mediaRepository.save(existingMedia);
        fileStorageService.delete(oldImagePath);
        return updatedMedia;
    }
}