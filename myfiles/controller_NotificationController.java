package com.dev.plateforme_de_dons.controller;

import com.dev.plateforme_de_dons.model.Notification;
import com.dev.plateforme_de_dons.model.User;
import com.dev.plateforme_de_dons.service.NotificationService;
import com.dev.plateforme_de_dons.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public String listNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationsPage = notificationService.getNotifications(user, pageable);

        model.addAttribute("notifications", notificationsPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notificationsPage.getTotalPages());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));

        return "notifications/list";
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listNotificationsJson(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationsPage = notificationService.getNotifications(user, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notificationsPage.getContent());
        response.put("currentPage", page);
        response.put("totalPages", notificationsPage.getTotalPages());
        response.put("unreadCount", notificationService.getUnreadCount(user));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/click")
    public String handleNotificationClick(
            @PathVariable Long id,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Notification notification = notificationService.getNotifications(user, Pageable.unpaged())
                .stream()
                .filter(n -> n.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification non trouvée"));

        notificationService.markAsRead(id, user);

        switch (notification.getType()) {
            case NEW_MESSAGE:
                if (notification.getSender() != null) {
                    if (notification.getAnnonce() != null) {
                        return "redirect:/messages/conversation/" + notification.getSender().getId() +
                                "?annonceId=" + notification.getAnnonce().getId();
                    } else {
                        return "redirect:/messages/conversation/" + notification.getSender().getId();
                    }
                }
                return "redirect:/messages";

            case NEW_ANNONCE_MATCH:
                if (notification.getAnnonce() != null) {
                    return "redirect:/annonces/" + notification.getAnnonce().getId();
                }
                return "redirect:/annonces";

            case ANNONCE_RESERVED:
            case ANNONCE_GIVEN:
                if (notification.getAnnonce() != null) {
                    return "redirect:/annonces/" + notification.getAnnonce().getId();
                }
                return "redirect:/mes-annonces";

            default:
                return "redirect:/notifications";
        }
    }

    @PostMapping("/{id}/read")
    @ResponseBody
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        notificationService.markAsRead(id, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mark-all-read")
    public String markAllAsRead(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        notificationService.markAllAsRead(user);
        return "redirect:/notifications";
    }

    @PostMapping(value = "/mark-all-read", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Void> markAllAsReadJson(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        notificationService.markAllAsRead(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Map<String, Long> response = new HashMap<>();
        response.put("count", notificationService.getUnreadCount(user));
        return ResponseEntity.ok(response);
    }
}