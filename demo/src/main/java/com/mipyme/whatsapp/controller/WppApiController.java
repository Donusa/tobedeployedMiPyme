package com.mipyme.whatsapp.controller;

import com.mipyme.whatsapp.model.WppConversation;
import com.mipyme.whatsapp.model.WppConnection;
import com.mipyme.whatsapp.model.WppMessage;
import com.mipyme.whatsapp.service.WppApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wpp")
public class WppApiController {

    private static final Logger logger = LoggerFactory.getLogger(WppApiController.class);

    private final WppApiService apiService;

    public WppApiController(WppApiService apiService) {
        this.apiService = apiService;
    }


    @GetMapping("/conversations")
    public ResponseEntity<List<WppConversation>> getConversations() {
        return ResponseEntity.ok(apiService.getConversations());
    }


    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<WppMessage>> getMessages(@PathVariable Long id) {
        return ResponseEntity.ok(apiService.getMessages(id));
    }


    @PostMapping("/messages/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, String> payload) {
        try {
            String contactWaId = payload.get("to");
            String text = payload.get("text");

            if (contactWaId == null || text == null || text.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Missing required fields: to, text"));
            }

            WppMessage message = apiService.sendMessage(contactWaId, text);
            return ResponseEntity.ok(Map.of(
                    "sent", true,
                    "wamid", message.getWamid()));
        } catch (Exception e) {
            logger.error("Error sending message", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/conversations/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        apiService.markConversationRead(id);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/connection/setup")
    public ResponseEntity<Map<String, Object>> setupConnection(@RequestBody Map<String, String> payload) {
        try {
            String wabaId = payload.get("wabaId");
            String phoneNumberId = payload.get("phoneNumberId");
            String displayPhoneNumber = payload.get("displayPhoneNumber");
            String accessToken = payload.get("accessToken");

            if (wabaId == null || wabaId.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Missing required field: wabaId"));
            }
            if (phoneNumberId == null || phoneNumberId.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Missing required field: phoneNumberId"));
            }
            if (accessToken == null || accessToken.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Missing required field: accessToken"));
            }

            WppConnection connection = apiService.setupConnection(wabaId, phoneNumberId, displayPhoneNumber, accessToken);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "WhatsApp connection configured successfully",
                    "connectionId", connection.getId(),
                    "wabaId", connection.getWabaId(),
                    "phoneNumberId", connection.getPhoneNumberId(),
                    "displayPhoneNumber", connection.getDisplayPhoneNumber() != null ? connection.getDisplayPhoneNumber() : "",
                    "status", connection.getStatus().toString()
            ));
        } catch (Exception e) {
            logger.error("Error setting up WhatsApp connection", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/conversations/sync")
    public ResponseEntity<?> syncHistory(
            @RequestParam(defaultValue = "10") int preview) {
        try {
            return ResponseEntity.ok(apiService.syncHistory(preview));
        } catch (Exception e) {
            logger.error("Error syncing conversation history", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
