package com.dev.plateforme_de_dons.service;

import com.dev.plateforme_de_dons.model.*;
import com.dev.plateforme_de_dons.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification createNotification(User user, String title, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        return notificationRepository.save(notification);
    }

    public Notification createNewAnnonceNotification(User user, Annonce annonce, SavedSearch savedSearch) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle("Nouvelle annonce correspondante");
        notification.setMessage("L'annonce \"" + annonce.getTitre() + "\" correspond à votre recherche \"" + savedSearch.getName() + "\"");
        notification.setType(NotificationType.NEW_ANNONCE_MATCH);
        notification.setAnnonce(annonce);
        notification.setSavedSearch(savedSearch);
        notification.setSender(annonce.getOwner());
        return notificationRepository.save(notification);
    }

    public Notification createMessageNotification(User user, User sender, Message message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setSender(sender);
        notification.setTitle("Nouveau message");
        notification.setMessage("Vous avez reçu un message de " + sender.getUsername());
        notification.setType(NotificationType.NEW_MESSAGE);
        if (message.getAnnonce() != null) {
            notification.setAnnonce(message.getAnnonce());
            notification.setMessage("Vous avez reçu un message de " + sender.getUsername() + " à propos de \"" + message.getAnnonce().getTitre() + "\"");
        }
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUnreadNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    public void markAsRead(Long notificationId, User user) {
        Optional<Notification> notifOpt = notificationRepository.findById(notificationId);
        if (notifOpt.isPresent()) {
            Notification notification = notifOpt.get();
            if (notification.getUser().getId().equals(user.getId())) {
                notification.markAsRead();
                notificationRepository.save(notification);
            }
        }
    }

    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadForUser(user);
    }
}