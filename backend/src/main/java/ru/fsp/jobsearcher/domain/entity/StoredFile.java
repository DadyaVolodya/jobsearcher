package ru.fsp.jobsearcher.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stored_file")
public class StoredFile {

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private String purpose;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
