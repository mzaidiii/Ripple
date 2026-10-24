package org.personal.ripple.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "analysis_jobs")
public class AnalysisJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    private String deliveryId;

    @NotNull
    private String repoFullName;

    @NotNull
    private String beforeSha;

    @NotNull
    private String afterSha;

    @NotNull
    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;

    private int attempts = 0;

    @Column(columnDefinition = "text")
    private String lastError;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant finishedAt;
}