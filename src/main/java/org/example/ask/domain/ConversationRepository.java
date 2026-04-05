package org.example.ask.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Page<Conversation> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Optional<Conversation> findByIdAndUser(Long id, User user);
}

