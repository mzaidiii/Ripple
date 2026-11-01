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
@Table(name = "change_summaries")
public class ChangeSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id ;

    @NotNull
    private UUID jobId ;

    @NotNull
    @Column(columnDefinition = "text")
    private String summary ;

    @NotNull
    private String model ;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
