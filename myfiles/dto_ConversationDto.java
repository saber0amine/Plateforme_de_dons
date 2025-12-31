package com.dev.plateforme_de_dons.dto;

import com.dev.plateforme_de_dons.model.User;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationDto {
    private User partner;
    private MessageDto lastMessage;
    private long unreadCount;
    private Long annonceId;
    private String annonceTitre;
}