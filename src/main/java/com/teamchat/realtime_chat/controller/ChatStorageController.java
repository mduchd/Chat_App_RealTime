package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.service.ChatStorageService;
import java.security.Principal;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatStorageController {
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<Map<String,String>> error(org.springframework.web.server.ResponseStatusException error) {
        return org.springframework.http.ResponseEntity.status(error.getStatusCode()).body(Map.of("message",error.getReason()==null?"Không thể xử lý yêu cầu.":error.getReason()));
    }
    private final ChatStorageService storage;
    public ChatStorageController(ChatStorageService storage) { this.storage=storage; }
    @GetMapping("/snapshot") public Map<String,Object> snapshot(Principal principal) { return storage.snapshot(principal); }
    @PostMapping("/sync") public void sync(Principal principal,@RequestBody Map<String,Object> request) { storage.sync(principal,request); }
    @PostMapping("/import") public void importLegacy(Principal principal,@RequestBody Map<String,Object> request) { storage.importLegacy(principal,request); }
    @PostMapping("/continue") public Map<String,String> continueLegacy(Principal principal,@RequestBody Map<String,String> request) {
        return storage.continueLegacy(principal,request.getOrDefault("conversationId",""));
    }
    @PostMapping("/desktop") public Map<String,Boolean> desktop(Principal principal,HttpSession session,@RequestBody Map<String,String> request) {
        return Map.of("claimed",storage.claimDesktop(principal,request.getOrDefault("key",""),request.getOrDefault("action","claim"),session.getId()));
    }
}
