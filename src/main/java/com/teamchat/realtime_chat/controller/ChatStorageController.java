package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.service.ChatStorageService;
import java.security.Principal;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatStorageController {
    private final ChatStorageService storage;
    public ChatStorageController(ChatStorageService storage) { this.storage=storage; }
    @GetMapping("/snapshot") public Map<String,Object> snapshot(Principal principal) { return storage.snapshot(principal); }
    @PostMapping("/sync") public void sync(Principal principal,@RequestBody Map<String,Object> request) { storage.sync(principal,request); }
    @PostMapping("/import") public void importLegacy(Principal principal,@RequestBody Map<String,Object> request) { storage.importLegacy(principal,request); }
    @PostMapping("/desktop") public Map<String,Boolean> desktop(Principal principal,HttpSession session,@RequestBody Map<String,String> request) {
        return Map.of("claimed",storage.claimDesktop(principal,request.getOrDefault("key",""),request.getOrDefault("action","claim"),session.getId()));
    }
}
