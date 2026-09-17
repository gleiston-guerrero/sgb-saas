package com.uteq.backend.repository;

import com.uteq.backend.entity.MessageChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageChatRepository extends JpaRepository<MessageChat, Long> {

    /** Lista los mensajes de la sesión dada del más antiguo al más reciente. */
    List<MessageChat> findBySessionIdOrderByCreatedAsc(UUID sessionId);
}
