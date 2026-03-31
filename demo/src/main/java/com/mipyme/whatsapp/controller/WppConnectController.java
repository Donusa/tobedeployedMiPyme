package com.mipyme.whatsapp.controller;

import com.mipyme.whatsapp.service.WppConnectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wpp/connect")
public class WppConnectController {

    private static final Logger logger = LoggerFactory.getLogger(WppConnectController.class);

    private final WppConnectService connectService;

    public WppConnectController(WppConnectService connectService) {
        this.connectService = connectService;
    }


    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startConnect() {
        try {
            Map<String, String> result = connectService.startSession();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error starting WPP connect session", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/finish")
    public ResponseEntity<Map<String, Object>> finishConnect(@RequestBody Map<String, String> payload) {
        try {
            String sid = payload.get("sid");
            String code = payload.get("code");
            String wabaId = payload.get("wabaId");
            String phoneNumberId = payload.get("phoneNumberId");

            if (sid == null || code == null || wabaId == null || phoneNumberId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Missing required fields: sid, code, wabaId, phoneNumberId"));
            }

            Map<String, Object> result = connectService.finishSession(sid, code, wabaId, phoneNumberId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error finishing WPP connect session", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
