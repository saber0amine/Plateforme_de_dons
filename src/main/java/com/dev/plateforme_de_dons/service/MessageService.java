package com.dev.plateforme_de_dons.service;

import com.dev.plateforme_de_dons.dto.ConversationDto;
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

import java.util.*;
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
    public List<ConversationDto> getConversationsWithLastMessage(User user) {
        List<Message> allMessages = messageRepository.findAllByUser(user, Pageable.unpaged()).getContent();

        Map<ConversationKey, List<Message>> groupedConversations = new HashMap<>();

        for (Message message : allMessages) {
            User partner = message.getSender().getId().equals(user.getId())
                    ? message.getReceiver()
                    : message.getSender();

            Long annonceId = message.getAnnonce() != null ? message.getAnnonce().getId() : null;
            ConversationKey key = new ConversationKey(partner.getId(), annonceId);

            groupedConversations.computeIfAbsent(key, k -> new ArrayList<>()).add(message);
        }

        List<ConversationDto> conversations = new ArrayList<>();

        for (Map.Entry<ConversationKey, List<Message>> entry : groupedConversations.entrySet()) {
            ConversationKey key = entry.getKey();
            List<Message> messages = entry.getValue();
            messages.sort(Comparator.comparing(Message::getSentAt));

            Message lastMessage = messages.get(messages.size() - 1);
            User partner = lastMessage.getSender().getId().equals(user.getId())
                    ? lastMessage.getReceiver()
                    : lastMessage.getSender();

            long unreadCount = messages.stream()
                    .filter(m -> m.getReceiver().getId().equals(user.getId()))
                    .filter(m -> !m.isRead())
                    .count();

            ConversationDto conv = new ConversationDto();
            conv.setPartner(partner);
            conv.setLastMessage(convertToDto(lastMessage));
            conv.setUnreadCount(unreadCount);
            conv.setAnnonceId(key.annonceId);
            if (key.annonceId != null && lastMessage.getAnnonce() != null) {
                conv.setAnnonceTitre(lastMessage.getAnnonce().getTitre());
            }

            conversations.add(conv);
        }

        conversations.sort((c1, c2) ->
                c2.getLastMessage().getSentAt().compareTo(c1.getLastMessage().getSentAt())
        );

        return conversations;
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
        Set<User> partners = new HashSet<>();
        partners.addAll(messageRepository.findReceiversForSender(user));
        partners.addAll(messageRepository.findSendersForReceiver(user));
        return new ArrayList<>(partners);
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

    private static class ConversationKey {
        private final Long partnerId;
        private final Long annonceId;

        public ConversationKey(Long partnerId, Long annonceId) {
            this.partnerId = partnerId;
            this.annonceId = annonceId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConversationKey that = (ConversationKey) o;
            return Objects.equals(partnerId, that.partnerId) && Objects.equals(annonceId, that.annonceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(partnerId, annonceId);
        }
    }
}