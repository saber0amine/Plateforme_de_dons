package com.dev.plateforme_de_dons.controller;

import com.dev.plateforme_de_dons.model.User;
import com.dev.plateforme_de_dons.service.AnnonceService;
import com.dev.plateforme_de_dons.service.MessageService;
import com.dev.plateforme_de_dons.service.NotificationService;
import com.dev.plateforme_de_dons.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final AnnonceService annonceService;
    private final UserService userService;
    private final MessageService messageService;
    private final NotificationService notificationService;

    @GetMapping({"/", "/home"})
    public String home(Model model, Authentication authentication) {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "datePublication"));
        var recentAnnonces = annonceService.findAllActive(pageable);

        model.addAttribute("annonces", recentAnnonces.getContent().stream()
                .map(annonceService::convertToDto)
                .toList());

        if (authentication != null && authentication.isAuthenticated()) {
            userService.findByUsername(authentication.getName()).ifPresent(user -> {
                model.addAttribute("unreadMessages", messageService.getUnreadCount(user));
                model.addAttribute("unreadNotifications", notificationService.getUnreadCount(user));
            });
        }

        return "home";
    }
}
