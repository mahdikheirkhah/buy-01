package com.backend.media_service.service;

import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.media_service.model.Media;
import com.backend.media_service.repository.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
}