package com.dev.plateforme_de_dons.service;

import com.dev.plateforme_de_dons.dto.ImageDto;
import com.dev.plateforme_de_dons.model.Annonce;
import com.dev.plateforme_de_dons.model.Image;
import com.dev.plateforme_de_dons.model.Lot;
import com.dev.plateforme_de_dons.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ImageService {

    private final ImageRepository imageRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    public Image uploadImageForAnnonce(MultipartFile file, Annonce annonce, boolean isPrimary) throws IOException {
        validateImage(file);

        String filename = generateUniqueFilename(file.getOriginalFilename());
        String storagePath = "annonces/" + annonce.getId();
        Path uploadPath = Paths.get(uploadDir, storagePath);

        // Créer le dossier s'il n'existe pas
        Files.createDirectories(uploadPath);

        // Sauvegarder le fichier
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Si c'est l'image principale, retirer le flag des autres
        if (isPrimary) {
            annonce.getImages().forEach(img -> img.setPrimary(false));
        }

        Image image = new Image(
                filename,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                storagePath + "/" + filename
        );
        image.setAnnonce(annonce);
        image.setPrimary(isPrimary);

        return imageRepository.save(image);
    }

    public Image uploadImageForLot(MultipartFile file, Lot lot, boolean isPrimary) throws IOException {
        validateImage(file);

        String filename = generateUniqueFilename(file.getOriginalFilename());
        String storagePath = "lots/" + lot.getId();
        Path uploadPath = Paths.get(uploadDir, storagePath);

        Files.createDirectories(uploadPath);

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        if (isPrimary) {
            lot.getImages().forEach(img -> img.setPrimary(false));
        }

        Image image = new Image(
                filename,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                storagePath + "/" + filename
        );
        image.setLot(lot);
        image.setPrimary(isPrimary);

        return imageRepository.save(image);
    }

    @Transactional(readOnly = true)
    public Optional<Image> findById(Long id) {
        return imageRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Image> findByAnnonce(Annonce annonce) {
        return imageRepository.findByAnnonce(annonce);
    }

    @Transactional(readOnly = true)
    public List<Image> findByLot(Lot lot) {
        return imageRepository.findByLot(lot);
    }

    @Transactional(readOnly = true)
    public byte[] getImageBytes(Image image) throws IOException {
        Path imagePath = Paths.get(uploadDir, image.getStoragePath());
        return Files.readAllBytes(imagePath);
    }

    public void deleteImage(Long imageId) throws IOException {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image non trouvée"));

        // Supprimer le fichier physique
        Path imagePath = Paths.get(uploadDir, image.getStoragePath());
        Files.deleteIfExists(imagePath);

        // Supprimer l'enregistrement en base
        imageRepository.delete(image);
    }

    public void setPrimaryImage(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image non trouvée"));

        if (image.getAnnonce() != null) {
            imageRepository.findByAnnonce(image.getAnnonce())
                    .forEach(img -> img.setPrimary(false));
        } else if (image.getLot() != null) {
            imageRepository.findByLot(image.getLot())
                    .forEach(img -> img.setPrimary(false));
        }

        image.setPrimary(true);
        imageRepository.save(image);
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Formats acceptés : JPEG, PNG, GIF, WebP");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Le fichier est trop volumineux. Taille maximale : 5 MB");
        }
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    public ImageDto convertToDto(Image image) {
        ImageDto dto = new ImageDto();
        dto.setId(image.getId());
        dto.setFilename(image.getFilename());
        dto.setOriginalFilename(image.getOriginalFilename());
        dto.setContentType(image.getContentType());
        dto.setSize(image.getSize());
        dto.setUploadedAt(image.getUploadedAt());
        dto.setPrimary(image.isPrimary());
        dto.setUrl("/api/images/" + image.getId());
        return dto;
    }

    public List<ImageDto> convertToDtoList(List<Image> images) {
        return images.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}