package org.personal.ripple.repository;

import org.personal.ripple.entity.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    boolean existsByDeliveryId (String deliveryId);
}
