package ru.fsp.jobsearcher.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.fsp.jobsearcher.domain.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByThreadIdOrderByCreatedAtAsc(UUID threadId);

    @Query("""
            select count(m) from ChatMessage m
            where m.threadId = :threadId
              and m.senderUserId <> :viewerId
              and m.createdAt > :since
            """)
    long countUnread(
            @Param("threadId") UUID threadId,
            @Param("viewerId") UUID viewerId,
            @Param("since") Instant since
    );
}
