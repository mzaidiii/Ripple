package org.personal.ripple.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.personal.ripple.security.SignatureVerifier;
import org.personal.ripple.services.WebhookService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService  webhookService;
    private final SignatureVerifier signatureVerifier;

    @PostMapping("/github")
    public ResponseEntity<String> received (@RequestHeader("X-Github-Event") String event ,
                                            @RequestHeader("X-Github-Delivery") String deliveryID ,
                                            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature ,
                                            @RequestBody String payload ){

        if(!signatureVerifier.isValid(payload,signature)){
            return ResponseEntity.status(401).body("Invalid Signature");
        }
        if( event.equals("push")){
            try {
                webhookService.handlePush(deliveryID,payload);
            } catch (DataIntegrityViolationException e){
                if (isUniqueViolation(e)) {
                    log.info("Duplicate delivery {} (race)", deliveryID);
                    return ResponseEntity.ok("Duplicate(race)");
                }
                log.error("Constraint violation for delivery {}", deliveryID, e);
                throw e;
            }
            return ResponseEntity.ok("Update Received");
        }
        return ResponseEntity.ok("Update ignored");
    }

    private boolean isUniqueViolation(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
