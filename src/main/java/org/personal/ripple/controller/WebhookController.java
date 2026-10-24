package org.personal.ripple.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.personal.ripple.security.SignatureVerifier;
import org.personal.ripple.services.WebhookService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
                log.warn(e.getLocalizedMessage());
               return ResponseEntity.status(200).body("Duplicate(race)");
            }

            return ResponseEntity.ok("Update Received");
        }
        return ResponseEntity.ok("Update ignored");
    }
}
