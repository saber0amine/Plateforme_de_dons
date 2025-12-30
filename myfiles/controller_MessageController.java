package com.dev.plateforme_de_dons.controller;

import com.dev.plateforme_de_dons.dto.MessageDto;
import com.dev.plateforme_de_dons.model.Annonce;
import com.dev.plateforme_de_dons.model.Message;
import com.dev.plateforme_de_dons.model.User;
import com.dev.plateforme_de_dons.service.AnnonceService;
import com.dev.plateforme_de_dons.service.MessageService;
import com.dev.plateforme_de_dons.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;
    private final AnnonceService annonceService;

    @GetMapping
    public String listMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        Page<Message> messagesPage = messageService.getAllMessages(user, pageable);

        List<User> conversationPartners = messageService.getConversationPartners(user);

        model.addAttribute("messages", messagesPage.map(messageService::convertToDto));
        model.addAttribute("conversations", conversationPartners);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", messagesPage.getTotalPages());
        model.addAttribute("unreadCount", messageService.getUnreadCount(user));

        return "messages/list";
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listMessagesJson(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        Page<Message> messagesPage = messageService.getAllMessages(user, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("messages", messagesPage.map(messageService::convertToDto).getContent());
        response.put("currentPage", page);
        response.put("totalPages", messagesPage.getTotalPages());
        response.put("unreadCount", messageService.getUnreadCount(user));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversation/{userId}")
    public String viewConversation(
            @PathVariable Long userId,
            @RequestParam(required = false) Long annonceId,
            Model model,
            Authentication authentication) {

        User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        User otherUser = userService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));

        Annonce annonce = null;
        if (annonceId != null) {
            annonce = annonceService.findById(annonceId).orElse(null);
        }

        List<Message> conversation = messageService.getConversation(currentUser, otherUser, annonce);
        messageService.markConversationAsRead(currentUser, otherUser, annonce);

        model.addAttribute("messages", messageService.convertToDtoList(conversation));
        model.addAttribute("otherUser", otherUser);
        model.addAttribute("annonce", annonce != null ? annonceService.convertToDto(annonce) : null);
        model.addAttribute("newMessage", new MessageDto());

        return "messages/conversation";
    }

    @GetMapping(value = "/conversation/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> viewConversationJson(
            @PathVariable Long userId,
            @RequestParam(required = false) Long annonceId,
            Authentication authentication) {

        User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        User otherUser = userService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Annonce annonce = null;
        if (annonceId != null) {
            annonce = annonceService.findById(annonceId).orElse(null);
        }

        List<Message> conversation = messageService.getConversation(currentUser, otherUser, annonce);
        messageService.markConversationAsRead(currentUser, otherUser, annonce);

        Map<String, Object> response = new HashMap<>();
        response.put("messages", messageService.convertToDtoList(conversation));
        response.put("otherUserId", otherUser.getId());
        response.put("otherUsername", otherUser.getUsername());
        if (annonce != null) {
            response.put("annonce", annonceService.convertToDto(annonce));
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/new")
    public String showNewMessageForm(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long annonceId,
            Model model,
            Authentication authentication) {

        MessageDto messageDto = new MessageDto();

        if (userId != null) {
            messageDto.setReceiverId(userId);
            userService.findById(userId).ifPresent(u -> messageDto.setReceiverUsername(u.getUsername()));
        }

        if (annonceId != null) {
            messageDto.setAnnonceId(annonceId);
            annonceService.findById(annonceId).ifPresent(a -> {
                messageDto.setAnnonceTitre(a.getTitre());
                if (userId == null) {
                    messageDto.setReceiverId(a.getOwner().getId());
                    messageDto.setReceiverUsername(a.getOwner().getUsername());
                }
            });
        }

        model.addAttribute("message", messageDto);
        return "messages/new";
    }

    @PostMapping
    public String sendMessage(
            @Valid @ModelAttribute("message") MessageDto messageDto,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            return "messages/new";
        }

        User sender = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        try {
            messageService.sendMessage(messageDto, sender);
            redirectAttributes.addFlashAttribute("success", "Message envoyé avec succès !");
            return "redirect:/messages/conversation/" + messageDto.getReceiverId() +
                    (messageDto.getAnnonceId() != null ? "?annonceId=" + messageDto.getAnnonceId() : "");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "messages/new";
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<MessageDto> sendMessageJson(
            @Valid @RequestBody MessageDto messageDto,
            Authentication authentication) {

        User sender = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Message message = messageService.sendMessage(messageDto, sender);
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.convertToDto(message));
    }

    @PostMapping("/{id}/read")
    @ResponseBody
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        messageService.markAsRead(id, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Map<String, Long> response = new HashMap<>();
        response.put("count", messageService.getUnreadCount(user));
        return ResponseEntity.ok(response);
    }
}
