package org.personal.ripple.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "webhook_deliveries")
public class WebhookDelivery {

    @Id
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id ;

    @NotNull
    @Column(unique = true)
    private String deliveryId;

    @NotNull
    private String eventType;

    @NotNull
    private String repoFullName;

    @NotNull
    private String ref ;

    @NotNull
    private String beforeSha;

    @NotNull
    private String afterSha;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload ;

    @NotNull
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    @CreationTimestamp
    private Instant receivedAt ;
}
