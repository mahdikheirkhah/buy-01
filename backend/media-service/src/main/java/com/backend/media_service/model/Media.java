package com.backend.media_service.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("media")
public class Media {
    @Id
    private String id;
    @NotBlank
    private String imagePath;
    @NotBlank
    private String productID;
}
