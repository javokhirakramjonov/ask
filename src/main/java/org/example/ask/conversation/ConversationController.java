package org.example.ask.conversation;

import lombok.RequiredArgsConstructor;
import org.example.ask.domain.*;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ConversationDto.ConversationResponse> upload(
            @RequestParam("audio") MultipartFile audio,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {

        User user = resolveUser(userDetails);
        Conversation conversation = conversationService.process(audio, user);
        return ResponseEntity.ok(ConversationDto.ConversationResponse.from(conversation));
    }

    @GetMapping
    public ResponseEntity<ConversationDto.PagedResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> pageResult = conversationRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        var content = pageResult.getContent().stream()
                .map(ConversationDto.ConversationResponse::from)
                .toList();

        return ResponseEntity.ok(new ConversationDto.PagedResponse(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.hasNext(),
                pageResult.hasPrevious()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDto.ConversationResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        return conversationRepository.findByIdAndUser(id, user)
                .map(ConversationDto.ConversationResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(403).build());
    }

    private User resolveUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}

