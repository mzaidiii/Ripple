package org.personal.ripple.controller;

import lombok.RequiredArgsConstructor;
import org.personal.ripple.security.SignatureVerifier;
import org.personal.ripple.services.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            webhookService.handlePush(deliveryID,payload);
            return ResponseEntity.ok("Update Received");
        }
        return ResponseEntity.ok("Update ignored");
    }
}
