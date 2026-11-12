package org.personal.ripple.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "repositories")
public class TrackedRepo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    private String fullName;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RepoStatus status = RepoStatus.PENDING;

    private String lastIndexedSha ;
    @Column(columnDefinition = "text")
    private String lastError;

    private Instant indexedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
