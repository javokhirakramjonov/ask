package org.example.ask.conversation;

import lombok.RequiredArgsConstructor;
import org.example.ask.domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ConversationWebController {

    private final ConversationService conversationService;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Value("${app.audio.max-size-bytes}")
    private long maxSizeBytes;

    @Value("${app.audio.max-duration-seconds}")
    private int maxDurationSeconds;

    @GetMapping({"/", "/home"})
    public String home(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @AuthenticationPrincipal UserDetails userDetails,
                       Model model) {
        User user = resolveUser(userDetails);
        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> pageResult = conversationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        model.addAttribute("conversations", pageResult);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("audioMaxSizeMb", maxSizeBytes / (1024 * 1024));
        model.addAttribute("audioMaxDurationSeconds", maxDurationSeconds);
        return "home";
    }

    @PostMapping("/conversations/upload")
    public String upload(@RequestParam("audio") MultipartFile audio,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            User user = resolveUser(userDetails);
            Conversation conversation = conversationService.process(audio, user);
            return "redirect:/conversations/" + conversation.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to process audio: " + e.getMessage());
            return "redirect:/home";
        }
    }

    /** Used by the in-page voice recorder — returns JSON so JS can redirect. */
    @PostMapping("/conversations/upload-json")
    @ResponseBody
    public ResponseEntity<?> uploadJson(@RequestParam("audio") MultipartFile audio,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = resolveUser(userDetails);
            Conversation conversation = conversationService.process(audio, user);
            return ResponseEntity.ok(Map.of("id", conversation.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/conversations/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model) {
        User user = resolveUser(userDetails);
        return conversationRepository.findByIdAndUser(id, user)
                .map(c -> {
                    model.addAttribute("conversation", c);
                    model.addAttribute("email", user.getEmail());
                    return "conversation/detail";
                })
                .orElse("redirect:/home");
    }

    private User resolveUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}

