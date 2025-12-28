package com.dev.plateforme_de_dons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    private Long id;

    @NotNull(message = "Le destinataire est obligatoire")
    private Long receiverId;

    private String receiverUsername;

    private Long senderId;
    private String senderUsername;

    private Long annonceId;
    private String annonceTitre;

    @NotBlank(message = "Le contenu du message est obligatoire")
    @Size(min = 1, max = 2000, message = "Le message doit contenir entre 1 et 2000 caractères")
    private String content;

    private LocalDateTime sentAt;
    private boolean read;
}
