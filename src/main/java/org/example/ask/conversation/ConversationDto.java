package org.example.ask.conversation;

import org.example.ask.domain.Conversation;
import org.example.ask.domain.ConversationType;

import java.time.LocalDateTime;

public class ConversationDto {

    public record ConversationResponse(
            Long id,
            String transcript,
            ConversationType type,
            String result,
            LocalDateTime createdAt
    ) {
        public static ConversationResponse from(Conversation c) {
            return new ConversationResponse(
                    c.getId(),
                    c.getTranscript(),
                    c.getType(),
                    c.getResult(),
                    c.getCreatedAt()
            );
        }
    }

    public record PagedResponse(
            java.util.List<ConversationResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {}
}

