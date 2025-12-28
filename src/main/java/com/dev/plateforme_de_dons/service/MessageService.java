package com.dev.plateforme_de_dons.service;

import com.dev.plateforme_de_dons.dto.MessageDto;
import com.dev.plateforme_de_dons.model.Annonce;
import com.dev.plateforme_de_dons.model.Message;
import com.dev.plateforme_de_dons.model.User;
import com.dev.plateforme_de_dons.repository.AnnonceRepository;
import com.dev.plateforme_de_dons.repository.MessageRepository;
import com.dev.plateforme_de_dons.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AnnonceRepository annonceRepository;
    private final NotificationService notificationService;

    public Message sendMessage(MessageDto dto, User sender) {
        User receiver = userRepository.findById(dto.getReceiverId())
                .orElseThrow(() -> new IllegalArgumentException("Destinataire non trouvé"));

        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Vous ne pouvez pas vous envoyer un message à vous-même");
        }

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(dto.getContent());

        if (dto.getAnnonceId() != null) {
            Annonce annonce = annonceRepository.findById(dto.getAnnonceId())
                    .orElseThrow(() -> new IllegalArgumentException("Annonce non trouvée"));
            message.setAnnonce(annonce);
        }

        message = messageRepository.save(message);

        notificationService.createMessageNotification(receiver, sender, message);

        return message;
    }

    @Transactional(readOnly = true)
    public Page<Message> getReceivedMessages(User user, Pageable pageable) {
        return messageRepository.findByReceiver(user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Message> getSentMessages(User user, Pageable pageable) {
        return messageRepository.findBySender(user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Message> getAllMessages(User user, Pageable pageable) {
        return messageRepository.findAllByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public List<Message> getConversation(User user1, User user2, Annonce annonce) {
        return messageRepository.findConversation(user1, user2, annonce);
    }

    @Transactional(readOnly = true)
    public List<User> getConversationPartners(User user) {
        return messageRepository.findConversationPartners(user);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return messageRepository.countByReceiverAndReadFalse(user);
    }

    public void markAsRead(Long messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message non trouvé"));

        if (!message.getReceiver().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à marquer ce message comme lu");
        }

        message.markAsRead();
        messageRepository.save(message);
    }

    public void markConversationAsRead(User currentUser, User otherUser, Annonce annonce) {
        List<Message> messages = messageRepository.findConversation(currentUser, otherUser, annonce);
        messages.stream()
                .filter(m -> m.getReceiver().getId().equals(currentUser.getId()))
                .filter(m -> !m.isRead())
                .forEach(m -> {
                    m.markAsRead();
                    messageRepository.save(m);
                });
    }

    @Transactional(readOnly = true)
    public Optional<Message> findById(Long id) {
        return messageRepository.findById(id);
    }

    public MessageDto convertToDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setReceiverId(message.getReceiver().getId());
        dto.setReceiverUsername(message.getReceiver().getUsername());
        dto.setContent(message.getContent());
        dto.setSentAt(message.getSentAt());
        dto.setRead(message.isRead());
        if (message.getAnnonce() != null) {
            dto.setAnnonceId(message.getAnnonce().getId());
            dto.setAnnonceTitre(message.getAnnonce().getTitre());
        }
        return dto;
    }

    public List<MessageDto> convertToDtoList(List<Message> messages) {
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}
