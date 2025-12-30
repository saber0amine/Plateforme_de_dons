package com.dev.plateforme_de_dons.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageDto {

    private Long id;
    private String filename;
    private String originalFilename;
    private String contentType;
    private Long size;
    private LocalDateTime uploadedAt;
    private boolean isPrimary;
    private String url;

    public ImageDto(Long id, String url, boolean isPrimary) {
        this.id = id;
        this.url = url;
        this.isPrimary = isPrimary;
    }
}